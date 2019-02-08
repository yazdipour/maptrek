package mobi.maptrek.data.source;

import mobi.maptrek.io.TrackManager;

public class FileDataSource extends MemoryDataSource {
    public String path;
    public long propertiesOffset;

    public boolean isNativeTrack() {
        return this.path != null && this.path.endsWith(TrackManager.EXTENSION);
    }
}
