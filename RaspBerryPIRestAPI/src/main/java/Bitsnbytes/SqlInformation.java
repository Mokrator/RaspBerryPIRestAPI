package Bitsnbytes;

import DataStorage.TableObject;
import java.util.ArrayList;

/**
 *
 * @author Rene
 */
public class SqlInformation {
    public String sql = "";
    public ArrayList<SqlParameter> parameters = new ArrayList<SqlParameter>();
    public ArrayList<TableObject> updatedObjects = new ArrayList<TableObject>();
}
