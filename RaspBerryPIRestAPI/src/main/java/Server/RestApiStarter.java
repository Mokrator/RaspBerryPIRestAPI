package Server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 *
 * @author Rene
 */
public class RestApiStarter {
    //private static ;
    public static void main(String[] args) throws IOException, SocketException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException, FileNotFoundException, SQLException
    {
        ConsoleHandler handler = new ConsoleHandler();
        RestApiServer.log.addHandler(handler);
        handler.setLevel(Level.FINEST);
        RestApiServer.log.setLevel(Level.FINEST);
        RestApiServer.Start();
    }
}
