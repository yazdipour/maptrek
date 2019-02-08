package mobi.maptrek.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import java.util.ArrayList;
import java.util.HashMap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapElement;
import org.oscim.core.MapPosition;
import org.oscim.layers.tile.JobQueue;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.MapTile.TileNode;
import org.oscim.layers.tile.TileDistanceSort;
import org.oscim.layers.tile.TileSet;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.QueryResult;
import org.oscim.tiling.TileSource;
import org.oscim.utils.PausableThread;
import org.oscim.utils.ScanBox;
import org.oscim.utils.quadtree.TileIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitmapTileMapPreviewView extends TextureView implements SurfaceTextureListener {
    private static final int BITMAP_TILE_SIZE = 256;
    private static final int CACHE_LIMIT = 40;
    private static final Logger logger = LoggerFactory.getLogger(BitmapTileMapPreviewView.class);
    private boolean mActive;
    private DrawThread mDrawThread;
    private int mHalfHeight;
    private int mHalfWidth;
    private final TileIndex<TileNode, MapTile> mIndex = new TileIndex<TileNode, MapTile>() {
        public void removeItem(MapTile t) {
            if (t.node == null) {
                throw new IllegalStateException("Already removed: " + t);
            }
            super.remove(t.node);
            t.node.item = null;
        }

        public TileNode create() {
            return new TileNode();
        }
    };
    private final JobQueue mJobQueue;
    private final ArrayList<MapTile> mJobs;
    private final float[] mMapPlane = new float[8];
    private TileSet mNewTiles;
    private MapPosition mPosition;
    private final ScanBox mScanBox = new ScanBox() {
        protected void setVisible(int y, int x1, int x2) {
            MapTile[] tiles = BitmapTileMapPreviewView.this.mNewTiles.tiles;
            int cnt = BitmapTileMapPreviewView.this.mNewTiles.cnt;
            int maxTiles = tiles.length;
            int xmax = 1 << this.mZoom;
            int x = x1;
            int cnt2 = cnt;
            while (x < x2) {
                MapTile tile = null;
                if (cnt2 == maxTiles) {
                    BitmapTileMapPreviewView.logger.warn("too many tiles {}", Integer.valueOf(maxTiles));
                    break;
                }
                int xx = x;
                if (x < 0 || x >= xmax) {
                    if (x < 0) {
                        xx = xmax + x;
                    } else {
                        xx = x - xmax;
                    }
                    if (xx >= 0) {
                        if (xx >= xmax) {
                            cnt = cnt2;
                            x++;
                            cnt2 = cnt;
                        }
                    }
                    cnt = cnt2;
                    x++;
                    cnt2 = cnt;
                }
                int i = 0;
                while (i < cnt2) {
                    if (tiles[i].tileX == xx && tiles[i].tileY == y) {
                        tile = tiles[i];
                        break;
                    }
                    i++;
                }
                if (tile == null) {
                    cnt = cnt2 + 1;
                    tiles[cnt2] = BitmapTileMapPreviewView.this.addTile(xx, y, this.mZoom);
                    x++;
                    cnt2 = cnt;
                }
                cnt = cnt2;
                x++;
                cnt2 = cnt;
            }
            BitmapTileMapPreviewView.this.mNewTiles.cnt = cnt2;
        }
    };
    protected BitmapTileLoader mTileLoader;
    private HashMap<MapTile, Bitmap> mTileMap;
    private TileSource mTileSource;
    private MapTile[] mTiles;
    private int mTilesCount;
    private int mTilesEnd;

    class DrawThread extends Thread {
        private boolean mDone = false;
        private final Object mWaitLock = new Object();

        DrawThread() {
        }

        private void doDraw(Canvas canvas) {
            int i = 0;
            double tileScale = 256.0d * BitmapTileMapPreviewView.this.mPosition.scale;
            canvas.drawColor(0, Mode.CLEAR);
            MapTile[] mapTileArr = BitmapTileMapPreviewView.this.mNewTiles.tiles;
            int length = mapTileArr.length;
            while (i < length) {
                MapTile tile = mapTileArr[i];
                if (tile != null && tile.getState() == 4) {
                    Bitmap bitmap = (Bitmap) BitmapTileMapPreviewView.this.mTileMap.get(tile);
                    if (bitmap != null) {
                        canvas.drawBitmap(bitmap, ((float) ((tile.x - BitmapTileMapPreviewView.this.mPosition.x) * tileScale)) + ((float) BitmapTileMapPreviewView.this.mHalfWidth), ((float) ((tile.y - BitmapTileMapPreviewView.this.mPosition.y) * tileScale)) + ((float) BitmapTileMapPreviewView.this.mHalfHeight), null);
                    }
                }
                i++;
            }
        }

        public void run() {
            while (!this.mDone) {
                try {
                    Canvas c = BitmapTileMapPreviewView.this.lockCanvas(null);
                    if (c != null) {
                        doDraw(c);
                    }
                    if (c != null) {
                        BitmapTileMapPreviewView.this.unlockCanvasAndPost(c);
                    }
                    BitmapTileMapPreviewView.logger.debug("  finished drawing");
                    synchronized (this.mWaitLock) {
                        try {
                            BitmapTileMapPreviewView.logger.debug("  waiting...");
                            if (!this.mDone) {
                                this.mWaitLock.wait();
                            }
                            BitmapTileMapPreviewView.logger.debug("  notified, done: {}", Boolean.valueOf(this.mDone));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (Throwable th) {
                    if (null != null) {
                        BitmapTileMapPreviewView.this.unlockCanvasAndPost(null);
                    }
                }
            }
        }

        void halt() {
            BitmapTileMapPreviewView.logger.debug("  try to halt");
            synchronized (this.mWaitLock) {
                BitmapTileMapPreviewView.logger.debug("  halt");
                this.mDone = true;
                this.mWaitLock.notify();
            }
        }
    }

    class BitmapTileLoader extends PausableThread implements ITileDataSink {
        private final String THREAD_NAME = "BitmapTileLoader";
        MapTile mTile;

        BitmapTileLoader() {
        }

        boolean loadTile(MapTile tile) {
            try {
                BitmapTileMapPreviewView.this.mTileSource.getDataSource().query(tile, this);
                return true;
            } catch (Exception e) {
                BitmapTileMapPreviewView.logger.error("{}: {}", (Object) tile, e.getMessage());
                return false;
            }
        }

        void go() {
            synchronized (this) {
                notify();
            }
        }

        protected void doWork() {
            this.mTile = BitmapTileMapPreviewView.this.mJobQueue.poll();
            if (this.mTile != null) {
                try {
                    BitmapTileMapPreviewView.logger.debug("{} : {} {}", BitmapTileMapPreviewView.this.mTileSource.getOption("path"), this.mTile, this.mTile.state());
                    loadTile(this.mTile);
                } catch (Exception e) {
                    e.printStackTrace();
                    completed(QueryResult.FAILED);
                }
            }
        }

        protected String getThreadName() {
            return this.THREAD_NAME;
        }

        protected int getThreadPriority() {
            return 3;
        }

        protected boolean hasWork() {
            return !BitmapTileMapPreviewView.this.mJobQueue.isEmpty();
        }

        void dispose() {
            BitmapTileMapPreviewView.this.mTileSource.getDataSource().dispose();
        }

        public void cancel() {
            BitmapTileMapPreviewView.this.mTileSource.getDataSource().cancel();
        }

        public void completed(QueryResult result) {
            boolean ok = result == QueryResult.SUCCESS;
            if (ok && (isCanceled() || isInterrupted())) {
                ok = false;
            }
            BitmapTileMapPreviewView.this.jobCompleted(this.mTile, ok);
            this.mTile = null;
        }

        public void process(MapElement element) {
        }

        public void setTileImage(org.oscim.backend.canvas.Bitmap bitmap) {
            if (isCanceled() || !this.mTile.state(2)) {
                bitmap.recycle();
                return;
            }
            BitmapTileMapPreviewView.this.mTileMap.put(this.mTile, Bitmap.createBitmap(bitmap.getPixels(), bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888));
        }
    }

    public BitmapTileMapPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        logger.debug("BitmapTileMapPreviewView");
        setSurfaceTextureListener(this);
        setOpaque(false);
        this.mJobQueue = new JobQueue();
        this.mJobs = new ArrayList();
        this.mTiles = new MapTile[40];
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        logger.debug("onSurfaceTextureAvailable({},{})", Integer.valueOf(width), Integer.valueOf(height));
        if (!this.mActive) {
            this.mTileSource.open();
        }
        this.mIndex.drop();
        this.mTileMap = new HashMap();
        this.mTilesEnd = 0;
        this.mTilesCount = 0;
        this.mHalfWidth = width / 2;
        this.mHalfHeight = height / 2;
        this.mNewTiles = new TileSet((((width / 256) + 1) * ((height / 256) + 1)) * 4);
        this.mNewTiles.cnt = 0;
        this.mMapPlane[0] = (float) (this.mHalfWidth + 256);
        this.mMapPlane[1] = (float) (this.mHalfHeight + 256);
        this.mMapPlane[2] = -this.mMapPlane[0];
        this.mMapPlane[3] = this.mMapPlane[1];
        this.mMapPlane[4] = -this.mMapPlane[0];
        this.mMapPlane[5] = -this.mMapPlane[1];
        this.mMapPlane[6] = this.mMapPlane[0];
        this.mMapPlane[7] = -this.mMapPlane[1];
        this.mScanBox.scan(this.mPosition.x, this.mPosition.y, this.mPosition.scale, this.mPosition.zoomLevel, this.mMapPlane);
        if (!this.mJobs.isEmpty()) {
            this.mJobQueue.setJobs((MapTile[]) this.mJobs.toArray(new MapTile[this.mJobs.size()]));
            this.mJobs.clear();
            this.mDrawThread = new DrawThread();
            this.mDrawThread.start();
            this.mTileLoader = new BitmapTileLoader();
            this.mTileLoader.start();
            this.mTileLoader.go();
        }
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        logger.debug("onSurfaceTextureDestroyed()");
        this.mTileLoader.pause();
        this.mTileLoader.finish();
        this.mTileLoader.dispose();
        try {
            this.mTileLoader.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.debug("  loader stopped");
        this.mJobQueue.clear();
        logger.debug("  queue cleared");
        this.mDrawThread.halt();
        try {
            this.mDrawThread.join();
        } catch (InterruptedException e2) {
            e2.printStackTrace();
        }
        logger.debug("  drawer stopped");
        this.mNewTiles.releaseTiles();
        this.mNewTiles = null;
        for (Bitmap bitmap : this.mTileMap.values()) {
            bitmap.recycle();
        }
        this.mTileMap.clear();
        this.mTileMap = null;
        if (!this.mActive) {
            this.mTileSource.close();
        }
        logger.debug("  finished");
        return true;
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        logger.debug("onSurfaceTextureSizeChanged({},{})", Integer.valueOf(width), Integer.valueOf(height));
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        logger.debug("onSurfaceTextureUpdated");
    }

    public void setTileSource(TileSource tileSource, boolean active) {
        this.mTileSource = tileSource;
        this.mActive = active;
        invalidate();
    }

    public void setLocation(GeoPoint location, int zoomLevel) {
        if (this.mTileSource == null) {
            logger.warn("Source should not be null");
            return;
        }
        this.mPosition = new MapPosition(location.getLatitude(), location.getLongitude(), 1.0d);
        this.mPosition.setZoomLevel(zoomLevel);
    }

    MapTile addTile(int x, int y, int zoomLevel) {
        MapTile tile = (MapTile) this.mIndex.getTile(x, y, zoomLevel);
        if (tile == null) {
            TileNode n = (TileNode) this.mIndex.add(x, y, zoomLevel);
            tile = new MapTile(n, x, y, zoomLevel);
            n.item = tile;
            tile = tile;
            tile.setState((byte) 2);
            this.mJobs.add(tile);
            addToCache(tile);
            return tile;
        } else if (tile.isActive()) {
            return tile;
        } else {
            tile.setState((byte) 2);
            this.mJobs.add(tile);
            return tile;
        }
    }

    public void jobCompleted(MapTile tile, boolean success) {
        if (success && tile.state(2)) {
            tile.setState((byte) 4);
        }
        if (!tile.state(64)) {
            logger.debug("  jobCompleted");
            synchronized (this.mDrawThread.mWaitLock) {
                logger.debug("  notify from jobCompleted");
                this.mDrawThread.mWaitLock.notify();
            }
        }
    }

    private void addToCache(MapTile tile) {
        if (this.mTilesEnd == this.mTiles.length) {
            if (this.mTilesEnd > this.mTilesCount) {
                TileDistanceSort.sort(this.mTiles, 0, this.mTilesEnd);
                this.mTilesEnd = this.mTilesCount;
            }
            if (this.mTilesEnd == this.mTiles.length) {
                logger.debug("realloc tiles {}", Integer.valueOf(this.mTilesEnd));
                MapTile[] tmp = new MapTile[(this.mTiles.length + 20)];
                System.arraycopy(this.mTiles, 0, tmp, 0, this.mTilesCount);
                this.mTiles = tmp;
            }
        }
        MapTile[] mapTileArr = this.mTiles;
        int i = this.mTilesEnd;
        this.mTilesEnd = i + 1;
        mapTileArr[i] = tile;
        this.mTilesCount++;
    }

    public void setShouldNotCloseDataSource() {
        this.mActive = true;
    }
}
