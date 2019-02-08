package mobi.maptrek.io.gpx;

import android.support.annotation.Nullable;
import android.util.Xml;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Track.TrackPoint;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.util.ProgressListener;
import org.xmlpull.v1.XmlSerializer;

public class GpxSerializer {
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
        serializer.setPrefix("", GpxFile.NS);
        serializer.startTag(GpxFile.NS, GpxFile.TAG_GPX);
        serializer.attribute("", GpxFile.ATTRIBUTE_CREATOR, "MapTrek http://maptrek.mobi");
        serializer.startTag(GpxFile.NS, GpxFile.TAG_METADATA);
        serializer.startTag(GpxFile.NS, "name");
        serializer.text(source.name);
        serializer.endTag(GpxFile.NS, "name");
        serializer.endTag(GpxFile.NS, GpxFile.TAG_METADATA);
        for (Waypoint waypoint : source.waypoints) {
            progress = serializeWaypoint(serializer, waypoint, progressListener, progress);
        }
        for (Track track2 : source.tracks) {
            progress = serializeTrack(serializer, track2, progressListener, progress);
        }
        serializer.endTag(GpxFile.NS, GpxFile.TAG_GPX);
        serializer.endDocument();
        serializer.flush();
        writer.close();
        if (progressListener != null) {
            progressListener.onProgressFinished();
        }
    }

    private static int serializeWaypoint(XmlSerializer serializer, Waypoint waypoint, ProgressListener progressListener, int progress) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(GpxFile.NS, GpxFile.TAG_WPT);
        serializer.attribute("", "lat", String.valueOf(waypoint.coordinates.getLatitude()));
        serializer.attribute("", "lon", String.valueOf(waypoint.coordinates.getLongitude()));
        serializer.startTag(GpxFile.NS, "name");
        serializer.text(waypoint.name);
        serializer.endTag(GpxFile.NS, "name");
        if (waypoint.description != null) {
            serializer.startTag(GpxFile.NS, GpxFile.TAG_DESC);
            serializer.cdsect(waypoint.description);
            serializer.endTag(GpxFile.NS, GpxFile.TAG_DESC);
        }
        if (waypoint.altitude != Integer.MIN_VALUE) {
            serializer.startTag(GpxFile.NS, "ele");
            serializer.text(String.valueOf(waypoint.altitude));
            serializer.endTag(GpxFile.NS, "ele");
        }
        if (waypoint.date != null) {
            serializer.startTag(GpxFile.NS, GpxFile.TAG_TIME);
            serializer.text(String.valueOf(GpxFile.formatTime(waypoint.date)));
            serializer.endTag(GpxFile.NS, GpxFile.TAG_TIME);
        }
        serializer.endTag(GpxFile.NS, GpxFile.TAG_WPT);
        progress++;
        if (progressListener != null) {
            progressListener.onProgressChanged(progress);
        }
        return progress;
    }

    private static int serializeTrack(XmlSerializer serializer, Track track, ProgressListener progressListener, int progress) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(GpxFile.NS, GpxFile.TAG_TRK);
        serializer.startTag(GpxFile.NS, "name");
        serializer.text(track.name);
        serializer.endTag(GpxFile.NS, "name");
        if (track.description != null) {
            serializer.startTag(GpxFile.NS, GpxFile.TAG_DESC);
            serializer.cdsect(track.description);
            serializer.endTag(GpxFile.NS, GpxFile.TAG_DESC);
        }
        serializer.startTag(GpxFile.NS, GpxFile.TAG_TRKSEG);
        boolean first = true;
        for (TrackPoint tp : track.points) {
            if (!(tp.continuous || first)) {
                serializer.endTag(GpxFile.NS, GpxFile.TAG_TRKSEG);
                serializer.startTag(GpxFile.NS, GpxFile.TAG_TRKSEG);
            }
            serializer.startTag(GpxFile.NS, GpxFile.TAG_TRKPT);
            serializer.attribute("", "lat", String.valueOf(((double) tp.latitudeE6) / 1000000.0d));
            serializer.attribute("", "lon", String.valueOf(((double) tp.longitudeE6) / 1000000.0d));
            if (tp.elevation != Float.NaN) {
                serializer.startTag(GpxFile.NS, "ele");
                serializer.text(String.valueOf(tp.elevation));
                serializer.endTag(GpxFile.NS, "ele");
            }
            if (tp.time > 0) {
                serializer.startTag(GpxFile.NS, GpxFile.TAG_TIME);
                serializer.text(GpxFile.formatTime(new Date(tp.time)));
                serializer.endTag(GpxFile.NS, GpxFile.TAG_TIME);
            }
            serializer.endTag(GpxFile.NS, GpxFile.TAG_TRKPT);
            first = false;
            progress++;
            if (progressListener != null) {
                progressListener.onProgressChanged(progress);
            }
        }
        serializer.endTag(GpxFile.NS, GpxFile.TAG_TRKSEG);
        serializer.endTag(GpxFile.NS, GpxFile.TAG_TRK);
        return progress;
    }
}
