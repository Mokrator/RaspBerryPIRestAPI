package Server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Rene
 */
public class HttpHandlerNonRest implements HttpHandler {
//    private final RestApiServer RestApiServer;
    private final String baseURL;
    private final String localFolder;
    private final String allowedUrlChars;

    public HttpHandlerNonRest(String baseURL, String localFolder, String allowedUrlChars) throws IOException {
        this.baseURL = baseURL;
        this.localFolder = localFolder;
        this.allowedUrlChars = allowedUrlChars;
        File wwwroot = Path.of(RestApiServer.GetWorkPath(), localFolder).toFile();
        if (!wwwroot.exists()) {
            wwwroot.mkdir();
        }
        else if (!wwwroot.isDirectory()) {
            throw new IOException("wwwroot is a existing file!");
        }
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            
            String readFile;
            if (path.equals("/favicon.ico")) {
                readFile = "favicon.ico";
            }
            else if (path.length() == baseURL.length() || !RestApiServer.CheckIP(exchange.getRemoteAddress().getHostString())) {
                if (path.length() != baseURL.length()) {
                    RestApiServer.log.log(Level.INFO, "retry from blocked in NonRestHandler {0}", exchange.getRequestURI());
                }
                readFile = "index.html";
            }
            else {
                readFile = path.substring(baseURL.length());
            }
            Headers responseHeaders = exchange.getResponseHeaders();
            switch (readFile.substring(readFile.lastIndexOf("."))) {
                case ".html" -> responseHeaders.add("content-type", "text/html; charset=utf-8");
                case ".js" -> responseHeaders.add("content-type", "application/javascript");
                case ".css" -> responseHeaders.add("content-type", "text/css");
                case ".ico" -> responseHeaders.add("content-type", "image/x-icon");
                case ".jpg" -> responseHeaders.add("content-type", "image/jpeg");
            }
            File localFile = Path.of(RestApiServer.GetWorkPath(), localFolder, readFile).toFile();
            if (!readFile.matches("^["+allowedUrlChars+"]*$") || !Path.of(RestApiServer.GetWorkPath(), localFolder).toFile().exists() || !localFile.exists() || !localFile.canRead()) {
                RestApiServer.log.log(Level.WARNING, "File not Found {0}", readFile);
                RestApiServer.ErrorOutput(exchange, responseHeaders, 404);
            }
            else {
                //httpsServer.log.log(Level.INFO, "retry from blocked in NonRestHandler {0}", exchange.getRequestURI());
                RestApiServer.log.log(Level.FINEST, "File read {0}", readFile);
                exchange.sendResponseHeaders(200, localFile.length());
                exchange.getResponseBody().write(FileUtils.readFileToByteArray(localFile));
            }
        }
    }
}
