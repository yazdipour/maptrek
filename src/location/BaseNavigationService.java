package mobi.maptrek.location;

import android.app.Service;

public abstract class BaseNavigationService extends Service {
    public static final String BROADCAST_NAVIGATION_STATE = "mobi.maptrek.navigationStateChanged";
    public static final String BROADCAST_NAVIGATION_STATUS = "mobi.maptrek.navigationStatusChanged";
    public static final int DIRECTION_FORWARD = 1;
    public static final int DIRECTION_REVERSE = -1;
    public static final String DISABLE_BACKGROUND_NAVIGATION = "mobi.maptrek.location.disableBackgroundNavigation";
    public static final String ENABLE_BACKGROUND_NAVIGATION = "mobi.maptrek.location.enableBackgroundNavigation";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_PROXIMITY = "proximity";
    public static final String EXTRA_ROUTE_DIRECTION = "direction";
    public static final String EXTRA_ROUTE_INDEX = "index";
    public static final String EXTRA_ROUTE_START = "start";
    public static final String NAVIGATE_ROUTE = "mobi.maptrek.location.NAVIGATE_ROUTE";
    public static final String NAVIGATE_TO_OBJECT = "mobi.maptrek.location.NAVIGATE_TO_OBJECT";
    public static final String NAVIGATE_TO_POINT = "mobi.maptrek.location.NAVIGATE_TO_POINT";
    public static final String PAUSE_NAVIGATION = "mobi.maptrek.location.pauseNavigation";
    public static final int STATE_NEXT_WPT = 2;
    public static final int STATE_REACHED = 3;
    public static final int STATE_STARTED = 1;
    public static final int STATE_STOPPED = 4;
    public static final String STOP_NAVIGATION = "mobi.maptrek.location.stopNavigation";
}
