package mobi.maptrek.util;

import com.skedgo.converter.TimezoneMapper;
import gov.nasa.worldwind.util.WWMath;
import java.util.Calendar;
import java.util.TimeZone;
import org.oscim.utils.GeoPointUtils;

public class SunriseSunset {
    private static final double ASTRONOMICAL = 108.0d;
    private static final double CIVIL = 96.0d;
    private static double D2R = 0.017453292519943295d;
    private static final double NAUTICAL = 102.0d;
    private static final double OFFICIAL = 90.8333d;
    private static double R2D = 57.29577951308232d;
    private int N = this.calendar.get(6);
    private Calendar calendar = Calendar.getInstance();
    private double latRad;
    private double lngHour;
    private double tzOffset = ((((double) (this.calendar.get(15) + this.calendar.get(16))) * 1.0d) / WWMath.HOUR_TO_MILLIS);
    private double zenith = OFFICIAL;

    public void setLocation(double latitude, double longitude) {
        this.tzOffset = (((double) TimeZone.getTimeZone(TimezoneMapper.latLngToTimezoneString(latitude, longitude)).getOffset(this.calendar.getTimeInMillis())) * 1.0d) / WWMath.HOUR_TO_MILLIS;
        this.latRad = Math.toRadians(latitude);
        this.lngHour = longitude / 15.0d;
    }

    public double compute(boolean sunrise) {
        double t = ((double) this.N) + ((((double) (sunrise ? 6 : 18)) - this.lngHour) / 24.0d);
        double M = (0.9856d * t) - 3.289d;
        double radM = M * D2R;
        double L = adjustDegrees((((1.916d * Math.sin(radM)) + M) + (0.02d * Math.sin(2.0d * radM))) + 282.634d);
        double RA = adjustDegrees(R2D * Math.atan(0.91764d * Math.tan(D2R * L)));
        RA = ((RA + (Math.floor(L / GeoPointUtils.LATITUDE_MAX) * GeoPointUtils.LATITUDE_MAX)) - (Math.floor(RA / GeoPointUtils.LATITUDE_MAX) * GeoPointUtils.LATITUDE_MAX)) / 15.0d;
        double sinDec = 0.39782d * Math.sin(D2R * L);
        double cosH = (Math.cos(this.zenith * D2R) - (Math.sin(this.latRad) * sinDec)) / (Math.cos(this.latRad) * Math.cos(Math.asin(sinDec)));
        if (cosH > 1.0d) {
            return Double.MAX_VALUE;
        }
        if (cosH < -1.0d) {
            return Double.MIN_VALUE;
        }
        double H = R2D * Math.acos(cosH);
        if (sunrise) {
            H = 360.0d - H;
        }
        return adjustTime(((((H / 15.0d) + RA) - (0.06571d * t)) - 6.622d) - this.lngHour);
    }

    public boolean isDaytime(double now) {
        boolean z = true;
        double sunrise = compute(true);
        double sunset = compute(false);
        if (sunrise == Double.MIN_VALUE || sunset == Double.MIN_NORMAL) {
            return true;
        }
        if (sunrise == Double.MAX_VALUE || sunset == Double.MAX_VALUE) {
            return false;
        }
        if (sunrise <= sunset) {
            if (now >= sunset || now <= sunrise) {
                z = false;
            }
            return z;
        } else if (now >= sunrise || now <= sunset) {
            return true;
        } else {
            return false;
        }
    }

    private static double adjustDegrees(double degrees) {
        if (degrees >= 360.0d) {
            degrees -= 360.0d;
        }
        if (degrees < 0.0d) {
            return degrees + 360.0d;
        }
        return degrees;
    }

    private static double adjustTime(double time) {
        if (time >= 24.0d) {
            time -= 24.0d;
        }
        if (time < 0.0d) {
            return time + 24.0d;
        }
        return time;
    }

    public CharSequence formatTime(double time) {
        return StringFormatter.timeR((int) (adjustTime(this.tzOffset + time) * 60.0d));
    }

    public double getUtcOffset() {
        return this.tzOffset;
    }
}
