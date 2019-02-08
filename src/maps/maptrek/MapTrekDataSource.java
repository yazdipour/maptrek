package mobi.maptrek.maps.maptrek;

import android.database.sqlite.SQLiteDatabase;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile.State;
import org.oscim.layers.tile.vector.VectorTileLoader;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;
import org.oscim.utils.geom.TileClipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MapTrekDataSource implements ITileDataSource {
    private static final int BUILDING_CLIP_BUFFER = 4;
    private static final int CLIP_BUFFER = 32;
    private static final int MAX_NATIVE_ZOOM = 14;
    private static final String SQL_GET_TILE = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";
    private static final Tag TAG_TREE = new Tag("natural", "tree");
    private static final Logger logger = LoggerFactory.getLogger(MapTrekDataSource.class);
    private boolean mContoursEnabled = true;
    private final SQLiteDatabase mDatabase;
    private final MapTrekTileDecoder mTileDecoder;

    private class NativeTileDataSink implements ITileDataSink {
        private int dx;
        private int dy;
        private TileClipper mBuildingTileClipper;
        private TileClipper mTileClipper;
        ITileDataSink mapDataSink;
        QueryResult result;
        private int scale = 1;
        private final Tile tile;

        NativeTileDataSink(ITileDataSink mapDataSink, Tile tile, int dz, int x, int y) {
            this.mapDataSink = mapDataSink;
            this.tile = tile;
            if (dz > 0) {
                this.scale = 1 << dz;
                this.dx = (tile.tileX - (x << dz)) * Tile.SIZE;
                this.dy = (tile.tileY - (y << dz)) * Tile.SIZE;
                this.mTileClipper = new TileClipper((float) ((this.dx - 32) / this.scale), (float) ((this.dy - 32) / this.scale), (float) (((this.dx + Tile.SIZE) + 32) / this.scale), (float) (((this.dy + Tile.SIZE) + 32) / this.scale));
                this.mBuildingTileClipper = new TileClipper((float) ((this.dx - 4) / this.scale), (float) ((this.dy - 4) / this.scale), (float) (((this.dx + Tile.SIZE) + 4) / this.scale), (float) (((this.dy + Tile.SIZE) + 4) / this.scale));
            }
        }

        public void process(MapElement el) {
            ExtendedMapElement element = (ExtendedMapElement) el;
            if (this.tile.zoomLevel < VectorTileLoader.STROKE_MAX_ZOOM && element.isBuildingPart && !element.isBuilding) {
                return;
            }
            if (MapTrekDataSource.this.mContoursEnabled || !element.isContour) {
                if (element.layer < 5) {
                    if (!"platform".equals(element.tags.getValue("railway"))) {
                        if (element.tags.containsKey("tunnel")) {
                            element.layer = 5;
                        }
                    } else {
                        return;
                    }
                }
                if (this.tile.zoomLevel < State.CANCEL && element.type == GeometryType.POINT && element.tags.contains(MapTrekDataSource.TAG_TREE)) {
                    GeometryBuffer geom = GeometryBuffer.makeCircle(element.getPointX(0), element.getPointY(0), 1.1f, 10);
                    element.ensurePointSize(geom.getNumPoints(), false);
                    element.type = GeometryType.POLY;
                    System.arraycopy(geom.points, 0, element.points, 0, geom.points.length);
                    element.index[0] = geom.points.length;
                    if (element.index.length > 1) {
                        element.index[1] = -1;
                    }
                }
                if (this.scale != 1) {
                    TileClipper clipper = (!element.isBuildingPart || element.isBuilding) ? this.mTileClipper : this.mBuildingTileClipper;
                    if (clipper.clip(element)) {
                        element.scale((float) this.scale, (float) this.scale);
                        element.translate((float) (-this.dx), (float) (-this.dy));
                        if (element.hasLabelPosition && element.labelPosition != null) {
                            element.labelPosition.x = (element.labelPosition.x * ((float) this.scale)) - ((float) this.dx);
                            element.labelPosition.y = (element.labelPosition.y * ((float) this.scale)) - ((float) this.dy);
                            if (element.labelPosition.x < 0.0f || element.labelPosition.x > ((float) Tile.SIZE) || element.labelPosition.y < 0.0f || element.labelPosition.y > ((float) Tile.SIZE)) {
                                element.labelPosition = null;
                            }
                        }
                    } else {
                        return;
                    }
                }
                if (element.id != 0) {
                    element.database = MapTrekDataSource.this;
                }
                this.mapDataSink.process(element);
            }
        }

        public void setTileImage(Bitmap bitmap) {
            this.mapDataSink.setTileImage(bitmap);
        }

        public void completed(QueryResult result) {
            this.result = result;
        }
    }

    MapTrekDataSource(SQLiteDatabase database) {
        this.mDatabase = database;
        this.mTileDecoder = new MapTrekTileDecoder();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void query(org.oscim.layers.tile.MapTile r18, org.oscim.tiling.ITileDataSink r19) {
        /*
        r17 = this;
        r0 = r18;
        r6 = r0.tileX;
        r0 = r18;
        r7 = r0.tileY;
        r0 = r18;
        r14 = r0.zoomLevel;
        r5 = r14 + -14;
        r2 = 14;
        if (r14 <= r2) goto L_0x0016;
    L_0x0012:
        r6 = r6 >> r5;
        r7 = r7 >> r5;
        r14 = 14;
    L_0x0016:
        r2 = 3;
        r8 = new java.lang.String[r2];
        r2 = 0;
        r3 = java.lang.String.valueOf(r14);
        r8[r2] = r3;
        r2 = 1;
        r3 = java.lang.String.valueOf(r6);
        r8[r2] = r3;
        r2 = 2;
        r3 = java.lang.String.valueOf(r7);
        r8[r2] = r3;
        r0 = r18;
        r2 = r0.zoomLevel;
        r3 = 7;
        if (r2 <= r3) goto L_0x0078;
    L_0x0035:
        r13 = org.oscim.tiling.QueryResult.TILE_NOT_FOUND;
    L_0x0037:
        r0 = r17;
        r2 = r0.mDatabase;	 Catch:{ Exception -> 0x0083 }
        r3 = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";
        r10 = r2.rawQuery(r3, r8);	 Catch:{ Exception -> 0x0083 }
        r15 = 0;
        r2 = r10.moveToFirst();	 Catch:{ Throwable -> 0x009e, all -> 0x00b7 }
        if (r2 == 0) goto L_0x006b;
    L_0x0048:
        r2 = 0;
        r9 = r10.getBlob(r2);	 Catch:{ Throwable -> 0x009e, all -> 0x00b7 }
        r1 = new mobi.maptrek.maps.maptrek.MapTrekDataSource$NativeTileDataSink;	 Catch:{ Throwable -> 0x009e, all -> 0x00b7 }
        r2 = r17;
        r3 = r19;
        r4 = r18;
        r1.<init>(r3, r4, r5, r6, r7);	 Catch:{ Throwable -> 0x009e, all -> 0x00b7 }
        r0 = r17;
        r2 = r0.mTileDecoder;	 Catch:{ Throwable -> 0x009e, all -> 0x00b7 }
        r3 = new java.io.ByteArrayInputStream;	 Catch:{ Throwable -> 0x009e, all -> 0x00b7 }
        r3.<init>(r9);	 Catch:{ Throwable -> 0x009e, all -> 0x00b7 }
        r0 = r18;
        r12 = r2.decode(r0, r1, r3);	 Catch:{ Throwable -> 0x009e, all -> 0x00b7 }
        if (r12 == 0) goto L_0x007b;
    L_0x0069:
        r13 = org.oscim.tiling.QueryResult.SUCCESS;	 Catch:{ Throwable -> 0x009e, all -> 0x00b7 }
    L_0x006b:
        if (r10 == 0) goto L_0x0072;
    L_0x006d:
        if (r15 == 0) goto L_0x0093;
    L_0x006f:
        r10.close();	 Catch:{ Throwable -> 0x007e }
    L_0x0072:
        r0 = r19;
        r0.completed(r13);
    L_0x0077:
        return;
    L_0x0078:
        r13 = org.oscim.tiling.QueryResult.SUCCESS;
        goto L_0x0037;
    L_0x007b:
        r13 = org.oscim.tiling.QueryResult.FAILED;	 Catch:{ Throwable -> 0x009e, all -> 0x00b7 }
        goto L_0x006b;
    L_0x007e:
        r2 = move-exception;
        r15.addSuppressed(r2);	 Catch:{ Exception -> 0x0083 }
        goto L_0x0072;
    L_0x0083:
        r11 = move-exception;
        r2 = logger;	 Catch:{ all -> 0x0097 }
        r3 = "Query error";
        r2.error(r3, r11);	 Catch:{ all -> 0x0097 }
        r13 = org.oscim.tiling.QueryResult.FAILED;	 Catch:{ all -> 0x0097 }
        r0 = r19;
        r0.completed(r13);
        goto L_0x0077;
    L_0x0093:
        r10.close();	 Catch:{ Exception -> 0x0083 }
        goto L_0x0072;
    L_0x0097:
        r2 = move-exception;
        r0 = r19;
        r0.completed(r13);
        throw r2;
    L_0x009e:
        r2 = move-exception;
        throw r2;	 Catch:{ all -> 0x00a0 }
    L_0x00a0:
        r3 = move-exception;
        r16 = r3;
        r3 = r2;
        r2 = r16;
    L_0x00a6:
        if (r10 == 0) goto L_0x00ad;
    L_0x00a8:
        if (r3 == 0) goto L_0x00b3;
    L_0x00aa:
        r10.close();	 Catch:{ Throwable -> 0x00ae }
    L_0x00ad:
        throw r2;	 Catch:{ Exception -> 0x0083 }
    L_0x00ae:
        r4 = move-exception;
        r3.addSuppressed(r4);	 Catch:{ Exception -> 0x0083 }
        goto L_0x00ad;
    L_0x00b3:
        r10.close();	 Catch:{ Exception -> 0x0083 }
        goto L_0x00ad;
    L_0x00b7:
        r2 = move-exception;
        r3 = r15;
        goto L_0x00a6;
        */
        throw new UnsupportedOperationException("Method not decompiled: mobi.maptrek.maps.maptrek.MapTrekDataSource.query(org.oscim.layers.tile.MapTile, org.oscim.tiling.ITileDataSink):void");
    }

    public void dispose() {
    }

    public void cancel() {
    }

    String getName(int lang, long elementId) {
        return MapTrekDatabaseHelper.getFeatureName(lang, elementId, this.mDatabase);
    }

    void setContoursEnabled(boolean enabled) {
        this.mContoursEnabled = enabled;
    }
}
