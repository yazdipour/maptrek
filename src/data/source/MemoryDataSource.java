package mobi.maptrek.data.source;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.DataSource.DataType;
import mobi.maptrek.location.BaseNavigationService;

public class MemoryDataSource extends DataSource implements WaypointDataSource, TrackDataSource {
    @NonNull
    public List<Track> tracks = new ArrayList();
    @NonNull
    public List<Waypoint> waypoints = new ArrayList();

    public class DataCursor extends AbstractCursor {
        public int getCount() {
            return MemoryDataSource.this.waypoints.size() + MemoryDataSource.this.tracks.size();
        }

        public String[] getColumnNames() {
            return new String[]{"_id", BaseNavigationService.EXTRA_ROUTE_INDEX};
        }

        public String getString(int column) {
            throw new RuntimeException("Not implemented");
        }

        public short getShort(int column) {
            throw new RuntimeException("Not implemented");
        }

        public int getInt(int column) {
            checkPosition();
            if (column != 1) {
                return 0;
            }
            int position = getPosition();
            return position >= MemoryDataSource.this.waypoints.size() ? position - MemoryDataSource.this.waypoints.size() : position;
        }

        public long getLong(int column) {
            checkPosition();
            if (column != 0) {
                return 0;
            }
            int position = getPosition();
            if (position < MemoryDataSource.this.waypoints.size()) {
                return ((Waypoint) MemoryDataSource.this.waypoints.get(position))._id;
            }
            return (long) ((Track) MemoryDataSource.this.tracks.get(position - MemoryDataSource.this.waypoints.size())).id;
        }

        public float getFloat(int column) {
            throw new RuntimeException("Not implemented");
        }

        public double getDouble(int column) {
            throw new RuntimeException("Not implemented");
        }

        public boolean isNull(int column) {
            return column == 0;
        }
    }

    public boolean isNativeTrack() {
        return false;
    }

    @NonNull
    public List<Waypoint> getWaypoints() {
        return this.waypoints;
    }

    public int getWaypointsCount() {
        return this.waypoints.size();
    }

    public Cursor getCursor() {
        return new DataCursor();
    }

    @DataType
    public int getDataType(int position) {
        if (position < 0) {
            throw new IndexOutOfBoundsException("Wrong index: " + position);
        } else if (position < this.waypoints.size()) {
            return 0;
        } else {
            if (position < this.waypoints.size() + this.tracks.size()) {
                return 1;
            }
            throw new IndexOutOfBoundsException("Wrong index: " + position);
        }
    }

    public Waypoint cursorToWaypoint(Cursor cursor) {
        return (Waypoint) this.waypoints.get(cursor.getInt(1));
    }

    public Track cursorToTrack(Cursor cursor) {
        return (Track) this.tracks.get(cursor.getInt(1));
    }

    @NonNull
    public List<Track> getTracks() {
        return this.tracks;
    }

    public int getTracksCount() {
        return this.tracks.size();
    }
}
