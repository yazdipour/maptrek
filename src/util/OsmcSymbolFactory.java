package mobi.maptrek.util;

import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.InputDeviceCompat;
import java.util.Arrays;
import java.util.HashSet;
import mobi.maptrek.MapTrek;
import mobi.maptrek.maps.MapService;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.Tag;
import org.oscim.map.Viewport;
import org.oscim.utils.ColorUtil;
import org.oscim.utils.ColorsCSS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsmcSymbolFactory {
    private static final HashSet<String> VALID_BACKGROUNDS = new HashSet(3);
    private static final HashSet<String> VALID_FOREGROUNDS = new HashSet(36);
    private static final Logger logger = LoggerFactory.getLogger(OsmcSymbolFactory.class);
    private final BitmapCache<String, Bitmap> mBitmapCache = new BitmapCache(InputDeviceCompat.SOURCE_TOUCHSCREEN);

    static {
        VALID_BACKGROUNDS.add(Tag.VALUE_ROUND);
        VALID_BACKGROUNDS.add("circle");
        VALID_BACKGROUNDS.add("frame");
        VALID_FOREGROUNDS.add("ammonit");
        VALID_FOREGROUNDS.add("arch");
        VALID_FOREGROUNDS.add("backslash");
        VALID_FOREGROUNDS.add("bar");
        VALID_FOREGROUNDS.add("black_red_diamond");
        VALID_FOREGROUNDS.add("bowl");
        VALID_FOREGROUNDS.add("circle");
        VALID_FOREGROUNDS.add("corner");
        VALID_FOREGROUNDS.add("cross");
        VALID_FOREGROUNDS.add("diamond");
        VALID_FOREGROUNDS.add("diamond_line");
        VALID_FOREGROUNDS.add("dot");
        VALID_FOREGROUNDS.add("drop_line");
        VALID_FOREGROUNDS.add("fork");
        VALID_FOREGROUNDS.add("hexagon");
        VALID_FOREGROUNDS.add("hiker");
        VALID_FOREGROUNDS.add("horse");
        VALID_FOREGROUNDS.add("lower");
        VALID_FOREGROUNDS.add("L");
        VALID_FOREGROUNDS.add("mine");
        VALID_FOREGROUNDS.add("pointer");
        VALID_FOREGROUNDS.add("rectangle");
        VALID_FOREGROUNDS.add("rectangle_line");
        VALID_FOREGROUNDS.add("shell");
        VALID_FOREGROUNDS.add("shell_modern");
        VALID_FOREGROUNDS.add("slash");
        VALID_FOREGROUNDS.add("stripe");
        VALID_FOREGROUNDS.add("tower");
        VALID_FOREGROUNDS.add("triangle");
        VALID_FOREGROUNDS.add("triangle_line");
        VALID_FOREGROUNDS.add("triangle_turned");
        VALID_FOREGROUNDS.add("turned_T");
        VALID_FOREGROUNDS.add("wheel");
        VALID_FOREGROUNDS.add("white_red_diamond");
        VALID_FOREGROUNDS.add("wolfshook");
        VALID_FOREGROUNDS.add(MapService.EXTRA_X);
    }

    @Nullable
    public synchronized Bitmap getBitmap(@NonNull String osmcSymbol, int symbolPercent) {
        Object bitmap;
        String key = osmcSymbol + "%%%" + String.valueOf(symbolPercent);
        Bitmap bitmap2 = this.mBitmapCache.get(key);
        if (bitmap2 != null) {
            bitmap = bitmap2;
        } else {
            String[] background;
            int size = (int) ((((double) symbolPercent) * 0.2d) * ((double) MapTrek.density));
            float hSize = (float) (size / 2);
            float pWidth = Viewport.VIEW_DISTANCE * MapTrek.density;
            float hWidth = pWidth / 2.0f;
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(size, size, Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            logger.debug("symbol: {}", (Object) osmcSymbol);
            String[] parts = osmcSymbol.trim().split("\\s*:\\s*");
            boolean isRound = false;
            if (parts.length > 1) {
                background = parts[1].trim().split("\\s*_\\s*");
            } else {
                background = parts[0].trim().split("\\s*_\\s*");
            }
            logger.debug("  background: {}", Arrays.toString(background));
            Integer backgroundColor = ColorsCSS.get(background[0]);
            if (background.length != 1 && VALID_BACKGROUNDS.contains(background[1])) {
                Paint paint = new Paint();
                paint.setStyle(Style.FILL);
                paint.setStrokeWidth(pWidth);
                int fillColor = "white".equals(background[0]) ? -16777216 : -1;
                int backColor = backgroundColor != null ? backgroundColor.intValue() : -16777216;
                if (Tag.VALUE_ROUND.equals(background[1])) {
                    isRound = true;
                    paint.setColor(backColor);
                    canvas.drawCircle(hSize, hSize, hSize - hWidth, paint);
                } else if ("circle".equals(background[1])) {
                    isRound = true;
                    paint.setColor(fillColor);
                    canvas.drawCircle(hSize, hSize, hSize - hWidth, paint);
                    paint.setColor(backColor);
                    paint.setStyle(Style.STROKE);
                    canvas.drawCircle(hSize, hSize, hSize - hWidth, paint);
                } else {
                    paint.setColor(fillColor);
                    canvas.drawRect(hWidth, hWidth, ((float) size) - hWidth, ((float) size) - hWidth, paint);
                    paint.setColor(backColor);
                    paint.setStyle(Style.STROKE);
                    canvas.drawRect(hWidth, hWidth, ((float) size) - hWidth, ((float) size) - hWidth, paint);
                }
            } else if (backgroundColor != null) {
                canvas.drawColor(backgroundColor.intValue());
            } else {
                canvas.drawColor(-1);
            }
            if (parts.length == 3 || parts.length > 4) {
                drawSymbol(canvas, parts[2], size);
            }
            if (parts.length > 5) {
                drawSymbol(canvas, parts[3], size);
            }
            if (parts.length > 3) {
                String text = parts[parts.length - 2];
                String color = parts[parts.length - 1];
                Integer textColor = ColorsCSS.get(color);
                if ("yellow".equals(color)) {
                    textColor = Integer.valueOf(ColorUtil.modHsv(textColor.intValue(), 1.0d, 1.2d, 0.8d, false));
                }
                if (textColor != null && text.length() > 0) {
                    Paint textPaint = new Paint();
                    textPaint.setColor(textColor.intValue());
                    textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                    textPaint.setTextAlign(Align.CENTER);
                    textPaint.setStyle(Style.FILL_AND_STROKE);
                    float width = ((float) size) - pWidth;
                    if (isRound) {
                        width = hSize;
                    } else if (text.length() == 2) {
                        width *= 0.7f;
                    } else if (text.length() == 1) {
                        width *= 0.5f;
                    }
                    setTextSizeForWidth(textPaint, width, text);
                    canvas.drawText(text, hSize, hSize - ((textPaint.descent() + textPaint.ascent()) / 2.0f), textPaint);
                }
            }
            Bitmap androidBitmap = new AndroidBitmap(bmp);
            this.mBitmapCache.put(key, androidBitmap);
            Bitmap bitmap3 = androidBitmap;
        }
        return bitmap;
    }

    private void drawSymbol(Canvas canvas, String foreground, int size) {
        if (foreground.length() != 0) {
            Integer foregroundColor = null;
            String symbol = null;
            if (VALID_FOREGROUNDS.contains(foreground)) {
                logger.debug("  foreground: black {}", (Object) foreground);
                if ("shell".equals(foreground) || "shell_modern".equals(foreground)) {
                    foregroundColor = Integer.valueOf(-256);
                } else {
                    foregroundColor = Integer.valueOf(-16777216);
                }
                symbol = foreground;
            } else {
                String[] foreground_parts = foreground.trim().split("\\s*_\\s*", 2);
                logger.debug("  foreground: {}", Arrays.toString(foreground_parts));
                if (foreground_parts.length == 2) {
                    foregroundColor = ColorsCSS.get(foreground_parts[0]);
                    if (VALID_FOREGROUNDS.contains(foreground_parts[1])) {
                        symbol = foreground_parts[1];
                    }
                }
            }
            if (foregroundColor != null && symbol != null) {
                try {
                    Bitmap symbolBitmap = CanvasAdapter.getBitmapAsset("symbols/osmc", symbol + ".svg", size, size, 100);
                    Paint paint = new Paint();
                    paint.setColorFilter(new PorterDuffColorFilter(foregroundColor.intValue(), Mode.SRC_IN));
                    canvas.drawBitmap(AndroidGraphics.getBitmap(symbolBitmap), 0.0f, 0.0f, paint);
                } catch (Throwable e) {
                    logger.error("Failed to load bitmap for " + symbol, e);
                }
            }
        }
    }

    private static void setTextSizeForWidth(Paint paint, float desiredWidth, String text) {
        paint.setTextSize(100.0f);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        paint.setTextSize((100.0f * desiredWidth) / (bounds.width() > bounds.height() ? (float) bounds.width() : (float) bounds.height()));
    }

    public void dispose() {
        this.mBitmapCache.clear();
    }
}
