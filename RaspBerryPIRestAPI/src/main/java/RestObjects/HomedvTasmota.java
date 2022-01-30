package RestObjects;

import Bitsnbytes.SqlParameter;
import Bitsnbytes.Utils;
import static RestObjects.Homedevice.GetUniqueColumns;
import static RestObjects.Homedevice.known;
import static Server.RestApiServer.log;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;

/**
 *
 * @author Rene
 */
public final class HomedvTasmota extends Homedevice {

    public static Homedevice SearchDevice(Connection sqlCon) throws SQLException {
        return new HomedvTasmota("192.168.4.1", "admin", "", true, Calendar.getInstance(), null, Calendar.getInstance(), sqlCon);
    }
    
    public static Homedevice AddKnownDevice(String hostname, String username, String password, Connection sqlCon) throws SQLException {
        return new HomedvTasmota(hostname, username, password, true, Calendar.getInstance(), null, Calendar.getInstance(), sqlCon);
    }

    public HomedvTasmota(String hostname, String username, String password, boolean readConfig, Calendar devicecreated, Calendar lastresponse, Calendar lastdeviceupdate, Connection sqlCon) throws SQLException {
        SetId(-1);
        SetData("tasmota", hostname, username, password, 80, readConfig, devicecreated, lastresponse, lastdeviceupdate, sqlCon);
    }
    
    @Override
    public void UpdateNewDevice() {
        
    }
    
    @Override
    public void ReadConfig(Connection sqlCon) {
        HttpRequest request = CreateRequest("/?m=1");
        HttpClient client = HttpClient.newHttpClient();
        client.sendAsync(request, BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() == 200) {
                if (response.body().startsWith("{t}{s}Voltage")) {
                    SetLastresponse(Calendar.getInstance());
                }
                try {
                    AddKnown(-1, sqlCon, "Homedevice", GetUniqueColumns(), new String[] { GetHostname() }, known);
                } catch (SQLException ex) {
                    log.severe(ex.getMessage());
                }
            }
            return response;
        });
    }

    @Override
    public HttpRequest CreateRequest(String relUrl) {
        URI u = URI.create("http://" + this.GetHostname() + ":"+ String.valueOf(this.GetApiport())
                //+ "/cm?user=" + URLEncoder.encode(this.GetUsername(), Charset.defaultCharset()) + "&password=" + URLEncoder.encode(this.GetPassword(), Charset.defaultCharset())
                + relUrl);
        return HttpRequest.newBuilder().uri(u).header("content-type", "text/html").header("Authorization", Utils.BasicAuth(this.GetUsername(), this.GetPassword())).build();
    }
}
