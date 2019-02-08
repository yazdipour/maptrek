package mobi.maptrek.data;

import java.util.ArrayList;
import java.util.List;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.style.TrackStyle;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;

public class Track {
    public String description;
    public int id;
    private BoundingBox mBox;
    private float mDistance;
    private TrackPoint mLastTrackPoint;
    public String name;
    public final List<TrackPoint> points;
    public boolean show;
    public DataSource source;
    public TrackStyle style;

    public class TrackPoint extends GeoPoint {
        public final float accuracy;
        public final float bearing;
        public final boolean continuous;
        public final float elevation;
        public final float speed;
        public final long time;

        public TrackPoint(boolean cont, int latE6, int lonE6, float elev, float spd, float brn, float acc, long t) {
            super(latE6, lonE6);
            this.continuous = cont;
            this.elevation = elev;
            this.speed = spd;
            this.bearing = brn;
            this.accuracy = acc;
            this.time = t;
        }
    }

    public BoundingBox getBoundingBox() {
        if (this.mBox == null) {
            this.mBox = new BoundingBox();
            synchronized (this.points) {
                for (TrackPoint point : this.points) {
                    this.mBox.extend(point.latitudeE6, point.longitudeE6);
                }
            }
        }
        return this.mBox;
    }

    public Track() {
        this("", false);
    }

    public Track(String name, boolean show) {
        this.style = new TrackStyle();
        this.points = new ArrayList();
        this.mDistance = Float.NaN;
        this.mBox = null;
        this.name = name;
        this.show = show;
    }

    public synchronized void copyFrom(Track track) {
        this.points.clear();
        this.points.addAll(track.points);
        this.mLastTrackPoint = track.getLastPoint();
        this.name = track.name;
        this.description = track.description;
        track.style.copy(this.style);
        this.mDistance = track.mDistance;
    }

    public void addPoint(boolean continuous, int latE6, int lonE6, float elev, float speed, float bearing, float accuracy, long time) {
        TrackPoint previous = this.mLastTrackPoint;
        if (this.mLastTrackPoint == null) {
            this.mDistance = 0.0f;
        }
        this.mLastTrackPoint = new TrackPoint(continuous, latE6, lonE6, elev, speed, bearing, accuracy, time);
        if (previous != null) {
            this.mDistance = (float) (((double) this.mDistance) + previous.vincentyDistance(this.mLastTrackPoint));
        }
        synchronized (this.points) {
            this.points.add(this.mLastTrackPoint);
        }
    }

    public void addPointFast(boolean continuous, int latE6, int lonE6, float elev, float speed, float bearing, float accuracy, long time) {
        this.mLastTrackPoint = new TrackPoint(continuous, latE6, lonE6, elev, speed, bearing, accuracy, time);
        synchronized (this.points) {
            this.points.add(this.mLastTrackPoint);
        }
    }

    public float getDistance() {
        if (Float.isNaN(this.mDistance)) {
            this.mDistance = 0.0f;
            synchronized (this.points) {
                TrackPoint previous = this.mLastTrackPoint;
                if (this.points.size() > 1) {
                    for (int i = this.points.size() - 2; i >= 0; i--) {
                        TrackPoint current = (TrackPoint) this.points.get(i);
                        this.mDistance = (float) (((double) this.mDistance) + previous.vincentyDistance(current));
                        previous = current;
                    }
                }
            }
        }
        return this.mDistance;
    }

    public synchronized void clear() {
        this.points.clear();
        this.mLastTrackPoint = null;
        this.mDistance = Float.NaN;
    }

    public TrackPoint getLastPoint() {
        return this.mLastTrackPoint;
    }
}
