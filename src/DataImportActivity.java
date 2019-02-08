package mobi.maptrek;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import mobi.maptrek.io.GPXManager;
import mobi.maptrek.io.KMLManager;
import mobi.maptrek.io.TrackManager;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.MonitoredInputStream;
import mobi.maptrek.util.MonitoredInputStream.ChangeListener;
import mobi.maptrek.util.ProgressHandler;
import mobi.maptrek.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataImportActivity extends Activity {
    private static final String DATA_IMPORT_FRAGMENT = "dataImportFragment";
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final DateFormat SUFFIX_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
    private static final Logger logger = LoggerFactory.getLogger(DataImportActivity.class);
    private Button mActionButton;
    private DataImportFragment mDataImportFragment;
    private TextView mFileNameView;
    private ProgressBar mProgressBar;
    private ImportProgressHandler mProgressHandler;
    private Runnable mTask;

    public static class DataImportFragment extends Fragment {
        private Handler mBackgroundHandler;
        private HandlerThread mBackgroundThread;
        private MonitoredInputStream mInputStream;
        private Intent mIntent;
        private ProgressListener mProgressListener;

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            this.mBackgroundThread = new HandlerThread("DataImportThread");
            this.mBackgroundThread.setPriority(1);
            this.mBackgroundThread.start();
            this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper());
        }

        public void onStart() {
            super.onStart();
            if (this.mIntent != null) {
                processIntent(this.mIntent);
                this.mIntent = null;
            }
        }

        public void onDetach() {
            super.onDetach();
            this.mProgressListener = null;
        }

        public void onDestroy() {
            super.onDestroy();
            this.mBackgroundThread.interrupt();
            this.mBackgroundHandler.removeCallbacksAndMessages(null);
            this.mBackgroundThread.quit();
            this.mBackgroundThread = null;
        }

        public void setIntent(Intent intent) {
            this.mIntent = intent;
        }

        public void setProgressHandler(ProgressListener listener) {
            this.mProgressListener = listener;
        }

        private void processIntent(final Intent intent) {
            Object action = intent.getAction();
            Object type = intent.getType();
            DataImportActivity.logger.debug("Action: {}", action);
            DataImportActivity.logger.debug("Type: {}", type);
            if ("android.intent.action.SEND".equals(action) || "android.intent.action.VIEW".equals(action)) {
                Uri uri = (Uri) intent.getParcelableExtra("android.intent.extra.STREAM");
                if (uri == null) {
                    uri = intent.getData();
                }
                DataImportActivity.logger.debug("Uri: {}", uri.toString());
                DataImportActivity.logger.debug("Authority: {}", uri.getAuthority());
                final Uri finalUri = uri;
                Runnable task = new Runnable() {
                    public void run() {
                        DataImportFragment.this.readFile(finalUri);
                    }
                };
                if ("file".equals(uri.getScheme())) {
                    ((DataImportActivity) getActivity()).askForPermission(task);
                } else {
                    startImport(task);
                }
            } else if ("android.intent.action.SEND_MULTIPLE".equals(action)) {
                startImport(new Runnable() {
                    public void run() {
                        ArrayList<Uri> uris = intent.getParcelableArrayListExtra("android.intent.extra.STREAM");
                        if (uris != null) {
                            Iterator it = uris.iterator();
                            while (it.hasNext()) {
                                DataImportFragment.this.readFile((Uri) it.next());
                            }
                        }
                    }
                });
            }
        }

        private void readFile(Uri uri) {
            DataImportActivity activity = (DataImportActivity) getActivity();
            String name = null;
            long length = -1;
            String scheme = uri.getScheme();
            if ("file".equals(scheme)) {
                File file = new File(uri.getPath());
                name = uri.getLastPathSegment();
                length = file.length();
                try {
                    this.mInputStream = new MonitoredInputStream(new FileInputStream(file));
                    DataImportActivity.logger.debug("file: {} [{}]", (Object) name, Long.valueOf(length));
                } catch (Throwable e) {
                    DataImportActivity.logger.error("Failed to get imported file stream", e);
                    activity.showError(getString(R.string.msgFailedToGetFile));
                    return;
                }
            } else if ("content".equals(scheme)) {
                ContentResolver resolver = activity.getContentResolver();
                Cursor cursor = resolver.query(uri, new String[]{"_display_name", "_size"}, null, null, null);
                if (cursor != null) {
                    DataImportActivity.logger.debug("   from cursor");
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(cursor.getColumnIndex("_display_name"));
                        length = cursor.getLong(cursor.getColumnIndex("_size"));
                    }
                    cursor.close();
                }
                if (name == null) {
                    cursor = resolver.query(uri, new String[]{"_data"}, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        name = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                if (length == -1) {
                    try {
                        AssetFileDescriptor afd = resolver.openAssetFileDescriptor(uri, "r");
                        if (afd != null) {
                            length = afd.getLength();
                        }
                    } catch (FileNotFoundException e2) {
                        e2.printStackTrace();
                    }
                }
                if (name == null) {
                    DataImportActivity.logger.error("Failed to get file name");
                    activity.showError(getString(R.string.msgFailedToGetFileName));
                    return;
                }
                try {
                    this.mInputStream = new MonitoredInputStream(resolver.openInputStream(uri));
                    DataImportActivity.logger.debug("Import: [{}][{}]", name, Long.valueOf(length));
                } catch (Throwable e3) {
                    DataImportActivity.logger.error("Failed to get imported file stream", e3);
                    activity.showError(getString(R.string.msgFailedToGetFile));
                    return;
                }
            } else {
                DataImportActivity.logger.warn("Unsupported transfer method");
                activity.showError(getString(R.string.msgFailedToGetFile));
                return;
            }
            if (name.endsWith(TrackManager.EXTENSION) || name.endsWith(KMLManager.EXTENSION) || name.endsWith(GPXManager.EXTENSION) || name.endsWith(".mbtiles") || name.endsWith(".sqlitedb")) {
                this.mProgressListener.onProgressStarted((int) length);
                this.mProgressListener.onProgressAnnotated(name);
                File dst = null;
                try {
                    dst = getDestinationFile(name);
                    this.mInputStream.addChangeListener(new ChangeListener() {
                        public void stateChanged(long location) {
                            if (DataImportFragment.this.mProgressListener != null) {
                                DataImportFragment.this.mProgressListener.onProgressChanged((int) location);
                            }
                        }
                    });
                    FileUtils.copyStreamToFile(this.mInputStream, dst);
                    this.mProgressListener.onProgressFinished();
                    return;
                } catch (IOException e4) {
                    e4.printStackTrace();
                    if (dst != null && dst.exists()) {
                        dst.delete();
                    }
                    activity.showError(getString(R.string.msgFailedToGetFile));
                    return;
                }
            }
            DataImportActivity.logger.warn("Unsupported file format");
            activity.showError(getString(R.string.msgUnsupportedFileFormat));
        }

        @Nullable
        private File getDestinationFile(String filename) {
            boolean isMap = filename.endsWith(".mbtiles") || filename.endsWith(".sqlitedb");
            File dir = getContext().getExternalFilesDir(isMap ? "maps" : "data");
            if (dir == null) {
                DataImportActivity.logger.error("Path for {} unavailable", isMap ? "maps" : "data");
                return null;
            }
            File destination = new File(dir, filename);
            if (!destination.exists()) {
                return destination;
            }
            String ext = filename.substring(filename.lastIndexOf("."));
            return new File(dir, filename.replace(ext, "-" + DataImportActivity.SUFFIX_FORMAT.format(new Date()) + ext));
        }

        public void startImport(Runnable task) {
            this.mBackgroundHandler.sendMessage(Message.obtain(this.mBackgroundHandler, task));
        }

        public void stopImport() {
            if (this.mInputStream != null) {
                try {
                    this.mInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressLint({"HandlerLeak"})
    private class ImportProgressHandler extends ProgressHandler {
        ImportProgressHandler(ProgressBar progressBar) {
            super(progressBar);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 3) {
                DataImportActivity.this.mFileNameView.setText(R.string.msgDataImported);
                DataImportActivity.this.mProgressBar.setVisibility(8);
                DataImportActivity.this.mActionButton.setText(MapTrek.isMainActivityRunning ? R.string.ok : R.string.startApplication);
                DataImportActivity.this.mActionButton.setTag(Boolean.valueOf(true));
            }
        }

        public void onProgressAnnotated(final String annotation) {
            DataImportActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    DataImportActivity.this.mFileNameView.setText(annotation);
                }
            });
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_data_import);
        this.mFileNameView = (TextView) findViewById(R.id.filename);
        this.mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        this.mActionButton = (Button) findViewById(R.id.action);
        FragmentManager fm = getFragmentManager();
        this.mDataImportFragment = (DataImportFragment) fm.findFragmentByTag(DATA_IMPORT_FRAGMENT);
        if (this.mDataImportFragment == null) {
            this.mDataImportFragment = new DataImportFragment();
            fm.beginTransaction().add(this.mDataImportFragment, DATA_IMPORT_FRAGMENT).commit();
            this.mDataImportFragment.setIntent(getIntent());
        }
        this.mProgressHandler = new ImportProgressHandler(this.mProgressBar);
        this.mDataImportFragment.setProgressHandler(this.mProgressHandler);
        this.mActionButton.setText(R.string.cancel);
        this.mActionButton.setTag(Boolean.valueOf(false));
        this.mActionButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (((Boolean) DataImportActivity.this.mActionButton.getTag()).booleanValue()) {
                    Intent iLaunch = new Intent("android.intent.action.MAIN");
                    iLaunch.addCategory("android.intent.category.LAUNCHER");
                    iLaunch.setComponent(new ComponentName(DataImportActivity.this.getApplicationContext(), MainActivity.class));
                    iLaunch.setFlags(270532608);
                    DataImportActivity.this.startActivity(iLaunch);
                } else {
                    DataImportActivity.this.mDataImportFragment.stopImport();
                }
                DataImportActivity.this.finish();
            }
        });
    }

    protected void onDestroy() {
        super.onDestroy();
        this.mProgressHandler = null;
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("filename", this.mFileNameView.getText().toString());
        savedInstanceState.putInt("progressBarMax", this.mProgressBar.getMax());
        savedInstanceState.putInt("progressBarProgress", this.mProgressBar.getProgress());
        savedInstanceState.putString("action", this.mActionButton.getText().toString());
        savedInstanceState.putBoolean("finished", ((Boolean) this.mActionButton.getTag()).booleanValue());
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.mFileNameView.setText(savedInstanceState.getString("filename"));
        this.mProgressBar.setMax(savedInstanceState.getInt("progressBarMax"));
        this.mProgressBar.setProgress(savedInstanceState.getInt("progressBarProgress"));
        this.mActionButton.setText(savedInstanceState.getString("action"));
        this.mActionButton.setTag(Boolean.valueOf(savedInstanceState.getBoolean("finished")));
    }

    private void askForPermission(Runnable task) {
        this.mTask = task;
        if (checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == 0) {
            this.mDataImportFragment.startImport(this.mTask);
        } else if (shouldShowRequestPermissionRationale("android.permission.READ_EXTERNAL_STORAGE")) {
            String name;
            try {
                PackageManager pm = getPackageManager();
                name = (String) pm.getPermissionInfo("android.permission.READ_EXTERNAL_STORAGE", 128).loadLabel(pm);
            } catch (Throwable e) {
                logger.error("Failed to obtain name for permission", e);
                name = "read external storage";
            }
            new Builder(this).setMessage(getString(R.string.msgReadExternalStorageRationale, new Object[]{name})).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    DataImportActivity.this.requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 1);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    DataImportActivity.this.finish();
                }
            }).create().show();
        } else {
            requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 1);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length <= 0 || grantResults[0] != 0) {
                    finish();
                    return;
                } else if (this.mTask != null) {
                    this.mDataImportFragment.startImport(this.mTask);
                    return;
                } else {
                    return;
                }
            default:
                return;
        }
    }

    private void showError(String message) {
        this.mFileNameView.setText(message);
        this.mProgressBar.setVisibility(8);
        this.mActionButton.setText(R.string.close);
        this.mActionButton.setTag(Boolean.valueOf(false));
    }
}
