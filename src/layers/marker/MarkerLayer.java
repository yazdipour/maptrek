package mobi.maptrek.layers.marker;

import org.oscim.layers.Layer;
import org.oscim.map.Map;

public abstract class MarkerLayer<Item extends MarkerItem> extends Layer {
    protected int mFocusColor;
    protected Item mFocusedItem;
    protected final MarkerRenderer mMarkerRenderer;

    protected abstract Item createItem(int i);

    public abstract int size();

    public MarkerLayer(Map map, MarkerSymbol defaultSymbol, float scale) {
        super(map);
        this.mMarkerRenderer = new MarkerRenderer(this, defaultSymbol, scale);
        this.mRenderer = this.mMarkerRenderer;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mMarkerRenderer.update();
    }

    protected final void populate() {
        this.mMarkerRenderer.populate(size());
    }

    public void setFocus(Item item) {
        this.mFocusedItem = item;
        this.mMarkerRenderer.update();
        this.mMap.updateMap(true);
    }

    public void setFocus(Item item, int color) {
        this.mFocusedItem = item;
        this.mFocusColor = color;
        this.mMarkerRenderer.update();
        this.mMap.updateMap(true);
    }

    public Item getFocus() {
        return this.mFocusedItem;
    }
}
