package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.widget.AutoScrollHelper;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.XAxis.XAxisPosition;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import info.andreynovikov.androidcolorpicker.ColorPickerDialog;
import info.andreynovikov.androidcolorpicker.ColorPickerSwatch;
import info.andreynovikov.androidcolorpicker.ColorPickerSwatch.OnColorSelectedListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Track.TrackPoint;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.MeanValue;
import mobi.maptrek.util.StringFormatter;
import org.oscim.core.GeoPoint;
import org.slf4j.Marker;

public class TrackInformation extends Fragment implements OnMenuItemClickListener, OnBackPressedListener {
    static final /* synthetic */ boolean $assertionsDisabled = (!TrackInformation.class.desiredAssertionStatus());
    private boolean mEditorMode;
    private FragmentHolder mFragmentHolder;
    private boolean mIsCurrent;
    private OnTrackActionListener mListener;
    private MapHolder mMapHolder;
    private ImageButton mMoreButton;
    private Track mTrack;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_track_information, container, false);
        this.mMoreButton = (ImageButton) rootView.findViewById(R.id.moreButton);
        this.mMoreButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                boolean z = true;
                if (TrackInformation.this.mEditorMode) {
                    TrackInformation.this.mTrack.name = ((EditText) rootView.findViewById(R.id.nameEdit)).getText().toString();
                    TrackInformation.this.mTrack.style.color = ((ColorPickerSwatch) rootView.findViewById(R.id.colorSwatch)).getColor();
                    TrackInformation.this.mListener.onTrackSave(TrackInformation.this.mTrack);
                    TrackInformation.this.setEditorMode(false);
                    return;
                }
                boolean z2;
                PopupMenu popup = new PopupMenu(TrackInformation.this.getContext(), TrackInformation.this.mMoreButton);
                TrackInformation.this.mMoreButton.setOnTouchListener(popup.getDragToOpenListener());
                popup.inflate(R.menu.context_menu_track);
                Menu menu = popup.getMenu();
                MenuItem findItem = menu.findItem(R.id.action_edit);
                if (TrackInformation.this.mIsCurrent) {
                    z2 = false;
                } else {
                    z2 = true;
                }
                findItem.setVisible(z2);
                MenuItem findItem2 = menu.findItem(R.id.action_delete);
                if (TrackInformation.this.mTrack.source == null || TrackInformation.this.mTrack.source.isNativeTrack()) {
                    z = false;
                }
                findItem2.setVisible(z);
                popup.setOnMenuItemClickListener(TrackInformation.this);
                popup.show();
            }
        });
        if (this.mIsCurrent) {
            ImageButton stopButton = (ImageButton) rootView.findViewById(R.id.stopButton);
            stopButton.setVisibility(0);
            stopButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    TrackInformation.this.mMapHolder.disableTracking();
                    TrackInformation.this.mFragmentHolder.popCurrent();
                }
            });
        }
        this.mEditorMode = false;
        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateTrackInformation();
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mListener = (OnTrackActionListener) context;
            try {
                this.mMapHolder = (MapHolder) context;
                try {
                    this.mFragmentHolder = (FragmentHolder) context;
                    this.mFragmentHolder.addBackClickListener(this);
                } catch (ClassCastException e) {
                    throw new ClassCastException(context.toString() + " must implement FragmentHolder");
                }
            } catch (ClassCastException e2) {
                throw new ClassCastException(context.toString() + " must implement MapHolder");
            }
        } catch (ClassCastException e3) {
            throw new ClassCastException(context.toString() + " must implement OnTrackActionListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mFragmentHolder.removeBackClickListener(this);
        this.mFragmentHolder = null;
        this.mMapHolder = null;
        this.mListener = null;
    }

    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                this.mListener.onTrackDelete(this.mTrack);
                this.mFragmentHolder.popCurrent();
                return true;
            case R.id.action_edit:
                setEditorMode(true);
                return true;
            case R.id.action_share:
                this.mListener.onTrackShare(this.mTrack);
                return true;
            case R.id.action_view:
                this.mListener.onTrackView(this.mTrack);
                this.mFragmentHolder.popAll();
                return true;
            default:
                return false;
        }
    }

    public void setTrack(Track track, boolean current) {
        this.mTrack = track;
        this.mIsCurrent = current;
        if (isVisible()) {
            updateTrackInformation();
        }
    }

    private void updateTrackInformation() {
        Activity activity = getActivity();
        Resources resources = getResources();
        View rootView = getView();
        if ($assertionsDisabled || rootView != null) {
            LineDataSet lineDataSet;
            XAxis xAxis;
            ((TextView) rootView.findViewById(R.id.name)).setText(this.mTrack.name);
            View sourceRow = rootView.findViewById(R.id.sourceRow);
            if (this.mTrack.source == null || this.mTrack.source.isNativeTrack()) {
                sourceRow.setVisibility(8);
            } else {
                ((TextView) rootView.findViewById(R.id.source)).setText(this.mTrack.source.name);
                sourceRow.setVisibility(0);
            }
            ((TextView) rootView.findViewById(R.id.pointCount)).setText(resources.getQuantityString(R.plurals.numberOfPoints, this.mTrack.points.size(), new Object[]{Integer.valueOf(this.mTrack.points.size())}));
            ((TextView) rootView.findViewById(R.id.distance)).setText(StringFormatter.distanceHP((double) this.mTrack.getDistance()));
            TrackPoint ftp = (TrackPoint) this.mTrack.points.get(0);
            TrackPoint ltp = this.mTrack.getLastPoint();
            boolean hasTime = ftp.time > 0 && ltp.time > 0;
            ((TextView) rootView.findViewById(R.id.startCoordinates)).setText(StringFormatter.coordinates(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, ((double) ftp.latitudeE6) / 1000000.0d, ((double) ftp.longitudeE6) / 1000000.0d));
            ((TextView) rootView.findViewById(R.id.finishCoordinates)).setText(StringFormatter.coordinates(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, ((double) ltp.latitudeE6) / 1000000.0d, ((double) ltp.longitudeE6) / 1000000.0d));
            View startDateRow = rootView.findViewById(R.id.startDateRow);
            View finishDateRow = rootView.findViewById(R.id.finishDateRow);
            View timeRow = rootView.findViewById(R.id.timeRow);
            if (hasTime) {
                String timeSpan;
                Date date = new Date(ftp.time);
                date = new Date(ltp.time);
                ((TextView) rootView.findViewById(R.id.startDate)).setText(String.format("%s %s", new Object[]{DateFormat.getDateFormat(activity).format(date), DateFormat.getTimeFormat(activity).format(date)}));
                ((TextView) rootView.findViewById(R.id.finishDate)).setText(String.format("%s %s", new Object[]{DateFormat.getDateFormat(activity).format(date), DateFormat.getTimeFormat(activity).format(date)}));
                startDateRow.setVisibility(0);
                finishDateRow.setVisibility(0);
                long elapsed = (ltp.time - ftp.time) / 1000;
                if (elapsed < 259200) {
                    timeSpan = DateUtils.formatElapsedTime(elapsed);
                } else {
                    timeSpan = DateUtils.formatDateRange(activity, ftp.time, ltp.time, 65536);
                }
                ((TextView) rootView.findViewById(R.id.timeSpan)).setText(timeSpan);
                timeRow.setVisibility(0);
            } else {
                startDateRow.setVisibility(8);
                finishDateRow.setVisibility(8);
                timeRow.setVisibility(8);
            }
            int segmentCount = 0;
            float minElevation = AutoScrollHelper.NO_MAX;
            float maxElevation = Float.MIN_VALUE;
            float maxSpeed = 0.0f;
            MeanValue mv = new MeanValue();
            boolean hasElevation = false;
            boolean hasSpeed = false;
            ArrayList<Entry> elevationValues = new ArrayList();
            ArrayList<Entry> speedValues = new ArrayList();
            ArrayList<String> xValues = new ArrayList();
            GeoPoint ptp = null;
            long startTime = ftp.time;
            int i = 0;
            for (GeoPoint point : this.mTrack.points) {
                if (!point.continuous) {
                    segmentCount++;
                }
                xValues.add(Marker.ANY_NON_NULL_MARKER + DateUtils.formatElapsedTime((long) (((int) (point.time - startTime)) / 1000)));
                if (!Float.isNaN(point.elevation)) {
                    elevationValues.add(new Entry(point.elevation, i));
                    if (point.elevation < minElevation && point.elevation != 0.0f) {
                        minElevation = point.elevation;
                    }
                    if (point.elevation > maxElevation) {
                        maxElevation = point.elevation;
                    }
                    if (point.elevation != 0.0f) {
                        hasElevation = true;
                    }
                }
                float speed = Float.NaN;
                if (!Float.isNaN(point.speed)) {
                    speed = point.speed;
                } else if (hasTime) {
                    if (ptp != null) {
                        speed = ((float) point.vincentyDistance(ptp)) / ((float) ((point.time - ptp.time) / 1000));
                    } else {
                        speed = 0.0f;
                    }
                }
                if (!Float.isNaN(speed)) {
                    speedValues.add(new Entry(StringFormatter.speedFactor * speed, i));
                    mv.addValue(speed);
                    if (speed > maxSpeed) {
                        maxSpeed = speed;
                    }
                    hasSpeed = true;
                }
                ptp = point;
                i++;
            }
            ((TextView) rootView.findViewById(R.id.segmentCount)).setText(resources.getQuantityString(R.plurals.numberOfSegments, segmentCount, new Object[]{Integer.valueOf(segmentCount)}));
            View statisticsHeader = rootView.findViewById(R.id.statisticsHeader);
            if (hasElevation || hasSpeed) {
                statisticsHeader.setVisibility(0);
            } else {
                statisticsHeader.setVisibility(8);
            }
            View elevationUpRow = rootView.findViewById(R.id.elevationUpRow);
            View elevationDownRow = rootView.findViewById(R.id.elevationDownRow);
            if (hasElevation) {
                ((TextView) rootView.findViewById(R.id.maxElevation)).setText(StringFormatter.elevationH(maxElevation));
                ((TextView) rootView.findViewById(R.id.minElevation)).setText(StringFormatter.elevationH(minElevation));
                elevationUpRow.setVisibility(0);
                elevationDownRow.setVisibility(0);
            } else {
                elevationUpRow.setVisibility(8);
                elevationDownRow.setVisibility(8);
            }
            View speedRow = rootView.findViewById(R.id.speedRow);
            if (hasSpeed) {
                float averageSpeed = mv.getMeanValue();
                ((TextView) rootView.findViewById(R.id.maxSpeed)).setText(String.format(Locale.getDefault(), "%s: %s", new Object[]{resources.getString(R.string.max_speed), StringFormatter.speedH(maxSpeed)}));
                ((TextView) rootView.findViewById(R.id.averageSpeed)).setText(String.format(Locale.getDefault(), "%s: %s", new Object[]{resources.getString(R.string.average_speed), StringFormatter.speedH(averageSpeed)}));
                speedRow.setVisibility(0);
            } else {
                speedRow.setVisibility(8);
            }
            View elevationHeader = rootView.findViewById(R.id.elevationHeader);
            LineChart elevationChart = (LineChart) rootView.findViewById(R.id.elevationChart);
            if (hasElevation) {
                lineDataSet = new LineDataSet(elevationValues, "Elevation");
                lineDataSet.setAxisDependency(AxisDependency.LEFT);
                lineDataSet.setDrawFilled(true);
                lineDataSet.setDrawCircles(false);
                lineDataSet.setColor(resources.getColor(R.color.colorAccentLight, activity.getTheme()));
                lineDataSet.setFillColor(lineDataSet.getColor());
                ArrayList<ILineDataSet> elevationDataSets = new ArrayList();
                elevationDataSets.add(lineDataSet);
                elevationChart.setData(new LineData((List) xValues, (List) elevationDataSets));
                elevationChart.getLegend().setEnabled(false);
                elevationChart.setDescription("");
                xAxis = elevationChart.getXAxis();
                xAxis.setDrawGridLines(false);
                xAxis.setDrawAxisLine(true);
                xAxis.setPosition(XAxisPosition.BOTTOM);
                elevationHeader.setVisibility(0);
                elevationChart.setVisibility(0);
                elevationChart.invalidate();
            } else {
                elevationHeader.setVisibility(8);
                elevationChart.setVisibility(8);
            }
            View speedHeader = rootView.findViewById(R.id.speedHeader);
            LineChart speedChart = (LineChart) rootView.findViewById(R.id.speedChart);
            if (hasSpeed) {
                lineDataSet = new LineDataSet(speedValues, "Speed");
                lineDataSet.setAxisDependency(AxisDependency.LEFT);
                lineDataSet.setDrawCircles(false);
                lineDataSet.setColor(resources.getColor(R.color.colorAccentLight, activity.getTheme()));
                ArrayList<ILineDataSet> speedDataSets = new ArrayList();
                speedDataSets.add(lineDataSet);
                speedChart.setData(new LineData((List) xValues, (List) speedDataSets));
                speedChart.getLegend().setEnabled(false);
                speedChart.setDescription("");
                xAxis = speedChart.getXAxis();
                xAxis.setDrawGridLines(false);
                xAxis.setDrawAxisLine(true);
                xAxis.setPosition(XAxisPosition.BOTTOM);
                speedHeader.setVisibility(0);
                speedChart.setVisibility(0);
                speedChart.invalidate();
                return;
            }
            speedHeader.setVisibility(8);
            speedChart.setVisibility(8);
            return;
        }
        throw new AssertionError();
    }

    private void setEditorMode(boolean enabled) {
        ViewGroup rootView = (ViewGroup) getView();
        if ($assertionsDisabled || rootView != null) {
            int viewsState;
            int editsState;
            final ColorPickerSwatch colorSwatch = (ColorPickerSwatch) rootView.findViewById(R.id.colorSwatch);
            if (enabled) {
                this.mMoreButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_done));
                ((EditText) rootView.findViewById(R.id.nameEdit)).setText(this.mTrack.name);
                colorSwatch.setColor(this.mTrack.style.color);
                colorSwatch.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        ColorPickerDialog dialog = new ColorPickerDialog();
                        dialog.setColors(MarkerStyle.DEFAULT_COLORS, TrackInformation.this.mTrack.style.color);
                        dialog.setArguments(R.string.color_picker_default_title, 4, 2);
                        dialog.setOnColorSelectedListener(new OnColorSelectedListener() {
                            public void onColorSelected(int color) {
                                colorSwatch.setColor(color);
                            }
                        });
                        dialog.show(TrackInformation.this.getFragmentManager(), "ColorPickerDialog");
                    }
                });
                viewsState = 8;
                editsState = 0;
                if (!this.mTrack.source.isNativeTrack()) {
                    HelperUtils.showTargetedAdvice(getActivity(), 1, (int) R.string.advice_update_external_source, this.mMoreButton, false);
                }
            } else {
                this.mMoreButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_more_vert));
                ((TextView) rootView.findViewById(R.id.name)).setText(this.mTrack.name);
                viewsState = 0;
                editsState = 8;
                ((InputMethodManager) getActivity().getSystemService("input_method")).hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
            TransitionManager.beginDelayedTransition(rootView, new Fade());
            rootView.findViewById(R.id.name).setVisibility(viewsState);
            rootView.findViewById(R.id.nameWrapper).setVisibility(editsState);
            colorSwatch.setVisibility(editsState);
            this.mEditorMode = enabled;
            return;
        }
        throw new AssertionError();
    }

    public boolean onBackClick() {
        if (!this.mEditorMode) {
            return false;
        }
        setEditorMode(false);
        return true;
    }
}
