package Homedv;

import RestObjects.HomedvSubs.Camera;
import DataStorage.TableObject;
import java.util.AbstractList;
import java.util.ArrayList;
import RestObjects.HomedvSubs.Relais;
import RestObjects.HomedvSubs.Meter;
import RestObjects.HomedvSubs.Input;
import RestObjects.HomedvSubs.Rgbled;
import RestObjects.HomedvSubs.Dimmer;
import java.util.Date;

/**
 *
 * @author Rene
 */
public abstract class Homedevice extends TableObject implements HomedeviceInterface {
    public static final AbstractList<TableObject> known = new ArrayList<TableObject>();
    private AbstractList<Camera> cameraList;
    public final AbstractList<Relais> relaisList = new ArrayList<Relais>();
    public final AbstractList<Meter> meterList = new ArrayList<Meter>();
    public final AbstractList<Input> inputList = new ArrayList<Input>();
    public final AbstractList<Rgbled> rgbledList = new ArrayList<Rgbled>();
    public final AbstractList<Dimmer> dimmerList = new ArrayList<Dimmer>();
    private int deviceid;
    private String devicetype;
    private String hostname;
    private int apiport;
    private String username;
    private String password;
    private Date devicecreated;
    private Date lastresponse;
    private Date lastdeviceupdate;
    public void SetData(String devicetype,String hostname, String username, String password, int apiport, boolean readConfig, Date devicecreated, Date lastresponse, Date lastdeviceupdate) {
        SetDevicetype(devicetype);
        SetHostname(hostname);
        SetApiport(apiport);
        SetUsername(username);
        SetPassword(password);
        SetDevicecreated(devicecreated);
        SetLastresponse(lastresponse);
        SetLastdeviceupdate(lastdeviceupdate);
        
        if (readConfig) {
            ReadConfig();
        }
        UpdateNewDevice();
    }
    public void SetDevicecreated(Date devicecreated) {
        this.devicecreated = devicecreated;
    }
    public void SetLastresponse(Date lastresponse) {
        this.lastresponse = lastresponse;
    }
    public Date GetLastresponse() {
        return lastresponse;
    }
    public void SetLastdeviceupdate(Date lastdeviceupdate) {
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
                case "Relais" -> myDevice.relaisList.add(new Relais());
                case "Meter" -> myDevice.meterList.add(new Meter());
                case "Input" -> myDevice.inputList.add(new Input());
                case "Rgbled" -> myDevice.rgbledList.add(new Rgbled());
                case "Dimmer" -> myDevice.dimmerList.add(new Dimmer());
            }
        }
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
        return deviceid;
    }

    @Override
    public void SetId(int id) {
        deviceid = id;
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