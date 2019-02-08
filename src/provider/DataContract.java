package mobi.maptrek.provider;

import android.net.Uri;
import mobi.maptrek.io.kml.KmlFile;

public final class DataContract {
    public static final String ACTION_PICK_MARKER = "mobi.maptrek.provider.PICK_MARKER";
    public static final String AUTHORITY = "mobi.maptrek.data";
    protected static final String MAPOBJECTS_PATH = "mapobjects";
    public static final Uri MAPOBJECTS_URI = Uri.parse("content://mobi.maptrek.data/mapobjects");
    public static final int MAPOBJECT_BITMAP_COLUMN = 2;
    public static final int MAPOBJECT_COLOR_COLUMN = 6;
    public static final String[] MAPOBJECT_COLUMNS = new String[]{"latitude", "longitude", "bitmap", "name", KmlFile.TAG_DESCRIPTION, "marker", "color"};
    public static final int MAPOBJECT_DESCRIPTION_COLUMN = 4;
    public static final String MAPOBJECT_ID_SELECTION = "IDLIST";
    public static final int MAPOBJECT_LATITUDE_COLUMN = 0;
    public static final int MAPOBJECT_LONGITUDE_COLUMN = 1;
    public static final int MAPOBJECT_MARKER_COLUMN = 5;
    public static final int MAPOBJECT_NAME_COLUMN = 3;
    protected static final String MARKERS_PATH = "markers";
    public static final Uri MARKERS_URI = Uri.parse("content://mobi.maptrek.data/markers");
    public static final int MARKER_COLUMN = 0;
    public static final String[] MARKER_COLUMNS = new String[]{"BITMAP"};
}
