package mobi.maptrek.data.source;

import android.database.Cursor;
import java.util.List;
import mobi.maptrek.data.Track;

public interface TrackDataSource {
    Track cursorToTrack(Cursor cursor);

    List<Track> getTracks();

    int getTracksCount();
}
