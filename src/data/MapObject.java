package mobi.maptrek.data;

import android.graphics.Bitmap;
import mobi.maptrek.data.style.MarkerStyle;
import org.oscim.core.GeoPoint;

public class MapObject {
    public long _id = 0;
    public int altitude = Integer.MIN_VALUE;
    private Bitmap bitmap;
    public GeoPoint coordinates;
    public String description;
    public String marker;
    public String name;
    public int proximity = 0;
    public MarkerStyle style = new MarkerStyle();
    public int textColor;

    public static class AddedEvent {
        public MapObject mapObject;

        public AddedEvent(MapObject mapObject) {
            this.mapObject = mapObject;
        }
    }

    public static class RemovedEvent {
        public MapObject mapObject;

        public RemovedEvent(MapObject mapObject) {
            this.mapObject = mapObject;
        }
    }

    public static class UpdatedEvent {
        public MapObject mapObject;

        public UpdatedEvent(MapObject mapObject) {
            this.mapObject = mapObject;
        }
    }

    public MapObject(double latitude, double longitude) {
        this.coordinates = new GeoPoint(latitude, longitude);
    }

    public MapObject(int latitudeE6, int longitudeE6) {
        this.coordinates = new GeoPoint(latitudeE6, longitudeE6);
    }

    public MapObject(String name, GeoPoint coordinates) {
        this.name = name;
        this.coordinates = coordinates;
    }

    public void setCoordinates(GeoPoint coordinates) {
        this.coordinates = coordinates;
    }

    public void setCoordinates(double latitude, double longitude) {
        setCoordinates(new GeoPoint(latitude, longitude));
    }

    public synchronized Bitmap getBitmap() {
        return this.bitmap;
    }

    public synchronized Bitmap getBitmapCopy() {
        return this.bitmap == null ? null : this.bitmap.copy(this.bitmap.getConfig(), false);
    }

    public synchronized void setBitmap(Bitmap bitmap) {
        if (this.bitmap != null) {
            this.bitmap.recycle();
        }
        this.bitmap = bitmap;
    }

    public boolean equals(Object o) {
        return (this._id != 0 && (o instanceof MapObject) && this._id == ((MapObject) o)._id) || super.equals(o);
    }
}
