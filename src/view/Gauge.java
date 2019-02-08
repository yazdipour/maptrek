package mobi.maptrek.view;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.Locale;
import mobi.maptrek.R;
import mobi.maptrek.util.StringFormatter;

public class Gauge extends RelativeLayout {
    public static final int TYPE_ALTITUDE = 4;
    public static final int TYPE_BEARING = 131072;
    public static final int TYPE_DISTANCE = 65536;
    public static final int TYPE_ELEVATION = 4096;
    public static final int TYPE_ETE = 2097152;
    public static final int TYPE_SPEED = 1;
    public static final int TYPE_TRACK = 2;
    public static final int TYPE_TURN = 262144;
    public static final int TYPE_VMG = 524288;
    public static final int TYPE_XTK = 1048576;
    private int mType;
    private TextView mUnitView;
    private float mValue;
    private TextView mValueView;

    public Gauge(Context context) {
        super(context);
    }

    public Gauge(Context context, int type) {
        super(context);
        this.mType = type;
        inflate(getContext(), R.layout.gauge, this);
        this.mValueView = (TextView) findViewById(R.id.gaugeValue);
        this.mUnitView = (TextView) findViewById(R.id.gaugeUnit);
        this.mUnitView.setText(getDefaultGaugeUnit(type));
    }

    public int getType() {
        return this.mType;
    }

    public void setValue(float value) {
        String indication;
        this.mValue = value;
        String unit = null;
        switch (this.mType) {
            case 1:
            case 524288:
                indication = StringFormatter.speedC(value);
                break;
            case 2:
            case 131072:
            case 262144:
                indication = StringFormatter.angleC((double) value);
                break;
            case 4:
            case 4096:
                indication = StringFormatter.elevationC(value);
                break;
            case 65536:
            case 1048576:
                String[] indications = StringFormatter.distanceC((double) value);
                indication = indications[0];
                unit = indications[1];
                break;
            default:
                indication = String.format(Locale.getDefault(), StringFormatter.precisionFormat, new Object[]{Float.valueOf(value)});
                break;
        }
        this.mValueView.setText(indication);
        if (unit != null) {
            this.mUnitView.setText(unit);
        }
    }

    public void refresh() {
        this.mUnitView.setText(getDefaultGaugeUnit(this.mType));
        setValue(this.mValue);
    }

    private String getDefaultGaugeUnit(int type) {
        switch (type) {
            case 1:
            case 524288:
                return StringFormatter.speedAbbr;
            case 2:
            case 131072:
            case 262144:
                return StringFormatter.angleAbbr;
            case 4:
            case 4096:
                return StringFormatter.elevationAbbr;
            case 65536:
            case 1048576:
                return StringFormatter.distanceAbbr;
            case 2097152:
                return StringFormatter.minuteAbbr;
            default:
                return "";
        }
    }
}
