package mobi.maptrek.io.kml;

import android.support.annotation.NonNull;
import android.util.Pair;
import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.io.kml.KmlFile.Folder;
import mobi.maptrek.io.kml.KmlFile.IconStyle;
import mobi.maptrek.io.kml.KmlFile.LineStyle;
import mobi.maptrek.io.kml.KmlFile.Placemark;
import mobi.maptrek.io.kml.KmlFile.Style;
import mobi.maptrek.io.kml.KmlFile.StyleMap;
import mobi.maptrek.io.kml.KmlFile.StyleType;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class KmlParser {
    private static final String NS = null;

    @NonNull
    public static FileDataSource parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", false);
            parser.setInput(in, null);
            parser.nextTag();
            FileDataSource readKml = readKml(parser);
            return readKml;
        } finally {
            in.close();
        }
    }

    @NonNull
    private static FileDataSource readKml(XmlPullParser parser) throws XmlPullParserException, IOException {
        FileDataSource dataSource = null;
        parser.require(2, NS, KmlFile.TAG_KML);
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                if (parser.getName().equals(KmlFile.TAG_DOCUMENT)) {
                    dataSource = readDocument(parser);
                } else {
                    skip(parser);
                }
            }
        }
        if (dataSource != null) {
            return dataSource;
        }
        throw new XmlPullParserException("No valid data", parser, null);
    }

    @NonNull
    private static FileDataSource readDocument(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, NS, KmlFile.TAG_DOCUMENT);
        FileDataSource dataSource = new FileDataSource();
        List<Folder> folders = new ArrayList();
        List<Placemark> placemarks = new ArrayList();
        HashMap<String, StyleType> styles = new HashMap();
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                int i = -1;
                switch (name.hashCode()) {
                    case -498064332:
                        if (name.equals(KmlFile.TAG_PLACEMARK)) {
                            i = 4;
                            break;
                        }
                        break;
                    case 3373707:
                        if (name.equals("name")) {
                            i = 0;
                            break;
                        }
                        break;
                    case 80227729:
                        if (name.equals(KmlFile.TAG_STYLE)) {
                            i = 1;
                            break;
                        }
                        break;
                    case 2062535179:
                        if (name.equals(KmlFile.TAG_STYLE_MAP)) {
                            i = 2;
                            break;
                        }
                        break;
                    case 2109868174:
                        if (name.equals(KmlFile.TAG_FOLDER)) {
                            i = 3;
                            break;
                        }
                        break;
                }
                switch (i) {
                    case 0:
                        dataSource.name = readTextElement(parser, "name");
                        break;
                    case 1:
                        Style style = readStyle(parser);
                        styles.put("#" + style.id, style);
                        break;
                    case 2:
                        StyleMap styleMap = readStyleMap(parser);
                        styles.put("#" + styleMap.id, styleMap);
                        break;
                    case 3:
                        folders.add(readFolder(parser));
                        break;
                    case 4:
                        placemarks.add(readPlacemark(parser));
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        if (dataSource.name == null && folders.size() == 1) {
            dataSource.name = ((Folder) folders.get(0)).name;
        }
        for (Placemark placemark : placemarks) {
            applyStyles(placemark, styles);
            if (placemark.point != null) {
                dataSource.waypoints.add(placemark.point);
            }
            if (placemark.track != null) {
                dataSource.tracks.add(placemark.track);
            }
        }
        for (Folder folder : folders) {
            for (Placemark placemark2 : folder.placemarks) {
                applyStyles(placemark2, styles);
                if (placemark2.point != null) {
                    dataSource.waypoints.add(placemark2.point);
                }
                if (placemark2.track != null) {
                    dataSource.tracks.add(placemark2.track);
                }
            }
        }
        return dataSource;
    }

    private static void applyStyles(Placemark placemark, Map<String, StyleType> styles) {
        StyleType styleType = placemark.style;
        if (placemark.styleUrl != null) {
            styleType = (StyleType) styles.get(placemark.styleUrl);
        }
        if (styleType instanceof StyleMap) {
            String url = (String) ((StyleMap) styleType).map.get("normal");
            if (url == null) {
                url = (String) ((StyleMap) styleType).map.values().iterator().next();
            }
            styleType = (StyleType) styles.get(url);
        }
        if (styleType != null) {
            Style style = (Style) styleType;
            if (!(style.iconStyle == null || placemark.point == null)) {
                placemark.point.style.color = style.iconStyle.color;
            }
            if (style.lineStyle != null && placemark.track != null) {
                placemark.track.style.color = style.lineStyle.color;
                placemark.track.style.width = style.lineStyle.width;
            }
        }
    }

    @NonNull
    private static Folder readFolder(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, NS, KmlFile.TAG_FOLDER);
        Folder folder = new Folder();
        folder.placemarks = new ArrayList();
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                Object obj = -1;
                switch (name.hashCode()) {
                    case -498064332:
                        if (name.equals(KmlFile.TAG_PLACEMARK)) {
                            obj = 1;
                            break;
                        }
                        break;
                    case 3373707:
                        if (name.equals("name")) {
                            obj = null;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        folder.name = readTextElement(parser, "name");
                        break;
                    case 1:
                        folder.placemarks.add(readPlacemark(parser));
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, KmlFile.TAG_FOLDER);
        return folder;
    }

    @NonNull
    private static Placemark readPlacemark(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, NS, KmlFile.TAG_PLACEMARK);
        Placemark placemark = new Placemark();
        String title = null;
        String description = null;
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                int i = -1;
                switch (name.hashCode()) {
                    case -1724546052:
                        if (name.equals(KmlFile.TAG_DESCRIPTION)) {
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
                    case 77292912:
                        if (name.equals(KmlFile.TAG_POINT)) {
                            i = 4;
                            break;
                        }
                        break;
                    case 80227729:
                        if (name.equals(KmlFile.TAG_STYLE)) {
                            i = 2;
                            break;
                        }
                        break;
                    case 1806700869:
                        if (name.equals(KmlFile.TAG_LINE_STRING)) {
                            i = 5;
                            break;
                        }
                        break;
                    case 1997899262:
                        if (name.equals(KmlFile.TAG_STYLE_URL)) {
                            i = 3;
                            break;
                        }
                        break;
                }
                switch (i) {
                    case 0:
                        title = readTextElement(parser, "name");
                        break;
                    case 1:
                        description = readTextElement(parser, KmlFile.TAG_DESCRIPTION).trim();
                        break;
                    case 2:
                        placemark.style = readStyle(parser);
                        break;
                    case 3:
                        placemark.styleUrl = readTextElement(parser, KmlFile.TAG_STYLE_URL);
                        break;
                    case 4:
                        placemark.point = readPoint(parser);
                        break;
                    case 5:
                        placemark.track = readLineString(parser);
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, KmlFile.TAG_PLACEMARK);
        if (placemark.point != null) {
            placemark.point.name = title;
            placemark.point.description = description;
        }
        if (placemark.track != null) {
            placemark.track.name = title;
            placemark.track.description = description;
        }
        return placemark;
    }

    @NonNull
    private static Waypoint readPoint(XmlPullParser parser) throws IOException, XmlPullParserException {
        RuntimeException e;
        parser.require(2, NS, KmlFile.TAG_POINT);
        String coordinatesString = null;
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                Object obj = -1;
                switch (name.hashCode()) {
                    case 1871919611:
                        if (name.equals(KmlFile.TAG_COORDINATES)) {
                            obj = null;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        coordinatesString = readTextElement(parser, KmlFile.TAG_COORDINATES);
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, KmlFile.TAG_POINT);
        if (coordinatesString == null) {
            throw new XmlPullParserException("Point must have coordinates", parser, null);
        }
        try {
            String[] coordinates = coordinatesString.split(",");
            Waypoint waypoint = new Waypoint(Double.parseDouble(coordinates[1]), Double.parseDouble(coordinates[0]));
            if (coordinates.length == 3) {
                double altitude = Double.parseDouble(coordinates[2]);
                if (altitude != 0.0d) {
                    waypoint.altitude = (int) altitude;
                }
            }
            waypoint.locked = true;
            return waypoint;
        } catch (NumberFormatException e2) {
            e = e2;
            throw new XmlPullParserException("Wrong coordinates format", parser, e);
        } catch (ArrayIndexOutOfBoundsException e3) {
            e = e3;
            throw new XmlPullParserException("Wrong coordinates format", parser, e);
        }
    }

    @NonNull
    private static Track readLineString(XmlPullParser parser) throws IOException, XmlPullParserException {
        RuntimeException e;
        parser.require(2, NS, KmlFile.TAG_LINE_STRING);
        Track track = new Track();
        String coordinatesString = null;
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                Object obj = -1;
                switch (name.hashCode()) {
                    case 1871919611:
                        if (name.equals(KmlFile.TAG_COORDINATES)) {
                            obj = null;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        coordinatesString = readTextElement(parser, KmlFile.TAG_COORDINATES);
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, KmlFile.TAG_LINE_STRING);
        if (coordinatesString == null) {
            throw new XmlPullParserException("LineString must have coordinates", parser, null);
        }
        boolean continuous = false;
        for (String point : coordinatesString.split("[\\s\\n]")) {
            String[] coordinates = point.split(",");
            if (coordinates.length >= 2) {
                try {
                    int latitudeE6 = (int) (Double.parseDouble(coordinates[1]) * 1000000.0d);
                    int longitudeE6 = (int) (Double.parseDouble(coordinates[0]) * 1000000.0d);
                    float altitude = 0.0f;
                    if (coordinates.length == 3) {
                        altitude = (float) Double.parseDouble(coordinates[2]);
                    }
                    track.addPointFast(continuous, latitudeE6, longitudeE6, altitude, Float.NaN, Float.NaN, Float.NaN, 0);
                    continuous = true;
                } catch (NumberFormatException e2) {
                    e = e2;
                } catch (ArrayIndexOutOfBoundsException e3) {
                    e = e3;
                }
            }
        }
        return track;
        throw new XmlPullParserException("Wrong coordinates format: " + point, parser, e);
    }

    @NonNull
    private static Style readStyle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, NS, KmlFile.TAG_STYLE);
        Style style = new Style();
        style.id = parser.getAttributeValue(null, "id");
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                Object obj = -1;
                switch (name.hashCode()) {
                    case 196834813:
                        if (name.equals(KmlFile.TAG_LINE_STYLE)) {
                            obj = 1;
                            break;
                        }
                        break;
                    case 602469528:
                        if (name.equals(KmlFile.TAG_ICON_STYLE)) {
                            obj = null;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        style.iconStyle = readIconStyle(parser);
                        break;
                    case 1:
                        style.lineStyle = readLineStyle(parser);
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, KmlFile.TAG_STYLE);
        return style;
    }

    @NonNull
    private static IconStyle readIconStyle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, NS, KmlFile.TAG_ICON_STYLE);
        IconStyle style = new IconStyle();
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                if (parser.getName().equals("color")) {
                    style.color = readColor(parser);
                } else {
                    skip(parser);
                }
            }
        }
        parser.require(3, NS, KmlFile.TAG_ICON_STYLE);
        return style;
    }

    @NonNull
    private static LineStyle readLineStyle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, NS, KmlFile.TAG_LINE_STYLE);
        LineStyle style = new LineStyle();
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                Object obj = -1;
                switch (name.hashCode()) {
                    case 94842723:
                        if (name.equals("color")) {
                            obj = null;
                            break;
                        }
                        break;
                    case 113126854:
                        if (name.equals(KmlFile.TAG_WIDTH)) {
                            obj = 1;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        style.color = readColor(parser);
                        break;
                    case 1:
                        style.width = readWidth(parser);
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, KmlFile.TAG_LINE_STYLE);
        return style;
    }

    @NonNull
    private static StyleMap readStyleMap(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, NS, KmlFile.TAG_STYLE_MAP);
        StyleMap styleMap = new StyleMap();
        styleMap.id = parser.getAttributeValue(null, "id");
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                Object obj = -1;
                switch (name.hashCode()) {
                    case 2479866:
                        if (name.equals(KmlFile.TAG_PAIR)) {
                            obj = null;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        Pair<String, String> pair = readPair(parser);
                        styleMap.map.put(pair.first, pair.second);
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, KmlFile.TAG_STYLE_MAP);
        return styleMap;
    }

    @NonNull
    private static Pair<String, String> readPair(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, NS, KmlFile.TAG_PAIR);
        String key = null;
        String url = null;
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                Object obj = -1;
                switch (name.hashCode()) {
                    case 106079:
                        if (name.equals(KmlFile.TAG_KEY)) {
                            obj = null;
                            break;
                        }
                        break;
                    case 1997899262:
                        if (name.equals(KmlFile.TAG_STYLE_URL)) {
                            obj = 1;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        key = readTextElement(parser, KmlFile.TAG_KEY);
                        break;
                    case 1:
                        url = readTextElement(parser, KmlFile.TAG_STYLE_URL);
                        break;
                    default:
                        skip(parser);
                        break;
                }
            }
        }
        parser.require(3, NS, KmlFile.TAG_PAIR);
        if (key != null && url != null) {
            return new Pair(key, url);
        }
        throw new XmlPullParserException("Pair should contain key and url", parser, null);
    }

    private static int readColor(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, NS, "color");
        String color = readText(parser);
        parser.require(3, NS, "color");
        return KmlFile.reverseColor((int) Long.parseLong(color, 16));
    }

    private static float readWidth(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, NS, KmlFile.TAG_WIDTH);
        float width = readFloat(parser);
        parser.require(3, NS, KmlFile.TAG_WIDTH);
        return width;
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

    @NonNull
    private static String readTextElement(XmlPullParser parser, String name) throws IOException, XmlPullParserException {
        parser.require(2, NS, name);
        String result = readText(parser);
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
