package mobi.maptrek.data;

import java.util.Date;
import mobi.maptrek.data.source.DataSource;

public class Waypoint extends MapObject {
    public Date date;
    public boolean locked;
    public DataSource source;

    public Waypoint(double latitude, double longitude) {
        super(latitude, longitude);
    }

    public Waypoint(int latitudeE6, int longitudeE6) {
        super(latitudeE6, longitudeE6);
    }

    public Waypoint(String name, double lat, double lon) {
        super(lat, lon);
        this.name = name;
    }
}
