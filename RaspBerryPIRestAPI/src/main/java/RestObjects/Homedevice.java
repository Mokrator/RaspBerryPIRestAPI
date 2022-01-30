package RestObjects;

import Bitsnbytes.SqlParameter;
import DataStorage.TableObject;
import HomeDv.HomedeviceInterface;
import java.util.AbstractList;
import java.util.ArrayList;
import HomeSubDv.SubDvRelais;
import HomeSubDv.SubDvMeter;
import HomeSubDv.SubDvInput;
import HomeSubDv.SubDvRgbled;
import HomeSubDv.SubDvDimmer;
import HomeSubDv.SubDvCamera;
import Server.RestApiServer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Properties;

/**
 *
 * @author Rene
 */
public abstract class Homedevice extends TableObject implements HomedeviceInterface {
    public static final AbstractList<TableObject> known = new ArrayList<TableObject>();
    public final AbstractList<SubDvCamera> cameraList = new ArrayList<>();
    public final AbstractList<SubDvRelais> relaisList = new ArrayList<>();
    public final AbstractList<SubDvMeter> meterList = new ArrayList<>();
    public final AbstractList<SubDvInput> inputList = new ArrayList<>();
    public final AbstractList<SubDvRgbled> rgbledList = new ArrayList<>();
    public final AbstractList<SubDvDimmer> dimmerList = new ArrayList<>();
    private int homedeviceid;
    private String devicetype;
    private String hostname;
    private int apiport;
    private boolean needreconfig;
    private String username;
    private String password;
    private Calendar devicecreated;
    private Calendar lastresponse;
    private Calendar lastdeviceupdate;
    
    private static final int CURRENT_TABLE_VERSION = 1;
    public static int SQLPrepare(int isversion, Connection sqlCon) throws SQLException {
        Statement updateCom = sqlCon.createStatement();
        if (isversion == 0) {
            updateCom.execute("create table " + Homedevice.class.getSimpleName().toLowerCase() + "s (homedeviceid INTEGER PRIMARY KEY, devicetype TEXT, hostname TEXT UNIQUE, username TEXT, password TEXT, devicecreated INTEGER, lastresponse INTEGER, lastdeviceupdate INTEGER, relaiscount INTEGER, metercount INTEGER, inputcount INTEGER, dimmercount INTEGER, cameracount INTEGER, rgbledcount INTEGER)");
            isversion++;
        }
        else while (isversion < CURRENT_TABLE_VERSION) {
            if (isversion == 1) {
                updateCom.execute("alter table homedevices ");
            }
            isversion++;
        }
        return isversion;
    }
    
    @Override
    public String GetChangesSQL(ArrayList<SqlParameter> parameters) {
        parameters.add(new SqlParameter(SqlParameter.ParType.String, devicetype));
        parameters.add(new SqlParameter(SqlParameter.ParType.String, hostname));
        parameters.add(new SqlParameter(SqlParameter.ParType.String, username));
        parameters.add(new SqlParameter(SqlParameter.ParType.String, password));
        parameters.add(new SqlParameter(SqlParameter.ParType.Calendar, devicecreated));
        parameters.add(new SqlParameter(SqlParameter.ParType.Calendar, lastresponse));
        parameters.add(new SqlParameter(SqlParameter.ParType.Calendar, lastdeviceupdate));
        parameters.add(new SqlParameter(SqlParameter.ParType.Integer, relaisList.size()));
        parameters.add(new SqlParameter(SqlParameter.ParType.Integer, meterList.size()));
        parameters.add(new SqlParameter(SqlParameter.ParType.Integer, inputList.size()));
        parameters.add(new SqlParameter(SqlParameter.ParType.Integer, dimmerList.size()));
        parameters.add(new SqlParameter(SqlParameter.ParType.Integer, cameraList.size()));
        parameters.add(new SqlParameter(SqlParameter.ParType.Integer, rgbledList.size()));
        parameters.add(new SqlParameter(SqlParameter.ParType.Integer, homedeviceid));
        if (this.GetNeedInsert()) {
            return "insert into homedevices (devicetype, hostname, username, password, devicecreated, lastresponse, lastdeviceupdate, relaiscount, metercount, inputcount, dimmercount, cameracount, rgbledcount, homedeviceid) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        }
        if (this.GetNeedUpdate()) {
            return "update homedevices set devicetype=?, hostname=?, username=?, password=?, devicecreated=?, lastresponse=?, lastdeviceupdate=?, relaiscount=?, metercount=?, inputcount=?, dimmercount=?, cameracount=?, rgbledcount=? where homedeviceid=?;";
        }
        else {
            RestApiServer.log.severe("ERROR no update or insert needed");
            return "ERROR";
        }
    }
    

    public void SetData(String devicetype,String hostname, String username, String password, int apiport, boolean readConfig, Calendar devicecreated, Calendar lastresponse, Calendar lastdeviceupdate, Connection sqlCon) throws SQLException {
        SetDevicetype(devicetype);
        SetHostname(hostname);
        SetApiport(apiport);
        SetUsername(username);
        SetPassword(password);
        SetDevicecreated(devicecreated);
        SetLastresponse(lastresponse);
        SetLastdeviceupdate(lastdeviceupdate);
        if (readConfig) {
            ReadConfig(sqlCon);
        }
        else {
            AddKnown(-1, sqlCon, "Homedevice", GetUniqueColumns(), new String[] { hostname }, known);
        }
        UpdateNewDevice();
    }
    public void SetDevicecreated(Calendar devicecreated) {
        this.devicecreated = devicecreated;
    }
    public void SetLastresponse(Calendar lastresponse) {
        this.lastresponse = lastresponse;
    }
    public Calendar GetLastresponse() {
        return lastresponse;
    }
    public void SetLastdeviceupdate(Calendar lastdeviceupdate) {
        this.lastdeviceupdate = lastdeviceupdate;
    }
    
    public static String GetUniqueColumns() {
        return "hostname=? COLLATE NOCASE";
    };
    
    public void SetDevicetype(String devicetype) {
        this.devicetype = devicetype;
    }
    public static void AddSubs(Homedevice myDevice, String type, int counter) {
        for (int i = 0; i < counter; i++) {
            switch (type) {
                case "Relais" -> myDevice.relaisList.add(new SubDvRelais());
                case "Meter" -> myDevice.meterList.add(new SubDvMeter());
                case "Input" -> myDevice.inputList.add(new SubDvInput());
                case "Rgbled" -> myDevice.rgbledList.add(new SubDvRgbled());
                case "Dimmer" -> myDevice.dimmerList.add(new SubDvDimmer());
            }
        }
    }
    

    @Override
    public Properties GetClientData() {
        Properties p = new Properties();
        p.put("deviceid", homedeviceid);
        p.put("needreconfig", needreconfig);
        p.put("devicetype", devicetype);
        p.put("lastresponse", lastresponse==null?0:lastresponse.getTimeInMillis());
        p.put("hostname", hostname);
        p.put("apiport", apiport);
        return p;
    }

    @Override
    public Properties GetAdminData() {
        Properties p = new Properties();
        p.put("deviceid", homedeviceid);
        p.put("needreconfig", needreconfig);
        p.put("devicetype", devicetype);
        p.put("lastresponse", lastresponse==null?0:lastresponse.getTimeInMillis());
        p.put("hostname", hostname);
        p.put("apiport", apiport);
        p.put("username", username);
        p.put("password", password);
        return p;
    }

    public void SetHostname (String hostname) {
        this.hostname=hostname;
    }
    public void SetApiport (int apiport) {
        this.apiport=apiport;
    }
    public void SetUsername (String username) {
        this.username=username;
    }
    public void SetPassword (String password) {
        this.password=password;
    }
    public String GetHostname () {
        return hostname;
    }
    public int GetApiport() {
        return apiport;
    }
    public String GetUsername () {
        return username;
    }
    public String GetPassword () {
        return password;
    }
    @Override
    public int GetId() {
        return homedeviceid;
    }

    @Override
    public void SetId(int id) {
        homedeviceid = id;
    }    
    
    @Override
    public boolean CheckIsSame(String[] uniquevals) {
        return uniquevals[0].toLowerCase().equals(hostname);
    }
    /*
    public CamConfig GetCamConfig();
    public int GetSwitchCount();
    public int GetRelaisCount(); // SingleColoredLights
    public int GetRGBLightCount();
    
    public Image GetImage();
    public String GetVideoURL();
    public boolean GetSwitch(int id);
    public boolean GetRelais(int id);
    public Color GetLightColor(int id);
    public byte GetLightBrightness(int id);
    
    public boolean SetCamHome();
    public boolean SetCamPos(float X, float Y);
    public boolean SetCamLeft();
    public boolean SetCamRight();
    public boolean SetCamUp();
    public boolean SetCamDown();
    public boolean SetCamZoomIn();
    public boolean SetCamZoomOut();
    public boolean SetCamFocusIn();
    public boolean SetCamFocusOut();
    public boolean SetCamFocus(float focusvalue);
    public boolean SetRelais(int id, boolean on);
    public boolean SetLight(int id, Color color, byte brightness);
*/
}
