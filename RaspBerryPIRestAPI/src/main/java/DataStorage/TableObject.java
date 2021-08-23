package DataStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractList;
import java.util.Date;
import org.json.JSONArray;
import RestObjects.User;

/**
 *
 * @author Rene
 */
public abstract class TableObject implements TableObjectInterface {
    private boolean needInsert = false;
    private boolean needUpdate = false;
    private boolean nonDublicate = true;
    public Date lastUsed = new Date();
    // "known" may not be here - will mix all objects together of all extended classes...
    
    /**
     * On Objectchanges edit following methods:
     * TableObject.CreateFromRes (only for NEW tables)
     * RestApiServer.CleanupCache (only for NEW tables)
     * RestApiServer.SaveDatabase (only for NEW tables)
     * XTableX.CreateFromRes (any changes to datastructure)
     * XTableX.GetChangesSQL (any changes to datastructure)
     * XTableX.GetUniqueColumns (if the unique-keys are changed)
     * XTableX.CheckIsSame (if the unique-keys are changed)
     * XTableX.GetClientData (for new Outputs)
     * XTableX.GetAdminData (for new Outputs)
     * DatabaseForRest.PrepareRestApiDB (remember to change the existing tables or build in versioncheck if you change columns)
     * 
     * @param objectType as the clasname is written...
     * @param res
     * @return 
     */
    private static TableObject CreateFromRes(String objectType, ResultSet res) throws SQLException {
        switch (objectType) {
            case "User" -> {
                return User.CreateFromRes(res);
            }
        }
        return null;
    }

    public boolean GetNonDublicate() {
        return nonDublicate;
    }
    public boolean GetNeedInsert() {
        return needInsert;
    }
    public boolean GetNeedUpdate() {
        return needUpdate;
    }
    public void SetNeedUpdate() {
        if (!needInsert) needUpdate = true;
    }
    public void UpdateInsertSuccess() {
        needInsert = false;
        needUpdate = false;
    }
    
    private static TableObject CreateFromCom(String objectType, PreparedStatement com) throws SQLException {
        com.execute();
        try (ResultSet res = com.getResultSet()) {
            if (res.next()) {
                return CreateFromRes(objectType, res);
            }
        }
        return null;
    }

    
    public static void ReadAllObjects(Connection sqlCon, String objectType, AbstractList<TableObject> known) throws SQLException {
        PreparedStatement com = sqlCon.prepareStatement("select * from " + objectType.toLowerCase() + "s;", java.sql. ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        CreateAllFromCom(objectType, com, known);
    }

    private static void CreateAllFromCom(String objectType, PreparedStatement com, AbstractList<TableObject> known) throws SQLException {
        com.execute();
        try (ResultSet res = com.getResultSet()) {
            while (res.next()) {
                if (!IsCached(res.getInt(1), known)) {
                    CreateFromRes(objectType, res);
                }
            }
        }
    }

    private static boolean IsCached(int id, AbstractList<TableObject> known) {
        return known.stream().anyMatch(o -> (o.GetId() == id));        
    }
    
    public static TableObject GetObjectById(Connection sqlCon, String objectType, int id, AbstractList<TableObject> known) throws SQLException {
        TableObject returnValue = null;
        for (TableObject o : known) {
            if (o.GetId() == id) {
                o.lastUsed = new Date();
                returnValue = o;
            }
        }
        if (returnValue == null) {
            PreparedStatement com = sqlCon.prepareStatement("select * from " + objectType.toLowerCase() + "s where " + objectType.toLowerCase() + "id=?", java.sql. ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            com.setInt(1, id);
            returnValue = CreateFromCom(objectType, com);
        }
        return returnValue;
    }
    
    public static TableObject GetUniqueObject(Connection sqlCon, String objectType, String sqlunique, String[] uniquevals, AbstractList<TableObject> known) throws SQLException {
        TableObject returnValue = null;
        for (TableObject o : known) {
            if (o.CheckIsSame(uniquevals)) { // <-- abbruch in dieser Zeile! ist 0 null oder was bescheuertes? uniquevalls ist ok.
                o.lastUsed = new Date();
                returnValue = o;
            }
        }
        if (returnValue == null) {
            PreparedStatement com = sqlCon.prepareStatement("select * from " + objectType.toLowerCase() + "s where " + sqlunique, java.sql. ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            for (int i = 0; i < uniquevals.length; i++) {
                com.setString(i + 1, uniquevals[i]);
            }
            returnValue = CreateFromCom(objectType, com);
        }
        return returnValue;
    }
    
    public static void CleanupCache(Date deleteBefore, AbstractList<TableObject> known) {
        known.stream().filter(o -> {
            return o!=null && !o.needInsert && !o.needUpdate && deleteBefore.after(o.lastUsed);
        }).forEachOrdered(o -> {
            known.remove(o);
        });
    }
    
    public static JSONArray GetAdminObjects(AbstractList<TableObject> known) {
        JSONArray returnValue = new JSONArray();
        known.forEach(o -> {
            returnValue.put(o.GetAdminData());
        });
        return returnValue;
    }
    
    public void AddKnown(int id, Connection sqlCon, String objectType, String sqlunique, String[] uniquevals, AbstractList<TableObject> known) throws SQLException {
        int nextid = 0;
        if (id == -1) {
            PreparedStatement com = sqlCon.prepareStatement("select max(" + objectType.toLowerCase() + "id), max(case when " + sqlunique + " then 1 else 0 end) from " + objectType.toLowerCase() + "s", java.sql. ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            for (int i = 0; i < uniquevals.length; i++) {
                com.setString(i + 1, uniquevals[i]);
            }
            com.execute();
            try (ResultSet res = com.getResultSet()) {
                if (res.next()) {
                    nextid = res.getInt(1)+1;
                    nonDublicate = res.getInt(2) == 0;
                }
            }
            for (TableObject o : known) {
                if (nextid < o.GetId()) {
                    nextid = o.GetId() + 1;
                }
                if (o.CheckIsSame(uniquevals)) {
                    nonDublicate = false;
                }
            }
        }
        if (nonDublicate) {
            if (id == -1)
            {
                SetId(nextid);
                needInsert = true;
            }
            known.add(this); // iinserted if new or read from db (new in cache)
        }
    }
}
