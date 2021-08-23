package DataStorage;

import Bitsnbytes.SqlParameter;
import Bitsnbytes.SqlInformation;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private void PrepareRestApiDB() throws SQLException {
        sqlCon.setAutoCommit(true);
        // Tabellen prüfen un dvorbereiten
        Statement com = sqlCon.createStatement();
        com.execute("select name from sqlite_master where type='table' and name!='sqlite_sequence'");
        boolean usersExists = false;
        try (ResultSet res = com.getResultSet()) {
            while (res.next()) {
                String tableName = res.getString(1);
                if (tableName.equals("users")) {
                    usersExists = true;
                }
            }
        }
        if (!usersExists) {
            com.execute("create table users (userid INTEGER PRIMARY KEY, login TEXT UNIQUE, salt TEXT, storedp TEXT, userstatus INTEGER, email TEXT, usercreated INTEGER, lastmailsent INTEGER, lastlogin INTEGER, lastuserupdate INTEGER)");
        }
    }
   
    public DatabaseForRest(String workPath) throws SQLException {
        sqlCon = DriverManager.getConnection("jdbc:sqlite:" + Path.of(workPath, "restapi.db").toString());
        PrepareRestApiDB();
    }
    
    public void stop() {
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
