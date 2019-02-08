package mobi.maptrek.maps.maptrek;

import java.io.IOException;
import java.io.InputStream;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.core.Tile;
import org.oscim.map.Viewport;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.source.PbfDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MapTrekTileDecoder extends PbfDecoder {
    private static final float REF_TILE_SIZE = 4096.0f;
    private static final int TAG_ELEM_BUILDING_COLOR = 36;
    private static final int TAG_ELEM_COORDINATES = 13;
    private static final int TAG_ELEM_ELEVATION = 33;
    private static final int TAG_ELEM_HEIGHT = 34;
    private static final int TAG_ELEM_HOUSE_NUMBER = 38;
    private static final int TAG_ELEM_ID = 4;
    private static final int TAG_ELEM_INDEX = 12;
    private static final int TAG_ELEM_KIND = 32;
    private static final int TAG_ELEM_LABEL = 31;
    private static final int TAG_ELEM_LAYER = 21;
    private static final int TAG_ELEM_MIN_HEIGHT = 35;
    private static final int TAG_ELEM_NUM_COORDINATES = 3;
    private static final int TAG_ELEM_NUM_INDICES = 1;
    private static final int TAG_ELEM_NUM_TAGS = 2;
    private static final int TAG_ELEM_ROOF_COLOR = 37;
    private static final int TAG_ELEM_TAGS = 11;
    private static final int TAG_TILE_LINE = 21;
    private static final int TAG_TILE_MESH = 24;
    private static final int TAG_TILE_NUM_KEYS = 12;
    private static final int TAG_TILE_NUM_TAGS = 11;
    private static final int TAG_TILE_NUM_VALUES = 13;
    private static final int TAG_TILE_POINT = 23;
    private static final int TAG_TILE_POLY = 22;
    private static final int TAG_TILE_TAGS = 16;
    private static final int TAG_TILE_TAG_KEYS = 14;
    private static final int TAG_TILE_TAG_VALUES = 15;
    private static final int TAG_TILE_VERSION = 1;
    private static final Logger log = LoggerFactory.getLogger(MapTrekTileDecoder.class);
    private final ExtendedMapElement mElem = new ExtendedMapElement();
    private final GeometryBuffer mLabel = new GeometryBuffer(2, 1);
    private ITileDataSink mMapDataSink;
    private int[] mSArray = new int[100];
    private final float mScaleFactor = (REF_TILE_SIZE / ((float) Tile.SIZE));
    private Tile mTile;
    private final TagSet mTileTags = new TagSet(100);

    MapTrekTileDecoder() {
    }

    public boolean decode(Tile tile, ITileDataSink sink, InputStream is) throws IOException {
        setInputStream(is);
        this.mTile = tile;
        this.mMapDataSink = sink;
        this.mTileTags.clearAndNullTags();
        int numTags = 0;
        int numKeys = -1;
        int numValues = -1;
        int curKey = 0;
        int curValue = 0;
        String[] keys = null;
        String[] values = null;
        while (hasData()) {
            int val = decodeVarint32();
            if (val <= 0) {
                return true;
            }
            int tag = val >> 3;
            switch (tag) {
                case 1:
                    int version = decodeVarint32();
                    break;
                case 11:
                    numTags = decodeVarint32();
                    log.debug("num tags " + numTags);
                    break;
                case 12:
                    numKeys = decodeVarint32();
                    log.debug("num keys " + numKeys);
                    keys = new String[numKeys];
                    break;
                case 13:
                    numValues = decodeVarint32();
                    log.debug("num values " + numValues);
                    values = new String[numValues];
                    break;
                case 14:
                    if (keys != null && curKey < numKeys) {
                        int curKey2 = curKey + 1;
                        keys[curKey] = decodeString().intern();
                        curKey = curKey2;
                        break;
                    }
                    log.error("{} wrong number of keys {}", (Object) this.mTile, Integer.valueOf(numKeys));
                    return false;
                case 15:
                    if (values != null && curValue < numValues) {
                        int curValue2 = curValue + 1;
                        values[curValue] = decodeString();
                        curValue = curValue2;
                        break;
                    }
                    log.error("{} wrong number of values {}", (Object) this.mTile, Integer.valueOf(numValues));
                    return false;
                case 16:
                    int len = numTags * 2;
                    if (this.mSArray.length < len) {
                        this.mSArray = new int[len];
                    }
                    decodeVarintArray(len, this.mSArray);
                    if (decodeTileTags(numTags, this.mSArray, keys, values)) {
                        break;
                    }
                    log.error("{} invalid tags", (Object) this.mTile);
                    return false;
                case 21:
                case 22:
                case 23:
                case 24:
                    decodeTileElement(tile, tag);
                    break;
                default:
                    log.error("{} invalid type for tile: {}", (Object) this.mTile, Integer.valueOf(tag));
                    return false;
            }
        }
        return true;
    }

    private boolean decodeTileTags(int numTags, int[] tagIdx, String[] keys, String[] vals) {
        int n = numTags << 1;
        for (int i = 0; i < n; i += 2) {
            String key;
            String val;
            Tag tag;
            int k = tagIdx[i];
            int v = tagIdx[i + 1];
            if (k >= 1024) {
                k -= 1024;
                if (k >= keys.length) {
                    return false;
                }
                key = keys[k];
            } else if (k > Tags.MAX_KEY) {
                log.warn("unknown tag key: {}", Integer.valueOf(k));
                key = String.valueOf(k);
            } else {
                key = Tags.keys[k];
            }
            if (v >= 1024) {
                v -= 1024;
                if (v >= vals.length) {
                    return false;
                }
                val = vals[v];
            } else if (v > Tags.MAX_VALUE) {
                log.warn("unknown tag value: {}", Integer.valueOf(v));
                val = "";
            } else {
                val = Tags.values[v];
            }
            if ("name".equals(key) || Tag.KEY_HOUSE_NUMBER.equals(key) || Tag.KEY_REF.equals(key) || "ele".equals(key)) {
                tag = new Tag(key, val, false);
            } else {
                tag = new Tag(key, val, false, true);
            }
            this.mTileTags.add(tag);
        }
        return true;
    }

    private int decodeWayIndices(int indexCnt, boolean shift) throws IOException {
        this.mElem.ensureIndexSize(indexCnt, false);
        decodeVarintArray(indexCnt, this.mElem.index);
        int[] index = this.mElem.index;
        int coordCnt = 0;
        if (shift) {
            for (int i = 0; i < indexCnt; i++) {
                coordCnt += index[i];
                index[i] = index[i] * 2;
            }
        }
        if (indexCnt < index.length) {
            index[indexCnt] = -1;
        }
        return coordCnt;
    }

    private boolean decodeTileElement(Tile tile, int type) throws IOException {
        this.mElem.clearData();
        int end = position() + decodeVarint32();
        int numIndices = 1;
        int numTags = 1;
        boolean fail = false;
        int coordCnt = 0;
        if (type == 23) {
            coordCnt = 1;
            this.mElem.index[0] = 2;
        }
        int kind = -1;
        String houseNumber = null;
        while (position() < end) {
            int val = decodeVarint32();
            if (val != 0) {
                int tag = val >> 3;
                int cnt;
                switch (tag) {
                    case 1:
                        numIndices = decodeVarint32();
                        break;
                    case 2:
                        numTags = decodeVarint32();
                        break;
                    case 3:
                        coordCnt = decodeVarint32();
                        break;
                    case 4:
                        this.mElem.id = decodeVarint64();
                        break;
                    case 11:
                        if (decodeElementTags(numTags)) {
                            break;
                        }
                        return false;
                    case 12:
                        if (type != 24) {
                            coordCnt = decodeWayIndices(numIndices, true);
                            break;
                        }
                        decodeWayIndices(numIndices, false);
                        break;
                    case 13:
                        if (coordCnt == 0) {
                            log.debug("{} no coordinates", (Object) this.mTile);
                        }
                        if (type != 24) {
                            this.mElem.ensurePointSize(coordCnt, false);
                            if (decodeInterleavedPoints(this.mElem, this.mScaleFactor) == coordCnt) {
                                break;
                            }
                            log.error("{} wrong number of coordintes {}/{}", this.mTile, Integer.valueOf(coordCnt), Integer.valueOf(decodeInterleavedPoints(this.mElem, this.mScaleFactor)));
                            fail = true;
                            break;
                        }
                        this.mElem.ensurePointSize((coordCnt * 3) / 2, false);
                        cnt = decodeInterleavedPoints3D(this.mElem.points, Viewport.VIEW_NEAR);
                        if (cnt != coordCnt * 3) {
                            log.error("{} wrong number of coordintes {}/{}", this.mTile, Integer.valueOf(coordCnt), Integer.valueOf(cnt));
                            fail = true;
                        }
                        this.mElem.pointPos = cnt;
                        break;
                    case 21:
                        this.mElem.layer = decodeVarint32();
                        break;
                    case 31:
                        cnt = decodeInterleavedPoints(this.mLabel, this.mScaleFactor);
                        if (cnt == 1) {
                            this.mElem.setLabelPosition(this.mLabel.getPointX(0), this.mLabel.getPointY(0));
                            break;
                        }
                        log.warn("{} wrong number of coordinates for label: {}", (Object) this.mTile, Integer.valueOf(cnt));
                        break;
                    case 32:
                        kind = decodeVarint32();
                        break;
                    case 33:
                        this.mElem.elevation = PbfDecoder.deZigZag(decodeVarint32());
                        break;
                    case 34:
                        this.mElem.buildingHeight = PbfDecoder.deZigZag(decodeVarint32());
                        break;
                    case 35:
                        this.mElem.buildingMinHeight = PbfDecoder.deZigZag(decodeVarint32());
                        break;
                    case 36:
                        this.mElem.buildingColor = decodeVarint32();
                        break;
                    case 37:
                        this.mElem.roofColor = decodeVarint32();
                        break;
                    case 38:
                        houseNumber = decodeString();
                        break;
                    default:
                        log.debug("{} invalid type for way: {}", (Object) this.mTile, Integer.valueOf(tag));
                        break;
                }
            } else if (!fail || numTags == 0 || numIndices == 0) {
                log.debug("{} failed: bytes:{} tags:{} ({},{})", this.mTile, Integer.valueOf(bytes), this.mElem.tags, Integer.valueOf(numIndices), Integer.valueOf(coordCnt));
                return false;
            } else {
                switch (type) {
                    case 21:
                        this.mElem.type = GeometryType.LINE;
                        break;
                    case 22:
                        this.mElem.type = GeometryType.POLY;
                        break;
                    case 23:
                        this.mElem.type = GeometryType.POINT;
                        break;
                    case 24:
                        this.mElem.type = GeometryType.TRIS;
                        break;
                }
                if (kind >= 0) {
                    this.mElem.kind = kind;
                    boolean place_road_building = (kind & 7) > 0;
                    kind >>= 3;
                    boolean someKind = kind > 0;
                    boolean hasKind = false;
                    int i = 0;
                    while (i < 16) {
                        if ((kind & 1) > 0 && Tags.kindZooms[i] <= tile.zoomLevel) {
                            this.mElem.tags.add(new Tag(Tags.kinds[i], Tag.VALUE_YES));
                            hasKind = true;
                        }
                        kind >>= 1;
                        i++;
                    }
                    if (!hasKind && !place_road_building && type == 23 && this.mElem.tags.size() <= 1) {
                        return true;
                    }
                    if (someKind) {
                        this.mElem.tags.add(new Tag("kind", Tag.VALUE_YES));
                    }
                }
                if (houseNumber != null) {
                    this.mElem.tags.add(new Tag(Tag.KEY_HOUSE_NUMBER, houseNumber, false));
                }
                this.mMapDataSink.process(this.mElem);
                return true;
            }
        }
        if (fail) {
        }
        log.debug("{} failed: bytes:{} tags:{} ({},{})", this.mTile, Integer.valueOf(bytes), this.mElem.tags, Integer.valueOf(numIndices), Integer.valueOf(coordCnt));
        return false;
    }

    private boolean decodeElementTags(int numTags) throws IOException {
        if (this.mSArray.length < numTags) {
            this.mSArray = new int[numTags];
        }
        int[] tagIds = this.mSArray;
        decodeVarintArray(numTags, tagIds);
        this.mElem.tags.clear();
        int max = this.mTileTags.size() - 1;
        for (int i = 0; i < numTags; i++) {
            int idx = tagIds[i];
            if (idx < 0 || idx > max) {
                log.error("{} invalid tag: {}", this.mTile, Integer.valueOf(idx), Integer.valueOf(i));
                return false;
            }
            Tag tag = this.mTileTags.get(idx);
            if ("contour".equals(tag.key)) {
                this.mElem.isContour = true;
            }
            if (Tag.KEY_BUILDING.equals(tag.key)) {
                this.mElem.isBuilding = true;
            }
            if (Tag.KEY_BUILDING_PART.equals(tag.key)) {
                this.mElem.isBuildingPart = true;
            }
            this.mElem.tags.add(tag);
        }
        return true;
    }
}
