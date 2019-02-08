package mobi.maptrek.location;

import mobi.maptrek.data.MapObject;

public interface INavigationService {
    float getBearing();

    float getDistance();

    int getEte();

    float getTurn();

    float getVmg();

    MapObject getWaypoint();

    float getXtk();

    boolean isNavigating();

    boolean isNavigatingViaRoute();
}
