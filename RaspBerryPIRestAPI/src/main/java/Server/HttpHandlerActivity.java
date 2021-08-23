package Server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.logging.Level;

/**
 * Handles Requests from active HomeDevices (like Switches) and updates Status in the server.
 * shold run localy with http or on the internet with https only.
 * minimal authentification should get provided by get params if used by https over the internet
 * Devices should be limited to http if device is NOT on the internet.
 * @author Rene
 */
public class HttpHandlerActivity implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            Headers responseHeaders = exchange.getResponseHeaders();
            //responseHeaders.add("accept", "application/json; charset=utf-8");
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
            else if (!exchange.getRequestMethod().equals("GET")) {  // wrong method
                blockUntil.add(Calendar.MINUTE, 15);
                RestApiServer.BlockIP(remoteAddress, blockUntil);
                RestApiServer.log.log(Level.SEVERE, "Blocked IP in RestHandler for nonpost {0}", exchange.getRequestURI());
                RestApiServer.ErrorOutput(exchange, responseHeaders, 405);
            }
            else {
                responseHeaders.add("content-type", "application/json; charset=utf-8");
            }
        }
    }
}
