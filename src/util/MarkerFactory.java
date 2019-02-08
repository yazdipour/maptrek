package mobi.maptrek.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.VectorDrawable;
import android.support.annotation.DrawableRes;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.style.MarkerStyle;

public class MarkerFactory {
    static final /* synthetic */ boolean $assertionsDisabled = (!MarkerFactory.class.desiredAssertionStatus());

    public static Bitmap getMarkerSymbol(Context context) {
        return getMarkerSymbol(context, MarkerStyle.DEFAULT_COLOR);
    }

    public static Bitmap getMarkerSymbol(Context context, int color) {
        return getMarkerSymbol(context, R.drawable.marker, color);
    }

    public static Bitmap getMarkerSymbol(Context context, @DrawableRes int drawableRes, int color) {
        VectorDrawable vectorDrawable = (VectorDrawable) context.getDrawable(drawableRes);
        if ($assertionsDisabled || vectorDrawable != null) {
            int size = (int) (25.0f * MapTrek.density);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, size, size);
            vectorDrawable.setTint(color);
            vectorDrawable.draw(canvas);
            return bitmap;
        }
        throw new AssertionError();
    }
}
