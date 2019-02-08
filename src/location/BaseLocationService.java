package mobi.maptrek.location;

import android.app.Service;

public abstract class BaseLocationService extends Service {
    public static final String BROADCAST_TRACK_SAVE = "mobi.maptrek.location.TrackSave";
    public static final String DISABLE_BACKGROUND_TRACK = "mobi.maptrek.location.disableBackgroundTrack";
    public static final String DISABLE_TRACK = "mobi.maptrek.location.disableTrack";
    public static final String ENABLE_BACKGROUND_TRACK = "mobi.maptrek.location.enableBackgroundTrack";
    public static final String ENABLE_TRACK = "mobi.maptrek.location.enableTrack";
    public static final int GPS_OFF = 1;
    public static final int GPS_OK = 3;
    public static final int GPS_SEARCHING = 2;
    public static final int LOCATION_DELAY = 300;
    public static final String MAPTREK_LOCATION_SERVICE = "mobi.maptrek.location";
    public static final String PAUSE_TRACK = "mobi.maptrek.location.pauseTrack";
}
