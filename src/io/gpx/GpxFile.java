package mobi.maptrek.io.gpx;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GpxFile {
    public static final String ATTRIBUTE_CREATOR = "creator";
    public static final String ATTRIBUTE_LAT = "lat";
    public static final String ATTRIBUTE_LON = "lon";
    public static final String NS = "http://www.topografix.com/GPX/1/1";
    public static final String TAG_DESC = "desc";
    public static final String TAG_ELE = "ele";
    public static final String TAG_GPX = "gpx";
    public static final String TAG_METADATA = "metadata";
    public static final String TAG_NAME = "name";
    public static final String TAG_TIME = "time";
    public static final String TAG_TRK = "trk";
    public static final String TAG_TRKPT = "trkpt";
    public static final String TAG_TRKSEG = "trkseg";
    public static final String TAG_WPT = "wpt";
    static final DateFormat TRKTIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    static final DateFormat TRKTIME_MS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());

    public static class Metadata {
        String name;
    }

    public static long parseTime(String timeString) throws ParseException {
        if (timeString.length() > 20) {
            return TRKTIME_MS.parse(timeString).getTime();
        }
        return TRKTIME.parse(timeString).getTime();
    }

    public static String formatTime(Date date) {
        return TRKTIME.format(date);
    }
}
