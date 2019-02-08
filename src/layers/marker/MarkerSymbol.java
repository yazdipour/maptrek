package mobi.maptrek.layers.marker;

import mobi.maptrek.layers.marker.MarkerItem.HotspotPlace;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.PointF;
import org.oscim.map.Viewport;

public class MarkerSymbol {
    final boolean mBillboard;
    final Bitmap[] mBitmap;
    final PointF mOffset;

    public MarkerSymbol(Bitmap bitmap, float relX, float relY) {
        this(bitmap, relX, relY, true);
    }

    public MarkerSymbol(Bitmap bitmap, float relX, float relY, boolean billboard) {
        this.mBitmap = new Bitmap[1];
        this.mBitmap[0] = bitmap;
        this.mOffset = new PointF(relX, relY);
        this.mBillboard = billboard;
    }

    public MarkerSymbol(Bitmap bitmap, HotspotPlace hotspot) {
        this(bitmap, hotspot, true);
    }

    public MarkerSymbol(Bitmap bitmap, HotspotPlace hotspot, boolean billboard) {
        switch (hotspot) {
            case BOTTOM_CENTER:
                this.mOffset = new PointF(0.5f, Viewport.VIEW_NEAR);
                break;
            case TOP_CENTER:
                this.mOffset = new PointF(0.5f, 0.0f);
                break;
            case RIGHT_CENTER:
                this.mOffset = new PointF(Viewport.VIEW_NEAR, 0.5f);
                break;
            case LEFT_CENTER:
                this.mOffset = new PointF(0.0f, 0.5f);
                break;
            case UPPER_RIGHT_CORNER:
                this.mOffset = new PointF(Viewport.VIEW_NEAR, 0.0f);
                break;
            case LOWER_RIGHT_CORNER:
                this.mOffset = new PointF(Viewport.VIEW_NEAR, Viewport.VIEW_NEAR);
                break;
            case UPPER_LEFT_CORNER:
                this.mOffset = new PointF(0.0f, 0.0f);
                break;
            case LOWER_LEFT_CORNER:
                this.mOffset = new PointF(0.0f, Viewport.VIEW_NEAR);
                break;
            default:
                this.mOffset = new PointF(0.5f, 0.5f);
                break;
        }
        this.mBitmap = new Bitmap[1];
        this.mBitmap[0] = bitmap;
        this.mBillboard = billboard;
    }

    public boolean isBillboard() {
        return this.mBillboard;
    }

    public PointF getHotspot() {
        return this.mOffset;
    }

    public Bitmap getBitmap() {
        return this.mBitmap[0];
    }

    public boolean isInside(float dx, float dy) {
        int w = this.mBitmap[0].getWidth();
        int h = this.mBitmap[0].getHeight();
        float ox = ((float) (-w)) * this.mOffset.x;
        float oy = ((float) (-h)) * (Viewport.VIEW_NEAR - this.mOffset.y);
        if (dx < ox || dy < oy || dx > ((float) w) + ox || dy > ((float) h) + oy) {
            return false;
        }
        return true;
    }
}
