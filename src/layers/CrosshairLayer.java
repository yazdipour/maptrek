package mobi.maptrek.layers;

import android.os.SystemClock;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.map.Map.UpdateListener;
import org.oscim.map.Viewport;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.renderer.MapRenderer;

public class CrosshairLayer extends Layer implements UpdateListener {
    private static final int DEFAULT_COLOR = -13421773;

    private class CrosshairRenderer extends LayerRenderer {
        private static final long ANIM_RATE = 50;
        private static final long FADE_DURATION = 1000;
        private static final long FADE_TIMEOUT = 3000;
        private int hColor;
        private int hMatrixPosition;
        private int hScale;
        private int hVertexPosition;
        private float mAlpha = Viewport.VIEW_NEAR;
        private long mAnimStart;
        private int mColor = CrosshairLayer.DEFAULT_COLOR;
        private boolean mFading = true;
        private boolean mInitialized;
        private long mLastShown;
        private boolean mRunAnim;
        private float mScale;
        private int mShaderProgram;

        CrosshairRenderer(float scale) {
            this.mScale = scale;
        }

        void setColor(int color) {
            this.mColor = color;
            CrosshairLayer.this.mMap.render();
        }

        void setFading(boolean fading) {
            this.mFading = fading;
            if (this.mFading) {
                animate(true);
            }
        }

        void show() {
            animate(false);
            this.mAlpha = Viewport.VIEW_NEAR;
            this.mLastShown = SystemClock.elapsedRealtime();
            if (this.mFading) {
                CrosshairLayer.this.mMap.postDelayed(new Runnable() {
                    public void run() {
                        if (CrosshairRenderer.this.mLastShown + CrosshairRenderer.FADE_TIMEOUT <= SystemClock.elapsedRealtime()) {
                            CrosshairRenderer.this.animate(true);
                        }
                    }
                }, 3100);
            }
        }

        private void animate(boolean enable) {
            if (this.mRunAnim != enable) {
                this.mRunAnim = enable;
                if (enable) {
                    Runnable action = new Runnable() {
                        private long lastRun;

                        public void run() {
                            if (CrosshairRenderer.this.mRunAnim) {
                                CrosshairLayer.this.mMap.postDelayed(this, Math.min(CrosshairRenderer.ANIM_RATE, SystemClock.elapsedRealtime() - this.lastRun));
                                CrosshairLayer.this.mMap.render();
                                this.lastRun = System.currentTimeMillis();
                            }
                        }
                    };
                    this.mAnimStart = SystemClock.elapsedRealtime();
                    CrosshairLayer.this.mMap.postDelayed(action, ANIM_RATE);
                }
            }
        }

        private float animPhase() {
            return ((float) ((MapRenderer.frametime - this.mAnimStart) % FADE_DURATION)) / 1000.0f;
        }

        public void update(GLViewport v) {
            if (!this.mInitialized) {
                init();
                show();
                this.mInitialized = true;
            }
            setReady(CrosshairLayer.this.isEnabled());
        }

        public void render(GLViewport v) {
            GLState.useProgram(this.mShaderProgram);
            GLState.blend(true);
            GLState.test(false, false);
            GLState.enableVertexArrays(this.hVertexPosition, -1);
            MapRenderer.bindQuadVertexVBO(this.hVertexPosition);
            if (this.mRunAnim) {
                float alpha = Viewport.VIEW_NEAR - animPhase();
                if (alpha > this.mAlpha || alpha < 0.01f) {
                    this.mAlpha = 0.0f;
                    animate(false);
                } else {
                    this.mAlpha = alpha;
                }
            }
            v.mvp.setTransScale(0.0f, 0.0f, Viewport.VIEW_NEAR);
            v.mvp.multiplyMM(v.proj, v.mvp);
            v.mvp.setAsUniform(this.hMatrixPosition);
            GLAdapter.gl.uniform1f(this.hScale, this.mScale);
            GLUtils.setColor(this.hColor, this.mColor, this.mAlpha);
            GLAdapter.gl.drawArrays(5, 0, 4);
        }

        private boolean init() {
            int shader = GLShader.loadShader("crosshair");
            if (shader == 0) {
                return false;
            }
            this.mShaderProgram = shader;
            this.hVertexPosition = GLAdapter.gl.getAttribLocation(shader, "a_pos");
            this.hMatrixPosition = GLAdapter.gl.getUniformLocation(shader, "u_mvp");
            this.hScale = GLAdapter.gl.getUniformLocation(shader, "u_scale");
            this.hColor = GLAdapter.gl.getUniformLocation(shader, "u_color");
            return true;
        }
    }

    public CrosshairLayer(Map map, float scale) {
        super(map);
        this.mRenderer = new CrosshairRenderer(scale);
    }

    public void setEnabled(boolean enabled) {
        if (enabled != isEnabled()) {
            super.setEnabled(enabled);
            if (enabled) {
                ((CrosshairRenderer) this.mRenderer).show();
            }
        }
    }

    public void onMapEvent(Event event, MapPosition mapPosition) {
        if (!isEnabled()) {
            return;
        }
        if (event == Map.MOVE_EVENT || event == Map.POSITION_EVENT) {
            ((CrosshairRenderer) this.mRenderer).show();
        }
    }

    public void lock(int color) {
        ((CrosshairRenderer) this.mRenderer).setColor(color);
        ((CrosshairRenderer) this.mRenderer).setFading(false);
        ((CrosshairRenderer) this.mRenderer).show();
    }

    public void unlock() {
        ((CrosshairRenderer) this.mRenderer).setColor(DEFAULT_COLOR);
        ((CrosshairRenderer) this.mRenderer).setFading(true);
    }
}
