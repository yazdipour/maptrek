package mobi.maptrek.layers.marker;

import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import java.util.Comparator;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.map.Viewport;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.utils.TimSort;
import org.oscim.utils.geom.GeometryUtils;

class MarkerRenderer extends BucketRenderer {
    private static final float FOCUS_CIRCLE_SIZE = 8.0f;
    private static TimSort<InternalItem> ZSORT = new TimSort();
    private static final String fShaderStr = "precision mediump float;varying vec2 v_tex;uniform float u_scale;uniform vec4 u_color;void main() {  float len = 1.0 - length(v_tex);  gl_FragColor = u_color * 0.5 * smoothstep(0.0, 1.0 / u_scale, len);}";
    private static final int mExtents = 100;
    private static final String vShaderStr = "precision mediump float;uniform mat4 u_mvp;uniform float u_scale;attribute vec2 a_pos;varying vec2 v_tex;void main() {  gl_Position = u_mvp * vec4(a_pos * u_scale, 0.0, 1.0);  v_tex = a_pos;}";
    private static final Comparator<InternalItem> zComparator = new Comparator<InternalItem>() {
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
    private int hColor;
    private int hMatrixPosition;
    private int hScale;
    private int hVertexPosition;
    private final float[] mBox = new float[8];
    final MarkerSymbol mDefaultMarker;
    private final Point mIndicatorPosition = new Point();
    private boolean mInitialized;
    private InternalItem[] mItems;
    private final Point mMapPoint = new Point();
    private final MarkerLayer<MarkerItem> mMarkerLayer;
    private final float mScale;
    private int mShaderProgram;
    private final SymbolBucket mSymbolLayer = new SymbolBucket();
    private boolean mUpdate;

    private static class InternalItem {
        boolean changes;
        float dy;
        MarkerItem item;
        double px;
        double py;
        boolean visible;
        float x;
        float y;

        private InternalItem() {
        }

        public String toString() {
            return "\n" + this.x + ":" + this.y + " / " + this.dy + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + this.visible;
        }
    }

    MarkerRenderer(MarkerLayer<MarkerItem> markerLayer, MarkerSymbol defaultSymbol, float scale) {
        this.mMarkerLayer = markerLayer;
        this.mDefaultMarker = defaultSymbol;
        this.mScale = scale;
    }

    public synchronized void update(GLViewport v) {
        if (!this.mInitialized) {
            init();
            this.mInitialized = true;
        }
        if (v.changed() || this.mUpdate) {
            this.mUpdate = false;
            double mx = v.pos.x;
            double my = v.pos.y;
            double scale = ((double) Tile.SIZE) * v.pos.scale;
            int numVisible = 0;
            this.mMarkerLayer.map().viewport().getMapExtents(this.mBox, 100.0f);
            long flip = ((long) (((double) Tile.SIZE) * v.pos.scale)) >> 1;
            if (this.mItems != null && this.mMarkerLayer.isEnabled()) {
                double angle = Math.toRadians((double) v.pos.bearing);
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                this.mIndicatorPosition.x = Double.MAX_VALUE;
                for (InternalItem it : this.mItems) {
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
                        if (it.item == this.mMarkerLayer.mFocusedItem) {
                            this.mIndicatorPosition.x = it.px;
                            this.mIndicatorPosition.y = it.py;
                        }
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
                    sort(this.mItems, 0, this.mItems.length);
                    for (InternalItem it2 : this.mItems) {
                        if (it2.visible) {
                            if (it2.changes) {
                                it2.visible = false;
                            } else {
                                MarkerSymbol marker = it2.item.getMarker();
                                if (marker == null) {
                                    marker = this.mDefaultMarker;
                                }
                                SymbolItem s = (SymbolItem) SymbolItem.pool.get();
                                s.set(it2.x, it2.y, marker.getBitmap(), true);
                                s.offset = marker.getHotspot();
                                s.billboard = marker.isBillboard();
                                this.mSymbolLayer.pushSymbol(s);
                            }
                        }
                    }
                    this.buckets.set(this.mSymbolLayer);
                    this.buckets.prepare();
                    compile();
                }
            } else if (this.buckets.get() != null) {
                this.buckets.clear();
                compile();
            }
        }
    }

    public void render(GLViewport v) {
        if (this.mIndicatorPosition.x != Double.MAX_VALUE) {
            GLState.useProgram(this.mShaderProgram);
            GLState.blend(true);
            GLState.test(false, false);
            GLState.enableVertexArrays(this.hVertexPosition, -1);
            MapRenderer.bindQuadVertexVBO(this.hVertexPosition);
            double tileScale = ((double) Tile.SIZE) * v.pos.scale;
            v.mvp.setTransScale((float) ((this.mIndicatorPosition.x - v.pos.x) * tileScale), (float) ((this.mIndicatorPosition.y - v.pos.y) * tileScale), Viewport.VIEW_NEAR);
            v.mvp.multiplyMM(v.viewproj, v.mvp);
            v.mvp.setAsUniform(this.hMatrixPosition);
            GLAdapter.gl.uniform1f(this.hScale, 8.0f * this.mScale);
            GLAdapter.gl.uniform4f(this.hColor, (((float) ((this.mMarkerLayer.mFocusColor >> 16) & 255)) * Viewport.VIEW_NEAR) / 255.0f, (((float) ((this.mMarkerLayer.mFocusColor >> 8) & 255)) * Viewport.VIEW_NEAR) / 255.0f, (((float) (this.mMarkerLayer.mFocusColor & 255)) * Viewport.VIEW_NEAR) / 255.0f, Viewport.VIEW_NEAR);
            GLAdapter.gl.drawArrays(5, 0, 4);
        }
        super.render(v);
    }

    void populate(int size) {
        InternalItem[] tmp = new InternalItem[size];
        for (int i = 0; i < size; i++) {
            InternalItem it = new InternalItem();
            tmp[i] = it;
            it.item = this.mMarkerLayer.createItem(i);
            MercatorProjection.project(it.item.getPoint(), this.mMapPoint);
            it.px = this.mMapPoint.x;
            it.py = this.mMapPoint.y;
        }
        synchronized (this) {
            this.mUpdate = true;
            this.mItems = tmp;
        }
    }

    public void update() {
        this.mUpdate = true;
    }

    private static void sort(InternalItem[] a, int lo, int hi) {
        if (hi - lo >= 2) {
            ZSORT.doSort(a, zComparator, lo, hi);
        }
    }

    private boolean init() {
        int shader = GLShader.createProgram(vShaderStr, fShaderStr);
        if (shader == 0) {
            return false;
        }
        this.mShaderProgram = shader;
        this.hVertexPosition = GLAdapter.gl.getAttribLocation(shader, "a_pos");
        this.hMatrixPosition = GLAdapter.gl.getUniformLocation(shader, "u_mvp");
        this.hScale = GLAdapter.gl.getUniformLocation(shader, "u_scale");
        this.hColor = GLAdapter.gl.getUniformLocation(shader, "u_color");
        return true;
    }
}
