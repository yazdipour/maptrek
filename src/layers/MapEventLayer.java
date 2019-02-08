package mobi.maptrek.layers;

import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.map.Map;

public class MapEventLayer extends Layer implements GestureListener {
    private final GestureListener mListener;

    public MapEventLayer(Map map, GestureListener listener) {
        super(map);
        this.mListener = listener;
    }

    public boolean onGesture(Gesture g, MotionEvent e) {
        return this.mListener.onGesture(g, e);
    }
}
