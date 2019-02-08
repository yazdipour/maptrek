package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import mobi.maptrek.Configuration;
import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.util.StringFormatter;
import org.oscim.core.GeoPoint;

public class MarkerInformation extends Fragment implements OnBackPressedListener {
    public static final String ARG_LATITUDE = "latitude";
    public static final String ARG_LONGITUDE = "longitude";
    public static final String ARG_NAME = "name";
    private FragmentHolder mFragmentHolder;
    private double mLatitude;
    private OnWaypointActionListener mListener;
    private double mLongitude;
    private MapHolder mMapHolder;
    private String mName;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_marker_information, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        String name;
        super.onActivityCreated(savedInstanceState);
        this.mLatitude = getArguments().getDouble("latitude");
        this.mLongitude = getArguments().getDouble("longitude");
        this.mName = getArguments().getString("name");
        if (savedInstanceState != null) {
            this.mLatitude = savedInstanceState.getDouble("latitude");
            this.mLongitude = savedInstanceState.getDouble("longitude");
            this.mName = savedInstanceState.getString("name");
        }
        if (this.mName == null || "".equals(this.mName)) {
            name = StringFormatter.coordinates(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, this.mLatitude, this.mLongitude);
        } else {
            name = this.mName;
        }
        ((TextView) getView().findViewById(R.id.name)).setText(name);
        final GeoPoint point = new GeoPoint(this.mLatitude, this.mLongitude);
        this.mMapHolder.showMarker(point, name);
        FloatingActionButton floatingButton = this.mFragmentHolder.enableActionButton();
        floatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_pin_drop));
        floatingButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String name;
                if (MarkerInformation.this.mName == null || "".equals(MarkerInformation.this.mName)) {
                    name = MarkerInformation.this.getString(R.string.waypoint_name, new Object[]{Integer.valueOf(Configuration.getPointsCounter())});
                } else {
                    name = MarkerInformation.this.mName;
                }
                MarkerInformation.this.mListener.onWaypointCreate(point, name, true, true);
                MarkerInformation.this.mFragmentHolder.disableActionButton();
                MarkerInformation.this.mFragmentHolder.popCurrent();
            }
        });
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
        this.mMapHolder.removeMarker();
        this.mFragmentHolder.removeBackClickListener(this);
        this.mFragmentHolder = null;
        this.mListener = null;
        this.mMapHolder = null;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("latitude", this.mLatitude);
        outState.putDouble("longitude", this.mLongitude);
        outState.putString("name", this.mName);
    }

    public boolean onBackClick() {
        this.mFragmentHolder.disableActionButton();
        return false;
    }
}
