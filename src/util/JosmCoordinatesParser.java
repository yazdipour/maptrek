package mobi.maptrek.util;

import android.support.annotation.NonNull;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.coords.MGRSCoord;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.oscim.core.GeoPoint;

public final class JosmCoordinatesParser {
    private static final String DEG = "°";
    public static final String EAST = "В";
    private static final char E_TR = EAST.charAt(0);
    private static final String MIN = "′";
    public static final String NORTH = "С";
    private static final char N_TR = NORTH.charAt(0);
    private static final Pattern P = Pattern.compile("([+|-]?\\d+[.,]\\d+)|([+|-]?\\d+)|(°|o|deg)|('|′|min)|(\"|″|sec)|([,;])|([NSEW" + N_TR + S_TR + E_TR + W_TR + "])|\\s+|.+", 2);
    private static final Pattern P_UTM = Pattern.compile("([1-9]\\d?)([CDEFGHJKLMNPQRSTUVWX])\\s?([ABCDEFGHJKLMNPQRSTUVWXYZ][ABCDEFGHJKLMNPQRSTUV])?\\s?((\\d+)(:?\\s(\\d+))?)");
    private static final Pattern P_XML = Pattern.compile("lat=[\"']([+|-]?\\d+[.,]\\d+)[\"']\\s+lon=[\"']([+|-]?\\d+[.,]\\d+)[\"']");
    private static final String SEC = "″";
    public static final String SOUTH = "Ю";
    private static final char S_TR = SOUTH.charAt(0);
    public static final String WEST = "З";
    private static final char W_TR = WEST.charAt(0);

    private static class LatLonHolder {
        private double lat;
        private double lon;

        private LatLonHolder() {
            this.lat = Double.NaN;
            this.lon = Double.NaN;
        }
    }

    public static class Result {
        public GeoPoint coordinates;
        public int offset;
    }

    private JosmCoordinatesParser() {
    }

    @NonNull
    public static Result parseWithResult(@NonNull String input) throws IllegalArgumentException {
        Result result = new Result();
        LatLonHolder latLon = new LatLonHolder();
        Matcher mXml = P_XML.matcher(input);
        if (mXml.matches()) {
            setLatLonObj(latLon, Double.valueOf(mXml.group(1).replace(',', '.')), Double.valueOf(0.0d), Double.valueOf(0.0d), "N", Double.valueOf(mXml.group(2).replace(',', '.')), Double.valueOf(0.0d), Double.valueOf(0.0d), "E");
            result.coordinates = new GeoPoint(latLon.lat, latLon.lon);
        } else {
            Matcher mUtm = P_UTM.matcher(input);
            if (mUtm.lookingAt()) {
                result.offset = mUtm.end();
                if (mUtm.group(3) != null) {
                    MGRSCoord mgrs = MGRSCoord.fromString(mUtm.group());
                    result.coordinates = new GeoPoint(mgrs.getLatitude().degrees, mgrs.getLongitude().degrees);
                } else {
                    double easting;
                    double northing;
                    int zone = Integer.valueOf(mUtm.group(1)).intValue();
                    String hemisphere = mUtm.group(2);
                    if ("N".equals(hemisphere)) {
                        hemisphere = AVKey.NORTH;
                    }
                    if ("S".equals(hemisphere)) {
                        hemisphere = AVKey.SOUTH;
                    }
                    if (mUtm.group(6) != null) {
                        easting = Double.valueOf(mUtm.group(5)).doubleValue();
                        northing = Double.valueOf(mUtm.group(6)).doubleValue();
                    } else {
                        String en = mUtm.group(4);
                        int l = en.length() >> 1;
                        easting = Double.valueOf(en.substring(0, l)).doubleValue();
                        northing = Double.valueOf(en.substring(l, en.length())).doubleValue();
                    }
                    UTMCoord utm = UTMCoord.fromUTM(zone, hemisphere, easting, northing);
                    result.coordinates = new GeoPoint(utm.getLatitude().degrees, utm.getLongitude().degrees);
                }
            } else {
                Matcher m = P.matcher(input);
                StringBuilder sb = new StringBuilder();
                List<Object> list = new ArrayList();
                while (m.find()) {
                    if (m.group(1) != null) {
                        sb.append('R');
                        list.add(Double.valueOf(m.group(1).replace(',', '.')));
                        result.offset = m.end();
                    } else if (m.group(2) != null) {
                        sb.append('Z');
                        list.add(Double.valueOf(m.group(2)));
                        result.offset = m.end();
                    } else if (m.group(3) != null) {
                        sb.append('o');
                        result.offset = m.end();
                    } else if (m.group(4) != null) {
                        sb.append('\'');
                        result.offset = m.end();
                    } else if (m.group(5) != null) {
                        sb.append('\"');
                        result.offset = m.end();
                    } else if (m.group(6) != null) {
                        sb.append(',');
                        result.offset = m.end();
                    } else if (m.group(7) != null) {
                        sb.append('x');
                        String c = m.group(7).toUpperCase(Locale.ENGLISH);
                        if ("N".equalsIgnoreCase(c) || "S".equalsIgnoreCase(c) || "E".equalsIgnoreCase(c) || "W".equalsIgnoreCase(c)) {
                            list.add(c);
                        } else {
                            List<Object> list2 = list;
                            list2.add(c.replace(N_TR, 'N').replace(S_TR, 'S').replace(E_TR, 'E').replace(W_TR, 'W'));
                        }
                        result.offset = m.end();
                    }
                }
                String pattern = sb.toString();
                Object[] params = list.toArray();
                if (pattern.matches("Ro?,?Ro?")) {
                    setLatLonObj(latLon, params[0], Double.valueOf(0.0d), Double.valueOf(0.0d), "N", params[1], Double.valueOf(0.0d), Double.valueOf(0.0d), "E");
                } else {
                    if (pattern.matches("xRo?,?xRo?")) {
                        setLatLonObj(latLon, params[1], Double.valueOf(0.0d), Double.valueOf(0.0d), params[0], params[3], Double.valueOf(0.0d), Double.valueOf(0.0d), params[2]);
                    } else {
                        if (pattern.matches("Ro?x,?Ro?x")) {
                            setLatLonObj(latLon, params[0], Double.valueOf(0.0d), Double.valueOf(0.0d), params[1], params[2], Double.valueOf(0.0d), Double.valueOf(0.0d), params[3]);
                        } else {
                            if (pattern.matches("Zo[RZ]'?,?Zo[RZ]'?|Z[RZ],?Z[RZ]")) {
                                setLatLonObj(latLon, params[0], params[1], Double.valueOf(0.0d), "N", params[2], params[3], Double.valueOf(0.0d), "E");
                            } else {
                                if (pattern.matches("xZo[RZ]'?,?xZo[RZ]'?|xZo?[RZ],?xZo?[RZ]")) {
                                    setLatLonObj(latLon, params[1], params[2], Double.valueOf(0.0d), params[0], params[4], params[5], Double.valueOf(0.0d), params[3]);
                                } else {
                                    if (pattern.matches("Zo[RZ]'?x,?Zo[RZ]'?x|Zo?[RZ]x,?Zo?[RZ]x")) {
                                        setLatLonObj(latLon, params[0], params[1], Double.valueOf(0.0d), params[2], params[3], params[4], Double.valueOf(0.0d), params[5]);
                                    } else {
                                        if (pattern.matches("ZoZ'[RZ]\"?,?ZoZ'[RZ]\"?|ZZ[RZ],?ZZ[RZ]")) {
                                            setLatLonObj(latLon, params[0], params[1], params[2], "N", params[3], params[4], params[5], "E");
                                        } else {
                                            if (pattern.matches("ZoZ'[RZ]\"?x,?ZoZ'[RZ]\"?x|ZZ[RZ]x,?ZZ[RZ]x")) {
                                                setLatLonObj(latLon, params[0], params[1], params[2], params[3], params[4], params[5], params[6], params[7]);
                                            } else {
                                                if (pattern.matches("xZoZ'[RZ]\"?,?xZoZ'[RZ]\"?|xZZ[RZ],?xZZ[RZ]")) {
                                                    setLatLonObj(latLon, params[1], params[2], params[3], params[0], params[5], params[6], params[7], params[4]);
                                                } else {
                                                    if (pattern.matches("ZZ[RZ],?ZZ[RZ]")) {
                                                        setLatLonObj(latLon, params[0], params[1], params[2], "N", params[3], params[4], params[5], "E");
                                                    } else {
                                                        throw new IllegalArgumentException("invalid format: " + pattern);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                result.coordinates = new GeoPoint(latLon.lat, latLon.lon);
            }
        }
        return result;
    }

    @NonNull
    public static GeoPoint parse(@NonNull String input) throws IllegalArgumentException {
        return parseWithResult(input).coordinates;
    }

    private static void setLatLonObj(LatLonHolder latLon, Object coord1deg, Object coord1min, Object coord1sec, Object card1, Object coord2deg, Object coord2min, Object coord2sec, Object card2) {
        setLatLon(latLon, ((Double) coord1deg).doubleValue(), ((Double) coord1min).doubleValue(), ((Double) coord1sec).doubleValue(), (String) card1, ((Double) coord2deg).doubleValue(), ((Double) coord2min).doubleValue(), ((Double) coord2sec).doubleValue(), (String) card2);
    }

    private static void setLatLon(LatLonHolder latLon, double coord1deg, double coord1min, double coord1sec, String card1, double coord2deg, double coord2min, double coord2sec, String card2) {
        setLatLon(latLon, coord1deg, coord1min, coord1sec, card1);
        setLatLon(latLon, coord2deg, coord2min, coord2sec, card2);
        if (Double.isNaN(latLon.lat) || Double.isNaN(latLon.lon)) {
            throw new IllegalArgumentException("invalid lat/lon parameters");
        }
    }

    private static void setLatLon(LatLonHolder latLon, double coordDeg, double coordMin, double coordSec, String card) {
        if (coordDeg < -180.0d || coordDeg > 180.0d || coordMin < 0.0d || coordMin >= 60.0d || coordSec < 0.0d || coordSec > 60.0d) {
            throw new IllegalArgumentException("out of range");
        }
        double coord = ((double) (coordDeg < 0.0d ? -1 : 1)) * ((Math.abs(coordDeg) + (coordMin / 60.0d)) + (coordSec / 3600.0d));
        if (!("N".equals(card) || "E".equals(card))) {
            coord = -coord;
        }
        if ("N".equals(card) || "S".equals(card)) {
            latLon.lat = coord;
        } else {
            latLon.lon = coord;
        }
    }
}
