package mobi.maptrek.io.kml;

import java.util.HashMap;
import java.util.List;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import org.oscim.backend.canvas.Color;

public class KmlFile {
    public static final String ATTRIBUTE_ID = "id";
    public static final String NS = "http://www.opengis.net/kml/2.2";
    public static final String TAG_BEGIN = "begin";
    public static final String TAG_COLOR = "color";
    public static final String TAG_COORDINATES = "coordinates";
    public static final String TAG_DESCRIPTION = "description";
    public static final String TAG_DOCUMENT = "Document";
    public static final String TAG_END = "end";
    public static final String TAG_FOLDER = "Folder";
    public static final String TAG_ICON_STYLE = "IconStyle";
    public static final String TAG_KEY = "key";
    public static final String TAG_KML = "kml";
    public static final String TAG_LINE_STRING = "LineString";
    public static final String TAG_LINE_STYLE = "LineStyle";
    public static final String TAG_LIST_ITEM_TYPE = "listItemType";
    public static final String TAG_LIST_STYLE = "ListStyle";
    public static final String TAG_NAME = "name";
    public static final String TAG_OPEN = "open";
    public static final String TAG_PAIR = "Pair";
    public static final String TAG_PLACEMARK = "Placemark";
    public static final String TAG_POINT = "Point";
    public static final String TAG_STYLE = "Style";
    public static final String TAG_STYLE_MAP = "StyleMap";
    public static final String TAG_STYLE_URL = "styleUrl";
    public static final String TAG_TESSELLATE = "tessellate";
    public static final String TAG_TIME_SPAN = "TimeSpan";
    public static final String TAG_WIDTH = "width";

    public static class ColorStyle {
        int color;
    }

    public static class Folder {
        String name;
        List<Placemark> placemarks;
    }

    public static class Placemark {
        Waypoint point;
        Style style;
        String styleUrl;
        Track track;
    }

    public static class StyleType {
        String id;
    }

    public static class IconStyle extends ColorStyle {
    }

    public static class LineStyle extends ColorStyle {
        float width;
    }

    public static class Style extends StyleType {
        IconStyle iconStyle;
        LineStyle lineStyle;
    }

    public static class StyleMap extends StyleType {
        HashMap<String, String> map = new HashMap();
    }

    static int reverseColor(int color) {
        return (((16711680 & color) >>> 16) | ((color & 255) << 16)) | (Color.GREEN & color);
    }
}
