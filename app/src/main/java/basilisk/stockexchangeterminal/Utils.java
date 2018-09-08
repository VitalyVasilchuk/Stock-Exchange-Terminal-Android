package basilisk.stockexchangeterminal;

public class Utils {
    public static String getFormattedValue(String s) {
        s.replace(",", ".");
        if (!s.contains(".")) s += ".";
        return (s + "0000000000").substring(0, 10);
    }
}
