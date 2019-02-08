package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import mobi.maptrek.Configuration;
import mobi.maptrek.LocationChangeListener;
import mobi.maptrek.MapHolder;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.ResUtils;
import mobi.maptrek.util.StringFormatter;
import org.oscim.core.GeoPoint;

public class AmenityInformation extends Fragment implements OnBackPressedListener, LocationChangeListener {
    static final /* synthetic */ boolean $assertionsDisabled = (!AmenityInformation.class.desiredAssertionStatus());
    public static final String ARG_LANG = "lang";
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    private FragmentHolder mFragmentHolder;
    private int mLang;
    private double mLatitude;
    private double mLongitude;
    private MapHolder mMapHolder;
    private Waypoint mWaypoint;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_amenity_information, container, false);
        this.mMapHolder.updateMapViewArea();
        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        double latitude = getArguments().getDouble("lat", Double.NaN);
        double longitude = getArguments().getDouble("lon", Double.NaN);
        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble("lat");
            longitude = savedInstanceState.getDouble("lon");
            this.mLang = savedInstanceState.getInt(ARG_LANG);
        }
        FloatingActionButton floatingButton = this.mFragmentHolder.enableActionButton();
        floatingButton.setImageResource(R.drawable.ic_navigate);
        floatingButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AmenityInformation.this.mFragmentHolder.disableActionButton();
                AmenityInformation.this.mMapHolder.navigateTo(AmenityInformation.this.mWaypoint.coordinates, AmenityInformation.this.mWaypoint.name);
                AmenityInformation.this.mFragmentHolder.popAll();
            }
        });
        this.mMapHolder.showMarker(this.mWaypoint.coordinates, this.mWaypoint.name);
        updateAmenityInformation(latitude, longitude);
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
    }

    public void onDetach() {
        super.onDetach();
        this.mMapHolder.removeMarker();
        this.mFragmentHolder.removeBackClickListener(this);
        this.mFragmentHolder = null;
        this.mMapHolder = null;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("lat", this.mLatitude);
        outState.putDouble("lon", this.mLongitude);
        outState.putInt(ARG_LANG, this.mLang);
    }

    public void setAmenity(long id) {
        this.mWaypoint = MapTrekDatabaseHelper.getAmenityData(this.mLang, id, MapTrek.getApplication().getDetailedMapDatabase());
        if (isVisible()) {
            this.mMapHolder.showMarker(this.mWaypoint.coordinates, this.mWaypoint.name);
            updateAmenityInformation(this.mLatitude, this.mLongitude);
        }
    }

    private void updateAmenityInformation(double latitude, double longitude) {
        View rootView = getView();
        if ($assertionsDisabled || rootView != null) {
            final Activity activity = getActivity();
            TextView nameView = (TextView) rootView.findViewById(R.id.name);
            if (nameView != null) {
                nameView.setText(this.mWaypoint.name);
            }
            View kindRow = rootView.findViewById(R.id.kindRow);
            if (!"".equals(this.mWaypoint.description)) {
                if (kindRow != null) {
                    kindRow.setVisibility(0);
                }
                TextView kindView = (TextView) rootView.findViewById(R.id.kind);
                if (kindView != null) {
                    Resources resources = activity.getResources();
                    kindView.setText(resources.getString(resources.getIdentifier(this.mWaypoint.description, "string", activity.getPackageName())));
                }
            } else if (kindRow != null) {
                kindRow.setVisibility(8);
            }
            ImageView iconView = (ImageView) rootView.findViewById(R.id.icon);
            if (iconView != null) {
                int icon = ResUtils.getKindIcon(this.mWaypoint.proximity);
                if (icon == 0) {
                    icon = R.drawable.ic_place;
                }
                iconView.setImageResource(icon);
            }
            TextView destinationView = (TextView) rootView.findViewById(R.id.destination);
            if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                GeoPoint geoPoint = new GeoPoint(latitude, longitude);
                String distance = StringFormatter.distanceH(geoPoint.vincentyDistance(this.mWaypoint.coordinates)) + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + StringFormatter.angleH(geoPoint.bearingTo(this.mWaypoint.coordinates));
                if (destinationView != null) {
                    destinationView.setVisibility(0);
                    destinationView.setTag(Boolean.valueOf(true));
                    destinationView.setText(distance);
                }
            } else if (destinationView != null) {
                destinationView.setVisibility(8);
            }
            final TextView coordsView = (TextView) rootView.findViewById(R.id.coordinates);
            if (coordsView != null) {
                coordsView.setText(StringFormatter.coordinates(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, this.mWaypoint.coordinates.getLatitude(), this.mWaypoint.coordinates.getLongitude()));
                if (HelperUtils.needsTargetedAdvice(1024)) {
                    final View view = rootView;
                    rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                        public void onGlobalLayout() {
                            view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            view.postDelayed(new Runnable() {
                                public void run() {
                                    if (AmenityInformation.this.isVisible()) {
                                        Rect r = new Rect();
                                        coordsView.getGlobalVisibleRect(r);
                                        HelperUtils.showTargetedAdvice(activity, 1024, R.string.advice_switch_coordinates_format, r);
                                    }
                                }
                            }, 1000);
                        }
                    });
                }
                coordsView.setOnTouchListener(new OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == 1) {
                            if (event.getX() >= ((float) (coordsView.getRight() - coordsView.getTotalPaddingRight()))) {
                                AmenityInformation.this.mMapHolder.shareLocation(AmenityInformation.this.mWaypoint.coordinates, AmenityInformation.this.mWaypoint.name);
                            } else {
                                StringFormatter.coordinateFormat++;
                                if (StringFormatter.coordinateFormat == 5) {
                                    StringFormatter.coordinateFormat = 0;
                                }
                                coordsView.setText(StringFormatter.coordinates(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, AmenityInformation.this.mWaypoint.coordinates.getLatitude(), AmenityInformation.this.mWaypoint.coordinates.getLongitude()));
                                Configuration.setCoordinatesFormat(StringFormatter.coordinateFormat);
                            }
                        }
                        return true;
                    }
                });
            }
            TextView elevationView = (TextView) rootView.findViewById(R.id.elevation);
            if (elevationView != null) {
                if (this.mWaypoint.altitude != Integer.MIN_VALUE) {
                    elevationView.setText(getString(R.string.waypoint_altitude, new Object[]{StringFormatter.elevationH((float) this.mWaypoint.altitude)}));
                    elevationView.setVisibility(0);
                } else {
                    elevationView.setVisibility(8);
                }
            }
            this.mLatitude = latitude;
            this.mLongitude = longitude;
            return;
        }
        throw new AssertionError();
    }

    public boolean onBackClick() {
        this.mFragmentHolder.disableActionButton();
        return false;
    }

    public void onLocationChanged(Location location) {
        updateAmenityInformation(location.getLatitude(), location.getLongitude());
    }

    public void setPreferredLanguage(String lang) {
        this.mLang = MapTrekDatabaseHelper.getLanguageId(lang);
    }
}
