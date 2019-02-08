package mobi.maptrek.layers.marker;

import org.oscim.core.GeoPoint;

public class MarkerItem {
    public static final int ITEM_STATE_FOCUSED_MASK = 4;
    public static final int ITEM_STATE_PRESSED_MASK = 1;
    public static final int ITEM_STATE_SELECTED_MASK = 2;
    public String description;
    public GeoPoint geoPoint;
    protected MarkerSymbol mMarker;
    public String title;
    public Object uid;

    public enum HotspotPlace {
        NONE,
        CENTER,
        BOTTOM_CENTER,
        TOP_CENTER,
        RIGHT_CENTER,
        LEFT_CENTER,
        UPPER_RIGHT_CORNER,
        LOWER_RIGHT_CORNER,
        UPPER_LEFT_CORNER,
        LOWER_LEFT_CORNER
    }

    public MarkerItem(String title, String description, GeoPoint geoPoint) {
        this(null, title, description, geoPoint);
    }

    public MarkerItem(Object uid, String title, String description, GeoPoint geoPoint) {
        this.title = title;
        this.description = description;
        this.geoPoint = geoPoint;
        this.uid = uid;
    }

    public Object getUid() {
        return this.uid;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSnippet() {
        return this.description;
    }

    public GeoPoint getPoint() {
        return this.geoPoint;
    }

    public void setPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public MarkerSymbol getMarker() {
        return this.mMarker;
    }

    public void setMarker(MarkerSymbol marker) {
        this.mMarker = marker;
    }
}
