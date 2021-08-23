package DataStorage;

import Bitsnbytes.SqlParameter;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author Rene
 */
public interface TableObjectInterface {
    /**
     * Used to lookup Cache on only cached elements with higher ids than maxid from db
     * @return id of object
     */
    public int GetId();
    
    /**
     * only used at creation of totaly new Object (for new the id is given with -1)
     * automated looked up for maxid and created in Cache only. See AddKnown function in TableObject abstract class
     * @param id 
     */
    public void SetId(int id);
    /**
     * Checks for dublicates or indicates same element by unique keys from cache
     * should be case insensitive like database (unless Database is case sensitive)
     * @param uniquevals
     * @return 
     */
    public boolean CheckIsSame(String[] uniquevals);
    
    /**
     * Beware of filling Properties with null-values!
     * @return Properties that can be send to Client
     */
    public Properties GetClientData();
    
    /**
     * Beware of filling Properties with null-values!
     * @return Properties that can be send to Admin
     */
    public Properties GetAdminData();
    
    public String GetChangesSQL(ArrayList<SqlParameter> parameters);
}
