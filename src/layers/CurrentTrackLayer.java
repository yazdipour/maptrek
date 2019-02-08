package mobi.maptrek.layers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Track.TrackPoint;
import mobi.maptrek.location.ILocationService;
import mobi.maptrek.location.ITrackingListener;
import mobi.maptrek.location.LocationService;
import org.oscim.backend.canvas.Color;
import org.oscim.map.Map;

public class CurrentTrackLayer extends TrackLayer {
    private boolean mBound;
    private Context mContext;
    private ServiceConnection mTrackingConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            CurrentTrackLayer.this.mTrackingService = (ILocationService) service;
            AsyncTask.execute(new Runnable() {
                public void run() {
                    CurrentTrackLayer.this.mTrack.copyFrom(CurrentTrackLayer.this.mTrackingService.getTrack());
                    CurrentTrackLayer.this.mTrackingService.registerTrackingCallback(CurrentTrackLayer.this.mTrackingListener);
                }
            });
        }

        public void onServiceDisconnected(ComponentName className) {
            CurrentTrackLayer.this.mTrackingService = null;
        }
    };
    private ITrackingListener mTrackingListener = new ITrackingListener() {
        public void onNewPoint(boolean continuous, double lat, double lon, float elev, float speed, float trk, float accuracy, long time) {
            if (CurrentTrackLayer.this.point != null) {
                CurrentTrackLayer.this.mTrack.addPoint(CurrentTrackLayer.this.point.continuous, CurrentTrackLayer.this.point.latitudeE6, CurrentTrackLayer.this.point.longitudeE6, CurrentTrackLayer.this.point.elevation, CurrentTrackLayer.this.point.speed, CurrentTrackLayer.this.point.bearing, CurrentTrackLayer.this.point.accuracy, CurrentTrackLayer.this.point.time);
                CurrentTrackLayer.this.updatePoints();
            }
            CurrentTrackLayer currentTrackLayer = CurrentTrackLayer.this;
            Track track = CurrentTrackLayer.this.mTrack;
            track.getClass();
            currentTrackLayer.point = new TrackPoint(continuous, (int) (1000000.0d * lat), (int) (1000000.0d * lon), elev, speed, trk, accuracy, time);
        }
    };
    private ILocationService mTrackingService;
    private TrackPoint point = null;

    public CurrentTrackLayer(Map map, Context context) {
        super(map, new Track());
        this.mContext = context;
        this.mBound = this.mContext.bindService(new Intent(this.mContext, LocationService.class), this.mTrackingConnection, 0);
        setColor(Color.fade(this.mLineStyle.color, 0.7d));
    }

    public void onDetach() {
        super.onDetach();
        unbind();
    }

    private void unbind() {
        if (this.mBound) {
            if (this.mTrackingService != null) {
                this.mTrackingService.unregisterTrackingCallback(this.mTrackingListener);
            }
            this.mContext.unbindService(this.mTrackingConnection);
            this.mBound = false;
        }
    }
}
