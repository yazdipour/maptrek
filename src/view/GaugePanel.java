package mobi.maptrek.view;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GaugePanel extends ViewGroup implements OnLongClickListener, OnMenuItemClickListener, SensorEventListener {
    public static final String DEFAULT_GAUGE_SET = "1,65536";
    private static final Logger logger = LoggerFactory.getLogger(GaugePanel.class);
    private SparseArray<Gauge> mGaugeMap = new SparseArray();
    private ArrayList<Gauge> mGauges = new ArrayList();
    private boolean mHasSensors;
    private List<View> mLineViewsBuffer = new ArrayList();
    private final List<Integer> mLineWidths = new ArrayList();
    private final List<List<View>> mLines = new ArrayList();
    private MapHolder mMapHolder;
    private boolean mNavigationMode = false;
    private Sensor mPressureSensor;
    private SensorManager mSensorManager;
    private boolean mVisible;

    public GaugePanel(Context context) {
        super(context);
    }

    public GaugePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GaugePanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int sizeWidth = (MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft()) - getPaddingRight();
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        int width = 0;
        int height = 0;
        int lineWidth = 0;
        int lineHeight = 0;
        int childCount = getChildCount();
        int i = 0;
        while (i < childCount) {
            View child = getChildAt(i);
            boolean lastChild = i == childCount + -1;
            if (child.getVisibility() != 8) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                LayoutParams lp = child.getLayoutParams();
                int childWidthMode = Integer.MIN_VALUE;
                int childWidthSize = sizeWidth;
                int childHeightMode = Integer.MIN_VALUE;
                int childHeightSize = sizeHeight;
                if (lp.width == -1) {
                    childWidthMode = 1073741824;
                } else if (lp.width >= 0) {
                    childWidthMode = 1073741824;
                    childWidthSize = lp.width;
                } else if (modeWidth == 0) {
                    childWidthMode = 0;
                    childWidthSize = 0;
                }
                if (lp.width == -1) {
                    childWidthMode = 1073741824;
                } else if (lp.height >= 0) {
                    childHeightMode = 1073741824;
                    childHeightSize = lp.height;
                } else if (modeHeight == 0) {
                    childHeightMode = 0;
                    childHeightSize = 0;
                }
                child.measure(MeasureSpec.makeMeasureSpec(childWidthSize, childWidthMode), MeasureSpec.makeMeasureSpec(childHeightSize, childHeightMode));
                int childHeight = child.getMeasuredHeight();
                if (lineHeight + childHeight > sizeHeight) {
                    height = Math.max(height, lineHeight);
                    lineHeight = childHeight;
                    width += lineWidth;
                    lineWidth = child.getMeasuredWidth();
                } else {
                    lineHeight += childHeight;
                    lineWidth = Math.max(lineWidth, child.getMeasuredWidth());
                }
                if (lastChild) {
                    height = Math.max(height, lineHeight);
                    width += lineWidth;
                }
            } else if (lastChild) {
                width += lineWidth;
                height = Math.max(height, lineHeight);
            }
            i++;
        }
        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();
        if (modeWidth != 1073741824) {
            sizeWidth = width;
        }
        if (modeHeight != 1073741824) {
            sizeHeight = height;
        }
        setMeasuredDimension(sizeWidth, sizeHeight);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int i;
        int childHeight;
        this.mLines.clear();
        this.mLineWidths.clear();
        int width = getWidth();
        int height = getHeight();
        int childCount = getChildCount();
        int linesSum = getPaddingTop();
        int lineWidth = 0;
        int lineHeight = 0;
        this.mLineViewsBuffer.clear();
        for (i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                int childWidth = child.getMeasuredWidth();
                childHeight = child.getMeasuredHeight();
                if (lineHeight + childHeight > height) {
                    this.mLineWidths.add(Integer.valueOf(lineWidth));
                    this.mLines.add(this.mLineViewsBuffer);
                    linesSum += lineWidth;
                    lineHeight = 0;
                    lineWidth = 0;
                    this.mLineViewsBuffer.clear();
                }
                lineHeight += childHeight;
                lineWidth = Math.max(lineWidth, childWidth);
                this.mLineViewsBuffer.add(child);
            }
        }
        this.mLineWidths.add(Integer.valueOf(lineWidth));
        this.mLines.add(this.mLineViewsBuffer);
        int horizontalGravityMargin = width - (linesSum + lineWidth);
        int numLines = this.mLines.size();
        int left = getPaddingLeft();
        for (i = 0; i < numLines; i++) {
            lineWidth = ((Integer) this.mLineWidths.get(i)).intValue();
            this.mLineViewsBuffer = (List) this.mLines.get(i);
            int top = getPaddingTop();
            int children = this.mLineViewsBuffer.size();
            for (int j = 0; j < children; j++) {
                child = (View) this.mLineViewsBuffer.get(j);
                if (child.getVisibility() != 8) {
                    childWidth = child.getMeasuredWidth();
                    childHeight = child.getMeasuredHeight();
                    int gravityMargin = lineWidth - childWidth;
                    child.layout((left + gravityMargin) + horizontalGravityMargin, top, ((left + childWidth) + gravityMargin) + horizontalGravityMargin, top + childHeight);
                    top += childHeight;
                }
            }
            left += lineWidth;
        }
    }

    public void initializeGauges(String settings) {
        this.mSensorManager = (SensorManager) getContext().getSystemService("sensor");
        this.mPressureSensor = this.mSensorManager.getDefaultSensor(6);
        for (String gaugeStr : settings.split(",")) {
            addGauge(Integer.valueOf(gaugeStr).intValue());
        }
        setNavigationMode(false);
    }

    public void setMapHolder(MapHolder mapHolder) {
        this.mMapHolder = mapHolder;
    }

    private String getGaugeName(int type) {
        Context context = getContext();
        switch (type) {
            case 1:
                return context.getString(R.string.gauge_speed);
            case 2:
                return context.getString(R.string.gauge_track);
            case 4:
                return context.getString(R.string.gauge_altitude);
            case 4096:
                return context.getString(R.string.gauge_elevation);
            case 65536:
                return context.getString(R.string.gauge_distance);
            case 131072:
                return context.getString(R.string.gauge_bearing);
            case 262144:
                return context.getString(R.string.gauge_turn);
            case 524288:
                return context.getString(R.string.gauge_vmg);
            case 1048576:
                return context.getString(R.string.gauge_xtk);
            case 2097152:
                return context.getString(R.string.gauge_ete);
            default:
                return "";
        }
    }

    private void addGauge(int type) {
        Gauge gauge = new Gauge(getContext(), type);
        gauge.setValue(Float.NaN);
        if (isNavigationGauge(type)) {
            addView(gauge);
            if (!this.mNavigationMode) {
                gauge.setVisibility(8);
            }
            this.mGauges.add(gauge);
        } else {
            int i = 0;
            while (i < this.mGauges.size() && !isNavigationGauge(((Gauge) this.mGauges.get(i)).getType())) {
                i++;
            }
            addView(gauge, i);
            this.mGauges.add(i, gauge);
        }
        this.mGaugeMap.put(type, gauge);
        this.mHasSensors = this.mGaugeMap.get(4096) != null;
        if (type == 4096 && this.mPressureSensor != null && this.mVisible) {
            this.mSensorManager.registerListener(this, this.mPressureSensor, 3, 1000);
        }
        gauge.setGravity(8388661);
        gauge.setOnLongClickListener(this);
    }

    private void removeGauge(int type) {
        Gauge gauge = (Gauge) this.mGaugeMap.get(type);
        removeView(gauge);
        this.mGauges.remove(gauge);
        this.mGaugeMap.remove(type);
        this.mHasSensors = this.mGaugeMap.get(4096) != null;
        if (type == 4096 && !this.mHasSensors && this.mVisible) {
            this.mSensorManager.unregisterListener(this);
        }
    }

    public boolean onLongClick(View v) {
        Context context = getContext();
        PopupMenu popup = new PopupMenu(context, v);
        Menu menu = popup.getMenu();
        if (v instanceof Gauge) {
            Gauge gauge = (Gauge) v;
            menu.add(0, gauge.getType(), 0, context.getString(R.string.remove_gauge, new Object[]{getGaugeName(gauge.getType())}));
            gauge.getType();
        }
        Iterator it = getAvailableGauges(0).iterator();
        while (it.hasNext()) {
            menu.add(0, ((Integer) it.next()).intValue(), 0, context.getString(R.string.add_gauge, new Object[]{getGaugeName(((Integer) it.next()).intValue())}));
        }
        popup.setOnMenuItemClickListener(this);
        popup.show();
        return true;
    }

    public boolean onMenuItemClick(MenuItem item) {
        TransitionManager.beginDelayedTransition(this);
        int type = item.getItemId();
        if (this.mGaugeMap.indexOfKey(type) >= 0) {
            removeGauge(type);
        } else {
            addGauge(type);
        }
        if (this.mMapHolder != null) {
            this.mMapHolder.updateMapViewArea();
        }
        return true;
    }

    public void setValue(int type, float value) {
        Gauge gauge = (Gauge) this.mGaugeMap.get(type);
        if (gauge != null) {
            gauge.setValue(value);
        }
    }

    public void refreshGauges() {
        Iterator it = this.mGauges.iterator();
        while (it.hasNext()) {
            ((Gauge) it.next()).refresh();
        }
    }

    public void onVisibilityChanged(boolean visible) {
        if (visible != this.mVisible) {
            this.mVisible = visible;
            if (!this.mHasSensors) {
                return;
            }
            if (!this.mVisible) {
                this.mSensorManager.unregisterListener(this);
            } else if (this.mPressureSensor != null) {
                this.mSensorManager.registerListener(this, this.mPressureSensor, 3, 1000);
            }
        }
    }

    public boolean setNavigationMode(boolean mode) {
        int visibility = 0;
        if (this.mNavigationMode == mode) {
            return false;
        }
        this.mNavigationMode = mode;
        if (!mode) {
            visibility = 8;
        }
        TransitionManager.beginDelayedTransition(this);
        Iterator it = this.mGauges.iterator();
        while (it.hasNext()) {
            Gauge gauge = (Gauge) it.next();
            if (isNavigationGauge(gauge.getType())) {
                gauge.setVisibility(visibility);
            }
        }
        return true;
    }

    private boolean isNavigationGauge(int type) {
        return type > 39321;
    }

    @NonNull
    private ArrayList<Integer> getAvailableGauges(int type) {
        ArrayList<Integer> gauges = new ArrayList();
        gauges.add(Integer.valueOf(1));
        gauges.add(Integer.valueOf(2));
        gauges.add(Integer.valueOf(4));
        if (this.mPressureSensor != null) {
            gauges.add(Integer.valueOf(4096));
        }
        if (this.mNavigationMode) {
            gauges.add(Integer.valueOf(65536));
            gauges.add(Integer.valueOf(131072));
            gauges.add(Integer.valueOf(262144));
        }
        for (int i = 0; i < this.mGaugeMap.size(); i++) {
            gauges.remove(Integer.valueOf(this.mGaugeMap.keyAt(i)));
        }
        gauges.remove(Integer.valueOf(type));
        return gauges;
    }

    public String getGaugeSettings() {
        String[] gauges = new String[this.mGauges.size()];
        for (int i = 0; i < gauges.length; i++) {
            gauges[i] = String.valueOf(((Gauge) this.mGauges.get(i)).getType());
        }
        return TextUtils.join(",", gauges);
    }

    public boolean hasVisibleGauges() {
        Iterator it = this.mGauges.iterator();
        while (it.hasNext()) {
            if (((Gauge) it.next()).getVisibility() == 0) {
                return true;
            }
        }
        return false;
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != 6) {
            return;
        }
        if (event.accuracy == -1 || event.accuracy == 0) {
            setValue(4096, Float.NaN);
        } else {
            setValue(4096, (float) (((1.0d - Math.pow((double) (event.values[0] / 1013.25f), 0.190284d)) * 145366.45d) / 3.281d));
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() != 6) {
            return;
        }
        if (accuracy == -1 || accuracy == 0) {
            setValue(4096, Float.NaN);
        }
    }
}
