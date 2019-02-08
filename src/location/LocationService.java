package mobi.maptrek.location;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.Action.Builder;
import android.app.Notification.BigTextStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Icon;
import android.location.GpsSatellite;
import android.location.GpsStatus.Listener;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.text.format.DateUtils;
import gov.nasa.worldwind.util.WWMath;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import mobi.maptrek.Configuration;
import mobi.maptrek.MainActivity;
import mobi.maptrek.MainActivity.TRACKING_STATE;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Track.TrackPoint;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.Manager.OnSaveListener;
import mobi.maptrek.location.ILocationRemoteService.Stub;
import mobi.maptrek.util.ProgressListener;
import mobi.maptrek.util.StringFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationService extends BaseLocationService implements LocationListener, NmeaListener, Listener, OnSharedPreferenceChangeListener {
    private static final boolean DEBUG_ERRORS = false;
    private static final int NOTIFICATION_ID = 25501;
    private static final String PREF_TRACKING_MIN_DISTANCE = "tracking_min_distance";
    private static final String PREF_TRACKING_MIN_TIME = "tracking_min_time";
    private static final int SKIP_INITIAL_LOCATIONS = 2;
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    private static final float TOO_SMALL_DISTANCE = 100.0f;
    private static final long TOO_SMALL_PERIOD = 60000;
    private static final boolean enableMockLocations = false;
    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);
    private final Binder mBinder = new LocalBinder();
    private boolean mContinuous = false;
    private float mDistanceNotified;
    private float mDistanceTracked = 0.0f;
    private String mErrorMsg = "";
    private long mErrorTime = 0;
    private int mFSats = 0;
    private boolean mForeground = false;
    private int mGpsStatus = 1;
    private float mHDOP = Float.NaN;
    private boolean mJustStarted = true;
    private Location mLastKnownLocation = null;
    private long mLastLocationMillis = 0;
    private Track mLastTrack;
    private Location mLastWrittenLocation = null;
    private final Set<ILocationListener> mLocationCallbacks = new HashSet();
    private LocationManager mLocationManager = null;
    private final Stub mLocationRemoteBinder = new Stub() {
        public void registerCallback(ILocationCallback callback) {
            LocationService.logger.debug("Register callback");
            if (callback != null) {
                if (!EnvironmentCompat.MEDIA_UNKNOWN.equals(LocationService.this.mLastKnownLocation.getProvider())) {
                    try {
                        callback.onLocationChanged();
                        callback.onGpsStatusChanged();
                    } catch (Throwable e) {
                        LocationService.logger.error("Location broadcast error", e);
                    }
                }
                LocationService.this.mLocationRemoteCallbacks.register(callback);
            }
        }

        public void unregisterCallback(ILocationCallback callback) {
            if (callback != null) {
                LocationService.this.mLocationRemoteCallbacks.unregister(callback);
            }
        }

        public boolean isLocating() {
            return LocationService.this.mLocationsEnabled;
        }

        public Location getLocation() throws RemoteException {
            return LocationService.this.mLastKnownLocation;
        }

        public int getStatus() throws RemoteException {
            return LocationService.this.mGpsStatus;
        }
    };
    private final RemoteCallbackList<ILocationCallback> mLocationRemoteCallbacks = new RemoteCallbackList();
    private boolean mLocationsEnabled = false;
    private long mMaxTime = 300000;
    private int mMinDistance = 3;
    private long mMinTime = 2000;
    private Handler mMockCallback = new Handler();
    private int mMockLocationTicker = 0;
    private float mNmeaGeoidHeight = Float.NaN;
    private ProgressListener mProgressListener;
    private final Runnable mSendMockLocation = new Runnable() {
        public void run() {
            LocationService.this.mMockCallback.postDelayed(this, 300);
            LocationService.this.mMockLocationTicker = LocationService.this.mMockLocationTicker + 1;
            int ddd = LocationService.this.mMockLocationTicker % Callback.DEFAULT_DRAG_ANIMATION_DURATION;
            if (ddd < 0 || ddd >= 10) {
                if (LocationService.this.mGpsStatus == 2) {
                    LocationService.this.mGpsStatus = 3;
                    LocationService.this.updateGpsStatus();
                }
                LocationService.this.mLastKnownLocation = new Location("gps");
                LocationService.this.mLastKnownLocation.setTime(System.currentTimeMillis());
                LocationService.this.mLastKnownLocation.setAccuracy((float) ((LocationService.this.mMockLocationTicker % 100) + 3));
                LocationService.this.mLastKnownLocation.setSpeed(20.0f);
                LocationService.this.mLastKnownLocation.setAltitude((double) (LocationService.this.mMockLocationTicker + 20));
                double lat = 60.0d + (((double) LocationService.this.mMockLocationTicker) * WWMath.SQUARE_METERS_TO_HECTARES);
                if (ddd < 10) {
                    LocationService.this.mLastKnownLocation.setBearing((float) ddd);
                }
                if (ddd < 90) {
                    LocationService.this.mLastKnownLocation.setBearing(10.0f);
                } else if (ddd < 110) {
                    LocationService.this.mLastKnownLocation.setBearing((float) (100 - ddd));
                } else if (ddd < 190) {
                    LocationService.this.mLastKnownLocation.setBearing(-10.0f);
                } else {
                    LocationService.this.mLastKnownLocation.setBearing((float) (ddd - 200));
                }
                LocationService.this.mLastKnownLocation.setLatitude(lat);
                LocationService.this.mLastKnownLocation.setLongitude(30.3d);
                LocationService.this.mNmeaGeoidHeight = 0.0f;
                LocationService.this.updateLocation();
                LocationService.this.mContinuous = true;
                return;
            }
            LocationService.this.mGpsStatus = 2;
            LocationService.this.mFSats = LocationService.this.mMockLocationTicker % 10;
            LocationService.this.mTSats = 25;
            LocationService.this.mContinuous = false;
            LocationService.this.updateGpsStatus();
        }
    };
    private int mTSats = 0;
    private SQLiteDatabase mTrackDB = null;
    private long mTrackStarted = 0;
    private final Set<ITrackingListener> mTrackingCallbacks = new HashSet();
    private boolean mTrackingEnabled = false;
    private long mTrackingStarted;
    private float mVDOP = Float.NaN;

    public class LocalBinder extends Binder implements ILocationService {
        public void registerLocationCallback(ILocationListener callback) {
            if (!LocationService.this.mLocationsEnabled) {
                LocationService.this.connect();
            }
            if (!EnvironmentCompat.MEDIA_UNKNOWN.equals(LocationService.this.mLastKnownLocation.getProvider())) {
                callback.onLocationChanged();
                callback.onGpsStatusChanged();
            }
            LocationService.this.mLocationCallbacks.add(callback);
        }

        public void unregisterLocationCallback(ILocationListener callback) {
            LocationService.this.mLocationCallbacks.remove(callback);
        }

        public void registerTrackingCallback(ITrackingListener callback) {
            LocationService.this.mTrackingCallbacks.add(callback);
        }

        public void unregisterTrackingCallback(ITrackingListener callback) {
            LocationService.this.mTrackingCallbacks.remove(callback);
        }

        public void setProgressListener(ProgressListener listener) {
            LocationService.this.mProgressListener = listener;
        }

        public boolean isLocating() {
            return LocationService.this.mLocationsEnabled;
        }

        public boolean isTracking() {
            return LocationService.this.mTrackingEnabled;
        }

        public Location getLocation() {
            return LocationService.this.mLastKnownLocation;
        }

        public int getStatus() {
            return LocationService.this.mGpsStatus;
        }

        public int getSatellites() {
            return (LocationService.this.mFSats << 7) + LocationService.this.mTSats;
        }

        public float getHDOP() {
            return LocationService.this.mHDOP;
        }

        public float getVDOP() {
            return LocationService.this.mVDOP;
        }

        public Track getTrack() {
            return LocationService.this.getTrack();
        }

        public Track getTrack(long start, long end) {
            return LocationService.this.getTrack(start, end);
        }

        public void saveTrack() {
            LocationService.this.saveTrack();
        }

        public void clearTrack() {
            LocationService.this.clearTrack();
        }

        public long getTrackStartTime() {
            return LocationService.this.getTrackStartTime();
        }

        public long getTrackEndTime() {
            return LocationService.this.getTrackEndTime();
        }
    }

    public void onCreate() {
        this.mLastKnownLocation = new Location(EnvironmentCompat.MEDIA_UNKNOWN);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        onSharedPreferenceChanged(sharedPreferences, PREF_TRACKING_MIN_TIME);
        onSharedPreferenceChanged(sharedPreferences, PREF_TRACKING_MIN_DISTANCE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        logger.debug("Service started");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return 2;
        }
        Object action = intent.getAction();
        logger.debug("Command: {}", action);
        if (action.equals(BaseLocationService.ENABLE_TRACK) || (action.equals(BaseLocationService.ENABLE_BACKGROUND_TRACK) && !this.mTrackingEnabled)) {
            this.mErrorMsg = "";
            this.mErrorTime = 0;
            this.mTrackingEnabled = true;
            this.mContinuous = false;
            this.mDistanceNotified = 0.0f;
            openDatabase();
            this.mTrackingStarted = SystemClock.uptimeMillis();
            this.mTrackStarted = System.currentTimeMillis();
        }
        if (action.equals(BaseLocationService.DISABLE_TRACK) || (action.equals(BaseLocationService.PAUSE_TRACK) && this.mTrackingEnabled)) {
            this.mTrackingEnabled = false;
            this.mForeground = false;
            updateDistanceTracked();
            closeDatabase();
            stopForeground(true);
            Configuration.updateTrackingTime((SystemClock.uptimeMillis() - this.mTrackingStarted) / TOO_SMALL_PERIOD);
            if (action.equals(BaseLocationService.DISABLE_TRACK)) {
                if (intent.getBooleanExtra("self", false)) {
                    Configuration.setTrackingState(TRACKING_STATE.DISABLED.ordinal());
                }
                tryToSaveTrack();
            }
            stopSelf();
        }
        if (action.equals(BaseLocationService.ENABLE_BACKGROUND_TRACK)) {
            this.mForeground = true;
            updateDistanceTracked();
            startForeground(NOTIFICATION_ID, getNotification());
        }
        if (action.equals(BaseLocationService.DISABLE_BACKGROUND_TRACK)) {
            this.mForeground = false;
            stopForeground(true);
        }
        updateNotification();
        return 1;
    }

    public void onDestroy() {
        updateDistanceTracked();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        disconnect();
        closeDatabase();
        logger.debug("Service stopped");
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        if (BaseLocationService.MAPTREK_LOCATION_SERVICE.equals(intent.getAction()) || ILocationRemoteService.class.getName().equals(intent.getAction())) {
            return this.mLocationRemoteBinder;
        }
        return this.mBinder;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREF_TRACKING_MIN_TIME.equals(key)) {
            this.mMinTime = 500;
        } else if (PREF_TRACKING_MIN_DISTANCE.equals(key)) {
            this.mMinDistance = 5;
        }
    }

    private void connect() {
        logger.debug("connect()");
        this.mLocationManager = (LocationManager) getSystemService("location");
        if (this.mLocationManager != null) {
            this.mLastLocationMillis = -2;
            this.mContinuous = false;
            this.mJustStarted = true;
            if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0) {
                this.mLocationManager.addGpsStatusListener(this);
                try {
                    this.mLocationManager.requestLocationUpdates("gps", 300, 0.0f, this);
                    this.mLocationManager.addNmeaListener(this);
                    this.mLocationsEnabled = true;
                    logger.debug("Gps provider set");
                    return;
                } catch (IllegalArgumentException e) {
                    logger.warn("Cannot set gps provider, likely no gps on device");
                    return;
                }
            }
            logger.error("Missing ACCESS_FINE_LOCATION permission");
        }
    }

    private void disconnect() {
        logger.debug("disconnect()");
        if (this.mLocationManager != null) {
            this.mLocationsEnabled = false;
            this.mLocationManager.removeNmeaListener(this);
            try {
                this.mLocationManager.removeUpdates(this);
            } catch (Throwable e) {
                logger.error("Failed to remove updates", e);
            }
            this.mLocationManager.removeGpsStatusListener(this);
            this.mLocationManager = null;
        }
    }

    private Notification getNotification() {
        int titleId = R.string.notifTracking;
        int ntfId = R.mipmap.ic_stat_tracking;
        if (this.mGpsStatus != 3) {
            titleId = R.string.notifLocationWaiting;
            ntfId = R.mipmap.ic_stat_waiting;
        }
        if (this.mGpsStatus == 1) {
            titleId = R.string.notifLocationWaiting;
            ntfId = R.mipmap.ic_stat_off;
        }
        if (this.mErrorTime > 0) {
            titleId = R.string.notifTrackingFailure;
            ntfId = R.mipmap.ic_stat_failure;
        }
        String timeTracked = (String) DateUtils.getRelativeTimeSpanString(getApplicationContext(), this.mTrackStarted);
        String distanceTracked = StringFormatter.distanceH((double) this.mDistanceTracked);
        StringBuilder stringBuilder = new StringBuilder(40);
        stringBuilder.append(getString(R.string.msgTracked, new Object[]{distanceTracked, timeTracked}));
        String message = stringBuilder.toString();
        stringBuilder.insert(0, ". ");
        stringBuilder.insert(0, getString(R.string.msgTracking));
        stringBuilder.append(". ");
        stringBuilder.append(getString(R.string.msgTrackingActions));
        stringBuilder.append(".");
        String bigText = stringBuilder.toString();
        Intent iLaunch = new Intent("android.intent.action.MAIN");
        iLaunch.addCategory("android.intent.category.LAUNCHER");
        iLaunch.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
        iLaunch.setFlags(270532608);
        PendingIntent piResult = PendingIntent.getActivity(this, 0, iLaunch, 268435456);
        Intent iStop = new Intent(BaseLocationService.DISABLE_TRACK, null, getApplicationContext(), LocationService.class);
        iStop.putExtra("self", true);
        PendingIntent piStop = PendingIntent.getService(this, 0, iStop, 268435456);
        Icon stopIcon = Icon.createWithResource(this, R.drawable.ic_stop);
        PendingIntent piPause = PendingIntent.getService(this, 0, new Intent(BaseLocationService.PAUSE_TRACK, null, getApplicationContext(), LocationService.class), 268435456);
        Icon pauseIcon = Icon.createWithResource(this, R.drawable.ic_pause);
        Action actionStop = new Builder(stopIcon, getString(R.string.actionStop), piStop).build();
        Action actionPause = new Builder(pauseIcon, getString(R.string.actionPause), piPause).build();
        Notification.Builder builder = new Notification.Builder(this);
        builder.setWhen(this.mErrorTime);
        builder.setSmallIcon(ntfId);
        builder.setContentIntent(piResult);
        builder.setContentTitle(getText(titleId));
        builder.setStyle(new BigTextStyle().setBigContentTitle(getText(titleId)).bigText(bigText));
        builder.addAction(actionPause);
        builder.addAction(actionStop);
        builder.setGroup("maptrek");
        builder.setCategory("service");
        builder.setPriority(-1);
        builder.setVisibility(1);
        builder.setColor(getResources().getColor(R.color.colorAccent, getTheme()));
        if (this.mErrorTime > 0) {
            builder.setContentText(message);
            builder.setOngoing(true);
        } else {
            builder.setContentText(message);
            builder.setOngoing(true);
        }
        return builder.build();
    }

    private void updateNotification() {
        if (this.mForeground) {
            logger.debug("updateNotification()");
            ((NotificationManager) getSystemService("notification")).notify(NOTIFICATION_ID, getNotification());
        }
    }

    private void openDatabase() {
        try {
            this.mTrackDB = SQLiteDatabase.openDatabase(new File(getExternalFilesDir("databases"), "track.sqlitedb").getAbsolutePath(), null, 268435472);
            Cursor cursor = this.mTrackDB.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = 'track'", null);
            if (cursor.getCount() == 0) {
                this.mTrackDB.execSQL("CREATE TABLE track (_id INTEGER PRIMARY KEY, latitude INTEGER, longitude INTEGER, code INTEGER, elevation REAL, speed REAL, track REAL, accuracy REAL, datetime INTEGER)");
            }
            cursor.close();
            this.mDistanceTracked = 0.0f;
            cursor = this.mTrackDB.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = 'track_properties'", null);
            if (cursor.getCount() == 0) {
                this.mTrackDB.execSQL("CREATE TABLE track_properties (_id INTEGER PRIMARY KEY, distance REAL)");
            } else {
                Cursor propertiesCursor = this.mTrackDB.rawQuery("SELECT * FROM track_properties ORDER BY _id DESC LIMIT 1", null);
                if (propertiesCursor.moveToFirst()) {
                    this.mDistanceTracked = propertiesCursor.getFloat(propertiesCursor.getColumnIndex("distance"));
                }
                propertiesCursor.close();
            }
            cursor.close();
            this.mTrackStarted = getTrackStartTime();
        } catch (Throwable e) {
            this.mTrackDB = null;
            logger.error("openDatabase", e);
            this.mErrorMsg = "Failed to open DB";
            this.mErrorTime = System.currentTimeMillis();
            updateNotification();
        }
    }

    private void closeDatabase() {
        if (this.mTrackDB != null) {
            this.mTrackDB.close();
            this.mTrackDB = null;
        }
    }

    public Track getTrack() {
        Track track = getTrack(0);
        this.mDistanceTracked = track.getDistance();
        return track;
    }

    public Track getTrack(long limit) {
        if (this.mTrackDB == null) {
            openDatabase();
        }
        Track track = new Track(getString(R.string.currentTrack), true);
        if (this.mTrackDB != null) {
            Cursor cursor = this.mTrackDB.rawQuery("SELECT * FROM track ORDER BY _id DESC" + (limit > 0 ? " LIMIT " + limit : ""), null);
            for (boolean hasItem = cursor.moveToLast(); hasItem; hasItem = cursor.moveToPrevious()) {
                boolean z;
                int latitudeE6 = cursor.getInt(cursor.getColumnIndex("latitude"));
                int longitudeE6 = cursor.getInt(cursor.getColumnIndex("longitude"));
                float elevation = cursor.getFloat(cursor.getColumnIndex("elevation"));
                float speed = cursor.getFloat(cursor.getColumnIndex("speed"));
                float bearing = cursor.getFloat(cursor.getColumnIndex("track"));
                float accuracy = cursor.getFloat(cursor.getColumnIndex("accuracy"));
                int code = cursor.getInt(cursor.getColumnIndex("code"));
                long time = cursor.getLong(cursor.getColumnIndex("datetime"));
                if (code == 0) {
                    z = true;
                } else {
                    z = false;
                }
                track.addPoint(z, latitudeE6, longitudeE6, elevation, speed, bearing, accuracy, time);
            }
            cursor.close();
        }
        return track;
    }

    public Track getTrack(long start, long end) {
        if (this.mTrackDB == null) {
            openDatabase();
        }
        Track track = new Track();
        if (this.mTrackDB != null) {
            Cursor cursor = this.mTrackDB.rawQuery("SELECT * FROM track WHERE datetime >= ? AND datetime <= ? ORDER BY _id DESC", new String[]{String.valueOf(start), String.valueOf(end)});
            for (boolean hasItem = cursor.moveToLast(); hasItem; hasItem = cursor.moveToPrevious()) {
                boolean z;
                int latitudeE6 = cursor.getInt(cursor.getColumnIndex("latitude"));
                int longitudeE6 = cursor.getInt(cursor.getColumnIndex("longitude"));
                float elevation = cursor.getFloat(cursor.getColumnIndex("elevation"));
                float speed = cursor.getFloat(cursor.getColumnIndex("speed"));
                float bearing = cursor.getFloat(cursor.getColumnIndex("track"));
                float accuracy = cursor.getFloat(cursor.getColumnIndex("accuracy"));
                int code = cursor.getInt(cursor.getColumnIndex("code"));
                long time = cursor.getLong(cursor.getColumnIndex("datetime"));
                if (code == 0) {
                    z = true;
                } else {
                    z = false;
                }
                track.addPoint(z, latitudeE6, longitudeE6, elevation, speed, bearing, accuracy, time);
            }
            cursor.close();
        }
        return track;
    }

    public long getTrackStartTime() {
        long res = Long.MIN_VALUE;
        if (this.mTrackDB == null) {
            openDatabase();
        }
        if (this.mTrackDB == null) {
            return Long.MIN_VALUE;
        }
        Cursor cursor = this.mTrackDB.rawQuery("SELECT MIN(datetime) FROM track WHERE datetime > 0", null);
        if (cursor.moveToFirst()) {
            res = cursor.getLong(0);
        }
        cursor.close();
        return res;
    }

    public long getTrackEndTime() {
        long res = Long.MAX_VALUE;
        if (this.mTrackDB == null) {
            openDatabase();
        }
        if (this.mTrackDB == null) {
            return Long.MAX_VALUE;
        }
        Cursor cursor = this.mTrackDB.rawQuery("SELECT MAX(datetime) FROM track", null);
        if (cursor.moveToFirst()) {
            res = cursor.getLong(0);
        }
        cursor.close();
        return res;
    }

    public void clearTrack() {
        this.mDistanceTracked = 0.0f;
        if (this.mTrackDB == null) {
            openDatabase();
        }
        if (this.mTrackDB != null) {
            this.mTrackDB.execSQL("DELETE FROM track");
            this.mTrackDB.execSQL("DELETE FROM track_properties");
        }
    }

    public void tryToSaveTrack() {
        this.mLastTrack = getTrack();
        if (this.mLastTrack.points.size() != 0) {
            long startTime = ((TrackPoint) this.mLastTrack.points.get(0)).time;
            long stopTime = this.mLastTrack.getLastPoint().time;
            long period = stopTime - startTime;
            int flags = 2560;
            if (period < 604800000) {
                flags = 2560 | 1;
            }
            if (period < 2419200000L) {
                flags |= 2;
            }
            this.mLastTrack.description = DateUtils.formatDateRange(this, startTime, stopTime, flags) + " — " + StringFormatter.distanceH((double) this.mLastTrack.getDistance());
            flags |= 524308;
            this.mLastTrack.name = DateUtils.formatDateRange(this, startTime, stopTime, flags);
            if (period < TOO_SMALL_PERIOD) {
                sendBroadcast(new Intent(BaseLocationService.BROADCAST_TRACK_SAVE).putExtra("saved", false).putExtra("reason", "period"));
                clearTrack();
            } else if (this.mLastTrack.getDistance() < TOO_SMALL_DISTANCE) {
                sendBroadcast(new Intent(BaseLocationService.BROADCAST_TRACK_SAVE).putExtra("saved", false).putExtra("reason", "distance"));
                clearTrack();
            } else {
                saveTrack();
            }
        }
    }

    private void saveTrack() {
        if (this.mLastTrack == null) {
            sendBroadcast(new Intent(BaseLocationService.BROADCAST_TRACK_SAVE).putExtra("saved", false).putExtra("reason", "missing"));
        } else if (getExternalFilesDir("data") == null) {
            logger.error("Can not save track: application data folder missing");
            sendBroadcast(new Intent(BaseLocationService.BROADCAST_TRACK_SAVE).putExtra("saved", false).putExtra("reason", "error").putExtra("exception", new RuntimeException("Application data folder missing")));
        } else {
            FileDataSource source = new FileDataSource();
            source.name = TIME_FORMAT.format(new Date(((TrackPoint) this.mLastTrack.points.get(0)).time));
            source.tracks.add(this.mLastTrack);
            Manager.save(this, source, new OnSaveListener() {
                public void onSaved(FileDataSource source) {
                    LocationService.this.sendBroadcast(new Intent(BaseLocationService.BROADCAST_TRACK_SAVE).putExtra("saved", true).putExtra("path", source.path));
                    LocationService.this.clearTrack();
                    LocationService.this.mLastTrack = null;
                }

                public void onError(FileDataSource source, Exception e) {
                    LocationService.this.sendBroadcast(new Intent(BaseLocationService.BROADCAST_TRACK_SAVE).putExtra("saved", false).putExtra("reason", "error").putExtra("exception", e));
                }
            }, this.mProgressListener);
        }
    }

    private void updateDistanceTracked() {
        if (this.mTrackDB != null) {
            this.mTrackDB.delete("track_properties", null, null);
            ContentValues values = new ContentValues();
            values.put("distance", Float.valueOf(this.mDistanceTracked));
            this.mTrackDB.insert("track_properties", null, values);
        }
    }

    public void addPoint(boolean continuous, double latitude, double longitude, float elevation, float speed, float bearing, float accuracy, long time) {
        if (this.mTrackDB == null) {
            openDatabase();
            if (this.mTrackDB == null) {
                return;
            }
        }
        ContentValues values = new ContentValues();
        values.put("latitude", Integer.valueOf((int) (1000000.0d * latitude)));
        values.put("longitude", Integer.valueOf((int) (1000000.0d * longitude)));
        values.put("code", Integer.valueOf(continuous ? 0 : 1));
        values.put("elevation", Float.valueOf(elevation));
        values.put("speed", Float.valueOf(speed));
        values.put("track", Float.valueOf(bearing));
        values.put("accuracy", Float.valueOf(accuracy));
        values.put("datetime", Long.valueOf(time));
        try {
            this.mTrackDB.insertOrThrow("track", null, values);
        } catch (Throwable e) {
            logger.error("addPoint", e);
            this.mErrorMsg = e.getMessage();
            this.mErrorTime = System.currentTimeMillis();
            updateNotification();
            closeDatabase();
        }
    }

    private void writeTrackPoint(Location loc, float distance, boolean continuous) {
        addPoint(continuous, loc.getLatitude(), loc.getLongitude(), (float) loc.getAltitude(), loc.getSpeed(), loc.getBearing(), loc.getAccuracy(), loc.getTime());
        this.mDistanceTracked += distance;
        this.mDistanceNotified += distance;
        if (this.mDistanceNotified > this.mDistanceTracked / TOO_SMALL_DISTANCE) {
            updateNotification();
            this.mDistanceNotified = 0.0f;
        }
        this.mLastWrittenLocation = loc;
        for (ITrackingListener callback : this.mTrackingCallbacks) {
            callback.onNewPoint(continuous, loc.getLatitude(), loc.getLongitude(), (float) loc.getAltitude(), loc.getSpeed(), loc.getBearing(), loc.getAccuracy(), loc.getTime());
        }
    }

    private void writeTrack(Location loc, boolean continuous) {
        float distance = 0.0f;
        long time = 0;
        if (this.mLastWrittenLocation != null) {
            distance = loc.distanceTo(this.mLastWrittenLocation);
            time = loc.getTime() - this.mLastWrittenLocation.getTime();
        }
        if (this.mLastWrittenLocation == null || !continuous || time > this.mMaxTime || (distance > ((float) this.mMinDistance) && time > this.mMinTime)) {
            writeTrackPoint(loc, distance, continuous);
        }
    }

    private void tearTrack() {
        if (!(this.mLastKnownLocation == this.mLastWrittenLocation || EnvironmentCompat.MEDIA_UNKNOWN.equals(this.mLastKnownLocation.getProvider()))) {
            writeTrackPoint(this.mLastKnownLocation, this.mLastWrittenLocation != null ? this.mLastKnownLocation.distanceTo(this.mLastWrittenLocation) : 0.0f, this.mContinuous);
        }
        this.mContinuous = false;
    }

    private void updateLocation() {
        final Location location = this.mLastKnownLocation;
        final boolean continuous = this.mContinuous;
        Handler handler = new Handler();
        if (this.mTrackingEnabled) {
            handler.post(new Runnable() {
                public void run() {
                    LocationService.this.writeTrack(location, continuous);
                }
            });
        }
        for (final ILocationListener callback : this.mLocationCallbacks) {
            handler.post(new Runnable() {
                public void run() {
                    callback.onLocationChanged();
                }
            });
        }
        int n = this.mLocationRemoteCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                ((ILocationCallback) this.mLocationRemoteCallbacks.getBroadcastItem(i)).onLocationChanged();
            } catch (Throwable e) {
                logger.error("Location broadcast error", e);
            }
        }
        this.mLocationRemoteCallbacks.finishBroadcast();
    }

    private void updateGpsStatus() {
        if (this.mGpsStatus == 2) {
            logger.debug("Searching: {}/{}", Integer.valueOf(this.mFSats), Integer.valueOf(this.mTSats));
        }
        updateNotification();
        Handler handler = new Handler();
        for (final ILocationListener callback : this.mLocationCallbacks) {
            handler.post(new Runnable() {
                public void run() {
                    callback.onGpsStatusChanged();
                }
            });
        }
        int n = this.mLocationRemoteCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                ((ILocationCallback) this.mLocationRemoteCallbacks.getBroadcastItem(i)).onGpsStatusChanged();
            } catch (Throwable e) {
                logger.error("Location broadcast error", e);
            }
        }
        this.mLocationRemoteCallbacks.finishBroadcast();
    }

    public void onLocationChanged(Location location) {
        if (this.mLastLocationMillis < 0) {
            this.mLastLocationMillis++;
            return;
        }
        long time = SystemClock.elapsedRealtime();
        long prevLocationMillis = this.mLastLocationMillis;
        float prevSpeed = this.mLastKnownLocation.getSpeed();
        float prevTrack = this.mLastKnownLocation.getBearing();
        this.mLastKnownLocation = location;
        if (this.mLastKnownLocation.getSpeed() == 0.0f && prevTrack != 0.0f) {
            this.mLastKnownLocation.setBearing(prevTrack);
        }
        this.mLastLocationMillis = time;
        if (!Float.isNaN(this.mNmeaGeoidHeight)) {
            this.mLastKnownLocation.setAltitude(this.mLastKnownLocation.getAltitude() + ((double) this.mNmeaGeoidHeight));
        }
        if (this.mJustStarted) {
            this.mJustStarted = prevSpeed == 0.0f;
        } else if (this.mLastKnownLocation.getSpeed() > 0.0f) {
            if (((double) Math.abs(this.mLastKnownLocation.getSpeed() - prevSpeed)) > (19.6d * ((double) (this.mLastLocationMillis - prevLocationMillis))) / WWMath.SECOND_TO_MILLIS) {
                this.mLastKnownLocation.setSpeed(prevSpeed);
            }
        }
        updateLocation();
        this.mContinuous = true;
    }

    public void onNmeaReceived(long timestamp, String nmea) {
        if (nmea.indexOf(10) != 0) {
            if (nmea.indexOf(10) > 0) {
                nmea = nmea.substring(0, nmea.indexOf(10) - 1);
            }
            int len = nmea.length();
            if (len >= 9) {
                if (nmea.charAt(len - 3) == '*') {
                    nmea = nmea.substring(0, len - 3);
                }
                String[] tokens = nmea.split(",");
                String sentenceId = tokens[0].length() > 5 ? tokens[0].substring(3, 6) : "";
                try {
                    if (sentenceId.equals("GGA") && tokens.length > 11) {
                        String heightOfGeoid = tokens[11];
                        if (!"".equals(heightOfGeoid)) {
                            this.mNmeaGeoidHeight = Float.parseFloat(heightOfGeoid);
                        }
                    } else if (sentenceId.equals("GSA") && tokens.length > 17) {
                        String pdop = tokens[15];
                        String hdop = tokens[16];
                        String vdop = tokens[17];
                        if (!"".equals(hdop)) {
                            this.mHDOP = Float.parseFloat(hdop);
                        }
                        if (!"".equals(vdop)) {
                            this.mVDOP = Float.parseFloat(vdop);
                        }
                    }
                } catch (Throwable e) {
                    logger.error("NFE", e);
                } catch (Throwable e2) {
                    logger.error("AIOOBE", e2);
                }
            }
        }
    }

    public void onProviderDisabled(String provider) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        if ("gps".equals(provider)) {
            switch (status) {
                case 0:
                case 1:
                    tearTrack();
                    updateNotification();
                    return;
                default:
                    return;
            }
        }
    }

    public void onGpsStatusChanged(int event) {
        switch (event) {
            case 1:
                this.mGpsStatus = 2;
                this.mTSats = 0;
                this.mFSats = 0;
                updateGpsStatus();
                return;
            case 2:
                tearTrack();
                this.mGpsStatus = 1;
                this.mTSats = 0;
                this.mFSats = 0;
                updateGpsStatus();
                return;
            case 3:
                this.mContinuous = false;
                return;
            case 4:
                if (this.mLocationManager != null) {
                    try {
                        this.mTSats = 0;
                        this.mFSats = 0;
                        for (GpsSatellite sat : this.mLocationManager.getGpsStatus(null).getSatellites()) {
                            this.mTSats++;
                            if (sat.usedInFix()) {
                                this.mFSats++;
                            }
                        }
                        if (this.mLastLocationMillis >= 0) {
                            if (SystemClock.elapsedRealtime() - this.mLastLocationMillis < 3000) {
                                this.mGpsStatus = 3;
                            } else {
                                if (this.mContinuous) {
                                    tearTrack();
                                }
                                this.mGpsStatus = 2;
                            }
                        }
                        updateGpsStatus();
                        return;
                    } catch (Throwable e) {
                        logger.error("Failed to update gps status", e);
                        return;
                    }
                }
                return;
            default:
                return;
        }
    }
}
