package mobi.maptrek;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import mobi.maptrek.data.MapObject;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.maptrek.Tags;
import mobi.maptrek.view.GaugePanel;
import org.greenrobot.eventbus.EventBus;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.map.Viewport;

public class Configuration {
    static final /* synthetic */ boolean $assertionsDisabled = (!Configuration.class.desiredAssertionStatus());
    public static final long ADVICE_ACTIVE_MAPS_SIZE = 32768;
    public static final long ADVICE_ADDING_PLACE = 64;
    public static final long ADVICE_ENABLE_LOCATIONS = 16;
    public static final long ADVICE_HIDE_MAP_OBJECTS = 4096;
    public static final long ADVICE_LOCKED_COORDINATES = 2048;
    public static final long ADVICE_LOCK_MAP_POSITION = 8192;
    public static final long ADVICE_MAP_SETTINGS = 32;
    public static final long ADVICE_MORE_GAUGES = 4;
    public static final long ADVICE_RECORDED_TRACKS = 256;
    public static final long ADVICE_RECORD_TRACK = 128;
    public static final long ADVICE_REMEMBER_SCALE = 8;
    public static final long ADVICE_SUNRISE_SUNSET = 2;
    public static final long ADVICE_SWITCH_COORDINATES_FORMAT = 1024;
    public static final long ADVICE_TEXT_SEARCH = 16384;
    public static final long ADVICE_UPDATE_EXTERNAL_SOURCE = 1;
    public static final long ADVICE_VIEW_DATA_ITEM = 512;
    private static final String LAST_SEEN_INTRODUCTION = "last_seen_introduction";
    private static final String PREF_ACTION_PANEL_STATE = "action_panel_state";
    private static final String PREF_ACTION_RATING = "action_rating";
    private static final String PREF_ACTIVITY = "activity";
    private static final String PREF_ADVICE_STATES = "advice_states";
    public static final String PREF_ANGLE_UNIT = "angle_unit";
    private static final String PREF_AUTO_TILT = "auto_tilt";
    private static final String PREF_BITMAP_MAP = "bitmap_map";
    private static final String PREF_BITMAP_MAP_TRANSPARENCY = "bitmap_map_transparency";
    private static final String PREF_COORDINATES_FORMAT = "coordinates_format";
    public static final String PREF_DISTANCE_UNIT = "distance_unit";
    public static final String PREF_ELEVATION_UNIT = "elevation_unit";
    private static final String PREF_EXCEPTION_SIZE = "exception_size";
    private static final String PREF_FULLSCREEN_TIMES = "fullscreen_times";
    private static final String PREF_GAUGES = "gauges";
    private static final String PREF_HIDE_MAP_OBJECTS = "hide_map_objects";
    private static final String PREF_HIDE_SYSTEM_UI = "hide_system_ui";
    public static final String PREF_HILLSHADES_TRANSPARENCY = "hillshades_transparency";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_LATITUDE = "latitude";
    private static final String PREF_LOCATION_STATE = "location_state";
    private static final String PREF_LONGITUDE = "longitude";
    private static final String PREF_MAP_3D_BUILDINGS = "map_3d_buildings";
    private static final String PREF_MAP_BEARING = "map_bearing";
    private static final String PREF_MAP_CONTOURS = "map_contours";
    private static final String PREF_MAP_FONT_SIZE = "map_font_size";
    private static final String PREF_MAP_GRID = "map_grid";
    public static final String PREF_MAP_HILLSHADES = "map_hillshades";
    private static final String PREF_MAP_SCALE = "map_scale";
    private static final String PREF_MAP_STYLE = "map_style";
    private static final String PREF_MAP_TILT = "map_tilt";
    private static final String PREF_NAVIGATION_LATITUDE = "navigation_waypoint_latitude";
    private static final String PREF_NAVIGATION_LONGITUDE = "navigation_waypoint_longitude";
    private static final String PREF_NAVIGATION_PROXIMITY = "navigation_waypoint_proximity";
    private static final String PREF_NAVIGATION_WAYPOINT = "navigation_waypoint";
    private static final String PREF_NIGHT_MODE_STATE = "night_mode_state";
    private static final String PREF_POINTS_COUNTER = "wpt_counter";
    private static final String PREF_PREVIOUS_LOCATION_STATE = "previous_location_state";
    private static final String PREF_REMEMBERED_SCALE = "remembered_scale";
    private static final String PREF_RUNNING_TIME = "running_time";
    public static final String PREF_SPEED_UNIT = "speed_unit";
    private static final String PREF_TRACKING_STATE = "tracking_state";
    private static final String PREF_TRACKING_TIME = "tracking_time";
    private static final String PREF_UID = "uid";
    public static final String PREF_UNIT_PRECISION = "unit_precision";
    private static SharedPreferences mSharedPreferences;

    public static class ChangedEvent {
        public String key;

        public ChangedEvent(String key) {
            this.key = key;
        }
    }

    public static void initialize(SharedPreferences sharedPreferences) {
        mSharedPreferences = sharedPreferences;
    }

    public static int getPointsCounter() {
        int counter = loadInt(PREF_POINTS_COUNTER, 0) + 1;
        saveInt(PREF_POINTS_COUNTER, counter);
        return counter;
    }

    public static long getUID() {
        long uid = loadLong(PREF_UID, 0) + 1;
        saveLong(PREF_UID, uid);
        return uid;
    }

    public static int getLocationState() {
        return loadInt(PREF_LOCATION_STATE, 0);
    }

    public static void setLocationState(int locationState) {
        saveInt(PREF_LOCATION_STATE, locationState);
    }

    public static int getPreviousLocationState() {
        return loadInt(PREF_PREVIOUS_LOCATION_STATE, 0);
    }

    public static void setPreviousLocationState(int locationState) {
        saveInt(PREF_PREVIOUS_LOCATION_STATE, locationState);
    }

    public static int getTrackingState() {
        return loadInt(PREF_TRACKING_STATE, 0);
    }

    public static void setTrackingState(int trackingState) {
        saveInt(PREF_TRACKING_STATE, trackingState);
    }

    public static boolean getActionPanelState() {
        return loadBoolean(PREF_ACTION_PANEL_STATE, true);
    }

    public static void setActionPanelState(boolean panelState) {
        saveBoolean(PREF_ACTION_PANEL_STATE, panelState);
    }

    @Nullable
    public static MapObject getNavigationPoint() {
        String navWpt = loadString(PREF_NAVIGATION_WAYPOINT, null);
        if (navWpt == null) {
            return null;
        }
        MapObject waypoint = new MapObject((double) mSharedPreferences.getFloat(PREF_NAVIGATION_LATITUDE, 0.0f), (double) mSharedPreferences.getFloat(PREF_NAVIGATION_LONGITUDE, 0.0f));
        waypoint.name = navWpt;
        waypoint.proximity = loadInt(PREF_NAVIGATION_PROXIMITY, 0);
        saveString(PREF_NAVIGATION_WAYPOINT, null);
        return waypoint;
    }

    public static void setNavigationPoint(@Nullable MapObject mapObject) {
        if (mapObject != null) {
            saveString(PREF_NAVIGATION_WAYPOINT, mapObject.name);
            saveFloat(PREF_NAVIGATION_LATITUDE, (float) mapObject.coordinates.getLatitude());
            saveFloat(PREF_NAVIGATION_LONGITUDE, (float) mapObject.coordinates.getLongitude());
            saveInt(PREF_NAVIGATION_PROXIMITY, mapObject.proximity);
            return;
        }
        saveString(PREF_NAVIGATION_WAYPOINT, null);
    }

    public static String getGauges() {
        return loadString(PREF_GAUGES, GaugePanel.DEFAULT_GAUGE_SET);
    }

    public static void setGauges(String gauges) {
        saveString(PREF_GAUGES, gauges);
    }

    @NonNull
    public static MapPosition getPosition() {
        MapPosition mapPosition = new MapPosition();
        int latitudeE6 = loadInt("latitude", 0);
        int longitudeE6 = loadInt("longitude", 0);
        float scale = loadFloat(PREF_MAP_SCALE, Viewport.VIEW_NEAR);
        float bearing = loadFloat(PREF_MAP_BEARING, 0.0f);
        float tilt = loadFloat(PREF_MAP_TILT, 0.0f);
        mapPosition.setPosition(((double) latitudeE6) / 1000000.0d, ((double) longitudeE6) / 1000000.0d);
        mapPosition.setScale((double) scale);
        mapPosition.setBearing(bearing);
        mapPosition.setTilt(tilt);
        return mapPosition;
    }

    public static void setPosition(@NonNull MapPosition mapPosition) {
        GeoPoint geoPoint = mapPosition.getGeoPoint();
        saveInt("latitude", geoPoint.latitudeE6);
        saveInt("longitude", geoPoint.longitudeE6);
        saveFloat(PREF_MAP_SCALE, (float) mapPosition.scale);
        saveFloat(PREF_MAP_BEARING, mapPosition.bearing);
        saveFloat(PREF_MAP_TILT, mapPosition.tilt);
    }

    public static boolean getBuildingsLayerEnabled() {
        return loadBoolean(PREF_MAP_3D_BUILDINGS, true);
    }

    public static void setBuildingsLayerEnabled(boolean buildingsLayerEnabled) {
        saveBoolean(PREF_MAP_3D_BUILDINGS, buildingsLayerEnabled);
    }

    public static boolean getContoursEnabled() {
        return loadBoolean(PREF_MAP_CONTOURS, true);
    }

    public static void setContoursEnabled(boolean contoursEnabled) {
        saveBoolean(PREF_MAP_CONTOURS, contoursEnabled);
    }

    public static boolean getHillshadesEnabled() {
        return loadBoolean(PREF_MAP_HILLSHADES, false);
    }

    public static void setHillshadesEnabled(boolean hillshadesEnabled) {
        saveBoolean(PREF_MAP_HILLSHADES, hillshadesEnabled);
    }

    public static int getHillshadesTransparency() {
        return loadInt(PREF_HILLSHADES_TRANSPARENCY, 50);
    }

    public static boolean getGridLayerEnabled() {
        return loadBoolean(PREF_MAP_GRID, false);
    }

    public static void setGridLayerEnabled(boolean GRIDLayerEnabled) {
        saveBoolean(PREF_MAP_GRID, GRIDLayerEnabled);
    }

    @Nullable
    public static String getBitmapMap() {
        return loadString(PREF_BITMAP_MAP, null);
    }

    public static void setBitmapMap(@Nullable MapFile mapFile) {
        if (mapFile != null) {
            saveString(PREF_BITMAP_MAP, mapFile.tileSource.getOption("path"));
        } else {
            saveString(PREF_BITMAP_MAP, null);
        }
    }

    public static boolean getAdviceState(long advice) {
        return (loadLong(PREF_ADVICE_STATES, 0) & advice) == 0;
    }

    public static void setAdviceState(long advice) {
        saveLong(PREF_ADVICE_STATES, loadLong(PREF_ADVICE_STATES, 0) | advice);
    }

    public static void resetAdviceState() {
        saveLong(PREF_ADVICE_STATES, 0);
    }

    public static int getNightModeState() {
        return loadInt(PREF_NIGHT_MODE_STATE, 0);
    }

    public static void setNightModeState(int nightModeState) {
        saveInt(PREF_NIGHT_MODE_STATE, nightModeState);
    }

    public static int getMapStyle() {
        return loadInt(PREF_MAP_STYLE, 2);
    }

    public static void setMapStyle(int style) {
        saveInt(PREF_MAP_STYLE, style);
    }

    public static int getActivity() {
        return loadInt(PREF_ACTIVITY, 0);
    }

    public static void setActivity(int activity) {
        saveInt(PREF_ACTIVITY, activity);
    }

    public static int getMapFontSize() {
        return loadInt(PREF_MAP_FONT_SIZE, 2);
    }

    public static void setMapFontSize(int mapFontSize) {
        saveInt(PREF_MAP_FONT_SIZE, mapFontSize);
    }

    public static String getLanguage() {
        return loadString(PREF_LANGUAGE, null);
    }

    public static void setLanguage(String language) {
        saveString(PREF_LANGUAGE, language);
    }

    public static boolean getHideMapObjects() {
        return loadBoolean(PREF_HIDE_MAP_OBJECTS, true);
    }

    public static void setHideMapObjects(boolean hideMapObjects) {
        saveBoolean(PREF_HIDE_MAP_OBJECTS, hideMapObjects);
    }

    public static int getBitmapMapTransparency() {
        return loadInt(PREF_BITMAP_MAP_TRANSPARENCY, 0);
    }

    public static void setBitmapMapTransparency(int transparency) {
        saveInt(PREF_BITMAP_MAP_TRANSPARENCY, transparency);
    }

    public static long getExceptionSize() {
        return loadLong(PREF_EXCEPTION_SIZE, 0);
    }

    public static void setExceptionSize(long size) {
        saveLong(PREF_EXCEPTION_SIZE, size);
    }

    public static int getSpeedUnit() {
        return Integer.parseInt(loadString(PREF_SPEED_UNIT, "0"));
    }

    public static int getDistanceUnit() {
        return Integer.parseInt(loadString(PREF_DISTANCE_UNIT, "0"));
    }

    public static int getElevationUnit() {
        return Integer.parseInt(loadString(PREF_ELEVATION_UNIT, "0"));
    }

    public static int getAngleUnit() {
        return Integer.parseInt(loadString(PREF_ANGLE_UNIT, "0"));
    }

    public static boolean getUnitPrecision() {
        return loadBoolean(PREF_UNIT_PRECISION, false);
    }

    public static int getCoordinatesFormat() {
        return loadInt(PREF_COORDINATES_FORMAT, 0);
    }

    public static void setCoordinatesFormat(int format) {
        saveInt(PREF_COORDINATES_FORMAT, format);
    }

    public static float getRememberedScale() {
        return loadFloat(PREF_REMEMBERED_SCALE, 32763.0f);
    }

    public static void setRememberedScale(float scale) {
        saveFloat(PREF_REMEMBERED_SCALE, scale);
    }

    public static float getAutoTilt() {
        return loadFloat(PREF_AUTO_TILT, Viewport.MAX_TILT);
    }

    public static void setAutoTilt(float tilt) {
        saveFloat(PREF_AUTO_TILT, tilt);
    }

    public static boolean getHideSystemUI() {
        return loadBoolean(PREF_HIDE_SYSTEM_UI, false);
    }

    public static void setHideSystemUI(boolean hide) {
        saveBoolean(PREF_HIDE_SYSTEM_UI, hide);
    }

    public static boolean ratingActionPerformed() {
        return loadBoolean(PREF_ACTION_RATING, false);
    }

    public static void setRatingActionPerformed() {
        saveBoolean(PREF_ACTION_RATING, true);
    }

    public static int getLastSeenIntroduction() {
        return loadInt(LAST_SEEN_INTRODUCTION, 0);
    }

    public static void setLastSeenIntroduction(int last) {
        saveInt(LAST_SEEN_INTRODUCTION, last);
    }

    public static long getRunningTime() {
        return loadLong(PREF_RUNNING_TIME, 0);
    }

    public static void updateRunningTime(long time) {
        saveLong(PREF_RUNNING_TIME, getRunningTime() + time);
    }

    public static long getTrackingTime() {
        return loadLong(PREF_TRACKING_TIME, 0);
    }

    public static void updateTrackingTime(long time) {
        saveLong(PREF_TRACKING_TIME, getTrackingTime() + time);
    }

    public static int getFullScreenTimes() {
        return loadInt(PREF_FULLSCREEN_TIMES, 0);
    }

    public static void accountFullScreen() {
        saveInt(PREF_FULLSCREEN_TIMES, getFullScreenTimes() + 1);
    }

    private static int loadInt(String key, int defValue) {
        if ($assertionsDisabled || mSharedPreferences != null) {
            return mSharedPreferences.getInt(key, defValue);
        }
        throw new AssertionError("Configuration not initialized");
    }

    private static void saveInt(String key, int value) {
        if ($assertionsDisabled || mSharedPreferences != null) {
            Editor editor = mSharedPreferences.edit();
            editor.putInt(key, value);
            editor.apply();
            EventBus.getDefault().post(new ChangedEvent(key));
            return;
        }
        throw new AssertionError("Configuration not initialized");
    }

    private static long loadLong(String key, long defValue) {
        if ($assertionsDisabled || mSharedPreferences != null) {
            return mSharedPreferences.getLong(key, defValue);
        }
        throw new AssertionError("Configuration not initialized");
    }

    private static void saveLong(String key, long value) {
        if ($assertionsDisabled || mSharedPreferences != null) {
            Editor editor = mSharedPreferences.edit();
            editor.putLong(key, value);
            editor.apply();
            EventBus.getDefault().post(new ChangedEvent(key));
            return;
        }
        throw new AssertionError("Configuration not initialized");
    }

    private static float loadFloat(String key, float defValue) {
        if ($assertionsDisabled || mSharedPreferences != null) {
            return mSharedPreferences.getFloat(key, defValue);
        }
        throw new AssertionError("Configuration not initialized");
    }

    private static void saveFloat(String key, float value) {
        if ($assertionsDisabled || mSharedPreferences != null) {
            Editor editor = mSharedPreferences.edit();
            editor.putFloat(key, value);
            editor.apply();
            EventBus.getDefault().post(new ChangedEvent(key));
            return;
        }
        throw new AssertionError("Configuration not initialized");
    }

    private static boolean loadBoolean(String key, boolean defValue) {
        if ($assertionsDisabled || mSharedPreferences != null) {
            return mSharedPreferences.getBoolean(key, defValue);
        }
        throw new AssertionError("Configuration not initialized");
    }

    private static void saveBoolean(String key, boolean value) {
        if ($assertionsDisabled || mSharedPreferences != null) {
            Editor editor = mSharedPreferences.edit();
            editor.putBoolean(key, value);
            editor.apply();
            EventBus.getDefault().post(new ChangedEvent(key));
            return;
        }
        throw new AssertionError("Configuration not initialized");
    }

    private static String loadString(String key, String defValue) {
        if ($assertionsDisabled || mSharedPreferences != null) {
            return mSharedPreferences.getString(key, defValue);
        }
        throw new AssertionError("Configuration not initialized");
    }

    private static void saveString(String key, String value) {
        if ($assertionsDisabled || mSharedPreferences != null) {
            Editor editor = mSharedPreferences.edit();
            editor.putString(key, value);
            editor.apply();
            EventBus.getDefault().post(new ChangedEvent(key));
            return;
        }
        throw new AssertionError("Configuration not initialized");
    }

    public static void loadKindZoomState() {
        if ($assertionsDisabled || mSharedPreferences != null) {
            for (int i = 0; i < Tags.kinds.length; i++) {
                Tags.kindZooms[i] = mSharedPreferences.getInt(Tags.kinds[i], Tags.kindZooms[i]);
            }
            return;
        }
        throw new AssertionError("Configuration not initialized");
    }

    public static void saveKindZoomState() {
        if ($assertionsDisabled || mSharedPreferences != null) {
            Editor editor = mSharedPreferences.edit();
            for (int i = 0; i < Tags.kinds.length; i++) {
                editor.putInt(Tags.kinds[i], Tags.kindZooms[i]);
                EventBus.getDefault().post(new ChangedEvent(Tags.kinds[i]));
            }
            editor.apply();
            return;
        }
        throw new AssertionError("Configuration not initialized");
    }
}
