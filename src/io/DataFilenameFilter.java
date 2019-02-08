package mobi.maptrek.io;

import java.io.File;
import java.io.FilenameFilter;

public class DataFilenameFilter implements FilenameFilter {
    public boolean accept(File dir, String filename) {
        String lc = filename.toLowerCase();
        return lc.endsWith(TrackManager.EXTENSION) || lc.endsWith(GPXManager.EXTENSION) || lc.endsWith(KMLManager.EXTENSION);
    }
}
