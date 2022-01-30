package DataStorage;

import Bitsnbytes.SqlParameter;
import Bitsnbytes.SqlInformation;
import Server.RestApiServer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.reflections.Reflections;
/**
 *
 * @author Rene
 */
public class DatabaseForRest {
    public static final int MAXPARAMETERS = 999;
    public final Connection sqlCon;
    
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
     * @throws SQLException 
     */
    public DatabaseForRest(String workPath) throws SQLException {
        sqlCon = DriverManager.getConnection("jdbc:sqlite:" + Path.of(workPath, "restapi.db").toString());
        sqlCon.setAutoCommit(true);
        // Tabellen prüfen und vorbereiten
        AbstractList<TableVersioncontrol> tableVersions = new ArrayList<>();
        Statement getexistingtables = sqlCon.createStatement();
        getexistingtables.execute("select name from sqlite_master where type='table' and name!='sqlite_sequence'");
        try (ResultSet res = getexistingtables.getResultSet()) {
            while (res.next()) {
                tableVersions.add(new TableVersioncontrol(res.getString(1), 1));
            }
        }
        getexistingtables.close();
        
        if (TableVersioncontrol.GetVersioncontrol(tableVersions, "tableversions") == null) {
            sqlCon.createStatement().execute("create table tableversions (tablename TEXT PRIMARY KEY COLLATE NOCASE, tableversion INT)");
        }
        else {
            try (Statement getknowntables = sqlCon.createStatement()) {
                getknowntables.execute("select * from tableversions");
                try (ResultSet res = getknowntables.getResultSet()) {
                    while (res.next()) {
                        TableVersioncontrol.SetOrAddVersioncontrol(tableVersions, res.getString("tablename"), res.getInt("tableversion"));
                    }
                }
            }
        }
        
        Reflections reflections = new Reflections("RestObjects");
        RestApiServer.dbTableObjectClasses = reflections.getSubTypesOf(TableObject.class);
        RestApiServer.allTableObjectClasses = reflections.getSubTypesOf(TableObject.class);
        
        RestApiServer.dbTableObjectClasses.forEach(o -> {
            boolean found = false;
            for (Field f : o.getDeclaredFields()) {
                if (f.getName().equals("known")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                RestApiServer.allTableObjectClasses.remove(o);
            }
        });
        
        RestApiServer.dbTableObjectClasses.forEach(o -> {
            
            if (o.getSuperclass().getSimpleName().equals("TableObject")) {
                TableVersioncontrol tableVersioncontrol = TableVersioncontrol.SetOrAddVersioncontrol(tableVersions, o.getSimpleName() + "s", 0);
                try {
                    Method method = o.getMethod("SQLPrepare", int.class, Connection.class);
                    method.invoke(null, tableVersioncontrol.tableVersion, sqlCon);
                } catch (NoSuchMethodException ex) {
                    RestApiServer.log.log(Level.SEVERE, "{0} has no method!", o.getCanonicalName());
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    if (!o.getSimpleName().equals("HomedvTasmota") && !o.getSimpleName().equals("HomedvShelly")) {
                        RestApiServer.log.log(Level.SEVERE, ex.getMessage());
                    }
                }
            }
        });
    }
    
    public void Stop() {
        try {
            if (!sqlCon.getAutoCommit())
            {
                sqlCon.commit();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseForRest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
  
    public void RunSaveCommands(SqlInformation sqlInformation) throws SQLException {
        PreparedStatement com = sqlCon.prepareStatement(sqlInformation.sql);
        int index = 0;
        for (SqlParameter p : sqlInformation.parameters) {
            index++;
            InsertParametersCom(com, p, index);
        }
        com.execute();
        sqlInformation.updatedObjects.forEach(o -> {
            o.UpdateInsertSuccess();
        });
    }
    
    public void InsertParametersCom(PreparedStatement com, SqlParameter parameter, int parameterIndex) throws SQLException {
        switch (parameter.myType) {
            case Date -> {
                if (parameter.myValue == null) {
                    com.setNull(parameterIndex, java.sql.Types.BIGINT);
                } else {
                    com.setLong(parameterIndex, ((Date)parameter.myValue).getTime());
                }
            }
            case Calendar -> {
                if (parameter.myValue == null) {
                    com.setNull(parameterIndex, java.sql.Types.BIGINT);
                } else {
                    com.setLong(parameterIndex, ((Calendar)parameter.myValue).getTimeInMillis());
                }
            }
            case String -> {
                if (parameter.myValue == null) {
                    com.setNull(parameterIndex, java.sql.Types.NVARCHAR);
                } else {
                    com.setString(parameterIndex, ((String)parameter.myValue));
                }
            }
            case Integer -> {
                if (parameter.myValue == null) {
                    com.setNull(parameterIndex, java.sql.Types.INTEGER);
                } else {
                    com.setInt(parameterIndex, ((int)parameter.myValue));
                }
            }
            case Long -> {
                if (parameter.myValue == null) {
                    com.setNull(parameterIndex, java.sql.Types.BIGINT);
                } else {
                    com.setLong(parameterIndex, ((long)parameter.myValue));
                }
            }
            case Float -> {
                if (parameter.myValue == null) {
                    com.setNull(parameterIndex, java.sql.Types.FLOAT);
                } else {
                    com.setFloat(parameterIndex, ((float)parameter.myValue));
                }
            }
            case Double -> {
                if (parameter.myValue == null) {
                    com.setNull(parameterIndex, java.sql.Types.DOUBLE);
                } else {
                    com.setDouble(parameterIndex, ((double)parameter.myValue));
                }
            }
        }
    }
}
