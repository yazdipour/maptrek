package mobi.maptrek.data.source;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.File;
import java.io.IOException;

class WaypointDbHelper extends SQLiteOpenHelper {
    private static final String ALTER_WAYPOINTS_1 = "ALTER TABLE waypoint ADD COLUMN locked INTEGER;";
    static final String COLUMN_ALTITUDE = "altitude";
    static final String COLUMN_COLOR = "color";
    static final String COLUMN_DATE = "date";
    static final String COLUMN_DESCRIPTION = "description";
    static final String COLUMN_ICON = "icon";
    static final String COLUMN_ID = "_id";
    static final String COLUMN_LATE6 = "latitude";
    static final String COLUMN_LOCKED = "locked";
    static final String COLUMN_LONE6 = "longitude";
    static final String COLUMN_NAME = "name";
    static final String COLUMN_PROXIMITY = "proximity";
    private static final int DATABASE_VERSION = 2;
    private static final String SQL_CREATE_WAYPOINT_SCHEMA = "CREATE TABLE waypoint(_id INTEGER PRIMARY KEY,name TEXT NOT NULL,latitude INTEGER NOT NULL,longitude INTEGER NOT NULL,altitude INTEGER,proximity INTEGER,description TEXT,date LONG,color INTEGER,icon TEXT,locked INTEGER);";
    static final String TABLE_NAME = "waypoint";

    WaypointDbHelper(Context context, File file) {
        super(context, file.getAbsolutePath(), null, 2);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_WAYPOINT_SCHEMA);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(ALTER_WAYPOINTS_1);
        }
    }
}
