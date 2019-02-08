package mobi.maptrek.io.kml;

import android.support.annotation.Nullable;
import android.util.Xml;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Track.TrackPoint;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.data.style.Style;
import mobi.maptrek.data.style.TrackStyle;
import mobi.maptrek.util.ProgressListener;
import org.xmlpull.v1.XmlSerializer;

public class KmlSerializer {
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

    public static void serialize(OutputStream outputStream, FileDataSource source, @Nullable ProgressListener progressListener) throws IOException {
        int progress = 0;
        if (progressListener != null) {
            int size = source.waypoints.size();
            for (Track track : source.tracks) {
                size += track.points.size();
            }
            progressListener.onProgressStarted(size);
        }
        XmlSerializer serializer = Xml.newSerializer();
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        serializer.setOutput(writer);
        serializer.startDocument("UTF-8", null);
        serializer.setPrefix("", KmlFile.NS);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_KML);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_DOCUMENT);
        if (source.tracks.size() > 0) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_FOLDER);
            serializer.startTag(KmlFile.NS, "name");
            serializer.text("Points");
            serializer.endTag(KmlFile.NS, "name");
            serializer.startTag(KmlFile.NS, KmlFile.TAG_OPEN);
            serializer.text("1");
            serializer.endTag(KmlFile.NS, KmlFile.TAG_OPEN);
        }
        for (Waypoint waypoint : source.waypoints) {
            progress = serializeWaypoint(serializer, waypoint, progressListener, progress);
        }
        if (source.tracks.size() > 0) {
            serializer.endTag(KmlFile.NS, KmlFile.TAG_FOLDER);
        }
        for (Track track2 : source.tracks) {
            progress = serializeTrack(serializer, track2, progressListener, progress);
        }
        serializer.endTag(KmlFile.NS, KmlFile.TAG_DOCUMENT);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_KML);
        serializer.endDocument();
        serializer.flush();
        writer.close();
        if (progressListener != null) {
            progressListener.onProgressFinished();
        }
    }

    private static int serializeWaypoint(XmlSerializer serializer, Waypoint waypoint, ProgressListener progressListener, int progress) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(KmlFile.NS, KmlFile.TAG_PLACEMARK);
        serializer.startTag(KmlFile.NS, "name");
        serializer.text(waypoint.name);
        serializer.endTag(KmlFile.NS, "name");
        if (waypoint.description != null) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
            serializer.cdsect(waypoint.description);
            serializer.endTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
        }
        if (!waypoint.style.isDefault()) {
            serializeStyle(serializer, waypoint.style);
        }
        serializer.startTag(KmlFile.NS, KmlFile.TAG_POINT);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_COORDINATES);
        serializer.text(String.valueOf(waypoint.coordinates.getLongitude()));
        serializer.text(",");
        serializer.text(String.valueOf(waypoint.coordinates.getLatitude()));
        if (waypoint.altitude != Integer.MIN_VALUE) {
            serializer.text(",");
            serializer.text(String.valueOf(waypoint.altitude));
        }
        serializer.endTag(KmlFile.NS, KmlFile.TAG_COORDINATES);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_POINT);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_PLACEMARK);
        progress++;
        if (progressListener != null) {
            progressListener.onProgressChanged(progress);
        }
        return progress;
    }

    private static int serializeTrack(XmlSerializer serializer, Track track, ProgressListener progressListener, int progress) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(KmlFile.NS, KmlFile.TAG_FOLDER);
        serializer.startTag(KmlFile.NS, "name");
        serializer.text(track.name);
        serializer.endTag(KmlFile.NS, "name");
        if (track.description != null) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
            serializer.cdsect(track.description);
            serializer.endTag(KmlFile.NS, KmlFile.TAG_DESCRIPTION);
        }
        serializer.startTag(KmlFile.NS, KmlFile.TAG_TIME_SPAN);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_BEGIN);
        serializer.text(sdf.format(new Date(((TrackPoint) track.points.get(0)).time)));
        serializer.endTag(KmlFile.NS, KmlFile.TAG_BEGIN);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_END);
        serializer.text(sdf.format(new Date(track.getLastPoint().time)));
        serializer.endTag(KmlFile.NS, KmlFile.TAG_END);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_TIME_SPAN);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_OPEN);
        serializer.text("0");
        serializer.endTag(KmlFile.NS, KmlFile.TAG_OPEN);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_STYLE);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_LIST_STYLE);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_LIST_ITEM_TYPE);
        serializer.text("checkHideChildren");
        serializer.endTag(KmlFile.NS, KmlFile.TAG_LIST_ITEM_TYPE);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_LIST_STYLE);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_STYLE);
        int part = 1;
        boolean first = true;
        startTrackPart(serializer, 1, track.name, track.style);
        for (TrackPoint point : track.points) {
            if (!first) {
                if (point.continuous) {
                    serializer.text(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR);
                } else {
                    stopTrackPart(serializer);
                    part++;
                    startTrackPart(serializer, part, track.name, track.style);
                }
            }
            serializer.text(String.valueOf(((double) point.longitudeE6) / 1000000.0d));
            serializer.text(",");
            serializer.text(String.valueOf(((double) point.latitudeE6) / 1000000.0d));
            if (point.elevation != Float.NaN) {
                serializer.text(",");
                serializer.text(String.valueOf(point.elevation));
            }
            first = false;
            progress++;
            if (progressListener != null) {
                progressListener.onProgressChanged(progress);
            }
        }
        stopTrackPart(serializer);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_FOLDER);
        return progress;
    }

    private static void startTrackPart(XmlSerializer serializer, int part, String name, Style style) throws IOException {
        serializer.startTag(KmlFile.NS, KmlFile.TAG_PLACEMARK);
        serializer.startTag(KmlFile.NS, "name");
        if (part > 1) {
            serializer.text(String.format(Locale.US, "%s #%d", new Object[]{name, Integer.valueOf(part)}));
        } else {
            serializer.text(name);
        }
        serializer.endTag(KmlFile.NS, "name");
        if (!style.isDefault()) {
            serializeStyle(serializer, style);
        }
        serializer.startTag(KmlFile.NS, KmlFile.TAG_LINE_STRING);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_TESSELLATE);
        serializer.text("1");
        serializer.endTag(KmlFile.NS, KmlFile.TAG_TESSELLATE);
        serializer.startTag(KmlFile.NS, KmlFile.TAG_COORDINATES);
    }

    private static void stopTrackPart(XmlSerializer serializer) throws IOException {
        serializer.endTag(KmlFile.NS, KmlFile.TAG_COORDINATES);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_LINE_STRING);
        serializer.endTag(KmlFile.NS, KmlFile.TAG_PLACEMARK);
    }

    private static void serializeStyle(XmlSerializer serializer, Style style) throws IOException {
        serializer.startTag(KmlFile.NS, KmlFile.TAG_STYLE);
        if (!(style.id == null || "".equals(style.id))) {
            serializer.attribute("", "id", style.id);
        }
        if (style instanceof MarkerStyle) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_ICON_STYLE);
            serializer.startTag(KmlFile.NS, "color");
            serializer.text(String.format("%08X", new Object[]{Integer.valueOf(KmlFile.reverseColor(((MarkerStyle) style).color))}));
            serializer.endTag(KmlFile.NS, "color");
            serializer.endTag(KmlFile.NS, KmlFile.TAG_ICON_STYLE);
        } else if (style instanceof TrackStyle) {
            serializer.startTag(KmlFile.NS, KmlFile.TAG_LINE_STYLE);
            serializer.startTag(KmlFile.NS, "color");
            serializer.text(String.format("%08X", new Object[]{Integer.valueOf(KmlFile.reverseColor(((TrackStyle) style).color))}));
            serializer.endTag(KmlFile.NS, "color");
            serializer.startTag(KmlFile.NS, KmlFile.TAG_WIDTH);
            serializer.text(String.valueOf(((TrackStyle) style).width));
            serializer.endTag(KmlFile.NS, KmlFile.TAG_WIDTH);
            serializer.endTag(KmlFile.NS, KmlFile.TAG_LINE_STYLE);
        }
        serializer.endTag(KmlFile.NS, KmlFile.TAG_STYLE);
    }
}
