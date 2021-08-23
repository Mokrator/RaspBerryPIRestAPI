package Bitsnbytes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;

/**
 * various default functions
 * @author Rene
 */
public class Utils {
    public static String BytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    public static String BasicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
    
    public static boolean CheckLogin(String login) {
        return login.matches("^[\\w\\.\\-]{3,16}$") && !login.matches("[\\-_\\.]{2}");
    }
    
    public static boolean CheckEmail(String email) {
        return email.matches("^[\\w\\.\\-]+@[\\w\\.\\-]+\\.[a-z]{2,10}$");
    }
    
    public static String Sha256(String input, MessageDigest digest) {
        return BytesToHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    }
    
    public static Date SetDate(long in) {
        Date out = new Date();
        if (in > 0) {
            out.setTime(in);
        } else {
            out = null;
        }
        return out;
    }
}
