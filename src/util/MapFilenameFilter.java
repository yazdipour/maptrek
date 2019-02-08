package mobi.maptrek.util;

import java.io.File;
import java.io.FilenameFilter;

public class MapFilenameFilter implements FilenameFilter {
    public boolean accept(File dir, String filename) {
        String lc = filename.toLowerCase();
        return lc.endsWith(".sqlitedb") || lc.endsWith(".mbtiles");
    }
}
