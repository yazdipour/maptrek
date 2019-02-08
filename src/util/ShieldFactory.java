package mobi.maptrek.util;

import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import mobi.maptrek.MapTrek;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.map.Viewport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShieldFactory {
    private static final Logger logger = LoggerFactory.getLogger(ShieldFactory.class);
    private final BitmapCache<String, Bitmap> mBitmapCache = new BitmapCache(512);
    private float mFontSize = Viewport.VIEW_NEAR;

    @Nullable
    public synchronized Bitmap getBitmap(@NonNull TagSet tags, String src, int percent) {
        Bitmap bitmap;
        String ref = tags.getValue(Tag.KEY_REF);
        if (ref == null) {
            bitmap = null;
        } else {
            bitmap = this.mBitmapCache.get(ref);
            if (bitmap == null) {
                String[] parts = src.replace("/shield/", "").trim().split("/");
                if (parts.length < 3) {
                    bitmap = null;
                } else {
                    String[] lines = ref.split(";");
                    float textSize = Float.parseFloat(parts[0]) * this.mFontSize;
                    int backColor = Color.parseColor(parts[1], -1);
                    int textColor = Color.parseColor(parts[2], -16777216);
                    float size = ((((float) percent) * 0.01f) * textSize) * MapTrek.density;
                    Paint textPaint = new Paint();
                    textPaint.setColor(textColor);
                    textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                    textPaint.setTextAlign(Align.CENTER);
                    textPaint.setStyle(Style.FILL_AND_STROKE);
                    textPaint.setAntiAlias(true);
                    textPaint.setTextSize(size);
                    float textHeight = 0.0f;
                    Rect bounds = new Rect();
                    for (String line : lines) {
                        Rect rect = new Rect();
                        textPaint.getTextBounds(line, 0, line.length(), rect);
                        bounds.union(rect);
                        bounds.bottom = (int) (((float) bounds.bottom) + textHeight);
                        if (textHeight == 0.0f) {
                            textHeight = textPaint.descent() - textPaint.ascent();
                        }
                    }
                    float gap = (4.0f * MapTrek.density) * this.mFontSize;
                    float border = (1.6f * MapTrek.density) * this.mFontSize;
                    float r = 2.0f * border;
                    float hb = border / 2.0f;
                    int width = (int) (((float) bounds.width()) + (2.0f * (gap + border)));
                    int height = (int) (((float) bounds.height()) + (2.0f * (gap + border)));
                    android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(width, height, Config.ARGB_8888);
                    Canvas canvas = new Canvas(bmp);
                    Paint paint = new Paint();
                    paint.setStyle(Style.FILL);
                    paint.setStrokeWidth(border);
                    paint.setColor(backColor);
                    canvas.drawRoundRect(hb, hb, ((float) width) - hb, ((float) height) - hb, r, r, paint);
                    paint.setColor(textColor);
                    paint.setStyle(Style.STROKE);
                    paint.setAntiAlias(true);
                    canvas.drawRoundRect(hb, hb, ((float) width) - hb, ((float) height) - hb, r, r, paint);
                    float x = ((float) width) / 2.0f;
                    float y = ((((float) height) / 2.0f) - ((textPaint.descent() + textPaint.ascent()) / 2.0f)) - ((((float) (lines.length - 1)) * textHeight) / 2.0f);
                    for (String line2 : lines) {
                        canvas.drawText(line2, x, y, textPaint);
                        y += textHeight;
                    }
                    bitmap = new AndroidBitmap(bmp);
                    this.mBitmapCache.put(ref, bitmap);
                }
            }
        }
        return bitmap;
    }

    public void setFontSize(float fontSize) {
        this.mFontSize = fontSize;
    }

    public void dispose() {
        this.mBitmapCache.clear();
    }
}
