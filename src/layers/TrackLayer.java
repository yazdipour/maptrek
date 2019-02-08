package mobi.maptrek.layers;

import mobi.maptrek.data.Track;
import mobi.maptrek.data.Track.TrackPoint;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.theme.styles.LineStyle;
import org.oscim.utils.FastMath;
import org.oscim.utils.async.SimpleWorker;
import org.oscim.utils.geom.LineClipper;

public class TrackLayer extends Layer {
    LineStyle mLineStyle;
    final Track mTrack;
    private boolean mUpdatePoints;
    private final Worker mWorker;

    static final class Task {
        RenderBuckets bucket = new RenderBuckets();
        MapPosition pos = new MapPosition();

        Task() {
        }
    }

    private final class Worker extends SimpleWorker<Task> {
        private static final int GROW_INDICES = 32;
        private static final int MIN_DIST = 3;
        private int[] index = new int[1];
        private final LineClipper mClipper = new LineClipper(-2048.0f, -2048.0f, LineBucket.DIR_SCALE, LineBucket.DIR_SCALE);
        private int mNumPoints;
        private float[] mPPoints = new float[0];
        private double[] mPreprojected = new double[2];
        private final int max = 2048;

        Worker(Map map) {
            super(map, 0, new Task(), new Task());
        }

        public boolean doWork(Task task) {
            int indexPos;
            int i;
            int size = this.mNumPoints;
            if (TrackLayer.this.mUpdatePoints) {
                synchronized (TrackLayer.this.mTrack) {
                    TrackLayer.this.mUpdatePoints = false;
                    indexPos = 0;
                    this.index[0] = -1;
                    size = TrackLayer.this.mTrack.points.size();
                    this.mNumPoints = size;
                    double[] points = this.mPreprojected;
                    if (size * 2 >= points.length) {
                        points = new double[(size * 2)];
                        this.mPreprojected = points;
                        this.mPPoints = new float[(size * 2)];
                    }
                    i = 0;
                    while (i < size) {
                        GeoPoint point = (TrackPoint) TrackLayer.this.mTrack.points.get(i);
                        MercatorProjection.project(point, points, i);
                        if (!point.continuous && i > 0) {
                            if (indexPos + 1 >= this.index.length) {
                                ensureIndexSize(indexPos + 1, true);
                            }
                            this.index[indexPos] = i;
                            indexPos++;
                            if (this.index.length > indexPos + 1) {
                                this.index[indexPos] = -1;
                            }
                        }
                        i++;
                    }
                }
            }
            if (size == 0 || !TrackLayer.this.isEnabled()) {
                if (task.bucket.get() != null) {
                    task.bucket.clear();
                    this.mMap.render();
                }
                return true;
            }
            LineBucket ll = task.bucket.getLineBucket(0);
            ll.line = TrackLayer.this.mLineStyle;
            ll.scale = ll.line.width;
            this.mMap.getMapPosition(task.pos);
            int zoomlevel = task.pos.zoomLevel;
            task.pos.scale = (double) (1 << zoomlevel);
            double mx = task.pos.x;
            double my = task.pos.y;
            double scale = ((double) Tile.SIZE) * task.pos.scale;
            int flip = 0;
            int maxx = Tile.SIZE << (zoomlevel - 1);
            int x = (int) ((this.mPreprojected[0] - mx) * scale);
            int y = (int) ((this.mPreprojected[1] - my) * scale);
            if (x > maxx) {
                x -= maxx * 2;
                flip = -1;
            } else {
                if (x < (-maxx)) {
                    x += maxx * 2;
                    flip = 1;
                }
            }
            this.mClipper.clipStart((float) x, (float) y);
            float[] projected = this.mPPoints;
            float prevX = (float) x;
            float prevY = (float) y;
            float[] segment = null;
            indexPos = 0;
            int j = 2;
            int i2 = addPoint(projected, 0, x, y);
            while (j < size * 2) {
                x = (int) ((this.mPreprojected[j + 0] - mx) * scale);
                y = (int) ((this.mPreprojected[j + 1] - my) * scale);
                int flipDirection = 0;
                if (x > maxx) {
                    x -= maxx * 2;
                    flipDirection = -1;
                } else {
                    if (x < (-maxx)) {
                        x += maxx * 2;
                        flipDirection = 1;
                    }
                }
                if (this.index[indexPos] == (j >> 1)) {
                    if (i2 > 2) {
                        ll.addLine(projected, i2, false);
                    }
                    this.mClipper.clipStart((float) x, (float) y);
                    i = addPoint(projected, 0, x, y);
                    indexPos++;
                } else if (flip != flipDirection) {
                    flip = flipDirection;
                    if (i2 > 2) {
                        ll.addLine(projected, i2, false);
                    }
                    this.mClipper.clipStart((float) x, (float) y);
                    i = addPoint(projected, 0, x, y);
                } else {
                    int clip = this.mClipper.clipNext((float) x, (float) y);
                    if (clip < 1) {
                        if (i2 > 2) {
                            ll.addLine(projected, i2, false);
                        }
                        if (clip < 0) {
                            segment = this.mClipper.getLine(segment, 0);
                            ll.addLine(segment, 4, false);
                            prevX = this.mClipper.outX2;
                            prevY = this.mClipper.outY2;
                        }
                        i = 0;
                    } else {
                        float dx = ((float) x) - prevX;
                        float dy = ((float) y) - prevY;
                        if (i2 == 0 || FastMath.absMaxCmp(dx, dy, Viewport.VIEW_DISTANCE)) {
                            i = i2 + 1;
                            prevX = (float) x;
                            projected[i2] = prevX;
                            i2 = i + 1;
                            prevY = (float) y;
                            projected[i] = prevY;
                        }
                        i = i2;
                    }
                }
                j += 2;
                i2 = i;
            }
            if (i2 > 2) {
                ll.addLine(projected, i2, false);
            }
            this.mMap.render();
            return true;
        }

        public void cleanup(Task task) {
            task.bucket.clear();
        }

        private int addPoint(float[] points, int i, int x, int y) {
            int i2 = i + 1;
            points[i] = (float) x;
            i = i2 + 1;
            points[i2] = (float) y;
            return i;
        }

        int[] ensureIndexSize(int size, boolean copy) {
            if (size < this.index.length) {
                return this.index;
            }
            int[] newIndex = new int[(size + 32)];
            if (copy) {
                System.arraycopy(this.index, 0, newIndex, 0, this.index.length);
            }
            this.index = newIndex;
            return this.index;
        }
    }

    private final class RenderPath extends BucketRenderer {
        private int mCurX = -1;
        private int mCurY = -1;
        private int mCurZ = -1;

        RenderPath() {
            this.buckets.addLineBucket(0, TrackLayer.this.mLineStyle);
        }

        public synchronized void update(GLViewport v) {
            int tz = 1 << v.pos.zoomLevel;
            int tx = (int) (v.pos.x * ((double) tz));
            int ty = (int) (v.pos.y * ((double) tz));
            if (!(tx == this.mCurX && ty == this.mCurY && tz == this.mCurZ)) {
                TrackLayer.this.mWorker.submit(100);
                this.mCurX = tx;
                this.mCurY = ty;
                this.mCurZ = tz;
            }
            Task t = (Task) TrackLayer.this.mWorker.poll();
            if (t != null) {
                this.mMapPosition.copy(t.pos);
                this.buckets.set(t.bucket.get());
                compile();
            }
        }
    }

    public TrackLayer(Map map, Track track) {
        super(map);
        this.mWorker = new Worker(map);
        this.mLineStyle = new LineStyle(track.style.color, track.style.width, Cap.BUTT);
        this.mRenderer = new RenderPath();
        this.mTrack = track;
        updatePoints();
    }

    void updatePoints() {
        this.mWorker.submit(10);
        this.mUpdatePoints = true;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mWorker.submit(10);
    }

    public Track getTrack() {
        return this.mTrack;
    }

    public void setColor(int color) {
        this.mLineStyle = new LineStyle(color, this.mLineStyle.width, this.mLineStyle.cap);
        this.mWorker.submit(10);
    }
}
