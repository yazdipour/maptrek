package mobi.maptrek.data.style;

import org.oscim.backend.canvas.Color;

public class MarkerStyle extends Style<MarkerStyle> {
    public static int DEFAULT_COLOR = Color.DKGRAY;
    public static final int[] DEFAULT_COLORS = new int[]{Color.DKGRAY, -14654801, -10965321, -740056, -2277816, -4224594, -10712898, -10896368, -1544140, -504764, -7583749, -11419154, -7551917, -4024195, -3246217, -7305542};
    public int color = DEFAULT_COLOR;
    public String icon;

    public boolean isDefault() {
        return this.color == DEFAULT_COLOR && this.icon == null;
    }

    public void copy(MarkerStyle style) {
        style.color = this.color;
        style.icon = this.icon;
    }
}
