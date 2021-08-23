package Server;

import Bitsnbytes.Utils;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import RestObjects.CommandHandler;
import RestObjects.UserSession;

/**
 *
 * @author Rene
 */
public class HttpHandlerRest implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add("accept", "application/json; charset=utf-8");
            responseHeaders.add("cache-control", "no-cache, no-store, must-revalidate");
            responseHeaders.add("Pragma", "no-cache");
            responseHeaders.add("Expires", "Mon, 04 Jan 2021 00:00:00 GMT");
            Calendar blockUntil = Calendar.getInstance(); // for ip-blocks
                    
            
            String remoteAddress = exchange.getRemoteAddress().getHostString();
            if (!RestApiServer.CheckIP(remoteAddress)) {
                RestApiServer.log.log(Level.INFO, "retry from blocked in RestHandler {0}", exchange.getRequestURI());
                try (OutputStream w = exchange.getResponseBody()) {
                    byte[] output = ("{\"updatetime\":" + Calendar.getInstance().getTimeInMillis() + ",\"status\":\"ok\"}").getBytes("utf-8");
                    exchange.sendResponseHeaders(200, output.length);
                    w.write(output);
                }
            }
            else if (!exchange.getRequestMethod().equals("POST")) {  // wrong method
                blockUntil.add(Calendar.MINUTE, 15);
                RestApiServer.BlockIP(remoteAddress, blockUntil);
                RestApiServer.log.log(Level.SEVERE, "Blocked IP in RestHandler for nonpost {0}", exchange.getRequestURI());
                RestApiServer.ErrorOutput(exchange, responseHeaders, 405);
            }
            else if (!CheckContentTypeIsJson(exchange)) {       // wrong header for type of data
                blockUntil.add(Calendar.MINUTE, 15);
                RestApiServer.BlockIP(remoteAddress, blockUntil);
                RestApiServer.log.log(Level.SEVERE, "Blocked IP in RestHandler for nonjson {0}", exchange.getRequestURI());
                RestApiServer.ErrorOutput(exchange, responseHeaders, 415);
            }
            else {
                responseHeaders.add("content-type", "application/json; charset=utf-8");
                String path = exchange.getRequestURI().getPath().substring(RestApiServer.GetRestPath().length()) ;
                String sessionid = exchange.getRequestURI().getQuery();
                if (sessionid != null && sessionid.length() != 64) sessionid = null;
                
                UserSession userSession;
                
                // Command-processing and buildup returnValue - prechecks ok, commandy might want to check if given data is ok.
                Properties returnValue = new Properties();
                Calendar updatetime = Calendar.getInstance();
                returnValue.put("updatetime", updatetime.getTimeInMillis());
                if (RestApiServer.GetServiceStopping()) {
                    RestApiServer.log.log(Level.WARNING, "access while stopping {0}", exchange.getRequestURI());
                    returnValue.put("status", "servicestopping");
                }
                else if (path.equals("getsession")) {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    userSession = UserSession.CreateUserSession(remoteAddress, Utils.Sha256(Double.toString(Math.random()) + "_" + remoteAddress, digest), updatetime);
                    RestApiServer.log.log(Level.FINE, "new Session {0}", userSession.GetSessionId());
                    returnValue.put("status", "ok");
                    returnValue.put("sessionTimeout", RestApiServer.SESSIONTIMEOUT);
                    returnValue.put("sessionid", userSession.GetSessionId());
                }
                else if ((userSession = UserSession.GetExistingUserSession(remoteAddress, sessionid)) == null) {
                    RestApiServer.log.log(Level.WARNING, "request without Session {0}", sessionid);
                    returnValue.put("status", "sessioninvalid");
                } else try {
                    /**
                     * Only process POST-Data if we have really a session (be aware of hacktries with compromited postData).
                     * catch jsonerrors: nonexistent Keys or not readable data from postData will result in malformedpostdata error
                     */
                    JSONObject postData = new JSONObject(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    if (postData.getBigInteger("lastupdate").longValue() != userSession.GetLastUpdate().getTimeInMillis()) {
                        RestApiServer.log.log(Level.WARNING, "lastupdatewrong Session {0}", sessionid);
                        userSession.LogOut("lastupdatewrong");
                        returnValue.put("status", "lastupdatewrong");
                        System.out.print("RestApiServer.Java:CheckSessionOk " + DateFormat.getTimeInstance(0, Locale.GERMAN).format(new Date()) + " (attackwarning) - lastupdatewrong \r\n");
                    }
                    else {
                        userSession.lastUsed = new Date();
                        if (userSession.GetUser() != null) {
                            userSession.GetUser().lastUsed = userSession.lastUsed;
                        }
                    }

                    userSession.SetUpdateTime(updatetime);

                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    
                    /*************************** Start Commands Processing! ***************************/
                    CommandHandler.HandleCom(path, userSession, postData, returnValue, blockUntil, remoteAddress, digest);
                    /*************************** End Commands Processing! ***************************/
                    RestApiServer.log.log(returnValue.getProperty("status").equals("ok")?Level.FINEST:Level.FINE, "command {0} - status {1} - Session {2}", new Object[]{path, returnValue.getProperty("status"), sessionid});
                }
                catch (JSONException e) {
                    blockUntil.add(Calendar.MINUTE, 15);
                    RestApiServer.BlockIP(remoteAddress, blockUntil);
                    RestApiServer.log.log(Level.SEVERE, "malformedpostdata {0} Session {1}", new Object[]{path, sessionid});
                    returnValue.put("status", "malformedpostdata");
                } catch (SQLException ex) {
                    returnValue.put("status", "dberror");
                    RestApiServer.log.log(Level.SEVERE, null, ex);
                }
                // returnValue ready for output, send http 200 and returnValue as json-body
                try (OutputStream w = exchange.getResponseBody()) {
                    byte[] output = JSONObject.valueToString(returnValue).getBytes("utf-8");
                    exchange.sendResponseHeaders(200, output.length);
                    w.write(output);
                }
            }
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(HttpHandlerRest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean CheckContentTypeIsJson(HttpExchange exchange) {
        Headers requestHeaders = exchange.getRequestHeaders();
        boolean isJson = false;
        for (String headerKey : requestHeaders.keySet()) {
            if (headerKey.toLowerCase().equals("content-type") && requestHeaders.get(headerKey).get(0).toLowerCase().equals("application/json; charset=utf-8")) {
                isJson = true;
            }
        }
        return isJson;
    }
}
