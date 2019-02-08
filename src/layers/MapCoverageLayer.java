package mobi.maptrek.layers;

import android.content.Context;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.support.v4.media.TransportMediator;
import android.text.format.Formatter;
import java.text.DateFormat;
import mobi.maptrek.Configuration;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.maps.maptrek.Index.ACTION;
import mobi.maptrek.maps.maptrek.Index.IndexStats;
import mobi.maptrek.maps.maptrek.Index.MapStateListener;
import mobi.maptrek.maps.maptrek.Index.MapStatus;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.FontStyle;
import org.oscim.core.Box;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.Gesture.DoubleTap;
import org.oscim.event.Gesture.LongPress;
import org.oscim.event.Gesture.Tap;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.vector.AbstractVectorLayer;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.MeshBucket;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.AreaStyle.AreaBuilder;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.LineStyle.LineBuilder;
import org.oscim.theme.styles.TextStyle;
import org.oscim.theme.styles.TextStyle.TextBuilder;
import org.oscim.utils.ColorUtil;
import org.oscim.utils.FastMath;
import org.oscim.utils.pool.Inlist;

public class MapCoverageLayer extends AbstractVectorLayer<MapFile> implements GestureListener, MapStateListener {
    private static final float AREA_ALPHA = 0.7f;
    private static final int COLOR_ACTIVE = ColorUtil.modHsv(Color.GREEN, 1.0d, 0.7d, 0.8d, false);
    private static final int COLOR_DELETED = ColorUtil.modHsv(-65536, 1.0d, 0.7d, 0.8d, false);
    private static final int COLOR_DOWNLOADING = ColorUtil.modHsv(COLOR_ACTIVE, 1.0d, 1.0d, 0.7d, false);
    private static final int COLOR_MISSING = ColorUtil.modHsv(Color.GRAY, 1.0d, 1.0d, 1.1d, false);
    private static final int COLOR_OUTDATED = ColorUtil.modHsv(-256, 1.0d, 0.6d, 0.8d, false);
    private static final int COLOR_SELECTED = ColorUtil.modHsv(Color.BLUE, 1.0d, 0.7d, 0.8d, false);
    private static final int COLOR_TEXT = Color.get(0, 96, 0);
    private static final int COLOR_TEXT_OUTLINE = Color.get(224, 224, 224);
    private static final int FADE_ZOOM = 3;
    private static final long MAP_EXPIRE_PERIOD = 6;
    private static final double MIN_SCALE = 10.0d;
    private static final double SYMBOL_MIN_SCALE = 30.0d;
    private static final double TEXT_MAX_SCALE = 260.0d;
    public static final double TEXT_MIN_SCALE = 40.0d;
    private static final float TILE_SCALE = 0.0078125f;
    private boolean mAccountHillshades;
    private Context mContext;
    private final DateFormat mDateFormat;
    private final AreaStyle mDeletedAreaStyle = ((AreaBuilder) AreaStyle.builder().fadeScale(3).blendColor(COLOR_DELETED).blendScale(10).color(Color.fade(COLOR_DELETED, 0.699999988079071d))).build();
    private final AreaStyle mDownloadingAreaStyle = ((AreaBuilder) AreaStyle.builder().fadeScale(3).blendColor(COLOR_DOWNLOADING).blendScale(10).color(Color.fade(COLOR_DOWNLOADING, 0.699999988079071d))).build();
    private final Bitmap mHillshadesBitmap;
    private final LineStyle mLineStyle;
    private final Index mMapIndex;
    private final AreaStyle mMissingAreaStyle = ((AreaBuilder) AreaStyle.builder().fadeScale(3).blendColor(COLOR_MISSING).blendScale(10).color(Color.fade(COLOR_MISSING, 0.699999988079071d))).build();
    private final AreaStyle mOutdatedAreaStyle = ((AreaBuilder) AreaStyle.builder().fadeScale(3).blendColor(COLOR_OUTDATED).blendScale(10).color(Color.fade(COLOR_OUTDATED, 0.699999988079071d))).build();
    private final AreaStyle mPresentAreaStyle = ((AreaBuilder) AreaStyle.builder().fadeScale(3).blendColor(COLOR_ACTIVE).blendScale(10).color(Color.fade(COLOR_ACTIVE, 0.699999988079071d))).build();
    private final Bitmap mPresentHillshadesBitmap;
    private final AreaStyle mSelectedAreaStyle = ((AreaBuilder) AreaStyle.builder().fadeScale(3).blendColor(COLOR_SELECTED).blendScale(10).color(Color.fade(COLOR_SELECTED, 0.699999988079071d))).build();
    private final TextStyle mSmallTextStyle;
    private final TextStyle mTextStyle;

    public MapCoverageLayer(Context context, Map map, Index mapIndex, float scale) {
        super(map);
        this.mContext = context;
        this.mMapIndex = mapIndex;
        this.mLineStyle = ((LineBuilder) ((LineBuilder) LineStyle.builder().fadeScale(4).color(Color.fade(Color.DKGRAY, 0.6000000238418579d))).strokeWidth(0.5f * scale)).fixed(true).build();
        this.mTextStyle = ((TextBuilder) ((TextBuilder) ((TextBuilder) TextStyle.builder().fontSize(11.0f * scale).fontStyle(FontStyle.BOLD).color(COLOR_TEXT)).strokeColor(COLOR_TEXT_OUTLINE)).strokeWidth(7.0f)).build();
        this.mSmallTextStyle = ((TextBuilder) ((TextBuilder) ((TextBuilder) TextStyle.builder().fontSize(Viewport.VIEW_FAR * scale).fontStyle(FontStyle.BOLD).color(COLOR_TEXT)).strokeColor(COLOR_TEXT_OUTLINE)).strokeWidth(5.0f)).build();
        this.mHillshadesBitmap = getHillshadesBitmap(Color.fade(Color.DKGRAY, 0.800000011920929d));
        this.mPresentHillshadesBitmap = getHillshadesBitmap(-1);
        this.mDateFormat = android.text.format.DateFormat.getDateFormat(context);
        this.mMapIndex.addMapStateListener(this);
        this.mAccountHillshades = Configuration.getHillshadesEnabled();
    }

    public void onDetach() {
        super.onDetach();
        this.mMapIndex.removeMapStateListener(this);
        this.mHillshadesBitmap.recycle();
        this.mPresentHillshadesBitmap.recycle();
    }

    public void onMapEvent(Event e, MapPosition pos) {
        super.onMapEvent(e, pos);
    }

    protected void processFeatures(Task t, Box b) {
        if (t.position.scale >= MIN_SCALE) {
            float scale = (float) ((t.position.scale * ((double) Tile.SIZE)) / 4.0d);
            float pxScale = (float) (t.position.scale / 4.0d);
            int tileXMin = ((int) (MercatorProjection.longitudeToX(b.xmin) / 0.0078125d)) - 2;
            int tileXMax = ((int) (MercatorProjection.longitudeToX(b.xmax) / 0.0078125d)) + 2;
            int tileYMin = FastMath.clamp(((int) (MercatorProjection.latitudeToY(b.ymax) / 0.0078125d)) - 2, 0, (int) TransportMediator.KEYCODE_MEDIA_PAUSE);
            int tileYMax = FastMath.clamp(((int) (MercatorProjection.latitudeToY(b.ymin) / 0.0078125d)) + 2, 0, (int) TransportMediator.KEYCODE_MEDIA_PAUSE);
            if (b.xmin < 0.0d) {
                tileXMin--;
            }
            boolean hasSizes = this.mMapIndex.hasDownloadSizes();
            boolean validSizes = hasSizes && !this.mMapIndex.expiredDownloadSizes();
            synchronized (this) {
                GeometryBuffer lines = new GeometryBuffer();
                GeometryBuffer missingAreas = new GeometryBuffer();
                GeometryBuffer selectedAreas = new GeometryBuffer();
                GeometryBuffer presentAreas = new GeometryBuffer();
                GeometryBuffer outdatedAreas = new GeometryBuffer();
                GeometryBuffer downloadingAreas = new GeometryBuffer();
                GeometryBuffer deletedAreas = new GeometryBuffer();
                Inlist text = null;
                if (t.position.scale >= TEXT_MIN_SCALE && t.position.scale <= TEXT_MAX_SCALE) {
                    text = t.buckets.getTextBucket(7);
                }
                Inlist symbols = new SymbolBucket();
                int tileX = tileXMin;
                while (tileX <= tileXMax) {
                    int tileY = tileYMin;
                    while (tileY <= tileYMax) {
                        int tileXX = tileX;
                        if (tileX < 0 || tileX >= 128) {
                            if (tileX < 0) {
                                tileXX = tileX + 128;
                            } else {
                                tileXX = tileX - 128;
                            }
                            if (tileXX < 0) {
                                continue;
                            } else if (tileXX > 128) {
                                continue;
                            }
                            tileY++;
                        }
                        MapStatus mapStatus = this.mMapIndex.getNativeMap(tileXX, tileY);
                        if (hasSizes && mapStatus.downloadSize == 0) {
                            tileY++;
                        } else {
                            GeometryBuffer areas = missingAreas;
                            if (mapStatus.downloading != 0) {
                                areas = downloadingAreas;
                            } else {
                                if (mapStatus.action == ACTION.REMOVE) {
                                    areas = deletedAreas;
                                } else {
                                    if (mapStatus.action == ACTION.DOWNLOAD) {
                                        areas = selectedAreas;
                                    } else if (mapStatus.created > (short) 0) {
                                        if (!hasSizes || ((long) mapStatus.created) + 6 >= ((long) mapStatus.downloadCreated)) {
                                            areas = presentAreas;
                                        } else {
                                            areas = outdatedAreas;
                                        }
                                    }
                                }
                            }
                            areas.startPolygon();
                            lines.startLine();
                            float x = (float) (((double) (((float) tileX) * TILE_SCALE)) - t.position.x);
                            float y = (float) (((double) (((float) tileY) * TILE_SCALE)) - t.position.y);
                            areas.addPoint(x * scale, y * scale);
                            lines.addPoint(x * scale, y * scale);
                            x += TILE_SCALE;
                            areas.addPoint(x * scale, y * scale);
                            lines.addPoint(x * scale, y * scale);
                            y += TILE_SCALE;
                            areas.addPoint(x * scale, y * scale);
                            lines.addPoint(x * scale, y * scale);
                            x -= TILE_SCALE;
                            areas.addPoint(x * scale, y * scale);
                            lines.addPoint(x * scale, y * scale);
                            y -= TILE_SCALE;
                            lines.addPoint(x * scale, y * scale);
                            if (t.position.scale >= SYMBOL_MIN_SCALE && t.position.scale <= TEXT_MAX_SCALE && this.mHillshadesBitmap != null && this.mPresentHillshadesBitmap != null) {
                                SymbolItem s = new SymbolItem();
                                if (mapStatus.hillshadeVersion > (byte) 0) {
                                    s.bitmap = this.mPresentHillshadesBitmap;
                                } else if (this.mAccountHillshades && mapStatus.hillshadeDownloadSize > 0) {
                                    s.bitmap = this.mHillshadesBitmap;
                                }
                                if (s.bitmap != null) {
                                    s.x = ((TILE_SCALE + x) * scale) - (((((float) s.bitmap.getWidth()) * pxScale) * TILE_SCALE) * 2.0f);
                                    s.y = (y * scale) + (((((float) s.bitmap.getHeight()) * pxScale) * TILE_SCALE) * 1.7f);
                                    s.billboard = false;
                                    symbols.addSymbol(s);
                                }
                            }
                            if (text != null) {
                                float tx = (0.00390625f + x) * scale;
                                float ty = (0.00390625f + y) * scale;
                                TextItem ti = (TextItem) TextItem.pool.get();
                                if (validSizes) {
                                    long size = mapStatus.downloadSize;
                                    if (this.mAccountHillshades) {
                                        size += mapStatus.hillshadeDownloadSize;
                                    }
                                    ti.set(tx, ty, Formatter.formatShortFileSize(this.mContext, size), this.mTextStyle);
                                    text.addText(ti);
                                    ty += this.mTextStyle.fontHeight / 5.0f;
                                }
                                if (validSizes || mapStatus.created > (short) 0) {
                                    ti = (TextItem) TextItem.pool.get();
                                    ti.set(tx, ty, this.mDateFormat.format(Long.valueOf(((long) ((mapStatus.created > (short) 0 ? mapStatus.created : mapStatus.downloadCreated) * 24)) * 3600000)), this.mSmallTextStyle);
                                    text.addText(ti);
                                }
                            }
                            tileY++;
                        }
                    }
                    tileX++;
                }
                LineBucket line = t.buckets.getLineBucket(0);
                if (line.line == null) {
                    line.line = this.mLineStyle;
                }
                line.addLine(lines);
                Inlist missing = t.buckets.getMeshBucket(1);
                if (missing.area == null) {
                    missing.area = this.mMissingAreaStyle;
                }
                missing.addMesh(missingAreas);
                line.next = missing;
                Inlist selected = t.buckets.getMeshBucket(2);
                if (selected.area == null) {
                    selected.area = this.mSelectedAreaStyle;
                }
                selected.addMesh(selectedAreas);
                missing.next = selected;
                Inlist present = t.buckets.getMeshBucket(3);
                if (present.area == null) {
                    present.area = this.mPresentAreaStyle;
                }
                present.addMesh(presentAreas);
                selected.next = present;
                Inlist outdated = t.buckets.getMeshBucket(4);
                if (outdated.area == null) {
                    outdated.area = this.mOutdatedAreaStyle;
                }
                outdated.addMesh(outdatedAreas);
                present.next = outdated;
                MeshBucket deleted = t.buckets.getMeshBucket(5);
                if (deleted.area == null) {
                    deleted.area = this.mDeletedAreaStyle;
                }
                deleted.addMesh(deletedAreas);
                outdated.next = deleted;
                MeshBucket downloading = t.buckets.getMeshBucket(6);
                if (downloading.area == null) {
                    downloading.area = this.mDownloadingAreaStyle;
                }
                downloading.addMesh(downloadingAreas);
                deleted.next = downloading;
                downloading.next = symbols;
                if (text != null) {
                    symbols.next = text;
                }
            }
        }
    }

    public boolean onGesture(Gesture gesture, MotionEvent event) {
        Point point = new Point();
        this.mMap.viewport().fromScreenPoint((double) event.getX(), (double) event.getY(), point);
        int tileX = (int) (point.getX() / 0.0078125d);
        int tileY = (int) (point.getY() / 0.0078125d);
        if (tileX < 0 || tileX > TransportMediator.KEYCODE_MEDIA_PAUSE || tileY < 0 || tileY > TransportMediator.KEYCODE_MEDIA_PAUSE) {
            return false;
        }
        MapStatus mapStatus = this.mMapIndex.getNativeMap(tileX, tileY);
        if (gesture instanceof LongPress) {
            if (mapStatus.downloading != 0) {
                this.mMapIndex.selectNativeMap(tileX, tileY, ACTION.CANCEL);
            } else if (mapStatus.created > (short) 0) {
                this.mMapIndex.selectNativeMap(tileX, tileY, ACTION.REMOVE);
            }
            return true;
        } else if (!(gesture instanceof Tap) && !(gesture instanceof DoubleTap)) {
            return false;
        } else {
            if (mapStatus.downloading != 0) {
                return true;
            }
            if (this.mMapIndex.hasDownloadSizes() && mapStatus.downloadSize == 0) {
                return true;
            }
            this.mMapIndex.selectNativeMap(tileX, tileY, ACTION.DOWNLOAD);
            return true;
        }
    }

    public void onHasDownloadSizes() {
        update();
    }

    public void onStatsChanged() {
        update();
    }

    public void onHillshadeAccountingChanged(boolean account) {
        this.mAccountHillshades = account;
        update();
    }

    public void onMapSelected(int x, int y, ACTION action, IndexStats stats) {
        update();
    }

    private static Bitmap getHillshadesBitmap(int color) {
        try {
            Bitmap bitmap = CanvasAdapter.getBitmapAsset("", "symbols/hillshades.svg", 0, 0, 70);
            android.graphics.Bitmap bitmapResult = android.graphics.Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapResult);
            Paint paint = new Paint();
            paint.setColorFilter(new PorterDuffColorFilter(color, Mode.SRC_IN));
            canvas.drawBitmap(AndroidGraphics.getBitmap(bitmap), 0.0f, 0.0f, paint);
            return new AndroidBitmap(bitmapResult);
        } catch (Throwable e) {
            log.error("Failed to read bitmap", e);
            return null;
        }
    }
}
