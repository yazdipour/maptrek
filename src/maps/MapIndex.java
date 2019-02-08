package mobi.maptrek.maps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import mobi.maptrek.maps.online.OnlineTileSource;
import mobi.maptrek.maps.online.TileSourceFactory;
import mobi.maptrek.util.FileList;
import mobi.maptrek.util.MapFilenameFilter;
import org.oscim.android.cache.TileCache;
import org.oscim.core.BoundingBox;
import org.oscim.tiling.source.sqlite.SQLiteMapInfo;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapIndex implements Serializable {
    private static final BoundingBox WORLD_BOUNDING_BOX = new BoundingBox(-85.0511d, -180.0d, 85.0511d, 180.0d);
    private static final Logger logger = LoggerFactory.getLogger(MapIndex.class);
    private static final long serialVersionUID = 1;
    private final Context mContext;
    private HashSet<MapFile> mMaps = new HashSet();

    @SuppressLint({"UseSparseArrays"})
    public MapIndex(@NonNull Context context, @Nullable File root) {
        this.mContext = context;
        if (root != null) {
            logger.debug("MapIndex({})", root.getAbsolutePath());
            for (File file : FileList.getFileListing(root, new MapFilenameFilter())) {
                loadMap(file);
            }
            File nativeDir = new File(root, "native");
            if (nativeDir.exists()) {
                deleteRecursive(nativeDir);
            }
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private void loadMap(@NonNull File file) {
        logger.debug("load({})", file.getName());
        byte[] buffer = new byte[13];
        try {
            FileInputStream is = new FileInputStream(file);
            if (is.read(buffer) != buffer.length) {
                throw new IOException("Unknown map file format");
            }
            is.close();
            MapFile mapFile = new MapFile();
            if (Arrays.equals(SQLiteTileSource.MAGIC, buffer)) {
                SQLiteTileSource tileSource = new SQLiteTileSource();
                if (tileSource.setMapFile(file.getAbsolutePath()) && tileSource.open().isSuccess()) {
                    SQLiteMapInfo info = tileSource.getMapInfo();
                    mapFile.name = info.name;
                    mapFile.boundingBox = info.boundingBox;
                    mapFile.tileSource = tileSource;
                    tileSource.close();
                }
            }
            if (mapFile.tileSource != null) {
                logger.debug("  added {}", mapFile.boundingBox);
                this.mMaps.add(mapFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeOnlineMapProviders() {
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent initializationIntent = new Intent("mobi.maptrek.maps.online.provider.action.INITIALIZE");
        for (ResolveInfo provider : packageManager.queryBroadcastReceivers(initializationIntent, 0)) {
            Intent intent = new Intent();
            intent.setClassName(provider.activityInfo.packageName, provider.activityInfo.name);
            intent.setAction(initializationIntent.getAction());
            this.mContext.sendBroadcast(intent);
            for (OnlineTileSource tileSource : TileSourceFactory.fromPlugin(this.mContext, packageManager, provider)) {
                MapFile mapFile = new MapFile(tileSource.getName());
                mapFile.tileSource = tileSource;
                mapFile.boundingBox = WORLD_BOUNDING_BOX;
                this.mMaps.add(mapFile);
            }
        }
    }

    @Nullable
    public MapFile getMap(@Nullable String filename) {
        if (filename == null) {
            return null;
        }
        Iterator it = this.mMaps.iterator();
        while (it.hasNext()) {
            MapFile map = (MapFile) it.next();
            if (filename.equals(map.tileSource.getOption("path"))) {
                return map;
            }
        }
        return null;
    }

    @NonNull
    public Collection<MapFile> getMaps() {
        return this.mMaps;
    }

    public void clear() {
        Iterator it = this.mMaps.iterator();
        while (it.hasNext()) {
            MapFile map = (MapFile) it.next();
            map.tileSource.close();
            if (map.tileSource.tileCache != null && (map.tileSource.tileCache instanceof TileCache)) {
                ((TileCache) map.tileSource.tileCache).dispose();
            }
        }
        this.mMaps.clear();
    }
}
