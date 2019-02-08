package mobi.maptrek.maps.maptrek;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.maps.MapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapTrekDatabaseHelper extends SQLiteOpenHelper {
    static final String[] ALL_COLUMNS_FEATURES = new String[]{"id", COLUMN_FEATURES_KIND, "lat", "lon"};
    static final String[] ALL_COLUMNS_FEATURE_NAMES = new String[]{"id", "lang", "name"};
    static final String[] ALL_COLUMNS_MAPS = new String[]{MapService.EXTRA_X, MapService.EXTRA_Y, COLUMN_MAPS_DATE, COLUMN_MAPS_VERSION, COLUMN_MAPS_DOWNLOADING, COLUMN_MAPS_HILLSHADE_DOWNLOADING};
    static final String[] ALL_COLUMNS_NAMES = new String[]{"ref", "name"};
    static final String[] ALL_COLUMNS_TILES = new String[]{COLUMN_TILES_ZOOM_LEVEL, COLUMN_TILES_COLUMN, COLUMN_TILES_ROW, COLUMN_TILES_DATA};
    private static final String COLUMN_FEATURES_ID = "id";
    private static final String COLUMN_FEATURES_KIND = "kind";
    private static final String COLUMN_FEATURES_LAT = "lat";
    private static final String COLUMN_FEATURES_LON = "lon";
    private static final String COLUMN_FEATURES_NAMES_LANG = "lang";
    private static final String COLUMN_FEATURES_NAMES_NAME = "name";
    static final String COLUMN_INFO_NAME = "name";
    static final String COLUMN_INFO_VALUE = "value";
    static final String COLUMN_MAPS_DATE = "date";
    static final String COLUMN_MAPS_DOWNLOADING = "downloading";
    static final String COLUMN_MAPS_HILLSHADE_DOWNLOADING = "hillshade_downloading";
    static final String COLUMN_MAPS_VERSION = "version";
    static final String COLUMN_MAPS_X = "x";
    static final String COLUMN_MAPS_Y = "y";
    private static final String COLUMN_MAP_FEATURES_COLUMN = "x";
    private static final String COLUMN_MAP_FEATURES_FEATURE = "feature";
    private static final String COLUMN_MAP_FEATURES_ROW = "y";
    static final String COLUMN_NAMES_NAME = "name";
    private static final String COLUMN_NAMES_REF = "ref";
    private static final String COLUMN_TILES_COLUMN = "tile_column";
    static final String COLUMN_TILES_DATA = "tile_data";
    private static final String COLUMN_TILES_ROW = "tile_row";
    private static final String COLUMN_TILES_ZOOM_LEVEL = "zoom_level";
    private static final int DATABASE_VERSION = 5;
    private static final String FTS_MERGE = "INSERT INTO names_fts(names_fts) VALUES('merge=300,8')";
    public static final String PRAGMA_ENABLE_VACUUM = "PRAGMA main.auto_vacuum = INCREMENTAL";
    public static final String PRAGMA_PAGE_SIZE = "PRAGMA main.page_size = 4096";
    public static final String PRAGMA_VACUUM = "PRAGMA main.incremental_vacuum(5000)";
    private static final String SQL_CREATE_FEATURES = "CREATE TABLE IF NOT EXISTS features (id INTEGER NOT NULL, kind INTEGER, lat REAL, lon REAL)";
    private static final String SQL_CREATE_FEATURE_NAMES = "CREATE TABLE IF NOT EXISTS feature_names (id INTEGER NOT NULL, lang INTEGER NOT NULL, name INTEGER NOT NULL)";
    private static final String SQL_CREATE_INFO = "CREATE TABLE IF NOT EXISTS metadata (name TEXT NOT NULL, value TEXT)";
    static final String SQL_CREATE_MAPS = "CREATE TABLE IF NOT EXISTS maps (x INTEGER NOT NULL, y INTEGER NOT NULL, date INTEGER NOT NULL DEFAULT 0, version INTEGER NOT NULL DEFAULT 0, downloading INTEGER NOT NULL DEFAULT 0, hillshade_downloading INTEGER NOT NULL DEFAULT 0)";
    private static final String SQL_CREATE_MAP_FEATURES = "CREATE TABLE IF NOT EXISTS map_features (x INTEGER NOT NULL, y INTEGER NOT NULL, feature INTEGER NOT NULL)";
    private static final String SQL_CREATE_NAMES = "CREATE TABLE IF NOT EXISTS names (ref INTEGER NOT NULL, name TEXT NOT NULL)";
    private static final String SQL_CREATE_NAMES_FTS = "CREATE VIRTUAL TABLE IF NOT EXISTS names_fts USING fts4(tokenize=unicode61, content=\"names\", name)";
    private static final String SQL_CREATE_TILES = "CREATE TABLE IF NOT EXISTS tiles (zoom_level INTEGER NOT NULL, tile_column INTEGER NOT NULL, tile_row INTEGER NOT NULL, tile_data BLOB NOT NULL)";
    private static final String SQL_GET_NAME = "SELECT names.name, lang FROM names INNER JOIN feature_names ON (ref = feature_names.name) WHERE id = ? AND lang IN (0, ?) ORDER BY lang";
    private static final String SQL_INDEX_FEATURES = "CREATE UNIQUE INDEX IF NOT EXISTS feature_id ON features (id)";
    private static final String SQL_INDEX_FEATURE_LANG = "CREATE UNIQUE INDEX IF NOT EXISTS feature_name_lang ON feature_names (id, lang)";
    private static final String SQL_INDEX_FEATURE_NAME = "CREATE UNIQUE INDEX IF NOT EXISTS feature_name_ref ON feature_names (id, lang, name)";
    private static final String SQL_INDEX_FEATURE_NAMES = "CREATE INDEX IF NOT EXISTS feature_names_ref ON feature_names (name)";
    private static final String SQL_INDEX_INFO = "CREATE UNIQUE INDEX IF NOT EXISTS property ON metadata (name)";
    static final String SQL_INDEX_MAPS = "CREATE UNIQUE INDEX IF NOT EXISTS maps_x_y ON maps (x, y)";
    private static final String SQL_INDEX_MAP_FEATURES = "CREATE INDEX IF NOT EXISTS map_feature_ids ON map_features (feature)";
    private static final String SQL_INDEX_MAP_FEATURE_REFS = "CREATE UNIQUE INDEX IF NOT EXISTS map_feature_refs ON map_features (x, y, feature)";
    private static final String SQL_INDEX_NAMES = "CREATE UNIQUE INDEX IF NOT EXISTS name_ref ON names (ref)";
    private static final String SQL_INDEX_TILES = "CREATE UNIQUE INDEX IF NOT EXISTS coord ON tiles (zoom_level, tile_column, tile_row)";
    private static final String SQL_INSERT_NAMES_FTS = "INSERT INTO names_fts(docid, name) SELECT ref, name FROM names";
    static final String SQL_REMOVE_FEATURES = "DELETE FROM features WHERE id IN (SELECT a.feature FROM map_features AS a LEFT JOIN map_features AS b ON (a.feature = b.feature AND (a.x != b.x OR a.y != b.y)) WHERE a.x = ? AND a.y = ? AND b.feature IS NULL)";
    static final String SQL_REMOVE_FEATURE_NAMES = "DELETE FROM feature_names WHERE id IN (SELECT feature_names.id FROM feature_names LEFT JOIN features ON (feature_names.id = features.id) WHERE features.id IS NULL)";
    static final String SQL_REMOVE_NAMES = "DELETE FROM names WHERE ref IN (SELECT ref FROM names LEFT JOIN feature_names ON (ref = feature_names.name) WHERE id IS NULL)";
    static final String SQL_REMOVE_NAMES_FTS = "DELETE FROM names_fts WHERE docid IN (";
    static final String SQL_REMOVE_TILES = "DELETE FROM tiles WHERE zoom_level = ? AND tile_column >= ? AND tile_column <= ? AND tile_row >= ? AND tile_row <= ?";
    static final String SQL_SELECT_UNUSED_NAMES = "SELECT ref FROM names LEFT JOIN feature_names ON (ref = feature_names.name) WHERE id IS NULL";
    static final String TABLE_FEATURES = "features";
    static final String TABLE_FEATURE_NAMES = "feature_names";
    static final String TABLE_INFO = "metadata";
    static final String TABLE_MAPS = "maps";
    static final String TABLE_MAP_FEATURES = "map_features";
    static final String TABLE_NAMES = "names";
    static final String TABLE_NAMES_FTS = "names_fts";
    static final String TABLE_TILES = "tiles";
    static final String WHERE_INFO_NAME = "name = ?";
    static final String WHERE_MAPS_PRESENT = "date > 0 OR downloading > 0";
    static final String WHERE_MAPS_XY = "x = ? AND y = ?";
    static final String WHERE_TILE_ZXY = "zoom_level = ? AND tile_column = ? AND tile_row = ?";
    private static final Logger logger = LoggerFactory.getLogger(MapTrekDatabaseHelper.class);

    public MapTrekDatabaseHelper(Context context, File file) {
        super(context, file.getAbsolutePath(), null, 5);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        createWorldMapTables(db);
        logger.info("Vacuuming maps database");
        Cursor cursor = db.rawQuery(PRAGMA_VACUUM, null);
        if (cursor.moveToFirst()) {
            logger.debug("  removed {} pages", Integer.valueOf(cursor.getCount()));
        }
        cursor.close();
        if (hasFullTextIndex(db)) {
            cursor = db.rawQuery(FTS_MERGE, null);
            if (cursor.moveToFirst()) {
                logger.debug("  merged FTS index");
            }
            cursor.close();
        }
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PRAGMA_ENABLE_VACUUM);
        db.execSQL(PRAGMA_PAGE_SIZE);
        db.execSQL(SQL_CREATE_MAPS);
        db.execSQL(SQL_CREATE_MAP_FEATURES);
        db.execSQL(SQL_CREATE_INFO);
        db.execSQL(SQL_CREATE_TILES);
        db.execSQL(SQL_CREATE_NAMES);
        db.execSQL(SQL_CREATE_FEATURES);
        db.execSQL(SQL_CREATE_FEATURE_NAMES);
        db.execSQL(SQL_INDEX_MAPS);
        db.execSQL(SQL_INDEX_MAP_FEATURES);
        db.execSQL(SQL_INDEX_MAP_FEATURE_REFS);
        db.execSQL(SQL_INDEX_TILES);
        db.execSQL(SQL_INDEX_INFO);
        db.execSQL(SQL_INDEX_NAMES);
        db.execSQL(SQL_INDEX_FEATURES);
        db.execSQL(SQL_INDEX_FEATURE_LANG);
        db.execSQL(SQL_INDEX_FEATURE_NAME);
        db.execSQL(SQL_INDEX_FEATURE_NAMES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        logger.debug("Upgrade from {} to {}", Integer.valueOf(oldVersion), Integer.valueOf(newVersion));
        if (oldVersion <= 2) {
            db.execSQL(SQL_INDEX_FEATURE_NAMES);
        }
        if (oldVersion <= 3) {
            db.execSQL("DROP INDEX IF EXISTS map_feature_ids");
            db.execSQL(SQL_INDEX_MAP_FEATURES);
        }
        if (oldVersion <= 4) {
            db.execSQL("ALTER TABLE maps ADD COLUMN version");
            db.execSQL("ALTER TABLE maps ADD COLUMN hillshade_downloading");
        }
    }

    private static void createWorldMapTables(SQLiteDatabase db) {
        try {
            db.rawQuery("SELECT * FROM maps LIMIT 1", null).close();
        } catch (SQLiteException e) {
            db.execSQL(SQL_CREATE_MAPS);
            db.execSQL(SQL_INDEX_MAPS);
        }
        try {
            db.rawQuery("SELECT * FROM map_features LIMIT 1", null).close();
        } catch (SQLiteException e2) {
            db.execSQL(SQL_CREATE_MAP_FEATURES);
            db.execSQL(SQL_INDEX_MAP_FEATURES);
            db.execSQL(SQL_INDEX_MAP_FEATURE_REFS);
        }
    }

    public static void createFtsTable(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_NAMES_FTS);
        logger.debug("Populate fts");
        db.execSQL(SQL_INSERT_NAMES_FTS);
        logger.debug("Finished populating fts");
    }

    public static boolean hasFullTextIndex(SQLiteDatabase db) {
        try {
            db.rawQuery("SELECT docid FROM names_fts WHERE names_fts MATCH ?", new String[]{"Antarctica"}).close();
            return true;
        } catch (SQLiteException e) {
            return false;
        }
    }

    public static Waypoint getAmenityData(int lang, long elementId, SQLiteDatabase db) {
        Waypoint waypoint;
        Throwable th;
        Throwable e;
        try {
            Cursor c = db.query(TABLE_FEATURES, ALL_COLUMNS_FEATURES, "id = ?", new String[]{String.valueOf(elementId)}, null, null, null);
            Throwable th2 = null;
            try {
                if (c.moveToFirst()) {
                    int kind = c.getInt(c.getColumnIndex(COLUMN_FEATURES_KIND));
                    waypoint = new Waypoint(getFeatureName(lang, elementId, db), c.getDouble(c.getColumnIndex("lat")), c.getDouble(c.getColumnIndex("lon")));
                    try {
                        waypoint._id = elementId;
                        waypoint.proximity = kind;
                        waypoint.description = Tags.getKindName(kind);
                    } catch (Throwable th3) {
                        th = th3;
                        if (c != null) {
                            if (th2 == null) {
                                try {
                                    c.close();
                                } catch (Throwable th4) {
                                    th2.addSuppressed(th4);
                                }
                            } else {
                                c.close();
                            }
                        }
                        throw th;
                    }
                }
                waypoint = null;
                if (c != null) {
                    if (th2 != null) {
                        try {
                            c.close();
                        } catch (Throwable th5) {
                            try {
                                th2.addSuppressed(th5);
                            } catch (Exception e2) {
                                e = e2;
                                logger.error("Query error", e);
                                return waypoint;
                            }
                        }
                    }
                    c.close();
                }
                return waypoint;
            } catch (Throwable th6) {
                th5 = th6;
                waypoint = null;
                if (c != null) {
                    if (th2 == null) {
                        c.close();
                    } else {
                        c.close();
                    }
                }
                throw th5;
            }
        } catch (Exception e3) {
            e = e3;
            waypoint = null;
            logger.error("Query error", e);
            return waypoint;
        }
    }

    static String getFeatureName(int lang, long elementId, SQLiteDatabase db) {
        try {
            Throwable th;
            Cursor c = db.rawQuery(SQL_GET_NAME, new String[]{String.valueOf(elementId), String.valueOf(lang)});
            Throwable th2 = null;
            try {
                String[] result = new String[c.getCount()];
                int i = 0;
                if (c.moveToFirst()) {
                    do {
                        result[i] = c.getString(0);
                        i++;
                    } while (c.moveToNext());
                }
                if (result.length <= 0) {
                    if (c != null) {
                        if (null != null) {
                            try {
                                c.close();
                            } catch (Throwable th3) {
                                th2.addSuppressed(th3);
                            }
                        } else {
                            c.close();
                        }
                    }
                    return null;
                } else if (result.length != 2 || result[1] == null) {
                    r5 = result[0];
                    if (c == null) {
                        return r5;
                    }
                    if (null != null) {
                        try {
                            c.close();
                            return r5;
                        } catch (Throwable th4) {
                            th2.addSuppressed(th4);
                            return r5;
                        }
                    }
                    c.close();
                    return r5;
                } else {
                    r5 = result[1];
                    if (c == null) {
                        return r5;
                    }
                    if (null != null) {
                        try {
                            c.close();
                            return r5;
                        } catch (Throwable th42) {
                            th2.addSuppressed(th42);
                            return r5;
                        }
                    }
                    c.close();
                    return r5;
                }
            } catch (Throwable th22) {
                Throwable th5 = th22;
                th22 = th3;
                th3 = th5;
            }
            throw th3;
            if (c != null) {
                if (th22 != null) {
                    try {
                        c.close();
                    } catch (Throwable th422) {
                        th22.addSuppressed(th422);
                    }
                } else {
                    c.close();
                }
            }
            throw th3;
        } catch (Throwable e) {
            logger.error("Query error", e);
        }
    }

    public static int getLanguageId(@Nullable String lang) {
        if (lang == null) {
            return 0;
        }
        int i = -1;
        switch (lang.hashCode()) {
            case 3201:
                if (lang.equals("de")) {
                    i = 1;
                    break;
                }
                break;
            case 3241:
                if (lang.equals("en")) {
                    i = 0;
                    break;
                }
                break;
            case 3651:
                if (lang.equals("ru")) {
                    i = 2;
                    break;
                }
                break;
        }
        switch (i) {
            case 0:
                return 840;
            case 1:
                return 276;
            case 2:
                return 643;
            default:
                return 0;
        }
    }
}
