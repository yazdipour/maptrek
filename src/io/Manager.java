package mobi.maptrek.io;

import android.content.Context;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Manager {
    static final /* synthetic */ boolean $assertionsDisabled = (!Manager.class.desiredAssertionStatus());
    protected static final Logger logger = LoggerFactory.getLogger(Manager.class);
    private Context mContext;

    public interface OnSaveListener {
        void onError(FileDataSource fileDataSource, Exception exception);

        void onSaved(FileDataSource fileDataSource);
    }

    private class SaveRunnable implements Runnable {
        private final FileDataSource mDataSource;
        private final File mFile;
        private final ProgressListener mProgressListener;
        private final OnSaveListener mSaveListener;

        SaveRunnable(File file, FileDataSource source, @Nullable OnSaveListener saveListener, @Nullable ProgressListener progressListener) {
            this.mFile = file;
            this.mDataSource = source;
            this.mSaveListener = saveListener;
            this.mProgressListener = progressListener;
        }

        public void run() {
            try {
                Process.setThreadPriority(10);
                synchronized (this.mDataSource) {
                    Manager.logger.debug("Saving data source...");
                    File newFile = new File(this.mFile.getParent(), System.currentTimeMillis() + ".tmp");
                    Manager.this.saveData(new FileOutputStream(newFile, false), this.mDataSource, this.mProgressListener);
                    if ((!this.mFile.exists() || this.mFile.delete()) && newFile.renameTo(this.mFile)) {
                        this.mDataSource.path = this.mFile.getAbsolutePath();
                        if (this.mSaveListener != null) {
                            this.mSaveListener.onSaved(this.mDataSource);
                        }
                        Manager.logger.debug("Done");
                    } else {
                        Manager.logger.error("Can not rename data source file after save");
                        if (this.mSaveListener != null) {
                            this.mSaveListener.onError(this.mDataSource, new Exception("Can not rename data source file after save"));
                        }
                    }
                }
            } catch (Throwable e) {
                Manager.logger.error("Can not save data source", e);
                if (this.mSaveListener != null) {
                    this.mSaveListener.onError(this.mDataSource, e);
                }
            }
        }
    }

    @NonNull
    public abstract String getExtension();

    @NonNull
    public abstract FileDataSource loadData(InputStream inputStream, String str) throws Exception;

    public abstract void saveData(OutputStream outputStream, FileDataSource fileDataSource, @Nullable ProgressListener progressListener) throws Exception;

    @Nullable
    public static Manager getDataManager(Context context, String file) {
        if (file.toLowerCase().endsWith(TrackManager.EXTENSION)) {
            return new TrackManager().setContext(context);
        }
        if (file.toLowerCase().endsWith(GPXManager.EXTENSION)) {
            return new GPXManager().setContext(context);
        }
        if (file.toLowerCase().endsWith(KMLManager.EXTENSION)) {
            return new KMLManager().setContext(context);
        }
        return null;
    }

    @Nullable
    private static Manager getDataManager(Context context, FileDataSource source) {
        if (source.path == null) {
            return new TrackManager().setContext(context);
        }
        if (source.path.toLowerCase().endsWith(GPXManager.EXTENSION)) {
            return new GPXManager().setContext(context);
        }
        if (source.path.toLowerCase().endsWith(KMLManager.EXTENSION)) {
            return new KMLManager().setContext(context);
        }
        if (source.path.toLowerCase().endsWith(TrackManager.EXTENSION)) {
            return new TrackManager().setContext(context);
        }
        return null;
    }

    public static void save(Context context, FileDataSource source) {
        save(context, source, null);
    }

    public static void save(Context context, FileDataSource source, OnSaveListener saveListener) {
        save(context, source, saveListener, null);
    }

    public static void save(Context context, FileDataSource source, OnSaveListener saveListener, ProgressListener progressListener) {
        Manager manager = getDataManager(context, source);
        if ($assertionsDisabled || manager != null) {
            manager.saveData(source, saveListener, progressListener);
            return;
        }
        throw new AssertionError("Failed to get IO manager for " + source.path);
    }

    protected final void saveData(FileDataSource source, @Nullable OnSaveListener saveListener, @Nullable ProgressListener progressListener) {
        File file;
        if (source.path == null) {
            String name = (source.name == null || "".equals(source.name)) ? "data_source_" + System.currentTimeMillis() : source.name;
            file = new File(this.mContext.getExternalFilesDir("data"), FileUtils.sanitizeFilename(name) + getExtension());
        } else {
            file = new File(source.path);
        }
        new Thread(new SaveRunnable(file, source, saveListener, progressListener)).start();
    }

    protected final Manager setContext(Context context) {
        this.mContext = context;
        return this;
    }
}
