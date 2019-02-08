package mobi.maptrek.layers;

import org.oscim.core.GeoPoint;
import org.oscim.layers.PathLayer;
import org.oscim.map.Map;

public class NavigationLayer extends PathLayer {
    private GeoPoint mDestination;
    private GeoPoint mPosition;

    public NavigationLayer(Map map, int lineColor, float lineWidth) {
        super(map, lineColor, lineWidth);
    }

    public void setDestination(GeoPoint destination) {
        synchronized (this.mPoints) {
            this.mDestination = destination;
            clearPath();
            if (this.mPosition != null) {
                addPoint(this.mPosition);
                addGreatCircle(this.mPosition, this.mDestination);
                addPoint(this.mDestination);
            }
        }
    }

    public GeoPoint getDestination() {
        return this.mDestination;
    }

    public void setPosition(double lat, double lon) {
        synchronized (this.mPoints) {
            this.mPosition = new GeoPoint(lat, lon);
            clearPath();
            addPoint(this.mPosition);
            addGreatCircle(this.mPosition, this.mDestination);
            addPoint(this.mDestination);
        }
    }
}
