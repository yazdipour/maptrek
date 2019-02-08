package mobi.maptrek.util;

import org.oscim.map.Viewport;

public class MeanValue {
    private float Ex = 0.0f;
    private float Ex2 = 0.0f;
    private float K = 0.0f;
    private float n = 0.0f;

    public void addValue(float x) {
        if (this.n == 0.0f) {
            this.K = x;
        }
        this.n += Viewport.VIEW_NEAR;
        this.Ex += x - this.K;
        this.Ex2 += (x - this.K) * (x - this.K);
    }

    public void removeValue(float x) {
        this.n -= Viewport.VIEW_NEAR;
        this.Ex -= x - this.K;
        this.Ex2 -= (x - this.K) * (x - this.K);
    }

    public float getMeanValue() {
        return this.K + (this.Ex / this.n);
    }

    public float getVariance() {
        return (this.Ex2 - ((this.Ex * this.Ex) / this.n)) / (this.n - Viewport.VIEW_NEAR);
    }
}
