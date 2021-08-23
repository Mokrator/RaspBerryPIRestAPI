package RestObjects;

import Server.RestApiServer;
import DataStorage.TableObject;
import Bitsnbytes.Utils;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import org.json.JSONObject;

/**
 *
 * @author Rene
 */
public class CommandHandler {
    public static void HandleCom(String path, UserSession userSession, JSONObject postData, Properties returnValue, Calendar blockUntil, String remoteAddress, MessageDigest digest) throws SQLException {
        switch (path) {
            /* Ping - might be used quite often */
            case "ping" -> returnValue.put("status", "ok");
            /* regular Client Commands*/
            
            /* regular Admin Commands*/
            case "addknowndevice" -> {
                if (userSession.GetUserEntry() == null || (userSession.GetUserEntry().userstatus & 32) < 32) {
                    returnValue.put("status", "missingpermissions");
                    blockUntil.add(Calendar.MINUTE, 15);
                    RestApiServer.BlockIP(remoteAddress, blockUntil);
                }
                else {
                    HomedvShelly.AddKnownDevice(postData.getString("host"), postData.getString("login"), postData.getString("pwd"));
                    HomedvTasmota.AddKnownDevice(postData.getString("host"), postData.getString("login"), postData.getString("pwd"));
                }
            }
            /* regular Admin Commands*/
            case "checknewdevice" -> {
                if (userSession.GetUserEntry() == null || (userSession.GetUserEntry().userstatus & 32) < 32) {
                    returnValue.put("status", "missingpermissions");
                    blockUntil.add(Calendar.MINUTE, 15);
                    RestApiServer.BlockIP(remoteAddress, blockUntil);
                }
                else {
                    HomedvShelly.SearchDevice();
                    HomedvTasmota.SearchDevice();
                }
            }
            case "getlist" -> {
                if (userSession.GetUserEntry() == null || (userSession.GetUserEntry().userstatus & 64) < 64) {
                    returnValue.put("status", "missingpermissions");
                    blockUntil.add(Calendar.MINUTE, 15);
                    RestApiServer.BlockIP(remoteAddress, blockUntil);
                }
                else {
                    boolean querydb = postData.getString("query").equals("querydb");
                    returnValue.put("type", postData.getString("type"));
                    switch (postData.getString("type")) {
                        case "User" -> {
                            if (querydb) {
                                User.ReadAllObjects(RestApiServer.GetConnection(), postData.getString("type"), User.known);
                            }
                            returnValue.put(postData.getString("type"), User.GetAdminObjects(User.known));
                            returnValue.put("status", "ok");
                        }
                        case "UserSession" -> {
                            // querydb ist bei sessions nicht möglich (keine DB!)
                            returnValue.put(postData.getString("type"), UserSession.GetAdminObjects(UserSession.known));
                            returnValue.put("status", "ok");
                        }
                        default -> {
                            returnValue.put("status", "wrongparameter");
                            blockUntil.add(Calendar.MINUTE, 15);
                            RestApiServer.BlockIP(remoteAddress, blockUntil);
                        }
                    }
                }
            }
            
            
            /* Login Logout Register, Shutdown the server (shouldnt be used too often) */
            case "getsalt" -> {
                if (!Utils.CheckLogin(postData.getString("login"))) {
                    blockUntil.add(Calendar.MINUTE, 15);
                    RestApiServer.BlockIP(remoteAddress, blockUntil);
                    returnValue.put("status", "invalidlogin");
                }
                else {
                    String getsaltTempLogin = postData.getString("login").toLowerCase();
                    TableObject GG = User.GetUniqueObject(RestApiServer.GetConnection(), "User", User.GetUniqueColumns(), new String[] { postData.getString("login") }, User.known);
                    if (GG == null) {
                        userSession.SetLoginEntry(null);
                    } else {
                        userSession.SetLoginEntry((User)GG);
                    }
                    if (userSession.GetLoginEntry() == null)
                    {
                        returnValue.put("salt", Utils.Sha256(getsaltTempLogin + "#.#.#" + RestApiServer.GetWorkPath(), digest));
                        returnValue.put("status", "ok");
                    }
                    else
                    {
                        returnValue.put("salt", userSession.GetLoginEntry().salt);
                        returnValue.put("status", "ok");
                    }
                }
            }
            case "login" -> {
                if (userSession.GetLoginEntry() != null && postData.has("pass") && postData.getString("pass").length() == 64 && Utils.Sha256(userSession.GetSessionId() + userSession.GetLoginEntry().storedp, digest).equals(postData.getString("pass"))) {
                    userSession.LoginSuccess();
                    User u = userSession.GetUserEntry();
                    u.lastlogin = new Date();
                    returnValue.put("hello", JSONObject.valueToString(u.GetClientData()));
                    returnValue.put("status", "ok");
                    userSession.GetUser().lastUsed = userSession.lastUsed;
                    u.SetNeedUpdate();
                }
                else {
                    blockUntil.add(Calendar.SECOND, 15);
                    RestApiServer.BlockIP(remoteAddress, blockUntil);
                    returnValue.put("status", "wronguserorpassword");
                }
            }
            case "logout" -> {
                userSession.LogOut("userrequest");
                returnValue.put("status", "ok");
            }
            case "register" -> {
                if (!Utils.CheckLogin(postData.getString("login"))) {
                    blockUntil.add(Calendar.MINUTE, 15);
                    RestApiServer.BlockIP(remoteAddress, blockUntil);
                    returnValue.put("status", "invalidlogin");
                }
                else if (!Utils.CheckEmail(postData.getString("email"))) {
                    blockUntil.add(Calendar.MINUTE, 15);
                    RestApiServer.BlockIP(remoteAddress, blockUntil);
                    returnValue.put("status", "invalidemail");
                }
                else {
                    String registerTempSalt = Utils.Sha256(String.valueOf(Math.random()), digest);
                    User newUser;
                    newUser = new User(-1, new Date(), new Date(), null, null, postData.getString("email"), postData.getString("login"), registerTempSalt, Utils.Sha256(registerTempSalt + postData.getString("storedp"), digest), 3, RestApiServer.GetConnection());
                    if (newUser.GetNonDublicate()) {
                        returnValue.put("status", "ok");
                    }
                    else {
                        returnValue.put("status", "dublicate");
                    }
                }
            }
            case "stopservice" -> {
                if (userSession.GetUserEntry() == null || (userSession.GetUserEntry().userstatus & 128) < 128) {
                    returnValue.put("status", "missingpermissions");
                    blockUntil.add(Calendar.MINUTE, 15);
                    RestApiServer.BlockIP(remoteAddress, blockUntil);
                }
                else {
                    RestApiServer.StopService();
                    returnValue.put("status", "servicestopping");
                }
            }
            default -> {// all seem to be ok until i could not find the command to run...
                returnValue.put("status", "comunknown"); // comand unknown! (no command run)
            }
        }
    }
}
