package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.os.Bundle;
import android.text.Editable;
import android.text.style.ForegroundColorSpan;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageButton;
import android.widget.TextView;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import java.util.Locale;
import mobi.maptrek.LocationState;
import mobi.maptrek.LocationStateChangeListener;
import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.ui.TextInputDialogFragment;
import mobi.maptrek.ui.TextInputDialogFragment.Builder;
import mobi.maptrek.ui.TextInputDialogFragment.TextInputDialogCallback;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.JosmCoordinatesParser;
import mobi.maptrek.util.JosmCoordinatesParser.Result;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.util.SunriseSunset;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.map.Map;
import org.oscim.map.Map.UpdateListener;

public class LocationInformation extends Fragment implements UpdateListener, TextInputDialogCallback, LocationStateChangeListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_ZOOM = "zoom";
    private int mColorDarkBlue;
    private int mColorRed;
    private int mColorTextPrimary;
    TextView mCoordinateDegMin;
    TextView mCoordinateDegMinSec;
    TextView mCoordinateDegree;
    TextView mCoordinateMgrs;
    TextView mCoordinateUtmUps;
    TextView mDeclination;
    private FragmentHolder mFragmentHolder;
    private double mLatitude;
    private double mLongitude;
    private MapHolder mMapHolder;
    TextView mOffset;
    private ViewGroup mRootView;
    TextView mSunrise;
    private SunriseSunset mSunriseSunset;
    TextView mSunriseTitle;
    TextView mSunset;
    TextView mSunsetTitle;
    private ImageButton mSwitchOffButton;
    private TextInputDialogFragment mTextInputDialog;
    private int mZoom;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_location_information, container, false);
        this.mSwitchOffButton = (ImageButton) this.mRootView.findViewById(R.id.switchOffButton);
        this.mSwitchOffButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                LocationInformation.this.mMapHolder.disableLocations();
            }
        });
        ((ImageButton) this.mRootView.findViewById(R.id.shareButton)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                LocationInformation.this.mMapHolder.shareLocation(new GeoPoint(LocationInformation.this.mLatitude, LocationInformation.this.mLongitude), null);
            }
        });
        ((ImageButton) this.mRootView.findViewById(R.id.inputButton)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                LocationInformation.this.mTextInputDialog = new Builder().setCallbacks(LocationInformation.this).setHint(LocationInformation.this.getContext().getString(R.string.coordinates)).setShowPasteButton(true).create();
                LocationInformation.this.mTextInputDialog.show(LocationInformation.this.getFragmentManager(), "coordinatesInput");
            }
        });
        this.mCoordinateDegree = (TextView) this.mRootView.findViewById(R.id.coordinate_degree);
        this.mCoordinateDegMin = (TextView) this.mRootView.findViewById(R.id.coordinate_degmin);
        this.mCoordinateDegMinSec = (TextView) this.mRootView.findViewById(R.id.coordinate_degminsec);
        this.mCoordinateUtmUps = (TextView) this.mRootView.findViewById(R.id.coordinate_utmups);
        this.mCoordinateMgrs = (TextView) this.mRootView.findViewById(R.id.coordinate_mgrs);
        this.mSunriseTitle = (TextView) this.mRootView.findViewById(R.id.sunriseTitle);
        this.mSunsetTitle = (TextView) this.mRootView.findViewById(R.id.sunsetTitle);
        this.mSunrise = (TextView) this.mRootView.findViewById(R.id.sunrise);
        this.mSunset = (TextView) this.mRootView.findViewById(R.id.sunset);
        this.mOffset = (TextView) this.mRootView.findViewById(R.id.offset);
        this.mDeclination = (TextView) this.mRootView.findViewById(R.id.declination);
        this.mRootView.findViewById(R.id.extendTable).setVisibility(0);
        if (HelperUtils.needsTargetedAdvice(2)) {
            this.mRootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    LocationInformation.this.mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    HelperUtils.showTargetedAdvice(LocationInformation.this.getActivity(), 2, (int) R.string.advice_sunrise_sunset, LocationInformation.this.mSunrise.getVisibility() == 0 ? LocationInformation.this.mSunrise : LocationInformation.this.mSunset, true);
                }
            });
        }
        return this.mRootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        double latitude = getArguments().getDouble("lat");
        double longitude = getArguments().getDouble("lon");
        int zoom = getArguments().getInt("zoom");
        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble("lat");
            longitude = savedInstanceState.getDouble("lon");
            zoom = savedInstanceState.getInt("zoom");
        }
        this.mSunriseSunset = new SunriseSunset();
        updateLocation(latitude, longitude, zoom);
    }

    public void onResume() {
        super.onResume();
        this.mMapHolder.getMap().events.bind(this);
        this.mMapHolder.addLocationStateChangeListener(this);
        TextInputDialogFragment coordinatesInput = (TextInputDialogFragment) getFragmentManager().findFragmentByTag("coordinatesInput");
        if (coordinatesInput != null) {
            coordinatesInput.setCallback(this);
        }
    }

    public void onPause() {
        super.onPause();
        this.mMapHolder.getMap().events.unbind(this);
        this.mMapHolder.removeLocationStateChangeListener(this);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.mColorTextPrimary = context.getColor(R.color.textColorPrimary);
        this.mColorDarkBlue = context.getColor(R.color.darkBlue);
        this.mColorRed = context.getColor(R.color.red);
        try {
            this.mMapHolder = (MapHolder) context;
            try {
                this.mFragmentHolder = (FragmentHolder) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement FragmentHolder");
            }
        } catch (ClassCastException e2) {
            throw new ClassCastException(context.toString() + " must implement MapHolder");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mFragmentHolder = null;
        this.mMapHolder = null;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("lat", this.mLatitude);
        outState.putDouble("lon", this.mLongitude);
        outState.putInt("zoom", this.mZoom);
    }

    private void updateLocation(double latitude, double longitude, int zoom) {
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mZoom = zoom;
        this.mCoordinateDegree.setText(StringFormatter.coordinates(0, MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, latitude, longitude));
        this.mCoordinateDegMin.setText(StringFormatter.coordinates(1, MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, latitude, longitude));
        this.mCoordinateDegMinSec.setText(StringFormatter.coordinates(2, MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, latitude, longitude));
        this.mCoordinateUtmUps.setText(StringFormatter.coordinates(3, MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, latitude, longitude));
        this.mCoordinateMgrs.setText(StringFormatter.coordinates(4, MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, latitude, longitude));
        this.mSunriseSunset.setLocation(latitude, longitude);
        double sunrise = this.mSunriseSunset.compute(true);
        double sunset = this.mSunriseSunset.compute(false);
        if (sunrise == Double.MAX_VALUE || sunset == Double.MAX_VALUE) {
            this.mSunrise.setText(R.string.never_rises);
            this.mSunsetTitle.setVisibility(8);
            this.mSunset.setVisibility(8);
        } else if (sunrise == Double.MIN_VALUE || sunset == Double.MIN_VALUE) {
            this.mSunset.setText(R.string.never_sets);
            this.mSunriseTitle.setVisibility(8);
            this.mSunrise.setVisibility(8);
        } else {
            this.mSunrise.setText(this.mSunriseSunset.formatTime(sunrise));
            this.mSunset.setText(this.mSunriseSunset.formatTime(sunset));
            this.mSunriseTitle.setVisibility(0);
            this.mSunrise.setVisibility(0);
            this.mSunsetTitle.setVisibility(0);
            this.mSunset.setVisibility(0);
        }
        this.mOffset.setText(StringFormatter.timeO((int) (this.mSunriseSunset.getUtcOffset() * 60.0d)));
        GeomagneticField mag = new GeomagneticField((float) latitude, (float) longitude, 0.0f, System.currentTimeMillis());
        this.mDeclination.setText(String.format(Locale.getDefault(), "%+.1f°", new Object[]{Float.valueOf(mag.getDeclination())}));
    }

    public void onMapEvent(Event e, MapPosition mapPosition) {
        if (e == Map.POSITION_EVENT) {
            updateLocation(mapPosition.getLatitude(), mapPosition.getLongitude(), mapPosition.getZoomLevel());
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void afterTextChanged(Editable s) {
        if (s.length() == 0) {
            this.mTextInputDialog.setDescription("");
            return;
        }
        try {
            Result result = JosmCoordinatesParser.parseWithResult(s.toString());
            s.setSpan(new ForegroundColorSpan(this.mColorDarkBlue), 0, result.offset, 33);
            s.setSpan(new ForegroundColorSpan(this.mColorTextPrimary), result.offset, s.length(), 33);
            this.mTextInputDialog.setDescription(StringFormatter.coordinates(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, result.coordinates.getLatitude(), result.coordinates.getLongitude()));
        } catch (IllegalArgumentException e) {
            s.setSpan(new ForegroundColorSpan(this.mColorRed), 0, s.length(), 33);
            this.mTextInputDialog.setDescription("");
        }
    }

    public void onTextInputPositiveClick(String id, String inputText) {
        this.mTextInputDialog = null;
        try {
            this.mMapHolder.setMapLocation(JosmCoordinatesParser.parse(inputText));
        } catch (IllegalArgumentException e) {
            HelperUtils.showError(getString(R.string.msgParseCoordinatesFailed), this.mFragmentHolder.getCoordinatorLayout());
        }
    }

    public void onTextInputNegativeClick(String id) {
        this.mTextInputDialog = null;
    }

    public void onLocationStateChanged(LocationState locationState) {
        int visibility = locationState == LocationState.DISABLED ? 8 : 0;
        TransitionManager.beginDelayedTransition(this.mRootView, new Fade());
        this.mSwitchOffButton.setVisibility(visibility);
    }
}
