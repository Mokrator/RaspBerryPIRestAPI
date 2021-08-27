package RestObjects;

import Homedv.Homedevice;
import Bitsnbytes.SqlParameter;
import Bitsnbytes.Utils;
import DataStorage.TableObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Rene
 */
public final class HomedvShelly extends Homedevice {
    
    public static Homedevice SearchDevice(Connection sqlCon) throws SQLException {
        return new HomedvShelly("192.168.33.1", "admin", "", true, new Date(), null, new Date(), sqlCon);
    }
    
    public static Homedevice AddKnownDevice(String hostname, String username, String password, Connection sqlCon) throws SQLException {
        return new HomedvShelly(hostname, username, password, true, new Date(), null, new Date(), sqlCon);
    }
    
    public HomedvShelly(String hostname, String username, String password, boolean readConfig, Date devicecreated, Date lastresponse, Date lastdeviceupdate, Connection sqlCon) throws SQLException {
        SetData("shelly", hostname, username, password, 80, readConfig, devicecreated, lastresponse, lastdeviceupdate);
        AddKnown(GetId(), sqlCon, "Homedevice", GetUniqueColumns(), new String[] { hostname }, known);
    }
        
    @Override
    public void UpdateNewDevice() {
        
    }

    @Override
    public void ReadConfig() {
        HttpRequest request = CreateRequest("/settings/");
        HttpClient client = HttpClient.newHttpClient();
        client.sendAsync(request, BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() == 200) {
                JSONObject settingData = new JSONObject(response.body());
                if (settingData.has("device") && settingData.has("wifi_ap")) {
                    SetHostname(settingData.getJSONObject("device").getString("hostname"));
                    SetLastresponse(new Date());
                }
                if (settingData.has("relays")) {
                    JSONArray relays = settingData.getJSONArray("relays");
                    AddSubs(this, "Relays", relays.length());
                    AddSubs(this, "Input", relays.length());
                }
                if (settingData.has("meters")) {
                    JSONArray meters = settingData.getJSONArray("meters");
                    AddSubs(this, "Meter", meters.length());
                }
            }
            return response;
        });
    }

    /**
     * must be static to use it without an object, so its neither in the interface ir abstract in TableObject...
     * @param res a resultrow whose select need to fit to the order of columns
     * @return a object of my type
     * @throws SQLException 
     */
    public static TableObject CreateFromRes(ResultSet res) throws SQLException {
        HomedvShelly retValue = new HomedvShelly(res.getString(3), res.getString(4), res.getString(5), false, Utils.SetDate(res.getLong(6)), Utils.SetDate(res.getLong(7)), Utils.SetDate(res.getLong(8)), null);
        retValue.SetId(res.getInt(1)); //String devicetype = res.getString(2);
        AddSubs(retValue, "Relay", res.getInt(9));
        AddSubs(retValue, "Meter", res.getInt(10));
        AddSubs(retValue, "Input", res.getInt(11));
        AddSubs(retValue, "Dimmer", res.getInt(12));
        AddSubs(retValue, "Camera", res.getInt(13));
        AddSubs(retValue, "Rgbled", res.getInt(14));
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
