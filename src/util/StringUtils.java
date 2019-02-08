package mobi.maptrek.util;

import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import java.util.StringTokenizer;

public class StringUtils {
    public static String capitalizeFirst(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        return !Character.isUpperCase(first) ? Character.toUpperCase(first) + s.substring(1) : s;
    }

    public static String capitalize(String line) {
        StringTokenizer token = new StringTokenizer(line);
        String CapLine = "";
        while (token.hasMoreTokens()) {
            String tok = token.nextToken();
            CapLine = CapLine + Character.toUpperCase(tok.charAt(0)) + tok.substring(1) + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR;
        }
        return CapLine.substring(0, CapLine.length() - 1);
    }
}
