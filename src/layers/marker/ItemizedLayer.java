package mobi.maptrek.layers.marker;

import java.util.ArrayList;
import java.util.List;
import org.oscim.core.Box;
import org.oscim.core.Point;
import org.oscim.event.Gesture;
import org.oscim.event.Gesture.LongPress;
import org.oscim.event.Gesture.Tap;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.map.Map;
import org.oscim.map.Viewport;

public class ItemizedLayer<Item extends MarkerItem> extends MarkerLayer<Item> implements GestureListener {
    private final ActiveItem mActiveItemLongPress;
    private final ActiveItem mActiveItemSingleTap;
    private int mDrawnItemsLimit;
    private final List<Item> mItemList;
    private OnItemGestureListener<Item> mOnItemGestureListener;
    private final Point mTmpPoint;

    interface ActiveItem {
        boolean run(int i);
    }

    public interface OnItemGestureListener<T> {
        boolean onItemLongPress(int i, T t);

        boolean onItemSingleTapUp(int i, T t);
    }

    public ItemizedLayer(Map map, MarkerSymbol defaultMarker, float scale) {
        this(map, new ArrayList(), defaultMarker, scale, null);
    }

    public ItemizedLayer(Map map, List<Item> list, MarkerSymbol defaultMarker, float scale, OnItemGestureListener<Item> listener) {
        super(map, defaultMarker, scale);
        this.mTmpPoint = new Point();
        this.mDrawnItemsLimit = ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        this.mActiveItemSingleTap = new ActiveItem() {
            public boolean run(int index) {
                return ItemizedLayer.this.mOnItemGestureListener != null && ItemizedLayer.this.onSingleTapUpHelper(index, (MarkerItem) ItemizedLayer.this.mItemList.get(index));
            }
        };
        this.mActiveItemLongPress = new ActiveItem() {
            public boolean run(int index) {
                ItemizedLayer<Item> that = ItemizedLayer.this;
                return that.mOnItemGestureListener != null && ItemizedLayer.this.onLongPressHelper(index, (MarkerItem) that.mItemList.get(index));
            }
        };
        this.mItemList = list;
        this.mOnItemGestureListener = listener;
        populate();
    }

    public void setOnItemGestureListener(OnItemGestureListener<Item> listener) {
        this.mOnItemGestureListener = listener;
    }

    protected Item createItem(int index) {
        return (MarkerItem) this.mItemList.get(index);
    }

    public int size() {
        return Math.min(this.mItemList.size(), this.mDrawnItemsLimit);
    }

    public boolean addItem(Item item) {
        boolean result = this.mItemList.add(item);
        populate();
        return result;
    }

    public void addItem(int location, Item item) {
        this.mItemList.add(location, item);
    }

    public boolean addItems(List<Item> items) {
        boolean result = this.mItemList.addAll(items);
        populate();
        return result;
    }

    public void removeAllItems() {
        removeAllItems(true);
    }

    public void removeAllItems(boolean withPopulate) {
        this.mItemList.clear();
        if (withPopulate) {
            populate();
        }
    }

    public boolean removeItem(Item item) {
        boolean result = this.mItemList.remove(item);
        populate();
        return result;
    }

    public Item removeItem(int position) {
        MarkerItem result = (MarkerItem) this.mItemList.remove(position);
        populate();
        return result;
    }

    public void updateItems() {
        populate();
    }

    private boolean onSingleTapUpHelper(int index, Item item) {
        return this.mOnItemGestureListener.onItemSingleTapUp(index, item);
    }

    private boolean onLongPressHelper(int index, Item item) {
        return this.mOnItemGestureListener.onItemLongPress(index, item);
    }

    protected boolean activateSelectedItems(MotionEvent event, ActiveItem task) {
        int size = this.mItemList.size();
        if (size == 0) {
            return false;
        }
        int eventX = ((int) event.getX()) - (this.mMap.getWidth() / 2);
        int eventY = ((int) event.getY()) - (this.mMap.getHeight() / 2);
        Viewport mapPosition = this.mMap.viewport();
        Box box = mapPosition.getBBox(null, 128);
        box.map2mercator();
        box.scale(1000000.0d);
        int nearest = -1;
        int inside = -1;
        double insideY = -1.7976931348623157E308d;
        double dist = 2500.0d;
        for (int i = 0; i < size; i++) {
            MarkerItem item = (MarkerItem) this.mItemList.get(i);
            if (box.contains((double) item.getPoint().longitudeE6, (double) item.getPoint().latitudeE6)) {
                mapPosition.toScreenPoint(item.getPoint(), this.mTmpPoint);
                float dx = (float) (this.mTmpPoint.x - ((double) eventX));
                float dy = (float) (this.mTmpPoint.y - ((double) eventY));
                MarkerSymbol it = item.getMarker();
                if (it == null) {
                    it = this.mMarkerRenderer.mDefaultMarker;
                }
                if (it.isInside(dx, dy) && this.mTmpPoint.y > insideY) {
                    insideY = this.mTmpPoint.y;
                    inside = i;
                }
                if (inside < 0) {
                    double d = (double) ((dx * dx) + (dy * dy));
                    if (d <= dist) {
                        dist = d;
                        nearest = i;
                    }
                }
            }
        }
        if (inside >= 0) {
            nearest = inside;
        }
        if (nearest < 0 || !task.run(nearest)) {
            return false;
        }
        this.mMarkerRenderer.update();
        this.mMap.render();
        return true;
    }

    public boolean onGesture(Gesture g, MotionEvent e) {
        if (g instanceof Tap) {
            return activateSelectedItems(e, this.mActiveItemSingleTap);
        }
        if (g instanceof LongPress) {
            return activateSelectedItems(e, this.mActiveItemLongPress);
        }
        return false;
    }

    public Item getByUid(Object uid) {
        for (MarkerItem it : this.mItemList) {
            if (uid.equals(it.getUid())) {
                return it;
            }
        }
        return null;
    }
}
