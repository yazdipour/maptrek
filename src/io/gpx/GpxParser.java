package mobi.maptrek.io.gpx;

import android.support.annotation.NonNull;
import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.io.gpx.GpxFile.Metadata;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class GpxParser {
    private static final String NS = null;

    @NonNull
    public static FileDataSource parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", false);
            parser.setInput(in, null);
            parser.nextTag();
            FileDataSource readGpx = readGpx(parser);
            return readGpx;
        } finally {
            in.close();
        }
    }

    @NonNull
    private static FileDataSource readGpx(XmlPullParser parser) throws XmlPullParserException, IOException {
        FileDataSource dataSource = new FileDataSource();
        parser.require(2, NS, GpxFile.TAG_GPX);
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                int i = -1;
                switch (name.hashCode()) {
                    case -450004177:
                        if (name.equals(GpxFile.TAG_METADATA)) {
                            i = 0;
                            break;
                        }
                        break;
                    case 115117:
                        if (name.equals(GpxFile.TAG_TRK)) {
                            i = 2;
                            break;
                        }
                        break;
                    case 117947:
                        if (name.equals(GpxFile.TAG_WPT)) {
                            i = 1;
                            break;
                        }
                        break;
                }
                switch (i) {
                    case 0:
                        dataSource.name = readMetadata(parser).name;
                        break;
                    case 1:
                        dataSource.waypoints.add(readWaypoint(parser));
                        break;
                    case 2:
                        dataSource.tracks.add(readTrack(parser));
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        return dataSource;
    }

    @NonNull
    private static Metadata readMetadata(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, NS, GpxFile.TAG_METADATA);
        Metadata metadata = new Metadata();
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                Object obj = -1;
                switch (name.hashCode()) {
                    case 3373707:
                        if (name.equals("name")) {
                            obj = null;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        metadata.name = readTextElement(parser, "name");
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, GpxFile.TAG_METADATA);
        return metadata;
    }

    @NonNull
    private static Waypoint readWaypoint(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, NS, GpxFile.TAG_WPT);
        Waypoint waypoint = new Waypoint((double) Float.valueOf(parser.getAttributeValue(null, "lat")).floatValue(), (double) Float.valueOf(parser.getAttributeValue(null, "lon")).floatValue());
        waypoint.locked = true;
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                boolean z = true;
                switch (name.hashCode()) {
                    case 100510:
                        if (name.equals("ele")) {
                            z = true;
                            break;
                        }
                        break;
                    case 3079825:
                        if (name.equals(GpxFile.TAG_DESC)) {
                            z = true;
                            break;
                        }
                        break;
                    case 3373707:
                        if (name.equals("name")) {
                            z = false;
                            break;
                        }
                        break;
                    case 3560141:
                        if (name.equals(GpxFile.TAG_TIME)) {
                            z = true;
                            break;
                        }
                        break;
                }
                switch (z) {
                    case false:
                        waypoint.name = readTextElement(parser, "name");
                        break;
                    case true:
                        waypoint.description = readTextElement(parser, GpxFile.TAG_DESC);
                        break;
                    case true:
                        waypoint.altitude = (int) readFloatElement(parser, "ele");
                        break;
                    case true:
                        waypoint.date = new Date(readTime(parser));
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, GpxFile.TAG_WPT);
        return waypoint;
    }

    @NonNull
    private static Track readTrack(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, NS, GpxFile.TAG_TRK);
        Track track = new Track();
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                int i = -1;
                switch (name.hashCode()) {
                    case -865403000:
                        if (name.equals(GpxFile.TAG_TRKSEG)) {
                            i = 2;
                            break;
                        }
                        break;
                    case 3079825:
                        if (name.equals(GpxFile.TAG_DESC)) {
                            i = 1;
                            break;
                        }
                        break;
                    case 3373707:
                        if (name.equals("name")) {
                            i = 0;
                            break;
                        }
                        break;
                }
                switch (i) {
                    case 0:
                        track.name = readTextElement(parser, "name");
                        break;
                    case 1:
                        track.description = readTextElement(parser, GpxFile.TAG_DESC);
                        break;
                    case 2:
                        readTrackSegment(parser, track);
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, GpxFile.TAG_TRK);
        return track;
    }

    private static void readTrackSegment(XmlPullParser parser, Track track) throws XmlPullParserException, IOException {
        parser.require(2, NS, GpxFile.TAG_TRKSEG);
        boolean continuous = false;
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                Object obj = -1;
                switch (name.hashCode()) {
                    case 110631025:
                        if (name.equals(GpxFile.TAG_TRKPT)) {
                            obj = null;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        readTrackPoint(parser, track, continuous);
                        continuous = true;
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, GpxFile.TAG_TRKSEG);
    }

    private static void readTrackPoint(XmlPullParser parser, Track track, boolean continuous) throws XmlPullParserException, IOException {
        parser.require(2, NS, GpxFile.TAG_TRKPT);
        float lat = Float.valueOf(parser.getAttributeValue(null, "lat")).floatValue();
        float lon = Float.valueOf(parser.getAttributeValue(null, "lon")).floatValue();
        float altitude = Float.NaN;
        long time = 0;
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                Object obj = -1;
                switch (name.hashCode()) {
                    case 100510:
                        if (name.equals("ele")) {
                            obj = null;
                            break;
                        }
                        break;
                    case 3560141:
                        if (name.equals(GpxFile.TAG_TIME)) {
                            obj = 1;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        altitude = readFloatElement(parser, "ele");
                        break;
                    case 1:
                        time = readTime(parser);
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, GpxFile.TAG_TRKPT);
        track.addPointFast(continuous, (int) (((double) lat) * 1000000.0d), (int) (((double) lon) * 1000000.0d), altitude, Float.NaN, Float.NaN, Float.NaN, time);
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != 2) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case 2:
                    depth++;
                    break;
                case 3:
                    depth--;
                    break;
                default:
                    break;
            }
        }
    }

    private static long readTime(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, NS, GpxFile.TAG_TIME);
        String timeString = readText(parser);
        parser.require(3, NS, GpxFile.TAG_TIME);
        try {
            return GpxFile.parseTime(timeString);
        } catch (ParseException e) {
            throw new XmlPullParserException("Unexpected time format: " + timeString, parser, e);
        }
    }

    @NonNull
    private static String readTextElement(XmlPullParser parser, String name) throws IOException, XmlPullParserException {
        parser.require(2, NS, name);
        String result = readText(parser);
        parser.require(3, NS, name);
        return result;
    }

    private static float readFloatElement(XmlPullParser parser, String name) throws IOException, XmlPullParserException {
        parser.require(2, NS, name);
        float result = readFloat(parser);
        parser.require(3, NS, name);
        return result;
    }

    @NonNull
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() != 4) {
            return result;
        }
        result = parser.getText();
        parser.nextTag();
        return result;
    }

    private static float readFloat(XmlPullParser parser) throws IOException, XmlPullParserException {
        String text = "";
        if (parser.next() == 4) {
            text = parser.getText();
            parser.nextTag();
        }
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            throw new XmlPullParserException("Expected float", parser, e);
        }
    }
}
