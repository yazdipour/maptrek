package mobi.maptrek.util;

import android.annotation.SuppressLint;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.coords.MGRSCoord;
import gov.nasa.worldwind.geom.coords.UPSCoord;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import gov.nasa.worldwind.util.WWMath;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.oscim.core.GeoPoint;
import org.oscim.map.Viewport;
import org.slf4j.Marker;

public class StringFormatter {
    public static String angleAbbr = "deg";
    public static double angleFactor = 1.0d;
    public static String angleFormat = "%.0f";
    static final DecimalFormat coordDegFormat = new DecimalFormat("#0.000000", new DecimalFormatSymbols(Locale.ENGLISH));
    static final DecimalFormat coordIntFormat = new DecimalFormat("00", new DecimalFormatSymbols(Locale.ENGLISH));
    static final DecimalFormat coordMinFormat = new DecimalFormat("00.0000", new DecimalFormatSymbols(Locale.ENGLISH));
    static final DecimalFormat coordSecFormat = new DecimalFormat("00.000", new DecimalFormatSymbols(Locale.ENGLISH));
    public static int coordinateFormat = 0;
    public static String distanceAbbr = "km";
    public static double distanceFactor = 1.0d;
    public static String distanceShortAbbr = "m";
    public static double distanceShortFactor = 1.0d;
    public static String elevationAbbr = "m";
    public static float elevationFactor = Viewport.VIEW_NEAR;
    public static String elevationFormat = "%.0f";
    public static String hourAbbr = "h";
    public static String minuteAbbr = "min";
    public static String precisionFormat = "%.0f";
    public static String secondAbbr = "sec";
    public static String speedAbbr = "m/s";
    public static float speedFactor = Viewport.VIEW_NEAR;
    static final DecimalFormat timeFormat = new DecimalFormat("00");

    @SuppressLint({"DefaultLocale"})
    public static String distanceHP(double distance) {
        double dist = (distance / WWMath.SECOND_TO_MILLIS) * distanceFactor;
        long rdist = (long) dist;
        long rfrac = (long) ((distanceShortFactor * (((dist - ((double) rdist)) * WWMath.SECOND_TO_MILLIS) / distanceFactor)) + 0.5d);
        if (rdist > 0 && rfrac > 0) {
            return String.format("%d %s %d %s", new Object[]{Long.valueOf(rdist), distanceAbbr, Long.valueOf(rfrac), distanceShortAbbr});
        } else if (rdist > 0) {
            return String.format("%d %s", new Object[]{Long.valueOf(rdist), distanceAbbr});
        } else {
            return String.format("%d %s", new Object[]{Long.valueOf(rfrac), distanceShortAbbr});
        }
    }

    public static String distanceH(double distance) {
        return distanceH(distance, 2000);
    }

    public static String distanceH(double distance, int threshold) {
        String[] dist = distanceC(distance, threshold);
        return dist[0] + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + dist[1];
    }

    public static String distanceH(double distance, String format) {
        return distanceH(distance, format, 2000);
    }

    public static String distanceH(double distance, String format, int threshold) {
        String[] dist = distanceC(distance, format, threshold);
        return dist[0] + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + dist[1];
    }

    public static String[] distanceC(double distance) {
        return distanceC(distance, 2000);
    }

    public static String[] distanceC(double distance, int threshold) {
        return distanceC(distance, "%.0f", threshold);
    }

    public static String[] distanceC(double distance, String format) {
        return distanceC(distance, format, 2000);
    }

    public static String[] distanceC(double distance, String format, int threshold) {
        double dist = distance * distanceShortFactor;
        String distunit = distanceShortAbbr;
        if (Math.abs(dist) > ((double) threshold)) {
            dist = ((dist / distanceShortFactor) / WWMath.SECOND_TO_MILLIS) * distanceFactor;
            distunit = distanceAbbr;
        }
        r3 = new String[2];
        r3[0] = String.format(format, new Object[]{Double.valueOf(dist)});
        r3[1] = distunit;
        return r3;
    }

    public static String speedH(float speed) {
        return speedC(speed) + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + speedAbbr;
    }

    public static String speedC(float speed) {
        return String.format(precisionFormat, new Object[]{Float.valueOf(speedFactor * speed)});
    }

    public static String elevationH(float elevation) {
        return elevationC(elevation) + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + elevationAbbr;
    }

    public static String elevationC(float elevation) {
        return String.format(elevationFormat, new Object[]{Float.valueOf(elevationFactor * elevation)});
    }

    public static String angleH(double angle) {
        if (angleFactor != 1.0d) {
            return angleC(angle) + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + angleAbbr;
        }
        return String.format(angleFormat, new Object[]{Double.valueOf(angle)}) + "°";
    }

    public static String angleC(double angle) {
        return String.format(angleFormat, new Object[]{Double.valueOf(angle / angleFactor)});
    }

    public static String coordinate(double coordinate) {
        return coordinate(coordinateFormat, coordinate);
    }

    public static String coordinate(int format, double coordinate) {
        double sign;
        double coord;
        int degrees;
        switch (format) {
            case 0:
                return coordDegFormat.format(coordinate);
            case 1:
                sign = Math.signum(coordinate);
                coord = Math.abs(coordinate);
                degrees = (int) Math.floor(coord);
                return coordIntFormat.format(((double) degrees) * sign) + "° " + coordMinFormat.format((coord - ((double) degrees)) * 60.0d) + "'";
            case 2:
                sign = Math.signum(coordinate);
                coord = Math.abs(coordinate);
                degrees = (int) Math.floor(coord);
                double min = (coord - ((double) degrees)) * 60.0d;
                int minutes = (int) Math.floor(min);
                return coordIntFormat.format(((double) degrees) * sign) + "° " + coordIntFormat.format((long) minutes) + "' " + coordSecFormat.format((min - ((double) minutes)) * 60.0d) + "\"";
            default:
                return String.valueOf(coordinate);
        }
    }

    public static String coordinates(GeoPoint point) {
        return coordinates(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, point.getLatitude(), point.getLongitude());
    }

    public static String coordinates(String delimiter, double latitude, double longitude) {
        return coordinates(coordinateFormat, delimiter, latitude, longitude);
    }

    public static String coordinates(int format, String delimiter, double latitude, double longitude) {
        switch (format) {
            case 0:
            case 1:
            case 2:
                return coordinate(format, latitude) + delimiter + coordinate(format, longitude);
            case 3:
                try {
                    Angle lat = Angle.fromDegrees(latitude);
                    Angle lon = Angle.fromDegrees(longitude);
                    if (latitude >= 84.0d || latitude <= -80.0d) {
                        return UPSCoord.fromLatLon(lat, lon).toString();
                    }
                    return UTMCoord.fromLatLon(lat, lon).toString();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    break;
                }
            case 4:
                try {
                    return MGRSCoord.fromLatLon(Angle.fromDegrees(latitude), Angle.fromDegrees(longitude)).toString();
                } catch (IllegalArgumentException e2) {
                    e2.printStackTrace();
                    break;
                }
        }
        return coordDegFormat.format(latitude) + delimiter + coordDegFormat.format(longitude);
    }

    public static String bearingSimpleH(double bearing) {
        if (bearing < 22.0d || bearing >= 338.0d) {
            return "↑";
        }
        if (bearing < 67.0d && bearing >= 22.0d) {
            return "↗";
        }
        if (bearing < 112.0d && bearing >= 67.0d) {
            return "→";
        }
        if (bearing < 158.0d && bearing >= 112.0d) {
            return "↘";
        }
        if (bearing < 202.0d && bearing >= 158.0d) {
            return "↓";
        }
        if (bearing < 248.0d && bearing >= 202.0d) {
            return "↙";
        }
        if (bearing < 292.0d && bearing >= 248.0d) {
            return "←";
        }
        if (bearing >= 338.0d || bearing < 292.0d) {
            return ".";
        }
        return "↖";
    }

    public static String[] timeC(int minutes) {
        int hour = 0;
        int min = minutes;
        if (min <= 1) {
            return new String[]{"< 1", minuteAbbr};
        }
        if (min > 59) {
            hour = (int) Math.floor((double) (min / 60));
            min -= hour * 60;
        }
        if (hour > 23) {
            return new String[]{"> 24", hourAbbr};
        }
        r2 = new String[2];
        r2[0] = String.format(Locale.getDefault(), "%02d:%02d", new Object[]{Integer.valueOf(hour), Integer.valueOf(min)});
        r2[1] = minuteAbbr;
        return r2;
    }

    public static String[] timeCP(int seconds, int timeout) {
        boolean t;
        if (seconds > timeout) {
            t = true;
        } else {
            t = false;
        }
        System.err.print("CP " + seconds + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + timeout);
        if (seconds > 59) {
            int min = (int) Math.floor((double) (seconds / 60));
            if (t) {
                min = (int) Math.floor((double) (timeout / 60));
                return new String[]{"> " + String.valueOf(min), minuteAbbr};
            }
            return new String[]{String.valueOf(min), minuteAbbr};
        } else if (t) {
            return new String[]{"> " + String.valueOf(timeout), secondAbbr};
        } else {
            return new String[]{String.valueOf(seconds), secondAbbr};
        }
    }

    public static String timeR(int minutes) {
        int hour = 0;
        int min = minutes;
        if (min > 59) {
            hour = (int) Math.floor((double) (min / 60));
            min -= hour * 60;
        }
        if (hour > 99) {
            return "--:--";
        }
        return String.format(Locale.getDefault(), "%02d:%02d", new Object[]{Integer.valueOf(hour), Integer.valueOf(min)});
    }

    public static String timeO(int minutes) {
        boolean minus = false;
        if (minutes < 0) {
            minus = true;
            minutes = -minutes;
        }
        int hour = 0;
        int min = minutes;
        if (min > 59) {
            hour = (int) Math.floor((double) (min / 60));
            min -= hour * 60;
        }
        Locale locale = Locale.getDefault();
        String str = "%s%d:%02d";
        Object[] objArr = new Object[3];
        objArr[0] = minus ? "-" : Marker.ANY_NON_NULL_MARKER;
        objArr[1] = Integer.valueOf(hour);
        objArr[2] = Integer.valueOf(min);
        return String.format(locale, str, objArr);
    }
}
