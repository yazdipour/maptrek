package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import at.grabner.circleprogress.CircleProgressView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.TrackDataSource;
import mobi.maptrek.data.source.WaypointDataSource;
import mobi.maptrek.io.GPXManager;
import mobi.maptrek.io.KMLManager;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.TrackManager;
import mobi.maptrek.provider.ExportProvider;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataExport extends DialogFragment implements ProgressListener {
    public static final int FORMAT_GPX = 1;
    public static final int FORMAT_KML = 2;
    public static final int FORMAT_NATIVE = 0;
    private static final Logger logger = LoggerFactory.getLogger(DataExport.class);
    private boolean mCanceled;
    private DataSource mDataSource;
    private int mFormat;
    private CircleProgressView mProgressView;
    private Track mTrack;

    public static class Builder {
        private DataSource mDataSource;
        private int mFormat;
        private Track mTrack;

        public Builder setDataSource(@NonNull DataSource dataSource) {
            this.mDataSource = dataSource;
            return this;
        }

        public Builder setTrack(@NonNull Track track) {
            this.mTrack = track;
            return this;
        }

        public Builder setFormat(int format) {
            this.mFormat = format;
            return this;
        }

        public DataExport create() {
            DataExport dialogFragment = new DataExport();
            if (this.mTrack != null) {
                dialogFragment.mTrack = this.mTrack;
            } else {
                dialogFragment.mDataSource = this.mDataSource;
            }
            dialogFragment.mFormat = this.mFormat;
            return dialogFragment;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ExportFormat {
    }

    private class ExportRunnable implements Runnable {
        ExportRunnable() {
        }

        public void run() {
            File exportFile;
            Activity activity = DataExport.this.getActivity();
            String mime = null;
            boolean nativeFile = DataExport.this.mFormat == 0;
            nativeFile = DataExport.this.mTrack != null ? nativeFile && DataExport.this.mTrack.source != null && DataExport.this.mTrack.source.isNativeTrack() : nativeFile && DataExport.this.mDataSource.isNativeTrack();
            if (nativeFile) {
                if (DataExport.this.mTrack != null) {
                    exportFile = new File(((FileDataSource) DataExport.this.mTrack.source).path);
                } else {
                    exportFile = new File(((FileDataSource) DataExport.this.mDataSource).path);
                }
                mime = "application/octet-stream";
                DataExport.this.onProgressStarted(100);
                DataExport.this.onProgressChanged(100);
                DataExport.this.onProgressFinished();
            } else {
                FileDataSource exportSource = new FileDataSource();
                if (DataExport.this.mTrack != null) {
                    exportSource.tracks.add(DataExport.this.mTrack);
                } else {
                    if (DataExport.this.mDataSource instanceof WaypointDataSource) {
                        exportSource.waypoints.addAll(((WaypointDataSource) DataExport.this.mDataSource).getWaypoints());
                    }
                    if (DataExport.this.mDataSource instanceof TrackDataSource) {
                        exportSource.tracks.addAll(((TrackDataSource) DataExport.this.mDataSource).getTracks());
                    }
                }
                Object exportDir = new File(activity.getExternalCacheDir(), "export");
                if (exportDir.exists() || exportDir.mkdir()) {
                    String extension = null;
                    switch (DataExport.this.mFormat) {
                        case 0:
                            extension = TrackManager.EXTENSION;
                            mime = "application/octet-stream";
                            break;
                        case 1:
                            extension = GPXManager.EXTENSION;
                            mime = "text/xml";
                            break;
                        case 2:
                            extension = KMLManager.EXTENSION;
                            mime = "application/vnd.google-earth.kml+xml";
                            break;
                    }
                    if (extension == null) {
                        DataExport.logger.error("Failed to determine extension for format: {}", Integer.valueOf(DataExport.this.mFormat));
                        DataExport.this.dismiss();
                        return;
                    }
                    String name;
                    if (DataExport.this.mTrack != null) {
                        name = DataExport.this.mTrack.name;
                    } else {
                        name = DataExport.this.mDataSource.name;
                    }
                    exportFile = new File(exportDir, FileUtils.sanitizeFilename(name) + extension);
                    if (exportFile.exists() && !exportFile.delete()) {
                        DataExport.logger.error("Failed to remove old file");
                    }
                    exportSource.name = name;
                    exportSource.path = exportFile.getAbsolutePath();
                    Manager manager = Manager.getDataManager(DataExport.this.getContext(), exportSource.path);
                    if (manager == null) {
                        DataExport.logger.error("Failed to get data manager for path: {}", (Object) exportSource.path);
                        DataExport.this.dismiss();
                        return;
                    }
                    try {
                        manager.saveData(new FileOutputStream(exportFile, false), exportSource, DataExport.this);
                    } catch (Throwable e) {
                        DataExport.logger.error("Data save error", e);
                        DataExport.this.dismiss();
                        return;
                    }
                }
                DataExport.logger.error("Failed to create export dir: {}", exportDir);
                DataExport.this.dismiss();
                return;
            }
            if (!DataExport.this.mCanceled) {
                Uri contentUri = ExportProvider.getUriForFile(activity, exportFile);
                Intent shareIntent = new Intent();
                shareIntent.setAction("android.intent.action.SEND");
                shareIntent.putExtra("android.intent.extra.STREAM", contentUri);
                shareIntent.setType(mime);
                DataExport.this.startActivity(Intent.createChooser(shareIntent, DataExport.this.getString(DataExport.this.mTrack != null ? R.string.share_track_intent_title : R.string.share_data_intent_title)));
                DataExport.this.dismiss();
            } else if (!nativeFile && exportFile.exists()) {
                exportFile.delete();
            }
        }
    }

    public void dismiss() {
        super.dismiss();
        this.mCanceled = true;
    }

    public void onStart() {
        super.onStart();
        new Thread(new ExportRunnable()).start();
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.mCanceled = false;
        Activity activity = getActivity();
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_progress, null);
        this.mProgressView = (CircleProgressView) dialogView.findViewById(R.id.progress);
        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(activity);
        dialogBuilder.setTitle(R.string.title_export_track);
        dialogBuilder.setNegativeButton(R.string.cancel, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialogBuilder.setView(dialogView);
        return dialogBuilder.create();
    }

    public void onProgressStarted(int length) {
        this.mProgressView.setMaxValue((float) length);
        this.mProgressView.setSeekModeEnabled(false);
    }

    public void onProgressChanged(int progress) {
        this.mProgressView.setValue((float) progress);
    }

    public void onProgressFinished() {
    }

    public void onProgressAnnotated(String annotation) {
    }
}
