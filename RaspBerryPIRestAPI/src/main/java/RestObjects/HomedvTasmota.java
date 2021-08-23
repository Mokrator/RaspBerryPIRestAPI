package RestObjects;

import Homedv.HomeDevice;
import Bitsnbytes.SqlParameter;
import Bitsnbytes.Utils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author Rene
 */
public final class HomedvTasmota extends HomeDevice {
    public static HomeDevice SearchDevice() {
        return new HomedvTasmota("192.168.4.1", "admin", "", true);
    }
    
    public HomedvTasmota(String hostname, String username, String password, boolean readConfig) {
        SetData(hostname, username, password, 80);
        if (readConfig) {
            ReadConfig();
        }
    }
    
    @Override
    public void ReadConfig() {
        HttpRequest request = CreateRequest("/?m=1");
        HttpClient client = HttpClient.newHttpClient();
        client.sendAsync(request, BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() == 200) {
                if (response.body().startsWith("{t}{s}Voltage")) {
                    
                }
            }
            return response;
        });
    }
    
    public static HomeDevice AddKnownDevice(String hostname, String username, String password) {
        HomedvTasmota retValue = new HomedvTasmota(hostname, username, password, true);
        return retValue;
    }

    @Override
    public Properties GetClientData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Properties GetAdminData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String GetChangesSQL(ArrayList<SqlParameter> parameters) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public HttpRequest CreateRequest(String relUrl) {
        URI u = URI.create("http://" + this.GetHostname() + ":"+ String.valueOf(this.GetApiport())
                //+ "/cm?user=" + URLEncoder.encode(this.GetUsername(), Charset.defaultCharset()) + "&password=" + URLEncoder.encode(this.GetPassword(), Charset.defaultCharset())
                + relUrl);
        return HttpRequest.newBuilder().uri(u).header("content-type", "text/html").header("Authorization", Utils.BasicAuth(this.GetUsername(), this.GetPassword())).build();
    }
}
