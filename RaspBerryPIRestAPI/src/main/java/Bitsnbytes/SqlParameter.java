package Bitsnbytes;

/**
 *
 * @author Rene
 */
public class SqlParameter {
    public static enum ParType {String, Date, Calendar, Integer, Long, Float, Double}
    public ParType myType;
    public Object myValue;
    public SqlParameter(ParType myType, Object myValue) {
        this.myType = myType;
        this.myValue = myValue;
    }
}
