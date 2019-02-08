package mobi.maptrek.maps.maptrek;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.net.Uri.Builder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import mobi.maptrek.R;
import mobi.maptrek.location.BaseNavigationService;
import mobi.maptrek.maps.MapService;
import mobi.maptrek.util.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Index {
    public static final String BASEMAP_FILENAME = "basemap.mtiles";
    public static final int BASEMAP_SIZE_STUB = 41;
    public static final String HILLSHADE_FILENAME = "hillshade.mbtiles";
    public static final String WORLDMAP_FILENAME = "world.mtiles";
    private static final Logger logger = LoggerFactory.getLogger(Index.class);
    private boolean mAccountHillshades;
    private long mBaseMapDownloadSize = 0;
    private short mBaseMapDownloadVersion = (short) 0;
    private short mBaseMapVersion = (short) 0;
    private final Context mContext;
    private final DownloadManager mDownloadManager;
    private boolean mExpiredDownloadSizes;
    private boolean mHasDownloadSizes;
    private boolean mHasHillshades;
    private SQLiteDatabase mHillshadeDatabase;
    private int mLoadedMaps = 0;
    private final Set<WeakReference<MapStateListener>> mMapStateListeners = new HashSet();
    private MapStatus[][] mMaps = ((MapStatus[][]) Array.newInstance(MapStatus.class, new int[]{128, 128}));
    private SQLiteDatabase mMapsDatabase;

    public enum ACTION {
        NONE,
        DOWNLOAD,
        CANCEL,
        REMOVE
    }

    public static class IndexStats {
        public int download = 0;
        public long downloadSize = 0;
        public int downloading = 0;
        public int loaded = 0;
        public int remove = 0;
    }

    public interface MapStateListener {
        void onHasDownloadSizes();

        void onHillshadeAccountingChanged(boolean z);

        void onMapSelected(int i, int i2, ACTION action, IndexStats indexStats);

        void onStatsChanged();
    }

    public static class MapStatus {
        public ACTION action = ACTION.NONE;
        public short created = (short) 0;
        public short downloadCreated;
        public long downloadSize;
        public long downloading;
        public long hillshadeDownloadSize;
        public byte hillshadeDownloadVersion;
        public long hillshadeDownloading;
        public byte hillshadeVersion = (byte) 0;
    }

    public boolean processDownloadedHillshade(int r20, int r21, java.lang.String r22, @android.support.annotation.Nullable mobi.maptrek.util.ProgressListener r23) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:41:? in {4, 10, 12, 20, 23, 27, 29, 31, 34, 36, 38, 39, 40, 42} preds:[]
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:129)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.rerun(BlockProcessor.java:44)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:57)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:59)
	at jadx.core.ProcessClass.process(ProcessClass.java:42)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r19 = this;
        r14 = new java.io.File;
        r0 = r22;
        r14.<init>(r0);
        r5 = logger;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = "Importing from {}";	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r7 = r14.getName();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5.error(r6, r7);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = 1;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r22;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r4 = android.database.sqlite.SQLiteDatabase.openDatabase(r0, r5, r6);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r17 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r15 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        if (r23 == 0) goto L_0x0033;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
    L_0x001f:
        r0 = r17;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = (long) r0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = "tiles";	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r8 = android.database.DatabaseUtils.queryNumEntries(r4, r5);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = r6 + r8;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = (int) r6;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r17 = r0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r23;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r1 = r17;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0.onProgressStarted(r1);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
    L_0x0033:
        r0 = r19;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = r0.mHillshadeDatabase;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = "REPLACE INTO tiles VALUES (?,?,?,?)";	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r16 = r5.compileStatement(r6);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r19;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = r0.mHillshadeDatabase;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5.beginTransaction();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = "tiles";	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_TILES;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r7 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r8 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r9 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r10 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r11 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r12 = r4.query(r5, r6, r7, r8, r9, r10, r11);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r12.moveToFirst();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
    L_0x0054:
        r5 = r12.isAfterLast();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        if (r5 != 0) goto L_0x00c6;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
    L_0x005a:
        r16.clearBindings();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = 1;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = r12.getInt(r6);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = (long) r6;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r16;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = 2;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = 1;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = r12.getInt(r6);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = (long) r6;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r16;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = 3;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = 2;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = r12.getInt(r6);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = (long) r6;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r16;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = 4;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = 3;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = r12.getBlob(r6);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r16;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0.bindBlob(r5, r6);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r16.execute();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        if (r23 == 0) goto L_0x0098;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
    L_0x0091:
        r15 = r15 + 1;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r23;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0.onProgressChanged(r15);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
    L_0x0098:
        r12.moveToNext();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        goto L_0x0054;
    L_0x009c:
        r13 = move-exception;
        r5 = mobi.maptrek.MapTrek.getApplication();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5.registerException(r13);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = logger;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = "Import failed";	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5.error(r6, r13);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = 0;
        r0 = r19;
        r6 = r0.mHillshadeDatabase;
        r6 = r6.inTransaction();
        if (r6 == 0) goto L_0x00bd;
    L_0x00b6:
        r0 = r19;
        r6 = r0.mHillshadeDatabase;
        r6.endTransaction();
    L_0x00bd:
        if (r23 == 0) goto L_0x00c2;
    L_0x00bf:
        r23.onProgressFinished();
    L_0x00c2:
        r14.delete();
    L_0x00c5:
        return r5;
    L_0x00c6:
        r12.close();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r19;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = r0.mHillshadeDatabase;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5.setTransactionSuccessful();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r19;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = r0.mHillshadeDatabase;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5.endTransaction();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = logger;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = "  imported tiles";	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5.error(r6);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r18 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = "metadata";	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = 1;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6 = new java.lang.String[r6];	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r7 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r8 = "value";	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r6[r7] = r8;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r7 = "name = ?";	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r8 = 1;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r8 = new java.lang.String[r8];	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r9 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r10 = "timestamp";	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r8[r9] = r10;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r9 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r10 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r11 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r12 = r4.query(r5, r6, r7, r8, r9, r10, r11);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = r12.moveToFirst();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        if (r5 == 0) goto L_0x0111;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
    L_0x0101:
        r5 = 0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = r12.getString(r5);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = java.lang.Integer.valueOf(r5);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = r5.intValue();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = (byte) r5;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r18 = r0;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
    L_0x0111:
        r12.close();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r4.close();	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r19;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = r0.mHasHillshades;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        if (r5 != 0) goto L_0x0126;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
    L_0x011d:
        r5 = 1;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        mobi.maptrek.Configuration.setHillshadesEnabled(r5);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r5 = 1;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r19;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0.mHasHillshades = r5;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
    L_0x0126:
        r0 = r19;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r1 = r20;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r2 = r21;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r3 = r18;	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0.setHillshadeDownloaded(r1, r2, r3);	 Catch:{ SQLiteException -> 0x009c, all -> 0x014d }
        r0 = r19;
        r5 = r0.mHillshadeDatabase;
        r5 = r5.inTransaction();
        if (r5 == 0) goto L_0x0142;
    L_0x013b:
        r0 = r19;
        r5 = r0.mHillshadeDatabase;
        r5.endTransaction();
    L_0x0142:
        if (r23 == 0) goto L_0x0147;
    L_0x0144:
        r23.onProgressFinished();
    L_0x0147:
        r14.delete();
        r5 = 1;
        goto L_0x00c5;
    L_0x014d:
        r5 = move-exception;
        r0 = r19;
        r6 = r0.mHillshadeDatabase;
        r6 = r6.inTransaction();
        if (r6 == 0) goto L_0x015f;
    L_0x0158:
        r0 = r19;
        r6 = r0.mHillshadeDatabase;
        r6.endTransaction();
    L_0x015f:
        if (r23 == 0) goto L_0x0164;
    L_0x0161:
        r23.onProgressFinished();
    L_0x0164:
        r14.delete();
        throw r5;
        */
        throw new UnsupportedOperationException("Method not decompiled: mobi.maptrek.maps.maptrek.Index.processDownloadedHillshade(int, int, java.lang.String, mobi.maptrek.util.ProgressListener):boolean");
    }

    public boolean processDownloadedMap(int r23, int r24, java.lang.String r25, @android.support.annotation.Nullable mobi.maptrek.util.ProgressListener r26) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:72:? in {4, 7, 13, 15, 17, 28, 30, 31, 34, 36, 38, 46, 53, 56, 60, 62, 65, 66, 68, 69, 70, 71, 73} preds:[]
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:129)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.rerun(BlockProcessor.java:44)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:57)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:59)
	at jadx.core.ProcessClass.process(ProcessClass.java:42)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r22 = this;
        r17 = new java.io.File;
        r0 = r17;
        r1 = r25;
        r0.<init>(r1);
        r5 = logger;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "Importing from {}";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r7 = r17.getName();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.error(r6, r7);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r25;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r4 = android.database.sqlite.SQLiteDatabase.openDatabase(r0, r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r21 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r18 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        if (r26 == 0) goto L_0x005d;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x0022:
        r0 = r21;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = (long) r0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = "names";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = android.database.DatabaseUtils.queryNumEntries(r4, r5);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r6 + r8;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = (int) r6;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r21 = r0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r21;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = (long) r0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = "features";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = android.database.DatabaseUtils.queryNumEntries(r4, r5);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r6 + r8;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = (int) r6;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r21 = r0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r21;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = (long) r0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = "feature_names";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = android.database.DatabaseUtils.queryNumEntries(r4, r5);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r6 + r8;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = (int) r6;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r21 = r0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r21;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = (long) r0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = "tiles";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = android.database.DatabaseUtils.queryNumEntries(r4, r5);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r6 + r8;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = (int) r6;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r21 = r0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r26;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r1 = r21;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.onProgressStarted(r1);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x005d:
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r16 = mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.hasFullTextIndex(r5);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "REPLACE INTO names VALUES (?,?)";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r19 = r5.compileStatement(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r20 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        if (r16 == 0) goto L_0x007d;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x0073:
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "INSERT INTO names_fts (docid, name) VALUES (?,?)";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r20 = r5.compileStatement(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x007d:
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.beginTransaction();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = "names";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_NAMES;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r7 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r9 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r10 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r11 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r12 = r4.query(r5, r6, r7, r8, r9, r10, r11);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r12.moveToFirst();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x0094:
        r5 = r12.isAfterLast();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        if (r5 != 0) goto L_0x011a;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x009a:
        r19.clearBindings();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getLong(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 2;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getString(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindString(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r19.execute();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        if (r20 == 0) goto L_0x00d4;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x00b8:
        r20.clearBindings();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getLong(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r20;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 2;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getString(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r20;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindString(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r20.execute();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x00d4:
        if (r26 == 0) goto L_0x00df;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x00d6:
        r18 = r18 + 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r26;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r1 = r18;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.onProgressChanged(r1);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x00df:
        r12.moveToNext();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        goto L_0x0094;
    L_0x00e3:
        r14 = move-exception;
        r5 = mobi.maptrek.MapTrek.getApplication();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.registerException(r14);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = logger;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "Import failed";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.error(r6, r14);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r10 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r23;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r7 = r24;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.setDownloading(r6, r7, r8, r10);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 0;
        r0 = r22;
        r6 = r0.mMapsDatabase;
        r6 = r6.inTransaction();
        if (r6 == 0) goto L_0x0111;
    L_0x010a:
        r0 = r22;
        r6 = r0.mMapsDatabase;
        r6.endTransaction();
    L_0x0111:
        if (r26 == 0) goto L_0x0116;
    L_0x0113:
        r26.onProgressFinished();
    L_0x0116:
        r17.delete();
    L_0x0119:
        return r5;
    L_0x011a:
        r12.close();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.setTransactionSuccessful();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.endTransaction();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = logger;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "  imported names";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.error(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "REPLACE INTO features VALUES (?,?,?,?)";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r19 = r5.compileStatement(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "REPLACE INTO map_features VALUES (?,?,?)";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r15 = r5.compileStatement(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r23;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = (long) r0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r15.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 2;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r24;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = (long) r0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r15.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.beginTransaction();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = "features";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_FEATURES;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r7 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r9 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r10 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r11 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r12 = r4.query(r5, r6, r7, r8, r9, r10, r11);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r12.moveToFirst();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x016b:
        r5 = r12.isAfterLast();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        if (r5 != 0) goto L_0x01da;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x0171:
        r19.clearBindings();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getLong(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 2;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getInt(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = (long) r6;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 3;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 2;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getDouble(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindDouble(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 4;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 3;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getDouble(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindDouble(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r19.execute();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 3;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getLong(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r15.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r15.execute();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        if (r26 == 0) goto L_0x01bb;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x01b2:
        r18 = r18 + 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r26;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r1 = r18;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.onProgressChanged(r1);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x01bb:
        r12.moveToNext();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        goto L_0x016b;
    L_0x01bf:
        r5 = move-exception;
        r0 = r22;
        r6 = r0.mMapsDatabase;
        r6 = r6.inTransaction();
        if (r6 == 0) goto L_0x01d1;
    L_0x01ca:
        r0 = r22;
        r6 = r0.mMapsDatabase;
        r6.endTransaction();
    L_0x01d1:
        if (r26 == 0) goto L_0x01d6;
    L_0x01d3:
        r26.onProgressFinished();
    L_0x01d6:
        r17.delete();
        throw r5;
    L_0x01da:
        r12.close();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.setTransactionSuccessful();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.endTransaction();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = logger;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "  imported features";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.error(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "REPLACE INTO feature_names VALUES (?,?,?)";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r19 = r5.compileStatement(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.beginTransaction();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = "feature_names";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_FEATURE_NAMES;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r7 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r9 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r10 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r11 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r12 = r4.query(r5, r6, r7, r8, r9, r10, r11);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r12.moveToFirst();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x0213:
        r5 = r12.isAfterLast();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        if (r5 != 0) goto L_0x0250;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x0219:
        r19.clearBindings();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getLong(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 2;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getInt(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = (long) r6;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 3;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 2;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getLong(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r19.execute();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        if (r26 == 0) goto L_0x024c;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x0243:
        r18 = r18 + 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r26;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r1 = r18;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.onProgressChanged(r1);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x024c:
        r12.moveToNext();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        goto L_0x0213;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x0250:
        r12.close();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.setTransactionSuccessful();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.endTransaction();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = logger;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "  imported feature names";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.error(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "REPLACE INTO tiles VALUES (?,?,?,?)";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r19 = r5.compileStatement(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.beginTransaction();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = "tiles";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_TILES;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r7 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r9 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r10 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r11 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r12 = r4.query(r5, r6, r7, r8, r9, r10, r11);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r12.moveToFirst();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x0289:
        r5 = r12.isAfterLast();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        if (r5 != 0) goto L_0x02d3;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x028f:
        r19.clearBindings();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getInt(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = (long) r6;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 2;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getInt(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = (long) r6;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 3;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 2;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getInt(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = (long) r6;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindLong(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = 4;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 3;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = r12.getBlob(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r19;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.bindBlob(r5, r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r19.execute();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        if (r26 == 0) goto L_0x02cf;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x02c6:
        r18 = r18 + 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r26;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r1 = r18;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.onProgressChanged(r1);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x02cf:
        r12.moveToNext();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        goto L_0x0289;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x02d3:
        r12.close();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.setTransactionSuccessful();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r0.mMapsDatabase;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.endTransaction();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = logger;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = "  imported tiles";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5.error(r6);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r13 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = "metadata";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6 = new java.lang.String[r6];	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r7 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = "value";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r6[r7] = r8;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r7 = "name = ?";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = 1;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8 = new java.lang.String[r8];	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r9 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r10 = "timestamp";	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r8[r9] = r10;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r9 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r10 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r11 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r12 = r4.query(r5, r6, r7, r8, r9, r10, r11);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r12.moveToFirst();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        if (r5 == 0) goto L_0x031a;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x030d:
        r5 = 0;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = r12.getString(r5);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r5 = java.lang.Short.valueOf(r5);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r13 = r5.shortValue();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
    L_0x031a:
        r12.close();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r4.close();	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r1 = r23;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r2 = r24;	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0.setDownloaded(r1, r2, r13);	 Catch:{ SQLiteException -> 0x00e3, all -> 0x01bf }
        r0 = r22;
        r5 = r0.mMapsDatabase;
        r5 = r5.inTransaction();
        if (r5 == 0) goto L_0x033a;
    L_0x0333:
        r0 = r22;
        r5 = r0.mMapsDatabase;
        r5.endTransaction();
    L_0x033a:
        if (r26 == 0) goto L_0x033f;
    L_0x033c:
        r26.onProgressFinished();
    L_0x033f:
        r17.delete();
        r5 = 1;
        goto L_0x0119;
        */
        throw new UnsupportedOperationException("Method not decompiled: mobi.maptrek.maps.maptrek.Index.processDownloadedMap(int, int, java.lang.String, mobi.maptrek.util.ProgressListener):boolean");
    }

    public Index(Context context, SQLiteDatabase mapsDatabase, SQLiteDatabase hillshadesDatabase) {
        this.mContext = context;
        this.mMapsDatabase = mapsDatabase;
        this.mHillshadeDatabase = hillshadesDatabase;
        this.mDownloadManager = (DownloadManager) context.getSystemService("download");
        try {
            Cursor cursor = this.mMapsDatabase.query("maps", MapTrekDatabaseHelper.ALL_COLUMNS_MAPS, "date > 0 OR downloading > 0", null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                int x = cursor.getInt(cursor.getColumnIndex(MapService.EXTRA_X));
                int y = cursor.getInt(cursor.getColumnIndex(MapService.EXTRA_Y));
                short date = cursor.getShort(cursor.getColumnIndex("date"));
                if (x == -1 && y == -1) {
                    this.mBaseMapVersion = date;
                    cursor.moveToNext();
                } else {
                    byte version = (byte) cursor.getShort(cursor.getColumnIndex("version"));
                    long downloading = cursor.getLong(cursor.getColumnIndex("downloading"));
                    long hillshadeDownloading = cursor.getLong(cursor.getColumnIndex("hillshade_downloading"));
                    MapStatus mapStatus = getNativeMap(x, y);
                    mapStatus.created = date;
                    mapStatus.hillshadeVersion = version;
                    logger.debug("index({}, {}, {}, {})", (Object[]) new Object[]{Integer.valueOf(x), Integer.valueOf(y), Short.valueOf(date), Byte.valueOf(version)});
                    int status = checkDownloadStatus(downloading);
                    if (status == 4 || status == 1 || status == 2) {
                        mapStatus.downloading = downloading;
                        logger.debug("  map downloading: {}", (Object) Long.valueOf(downloading));
                    } else {
                        downloading = 0;
                        setDownloading(x, y, 0, hillshadeDownloading);
                        logger.debug("  cleared");
                    }
                    status = checkDownloadStatus(hillshadeDownloading);
                    if (status == 4 || status == 1 || status == 2) {
                        mapStatus.hillshadeDownloading = hillshadeDownloading;
                        logger.debug("  hillshade downloading: {}", (Object) Long.valueOf(downloading));
                    } else {
                        setDownloading(x, y, downloading, 0);
                        logger.debug("  cleared");
                    }
                    if (date > (short) 0) {
                        this.mLoadedMaps++;
                    }
                    cursor.moveToNext();
                }
            }
            cursor.close();
        } catch (Throwable e) {
            logger.error("Failed to read map index", e);
            this.mMapsDatabase.execSQL("CREATE TABLE IF NOT EXISTS maps (x INTEGER NOT NULL, y INTEGER NOT NULL, date INTEGER NOT NULL DEFAULT 0, version INTEGER NOT NULL DEFAULT 0, downloading INTEGER NOT NULL DEFAULT 0, hillshade_downloading INTEGER NOT NULL DEFAULT 0)");
            this.mMapsDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS maps_x_y ON maps (x, y)");
        }
        this.mHasHillshades = DatabaseUtils.queryNumEntries(this.mHillshadeDatabase, "tiles") > 0;
    }

    public short getBaseMapVersion() {
        return this.mBaseMapVersion;
    }

    public boolean isBaseMapOutdated() {
        return this.mBaseMapVersion > (short) 0 && this.mBaseMapVersion < this.mBaseMapDownloadVersion;
    }

    public boolean hasHillshades() {
        return this.mHasHillshades;
    }

    public long getBaseMapSize() {
        return this.mBaseMapDownloadSize > 0 ? this.mBaseMapDownloadSize : 42991616;
    }

    public long getMapDatabaseSize() {
        return new File(this.mMapsDatabase.getPath()).length() + new File(this.mHillshadeDatabase.getPath()).length();
    }

    public void setBaseMapStatus(short date, int size) {
        this.mBaseMapDownloadVersion = date;
        this.mBaseMapDownloadSize = (long) size;
    }

    @NonNull
    public MapStatus getNativeMap(int x, int y) {
        if (this.mMaps[x][y] == null) {
            this.mMaps[x][y] = new MapStatus();
        }
        return this.mMaps[x][y];
    }

    public void selectNativeMap(int x, int y, ACTION action) {
        IndexStats stats = getMapStats();
        MapStatus mapStatus = getNativeMap(x, y);
        if (mapStatus.action == action) {
            mapStatus.action = ACTION.NONE;
            if (action == ACTION.DOWNLOAD) {
                stats.download--;
                if (this.mHasDownloadSizes) {
                    stats.downloadSize -= mapStatus.downloadSize;
                    if (this.mAccountHillshades) {
                        stats.downloadSize -= mapStatus.hillshadeDownloadSize;
                    }
                }
            }
            if (action == ACTION.REMOVE) {
                stats.remove--;
            }
        } else if (action == ACTION.DOWNLOAD) {
            mapStatus.action = action;
            stats.download++;
            if (this.mHasDownloadSizes) {
                stats.downloadSize += mapStatus.downloadSize;
                if (this.mAccountHillshades) {
                    stats.downloadSize += mapStatus.hillshadeDownloadSize;
                }
            }
        } else if (action == ACTION.REMOVE) {
            mapStatus.action = action;
            stats.remove++;
        }
        for (WeakReference<MapStateListener> weakRef : this.mMapStateListeners) {
            MapStateListener mapStateListener = (MapStateListener) weakRef.get();
            if (mapStateListener != null) {
                mapStateListener.onMapSelected(x, y, mapStatus.action, stats);
            }
        }
    }

    public void removeNativeMap(int x, int y, @Nullable ProgressListener progressListener) {
        if (this.mMaps[x][y] != null && this.mMaps[x][y].created != (short) 0) {
            boolean hillshades = ((long) this.mMaps[x][y].hillshadeVersion) != 0;
            logger.error("Removing map: {} {}", Integer.valueOf(x), Integer.valueOf(y));
            if (progressListener != null) {
                progressListener.onProgressStarted(100);
            }
            try {
                SQLiteStatement statement = this.mMapsDatabase.compileStatement("DELETE FROM tiles WHERE zoom_level = ? AND tile_column >= ? AND tile_column <= ? AND tile_row >= ? AND tile_row <= ?");
                SQLiteStatement hillshadeStatement = this.mHillshadeDatabase.compileStatement("DELETE FROM tiles WHERE zoom_level = ? AND tile_column >= ? AND tile_column <= ? AND tile_row >= ? AND tile_row <= ?");
                int z = 8;
                while (z < 15) {
                    int s = z - 7;
                    int cmin = x << s;
                    int cmax = ((x + 1) << s) - 1;
                    int rmin = y << s;
                    int rmax = ((y + 1) << s) - 1;
                    statement.clearBindings();
                    statement.bindLong(1, (long) z);
                    statement.bindLong(2, (long) cmin);
                    statement.bindLong(3, (long) cmax);
                    statement.bindLong(4, (long) rmin);
                    statement.bindLong(5, (long) rmax);
                    statement.executeUpdateDelete();
                    if (hillshades && z < 13) {
                        hillshadeStatement.clearBindings();
                        hillshadeStatement.bindLong(1, (long) z);
                        hillshadeStatement.bindLong(2, (long) cmin);
                        hillshadeStatement.bindLong(3, (long) cmax);
                        hillshadeStatement.bindLong(4, (long) rmin);
                        hillshadeStatement.bindLong(5, (long) rmax);
                        hillshadeStatement.executeUpdateDelete();
                    }
                    z++;
                }
                if (progressListener != null) {
                    progressListener.onProgressChanged(10);
                }
                logger.error("  removed tiles");
                statement = this.mMapsDatabase.compileStatement("DELETE FROM features WHERE id IN (SELECT a.feature FROM map_features AS a LEFT JOIN map_features AS b ON (a.feature = b.feature AND (a.x != b.x OR a.y != b.y)) WHERE a.x = ? AND a.y = ? AND b.feature IS NULL)");
                statement.bindLong(1, (long) x);
                statement.bindLong(2, (long) y);
                statement.executeUpdateDelete();
                if (progressListener != null) {
                    progressListener.onProgressChanged(20);
                }
                logger.error("  removed features");
                this.mMapsDatabase.compileStatement("DELETE FROM feature_names WHERE id IN (SELECT feature_names.id FROM feature_names LEFT JOIN features ON (feature_names.id = features.id) WHERE features.id IS NULL)").executeUpdateDelete();
                if (progressListener != null) {
                    progressListener.onProgressChanged(40);
                }
                logger.error("  removed feature names");
                if (MapTrekDatabaseHelper.hasFullTextIndex(this.mMapsDatabase)) {
                    ArrayList<Long> ids = new ArrayList();
                    Cursor cursor = this.mMapsDatabase.rawQuery("SELECT ref FROM names LEFT JOIN feature_names ON (ref = feature_names.name) WHERE id IS NULL", null);
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        ids.add(Long.valueOf(cursor.getLong(0)));
                        cursor.moveToNext();
                    }
                    cursor.close();
                    if (ids.size() > 0) {
                        StringBuilder sql = new StringBuilder();
                        sql.append("DELETE FROM names_fts WHERE docid IN (");
                        String sep = "";
                        Iterator it = ids.iterator();
                        while (it.hasNext()) {
                            Long id = (Long) it.next();
                            sql.append(sep);
                            sql.append(String.valueOf(id));
                            sep = ",";
                        }
                        sql.append(")");
                        this.mMapsDatabase.compileStatement(sql.toString()).executeUpdateDelete();
                    }
                    if (progressListener != null) {
                        progressListener.onProgressChanged(60);
                    }
                    logger.error("  removed names fts");
                }
                this.mMapsDatabase.compileStatement("DELETE FROM names WHERE ref IN (SELECT ref FROM names LEFT JOIN feature_names ON (ref = feature_names.name) WHERE id IS NULL)").executeUpdateDelete();
                if (progressListener != null) {
                    progressListener.onProgressChanged(100);
                }
                logger.error("  removed names");
                setDownloaded(x, y, (short) 0);
                setHillshadeDownloaded(x, y, (byte) 0);
                if (progressListener != null) {
                    progressListener.onProgressFinished();
                }
            } catch (Throwable e) {
                logger.error("Query error", e);
            }
        }
    }

    public void setNativeMapStatus(int x, int y, short date, long size) {
        if (this.mMaps[x][y] == null) {
            getNativeMap(x, y);
        }
        this.mMaps[x][y].downloadCreated = date;
        this.mMaps[x][y].downloadSize = size;
    }

    public void accountHillshades(boolean account) {
        this.mAccountHillshades = account;
        for (WeakReference<MapStateListener> weakRef : this.mMapStateListeners) {
            MapStateListener mapStateListener = (MapStateListener) weakRef.get();
            if (mapStateListener != null) {
                mapStateListener.onHillshadeAccountingChanged(account);
            }
        }
    }

    public void setHillshadeStatus(int x, int y, byte version, long size) {
        if (this.mMaps[x][y] == null) {
            getNativeMap(x, y);
        }
        this.mMaps[x][y].hillshadeDownloadVersion = version;
        this.mMaps[x][y].hillshadeDownloadSize = size;
    }

    public void clearSelections() {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                if (this.mMaps[x][y] != null) {
                    this.mMaps[x][y].action = ACTION.NONE;
                }
            }
        }
    }

    public void cancelDownload(int x, int y) {
        MapStatus map = getNativeMap(x, y);
        this.mDownloadManager.remove(new long[]{map.downloading});
        if (map.hillshadeDownloading != 0) {
            this.mDownloadManager.remove(new long[]{map.hillshadeDownloading});
        }
        setDownloading(x, y, 0, 0);
        selectNativeMap(x, y, ACTION.NONE);
    }

    public boolean isDownloading(int x, int y) {
        return (this.mMaps[x][y] == null || this.mMaps[x][y].downloading == 0) ? false : true;
    }

    public IndexStats getMapStats() {
        IndexStats stats = new IndexStats();
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                MapStatus mapStatus = getNativeMap(x, y);
                if (mapStatus.action == ACTION.DOWNLOAD) {
                    stats.download++;
                    if (this.mHasDownloadSizes) {
                        stats.downloadSize += mapStatus.downloadSize;
                        if (this.mAccountHillshades) {
                            stats.downloadSize += mapStatus.hillshadeDownloadSize;
                        }
                    }
                }
                if (mapStatus.action == ACTION.REMOVE) {
                    stats.remove++;
                }
                if (mapStatus.downloading != 0) {
                    stats.downloading++;
                }
            }
        }
        stats.loaded = this.mLoadedMaps;
        return stats;
    }

    public void downloadBaseMap() {
        Request request = new Request(new Builder().scheme("https").authority("maptrek.mobi").appendPath("maps").appendPath(BASEMAP_FILENAME).build());
        request.setTitle(this.mContext.getString(R.string.baseMapTitle));
        request.setDescription(this.mContext.getString(R.string.app_name));
        File root = new File(this.mMapsDatabase.getPath()).getParentFile();
        File file = new File(root, BASEMAP_FILENAME);
        if (file.exists()) {
            file.delete();
        }
        request.setDestinationInExternalFilesDir(this.mContext, root.getName(), BASEMAP_FILENAME);
        request.setVisibleInDownloadsUi(false);
        this.mDownloadManager.enqueue(request);
    }

    public void manageNativeMaps(boolean hillshadesEnabled) {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                MapStatus mapStatus = getNativeMap(x, y);
                if (mapStatus.action != ACTION.NONE) {
                    if (mapStatus.action == ACTION.REMOVE) {
                        Intent deleteIntent = new Intent("android.intent.action.DELETE", null, this.mContext, MapService.class);
                        deleteIntent.putExtra(MapService.EXTRA_X, x);
                        deleteIntent.putExtra(MapService.EXTRA_Y, y);
                        this.mContext.startService(deleteIntent);
                        mapStatus.action = ACTION.NONE;
                    } else {
                        long mapDownloadId = requestDownload(x, y, false);
                        long hillshadeDownloadId = 0;
                        if (hillshadesEnabled && mapStatus.hillshadeDownloadVersion > mapStatus.hillshadeVersion) {
                            hillshadeDownloadId = requestDownload(x, y, true);
                        }
                        setDownloading(x, y, mapDownloadId, hillshadeDownloadId);
                        mapStatus.action = ACTION.NONE;
                    }
                }
            }
        }
    }

    private long requestDownload(int x, int y, boolean hillshade) {
        String ext = hillshade ? "mbtiles" : "mtiles";
        String fileName = String.format(Locale.ENGLISH, "%d-%d.%s", new Object[]{Integer.valueOf(x), Integer.valueOf(y), ext});
        Request request = new Request(new Builder().scheme("https").authority("maptrek.mobi").appendPath(hillshade ? "hillshades" : "maps").appendPath(String.valueOf(x)).appendPath(fileName).build());
        request.setTitle(this.mContext.getString(hillshade ? R.string.hillshadeTitle : R.string.mapTitle, new Object[]{Integer.valueOf(x), Integer.valueOf(y)}));
        request.setDescription(this.mContext.getString(R.string.app_name));
        File root = new File(this.mMapsDatabase.getPath()).getParentFile();
        File file = new File(root, fileName);
        if (file.exists()) {
            file.delete();
        }
        request.setDestinationInExternalFilesDir(this.mContext, root.getName(), fileName);
        request.setVisibleInDownloadsUi(false);
        return this.mDownloadManager.enqueue(request);
    }

    private void setDownloaded(int x, int y, short date) {
        ContentValues values = new ContentValues();
        values.put("date", Short.valueOf(date));
        values.put("downloading", Long.valueOf(0));
        if (this.mMapsDatabase.update("maps", values, "x = ? AND y = ?", new String[]{String.valueOf(x), String.valueOf(y)}) == 0) {
            values.put(MapService.EXTRA_X, Integer.valueOf(x));
            values.put(MapService.EXTRA_Y, Integer.valueOf(y));
            this.mMapsDatabase.insert("maps", null, values);
        }
        if (x == -1 && y == -1) {
            this.mBaseMapVersion = date;
        } else if (x >= 0 && y >= 0) {
            MapStatus mapStatus = getNativeMap(x, y);
            mapStatus.created = date;
            mapStatus.downloading = 0;
            for (WeakReference<MapStateListener> weakRef : this.mMapStateListeners) {
                MapStateListener mapStateListener = (MapStateListener) weakRef.get();
                if (mapStateListener != null) {
                    mapStateListener.onStatsChanged();
                }
            }
        }
    }

    private void setHillshadeDownloaded(int x, int y, byte version) {
        ContentValues values = new ContentValues();
        values.put("version", Byte.valueOf(version));
        values.put("hillshade_downloading", Long.valueOf(0));
        if (this.mMapsDatabase.update("maps", values, "x = ? AND y = ?", new String[]{String.valueOf(x), String.valueOf(y)}) == 0) {
            values.put(MapService.EXTRA_X, Integer.valueOf(x));
            values.put(MapService.EXTRA_Y, Integer.valueOf(y));
            this.mMapsDatabase.insert("maps", null, values);
        }
        MapStatus mapStatus = getNativeMap(x, y);
        mapStatus.hillshadeVersion = version;
        mapStatus.hillshadeDownloading = 0;
        for (WeakReference<MapStateListener> weakRef : this.mMapStateListeners) {
            MapStateListener mapStateListener = (MapStateListener) weakRef.get();
            if (mapStateListener != null) {
                mapStateListener.onStatsChanged();
            }
        }
    }

    private void setDownloading(int x, int y, long enqueue, long hillshadeEnquire) {
        final long j = enqueue;
        final int i = x;
        final int i2 = y;
        new Thread(new Runnable() {
            public void run() {
                ContentValues values = new ContentValues();
                values.put("downloading", Long.valueOf(j));
                if (Index.this.mMapsDatabase.update("maps", values, "x = ? AND y = ?", new String[]{String.valueOf(i), String.valueOf(i2)}) == 0) {
                    values.put(MapService.EXTRA_X, Integer.valueOf(i));
                    values.put(MapService.EXTRA_Y, Integer.valueOf(i2));
                    Index.this.mMapsDatabase.insert("maps", null, values);
                }
            }
        }).start();
        if (x >= 0 && y >= 0) {
            MapStatus mapStatus = getNativeMap(x, y);
            mapStatus.downloading = enqueue;
            mapStatus.hillshadeDownloading = hillshadeEnquire;
            for (WeakReference<MapStateListener> weakRef : this.mMapStateListeners) {
                MapStateListener mapStateListener = (MapStateListener) weakRef.get();
                if (mapStateListener != null) {
                    mapStateListener.onStatsChanged();
                }
            }
        }
    }

    private int checkDownloadStatus(long enqueue) {
        Query query = new Query();
        query.setFilterById(new long[]{enqueue});
        Cursor c = this.mDownloadManager.query(query);
        int status = 0;
        if (c.moveToFirst()) {
            status = c.getInt(c.getColumnIndex("status"));
        }
        c.close();
        return status;
    }

    public boolean hasDownloadSizes() {
        return this.mHasDownloadSizes;
    }

    public boolean expiredDownloadSizes() {
        return this.mExpiredDownloadSizes;
    }

    public void setHasDownloadSizes(boolean hasSizes, boolean expired) {
        this.mHasDownloadSizes = hasSizes;
        this.mExpiredDownloadSizes = expired;
        if (hasSizes) {
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    MapStatus mapStatus = getNativeMap(x, y);
                    if (mapStatus.action == ACTION.DOWNLOAD && mapStatus.downloadSize == 0) {
                        selectNativeMap(x, y, ACTION.NONE);
                    }
                }
            }
            for (WeakReference<MapStateListener> weakRef : this.mMapStateListeners) {
                MapStateListener mapStateListener = (MapStateListener) weakRef.get();
                if (mapStateListener != null) {
                    mapStateListener.onHasDownloadSizes();
                }
            }
        }
    }

    public void addMapStateListener(MapStateListener listener) {
        this.mMapStateListeners.add(new WeakReference(listener));
    }

    public void removeMapStateListener(MapStateListener listener) {
        Iterator<WeakReference<MapStateListener>> iterator = this.mMapStateListeners.iterator();
        while (iterator.hasNext()) {
            if (((WeakReference) iterator.next()).get() == listener) {
                iterator.remove();
            }
        }
    }

    public static Uri getIndexUri() {
        return new Builder().scheme("https").authority("maptrek.mobi").appendPath("maps").appendPath(BaseNavigationService.EXTRA_ROUTE_INDEX).build();
    }

    public static Uri getHillshadeIndexUri() {
        return new Builder().scheme("https").authority("maptrek.mobi").appendPath("hillshades").appendPath(BaseNavigationService.EXTRA_ROUTE_INDEX).build();
    }

    public int getMapsCount() {
        return this.mLoadedMaps;
    }
}
