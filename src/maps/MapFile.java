package mobi.maptrek.maps;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MercatorProjection;
import org.oscim.layers.tile.TileLayer;
import org.oscim.tiling.TileSource;

public class MapFile {
    public BoundingBox boundingBox;
    public String name;
    public double[] polygonPoints;
    public transient TileLayer tileLayer;
    public TileSource tileSource;

    MapFile() {
    }

    public MapFile(String name) {
        this.name = name;
    }

    public boolean contains(double x, double y) {
        if (this.polygonPoints == null) {
            return this.boundingBox.contains(new GeoPoint(MercatorProjection.toLatitude(y), MercatorProjection.toLongitude(x)));
        }
        int j = this.polygonPoints.length - 2;
        boolean inside = false;
        for (int i = 0; i < this.polygonPoints.length; i += 2) {
            double ix = this.polygonPoints[i];
            double iy = this.polygonPoints[i + 1];
            double jx = this.polygonPoints[j];
            double jy = this.polygonPoints[j + 1];
            if (((iy < y && jy >= y) || (jy < y && iy >= y)) && ((((y - iy) * 1.0d) / (jy - iy)) * (jx - ix)) + ix < x) {
                inside = !inside;
            }
            j = i;
        }
        return inside;
    }
}
