package mobi.maptrek.layers;

import android.graphics.Bitmap;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import mobi.maptrek.MapTrek;
import mobi.maptrek.data.MapObject;
import mobi.maptrek.data.MapObject.AddedEvent;
import mobi.maptrek.data.MapObject.RemovedEvent;
import mobi.maptrek.data.MapObject.UpdatedEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.PointF;
import org.oscim.core.Tile;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.renderer.bucket.TextBucket;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.TextStyle;
import org.oscim.theme.styles.TextStyle.TextBuilder;
import org.oscim.utils.geom.GeometryUtils;

public class MapObjectLayer extends Layer {

    private class MapObjectRenderer extends BucketRenderer {
        private final float[] mBox;
        private int mExtents;
        private ArrayList<InternalItem> mItems;
        private ArrayList<Bitmap> mOldBitmaps;
        private final float mScale;
        private final SymbolBucket mSymbolBucket;
        private final TextBucket mTextBucket;
        private boolean mUpdate;
        private ArrayList<Bitmap> mUsedBitmaps;
        final Comparator<InternalItem> zComparator;

        class InternalItem {
            boolean changes;
            float dy;
            MapObject item;
            private final Point mMapPoint = new Point();
            double px;
            double py;
            boolean visible;
            float x;
            float y;

            InternalItem(MapObject item) {
                this.item = item;
                MercatorProjection.project(item.coordinates, this.mMapPoint);
                this.px = this.mMapPoint.x;
                this.py = this.mMapPoint.y;
            }

            public String toString() {
                return this.px + ":" + this.py + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + this.x + ":" + this.y + " / " + this.dy + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + this.visible;
            }
        }

        MapObjectRenderer(float scale) {
            this.mBox = new float[8];
            this.mExtents = 100;
            this.mItems = new ArrayList();
            this.mUsedBitmaps = new ArrayList();
            this.mOldBitmaps = new ArrayList();
            this.zComparator = new Comparator<InternalItem>() {
                public int compare(InternalItem a, InternalItem b) {
                    if (a.visible && b.visible) {
                        if (a.dy > b.dy) {
                            return -1;
                        }
                        if (a.dy < b.dy) {
                            return 1;
                        }
                    } else if (a.visible) {
                        return -1;
                    } else {
                        if (b.visible) {
                            return 1;
                        }
                    }
                    return 0;
                }
            };
            this.mSymbolBucket = new SymbolBucket();
            this.mTextBucket = new TextBucket();
            this.mSymbolBucket.next = this.mTextBucket;
            this.mScale = scale;
        }

        public void update() {
            this.mUpdate = true;
        }

        public synchronized void update(GLViewport v) {
            if (v.changed() || this.mUpdate) {
                this.mUpdate = false;
                double mx = v.pos.x;
                double my = v.pos.y;
                double scale = ((double) Tile.SIZE) * v.pos.scale;
                int numVisible = 0;
                MapObjectLayer.this.map().viewport().getMapExtents(this.mBox, (float) this.mExtents);
                long flip = ((long) (((double) Tile.SIZE) * v.pos.scale)) >> 1;
                Iterator<MapObject> mapObjects = MapTrek.getMapObjects();
                if (mapObjects.hasNext()) {
                    InternalItem it;
                    this.mItems.clear();
                    while (mapObjects.hasNext()) {
                        this.mItems.add(new InternalItem((MapObject) mapObjects.next()));
                    }
                    double angle = Math.toRadians((double) v.pos.bearing);
                    float cos = (float) Math.cos(angle);
                    float sin = (float) Math.sin(angle);
                    Iterator it2 = this.mItems.iterator();
                    while (it2.hasNext()) {
                        it = (InternalItem) it2.next();
                        it.changes = false;
                        it.x = (float) ((it.px - mx) * scale);
                        it.y = (float) ((it.py - my) * scale);
                        if (it.x > ((float) flip)) {
                            it.x -= (float) (flip << 1);
                        } else if (it.x < ((float) (-flip))) {
                            it.x += (float) (flip << 1);
                        }
                        if (GeometryUtils.pointInPoly(it.x, it.y, this.mBox, 8, 0)) {
                            it.dy = (it.x * sin) + (it.y * cos);
                            if (!it.visible) {
                                it.visible = true;
                            }
                            numVisible++;
                        } else if (it.visible) {
                            it.changes = true;
                        }
                    }
                    this.buckets.clear();
                    if (numVisible == 0) {
                        compile();
                    } else {
                        this.mMapPosition.copy(v.pos);
                        this.mMapPosition.bearing = -this.mMapPosition.bearing;
                        this.mOldBitmaps.addAll(this.mUsedBitmaps);
                        this.mUsedBitmaps.clear();
                        Collections.sort(this.mItems, this.zComparator);
                        int color = 0;
                        TextStyle textStyle = null;
                        Iterator it3 = this.mItems.iterator();
                        while (it3.hasNext()) {
                            it = (InternalItem) it3.next();
                            if (it.visible) {
                                if (it.changes) {
                                    it.visible = false;
                                } else {
                                    Bitmap bitmap = it.item.getBitmapCopy();
                                    if (bitmap != null) {
                                        SymbolItem s = (SymbolItem) SymbolItem.pool.get();
                                        s.set(it.x, it.y, (org.oscim.backend.canvas.Bitmap) new AndroidBitmap(bitmap), false);
                                        s.offset = new PointF(0.5f, 0.5f);
                                        this.mSymbolBucket.pushSymbol(s);
                                        this.mUsedBitmaps.add(bitmap);
                                        if (textStyle == null || color != it.item.textColor) {
                                            color = it.item.textColor;
                                            textStyle = ((TextBuilder) ((TextBuilder) TextStyle.builder().fontSize(10.0f * this.mScale).color(color)).outline(-1, 2.0f)).isCaption(true).build();
                                        }
                                        TextItem t = (TextItem) TextItem.pool.get();
                                        t.set(it.x, it.y - ((float) (bitmap.getHeight() / 2)), it.item.name, textStyle);
                                        this.mTextBucket.addText(t);
                                    }
                                }
                            }
                        }
                        this.buckets.set(this.mSymbolBucket);
                        this.buckets.prepare();
                        compile();
                        it2 = this.mOldBitmaps.iterator();
                        while (it2.hasNext()) {
                            ((Bitmap) it2.next()).recycle();
                        }
                        this.mOldBitmaps.clear();
                    }
                } else if (this.buckets.get() != null) {
                    this.buckets.clear();
                    compile();
                }
            }
        }

        @Subscribe
        public void onMapObjectAdded(AddedEvent event) {
            this.mUpdate = true;
        }

        @Subscribe
        public void onMapObjectRemoved(RemovedEvent event) {
            this.mUpdate = true;
        }

        @Subscribe
        public void onMapObjectUpdated(UpdatedEvent event) {
            this.mUpdate = true;
        }
    }

    public MapObjectLayer(Map map, float scale) {
        super(map);
        this.mRenderer = new MapObjectRenderer(scale);
        EventBus.getDefault().register(this.mRenderer);
    }

    public void onDetach() {
        EventBus.getDefault().unregister(this.mRenderer);
    }
}
