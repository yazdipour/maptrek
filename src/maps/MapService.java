package mobi.maptrek.maps;

import android.app.IntentService;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.media.TransportMediator;
import mobi.maptrek.MainActivity;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapService extends IntentService {
    public static final String BROADCAST_MAP_ADDED = "mobi.maptrek.MapAdded";
    public static final String BROADCAST_MAP_REMOVED = "mobi.maptrek.MapRemoved";
    public static final String EXTRA_X = "x";
    public static final String EXTRA_Y = "y";
    private static final Logger logger = LoggerFactory.getLogger(MapService.class);

    private class OperationProgressListener implements ProgressListener {
        private final Builder builder;
        private final NotificationManager notificationManager;
        int progress = 0;
        float step = 0.0f;

        OperationProgressListener(NotificationManager notificationManager, Builder builder) {
            this.notificationManager = notificationManager;
            this.builder = builder;
        }

        public void onProgressStarted(int length) {
            this.step = ((float) length) / 100.0f;
            if (this.step != 0.0f) {
                this.builder.setContentText(MapService.this.getString(R.string.processed, new Object[]{Integer.valueOf(0)})).setProgress(100, 0, false);
                this.notificationManager.notify(0, this.builder.build());
            }
        }

        public void onProgressChanged(int progress) {
            if (this.step != 0.0f) {
                int percent = (int) (((float) progress) / this.step);
                if (percent > this.progress) {
                    this.progress = percent;
                    this.builder.setContentText(MapService.this.getString(R.string.processed, new Object[]{Integer.valueOf(this.progress)})).setProgress(100, this.progress, false);
                    this.notificationManager.notify(0, this.builder.build());
                }
            }
        }

        public void onProgressFinished() {
            if (this.step != 0.0f) {
                this.builder.setProgress(0, 0, false);
                this.notificationManager.notify(0, this.builder.build());
            }
        }

        public void onProgressAnnotated(String annotation) {
        }
    }

    public MapService() {
        super("ImportService");
    }

    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            boolean actionImport = "android.intent.action.INSERT".equals(intent.getAction());
            boolean actionRemoval = "android.intent.action.DELETE".equals(intent.getAction());
            Intent launchIntent = new Intent("android.intent.action.MAIN");
            launchIntent.addCategory("android.intent.category.LAUNCHER");
            launchIntent.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
            launchIntent.setFlags(270532608);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 268435456);
            NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
            Builder builder = new Builder(this);
            builder.setContentTitle(getString(actionImport ? R.string.title_map_import : R.string.title_map_removal)).setSmallIcon(R.drawable.ic_import_export).setGroup("maptrek").setCategory("service").setPriority(-1).setVisibility(1).setColor(getResources().getColor(R.color.colorAccent, getTheme()));
            notificationManager.notify(0, builder.build());
            MapTrek application = MapTrek.getApplication();
            Index mapIndex = application.getMapIndex();
            if (actionImport) {
                int x;
                int y;
                Uri uri = intent.getData();
                String filename = uri.getLastPathSegment();
                boolean hillshade = false;
                if (Index.BASEMAP_FILENAME.equals(filename)) {
                    x = -1;
                    y = -1;
                    builder.setContentTitle(getString(R.string.baseMapTitle));
                } else {
                    String[] parts = filename.split("[\\-\\.]");
                    try {
                        if (parts.length != 3) {
                            throw new NumberFormatException("unexpected name");
                        }
                        x = Integer.valueOf(parts[0]).intValue();
                        y = Integer.valueOf(parts[1]).intValue();
                        hillshade = "mbtiles".equals(parts[2]);
                        if (x > TransportMediator.KEYCODE_MEDIA_PAUSE || y > TransportMediator.KEYCODE_MEDIA_PAUSE) {
                            throw new NumberFormatException("out of range");
                        }
                        builder.setContentTitle(getString(hillshade ? R.string.hillshadeTitle : R.string.mapTitle, new Object[]{Integer.valueOf(x), Integer.valueOf(y)}));
                    } catch (NumberFormatException e) {
                        logger.error(e.getMessage());
                        builder.setContentIntent(pendingIntent);
                        builder.setContentText(getString(R.string.failed)).setProgress(0, 0, false);
                        notificationManager.notify(0, builder.build());
                        return;
                    }
                }
                notificationManager.notify(0, builder.build());
                if (processDownload(mapIndex, x, y, hillshade, uri.getPath(), new OperationProgressListener(notificationManager, builder))) {
                    application.sendBroadcast(new Intent(BROADCAST_MAP_ADDED).putExtra(EXTRA_X, x).putExtra(EXTRA_Y, y));
                    builder.setContentText(getString(R.string.complete));
                    notificationManager.notify(0, builder.build());
                    notificationManager.cancel(0);
                } else {
                    builder.setContentIntent(pendingIntent);
                    builder.setContentText(getString(R.string.failed)).setProgress(0, 0, false);
                    notificationManager.notify(0, builder.build());
                }
            }
            if (actionRemoval) {
                mapIndex.removeNativeMap(intent.getIntExtra(EXTRA_X, -1), intent.getIntExtra(EXTRA_Y, -1), new OperationProgressListener(notificationManager, builder));
                application.sendBroadcast(new Intent(BROADCAST_MAP_REMOVED).putExtras(intent));
                notificationManager.cancel(0);
            }
        }
    }

    private boolean processDownload(Index mapIndex, int x, int y, boolean hillshade, String path, OperationProgressListener progressListener) {
        if (hillshade) {
            return mapIndex.processDownloadedHillshade(x, y, path, progressListener);
        }
        return mapIndex.processDownloadedMap(x, y, path, progressListener);
    }
}
