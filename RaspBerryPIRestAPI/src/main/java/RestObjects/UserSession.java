package RestObjects;

import Server.RestApiServer;
import DataStorage.TableObject;
import Bitsnbytes.SqlParameter;
import java.text.DateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

/**
 *
 * @author Rene
 */
public class UserSession extends TableObject {
    public static final AbstractList<TableObject> known = new ArrayList<TableObject>();
    private User logintry;
    private User user;
    private final String sessionid;
    private final String remoteAddress;
    private Calendar sessionlastupdate;
    private Calendar prevupdate;
    private Calendar logout = null;
    private final Calendar sessioncreated;
    private String logoutreason = null;
    
    public UserSession(String remoteAddress, String sessionid, Calendar updatetime) {
        this.remoteAddress = remoteAddress;
        this.sessionid = sessionid;
        this.sessioncreated = updatetime;
        this.sessionlastupdate = updatetime;
        this.prevupdate = null;
    }
    
    public void SetUpdateTime(Calendar updatetime) {
        prevupdate = sessionlastupdate;
        sessionlastupdate = updatetime;
    }
    
    public User GetUser() {
        return user;
    }
    
    public boolean IsThisUserSession(String remoteAddress, String sessionid) {
        // no use of loggedout sessions
        // no sessionstealing from other ip
        boolean isit = logout == null && this.remoteAddress.equals(remoteAddress) && this.sessionid.equals(sessionid);
        return isit;
    }
    
    public void LogOut(String logoutreason) {
        logout = Calendar.getInstance();
        this.logoutreason = logoutreason;
    }
    
    public boolean CheckValid(Calendar forgetTime) {
        if (logout == null && !sessionlastupdate.after(forgetTime)) {
            LogOut("sessiontimeout");
        }
        return logout == null && sessionlastupdate.after(forgetTime);
    }
    
    public String GetSessionId() {
        return sessionid;
    }
    
    public Calendar GetLastUpdate() {
        return sessionlastupdate;
    }
    
    public Calendar GetPrevUpdate() {
        return prevupdate;
    }
    
    public void SetLoginEntry(User user) {
        logintry = user;
    }
    
    public void LoginSuccess() {
        user = logintry;
        logintry = null;
    }
    
    public User GetLoginEntry() {
        return logintry;
    }
    
    public User GetUserEntry() {
        return user;
    }
    
    public static UserSession CreateUserSession(String remoteAddress, String sessionid, Calendar updatetime) {
        UserSession newSession = new UserSession(remoteAddress, sessionid, updatetime);
        UserSession.known.add(newSession);
        return newSession;
    }
    
    public static UserSession GetExistingUserSession(String remoteAddress, String sessionid) {
        if (sessionid == null) {
            System.out.print("RestApiServer.Java:CheckSessionOk " + DateFormat.getTimeInstance(0, Locale.GERMAN).format(new Date()) + " (attackwarning) - No SessionID in command-POST \r\n");
            return null;
        }
        Iterator<TableObject> iter = UserSession.known.iterator();
        while (iter.hasNext()) {
            UserSession userSession = (UserSession)iter.next();
            Calendar forgetTime = Calendar.getInstance();
            forgetTime.add(Calendar.SECOND, -RestApiServer.SESSIONTIMEOUT);
            if (userSession.CheckValid(forgetTime) && userSession.IsThisUserSession(remoteAddress, sessionid))
            {
                return userSession;
            }
        }
        return null;
    }
        
    @Override
    public Properties GetClientData() {
        return new Properties();
    }

    @Override
    public Properties GetAdminData() {
        Properties p = new Properties();
        p.put("sessionid", sessionid);
        p.put("userid", user == null?0:user.userid);
        p.put("login", user == null?"":user.login);
        p.put("logintry", logintry == null?"":logintry.login);
        p.put("remoteAddress", remoteAddress);
        p.put("sessioncreated", sessioncreated==null?0:sessioncreated.getTimeInMillis());
        p.put("sessionlastupdate", sessionlastupdate==null?0:sessionlastupdate.getTimeInMillis());
        p.put("logout", logout==null?0:logout.getTimeInMillis());
        p.put("logoutreason", logoutreason==null?"":logoutreason);
        p.put("lastUsed", lastUsed==null?0:lastUsed);
        return p;
    }

// wird nicht in die Datenbank geschrieben...
    @Override
    public int GetId() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    public void SetId(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    public boolean CheckIsSame(String[] uniquevals) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    public String GetChangesSQL(ArrayList<SqlParameter> parameters) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
