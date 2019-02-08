package mobi.maptrek;

import android.app.Fragment;
import android.os.Bundle;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.WaypointDbDataSource;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;
import mobi.maptrek.util.OsmcSymbolFactory;
import mobi.maptrek.util.ShieldFactory;

public class DataFragment extends Fragment {
    private MapFile mBitmapLayerMap;
    private Waypoint mEditedWaypoint;
    private MapIndex mMapIndex;
    private OsmcSymbolFactory mOsmcSymbolFactory;
    private ShieldFactory mShieldFactory;
    private WaypointDbDataSource mWaypointDbDataSource;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public MapIndex getMapIndex() {
        return this.mMapIndex;
    }

    public void setMapIndex(MapIndex mapIndex) {
        this.mMapIndex = mapIndex;
    }

    public Waypoint getEditedWaypoint() {
        return this.mEditedWaypoint;
    }

    public void setEditedWaypoint(Waypoint waypoint) {
        this.mEditedWaypoint = waypoint;
    }

    public WaypointDbDataSource getWaypointDbDataSource() {
        return this.mWaypointDbDataSource;
    }

    public void setWaypointDbDataSource(WaypointDbDataSource waypointDbDataSource) {
        this.mWaypointDbDataSource = waypointDbDataSource;
    }

    public MapFile getBitmapLayerMap() {
        return this.mBitmapLayerMap;
    }

    public void setBitmapLayerMap(MapFile bitmapLayerMap) {
        this.mBitmapLayerMap = bitmapLayerMap;
    }

    public ShieldFactory getShieldFactory() {
        return this.mShieldFactory;
    }

    public void setShieldFactory(ShieldFactory shieldFactory) {
        this.mShieldFactory = shieldFactory;
    }

    public OsmcSymbolFactory getOsmcSymbolFactory() {
        return this.mOsmcSymbolFactory;
    }

    public void setOsmcSymbolFactory(OsmcSymbolFactory factory) {
        this.mOsmcSymbolFactory = factory;
    }
}
