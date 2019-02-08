package mobi.maptrek;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.BackStackEntry;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar.BaseCallback;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.Snackbar.Callback;
import android.support.v4.media.TransportMediator;
import android.support.v4.view.GravityCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import gov.nasa.worldwind.util.WWMath;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import mobi.maptrek.Configuration.ChangedEvent;
import mobi.maptrek.data.MapObject;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.WaypointDbDataSource;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.data.style.TrackStyle;
import mobi.maptrek.fragments.About;
import mobi.maptrek.fragments.AmenityInformation;
import mobi.maptrek.fragments.AmenitySetupDialog;
import mobi.maptrek.fragments.AmenitySetupDialog.AmenitySetupDialogCallback;
import mobi.maptrek.fragments.BaseMapDownload;
import mobi.maptrek.fragments.CrashReport;
import mobi.maptrek.fragments.DataExport;
import mobi.maptrek.fragments.DataList;
import mobi.maptrek.fragments.DataSourceList;
import mobi.maptrek.fragments.FragmentHolder;
import mobi.maptrek.fragments.LocationInformation;
import mobi.maptrek.fragments.LocationShareDialog;
import mobi.maptrek.fragments.MapList;
import mobi.maptrek.fragments.MapSelection;
import mobi.maptrek.fragments.MarkerInformation;
import mobi.maptrek.fragments.OnBackPressedListener;
import mobi.maptrek.fragments.OnFeatureActionListener;
import mobi.maptrek.fragments.OnLocationListener;
import mobi.maptrek.fragments.OnMapActionListener;
import mobi.maptrek.fragments.OnTrackActionListener;
import mobi.maptrek.fragments.OnWaypointActionListener;
import mobi.maptrek.fragments.PanelMenuFragment;
import mobi.maptrek.fragments.Settings;
import mobi.maptrek.fragments.TextSearchFragment;
import mobi.maptrek.fragments.TrackInformation;
import mobi.maptrek.fragments.TrackProperties;
import mobi.maptrek.fragments.TrackProperties.OnTrackPropertiesChangedListener;
import mobi.maptrek.fragments.WaypointInformation;
import mobi.maptrek.fragments.WaypointProperties;
import mobi.maptrek.fragments.WaypointProperties.OnWaypointPropertiesChangedListener;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.Manager.OnSaveListener;
import mobi.maptrek.io.TrackManager;
import mobi.maptrek.layers.CrosshairLayer;
import mobi.maptrek.layers.CurrentTrackLayer;
import mobi.maptrek.layers.LocationOverlay;
import mobi.maptrek.layers.MapCoverageLayer;
import mobi.maptrek.layers.MapEventLayer;
import mobi.maptrek.layers.MapObjectLayer;
import mobi.maptrek.layers.NavigationLayer;
import mobi.maptrek.layers.TrackLayer;
import mobi.maptrek.layers.building.BuildingLayer;
import mobi.maptrek.layers.marker.ItemizedLayer;
import mobi.maptrek.layers.marker.ItemizedLayer.OnItemGestureListener;
import mobi.maptrek.layers.marker.MarkerItem;
import mobi.maptrek.layers.marker.MarkerItem.HotspotPlace;
import mobi.maptrek.layers.marker.MarkerLayer;
import mobi.maptrek.layers.marker.MarkerSymbol;
import mobi.maptrek.location.BaseLocationService;
import mobi.maptrek.location.BaseNavigationService;
import mobi.maptrek.location.ILocationListener;
import mobi.maptrek.location.ILocationService;
import mobi.maptrek.location.INavigationService;
import mobi.maptrek.location.LocationService;
import mobi.maptrek.location.NavigationService;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;
import mobi.maptrek.maps.MapService;
import mobi.maptrek.maps.Themes;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.maps.maptrek.Index.ACTION;
import mobi.maptrek.maps.maptrek.LabelTileLoaderHook;
import mobi.maptrek.maps.maptrek.MapTrekTileSource;
import mobi.maptrek.maps.maptrek.MapTrekTileSource.OnDataMissingListener;
import mobi.maptrek.maps.maptrek.Tags;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.MarkerFactory;
import mobi.maptrek.util.MathUtils;
import mobi.maptrek.util.OsmcSymbolFactory;
import mobi.maptrek.util.ProgressHandler;
import mobi.maptrek.util.ShieldFactory;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.util.SunriseSunset;
import mobi.maptrek.view.GaugePanel;
import mobi.maptrek.view.PanelMenu;
import mobi.maptrek.view.PanelMenu.OnPrepareMenuListener;
import org.greenrobot.eventbus.Subscribe;
import org.oscim.android.MapView;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.AbstractMapEventLayer;
import org.oscim.layers.Layer;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.map.Map.InputListener;
import org.oscim.map.Map.UpdateListener;
import org.oscim.map.Viewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.ThemeLoader;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;
import org.oscim.utils.Osm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainActivity extends BasePluginActivity implements ILocationListener, DataHolder, MapHolder, InputListener, UpdateListener, GestureListener, FragmentHolder, OnWaypointPropertiesChangedListener, OnTrackPropertiesChangedListener, OnLocationListener, OnWaypointActionListener, OnTrackActionListener, OnMapActionListener, OnFeatureActionListener, OnItemGestureListener<MarkerItem>, OnMenuItemClickListener, LoaderCallbacks<List<FileDataSource>>, OnBackStackChangedListener, OnDataMissingListener, AmenitySetupDialogCallback {
    private static final int MAP_3D = 5;
    private static final int MAP_3D_DATA = 8;
    private static final int MAP_BASE = 2;
    public static final int MAP_BEARING_ANIMATION_DURATION = 300;
    private static final int MAP_DATA = 7;
    private static final int MAP_EVENTS = 1;
    private static final int MAP_LABELS = 6;
    private static final int MAP_MAPS = 3;
    private static final int MAP_MAP_OVERLAYS = 4;
    private static final int MAP_OVERLAYS = 10;
    private static final int MAP_POSITIONAL = 9;
    public static final int MAP_POSITION_ANIMATION_DURATION = 500;
    private static final int NIGHT_CHECK_PERIOD = 180000;
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private static final int TRACK_ROTATION_DELAY = 1000;
    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);
    private float deltaX;
    private float deltaY;
    private float downX;
    private float downY;
    private FloatingActionButton mActionButton;
    private MarkerItem mActiveMarker;
    private float mAutoTilt;
    private boolean mAutoTiltSet;
    private boolean mAutoTiltShouldSet;
    private float mAveragedBearing = 0.0f;
    final Handler mBackHandler = new Handler();
    private final Set<WeakReference<OnBackPressedListener>> mBackListeners = new HashSet();
    private Toast mBackToast;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private VectorTileLayer mBaseLayer;
    private boolean mBaseMapWarningShown = false;
    private MapFile mBitmapLayerMap;
    private int mBitmapMapTransparency = 0;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Object action = intent.getAction();
            MainActivity.logger.debug("Broadcast: {}", action);
            if (action.equals(MapService.BROADCAST_MAP_ADDED) || action.equals(MapService.BROADCAST_MAP_REMOVED)) {
                MainActivity.this.mMap.clearMap();
            }
            if (action.equals(BaseLocationService.BROADCAST_TRACK_SAVE)) {
                final Bundle extras = intent.getExtras();
                if (extras.getBoolean("saved")) {
                    MainActivity.logger.debug("Track saved: {}", extras.getString("path"));
                    Snackbar.make(MainActivity.this.mCoordinatorLayout, (int) R.string.msgTrackSaved, 0).setAction((int) R.string.actionCustomize, new OnClickListener() {
                        public void onClick(View view) {
                            MainActivity.this.onTrackProperties(extras.getString("path"));
                        }
                    }).setCallback(new Callback() {
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            if (event != 1) {
                                HelperUtils.showTargetedAdvice(MainActivity.this, 256, (int) R.string.advice_recorded_tracks, MainActivity.this.mRecordButton, false);
                            }
                        }
                    }).show();
                    return;
                }
                Object reason = extras.getString("reason");
                MainActivity.logger.warn("Track not saved: {}", reason);
                if ("period".equals(reason) || "distance".equals(reason)) {
                    Snackbar.make(MainActivity.this.mCoordinatorLayout, "period".equals(reason) ? R.string.msgTrackNotSavedPeriod : R.string.msgTrackNotSavedDistance, 0).setAction((int) R.string.actionSave, new OnClickListener() {
                        public void onClick(View view) {
                            MainActivity.this.mLocationService.saveTrack();
                        }
                    }).show();
                } else {
                    Exception e = (Exception) extras.getSerializable("exception");
                    if (e == null) {
                        e = new RuntimeException("Unknown error");
                    }
                    HelperUtils.showSaveError(MainActivity.this, MainActivity.this.mCoordinatorLayout, e);
                }
            }
            if (action.equals(BaseNavigationService.BROADCAST_NAVIGATION_STATE)) {
                MainActivity.this.enableNavigation();
                MainActivity.this.updateNavigationUI();
            }
            if (action.equals(BaseNavigationService.BROADCAST_NAVIGATION_STATUS) && MainActivity.this.mNavigationService != null) {
                MainActivity.this.mGaugePanel.setValue(65536, MainActivity.this.mNavigationService.getDistance());
                MainActivity.this.mGaugePanel.setValue(131072, MainActivity.this.mNavigationService.getBearing());
                MainActivity.this.mGaugePanel.setValue(262144, MainActivity.this.mNavigationService.getTurn());
                MainActivity.this.mGaugePanel.setValue(524288, MainActivity.this.mNavigationService.getVmg());
                MainActivity.this.mGaugePanel.setValue(1048576, MainActivity.this.mNavigationService.getXtk());
                MainActivity.this.mGaugePanel.setValue(2097152, (float) MainActivity.this.mNavigationService.getEte());
                MainActivity.this.adjustNavigationArrow(MainActivity.this.mNavigationService.getTurn());
                MainActivity.this.updateNavigationUI();
            }
        }
    };
    private BuildingLayer mBuildingsLayer;
    private boolean mBuildingsLayerEnabled = true;
    private int mColorAccent;
    private int mColorPrimaryDark;
    private View mCompassView;
    private CoordinatorLayout mCoordinatorLayout;
    private CrosshairLayer mCrosshairLayer;
    private CurrentTrackLayer mCurrentTrackLayer;
    private List<FileDataSource> mData = new ArrayList();
    private DataFragment mDataFragment;
    private Track mEditedTrack;
    private Waypoint mEditedWaypoint;
    private ViewGroup mExtendPanel;
    private float mFingerTipSize;
    private boolean mFirstMove = true;
    private FragmentManager mFragmentManager;
    private GaugePanel mGaugePanel;
    private TileGridLayer mGridLayer;
    private boolean mHideMapObjects = true;
    private BitmapTileLayer mHillshadeLayer;
    private boolean mIsLocationBound = false;
    private boolean mIsNavigationBound = false;
    private LabelTileLoaderHook mLabelTileLoaderHook;
    private LabelLayer mLabelsLayer;
    private long mLastLocationMilliseconds = 0;
    private TextView mLicense;
    private FloatingActionButton mListActionButton;
    private ImageButton mLocationButton;
    private final Set<WeakReference<LocationChangeListener>> mLocationChangeListeners = new HashSet();
    private ServiceConnection mLocationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            MainActivity.logger.debug("onServiceConnected: LocationService");
            MainActivity.this.mLocationService = (ILocationService) binder;
            MainActivity.this.mLocationService.registerLocationCallback(MainActivity.this);
            MainActivity.this.mLocationService.setProgressListener(MainActivity.this.mProgressHandler);
            MainActivity.this.updateNavigationUI();
        }

        public void onServiceDisconnected(ComponentName className) {
            MainActivity.logger.debug("onServiceDisconnected: LocationService");
            MainActivity.this.mLocationService = null;
            MainActivity.this.updateNavigationUI();
        }
    };
    private LocationOverlay mLocationOverlay;
    private VectorDrawable mLocationSearchingDrawable;
    private ILocationService mLocationService = null;
    private LocationState mLocationState;
    private final Set<WeakReference<LocationStateChangeListener>> mLocationStateChangeListeners = new HashSet();
    private Handler mMainHandler;
    protected Map mMap;
    private View mMapButtonHolder;
    private MapCoverageLayer mMapCoverageLayer;
    private Button mMapDownloadButton;
    private MapEventLayer mMapEventLayer;
    private MapIndex mMapIndex;
    private MapPosition mMapPosition = new MapPosition();
    private MapScaleBarLayer mMapScaleBarLayer;
    protected MapView mMapView;
    private ImageButton mMapsButton;
    private MarkerItem mMarker;
    private ItemizedLayer<MarkerItem> mMarkerLayer;
    private ImageButton mMoreButton;
    private int mMovementAnimationDuration = 300;
    private int mMovingOffset = 0;
    private VectorDrawable mMyLocationDrawable;
    private Index mNativeMapIndex;
    private MapTrekTileSource mNativeTileSource;
    private View mNavigationArrowView;
    private ServiceConnection mNavigationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            MainActivity.logger.debug("onServiceConnected: NavigationService");
            MainActivity.this.mNavigationService = (INavigationService) binder;
            MainActivity.this.updateNavigationUI();
        }

        public void onServiceDisconnected(ComponentName className) {
            MainActivity.logger.debug("onServiceDisconnected: NavigationService");
            MainActivity.this.mNavigationService = null;
            MainActivity.this.updateNavigationUI();
        }
    };
    private NavigationLayer mNavigationLayer;
    private VectorDrawable mNavigationNorthDrawable;
    private INavigationService mNavigationService = null;
    private VectorDrawable mNavigationTrackDrawable;
    private long mNextNightCheck = 0;
    private boolean mNightMode = false;
    private NIGHT_MODE_STATE mNightModeState;
    private OsmcSymbolFactory mOsmcSymbolFactory;
    private int mPanelBackground;
    private int mPanelExtendedBackground;
    private int mPanelSolidBackground;
    private PANEL_STATE mPanelState;
    private ImageButton mPlacesButton;
    private View mPopupAnchor;
    private boolean mPositionLocked = false;
    private LocationState mPreviousLocationState;
    private ProgressBar mProgressBar;
    private ProgressHandler mProgressHandler;
    private ImageButton mRecordButton;
    private TextView mSatellitesText;
    private LocationState mSavedLocationState;
    private GeoPoint mSelectedPoint;
    private ShieldFactory mShieldFactory;
    private int mSlideGravity;
    private long mStartTime;
    private int mStatusBarHeight;
    private SunriseSunset mSunriseSunset;
    private int mTotalDataItems = 0;
    private long mTrackingDelay;
    private int mTrackingOffset = 0;
    private double mTrackingOffsetFactor = 1.0d;
    private TRACKING_STATE mTrackingState;
    private boolean mVerticalOrientation;
    private BroadcastReceiver mWaypointBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Object action = intent.getAction();
            MainActivity.logger.debug("Broadcast: {}", action);
            if (action.equals(WaypointDbDataSource.BROADCAST_WAYPOINTS_RESTORED)) {
                for (Waypoint waypoint : MainActivity.this.mWaypointDbDataSource.getWaypoints()) {
                    MainActivity.this.removeWaypointMarker(waypoint);
                }
                MainActivity.this.mWaypointDbDataSource.close();
            }
            if (action.equals(WaypointDbDataSource.BROADCAST_WAYPOINTS_REWRITTEN)) {
                MainActivity.this.mWaypointDbDataSource.open();
                MainActivity.this.mWaypointDbDataSource.notifyListeners();
                for (Waypoint waypoint2 : MainActivity.this.mWaypointDbDataSource.getWaypoints()) {
                    if (MainActivity.this.mEditedWaypoint != null && MainActivity.this.mEditedWaypoint._id == waypoint2._id) {
                        MainActivity.this.mEditedWaypoint = waypoint2;
                    }
                    MainActivity.this.addWaypointMarker(waypoint2);
                }
            }
        }
    };
    private WaypointDbDataSource mWaypointDbDataSource;
    private boolean secondBack;

    private enum NIGHT_MODE_STATE {
        AUTO,
        DAY,
        NIGHT
    }

    private enum PANEL_STATE {
        NONE,
        LOCATION,
        RECORD,
        PLACES,
        MAPS,
        MORE
    }

    public enum TRACKING_STATE {
        DISABLED,
        PENDING,
        TRACKING
    }

    @SuppressLint({"ShowToast"})
    protected void onCreate(Bundle savedInstanceState) {
        String language;
        super.onCreate(savedInstanceState);
        logger.debug("onCreate()");
        Window window = getWindow();
        window.addFlags(67108864);
        setContentView(R.layout.activity_main);
        MapTrek application = MapTrek.getApplication();
        Resources resources = getResources();
        Theme theme = getTheme();
        this.mColorAccent = resources.getColor(R.color.colorAccent, theme);
        this.mColorPrimaryDark = resources.getColor(R.color.colorPrimaryDark, theme);
        this.mPanelBackground = resources.getColor(R.color.panelBackground, theme);
        this.mPanelSolidBackground = resources.getColor(R.color.panelSolidBackground, theme);
        this.mPanelExtendedBackground = resources.getColor(R.color.panelExtendedBackground, theme);
        this.mStatusBarHeight = getStatusBarHeight();
        this.mMainHandler = new Handler(Looper.getMainLooper());
        this.mBackgroundThread = new HandlerThread("BackgroundThread");
        this.mBackgroundThread.setPriority(1);
        this.mBackgroundThread.start();
        this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper());
        this.mFingerTipSize = (float) (((double) MapTrek.ydpi) * 0.1d);
        this.mSunriseSunset = new SunriseSunset();
        this.mNightModeState = NIGHT_MODE_STATE.DAY;
        TrackStyle.DEFAULT_COLOR = resources.getColor(R.color.trackColor, theme);
        TrackStyle.DEFAULT_WIDTH = (float) resources.getInteger(R.integer.trackWidth);
        this.mFragmentManager = getFragmentManager();
        this.mFragmentManager.addOnBackStackChangedListener(this);
        this.mDataFragment = (DataFragment) this.mFragmentManager.findFragmentByTag("data");
        File mapsDir = getExternalFilesDir("maps");
        this.mNativeMapIndex = application.getMapIndex();
        if (this.mDataFragment == null) {
            this.mDataFragment = new DataFragment();
            this.mFragmentManager.beginTransaction().add(this.mDataFragment, "data").commit();
            this.mMapIndex = new MapIndex(getApplicationContext(), mapsDir);
            initializePlugins();
            this.mMapIndex.initializeOnlineMapProviders();
            this.mWaypointDbDataSource = new WaypointDbDataSource(getApplicationContext(), new File(getExternalFilesDir("databases"), "waypoints.sqlitedb"));
            this.mBitmapLayerMap = this.mMapIndex.getMap(Configuration.getBitmapMap());
            if (Configuration.getLanguage() == null) {
                language = resources.getConfiguration().locale.getLanguage();
                if (!Arrays.asList(new String[]{"en", "de", "ru"}).contains(language)) {
                    language = "none";
                }
                Configuration.setLanguage(language);
            }
            this.mShieldFactory = new ShieldFactory();
            this.mOsmcSymbolFactory = new OsmcSymbolFactory();
        } else {
            this.mMapIndex = this.mDataFragment.getMapIndex();
            this.mEditedWaypoint = this.mDataFragment.getEditedWaypoint();
            this.mWaypointDbDataSource = this.mDataFragment.getWaypointDbDataSource();
            this.mBitmapLayerMap = this.mDataFragment.getBitmapLayerMap();
            this.mShieldFactory = this.mDataFragment.getShieldFactory();
            this.mOsmcSymbolFactory = this.mDataFragment.getOsmcSymbolFactory();
        }
        this.mLocationState = LocationState.DISABLED;
        this.mSavedLocationState = LocationState.DISABLED;
        this.mPreviousLocationState = LocationState.NORTH;
        this.mPanelState = PANEL_STATE.NONE;
        this.mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        this.mActionButton = (FloatingActionButton) findViewById(R.id.actionButton);
        this.mListActionButton = (FloatingActionButton) findViewById(R.id.listActionButton);
        this.mLocationButton = (ImageButton) findViewById(R.id.locationButton);
        this.mRecordButton = (ImageButton) findViewById(R.id.recordButton);
        this.mPlacesButton = (ImageButton) findViewById(R.id.placesButton);
        this.mMapsButton = (ImageButton) findViewById(R.id.mapsButton);
        this.mMoreButton = (ImageButton) findViewById(R.id.moreButton);
        this.mMapDownloadButton = (Button) findViewById(R.id.mapDownloadButton);
        this.mLicense = (TextView) findViewById(R.id.license);
        this.mLicense.setClickable(true);
        this.mLicense.setMovementMethod(LinkMovementMethod.getInstance());
        this.mPopupAnchor = findViewById(R.id.popupAnchor);
        this.mGaugePanel = (GaugePanel) findViewById(R.id.gaugePanel);
        this.mGaugePanel.setTag(Boolean.TRUE);
        this.mGaugePanel.setMapHolder(this);
        this.mSatellitesText = (TextView) findViewById(R.id.satellites);
        this.mMapButtonHolder = findViewById(R.id.mapButtonHolder);
        this.mCompassView = findViewById(R.id.compass);
        this.mNavigationArrowView = findViewById(R.id.navigationArrow);
        this.mNavigationArrowView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.setMapLocation(MainActivity.this.mNavigationService.getWaypoint().coordinates);
            }
        });
        this.mNavigationArrowView.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                MainActivity.this.showNavigationMenu();
                return true;
            }
        });
        this.mExtendPanel = (ViewGroup) findViewById(R.id.extendPanel);
        this.mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        this.mExtendPanel.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int width = v.getWidth();
                int height = v.getHeight();
                MainActivity.logger.debug("onLayoutChange({}, {})", Integer.valueOf(width), Integer.valueOf(height));
                if (width == 0 || height == 0) {
                    v.setTranslationX(0.0f);
                    v.setTranslationY(0.0f);
                    return;
                }
                int rootWidth = MainActivity.this.mCoordinatorLayout.getWidth();
                int rootHeight = MainActivity.this.mCoordinatorLayout.getHeight();
                int cWidth;
                switch (MainActivity.this.mPanelState) {
                    case RECORD:
                        if (MainActivity.this.mVerticalOrientation) {
                            cWidth = (int) (((float) MainActivity.this.mRecordButton.getWidth()) + MainActivity.this.mRecordButton.getX());
                            if (width < cWidth) {
                                v.setTranslationX((float) (cWidth - width));
                                return;
                            }
                            return;
                        }
                        return;
                    case PLACES:
                        if (MainActivity.this.mVerticalOrientation) {
                            cWidth = (int) (((float) MainActivity.this.mPlacesButton.getWidth()) + MainActivity.this.mPlacesButton.getX());
                            if (width < cWidth) {
                                v.setTranslationX((float) (cWidth - width));
                                return;
                            }
                            return;
                        }
                        return;
                    case MAPS:
                        if (!MainActivity.this.mVerticalOrientation) {
                            v.setTranslationY((float) (rootHeight - height));
                            return;
                        } else if (width < ((int) (((float) rootWidth) - MainActivity.this.mMapsButton.getX()))) {
                            v.setTranslationX(MainActivity.this.mMapsButton.getX());
                            return;
                        } else {
                            v.setTranslationX((float) (rootWidth - width));
                            return;
                        }
                    case MORE:
                        if (MainActivity.this.mVerticalOrientation) {
                            v.setTranslationX((float) (rootWidth - width));
                            return;
                        } else {
                            v.setTranslationY((float) (rootHeight - height));
                            return;
                        }
                    default:
                        return;
                }
            }
        });
        this.mExtendPanel.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
            public void onChildViewAdded(View parent, View child) {
                if (!MainActivity.this.mVerticalOrientation) {
                    switch (MainActivity.this.mPanelState) {
                        case RECORD:
                            child.setMinimumHeight((int) (((float) MainActivity.this.mRecordButton.getHeight()) + MainActivity.this.mRecordButton.getY()));
                            return;
                        case PLACES:
                            child.setMinimumHeight((int) (((float) MainActivity.this.mPlacesButton.getHeight()) + MainActivity.this.mPlacesButton.getY()));
                            return;
                        case MAPS:
                            child.setMinimumHeight((int) (((float) MainActivity.this.mCoordinatorLayout.getHeight()) - MainActivity.this.mMapsButton.getY()));
                            return;
                        default:
                            return;
                    }
                }
            }

            public void onChildViewRemoved(View parent, View child) {
            }
        });
        this.mMapView = (MapView) findViewById(R.id.mapView);
        this.mMap = this.mMapView.map();
        MapPosition mapPosition = Configuration.getPosition();
        this.mMap.setMapPosition(mapPosition);
        if (mapPosition.x == 0.5d && mapPosition.y == 0.5d) {
            String language2 = resources.getConfiguration().locale.getLanguage();
            Object obj = -1;
            switch (language2.hashCode()) {
                case 3201:
                    if (language2.equals("de")) {
                        obj = null;
                        break;
                    }
                    break;
                case 3651:
                    if (language2.equals("ru")) {
                        obj = 1;
                        break;
                    }
                    break;
            }
            switch (obj) {
                case null:
                    this.mMap.setMapPosition(50.8d, 10.45d, 96.0d);
                    break;
                case 1:
                    this.mMap.setMapPosition(56.4d, 39.0d, 32.0d);
                    break;
                default:
                    this.mMap.setMapPosition(-19.0d, -12.0d, 4.0d);
                    break;
            }
        }
        this.mAutoTilt = Configuration.getAutoTilt();
        this.mNavigationNorthDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_navigation_north, theme);
        this.mNavigationTrackDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_navigation_track, theme);
        this.mMyLocationDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_my_location, theme);
        this.mLocationSearchingDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_location_searching, theme);
        Layers layers = this.mMap.layers();
        layers.addGroup(1);
        layers.addGroup(2);
        this.mNativeTileSource = new MapTrekTileSource(application.getDetailedMapDatabase());
        this.mNativeTileSource.setContoursEnabled(Configuration.getContoursEnabled());
        this.mBaseLayer = new OsmTileLayer(this.mMap);
        this.mBaseLayer.setTileSource(this.mNativeTileSource);
        this.mMap.setBaseMap(this.mBaseLayer);
        this.mNativeTileSource.setOnDataMissingListener(this);
        layers.addGroup(3);
        layers.addGroup(4);
        layers.addGroup(5);
        layers.addGroup(6);
        layers.addGroup(7);
        layers.addGroup(8);
        layers.addGroup(9);
        layers.addGroup(10);
        if (Configuration.getHillshadesEnabled()) {
            showHillShade();
        }
        this.mGridLayer = new TileGridLayer(this.mMap, MapTrek.density * 0.75f);
        if (Configuration.getGridLayerEnabled()) {
            layers.add(this.mGridLayer, 10);
        }
        this.mBuildingsLayerEnabled = Configuration.getBuildingsLayerEnabled();
        if (this.mBuildingsLayerEnabled) {
            this.mBuildingsLayer = new BuildingLayer(this.mMap, this.mBaseLayer);
            layers.add(this.mBuildingsLayer, 5);
        }
        this.mLabelTileLoaderHook = new LabelTileLoaderHook(this.mShieldFactory, this.mOsmcSymbolFactory);
        language = Configuration.getLanguage();
        if (!"none".equals(language)) {
            this.mLabelTileLoaderHook.setPreferredLanguage(language);
        }
        this.mLabelsLayer = new LabelLayer(this.mMap, this.mBaseLayer, this.mLabelTileLoaderHook);
        layers.add(this.mLabelsLayer, 6);
        this.mMapScaleBarLayer = new MapScaleBarLayer(this.mMap, new DefaultMapScaleBar(this.mMap, MapTrek.density * 0.75f));
        this.mCrosshairLayer = new CrosshairLayer(this.mMap, MapTrek.density);
        this.mLocationOverlay = new LocationOverlay(this.mMap, MapTrek.density);
        layers.add(this.mMapScaleBarLayer, 10);
        layers.add(this.mCrosshairLayer, 10);
        layers.add(this.mLocationOverlay, 9);
        layers.add(new MapObjectLayer(this.mMap, MapTrek.density), 8);
        this.mMarkerLayer = new ItemizedLayer(this.mMap, new ArrayList(), new MarkerSymbol(new AndroidBitmap(MarkerFactory.getMarkerSymbol(this)), HotspotPlace.BOTTOM_CENTER), MapTrek.density, this);
        layers.add(this.mMarkerLayer, 8);
        this.mWaypointDbDataSource.open();
        for (Waypoint waypoint : this.mWaypointDbDataSource.getWaypoints()) {
            if (this.mEditedWaypoint != null && this.mEditedWaypoint._id == waypoint._id) {
                this.mEditedWaypoint = waypoint;
            }
            addWaypointMarker(waypoint);
            this.mTotalDataItems++;
        }
        registerReceiver(this.mWaypointBroadcastReceiver, new IntentFilter(WaypointDbDataSource.BROADCAST_WAYPOINTS_RESTORED));
        registerReceiver(this.mWaypointBroadcastReceiver, new IntentFilter(WaypointDbDataSource.BROADCAST_WAYPOINTS_REWRITTEN));
        this.mHideMapObjects = Configuration.getHideMapObjects();
        this.mBitmapMapTransparency = Configuration.getBitmapMapTransparency();
        if (this.mBitmapLayerMap != null) {
            showBitmapMap(this.mBitmapLayerMap, false);
        }
        setNightMode(false);
        this.mBackToast = Toast.makeText(this, R.string.msgBackQuit, 0);
        this.mProgressHandler = new ProgressHandler(this.mProgressBar);
        this.mLocationButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.onLocationClicked();
            }
        });
        this.mLocationButton.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                MainActivity.this.onLocationLongClicked();
                return true;
            }
        });
        this.mRecordButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.onRecordClicked();
            }
        });
        this.mRecordButton.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                MainActivity.this.onRecordLongClicked();
                return true;
            }
        });
        this.mPlacesButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.onPlacesClicked();
            }
        });
        this.mPlacesButton.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                MainActivity.this.onPlacesLongClicked();
                return true;
            }
        });
        this.mMapsButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.onMapsClicked();
            }
        });
        this.mMapsButton.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                MainActivity.this.onMapsLongClicked();
                return true;
            }
        });
        this.mMoreButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.onMoreClicked();
            }
        });
        this.mMoreButton.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                MainActivity.this.onMoreLongClicked();
                return true;
            }
        });
        this.mMapDownloadButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.onMapDownloadClicked();
            }
        });
        int state = Configuration.getLocationState();
        if (state >= LocationState.NORTH.ordinal()) {
            this.mSavedLocationState = LocationState.values()[state];
        }
        this.mPreviousLocationState = LocationState.values()[Configuration.getPreviousLocationState()];
        this.mTrackingState = TRACKING_STATE.values()[Configuration.getTrackingState()];
        this.mGaugePanel.initializeGauges(Configuration.getGauges());
        showActionPanel(Configuration.getActionPanelState(), false);
        MapObject mapObject = Configuration.getNavigationPoint();
        if (mapObject != null) {
            startNavigation(mapObject);
        }
        getLoaderManager();
        window.setBackgroundDrawable(new ColorDrawable(resources.getColor(R.color.colorBackground, theme)));
        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() ^ 8192);
        this.mStartTime = SystemClock.uptimeMillis();
        onNewIntent(getIntent());
        boolean freshInstall = true;
        try {
            freshInstall = getPackageManager().getPackageInfo(getPackageName(), 0).firstInstallTime == getPackageManager().getPackageInfo(getPackageName(), 0).lastUpdateTime;
        } catch (NameNotFoundException e) {
            logger.error("Can not find myself");
        }
        if (Configuration.getLastSeenIntroduction() >= 3) {
            return;
        }
        if (freshInstall) {
            Configuration.setLastSeenIntroduction(3);
        } else {
            startActivity(new Intent(this, IntroductionActivity.class));
        }
    }

    protected void onNewIntent(Intent intent) {
        Object action = intent.getAction();
        logger.debug("New intent: {}", action);
        String scheme = intent.getScheme();
        MapPosition position;
        if ("mobi.maptrek.action.CENTER_ON_COORDINATES".equals(action)) {
            position = this.mMap.getMapPosition();
            position.setPosition(intent.getDoubleExtra("lat", position.getLatitude()), intent.getDoubleExtra("lon", position.getLongitude()));
            setMapLocation(position.getGeoPoint());
        } else if ("mobi.maptrek.action.NAVIGATE_TO_OBJECT".equals(action)) {
            startNavigation(intent.getLongExtra("id", 0));
        } else if ("mobi.maptrek.action.RESET_ADVICES".equals(action)) {
            this.mBackgroundHandler.postDelayed(new Runnable() {
                public void run() {
                    Configuration.resetAdviceState();
                }
            }, 10000);
            Snackbar.make(this.mCoordinatorLayout, (int) R.string.msgAdvicesReset, 0).show();
        } else if ("geo".equals(scheme)) {
            uri = intent.getData();
            logger.debug("   {}", uri.toString());
            String data = uri.getSchemeSpecificPart();
            String query = uri.getQuery();
            int zoom = 0;
            if (query != null) {
                data = data.substring(0, data.indexOf(query) - 1);
                if (query.startsWith("z=")) {
                    try {
                        zoom = Integer.parseInt(query.substring(2));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                String[] ll = data.split(",");
                marker = null;
                double lat = Double.parseDouble(ll[0]);
                double lon = Double.parseDouble(ll[1]);
                if (lat == 0.0d && lon == 0.0d && query != null) {
                    int bracket = query.indexOf("(");
                    if (bracket > -1) {
                        data = query.substring(2, query.indexOf("("));
                    } else {
                        data = query.substring(2);
                    }
                    ll = data.split(",\\s*");
                    lat = Double.parseDouble(ll[0]);
                    lon = Double.parseDouble(ll[1]);
                    if (bracket > -1) {
                        marker = query.substring(query.indexOf("(") + 1, query.indexOf(")"));
                    }
                }
                position = this.mMap.getMapPosition();
                position.setPosition(lat, lon);
                if (zoom > 0) {
                    position.setZoomLevel(zoom);
                }
                this.mMap.setMapPosition(position);
                showMarkerInformation(position.getGeoPoint(), marker);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } else if ("http".equals(scheme) || "https".equals(scheme)) {
            uri = intent.getData();
            logger.debug("   {}", uri.toString());
            List<String> path = uri.getPathSegments();
            if ("go".equals(path.get(0))) {
                position = Osm.decodeShortLink((String) path.get(1));
                marker = uri.getQueryParameter("m");
                this.mMap.setMapPosition(position);
                showMarkerInformation(position.getGeoPoint(), marker);
            }
        }
    }

    protected void onStart() {
        super.onStart();
        logger.debug("onStart()");
        ((DataLoader) getLoaderManager().initLoader(0, null, this)).setProgressHandler(this.mProgressHandler);
        registerReceiver(this.mBroadcastReceiver, new IntentFilter(MapService.BROADCAST_MAP_ADDED));
        registerReceiver(this.mBroadcastReceiver, new IntentFilter(MapService.BROADCAST_MAP_REMOVED));
        registerReceiver(this.mBroadcastReceiver, new IntentFilter(BaseLocationService.BROADCAST_TRACK_SAVE));
        registerReceiver(this.mBroadcastReceiver, new IntentFilter(BaseNavigationService.BROADCAST_NAVIGATION_STATUS));
        registerReceiver(this.mBroadcastReceiver, new IntentFilter(BaseNavigationService.BROADCAST_NAVIGATION_STATE));
        MapTrek.isMainActivityRunning = true;
    }

    protected void onResume() {
        boolean z;
        super.onResume();
        logger.debug("onResume()");
        if (this.mSavedLocationState != LocationState.DISABLED) {
            askForPermission();
        }
        if (this.mTrackingState == TRACKING_STATE.TRACKING) {
            enableTracking();
            startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.DISABLE_BACKGROUND_TRACK));
        }
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(NavigationService.PREF_NAVIGATION_BACKGROUND, false)) {
            startService(new Intent(getApplicationContext(), NavigationService.class).setAction(BaseNavigationService.DISABLE_BACKGROUND_NAVIGATION));
            enableNavigation();
        }
        if (getResources().getConfiguration().orientation == 1) {
            z = true;
        } else {
            z = false;
        }
        this.mVerticalOrientation = z;
        this.mSlideGravity = this.mVerticalOrientation ? 80 : GravityCompat.END;
        this.mMapEventLayer = new MapEventLayer(this.mMap, this);
        this.mMap.layers().add(this.mMapEventLayer, 1);
        this.mMap.events.bind(this);
        this.mMap.input.bind(this);
        this.mMapView.onResume();
        updateLocationDrawable();
        adjustCompass(this.mMap.getMapPosition().bearing);
        this.mLicense.setText(Html.fromHtml(getString(R.string.osmLicense)));
        this.mLicense.setVisibility(0);
        Message m = Message.obtain(this.mMainHandler, new Runnable() {
            public void run() {
                MainActivity.this.mLicense.animate().alpha(0.0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        MainActivity.this.mLicense.setVisibility(8);
                        MainActivity.this.mLicense.animate().setListener(null);
                    }
                });
            }
        });
        m.what = R.id.msgRemoveLicense;
        this.mMainHandler.sendMessageDelayed(m, 10000);
        String userNotification = MapTrek.getApplication().getUserNotification();
        if (userNotification != null) {
            HelperUtils.showError(userNotification, this.mCoordinatorLayout);
        }
        FragmentTransaction ft;
        if (MapTrek.getApplication().hasPreviousRunsExceptions()) {
            Fragment fragment = Fragment.instantiate(this, CrashReport.class.getName());
            fragment.setEnterTransition(new Slide());
            ft = this.mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "crashReport");
            ft.addToBackStack("crashReport");
            ft.commit();
        } else if (!this.mBaseMapWarningShown && this.mNativeMapIndex.getBaseMapVersion() == (short) 0) {
            BaseMapDownload fragment2 = (BaseMapDownload) Fragment.instantiate(this, BaseMapDownload.class.getName());
            fragment2.setMapIndex(this.mNativeMapIndex);
            fragment2.setEnterTransition(new Slide());
            ft = this.mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment2, "baseMapDownload");
            ft.addToBackStack("baseMapDownload");
            ft.commit();
            this.mBaseMapWarningShown = true;
        }
        if (Configuration.getHideSystemUI()) {
            hideSystemUI();
        }
        updateMapViewArea();
        this.mMap.updateMap(true);
    }

    protected void onPause() {
        super.onPause();
        logger.debug("onPause()");
        if (this.mLocationState != LocationState.SEARCHING) {
            this.mSavedLocationState = this.mLocationState;
        }
        this.mMapView.onPause();
        this.mMap.events.unbind(this);
        this.mMap.layers().remove(this.mMapEventLayer);
        this.mMapEventLayer = null;
        this.mGaugePanel.onVisibilityChanged(false);
        Configuration.setPosition(this.mMap.getMapPosition());
        Configuration.setBitmapMap(this.mBitmapLayerMap);
        Configuration.setLocationState(this.mSavedLocationState.ordinal());
        Configuration.setPreviousLocationState(this.mPreviousLocationState.ordinal());
        Configuration.setTrackingState(this.mTrackingState.ordinal());
        Configuration.setGauges(this.mGaugePanel.getGaugeSettings());
        if (!isChangingConfigurations()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            if (this.mTrackingState == TRACKING_STATE.TRACKING) {
                startService(intent.setAction(BaseLocationService.ENABLE_BACKGROUND_TRACK));
            } else {
                stopService(intent);
            }
            if (this.mNavigationService != null) {
                intent = new Intent(getApplicationContext(), NavigationService.class);
                if (this.mNavigationService.isNavigating()) {
                    startService(intent.setAction(BaseNavigationService.ENABLE_BACKGROUND_NAVIGATION));
                } else {
                    stopService(intent);
                }
            }
            disableNavigation();
            disableLocations();
        }
    }

    protected void onStop() {
        super.onStop();
        logger.debug("onStop()");
        MapTrek.isMainActivityRunning = false;
        unregisterReceiver(this.mBroadcastReceiver);
        Loader<List<FileDataSource>> loader = getLoaderManager().getLoader(0);
        if (loader != null) {
            ((DataLoader) loader).setProgressHandler(null);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        logger.debug("onDestroy()");
        Configuration.updateRunningTime((SystemClock.uptimeMillis() - this.mStartTime) / 60000);
        this.mMap.destroy();
        for (FileDataSource source : this.mData) {
            source.setVisible(false);
        }
        unregisterReceiver(this.mWaypointBroadcastReceiver);
        this.mWaypointDbDataSource.close();
        this.mProgressHandler = null;
        logger.debug("  stopping threads...");
        this.mBackgroundThread.interrupt();
        this.mBackgroundHandler.removeCallbacksAndMessages(null);
        this.mBackgroundThread.quit();
        this.mBackgroundThread = null;
        this.mMainHandler = null;
        if (isFinishing()) {
            this.mMapIndex.clear();
            sendBroadcast(new Intent("mobi.maptrek.plugins.action.FINALIZE"));
            this.mShieldFactory.dispose();
            this.mOsmcSymbolFactory.dispose();
        }
        this.mFragmentManager = null;
        logger.debug("  done!");
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        logger.debug("onSaveInstanceState()");
        if (this.mLocationService != null) {
            startService(new Intent(getApplicationContext(), LocationService.class));
        }
        if (this.mNavigationService != null) {
            startService(new Intent(getApplicationContext(), NavigationService.class));
        }
        this.mDataFragment.setMapIndex(this.mMapIndex);
        this.mDataFragment.setEditedWaypoint(this.mEditedWaypoint);
        this.mDataFragment.setWaypointDbDataSource(this.mWaypointDbDataSource);
        this.mDataFragment.setBitmapLayerMap(this.mBitmapLayerMap);
        this.mDataFragment.setShieldFactory(this.mShieldFactory);
        this.mDataFragment.setOsmcSymbolFactory(this.mOsmcSymbolFactory);
        savedInstanceState.putSerializable("savedLocationState", this.mSavedLocationState);
        savedInstanceState.putSerializable("previousLocationState", this.mPreviousLocationState);
        savedInstanceState.putLong("lastLocationMilliseconds", this.mLastLocationMilliseconds);
        savedInstanceState.putFloat("averagedBearing", this.mAveragedBearing);
        savedInstanceState.putInt("movementAnimationDuration", this.mMovementAnimationDuration);
        savedInstanceState.putBoolean("savedNavigationState", this.mNavigationService != null);
        if (this.mProgressBar.getVisibility() == 0) {
            savedInstanceState.putInt("progressBar", this.mProgressBar.getMax());
        }
        savedInstanceState.putSerializable("panelState", this.mPanelState);
        savedInstanceState.putBoolean("nightMode", this.mNightMode);
        savedInstanceState.putBoolean("autoTiltShouldSet", this.mAutoTiltShouldSet);
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        logger.debug("onRestoreInstanceState()");
        super.onRestoreInstanceState(savedInstanceState);
        this.mSavedLocationState = (LocationState) savedInstanceState.getSerializable("savedLocationState");
        this.mPreviousLocationState = (LocationState) savedInstanceState.getSerializable("previousLocationState");
        this.mLastLocationMilliseconds = savedInstanceState.getLong("lastLocationMilliseconds");
        this.mAveragedBearing = savedInstanceState.getFloat("averagedBearing");
        this.mMovementAnimationDuration = savedInstanceState.getInt("movementAnimationDuration");
        if (savedInstanceState.getBoolean("savedNavigationState", false)) {
            enableNavigation();
        }
        if (savedInstanceState.containsKey("progressBar")) {
            this.mProgressBar.setVisibility(0);
            this.mProgressBar.setMax(savedInstanceState.getInt("progressBar"));
        }
        this.mAutoTiltShouldSet = savedInstanceState.getBoolean("autoTiltShouldSet");
        setPanelState((PANEL_STATE) savedInstanceState.getSerializable("panelState"));
    }

    public boolean onMenuItemClick(MenuItem item) {
        Fragment fragment;
        FragmentTransaction ft;
        Builder builder;
        Bundle args;
        switch (item.getItemId()) {
            case R.id.action3dBuildings:
                this.mBuildingsLayerEnabled = item.isChecked();
                if (this.mBuildingsLayerEnabled) {
                    this.mBuildingsLayer = new BuildingLayer(this.mMap, this.mBaseLayer);
                    this.mMap.layers().add(this.mBuildingsLayer, 5);
                    this.mMap.clearMap();
                } else {
                    this.mMap.layers().remove(this.mBuildingsLayer);
                    this.mBuildingsLayer = null;
                }
                Configuration.setBuildingsLayerEnabled(this.mBuildingsLayerEnabled);
                this.mMap.updateMap(true);
                return true;
            case R.id.actionAbout:
                fragment = Fragment.instantiate(this, About.class.getName());
                fragment.setEnterTransition(new Slide(this.mSlideGravity));
                fragment.setReturnTransition(new Slide(this.mSlideGravity));
                ft = this.mFragmentManager.beginTransaction();
                ft.replace(R.id.contentPanel, fragment, "about");
                ft.addToBackStack("about");
                ft.commit();
                return true;
            case R.id.actionActivity:
                int activity = Configuration.getActivity();
                builder = new Builder(this);
                builder.setTitle(R.string.actionActivity);
                builder.setSingleChoiceItems(R.array.activities, activity, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Configuration.setActivity(which);
                        MainActivity.this.mBackgroundHandler.post(new Runnable() {
                            public void run() {
                                MainActivity.this.setNightMode(MainActivity.this.mNightModeState == NIGHT_MODE_STATE.NIGHT);
                            }
                        });
                    }
                });
                builder.create().show();
                return true;
            case R.id.actionAddGauge:
                this.mGaugePanel.onLongClick(this.mGaugePanel);
                return true;
            case R.id.actionAddWaypointHere:
                removeMarker();
                String name = getString(R.string.waypoint_name, new Object[]{Integer.valueOf(Configuration.getPointsCounter())});
                onWaypointCreate(this.mSelectedPoint, name, false, true);
                return true;
            case R.id.actionAmenityZooms:
                new AmenitySetupDialog.Builder().setCallback(this).create().show(getFragmentManager(), "amenitySetup");
                return true;
            case R.id.actionAutoTilt:
                this.mMap.getMapPosition(this.mMapPosition);
                if (item.isChecked()) {
                    Configuration.setAutoTilt(Viewport.MAX_TILT);
                    this.mAutoTilt = Viewport.MAX_TILT;
                } else {
                    Configuration.setAutoTilt(-1.0f);
                    this.mAutoTilt = -1.0f;
                    if (this.mAutoTiltSet) {
                        this.mMapPosition.setTilt(0.0f);
                        this.mMap.animator().animateTo(300, this.mMapPosition);
                        this.mAutoTiltSet = false;
                    }
                }
                return true;
            case R.id.actionContours:
                this.mNativeTileSource.setContoursEnabled(item.isChecked());
                this.mMap.clearMap();
                Configuration.setContoursEnabled(item.isChecked());
                return true;
            case R.id.actionFontSize:
                builder = new Builder(this);
                builder.setTitle(R.string.actionFontSize);
                builder.setItems(R.array.font_size_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Configuration.setMapFontSize(which);
                        MainActivity.this.mBackgroundHandler.post(new Runnable() {
                            public void run() {
                                MainActivity.this.setNightMode(MainActivity.this.mNightModeState == NIGHT_MODE_STATE.NIGHT);
                            }
                        });
                    }
                });
                builder.create().show();
                return true;
            case R.id.actionGrid:
                if (item.isChecked()) {
                    this.mMap.layers().add(this.mGridLayer, 10);
                } else {
                    this.mMap.layers().remove(this.mGridLayer);
                }
                Configuration.setGridLayerEnabled(item.isChecked());
                this.mMap.updateMap(true);
                return true;
            case R.id.actionHideSystemUI:
                if (Configuration.getHideSystemUI()) {
                    showSystemUI();
                } else {
                    hideSystemUI();
                }
                return true;
            case R.id.actionHillshades:
                Configuration.setHillshadesEnabled(item.isChecked());
                return true;
            case R.id.actionLanguage:
                builder = new Builder(this);
                builder.setTitle(R.string.actionLanguage);
                builder.setItems(R.array.language_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String language = MainActivity.this.getResources().getStringArray(R.array.language_code_array)[which];
                        if ("none".equals(language)) {
                            MainActivity.this.mLabelTileLoaderHook.setPreferredLanguage(null);
                        } else {
                            MainActivity.this.mLabelTileLoaderHook.setPreferredLanguage(language);
                        }
                        MainActivity.this.mMap.clearMap();
                        Configuration.setLanguage(language);
                    }
                });
                builder.create().show();
                return true;
            case R.id.actionManageMaps:
                startMapSelection(true);
                return true;
            case R.id.actionNavigateHere:
                removeMarker();
                MapObject mapObject = new MapObject(this.mSelectedPoint.getLatitude(), this.mSelectedPoint.getLongitude());
                mapObject.name = getString(R.string.selectedLocation);
                startNavigation(mapObject);
                return true;
            case R.id.actionNightMode:
                builder = new Builder(this);
                builder.setTitle(R.string.actionNightMode);
                builder.setItems(R.array.night_mode_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.mNightModeState = NIGHT_MODE_STATE.values()[which];
                        if ((MainActivity.this.mNightModeState == NIGHT_MODE_STATE.NIGHT) != MainActivity.this.mNightMode) {
                            MainActivity.this.mBackgroundHandler.post(new Runnable() {
                                public void run() {
                                    MainActivity.this.setNightMode(MainActivity.this.mNightModeState == NIGHT_MODE_STATE.NIGHT);
                                }
                            });
                            Configuration.setNightModeState(MainActivity.this.mNightModeState.ordinal());
                        }
                    }
                });
                builder.create().show();
                return true;
            case R.id.actionOtherFeatures:
                PanelMenuFragment fragment2 = (PanelMenuFragment) Fragment.instantiate(this, PanelMenuFragment.class.getName());
                fragment2.setMenu(R.menu.menu_map_features, new OnPrepareMenuListener() {
                    public void onPrepareMenu(PanelMenu menu) {
                        menu.findItem(R.id.action3dBuildings).setChecked(MainActivity.this.mBuildingsLayerEnabled);
                        menu.findItem(R.id.actionHillshades).setChecked(Configuration.getHillshadesEnabled());
                        menu.findItem(R.id.actionContours).setChecked(Configuration.getContoursEnabled());
                        menu.findItem(R.id.actionGrid).setChecked(MainActivity.this.mMap.layers().contains(MainActivity.this.mGridLayer));
                    }
                });
                showExtendPanel(PANEL_STATE.MAPS, "mapFeaturesMenu", fragment2);
                return true;
            case R.id.actionOverviewRoute:
                if (this.mLocationState == LocationState.NORTH || this.mLocationState == LocationState.TRACK) {
                    this.mLocationState = LocationState.ENABLED;
                    updateLocationDrawable();
                }
                BoundingBox box = new BoundingBox();
                this.mMap.getMapPosition(this.mMapPosition);
                box.extend(this.mMapPosition.getLatitude(), this.mMapPosition.getLongitude());
                MapObject mapObject2 = this.mNavigationService.getWaypoint();
                box.extend(mapObject2.coordinates.getLatitude(), mapObject2.coordinates.getLongitude());
                box.extendBy(0.05d);
                this.mMap.animator().animateTo(box);
                return true;
            case R.id.actionRate:
                Snackbar snackbar = (Snackbar) Snackbar.make(this.mCoordinatorLayout, (int) R.string.msgRateApplication, -2).setAction((int) R.string.iamin, new OnClickListener() {
                    public void onClick(View view) {
                        String packageName = MainActivity.this.getPackageName();
                        try {
                            MainActivity.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + packageName)));
                        } catch (ActivityNotFoundException e) {
                            MainActivity.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
                        }
                    }
                }).addCallback(new BaseCallback<Snackbar>() {
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        Configuration.setRatingActionPerformed();
                    }
                });
                ((TextView) snackbar.getView().findViewById(R.id.snackbar_text)).setMaxLines(99);
                snackbar.show();
                return true;
            case R.id.actionRememberScale:
                HelperUtils.showTargetedAdvice((Activity) this, 8, (int) R.string.advice_remember_scale, this.mPopupAnchor, true);
                removeMarker();
                this.mMap.getMapPosition(this.mMapPosition);
                Configuration.setRememberedScale((float) this.mMapPosition.getScale());
                return true;
            case R.id.actionRememberTilt:
                removeMarker();
                this.mMap.getMapPosition(this.mMapPosition);
                this.mAutoTilt = this.mMapPosition.getTilt();
                Configuration.setAutoTilt(this.mAutoTilt);
                this.mAutoTiltSet = true;
                this.mAutoTiltShouldSet = true;
                return true;
            case R.id.actionSearch:
                args = new Bundle(2);
                if (this.mLocationState == LocationState.DISABLED || this.mLocationService == null) {
                    MapPosition position = this.mMap.getMapPosition();
                    args.putDouble("lat", position.getLatitude());
                    args.putDouble("lon", position.getLongitude());
                } else {
                    Location location = this.mLocationService.getLocation();
                    args.putDouble("lat", location.getLatitude());
                    args.putDouble("lon", location.getLongitude());
                }
                if (this.mFragmentManager.getBackStackEntryCount() > 0) {
                    popAll();
                }
                showExtendPanel(PANEL_STATE.MORE, "search", Fragment.instantiate(this, TextSearchFragment.class.getName(), args));
                return true;
            case R.id.actionSettings:
                args = new Bundle(1);
                args.putBoolean(Settings.ARG_HILLSHADES_AVAILABLE, this.mNativeMapIndex.hasHillshades());
                fragment = Fragment.instantiate(this, Settings.class.getName(), args);
                fragment.setEnterTransition(new Slide(this.mSlideGravity));
                fragment.setReturnTransition(new Slide(this.mSlideGravity));
                ft = this.mFragmentManager.beginTransaction();
                ft.replace(R.id.contentPanel, fragment, "settings");
                ft.addToBackStack("settings");
                ft.commit();
                return true;
            case R.id.actionShareCoordinates:
                removeMarker();
                shareLocation(this.mSelectedPoint, null);
                return true;
            case R.id.actionStopNavigation:
                stopNavigation();
                return true;
            case R.id.actionStyle:
                builder = new Builder(this);
                builder.setTitle(R.string.actionStyle);
                builder.setItems(R.array.mapStyles, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Configuration.setMapStyle(which);
                        MainActivity.this.mBackgroundHandler.post(new Runnable() {
                            public void run() {
                                MainActivity.this.setNightMode(MainActivity.this.mNightModeState == NIGHT_MODE_STATE.NIGHT);
                            }
                        });
                    }
                });
                builder.create().show();
                return true;
            default:
                Intent intent = item.getIntent();
                if (intent == null) {
                    return false;
                }
                startActivity(intent);
                return true;
        }
    }

    public void onLocationChanged() {
        if (this.mLocationState == LocationState.SEARCHING) {
            this.mLocationState = this.mSavedLocationState;
            this.mMap.getEventLayer().setFixOnCenter(true);
            updateLocationDrawable();
            this.mLocationOverlay.setEnabled(true);
            this.mMap.updateMap(true);
        }
        Location location = this.mLocationService.getLocation();
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float bearing = location.getBearing();
        if (bearing < this.mAveragedBearing - 180.0f) {
            this.mAveragedBearing -= 360.0f;
        } else if (this.mAveragedBearing < bearing - 180.0f) {
            this.mAveragedBearing += 360.0f;
        }
        this.mAveragedBearing = (float) movingAverage((double) bearing, (double) this.mAveragedBearing);
        if (this.mAveragedBearing < 0.0f) {
            this.mAveragedBearing += 360.0f;
        }
        if (this.mAveragedBearing >= 360.0f) {
            this.mAveragedBearing -= 360.0f;
        }
        updateGauges();
        if (this.mLocationState == LocationState.NORTH || this.mLocationState == LocationState.TRACK) {
            double offset;
            long time = SystemClock.uptimeMillis();
            this.mMovementAnimationDuration = (int) movingAverage((double) Math.min(1500, time - this.mLastLocationMilliseconds), (double) this.mMovementAnimationDuration);
            this.mMap.getMapPosition(this.mMapPosition);
            boolean rotate = this.mLocationState == LocationState.TRACK && this.mTrackingDelay < time;
            if (rotate) {
                offset = ((double) this.mTrackingOffset) / this.mTrackingOffsetFactor;
                if (this.mAutoTilt > 0.0f && !this.mAutoTiltSet && this.mAutoTiltShouldSet) {
                    this.mMapPosition.setTilt(this.mAutoTilt);
                }
            } else {
                offset = (double) this.mMovingOffset;
            }
            offset /= this.mMapPosition.scale * ((double) Tile.SIZE);
            double rad = Math.toRadians((double) this.mAveragedBearing);
            double dx = offset * Math.sin(rad);
            double dy = offset * Math.cos(rad);
            if (!this.mPositionLocked) {
                this.mMapPosition.setX(MercatorProjection.longitudeToX(lon) + dx);
                this.mMapPosition.setY(MercatorProjection.latitudeToY(lat) - dy);
                this.mMapPosition.setBearing(-this.mAveragedBearing);
                this.mMap.animator().animateTo((long) this.mMovementAnimationDuration, this.mMapPosition, rotate);
            }
        }
        this.mLocationOverlay.setPosition(lat, lon, bearing);
        if (this.mNavigationLayer != null) {
            this.mNavigationLayer.setPosition(lat, lon);
        }
        this.mLastLocationMilliseconds = SystemClock.uptimeMillis();
        for (WeakReference<LocationChangeListener> weakRef : this.mLocationChangeListeners) {
            LocationChangeListener locationChangeListener = (LocationChangeListener) weakRef.get();
            if (locationChangeListener != null) {
                locationChangeListener.onLocationChanged(location);
            }
        }
    }

    public void onGpsStatusChanged() {
        logger.debug("onGpsStatusChanged()");
        if (this.mLocationService.getStatus() == 2) {
            int satellites = this.mLocationService.getSatellites();
            this.mSatellitesText.setText(String.format(Locale.getDefault(), "%d / %s", new Object[]{Integer.valueOf(satellites >> 7), Integer.valueOf(satellites & TransportMediator.KEYCODE_MEDIA_PAUSE)}));
            if (this.mLocationState != LocationState.SEARCHING) {
                this.mSavedLocationState = this.mLocationState;
                this.mLocationState = LocationState.SEARCHING;
                this.mMap.getEventLayer().setFixOnCenter(false);
                this.mLocationOverlay.setEnabled(false);
                updateLocationDrawable();
            }
        }
        updateNavigationUI();
    }

    private void onLocationClicked() {
        boolean z = true;
        switch (this.mLocationState) {
            case DISABLED:
                askForPermission();
                break;
            case SEARCHING:
                this.mLocationState = LocationState.DISABLED;
                disableLocations();
                break;
            case ENABLED:
                this.mLocationState = this.mPreviousLocationState;
                this.mPreviousLocationState = LocationState.NORTH;
                this.mMap.getEventLayer().setFixOnCenter(true);
                this.mMap.getMapPosition(this.mMapPosition);
                this.mMapPosition.setPosition(this.mLocationService.getLocation().getLatitude(), this.mLocationService.getLocation().getLongitude());
                this.mMap.animator().animateTo(500, this.mMapPosition);
                break;
            case NORTH:
                this.mLocationState = LocationState.TRACK;
                this.mMap.getEventLayer().enableRotation(false);
                this.mMap.getEventLayer().setFixOnCenter(true);
                this.mTrackingDelay = SystemClock.uptimeMillis() + 1000;
                if (this.mMapPosition.getTilt() != 0.0f) {
                    z = false;
                }
                this.mAutoTiltShouldSet = z;
                break;
            case TRACK:
                this.mLocationState = LocationState.ENABLED;
                this.mMap.getEventLayer().enableRotation(true);
                this.mMap.getEventLayer().setFixOnCenter(false);
                this.mMap.getMapPosition(this.mMapPosition);
                this.mMapPosition.setBearing(0.0f);
                long duration = 300;
                if (this.mAutoTiltSet) {
                    this.mMapPosition.setTilt(0.0f);
                    this.mAutoTiltSet = false;
                    duration = 500;
                }
                this.mAutoTiltShouldSet = false;
                this.mMap.animator().animateTo(duration, this.mMapPosition);
                break;
        }
        updateLocationDrawable();
    }

    private void onLocationLongClicked() {
        this.mMap.getMapPosition(this.mMapPosition);
        Bundle args = new Bundle(2);
        args.putDouble("lat", this.mMapPosition.getLatitude());
        args.putDouble("lon", this.mMapPosition.getLongitude());
        args.putInt("zoom", this.mMapPosition.getZoomLevel());
        showExtendPanel(PANEL_STATE.LOCATION, "locationInformation", Fragment.instantiate(this, LocationInformation.class.getName(), args));
    }

    private void onRecordClicked() {
        if (!HelperUtils.showTargetedAdvice((Activity) this, 128, (int) R.string.advice_record_track, this.mRecordButton, false)) {
            if (this.mLocationState == LocationState.DISABLED) {
                this.mTrackingState = TRACKING_STATE.PENDING;
                askForPermission();
            } else if (this.mTrackingState == TRACKING_STATE.TRACKING) {
                Track currentTrack = this.mCurrentTrackLayer.getTrack();
                if (currentTrack.points.size() == 0) {
                    disableTracking();
                } else {
                    onTrackDetails(currentTrack, true);
                }
            } else {
                enableTracking();
            }
        }
    }

    private void onRecordLongClicked() {
        Bundle args = new Bundle(1);
        args.putBoolean(DataSourceList.ARG_NATIVE_TRACKS, true);
        showExtendPanel(PANEL_STATE.RECORD, "nativeTrackList", Fragment.instantiate(this, DataSourceList.class.getName(), args));
    }

    private void onPlacesClicked() {
        boolean hasExtraSources = false;
        for (FileDataSource source : this.mData) {
            if (!source.isNativeTrack()) {
                hasExtraSources = true;
                break;
            }
        }
        if (hasExtraSources) {
            Bundle args = new Bundle(1);
            args.putBoolean(DataSourceList.ARG_NATIVE_TRACKS, false);
            showExtendPanel(PANEL_STATE.PLACES, "dataSourceList", Fragment.instantiate(this, DataSourceList.class.getName(), args));
            return;
        }
        args = new Bundle(3);
        if (this.mLocationState == LocationState.DISABLED || this.mLocationService == null) {
            MapPosition position = this.mMap.getMapPosition();
            args.putDouble("lat", position.getLatitude());
            args.putDouble("lon", position.getLongitude());
        } else {
            Location location = this.mLocationService.getLocation();
            args.putDouble("lat", location.getLatitude());
            args.putDouble("lon", location.getLongitude());
        }
        args.putBoolean("msg", true);
        DataList fragment = (DataList) Fragment.instantiate(this, DataList.class.getName(), args);
        fragment.setDataSource(this.mWaypointDbDataSource);
        showExtendPanel(PANEL_STATE.PLACES, "dataList", fragment);
    }

    private void onPlacesLongClicked() {
        GeoPoint geoPoint;
        if (this.mLocationState == LocationState.NORTH || this.mLocationState == LocationState.TRACK) {
            Point point = this.mLocationOverlay.getPosition();
            geoPoint = new GeoPoint(MercatorProjection.toLatitude(point.y), MercatorProjection.toLongitude(point.x));
        } else {
            geoPoint = this.mMap.getMapPosition().getGeoPoint();
        }
        onWaypointCreate(geoPoint, getString(R.string.waypoint_name, new Object[]{Integer.valueOf(Configuration.getPointsCounter())}), false, true);
    }

    private void onMapsClicked() {
        this.mMap.getMapPosition(this.mMapPosition);
        Bundle args = new Bundle(5);
        args.putDouble("lat", this.mMapPosition.getLatitude());
        args.putDouble("lon", this.mMapPosition.getLongitude());
        args.putInt("zoom", this.mMapPosition.getZoomLevel());
        args.putBoolean(MapList.ARG_HIDE_OBJECTS, this.mHideMapObjects);
        args.putInt(MapList.ARG_TRANSPARENCY, this.mBitmapMapTransparency);
        MapList fragment = (MapList) Fragment.instantiate(this, MapList.class.getName(), args);
        fragment.setMaps(this.mMapIndex.getMaps(), this.mBitmapLayerMap);
        showExtendPanel(PANEL_STATE.MAPS, "mapsList", fragment);
    }

    private void onMapsLongClicked() {
        PanelMenuFragment fragment = (PanelMenuFragment) Fragment.instantiate(this, PanelMenuFragment.class.getName());
        fragment.setMenu(R.menu.menu_map, new OnPrepareMenuListener() {
            public void onPrepareMenu(PanelMenu menu) {
                Resources resources = MainActivity.this.getResources();
                ((TextView) menu.findItem(R.id.actionNightMode).getActionView()).setText(resources.getStringArray(R.array.night_mode_array)[MainActivity.this.mNightModeState.ordinal()]);
                ((TextView) menu.findItem(R.id.actionStyle).getActionView()).setText(resources.getStringArray(R.array.mapStyles)[Configuration.getMapStyle()]);
                ((TextView) menu.findItem(R.id.actionFontSize).getActionView()).setText(resources.getStringArray(R.array.font_size_array)[Configuration.getMapFontSize()]);
                ((TextView) menu.findItem(R.id.actionLanguage).getActionView()).setText(Configuration.getLanguage());
                menu.findItem(R.id.actionAutoTilt).setChecked(MainActivity.this.mAutoTilt != -1.0f);
                menu.removeItem(R.id.actionNightMode);
            }
        });
        showExtendPanel(PANEL_STATE.MAPS, "mapMenu", fragment);
    }

    private void onMoreClicked() {
        if (this.mLocationButton.getVisibility() == 0) {
            PanelMenuFragment fragment = (PanelMenuFragment) Fragment.instantiate(this, PanelMenuFragment.class.getName());
            fragment.setMenu(R.menu.menu_main, new OnPrepareMenuListener() {
                public void onPrepareMenu(PanelMenu menu) {
                    Resources resources = MainActivity.this.getResources();
                    MenuItem item = menu.findItem(R.id.actionActivity);
                    String[] activities = resources.getStringArray(R.array.activities);
                    int activity = Configuration.getActivity();
                    if (activity > 0) {
                        ((TextView) item.getActionView()).setText(activities[activity]);
                    }
                    menu.findItem(R.id.actionHideSystemUI).setChecked(Configuration.getHideSystemUI());
                    if (Configuration.ratingActionPerformed() || (Configuration.getRunningTime() < 120 && MainActivity.this.mWaypointDbDataSource.getWaypointsCount() < 3 && MainActivity.this.mData.size() == 0 && MainActivity.this.mMapIndex.getMaps().size() == 0)) {
                        menu.removeItem(R.id.actionRate);
                    }
                    if (MainActivity.this.mGaugePanel.hasVisibleGauges() || !(MainActivity.this.mLocationState == LocationState.NORTH || MainActivity.this.mLocationState == LocationState.TRACK)) {
                        menu.removeItem(R.id.actionAddGauge);
                    }
                    java.util.Map<String, Pair<Drawable, Intent>> tools = MainActivity.this.getPluginsTools();
                    String[] toolNames = (String[]) tools.keySet().toArray(new String[0]);
                    Arrays.sort(toolNames, Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER));
                    for (String toolName : toolNames) {
                        menu.add(-1, 0, toolName).setIntent((Intent) ((Pair) tools.get(toolName)).second);
                    }
                }
            });
            showExtendPanel(PANEL_STATE.MORE, "panelMenu", fragment);
            return;
        }
        showActionPanel(true, true);
    }

    private void onMoreLongClicked() {
        boolean show = this.mLocationButton.getVisibility() == 4;
        showActionPanel(show, true);
        if (!show && !Configuration.getHideSystemUI()) {
            hideSystemUI();
        }
    }

    private void onMapDownloadClicked() {
        this.mMapDownloadButton.setVisibility(8);
        startMapSelection(false);
    }

    public void onCompassClicked(View view) {
        if (this.mLocationState == LocationState.TRACK) {
            this.mLocationState = LocationState.NORTH;
            updateLocationDrawable();
            this.mMap.getEventLayer().enableRotation(true);
        }
        this.mMap.getMapPosition(this.mMapPosition);
        this.mMapPosition.setBearing(0.0f);
        this.mMap.animator().animateTo(300, this.mMapPosition);
    }

    public boolean onItemSingleTapUp(int index, MarkerItem item) {
        Object uid = item.getUid();
        if (uid != null) {
            onWaypointDetails((Waypoint) uid, false);
        }
        return uid != null;
    }

    public boolean onItemLongPress(int index, MarkerItem item) {
        if (this.mLocationState != LocationState.DISABLED && this.mLocationState != LocationState.ENABLED) {
            return false;
        }
        Waypoint uid = item.getUid();
        if (uid == null || !uid.locked) {
            this.mActiveMarker = item;
            Point point = new Point();
            this.mMap.viewport().toScreenPoint(item.getPoint(), point);
            this.deltaX = (float) (((double) this.downX) - point.x);
            this.deltaY = (float) (((double) this.downY) - point.y);
            this.mMap.getEventLayer().enableMove(false);
            this.mMap.animator().animateTo(250, this.mMap.viewport().fromScreenPoint((float) (this.mMap.getWidth() / 2), ((float) (this.mMap.getHeight() / 2)) + (Viewport.VIEW_DISTANCE * this.mFingerTipSize)), 1.0d, true);
            return true;
        }
        Toast.makeText(this, R.string.msgWaypointLocked, 0).show();
        return true;
    }

    private void enableLocations() {
        this.mIsLocationBound = bindService(new Intent(getApplicationContext(), LocationService.class), this.mLocationConnection, 1);
        this.mLocationState = LocationState.SEARCHING;
        if (this.mSavedLocationState == LocationState.DISABLED) {
            this.mSavedLocationState = this.mPreviousLocationState;
            this.mPreviousLocationState = LocationState.NORTH;
        }
        if (this.mTrackingState == TRACKING_STATE.PENDING) {
            enableTracking();
        }
        updateLocationDrawable();
    }

    public void disableLocations() {
        if (this.mLocationService != null) {
            this.mLocationService.unregisterLocationCallback(this);
            this.mLocationService.setProgressListener(null);
        }
        if (this.mIsLocationBound) {
            unbindService(this.mLocationConnection);
            this.mIsLocationBound = false;
            this.mLocationOverlay.setEnabled(false);
            this.mMap.updateMap(true);
        }
        this.mLocationState = LocationState.DISABLED;
        updateLocationDrawable();
    }

    public void setMapLocation(@NonNull GeoPoint point) {
        if (this.mSavedLocationState == LocationState.NORTH || this.mSavedLocationState == LocationState.TRACK) {
            this.mSavedLocationState = LocationState.ENABLED;
        }
        if (this.mLocationState == LocationState.NORTH || this.mLocationState == LocationState.TRACK) {
            this.mLocationState = LocationState.ENABLED;
            updateLocationDrawable();
        }
        if (this.mMap.getMapPosition().scale > 256.0d) {
            this.mMap.animator().animateTo(point);
        } else {
            this.mMap.animator().animateTo(500, point, 32768.0d, false);
        }
    }

    public void showMarker(@NonNull GeoPoint point, String name) {
        removeMarker();
        this.mMarker = new MarkerItem(name, null, point);
        this.mMarker.setMarker(new MarkerSymbol(new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, R.drawable.round_marker, this.mColorAccent)), HotspotPlace.CENTER));
        this.mMarkerLayer.addItem(this.mMarker);
        this.mMap.updateMap(true);
    }

    public void removeMarker() {
        if (this.mMarker != null) {
            this.mMarkerLayer.removeItem(this.mMarker);
            this.mMap.updateMap(true);
            this.mMarker.getMarker().getBitmap().recycle();
            this.mMarker = null;
        }
    }

    private void enableNavigation() {
        logger.debug("enableNavigation");
        this.mIsNavigationBound = bindService(new Intent(getApplicationContext(), NavigationService.class), this.mNavigationConnection, 1);
    }

    private void disableNavigation() {
        logger.debug("disableNavigation");
        if (this.mIsNavigationBound) {
            unbindService(this.mNavigationConnection);
            this.mIsNavigationBound = false;
        }
        updateNavigationUI();
    }

    private void startNavigation(MapObject mapObject) {
        enableNavigation();
        Intent i = new Intent(this, NavigationService.class).setAction(BaseNavigationService.NAVIGATE_TO_POINT);
        i.putExtra("name", mapObject.name);
        i.putExtra("latitude", mapObject.coordinates.getLatitude());
        i.putExtra("longitude", mapObject.coordinates.getLongitude());
        i.putExtra(BaseNavigationService.EXTRA_PROXIMITY, mapObject.proximity);
        startService(i);
        if (this.mLocationState == LocationState.DISABLED) {
            askForPermission();
        }
    }

    private void startNavigation(long id) {
        if (MapTrek.getMapObject(id) != null) {
            enableNavigation();
            Intent i = new Intent(this, NavigationService.class).setAction(BaseNavigationService.NAVIGATE_TO_OBJECT);
            i.putExtra("id", id);
            startService(i);
            if (this.mLocationState == LocationState.DISABLED) {
                askForPermission();
            }
        }
    }

    public void stopNavigation() {
        startService(new Intent(this, NavigationService.class).setAction(BaseNavigationService.STOP_NAVIGATION));
    }

    private void enableTracking() {
        startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.ENABLE_TRACK));
        if (this.mCurrentTrackLayer == null) {
            this.mCurrentTrackLayer = new CurrentTrackLayer(this.mMap, getApplicationContext());
            this.mMap.layers().add(this.mCurrentTrackLayer, 7);
            this.mMap.updateMap(true);
        }
        this.mTrackingState = TRACKING_STATE.TRACKING;
        updateLocationDrawable();
    }

    public void disableTracking() {
        startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.DISABLE_TRACK));
        this.mMap.layers().remove(this.mCurrentTrackLayer);
        if (this.mCurrentTrackLayer != null) {
            this.mCurrentTrackLayer.onDetach();
        }
        this.mCurrentTrackLayer = null;
        this.mMap.updateMap(true);
        this.mTrackingState = TRACKING_STATE.DISABLED;
        updateLocationDrawable();
    }

    public void navigateTo(@NonNull GeoPoint coordinates, @Nullable String name) {
        startNavigation(new MapObject(name, coordinates));
    }

    public boolean isNavigatingTo(@NonNull GeoPoint coordinates) {
        if (this.mNavigationService != null && this.mNavigationService.isNavigating()) {
            return this.mNavigationService.getWaypoint().coordinates.equals(coordinates);
        }
        return false;
    }

    public void addLocationStateChangeListener(LocationStateChangeListener listener) {
        this.mLocationStateChangeListeners.add(new WeakReference(listener));
        listener.onLocationStateChanged(this.mLocationState);
    }

    public void removeLocationStateChangeListener(LocationStateChangeListener listener) {
        Iterator<WeakReference<LocationStateChangeListener>> iterator = this.mLocationStateChangeListeners.iterator();
        while (iterator.hasNext()) {
            if (((WeakReference) iterator.next()).get() == listener) {
                iterator.remove();
            }
        }
    }

    public void addLocationChangeListener(LocationChangeListener listener) {
        this.mLocationChangeListeners.add(new WeakReference(listener));
    }

    public void removeLocationChangeListener(LocationChangeListener listener) {
        Iterator<WeakReference<LocationChangeListener>> iterator = this.mLocationChangeListeners.iterator();
        while (iterator.hasNext()) {
            if (((WeakReference) iterator.next()).get() == listener) {
                iterator.remove();
            }
        }
    }

    public void onInputEvent(Event e, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == 0) {
            this.downX = motionEvent.getX() - ((float) (this.mMap.getWidth() / 2));
            this.downY = motionEvent.getY() - ((float) (this.mMap.getHeight() / 2));
        }
        if (this.mActiveMarker != null) {
            if (action == 1 || action == 3) {
                Waypoint waypoint = (Waypoint) this.mActiveMarker.getUid();
                waypoint.setCoordinates(this.mActiveMarker.getPoint());
                onWaypointSave(waypoint);
                this.mActiveMarker = null;
                this.mMap.animator().animateTo(250, this.mMap.viewport().fromScreenPoint((float) (this.mMap.getWidth() / 2), ((float) (this.mMap.getHeight() / 2)) - this.mFingerTipSize), 1.0d, true);
                this.mMap.getEventLayer().enableMove(true);
            } else if (action == 2) {
                this.mActiveMarker.setPoint(this.mMap.viewport().fromScreenPoint(motionEvent.getX() - this.deltaX, (motionEvent.getY() - this.deltaY) - (Viewport.VIEW_DISTANCE * this.mFingerTipSize)));
                this.mMarkerLayer.updateItems();
                this.mMap.updateMap(true);
            }
        }
    }

    public void onMapEvent(Event e, MapPosition mapPosition) {
        if (e == Map.POSITION_EVENT) {
            this.mTrackingOffsetFactor = Math.cos(Math.toRadians((double) mapPosition.tilt) * 0.85d);
            if (this.mCompassView.getVisibility() == 8 && mapPosition.bearing != 0.0f && this.mLocationState != LocationState.TRACK && Math.abs(mapPosition.bearing) < 1.5f) {
                mapPosition.setBearing(0.0f);
                this.mMap.setMapPosition(mapPosition);
            }
            adjustCompass(mapPosition.bearing);
            if (this.mAutoTiltSet) {
                if (this.mAutoTilt != mapPosition.tilt) {
                    this.mAutoTiltSet = false;
                    this.mAutoTiltShouldSet = false;
                }
            } else if (this.mAutoTiltShouldSet) {
                this.mAutoTiltSet = mapPosition.tilt == this.mAutoTilt;
            }
        }
        if (e == Map.MOVE_EVENT) {
            if (this.mLocationState == LocationState.NORTH || this.mLocationState == LocationState.TRACK) {
                this.mPreviousLocationState = this.mLocationState;
                this.mLocationState = LocationState.ENABLED;
                updateLocationDrawable();
            }
            if (this.mFirstMove) {
                this.mFirstMove = false;
                this.mPopupAnchor.setX(((float) this.mMap.getWidth()) - (32.0f * MapTrek.density));
                this.mPopupAnchor.setY(((float) this.mStatusBarHeight) + (Viewport.VIEW_FAR * MapTrek.density));
                HelperUtils.showTargetedAdvice((Activity) this, 8192, (int) R.string.advice_lock_map_position, this.mPopupAnchor, (int) R.drawable.ic_volume_down);
            }
        }
        if (this.mMapDownloadButton.getVisibility() == 8) {
            return;
        }
        if (mapPosition.zoomLevel < 8) {
            this.mMapDownloadButton.setVisibility(8);
            this.mMapDownloadButton.setTag(null);
        } else if (e == Map.MOVE_EVENT) {
            Message m = Message.obtain(this.mMainHandler, new Runnable() {
                public void run() {
                    MainActivity.this.mMapDownloadButton.setVisibility(8);
                    MainActivity.this.mMapDownloadButton.setTag(null);
                }
            });
            m.what = R.id.msgRemoveMapDownloadButton;
            this.mMainHandler.sendMessageDelayed(m, 1000);
        }
    }

    public boolean onGesture(Gesture gesture, MotionEvent event) {
        this.mMap.getMapPosition(this.mMapPosition);
        if (gesture == Gesture.LONG_PRESS) {
            if (!this.mMap.getEventLayer().moveEnabled()) {
                return true;
            }
            this.mPopupAnchor.setX(event.getX() + this.mFingerTipSize);
            this.mPopupAnchor.setY(event.getY() - this.mFingerTipSize);
            this.mSelectedPoint = this.mMap.viewport().fromScreenPoint(event.getX(), event.getY());
            showMarker(this.mSelectedPoint, null);
            PopupMenu popup = new PopupMenu(this, this.mPopupAnchor);
            popup.inflate(R.menu.context_menu_map);
            Menu menu = popup.getMenu();
            if (((int) Configuration.getRememberedScale()) == ((int) this.mMapPosition.getScale())) {
                menu.removeItem(R.id.actionRememberScale);
            }
            if (this.mLocationState != LocationState.TRACK || this.mAutoTilt == -1.0f || MathUtils.equals(this.mAutoTilt, this.mMapPosition.getTilt())) {
                menu.removeItem(R.id.actionRememberTilt);
            }
            popup.setOnMenuItemClickListener(this);
            popup.setOnDismissListener(new OnDismissListener() {
                public void onDismiss(PopupMenu menu) {
                    MainActivity.this.removeMarker();
                }
            });
            popup.show();
            return true;
        } else if (gesture != Gesture.TRIPLE_TAP) {
            return false;
        } else {
            this.mMap.animator().animateZoom(300, ((double) Configuration.getRememberedScale()) / this.mMapPosition.getScale(), 0.0f, 0.0f);
            return true;
        }
    }

    private void adjustCompass(float bearing) {
        if (this.mCompassView.getRotation() != bearing) {
            this.mCompassView.setRotation(bearing);
            if (Math.abs(bearing) < Viewport.VIEW_NEAR && this.mCompassView.getAlpha() == Viewport.VIEW_NEAR) {
                this.mCompassView.animate().alpha(0.0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        MainActivity.this.mCompassView.setVisibility(8);
                    }
                });
            } else if (this.mCompassView.getVisibility() == 8) {
                this.mCompassView.setAlpha(0.0f);
                this.mCompassView.setVisibility(0);
                this.mCompassView.animate().alpha(Viewport.VIEW_NEAR).setDuration(500).setListener(null);
            }
        }
    }

    private void adjustNavigationArrow(float turn) {
        if (this.mNavigationArrowView.getRotation() != turn) {
            this.mNavigationArrowView.setRotation(turn);
        }
    }

    private void showNavigationMenu() {
        PopupMenu popup = new PopupMenu(this, this.mMapButtonHolder);
        popup.inflate(R.menu.context_menu_navigation);
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    private void updateLocationDrawable() {
        logger.debug("updateLocationDrawable()");
        if (this.mRecordButton.getTag() != this.mTrackingState) {
            this.mRecordButton.getDrawable().setTint(this.mTrackingState == TRACKING_STATE.TRACKING ? this.mColorAccent : this.mColorPrimaryDark);
            this.mRecordButton.setTag(this.mTrackingState);
        }
        if (this.mLocationButton.getTag() != this.mLocationState) {
            if (this.mLocationButton.getTag() == LocationState.SEARCHING) {
                this.mLocationButton.clearAnimation();
                this.mSatellitesText.animate().translationY(-200.0f);
            }
            final ViewPropertyAnimator gaugePanelAnimator = this.mGaugePanel.animate();
            gaugePanelAnimator.setListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (MainActivity.this.mLocationState == LocationState.NORTH) {
                        HelperUtils.showTargetedAdvice(MainActivity.this, 4, (int) R.string.advice_more_gauges, MainActivity.this.mGaugePanel, true);
                    }
                    if (MainActivity.this.mLocationState == LocationState.SEARCHING) {
                        MainActivity.this.mSatellitesText.animate().translationY(Viewport.VIEW_FAR);
                    }
                    gaugePanelAnimator.setListener(null);
                    MainActivity.this.updateMapViewArea();
                }
            });
            switch (this.mLocationState) {
                case DISABLED:
                    this.mNavigationNorthDrawable.setTint(this.mColorPrimaryDark);
                    this.mLocationButton.setImageDrawable(this.mNavigationNorthDrawable);
                    this.mCrosshairLayer.setEnabled(true);
                    if (this.mGaugePanel.getWidth() > 0) {
                        gaugePanelAnimator.translationX((float) (-this.mGaugePanel.getWidth()));
                        this.mGaugePanel.onVisibilityChanged(false);
                        break;
                    }
                    break;
                case SEARCHING:
                    this.mLocationSearchingDrawable.setTint(this.mColorAccent);
                    this.mLocationButton.setImageDrawable(this.mLocationSearchingDrawable);
                    Animation rotation = new RotateAnimation(0.0f, 360.0f, 1, 0.5f, 1, 0.5f);
                    rotation.setInterpolator(new LinearInterpolator());
                    rotation.setRepeatCount(-1);
                    rotation.setDuration(1000);
                    this.mLocationButton.startAnimation(rotation);
                    if (this.mGaugePanel.getVisibility() != 4) {
                        gaugePanelAnimator.translationX((float) (-this.mGaugePanel.getWidth()));
                        this.mGaugePanel.onVisibilityChanged(false);
                        break;
                    }
                    this.mSatellitesText.animate().translationY(Viewport.VIEW_FAR);
                    break;
                case ENABLED:
                    this.mMyLocationDrawable.setTint(this.mColorPrimaryDark);
                    this.mLocationButton.setImageDrawable(this.mMyLocationDrawable);
                    this.mCrosshairLayer.setEnabled(true);
                    gaugePanelAnimator.translationX((float) (-this.mGaugePanel.getWidth()));
                    this.mGaugePanel.onVisibilityChanged(false);
                    break;
                case NORTH:
                    this.mNavigationNorthDrawable.setTint(this.mColorAccent);
                    this.mLocationButton.setImageDrawable(this.mNavigationNorthDrawable);
                    this.mCrosshairLayer.setEnabled(false);
                    gaugePanelAnimator.translationX(0.0f);
                    this.mGaugePanel.onVisibilityChanged(true);
                    break;
                case TRACK:
                    this.mNavigationTrackDrawable.setTint(this.mColorAccent);
                    this.mLocationButton.setImageDrawable(this.mNavigationTrackDrawable);
                    this.mCrosshairLayer.setEnabled(false);
                    gaugePanelAnimator.translationX(0.0f);
                    this.mGaugePanel.onVisibilityChanged(true);
                    break;
            }
            this.mLocationButton.setTag(this.mLocationState);
            for (WeakReference<LocationStateChangeListener> weakRef : this.mLocationStateChangeListeners) {
                LocationStateChangeListener locationStateChangeListener = (LocationStateChangeListener) weakRef.get();
                if (locationStateChangeListener != null) {
                    locationStateChangeListener.onLocationStateChanged(this.mLocationState);
                }
            }
        }
    }

    private void updateGauges() {
        Location location = this.mLocationService.getLocation();
        this.mGaugePanel.setValue(1, location.getSpeed());
        this.mGaugePanel.setValue(2, location.getBearing());
        this.mGaugePanel.setValue(4, (float) location.getAltitude());
    }

    private void updateNavigationUI() {
        boolean enabled;
        logger.debug("updateNavigationUI()");
        if (this.mLocationService == null || this.mLocationService.getStatus() != 3 || this.mNavigationService == null || !this.mNavigationService.isNavigating()) {
            enabled = false;
        } else {
            enabled = true;
        }
        boolean changed = this.mGaugePanel.setNavigationMode(enabled);
        if (enabled) {
            if (this.mNavigationArrowView.getVisibility() == 8) {
                this.mNavigationArrowView.setAlpha(0.0f);
                this.mNavigationArrowView.setVisibility(0);
                this.mNavigationArrowView.animate().alpha(Viewport.VIEW_NEAR).setDuration(500).setListener(null);
            }
            GeoPoint destination = this.mNavigationService.getWaypoint().coordinates;
            if (this.mNavigationLayer == null) {
                this.mNavigationLayer = new NavigationLayer(this.mMap, 1728052992, Viewport.VIEW_FAR);
                this.mNavigationLayer.setDestination(destination);
                Point point = this.mLocationOverlay.getPosition();
                this.mNavigationLayer.setPosition(MercatorProjection.toLatitude(point.y), MercatorProjection.toLongitude(point.x));
                this.mMap.layers().add(this.mNavigationLayer, 9);
            } else if (!destination.equals(this.mNavigationLayer.getDestination())) {
                this.mNavigationLayer.setDestination(destination);
            }
        } else {
            if (this.mNavigationArrowView.getAlpha() == Viewport.VIEW_NEAR) {
                this.mNavigationArrowView.animate().alpha(0.0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        MainActivity.this.mNavigationArrowView.setVisibility(8);
                    }
                });
            }
            if (this.mNavigationLayer != null) {
                this.mMap.layers().remove(this.mNavigationLayer);
                this.mNavigationLayer = null;
            }
        }
        if (changed) {
            updateMapViewArea();
        }
    }

    public void showMarkerInformation(@NonNull GeoPoint point, @Nullable String name) {
        if (this.mFragmentManager.getBackStackEntryCount() > 0) {
            popAll();
        }
        Bundle args = new Bundle(3);
        args.putDouble("latitude", point.getLatitude());
        args.putDouble("longitude", point.getLongitude());
        args.putString("name", name);
        Fragment fragment = Fragment.instantiate(this, MarkerInformation.class.getName(), args);
        fragment.setEnterTransition(new Slide());
        FragmentTransaction ft = this.mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "markerInformation");
        ft.addToBackStack("markerInformation");
        ft.commit();
        updateMapViewArea();
    }

    private void onWaypointProperties(Waypoint waypoint) {
        this.mEditedWaypoint = waypoint;
        Bundle args = new Bundle(2);
        args.putString("name", this.mEditedWaypoint.name);
        args.putInt("color", this.mEditedWaypoint.style.color);
        Fragment fragment = Fragment.instantiate(this, WaypointProperties.class.getName(), args);
        fragment.setEnterTransition(new Fade());
        FragmentTransaction ft = this.mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "waypointProperties");
        ft.addToBackStack("waypointProperties");
        ft.commit();
        updateMapViewArea();
    }

    public void onWaypointCreate(GeoPoint point, String name, boolean locked, boolean customize) {
        final Waypoint waypoint = new Waypoint(name, point.getLatitude(), point.getLongitude());
        waypoint.date = new Date();
        waypoint.locked = locked;
        this.mWaypointDbDataSource.saveWaypoint(waypoint);
        this.mMarkerLayer.addItem(new MarkerItem(waypoint, name, null, point));
        this.mMap.updateMap(true);
        if (customize) {
            Snackbar.make(this.mCoordinatorLayout, (int) R.string.msgWaypointSaved, 0).setAction((int) R.string.actionCustomize, new OnClickListener() {
                public void onClick(View view) {
                    MainActivity.this.onWaypointProperties(waypoint);
                }
            }).show();
        }
    }

    public void onWaypointView(Waypoint waypoint) {
        setMapLocation(waypoint.coordinates);
    }

    public void onWaypointFocus(Waypoint waypoint) {
        if (waypoint != null) {
            this.mMarkerLayer.setFocus(this.mMarkerLayer.getByUid(waypoint), waypoint.style.color);
        } else {
            this.mMarkerLayer.setFocus(null);
        }
    }

    public void onWaypointDetails(Waypoint waypoint, boolean fromList) {
        Bundle args = new Bundle(3);
        args.putBoolean(WaypointInformation.ARG_DETAILS, fromList);
        if (fromList || this.mLocationState != LocationState.DISABLED) {
            if (this.mLocationState == LocationState.DISABLED || this.mLocationService == null) {
                MapPosition position = this.mMap.getMapPosition();
                args.putDouble("lat", position.getLatitude());
                args.putDouble("lon", position.getLongitude());
            } else {
                Location location = this.mLocationService.getLocation();
                args.putDouble("lat", location.getLatitude());
                args.putDouble("lon", location.getLongitude());
            }
        }
        Fragment fragment = this.mFragmentManager.findFragmentByTag("waypointInformation");
        if (fragment == null) {
            fragment = Fragment.instantiate(this, WaypointInformation.class.getName(), args);
            Slide slide = new Slide(80);
            slide.setDuration((long) getResources().getInteger(17694720));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = this.mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "waypointInformation");
            ft.addToBackStack("waypointInformation");
            ft.commit();
            updateMapViewArea();
        }
        ((WaypointInformation) fragment).setWaypoint(waypoint);
        this.mExtendPanel.setForeground(getDrawable(R.drawable.dim));
        this.mExtendPanel.getForeground().setAlpha(0);
        ObjectAnimator anim = ObjectAnimator.ofInt(this.mExtendPanel.getForeground(), "alpha", new int[]{0, 255});
        anim.setDuration(500);
        anim.start();
    }

    public void onWaypointNavigate(Waypoint waypoint) {
        startNavigation((MapObject) waypoint);
    }

    public void onWaypointShare(Waypoint waypoint) {
        shareLocation(waypoint.coordinates, waypoint.name);
    }

    public void onWaypointSave(final Waypoint waypoint) {
        if (waypoint.source instanceof WaypointDbDataSource) {
            this.mWaypointDbDataSource.saveWaypoint(waypoint);
        } else {
            Manager.save(getApplicationContext(), (FileDataSource) waypoint.source, new OnSaveListener() {
                public void onSaved(FileDataSource source) {
                    MainActivity.this.mMainHandler.post(new Runnable() {
                        public void run() {
                            waypoint.source.notifyListeners();
                        }
                    });
                }

                public void onError(FileDataSource source, Exception e) {
                    HelperUtils.showSaveError(MainActivity.this, MainActivity.this.mCoordinatorLayout, e);
                }
            }, this.mProgressHandler);
        }
        removeWaypointMarker(waypoint);
        addWaypointMarker(waypoint);
        this.mMap.updateMap(true);
    }

    public void onWaypointDelete(final Waypoint waypoint) {
        removeWaypointMarker(waypoint);
        this.mMap.updateMap(true);
        Snackbar.make(this.mCoordinatorLayout, (int) R.string.msgWaypointDeleted, 0).setCallback(new Callback() {
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                if (event != 1) {
                    if (waypoint.source instanceof WaypointDbDataSource) {
                        MainActivity.this.mWaypointDbDataSource.deleteWaypoint(waypoint);
                    } else {
                        ((FileDataSource) waypoint.source).waypoints.remove(waypoint);
                        Manager.save(MainActivity.this.getApplicationContext(), (FileDataSource) waypoint.source, new OnSaveListener() {
                            public void onSaved(FileDataSource source) {
                                MainActivity.this.mMainHandler.post(new Runnable() {
                                    public void run() {
                                        waypoint.source.notifyListeners();
                                    }
                                });
                            }

                            public void onError(FileDataSource source, Exception e) {
                                HelperUtils.showSaveError(MainActivity.this, MainActivity.this.mCoordinatorLayout, e);
                            }
                        }, MainActivity.this.mProgressHandler);
                    }
                    MainActivity.this.mTotalDataItems = MainActivity.this.mTotalDataItems - 1;
                }
            }
        }).setAction((int) R.string.actionUndo, new OnClickListener() {
            public void onClick(View view) {
                MainActivity.this.addWaypointMarker(waypoint);
                MainActivity.this.mMap.updateMap(true);
            }
        }).show();
    }

    public void onWaypointsDelete(final Set<Waypoint> waypoints) {
        for (Waypoint waypoint : waypoints) {
            removeWaypointMarker(waypoint);
        }
        this.mMap.updateMap(true);
        int count = waypoints.size();
        Snackbar.make(this.mCoordinatorLayout, getResources().getQuantityString(R.plurals.waypointsDeleted, count, new Object[]{Integer.valueOf(count)}), 0).setCallback(new Callback() {
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                if (event != 1) {
                    HashSet<FileDataSource> sources = new HashSet();
                    for (Waypoint waypoint : waypoints) {
                        if (waypoint.source instanceof WaypointDbDataSource) {
                            MainActivity.this.mWaypointDbDataSource.deleteWaypoint(waypoint);
                        } else {
                            ((FileDataSource) waypoint.source).waypoints.remove(waypoint);
                            sources.add((FileDataSource) waypoint.source);
                        }
                        MainActivity.this.mTotalDataItems = MainActivity.this.mTotalDataItems - 1;
                    }
                    Iterator it = sources.iterator();
                    while (it.hasNext()) {
                        Manager.save(MainActivity.this.getApplicationContext(), (FileDataSource) it.next(), new OnSaveListener() {
                            public void onSaved(final FileDataSource source) {
                                MainActivity.this.mMainHandler.post(new Runnable() {
                                    public void run() {
                                        source.notifyListeners();
                                    }
                                });
                            }

                            public void onError(FileDataSource source, Exception e) {
                                HelperUtils.showSaveError(MainActivity.this, MainActivity.this.mCoordinatorLayout, e);
                            }
                        }, MainActivity.this.mProgressHandler);
                    }
                }
            }
        }).setAction((int) R.string.actionUndo, new OnClickListener() {
            public void onClick(View view) {
                for (Waypoint waypoint : waypoints) {
                    MainActivity.this.addWaypointMarker(waypoint);
                }
                MainActivity.this.mMap.updateMap(true);
            }
        }).show();
    }

    public void onWaypointPropertiesChanged(String name, int color) {
        boolean colorChanged = this.mEditedWaypoint.style.color != color;
        this.mEditedWaypoint.name = name;
        this.mEditedWaypoint.style.color = color;
        MarkerItem item = this.mMarkerLayer.getByUid(this.mEditedWaypoint);
        item.title = name;
        if (colorChanged) {
            item.setMarker(new MarkerSymbol(new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, color)), HotspotPlace.BOTTOM_CENTER));
        }
        this.mMarkerLayer.updateItems();
        this.mMap.updateMap(true);
        this.mWaypointDbDataSource.saveWaypoint(this.mEditedWaypoint);
        this.mEditedWaypoint = null;
    }

    private void onTrackProperties(String path) {
        logger.debug("onTrackProperties({})", (Object) path);
        for (FileDataSource source : this.mData) {
            if (source.path.equals(path)) {
                this.mEditedTrack = (Track) source.tracks.get(0);
                break;
            }
        }
        if (this.mEditedTrack != null) {
            Bundle args = new Bundle(2);
            args.putString("name", this.mEditedTrack.name);
            args.putInt("color", this.mEditedTrack.style.color);
            Fragment fragment = Fragment.instantiate(this, TrackProperties.class.getName(), args);
            fragment.setEnterTransition(new Fade());
            FragmentTransaction ft = this.mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "trackProperties");
            ft.addToBackStack("trackProperties");
            ft.commit();
            updateMapViewArea();
        }
    }

    public void onTrackPropertiesChanged(String name, int color) {
        this.mEditedTrack.name = name;
        this.mEditedTrack.style.color = color;
        onTrackSave(this.mEditedTrack);
        this.mEditedTrack = null;
    }

    public void onTrackView(Track track) {
        if (this.mLocationState == LocationState.NORTH || this.mLocationState == LocationState.TRACK) {
            this.mLocationState = LocationState.ENABLED;
            updateLocationDrawable();
        }
        BoundingBox box = track.getBoundingBox();
        box.extendBy(0.05d);
        this.mMap.animator().animateTo(box);
    }

    public void onTrackDetails(Track track) {
        onTrackDetails(track, false);
    }

    private void onTrackDetails(Track track, boolean current) {
        Fragment fragment = this.mFragmentManager.findFragmentByTag("trackInformation");
        if (fragment == null) {
            fragment = Fragment.instantiate(this, TrackInformation.class.getName());
            Slide slide = new Slide(this.mSlideGravity);
            slide.setDuration((long) getResources().getInteger(17694720));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = this.mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "trackInformation");
            ft.addToBackStack("trackInformation");
            ft.commit();
            updateMapViewArea();
        }
        ((TrackInformation) fragment).setTrack(track, current);
        this.mExtendPanel.setForeground(getDrawable(R.drawable.dim));
        this.mExtendPanel.getForeground().setAlpha(0);
        ObjectAnimator anim = ObjectAnimator.ofInt(this.mExtendPanel.getForeground(), "alpha", new int[]{0, 255});
        anim.setDuration(500);
        anim.start();
    }

    public void onTrackShare(final Track track) {
        final AtomicInteger selected = new AtomicInteger(0);
        Builder builder = new Builder(this);
        builder.setTitle(R.string.title_select_format);
        builder.setSingleChoiceItems(R.array.track_format_array, selected.get(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                selected.set(which);
            }
        });
        builder.setPositiveButton(R.string.actionContinue, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                DataExport.Builder builder = new DataExport.Builder();
                builder.setTrack(track).setFormat(selected.get()).create().show(MainActivity.this.mFragmentManager, "trackExport");
            }
        });
        builder.setNeutralButton(R.string.explain, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(-3).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Builder builder = new Builder(MainActivity.this);
                String msgNative = MainActivity.this.getString(R.string.msgNativeFormatExplanation);
                builder.setMessage(msgNative + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + MainActivity.this.getString(R.string.msgOtherFormatsExplanation));
                builder.setPositiveButton(R.string.ok, null);
                builder.create().show();
            }
        });
    }

    public void onTrackSave(final Track track) {
        FileDataSource fileSource = track.source;
        Manager manager = Manager.getDataManager(getApplicationContext(), fileSource.path);
        if (manager instanceof TrackManager) {
            try {
                ((TrackManager) manager).saveProperties(fileSource);
                File thisFile = new File(fileSource.path);
                File thatFile = new File(thisFile.getParent(), FileUtils.sanitizeFilename(track.name) + TrackManager.EXTENSION);
                if (!thisFile.equals(thatFile)) {
                    Loader<List<FileDataSource>> loader = getLoaderManager().getLoader(0);
                    if (loader != null) {
                        ((DataLoader) loader).renameSource(fileSource, thatFile);
                    } else if (thisFile.renameTo(thatFile)) {
                        fileSource.path = thatFile.getAbsolutePath();
                    }
                }
            } catch (Exception e) {
                HelperUtils.showSaveError(this, this.mCoordinatorLayout, e);
                e.printStackTrace();
            }
        } else {
            Manager.save(getApplicationContext(), (FileDataSource) track.source, new OnSaveListener() {
                public void onSaved(FileDataSource source) {
                    MainActivity.this.mMainHandler.post(new Runnable() {
                        public void run() {
                            track.source.notifyListeners();
                        }
                    });
                }

                public void onError(FileDataSource source, Exception e) {
                    HelperUtils.showSaveError(MainActivity.this, MainActivity.this.mCoordinatorLayout, e);
                }
            }, this.mProgressHandler);
        }
        Iterator it = this.mMap.layers().iterator();
        while (it.hasNext()) {
            Layer layer = (Layer) it.next();
            if ((layer instanceof TrackLayer) && ((TrackLayer) layer).getTrack().equals(track)) {
                ((TrackLayer) layer).setColor(track.style.color);
            }
        }
        this.mMap.updateMap(true);
    }

    public void onTrackDelete(final Track track) {
        Iterator<Layer> i = this.mMap.layers().iterator();
        while (i.hasNext()) {
            Layer layer = (Layer) i.next();
            if ((layer instanceof TrackLayer) && ((TrackLayer) layer).getTrack().equals(track)) {
                i.remove();
                layer.onDetach();
                break;
            }
        }
        this.mMap.updateMap(true);
        Snackbar.make(this.mCoordinatorLayout, (int) R.string.msgTrackDeleted, 0).setCallback(new Callback() {
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                if (event != 1) {
                    ((FileDataSource) track.source).tracks.remove(track);
                    Manager.save(MainActivity.this.getApplicationContext(), (FileDataSource) track.source, new OnSaveListener() {
                        public void onSaved(FileDataSource source) {
                            MainActivity.this.mMainHandler.post(new Runnable() {
                                public void run() {
                                    track.source.notifyListeners();
                                }
                            });
                        }

                        public void onError(FileDataSource source, Exception e) {
                            HelperUtils.showSaveError(MainActivity.this, MainActivity.this.mCoordinatorLayout, e);
                        }
                    }, MainActivity.this.mProgressHandler);
                    MainActivity.this.mTotalDataItems = MainActivity.this.mTotalDataItems - 1;
                }
            }
        }).setAction((int) R.string.actionUndo, new OnClickListener() {
            public void onClick(View view) {
                MainActivity.this.mMap.layers().add(new TrackLayer(MainActivity.this.mMap, track), 7);
                MainActivity.this.mMap.updateMap(true);
            }
        }).show();
    }

    public void onTracksDelete(Set<Track> set) {
    }

    public void onFeatureDetails(long id) {
        Bundle args = new Bundle(3);
        if (this.mLocationState == LocationState.DISABLED || this.mLocationService == null) {
            MapPosition position = this.mMap.getMapPosition();
            args.putDouble("lat", position.getLatitude());
            args.putDouble("lon", position.getLongitude());
        } else {
            Location location = this.mLocationService.getLocation();
            args.putDouble("lat", location.getLatitude());
            args.putDouble("lon", location.getLongitude());
        }
        Fragment fragment = this.mFragmentManager.findFragmentByTag("amenityInformation");
        if (fragment == null) {
            fragment = Fragment.instantiate(this, AmenityInformation.class.getName(), args);
            Slide slide = new Slide(80);
            slide.setDuration((long) getResources().getInteger(17694720));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = this.mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "amenityInformation");
            ft.addToBackStack("amenityInformation");
            ft.commit();
            updateMapViewArea();
        }
        ((AmenityInformation) fragment).setPreferredLanguage(Configuration.getLanguage());
        ((AmenityInformation) fragment).setAmenity(id);
        this.mExtendPanel.setForeground(getDrawable(R.drawable.dim));
        this.mExtendPanel.getForeground().setAlpha(0);
        ObjectAnimator anim = ObjectAnimator.ofInt(this.mExtendPanel.getForeground(), "alpha", new int[]{0, 255});
        anim.setDuration(500);
        anim.start();
    }

    public void shareLocation(@NonNull GeoPoint coordinates, @Nullable String name) {
        LocationShareDialog dialogFragment = new LocationShareDialog();
        Bundle args = new Bundle();
        args.putDouble("latitude", coordinates.getLatitude());
        args.putDouble("longitude", coordinates.getLongitude());
        args.putInt("zoom", this.mMap.getMapPosition().getZoomLevel());
        if (name != null) {
            args.putString("name", name);
        }
        dialogFragment.setArguments(args);
        dialogFragment.show(this.mFragmentManager, "locationShare");
    }

    private void showHideMapObjects(boolean hasBitmapMap) {
        Layers layers = this.mMap.layers();
        if (hasBitmapMap && this.mHideMapObjects && layers.contains(this.mLabelsLayer)) {
            if (this.mBuildingsLayerEnabled) {
                layers.remove(this.mBuildingsLayer);
            }
            layers.remove(this.mLabelsLayer);
        }
        if ((!hasBitmapMap || !this.mHideMapObjects) && !layers.contains(this.mLabelsLayer)) {
            if (this.mBuildingsLayerEnabled) {
                layers.add(this.mBuildingsLayer, 5);
            }
            layers.add(this.mLabelsLayer, 6);
        }
    }

    private void startMapSelection(boolean zoom) {
        if (this.mFragmentManager.getBackStackEntryCount() > 0) {
            popAll();
        }
        if (zoom) {
            MapPosition mapPosition = this.mMap.getMapPosition();
            mapPosition.setScale(45.0d);
            mapPosition.setBearing(0.0f);
            mapPosition.setTilt(0.0f);
            this.mMap.animator().animateTo(500, mapPosition);
        }
        MapSelection fragment = (MapSelection) Fragment.instantiate(this, MapSelection.class.getName());
        fragment.setMapIndex(this.mNativeMapIndex);
        fragment.setEnterTransition(new Slide());
        FragmentTransaction ft = this.mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "mapSelection");
        ft.addToBackStack("mapSelection");
        ft.commit();
        updateMapViewArea();
    }

    public void onMapSelected(MapFile mapFile) {
        if (this.mBitmapLayerMap != null) {
            this.mMap.layers().remove(this.mBitmapLayerMap.tileLayer);
            this.mBitmapLayerMap.tileSource.close();
            if (mapFile == this.mBitmapLayerMap) {
                showHideMapObjects(false);
                this.mMap.updateMap(true);
                this.mBitmapLayerMap = null;
                return;
            }
        }
        showBitmapMap(mapFile, true);
    }

    public void onHideMapObjects(boolean hide) {
        this.mHideMapObjects = hide;
        showHideMapObjects(this.mBitmapLayerMap != null);
        this.mMap.updateMap(true);
        Configuration.setHideMapObjects(hide);
    }

    public void onTransparencyChanged(int transparency) {
        this.mBitmapMapTransparency = transparency;
        if (this.mBitmapLayerMap != null && (this.mBitmapLayerMap.tileLayer instanceof BitmapTileLayer)) {
            ((BitmapTileLayer) this.mBitmapLayerMap.tileLayer).setBitmapAlpha(Viewport.VIEW_NEAR - (((float) this.mBitmapMapTransparency) * 0.01f));
        }
        Configuration.setBitmapMapTransparency(transparency);
    }

    public void onBeginMapManagement() {
        this.mMapCoverageLayer = new MapCoverageLayer(getApplicationContext(), this.mMap, this.mNativeMapIndex, MapTrek.density);
        this.mMap.layers().add(this.mMapCoverageLayer, 10);
        MapPosition mapPosition = this.mMap.getMapPosition();
        if (mapPosition.zoomLevel > 8) {
            mapPosition.setZoomLevel(8);
            this.mMap.animator().animateTo(500, mapPosition);
        } else {
            this.mMap.updateMap(true);
        }
        int[] xy = (int[]) this.mMapDownloadButton.getTag();
        if (xy != null) {
            this.mNativeMapIndex.selectNativeMap(xy[0], xy[1], ACTION.DOWNLOAD);
        }
    }

    public void onFinishMapManagement() {
        this.mMap.layers().remove(this.mMapCoverageLayer);
        this.mMapCoverageLayer.onDetach();
        this.mMap.updateMap(true);
        this.mNativeMapIndex.clearSelections();
        this.mMapCoverageLayer = null;
    }

    public void onManageNativeMaps(boolean hillshadesEnabled) {
        this.mNativeMapIndex.manageNativeMaps(hillshadesEnabled);
    }

    private void showHillShade() {
        TileSource hillShadeTileSource = MapTrek.getApplication().getHillShadeTileSource();
        if (hillShadeTileSource != null) {
            this.mHillshadeLayer = new BitmapTileLayer(this.mMap, hillShadeTileSource, ((float) Configuration.getHillshadesTransparency()) * 0.01f);
            this.mMap.layers().add(this.mHillshadeLayer, 4);
            this.mMap.updateMap(true);
        }
    }

    private void hideHillShade() {
        this.mMap.layers().remove(this.mHillshadeLayer);
        this.mHillshadeLayer.onDetach();
        this.mMap.updateMap(true);
        this.mHillshadeLayer = null;
    }

    private void showBitmapMap(MapFile mapFile, boolean reposition) {
        logger.debug("showBitmapMap({})", mapFile.name);
        showHideMapObjects(true);
        mapFile.tileSource.open();
        if ("vtm".equals(mapFile.tileSource.getOption("format"))) {
            OsmTileLayer layer = new OsmTileLayer(this.mMap);
            layer.setTileSource(mapFile.tileSource);
            layer.setRenderTheme(ThemeLoader.load(Themes.MAPTREK));
            mapFile.tileLayer = layer;
        } else {
            mapFile.tileLayer = new BitmapTileLayer(this.mMap, mapFile.tileSource, Viewport.VIEW_NEAR - (((float) this.mBitmapMapTransparency) * 0.01f));
        }
        this.mMap.layers().add(mapFile.tileLayer, 3);
        this.mBitmapLayerMap = mapFile;
        if (reposition) {
            MapPosition position = this.mMap.getMapPosition();
            boolean positionChanged = false;
            if (!mapFile.boundingBox.contains(position.getGeoPoint())) {
                position.setPosition(mapFile.boundingBox.getCenterPoint());
                positionChanged = true;
            }
            if (position.getZoomLevel() > mapFile.tileSource.getZoomLevelMax()) {
                position.setScale((double) ((1 << mapFile.tileSource.getZoomLevelMax()) - 5));
                positionChanged = true;
            }
            int minZoomLevel = mapFile.tileSource.getZoomLevelMin();
            if (mapFile.tileSource instanceof SQLiteTileSource) {
                minZoomLevel = ((SQLiteTileSource) mapFile.tileSource).sourceZoomMin;
            }
            double minScale = ((((double) (1 << minZoomLevel)) * 0.7d) + (((double) (1 << (minZoomLevel + 1))) * 0.3d)) + 5.0d;
            if (position.getScale() < minScale) {
                position.setScale(minScale);
                positionChanged = true;
            }
            if (positionChanged) {
                this.mMap.animator().animateTo(500, position);
            } else {
                this.mMap.clearMap();
            }
        }
    }

    private void showActionPanel(boolean show, boolean animate) {
        Configuration.setActionPanelState(show);
        final View mAPB = findViewById(R.id.actionPanelBackground);
        if (animate && this.mFragmentManager.getBackStackEntryCount() > 0) {
            popAll();
        }
        if (animate) {
            this.mMoreButton.animate().rotationBy(180.0f).setDuration(150).setListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    MainActivity.this.mMoreButton.setRotation(0.0f);
                }
            });
        }
        if (show) {
            mAPB.setVisibility(0);
            if (animate) {
                mAPB.animate().setDuration(150).alpha(Viewport.VIEW_NEAR);
            } else {
                mAPB.setAlpha(Viewport.VIEW_NEAR);
            }
            this.mMapsButton.setVisibility(0);
            if (animate) {
                this.mMapsButton.animate().alpha(Viewport.VIEW_NEAR).setDuration(30).setListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        MainActivity.this.mPlacesButton.setVisibility(0);
                        MainActivity.this.mPlacesButton.animate().alpha(Viewport.VIEW_NEAR).setDuration(30).setListener(new AnimatorListenerAdapter() {
                            public void onAnimationEnd(Animator animation) {
                                MainActivity.this.mRecordButton.setVisibility(0);
                                MainActivity.this.mRecordButton.animate().alpha(Viewport.VIEW_NEAR).setDuration(30).setListener(new AnimatorListenerAdapter() {
                                    public void onAnimationEnd(Animator animation) {
                                        MainActivity.this.mLocationButton.setVisibility(0);
                                        MainActivity.this.mLocationButton.animate().alpha(Viewport.VIEW_NEAR).setDuration(30).setListener(new AnimatorListenerAdapter() {
                                            public void onAnimationEnd(Animator animation) {
                                                MainActivity.this.mExtendPanel.postInvalidate();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
                return;
            }
            this.mMapsButton.setAlpha(Viewport.VIEW_NEAR);
            this.mPlacesButton.setVisibility(0);
            this.mPlacesButton.setAlpha(Viewport.VIEW_NEAR);
            this.mRecordButton.setVisibility(0);
            this.mRecordButton.setAlpha(Viewport.VIEW_NEAR);
            this.mLocationButton.setVisibility(0);
            this.mLocationButton.setAlpha(Viewport.VIEW_NEAR);
        } else if (animate) {
            mAPB.animate().alpha(0.0f).setDuration(150);
            this.mLocationButton.animate().alpha(0.0f).setDuration(30).setListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    MainActivity.this.mLocationButton.setVisibility(4);
                    MainActivity.this.mRecordButton.animate().alpha(0.0f).setDuration(30).setListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            MainActivity.this.mRecordButton.setVisibility(4);
                            MainActivity.this.mPlacesButton.animate().alpha(0.0f).setDuration(30).setListener(new AnimatorListenerAdapter() {
                                public void onAnimationEnd(Animator animation) {
                                    MainActivity.this.mPlacesButton.setVisibility(4);
                                    MainActivity.this.mMapsButton.animate().alpha(0.0f).setDuration(30).setListener(new AnimatorListenerAdapter() {
                                        public void onAnimationEnd(Animator animation) {
                                            MainActivity.this.mMapsButton.setVisibility(4);
                                            mAPB.setVisibility(4);
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });
        } else {
            mAPB.setAlpha(0.0f);
            this.mLocationButton.setAlpha(0.0f);
            this.mLocationButton.setVisibility(4);
            this.mRecordButton.setAlpha(0.0f);
            this.mRecordButton.setVisibility(4);
            this.mPlacesButton.setAlpha(0.0f);
            this.mPlacesButton.setVisibility(4);
            this.mMapsButton.setAlpha(0.0f);
            this.mMapsButton.setVisibility(4);
            mAPB.setVisibility(4);
        }
    }

    private void showExtendPanel(PANEL_STATE panel, String name, Fragment fragment) {
        if (this.mPanelState != PANEL_STATE.NONE) {
            BackStackEntry bse = this.mFragmentManager.getBackStackEntryAt(0);
            this.mFragmentManager.popBackStackImmediate(bse.getId(), 1);
            if (name.equals(bse.getName())) {
                setPanelState(PANEL_STATE.NONE);
                return;
            }
        }
        this.mExtendPanel.setForeground(null);
        FragmentTransaction ft = this.mFragmentManager.beginTransaction();
        fragment.setEnterTransition(new TransitionSet().addTransition(new Slide(this.mSlideGravity)).addTransition(new Visibility() {
            public Animator onAppear(ViewGroup sceneRoot, View v, TransitionValues startValues, TransitionValues endValues) {
                return ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), new Object[]{Integer.valueOf(MainActivity.this.getColor(R.color.panelBackground)), Integer.valueOf(MainActivity.this.getColor(R.color.panelSolidBackground))});
            }
        }));
        fragment.setReturnTransition(new TransitionSet().addTransition(new Slide(this.mSlideGravity)).addTransition(new Visibility() {
            public Animator onDisappear(ViewGroup sceneRoot, View v, TransitionValues startValues, TransitionValues endValues) {
                return ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), new Object[]{Integer.valueOf(MainActivity.this.getColor(R.color.panelSolidBackground)), Integer.valueOf(MainActivity.this.getColor(R.color.panelBackground))});
            }
        }));
        ft.replace(R.id.extendPanel, fragment, name);
        ft.addToBackStack(name);
        ft.commit();
        setPanelState(panel);
        if ("dataList".equals(name) || "dataSourceList".equals(name)) {
            HelperUtils.showTargetedAdvice((Activity) this, 64, (int) R.string.advice_adding_place, this.mPlacesButton, false);
        }
    }

    private void setPanelState(PANEL_STATE state) {
        if (this.mPanelState != state) {
            PANEL_STATE thisState;
            View thisView;
            int thisFrom;
            int thisTo;
            int otherFrom;
            int otherTo;
            View mLBB = findViewById(R.id.locationButtonBackground);
            View mRBB = findViewById(R.id.recordButtonBackground);
            View mPBB = findViewById(R.id.placesButtonBackground);
            View mOBB = findViewById(R.id.mapsButtonBackground);
            View mMBB = findViewById(R.id.moreButtonBackground);
            final ArrayList<View> otherViews = new ArrayList();
            if (this.mPanelState != PANEL_STATE.NONE && state != PANEL_STATE.NONE) {
                switch (this.mPanelState) {
                    case RECORD:
                        otherViews.add(mRBB);
                        break;
                    case PLACES:
                        otherViews.add(mPBB);
                        break;
                    case MAPS:
                        otherViews.add(mOBB);
                        break;
                    case MORE:
                        otherViews.add(mMBB);
                        break;
                    case LOCATION:
                        otherViews.add(mLBB);
                        break;
                    default:
                        break;
                }
            }
            otherViews.add(mLBB);
            otherViews.add(mRBB);
            otherViews.add(mPBB);
            otherViews.add(mOBB);
            otherViews.add(mMBB);
            if (state == PANEL_STATE.NONE) {
                thisState = this.mPanelState;
            } else {
                thisState = state;
            }
            switch (thisState) {
                case RECORD:
                    thisView = mRBB;
                    break;
                case PLACES:
                    thisView = mPBB;
                    break;
                case MAPS:
                    thisView = mOBB;
                    break;
                case MORE:
                    thisView = mMBB;
                    break;
                case LOCATION:
                    thisView = mLBB;
                    break;
                default:
                    return;
            }
            otherViews.remove(thisView);
            if (state == PANEL_STATE.NONE) {
                thisFrom = this.mPanelSolidBackground;
                thisTo = this.mPanelBackground;
                otherFrom = this.mPanelExtendedBackground;
                otherTo = this.mPanelBackground;
            } else {
                if (this.mPanelState == PANEL_STATE.NONE) {
                    thisFrom = this.mPanelBackground;
                } else {
                    thisFrom = this.mPanelExtendedBackground;
                }
                thisTo = this.mPanelSolidBackground;
                if (this.mPanelState == PANEL_STATE.NONE) {
                    otherFrom = this.mPanelBackground;
                } else {
                    otherFrom = this.mPanelSolidBackground;
                }
                otherTo = this.mPanelExtendedBackground;
            }
            ValueAnimator otherColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), new Object[]{Integer.valueOf(otherFrom), Integer.valueOf(otherTo)});
            ValueAnimator thisColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), new Object[]{Integer.valueOf(thisFrom), Integer.valueOf(thisTo)});
            final View view = thisView;
            thisColorAnimation.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animator) {
                    view.setBackgroundColor(((Integer) animator.getAnimatedValue()).intValue());
                }
            });
            otherColorAnimation.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animator) {
                    int color = ((Integer) animator.getAnimatedValue()).intValue();
                    Iterator it = otherViews.iterator();
                    while (it.hasNext()) {
                        ((View) it.next()).setBackgroundColor(color);
                    }
                }
            });
            AnimatorSet s = new AnimatorSet();
            s.play(thisColorAnimation).with(otherColorAnimation);
            s.start();
            this.mPanelState = state;
            updateMapViewArea();
        }
    }

    public FloatingActionButton enableActionButton() {
        if (this.mListActionButton.getVisibility() == 0) {
            this.mListActionButton.setVisibility(4);
        }
        TransitionManager.beginDelayedTransition(this.mCoordinatorLayout, new Fade());
        this.mActionButton.setVisibility(0);
        return this.mActionButton;
    }

    public void disableActionButton() {
        this.mActionButton.setVisibility(8);
        if (this.mListActionButton.getVisibility() == 4) {
            this.mListActionButton.setVisibility(0);
        }
    }

    public FloatingActionButton enableListActionButton() {
        TransitionManager.beginDelayedTransition(this.mCoordinatorLayout, new Fade());
        this.mListActionButton.setVisibility(0);
        return this.mListActionButton;
    }

    public void disableListActionButton() {
        this.mListActionButton.setVisibility(8);
    }

    public void addBackClickListener(OnBackPressedListener listener) {
        this.mBackListeners.add(new WeakReference(listener));
    }

    public void removeBackClickListener(OnBackPressedListener listener) {
        Iterator<WeakReference<OnBackPressedListener>> iterator = this.mBackListeners.iterator();
        while (iterator.hasNext()) {
            if (((WeakReference) iterator.next()).get() == listener) {
                iterator.remove();
            }
        }
    }

    public void popCurrent() {
        logger.debug("popCurrent()");
        int count = this.mFragmentManager.getBackStackEntryCount();
        if (count > 0) {
            String fragmentName = this.mFragmentManager.getBackStackEntryAt(count - 1).getName();
            if ("baseMapDownload".equals(fragmentName)) {
                if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != 0) {
                    HelperUtils.showTargetedAdvice((Activity) this, 16, (int) R.string.advice_enable_locations, this.mLocationButton, false);
                }
            } else if ("trackProperties".equals(fragmentName)) {
                HelperUtils.showTargetedAdvice((Activity) this, 256, (int) R.string.advice_recorded_tracks, this.mRecordButton, false);
            }
        }
        this.mFragmentManager.popBackStack();
    }

    public void popAll() {
        logger.debug("popAll()");
        this.mFragmentManager.popBackStack(this.mFragmentManager.getBackStackEntryAt(0).getId(), 1);
    }

    public CoordinatorLayout getCoordinatorLayout() {
        return this.mCoordinatorLayout;
    }

    private boolean backKeyIntercepted() {
        boolean intercepted = false;
        for (WeakReference<OnBackPressedListener> weakRef : this.mBackListeners) {
            OnBackPressedListener onBackClickListener = (OnBackPressedListener) weakRef.get();
            if (onBackClickListener != null) {
                boolean isFragIntercept = onBackClickListener.onBackClick();
                if (!intercepted) {
                    intercepted = isFragIntercept;
                }
            }
        }
        return intercepted;
    }

    public void onBackPressed() {
        logger.debug("onBackPressed()");
        if (!backKeyIntercepted()) {
            int count = this.mFragmentManager.getBackStackEntryCount();
            if (count > 0) {
                BackStackEntry bse = this.mFragmentManager.getBackStackEntryAt(count - 1);
                if ("settings".equals(bse.getName())) {
                    HelperUtils.showTargetedAdvice((Activity) this, 32, (int) R.string.advice_map_settings, this.mMapsButton, false);
                }
                if ("trackProperties".equals(bse.getName())) {
                    HelperUtils.showTargetedAdvice((Activity) this, 256, (int) R.string.advice_recorded_tracks, this.mRecordButton, false);
                }
                super.onBackPressed();
                if (count == 1 && this.mPanelState != PANEL_STATE.NONE) {
                    setPanelState(PANEL_STATE.NONE);
                }
            } else if (count == 0 || this.secondBack) {
                finish();
            } else {
                this.secondBack = true;
                this.mBackToast.show();
                this.mBackHandler.postDelayed(new Runnable() {
                    public void run() {
                        MainActivity.this.secondBack = false;
                    }
                }, 2000);
            }
        }
    }

    public void onBackStackChanged() {
        logger.debug("onBackStackChanged()");
        int count = this.mFragmentManager.getBackStackEntryCount();
        if (count != 0) {
            Fragment f = this.mFragmentManager.findFragmentByTag(this.mFragmentManager.getBackStackEntryAt(count - 1).getName());
            if (f != null) {
                View v = f.getView();
                if (v != null) {
                    final ViewGroup p = (ViewGroup) v.getParent();
                    if (p.getForeground() != null) {
                        p.setForeground(getDrawable(R.drawable.dim));
                        p.getForeground().setAlpha(0);
                        ObjectAnimator anim = ObjectAnimator.ofInt(p.getForeground(), "alpha", new int[]{255, 0});
                        anim.addListener(new AnimatorListener() {
                            public void onAnimationStart(Animator animation) {
                            }

                            public void onAnimationEnd(Animator animation) {
                                p.setForeground(null);
                            }

                            public void onAnimationCancel(Animator animation) {
                                p.setForeground(null);
                            }

                            public void onAnimationRepeat(Animator animation) {
                            }
                        });
                        anim.setDuration(500);
                        anim.start();
                    }
                }
            }
        } else if (this.mPanelState != PANEL_STATE.NONE) {
            setPanelState(PANEL_STATE.NONE);
        }
    }

    public void onDataMissing(final int x, final int y, byte zoom) {
        if (!this.mMap.animator().isActive() && this.mMapCoverageLayer == null && !this.mNativeMapIndex.isDownloading(x, y)) {
            if (!this.mNativeMapIndex.hasDownloadSizes() || this.mNativeMapIndex.getNativeMap(x, y).downloadSize != 0) {
                this.mMap.getMapPosition(this.mMapPosition);
                if (this.mBitmapLayerMap == null || !this.mBitmapLayerMap.contains(this.mMapPosition.getX(), this.mMapPosition.getY())) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (MainActivity.this.mMapDownloadButton.getVisibility() == 8) {
                                MainActivity.this.mMapDownloadButton.setText(R.string.mapDownloadText);
                                MainActivity.this.mMapDownloadButton.setVisibility(0);
                            }
                            MainActivity.this.mMapDownloadButton.setTag(new int[]{x, y});
                            MainActivity.this.mMainHandler.removeMessages(R.id.msgRemoveMapDownloadButton);
                        }
                    });
                }
            }
        }
    }

    public void updateMapViewArea() {
        logger.debug("updateMapViewArea()");
        final ViewTreeObserver vto = this.mMapView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                ViewTreeObserver ob;
                MainActivity.logger.debug("onGlobalLayout()");
                ((MarginLayoutParams) MainActivity.this.mCoordinatorLayout.getLayoutParams()).topMargin = MainActivity.this.mStatusBarHeight;
                if (MainActivity.this.mFragmentManager.getBackStackEntryCount() == 0) {
                    if (MainActivity.this.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != 0) {
                        HelperUtils.showTargetedAdvice(MainActivity.this, 16, (int) R.string.advice_enable_locations, MainActivity.this.mLocationButton, false);
                    } else if (MainActivity.this.mTotalDataItems > 5 && MainActivity.this.mPanelState == PANEL_STATE.NONE) {
                        MainActivity.this.mPopupAnchor.setX(((float) MainActivity.this.mMap.getWidth()) - (32.0f * MapTrek.density));
                        MainActivity.this.mPopupAnchor.setY(((float) MainActivity.this.mStatusBarHeight) + (Viewport.VIEW_FAR * MapTrek.density));
                        HelperUtils.showTargetedAdvice(MainActivity.this, 4096, (int) R.string.advice_hide_map_objects, MainActivity.this.mPopupAnchor, (int) R.drawable.ic_volume_up);
                    }
                }
                if (Boolean.TRUE.equals(MainActivity.this.mGaugePanel.getTag())) {
                    MainActivity.this.mGaugePanel.setTranslationX((float) (-MainActivity.this.mGaugePanel.getWidth()));
                    MainActivity.this.mGaugePanel.setVisibility(0);
                    MainActivity.this.mGaugePanel.setTag(null);
                }
                Rect area = new Rect();
                MainActivity.this.mMapView.getLocalVisibleRect(area);
                int mapWidth = area.width();
                int mapHeight = area.height();
                area.top = MainActivity.this.mStatusBarHeight;
                area.left = (int) (((float) MainActivity.this.mGaugePanel.getRight()) + MainActivity.this.mGaugePanel.getTranslationX());
                View v = MainActivity.this.findViewById(R.id.actionPanel);
                if (v != null) {
                    if (MainActivity.this.mVerticalOrientation) {
                        area.bottom = v.getTop();
                    } else {
                        area.right = v.getLeft();
                    }
                }
                if (MainActivity.this.mPanelState != PANEL_STATE.NONE) {
                    if (MainActivity.this.mVerticalOrientation) {
                        area.bottom = MainActivity.this.mExtendPanel.getTop();
                    } else {
                        area.right = MainActivity.this.mExtendPanel.getLeft();
                    }
                }
                int count = MainActivity.this.mFragmentManager.getBackStackEntryCount();
                if (count > 0) {
                    BackStackEntry bse = MainActivity.this.mFragmentManager.getBackStackEntryAt(count - 1);
                    View contentPanel = MainActivity.this.mCoordinatorLayout.findViewById(R.id.contentPanel);
                    if ("search".equals(bse.getName())) {
                        if (MainActivity.this.mVerticalOrientation) {
                            area.bottom = contentPanel.getTop();
                        } else {
                            area.right = contentPanel.getLeft();
                        }
                    }
                }
                if (!area.isEmpty()) {
                    int pointerOffset = (int) (50.0f * MapTrek.density);
                    int centerX = mapWidth / 2;
                    int centerY = mapHeight / 2;
                    MainActivity.this.mMovingOffset = Math.min(centerX - area.left, area.right - centerX);
                    MainActivity.this.mMovingOffset = Math.min(MainActivity.this.mMovingOffset, centerY - area.top);
                    MainActivity.this.mMovingOffset = Math.min(MainActivity.this.mMovingOffset, area.bottom - centerY);
                    MainActivity.this.mMovingOffset = MainActivity.this.mMovingOffset - pointerOffset;
                    if (MainActivity.this.mMovingOffset < 0) {
                        MainActivity.this.mMovingOffset = 0;
                    }
                    MainActivity.this.mTrackingOffset = (area.bottom - (mapHeight / 2)) - (pointerOffset * 2);
                    MainActivity.this.mMapScaleBarLayer.getRenderer().setOffset(((float) area.left) + (Viewport.VIEW_FAR * MapTrek.density), (float) area.top);
                }
                if (vto.isAlive()) {
                    ob = vto;
                } else {
                    ob = MainActivity.this.mMapView.getViewTreeObserver();
                }
                ob.removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void askForPermission() {
        if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0) {
            enableLocations();
        } else if (shouldShowRequestPermissionRationale("android.permission.ACCESS_FINE_LOCATION")) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 1);
        } else {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 1);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == 0) {
                    enableLocations();
                    return;
                }
                return;
            default:
                return;
        }
    }

    public Loader<List<FileDataSource>> onCreateLoader(int id, Bundle args) {
        logger.debug("onCreateLoader({})", Integer.valueOf(id));
        return new DataLoader(this);
    }

    public void onLoadFinished(Loader<List<FileDataSource>> loader, List<FileDataSource> data) {
        logger.debug("onLoadFinished()");
        if (data != null) {
            this.mData = data;
            for (FileDataSource source : this.mData) {
                if (source.isLoaded() && source.isLoadable() && !source.isVisible()) {
                    addSourceToMap(source);
                    source.setVisible(true);
                }
            }
            Fragment dataSourceList = this.mFragmentManager.findFragmentByTag("dataSourceList");
            if (dataSourceList != null) {
                ((DataSourceList) dataSourceList).updateData();
            }
            Fragment nativeTrackList = this.mFragmentManager.findFragmentByTag("nativeTrackList");
            if (nativeTrackList != null) {
                ((DataSourceList) nativeTrackList).updateData();
            }
            this.mMap.updateMap(true);
        }
    }

    public void onLoaderReset(Loader<List<FileDataSource>> loader) {
    }

    private void addSourceToMap(FileDataSource source) {
        for (Waypoint waypoint : source.waypoints) {
            addWaypointMarker(waypoint);
            this.mTotalDataItems++;
        }
        for (Track track : source.tracks) {
            this.mMap.layers().add(new TrackLayer(this.mMap, track), 7);
            this.mTotalDataItems++;
        }
    }

    private void removeSourceFromMap(FileDataSource source) {
        for (Waypoint waypoint : source.waypoints) {
            removeWaypointMarker(waypoint);
            this.mTotalDataItems--;
        }
        Iterator<Layer> i = this.mMap.layers().iterator();
        while (i.hasNext()) {
            Layer layer = (Layer) i.next();
            if ((layer instanceof TrackLayer) && source.tracks.contains(((TrackLayer) layer).getTrack())) {
                i.remove();
                layer.onDetach();
                this.mTotalDataItems--;
            }
        }
    }

    private void addWaypointMarker(Waypoint waypoint) {
        MarkerItem marker = new MarkerItem(waypoint, waypoint.name, waypoint.description, waypoint.coordinates);
        if (!(waypoint.style.color == 0 || waypoint.style.color == MarkerStyle.DEFAULT_COLOR)) {
            marker.setMarker(new MarkerSymbol(new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, waypoint.style.color)), HotspotPlace.BOTTOM_CENTER));
        }
        this.mMarkerLayer.addItem(marker);
    }

    private void removeWaypointMarker(Waypoint waypoint) {
        this.mMarkerLayer.removeItem(this.mMarkerLayer.getByUid(waypoint));
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 25) {
            if (!this.mMap.getEventLayer().moveEnabled()) {
                return true;
            }
            AbstractMapEventLayer eventLayer = this.mMap.getEventLayer();
            eventLayer.enableMove(false);
            eventLayer.enableRotation(false);
            eventLayer.enableTilt(false);
            eventLayer.enableZoom(false);
            this.mCrosshairLayer.lock(this.mColorAccent);
            this.mPositionLocked = true;
            return true;
        } else if (keyCode != 24) {
            return super.onKeyDown(keyCode, event);
        } else {
            Iterator it = this.mMap.layers().iterator();
            while (it.hasNext()) {
                Layer layer = (Layer) it.next();
                if ((layer instanceof TrackLayer) || (layer instanceof MapObjectLayer) || (layer instanceof MarkerLayer)) {
                    layer.setEnabled(false);
                }
            }
            this.mMap.updateMap(true);
            return true;
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 25) {
            AbstractMapEventLayer eventLayer = this.mMap.getEventLayer();
            eventLayer.enableMove(true);
            eventLayer.enableRotation(true);
            eventLayer.enableTilt(true);
            eventLayer.enableZoom(true);
            this.mCrosshairLayer.unlock();
            this.mPositionLocked = false;
            return true;
        } else if (keyCode != 24) {
            return super.onKeyUp(keyCode, event);
        } else {
            Iterator it = this.mMap.layers().iterator();
            while (it.hasNext()) {
                Layer layer = (Layer) it.next();
                if ((layer instanceof TrackLayer) || (layer instanceof MapObjectLayer) || (layer instanceof MarkerLayer)) {
                    layer.setEnabled(true);
                }
            }
            this.mMap.updateMap(true);
            return true;
        }
    }

    public Map getMap() {
        return this.mMap;
    }

    @NonNull
    public WaypointDbDataSource getWaypointDataSource() {
        return this.mWaypointDbDataSource;
    }

    @NonNull
    public List<FileDataSource> getData() {
        return this.mData;
    }

    public void setDataSourceAvailability(FileDataSource source, boolean available) {
        if (!available) {
            removeSourceFromMap(source);
        } else if (source.isLoaded()) {
            addSourceToMap(source);
        }
        source.setVisible(available);
        Loader<List<FileDataSource>> loader = getLoaderManager().getLoader(0);
        if (loader != null) {
            ((DataLoader) loader).markDataSourceLoadable(source, available);
        }
        this.mMap.updateMap(true);
    }

    public void onDataSourceSelected(@NonNull DataSource source) {
        Bundle args = new Bundle(3);
        if (this.mLocationState == LocationState.DISABLED || this.mLocationService == null) {
            MapPosition position = this.mMap.getMapPosition();
            args.putDouble("lat", position.getLatitude());
            args.putDouble("lon", position.getLongitude());
        } else {
            Location location = this.mLocationService.getLocation();
            args.putDouble("lat", location.getLatitude());
            args.putDouble("lon", location.getLongitude());
        }
        args.putInt(DataList.ARG_HEIGHT, this.mExtendPanel.getHeight());
        DataList fragment = (DataList) Fragment.instantiate(this, DataList.class.getName(), args);
        fragment.setDataSource(source);
        FragmentTransaction ft = this.mFragmentManager.beginTransaction();
        fragment.setEnterTransition(new Fade());
        ft.add(R.id.extendPanel, fragment, "dataList");
        ft.addToBackStack("dataList");
        ft.commit();
    }

    public void onDataSourceShare(@NonNull final DataSource dataSource) {
        boolean askName;
        if (dataSource.name == null || (dataSource instanceof WaypointDbDataSource)) {
            askName = true;
        } else {
            askName = false;
        }
        final AtomicInteger selected = new AtomicInteger(0);
        final EditText inputView = new EditText(this);
        final DataSource dataSource2 = dataSource;
        final DialogInterface.OnClickListener exportAction = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (askName) {
                    dataSource2.name = inputView.getText().toString();
                }
                new DataExport.Builder().setDataSource(dataSource2).setFormat(dataSource2.isNativeTrack() ? selected.get() : selected.get() + 1).create().show(MainActivity.this.mFragmentManager, "dataExport");
            }
        };
        Builder builder = new Builder(this);
        builder.setTitle(R.string.title_select_format);
        builder.setSingleChoiceItems(dataSource.isNativeTrack() ? R.array.track_format_array : R.array.data_format_array, selected.get(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                selected.set(which);
            }
        });
        if (askName) {
            builder.setPositiveButton(R.string.actionContinue, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    Builder nameBuilder = new Builder(MainActivity.this);
                    nameBuilder.setTitle(R.string.title_input_name);
                    nameBuilder.setPositiveButton(R.string.actionContinue, null);
                    final AlertDialog dialog = nameBuilder.create();
                    if (dataSource.name != null) {
                        inputView.setText(dataSource.name);
                    }
                    int margin = MainActivity.this.getResources().getDimensionPixelOffset(R.dimen.dialogContentMargin);
                    dialog.setView(inputView, margin, margin >> 1, margin, 0);
                    Window window = dialog.getWindow();
                    if (window != null) {
                        window.setSoftInputMode(16);
                    }
                    dialog.show();
                    dialog.getButton(-1).setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            if (!inputView.getText().toString().trim().isEmpty()) {
                                exportAction.onClick(dialog, -1);
                                InputMethodManager imm = (InputMethodManager) MainActivity.this.getSystemService("input_method");
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(inputView.getRootView().getWindowToken(), 0);
                                }
                                dialog.dismiss();
                            }
                        }
                    });
                }
            });
        } else {
            builder.setPositiveButton(R.string.actionContinue, exportAction);
        }
        builder.setNeutralButton(R.string.explain, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(-3).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Builder builder = new Builder(MainActivity.this);
                StringBuilder stringBuilder = new StringBuilder();
                if (dataSource.isNativeTrack()) {
                    stringBuilder.append(MainActivity.this.getString(R.string.msgNativeFormatExplanation));
                    stringBuilder.append(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR);
                }
                stringBuilder.append(MainActivity.this.getString(R.string.msgOtherFormatsExplanation));
                builder.setMessage(stringBuilder.toString());
                builder.setPositiveButton(R.string.ok, null);
                builder.create().show();
            }
        });
    }

    public void onDataSourceDelete(@NonNull final DataSource source) {
        if (source instanceof FileDataSource) {
            Builder builder = new Builder(this);
            builder.setTitle(R.string.title_delete_permanently);
            builder.setMessage(R.string.msgDeleteSourcePermanently);
            builder.setPositiveButton(R.string.actionContinue, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    File sourceFile = new File(((FileDataSource) source).path);
                    if (!sourceFile.exists()) {
                        return;
                    }
                    if (sourceFile.delete()) {
                        MainActivity.this.removeSourceFromMap((FileDataSource) source);
                    } else {
                        HelperUtils.showError(MainActivity.this.getString(R.string.msgDeleteFailed), MainActivity.this.mCoordinatorLayout);
                    }
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return;
        }
        HelperUtils.showError(getString(R.string.msgCannotDeleteNativeSource), this.mCoordinatorLayout);
    }

    public void onAmenityKindVisibilityChanged() {
        Configuration.saveKindZoomState();
        this.mMap.clearMap();
    }

    @Subscribe
    public void onConfigurationChanged(ChangedEvent event) {
        String str = event.key;
        Object obj = -1;
        switch (str.hashCode()) {
            case -1419644029:
                if (str.equals(Configuration.PREF_UNIT_PRECISION)) {
                    obj = 4;
                    break;
                }
                break;
            case -560842866:
                if (str.equals(Configuration.PREF_DISTANCE_UNIT)) {
                    obj = 1;
                    break;
                }
                break;
            case 331392368:
                if (str.equals(Configuration.PREF_ANGLE_UNIT)) {
                    obj = 3;
                    break;
                }
                break;
            case 632333948:
                if (str.equals(Configuration.PREF_SPEED_UNIT)) {
                    obj = null;
                    break;
                }
                break;
            case 634839082:
                if (str.equals(Configuration.PREF_MAP_HILLSHADES)) {
                    obj = 5;
                    break;
                }
                break;
            case 1026855728:
                if (str.equals(Configuration.PREF_HILLSHADES_TRANSPARENCY)) {
                    obj = 6;
                    break;
                }
                break;
            case 1903416870:
                if (str.equals(Configuration.PREF_ELEVATION_UNIT)) {
                    obj = 2;
                    break;
                }
                break;
        }
        int unit;
        Resources resources;
        switch (obj) {
            case null:
                unit = Configuration.getSpeedUnit();
                resources = getResources();
                StringFormatter.speedFactor = Float.parseFloat(resources.getStringArray(R.array.speed_factors)[unit]);
                StringFormatter.speedAbbr = resources.getStringArray(R.array.speed_abbreviations)[unit];
                this.mGaugePanel.refreshGauges();
                return;
            case 1:
                unit = Configuration.getDistanceUnit();
                resources = getResources();
                StringFormatter.distanceFactor = Double.parseDouble(resources.getStringArray(R.array.distance_factors)[unit]);
                StringFormatter.distanceAbbr = resources.getStringArray(R.array.distance_abbreviations)[unit];
                StringFormatter.distanceShortFactor = Double.parseDouble(resources.getStringArray(R.array.distance_factors_short)[unit]);
                StringFormatter.distanceShortAbbr = resources.getStringArray(R.array.distance_abbreviations_short)[unit];
                this.mGaugePanel.refreshGauges();
                return;
            case 2:
                unit = Configuration.getElevationUnit();
                resources = getResources();
                StringFormatter.elevationFactor = Float.parseFloat(resources.getStringArray(R.array.elevation_factors)[unit]);
                StringFormatter.elevationAbbr = resources.getStringArray(R.array.elevation_abbreviations)[unit];
                this.mGaugePanel.refreshGauges();
                this.mMap.clearMap();
                return;
            case 3:
                unit = Configuration.getAngleUnit();
                resources = getResources();
                StringFormatter.angleFactor = Double.parseDouble(resources.getStringArray(R.array.angle_factors)[unit]);
                StringFormatter.angleAbbr = resources.getStringArray(R.array.angle_abbreviations)[unit];
                this.mGaugePanel.refreshGauges();
                return;
            case 4:
                StringFormatter.precisionFormat = Configuration.getUnitPrecision() ? "%.1f" : "%.0f";
                this.mGaugePanel.refreshGauges();
                return;
            case 5:
                if (Configuration.getHillshadesEnabled()) {
                    showHillShade();
                } else {
                    hideHillShade();
                }
                this.mMap.clearMap();
                return;
            case 6:
                int transparency = Configuration.getHillshadesTransparency();
                if (this.mHillshadeLayer != null) {
                    this.mHillshadeLayer.setBitmapAlpha(Viewport.VIEW_NEAR - (((float) transparency) * 0.01f));
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void checkNightMode(Location location) {
        if (this.mNextNightCheck <= this.mLastLocationMilliseconds) {
            this.mSunriseSunset.setLocation(location.getLatitude(), location.getLongitude());
            final boolean isNightTime = !this.mSunriseSunset.isDaytime(((((double) location.getTime()) * 1.0d) / WWMath.HOUR_TO_MILLIS) % 24.0d);
            if ((this.mNightMode ^ isNightTime) != 0) {
                this.mBackgroundHandler.post(new Runnable() {
                    public void run() {
                        MainActivity.this.setNightMode(isNightTime);
                    }
                });
            }
            this.mNextNightCheck = this.mLastLocationMilliseconds + 180000;
        }
    }

    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level > 60) {
            this.mShieldFactory.dispose();
            this.mOsmcSymbolFactory.dispose();
            this.mMap.clearMap();
        }
    }

    private void setNightMode(boolean night) {
        ThemeFile themeFile;
        Configuration.loadKindZoomState();
        switch (Configuration.getActivity()) {
            case 1:
                if (Tags.kindZooms[13] == 18) {
                    Tags.kindZooms[13] = 14;
                }
                themeFile = Themes.MAPTREK;
                break;
            case 2:
                themeFile = Themes.WINTER;
                break;
            default:
                if (!night) {
                    themeFile = Themes.MAPTREK;
                    break;
                } else {
                    themeFile = Themes.NEWTRON;
                    break;
                }
        }
        IRenderTheme theme = ThemeLoader.load(themeFile);
        float fontSize = Themes.MAP_FONT_SIZES[Configuration.getMapFontSize()];
        theme.scaleTextSize(fontSize);
        this.mMap.setTheme(theme, true);
        this.mShieldFactory.setFontSize(fontSize);
        this.mShieldFactory.dispose();
        this.mOsmcSymbolFactory.dispose();
        this.mNightMode = night;
    }

    private void hideSystemUI() {
        Configuration.setHideSystemUI(true);
        Configuration.accountFullScreen();
        this.mStatusBarHeight = 0;
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(5894);
        decorView.invalidate();
        updateMapViewArea();
    }

    private void showSystemUI() {
        this.mMainHandler.removeMessages(R.id.msgHideSystemUI);
        Configuration.setHideSystemUI(false);
        this.mStatusBarHeight = getStatusBarHeight();
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(0);
        decorView.invalidate();
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public String getStatsString() {
        return Configuration.getRunningTime() + "," + Configuration.getTrackingTime() + "," + this.mWaypointDbDataSource.getWaypointsCount() + "," + this.mData.size() + "," + this.mNativeMapIndex.getMapsCount() + "," + this.mMapIndex.getMaps().size() + "," + Configuration.getFullScreenTimes();
    }

    private double movingAverage(double current, double previous) {
        return (0.2d * previous) + (0.8d * current);
    }
}
