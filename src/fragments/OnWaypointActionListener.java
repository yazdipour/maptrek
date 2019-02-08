package mobi.maptrek.fragments;

import java.util.Set;
import mobi.maptrek.data.Waypoint;
import org.oscim.core.GeoPoint;

public interface OnWaypointActionListener {
    void onWaypointCreate(GeoPoint geoPoint, String str, boolean z, boolean z2);

    void onWaypointDelete(Waypoint waypoint);

    void onWaypointDetails(Waypoint waypoint, boolean z);

    void onWaypointFocus(Waypoint waypoint);

    void onWaypointNavigate(Waypoint waypoint);

    void onWaypointSave(Waypoint waypoint);

    void onWaypointShare(Waypoint waypoint);

    void onWaypointView(Waypoint waypoint);

    void onWaypointsDelete(Set<Waypoint> set);
}
