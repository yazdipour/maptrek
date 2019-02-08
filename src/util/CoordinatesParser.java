package mobi.maptrek.util;

import android.support.annotation.NonNull;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.coords.MGRSCoord;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import java.util.ArrayList;
import java.util.List;
import org.oscim.core.GeoPoint;

public class CoordinatesParser {

    public static class Result {
        public GeoPoint coordinates;
        public List<Token> tokens;
    }

    public static class Token {
        public final String c;
        public final int i;
        public final int l;
        public final Type t;

        Token(Type t, String c, int i, int l) {
            this.t = t;
            this.c = c;
            this.i = i;
            this.l = l;
        }

        public boolean equals(Object o) {
            return (o instanceof Token) && this.t == ((Token) o).t && this.c.equals(((Token) o).c);
        }

        public String toString() {
            if (this.t == Type.H_PREFIX) {
                return "HPREF<" + this.c + ">[" + this.i + "," + this.l + "]";
            }
            if (this.t == Type.H_SUFFIX) {
                return "HSUFF<" + this.c + ">[" + this.i + "," + this.l + "]";
            }
            if (this.t == Type.DEG) {
                return "DEG<" + this.c + ">[" + this.i + "," + this.l + "]";
            }
            if (this.t == Type.MIN) {
                return "MIN<" + this.c + ">[" + this.i + "," + this.l + "]";
            }
            if (this.t == Type.SEC) {
                return "SEC<" + this.c + ">[" + this.i + "," + this.l + "]";
            }
            if (this.t == Type.UTM_ZONE) {
                return "UTM_ZONE<" + this.c + ">[" + this.i + "," + this.l + "]";
            }
            if (this.t == Type.UTM_EASTING) {
                return "UTM_EASTING<" + this.c + ">[" + this.i + "," + this.l + "]";
            }
            if (this.t == Type.UTM_NORTHING) {
                return "UTM_NORTHING<" + this.c + ">[" + this.i + "," + this.l + "]";
            }
            if (this.t == Type.MGRS) {
                return "MGRS<" + this.c + ">[" + this.i + "," + this.l + "]";
            }
            return this.t.toString() + "[" + this.i + "," + this.l + "]";
        }
    }

    enum Type {
        H_PREFIX,
        H_SUFFIX,
        DEG,
        MIN,
        SEC,
        UTM_ZONE,
        UTM_EASTING,
        UTM_NORTHING,
        MGRS
    }

    @NonNull
    static List<Token> lex(@NonNull String input) throws IllegalArgumentException {
        List<Token> result = new ArrayList();
        StringBuilder atom = null;
        int i = 0;
        int len = input.length();
        boolean before = true;
        while (i < len) {
            char c = input.charAt(i);
            if (!Character.isDigit(c) && c != '.') {
                if (atom != null && c >= 'C' && c <= 'X') {
                    atom.append(c);
                    lexUtmOrGprs(result, atom, input, len, i);
                    atom = null;
                    break;
                } else if (c == 'N' || c == 'E' || c == '+') {
                    result.add(new Token(before ? Type.H_PREFIX : Type.H_SUFFIX, String.valueOf(c), i, 1));
                } else if (c == 'S' || c == 'W' || c == '-') {
                    result.add(new Token(before ? Type.H_PREFIX : Type.H_SUFFIX, String.valueOf(c), i, 1));
                } else if (Character.isWhitespace(c)) {
                    if (atom != null) {
                        result.add(new Token(Type.DEG, atom.toString(), i - atom.length(), atom.length()));
                    }
                    atom = null;
                    before = true;
                } else if (c == '°') {
                    if (atom != null) {
                        result.add(new Token(Type.DEG, atom.toString(), i - atom.length(), atom.length() + 1));
                    }
                    atom = null;
                } else if (c == '\'') {
                    if (atom != null) {
                        result.add(new Token(Type.MIN, atom.toString(), i - atom.length(), atom.length() + 1));
                    }
                    atom = null;
                } else if (c == '\"') {
                    if (atom != null) {
                        result.add(new Token(Type.SEC, atom.toString(), i - atom.length(), atom.length() + 1));
                    }
                    atom = null;
                }
            } else {
                if (atom == null) {
                    atom = new StringBuilder(1).append(c);
                } else {
                    atom.append(c);
                }
                before = false;
            }
            i++;
        }
        if (atom != null) {
            result.add(new Token(Type.DEG, atom.toString(), i - atom.length(), atom.length()));
        }
        return result;
    }

    private static void lexUtmOrGprs(List<Token> result, StringBuilder atom, String input, int len, int start) throws IllegalArgumentException {
        int i = start + 1;
        Type type = null;
        int si = 0;
        int ws = 0;
        StringBuilder buffer = new StringBuilder();
        Token zone = new Token(Type.UTM_ZONE, atom.toString(), i - atom.length(), atom.length());
        Token easting = null;
        while (i < len) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                if (ws == 2 && type != Type.MGRS) {
                    throw new IllegalArgumentException("Not a UTM coordinate");
                } else if (ws == 3) {
                    throw new IllegalArgumentException("Not a MGRS coordinate");
                } else {
                    if (type == Type.UTM_EASTING) {
                        easting = new Token(Type.UTM_EASTING, buffer.toString(), i - buffer.length(), buffer.length());
                        buffer.setLength(0);
                        type = Type.UTM_NORTHING;
                    }
                    if (type == null) {
                        type = Type.UTM_EASTING;
                    }
                    ws++;
                }
            } else if ('A' <= c && c <= 'Z') {
                if (si == 0) {
                    type = Type.MGRS;
                }
                if (si == 2) {
                    throw new IllegalArgumentException("Not a MGRS coordinate");
                }
                atom.append(c);
                si++;
            } else if (Character.isDigit(c)) {
                atom.append(c);
                buffer.append(c);
            }
            i++;
        }
        if (type == Type.MGRS) {
            result.add(new Token(type, atom.toString(), start - 2, atom.length() + ws));
            return;
        }
        result.add(zone);
        if (easting != null) {
            result.add(easting);
        }
        if (buffer.length() > 0) {
            result.add(new Token(Type.UTM_NORTHING, buffer.toString(), i - buffer.length(), buffer.length()));
        }
    }

    @NonNull
    public static Result parseWithResult(@NonNull String input) throws IllegalArgumentException {
        Result result = new Result();
        result.tokens = lex(input);
        if (result.tokens.size() == 0) {
            throw new IllegalArgumentException("Wrong coordinates format");
        }
        switch (((Token) result.tokens.get(0)).t) {
            case MGRS:
                MGRSCoord coord = MGRSCoord.fromString(((Token) result.tokens.get(0)).c);
                result.coordinates = new GeoPoint(coord.getLatitude().degrees, coord.getLongitude().degrees);
                break;
            case UTM_ZONE:
                result.coordinates = parseUtmTokens(result.tokens);
                break;
            default:
                double lat = Double.NaN;
                double lon = Double.NaN;
                double latSign = 1.0d;
                double lonSign = 1.0d;
                for (Token token : result.tokens) {
                    if (token.t == Type.H_PREFIX) {
                        if (Double.isNaN(lat)) {
                            latSign = ("-".equals(token.c) || "S".equals(token.c) || "W".equals(token.c)) ? -1.0d : 1.0d;
                        } else if (Double.isNaN(lon)) {
                            lonSign = ("-".equals(token.c) || "S".equals(token.c) || "W".equals(token.c)) ? -1.0d : 1.0d;
                        } else {
                            throw new IllegalArgumentException("Wrong coordinates format");
                        }
                    }
                    if (token.t == Type.H_SUFFIX) {
                        if (!Double.isNaN(lon)) {
                            lonSign = ("-".equals(token.c) || "S".equals(token.c) || "W".equals(token.c)) ? -1.0d : 1.0d;
                        } else if (Double.isNaN(lat)) {
                            throw new IllegalArgumentException("Wrong coordinates format");
                        } else {
                            latSign = ("-".equals(token.c) || "S".equals(token.c) || "W".equals(token.c)) ? -1.0d : 1.0d;
                        }
                    }
                    if (token.t == Type.DEG) {
                        if (Double.isNaN(lat)) {
                            lat = Double.valueOf(token.c).doubleValue();
                        } else {
                            lon = Double.valueOf(token.c).doubleValue();
                        }
                    }
                    if (token.t == Type.MIN) {
                        if (!Double.isNaN(lon)) {
                            lon += (Math.signum(lon) * Double.valueOf(token.c).doubleValue()) / 60.0d;
                        } else if (Double.isNaN(lat)) {
                            throw new IllegalArgumentException("Wrong coordinates format");
                        } else {
                            lat += (Math.signum(lat) * Double.valueOf(token.c).doubleValue()) / 60.0d;
                        }
                    }
                    if (token.t == Type.SEC) {
                        if (!Double.isNaN(lon)) {
                            lon += (Math.signum(lon) * Double.valueOf(token.c).doubleValue()) / 3600.0d;
                        } else if (Double.isNaN(lat)) {
                            throw new IllegalArgumentException("Wrong coordinates format");
                        } else {
                            lat += (Math.signum(lat) * Double.valueOf(token.c).doubleValue()) / 3600.0d;
                        }
                    }
                }
                if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                    result.coordinates = new GeoPoint(lat * latSign, lon * lonSign);
                    break;
                }
                throw new IllegalArgumentException("Wrong coordinates format");
                break;
        }
        return result;
    }

    @NonNull
    public static GeoPoint parse(@NonNull String input) throws IllegalArgumentException {
        return parseWithResult(input).coordinates;
    }

    @NonNull
    private static GeoPoint parseUtmTokens(List<Token> tokens) throws IllegalArgumentException {
        int zone = 0;
        String hemisphere = null;
        double easting = Double.NaN;
        double northing = Double.NaN;
        for (Token token : tokens) {
            if (token.t == Type.UTM_ZONE) {
                zone = Integer.valueOf(token.c.substring(0, token.c.length() - 1)).intValue();
                hemisphere = token.c.substring(token.c.length() - 1, token.c.length());
                if ("N".equals(hemisphere)) {
                    hemisphere = AVKey.NORTH;
                }
                if ("S".equals(hemisphere)) {
                    hemisphere = AVKey.SOUTH;
                }
            }
            if (token.t == Type.UTM_EASTING) {
                easting = Double.valueOf(token.c).doubleValue();
            }
            if (token.t == Type.UTM_NORTHING) {
                northing = Double.valueOf(token.c).doubleValue();
            }
        }
        if (zone == 0 || Double.isNaN(easting) || Double.isNaN(northing)) {
            throw new IllegalArgumentException("Wrong UTM coordinates format");
        }
        UTMCoord coord = UTMCoord.fromUTM(zone, hemisphere, easting, northing);
        return new GeoPoint(coord.getLatitude().degrees, coord.getLongitude().degrees);
    }
}
