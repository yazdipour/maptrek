package mobi.maptrek.io;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.InputStream;
import java.io.OutputStream;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.io.gpx.GpxParser;
import mobi.maptrek.io.gpx.GpxSerializer;
import mobi.maptrek.util.ProgressListener;

public class GPXManager extends Manager {
    public static final String EXTENSION = ".gpx";

    @NonNull
    public FileDataSource loadData(InputStream inputStream, String filePath) throws Exception {
        FileDataSource dataSource = GpxParser.parse(inputStream);
        int hash = filePath.hashCode() * 31;
        int i = 1;
        for (Waypoint waypoint : dataSource.waypoints) {
            waypoint._id = (long) (((waypoint.name.hashCode() + hash) * 31) + i);
            waypoint.source = dataSource;
            i++;
        }
        for (Track track : dataSource.tracks) {
            track.id = ((track.name.hashCode() + hash) * 31) + i;
            track.source = dataSource;
            i++;
        }
        return dataSource;
    }

    public void saveData(OutputStream outputStream, FileDataSource source, @Nullable ProgressListener progressListener) throws Exception {
        GpxSerializer.serialize(outputStream, source, progressListener);
    }

    @NonNull
    public String getExtension() {
        return EXTENSION;
    }
}
