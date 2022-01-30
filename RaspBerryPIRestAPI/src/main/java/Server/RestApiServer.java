package Server;

import DataStorage.DatabaseForRest;
import DataStorage.TableObject;
import Bitsnbytes.SqlInformation;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.security.KeyStore;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.AbstractList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import com.sun.net.httpserver.HttpServer;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 *
 * @author Rene
 */
public final class RestApiServer {
    public static final Logger log = Logger.getAnonymousLogger();
    public static Set<Class<? extends TableObject>> dbTableObjectClasses;
    public static Set<Class<? extends TableObject>> allTableObjectClasses;
    public static final int SESSIONTIMEOUT = 46;
    
    private static final int SSLPORT = 3674;
    private static final int HOMEPORT = 88;
    private static final boolean GETINPUTFROMHOMEBYHTTPS = false;
    private static final boolean RUNALLSERVICEHTTP = false;
    private static final String HOMEURL = "/activity/";
    private static final String BASEURL = "/";
    private static final String RESTURL = "/rest/";
    private static final String ALLOWURLCHARS = "\\-\\.\\w";
    private static final int SAVEINTERVAL = 3600*3;
    private static final int PURGEUNUSEDOBJECTAFTER = 3600;
    private static final int PURGECHECKINTERVAL = 300;
    
    private static String localFolder;
    private static char[] keystorePassword;
    private static String workPath;
    private static HttpsServer httpsServer;
    private static HttpServer httpServer;
    private static final Timer checkTimer = new Timer("checkTimer");
    private static final Map<String,Calendar> ipBlocks = new HashMap<>();
    private static DatabaseForRest myRestDB;
    private static boolean serviceIsStopping = false;
    private static Date nextDbSave = new Date();
    private static boolean timerActivity = false;
    private static boolean startShutdown = false;
    private static final Date nextCheckCache = new Date();
    

    /**
     * On Objectchanges edit following methods:
     * TableObject.CreateFromRes (only for NEW tables)
     * RestApiServer.SaveDatabase (only for NEW tables)
     * XTableX.CreateFromRes (any changes to datastructure)
     * XTableX.GetChangesSQL (any changes to datastructure)
     * XTableX.GetUniqueColumns (if the unique-keys are changed)
     * XTableX.CheckIsSame (if the unique-keys are changed)
     * XTableX.GetClientData (for new Outputs)
     * XTableX.GetAdminData (for new Outputs)
     * DatabaseForRest.PrepareRestApiDB (remember to change the existing tables or build in versioncheck if you change columns)
     */
    private static void CleanupCache() throws NoSuchFieldException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Date deleteBefore = new Date();
        deleteBefore.setTime(deleteBefore.getTime() - 1000*PURGEUNUSEDOBJECTAFTER);
        for (Class<? extends TableObject> o : RestApiServer.allTableObjectClasses) {
            try {
                o.getMethod("CleanupCache", Date.class, AbstractList.class).invoke(null, deleteBefore, (AbstractList<TableObject>)o.getField("known").get(null));
            } catch (NoSuchMethodException | SecurityException ex) {
                log.severe(TableObject.class.getName() + " " +  ex.getMessage());
            }
        }
    }
    private static boolean SaveDatabase() {
        int errCount = 0;
        boolean success = false;
        while (!success && errCount < 15) {
            try {
                for (Class<? extends TableObject> o : RestApiServer.allTableObjectClasses) {
                    SaveKnown((AbstractList<TableObject>)o.getField("known").get(null));
                }
                SetNextDbSave();
                success = true;
            } catch (SQLException | NoSuchFieldException | IllegalAccessException ex) {
                log.severe(ex.getMessage());
                errCount = 15;
            } catch(java.util.ConcurrentModificationException ex) {
                log.warning("java.util.ConcurrentModificationException while SAVING!");
                errCount++;
                if (errCount < 15) {
                    subsleep(200);
                }
                else {
                    log.severe("FINAL java.util.ConcurrentModificationException while SAVING!");
                }
            }
        }
        return success;
    }
    
    private static void SaveKnown(AbstractList<TableObject> known) throws SQLException {
        for (TableObject o : known) {
            if (o.GetNeedInsert() || o.GetNeedUpdate()) {
                SqlInformation sqlInformation = new SqlInformation();
                AppendCom(sqlInformation, o);
                myRestDB.RunSaveCommands(sqlInformation);
            }
        }
    }
    public static boolean IsTimerActive() {return timerActivity; }
    private static final TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            timerActivity = true;
            if (startShutdown) try {
                startShutdown = false;
                StopSocketService();
                timerActivity = false;
                return;
            }
            catch (InterruptedException | SQLException | NoSuchFieldException | IllegalAccessException ex) {
                log.severe(ex.getMessage());
            }
            if (serviceIsStopping) return;
            Date now = new Date();
            if (nextDbSave.before(now)) {
                SaveDatabase();
            }
            else if (nextCheckCache.before(now)) {
                try {
                    nextCheckCache.setTime(now.getTime() + 1000*PURGECHECKINTERVAL);

                    CleanupCache();

                    RestApiServer.log.warning("clearing objects finished");
                }
                catch(java.util.ConcurrentModificationException ex) {
                    log.severe("java.util.ConcurrentModificationException while CLEANUP");
                } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    log.severe(ex.getMessage());
                }
            }
            timerActivity = false;
        }
    };

    public static void AppendCom(SqlInformation sqlInformation, TableObject tableObject) {
        sqlInformation.sql += tableObject.GetChangesSQL(sqlInformation.parameters) + "\r\n";
        sqlInformation.updatedObjects.add(tableObject);
    }

    public static void SetNextDbSave() {
        nextDbSave.setTime(new Date().getTime()+SAVEINTERVAL*1000); // save all 4 hours and on exit, rest runs in memory...
    }
    
    public static Connection GetConnection() {
        return myRestDB.sqlCon;
    }
    
    /**
     * in the working directory (check GetWorkPath)
     * Save the pkcs12 formated (some binary file) ssh-key into the file https.pkcs12
     * 
     * @throws FileNotFoundException
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     * @throws SQLException 
     */
    public static void Start() throws FileNotFoundException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException, SQLException {
        if (SSLPORT < 1 || SSLPORT > 65535) {
            throw new SocketException("Configured SSL Port out of valid Range Exception");
        }
        if (HOMEPORT < 1 || HOMEPORT > 65535 || HOMEPORT == SSLPORT) {
            throw new SocketException("Configured Home Port out of valid Range Exception");
        }
        workPath = ReturnDeterminedWorkPath("RestApi");
        if ("/".equals(BASEURL)) {
            localFolder = "wwwroot".replaceAll("[^"+ALLOWURLCHARS+"]", "");
        }
        else {
            localFolder = BASEURL.replaceAll("[^"+ALLOWURLCHARS+"]", "");
        }
        String lineBreak = "\n";
        log.warning(String.format("Information:" + lineBreak + "Working directory is "+workPath+" !" + lineBreak + "Webroot directory for html files is "+localFolder+" !" + lineBreak + "Store the passwort to the keystore base64encoded as UTF-8 file in the working-directory named https.pkcs12.pwd !\n" + "Store the pkcs12 encoded binary file ssh-private key in the working-directory named https.pkcs12 !" + lineBreak + lineBreak));
        httpsServer = HttpsServer.create(new InetSocketAddress(SSLPORT), 0);
        httpServer = HttpServer.create(new InetSocketAddress(HOMEPORT), 0);
        File pwdfile = Path.of(workPath, "https.pkcs12.pwd").toFile();
        if (pwdfile.exists() && pwdfile.isFile() && pwdfile.canRead()) {
            keystorePassword = new String(Base64.getDecoder().decode(FileUtils.readFileToString(pwdfile, StandardCharsets.UTF_8).replace("\uFEFF", "")), StandardCharsets.UTF_8).toCharArray();
        }
        else {
            throw new FileNotFoundException("Password not found.");
        }
        
        myRestDB = new DatabaseForRest(GetWorkPath());
        nextCheckCache.setTime(nextCheckCache.getTime() + 1000*PURGECHECKINTERVAL);
        RunSocketService();
        SetNextDbSave();
        checkTimer.scheduleAtFixedRate(timerTask, 30, 300);
        
    }

    private static void RunSocketService() throws FileNotFoundException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException {
         // initialise the keystore
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream(Path.of(GetWorkPath(), "https.pkcs12").toString());
        ks.load(fis, keystorePassword);

        // setup the key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keystorePassword);
        // setup the trust manager factory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
        // setup the HTTPS context and parameters
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                try {
                    // initialise the SSL context
                    SSLContext context = getSSLContext();
                    SSLEngine engine = context.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    // Set the SSL parameters
                    SSLParameters sslParameters = context.getSupportedSSLParameters();
                    params.setSSLParameters(sslParameters);
                }
                catch (Exception ex) {
                    System.out.println("Failed to create HTTPS port");
                }
            }
        });
        
        httpServer.setExecutor(Executors.newCachedThreadPool()); // creates a default executor for multithreaded server
        httpsServer.setExecutor(Executors.newCachedThreadPool()); // creates a default executor for multithreaded server
        
        HttpHandlerActivity httpHandlerHome = new HttpHandlerActivity();
        HttpHandlerRest handleRest = new HttpHandlerRest();
        HttpHandlerNonRest handleNonRest = new HttpHandlerNonRest(BASEURL, localFolder, ALLOWURLCHARS);
        
        httpsServer.createContext(RESTURL, (HttpHandler)handleRest);
        
        if (RUNALLSERVICEHTTP) {
            httpServer.createContext(RESTURL, (HttpHandler)handleRest);
            httpServer.createContext(BASEURL, (HttpHandler)handleNonRest);
            if (!"/".equals(BASEURL)){
                httpServer.createContext("/favicon.ico", (HttpHandler)handleNonRest);
            }
        }
        else {
            httpsServer.createContext(RESTURL, (HttpHandler)handleRest);
            httpsServer.createContext(BASEURL, (HttpHandler)handleNonRest);
            if (!"/".equals(BASEURL)){
                httpsServer.createContext("/favicon.ico", (HttpHandler)handleNonRest);
            }
        }
        if (GETINPUTFROMHOMEBYHTTPS) {
            httpsServer.createContext(HOMEURL, (HttpHandler)httpHandlerHome);
        }
        else {
            httpServer.createContext(HOMEURL, (HttpHandler)httpHandlerHome);
        }
        httpServer.start();
        httpsServer.start();
    }

    public static void BlockIP(String remoteAddress, Calendar blockUntil) {
        ipBlocks.put(remoteAddress, blockUntil);
    }
    
    public static boolean CheckIP(String remoteAddress) {
        ipBlocks.forEach((BiConsumer<? super String, ? super Calendar>) (checkRemAddr, blockUntil) -> {
            if (blockUntil.before(Calendar.getInstance())) {
                ipBlocks.remove(checkRemAddr);
            }
        });
        return !ipBlocks.containsKey(remoteAddress);
    }

    public static boolean GetServiceStopping() {
        return serviceIsStopping;
    }
    public static void StopService() {
        if (!serviceIsStopping) startShutdown = true;
    }
    
    public static void StopSocketService() throws InterruptedException, SQLException, NoSuchFieldException, IllegalAccessException {
        serviceIsStopping = true;
        checkTimer.cancel();
        if (!SaveDatabase()) {
            log.severe("Database not Saved!");
        }
        httpServer.stop(1);
        httpsServer.stop(3);
        myRestDB.Stop();
    }
    
    public static void subsleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            log.severe(RestApiServer.class.getName() + " " + ex.getMessage());
        }
    }
    
    public static String GetWorkPath() {
        return workPath;
    }
    
    public static String GetRestPath() {
        return RESTURL;
    }
    
    private static String ReturnDeterminedWorkPath(String appName) throws FileNotFoundException {
        String[] configPaths = {"APPDATA", "HOME"};
        String configPath = null;
        for (String myPath: configPaths) {
            configPath = System.getenv(myPath);
            if (configPath != null) break;
        }
        if (configPath == null) {
            throw new FileNotFoundException("BasePath for the settings-directory not found, check the enviroment-variable HOME (Linux) or APPDATA (Windows).");
        }
        if (!Path.of(configPath, appName).toFile().exists()) {
            Path.of(configPath, appName).toFile().mkdir();
        }

        if (!Path.of(configPath, appName, "https.pkcs12").toFile().exists()) {
            if (configPath == null) throw new FileNotFoundException("BasePath for the settings-directory not found, check the enviroment-variable HOME (Linux) or APPDATA (Windows).");
        }
        return Path.of(configPath, appName).toString();
    }
    
    public static void ErrorOutput(HttpExchange exchange, Headers responseHeaders, int error) throws IOException {
        File errorData = Path.of(GetWorkPath(), localFolder, "errors", String.valueOf(error) + ".html").toFile();
        if ((errorData.exists() && errorData.canRead()) && "GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(error, errorData.length());
            OutputStream out = exchange.getResponseBody();
            out.write(FileUtils.readFileToByteArray(errorData));
        }
        else {
            exchange.sendResponseHeaders(error, 0);
        }
    }
}
