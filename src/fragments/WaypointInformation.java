package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import info.andreynovikov.androidcolorpicker.ColorPickerDialog;
import info.andreynovikov.androidcolorpicker.ColorPickerSwatch;
import info.andreynovikov.androidcolorpicker.ColorPickerSwatch.OnColorSelectedListener;
import java.util.ArrayList;
import mobi.maptrek.Configuration;
import mobi.maptrek.LocationChangeListener;
import mobi.maptrek.MapHolder;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.view.LimitedWebView;
import org.oscim.core.GeoPoint;
import org.oscim.theme.styles.LineStyle;

public class WaypointInformation extends Fragment implements OnBackPressedListener, LocationChangeListener {
    static final /* synthetic */ boolean $assertionsDisabled = (!WaypointInformation.class.desiredAssertionStatus());
    public static final String ARG_DETAILS = "details";
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    final int SWIPE_MAX_OFF_PATH = ((int) (LineStyle.REPEAT_START_DEFAULT * MapTrek.density));
    final int SWIPE_MIN_DISTANCE = ((int) (40.0f * MapTrek.density));
    final int SWIPE_THRESHOLD_VELOCITY = Callback.DEFAULT_DRAG_ANIMATION_DURATION;
    private boolean mEditorMode;
    private boolean mExpanded;
    private FloatingActionButton mFloatingButton;
    private FragmentHolder mFragmentHolder;
    private double mLatitude;
    private OnWaypointActionListener mListener;
    private double mLongitude;
    private MapHolder mMapHolder;
    private Waypoint mWaypoint;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_waypoint_information, container, false);
        ImageButton navigateButton = (ImageButton) rootView.findViewById(R.id.navigateButton);
        ImageButton shareButton = (ImageButton) rootView.findViewById(R.id.shareButton);
        ImageButton deleteButton = (ImageButton) rootView.findViewById(R.id.deleteButton);
        ((ImageButton) rootView.findViewById(R.id.editButton)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                WaypointInformation.this.setEditorMode(true);
            }
        });
        navigateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                WaypointInformation.this.onNavigate();
            }
        });
        shareButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                WaypointInformation.this.mFragmentHolder.disableActionButton();
                WaypointInformation.this.mFragmentHolder.popCurrent();
                WaypointInformation.this.mListener.onWaypointShare(WaypointInformation.this.mWaypoint);
            }
        });
        deleteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(WaypointInformation.this.getContext(), R.anim.shake));
            }
        });
        deleteButton.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                WaypointInformation.this.mFragmentHolder.disableActionButton();
                WaypointInformation.this.mFragmentHolder.popCurrent();
                WaypointInformation.this.mListener.onWaypointDelete(WaypointInformation.this.mWaypoint);
                return true;
            }
        });
        this.mExpanded = false;
        final GestureDetector gesture = new GestureDetector(getActivity(), new SimpleOnGestureListener() {
            public boolean onDown(MotionEvent e) {
                return true;
            }

            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(e1.getX() - e2.getX()) > ((float) WaypointInformation.this.SWIPE_MAX_OFF_PATH)) {
                    return false;
                }
                if (!WaypointInformation.this.mExpanded && e1.getY() - e2.getY() > ((float) WaypointInformation.this.SWIPE_MIN_DISTANCE) && Math.abs(velocityY) > LineStyle.REPEAT_GAP_DEFAULT) {
                    WaypointInformation.this.expand();
                    WaypointInformation.this.updateWaypointInformation(WaypointInformation.this.mLatitude, WaypointInformation.this.mLongitude);
                } else if (!WaypointInformation.this.mEditorMode && e2.getY() - e1.getY() > ((float) WaypointInformation.this.SWIPE_MIN_DISTANCE) && Math.abs(velocityY) > LineStyle.REPEAT_GAP_DEFAULT) {
                    WaypointInformation.this.mFragmentHolder.disableActionButton();
                    WaypointInformation.this.mFragmentHolder.popCurrent();
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });
        rootView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gesture.onTouchEvent(event);
            }
        });
        this.mEditorMode = false;
        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        double latitude = getArguments().getDouble("lat", Double.NaN);
        double longitude = getArguments().getDouble("lon", Double.NaN);
        boolean full = getArguments().getBoolean(ARG_DETAILS);
        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble("lat");
            longitude = savedInstanceState.getDouble("lon");
            full = savedInstanceState.getBoolean(ARG_DETAILS);
        }
        if (full) {
            expand();
        }
        this.mListener.onWaypointFocus(this.mWaypoint);
        updateWaypointInformation(latitude, longitude);
    }

    public void onResume() {
        super.onResume();
        this.mMapHolder.addLocationChangeListener(this);
    }

    public void onPause() {
        super.onPause();
        this.mMapHolder.removeLocationChangeListener(this);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mListener = (OnWaypointActionListener) context;
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
            throw new ClassCastException(context.toString() + " must implement OnWaypointActionListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mListener.onWaypointFocus(null);
        this.mFragmentHolder.removeBackClickListener(this);
        this.mFragmentHolder = null;
        this.mListener = null;
        this.mMapHolder = null;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("lat", this.mLatitude);
        outState.putDouble("lon", this.mLongitude);
        outState.putBoolean(ARG_DETAILS, this.mExpanded);
    }

    private void expand() {
        final ViewGroup rootView = (ViewGroup) getView();
        if ($assertionsDisabled || rootView != null) {
            rootView.findViewById(R.id.extendTable).setVisibility(0);
            rootView.findViewById(R.id.dottedLine).setVisibility(4);
            rootView.findViewById(R.id.source).setVisibility(8);
            TextView destination = (TextView) rootView.findViewById(R.id.destination);
            destination.setTextAppearance(16973894);
            destination.setTextColor(getContext().getColor(R.color.colorAccent));
            LayoutParams params = (LayoutParams) destination.getLayoutParams();
            params.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.fragment_padding);
            params.bottomMargin = -params.topMargin;
            destination.setLayoutParams(params);
            rootView.findViewById(R.id.navigateButton).setVisibility(8);
            rootView.findViewById(R.id.editButton).setVisibility(0);
            this.mFloatingButton = this.mFragmentHolder.enableActionButton();
            setFloatingPointDrawable();
            this.mFloatingButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!WaypointInformation.this.isVisible()) {
                        return;
                    }
                    if (WaypointInformation.this.mEditorMode) {
                        WaypointInformation.this.mWaypoint.name = ((EditText) rootView.findViewById(R.id.nameEdit)).getText().toString();
                        WaypointInformation.this.mWaypoint.description = ((EditText) rootView.findViewById(R.id.descriptionEdit)).getText().toString();
                        WaypointInformation.this.mWaypoint.style.color = ((ColorPickerSwatch) rootView.findViewById(R.id.colorSwatch)).getColor();
                        WaypointInformation.this.mListener.onWaypointSave(WaypointInformation.this.mWaypoint);
                        WaypointInformation.this.mListener.onWaypointFocus(WaypointInformation.this.mWaypoint);
                        WaypointInformation.this.setEditorMode(false);
                        return;
                    }
                    WaypointInformation.this.mFragmentHolder.disableActionButton();
                    WaypointInformation.this.onNavigate();
                }
            });
            this.mMapHolder.updateMapViewArea();
            this.mExpanded = true;
            return;
        }
        throw new AssertionError();
    }

    private void onNavigate() {
        if (this.mMapHolder.isNavigatingTo(this.mWaypoint.coordinates)) {
            this.mMapHolder.stopNavigation();
        } else {
            this.mListener.onWaypointNavigate(this.mWaypoint);
        }
        this.mFragmentHolder.popAll();
    }

    public void setWaypoint(Waypoint waypoint) {
        this.mWaypoint = waypoint;
        if (isVisible()) {
            this.mListener.onWaypointFocus(this.mWaypoint);
            updateWaypointInformation(this.mLatitude, this.mLongitude);
        }
    }

    private void updateWaypointInformation(double latitude, double longitude) {
        Activity activity = getActivity();
        View rootView = getView();
        if ($assertionsDisabled || rootView != null) {
            TextView nameView = (TextView) rootView.findViewById(R.id.name);
            if (nameView != null) {
                nameView.setText(this.mWaypoint.name);
            }
            TextView sourceView = (TextView) rootView.findViewById(R.id.source);
            if (sourceView != null) {
                sourceView.setText(this.mWaypoint.source.name);
            }
            sourceView = (TextView) rootView.findViewById(R.id.sourceExtended);
            if (sourceView != null) {
                sourceView.setText(this.mWaypoint.source.name);
            }
            TextView destinationView = (TextView) rootView.findViewById(R.id.destination);
            if (destinationView != null) {
                if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                    destinationView.setVisibility(8);
                } else {
                    GeoPoint geoPoint = new GeoPoint(latitude, longitude);
                    String distance = StringFormatter.distanceH(geoPoint.vincentyDistance(this.mWaypoint.coordinates)) + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + StringFormatter.angleH(geoPoint.bearingTo(this.mWaypoint.coordinates));
                    destinationView.setVisibility(0);
                    destinationView.setText(distance);
                }
            }
            final TextView coordsView = (TextView) rootView.findViewById(R.id.coordinates);
            if (coordsView != null) {
                coordsView.setText(StringFormatter.coordinates(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, this.mWaypoint.coordinates.getLatitude(), this.mWaypoint.coordinates.getLongitude()));
                Drawable drawable = activity.getDrawable(this.mWaypoint.locked ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open);
                if (drawable != null) {
                    int drawableSize = (int) Math.round(((double) coordsView.getLineHeight()) * 0.7d);
                    int drawablePadding = (int) (MapTrek.density * 1.5f);
                    drawable.setBounds(0, drawablePadding, drawableSize, drawableSize + drawablePadding);
                    drawable.setTint(activity.getColor(this.mWaypoint.locked ? R.color.red : R.color.colorPrimaryDark));
                    coordsView.setCompoundDrawables(null, null, drawable, null);
                }
                coordsView.setOnTouchListener(new OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        boolean z = false;
                        if ((event.getAction() & 255) != 1 || event.getX() < ((float) (coordsView.getRight() - coordsView.getTotalPaddingRight()))) {
                            return false;
                        }
                        Waypoint access$300 = WaypointInformation.this.mWaypoint;
                        if (!WaypointInformation.this.mWaypoint.locked) {
                            z = true;
                        }
                        access$300.locked = z;
                        WaypointInformation.this.mListener.onWaypointSave(WaypointInformation.this.mWaypoint);
                        WaypointInformation.this.mListener.onWaypointFocus(WaypointInformation.this.mWaypoint);
                        WaypointInformation.this.updateWaypointInformation(WaypointInformation.this.mLatitude, WaypointInformation.this.mLongitude);
                        return true;
                    }
                });
                coordsView.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        StringFormatter.coordinateFormat++;
                        if (StringFormatter.coordinateFormat == 5) {
                            StringFormatter.coordinateFormat = 0;
                        }
                        coordsView.setText(StringFormatter.coordinates(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, WaypointInformation.this.mWaypoint.coordinates.getLatitude(), WaypointInformation.this.mWaypoint.coordinates.getLongitude()));
                        Configuration.setCoordinatesFormat(StringFormatter.coordinateFormat);
                    }
                });
                if (HelperUtils.needsTargetedAdvice(1024) || HelperUtils.needsTargetedAdvice(2048)) {
                    final View view = rootView;
                    rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                        public void onGlobalLayout() {
                            view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            Rect r = new Rect();
                            coordsView.getGlobalVisibleRect(r);
                            if (!HelperUtils.showTargetedAdvice(WaypointInformation.this.getActivity(), 1024, R.string.advice_switch_coordinates_format, r)) {
                                if (coordsView.getLayoutDirection() == 0) {
                                    r.left = r.right - coordsView.getTotalPaddingRight();
                                } else {
                                    r.right = r.left + coordsView.getTotalPaddingLeft();
                                }
                                HelperUtils.showTargetedAdvice(WaypointInformation.this.getActivity(), 2048, R.string.advice_locked_coordinates, r);
                            }
                        }
                    });
                }
            }
            TextView altitudeView = (TextView) rootView.findViewById(R.id.altitude);
            if (altitudeView != null) {
                if (this.mWaypoint.altitude != Integer.MIN_VALUE) {
                    altitudeView.setText(getString(R.string.waypoint_altitude, new Object[]{StringFormatter.elevationH((float) this.mWaypoint.altitude)}));
                    altitudeView.setVisibility(0);
                } else {
                    altitudeView.setVisibility(8);
                }
            }
            TextView proximityView = (TextView) rootView.findViewById(R.id.proximity);
            if (proximityView != null) {
                if (this.mWaypoint.proximity > 0) {
                    proximityView.setText(getString(R.string.waypoint_proximity, new Object[]{StringFormatter.distanceH((double) this.mWaypoint.proximity)}));
                    proximityView.setVisibility(0);
                } else {
                    proximityView.setVisibility(8);
                }
            }
            TextView dateView = (TextView) rootView.findViewById(R.id.date);
            if (dateView != null) {
                if (this.mWaypoint.date != null) {
                    String date = DateFormat.getDateFormat(activity).format(this.mWaypoint.date);
                    String time = DateFormat.getTimeFormat(activity).format(this.mWaypoint.date);
                    dateView.setText(getString(R.string.datetime, new Object[]{date, time}));
                    rootView.findViewById(R.id.dateRow).setVisibility(0);
                } else {
                    rootView.findViewById(R.id.dateRow).setVisibility(8);
                }
            }
            ViewGroup row = (ViewGroup) rootView.findViewById(R.id.descriptionRow);
            if (row != null) {
                if (this.mWaypoint.description == null || "".equals(this.mWaypoint.description)) {
                    row.setVisibility(8);
                } else {
                    setDescription(rootView);
                    row.setVisibility(0);
                }
            }
            if (this.mMapHolder.isNavigatingTo(this.mWaypoint.coordinates)) {
                ((ImageButton) rootView.findViewById(R.id.navigateButton)).setImageResource(R.drawable.ic_navigation_off);
            }
            this.mLatitude = latitude;
            this.mLongitude = longitude;
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
                this.mFloatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_done));
                ((EditText) rootView.findViewById(R.id.nameEdit)).setText(this.mWaypoint.name);
                ((EditText) rootView.findViewById(R.id.descriptionEdit)).setText(this.mWaypoint.description);
                colorSwatch.setColor(this.mWaypoint.style.color);
                colorSwatch.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        ArrayList<Integer> colorList = new ArrayList(7);
                        ColorPickerDialog dialog = new ColorPickerDialog();
                        dialog.setColors(MarkerStyle.DEFAULT_COLORS, colorSwatch.getColor());
                        dialog.setArguments(R.string.color_picker_default_title, 4, 2);
                        dialog.setOnColorSelectedListener(new OnColorSelectedListener() {
                            public void onColorSelected(int color) {
                                colorSwatch.setColor(color);
                            }
                        });
                        dialog.show(WaypointInformation.this.getFragmentManager(), "ColorPickerDialog");
                    }
                });
                viewsState = 8;
                editsState = 0;
                if (this.mWaypoint.source instanceof FileDataSource) {
                    HelperUtils.showTargetedAdvice(getActivity(), 1, (int) R.string.advice_update_external_source, this.mFloatingButton, false);
                }
            } else {
                setFloatingPointDrawable();
                ((TextView) rootView.findViewById(R.id.name)).setText(this.mWaypoint.name);
                setDescription(rootView);
                viewsState = 0;
                editsState = 8;
                ((InputMethodManager) getActivity().getSystemService("input_method")).hideSoftInputFromWindow(rootView.getWindowToken(), 0);
            }
            TransitionManager.beginDelayedTransition(rootView, new Fade());
            rootView.findViewById(R.id.name).setVisibility(viewsState);
            rootView.findViewById(R.id.nameWrapper).setVisibility(editsState);
            if (enabled || !(this.mWaypoint.description == null || "".equals(this.mWaypoint.description))) {
                rootView.findViewById(R.id.descriptionRow).setVisibility(0);
            } else {
                rootView.findViewById(R.id.descriptionRow).setVisibility(8);
            }
            rootView.findViewById(R.id.description).setVisibility(viewsState);
            rootView.findViewById(R.id.descriptionWrapper).setVisibility(editsState);
            colorSwatch.setVisibility(editsState);
            if (!(Double.isNaN(this.mLatitude) || Double.isNaN(this.mLongitude))) {
                rootView.findViewById(R.id.destination).setVisibility(viewsState);
            }
            if (this.mWaypoint.date != null) {
                rootView.findViewById(R.id.dateRow).setVisibility(viewsState);
            }
            rootView.findViewById(R.id.editButton).setVisibility(viewsState);
            rootView.findViewById(R.id.shareButton).setVisibility(viewsState);
            this.mEditorMode = enabled;
            return;
        }
        throw new AssertionError();
    }

    private void setFloatingPointDrawable() {
        if (this.mMapHolder.isNavigatingTo(this.mWaypoint.coordinates)) {
            this.mFloatingButton.setImageResource(R.drawable.ic_navigation_off);
        } else {
            this.mFloatingButton.setImageResource(R.drawable.ic_navigate);
        }
    }

    private void setDescription(View rootView) {
        View description = rootView.findViewById(R.id.description);
        if (description instanceof LimitedWebView) {
            setWebViewText((LimitedWebView) description);
        } else if (this.mWaypoint.description != null && this.mWaypoint.description.contains("<") && this.mWaypoint.description.contains(">")) {
            ViewGroup parent = (ViewGroup) description.getParent();
            int index = parent.indexOfChild(description);
            parent.removeView(description);
            LimitedWebView webView = new LimitedWebView(getContext());
            webView.setId(R.id.description);
            webView.setMaxHeight((int) TypedValue.applyDimension(1, LineStyle.REPEAT_GAP_DEFAULT, getResources().getDisplayMetrics()));
            webView.setLayoutParams(new FrameLayout.LayoutParams(-1, -2));
            parent.addView(webView, index);
            setWebViewText(webView);
        } else {
            ((TextView) description).setText(this.mWaypoint.description);
            ((TextView) description).setMovementMethod(new ScrollingMovementMethod());
        }
    }

    private void setWebViewText(LimitedWebView webView) {
        String descriptionHtml = "<style type=\"text/css\">html,body{margin:0}</style>\n" + this.mWaypoint.description;
        webView.setBackgroundColor(0);
        webView.setLayerType(1, null);
        WebSettings settings = webView.getSettings();
        settings.setDefaultTextEncodingName("utf-8");
        settings.setAllowFileAccess(true);
        LimitedWebView limitedWebView = webView;
        limitedWebView.loadDataWithBaseURL(Uri.fromFile(getContext().getExternalFilesDir("data")).toString() + "/", descriptionHtml, "text/html", "utf-8", null);
    }

    public boolean onBackClick() {
        if (this.mEditorMode) {
            setEditorMode(false);
            return true;
        }
        this.mFragmentHolder.disableActionButton();
        return false;
    }

    public void onLocationChanged(Location location) {
        if (!this.mEditorMode) {
            updateWaypointInformation(location.getLatitude(), location.getLongitude());
        }
    }
}
