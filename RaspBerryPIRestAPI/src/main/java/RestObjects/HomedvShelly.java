package RestObjects;

import Homedv.HomeDevice;
import Bitsnbytes.SqlParameter;
import Bitsnbytes.Utils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Properties;
import org.json.JSONObject;

/**
 *
 * @author Rene
 */
public final class HomedvShelly extends HomeDevice {
    public static HomeDevice SearchDevice() {
        return new HomedvShelly("192.168.33.1", "admin", "", true);
    }
    
    public HomedvShelly(String hostname, String username, String password, boolean readConfig) {
        SetData(hostname, username, password, 80);
        if (readConfig) {
            ReadConfig();
        }
    }

    @Override
    public void ReadConfig() {
        HttpRequest request = CreateRequest("/settings/");
        HttpClient client = HttpClient.newHttpClient();
        client.sendAsync(request, BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() == 200) {
                JSONObject settingData = new JSONObject(response.body());
                if (settingData.has("device") && settingData.has("wifi_ap"))
                {
                    
                }
            }
            return response;
        });
    }
    
    public static HomeDevice AddKnownDevice(String hostname, String username, String password) {
        HomedvShelly retValue = new HomedvShelly(hostname, username, password, true);
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
        URI u = URI.create("http://" + this.GetHostname() + ":"+ String.valueOf(this.GetApiport()) + relUrl);
        return HttpRequest.newBuilder().uri(u).header("content-type", "application/json").header("Authorization", Utils.BasicAuth(this.GetUsername(), this.GetPassword())).build();
    }
}
