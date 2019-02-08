package mobi.maptrek.util;

import org.oscim.utils.GeoPointUtils;

public class Geo {
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double L = Math.toRadians(lon2 - lon1);
        double U1 = Math.atan((1.0d - UTMCoordConverter.WGS84_F) * Math.tan(Math.toRadians(lat1)));
        double U2 = Math.atan((1.0d - UTMCoordConverter.WGS84_F) * Math.tan(Math.toRadians(lat2)));
        double sinU1 = Math.sin(U1);
        double cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2);
        double cosU2 = Math.cos(U2);
        double lambda = L;
        double iterLimit = 100.0d;
        do {
            double sinLambda = Math.sin(lambda);
            double cosLambda = Math.cos(lambda);
            double sinSigma = Math.sqrt(((cosU2 * sinLambda) * (cosU2 * sinLambda)) + (((cosU1 * sinU2) - ((sinU1 * cosU2) * cosLambda)) * ((cosU1 * sinU2) - ((sinU1 * cosU2) * cosLambda))));
            if (sinSigma != 0.0d) {
                double cosSigma = (sinU1 * sinU2) + ((cosU1 * cosU2) * cosLambda);
                double sigma = Math.atan2(sinSigma, cosSigma);
                double sinAlpha = ((cosU1 * cosU2) * sinLambda) / sinSigma;
                double cosSqAlpha = 1.0d - (sinAlpha * sinAlpha);
                double cos2SigmaM = cosSigma - (((2.0d * sinU1) * sinU2) / cosSqAlpha);
                double C = ((UTMCoordConverter.WGS84_F / 16.0d) * cosSqAlpha) * (4.0d + ((4.0d - (3.0d * cosSqAlpha)) * UTMCoordConverter.WGS84_F));
                double lambdaP = lambda;
                lambda = L + ((((1.0d - C) * UTMCoordConverter.WGS84_F) * sinAlpha) * (((C * sinSigma) * (((C * cosSigma) * (-1.0d + ((2.0d * cos2SigmaM) * cos2SigmaM))) + cos2SigmaM)) + sigma));
                if (Math.abs(lambda - lambdaP) <= 1.0E-12d) {
                    break;
                }
                iterLimit -= 1.0d;
            } else {
                return 0.0d;
            }
        } while (iterLimit > 0.0d);
        if (iterLimit == 0.0d) {
            return -1.0d;
        }
        double uSq = (((UTMCoordConverter.WGS84_A * UTMCoordConverter.WGS84_A) - (6356752.314245d * 6356752.314245d)) * cosSqAlpha) / (6356752.314245d * 6356752.314245d);
        double B = (uSq / 1024.0d) * (256.0d + ((-128.0d + ((74.0d - (47.0d * uSq)) * uSq)) * uSq));
        return (6356752.314245d * (1.0d + ((uSq / 16384.0d) * (4096.0d + ((-768.0d + ((320.0d - (175.0d * uSq)) * uSq)) * uSq))))) * (sigma - ((B * sinSigma) * (((B / 4.0d) * (((-1.0d + ((2.0d * cos2SigmaM) * cos2SigmaM)) * cosSigma) - ((((B / 6.0d) * cos2SigmaM) * (-3.0d + ((4.0d * sinSigma) * sinSigma))) * (-3.0d + ((4.0d * cos2SigmaM) * cos2SigmaM))))) + cos2SigmaM)));
    }

    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double deltaLong = Math.toRadians(lon2 - lon1);
        double rlat1 = Math.toRadians(lat1);
        double rlat2 = Math.toRadians(lat2);
        return (360.0d + Math.toDegrees(Math.atan2(Math.sin(deltaLong) * Math.cos(rlat2), (Math.cos(rlat1) * Math.sin(rlat2)) - ((Math.sin(rlat1) * Math.cos(rlat2)) * Math.cos(deltaLong))))) % 360.0d;
    }

    public static double[] projection(double lat, double lon, double distance, double bearing) {
        double cos2SigmaM;
        double sinSigma;
        double cosSigma;
        double s = distance;
        double alpha1 = Math.toRadians(bearing);
        double sinAlpha1 = Math.sin(alpha1);
        double cosAlpha1 = Math.cos(alpha1);
        double tanU1 = (1.0d - UTMCoordConverter.WGS84_F) * Math.tan(Math.toRadians(lat));
        double cosU1 = 1.0d / Math.sqrt(1.0d + (tanU1 * tanU1));
        double sinU1 = tanU1 * cosU1;
        double sigma1 = Math.atan2(tanU1, cosAlpha1);
        double sinAlpha = cosU1 * sinAlpha1;
        double cosSqAlpha = 1.0d - (sinAlpha * sinAlpha);
        double uSq = (((UTMCoordConverter.WGS84_A * UTMCoordConverter.WGS84_A) - (6356752.3142d * 6356752.3142d)) * cosSqAlpha) / (6356752.3142d * 6356752.3142d);
        double A = 1.0d + ((uSq / 16384.0d) * (4096.0d + ((-768.0d + ((320.0d - (175.0d * uSq)) * uSq)) * uSq)));
        double B = (uSq / 1024.0d) * (256.0d + ((-128.0d + ((74.0d - (47.0d * uSq)) * uSq)) * uSq));
        double sigma = s / (6356752.3142d * A);
        double iterLimit = 100.0d;
        do {
            cos2SigmaM = Math.cos((2.0d * sigma1) + sigma);
            sinSigma = Math.sin(sigma);
            cosSigma = Math.cos(sigma);
            double sigmaP = sigma;
            sigma = (s / (6356752.3142d * A)) + ((B * sinSigma) * (((B / 4.0d) * (((-1.0d + ((2.0d * cos2SigmaM) * cos2SigmaM)) * cosSigma) - ((((B / 6.0d) * cos2SigmaM) * (-3.0d + ((4.0d * sinSigma) * sinSigma))) * (-3.0d + ((4.0d * cos2SigmaM) * cos2SigmaM))))) + cos2SigmaM));
            if (Math.abs(sigma - sigmaP) <= 1.0E-12d) {
                break;
            }
            iterLimit -= 1.0d;
        } while (iterLimit > 0.0d);
        double tmp = (sinU1 * sinSigma) - ((cosU1 * cosSigma) * cosAlpha1);
        double lat2 = Math.atan2((sinU1 * cosSigma) + ((cosU1 * sinSigma) * cosAlpha1), (1.0d - UTMCoordConverter.WGS84_F) * Math.sqrt((sinAlpha * sinAlpha) + (tmp * tmp)));
        double C = ((UTMCoordConverter.WGS84_F / 16.0d) * cosSqAlpha) * (4.0d + ((4.0d - (3.0d * cosSqAlpha)) * UTMCoordConverter.WGS84_F));
        double L = Math.atan2(sinSigma * sinAlpha1, (cosU1 * cosSigma) - ((sinU1 * sinSigma) * cosAlpha1)) - ((((1.0d - C) * UTMCoordConverter.WGS84_F) * sinAlpha) * (((C * sinSigma) * (((C * cosSigma) * (-1.0d + ((2.0d * cos2SigmaM) * cos2SigmaM))) + cos2SigmaM)) + sigma));
        return new double[]{Math.toDegrees(lat2), Math.toDegrees(L) + lon};
    }

    public static double vmg(double speed, double turn) {
        return Math.cos(Math.toRadians(turn)) * speed;
    }

    public static double xtk(double distance, double dtk, double bearing) {
        double dte = 0.0d;
        double dtesign = 1.0d;
        if (bearing > dtk) {
            dte = bearing - dtk;
        } else if (bearing < dtk) {
            dte = dtk - bearing;
            dtesign = -1.0d;
        }
        if (dte > 180.0d) {
            dte = 360.0d - dte;
            dtesign *= -1.0d;
        }
        if (dte > GeoPointUtils.LATITUDE_MAX) {
            return Double.NEGATIVE_INFINITY;
        }
        return (Math.sin(Math.toRadians(dte)) * distance) * dtesign;
    }

    public static double turn(double deg1, double deg2) {
        double deg = 0.0d;
        double degsign = 1.0d;
        if (deg2 > deg1) {
            deg = deg2 - deg1;
        } else if (deg2 < deg1) {
            deg = deg1 - deg2;
            degsign = -1.0d;
        }
        if (deg > 180.0d) {
            deg = 360.0d - deg;
            degsign *= -1.0d;
        }
        return deg * degsign;
    }
}
