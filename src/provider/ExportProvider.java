package mobi.maptrek.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.FileDescriptorDetachedException;
import android.os.ParcelFileDescriptor.OnCloseListener;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import mobi.maptrek.data.source.WaypointDbDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportProvider extends ContentProvider {
    private static final String AUTHORITY = "mobi.maptrek.files";
    private static final String[] COLUMNS = new String[]{"_display_name", "_size", COLUMN_LAST_MODIFIED};
    private static final String COLUMN_LAST_MODIFIED = "_last_modified";
    private static final Logger logger = LoggerFactory.getLogger(ExportProvider.class);
    private static PathStrategy mCachedStrategy;
    private Handler mHandler;
    private PathStrategy mStrategy;

    interface PathStrategy {
        File getFileForUri(Uri uri);

        Uri getUriForFile(File file);
    }

    private static class SimplePathStrategy implements PathStrategy {
        private final HashMap<String, File> mRoots = new HashMap();

        SimplePathStrategy() {
        }

        void addRoot(String name, File root) {
            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("Name must not be empty");
            }
            try {
                this.mRoots.put(name, root.getCanonicalFile());
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + root, e);
            }
        }

        public Uri getUriForFile(File file) {
            try {
                String rootPath;
                String path = file.getCanonicalPath();
                Entry<String, File> mostSpecific = null;
                for (Entry<String, File> root : this.mRoots.entrySet()) {
                    rootPath = ((File) root.getValue()).getPath();
                    if (path.startsWith(rootPath) && (mostSpecific == null || rootPath.length() > ((File) mostSpecific.getValue()).getPath().length())) {
                        mostSpecific = root;
                    }
                }
                if (mostSpecific == null) {
                    throw new IllegalArgumentException("Failed to find configured root that contains " + path);
                }
                rootPath = ((File) mostSpecific.getValue()).getPath();
                if (rootPath.endsWith("/")) {
                    path = path.substring(rootPath.length());
                } else {
                    path = path.substring(rootPath.length() + 1);
                }
                return new Builder().scheme("content").authority("mobi.maptrek.files").encodedPath(Uri.encode((String) mostSpecific.getKey()) + '/' + Uri.encode(path, "/")).build();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
            }
        }

        public File getFileForUri(Uri uri) {
            String path = uri.getEncodedPath();
            int splitIndex = path.indexOf(47, 1);
            String tag = Uri.decode(path.substring(1, splitIndex));
            path = Uri.decode(path.substring(splitIndex + 1));
            File root = (File) this.mRoots.get(tag);
            if (root == null) {
                throw new IllegalArgumentException("Unable to find configured root for " + uri);
            }
            File file = new File(root, path);
            try {
                file = file.getCanonicalFile();
                if (file.getPath().startsWith(root.getPath())) {
                    return file;
                }
                throw new SecurityException("Resolved path jumped beyond configured root");
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
            }
        }
    }

    public boolean onCreate() {
        this.mHandler = new Handler(Looper.getMainLooper());
        return true;
    }

    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        if (info.grantUriPermissions) {
            this.mStrategy = getPathStrategy(context);
            return;
        }
        throw new SecurityException("Provider must grant uri permissions");
    }

    public static Uri getUriForFile(Context context, File file) {
        return getPathStrategy(context).getUriForFile(file);
    }

    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File file = this.mStrategy.getFileForUri(uri);
        if (projection == null) {
            projection = COLUMNS;
        }
        String[] cols = new String[projection.length];
        Object[] values = new Object[projection.length];
        int length = projection.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            int i3;
            String col = projection[i];
            if ("_display_name".equals(col)) {
                cols[i2] = "_display_name";
                i3 = i2 + 1;
                values[i2] = file.getName();
            } else if ("_size".equals(col)) {
                cols[i2] = "_size";
                i3 = i2 + 1;
                values[i2] = Long.valueOf(file.length());
            } else if (COLUMN_LAST_MODIFIED.equals(col)) {
                cols[i2] = COLUMN_LAST_MODIFIED;
                i3 = i2 + 1;
                values[i2] = Long.valueOf(file.lastModified());
            } else {
                i3 = i2;
            }
            i++;
            i2 = i3;
        }
        cols = copyOf(cols, i2);
        values = copyOf(values, i2);
        MatrixCursor cursor = new MatrixCursor(cols, 1);
        cursor.addRow(values);
        return cursor;
    }

    public String getType(@NonNull Uri uri) {
        File file = this.mStrategy.getFileForUri(uri);
        int lastDot = file.getName().lastIndexOf(46);
        if (lastDot >= 0) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(lastDot + 1));
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("No external inserts");
    }

    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("No external updates");
    }

    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return this.mStrategy.getFileForUri(uri).delete() ? 1 : 0;
    }

    public ParcelFileDescriptor openFile(@NonNull final Uri uri, @NonNull final String mode) throws FileNotFoundException {
        File file = this.mStrategy.getFileForUri(uri);
        int fileMode = modeToMode(mode);
        if ("rwt".equals(mode) && uri.getLastPathSegment().endsWith(".sqlitedb")) {
            file = new File(file.getAbsolutePath() + ".restore");
        }
        logger.error("openFile: {} {} {}", uri, file, mode);
        try {
            return ParcelFileDescriptor.open(file, fileMode, this.mHandler, new OnCloseListener() {
                public void onClose(IOException e) {
                    if (e == null || !(e instanceof FileDescriptorDetachedException)) {
                        if ("rwt".equals(mode)) {
                            ExportProvider.logger.error("saved");
                            ExportProvider.this.getContext().sendOrderedBroadcast(new Intent(WaypointDbDataSource.BROADCAST_WAYPOINTS_RESTORED), null);
                        }
                        if ("export".equals(uri.getPathSegments().get(0))) {
                            ExportProvider.this.mStrategy.getFileForUri(uri).delete();
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            return ParcelFileDescriptor.open(file, fileMode);
        }
    }

    private static PathStrategy getPathStrategy(Context context) {
        synchronized (ExportProvider.class) {
            if (mCachedStrategy == null) {
                SimplePathStrategy simplePathStrategy = new SimplePathStrategy();
                simplePathStrategy.addRoot("data", buildPath(context.getExternalFilesDir("data"), new String[0]));
                simplePathStrategy.addRoot("databases", buildPath(context.getExternalFilesDir("databases"), new String[0]));
                simplePathStrategy.addRoot("export", buildPath(context.getExternalCacheDir(), "export"));
                mCachedStrategy = simplePathStrategy;
            }
        }
        return mCachedStrategy;
    }

    private static int modeToMode(String mode) {
        if ("r".equals(mode)) {
            return 268435456;
        }
        if ("w".equals(mode) || "wt".equals(mode)) {
            return 738197504;
        }
        if ("wa".equals(mode)) {
            return 704643072;
        }
        if ("rw".equals(mode)) {
            return 939524096;
        }
        if ("rwt".equals(mode)) {
            return 1006632960;
        }
        throw new IllegalArgumentException("Invalid mode: " + mode);
    }

    private static File buildPath(File base, String... segments) {
        File cur = base;
        int length = segments.length;
        int i = 0;
        File cur2 = cur;
        while (i < length) {
            String segment = segments[i];
            if (segment != null) {
                cur = new File(cur2, segment);
            } else {
                cur = cur2;
            }
            i++;
            cur2 = cur;
        }
        return cur2;
    }

    private static String[] copyOf(String[] original, int newLength) {
        String[] result = new String[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    private static Object[] copyOf(Object[] original, int newLength) {
        Object[] result = new Object[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }
}
