package mobi.maptrek.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import mobi.maptrek.MapTrek;
import mobi.maptrek.data.MapObject;
import mobi.maptrek.data.MapObject.UpdatedEvent;
import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataProvider extends ContentProvider {
    private static final int MAP_OBJECTS = 1;
    private static final int MAP_OBJECTS_ID = 2;
    private static final int MARKERS_ID = 3;
    private static final Logger logger = LoggerFactory.getLogger(DataProvider.class);
    private static final UriMatcher uriMatcher = new UriMatcher(-1);

    static {
        uriMatcher.addURI(DataContract.AUTHORITY, "mapobjects", 1);
        uriMatcher.addURI(DataContract.AUTHORITY, "mapobjects/#", 2);
        uriMatcher.addURI(DataContract.AUTHORITY, "markers/*", 3);
    }

    public boolean onCreate() {
        return true;
    }

    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case 1:
                return "vnd.android.cursor.dir/vnd.mobi.maptrek.provider.mapobject";
            case 2:
                return "vnd.android.cursor.item/vnd.mobi.maptrek.provider.mapobject";
            case 3:
                return "vnd.android.cursor.item/vnd.mobi.maptrek.provider.marker";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        logger.debug("query({})", (Object) uri);
        if (uriMatcher.match(uri) != 3) {
            throw new UnsupportedOperationException("Querying objects is not supported");
        }
        String id = uri.getLastPathSegment();
        return new MatrixCursor(projection);
    }

    public Uri insert(@NonNull Uri uri, ContentValues values) {
        logger.debug("insert({})", (Object) uri);
        if (uriMatcher.match(uri) != 1) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        } else if (values == null) {
            throw new IllegalArgumentException("Values can not be null");
        } else {
            MapObject mo = new MapObject(0, 0);
            populateFields(mo, values);
            Uri objectUri = ContentUris.withAppendedId(DataContract.MAPOBJECTS_URI, MapTrek.addMapObject(mo));
            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(objectUri, null);
            }
            return objectUri;
        }
    }

    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        logger.debug("update({})", (Object) uri);
        if (uriMatcher.match(uri) != 2) {
            if (uriMatcher.match(uri) == 1) {
                throw new UnsupportedOperationException("Currently only updating one object by ID is supported");
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        } else if (values == null) {
            throw new IllegalArgumentException("Values can not be null");
        } else {
            MapObject mo = MapTrek.getMapObject(ContentUris.parseId(uri));
            if (mo == null) {
                return 0;
            }
            populateFields(mo, values);
            EventBus.getDefault().post(new UpdatedEvent(mo));
            Context context = getContext();
            if (context == null) {
                return 1;
            }
            context.getContentResolver().notifyChange(uri, null);
            return 1;
        }
    }

    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int i = 0;
        logger.debug("delete({})", (Object) uri);
        long[] ids = null;
        if (uriMatcher.match(uri) == 1) {
            if (DataContract.MAPOBJECT_ID_SELECTION.equals(selection)) {
                ids = new long[selectionArgs.length];
                for (int i2 = 0; i2 < ids.length; i2++) {
                    ids[i2] = Long.parseLong(selectionArgs[i2], 10);
                }
            } else {
                throw new IllegalArgumentException("Deleting is supported only by ID");
            }
        }
        if (uriMatcher.match(uri) == 2) {
            ids = new long[]{ContentUris.parseId(uri)};
        }
        if (ids == null) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int result = 0;
        int length = ids.length;
        while (i < length) {
            if (MapTrek.removeMapObject(ids[i])) {
                result++;
            }
            i++;
        }
        return result;
    }

    private void populateFields(MapObject mapObject, ContentValues values) {
        double latitude = 0.0d;
        double longitude = 0.0d;
        String key = DataContract.MAPOBJECT_COLUMNS[3];
        if (values.containsKey(key)) {
            mapObject.name = values.getAsString(key);
        }
        key = DataContract.MAPOBJECT_COLUMNS[4];
        if (values.containsKey(key)) {
            mapObject.description = values.getAsString(key);
        }
        key = DataContract.MAPOBJECT_COLUMNS[0];
        if (values.containsKey(key)) {
            latitude = values.getAsDouble(key).doubleValue();
        }
        key = DataContract.MAPOBJECT_COLUMNS[1];
        if (values.containsKey(key)) {
            longitude = values.getAsDouble(key).doubleValue();
        }
        key = DataContract.MAPOBJECT_COLUMNS[5];
        if (values.containsKey(key)) {
            mapObject.marker = values.getAsString(key);
        }
        key = DataContract.MAPOBJECT_COLUMNS[6];
        if (values.containsKey(key)) {
            mapObject.textColor = values.getAsInteger(key).intValue();
            mapObject.style.color = mapObject.textColor;
        }
        key = DataContract.MAPOBJECT_COLUMNS[2];
        if (values.containsKey(key)) {
            byte[] bytes = values.getAsByteArray(key);
            if (bytes != null) {
                mapObject.setBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            }
        }
        if (mapObject.coordinates.getLatitude() != latitude || mapObject.coordinates.getLongitude() != longitude) {
            mapObject.setCoordinates(latitude, longitude);
        }
    }
}
