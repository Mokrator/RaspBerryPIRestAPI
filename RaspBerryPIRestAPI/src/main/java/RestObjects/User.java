package RestObjects;

import Server.RestApiServer;
import DataStorage.TableObject;
import Bitsnbytes.SqlParameter;
import Bitsnbytes.Utils;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

/**
 *
 * @author Rene
 */
public class User extends TableObject {
    public static final AbstractList<TableObject> known = new ArrayList<TableObject>();
    public int userid;
    public Date usercreated;
    public Date lastuserupdate;
    public Date lastlogin;
    public Date lastmailsent;
    public String email;
    public String login;
    public String salt;
    public String storedp;
    /**
     *   1: Active
     *   2: Mail not sent
     *   4: Mail not confirmed
     *   8: needChangePW
     *  16: 
     *  32: Admin (Devices)
     *  64: Admin (Users)
     * 128: Admin (Service+SQLMaster)
     */
    public int userstatus;
    
    public User(int userid, Date usercreated, Date lastuserupdate, Date lastlogin, Date lastmailsent, String email, String login, String salt, String storedp, int userstatus, Connection sqlCon) throws SQLException {
        this.userid=userid;
        this.usercreated=usercreated;
        this.lastuserupdate=lastuserupdate;
        this.lastlogin=lastlogin;
        this.lastmailsent=lastmailsent;
        this.email=email;
        this.login=login;
        this.salt=salt;
        this.storedp=storedp;
        this.userstatus=userstatus;
        AddKnown(userid, sqlCon, "User", GetUniqueColumns(), new String[] { login }, known);
    }
    
    public static String GetUniqueColumns() {
        return "login=? COLLATE NOCASE";
    };

    @Override
    public boolean CheckIsSame(String[] uniquevals) {
        return uniquevals[0].toLowerCase().equals(login.toLowerCase());
    }
    
    /**
     * must be static to use it without an object, so its neither in the interface ir abstract in TableObject...
     * @param res a resultrow whose select need to fit to the order of columns
     * @return a object of my type
     * @throws SQLException 
     */
    public static TableObject CreateFromRes(ResultSet res) throws SQLException {
        int userid = res.getInt(1);
        String login = res.getString(2);
        String salt = res.getString(3);
        String storedp = res.getString(4);
        int userstatus = res.getInt(5);
        String email = res.getString(6);
        Date usercreated = Utils.SetDate(res.getLong(7));
        Date lastmailsent = Utils.SetDate(res.getLong(8));
        Date lastlogin = Utils.SetDate(res.getLong(9));
        Date lastuserupdate = Utils.SetDate(res.getLong(10));
        return new User(userid, usercreated, lastuserupdate, lastlogin, lastmailsent, email, login, salt, storedp, userstatus, null);
    }
    
    @Override
    public String GetChangesSQL(ArrayList<SqlParameter> parameters) {
        parameters.add(new SqlParameter(SqlParameter.ParType.Date, usercreated));
        parameters.add(new SqlParameter(SqlParameter.ParType.Date, lastuserupdate));
        parameters.add(new SqlParameter(SqlParameter.ParType.Date, lastlogin));
        parameters.add(new SqlParameter(SqlParameter.ParType.Date, lastmailsent));
        parameters.add(new SqlParameter(SqlParameter.ParType.String, email));
        parameters.add(new SqlParameter(SqlParameter.ParType.String, login));
        parameters.add(new SqlParameter(SqlParameter.ParType.String, salt));
        parameters.add(new SqlParameter(SqlParameter.ParType.String, storedp));
        parameters.add(new SqlParameter(SqlParameter.ParType.Integer, userstatus));
        parameters.add(new SqlParameter(SqlParameter.ParType.Integer, userid));
        if (this.GetNeedInsert()) {
            return "insert into users (usercreated, lastuserupdate, lastlogin, lastmailsent, email, login, salt, storedp, userstatus, userid) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        }
        if (this.GetNeedUpdate()) {
            return "update users set usercreated=?, lastuserupdate=?, lastlogin=?, lastmailsent=?, email=?, login=?, salt=?, storedp=?, userstatus=? where userid=?;";
        }
        else {
            RestApiServer.log.severe("ERROR no update or insert needed");
            return "ERROR";
        }
    }

    @Override
    public Properties GetClientData() {
        Properties p = new Properties();
        p.put("userid", userid);
        p.put("login", login);
        p.put("usercreated", usercreated.getTime());
        p.put("lastuserupdate", lastuserupdate!=null?0:lastuserupdate.getTime());
        p.put("lastlogin", lastlogin==null?0:lastlogin.getTime());
        p.put("lastmailsent", lastmailsent==null?0:lastmailsent.getTime());
        p.put("emailpartial", email.substring(0, Math.min(email.indexOf("@")*2/3, 5)) + "..." + email.substring(email.indexOf("@")));
        p.put("userstatus", userstatus);
        return p;
    }

    @Override
    public Properties GetAdminData() {
        Properties p = new Properties();
        p.put("userid", userid);
        p.put("login", login);
        p.put("usercreated", usercreated);
        p.put("lastuserupdate", lastuserupdate==null?0:lastuserupdate.getTime());
        p.put("lastlogin", lastlogin==null?0:lastlogin.getTime());
        p.put("lastmailsent", lastmailsent==null?0:lastmailsent.getTime());
        p.put("email", email);
        p.put("userstatus", userstatus);
        p.put("lastUsed", lastUsed==null?0:lastUsed);
        p.put("needInsert", GetNeedInsert());
        p.put("needUpdate", GetNeedUpdate());
        return p;
    }
    
    @Override
    public int GetId() {
        return userid;
    }
    
    @Override
    public void SetId(int userid) {
        this.userid = userid;
    }
}
