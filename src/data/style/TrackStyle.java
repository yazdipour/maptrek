package mobi.maptrek.data.style;

import org.oscim.backend.canvas.Color;

public class TrackStyle extends Style<TrackStyle> {
    public static int DEFAULT_COLOR = Color.MAGENTA;
    public static float DEFAULT_WIDTH = 5.0f;
    public int color = DEFAULT_COLOR;
    public float width = DEFAULT_WIDTH;

    public boolean isDefault() {
        return this.color == DEFAULT_COLOR && this.width == DEFAULT_WIDTH;
    }

    public void copy(TrackStyle style) {
        style.color = this.color;
        style.width = this.width;
    }
}
