package mobi.maptrek.layers.building;

import mobi.maptrek.maps.maptrek.ExtendedMapElement;
import org.oscim.backend.canvas.Color;
import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.ExtrusionBucket;
import org.oscim.renderer.bucket.ExtrusionBuckets;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.theme.styles.ExtrusionStyle;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.utils.pool.Inlist;

public class BuildingLayer extends org.oscim.layers.tile.buildings.BuildingLayer {
    public BuildingLayer(Map map, VectorTileLayer tileLayer) {
        super(map, tileLayer);
    }

    public boolean process(MapTile tile, RenderBuckets buckets, MapElement el, RenderStyle style, int level) {
        if (!(style instanceof ExtrusionStyle)) {
            return false;
        }
        ExtrusionStyle extrusion = (ExtrusionStyle) style;
        ExtendedMapElement element = (ExtendedMapElement) el;
        int height = element.buildingHeight > 0 ? element.buildingHeight : 1200;
        int minHeight = element.buildingMinHeight;
        float[] colors = extrusion.colors;
        if (!(element.buildingColor == 0 && element.roofColor == 0)) {
            colors = new float[16];
            System.arraycopy(extrusion.colors, 0, colors, 0, colors.length);
            if (element.roofColor != 0) {
                colors[0] = Color.rToFloat(element.roofColor) * 0.9f;
                colors[1] = Color.gToFloat(element.roofColor) * 0.9f;
                colors[2] = Color.bToFloat(element.roofColor) * 0.9f;
                colors[3] = 0.9f;
            }
            if (element.buildingColor != 0) {
                colors[4] = Color.rToFloat(element.buildingColor) * 0.9f;
                colors[5] = Color.gToFloat(element.buildingColor) * 0.9f;
                colors[6] = Color.bToFloat(element.buildingColor) * 0.9f;
                colors[7] = 0.9f;
                colors[8] = Color.rToFloat(element.buildingColor) * 0.9f;
                colors[9] = Color.gToFloat(element.buildingColor) * 0.9f;
                colors[10] = Color.bToFloat(element.buildingColor) * 0.9f;
                colors[11] = 0.9f;
            }
        }
        ExtrusionBuckets ebs = org.oscim.layers.tile.buildings.BuildingLayer.get(tile);
        for (ExtrusionBucket b = ebs.buckets; b != null; b = b.next()) {
            if (b.colors == colors) {
                b.add(element, (float) height, (float) minHeight);
                return true;
            }
        }
        ebs.buckets = (ExtrusionBucket) Inlist.push(ebs.buckets, new ExtrusionBucket(0, (float) MercatorProjection.groundResolutionWithScale(MercatorProjection.toLatitude(tile.y), (double) (1 << tile.zoomLevel)), colors));
        ebs.buckets.add(element, (float) height, (float) minHeight);
        return true;
    }
}
