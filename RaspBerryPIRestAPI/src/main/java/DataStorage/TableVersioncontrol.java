/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DataStorage;

import java.util.AbstractList;

/**
 *
 * @author Rene
 */
public class TableVersioncontrol {
    public static TableVersioncontrol GetVersioncontrol(AbstractList<TableVersioncontrol> tableVersions, String needle) {
        for (TableVersioncontrol o : tableVersions) {
            if (o.tableName.toLowerCase().equals(needle.toLowerCase())) {
                return o;
            }
        }
        return null;
    }
    public static TableVersioncontrol SetOrAddVersioncontrol(AbstractList<TableVersioncontrol> tableVersions, String tableName, int tableVersion) {
        TableVersioncontrol myControl = null;
        for (TableVersioncontrol o : tableVersions) {
            if (o.tableName.toLowerCase().equals(tableName.toLowerCase())) {
                myControl = o;
            }
        }
        if (myControl == null) {
            myControl = new TableVersioncontrol(tableName, tableVersion);
            tableVersions.add(myControl);
        }
        else if (tableVersion > 0) {
            myControl.tableVersion = tableVersion;
        }
        return myControl;
    }
    
    public TableVersioncontrol(String tableName, int tableVersion) {
        this.tableName = tableName;
        this.tableVersion = tableVersion;
    }
    public String tableName;
    public int tableVersion;
}
