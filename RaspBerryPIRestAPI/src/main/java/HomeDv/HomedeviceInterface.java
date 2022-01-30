package HomeDv;

import java.net.http.HttpRequest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Rene
 */
public interface HomedeviceInterface {
    /***
     * Reads config from device (add the addequate number of subdevices, read real hostname for ap-devices [or create one if by ip added new device has no hostname!] )
     */
    public void ReadConfig(Connection sqlCon);
    
    /***
     * prepare a Request for this devicetype (with auth if needed)
     * @param relUrl
     * @return 
     */
    public HttpRequest CreateRequest(String relUrl);
    
    /**
     * Change device from AP to enter a prconfigured wifi... (hostname should be set to a name in order to find the device with its new ip later...)
     */
    public void UpdateNewDevice();

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
    
    public enum CamConfig {
        PhotoShot(1),
        HasVLCVideo(2),
        CanTurn(4),
        CanIncline(8),
        CanManualFocus(16),
        CanZoom(32);
        private final int _val;
        public int getValue() {
            return _val;
        }
        CamConfig(int val) {
            _val = val;
        }
        public static List<CamConfig> parseArticlePermissions(int val) {
            List<CamConfig> apList = new ArrayList<>();
            for (CamConfig ap : values()) {
                if ((val & ap.getValue()) != 0) {
                    apList.add(ap);
                }
            }
            return apList;
        }
    }
}
