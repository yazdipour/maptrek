package mobi.maptrek;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.oscim.core.GeoPoint;
import org.oscim.map.Map;

public interface MapHolder {
    void addLocationChangeListener(LocationChangeListener locationChangeListener);

    void addLocationStateChangeListener(LocationStateChangeListener locationStateChangeListener);

    void disableLocations();

    void disableTracking();

    Map getMap();

    boolean isNavigatingTo(@NonNull GeoPoint geoPoint);

    void navigateTo(@NonNull GeoPoint geoPoint, @Nullable String str);

    void removeLocationChangeListener(LocationChangeListener locationChangeListener);

    void removeLocationStateChangeListener(LocationStateChangeListener locationStateChangeListener);

    void removeMarker();

    void setMapLocation(@NonNull GeoPoint geoPoint);

    void shareLocation(@NonNull GeoPoint geoPoint, @Nullable String str);

    void showMarker(@NonNull GeoPoint geoPoint, @Nullable String str);

    void stopNavigation();

    void updateMapViewArea();
}
