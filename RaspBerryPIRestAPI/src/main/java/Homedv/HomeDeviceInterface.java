package Homedv;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Rene
 */
public interface HomeDeviceInterface {
    public void ReadConfig();
    public HttpRequest CreateRequest(String relUrl);
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
