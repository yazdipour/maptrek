package mobi.maptrek.maps.maptrek;

import mobi.maptrek.util.OsmcSymbolFactory;
import mobi.maptrek.util.ShieldFactory;
import mobi.maptrek.util.StringFormatter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.core.PointF;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.vector.VectorTileLayer.TileLoaderThemeHook;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.layers.tile.vector.labeling.LabelTileData;
import org.oscim.layers.tile.vector.labeling.WayDecorator;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.SymbolStyle;
import org.oscim.theme.styles.SymbolStyle.SymbolBuilder;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.geom.PolyLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabelTileLoaderHook implements TileLoaderThemeHook {
    private static final String LABEL_DATA = LabelLayer.class.getName();
    private static final Logger logger = LoggerFactory.getLogger(LabelTileLoaderHook.class);
    private int mLang = 0;
    private final OsmcSymbolFactory mOsmcSymbolFactory;
    private final ShieldFactory mShieldFactory;
    private final SymbolBuilder<?> mSymbolBuilder = SymbolStyle.builder();

    public LabelTileLoaderHook(ShieldFactory shieldFactory, OsmcSymbolFactory osmcSymbolFactory) {
        this.mShieldFactory = shieldFactory;
        this.mOsmcSymbolFactory = osmcSymbolFactory;
    }

    private LabelTileData get(MapTile tile) {
        LabelTileData ld = (LabelTileData) tile.getData(LABEL_DATA);
        if (ld != null) {
            return ld;
        }
        ld = new LabelTileData();
        tile.addData(LABEL_DATA, ld);
        return ld;
    }

    public boolean process(MapTile tile, RenderBuckets buckets, MapElement element, RenderStyle style, int level) {
        LabelTileData ld;
        int offset;
        int i;
        int n;
        PointF p;
        if (style instanceof TextStyle) {
            ld = get(tile);
            TextStyle text = (TextStyle) style.current();
            String value;
            if (element.type == GeometryType.LINE) {
                value = getTextValue(element, text.textKey);
                if (value == null) {
                    return false;
                }
                offset = 0;
                for (int length : element.index) {
                    if (length < 4) {
                        break;
                    }
                    WayDecorator.renderText(null, element.points, value, text, offset, length, ld);
                    offset += length;
                }
            } else if (element.type == GeometryType.POLY) {
                PointF label = element.labelPosition;
                if ((element instanceof ExtendedMapElement) && ((ExtendedMapElement) element).hasLabelPosition && label == null) {
                    return false;
                }
                if (label != null && (label.x < 0.0f || label.x > ((float) Tile.SIZE) || label.y < 0.0f || label.y > ((float) Tile.SIZE))) {
                    return false;
                }
                if (text.areaSize > 0.0f && element.area() / ((float) (Tile.SIZE * Tile.SIZE)) < text.areaSize) {
                    return false;
                }
                value = getTextValue(element, text.textKey);
                if (value == null) {
                    return false;
                }
                if (label == null) {
                    label = PolyLabel.get(element);
                }
                ld.labels.push(((TextItem) TextItem.pool.get()).set(label.x, label.y, value, text));
            } else if (element.type == GeometryType.POINT) {
                value = getTextValue(element, text.textKey);
                if (value == null) {
                    return false;
                }
                n = element.getNumPoints();
                for (i = 0; i < n; i++) {
                    p = element.getPoint(i);
                    ld.labels.push(((TextItem) TextItem.pool.get()).set(p.x, p.y, value, text));
                }
            }
        } else if (style instanceof SymbolStyle) {
            SymbolStyle symbol = (SymbolStyle) style.current();
            if (symbol.src != null) {
                Bitmap bitmap;
                if (symbol.src.equals("/osmc-symbol")) {
                    bitmap = this.mOsmcSymbolFactory.getBitmap(element.tags.getValue("osmc:symbol"), symbol.symbolPercent);
                    if (bitmap != null) {
                        symbol = this.mSymbolBuilder.set(symbol).bitmap(bitmap).build();
                    }
                } else if (symbol.src.startsWith("/shield/")) {
                    bitmap = this.mShieldFactory.getBitmap(element.tags, symbol.src, symbol.symbolPercent);
                    if (bitmap != null) {
                        symbol = this.mSymbolBuilder.set(symbol).bitmap(bitmap).build();
                    }
                }
            }
            if (symbol.bitmap == null && symbol.texture == null) {
                return false;
            }
            ld = get(tile);
            SymbolItem it;
            if (element.type == GeometryType.POINT) {
                n = element.getNumPoints();
                for (i = 0; i < n; i++) {
                    p = element.getPoint(i);
                    it = (SymbolItem) SymbolItem.pool.get();
                    if (symbol.bitmap != null) {
                        it.set(p.x, p.y, symbol.bitmap, 0, 0.0f, true, symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap);
                    } else {
                        it.set(p.x, p.y, symbol.texture, 0, 0.0f, true, symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap);
                    }
                    ld.symbols.push(it);
                }
            } else if (element.type == GeometryType.LINE) {
                offset = 0;
                for (int length2 : element.index) {
                    if (length2 < 4) {
                        break;
                    }
                    WayDecorator.renderSymbol(null, element.points, symbol, offset, length2, ld);
                    offset += length2;
                }
            } else if (element.type == GeometryType.POLY) {
                PointF centroid = element.labelPosition;
                if (centroid == null) {
                    return false;
                }
                if (centroid.x < 0.0f || centroid.x > ((float) Tile.SIZE) || centroid.y < 0.0f || centroid.y > ((float) Tile.SIZE)) {
                    return false;
                }
                it = (SymbolItem) SymbolItem.pool.get();
                if (symbol.bitmap != null) {
                    it.set(centroid.x, centroid.y, symbol.bitmap, 0, 0.0f, true, symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap);
                } else {
                    it.set(centroid.x, centroid.y, symbol.texture, 0, 0.0f, true, symbol.mergeGap, symbol.mergeGroup, symbol.mergeGroupGap, symbol.textOverlap);
                }
                ld.symbols.push(it);
            }
        }
        return false;
    }

    public void complete(MapTile tile, boolean success) {
    }

    private String getTextValue(MapElement element, String key) {
        ExtendedMapElement extendedElement;
        if ("name".equals(key) && (element instanceof ExtendedMapElement)) {
            extendedElement = (ExtendedMapElement) element;
            if (extendedElement.id == 0) {
                return null;
            }
            String name = extendedElement.database.getName(this.mLang, extendedElement.id);
            if (name != null) {
                return name;
            }
        }
        if ("ele".equals(key) && (element instanceof ExtendedMapElement)) {
            extendedElement = (ExtendedMapElement) element;
            if (extendedElement.elevation != 0) {
                if (element.tags.containsKey("contour")) {
                    return StringFormatter.elevationC((float) extendedElement.elevation);
                }
                return StringFormatter.elevationH((float) extendedElement.elevation);
            }
        }
        String value = element.tags.getValue(key);
        if (value == null || value.length() <= 0) {
            return null;
        }
        return value;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.mLang = MapTrekDatabaseHelper.getLanguageId(preferredLanguage);
    }
}
