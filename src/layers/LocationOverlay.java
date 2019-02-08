package mobi.maptrek.layers;

import android.os.SystemClock;
import org.oscim.backend.GLAdapter;
import org.oscim.core.Box;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.renderer.MapRenderer;
import org.oscim.utils.FastMath;
import org.oscim.utils.math.Interpolation;

public class LocationOverlay extends Layer {
    private float mBearing;
    private final Point mLocation = new Point();

    private class LocationIndicator extends LayerRenderer {
        private static final long ANIM_RATE = 50;
        private static final long INTERVAL = 8000;
        private int hDirection;
        private int hMatrixPosition;
        private int hPhase;
        private int hScale;
        private int hType;
        private int hVertexPosition;
        private long mAnimStart;
        private final Box mBBox = new Box();
        private final Point mIndicatorPosition = new Point();
        private boolean mInitialized;
        private boolean mLocationIsVisible;
        private boolean mReanimated = false;
        private boolean mRunAnim;
        private final float mScale;
        private int mShaderProgram;

        LocationIndicator(float scale) {
            this.mScale = scale;
        }

        private void animate(boolean enable) {
            if (this.mRunAnim != enable) {
                this.mReanimated = true;
                this.mRunAnim = enable;
                if (enable) {
                    Runnable action = new Runnable() {
                        private long lastRun;

                        public void run() {
                            if (LocationIndicator.this.mRunAnim) {
                                LocationOverlay.this.mMap.postDelayed(this, Math.min(LocationIndicator.ANIM_RATE, SystemClock.elapsedRealtime() - this.lastRun));
                                LocationOverlay.this.mMap.render();
                                this.lastRun = SystemClock.elapsedRealtime();
                            }
                        }
                    };
                    this.mAnimStart = SystemClock.elapsedRealtime();
                    LocationOverlay.this.mMap.postDelayed(action, ANIM_RATE);
                }
            }
        }

        private float animPhase() {
            return ((float) ((MapRenderer.frametime - this.mAnimStart) % INTERVAL)) / 8000.0f;
        }

        public void update(GLViewport v) {
            if (!this.mInitialized) {
                init();
                this.mInitialized = true;
            }
            if (!LocationOverlay.this.isEnabled()) {
                setReady(false);
            } else if (v.changed() || this.mReanimated || isReady()) {
                setReady(true);
                v.getBBox(this.mBBox, 0);
                this.mLocationIsVisible = this.mBBox.contains(LocationOverlay.this.mLocation);
                this.mIndicatorPosition.x = FastMath.clamp(LocationOverlay.this.mLocation.x, this.mBBox.xmin, this.mBBox.xmax);
                this.mIndicatorPosition.y = FastMath.clamp(LocationOverlay.this.mLocation.y, this.mBBox.ymin, this.mBBox.ymax);
            }
        }

        public void render(GLViewport v) {
            GLState.useProgram(this.mShaderProgram);
            GLState.blend(true);
            GLState.test(false, false);
            GLState.enableVertexArrays(this.hVertexPosition, -1);
            MapRenderer.bindQuadVertexVBO(this.hVertexPosition);
            double tileScale = ((double) Tile.SIZE) * v.pos.scale;
            v.mvp.setTransScale((float) ((this.mIndicatorPosition.x - v.pos.x) * tileScale), (float) ((this.mIndicatorPosition.y - v.pos.y) * tileScale), Viewport.VIEW_NEAR);
            v.mvp.multiplyMM(v.viewproj, v.mvp);
            v.mvp.setAsUniform(this.hMatrixPosition);
            GLAdapter.gl.uniform1f(this.hScale, this.mScale);
            if (this.mLocationIsVisible) {
                animate(false);
                GLAdapter.gl.uniform1f(this.hPhase, Viewport.VIEW_NEAR);
                float rotation = LocationOverlay.this.mBearing - 90.0f;
                if (rotation > 180.0f) {
                    rotation -= 360.0f;
                } else if (rotation < -180.0f) {
                    rotation += 360.0f;
                }
                GLAdapter.gl.uniform2f(this.hDirection, (float) Math.cos(Math.toRadians((double) rotation)), (float) Math.sin(Math.toRadians((double) rotation)));
            } else {
                animate(true);
                GLAdapter.gl.uniform1f(this.hPhase, 0.8f + (0.2f * Interpolation.swing.apply(Math.abs(animPhase() - 0.5f) * 2.0f)));
                GLAdapter.gl.uniform2f(this.hDirection, 0.0f, 0.0f);
            }
            GLAdapter.gl.uniform1f(this.hType, 0.0f);
            GLAdapter.gl.drawArrays(5, 0, 4);
        }

        private boolean init() {
            int shader = GLShader.loadShader("location_pointer");
            if (shader == 0) {
                return false;
            }
            this.mShaderProgram = shader;
            this.hVertexPosition = GLAdapter.gl.getAttribLocation(shader, "a_pos");
            this.hMatrixPosition = GLAdapter.gl.getUniformLocation(shader, "u_mvp");
            this.hPhase = GLAdapter.gl.getUniformLocation(shader, "u_phase");
            this.hScale = GLAdapter.gl.getUniformLocation(shader, "u_scale");
            this.hDirection = GLAdapter.gl.getUniformLocation(shader, "u_dir");
            this.hType = GLAdapter.gl.getUniformLocation(shader, "u_type");
            return true;
        }
    }

    public LocationOverlay(Map map, float scale) {
        super(map);
        this.mRenderer = new LocationIndicator(scale);
        setEnabled(false);
    }

    public void setPosition(double latitude, double longitude, float bearing) {
        this.mLocation.x = MercatorProjection.longitudeToX(longitude);
        this.mLocation.y = MercatorProjection.latitudeToY(latitude);
        this.mBearing = bearing;
        ((LocationIndicator) this.mRenderer).animate(true);
    }

    public Point getPosition() {
        return new Point(this.mLocation.x, this.mLocation.y);
    }

    public void setEnabled(boolean enabled) {
        if (enabled != isEnabled()) {
            super.setEnabled(enabled);
            ((LocationIndicator) this.mRenderer).animate(enabled);
        }
    }
}
