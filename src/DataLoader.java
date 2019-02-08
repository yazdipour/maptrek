package mobi.maptrek;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.FileObserver;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DataLoader extends AsyncTaskLoader<List<FileDataSource>> {
    private static final String DO_NOT_LOAD_FLAG = ".do_not_load";
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    private List<FileDataSource> mData;
    private final Set<String> mFiles = new HashSet();
    private FileObserver mObserver;
    private ProgressListener mProgressListener;

    DataLoader(Context ctx) {
        super(ctx);
    }

    void setProgressHandler(ProgressListener listener) {
        this.mProgressListener = listener;
    }

    void renameSource(FileDataSource source, File thatFile) {
        if (new File(source.path).renameTo(thatFile)) {
            synchronized (this.mFiles) {
                this.mFiles.remove(source.path);
                source.path = thatFile.getAbsolutePath();
                this.mFiles.add(source.path);
            }
        }
    }

    void markDataSourceLoadable(FileDataSource source, boolean loadable) {
        source.setLoadable(loadable);
        File flag = new File(source.path + DO_NOT_LOAD_FLAG);
        if (loadable) {
            flag.delete();
            return;
        }
        try {
            flag.createNewFile();
            logger.debug("contains: {}", Boolean.valueOf(this.mData.contains(source)));
        } catch (Throwable e) {
            logger.error("Failed to create flag", e);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.util.List<mobi.maptrek.data.source.FileDataSource> loadInBackground() {
        /*
        r28 = this;
        r21 = logger;
        r22 = "loadInBackground()";
        r21.debug(r22);
        r21 = r28.getContext();
        r22 = "data";
        r5 = r21.getExternalFilesDir(r22);
        if (r5 != 0) goto L_0x0015;
    L_0x0013:
        r4 = 0;
    L_0x0014:
        return r4;
    L_0x0015:
        r21 = new mobi.maptrek.io.DataFilenameFilter;
        r21.<init>();
        r0 = r21;
        r9 = r5.listFiles(r0);
        if (r9 != 0) goto L_0x0024;
    L_0x0022:
        r4 = 0;
        goto L_0x0014;
    L_0x0024:
        r4 = new java.util.ArrayList;
        r4.<init>();
        r12 = new java.util.ArrayList;
        r12.<init>();
        r16 = 0;
        r0 = r9.length;
        r22 = r0;
        r21 = 0;
    L_0x0035:
        r0 = r21;
        r1 = r22;
        if (r0 >= r1) goto L_0x00a5;
    L_0x003b:
        r7 = r9[r21];
        r18 = r7.getAbsolutePath();
        r0 = r28;
        r0 = r0.mFiles;
        r23 = r0;
        monitor-enter(r23);
        r0 = r28;
        r0 = r0.mFiles;	 Catch:{ all -> 0x00a2 }
        r24 = r0;
        r0 = r24;
        r1 = r18;
        r24 = r0.contains(r1);	 Catch:{ all -> 0x00a2 }
        if (r24 != 0) goto L_0x009e;
    L_0x0058:
        r14 = new java.io.File;	 Catch:{ all -> 0x00a2 }
        r24 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00a2 }
        r24.<init>();	 Catch:{ all -> 0x00a2 }
        r0 = r24;
        r1 = r18;
        r24 = r0.append(r1);	 Catch:{ all -> 0x00a2 }
        r25 = ".do_not_load";
        r24 = r24.append(r25);	 Catch:{ all -> 0x00a2 }
        r24 = r24.toString();	 Catch:{ all -> 0x00a2 }
        r0 = r24;
        r14.<init>(r0);	 Catch:{ all -> 0x00a2 }
        r13 = r14.exists();	 Catch:{ all -> 0x00a2 }
        if (r13 != 0) goto L_0x008c;
    L_0x007c:
        r0 = r16;
        r0 = (long) r0;	 Catch:{ all -> 0x00a2 }
        r24 = r0;
        r26 = r7.length();	 Catch:{ all -> 0x00a2 }
        r24 = r24 + r26;
        r0 = r24;
        r0 = (int) r0;	 Catch:{ all -> 0x00a2 }
        r16 = r0;
    L_0x008c:
        r24 = new android.util.Pair;	 Catch:{ all -> 0x00a2 }
        r25 = java.lang.Boolean.valueOf(r13);	 Catch:{ all -> 0x00a2 }
        r0 = r24;
        r1 = r25;
        r0.<init>(r7, r1);	 Catch:{ all -> 0x00a2 }
        r0 = r24;
        r12.add(r0);	 Catch:{ all -> 0x00a2 }
    L_0x009e:
        monitor-exit(r23);	 Catch:{ all -> 0x00a2 }
        r21 = r21 + 1;
        goto L_0x0035;
    L_0x00a2:
        r21 = move-exception;
        monitor-exit(r23);	 Catch:{ all -> 0x00a2 }
        throw r21;
    L_0x00a5:
        r0 = r28;
        r0 = r0.mProgressListener;
        r21 = r0;
        if (r21 == 0) goto L_0x00ba;
    L_0x00ad:
        r0 = r28;
        r0 = r0.mProgressListener;
        r21 = r0;
        r0 = r21;
        r1 = r16;
        r0.onProgressStarted(r1);
    L_0x00ba:
        r19 = 0;
        r22 = r12.iterator();
    L_0x00c0:
        r21 = r22.hasNext();
        if (r21 == 0) goto L_0x0014;
    L_0x00c6:
        r17 = r22.next();
        r17 = (android.util.Pair) r17;
        r21 = r28.isLoadInBackgroundCanceled();
        if (r21 == 0) goto L_0x00dc;
    L_0x00d2:
        r21 = logger;
        r22 = "loadInBackgroundCanceled";
        r21.debug(r22);
        r4 = 0;
        goto L_0x0014;
    L_0x00dc:
        r0 = r17;
        r7 = r0.first;
        r7 = (java.io.File) r7;
        r0 = r17;
        r0 = r0.second;
        r21 = r0;
        r21 = (java.lang.Boolean) r21;
        r13 = r21.booleanValue();
        r23 = logger;
        r24 = "  {} -> {}";
        if (r13 == 0) goto L_0x013c;
    L_0x00f4:
        r21 = "skip";
    L_0x00f6:
        r25 = r7.getName();
        r0 = r23;
        r1 = r24;
        r2 = r21;
        r3 = r25;
        r0.debug(r1, r2, r3);
        if (r13 == 0) goto L_0x013f;
    L_0x0107:
        r20 = new mobi.maptrek.data.source.FileDataSource;
        r20.<init>();
        r21 = r7.getName();
        r23 = 0;
        r24 = r7.getName();
        r25 = ".";
        r24 = r24.lastIndexOf(r25);
        r0 = r21;
        r1 = r23;
        r2 = r24;
        r21 = r0.substring(r1, r2);
        r0 = r21;
        r1 = r20;
        r1.name = r0;
        r21 = r7.getAbsolutePath();
        r0 = r21;
        r1 = r20;
        r1.path = r0;
        r0 = r20;
        r4.add(r0);
        goto L_0x00c0;
    L_0x013c:
        r21 = "load";
        goto L_0x00f6;
    L_0x013f:
        r11 = new mobi.maptrek.util.MonitoredInputStream;	 Catch:{ Exception -> 0x01fa }
        r21 = new java.io.FileInputStream;	 Catch:{ Exception -> 0x01fa }
        r0 = r21;
        r0.<init>(r7);	 Catch:{ Exception -> 0x01fa }
        r0 = r21;
        r11.<init>(r0);	 Catch:{ Exception -> 0x01fa }
        r10 = r19;
        r21 = new mobi.maptrek.DataLoader$1;	 Catch:{ Exception -> 0x01fa }
        r0 = r21;
        r1 = r28;
        r0.<init>(r10);	 Catch:{ Exception -> 0x01fa }
        r0 = r21;
        r11.addChangeListener(r0);	 Catch:{ Exception -> 0x01fa }
        r21 = r28.getContext();	 Catch:{ Exception -> 0x01fa }
        r23 = r7.getName();	 Catch:{ Exception -> 0x01fa }
        r0 = r21;
        r1 = r23;
        r15 = mobi.maptrek.io.Manager.getDataManager(r0, r1);	 Catch:{ Exception -> 0x01fa }
        if (r15 == 0) goto L_0x01e8;
    L_0x016f:
        r21 = r7.getAbsolutePath();	 Catch:{ Exception -> 0x01fa }
        r0 = r21;
        r20 = r15.loadData(r11, r0);	 Catch:{ Exception -> 0x01fa }
        r21 = r7.getAbsolutePath();	 Catch:{ Exception -> 0x01fa }
        r0 = r21;
        r1 = r20;
        r1.path = r0;	 Catch:{ Exception -> 0x01fa }
        r0 = r20;
        r0 = r0.name;	 Catch:{ Exception -> 0x01fa }
        r21 = r0;
        if (r21 == 0) goto L_0x019d;
    L_0x018b:
        r21 = "";
        r0 = r20;
        r0 = r0.name;	 Catch:{ Exception -> 0x01fa }
        r23 = r0;
        r0 = r21;
        r1 = r23;
        r21 = r0.equals(r1);	 Catch:{ Exception -> 0x01fa }
        if (r21 == 0) goto L_0x01b9;
    L_0x019d:
        r8 = r7.getName();	 Catch:{ Exception -> 0x01fa }
        r21 = 0;
        r23 = ".";
        r0 = r23;
        r23 = r8.lastIndexOf(r0);	 Catch:{ Exception -> 0x01fa }
        r0 = r21;
        r1 = r23;
        r21 = r8.substring(r0, r1);	 Catch:{ Exception -> 0x01fa }
        r0 = r21;
        r1 = r20;
        r1.name = r0;	 Catch:{ Exception -> 0x01fa }
    L_0x01b9:
        r20.setLoaded();	 Catch:{ Exception -> 0x01fa }
        r0 = r20;
        r4.add(r0);	 Catch:{ Exception -> 0x01fa }
        r0 = r15 instanceof mobi.maptrek.io.TrackManager;	 Catch:{ Exception -> 0x01fa }
        r21 = r0;
        if (r21 == 0) goto L_0x01e8;
    L_0x01c7:
        r0 = r20;
        r0 = r0.tracks;	 Catch:{ Exception -> 0x01fa }
        r21 = r0;
        r23 = 0;
        r0 = r21;
        r1 = r23;
        r21 = r0.get(r1);	 Catch:{ Exception -> 0x01fa }
        r21 = (mobi.maptrek.data.Track) r21;	 Catch:{ Exception -> 0x01fa }
        r21 = r21.getLastPoint();	 Catch:{ Exception -> 0x01fa }
        r0 = r21;
        r0 = r0.time;	 Catch:{ Exception -> 0x01fa }
        r24 = r0;
        r0 = r24;
        r7.setLastModified(r0);	 Catch:{ Exception -> 0x01fa }
    L_0x01e8:
        r0 = r19;
        r0 = (long) r0;
        r24 = r0;
        r26 = r7.length();
        r24 = r24 + r26;
        r0 = r24;
        r0 = (int) r0;
        r19 = r0;
        goto L_0x00c0;
    L_0x01fa:
        r6 = move-exception;
        r21 = logger;
        r23 = "File error: {}, {}";
        r24 = r7.getAbsolutePath();
        r0 = r21;
        r1 = r23;
        r2 = r24;
        r0.error(r1, r2, r6);
        goto L_0x01e8;
        */
        throw new UnsupportedOperationException("Method not decompiled: mobi.maptrek.DataLoader.loadInBackground():java.util.List<mobi.maptrek.data.source.FileDataSource>");
    }

    public void deliverResult(List<FileDataSource> data) {
        logger.debug("deliverResult()");
        if (this.mProgressListener != null) {
            this.mProgressListener.onProgressFinished();
        }
        if (!isReset()) {
            synchronized (this.mFiles) {
                if (this.mData == null) {
                    this.mData = data;
                } else {
                    ArrayList<FileDataSource> newData = new ArrayList(this.mData.size());
                    newData.addAll(this.mData);
                    newData.addAll(data);
                    this.mData = newData;
                }
                for (FileDataSource source : data) {
                    this.mFiles.add(source.path);
                }
            }
            if (isStarted()) {
                super.deliverResult(this.mData);
            }
        }
    }

    protected void onStartLoading() {
        logger.debug("onStartLoading()");
        if (this.mData != null) {
            deliverResult(new ArrayList());
        }
        if (this.mObserver == null) {
            final File dir = getContext().getExternalFilesDir("data");
            if (dir != null) {
                this.mObserver = new FileObserver(dir.getAbsolutePath(), 712) {
                    public void onEvent(int event, String path) {
                        if (event != 32768 && path != null) {
                            path = dir.getAbsolutePath() + File.separator + path;
                            DataLoader.logger.debug("{}: {}", (Object) path, Integer.valueOf(event));
                            boolean loadFlag = false;
                            if (path.endsWith(DataLoader.DO_NOT_LOAD_FLAG)) {
                                if (event != 8) {
                                    path = path.substring(0, path.indexOf(DataLoader.DO_NOT_LOAD_FLAG));
                                    loadFlag = true;
                                } else {
                                    return;
                                }
                            }
                            synchronized (DataLoader.this.mFiles) {
                                boolean loadedSource = false;
                                Iterator<FileDataSource> i = DataLoader.this.mData.iterator();
                                while (i.hasNext()) {
                                    FileDataSource source = (FileDataSource) i.next();
                                    if (source.path.equals(path)) {
                                        if (loadFlag && source.isLoaded()) {
                                            loadedSource = true;
                                        } else {
                                            i.remove();
                                        }
                                    }
                                }
                                if (!loadedSource) {
                                    DataLoader.this.mFiles.remove(path);
                                    DataLoader.this.onContentChanged();
                                }
                            }
                        }
                    }
                };
                this.mObserver.startWatching();
            } else {
                return;
            }
        }
        if (takeContentChanged() || this.mData == null) {
            forceLoad();
        }
    }

    protected void onStopLoading() {
        logger.debug("onStopLoading()");
        cancelLoad();
    }

    protected void onReset() {
        logger.debug("onReset()");
        onStopLoading();
        this.mFiles.clear();
        if (this.mData != null) {
            this.mData.clear();
        }
        this.mData = null;
        if (this.mObserver != null) {
            this.mObserver.stopWatching();
            this.mObserver = null;
        }
    }

    public void onCanceled(List<FileDataSource> data) {
        logger.debug("onCanceled()");
        super.onCanceled(data);
    }
}
