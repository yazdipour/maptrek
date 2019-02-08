package mobi.maptrek;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import mobi.maptrek.maps.MapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadReceiver extends BroadcastReceiver {
    private static final Logger logger = LoggerFactory.getLogger(DownloadReceiver.class);

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.DOWNLOAD_COMPLETE")) {
            long ref = intent.getLongExtra("extra_download_id", -1);
            DownloadManager downloadManager = (DownloadManager) context.getSystemService("download");
            Query query = new Query();
            query.setFilterById(new long[]{ref});
            Cursor cursor = downloadManager.query(query);
            if (cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndex("status")) == 8) {
                Object fileName = cursor.getString(cursor.getColumnIndex("local_uri"));
                Uri uri = Uri.parse(fileName);
                logger.debug("Downloaded: {}", fileName);
                context.startService(new Intent("android.intent.action.INSERT", uri, context, MapService.class));
            }
        }
    }
}
