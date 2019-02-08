package mobi.maptrek.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    public static String unusable = "*+~|<>!?\\/:";

    public static String sanitizeFilename(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (unusable.indexOf(name.charAt(i)) > -1) {
                sb.append("_");
            } else {
                sb.append(name.charAt(i));
            }
        }
        return sb.toString();
    }

    public static void copyFile(File from, File to) throws IOException {
        copyStreamToFile(new FileInputStream(from), to);
    }

    public static void copyStreamToFile(InputStream in, File to) throws IOException {
        OutputStream out = new FileOutputStream(to);
        byte[] buffer = new byte[1024];
        while (true) {
            int read = in.read(buffer);
            if (read != -1) {
                out.write(buffer, 0, read);
            } else {
                in.close();
                out.flush();
                out.close();
                return;
            }
        }
    }
}
