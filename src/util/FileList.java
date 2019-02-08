package mobi.maptrek.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FileList {
    public static List<File> getFileListing(File startingDir, FilenameFilter filter) {
        List<File> result = getFileListingNoSort(startingDir, filter);
        Collections.sort(result);
        return result;
    }

    private static List<File> getFileListingNoSort(File startingDir, FilenameFilter filter) {
        List<File> result = new ArrayList();
        File[] files = startingDir.listFiles(filter);
        if (files != null) {
            result.addAll(Arrays.asList(files));
        }
        File[] dirs = startingDir.listFiles(new DirFileFilter());
        if (dirs != null) {
            for (File dir : dirs) {
                result.addAll(getFileListingNoSort(dir, filter));
            }
        }
        if (startingDir.isDirectory() && ((files == null || files.length == 0) && (dirs == null || dirs.length == 0))) {
            String[] items = startingDir.list();
            if (items == null || items.length == 0) {
                startingDir.delete();
            }
        }
        return result;
    }
}
