package mobi.maptrek.location;

import android.location.Location;
import mobi.maptrek.data.Track;
import mobi.maptrek.util.ProgressListener;

public interface ILocationService {
    void clearTrack();

    float getHDOP();

    Location getLocation();

    int getSatellites();

    int getStatus();

    Track getTrack();

    Track getTrack(long j, long j2);

    long getTrackEndTime();

    long getTrackStartTime();

    float getVDOP();

    boolean isLocating();

    boolean isTracking();

    void registerLocationCallback(ILocationListener iLocationListener);

    void registerTrackingCallback(ITrackingListener iTrackingListener);

    void saveTrack();

    void setProgressListener(ProgressListener progressListener);

    void unregisterLocationCallback(ILocationListener iLocationListener);

    void unregisterTrackingCallback(ITrackingListener iTrackingListener);
}
