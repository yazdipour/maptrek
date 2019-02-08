package mobi.maptrek.location;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.Action.Builder;
import android.app.Notification.BigTextStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import mobi.maptrek.Configuration;
import mobi.maptrek.MainActivity;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.MapObject;
import mobi.maptrek.data.MapObject.UpdatedEvent;
import mobi.maptrek.data.Route;
import mobi.maptrek.util.Geo;
import mobi.maptrek.util.StringFormatter;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.oscim.core.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavigationService extends BaseNavigationService implements OnSharedPreferenceChangeListener {
    private static final int NOTIFICATION_ID = 25502;
    public static final String PREF_NAVIGATION_BACKGROUND = "navigation_background";
    private static final String PREF_NAVIGATION_PROXIMITY = "navigation_proximity";
    private static final String PREF_NAVIGATION_TRAVERSE = "navigation_traverse";
    private static final Logger logger = LoggerFactory.getLogger(NavigationService.class);
    private double avvmg = 0.0d;
    private ServiceConnection locationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            NavigationService.this.mLocationService = (ILocationService) service;
            NavigationService.this.mLocationService.registerLocationCallback(NavigationService.this.locationListener);
            NavigationService.logger.debug("Location service connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            NavigationService.this.mLocationService = null;
            NavigationService.logger.debug("Location service disconnected");
        }
    };
    private ILocationListener locationListener = new ILocationListener() {
        public void onLocationChanged() {
            if (NavigationService.this.mLocationService != null) {
                NavigationService.this.mLastKnownLocation = NavigationService.this.mLocationService.getLocation();
                if (NavigationService.this.navWaypoint != null) {
                    NavigationService.this.calculateNavigationStatus(NavigationService.this.mLastKnownLocation, 0.0d, 0.0d);
                }
            }
        }

        public void onGpsStatusChanged() {
        }
    };
    private final Binder mBinder = new LocalBinder();
    private boolean mForeground = false;
    private Location mLastKnownLocation;
    private ILocationService mLocationService = null;
    private int mRouteProximity = Callback.DEFAULT_DRAG_ANIMATION_DURATION;
    private boolean mUseTraverse = true;
    public double navBearing = 0.0d;
    public double navCourse = 0.0d;
    public int navCurrentRoutePoint = -1;
    public int navDirection = 0;
    public double navDistance = 0.0d;
    public int navETE = ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
    public int navProximity = 0;
    public Route navRoute = null;
    private double navRouteDistance = -1.0d;
    public long navTurn = 0;
    public double navVMG = 0.0d;
    public MapObject navWaypoint = null;
    public double navXTK = Double.NEGATIVE_INFINITY;
    public MapObject prevWaypoint = null;
    private long tics = 0;
    private double[] vmgav = null;

    public class LocalBinder extends Binder implements INavigationService {
        public boolean isNavigating() {
            return NavigationService.this.navWaypoint != null;
        }

        public boolean isNavigatingViaRoute() {
            return NavigationService.this.navRoute != null;
        }

        public MapObject getWaypoint() {
            return NavigationService.this.navWaypoint;
        }

        public float getDistance() {
            return (float) NavigationService.this.navDistance;
        }

        public float getBearing() {
            return (float) NavigationService.this.navBearing;
        }

        public float getTurn() {
            return (float) NavigationService.this.navTurn;
        }

        public float getVmg() {
            return (float) NavigationService.this.navVMG;
        }

        public float getXtk() {
            return (float) NavigationService.this.navXTK;
        }

        public int getEte() {
            return NavigationService.this.navETE;
        }
    }

    public void onCreate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        onSharedPreferenceChanged(sharedPreferences, PREF_NAVIGATION_PROXIMITY);
        onSharedPreferenceChanged(sharedPreferences, PREF_NAVIGATION_TRAVERSE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        logger.debug("Service started");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return 2;
        }
        Object action = intent.getAction();
        Bundle extras = intent.getExtras();
        logger.debug("Command: {}", action);
        if (action.equals(BaseNavigationService.NAVIGATE_TO_POINT)) {
            MapObject mo = new MapObject(extras.getDouble("latitude"), extras.getDouble("longitude"));
            mo.name = extras.getString("name");
            mo.proximity = extras.getInt(BaseNavigationService.EXTRA_PROXIMITY);
            navigateTo(mo);
        }
        if (action.equals(BaseNavigationService.NAVIGATE_TO_OBJECT)) {
            mo = MapTrek.getMapObject(extras.getLong("id"));
            if (mo == null) {
                return 2;
            }
            navigateTo(mo);
        }
        if (action.equals(BaseNavigationService.NAVIGATE_ROUTE)) {
            int index = extras.getInt(BaseNavigationService.EXTRA_ROUTE_INDEX);
            int dir = extras.getInt(BaseNavigationService.EXTRA_ROUTE_DIRECTION, 1);
            int start = extras.getInt(BaseNavigationService.EXTRA_ROUTE_START, -1);
            if (null == null) {
                return 2;
            }
            navigateTo(null, dir);
            if (start != -1) {
                setRouteWaypoint(start);
            }
        }
        if (action.equals(BaseNavigationService.STOP_NAVIGATION) || action.equals(BaseNavigationService.PAUSE_NAVIGATION)) {
            this.mForeground = false;
            stopForeground(true);
            if (action.equals(BaseNavigationService.STOP_NAVIGATION)) {
                stopNavigation();
            }
            Configuration.setNavigationPoint(this.navWaypoint);
            stopSelf();
        }
        if (action.equals(BaseNavigationService.ENABLE_BACKGROUND_NAVIGATION)) {
            this.mForeground = true;
            startForeground(NOTIFICATION_ID, getNotification());
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putBoolean(PREF_NAVIGATION_BACKGROUND, true);
            editor.apply();
        }
        if (action.equals(BaseNavigationService.DISABLE_BACKGROUND_NAVIGATION)) {
            this.mForeground = false;
            stopForeground(true);
            editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putBoolean(PREF_NAVIGATION_BACKGROUND, false);
            editor.apply();
        }
        updateNotification();
        return 1;
    }

    public void onDestroy() {
        disconnect();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        logger.debug("Service stopped");
    }

    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREF_NAVIGATION_PROXIMITY.equals(key)) {
            this.mRouteProximity = Callback.DEFAULT_DRAG_ANIMATION_DURATION;
        } else if (PREF_NAVIGATION_TRAVERSE.equals(key)) {
            this.mUseTraverse = true;
        }
    }

    private void connect() {
        EventBus.getDefault().register(this);
        bindService(new Intent(this, LocationService.class), this.locationConnection, 1);
    }

    private void disconnect() {
        EventBus.getDefault().unregister(this);
        if (this.mLocationService != null) {
            this.mLocationService.unregisterLocationCallback(this.locationListener);
            unbindService(this.locationConnection);
            this.mLocationService = null;
        }
    }

    private Notification getNotification() {
        String title = getString(R.string.msgNavigating, new Object[]{this.navWaypoint.name});
        String bearing = StringFormatter.angleH(this.navBearing);
        String distance = StringFormatter.distanceH(this.navDistance);
        StringBuilder stringBuilder = new StringBuilder(40);
        stringBuilder.append(getString(R.string.msgNavigationProgress, new Object[]{distance, bearing}));
        String message = stringBuilder.toString();
        stringBuilder.append(". ");
        stringBuilder.append(getString(R.string.msgNavigationActions));
        stringBuilder.append(".");
        String bigText = stringBuilder.toString();
        Intent iLaunch = new Intent("android.intent.action.MAIN");
        iLaunch.addCategory("android.intent.category.LAUNCHER");
        iLaunch.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
        iLaunch.setFlags(270532608);
        PendingIntent piResult = PendingIntent.getActivity(this, 0, iLaunch, 268435456);
        PendingIntent piStop = PendingIntent.getService(this, 0, new Intent(BaseNavigationService.STOP_NAVIGATION, null, getApplicationContext(), NavigationService.class), 268435456);
        Icon stopIcon = Icon.createWithResource(this, R.drawable.ic_cancel_black);
        PendingIntent piPause = PendingIntent.getService(this, 0, new Intent(BaseNavigationService.PAUSE_NAVIGATION, null, getApplicationContext(), NavigationService.class), 268435456);
        Icon pauseIcon = Icon.createWithResource(this, R.drawable.ic_pause);
        Action actionStop = new Builder(stopIcon, getString(R.string.actionStop), piStop).build();
        Action actionPause = new Builder(pauseIcon, getString(R.string.actionPause), piPause).build();
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_stat_navigation);
        builder.setContentIntent(piResult);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setWhen(System.currentTimeMillis());
        builder.setStyle(new BigTextStyle().setBigContentTitle(title).bigText(bigText));
        builder.addAction(actionPause);
        builder.addAction(actionStop);
        builder.setGroup("maptrek");
        builder.setCategory("service");
        builder.setPriority(-1);
        builder.setVisibility(1);
        builder.setColor(getResources().getColor(R.color.colorAccent, getTheme()));
        builder.setOngoing(true);
        return builder.build();
    }

    private void updateNotification() {
        if (this.mForeground) {
            ((NotificationManager) getSystemService("notification")).notify(NOTIFICATION_ID, getNotification());
        }
    }

    public void stopNavigation() {
        logger.debug("Stop navigation");
        updateNavigationState(4);
        stopForeground(true);
        clearNavigation();
        disconnect();
    }

    private void clearNavigation() {
        this.navWaypoint = null;
        this.prevWaypoint = null;
        this.navRoute = null;
        this.navDirection = 0;
        this.navCurrentRoutePoint = -1;
        this.navProximity = this.mRouteProximity;
        this.navDistance = 0.0d;
        this.navBearing = 0.0d;
        this.navTurn = 0;
        this.navVMG = 0.0d;
        this.navETE = ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        this.navCourse = 0.0d;
        this.navXTK = Double.NEGATIVE_INFINITY;
        this.vmgav = null;
        this.avvmg = 0.0d;
    }

    private void navigateTo(MapObject waypoint) {
        if (this.navWaypoint != null) {
            stopNavigation();
        }
        connect();
        this.vmgav = new double[]{0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d};
        this.navWaypoint = waypoint;
        this.navProximity = this.navWaypoint.proximity > 0 ? this.navWaypoint.proximity : this.mRouteProximity;
        updateNavigationState(1);
        if (this.mLastKnownLocation != null) {
            calculateNavigationStatus(this.mLastKnownLocation, 0.0d, 0.0d);
        }
    }

    private void navigateTo(Route route, int direction) {
        if (this.navWaypoint != null) {
            stopNavigation();
        }
        connect();
        this.vmgav = new double[]{0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d};
        this.navRoute = route;
        this.navDirection = direction;
        this.navCurrentRoutePoint = this.navDirection == 1 ? 1 : this.navRoute.length() - 2;
        this.navWaypoint = this.navRoute.getWaypoint(this.navCurrentRoutePoint);
        this.prevWaypoint = this.navRoute.getWaypoint(this.navCurrentRoutePoint - this.navDirection);
        this.navProximity = this.navWaypoint.proximity > 0 ? this.navWaypoint.proximity : this.mRouteProximity;
        this.navRouteDistance = -1.0d;
        this.navCourse = this.prevWaypoint.coordinates.bearingTo(this.navWaypoint.coordinates);
        updateNavigationState(1);
        if (this.mLastKnownLocation != null) {
            calculateNavigationStatus(this.mLastKnownLocation, 0.0d, 0.0d);
        }
    }

    public void setRouteWaypoint(int waypoint) {
        this.navCurrentRoutePoint = waypoint;
        this.navWaypoint = this.navRoute.getWaypoint(this.navCurrentRoutePoint);
        int prev = this.navCurrentRoutePoint - this.navDirection;
        if (prev < 0 || prev >= this.navRoute.length()) {
            this.prevWaypoint = null;
        } else {
            this.prevWaypoint = this.navRoute.getWaypoint(prev);
        }
        this.navProximity = this.navWaypoint.proximity > 0 ? this.navWaypoint.proximity : this.mRouteProximity;
        this.navRouteDistance = -1.0d;
        this.navCourse = this.prevWaypoint == null ? 0.0d : this.prevWaypoint.coordinates.bearingTo(this.navWaypoint.coordinates);
        updateNavigationState(2);
    }

    public MapObject getNextRouteWaypoint() {
        try {
            return this.navRoute.getWaypoint(this.navCurrentRoutePoint + this.navDirection);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public void nextRouteWaypoint() throws IndexOutOfBoundsException {
        this.navCurrentRoutePoint += this.navDirection;
        this.navWaypoint = this.navRoute.getWaypoint(this.navCurrentRoutePoint);
        this.prevWaypoint = this.navRoute.getWaypoint(this.navCurrentRoutePoint - this.navDirection);
        this.navProximity = this.navWaypoint.proximity > 0 ? this.navWaypoint.proximity : this.mRouteProximity;
        this.navRouteDistance = -1.0d;
        this.navCourse = this.prevWaypoint.coordinates.bearingTo(this.navWaypoint.coordinates);
        updateNavigationState(2);
    }

    public void prevRouteWaypoint() throws IndexOutOfBoundsException {
        this.navCurrentRoutePoint -= this.navDirection;
        this.navWaypoint = this.navRoute.getWaypoint(this.navCurrentRoutePoint);
        int prev = this.navCurrentRoutePoint - this.navDirection;
        if (prev < 0 || prev >= this.navRoute.length()) {
            this.prevWaypoint = null;
        } else {
            this.prevWaypoint = this.navRoute.getWaypoint(prev);
        }
        this.navProximity = this.navWaypoint.proximity > 0 ? this.navWaypoint.proximity : this.mRouteProximity;
        this.navRouteDistance = -1.0d;
        this.navCourse = this.prevWaypoint == null ? 0.0d : this.prevWaypoint.coordinates.bearingTo(this.navWaypoint.coordinates);
        updateNavigationState(2);
    }

    public boolean hasNextRouteWaypoint() {
        if (this.navRoute == null) {
            return false;
        }
        boolean hasNext = false;
        if (this.navDirection == 1) {
            if (this.navCurrentRoutePoint + this.navDirection < this.navRoute.length()) {
                hasNext = true;
            } else {
                hasNext = false;
            }
        }
        if (this.navDirection == -1) {
            if (this.navCurrentRoutePoint + this.navDirection >= 0) {
                hasNext = true;
            } else {
                hasNext = false;
            }
        }
        return hasNext;
    }

    public boolean hasPrevRouteWaypoint() {
        if (this.navRoute == null) {
            return false;
        }
        boolean hasPrev = false;
        if (this.navDirection == 1) {
            if (this.navCurrentRoutePoint - this.navDirection >= 0) {
                hasPrev = true;
            } else {
                hasPrev = false;
            }
        }
        if (this.navDirection == -1) {
            if (this.navCurrentRoutePoint - this.navDirection < this.navRoute.length()) {
                hasPrev = true;
            } else {
                hasPrev = false;
            }
        }
        return hasPrev;
    }

    public int navRouteCurrentIndex() {
        return this.navDirection == 1 ? this.navCurrentRoutePoint : (this.navRoute.length() - this.navCurrentRoutePoint) - 1;
    }

    public double navRouteDistanceLeft() {
        if (this.navRouteDistance < 0.0d) {
            this.navRouteDistance = navRouteDistanceLeftTo(this.navRoute.length() - 1);
        }
        return this.navRouteDistance;
    }

    public double navRouteDistanceLeftTo(int index) {
        if (index - navRouteCurrentIndex() <= 0) {
            return 0.0d;
        }
        double distance = 0.0d;
        if (this.navDirection == 1) {
            distance = this.navRoute.distanceBetween(this.navCurrentRoutePoint, index);
        }
        if (this.navDirection == -1) {
            return this.navRoute.distanceBetween((this.navRoute.length() - index) - 1, this.navCurrentRoutePoint);
        }
        return distance;
    }

    public int navRouteWaypointETE(int index) {
        if (index == 0) {
            return 0;
        }
        if (this.avvmg <= 0.0d) {
            return ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        }
        int i = this.navDirection == 1 ? index : (this.navRoute.length() - index) - 1;
        return (int) Math.round((this.navRoute.getWaypoint(i).coordinates.vincentyDistance(this.navRoute.getWaypoint(i - this.navDirection).coordinates) / this.avvmg) / 60.0d);
    }

    public int navRouteETE(double distance) {
        if (this.avvmg > 0.0d) {
            return (int) Math.round((distance / this.avvmg) / 60.0d);
        }
        return ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
    }

    public int navRouteETETo(int index) {
        double distance = navRouteDistanceLeftTo(index);
        if (distance <= 0.0d) {
            return 0;
        }
        return navRouteETE(distance);
    }

    private void calculateNavigationStatus(Location loc, double smoothspeed, double avgspeed) {
        GeoPoint geoPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        double distance = geoPoint.vincentyDistance(this.navWaypoint.coordinates);
        double bearing = geoPoint.bearingTo(this.navWaypoint.coordinates);
        long turn = Math.round(bearing - ((double) loc.getBearing()));
        if (Math.abs(turn) > 180) {
            turn -= ((long) Math.signum((float) turn)) * 360;
        }
        double vmg = Geo.vmg(smoothspeed, (double) Math.abs(turn));
        double curavvmg = Geo.vmg(avgspeed, (double) Math.abs(turn));
        if (this.avvmg == 0.0d || this.tics % 10 == 0) {
            for (int i = this.vmgav.length - 1; i > 0; i--) {
                this.avvmg += this.vmgav[i];
                this.vmgav[i] = this.vmgav[i - 1];
            }
            this.avvmg += curavvmg;
            this.vmgav[0] = curavvmg;
            this.avvmg /= (double) this.vmgav.length;
        }
        int ete = ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        if (this.avvmg > 0.0d) {
            ete = (int) Math.round((distance / this.avvmg) / 60.0d);
        }
        double xtk = Double.NEGATIVE_INFINITY;
        if (this.navRoute != null) {
            boolean hasNext = hasNextRouteWaypoint();
            if (distance < ((double) this.navProximity)) {
                if (hasNext) {
                    nextRouteWaypoint();
                    return;
                }
                updateNavigationState(3);
                stopNavigation();
                return;
            } else if (this.prevWaypoint != null) {
                xtk = Geo.xtk(distance, this.prevWaypoint.coordinates.bearingTo(this.navWaypoint.coordinates), bearing);
                if (xtk == Double.NEGATIVE_INFINITY && this.mUseTraverse && hasNext) {
                    double cxtk2 = Double.NEGATIVE_INFINITY;
                    MapObject nextWpt = getNextRouteWaypoint();
                    if (nextWpt != null) {
                        cxtk2 = Geo.xtk(0.0d, nextWpt.coordinates.bearingTo(this.navWaypoint.coordinates), bearing);
                    }
                    if (cxtk2 != Double.NEGATIVE_INFINITY) {
                        nextRouteWaypoint();
                        return;
                    }
                }
            }
        }
        this.tics++;
        if (distance != this.navDistance || bearing != this.navBearing || turn != this.navTurn || vmg != this.navVMG || ete != this.navETE || xtk != this.navXTK) {
            this.navDistance = distance;
            this.navBearing = bearing;
            this.navTurn = turn;
            this.navVMG = vmg;
            this.navETE = ete;
            this.navXTK = xtk;
            updateNavigationStatus();
        }
    }

    private void updateNavigationState(int state) {
        if (!(state == 4 || state == 3)) {
            updateNotification();
        }
        sendBroadcast(new Intent(BaseNavigationService.BROADCAST_NAVIGATION_STATE).putExtra("state", state));
        logger.trace("State dispatched");
    }

    private void updateNavigationStatus() {
        updateNotification();
        sendBroadcast(new Intent(BaseNavigationService.BROADCAST_NAVIGATION_STATUS));
        logger.trace("Status dispatched");
    }

    @Subscribe
    public void onMapObjectUpdated(UpdatedEvent event) {
        logger.error("onMapObjectUpdated({})", Boolean.valueOf(event.mapObject.equals(this.navWaypoint)));
        if (event.mapObject.equals(this.navWaypoint)) {
            calculateNavigationStatus(this.mLastKnownLocation, 0.0d, 0.0d);
        }
    }
}
