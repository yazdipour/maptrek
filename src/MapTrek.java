package mobi.maptrek;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.LongSparseArray;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Iterator;
import mobi.maptrek.data.MapObject;
import mobi.maptrek.data.MapObject.AddedEvent;
import mobi.maptrek.data.MapObject.RemovedEvent;
import mobi.maptrek.maps.maptrek.HillshadeDatabaseHelper;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper;
import mobi.maptrek.util.LongSparseArrayIterator;
import mobi.maptrek.util.StringFormatter;
import org.greenrobot.eventbus.EventBus;
import org.oscim.backend.CanvasAdapter;
import org.oscim.map.Viewport;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;
import org.oscim.utils.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapTrek extends Application {
    public static final String EXCEPTION_PATH = "exception.txt";
    public static float density = Viewport.VIEW_NEAR;
    public static boolean isMainActivityRunning = false;
    private static final Logger logger = LoggerFactory.getLogger(MapTrek.class);
    private static MapTrek mSelf;
    private static final LongSparseArray<MapObject> mapObjects = new LongSparseArray();
    public static float ydpi = CanvasAdapter.DEFAULT_DPI;
    private SQLiteDatabase mDetailedMapDatabase;
    private MapTrekDatabaseHelper mDetailedMapHelper;
    private DefaultExceptionHandler mExceptionHandler;
    private File mExceptionLog;
    private SQLiteDatabase mHillshadeDatabase;
    private HillshadeDatabaseHelper mHillshadeHelper;
    private Index mIndex;
    private String mUserNotification;

    private class DefaultExceptionHandler implements UncaughtExceptionHandler {
        private UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        DefaultExceptionHandler() {
        }

        void caughtException(Thread thread, Throwable ex) {
            try {
                StringBuilder msg = new StringBuilder();
                msg.append(DateFormat.format("dd.MM.yyyy hh:mm:ss", System.currentTimeMillis()));
                try {
                    PackageInfo info = MapTrek.this.getPackageManager().getPackageInfo(MapTrek.this.getPackageName(), 0);
                    if (info != null) {
                        msg.append("\nVersion : ").append(info.versionCode).append(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR).append(info.versionName);
                    }
                } catch (Throwable th) {
                }
                msg.append("\n").append("Thread : ").append(thread.toString()).append("\nException :\n\n");
                if (MapTrek.this.mExceptionLog.getParentFile().canWrite()) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(MapTrek.this.mExceptionLog, false));
                    writer.write(msg.toString());
                    ex.printStackTrace(new PrintWriter(writer));
                    writer.write("\n\n");
                    writer.close();
                }
            } catch (Throwable e) {
                MapTrek.logger.error("Exception while handle other exception", e);
            }
        }

        public void uncaughtException(Thread thread, Throwable ex) {
            caughtException(thread, ex);
            this.defaultHandler.uncaughtException(thread, ex);
        }
    }

    static {
        Parameters.CUSTOM_TILE_SIZE = true;
        Parameters.MAP_EVENT_LAYER2 = true;
        Parameters.POT_TEXTURES = true;
    }

    public void onCreate() {
        super.onCreate();
        mSelf = this;
        File exportDir = new File(getExternalCacheDir(), "export");
        if (!exportDir.exists()) {
            exportDir.mkdir();
        }
        this.mExceptionLog = new File(exportDir, EXCEPTION_PATH);
        this.mExceptionHandler = new DefaultExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this.mExceptionHandler);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        Configuration.initialize(PreferenceManager.getDefaultSharedPreferences(this));
        initializeSettings();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        density = metrics.density;
        ydpi = metrics.ydpi;
        mapObjects.clear();
    }

    private void initializeSettings() {
        Resources resources = getResources();
        int unit = Configuration.getSpeedUnit();
        StringFormatter.speedFactor = Float.parseFloat(resources.getStringArray(R.array.speed_factors)[unit]);
        StringFormatter.speedAbbr = resources.getStringArray(R.array.speed_abbreviations)[unit];
        unit = Configuration.getDistanceUnit();
        StringFormatter.distanceFactor = Double.parseDouble(resources.getStringArray(R.array.distance_factors)[unit]);
        StringFormatter.distanceAbbr = resources.getStringArray(R.array.distance_abbreviations)[unit];
        StringFormatter.distanceShortFactor = Double.parseDouble(resources.getStringArray(R.array.distance_factors_short)[unit]);
        StringFormatter.distanceShortAbbr = resources.getStringArray(R.array.distance_abbreviations_short)[unit];
        unit = Configuration.getElevationUnit();
        StringFormatter.elevationFactor = Float.parseFloat(resources.getStringArray(R.array.elevation_factors)[unit]);
        StringFormatter.elevationAbbr = resources.getStringArray(R.array.elevation_abbreviations)[unit];
        unit = Configuration.getAngleUnit();
        StringFormatter.angleFactor = Double.parseDouble(resources.getStringArray(R.array.angle_factors)[unit]);
        StringFormatter.angleAbbr = resources.getStringArray(R.array.angle_abbreviations)[unit];
        StringFormatter.precisionFormat = Configuration.getUnitPrecision() ? "%.1f" : "%.0f";
        StringFormatter.coordinateFormat = Configuration.getCoordinatesFormat();
        Configuration.loadKindZoomState();
    }

    public static MapTrek getApplication() {
        return mSelf;
    }

    public synchronized SQLiteDatabase getDetailedMapDatabase() {
        SQLiteDatabase sQLiteDatabase;
        boolean fresh = true;
        synchronized (this) {
            if (this.mDetailedMapHelper == null) {
                File dbFile = new File(getExternalFilesDir("native"), Index.WORLDMAP_FILENAME);
                if (dbFile.exists()) {
                    fresh = false;
                }
                if (fresh) {
                    copyAsset("databases/basemap.mtiles", dbFile);
                }
                this.mDetailedMapHelper = new MapTrekDatabaseHelper(this, dbFile);
                this.mDetailedMapHelper.setWriteAheadLoggingEnabled(true);
                try {
                    this.mDetailedMapDatabase = this.mDetailedMapHelper.getWritableDatabase();
                } catch (SQLiteException e) {
                    this.mDetailedMapHelper.close();
                    if (dbFile.delete()) {
                        copyAsset("databases/basemap.mtiles", dbFile);
                        this.mDetailedMapHelper = new MapTrekDatabaseHelper(this, dbFile);
                        this.mDetailedMapHelper.setWriteAheadLoggingEnabled(true);
                        this.mDetailedMapDatabase = this.mDetailedMapHelper.getWritableDatabase();
                        fresh = true;
                        this.mUserNotification = getString(R.string.msgMapDatabaseError);
                    }
                }
                if (fresh) {
                    MapTrekDatabaseHelper.createFtsTable(this.mDetailedMapDatabase);
                }
            }
            sQLiteDatabase = this.mDetailedMapDatabase;
        }
        return sQLiteDatabase;
    }

    private synchronized HillshadeDatabaseHelper getHillshadeDatabaseHelper(boolean reset) {
        if (this.mHillshadeHelper == null) {
            File file = new File(getExternalFilesDir("native"), Index.HILLSHADE_FILENAME);
            if (reset) {
                file.delete();
            }
            this.mHillshadeHelper = new HillshadeDatabaseHelper(this, file);
            this.mHillshadeHelper.setWriteAheadLoggingEnabled(true);
        }
        return this.mHillshadeHelper;
    }

    public synchronized SQLiteDatabase getHillshadeDatabase() {
        if (this.mHillshadeDatabase == null) {
            try {
                this.mHillshadeDatabase = getHillshadeDatabaseHelper(false).getWritableDatabase();
            } catch (SQLiteException e) {
                this.mHillshadeHelper.close();
                this.mHillshadeHelper = null;
                this.mHillshadeDatabase = getHillshadeDatabaseHelper(true).getWritableDatabase();
                this.mUserNotification = getString(R.string.msgHillshadeDatabaseError);
            }
        }
        return this.mHillshadeDatabase;
    }

    @Nullable
    public SQLiteTileSource getHillShadeTileSource() {
        SQLiteTileSource tileSource = new SQLiteTileSource(getHillshadeDatabaseHelper(false));
        return tileSource.open().isSuccess() ? tileSource : null;
    }

    public Index getMapIndex() {
        if (this.mIndex == null) {
            this.mIndex = new Index(this, getDetailedMapDatabase(), getHillshadeDatabase());
        }
        return this.mIndex;
    }

    private void copyAsset(String asset, File outFile) {
        try {
            InputStream in = getAssets().open(asset);
            OutputStream out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            while (true) {
                int read = in.read(buffer);
                if (read != -1) {
                    out.write(buffer, 0, read);
                } else {
                    in.close();
                    out.close();
                    return;
                }
            }
        } catch (Throwable e) {
            logger.error("Failed to copy world map asset", e);
        }
    }

    public boolean hasPreviousRunsExceptions() {
        long size = Configuration.getExceptionSize();
        if (!this.mExceptionLog.exists() || this.mExceptionLog.length() <= 0) {
            if (size > 0) {
                Configuration.setExceptionSize(0);
            }
        } else if (size != this.mExceptionLog.length()) {
            Configuration.setExceptionSize(this.mExceptionLog.length());
            return true;
        }
        return false;
    }

    public String getUserNotification() {
        String notification = this.mUserNotification;
        this.mUserNotification = null;
        return notification;
    }

    public static long getNewUID() {
        return Configuration.getUID();
    }

    public static long addMapObject(MapObject mapObject) {
        mapObject._id = getNewUID();
        logger.debug("addMapObject({})", Long.valueOf(mapObject._id));
        synchronized (mapObjects) {
            mapObjects.put(mapObject._id, mapObject);
        }
        EventBus.getDefault().post(new AddedEvent(mapObject));
        return mapObject._id;
    }

    public static boolean removeMapObject(long id) {
        boolean z;
        synchronized (mapObjects) {
            logger.debug("removeMapObject({})", Long.valueOf(id));
            MapObject mapObject = (MapObject) mapObjects.get(id);
            mapObjects.delete(id);
            if (mapObject != null) {
                mapObject.setBitmap(null);
                EventBus.getDefault().post(new RemovedEvent(mapObject));
            }
            z = mapObject != null;
        }
        return z;
    }

    @Nullable
    public static MapObject getMapObject(long id) {
        return (MapObject) mapObjects.get(id);
    }

    @NonNull
    public static Iterator<MapObject> getMapObjects() {
        return LongSparseArrayIterator.iterate(mapObjects);
    }

    public File getExceptionLog() {
        return this.mExceptionLog;
    }

    public void registerException(Throwable ex) {
        this.mExceptionHandler.caughtException(Thread.currentThread(), ex);
    }
}
