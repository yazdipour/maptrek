package mobi.maptrek.maps.maptrek;

import android.database.sqlite.SQLiteDatabase;
import java.util.HashSet;
import java.util.Iterator;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.TileSource.OpenResult;
import org.oscim.utils.geom.TileClipper;

public class MapTrekTileSource extends TileSource {
    private static final int CLIP_BUFFER = 32;
    private static final MapElement mLand = new MapElement();
    private boolean mContoursEnabled = true;
    private final HashSet<MapTrekDataSource> mMapTrekDataSources;
    private final SQLiteDatabase mNativeMapDatabase;
    private OnDataMissingListener mOnDataMissingListener;

    public interface OnDataMissingListener {
        void onDataMissing(int i, int i2, byte b);
    }

    private class NativeDataSource implements ITileDataSource {
        private MapTrekDataSource mNativeDataSource;

        NativeDataSource(MapTrekDataSource nativeDataSource) {
            this.mNativeDataSource = nativeDataSource;
        }

        public void query(MapTile tile, ITileDataSink mapDataSink) {
            mapDataSink.process(MapTrekTileSource.mLand);
            ProxyTileDataSink proxyDataSink = new ProxyTileDataSink(mapDataSink);
            this.mNativeDataSource.query(tile, proxyDataSink);
            if (proxyDataSink.result == QueryResult.TILE_NOT_FOUND && ((double) tile.distance) == 0.0d && MapTrekTileSource.this.mOnDataMissingListener != null) {
                MapTrekTileSource.this.mOnDataMissingListener.onDataMissing(tile.tileX >> (tile.zoomLevel - 7), tile.tileY >> (tile.zoomLevel - 7), (byte) 7);
            }
            if (proxyDataSink.result != QueryResult.SUCCESS) {
                MapTile baseTile = tile;
                ITileDataSink dataSink = mapDataSink;
                if (tile.zoomLevel > (byte) 7) {
                    int dz = tile.zoomLevel - 7;
                    baseTile = new MapTile(tile.node, tile.tileX >> dz, tile.tileY >> dz, 7);
                    dataSink = new TransformTileDataSink(baseTile, tile, mapDataSink);
                }
                this.mNativeDataSource.query(baseTile, dataSink);
                return;
            }
            mapDataSink.completed(proxyDataSink.result);
        }

        public void dispose() {
            this.mNativeDataSource.dispose();
        }

        public void cancel() {
            this.mNativeDataSource.cancel();
        }
    }

    private class ProxyTileDataSink implements ITileDataSink {
        boolean hasElements = false;
        ITileDataSink mapDataSink;
        QueryResult result;

        ProxyTileDataSink(ITileDataSink mapDataSink) {
            this.mapDataSink = mapDataSink;
        }

        public void process(MapElement element) {
            this.mapDataSink.process(element);
            this.hasElements = true;
        }

        public void setTileImage(Bitmap bitmap) {
            this.mapDataSink.setTileImage(bitmap);
        }

        public void completed(QueryResult result) {
            this.result = result;
        }
    }

    private class TransformTileDataSink implements ITileDataSink {
        private final float dx;
        private final float dy;
        private TileClipper mTileClipper = new TileClipper((this.dx - 32.0f) / this.scale, (this.dy - 32.0f) / this.scale, ((this.dx + ((float) Tile.SIZE)) + 32.0f) / this.scale, ((this.dy + ((float) Tile.SIZE)) + 32.0f) / this.scale);
        ITileDataSink mapDataSink;
        private final float scale;

        TransformTileDataSink(MapTile baseTile, MapTile tile, ITileDataSink mapDataSink) {
            this.mapDataSink = mapDataSink;
            int dz = tile.zoomLevel - baseTile.zoomLevel;
            this.scale = (float) Math.pow(2.0d, (double) dz);
            this.dx = (float) ((tile.tileX - (baseTile.tileX << dz)) * Tile.SIZE);
            this.dy = (float) ((tile.tileY - (baseTile.tileY << dz)) * Tile.SIZE);
        }

        public void process(MapElement el) {
            ExtendedMapElement element = (ExtendedMapElement) el;
            if (this.mTileClipper.clip(element)) {
                element.scale(this.scale, this.scale);
                element.translate(-this.dx, -this.dy);
                if (element.hasLabelPosition && element.labelPosition != null) {
                    element.labelPosition.x = (element.labelPosition.x * this.scale) - this.dx;
                    element.labelPosition.y = (element.labelPosition.y * this.scale) - this.dy;
                    if (element.labelPosition.x < 0.0f || element.labelPosition.x > ((float) Tile.SIZE) || element.labelPosition.y < 0.0f || element.labelPosition.y > ((float) Tile.SIZE)) {
                        element.labelPosition = null;
                    }
                }
                this.mapDataSink.process(element);
            }
        }

        public void setTileImage(Bitmap bitmap) {
        }

        public void completed(QueryResult result) {
            this.mapDataSink.completed(result);
        }
    }

    static {
        mLand.tags.add(new Tag("natural", "land"));
        mLand.startPolygon();
        mLand.addPoint(-16.0f, -16.0f);
        mLand.addPoint((float) (Tile.SIZE + 16), -16.0f);
        mLand.addPoint((float) (Tile.SIZE + 16), (float) (Tile.SIZE + 16));
        mLand.addPoint(-16.0f, (float) (Tile.SIZE + 16));
    }

    public MapTrekTileSource(SQLiteDatabase nativeMapDatabase) {
        super(2, 17);
        this.mNativeMapDatabase = nativeMapDatabase;
        this.mMapTrekDataSources = new HashSet();
    }

    public void setContoursEnabled(boolean enabled) {
        this.mContoursEnabled = enabled;
        Iterator it = this.mMapTrekDataSources.iterator();
        while (it.hasNext()) {
            ((MapTrekDataSource) it.next()).setContoursEnabled(enabled);
        }
    }

    public void setOnDataMissingListener(OnDataMissingListener onDataMissingListener) {
        this.mOnDataMissingListener = onDataMissingListener;
    }

    public ITileDataSource getDataSource() {
        MapTrekDataSource mapTrekDataSource = new MapTrekDataSource(this.mNativeMapDatabase);
        mapTrekDataSource.setContoursEnabled(this.mContoursEnabled);
        this.mMapTrekDataSources.add(mapTrekDataSource);
        return new NativeDataSource(mapTrekDataSource);
    }

    public OpenResult open() {
        try {
            return OpenResult.SUCCESS;
        } catch (Exception e) {
            return new OpenResult(e.getMessage());
        }
    }

    public void close() {
    }
}
