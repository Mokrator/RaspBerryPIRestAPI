package Homedv;

import RestObjects.HomedvSubs.Camera;
import DataStorage.TableObject;
import java.util.AbstractList;
import java.util.ArrayList;
import RestObjects.HomedvSubs.Relais;
import RestObjects.HomedvSubs.Inputs;
import RestObjects.HomedvSubs.Rgbled;
import RestObjects.HomedvSubs.Dimmer;

/**
 *
 * @author Rene
 */
public abstract class HomeDevice extends TableObject implements HomeDeviceInterface {
    public static final AbstractList<TableObject> known = new ArrayList<TableObject>();
    private AbstractList<Camera> cameraList;
    private AbstractList<Relais> relaisList;
    private AbstractList<Inputs> inputList;
    private AbstractList<Rgbled> rgbledList;
    private AbstractList<Dimmer> dimmerList;
    private int deviceid;
    private String hostname;
    private int apiport;
    private String username;
    private String password;
    public void SetData(String hostname, String username, String password, int apiport) {
        SetHostname(hostname);
        SetApiport(apiport);
        SetUsername(username);
        SetPassword(password);
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
