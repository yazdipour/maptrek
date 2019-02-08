package mobi.maptrek.data.source;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import mobi.maptrek.R;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.io.kml.KmlFile;
import mobi.maptrek.location.BaseNavigationService;

public class WaypointDbDataSource extends DataSource implements WaypointDataSource {
    public static final String BROADCAST_WAYPOINTS_MODIFIED = "mobi.maptrek.event.WaypointsModified";
    public static final String BROADCAST_WAYPOINTS_RESTORED = "mobi.maptrek.event.WaypointsRestored";
    public static final String BROADCAST_WAYPOINTS_REWRITTEN = "mobi.maptrek.event.WaypointsRewritten";
    private static final Intent mBroadcastIntent = new Intent().setAction(BROADCAST_WAYPOINTS_MODIFIED).addFlags(32);
    private String[] mAllColumns = new String[]{"_id", "name", "latitude", "longitude", "altitude", BaseNavigationService.EXTRA_PROXIMITY, KmlFile.TAG_DESCRIPTION, "date", "color", "icon", "locked"};
    private Context mContext;
    private SQLiteDatabase mDatabase;
    private WaypointDbHelper mDbHelper;

    public WaypointDbDataSource(Context context, File file) {
        this.mContext = context;
        this.mDbHelper = new WaypointDbHelper(context, file);
        this.name = context.getString(R.string.waypointStoreName);
    }

    public void open() throws SQLException {
        this.mDatabase = this.mDbHelper.getWritableDatabase();
        setLoaded();
    }

    public void close() {
        this.mDbHelper.close();
    }

    public boolean isOpen() {
        return this.mDatabase != null && this.mDatabase.isOpen();
    }

    public void saveWaypoint(Waypoint waypoint) {
        int i;
        ContentValues values = new ContentValues();
        if (waypoint._id > 0) {
            values.put("_id", Long.valueOf(waypoint._id));
        }
        values.put("name", waypoint.name);
        values.put("latitude", Integer.valueOf(waypoint.coordinates.latitudeE6));
        values.put("longitude", Integer.valueOf(waypoint.coordinates.longitudeE6));
        if (waypoint.altitude != Integer.MIN_VALUE) {
            values.put("altitude", Integer.valueOf(waypoint.altitude));
        }
        if (waypoint.proximity != 0) {
            values.put(BaseNavigationService.EXTRA_PROXIMITY, Integer.valueOf(waypoint.proximity));
        }
        if (waypoint.description != null) {
            values.put(KmlFile.TAG_DESCRIPTION, waypoint.description);
        }
        if (waypoint.date != null) {
            values.put("date", Long.valueOf(waypoint.date.getTime()));
        }
        values.put("color", Integer.valueOf(waypoint.style.color));
        if (waypoint.style.icon != null) {
            values.put("icon", waypoint.style.icon);
        }
        String str = "locked";
        if (waypoint.locked) {
            i = 1;
        } else {
            i = 0;
        }
        values.put(str, Integer.valueOf(i));
        int id = (int) this.mDatabase.insertWithOnConflict("waypoint", null, values, 4);
        if (id == -1) {
            this.mDatabase.update("waypoint", values, "_id=?", new String[]{String.valueOf(waypoint._id)});
        } else {
            waypoint._id = (long) id;
            waypoint.source = this;
        }
        notifyListeners();
    }

    public void deleteWaypoint(Waypoint waypoint) {
        this.mDatabase.delete("waypoint", "_id = " + waypoint._id, null);
        notifyListeners();
    }

    public List<Waypoint> getWaypoints() {
        List<Waypoint> waypoints = new ArrayList();
        Cursor cursor = getCursor();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            waypoints.add(cursorToWaypoint(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        return waypoints;
    }

    public int getWaypointsCount() {
        return getCursor().getCount();
    }

    public Cursor getCursor() {
        return this.mDatabase.query("waypoint", this.mAllColumns, null, null, null, null, "name");
    }

    public int getDataType(int position) {
        return 0;
    }

    public void notifyListeners() {
        super.notifyListeners();
        this.mContext.sendBroadcast(mBroadcastIntent);
    }

    public Waypoint cursorToWaypoint(Cursor cursor) {
        Waypoint waypoint = new Waypoint(cursor.getInt(cursor.getColumnIndex("latitude")), cursor.getInt(cursor.getColumnIndex("longitude")));
        waypoint._id = cursor.getLong(cursor.getColumnIndex("_id"));
        waypoint.name = cursor.getString(cursor.getColumnIndex("name"));
        if (!cursor.isNull(cursor.getColumnIndex("altitude"))) {
            waypoint.altitude = cursor.getInt(cursor.getColumnIndex("altitude"));
        }
        if (!cursor.isNull(cursor.getColumnIndex(BaseNavigationService.EXTRA_PROXIMITY))) {
            waypoint.proximity = cursor.getInt(cursor.getColumnIndex(BaseNavigationService.EXTRA_PROXIMITY));
        }
        if (!cursor.isNull(cursor.getColumnIndex(KmlFile.TAG_DESCRIPTION))) {
            waypoint.description = cursor.getString(cursor.getColumnIndex(KmlFile.TAG_DESCRIPTION));
        }
        if (!cursor.isNull(cursor.getColumnIndex("date"))) {
            waypoint.date = new Date(cursor.getLong(cursor.getColumnIndex("date")));
        }
        waypoint.style.color = cursor.getInt(cursor.getColumnIndex("color"));
        if (!cursor.isNull(cursor.getColumnIndex("icon"))) {
            waypoint.style.icon = cursor.getString(cursor.getColumnIndex("icon"));
        }
        if (!cursor.isNull(cursor.getColumnIndex("locked"))) {
            waypoint.locked = cursor.getInt(cursor.getColumnIndex("locked")) > 0;
        }
        waypoint.source = this;
        return waypoint;
    }

    public boolean isNativeTrack() {
        return false;
    }
}
