package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import at.grabner.circleprogress.CircleProgressView;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import mobi.maptrek.MapHolder;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper;
import mobi.maptrek.maps.maptrek.Tags;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.JosmCoordinatesParser;
import mobi.maptrek.util.ResUtils;
import mobi.maptrek.util.StringFormatter;
import org.oscim.core.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class TextSearchFragment extends ListFragment implements OnClickListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    private static final int MSG_CREATE_FTS = 1;
    private static final int MSG_SEARCH = 2;
    private static final String[] columns = new String[]{"_id", "name", "kind", "lat", "lon"};
    private static final Logger logger = LoggerFactory.getLogger(TextSearchFragment.class);
    private DataListAdapter mAdapter;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private CancellationSignal mCancellationSignal;
    private GeoPoint mCoordinates;
    private SQLiteDatabase mDatabase;
    private MatrixCursor mEmptyCursor = new MatrixCursor(columns);
    private OnFeatureActionListener mFeatureActionListener;
    private ImageButton mFilterButton;
    private GeoPoint mFoundPoint;
    private CircleProgressView mFtsWait;
    private CharSequence[] mKinds;
    private OnLocationListener mLocationListener;
    private MapHolder mMapHolder;
    private TextView mMessage;
    private View mSearchFooter;
    private int mSelectedKind;
    private String mText;
    private boolean mUpdating;

    private class DataListAdapter extends CursorAdapter {
        private final int mAccentColor;
        private LayoutInflater mInflater;

        DataListAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mAccentColor = TextSearchFragment.this.getResources().getColor(R.color.colorAccentLight, context.getTheme());
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = this.mInflater.inflate(R.layout.list_item_amenity, parent, false);
            if (view != null) {
                ItemHolder holder = new ItemHolder();
                holder.name = (TextView) view.findViewById(R.id.name);
                holder.distance = (TextView) view.findViewById(R.id.distance);
                holder.icon = (ImageView) view.findViewById(R.id.icon);
                holder.viewButton = (ImageView) view.findViewById(R.id.view);
                view.setTag(holder);
            }
            return view;
        }

        public void bindView(View view, Context context, Cursor cursor) {
            ItemHolder holder = (ItemHolder) view.getTag();
            String name = cursor.getString(cursor.getColumnIndex("name"));
            int kind = cursor.getInt(cursor.getColumnIndex("kind"));
            float lat = cursor.getFloat(cursor.getColumnIndex("lat"));
            float lon = cursor.getFloat(cursor.getColumnIndex("lon"));
            final GeoPoint coordinates = new GeoPoint((double) lat, (double) lon);
            String distance = StringFormatter.distanceH(TextSearchFragment.this.mCoordinates.vincentyDistance(coordinates)) + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + StringFormatter.angleH(TextSearchFragment.this.mCoordinates.bearingTo(coordinates));
            holder.name.setText(name);
            holder.distance.setText(distance);
            holder.viewButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    TextSearchFragment.this.mMapHolder.setMapLocation(coordinates);
                }
            });
            int color = this.mAccentColor;
            int icon = ResUtils.getKindIcon(kind);
            if (icon == 0) {
                icon = R.drawable.ic_place;
            }
            holder.icon.setImageResource(icon);
            Drawable background = holder.icon.getBackground().mutate();
            if (background instanceof ShapeDrawable) {
                ((ShapeDrawable) background).getPaint().setColor(color);
            } else if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(color);
            }
        }
    }

    private static class ItemHolder {
        TextView distance;
        ImageView icon;
        TextView name;
        ImageView viewButton;

        private ItemHolder() {
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        this.mBackgroundThread = new HandlerThread("SearchThread");
        this.mBackgroundThread.start();
        this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper());
        this.mUpdating = false;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_list, container, false);
        this.mFtsWait = (CircleProgressView) rootView.findViewById(R.id.ftsWait);
        this.mMessage = (TextView) rootView.findViewById(R.id.message);
        this.mFilterButton = (ImageButton) rootView.findViewById(R.id.filterButton);
        this.mFilterButton.setOnClickListener(this);
        this.mSearchFooter = rootView.findViewById(R.id.searchFooter);
        EditText textEdit = (EditText) rootView.findViewById(R.id.textEdit);
        textEdit.requestFocus();
        textEdit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TextSearchFragment.this.mAdapter.changeCursor(TextSearchFragment.this.mEmptyCursor);
                    TextSearchFragment.this.updateListHeight();
                    TextSearchFragment.this.mText = null;
                    return;
                }
                TextSearchFragment.this.mText = s.toString();
                TextSearchFragment.this.search();
            }
        });
        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle arguments = getArguments();
        double latitude = arguments.getDouble("lat");
        double longitude = arguments.getDouble("lon");
        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble("lat");
            longitude = savedInstanceState.getDouble("lon");
        }
        Activity activity = getActivity();
        this.mCoordinates = new GeoPoint(latitude, longitude);
        this.mDatabase = MapTrek.getApplication().getDetailedMapDatabase();
        this.mAdapter = new DataListAdapter(activity, this.mEmptyCursor, 0);
        setListAdapter(this.mAdapter);
        Resources resources = activity.getResources();
        String packageName = activity.getPackageName();
        this.mKinds = new CharSequence[(Tags.kinds.length + 2)];
        this.mKinds[0] = activity.getString(R.string.any);
        this.mKinds[1] = resources.getString(R.string.kind_place);
        int i = 0;
        while (i < Tags.kinds.length) {
            int id = resources.getIdentifier(Tags.kinds[i], "string", packageName);
            this.mKinds[i + 2] = id != 0 ? resources.getString(id) : Tags.kinds[i];
            i++;
        }
        if (this.mUpdating || !MapTrekDatabaseHelper.hasFullTextIndex(this.mDatabase)) {
            this.mSearchFooter.setVisibility(8);
            this.mFtsWait.spin();
            this.mFtsWait.setVisibility(0);
            this.mMessage.setText(R.string.msgWaitForFtsTable);
            this.mMessage.setVisibility(0);
            if (this.mUpdating) {
                this.mBackgroundHandler.post(new Runnable() {
                    public void run() {
                        TextSearchFragment.this.hideProgress();
                    }
                });
                return;
            }
            this.mUpdating = true;
            Message m = Message.obtain(this.mBackgroundHandler, new Runnable() {
                public void run() {
                    MapTrekDatabaseHelper.createFtsTable(TextSearchFragment.this.mDatabase);
                    TextSearchFragment.this.hideProgress();
                    TextSearchFragment.this.mUpdating = false;
                }
            });
            m.what = 1;
            this.mBackgroundHandler.sendMessage(m);
            return;
        }
        HelperUtils.showTargetedAdvice(getActivity(), 16384, (int) R.string.advice_text_search, this.mSearchFooter, false);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mFeatureActionListener = (OnFeatureActionListener) context;
            try {
                this.mLocationListener = (OnLocationListener) context;
                try {
                    this.mMapHolder = (MapHolder) context;
                } catch (ClassCastException e) {
                    throw new ClassCastException(context.toString() + " must implement MapHolder");
                }
            } catch (ClassCastException e2) {
                throw new ClassCastException(context.toString() + " must implement OnLocationListener");
            }
        } catch (ClassCastException e3) {
            throw new ClassCastException(context.toString() + " must implement OnFeatureActionListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mFeatureActionListener = null;
        this.mLocationListener = null;
        this.mMapHolder = null;
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.mCancellationSignal != null) {
            this.mCancellationSignal.cancel();
        }
        this.mBackgroundThread.interrupt();
        this.mBackgroundHandler.removeCallbacksAndMessages(null);
        this.mBackgroundThread.quit();
        this.mBackgroundThread = null;
    }

    public void onListItemClick(ListView lv, View v, int position, long id) {
        View view = getView();
        if (view != null) {
            ((InputMethodManager) getActivity().getSystemService("input_method")).hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        if (id == 0) {
            this.mMapHolder.setMapLocation(this.mFoundPoint);
            this.mLocationListener.showMarkerInformation(this.mFoundPoint, StringFormatter.coordinates(this.mFoundPoint));
            return;
        }
        this.mFeatureActionListener.onFeatureDetails(id);
    }

    private void hideProgress() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    TextSearchFragment.this.mFtsWait.setVisibility(8);
                    TextSearchFragment.this.mFtsWait.stopSpinning();
                    TextSearchFragment.this.mMessage.setVisibility(8);
                    TextSearchFragment.this.mSearchFooter.setVisibility(0);
                    HelperUtils.showTargetedAdvice(TextSearchFragment.this.getActivity(), 16384, (int) R.string.advice_text_search, TextSearchFragment.this.mSearchFooter, false);
                }
            });
        }
    }

    public void onClick(View view) {
        if (view == this.mFilterButton) {
            Builder builder = new Builder(getActivity());
            builder.setSingleChoiceItems(this.mKinds, this.mSelectedKind, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    boolean changed = which != TextSearchFragment.this.mSelectedKind;
                    TextSearchFragment.this.mSelectedKind = which;
                    TextSearchFragment.this.mFilterButton.setColorFilter(TextSearchFragment.this.getActivity().getColor(TextSearchFragment.this.mSelectedKind > 0 ? R.color.colorAccent : R.color.colorPrimaryDark));
                    if (changed && TextSearchFragment.this.mText != null) {
                        TextSearchFragment.this.search();
                    }
                }
            });
            builder.create().show();
        }
    }

    private void search() {
        int mask = 1;
        String[] words = this.mText.split(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR);
        for (int i = 0; i < words.length; i++) {
            if (words[i].length() > 2) {
                words[i] = words[i] + Marker.ANY_MARKER;
            }
        }
        final Object match = TextUtils.join(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, words);
        logger.debug("search term: {}", match);
        String kindFilter = "";
        if (this.mSelectedKind > 0) {
            if (this.mSelectedKind != 1) {
                mask = 1 << (this.mSelectedKind + 1);
            }
            kindFilter = " AND (kind & " + mask + ") == " + mask;
            logger.debug("kind filter: {}", (Object) kindFilter);
        }
        final String sql = "SELECT DISTINCT features.id AS _id, kind, lat, lon, names.name AS name FROM names_fts INNER JOIN names ON (names_fts.docid = names.ref) INNER JOIN feature_names ON (names.ref = feature_names.name) INNER JOIN features ON (feature_names.id = features.id) WHERE names_fts MATCH ? AND (lat != 0 OR lon != 0)" + kindFilter + " LIMIT 200";
        this.mFilterButton.setImageResource(R.drawable.ic_hourglass_empty);
        this.mFilterButton.setColorFilter(getActivity().getColor(R.color.colorPrimaryDark));
        this.mFilterButton.setOnClickListener(null);
        Message m = Message.obtain(this.mBackgroundHandler, new Runnable() {
            public void run() {
                if (TextSearchFragment.this.mCancellationSignal != null) {
                    TextSearchFragment.this.mCancellationSignal.cancel();
                }
                TextSearchFragment.this.mCancellationSignal = new CancellationSignal();
                final Cursor cursor = TextSearchFragment.this.mDatabase.rawQuery(sql, new String[]{match}, TextSearchFragment.this.mCancellationSignal);
                if (TextSearchFragment.this.mCancellationSignal.isCanceled()) {
                    TextSearchFragment.this.mCancellationSignal = null;
                    return;
                }
                TextSearchFragment.this.mCancellationSignal = null;
                final Activity activity = TextSearchFragment.this.getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            Cursor resultCursor = cursor;
                            if (cursor.getCount() == 0) {
                                try {
                                    TextSearchFragment.this.mFoundPoint = JosmCoordinatesParser.parse(TextSearchFragment.this.mText);
                                    MatrixCursor pointCursor = new MatrixCursor(new String[]{"_id", "kind", "lat", "lon", "name"});
                                    pointCursor.addRow(new Object[]{Integer.valueOf(0), Integer.valueOf(0), Double.valueOf(TextSearchFragment.this.mFoundPoint.getLatitude()), Double.valueOf(TextSearchFragment.this.mFoundPoint.getLongitude()), StringFormatter.coordinates(TextSearchFragment.this.mFoundPoint)});
                                    resultCursor = pointCursor;
                                } catch (IllegalArgumentException e) {
                                }
                            }
                            TextSearchFragment.this.mAdapter.changeCursor(resultCursor);
                            TextSearchFragment.this.mFilterButton.setImageResource(R.drawable.ic_filter);
                            TextSearchFragment.this.mFilterButton.setColorFilter(activity.getColor(TextSearchFragment.this.mSelectedKind > 0 ? R.color.colorAccent : R.color.colorPrimaryDark));
                            TextSearchFragment.this.mFilterButton.setOnClickListener(TextSearchFragment.this);
                            TextSearchFragment.this.updateListHeight();
                        }
                    });
                }
            }
        });
        m.what = 2;
        this.mBackgroundHandler.sendMessage(m);
    }

    private void updateListHeight() {
        ListView listView = getListView();
        LayoutParams params = (LayoutParams) listView.getLayoutParams();
        if (this.mAdapter.getCount() > 5) {
            params.height = (int) (5.5d * ((double) getItemHeight()));
        } else {
            params.height = 0;
        }
        listView.setLayoutParams(params);
        this.mMapHolder.updateMapViewArea();
    }

    public float getItemHeight() {
        TypedValue value = new TypedValue();
        getActivity().getTheme().resolveAttribute(16842829, value, true);
        return TypedValue.complexToDimension(value.data, getResources().getDisplayMetrics());
    }
}
