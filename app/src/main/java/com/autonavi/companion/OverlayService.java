package com.autonavi.companion;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OverlayService extends Service {
    private static final String TAG = "AmapCompanion";
    public static final String ACTION_STOP_SERVICE = "com.autonavi.companion.STOP_OVERLAY_SERVICE";
    private static final String CHANNEL_ID = "amap_companion";
    private static final String ACTION_SEND = "AUTONAVI_STANDARD_BROADCAST_SEND";
    private static final String ACTION_RECV = "AUTONAVI_STANDARD_BROADCAST_RECV";
    private static final long ALERT_TTL_MS = 5000L;
    private static final String DIY_DIR_NAME = "amap_companion/diy";
    private static final long LIGHT_TTL_MS = 4500L;
    private static final long LIGHT_TICK_MS = 1000L;
    private static final long DISPLAY_POLICY_POLL_MS = 1500L;
    private static final long NAVIGATION_ACTIVE_TTL_MS = 12000L;
    private static final long TARGET_BROADCAST_ACTIVE_TTL_MS = 15000L;
    private static final long PANEL_WIDTH_SHRINK_DELAY_MS = 3000L;
    private static final long TMC_TTL_MS = 12000L;
    private static final long FULL_MODE_TURN_MS = 10000L;
    private static final long FULL_MODE_ETA_MS = 5000L;
    private static final long OVERSPEED_BLINK_MS = 5000L;
    private static final long OVERSPEED_MILD_REST_MS = 20000L;
    private static final long OVERSPEED_MEDIUM_REST_MS = 10000L;
    private static final int OVERSPEED_NONE = 0;
    private static final int OVERSPEED_MILD = 1;
    private static final int OVERSPEED_MEDIUM = 2;
    private static final int OVERSPEED_HIGH = 3;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable panelContentChanged = this::refreshPanelVisibility;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private LinearLayout panel;
    private LinearLayout modeRow;
    private TextView modeText;
    private TextView titleText;
    private View summaryDivider;
    private LinearLayout summaryRow;
    private TextView headingInfoText;
    private TextView roadInfoText;
    private LinearLayout turnCard;
    private TextView turnLeadText;
    private ImageView turnLeadIconView;
    private TextView turnText;
    private TextView turnDistanceText;
    private ImageView turnIconView;
    private TextView turnDistBadge;
    private LinearLayout turnRowLayout;
    private LinearLayout laneSection;
    private LaneBarView laneBar;
    private LinearLayout lightRow;
    private TextView serviceAreaText;
    private TextView etaText;
    private LinearLayout alertCard;
    private TextView limitBadgeText;
    private TextView alertCaptionText;
    private TextView alertText;
    private LinearLayout alertRow;
    private LinearLayout navTurnBox;
    private ImageView navTurnIconView;
    private TextView navTurnDistText;
    private TextView detailText;
    private Context clusterContext;
    private WindowManager clusterWindowManager;
    private WindowManager.LayoutParams clusterParams;
    private LinearLayout clusterPanel;
    private LinearLayout clusterModeRow;
    private TextView clusterModeText;
    private TextView clusterTitleText;
    private View clusterSummaryDivider;
    private LinearLayout clusterSummaryRow;
    private TextView clusterHeadingInfoText;
    private TextView clusterRoadInfoText;
    private LinearLayout clusterTurnCard;
    private TextView clusterTurnLeadText;
    private ImageView clusterTurnLeadIconView;
    private TextView clusterTurnText;
    private TextView clusterTurnDistanceText;
    private ImageView clusterTurnIconView;
    private TextView clusterTurnDistBadge;
    private LinearLayout clusterTurnRowLayout;
    private LinearLayout clusterLaneSection;
    private LaneBarView clusterLaneBar;
    private LinearLayout clusterLightRow;
    private TextView clusterServiceAreaText;
    private TextView clusterEtaText;
    private LinearLayout clusterAlertCard;
    private TextView clusterLimitBadgeText;
    private TextView clusterAlertCaptionText;
    private TextView clusterAlertText;
    private LinearLayout clusterAlertRow;
    private LinearLayout clusterNavTurnBox;
    private ImageView clusterNavTurnIconView;
    private TextView clusterNavTurnDistText;
    private TextView clusterDetailText;
    private Display clusterDisplay;
    private boolean clusterMirrorEnabled;
    private int clusterMirrorRetryCount;
    // Dynamic island fields
    private LinearLayout compactWidgetRow;
    private TextView compactNavTurnRoadText;
    private TextView compactCruiseRoadText;
    private TextView compactCruiseDirText;
    private LinearLayout clusterCompactWidgetRow;
    private TextView clusterCompactNavTurnRoadText;
    private TextView clusterCompactCruiseRoadText;
    private TextView clusterCompactCruiseDirText;
    private LinearLayout compactLaneBox;
    private LinearLayout clusterCompactLaneBox;
    private DynamicIslandViews mainDynamicIslandViews;
    private DynamicIslandViews clusterDynamicIslandViews;
    // Card UI fields
    private LinearLayout cardCruiseRow1, cardCruiseRow2, cardNavArea;
    private LinearLayout clusterCardCruiseRow1, clusterCardCruiseRow2, clusterCardNavArea;
    private LinearLayout cardCruiseLaneSection, cardNavLaneSection;
    private LinearLayout clusterCardCruiseLaneSection, clusterCardNavLaneSection;
    private LaneBarView cardCruiseLaneBar, cardNavLaneBar;
    private LinearLayout cardCruiseLightRow, cardNavLightRow;
    private LinearLayout cardCruiseEdogRow, cardNavEdogRow;
    private LaneBarView clusterCardCruiseLaneBar, clusterCardNavLaneBar;
    private LinearLayout clusterCardCruiseLightRow, clusterCardNavLightRow;
    private LinearLayout clusterCardCruiseEdogRow, clusterCardNavEdogRow;
    // Dynamic island alternator fields
    private LinearLayout fullModeTurnInfoCol;
    private LinearLayout fullModeEtaInfoCol;
    private LinearLayout fullModeClusterTurnInfoCol;
    private LinearLayout fullModeClusterEtaInfoCol;
    private TextView fullModeEtaRemainDist;
    private TextView fullModeEtaArriveTime;
    private TextView fullModeClusterEtaRemainDist;
    private TextView fullModeClusterEtaArriveTime;
    private Runnable fullModeAlternator;
    private boolean fullModeShowEta = false;
    private float fullModeScale = 2f;
    private final HashMap<Integer, TrafficLightParser.LightState> trafficLights = new HashMap<>();
    private final HashMap<String, Bitmap> diyArrowCache = new HashMap<>();
    private final HashMap<String, Long> diyArrowModified = new HashMap<>();
    private boolean inCruiseMode;
    private float downRawX;
    private float downRawY;
    private int downX;
    private int downY;
    private boolean dragging;
    private float clusterDownRawX;
    private float clusterDownRawY;
    private int clusterDownX;
    private int clusterDownY;
    private boolean clusterDragging;
    private String lastDetailedMode;
    private String currentModeLabel = "";
    private String currentRoadName = "";
    private String currentHeadingSummary = "";
    private String currentRoadTypeSummary = "";
    private int currentRoadType = -1; // 0=高速 6=快速路, for exit-info gating
    private String currentTurnLead = "";
    private String currentTurnRoad = "";
    private String currentTurnDistance = "";
    private int currentTurnDistanceMeters = -1;
    private int currentTurnIcon = 0;
    private int currentLimitSpeed = -1;
    private int currentCameraType = -1;
    private int currentRawKeyType = -1;
    private String currentEtaSummary = "";
    private String currentAlertSummary = "";
    private String currentDetailSummary = "";
    private PluginRenderer pluginRenderer;
    private PluginRenderer clusterPluginRenderer;
    // Overspeed warning (all UI styles)
    private int currentVehicleSpeed = -1;
    private Runnable overspeedBlinks;
    private boolean overspeedBlinkOn;
    private boolean overspeedBlinkPhase;
    private int overspeedColor;
    private int overspeedLevel;
    private long overspeedPhaseStartedAt;
    private GradientDrawable panelBackground;
    private GradientDrawable clusterPanelBackground;
    private long alertUpdatedAt;
    private int navigationTurnDir = -1;
    private Runnable turnBlink;
    private boolean turnBlinkOn;
    private final java.util.Set<Integer> breathingLightKeys = new java.util.HashSet<>();
    // Exit/entrance info. Navi-Link reads EXIT_NAME_INFO / EXIT_DIRECTION_INFO from 10001.
    private int exitNameNum = -1;
    private String exitDirection = "";
    private String exitLabel = "";
    private boolean routeGuidanceExitSupported;
    private String routeGuidanceExitLabel = "";
    private long exitDistance = -1;
    private long exitTime = -1;
    private int exitResultState = -1;
    // Exit-number overlay TextViews (one per arrow position)
    private TextView navExitText;
    private TextView clusterNavExitText;
    private TextView turnExitText;
    private TextView clusterTurnExitText;
    private TextView turnLeadExitText;
    private TextView clusterTurnLeadExitText;
    // Exit alternator animation
    private Runnable exitArrowTick;
    private long exitAlternatorStartMs;
    private boolean exitAlternatorActive;
    private Runnable exitStopPending; // debounce road-type flapping
    private static final int EXIT_LABEL_COLOR = 0xFFFFAA33;

    private Runnable mainPanelWidthUnlock;
    private Runnable clusterPanelWidthUnlock;
    private int mainPanelBaseMinWidth = -1;
    private int clusterPanelBaseMinWidth = -1;
    private int mainPanelBaseMinHeight = -1;
    private int clusterPanelBaseMinHeight = -1;
    private int mainPanelHeldMinWidth;
    private int clusterPanelHeldMinWidth;
    private int mainPanelHeldMinHeight;
    private int clusterPanelHeldMinHeight;
    private TmcProgressBar mainTmcProgressBar;
    private TmcProgressBar clusterTmcProgressBar;
    private String cachedTmcJson = "";
    private long tmcUpdatedAt;
    private int cachedEdogSpeed = -1;
    private int cachedCameraIndex = -1;
    private int cachedCameraDist = -1;
    private int cachedCameraType = -1;
    private int cachedLightNum = -1;
    private long cachedEdogUpdatedAt;
    private int[] lastLaneData;
    private boolean[] lastLaneAdvised;
    private float overlayScale = 2f;
    private float clusterScale = 2f;
    private Runnable pendingClusterMirrorRebuild;
    private float activeDensity = -1f;
    private boolean onCreateDelayed;
    private boolean targetAppForeground;
    private boolean targetForegroundStateKnownFromBroadcast;
    private boolean targetAppForegroundFromBroadcast;
    private boolean targetBroadcastActive;
    private boolean navigationOrCruiseActive;
    private long lastNavigationSignalAt;
    private long lastTargetBroadcastAt;
    private final View.OnLayoutChangeListener clusterBoundsListener =
            (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateClusterPosition();

    private static final class DynamicIslandViews {
        LinearLayout root;
        TextView mode;
        LinearLayout navTurnBox;
        ImageView navIcon;
        TextView navExitText;
        TextView navDistance;
        TextView navRoad;
        LinearLayout turnInfoCol;
        LinearLayout etaInfoCol;
        TextView etaRemainDist;
        TextView etaArriveTime;
        LinearLayout cruiseLeft;
        TextView cruiseRoad;
        TextView cruiseDirection;
        LinearLayout laneBox;
        LaneBarView laneBar;
        LinearLayout lightRow;
        LinearLayout widgetRow;
    }

    private final Runnable lanePoll = new Runnable() {
        @Override
        public void run() {
            if (shouldRequestAmapData()) {
                requestLaneInfo();
                requestTrafficLightInfo();
                requestTmcInfo();
            }
            mainHandler.postDelayed(this, 6000L);
        }
    };

    private final Runnable alertClear = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - alertUpdatedAt >= ALERT_TTL_MS) {
                clearAlertDetails();
            }
        }
    };

    private final Runnable trafficLightTicker = new Runnable() {
        @Override
        public void run() {
            renderTrafficLights();
        }
    };

    private final Runnable tmcClear = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - tmcUpdatedAt >= TMC_TTL_MS) {
                clearTmcData();
            }
        }
    };

    private final Runnable displayPolicyPoll = new Runnable() {
        @Override
        public void run() {
            refreshDisplayPolicies();
            mainHandler.postDelayed(this, DISPLAY_POLICY_POLL_MS);
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleBroadcast(intent);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, buildNotification());
        registerAmapReceivers();
        stopSelfIfNoVisuals();
        if (shouldRequestAmapData()) {
            requestLaneInfo();
            requestExitInfo();
            requestTmcInfo();
        }
        mainHandler.postDelayed(lanePoll, 6000L);
        mainHandler.post(displayPolicyPoll);
        onCreateDelayed = true;
        mainHandler.postDelayed(() -> {
            onCreateDelayed = false;
            ensureOverlay();
            ensureClusterMirror();
            stopSelfIfNoVisuals();
        }, 800);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            shutdownWindowsImmediately();
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }
        if (intent != null && AppPrefs.ACTION_DIAGNOSTIC_REPLAY.equals(intent.getAction())) {
            handleBroadcast(intent);
            return START_STICKY;
        }
        if (!onCreateDelayed) {
            ensureOverlay();
            ensureClusterMirror();
            stopSelfIfNoVisuals();
        }
        if (shouldRequestAmapData()) {
            requestLaneInfo();
            requestTrafficLightInfo();
            requestExitInfo();
            requestTmcInfo();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        shutdownWindowsImmediately();
        try {
            unregisterReceiver(receiver);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    private void shutdownWindowsImmediately() {
        onCreateDelayed = false;
        mainHandler.removeCallbacksAndMessages(null);
        pendingClusterMirrorRebuild = null;
        mainPanelWidthUnlock = null;
        clusterPanelWidthUnlock = null;
        overspeedBlinks = null;
        fullModeAlternator = null;
        turnBlink = null;
        exitArrowTick = null;
        exitStopPending = null;
        exitAlternatorActive = false;
        mainTmcProgressBar = null;
        clusterTmcProgressBar = null;
        mainDynamicIslandViews = null;
        clusterDynamicIslandViews = null;
        cachedTmcJson = "";
        tmcUpdatedAt = 0L;
        clearEdogCache();
        navExitText = null;
        clusterNavExitText = null;
        turnExitText = null;
        clusterTurnExitText = null;
        turnLeadExitText = null;
        clusterTurnLeadExitText = null;
        breathingLightKeys.clear();
        dismissClusterMirror();
        if (windowManager != null && panel != null && panel.getParent() != null) {
            try {
                panel.setVisibility(View.GONE);
                windowManager.removeViewImmediate(panel);
            } catch (Throwable ignored) {
            }
        }
        panel = null;
        params = null;
        windowManager = null;
        panelBackground = null;
        clusterPanelBackground = null;
        try {
            stopForeground(true);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerAmapReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SEND);
        filter.addAction(ACTION_RECV);
        filter.addAction("AUTO_GUIDE_INFO_FOR_INTERNAL_WIDGET");
        filter.addAction("AUTO_STATUS_FOR_INTERNAL_WIDGET");
        filter.addAction("com.autonavi.amapauto.AUTO_WIDGET_UPDATE_ROAD_NAME_INFO");
        filter.addAction("com.autonavi.amapauto.AUTO_WIDGET_UPDATE_SILENCE_ROADNAME_INFO");
        filter.addAction("com.autonavi.amapauto.AUTO_WIDGET_UPDATE_GPS_INFO");
        filter.addAction("com.autonavi.amapauto.AUTO_WIDGET_UPDATE_CAR_DIRECTION");
        filter.addAction("com.autonavi.amapauto.AUTO_WIDGET_UPDATE_CAMERA_INFO");
        filter.addAction("com.autonavi.amapauto.AUTO_WIDGET_UPDATE_TRAFFIC_LIGHT_INFO");
        filter.addAction("com.autonavi.amapauto.AUTO_WIDGET_UPDATE_CRUISE_TRAFFIC_LIGHT_INFO");
        filter.addAction(AppPrefs.ACTION_MAIN_OVERLAY_CHANGED);
        filter.addAction(AppPrefs.ACTION_OVERLAY_SCALE_CHANGED);
        filter.addAction(AppPrefs.ACTION_CLUSTER_MIRROR_CHANGED);
        filter.addAction(AppPrefs.ACTION_CLUSTER_POSITION_CHANGED);
        filter.addAction(AppPrefs.ACTION_OVERLAY_CONTENT_CHANGED);
        filter.addAction(AppPrefs.ACTION_OVERLAY_STYLE_CHANGED);
        filter.addAction(AppPrefs.ACTION_DISPLAY_POLICY_CHANGED);
        filter.addAction(AppPrefs.ACTION_PLUGINS_CHANGED);
        filter.addAction(AppPrefs.ACTION_DIAGNOSTIC_REPLAY);
        try {
            registerReceiver(receiver, filter);
        } catch (Throwable t) {
            Log.e(TAG, "register receiver failed", t);
        }
    }

    private void ensureOverlay() {
        if (panel != null) {
            syncMainOverlayAttachment();
            return;
        }

        overlayScale = AppPrefs.getOverlayScale(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        panel = buildPanelForContext(this, overlayScale, false);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = getSavedOverlayX();
        params.y = getSavedOverlayY();

        android.graphics.Point screenSize = new android.graphics.Point();
        windowManager.getDefaultDisplay().getRealSize(screenSize);
        if (screenSize.x > 0) {
            params.x = clampOverlayAxis(params.x, screenSize.x, 100);
        }
        if (screenSize.y > 0) {
            params.y = clampOverlayAxis(params.y, screenSize.y, 100);
        }

        panel.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    downX = params.x;
                    downY = params.y;
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getRawX() - downRawX) > dp(4)
                            || Math.abs(event.getRawY() - downRawY) > dp(4)) {
                        dragging = true;
                    }
                    params.x = downX + Math.round(event.getRawX() - downRawX);
                    params.y = downY + Math.round(event.getRawY() - downRawY);
                    updateOverlayPosition();
                    return true;
                case MotionEvent.ACTION_UP:
                    saveOverlayPosition();
                    if (!dragging) {
                        openMainActivity();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    return true;
                default:
                    return true;
            }
        });

        syncMainOverlayAttachment();
        applyContentVisibilityPrefs();
        updateClusterPosition();
    }

    private void syncMainOverlayAttachment() {
        if (windowManager == null || panel == null || params == null) {
            Log.d(TAG, "syncMainOverlayAttachment: null check failed wm=" + (windowManager != null) + " panel=" + (panel != null) + " params=" + (params != null));
            return;
        }
        boolean enabled = (AppPrefs.isMainOverlayEnabled(this)
                || shouldShowMainOverlayForTargetBroadcast())
                && !shouldHideMainOverlayForTargetForeground();
        boolean attached = panel.getParent() != null;
        Log.d(TAG, "syncMainOverlayAttachment: enabled=" + enabled + " attached=" + attached);
        if (enabled && !attached) {
            try {
                windowManager.addView(panel, params);
                Log.d(TAG, "overlay added");
            } catch (Throwable t) {
                Log.e(TAG, "overlay add failed", t);
            }
            return;
        }
        if (!enabled && attached) {
            try {
                windowManager.removeView(panel);
                Log.d(TAG, "overlay removed by preference or display policy");
            } catch (Throwable t) {
                Log.e(TAG, "overlay remove failed", t);
            }
        }
    }

    private void ensureClusterMirror() {
        clusterMirrorEnabled = AppPrefs.isClusterMirrorEnabled(this);
        if (!clusterMirrorEnabled) {
            clusterMirrorRetryCount = 0;
            dismissClusterMirror();
            return;
        }
        if (shouldHideClusterMirrorForInactiveNavigation()) {
            clusterMirrorRetryCount = 0;
            dismissClusterMirror();
            return;
        }
        activateClusterBridge();
        Display display = findClusterDisplay();
        if (display == null) {
            dismissClusterMirror();
            Log.w(TAG, "cluster mirror enabled but no secondary display found");
            if (clusterMirrorRetryCount < 5) {
                clusterMirrorRetryCount++;
                mainHandler.postDelayed(() -> {
                    if (AppPrefs.isClusterMirrorEnabled(this)) {
                        ensureClusterMirror();
                    }
                }, 2500L);
            }
            return;
        }
        float requestedClusterScale = AppPrefs.getClusterScale(this);
        float nextClusterScale = requestedClusterScale;
        boolean scaleChanged = Math.abs(nextClusterScale - clusterScale) > 0.001f;
        clusterMirrorRetryCount = 0;
        if (clusterPanel != null && clusterDisplay != null
                && clusterDisplay.getDisplayId() == display.getDisplayId()
                && !scaleChanged) {
            updateClusterPosition();
            return;
        }
        dismissClusterMirror();
        clusterScale = nextClusterScale;
        clusterDisplay = display;
        try {
            clusterContext = createDisplayContext(display);
        } catch (Throwable t) {
            Log.e(TAG, "createDisplayContext failed", t);
            clusterContext = this;
        }
        if (clusterContext == null) {
            clusterContext = this;
        }
        clusterWindowManager = (WindowManager) clusterContext.getSystemService(WINDOW_SERVICE);
        if (clusterWindowManager == null) {
            Log.e(TAG, "cluster WindowManager is null");
            return;
        }
        clusterPanel = buildPanelForContext(clusterContext, clusterScale, true);
        clusterPanel.addOnLayoutChangeListener(clusterBoundsListener);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        clusterParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                PixelFormat.TRANSLUCENT);
        clusterParams.gravity = Gravity.TOP | Gravity.LEFT;
        clusterParams.x = getSavedClusterX();
        clusterParams.y = getSavedClusterY();

        clusterPanel.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    clusterDownRawX = event.getRawX();
                    clusterDownRawY = event.getRawY();
                    clusterDownX = clusterParams.x;
                    clusterDownY = clusterParams.y;
                    clusterDragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getRawX() - clusterDownRawX) > dp(4)
                            || Math.abs(event.getRawY() - clusterDownRawY) > dp(4)) {
                        clusterDragging = true;
                    }
                    clusterParams.x = clusterDownX + Math.round(event.getRawX() - clusterDownRawX);
                    clusterParams.y = clusterDownY + Math.round(event.getRawY() - clusterDownRawY);
                    updateClusterPosition();
                    return true;
                case MotionEvent.ACTION_UP:
                    saveClusterPosition();
                    return true;
                default:
                    return true;
            }
        });

        try {
            clusterWindowManager.addView(clusterPanel, clusterParams);
            clusterPanel.post(this::updateClusterPosition);
            syncClusterFromMain();
            applyContentVisibilityPrefs();
            mainHandler.postDelayed(() -> {
                if (clusterParams != null && clusterPanel != null && clusterPanel.getParent() != null) {
                    clusterParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                    try {
                        clusterWindowManager.updateViewLayout(clusterPanel, clusterParams);
                    } catch (Throwable ignored) {}
                }
            }, 4000);
            Log.d(TAG, "cluster mirror shown on display " + display.getDisplayId()
                    + ", requestedScale=" + requestedClusterScale
                    + ", appliedScale=" + clusterScale);
        } catch (Throwable t) {
            Log.e(TAG, "cluster mirror add failed", t);
            dismissClusterMirror();
        }
    }

    private boolean canUseOverlayWindowType() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        try {
            Method method = android.provider.Settings.class
                    .getMethod("canDrawOverlays", Context.class);
            Object result = method.invoke(null, this);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void rebuildClusterMirrorForStyleChange() {
        dismissClusterMirror();
        if (pendingClusterMirrorRebuild != null) {
            mainHandler.removeCallbacks(pendingClusterMirrorRebuild);
        }
        pendingClusterMirrorRebuild = () -> {
            pendingClusterMirrorRebuild = null;
            if (!AppPrefs.isClusterMirrorEnabled(this)) {
                return;
            }
            ensureClusterMirror();
            syncClusterFromMain();
            applyContentVisibilityPrefs();
            updateClusterPosition();
        };
        mainHandler.postDelayed(pendingClusterMirrorRebuild, 120L);
    }

    private void rebuildOverlaysForStyleChange() {
        if (pendingClusterMirrorRebuild != null) {
            mainHandler.removeCallbacks(pendingClusterMirrorRebuild);
            pendingClusterMirrorRebuild = null;
        }
        boolean rebuildCluster = AppPrefs.isClusterMirrorEnabled(this);
        dismissClusterMirror();
        rebuildOverlay();
        applyContentVisibilityPrefs();
        if (!rebuildCluster) {
            return;
        }
        pendingClusterMirrorRebuild = () -> {
            pendingClusterMirrorRebuild = null;
            if (!AppPrefs.isClusterMirrorEnabled(this)) {
                return;
            }
            ensureClusterMirror();
            syncClusterFromMain();
            applyContentVisibilityPrefs();
            updateClusterPosition();
        };
        mainHandler.postDelayed(pendingClusterMirrorRebuild, 160L);
    }

    private Display findClusterDisplay() {
        DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (manager == null) {
            return null;
        }
        int preferredDisplayId = AppPrefs.getClusterDisplayId(this);
        if (preferredDisplayId >= 0) {
            Display[] displays = manager.getDisplays();
            for (Display display : displays) {
                if (display != null && display.getDisplayId() == preferredDisplayId) {
                    return display;
                }
            }
            Log.w(TAG, "preferred cluster display missing: " + preferredDisplayId);
            return null;
        }
        Display[] presentationDisplays = manager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        for (Display display : presentationDisplays) {
            if (display != null && display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                return display;
            }
        }
        Display[] displays = manager.getDisplays();
        for (Display display : displays) {
            if (display != null && display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                return display;
            }
        }
        return null;
    }

    private LinearLayout buildCardPanel(Context context, float scale, boolean cluster) {
        LinearLayout card = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.panel_card, null);

        // Dynamic background
        int padH = scaledDp(5, scale);
        int padTop = scaledDp(4, scale);
        int padBottom = scaledDp(2, scale);
        card.setPadding(padH, padTop, padH, padBottom);
        GradientDrawable bg = new GradientDrawable();
        int opacity = AppPrefs.getBackgroundOpacityPercent(this);
        bg.setColor(withAlpha(AppPrefs.getBackgroundColor(this), opacity));
        bg.setCornerRadius(scaledDp(12, scale));
        bg.setStroke(scaledDp(1, scale), withAlpha(0xFFFFFFFF, AppPrefs.strokeOpacityForBackground(opacity)));
        card.setBackground(bg);

        TextView mode = (TextView) card.findViewById(R.id.mode_text);
        mode.setText("待接收导航/巡航信息");
        mode.setTextColor(primaryTextColor());
        mode.setTextSize(scaledSp(13f, scale));
        mode.setGravity(Gravity.CENTER);
        mode.setSingleLine(true);

        // ===== CRUISE ROW 1 =====
        LinearLayout cruiseRow1 = (LinearLayout) card.findViewById(R.id.card_cruise_row1);
        cruiseRow1.getLayoutParams().height = scaledDp(32, scale);

        TextView roadText = (TextView) card.findViewById(R.id.compact_cruise_road_text);
        roadText.setTextColor(primaryTextColor());
        roadText.setTextSize(scaledSp(14f, scale));
        roadText.setPadding(0, 0, scaledDp(8, scale), 0);

        TextView dirText = (TextView) card.findViewById(R.id.compact_cruise_dir_text);
        dirText.setTextColor(primaryTextColor());
        dirText.setTextSize(scaledSp(14f, scale));
        dirText.setPadding(0, 0, 0, 0);

        // Replace cruise edog placeholder
        LinearLayout cruiseEdogPlaceholder = (LinearLayout) card.findViewById(R.id.card_cruise_edog_row);
        cruiseEdogPlaceholder.removeAllViews();
        LinearLayout cruiseEdog = buildEdogAlertRow(context, scale);
        cruiseEdog.setVisibility(View.VISIBLE);
        cruiseEdogPlaceholder.addView(cruiseEdog, new LinearLayout.LayoutParams(-2, -2));

        // ===== CRUISE ROW 2 =====
        LinearLayout cruiseRow2 = (LinearLayout) card.findViewById(R.id.card_cruise_row2);
        cruiseRow2.getLayoutParams().height = scaledDp(35, scale);
        LinearLayout.LayoutParams r2lp = (LinearLayout.LayoutParams) cruiseRow2.getLayoutParams();
        r2lp.topMargin = scaledDp(3, scale);

        LinearLayout cruiseLaneBox = (LinearLayout) card.findViewById(R.id.card_cruise_lane_box);
        LaneBarView cruiseLane = installLaneBar(card, R.id.card_cruise_lane_placeholder, scale, 0.9f, 32, 2, true, true, 1);

        LinearLayout cruiseLights = (LinearLayout) card.findViewById(R.id.card_cruise_light_row);

        // ===== NAV AREA =====
        LinearLayout navArea = (LinearLayout) card.findViewById(R.id.card_nav_area);

        LinearLayout navLeft = (LinearLayout) card.findViewById(R.id.nav_turn_box);
        LinearLayout.LayoutParams leftLp = (LinearLayout.LayoutParams) navLeft.getLayoutParams();
        leftLp.rightMargin = scaledDp(8, scale);

        ImageView navIcon = (ImageView) card.findViewById(R.id.nav_turn_icon);
        int iconSize = scaledDp(44, scale);
        LinearLayout.LayoutParams iconLp = (LinearLayout.LayoutParams) navIcon.getLayoutParams();
        iconLp.width = iconSize;
        iconLp.height = iconSize;
        if (cluster) {
            clusterNavExitText = wrapArrowWithExitOverlay(navIcon, scale);
        } else {
            navExitText = wrapArrowWithExitOverlay(navIcon, scale);
        }

        TextView navDist = (TextView) card.findViewById(R.id.nav_turn_dist);
        navDist.setTextColor(primaryTextColor());
        navDist.setTextSize(scaledSp(16f, scale));

        TextView navRoad = (TextView) card.findViewById(R.id.compact_nav_turn_road_text);
        navRoad.setTextColor(primaryTextColor());
        navRoad.setTextSize(scaledSp(15f, scale));
        View navInfoRow = (View) navRoad.getParent();
        if (navInfoRow != null) {
            ViewGroup.LayoutParams infoLp = navInfoRow.getLayoutParams();
            infoLp.height = scaledDp(30, scale);
            navInfoRow.setLayoutParams(infoLp);
        }

        TextView navEta = (TextView) card.findViewById(R.id.eta_text);
        navEta.setTextColor(primaryTextColor());
        navEta.setTextSize(scaledSp(15f, scale));
        navEta.setPadding(scaledDp(6, scale), 0, 0, 0);

        LinearLayout navLaneBox = (LinearLayout) card.findViewById(R.id.lane_section);
        LaneBarView navLane = installLaneBar(card, R.id.lane_bar_placeholder, scale, 0.9f, 36, 2, true, true, 1);
        View navDetailRow = (View) navLaneBox.getParent();
        if (navDetailRow != null) {
            ViewGroup.LayoutParams detailParams = navDetailRow.getLayoutParams();
            detailParams.height = scaledDp(42, scale);
            if (detailParams instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) detailParams).topMargin = scaledDp(2, scale);
            }
            navDetailRow.setLayoutParams(detailParams);
        }

        LinearLayout navLights = (LinearLayout) card.findViewById(R.id.light_row);

        // Replace nav edog placeholder
        LinearLayout navEdogPlaceholder = (LinearLayout) card.findViewById(R.id.card_nav_edog_row);
        navEdogPlaceholder.removeAllViews();
        LinearLayout navEdog = buildEdogAlertRow(context, scale);
        navEdog.setVisibility(View.VISIBLE);
        navEdogPlaceholder.addView(navEdog, new LinearLayout.LayoutParams(-2, -2));

        // Assign field references
        if (cluster) {
            clusterPanel = card;
            clusterCardCruiseRow1 = cruiseRow1;
            clusterCardCruiseRow2 = cruiseRow2;
            clusterCardNavArea = navArea;
            clusterModeText = mode;
            clusterNavTurnBox = navLeft;
            clusterNavTurnIconView = navIcon;
            clusterNavTurnDistText = navDist;
            clusterCompactCruiseDirText = dirText;
            clusterCompactCruiseRoadText = roadText;
            clusterCompactNavTurnRoadText = navRoad;
            clusterEtaText = navEta;
            clusterLaneSection = navLaneBox;
            clusterLaneBar = navLane;
            clusterLightRow = navLights;
            clusterAlertRow = navEdog;
            clusterCardCruiseLaneSection = cruiseLaneBox;
            clusterCardNavLaneSection = navLaneBox;
            clusterCardCruiseLaneBar = cruiseLane;
            clusterCardCruiseLightRow = cruiseLights;
            clusterCardCruiseEdogRow = cruiseEdog;
            clusterCardNavLaneBar = navLane;
            clusterCardNavLightRow = navLights;
            clusterCardNavEdogRow = navEdog;
        } else {
            panel = card;
            cardCruiseRow1 = cruiseRow1;
            cardCruiseRow2 = cruiseRow2;
            cardNavArea = navArea;
            modeText = mode;
            navTurnBox = navLeft;
            navTurnIconView = navIcon;
            navTurnDistText = navDist;
            compactCruiseDirText = dirText;
            compactCruiseRoadText = roadText;
            compactNavTurnRoadText = navRoad;
            etaText = navEta;
            laneSection = navLaneBox;
            laneBar = navLane;
            lightRow = navLights;
            alertRow = navEdog;
            cardCruiseLaneSection = cruiseLaneBox;
            cardNavLaneSection = navLaneBox;
            cardCruiseLaneBar = cruiseLane;
            cardCruiseLightRow = cruiseLights;
            cardCruiseEdogRow = cruiseEdog;
            cardNavLaneBar = navLane;
            cardNavLightRow = navLights;
            cardNavEdogRow = navEdog;
        }
        applyTextPalette();
        updateCardLayout();
        return card;
    }

    private LinearLayout buildClassicPanel(Context context, float scale, boolean cluster) {
        if (shouldUseXmlClassicPanel()) {
            return buildClassicPanelFromXml(context, scale, cluster);
        }

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(scaledDp(12, scale), scaledDp(10, scale), scaledDp(12, scale), scaledDp(10, scale));
        root.setBackground(cluster ? createClusterPanelBackground() : createMainPanelBackground());

        TextView mode = new TextView(context);
        mode.setTextSize(scaledSp(13f, scale));
        mode.setSingleLine(true);
        mode.setGravity(Gravity.CENTER);
        mode.setText("待接收导航/巡航信息");
        root.addView(mode, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout turnRow = new LinearLayout(context);
        turnRow.setOrientation(LinearLayout.VERTICAL);
        turnRow.setGravity(Gravity.CENTER);
        turnRow.setPadding(scaledDp(14, scale), scaledDp(8, scale), scaledDp(16, scale), scaledDp(9, scale));
        GradientDrawable turnBg = new GradientDrawable();
        turnBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        turnBg.setColors(new int[]{0xFF1D4ED8, 0xFF0891B2});
        turnBg.setCornerRadius(scaledDp(10, scale));
        turnRow.setBackground(turnBg);
        turnRow.setVisibility(View.GONE);
        turnRow.setMinimumHeight(scaledDp(62, scale));

        TextView turn = new TextView(context);
        turn.setTextColor(Color.WHITE);
        turn.setTextSize(scaledSp(22f, scale));
        turn.setTypeface(Typeface.DEFAULT_BOLD);
        turn.setSingleLine(true);
        turn.setMaxLines(1);
        turn.setEllipsize(TextUtils.TruncateAt.END);
        turn.setGravity(Gravity.CENTER);
        turnRow.addView(turn, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout turnDetailRow = new LinearLayout(context);
        turnDetailRow.setOrientation(LinearLayout.HORIZONTAL);
        turnDetailRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailRowLp = new LinearLayout.LayoutParams(-2, -2);
        detailRowLp.setMargins(0, scaledDp(4, scale), 0, 0);
        turnRow.addView(turnDetailRow, detailRowLp);

        ImageView turnIcon = new ImageView(context);
        turnIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        turnIcon.setVisibility(View.GONE);
        LinearLayout.LayoutParams turnIconLp = new LinearLayout.LayoutParams(scaledDp(30, scale), scaledDp(30, scale));
        turnIconLp.setMargins(0, 0, scaledDp(7, scale), 0);
        turnDetailRow.addView(turnIcon, turnIconLp);
        if (cluster) {
            clusterTurnExitText = wrapArrowWithExitOverlay(turnIcon, scale);
        } else {
            turnExitText = wrapArrowWithExitOverlay(turnIcon, scale);
        }

        TextView turnDistance = new TextView(context);
        turnDistance.setTextColor(Color.WHITE);
        turnDistance.setTextSize(scaledSp(22f, scale));
        turnDistance.setTypeface(Typeface.DEFAULT_BOLD);
        turnDistance.setGravity(Gravity.CENTER);
        turnDetailRow.addView(turnDistance, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout.LayoutParams turnRowLp = new LinearLayout.LayoutParams(-2, -2);
        turnRowLp.setMargins(0, scaledDp(6, scale), 0, scaledDp(5, scale));
        root.addView(turnRow, turnRowLp);

        LinearLayout laneBox = new LinearLayout(context);
        laneBox.setOrientation(LinearLayout.VERTICAL);
        laneBox.setGravity(Gravity.CENTER_HORIZONTAL);
        laneBox.setPadding(scaledDp(8, scale), scaledDp(5, scale), scaledDp(8, scale), scaledDp(7, scale));
        laneBox.setVisibility(View.GONE);

        LaneBarView lane = new LaneBarView(context);
        lane.setFrameScaleMultiplier(scale);
        lane.setScaleMultiplier(1.5f);
        LinearLayout.LayoutParams laneLp = new LinearLayout.LayoutParams(-2, -2);
        laneLp.setMargins(0, 0, 0, 0);
        laneBox.addView(lane, laneLp);
        LinearLayout.LayoutParams laneSectionLp = new LinearLayout.LayoutParams(-2, -2);
        laneSectionLp.setMargins(0, scaledDp(5, scale), 0, scaledDp(4, scale));
        root.addView(laneBox, laneSectionLp);

        LinearLayout lights = new LinearLayout(context);
        lights.setOrientation(LinearLayout.HORIZONTAL);
        lights.setGravity(Gravity.CENTER);
        lights.setVisibility(View.GONE);
        root.addView(lights, new LinearLayout.LayoutParams(-2, -2));

        TextView serviceArea = compactText(context, 13f, false, scale);
        serviceArea.setSingleLine(false);
        serviceArea.setMaxLines(4);
        serviceArea.setGravity(Gravity.CENTER);
        serviceArea.setVisibility(View.GONE);
        LinearLayout.LayoutParams serviceAreaLp = new LinearLayout.LayoutParams(-2, -2);
        serviceAreaLp.setMargins(0, scaledDp(3, scale), 0, 0);
        root.addView(serviceArea, serviceAreaLp);

        TextView eta = new TextView(context);
        eta.setTextSize(scaledSp(15f, scale));
        eta.setSingleLine(false);
        eta.setMaxLines(4);
        eta.setGravity(Gravity.CENTER);
        eta.setVisibility(View.GONE);
        root.addView(eta, new LinearLayout.LayoutParams(-2, -2));

        TextView alert = compactText(context, 14f, false, scale);
        alert.setVisibility(View.GONE);
        root.addView(alert, new LinearLayout.LayoutParams(-2, 0));

        LinearLayout edogAlertRow = buildEdogAlertRow(context, scale);
        edogAlertRow.setVisibility(View.GONE);
        LinearLayout.LayoutParams alertLp = new LinearLayout.LayoutParams(-2, -2);
        alertLp.setMargins(0, scaledDp(5, scale), 0, 0);
        root.addView(edogAlertRow, alertLp);

        TextView detail = compactText(context, 12f, true, scale);
        detail.setMaxLines(4);
        detail.setVisibility(View.GONE);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-2, -2);
        detailLp.setMargins(0, scaledDp(3, scale), 0, 0);
        root.addView(detail, detailLp);

        if (cluster) {
            clusterPanel = root;
            clusterModeRow = null;
            clusterModeText = mode;
            clusterTitleText = null;
            clusterSummaryDivider = null;
            clusterSummaryRow = null;
            clusterHeadingInfoText = null;
            clusterRoadInfoText = null;
            clusterTurnCard = null;
            clusterTurnLeadText = null;
            clusterTurnText = turn;
            clusterTurnDistanceText = turnDistance;
            clusterTurnIconView = turnIcon;
            clusterTurnRowLayout = turnRow;
            clusterLaneSection = laneBox;
            clusterLaneBar = lane;
            clusterLightRow = lights;
            clusterServiceAreaText = serviceArea;
            clusterEtaText = eta;
            clusterAlertCard = null;
            clusterLimitBadgeText = null;
            clusterAlertCaptionText = null;
            clusterAlertText = alert;
            clusterAlertRow = edogAlertRow;
            clusterDetailText = detail;
        } else {
            panel = root;
            modeRow = null;
            modeText = mode;
            titleText = null;
            summaryDivider = null;
            summaryRow = null;
            headingInfoText = null;
            roadInfoText = null;
            turnCard = null;
            turnLeadText = null;
            turnText = turn;
            turnDistanceText = turnDistance;
            turnIconView = turnIcon;
            turnRowLayout = turnRow;
            laneSection = laneBox;
            laneBar = lane;
            lightRow = lights;
            serviceAreaText = serviceArea;
            etaText = eta;
            alertCard = null;
            limitBadgeText = null;
            alertCaptionText = null;
            alertText = alert;
            alertRow = edogAlertRow;
            detailText = detail;
        }

        applyTextPalette();
        return root;
    }

    private boolean shouldUseXmlClassicPanel() {
        return true;
    }

    private LinearLayout buildClassicPanelFromXml(Context context, float scale, boolean cluster) {
        LinearLayout root = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.panel_classic, null);
        root.setPadding(scaledDp(12, scale), scaledDp(10, scale), scaledDp(12, scale), scaledDp(10, scale));
        root.setBackground(cluster ? createClusterPanelBackground() : createMainPanelBackground());

        TextView mode = (TextView) root.findViewById(R.id.mode_text);
        mode.setText("待接收导航/巡航信息");
        mode.setTextSize(scaledSp(13f, scale));

        LinearLayout turnRow = (LinearLayout) root.findViewById(R.id.turn_row);
        turnRow.setPadding(scaledDp(14, scale), scaledDp(8, scale), scaledDp(16, scale), scaledDp(9, scale));
        turnRow.setMinimumHeight(scaledDp(62, scale));
        GradientDrawable turnBg = new GradientDrawable();
        turnBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        turnBg.setColors(new int[]{0xFF1D4ED8, 0xFF0891B2});
        turnBg.setCornerRadius(scaledDp(10, scale));
        turnRow.setBackground(turnBg);
        LinearLayout.LayoutParams turnRowLp = (LinearLayout.LayoutParams) turnRow.getLayoutParams();
        turnRowLp.setMargins(0, scaledDp(6, scale), 0, scaledDp(5, scale));

        TextView turn = (TextView) root.findViewById(R.id.turn_text);
        turn.setTextSize(scaledSp(22f, scale));

        ImageView turnIcon = (ImageView) root.findViewById(R.id.turn_icon);
        LinearLayout.LayoutParams turnIconLp = (LinearLayout.LayoutParams) turnIcon.getLayoutParams();
        turnIconLp.width = scaledDp(30, scale);
        turnIconLp.height = scaledDp(30, scale);
        turnIconLp.setMargins(0, 0, scaledDp(7, scale), 0);
        if (cluster) {
            clusterTurnExitText = wrapArrowWithExitOverlay(turnIcon, scale);
        } else {
            turnExitText = wrapArrowWithExitOverlay(turnIcon, scale);
        }

        TextView turnDistance = (TextView) root.findViewById(R.id.turn_distance);
        turnDistance.setTextSize(scaledSp(22f, scale));

        LinearLayout laneBox = (LinearLayout) root.findViewById(R.id.lane_section);
        laneBox.setPadding(scaledDp(8, scale), scaledDp(5, scale), scaledDp(8, scale), scaledDp(7, scale));
        LinearLayout.LayoutParams laneBoxLp = (LinearLayout.LayoutParams) laneBox.getLayoutParams();
        laneBoxLp.setMargins(0, scaledDp(5, scale), 0, scaledDp(4, scale));
        LaneBarView lane = installLaneBarSimple(root, R.id.lane_bar_placeholder, scale, 1.5f);

        LinearLayout lights = (LinearLayout) root.findViewById(R.id.light_row);

        TextView serviceArea = (TextView) root.findViewById(R.id.service_area_text);
        serviceArea.setTextSize(scaledSp(13f, scale));
        serviceArea.setSingleLine(false);
        serviceArea.setMaxLines(4);
        LinearLayout.LayoutParams serviceAreaLp = (LinearLayout.LayoutParams) serviceArea.getLayoutParams();
        serviceAreaLp.setMargins(0, scaledDp(3, scale), 0, 0);

        TextView eta = (TextView) root.findViewById(R.id.eta_text);
        eta.setTextSize(scaledSp(15f, scale));

        TextView alert = (TextView) root.findViewById(R.id.alert_text);
        alert.setTextSize(scaledSp(14f, scale));

        LinearLayout edogAlertRow = (LinearLayout) root.findViewById(R.id.alert_row);
        edogAlertRow.removeAllViews();
        LinearLayout dynamicEdog = buildEdogAlertRow(context, scale);
        dynamicEdog.setVisibility(View.VISIBLE);
        edogAlertRow.addView(dynamicEdog, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams alertLp = (LinearLayout.LayoutParams) edogAlertRow.getLayoutParams();
        alertLp.setMargins(0, scaledDp(5, scale), 0, 0);

        TextView detail = (TextView) root.findViewById(R.id.detail_text);
        detail.setTextSize(scaledSp(12f, scale));
        LinearLayout.LayoutParams detailLp = (LinearLayout.LayoutParams) detail.getLayoutParams();
        detailLp.setMargins(0, scaledDp(3, scale), 0, 0);

        if (cluster) {
            clusterPanel = root;
            clusterModeRow = null;
            clusterModeText = mode;
            clusterTitleText = null;
            clusterSummaryDivider = null;
            clusterSummaryRow = null;
            clusterHeadingInfoText = null;
            clusterRoadInfoText = null;
            clusterTurnCard = null;
            clusterTurnLeadText = null;
            clusterTurnText = turn;
            clusterTurnDistanceText = turnDistance;
            clusterTurnIconView = turnIcon;
            clusterTurnRowLayout = turnRow;
            clusterLaneSection = laneBox;
            clusterLaneBar = lane;
            clusterLightRow = lights;
            clusterServiceAreaText = serviceArea;
            clusterEtaText = eta;
            clusterAlertCard = null;
            clusterLimitBadgeText = null;
            clusterAlertCaptionText = null;
            clusterAlertText = alert;
            clusterAlertRow = edogAlertRow;
            clusterDetailText = detail;
        } else {
            panel = root;
            modeRow = null;
            modeText = mode;
            titleText = null;
            summaryDivider = null;
            summaryRow = null;
            headingInfoText = null;
            roadInfoText = null;
            turnCard = null;
            turnLeadText = null;
            turnText = turn;
            turnDistanceText = turnDistance;
            turnIconView = turnIcon;
            turnRowLayout = turnRow;
            laneSection = laneBox;
            laneBar = lane;
            lightRow = lights;
            serviceAreaText = serviceArea;
            etaText = eta;
            alertCard = null;
            limitBadgeText = null;
            alertCaptionText = null;
            alertText = alert;
            alertRow = edogAlertRow;
            detailText = detail;
        }

        applyTextPalette();
        return root;
    }

    private LinearLayout buildDashboardPanel(Context context, float scale, boolean cluster) {
        LinearLayout root = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.panel_dashboard, null);

        // Dynamic scaling
        root.setPadding(scaledDp(cluster ? 8 : 10, scale), scaledDp(cluster ? 7 : 9, scale),
                scaledDp(cluster ? 8 : 10, scale), scaledDp(cluster ? 7 : 9, scale));
        root.setMinimumWidth(scaledDp(cluster ? 300 : 314, scale));
        root.setBackground(cluster ? createClusterPanelBackground() : createMainPanelBackground());

        LinearLayout header = (LinearLayout) root.findViewById(R.id.mode_row);

        // Badge: apply dynamic circle background
        TextView badge = (TextView) root.findViewById(R.id.mode_badge);
        badge.setTextSize(scaledSp(16f, scale));
        LinearLayout.LayoutParams badgeLp = (LinearLayout.LayoutParams) badge.getLayoutParams();
        badgeLp.width = scaledDp(32, scale);
        badgeLp.height = scaledDp(32, scale);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        badgeBg.setColor(0xCC0A1422);
        badgeBg.setStroke(scaledDp(2, scale), 0xFF3B82F6);
        badge.setBackground(badgeBg);

        TextView mode = (TextView) root.findViewById(R.id.mode_text);
        mode.setTextSize(scaledSp(13f, scale));
        LinearLayout.LayoutParams modeLp = (LinearLayout.LayoutParams) mode.getLayoutParams();
        modeLp.leftMargin = scaledDp(8, scale);

        TextView title = (TextView) root.findViewById(R.id.title_text);
        title.setTextSize(scaledSp(cluster ? 24f : 26f, scale));
        LinearLayout.LayoutParams titleLp = (LinearLayout.LayoutParams) title.getLayoutParams();
        titleLp.topMargin = scaledDp(cluster ? 10 : 12, scale);
        titleLp.bottomMargin = scaledDp(cluster ? 6 : 8, scale);

        View divider = root.findViewById(R.id.summary_divider);
        divider.setBackgroundColor(withAlpha(0xFFFFFFFF, 12));
        LinearLayout.LayoutParams dividerLp = (LinearLayout.LayoutParams) divider.getLayoutParams();
        dividerLp.width = scaledDp(220, scale);
        dividerLp.height = scaledDp(1, scale);

        LinearLayout summary = (LinearLayout) root.findViewById(R.id.summary_row);
        summary.setBackground(createSectionBackground(scale));
        summary.setPadding(scaledDp(9, scale), scaledDp(11, scale), scaledDp(9, scale), scaledDp(11, scale));
        LinearLayout.LayoutParams summaryLp = sectionLp(scale, cluster ? 5f : 9f);
        summary.setLayoutParams(summaryLp);

        TextView heading = (TextView) root.findViewById(R.id.heading_info_text);
        heading.setTextSize(scaledSp(14f, scale));

        View summaryMid = root.findViewById(R.id.summary_mid_divider);
        summaryMid.setBackgroundColor(withAlpha(0xFFFFFFFF, 10));
        LinearLayout.LayoutParams summaryMidLp = (LinearLayout.LayoutParams) summaryMid.getLayoutParams();
        summaryMidLp.width = scaledDp(1, scale);
        summaryMidLp.height = scaledDp(42, scale);
        summaryMidLp.leftMargin = scaledDp(8, scale);
        summaryMidLp.rightMargin = scaledDp(8, scale);

        TextView roadInfo = (TextView) root.findViewById(R.id.road_info_text);
        roadInfo.setTextSize(scaledSp(14f, scale));

        LinearLayout turnBox = (LinearLayout) root.findViewById(R.id.turn_card);
        turnBox.setBackground(createSectionBackground(scale));
        turnBox.setPadding(scaledDp(cluster ? 9 : 10, scale), scaledDp(cluster ? 10 : 11, scale),
                scaledDp(cluster ? 9 : 10, scale), scaledDp(cluster ? 10 : 11, scale));
        turnBox.setLayoutParams(sectionLp(scale, cluster ? 5f : 9f));

        ImageView turnLeadIcon = (ImageView) root.findViewById(R.id.turn_lead_icon);
        int turnLeadIconSize = scaledDp(24, scale);
        LinearLayout.LayoutParams turnLeadIconLp = (LinearLayout.LayoutParams) turnLeadIcon.getLayoutParams();
        turnLeadIconLp.width = turnLeadIconSize;
        turnLeadIconLp.height = turnLeadIconSize;
        if (cluster) {
            clusterTurnLeadExitText = wrapArrowWithExitOverlay(turnLeadIcon, scale);
        } else {
            turnLeadExitText = wrapArrowWithExitOverlay(turnLeadIcon, scale);
        }

        TextView turnLead = (TextView) root.findViewById(R.id.turn_lead_text);
        turnLead.setTextSize(scaledSp(13f, scale));
        LinearLayout.LayoutParams turnLeadTextLp = (LinearLayout.LayoutParams) turnLead.getLayoutParams();
        turnLeadTextLp.leftMargin = scaledDp(3, scale);

        TextView turnRoad = (TextView) root.findViewById(R.id.turn_text);
        turnRoad.setTextSize(scaledSp(cluster ? 22f : 24f, scale));
        LinearLayout.LayoutParams turnRoadLp = (LinearLayout.LayoutParams) turnRoad.getLayoutParams();
        turnRoadLp.topMargin = scaledDp(6, scale);

        TextView turnDistance = (TextView) root.findViewById(R.id.turn_distance);
        turnDistance.setTextSize(scaledSp(cluster ? 22f : 24f, scale));
        LinearLayout.LayoutParams turnDistanceLp = (LinearLayout.LayoutParams) turnDistance.getLayoutParams();
        turnDistanceLp.leftMargin = scaledDp(12, scale);

        LinearLayout laneBox = (LinearLayout) root.findViewById(R.id.lane_section);
        laneBox.setPadding(scaledDp(7, scale), scaledDp(7, scale), scaledDp(7, scale), scaledDp(8, scale));
        laneBox.setLayoutParams(sectionLp(scale, cluster ? 5f : 8f));
        LaneBarView lane = installLaneBarSimple(root, R.id.lane_bar_placeholder, scale, 1.5f);

        LinearLayout lights = (LinearLayout) root.findViewById(R.id.light_row);
        lights.setLayoutParams(sectionLp(scale, cluster ? 5f : 6f));

        TextView eta = (TextView) root.findViewById(R.id.eta_text);
        eta.setTextSize(scaledSp(14f, scale));
        eta.setPadding(scaledDp(8, scale), scaledDp(8, scale), scaledDp(8, scale), scaledDp(8, scale));
        eta.setBackground(createSectionBackground(scale));
        eta.setLayoutParams(sectionLp(scale, cluster ? 5f : 8f));

        LinearLayout alertBox = (LinearLayout) root.findViewById(R.id.alert_card);
        alertBox.setPadding(scaledDp(9, scale), scaledDp(10, scale), scaledDp(9, scale), scaledDp(10, scale));
        alertBox.setBackground(createSectionBackground(scale));
        alertBox.setLayoutParams(sectionLp(scale, cluster ? 5f : 8f));

        // Replace edog placeholder with dynamic edog row
        LinearLayout edogPlaceholder = (LinearLayout) root.findViewById(R.id.alert_row);
        edogPlaceholder.removeAllViews();
        LinearLayout edogAlertRow = buildEdogAlertRow(context, scale);
        edogPlaceholder.addView(edogAlertRow, new LinearLayout.LayoutParams(-2, -2));

        TextView limitBadge = (TextView) root.findViewById(R.id.limit_badge_text);
        limitBadge.setTextSize(scaledSp(14f, scale));

        TextView alertCaption = (TextView) root.findViewById(R.id.alert_caption_text);
        alertCaption.setTextSize(scaledSp(12f, scale));

        TextView alert = (TextView) root.findViewById(R.id.alert_text);
        alert.setTextSize(scaledSp(14f, scale));
        alert.setPadding(0, scaledDp(4, scale), 0, 0);

        TextView detail = (TextView) root.findViewById(R.id.detail_text);
        detail.setTextSize(scaledSp(12f, scale));
        detail.setPadding(scaledDp(8, scale), scaledDp(8, scale), scaledDp(8, scale), scaledDp(8, scale));
        detail.setBackground(createSectionBackground(scale));
        detail.setLayoutParams(sectionLp(scale, cluster ? 5f : 6f));

        if (cluster) {
            clusterPanel = root;
            clusterModeRow = header;
            clusterModeText = mode;
            clusterTitleText = title;
            clusterSummaryDivider = divider;
            clusterSummaryRow = summary;
            clusterHeadingInfoText = heading;
            clusterRoadInfoText = roadInfo;
            clusterTurnCard = turnBox;
            clusterTurnLeadText = turnLead;
            clusterTurnLeadIconView = turnLeadIcon;
            clusterTurnText = turnRoad;
            clusterTurnDistanceText = turnDistance;
            clusterLaneSection = laneBox;
            clusterLaneBar = lane;
            clusterLightRow = lights;
            clusterEtaText = eta;
            clusterAlertCard = alertBox;
            clusterLimitBadgeText = limitBadge;
            clusterAlertCaptionText = alertCaption;
            clusterAlertText = alert;
            clusterAlertRow = edogAlertRow;
            clusterDetailText = detail;
        } else {
            panel = root;
            modeRow = header;
            modeText = mode;
            titleText = title;
            summaryDivider = divider;
            summaryRow = summary;
            headingInfoText = heading;
            roadInfoText = roadInfo;
            turnCard = turnBox;
            turnLeadText = turnLead;
            turnLeadIconView = turnLeadIcon;
            turnText = turnRoad;
            turnDistanceText = turnDistance;
            laneSection = laneBox;
            laneBar = lane;
            lightRow = lights;
            etaText = eta;
            alertCard = alertBox;
            limitBadgeText = limitBadge;
            alertCaptionText = alertCaption;
            alertText = alert;
            alertRow = edogAlertRow;
            detailText = detail;
        }

        applyTextPalette();
        refreshRoadTitle();
        refreshStatusSummary();
        refreshTurnCard();
        refreshAlertCard();
        return root;
    }

    private GradientDrawable createDynamicIslandBackground(float scale) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(scaledDp(999, scale));
        int opacity = AppPrefs.getBackgroundOpacityPercent(this);
        bg.setColor(withAlpha(AppPrefs.getBackgroundColor(this), opacity));
        bg.setStroke(scaledDp(1, scale), withAlpha(0xFFFFFFFF, AppPrefs.strokeOpacityForBackground(opacity)));
        return bg;
    }

    private LinearLayout buildDynamicIslandFullPanel(Context context, float scale, boolean cluster) {
        fullModeScale = scale;
        int fullInfoWidth = scaledDp(76, scale);
        DynamicIslandViews island = new DynamicIslandViews();

        LinearLayout root = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.panel_dynamic_island_full, null);
        island.root = root;
        root.setBackground(createDynamicIslandBackground(scale));

        // Content row scaling
        LinearLayout content = (LinearLayout) root.findViewById(R.id.content_row);
        content.setPadding(scaledDp(6, scale), scaledDp(3, scale), scaledDp(6, scale), scaledDp(3, scale));
        content.setMinimumHeight(scaledDp(42, scale));

        TextView mode = (TextView) root.findViewById(R.id.mode_text);
        island.mode = mode;

        // Nav turn section
        LinearLayout navTurn = (LinearLayout) root.findViewById(R.id.nav_turn_box);
        ImageView navIcon = (ImageView) root.findViewById(R.id.nav_turn_icon);
        island.navTurnBox = navTurn;
        island.navIcon = navIcon;
        int navIconSize = scaledDp(28, scale);
        LinearLayout.LayoutParams navIconLp = (LinearLayout.LayoutParams) navIcon.getLayoutParams();
        navIconLp.width = navIconSize;
        navIconLp.height = navIconSize;
        navIconLp.leftMargin = scaledDp(10, scale);
        island.navExitText = wrapArrowWithExitOverlay(navIcon, scale);

        // Turn info column
        LinearLayout distRoadCol = (LinearLayout) root.findViewById(R.id.full_mode_turn_info_col);
        island.turnInfoCol = distRoadCol;
        distRoadCol.getLayoutParams().width = fullInfoWidth;
        LinearLayout.LayoutParams distRoadColLp = (LinearLayout.LayoutParams) distRoadCol.getLayoutParams();
        distRoadColLp.leftMargin = scaledDp(3, scale);
        distRoadColLp.rightMargin = scaledDp(3, scale);

        TextView navDist = (TextView) root.findViewById(R.id.nav_turn_dist);
        island.navDistance = navDist;
        navDist.setTextColor(primaryTextColor());
        navDist.setTextSize(scaledSp(14f, scale));

        TextView navRoad = (TextView) root.findViewById(R.id.compact_nav_turn_road_text);
        island.navRoad = navRoad;
        navRoad.setTextColor(primaryTextColor());
        navRoad.setTextSize(scaledSp(10f, scale));
        navRoad.setHorizontallyScrolling(true);
        navRoad.setMarqueeRepeatLimit(-1);
        navRoad.setSelected(true);
        LinearLayout.LayoutParams navRoadLp = (LinearLayout.LayoutParams) navRoad.getLayoutParams();
        navRoadLp.topMargin = scaledDp(1, scale);

        LinearLayout etaInfoCol = (LinearLayout) root.findViewById(R.id.full_mode_eta_info_col);
        island.etaInfoCol = etaInfoCol;
        etaInfoCol.getLayoutParams().width = fullInfoWidth;
        LinearLayout.LayoutParams etaInfoColLp = (LinearLayout.LayoutParams) etaInfoCol.getLayoutParams();
        etaInfoColLp.leftMargin = scaledDp(3, scale);
        etaInfoColLp.rightMargin = scaledDp(3, scale);

        TextView etaRemainDist = (TextView) root.findViewById(R.id.full_mode_eta_remain_dist);
        island.etaRemainDist = etaRemainDist;
        etaRemainDist.setTextColor(primaryTextColor());
        etaRemainDist.setTextSize(scaledSp(13f, scale));
        etaRemainDist.setSingleLine(true);

        TextView etaArriveTime = (TextView) root.findViewById(R.id.full_mode_eta_arrive_time);
        island.etaArriveTime = etaArriveTime;
        etaArriveTime.setTextColor(primaryTextColor());
        etaArriveTime.setTextSize(scaledSp(10f, scale));
        LinearLayout.LayoutParams etaArriveLp = (LinearLayout.LayoutParams) etaArriveTime.getLayoutParams();
        etaArriveLp.topMargin = scaledDp(1, scale);

        // Cruise left section
        LinearLayout cruiseLeft = (LinearLayout) root.findViewById(R.id.compact_cruise_left);
        island.cruiseLeft = cruiseLeft;
        cruiseLeft.getLayoutParams().width = fullInfoWidth;
        LinearLayout.LayoutParams cruiseLeftLp = (LinearLayout.LayoutParams) cruiseLeft.getLayoutParams();
        cruiseLeftLp.rightMargin = scaledDp(4, scale);

        TextView cruiseRoad = (TextView) root.findViewById(R.id.compact_cruise_road_text);
        island.cruiseRoad = cruiseRoad;
        cruiseRoad.setTextColor(primaryTextColor());
        cruiseRoad.setTextSize(scaledSp(10f, scale));

        TextView cruiseDir = (TextView) root.findViewById(R.id.compact_cruise_dir_text);
        island.cruiseDirection = cruiseDir;
        cruiseDir.setTextColor(primaryTextColor());
        cruiseDir.setTextSize(scaledSp(10f, scale));

        // Lane section
        LinearLayout laneBox = (LinearLayout) root.findViewById(R.id.lane_section);
        island.laneBox = laneBox;
        LinearLayout.LayoutParams laneBoxLp = (LinearLayout.LayoutParams) laneBox.getLayoutParams();
        laneBoxLp.rightMargin = scaledDp(2, scale);
        LaneBarView lane = installLaneBar(root, R.id.lane_bar_placeholder, scale, 0.9f, 36, 2, true, true, 1);
        island.laneBar = lane;

        LinearLayout lights = (LinearLayout) root.findViewById(R.id.light_row);
        island.lightRow = lights;

        LinearLayout widgetRow = (LinearLayout) root.findViewById(R.id.compact_widget_row);
        island.widgetRow = widgetRow;
        LinearLayout.LayoutParams widgetRowLp = (LinearLayout.LayoutParams) widgetRow.getLayoutParams();
        widgetRowLp.leftMargin = scaledDp(3, scale);

        if (cluster) {
            clusterDynamicIslandViews = island;
            clusterPanel = root;
            clusterModeText = mode;
            clusterNavExitText = island.navExitText;
            clusterCompactWidgetRow = widgetRow;
            clusterCompactNavTurnRoadText = navRoad;
            clusterCompactCruiseRoadText = cruiseRoad;
            clusterCompactCruiseDirText = cruiseDir;
            clusterCompactLaneBox = laneBox;
            fullModeClusterTurnInfoCol = distRoadCol;
            fullModeClusterEtaInfoCol = etaInfoCol;
            fullModeClusterEtaRemainDist = etaRemainDist;
            fullModeClusterEtaArriveTime = etaArriveTime;
            clusterNavTurnBox = navTurn;
            clusterNavTurnIconView = navIcon;
            clusterNavTurnDistText = navDist;
            clusterLaneSection = laneBox;
            clusterLaneBar = lane;
            clusterLightRow = lights;
            clusterTurnCard = null;
            clusterTurnText = null;
            clusterTurnDistanceText = null;
            clusterTurnIconView = null;
            clusterTurnDistBadge = null;
            clusterTurnRowLayout = null;
            clusterModeRow = null;
            clusterTitleText = null;
            clusterSummaryDivider = null;
            clusterSummaryRow = null;
            clusterHeadingInfoText = null;
            clusterRoadInfoText = null;
            clusterEtaText = null;
            clusterAlertCard = null;
            clusterLimitBadgeText = null;
            clusterAlertCaptionText = null;
            clusterAlertText = null;
            clusterAlertRow = null;
            clusterDetailText = null;
            clusterTurnLeadText = null;
            clusterTurnLeadIconView = null;
        } else {
            mainDynamicIslandViews = island;
            panel = root;
            modeText = mode;
            navExitText = island.navExitText;
            compactWidgetRow = widgetRow;
            compactNavTurnRoadText = navRoad;
            compactCruiseRoadText = cruiseRoad;
            compactCruiseDirText = cruiseDir;
            compactLaneBox = laneBox;
            fullModeTurnInfoCol = distRoadCol;
            fullModeEtaInfoCol = etaInfoCol;
            fullModeEtaRemainDist = etaRemainDist;
            fullModeEtaArriveTime = etaArriveTime;
            navTurnBox = navTurn;
            navTurnIconView = navIcon;
            navTurnDistText = navDist;
            laneSection = laneBox;
            laneBar = lane;
            lightRow = lights;
            turnCard = null;
            turnText = null;
            turnDistanceText = null;
            turnIconView = null;
            turnDistBadge = null;
            turnRowLayout = null;
            modeRow = null;
            titleText = null;
            summaryDivider = null;
            summaryRow = null;
            headingInfoText = null;
            roadInfoText = null;
            etaText = null;
            alertCard = null;
            limitBadgeText = null;
            alertCaptionText = null;
            alertText = null;
            alertRow = null;
            detailText = null;
            turnLeadText = null;
            turnLeadIconView = null;
        }

        applyTextPalette();
        refreshTurnCard();
        return root;
    }

    private boolean shouldShowStandbyStatusDetails() {
        if (TextUtils.isEmpty(currentModeLabel)) {
            return false;
        }
        return currentModeLabel.startsWith("\u5bfc\u822a")
                || currentModeLabel.startsWith("\u5de1\u822a")
                || currentModeLabel.startsWith("\u6a21\u62df\u5bfc\u822a");
    }

    private LinearLayout.LayoutParams sectionLp(float scale, float topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, scaledDp(topMarginDp, scale), 0, 0);
        return lp;
    }

    private TextView infoBlockText(Context context, String text, float scale) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(scaledSp(13.5f, scale));
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private TextView circleBadge(Context context, String text, float scale, int strokeColor, int fillColor) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(strokeColor);
        view.setTextSize(scaledSp(16f, scale));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(fillColor);
        bg.setStroke(scaledDp(2, scale), strokeColor);
        view.setBackground(bg);
        return view;
    }

    private TextView speedBadge(Context context, String text, float scale) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(Color.WHITE);
        view.setTextSize(scaledSp(20f, scale));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0x12000000);
        bg.setStroke(scaledDp(3, scale), 0xFFEF4444);
        view.setBackground(bg);
        return view;
    }

    private GradientDrawable createSectionBackground(float scale) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(withAlpha(0xFF132131, 86));
        bg.setCornerRadius(scaledDp(14, scale));
        bg.setStroke(scaledDp(1, scale), withAlpha(0xFFFFFFFF, 10));
        return bg;
    }

    // -- Adaptive speed-text ------------------------------------------
    private final android.graphics.Paint fitPaint = new android.graphics.Paint();

    private float fitCircleTextSize(float circleDp, float scale, String text) {
        if (TextUtils.isEmpty(text)) return scaledSp(circleDp * 0.22f, scale);
        float maxWidthPx = scaledDp(Math.round(circleDp * 0.72f), scale);
        float lo = scaledSp(circleDp * 0.14f, scale);
        float hi = scaledSp(circleDp * 0.55f, scale);
        fitPaint.setTypeface(Typeface.DEFAULT_BOLD);
        fitPaint.setAntiAlias(true);
        for (int i = 0; i < 12; i++) {
            float mid = (lo + hi) / 2f;
            fitPaint.setTextSize(mid);
            if (fitPaint.measureText(text) <= maxWidthPx) lo = mid;
            else hi = mid;
        }
        return lo;
    }

    private float fitCircleTextSizePx(int circlePx, String text) {
        if (TextUtils.isEmpty(text) || circlePx <= 0) return circlePx * 0.22f;
        float maxWidthPx = circlePx * 0.72f;
        float lo = circlePx * 0.12f;
        float hi = circlePx * 0.52f;
        fitPaint.setTypeface(Typeface.DEFAULT_BOLD);
        fitPaint.setAntiAlias(true);
        for (int i = 0; i < 12; i++) {
            float mid = (lo + hi) / 2f;
            fitPaint.setTextSize(mid);
            if (fitPaint.measureText(text) <= maxWidthPx) lo = mid;
            else hi = mid;
        }
        return lo;
    }

    private LinearLayout buildPanelForContext(Context context, float scale, boolean cluster) {
        float oldDensity = activeDensity;
        activeDensity = context.getResources().getDisplayMetrics().density;
        try {
            resetPanelUiRefs(cluster);
            boolean pluginPanel = false;
            LinearLayout panel = null;
            String selectedStyle = AppPrefs.getOverlayUiStyle(this);
            PluginManifest uiPlugin = PluginManager.activeManifest(this, PluginManifest.CAP_UI);
            if (uiPlugin != null) {
                try {
                    PluginRenderer.Result result = PluginRenderer.render(context, uiPlugin,
                            PluginManifest.CAP_UI, scale, currentPluginState());
                    if (result != null && result.root != null) {
                        panel = result.root;
                        assignPluginPanelRefs(result, cluster);
                        pluginPanel = true;
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "plugin UI render failed, fallback to built-in UI", t);
                }
            }
            if (panel == null && OverlayUiStyles.isPluginStyle(selectedStyle)) {
                PluginManifest stylePlugin = selectedOverlayStylePlugin(selectedStyle);
                if (stylePlugin != null) {
                    try {
                        PluginRenderer.Result result = PluginRenderer.render(context, stylePlugin,
                                PluginManifest.CAP_OVERLAY_STYLE, scale, currentPluginState());
                        if (result != null && result.root != null) {
                            panel = result.root;
                            assignPluginPanelRefs(result, cluster);
                            pluginPanel = true;
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "plugin overlay style render failed, fallback to built-in UI", t);
                    }
                }
            }
            if (panel == null) {
                String fallbackStyle = OverlayUiStyles.isPluginStyle(selectedStyle) ? OverlayUiStyles.OLD : selectedStyle;
                panel = buildPanelForStyle(fallbackStyle, context, scale, cluster);
            }
            installTmcProgressForeground(panel, context, scale, cluster);
            FontManager.applyToViewTree(context, panel);
            if (pluginPanel) {
                syncFreshPluginPanelState();
            }
            if (!pluginPanel && isDynamicIslandOrCard()) {
                updateDynamicIslandLayout();
                updateCardLayout();
            }
            // Store panel background for overspeed warning border
            if (panel.getBackground() instanceof GradientDrawable) {
                if (cluster) {
                    clusterPanelBackground = (GradientDrawable) panel.getBackground();
                } else {
                    panelBackground = (GradientDrawable) panel.getBackground();
                }
            }
            return panel;
        } finally {
            activeDensity = oldDensity;
        }
    }

    private PluginManifest selectedOverlayStylePlugin(String selectedStyle) {
        if (!OverlayUiStyles.isPluginStyle(selectedStyle)) {
            return null;
        }
        String pluginId = OverlayUiStyles.pluginIdFromStyle(selectedStyle);
        try {
            PluginManifest manifest = PluginManager.installedPlugin(this, pluginId);
            if (manifest != null && manifest.hasCapability(PluginManifest.CAP_OVERLAY_STYLE)) {
                return manifest;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void installTmcProgressForeground(LinearLayout target, Context context, float scale, boolean cluster) {
        if (target == null) {
            return;
        }
        TmcProgressBar bar = new TmcProgressBar(context);
        // Use main display density so TMC bar proportions match across screens
        if (cluster) {
            bar.setDensityOverride(getResources().getDisplayMetrics().density);
        }
        String style = AppPrefs.getOverlayUiStyle(this);
        if (OverlayUiStyles.DYNAMIC_ISLAND_FULL.equals(style)) {
            bar.setCapsuleInsetMode(true);
        } else if (OverlayUiStyles.CARD.equals(style)) {
            bar.setHorizontalInsetPx(scaledDp(12, scale));
        } else {
            bar.setHorizontalInsetPx(cluster ? clusterDp(14) : dp(14));
        }
        if (AppPrefs.isTmcBarVisible(this)
                && !TextUtils.isEmpty(cachedTmcJson)
                && System.currentTimeMillis() - tmcUpdatedAt < TMC_TTL_MS) {
            bar.updateTmcData(cachedTmcJson);
        }
        target.setForeground(bar);
        if (cluster) {
            clusterTmcProgressBar = bar;
        } else {
            mainTmcProgressBar = bar;
        }
    }

    private void resetPanelUiRefs(boolean cluster) {
        if (cluster) {
            clusterPluginRenderer = null;
            clusterModeRow = null;
            clusterModeText = null;
            clusterTitleText = null;
            clusterSummaryDivider = null;
            clusterSummaryRow = null;
            clusterHeadingInfoText = null;
            clusterRoadInfoText = null;
            clusterTurnCard = null;
            clusterTurnLeadText = null;
            clusterTurnLeadIconView = null;
            clusterTurnLeadExitText = null;
            clusterTurnText = null;
            clusterTurnDistanceText = null;
            clusterTurnIconView = null;
            clusterTurnExitText = null;
            clusterTurnDistBadge = null;
            clusterTurnRowLayout = null;
            clusterLaneSection = null;
            clusterLaneBar = null;
            clusterLightRow = null;
            clusterServiceAreaText = null;
            clusterEtaText = null;
            clusterAlertCard = null;
            clusterLimitBadgeText = null;
            clusterAlertCaptionText = null;
            clusterAlertText = null;
            clusterAlertRow = null;
            clusterNavTurnBox = null;
            clusterNavTurnIconView = null;
            clusterNavExitText = null;
            clusterNavTurnDistText = null;
            clusterDetailText = null;
            clusterCompactWidgetRow = null;
            clusterCompactNavTurnRoadText = null;
            clusterCompactCruiseRoadText = null;
            clusterCompactCruiseDirText = null;
            clusterCompactLaneBox = null;
            clusterDynamicIslandViews = null;
            clusterCardCruiseRow1 = null;
            clusterCardCruiseRow2 = null;
            clusterCardNavArea = null;
            clusterCardCruiseLaneSection = null;
            clusterCardNavLaneSection = null;
            clusterCardCruiseLaneBar = null;
            clusterCardNavLaneBar = null;
            clusterCardCruiseLightRow = null;
            clusterCardNavLightRow = null;
            clusterCardCruiseEdogRow = null;
            clusterCardNavEdogRow = null;
            clusterTmcProgressBar = null;
            fullModeClusterTurnInfoCol = null;
            fullModeClusterEtaInfoCol = null;
            fullModeClusterEtaRemainDist = null;
            fullModeClusterEtaArriveTime = null;
            return;
        }

        pluginRenderer = null;
        modeRow = null;
        modeText = null;
        titleText = null;
        summaryDivider = null;
        summaryRow = null;
        headingInfoText = null;
        roadInfoText = null;
        turnCard = null;
        turnLeadText = null;
        turnLeadIconView = null;
        turnText = null;
        turnDistanceText = null;
        turnIconView = null;
        turnDistBadge = null;
        turnRowLayout = null;
        laneSection = null;
        laneBar = null;
        lightRow = null;
        serviceAreaText = null;
        etaText = null;
        alertCard = null;
        limitBadgeText = null;
        alertCaptionText = null;
        alertText = null;
        alertRow = null;
        navTurnBox = null;
        navTurnIconView = null;
        navExitText = null;
        navTurnDistText = null;
        detailText = null;
        compactWidgetRow = null;
        compactNavTurnRoadText = null;
        compactCruiseRoadText = null;
        compactCruiseDirText = null;
        compactLaneBox = null;
        mainDynamicIslandViews = null;
        cardCruiseRow1 = null;
        cardCruiseRow2 = null;
        cardNavArea = null;
        cardCruiseLaneSection = null;
        cardNavLaneSection = null;
        cardCruiseLaneBar = null;
        cardNavLaneBar = null;
        cardCruiseLightRow = null;
        cardNavLightRow = null;
        cardCruiseEdogRow = null;
        cardNavEdogRow = null;
        mainTmcProgressBar = null;
        fullModeTurnInfoCol = null;
        fullModeEtaInfoCol = null;
        fullModeEtaRemainDist = null;
        fullModeEtaArriveTime = null;
    }

    private LinearLayout buildPanelForStyle(String style, Context context, float scale, boolean cluster) {
        String normalized = OverlayUiStyles.normalize(style);
        if (OverlayUiStyles.CARD.equals(normalized)) {
            return buildCardPanel(context, scale, cluster);
        }
        if (OverlayUiStyles.DYNAMIC_ISLAND_FULL.equals(normalized)) {
            return buildDynamicIslandFullPanel(context, scale, cluster);
        }
        if (OverlayUiStyles.NEW.equals(normalized)) {
            return buildDashboardPanel(context, scale * 0.86f, cluster);
        }
        return buildClassicPanel(context, scale, cluster);
    }

    // -- Highway exit/entrance info (KEY_TYPE 12011) -------------------------

    private void updateClusterPosition() {
        if (clusterWindowManager == null || clusterPanel == null || clusterParams == null) {
            return;
        }
        int x = clusterParams.x;
        int y = clusterParams.y;
        int displayWidth = 0;
        int displayHeight = 0;
        if (clusterDisplay != null) {
            Point size = new Point();
            clusterDisplay.getRealSize(size);
            displayWidth = size.x;
            displayHeight = size.y;
        }
        int panelWidth = clusterPanel.getWidth();
        int panelHeight = clusterPanel.getHeight();
        if (panelWidth <= 0 || panelHeight <= 0) {
            int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            clusterPanel.measure(widthSpec, heightSpec);
            panelWidth = Math.max(panelWidth, clusterPanel.getMeasuredWidth());
            panelHeight = Math.max(panelHeight, clusterPanel.getMeasuredHeight());
        }
        if (displayWidth > 0 && panelWidth > 0) {
            x = clampOverlayAxis(x, displayWidth, panelWidth);
        }
        if (displayHeight > 0 && panelHeight > 0) {
            y = clampOverlayAxis(y, displayHeight, panelHeight);
        }
        clusterParams.x = x;
        clusterParams.y = y;
        try {
            if (clusterPanel.getParent() != null) {
                clusterWindowManager.updateViewLayout(clusterPanel, clusterParams);
            }
        } catch (Throwable t) {
            Log.e(TAG, "cluster position update failed", t);
        }
    }

    private int clampOverlayAxis(int value, int displaySize, int panelSize) {
        if (displaySize <= 0 || panelSize <= 0) return value;
        // Keep the panel fully within the screen: left/top edge >= 0, right/bottom edge <= displaySize
        int min = 0;
        int max = Math.max(0, displaySize - panelSize);
        return Math.max(min, Math.min(value, max));
    }

    private void applySavedClusterPosition() {
        if (clusterWindowManager == null || clusterPanel == null || clusterParams == null) {
            ensureClusterMirror();
            return;
        }
        clusterParams.x = getSavedClusterX();
        clusterParams.y = getSavedClusterY();
        updateClusterPosition();
        saveClusterPosition();
    }

    private void dismissClusterMirror() {
        if (clusterPanel != null) {
            clusterPanel.removeOnLayoutChangeListener(clusterBoundsListener);
            clusterPanel.setVisibility(View.GONE);
            clusterPanel.invalidate();
        }
        if (clusterWindowManager != null && clusterPanel != null && clusterPanel.getParent() != null) {
            try {
                if (clusterParams != null) {
                    clusterWindowManager.updateViewLayout(clusterPanel, clusterParams);
                }
            } catch (Throwable ignored) {
            }
            try {
                clusterWindowManager.removeViewImmediate(clusterPanel);
            } catch (Throwable ignored) {
            }
        }
        fullModeClusterTurnInfoCol = null;
        fullModeClusterEtaInfoCol = null;
        fullModeClusterEtaRemainDist = null;
        fullModeClusterEtaArriveTime = null;
        clusterContext = null;
        clusterWindowManager = null;
        clusterParams = null;
        clusterPanel = null;
        resetPanelUiRefs(true);
        resetClusterPanelWidthStabilizer();
        clusterModeRow = null;
        clusterModeText = null;
        clusterTitleText = null;
        clusterSummaryDivider = null;
        clusterSummaryRow = null;
        clusterHeadingInfoText = null;
        clusterRoadInfoText = null;
        clusterTurnCard = null;
        clusterTurnLeadText = null;
        clusterTurnText = null;
        clusterTurnDistanceText = null;
        clusterLaneSection = null;
        clusterLaneBar = null;
        clusterLightRow = null;
        clusterServiceAreaText = null;
        clusterEtaText = null;
        clusterAlertCard = null;
        clusterLimitBadgeText = null;
        clusterAlertCaptionText = null;
        clusterAlertText = null;
        clusterDetailText = null;
        clusterCompactWidgetRow = null;
        clusterCompactNavTurnRoadText = null;
        clusterCompactCruiseRoadText = null;
        clusterCompactCruiseDirText = null;
        clusterCompactLaneBox = null;
        clusterDisplay = null;
        clusterScale = -1f;
    }

    private void activateClusterBridge() {
        try {
            Intent activate = new Intent(ACTION_SEND);
            activate.putExtra("KEY_TYPE", 13014);
            activate.putExtra("EXTRA_ACTIVATE_STATE", 0);
            sendBroadcast(activate);
        } catch (Throwable t) {
            Log.e(TAG, "cluster activation broadcast failed", t);
        }
    }

    private void refreshDisplayPolicies() {
        boolean foregroundChanged = false;
        if (AppPrefs.isHideMainWhenTargetForegroundEnabled(this)) {
            boolean foreground = targetForegroundStateKnownFromBroadcast
                    ? targetAppForegroundFromBroadcast
                    : isTargetAppForeground();
            foregroundChanged = targetAppForeground != foreground;
            targetAppForeground = foreground;
        } else if (targetAppForeground) {
            targetAppForeground = false;
            foregroundChanged = true;
        }

        boolean targetBroadcastChanged = expireTargetBroadcastActivityIfNeeded();
        boolean navigationChanged = expireNavigationActivityIfNeeded();
        if (navigationChanged && !navigationOrCruiseActive) {
            clearInactiveNavigationUi();
        }
        if (foregroundChanged || targetBroadcastChanged) {
            syncMainOverlayAttachment();
        }
        if (navigationChanged) {
            ensureClusterMirror();
        }
        if (foregroundChanged || navigationChanged) {
            refreshPanelVisibility();
        }
    }

    private boolean shouldHideMainOverlayForTargetForeground() {
        return AppPrefs.isHideMainWhenTargetForegroundEnabled(this) && targetAppForeground;
    }

    private boolean shouldShowMainOverlayForTargetBroadcast() {
        return AppPrefs.isShowMainWhenTargetForegroundEnabled(this) && targetBroadcastActive;
    }

    private boolean shouldHideClusterMirrorForInactiveNavigation() {
        return AppPrefs.isHideClusterWhenInactiveEnabled(this)
                && !navigationOrCruiseActive
                && !shouldShowMainOverlayForTargetBroadcast();
    }

    private boolean updateNavigationActivityFromExtras(Bundle extras) {
        int keyType = intValue(extras, "KEY_TYPE", -1);
        int state = intValue(extras, "EXTRA_STATE", -1);
        boolean explicitExit = keyType == 10019 && (state == 9 || state == 12 || state == 25);
        boolean activeSignal = isNavigationActivitySignal(extras, keyType, state);
        boolean before = navigationOrCruiseActive;
        if (explicitExit) {
            navigationOrCruiseActive = false;
            lastNavigationSignalAt = 0L;
        } else if (activeSignal) {
            navigationOrCruiseActive = true;
            lastNavigationSignalAt = System.currentTimeMillis();
        }
        return before != navigationOrCruiseActive;
    }

    private boolean updateTargetForegroundFromExtras(Bundle extras) {
        int keyType = intValue(extras, "KEY_TYPE", -1);
        if (keyType != AmapConstants.KEY_TYPE_NAVIGATION_STATE) {
            return false;
        }
        int state = intValue(extras, "EXTRA_STATE", -1);
        if (state != AmapConstants.APP_STATE_FOREGROUND
                && state != AmapConstants.APP_STATE_BACKGROUND) {
            return false;
        }
        boolean foreground = state == AmapConstants.APP_STATE_FOREGROUND;
        boolean changed = !targetForegroundStateKnownFromBroadcast
                || targetAppForegroundFromBroadcast != foreground;
        targetForegroundStateKnownFromBroadcast = true;
        targetAppForegroundFromBroadcast = foreground;
        if (AppPrefs.isHideMainWhenTargetForegroundEnabled(this)) {
            changed = changed || targetAppForeground != foreground;
            targetAppForeground = foreground;
        }
        Log.d(TAG, "target foreground state from AMap broadcast: " + foreground);
        return changed;
    }

    private void clearInactiveNavigationUi() {
        inCruiseMode = false;
        navigationTurnDir = -1;
        currentRoadName = "";
        currentHeadingSummary = "";
        currentRoadTypeSummary = "";
        currentModeLabel = "待接收导航/巡航信息";
        currentEtaSummary = "";
        currentAlertSummary = "";
        currentDetailSummary = "";
        currentLimitSpeed = -1;
        currentCameraType = -1;
        lastDetailedMode = null;
        if (modeText != null) {
            modeText.setText(currentModeLabel);
        }
        if (clusterModeText != null) {
            clusterModeText.setText(currentModeLabel);
        }
        clearTurnState();
        clearExitInfoState();
        hideLaneData();
        trafficLights.clear();
        renderTrafficLights();
        clearTmcData();
        clearAlertDetails();
        clearServiceAreaDetails();
        // Clear TMC traffic light bar
        if (etaText != null) {
            etaText.setText("");
            etaText.setVisibility(View.GONE);
        }
        if (clusterEtaText != null) {
            clusterEtaText.setText("");
            clusterEtaText.setVisibility(View.GONE);
        }
        if (detailText != null) {
            detailText.setText("");
            detailText.setVisibility(View.GONE);
        }
        if (clusterDetailText != null) {
            clusterDetailText.setText("");
            clusterDetailText.setVisibility(View.GONE);
        }
        updateDynamicIslandLayout();
        updateCardLayout();
        refreshRoadTitle();
        refreshPluginRenderers();
        releasePanelSizeHoldsNow();
        refreshPanelVisibility();
    }

    private boolean expireNavigationActivityIfNeeded() {
        if (!navigationOrCruiseActive || lastNavigationSignalAt <= 0L) {
            return false;
        }
        if (System.currentTimeMillis() - lastNavigationSignalAt < NAVIGATION_ACTIVE_TTL_MS) {
            return false;
        }
        navigationOrCruiseActive = false;
        lastNavigationSignalAt = 0L;
        return true;
    }

    private boolean updateTargetBroadcastActivity(String action) {
        if (!isAmapRuntimeBroadcastAction(action)) {
            return false;
        }
        boolean before = targetBroadcastActive;
        targetBroadcastActive = true;
        lastTargetBroadcastAt = System.currentTimeMillis();
        return before != targetBroadcastActive;
    }

    private boolean expireTargetBroadcastActivityIfNeeded() {
        if (!targetBroadcastActive || lastTargetBroadcastAt <= 0L) {
            return false;
        }
        if (System.currentTimeMillis() - lastTargetBroadcastAt < TARGET_BROADCAST_ACTIVE_TTL_MS) {
            return false;
        }
        targetBroadcastActive = false;
        lastTargetBroadcastAt = 0L;
        return true;
    }

    private boolean isAmapRuntimeBroadcastAction(String action) {
        return ACTION_SEND.equals(action)
                || ACTION_RECV.equals(action)
                || "AUTO_GUIDE_INFO_FOR_INTERNAL_WIDGET".equals(action)
                || "AUTO_STATUS_FOR_INTERNAL_WIDGET".equals(action)
                || (action != null && action.startsWith("com.autonavi.amapauto."));
    }

    private boolean shouldRequestAmapData() {
        return AppPrefs.isMainOverlayEnabled(this)
                || AppPrefs.isClusterMirrorEnabled(this)
                || targetBroadcastActive
                || navigationOrCruiseActive;
    }

    private boolean isNavigationActivitySignal(Bundle extras, int keyType, int state) {
        if (keyType == 10019) {
            return state == 5 || state == 6 || state == 8 || state == 10 || state == 11 || state == 24;
        }
        if (keyType == 10001 || keyType == 60021 || keyType == 13012 || keyType == 13011) {
            return true;
        }
        if (keyType == AmapConstants.KEY_TYPE_TRAFFIC_LIGHT && TrafficLightParser.hasCountdownPayload(extras)) {
            return true;
        }
        return hasAny(extras,
                "ROUTE_REMAIN_DIS_AUTO", "ROUTE_REMAIN_TIME_AUTO",
                "ROUTE_REMAIN_DIS", "ROUTE_REMAIN_TIME",
                "SEG_REMAIN_DIS", "NEXT_SEG_REMAIN_DIS",
                "trafficLightStatus", "redLightCountDownSeconds", "greenLightLastSecond");
    }

    private boolean isTargetAppForeground() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        String targetPackage = AppPrefs.getTargetPackage(this);
        try {
            List<ActivityManager.RunningTaskInfo> tasks = manager.getRunningTasks(1);
            if (tasks != null && !tasks.isEmpty()
                    && tasks.get(0).topActivity != null
                    && targetPackage.equals(tasks.get(0).topActivity.getPackageName())) {
                return true;
            }
        } catch (Throwable t) {
            Log.d(TAG, "read running task failed", t);
        }
        if (!AppPrefs.hasUsageStatsAccess(this)) {
            Log.d(TAG, "usage stats access not granted; cannot detect target foreground");
            return false;
        }
        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            if (usageStatsManager == null) {
                return false;
            }
            long now = System.currentTimeMillis();
            UsageEvents events = usageStatsManager.queryEvents(now - 10000L, now);
            if (events != null) {
                UsageEvents.Event event = new UsageEvents.Event();
                String latestForegroundPackage = null;
                long latestForegroundAt = 0L;
                while (events.hasNextEvent()) {
                    events.getNextEvent(event);
                    int type = event.getEventType();
                    if ((type == UsageEvents.Event.MOVE_TO_FOREGROUND
                            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            && type == UsageEvents.Event.ACTIVITY_RESUMED))
                            && event.getTimeStamp() >= latestForegroundAt) {
                        latestForegroundAt = event.getTimeStamp();
                        latestForegroundPackage = event.getPackageName();
                    }
                }
                if (!TextUtils.isEmpty(latestForegroundPackage)) {
                    return targetPackage.equals(latestForegroundPackage);
                }
            }
            List<UsageStats> stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, now - 10000L, now);
            UsageStats latest = null;
            for (UsageStats stat : stats) {
                if (stat == null || TextUtils.isEmpty(stat.getPackageName())) {
                    continue;
                }
                if (latest == null || stat.getLastTimeUsed() > latest.getLastTimeUsed()) {
                    latest = stat;
                }
            }
            return latest != null && targetPackage.equals(latest.getPackageName());
        } catch (Throwable t) {
            Log.d(TAG, "read usage stats failed", t);
        }
        return false;
    }

    private int getSavedOverlayX() {
        return getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE)
                .getInt(AppPrefs.KEY_OVERLAY_X, rawDp(24));
    }

    private int getSavedOverlayY() {
        return getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE)
                .getInt(AppPrefs.KEY_OVERLAY_Y, rawDp(220));
    }

    private int getSavedClusterX() {
        return AppPrefs.getClusterX(this, rawDp(24));
    }

    private int getSavedClusterY() {
        return AppPrefs.getClusterY(this, rawDp(120));
    }

    private void saveOverlayPosition() {
        if (params == null) {
            return;
        }
        getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE)
                .edit()
                .putInt(AppPrefs.KEY_OVERLAY_X, params.x)
                .putInt(AppPrefs.KEY_OVERLAY_Y, params.y)
                .apply();
    }

    private void saveClusterPosition() {
        if (clusterParams == null) {
            return;
        }
        getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE)
                .edit()
                .putInt(AppPrefs.KEY_CLUSTER_X, clusterParams.x)
                .putInt(AppPrefs.KEY_CLUSTER_Y, clusterParams.y)
                .apply();
    }

    private void syncClusterFromMain() {
        if (isAnyDynamicIslandUiEnabled()) {
            updateDynamicIslandLayout();
            syncDynamicIslandClusterNavigationState();
            applyNavigationTextVisualState();
        } else if (AppPrefs.isCardUiEnabled(this)) {
            // Card UI: sync cruise/nav mode and text through updateCardLayout
            updateCardLayout();
            // Sync card-specific text fields
            if (clusterCompactCruiseRoadText != null && compactCruiseRoadText != null) {
                String road = compactCruiseRoadText.getText().toString();
                if (!TextUtils.isEmpty(road) && !TextUtils.equals(clusterCompactCruiseRoadText.getText(), road)) {
                    clusterCompactCruiseRoadText.setText(road);
                }
            }
            if (clusterCompactCruiseDirText != null && compactCruiseDirText != null) {
                String dir = compactCruiseDirText.getText().toString();
                if (!TextUtils.isEmpty(dir) && !TextUtils.equals(clusterCompactCruiseDirText.getText(), dir)) {
                    clusterCompactCruiseDirText.setText(dir);
                }
            }
            if (clusterCompactNavTurnRoadText != null && compactNavTurnRoadText != null) {
                String navRoad = compactNavTurnRoadText.getText().toString();
                updateCompactMarqueeText(clusterCompactNavTurnRoadText, navRoad);
            }
            if (clusterNavTurnDistText != null && navTurnDistText != null) {
                copyTextState(navTurnDistText, clusterNavTurnDistText);
            }
            if (clusterEtaText != null && etaText != null) {
                copyTextState(etaText, clusterEtaText);
            }
            copyVisibility(laneSection, clusterLaneSection);
        } else {
            copyTextState(modeText, clusterModeText);
            copyTextState(turnText, clusterTurnText);
            copyTextState(serviceAreaText, clusterServiceAreaText);
            copyTextState(etaText, clusterEtaText);
            copyTextState(alertText, clusterAlertText);
            copyTextState(detailText, clusterDetailText);
            copyVisibility(laneSection, clusterLaneSection);
        }
        applyCachedLaneData();
        renderTrafficLights();
        applyContentVisibilityPrefs();
        updateClusterPosition();
    }

    private void copyTextState(TextView source, TextView target) {
        if (source == null || target == null) {
            return;
        }
        target.setText(source.getText());
        target.setVisibility(source.getVisibility());
    }

    private void copyVisibility(View source, View target) {
        if (source != null && target != null) {
            target.setVisibility(source.getVisibility());
        }
    }

    private void syncDynamicIslandClusterNavigationState() {
        if (!isAnyDynamicIslandUiEnabled()) {
            return;
        }
        boolean isNav = AppPrefs.isTurnVisible(this) && !inCruiseMode && currentTurnIcon > 0;
        boolean isCruise = inCruiseMode;
        renderDynamicIslandView(clusterDynamicIslandViews, isNav, isCruise, !isNav && !isCruise,
                clusterScale > 0 ? clusterScale : overlayScale);
        copyTextState(fullModeEtaRemainDist, fullModeClusterEtaRemainDist);
        copyTextState(fullModeEtaArriveTime, fullModeClusterEtaArriveTime);
        if (shouldPinFullModeTurnInfo()) {
            if (fullModeClusterTurnInfoCol != null) {
                fullModeClusterTurnInfoCol.setVisibility(View.VISIBLE);
            }
            if (fullModeClusterEtaInfoCol != null) {
                fullModeClusterEtaInfoCol.setVisibility(View.GONE);
            }
        } else {
            copyVisibility(fullModeTurnInfoCol, fullModeClusterTurnInfoCol);
            copyVisibility(fullModeEtaInfoCol, fullModeClusterEtaInfoCol);
        }
    }

    private void updateCompactMarqueeText(TextView view, String text) {
        if (view == null) {
            return;
        }
        String next = text == null ? "" : text;
        if (TextUtils.isEmpty(next)) {
            view.setVisibility(View.GONE);
            return;
        }
        if (!TextUtils.equals(view.getText(), next)) {
            view.setText(next);
            view.setSelected(false);
            view.post(() -> view.setSelected(true));
        }
        view.setVisibility(View.VISIBLE);
    }


	private void updateCompactCruiseDirectionText(TextView view) {
        if (view == null) {
            return;
        }
        if (AppPrefs.isCardUiEnabled(this)) {
            String heading = !TextUtils.isEmpty(currentHeadingSummary) ? currentHeadingSummary : "--";
            String next = "【" + heading + "】 ";
            if (!TextUtils.equals(view.getText(), next)) {
                view.setText(next);
            }
            view.setVisibility(View.VISIBLE);
            return;
        }
        if (!TextUtils.isEmpty(currentHeadingSummary)) {
            String next = "\u8f66\u5934\uff1a" + currentHeadingSummary;
            if (!TextUtils.equals(view.getText(), next)) {
                view.setText(next);
            }
        } else if (TextUtils.isEmpty(view.getText())) {
            view.setText("\u8f66\u5934\uff1a--");
        }
        view.setVisibility(View.VISIBLE);
    }
    private void updateClusterCompactTurnText() {
        if (clusterCompactNavTurnRoadText != null) {
            String roadName = currentTurnRoad;
            if (TextUtils.isEmpty(roadName) || "\u4e0b\u4e00\u8def\u53e3".equals(roadName)) {
                roadName = currentRoadName;
            }
            updateCompactMarqueeText(clusterCompactNavTurnRoadText, roadName);
        }
        if (clusterCompactCruiseRoadText != null && inCruiseMode) {
            String road = !TextUtils.isEmpty(currentRoadName) ? currentRoadName : "";
            if (!TextUtils.equals(clusterCompactCruiseRoadText.getText(), road)) {
                clusterCompactCruiseRoadText.setText(road);
            }
        }
        updateCompactCruiseDirectionText(clusterCompactCruiseDirText);
    }


    private boolean isDynamicIslandOrCard() {
        return isAnyDynamicIslandUiEnabled()
                || AppPrefs.isCardUiEnabled(this);
    }

    private boolean isAnyDynamicIslandUiEnabled() {
        return AppPrefs.isDynamicIslandUiEnabled(this);
    }

        private void showAnyPanel() {
        refreshPanelVisibility();
    }

    private void refreshPanelVisibility() {
        if (panel != null) {
            applyOverlayTextOutlines(panel);
            panel.setVisibility(hasVisibleChildren(panel) ? View.VISIBLE : View.GONE);
            schedulePanelSizeStabilizer(panel, false);
        }
        if (clusterPanel != null) {
            applyOverlayTextOutlines(clusterPanel);
            clusterPanel.setVisibility(hasVisibleChildren(clusterPanel) ? View.VISIBLE : View.GONE);
            schedulePanelSizeStabilizer(clusterPanel, true);
        }
    }

    private void assignPluginPanelRefs(PluginRenderer.Result result, boolean cluster) {
        if (cluster) {
            clusterPluginRenderer = result.renderer;
            clusterModeText = result.modeText;
            clusterTurnText = result.turnText;
            clusterTurnDistanceText = result.turnDistanceText;
            clusterTurnIconView = result.turnIconView;
            clusterLaneSection = result.laneSection;
            clusterLaneBar = result.laneBar;
            clusterLightRow = result.lightRow;
            clusterEtaText = result.etaText;
            clusterAlertRow = result.alertRow;
            clusterAlertText = result.alertText;
            clusterDetailText = result.detailText;
            return;
        }
        pluginRenderer = result.renderer;
        modeText = result.modeText;
        turnText = result.turnText;
        turnDistanceText = result.turnDistanceText;
        turnIconView = result.turnIconView;
        laneSection = result.laneSection;
        laneBar = result.laneBar;
        lightRow = result.lightRow;
        etaText = result.etaText;
        alertRow = result.alertRow;
        alertText = result.alertText;
        detailText = result.detailText;
    }

    private void syncFreshPluginPanelState() {
        refreshTurnCard();
        applyCachedLaneData();
        syncLaneVisibility();
        renderTrafficLights();
        refreshAlertTextFromCurrentState();
        refreshAlertCard();
        PluginRenderer.State state = currentPluginState();
        if (pluginRenderer != null) {
            pluginRenderer.refresh(state);
        }
        if (clusterPluginRenderer != null) {
            clusterPluginRenderer.refresh(state);
        }
    }

    private void refreshAlertTextFromCurrentState() {
        if (alertText != null) {
            alertText.setText(currentAlertSummary);
            alertText.setVisibility(TextUtils.isEmpty(currentAlertSummary) ? View.GONE : View.VISIBLE);
        }
        if (clusterAlertText != null) {
            clusterAlertText.setText(currentAlertSummary);
            clusterAlertText.setVisibility(TextUtils.isEmpty(currentAlertSummary) ? View.GONE : View.VISIBLE);
        }
        syncAlertVisibility();
    }

    private void applyOverlayTextOutlines(View view) {
        if (view == null) {
            return;
        }
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            int color = text.getCurrentTextColor();
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            int luminance = (red * 299 + green * 587 + blue * 114) / 1000;
            int outline = luminance >= 150 ? 0xE6000000 : 0xE6FFFFFF;
            float density = text.getResources().getDisplayMetrics().density;
            float radius = Math.max(1.2f * density,
                    Math.min(2.4f * density, text.getTextSize() * 0.055f));
            text.setShadowLayer(radius, 0f, 0f, outline);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyOverlayTextOutlines(group.getChildAt(i));
            }
        }
    }

    private void schedulePanelSizeStabilizer(LinearLayout target, boolean cluster) {
        if (target == null) {
            return;
        }
        target.post(() -> stabilizePanelSize(target, cluster));
    }

    private void stabilizePanelSize(LinearLayout target, boolean cluster) {
        if (target == null) {
            return;
        }
        int baseMin = cluster ? clusterPanelBaseMinWidth : mainPanelBaseMinWidth;
        if (baseMin < 0) {
            baseMin = Math.max(0, target.getMinimumWidth());
            if (cluster) {
                clusterPanelBaseMinWidth = baseMin;
            } else {
                mainPanelBaseMinWidth = baseMin;
            }
        }
        int baseMinHeight = cluster ? clusterPanelBaseMinHeight : mainPanelBaseMinHeight;
        if (baseMinHeight < 0) {
            baseMinHeight = Math.max(0, target.getMinimumHeight());
            if (cluster) {
                clusterPanelBaseMinHeight = baseMinHeight;
            } else {
                mainPanelBaseMinHeight = baseMinHeight;
            }
        }

        int[] measured = measurePanelContent(target, baseMin, baseMinHeight);
        int width = measured[0];
        int height = measured[1];

        int held = cluster ? clusterPanelHeldMinWidth : mainPanelHeldMinWidth;
        int heldHeight = cluster ? clusterPanelHeldMinHeight : mainPanelHeldMinHeight;
        int threshold = panelShrinkThresholdPx(cluster);
        int nextMin = Math.max(baseMin, held);
        int nextMinHeight = Math.max(baseMinHeight, heldHeight);
        if (held <= 0 || width > held + threshold) {
            nextMin = Math.max(baseMin, width);
        }
        if (heldHeight <= 0 || height > heldHeight + threshold) {
            nextMinHeight = Math.max(baseMinHeight, height);
        }
        boolean expanded = nextMin > held || nextMinHeight > heldHeight;
        if (cluster) {
            clusterPanelHeldMinWidth = nextMin;
            clusterPanelHeldMinHeight = nextMinHeight;
        } else {
            mainPanelHeldMinWidth = nextMin;
            mainPanelHeldMinHeight = nextMinHeight;
        }
        if (target.getMinimumWidth() != nextMin) {
            target.setMinimumWidth(nextMin);
            target.requestLayout();
        }
        if (target.getMinimumHeight() != nextMinHeight) {
            target.setMinimumHeight(nextMinHeight);
            target.requestLayout();
        }

        // Push expanded size to WindowManager immediately
        if (expanded) {
            if (cluster) {
                clusterWindowManager.updateViewLayout(clusterPanel, clusterParams);
            } else {
                windowManager.updateViewLayout(panel, params);
            }
        }

        Runnable oldUnlock = cluster ? clusterPanelWidthUnlock : mainPanelWidthUnlock;
        if (!expanded && oldUnlock != null) {
            return;
        }
        if (oldUnlock != null) {
            mainHandler.removeCallbacks(oldUnlock);
        }
        Runnable unlock = () -> unlockPanelWidth(target, cluster);
        if (cluster) {
            clusterPanelWidthUnlock = unlock;
        } else {
            mainPanelWidthUnlock = unlock;
        }
        mainHandler.postDelayed(unlock, PANEL_WIDTH_SHRINK_DELAY_MS);
    }

    private void unlockPanelWidth(LinearLayout target, boolean cluster) {
        if (target == null || target != (cluster ? clusterPanel : panel)) {
            return;
        }
        int baseMin = Math.max(0, cluster ? clusterPanelBaseMinWidth : mainPanelBaseMinWidth);
        int baseMinHeight = Math.max(0, cluster ? clusterPanelBaseMinHeight : mainPanelBaseMinHeight);
        int held = cluster ? clusterPanelHeldMinWidth : mainPanelHeldMinWidth;
        int heldHeight = cluster ? clusterPanelHeldMinHeight : mainPanelHeldMinHeight;
        int[] measured = measurePanelContent(target, baseMin, baseMinHeight);
        int targetMin = Math.max(baseMin, measured[0]);
        int targetMinHeight = Math.max(baseMinHeight, measured[1]);
        int threshold = panelShrinkThresholdPx(cluster);
        if ((held > 0 && held - targetMin <= threshold)
                && (heldHeight > 0 && heldHeight - targetMinHeight <= threshold)) {
            if (cluster) {
                clusterPanelWidthUnlock = null;
            } else {
                mainPanelWidthUnlock = null;
            }
            return;
        }
        if (cluster) {
            clusterPanelHeldMinWidth = targetMin;
            clusterPanelHeldMinHeight = targetMinHeight;
            clusterPanelWidthUnlock = null;
        } else {
            mainPanelHeldMinWidth = targetMin;
            mainPanelHeldMinHeight = targetMinHeight;
            mainPanelWidthUnlock = null;
        }
        boolean changed = false;
        if (target.getMinimumWidth() != targetMin) {
            target.setMinimumWidth(targetMin);
            changed = true;
        }
        if (target.getMinimumHeight() != targetMinHeight) {
            target.setMinimumHeight(targetMinHeight);
            changed = true;
        }
        if (changed) {
            target.requestLayout();
        }
        target.post(() -> {
            if (target != (cluster ? clusterPanel : panel)) {
                return;
            }
            if (cluster) {
                updateClusterPosition();
            } else {
                updateOverlayPosition();
            }
        });
    }

    private void resetMainPanelWidthStabilizer() {
        if (mainPanelWidthUnlock != null) {
            mainHandler.removeCallbacks(mainPanelWidthUnlock);
            mainPanelWidthUnlock = null;
        }
        mainPanelBaseMinWidth = -1;
        mainPanelBaseMinHeight = -1;
        mainPanelHeldMinWidth = 0;
        mainPanelHeldMinHeight = 0;
    }

    private void resetClusterPanelWidthStabilizer() {
        if (clusterPanelWidthUnlock != null) {
            mainHandler.removeCallbacks(clusterPanelWidthUnlock);
            clusterPanelWidthUnlock = null;
        }
        clusterPanelBaseMinWidth = -1;
        clusterPanelBaseMinHeight = -1;
        clusterPanelHeldMinWidth = 0;
        clusterPanelHeldMinHeight = 0;
    }

    private void releasePanelSizeHoldsNow() {
        releasePanelSizeHold(false);
        releasePanelSizeHold(true);
    }

    private void releasePanelSizeHold(boolean cluster) {
        LinearLayout target = cluster ? clusterPanel : panel;
        if (cluster) {
            if (clusterPanelWidthUnlock != null) {
                mainHandler.removeCallbacks(clusterPanelWidthUnlock);
                clusterPanelWidthUnlock = null;
            }
        } else if (mainPanelWidthUnlock != null) {
            mainHandler.removeCallbacks(mainPanelWidthUnlock);
            mainPanelWidthUnlock = null;
        }

        int baseMin = cluster ? clusterPanelBaseMinWidth : mainPanelBaseMinWidth;
        int baseMinHeight = cluster ? clusterPanelBaseMinHeight : mainPanelBaseMinHeight;
        int width = Math.max(0, baseMin);
        int height = Math.max(0, baseMinHeight);
        if (cluster) {
            clusterPanelHeldMinWidth = 0;
            clusterPanelHeldMinHeight = 0;
        } else {
            mainPanelHeldMinWidth = 0;
            mainPanelHeldMinHeight = 0;
        }
        if (target == null) {
            return;
        }
        boolean changed = false;
        if (target.getMinimumWidth() != width) {
            target.setMinimumWidth(width);
            changed = true;
        }
        if (target.getMinimumHeight() != height) {
            target.setMinimumHeight(height);
            changed = true;
        }
        if (changed) {
            target.requestLayout();
        }
        target.post(() -> {
            if (cluster) {
                updateClusterPosition();
            } else {
                updateOverlayPosition();
            }
        });
    }

    private boolean hasVisibleChildren(LinearLayout layout) {
        if (layout == null) {
            return false;
        }
        for (int i = 0; i < layout.getChildCount(); i++) {
            if (layout.getChildAt(i).getVisibility() == View.VISIBLE) {
                return true;
            }
        }
        return false;
    }

    private void updateOverlayPosition() {
        if (params != null) {
            android.graphics.Point screenSize = new android.graphics.Point();
            windowManager.getDefaultDisplay().getRealSize(screenSize);
            int panelWidth = panel != null ? panel.getWidth() : 0;
            int panelHeight = panel != null ? panel.getHeight() : 0;
            if (panelWidth <= 0 || panelHeight <= 0) {
                if (panel != null) {
                    int wSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                    int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                    panel.measure(wSpec, hSpec);
                    panelWidth = Math.max(panelWidth, panel.getMeasuredWidth());
                    panelHeight = Math.max(panelHeight, panel.getMeasuredHeight());
                }
            }
            if (screenSize.x > 0 && panelWidth > 0) {
                params.x = clampOverlayAxis(params.x, screenSize.x, panelWidth);
            }
            if (screenSize.y > 0 && panelHeight > 0) {
                params.y = clampOverlayAxis(params.y, screenSize.y, panelHeight);
            }
        }
        try {
            if (windowManager != null && panel != null && panel.getParent() != null) {
                windowManager.updateViewLayout(panel, params);
            }
        } catch (Throwable t) {
            Log.e(TAG, "drag update failed", t);
        }
        updateClusterPosition();
    }

    private void rebuildOverlay() {
        int oldX = params != null ? params.x : rawDp(24);
        int oldY = params != null ? params.y : rawDp(220);
        stopFullModeAlternator();
        if (windowManager != null && panel != null && panel.getParent() != null) {
            try {
                windowManager.removeView(panel);
            } catch (Throwable t) {
                Log.e(TAG, "overlay remove for scale failed", t);
            }
        }
        fullModeTurnInfoCol = null;
        fullModeEtaInfoCol = null;
        fullModeEtaRemainDist = null;
        fullModeEtaArriveTime = null;
        fullModeClusterEtaRemainDist = null;
        fullModeClusterEtaArriveTime = null;
        mainDynamicIslandViews = null;
        panel = null;
        resetMainPanelWidthStabilizer();
        modeRow = null;
        modeText = null;
        titleText = null;
        summaryDivider = null;
        summaryRow = null;
        headingInfoText = null;
        roadInfoText = null;
        turnCard = null;
        turnLeadText = null;
        turnLeadIconView = null;
        turnText = null;
        turnDistanceText = null;
        turnIconView = null;
        turnDistBadge = null;
        turnRowLayout = null;
        navTurnBox = null;
        navTurnIconView = null;
        navTurnDistText = null;
        laneSection = null;
        laneBar = null;
        lightRow = null;
        etaText = null;
        alertCard = null;
        limitBadgeText = null;
        alertCaptionText = null;
        alertText = null;
        alertRow = null;
        detailText = null;
        compactWidgetRow = null;
        compactNavTurnRoadText = null;
        compactCruiseRoadText = null;
        compactCruiseDirText = null;
        compactLaneBox = null;
        ensureOverlay();
        if (params != null) {
            params.x = oldX;
            params.y = oldY;
            updateOverlayPosition();
        }
        requestLaneInfo();
        requestTrafficLightInfo();
        requestTmcInfo();
    }

    private void stopSelfIfNoVisuals() {
        if (!AppPrefs.isMainOverlayEnabled(this)
                && !AppPrefs.isClusterMirrorEnabled(this)
                && !AppPrefs.isAutoStartEnabled(this)
                && !AppPrefs.isShowMainWhenTargetForegroundEnabled(this)) {
            stopSelf();
        }
    }

    private void openMainActivity() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } catch (Throwable t) {
            Log.e(TAG, "open main activity failed", t);
        }
    }

    private void handleBroadcast(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (AppPrefs.ACTION_DIAGNOSTIC_REPLAY.equals(action)) {
            handleDiagnosticReplay(intent);
            return;
        }
        if (AppPrefs.ACTION_OVERLAY_SCALE_CHANGED.equals(action)) {
            rebuildOverlay();
            return;
        }
        if (AppPrefs.ACTION_MAIN_OVERLAY_CHANGED.equals(action)) {
            ensureOverlay();
            stopSelfIfNoVisuals();
            return;
        }
        if (AppPrefs.ACTION_CLUSTER_MIRROR_CHANGED.equals(action)) {
            clusterScale = -1f;
            ensureClusterMirror();
            stopSelfIfNoVisuals();
            return;
        }
        if (AppPrefs.ACTION_CLUSTER_POSITION_CHANGED.equals(action)) {
            applySavedClusterPosition();
            return;
        }
        if (AppPrefs.ACTION_OVERLAY_STYLE_CHANGED.equals(action)) {
            rebuildOverlaysForStyleChange();
            return;
        }
        if (AppPrefs.ACTION_PLUGINS_CHANGED.equals(action)) {
            rebuildOverlaysForStyleChange();
            return;
        }
        if (AppPrefs.ACTION_OVERLAY_CONTENT_CHANGED.equals(action)) {
            applyContentVisibilityPrefs();
            return;
        }
        if (AppPrefs.ACTION_DISPLAY_POLICY_CHANGED.equals(action)) {
            refreshDisplayPolicies();
            updateOverspeedWarning();
            stopSelfIfNoVisuals();
            return;
        }
        boolean targetBroadcastChanged = updateTargetBroadcastActivity(action);
        if (targetBroadcastChanged) {
            ensureOverlay();
            syncMainOverlayAttachment();
            ensureClusterMirror();
        }
        Bundle extras = intent.getExtras();
        Log.d(TAG, "recv action=" + action + " extras=" + describeExtras(extras));
        if (extras == null) {
            return;
        }
        if (isAmapRuntimeBroadcastAction(action)) {
            BroadcastEventRecorder.record(intent);
        }
        currentRawKeyType = intValue(extras, "KEY_TYPE", -1);

        ensureOverlay();
        boolean dayNightChanged = updateDayNightStateFromExtras(extras);
        boolean foregroundChanged = updateTargetForegroundFromExtras(extras);
        boolean navigationActivityChanged = updateNavigationActivityFromExtras(extras);
        boolean displayPolicyChanged = targetBroadcastChanged
                || foregroundChanged
                || navigationActivityChanged;
        updateModeFromExtras(extras);
        updateProtocolDetails(extras);  // must run before updateTurn* so roadType is current
        updateRouteGuidanceExitInfo(extras);
        updateTurnFromExtras(extras);
        updateEtaFromExtras(extras);
        updateLaneFromExtras(extras);

        int keyType = currentRawKeyType;

        if (dayNightChanged) {
            applyTextPalette();
        }

        if (keyType == 13011 || hasAny(extras, "EXTRA_TMC_SEGMENT", "extra_tmc_segment")) {
            updateTmcData(valueString(extras, "EXTRA_TMC_SEGMENT", "extra_tmc_segment"));
        }

        // Highway exit/entrance info (KEY_TYPE 12011)
        if (ACTION_SEND.equals(action) && keyType == 12011) {
            handleExitInfo(extras);
        }

        boolean trafficLightAction = action != null
                && action.toLowerCase(java.util.Locale.US).contains("traffic_light");
        if (trafficLightAction || TrafficLightParser.hasTrafficLightPayload(extras)) {
            updateTrafficLights(extras);
        }

        if (ACTION_SEND.equals(action) && intValue(extras, "KEY_TYPE", -1) == 13012) {
            updateLaneFromExtras(extras);
        }
        // Capture vehicle speed from any broadcast (nav 10001, cruise 10019/60021, etc.)
        int speed = intValue(extras, "CUR_SPEED", intValue(extras, "SPEED", -1));
        if (speed >= 0) {
            currentVehicleSpeed = speed;
        }
        updateOverspeedWarning();
        refreshPluginRenderers();
        if (displayPolicyChanged) {
            syncMainOverlayAttachment();
            ensureClusterMirror();
        }
    }

    private boolean updateDayNightStateFromExtras(Bundle extras) {
        if (currentRawKeyType != AmapConstants.KEY_TYPE_NAVIGATION_STATE) {
            return false;
        }
        int state = intValue(extras, "EXTRA_STATE", -1);
        return AppPrefs.updateDayNightState(this, state);
    }

    private void handleDiagnosticReplay(Intent intent) {
        String eventJson = intent.getStringExtra(AppPrefs.EXTRA_DIAGNOSTIC_EVENT_JSON);
        if (TextUtils.isEmpty(eventJson)) {
            Log.d(TAG, "diagnostic replay skipped: empty event");
            return;
        }
        try {
            BroadcastEvent event = BroadcastEvent.fromJson(eventJson);
            if (event == null) {
                Log.d(TAG, "diagnostic replay skipped: invalid event");
                return;
            }
            Intent replay = event.toReplayIntent();
            if (replay == null) {
                Log.d(TAG, "diagnostic replay skipped: empty action");
                return;
            }
            replay.putExtra(AppPrefs.EXTRA_DIAGNOSTIC_REPLAY, true);
            handleBroadcast(replay);
        } catch (Throwable t) {
            Log.e(TAG, "diagnostic replay failed", t);
        }
    }

    private PluginRenderer.State currentPluginState() {
        return new PluginRenderer.State(
                nullToEmpty(currentModeLabel),
                nullToEmpty(currentRoadName),
                nullToEmpty(currentHeadingSummary),
                nullToEmpty(currentTurnLead),
                nullToEmpty(currentTurnDistance),
                nullToEmpty(currentTurnRoad),
                currentTurnIcon,
                nullToEmpty(currentEtaSummary),
                nullToEmpty(currentAlertSummary),
                nullToEmpty(currentDetailSummary),
                currentLimitSpeed,
                currentVehicleSpeed,
                currentCameraType,
                currentRawKeyType);
    }

    private void refreshPluginRenderers() {
        PluginRenderer.State state = currentPluginState();
        if (pluginRenderer != null) {
            pluginRenderer.refresh(state);
        }
        if (clusterPluginRenderer != null) {
            clusterPluginRenderer.refresh(state);
        }
        refreshPanelVisibility();
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private void applyContentVisibilityPrefs() {
        syncModeVisibility();
        syncTurnVisibility();
        syncLaneVisibility();
        syncEtaVisibility();
        syncAlertVisibility();
        if (!AppPrefs.isAlertVisible(this)) {
            clearCompactWidgetRow();
        }
        if (AppPrefs.isTmcBarVisible(this)) {
            if (!TextUtils.isEmpty(cachedTmcJson)
                    && System.currentTimeMillis() - tmcUpdatedAt < TMC_TTL_MS) {
                if (mainTmcProgressBar != null) mainTmcProgressBar.updateTmcData(cachedTmcJson);
                if (clusterTmcProgressBar != null) clusterTmcProgressBar.updateTmcData(cachedTmcJson);
            }
            // Keep TMC bar hidden during overspeed border
            if (overspeedLevel != OVERSPEED_NONE) {
                setTmcBarVisible(false);
            }
        } else {
            clearTmcDrawablesOnly();
        }
        syncServiceAreaVisibility();
        syncDetailVisibility();
        syncTrafficLightVisibility();
        updateDynamicIslandLayout();
        refreshPanelVisibility();
        updateClusterPosition();
    }

    private void applyPanelStyle() {
        if (panel != null) {
            panel.setBackground(createMainPanelBackground());
        }
        if (clusterPanel != null) {
            clusterPanel.setBackground(createClusterPanelBackground());
        }
        applyTextPalette();
    }

    private GradientDrawable createMainPanelBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        int opacity = AppPrefs.getBackgroundOpacityPercent(this);
        bg.setColor(withAlpha(AppPrefs.getBackgroundColor(this), opacity));
        bg.setStroke(dp(1), withAlpha(0xFFFFFFFF, AppPrefs.strokeOpacityForBackground(opacity)));
        return bg;
    }

    private GradientDrawable createClusterPanelBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(clusterDp(14));
        int opacity = AppPrefs.getBackgroundOpacityPercent(this);
        bg.setColor(withAlpha(AppPrefs.getBackgroundColor(this), opacity));
        bg.setStroke(clusterDp(1), withAlpha(0xFFFFFFFF, AppPrefs.strokeOpacityForBackground(opacity)));
        return bg;
    }

    private void applyTextPalette() {
        int primary = primaryTextColor();
        int alert = alertTextColor();
        int detail = detailTextColor();
        if (modeText != null) {
            modeText.setTextColor(primary);
        }
        if (titleText != null) {
            titleText.setTextColor(primary);
        }
        if (headingInfoText != null) {
            headingInfoText.setTextColor(primary);
        }
        if (roadInfoText != null) {
            roadInfoText.setTextColor(primary);
        }
        if (turnLeadText != null) {
            turnLeadText.setTextColor(0xFF60A5FA);
        }
        if (turnText != null) {
            turnText.setTextColor(primary);
        }
        if (turnDistanceText != null) {
            turnDistanceText.setTextColor(primary);
        }
        if (etaText != null) {
            etaText.setTextColor(primary);
        }
        if (alertCaptionText != null) {
            alertCaptionText.setTextColor(0xFFCBD5E1);
        }
        if (limitBadgeText != null) {
            limitBadgeText.setTextColor(Color.WHITE);
        }
        if (alertText != null) {
            alertText.setTextColor(primary);
        }
        if (serviceAreaText != null) {
            serviceAreaText.setTextColor(primary);
        }
        if (navTurnDistText != null) {
            navTurnDistText.setTextColor(primary);
            navTurnDistText.setAlpha(1f);
        }
        if (compactNavTurnRoadText != null) {
            compactNavTurnRoadText.setTextColor(primary);
            compactNavTurnRoadText.setAlpha(1f);
        }
        applyEdogAlertTextColor(alertRow, primary);
        if (detailText != null) {
            detailText.setTextColor(detail);
        }
        if (clusterModeText != null) {
            clusterModeText.setTextColor(primary);
        }
        if (clusterTitleText != null) {
            clusterTitleText.setTextColor(primary);
        }
        if (clusterHeadingInfoText != null) {
            clusterHeadingInfoText.setTextColor(primary);
        }
        if (clusterRoadInfoText != null) {
            clusterRoadInfoText.setTextColor(primary);
        }
        if (clusterTurnLeadText != null) {
            clusterTurnLeadText.setTextColor(0xFF60A5FA);
        }
        if (clusterTurnText != null) {
            clusterTurnText.setTextColor(primary);
        }
        if (clusterTurnDistanceText != null) {
            clusterTurnDistanceText.setTextColor(primary);
        }
        if (clusterEtaText != null) {
            clusterEtaText.setTextColor(primary);
        }
        if (clusterAlertCaptionText != null) {
            clusterAlertCaptionText.setTextColor(0xFFCBD5E1);
        }
        if (clusterLimitBadgeText != null) {
            clusterLimitBadgeText.setTextColor(Color.WHITE);
        }
        if (clusterAlertText != null) {
            clusterAlertText.setTextColor(primary);
        }
        if (clusterServiceAreaText != null) {
            clusterServiceAreaText.setTextColor(primary);
        }
        if (clusterNavTurnDistText != null) {
            clusterNavTurnDistText.setTextColor(primary);
            clusterNavTurnDistText.setAlpha(1f);
        }
        if (clusterCompactNavTurnRoadText != null) {
            clusterCompactNavTurnRoadText.setTextColor(primary);
            clusterCompactNavTurnRoadText.setAlpha(1f);
        }
        applyEdogAlertTextColor(clusterAlertRow, primary);
        if (clusterDetailText != null) {
            clusterDetailText.setTextColor(detail);
        }
        applyNavigationTextVisualState();
        applyOverlayTextOutlines(panel);
        applyOverlayTextOutlines(clusterPanel);
    }

    private void applyEdogAlertTextColor(LinearLayout row, int primary) {
        if (row == null) {
            return;
        }
        View cameraBox = row.findViewWithTag("camera_box");
        if (cameraBox instanceof LinearLayout) {
            LinearLayout box = (LinearLayout) cameraBox;
            if (box.getChildCount() > 1 && box.getChildAt(1) instanceof TextView) {
                ((TextView) box.getChildAt(1)).setTextColor(primary);
            }
        }
        View lightBox = row.findViewWithTag("light_box");
        if (lightBox instanceof LinearLayout) {
            LinearLayout box = (LinearLayout) lightBox;
            if (box.getChildCount() > 1 && box.getChildAt(1) instanceof TextView) {
                ((TextView) box.getChildAt(1)).setTextColor(primary);
            }
        }
    }

    private int primaryTextColor() {
        if (AppPrefs.isCustomTextColorEnabled(this)) {
            return AppPrefs.getTextColor(this);
        }
        return AppPrefs.usesDarkTextPalette(this) ? 0xFF0F172A : 0xFFE8EAED;
    }

    private int alertTextColor() {
        if (AppPrefs.isCustomTextColorEnabled(this)) {
            return AppPrefs.getTextColor(this);
        }
        return AppPrefs.usesDarkTextPalette(this) ? 0xFF7C2D12 : 0xFFFFF7ED;
    }

    private int detailTextColor() {
        if (AppPrefs.isCustomTextColorEnabled(this)) {
            return AppPrefs.getTextColor(this);
        }
        return AppPrefs.usesDarkTextPalette(this) ? 0xFF1E3A8A : 0xFFC7D2FE;
    }

    private int withAlpha(int color, int alphaPercent) {
        int alpha = Math.max(0, Math.min(255, Math.round(alphaPercent * 255f / 100f)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private void refreshRoadTitle() {
        String road = TextUtils.isEmpty(currentRoadName) ? "待命" : currentRoadName;
        if (titleText != null) {
            titleText.setText(road);
        }
        if (clusterTitleText != null) {
            clusterTitleText.setText(road);
        }
    }

    private void refreshStatusSummary() {
        String heading = TextUtils.isEmpty(currentHeadingSummary) ? "车头\n--" : "车头\n" + currentHeadingSummary;
        String roadType = TextUtils.isEmpty(currentRoadTypeSummary) ? "道路\n未透出" : "道路\n" + currentRoadTypeSummary;
        if (headingInfoText != null) {
            headingInfoText.setText(heading);
        }
        if (clusterHeadingInfoText != null) {
            clusterHeadingInfoText.setText(heading);
        }
        if (roadInfoText != null) {
            roadInfoText.setText(roadType);
        }
        if (clusterRoadInfoText != null) {
            clusterRoadInfoText.setText(roadType);
        }
    }

    private void refreshTurnCard() {
        String lead = TextUtils.isEmpty(currentTurnLead) ? "下一路口" : currentTurnLead;
        if (turnLeadText != null) {
            turnLeadText.setText(lead);
        }
        if (clusterTurnLeadText != null) {
            clusterTurnLeadText.setText(lead);
        }
        if (turnLeadIconView != null) {
            applyTurnIcon(turnLeadIconView, currentTurnIcon);
            turnLeadIconView.setVisibility(currentTurnIcon > 0 ? View.VISIBLE : View.GONE);
        }
        if (clusterTurnLeadIconView != null) {
            applyTurnIcon(clusterTurnLeadIconView, currentTurnIcon);
            clusterTurnLeadIconView.setVisibility(currentTurnIcon > 0 ? View.VISIBLE : View.GONE);
        }
        String turnRoadText = currentTurnRoad;
        if (turnDistanceText == null && !TextUtils.isEmpty(currentTurnDistance)) {
            turnRoadText = TextUtils.isEmpty(turnRoadText)
                    ? currentTurnDistance
                    : turnRoadText + "  " + currentTurnDistance;
        }
        String clusterTurnRoadText = currentTurnRoad;
        if (clusterTurnDistanceText == null && !TextUtils.isEmpty(currentTurnDistance)) {
            clusterTurnRoadText = TextUtils.isEmpty(clusterTurnRoadText)
                    ? currentTurnDistance
                    : clusterTurnRoadText + "  " + currentTurnDistance;
        }
        if (turnText != null) {
            turnText.setText(turnRoadText);
            turnText.setVisibility(TextUtils.isEmpty(turnRoadText) ? View.GONE : View.VISIBLE);
        }
        if (clusterTurnText != null) {
            clusterTurnText.setText(clusterTurnRoadText);
            clusterTurnText.setVisibility(TextUtils.isEmpty(clusterTurnRoadText) ? View.GONE : View.VISIBLE);
        }
        if (turnDistanceText != null) {
            turnDistanceText.setText(currentTurnDistance);
            turnDistanceText.setVisibility(TextUtils.isEmpty(currentTurnDistance) ? View.GONE : View.VISIBLE);
        }
        if (clusterTurnDistanceText != null) {
            clusterTurnDistanceText.setText(currentTurnDistance);
            clusterTurnDistanceText.setVisibility(TextUtils.isEmpty(currentTurnDistance) ? View.GONE : View.VISIBLE);
        }
        if (turnIconView != null) {
            applyTurnIcon(turnIconView, currentTurnIcon);
            turnIconView.setVisibility(currentTurnIcon > 0 ? View.VISIBLE : View.GONE);
        }
        if (clusterTurnIconView != null) {
            applyTurnIcon(clusterTurnIconView, currentTurnIcon);
            clusterTurnIconView.setVisibility(currentTurnIcon > 0 ? View.VISIBLE : View.GONE);
        }
        updateDistanceBadge(turnDistBadge, currentTurnDistance);
        updateDistanceBadge(clusterTurnDistBadge, currentTurnDistance);
        updateNavTurn(navTurnBox, navTurnIconView, navTurnDistText);
        updateNavTurn(clusterNavTurnBox, clusterNavTurnIconView, clusterNavTurnDistText);
        // Dynamic island: update nav turn road name
        if (compactNavTurnRoadText != null) {
            String roadName = currentTurnRoad;
            if ("下一路口".equals(roadName) && !TextUtils.isEmpty(currentRoadName)) {
                roadName = currentRoadName;
            }
            if (TextUtils.isEmpty(roadName) || "下一路口".equals(roadName)) {
                compactNavTurnRoadText.setVisibility(View.GONE);
            } else {
                updateCompactMarqueeText(compactNavTurnRoadText, roadName);
            }
        }
        if (compactCruiseRoadText != null && inCruiseMode) {
            if (!AppPrefs.isCardUiEnabled(this)) {
                String road = !TextUtils.isEmpty(currentRoadName) ? currentRoadName : "";
                if (!TextUtils.equals(compactCruiseRoadText.getText(), road)) {
                    compactCruiseRoadText.setText(road);
                }
            }
        }
        updateClusterCompactTurnText();
        if (isDynamicIslandOrCard()) {
            ensureFullModeAlternator();
            updateDynamicIslandLayout();
            updateCardLayout();
        }
        applyNavigationTextVisualState();
        syncLaneVisibility();
    }

    private void restoreTurnBlinkVisualState() {
        applyNavigationTextVisualState();
    }

    private void clearTurnState() {
        currentTurnLead = "";
        currentTurnRoad = "";
        currentTurnDistance = "";
        currentTurnDistanceMeters = -1;
        currentTurnIcon = 0;
        navigationTurnDir = -1;
        stopTurnBlink();
        // NOTE: exit info (12011) is independent of turn state (10001);
        // do NOT clear exitResultState/exitNameNum here — let ensureExitAlternator
        // decide whether to pause/resume based on currentTurnIcon.
        refreshTurnCard();
        if (compactNavTurnRoadText != null) {
            compactNavTurnRoadText.setVisibility(View.GONE);
        }
        if (clusterCompactNavTurnRoadText != null) {
            clusterCompactNavTurnRoadText.setVisibility(View.GONE);
        }
        stopCompactBreathing();
        stopFullModeAlternator();
        setPairedVisibility(turnCard, clusterTurnCard, false);
        setPairedVisibility(turnRowLayout, clusterTurnRowLayout, false);
        setPairedVisibility(turnText, clusterTurnText, false);
        setPairedVisibility(turnDistanceText, clusterTurnDistanceText, false);
        setPairedVisibility(turnIconView, clusterTurnIconView, false);
        if (turnIconView != null) {
            turnIconView.setImageDrawable(null);
        }
        if (clusterTurnIconView != null) {
            clusterTurnIconView.setImageDrawable(null);
        }
        if (turnLeadIconView != null) {
            turnLeadIconView.setImageDrawable(null);
            turnLeadIconView.setVisibility(View.GONE);
        }
        if (clusterTurnLeadIconView != null) {
            clusterTurnLeadIconView.setImageDrawable(null);
            clusterTurnLeadIconView.setVisibility(View.GONE);
        }
        if (navTurnBox != null) {
            navTurnBox.setVisibility(View.GONE);
        }
        if (clusterNavTurnBox != null) {
            clusterNavTurnBox.setVisibility(View.GONE);
        }
        if (navTurnIconView != null) {
            navTurnIconView.setImageDrawable(null);
        }
        if (clusterNavTurnIconView != null) {
            clusterNavTurnIconView.setImageDrawable(null);
        }
        if (navTurnDistText != null) {
            navTurnDistText.setText("");
        }
        if (clusterNavTurnDistText != null) {
            clusterNavTurnDistText.setText("");
        }
        syncLaneVisibility();
    }

    private void refreshAlertCard() {
        String badge = currentLimitSpeed > 0 ? String.valueOf(currentLimitSpeed) : "--";
        if (limitBadgeText != null) {
            limitBadgeText.setText(badge);
        }
        if (clusterLimitBadgeText != null) {
            clusterLimitBadgeText.setText(badge);
        }
    }

    private void syncModeVisibility() {
        boolean visible = AppPrefs.isModeVisible(this) && modeText != null;
        if (modeRow != null || clusterModeRow != null) {
            setPairedVisibility(modeRow, clusterModeRow, visible);
            setPairedVisibility(titleText, clusterTitleText, visible);
        } else {
            setPairedVisibility(modeText, clusterModeText, visible);
        }
    }

    private void syncTurnVisibility() {
        boolean visible = AppPrefs.isTurnVisible(this)
                && ((turnText != null && !TextUtils.isEmpty(turnText.getText()))
                || (turnDistanceText != null && !TextUtils.isEmpty(turnDistanceText.getText()))
                || currentTurnIcon > 0);
        if (turnRowLayout != null || clusterTurnRowLayout != null) {
            setPairedVisibility(turnRowLayout, clusterTurnRowLayout, visible);
        } else if (turnCard != null || clusterTurnCard != null) {
            setPairedVisibility(turnCard, clusterTurnCard, visible);
        } else {
            setPairedVisibility(turnText, clusterTurnText, visible);
        }
    }

    private void syncLaneVisibility() {
        boolean turnPriority = (navTurnBox != null && navTurnBox.getVisibility() == View.VISIBLE)
                || (clusterNavTurnBox != null && clusterNavTurnBox.getVisibility() == View.VISIBLE);
        // Dynamic island mode: lanes always visible alongside nav turn info
        if (isDynamicIslandOrCard()) {
            turnPriority = false;
        }
        boolean visible = AppPrefs.isLaneVisible(this)
                && laneBar != null
                && laneBar.getVisibility() == View.VISIBLE
                && !turnPriority;
        setPairedVisibility(laneSection, clusterLaneSection, visible);
    }

    private void syncTrafficLightVisibility() {
        boolean visible = AppPrefs.isLightVisible(this)
                && lightRow != null
                && lightRow.getChildCount() > 0;
        setPairedVisibility(lightRow, clusterLightRow, visible);
    }

    private void syncEtaVisibility() {
        boolean visible = (AppPrefs.isEtaVisible(this) || AppPrefs.shouldShowDestination(this))
                && etaText != null
                && !TextUtils.isEmpty(etaText.getText());
        setPairedVisibility(etaText, clusterEtaText, visible);
    }

    private void syncServiceAreaVisibility() {
        boolean visible = AppPrefs.isServiceAreaVisible(this)
                && serviceAreaText != null
                && !TextUtils.isEmpty(serviceAreaText.getText());
        setPairedVisibility(serviceAreaText, clusterServiceAreaText, visible);
    }

    private void syncAlertVisibility() {
        boolean visible;
        if (AppPrefs.isCardUiEnabled(this)) {
            visible = AppPrefs.isAlertVisible(this)
                    && hasFreshEdogAlert();
        } else {
            visible = AppPrefs.isAlertVisible(this)
                    && hasFreshEdogAlert();
        }
        if (alertCard != null || clusterAlertCard != null) {
            setPairedVisibility(alertCard, clusterAlertCard, visible);
        } else if (alertRow != null || clusterAlertRow != null) {
            setPairedVisibility(alertRow, clusterAlertRow, visible);
        } else {
            setPairedVisibility(alertText, clusterAlertText, visible);
        }
    }

    private void syncDetailVisibility() {
        boolean visible = AppPrefs.isDetailVisible(this)
                && detailText != null
                && !TextUtils.isEmpty(detailText.getText());
        setPairedVisibility(detailText, clusterDetailText, visible);
    }

    private void setPairedVisibility(View main, View cluster, boolean visible) {
        int state = visible ? View.VISIBLE : View.GONE;
        boolean changed = false;
        if (main != null) {
            changed |= main.getVisibility() != state;
            main.setVisibility(state);
        }
        if (cluster != null) {
            changed |= cluster.getVisibility() != state;
            cluster.setVisibility(state);
        }
        if (changed) {
            mainHandler.removeCallbacks(panelContentChanged);
            mainHandler.post(panelContentChanged);
        }
    }

    private TextView compactText(float size, boolean detailStyle) {
        return compactText(this, size, detailStyle);
    }

    private TextView compactText(Context context, float size, boolean detailStyle) {
        return compactText(context, size, detailStyle, overlayScale);
    }

    private TextView compactText(Context context, float size, boolean detailStyle, float scale) {
        TextView view = new TextView(context);
        view.setTextColor(detailStyle ? detailTextColor() : alertTextColor());
        view.setTextSize(scaledSp(size, scale));
        view.setSingleLine(false);
        view.setMaxLines(2);
        view.setGravity(Gravity.CENTER);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setPadding(scaledDp(8, scale), scaledDp(2, scale), scaledDp(8, scale), scaledDp(2, scale));
        return view;
    }

    private LinearLayout buildEdogAlertRow(Context context, float scale) {
        return buildEdogAlertRow(context, scale, 24);
    }

    private LinearLayout buildEdogAlertRow(Context context, float scale, int iconSizeDp) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        int iconSize = scaledDp(iconSizeDp, scale);

        FrameLayout speedBox = new FrameLayout(context);
        speedBox.setTag("speed_box");
        ImageView speedIcon = new ImageView(context);
        applyEdogIcon(speedIcon, -1, true);
        speedIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        speedBox.addView(speedIcon, new FrameLayout.LayoutParams(iconSize, iconSize));
        TextView speedText = new TextView(context);
        speedText.setTextColor(0xFFDC2626);
        speedText.setTextSize(scaledSp(11f, scale));
        speedText.setTypeface(Typeface.DEFAULT_BOLD);
        speedText.setGravity(Gravity.CENTER);
        speedText.setIncludeFontPadding(false);
        speedBox.addView(speedText, new FrameLayout.LayoutParams(iconSize, iconSize));
        speedBox.setVisibility(View.GONE);
        LinearLayout.LayoutParams speedLp = new LinearLayout.LayoutParams(-2, -2);
        speedLp.setMargins(0, 0, scaledDp(6, scale), 0);
        row.addView(speedBox, speedLp);

        LinearLayout cameraBox = new LinearLayout(context);
        cameraBox.setTag("camera_box");
        cameraBox.setOrientation(LinearLayout.HORIZONTAL);
        cameraBox.setGravity(Gravity.CENTER_VERTICAL);
        cameraBox.setVisibility(View.GONE);
        FrameLayout cameraIconFrame = new FrameLayout(context);
        cameraIconFrame.setTag("camera_icon_frame");
        ImageView cameraIcon = new ImageView(context);
        applyEdogIcon(cameraIcon, -1, false);
        cameraIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cameraIconFrame.addView(cameraIcon, new FrameLayout.LayoutParams(iconSize, iconSize));
        TextView cameraSpeedOverlay = new TextView(context);
        cameraSpeedOverlay.setTextColor(0xFFDC2626);
        cameraSpeedOverlay.setTextSize(scaledSp(11f, scale));
        cameraSpeedOverlay.setTypeface(Typeface.DEFAULT_BOLD);
        cameraSpeedOverlay.setGravity(Gravity.CENTER);
        cameraSpeedOverlay.setIncludeFontPadding(false);
        cameraSpeedOverlay.setVisibility(View.GONE);
        cameraIconFrame.addView(cameraSpeedOverlay, new FrameLayout.LayoutParams(iconSize, iconSize));
        cameraBox.addView(cameraIconFrame, new LinearLayout.LayoutParams(iconSize, iconSize));
        TextView cameraText = new TextView(context);
        cameraText.setTextColor(primaryTextColor());
        cameraText.setTextSize(scaledSp(12f, scale));
        cameraText.setTypeface(Typeface.DEFAULT_BOLD);
        cameraBox.addView(cameraText, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams cameraLp = new LinearLayout.LayoutParams(-2, -2);
        cameraLp.setMargins(0, 0, scaledDp(6, scale), 0);
        row.addView(cameraBox, cameraLp);

        LinearLayout lightBox = new LinearLayout(context);
        lightBox.setTag("light_box");
        lightBox.setOrientation(LinearLayout.HORIZONTAL);
        lightBox.setGravity(Gravity.CENTER_VERTICAL);
        lightBox.setVisibility(View.GONE);
        ImageView lightIcon = new ImageView(context);
        applyTrafficLightEdogIcon(lightIcon);
        lightIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        lightBox.addView(lightIcon, new LinearLayout.LayoutParams(iconSize, iconSize));
        TextView lightCount = new TextView(context);
        lightCount.setTextColor(primaryTextColor());
        lightCount.setTextSize(scaledSp(13f, scale));
        lightCount.setTypeface(Typeface.DEFAULT_BOLD);
        lightBox.addView(lightCount, new LinearLayout.LayoutParams(-2, -2));
        row.addView(lightBox, new LinearLayout.LayoutParams(-2, -2));

        FontManager.applyToViewTree(context, row);
        return row;
    }

    private void updateDistanceBadge(TextView badge, String distance) {
        if (badge == null) {
            return;
        }
        if (TextUtils.isEmpty(distance)) {
            badge.setText("");
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setText(distance);
        badge.setVisibility(View.VISIBLE);
    }

    private void updateNavTurn(LinearLayout box, ImageView iconView, TextView distanceText) {
        if (box == null) {
            return;
        }
        if (currentTurnIcon <= 0) {
            box.setVisibility(View.GONE);
            return;
        }
        applyTurnIcon(iconView, currentTurnIcon);
        if (distanceText != null) {
            distanceText.setText(TextUtils.isEmpty(currentTurnDistance) ? "" : currentTurnDistance);
        }
        box.setVisibility(View.VISIBLE);
    }

    private static final int TURN_ARROW_GREEN = 0xFF22C55E;
    private static final android.graphics.PorterDuffColorFilter TURN_GREEN_FILTER =
            new android.graphics.PorterDuffColorFilter(TURN_ARROW_GREEN, android.graphics.PorterDuff.Mode.SRC_IN);

    private void applyTurnIcon(ImageView view, int icon) {
        if (view == null) {
            return;
        }
        int compatible = compatibleTurnIcon(icon);
        Bitmap pluginIcon = PluginAssets.activeIconBitmap(this,
                "turn_" + compatible,
                "sou" + compatible,
                "sou" + compatible + "_night_a530",
                "sou" + compatible + "_night");
        if (pluginIcon != null) {
            view.clearColorFilter();
            view.setImageBitmap(pluginIcon);
            view.setRotation(0f);
            view.setScaleX(1f);
            view.setVisibility(View.VISIBLE);
            return;
        }
        int resId = turnIconResource(icon);
        float rotation = 0f;
        float scaleX = 1f;
        if (resId == 0) {
            resId = fallbackTurnIconResource(icon);
        }
        view.setImageResource(resId);
        view.setColorFilter(TURN_GREEN_FILTER);
        view.setRotation(rotation);
        view.setScaleX(scaleX);
        if (!shouldSyncTurnTextWithArrow() && !exitAlternatorActive) {
            view.setAlpha(1f);
        }
        view.setVisibility(View.VISIBLE);
    }

    private int turnIconResource(int icon) {
        icon = compatibleTurnIcon(icon);
        if (icon <= 0) {
            return 0;
        }
        // The broadcast's 50+ action codes are not the same namespace as sou50+ resources:
        // sou50-sou69 are roundabout-exit artwork, while values like NEW_ICON=65 can mean
        // "enter/continue on main road". Only direct-map the base AutoNavi turn icons here.
        if (icon > 28) {
            return 0;
        }
        return souTurnIconResource(icon);
    }

    private int souTurnIconResource(int icon) {
        int id = getResources().getIdentifier("sou" + icon + "_night_a530", "drawable", getPackageName());
        if (id != 0) {
            return id;
        }
        return getResources().getIdentifier("sou" + icon + "_night", "drawable", getPackageName());
    }

    private int fallbackTurnIconResource(int icon) {
        icon = compatibleTurnIcon(icon);
        switch (icon) {
            case 2:
                return souTurnIconResource(2);
            case 3:
            case 7:
                return souTurnIconResource(3);
            case 4:
            case 6:
                return souTurnIconResource(4);
            case 5:
                return souTurnIconResource(5);
            case 8:
            case 10:
            case 11:
            case 12:
                return souTurnIconResource(8);
            case 19:
                return souTurnIconResource(19);
            case 1:
            case 9:
            case 20:
            default:
                return souTurnIconResource(9);
        }
    }

    private int compatibleTurnIcon(int icon) {
        // AutoNavi broadcasts action codes, not drawable ids. Its own GuideInfoProtocolData
        // switchIcon() maps these extension actions back to base turn icons on supported builds.
        if (icon == 65) {
            return 4;
        }
        if (icon == 66) {
            return 5;
        }
        return icon;
    }

    private void updateModeFromExtras(Bundle extras) {
        int keyType = intValue(extras, "KEY_TYPE", -1);
        int state = intValue(extras, "EXTRA_STATE", -1);
        if (keyType != 10001 && keyType != 10019 && keyType != 60021) {
            return;
        }
        if (keyType == 10019 && state != 8 && state != 9 && state != 24 && state != 25) {
            return;
        }
        int type = intValue(extras, "TYPE", -1);
        int speed = intValue(extras, "CUR_SPEED", intValue(extras, "SPEED", -1));
        String road = valueString(extras, "CUR_ROAD_NAME", "NEXT_ROAD_NAME", "ROAD_NAME", "roadName");
        boolean hasRoute = hasAny(extras, "ROUTE_REMAIN_DIS_AUTO", "ROUTE_REMAIN_TIME_AUTO",
                "ROUTE_REMAIN_DIS", "ROUTE_REMAIN_TIME", "ETA_TEXT");

        String mode;
        if (keyType == 10019 && state == 24) {
            mode = "\u5de1\u822a";
            inCruiseMode = true;
        } else if (keyType == 10019 && state == 25) {
            mode = "\u5de1\u822a\u5df2\u9000\u51fa";
            inCruiseMode = false;
            navigationTurnDir = -1;
            lastDetailedMode = null;
            currentRoadName = "";
            currentHeadingSummary = "";
            currentRoadTypeSummary = "";
            currentRoadType = -1;
            clearTurnState();
            clearExitInfoState();
            clearTmcData();
            clearAlertDetails();
            clearServiceAreaDetails();
            releasePanelSizeHoldsNow();
            updateDynamicIslandLayout();
        } else if (keyType == 10019 && state == 8) {
            mode = "\u5bfc\u822a";
            inCruiseMode = false;
        } else if (keyType == 10019 && state == 9) {
            mode = "\u5bfc\u822a\u5df2\u9000\u51fa";
            inCruiseMode = false;
            navigationTurnDir = -1;
            currentRoadName = "";
            currentHeadingSummary = "";
            currentRoadTypeSummary = "";
            currentRoadType = -1;
            updateDynamicIslandLayout();
            if (etaText != null) {
                etaText.setVisibility(View.GONE);
            }
            if (clusterEtaText != null) {
                clusterEtaText.setVisibility(View.GONE);
            }
            if (lightRow != null) {
                lightRow.setVisibility(View.GONE);
            }
            if (clusterLightRow != null) {
                clusterLightRow.setVisibility(View.GONE);
            }
            trafficLights.clear();
            hideLaneData();
            clearTurnState();
            clearExitInfoState();
            clearTmcData();
            clearAlertDetails();
            clearServiceAreaDetails();
            if (detailText != null) {
                detailText.setVisibility(View.GONE);
            }
            if (clusterDetailText != null) {
                clusterDetailText.setVisibility(View.GONE);
            }
            currentLimitSpeed = -1;
            releasePanelSizeHoldsNow();
        } else if (type == 1) {
            mode = "\u6a21\u62df\u5bfc\u822a";
        } else if (type == 2 || (!hasRoute && (speed >= 0 || !TextUtils.isEmpty(road)))) {
            mode = "\u5de1\u822a";
            inCruiseMode = true;
        } else if (keyType == 10001 || hasRoute) {
            mode = "\u5bfc\u822a";
            inCruiseMode = false;
        } else {
            mode = "\u5df2\u8fde\u63a5";
            currentRoadName = "";
            currentRoadTypeSummary = "";
            currentRoadType = -1;
            clearTurnState();
            clearExitInfoState();
            clearTmcData();
            clearServiceAreaDetails();
        }

        StringBuilder sb = new StringBuilder(mode);
        if (!TextUtils.isEmpty(road)) {
            sb.append(" \u00b7 ").append(road);
        }
        if (speed >= 0) {
            sb.append(" \u00b7 ").append(speed).append(" km/h");
        }
        String text = sb.toString();
        currentModeLabel = text;
        if ("\u5df2\u8fde\u63a5".equals(mode) && !TextUtils.isEmpty(lastDetailedMode)) {
            return;
        }
        if (!"\u5df2\u8fde\u63a5".equals(mode)
                && (!TextUtils.isEmpty(road) || speed >= 0 || "\u5de1\u822a".equals(mode))) {
            lastDetailedMode = text;
        }
        if (!TextUtils.isEmpty(road)) {
            currentRoadName = road;
        }
        if (modeText != null) {
            modeText.setText(text);
        }
        if (clusterModeText != null) {
            clusterModeText.setText(text);
        }
        refreshRoadTitle();
        refreshAlertCard();
        updateDynamicIslandLayout();
        syncModeVisibility();
        if (AppPrefs.isModeVisible(this)) {
            showAnyPanel();
        }
    }

    private void updateTurnFromExtras(Bundle extras) {
        boolean compactMode = isAnyDynamicIslandUiEnabled();
        if (turnText == null && (compactNavTurnRoadText == null || !compactMode) && !AppPrefs.isCardUiEnabled(this)) {
            return;
        }
        int keyType = intValue(extras, "KEY_TYPE", -1);
        if (keyType != 10001) {
            return;
        }
        if (inCruiseMode) {
            clearTurnState();
            return;
        }
        int icon = intValue(extras, "NEW_ICON", intValue(extras, "ICON", 0));
        if (icon <= 0) {
            clearTurnState();
            return;
        }
        navigationTurnDir = turnIconToTrafficDir(icon);
        currentTurnIcon = icon;
        // Always extract raw meters for distance-based blink, regardless of auto-format
        int meters = intValue(extras, "SEG_REMAIN_DIS", intValue(extras, "NEXT_SEG_REMAIN_DIS", -1));
        String distance = valueString(extras, "SEG_REMAIN_DIS_AUTO", "NEXT_SEG_REMAIN_DIS_AUTO");
        if (TextUtils.isEmpty(distance)) {
            if (meters > 0) {
                distance = (compactMode || AppPrefs.isCardUiEnabled(this)) ? formatDistanceCompact(meters) : formatDistance(meters);
            }
        } else if (compactMode || AppPrefs.isCardUiEnabled(this)) {
            distance = distance.replace("\u516c\u91cc", "km").replace("\u7c73", "m");
        }
        // Fallback: parse formatted string if raw integer unavailable
        if (meters <= 0 && !TextUtils.isEmpty(distance)) {
            meters = parseDistanceMeters(distance);
        }
        currentTurnDistanceMeters = meters > 0 ? meters : -1;
        String nextRoad = valueString(extras, "NEXT_ROAD_NAME", "nextRoadName", "NEXT_ROAD",
                "NEXT_ROAD_NAME_AUTO", "SEG_ROAD_NAME", "NEXT_SEG_ROAD_NAME", "ROAD_NAME", "roadName");
        currentTurnLead = "\u4e0b\u4e00\u8def\u53e3";
        currentTurnRoad = !TextUtils.isEmpty(nextRoad) ? nextRoad
                : !TextUtils.isEmpty(currentRoadName) ? currentRoadName : "\u4e0b\u4e00\u8def\u53e3";
        currentTurnDistance = TextUtils.isEmpty(distance) ? "" : distance;
        ensureExitAlternator();
        refreshTurnCard();
        syncTurnVisibility();
        startTurnBlink();
        if (AppPrefs.isTurnVisible(this)) {
            showAnyPanel();
        }
    }

    private void updateTrafficLights(Bundle extras) {
        if (lightRow == null) {
            return;
        }
        TrafficLightParser.Result result = TrafficLightParser.parse(
                extras, inCruiseMode, navigationTurnDir, currentTurnIcon, trafficLights);
        inCruiseMode = result.setInCruiseMode;
        if (result.changed) {
            replaceTrafficLights(result.lights);
        }
        renderTrafficLights();
    }

    private boolean preferLightState(TrafficLightParser.LightState candidate, TrafficLightParser.LightState old) {
        if (TrafficLightParser.isRedLightStatus(old.status) && !TrafficLightParser.isRedLightStatus(candidate.status)) {
            return false;
        }
        if (TrafficLightParser.isRedLightStatus(candidate.status) && !TrafficLightParser.isRedLightStatus(old.status)) {
            return true;
        }
        return candidate.seconds > 0;
    }

    private void replaceTrafficLights(HashMap<Integer, TrafficLightParser.LightState> nextLights) {
        trafficLights.clear();
        trafficLights.putAll(nextLights);
    }

    private void mergeTrafficLights(HashMap<Integer, TrafficLightParser.LightState> nextLights) {
        for (Map.Entry<Integer, TrafficLightParser.LightState> entry : nextLights.entrySet()) {
            TrafficLightParser.LightState old = trafficLights.get(entry.getKey());
            TrafficLightParser.LightState state = entry.getValue();
            if (old == null || old.status != state.status || preferLightState(state, old)) {
                trafficLights.put(entry.getKey(), state);
            }
        }
    }

    private void renderTrafficLights() {
        if (lightRow == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, TrafficLightParser.LightState>> iterator = trafficLights.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrafficLightParser.LightState> entry = iterator.next();
            TrafficLightParser.LightState state = entry.getValue();
            if (now - state.updatedAt > state.ttlMs || TrafficLightParser.currentLightSeconds(state, now) <= 0) {
                iterator.remove();
            }
        }
        if (trafficLights.isEmpty()) {
            mainHandler.removeCallbacks(trafficLightTicker);
            lightRow.removeAllViews();
            lightRow.setVisibility(View.GONE);
            if (clusterLightRow != null) {
                clusterLightRow.removeAllViews();
                clusterLightRow.setVisibility(View.GONE);
            }
            return;
        }

        ArrayList<Integer> keys = new ArrayList<>(trafficLights.keySet());
        Collections.sort(keys, (a, b) -> TrafficLightParser.directionPriority(a, inCruiseMode) - TrafficLightParser.directionPriority(b, inCruiseMode));
        if (!inCruiseMode && keys.size() > 1) {
            Integer preferred = preferredNavigationLightKey(keys);
            keys.clear();
            if (preferred != null) {
                keys.add(preferred);
            }
        }
        lightRow.removeAllViews();
        if (clusterLightRow != null) { clusterLightRow.removeAllViews(); }
        int activeLightCount = 0;
        boolean anyBreathing = false;
        for (Integer key : keys) {
            TrafficLightParser.LightState state = trafficLights.get(key);
            if (state == null) continue;
            int s = TrafficLightParser.currentLightSeconds(state, now);
            if (s > 0) {
                activeLightCount++;
                if (s <= 4) { anyBreathing = true; breathingLightKeys.add(key); }
                else { breathingLightKeys.remove(key); }
            }
        }
        String uiStyle = AppPrefs.getOverlayUiStyle(this);
        boolean isClassicOrNew = OverlayUiStyles.OLD.equals(uiStyle) || OverlayUiStyles.NEW.equals(uiStyle);
        float lightCountScale = 1f;
        if (isClassicOrNew && activeLightCount >= 2) lightCountScale = 0.75f;
        float mainScale = overlayScale * lightCountScale;
        float clScale = clusterScale * lightCountScale;
        long breathNow = System.currentTimeMillis();
        float breathAlpha = 1f;
        if (anyBreathing) {
            double phase = (breathNow % 1000L) / 1000.0;
            breathAlpha = 0.25f + 0.75f * (float) Math.abs(Math.sin(phase * Math.PI));
        }
        boolean showMainDirectionLabel = true;
        boolean showClusterDirectionLabel = true;
        for (Integer key : keys) {
            TrafficLightParser.LightState state = trafficLights.get(key);
            if (state == null) continue;
            int seconds = TrafficLightParser.currentLightSeconds(state, now);
            if (seconds <= 0) { breathingLightKeys.remove(key); continue; }
            View pill = lightPill(this, state, showMainDirectionLabel, mainScale, seconds);
            if (anyBreathing && seconds <= 4) pill.setAlpha(breathAlpha);
            lightRow.addView(pill);
            if (clusterLightRow != null && clusterContext != null) {
                View clusterPill = lightPill(clusterContext, state, showClusterDirectionLabel, clScale, seconds);
                if (anyBreathing && seconds <= 4) clusterPill.setAlpha(breathAlpha);
                clusterLightRow.addView(clusterPill);
            }
        }
        syncTrafficLightVisibility();
        if (AppPrefs.isLightVisible(this) && lightRow.getChildCount() > 0) showAnyPanel();
        mainHandler.removeCallbacks(trafficLightTicker);
        if (!trafficLights.isEmpty()) {
            mainHandler.postDelayed(trafficLightTicker, anyBreathing ? 120L : LIGHT_TICK_MS);
        }
    }

    private Integer preferredNavigationLightKey(ArrayList<Integer> keys) {
        if (navigationTurnDir >= 0 && trafficLights.containsKey(navigationTurnDir)) {
            return navigationTurnDir;
        }
        Integer best = null;
        for (Integer key : keys) {
            TrafficLightParser.LightState state = trafficLights.get(key);
            if (state == null) {
                continue;
            }
            if (best == null) {
                best = key;
                continue;
            }
            TrafficLightParser.LightState old = trafficLights.get(best);
            if (old == null || TrafficLightParser.currentLightSeconds(state, System.currentTimeMillis())
                    < TrafficLightParser.currentLightSeconds(old, System.currentTimeMillis())) {
                best = key;
            }
        }
        return best;
    }

    private View lightPill(TrafficLightParser.LightState state, boolean showDirectionLabel) {
        return lightPill(this, state, showDirectionLabel);
    }

    private View lightPill(Context context, TrafficLightParser.LightState state, boolean showDirectionLabel) {
        return lightPill(context, state, showDirectionLabel, overlayScale);
    }

    private View lightPill(Context context, TrafficLightParser.LightState state, boolean showDirectionLabel, float scale) {
        return lightPill(context, state, showDirectionLabel, scale,
                TrafficLightParser.currentLightSeconds(state, System.currentTimeMillis()));
    }

    private View lightPill(Context context, TrafficLightParser.LightState state, boolean showDirectionLabel,
                           float scale, int seconds) {
        float oldDensity = activeDensity;
        activeDensity = context.getResources().getDisplayMetrics().density;
        try {
            boolean showArrowBadge = showDirectionLabel && state.dir >= 0;

            // Dynamic island (main mode) cruise: use classic capsule style matching lane size
            boolean useClassicCruisePill = AppPrefs.isCardUiEnabled(this)
                    || (AppPrefs.isDynamicIslandUiEnabled(this) && inCruiseMode);
            // Dynamic island (test mode) keeps its own compact style
            if (!useClassicCruisePill && AppPrefs.isDynamicIslandUiEnabled(this)) {
                return buildCompactLightPill(context, state, showArrowBadge, scale, seconds, oldDensity);
            }

            boolean dynamicIsland = AppPrefs.isDynamicIslandUiEnabled(this);
            // Cruise mode in main dynamic island uses classic sizing matched to lane
            if (useClassicCruisePill) {
                dynamicIsland = true;
            }
            int pillHeight = useClassicCruisePill ? 24 : (dynamicIsland ? 36 : 44);
            int arrowSize = useClassicCruisePill ? 14 : (dynamicIsland ? 17 : 25);
            int minW = showArrowBadge ? (useClassicCruisePill ? 46 : (dynamicIsland ? 76 : 92))
                    : (useClassicCruisePill ? 44 : (dynamicIsland ? 62 : 76));

            LinearLayout view = new LinearLayout(context);
            view.setOrientation(LinearLayout.HORIZONTAL);
            view.setGravity(Gravity.CENTER);
            view.setMinimumWidth(scaledDp(minW, scale));
            view.setMinimumHeight(scaledDp(pillHeight, scale));
            int padH = useClassicCruisePill ? 6 : (showArrowBadge ? (dynamicIsland ? 5 : 6) : (dynamicIsland ? 7 : 8));
            int padV = useClassicCruisePill ? 1 : (dynamicIsland ? 3 : 4);
            int padR = useClassicCruisePill ? 6 : (showArrowBadge ? (dynamicIsland ? 8 : 10) : (dynamicIsland ? 8 : 10));
            view.setPadding(scaledDp(padH, scale), scaledDp(padV, scale),
                    scaledDp(padR, scale), scaledDp(padV, scale));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(withAlpha(state.color, 92));
            bg.setCornerRadius(scaledDp(pillHeight / 2, scale));
            bg.setStroke(scaledDp(1, scale), withAlpha(0xFFFFFFFF, 72));
            view.setBackground(bg);

            if (showArrowBadge) {
                View arrow;
                if (useClassicCruisePill) {
                    // Create arrow without inner padding for tighter cruise capsule
                    Bitmap diyBitmap = loadDiyArrowBitmap(state.dir);
                    if (diyBitmap != null) {
                        ImageView img = new ImageView(context);
                        img.setImageBitmap(diyBitmap);
                        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        arrow = img;
                    } else {
                        ImageView img = new ImageView(context);
                        img.setImageResource(defaultCruiseArrowResource(state.dir));
                        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        arrow = img;
                    }
                } else {
                    arrow = diyArrowBadge(context, state, scale);
                }
                LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(
                        scaledDp(arrowSize, scale), scaledDp(arrowSize, scale));
                arrowLp.setMargins(0, 0, scaledDp(useClassicCruisePill ? 1 : (dynamicIsland ? 5 : 6), scale), 0);
                view.addView(arrow, arrowLp);
            } else if (!useClassicCruisePill && state.color != AmapConstants.COLOR_YELLOW) {
                TextView dot = new TextView(context);
                GradientDrawable dotBg = new GradientDrawable();
                dotBg.setShape(GradientDrawable.OVAL);
                dotBg.setColor(state.color);
                dotBg.setStroke(scaledDp(3, scale), withAlpha(0xFFFFFFFF, 76));
                dot.setBackground(dotBg);
                LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(
                        scaledDp(arrowSize, scale), scaledDp(arrowSize, scale));
                dotLp.setMargins(0, 0, scaledDp(dynamicIsland ? 5 : 7, scale), 0);
                view.addView(dot, dotLp);
            }

            LinearLayout textColumn = new LinearLayout(context);
            textColumn.setOrientation(LinearLayout.VERTICAL);
            textColumn.setGravity(Gravity.CENTER);

            TextView time = new TextView(context);
            time.setText(String.valueOf(seconds));
            time.setTextColor(Color.WHITE);
            time.setTextSize(TypedValue.COMPLEX_UNIT_PX, scaledDp(useClassicCruisePill ? 14f : (dynamicIsland ? 17f : 21f), scale));
            time.setTypeface(Typeface.DEFAULT_BOLD);
            time.setGravity(Gravity.CENTER);
            textColumn.addView(time, new LinearLayout.LayoutParams(-2, -2));

            view.addView(textColumn, new LinearLayout.LayoutParams(-2, -2));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, scaledDp(pillHeight, scale));
            lp.setMargins(scaledDp(useClassicCruisePill ? 2 : (dynamicIsland ? 2 : 3), scale), scaledDp(1, scale),
                    scaledDp(useClassicCruisePill ? 2 : (dynamicIsland ? 2 : 3), scale), scaledDp(1, scale));
            view.setLayoutParams(lp);
            FontManager.applyToViewTree(context, view);
            return view;
        } finally {
            activeDensity = oldDensity;
        }
    }

    private View buildCompactLightPill(Context context, TrafficLightParser.LightState state, boolean showArrowBadge,
                                        float scale, int seconds, float oldDensity) {
        int circleSize = scaledDp(28, scale);

        FrameLayout circle = new FrameLayout(context);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(withAlpha(state.color, 92));
        circleBg.setStroke(scaledDp(2, scale), state.color);
        circle.setBackground(circleBg);

        boolean cruiseArrow = inCruiseMode && showArrowBadge;
        if (cruiseArrow) {
            View arrow = diyArrowBadge(context, state, scale);
            int arrowSize = scaledDp(20, scale);
            FrameLayout.LayoutParams arrowLp = new FrameLayout.LayoutParams(arrowSize, arrowSize);
            arrowLp.gravity = Gravity.CENTER;
            circle.addView(arrow, arrowLp);
        } else {
            TextView time = new TextView(context);
            time.setText(String.valueOf(seconds));
            time.setTextColor(Color.WHITE);
            time.setTextSize(TypedValue.COMPLEX_UNIT_PX, scaledDp(seconds >= 100 ? 10.5f : 12.5f, scale));
            time.setTypeface(Typeface.DEFAULT_BOLD);
            time.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams timeLp = new FrameLayout.LayoutParams(-1, -1);
            timeLp.gravity = Gravity.CENTER;
            circle.addView(time, timeLp);
        }

        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.CENTER);
        view.addView(circle, new LinearLayout.LayoutParams(circleSize, circleSize));
        if (cruiseArrow) {
            TextView time = new TextView(context);
            time.setText(String.valueOf(seconds));
            time.setTextColor(Color.WHITE);
            time.setTextSize(TypedValue.COMPLEX_UNIT_PX, scaledDp(seconds >= 100 ? 10.5f : 12f, scale));
            time.setTypeface(Typeface.DEFAULT_BOLD);
            time.setGravity(Gravity.CENTER);
            time.setIncludeFontPadding(false);
            LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(-2, -2);
            timeLp.setMargins(0, scaledDp(1, scale), 0, 0);
            view.addView(time, timeLp);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(scaledDp(2, scale), scaledDp(1, scale), scaledDp(2, scale), scaledDp(1, scale));
        view.setLayoutParams(lp);
        FontManager.applyToViewTree(context, view);
        activeDensity = oldDensity;
        return view;
    }

    private View diyArrowBadge(Context context, TrafficLightParser.LightState state, float scale) {
        Bitmap bitmap = loadDiyArrowBitmap(state.dir);
        if (bitmap != null) {
            ImageView image = new ImageView(context);
            image.setImageBitmap(bitmap);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setAdjustViewBounds(false);
            int padding = arrowImagePaddingDp(state.dir);
            image.setPadding(scaledDp(padding, scale), scaledDp(padding, scale),
                    scaledDp(padding, scale), scaledDp(padding, scale));
            return image;
        }

        ImageView image = new ImageView(context);
        image.setImageResource(defaultCruiseArrowResource(state.dir));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAdjustViewBounds(false);
        int padding = arrowImagePaddingDp(state.dir);
        image.setPadding(scaledDp(padding, scale), scaledDp(padding, scale),
                scaledDp(padding, scale), scaledDp(padding, scale));
        return image;
    }

    private int defaultCruiseArrowResource(int dir) {
        int souIcon = trafficDirToTurnIcon(dir);
        int souRes = turnIconResource(souIcon);
        if (souRes != 0) {
            return souRes;
        }
        if (dir == 0) {
            return souTurnIconResource(8);
        }
        if (dir == 1 || dir == 5 || dir == 6) {
            return souTurnIconResource(2);
        }
        if (dir == 2 || dir == 3 || dir == 7 || dir == 8) {
            return souTurnIconResource(3);
        }
        return souTurnIconResource(9);
    }

    private int arrowImagePaddingDp(int dir) {
        return dir == 0 ? 3 : 1;
    }

    private int trafficDirToTurnIcon(int dir) {
        if (dir == 0) {
            return 8;
        }
        if (dir == 1 || dir == 5 || dir == 6) {
            return 2;
        }
        if (dir == 2 || dir == 3 || dir == 7 || dir == 8) {
            return 3;
        }
        return 9;
    }

    private Bitmap loadDiyArrowBitmap(int dir) {
        Bitmap pluginBitmap = PluginAssets.activeIconBitmap(this,
                diyArrowBaseName(dir),
                "traffic_light_" + diyArrowBaseName(dir));
        if (pluginBitmap != null) {
            return pluginBitmap;
        }
        String[] fileNames = diyArrowFileNames(dir);
        if (fileNames.length == 0) {
            return null;
        }
        File diyDir = new File(Environment.getExternalStorageDirectory(), DIY_DIR_NAME);
        try {
            if (!diyDir.isDirectory()) {
                diyDir.mkdirs();
            }
        } catch (Throwable ignored) {
        }
        for (String fileName : fileNames) {
            try {
                File file = new File(diyDir, fileName);
                if (!file.isFile()) {
                    diyArrowCache.remove(fileName);
                    diyArrowModified.remove(fileName);
                    continue;
                }
                long modified = file.lastModified();
                Long cachedModified = diyArrowModified.get(fileName);
                Bitmap cached = diyArrowCache.get(fileName);
                if (cached != null && cachedModified != null && cachedModified == modified && !cached.isRecycled()) {
                    return cached;
                }
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap == null) {
                    diyArrowCache.remove(fileName);
                    diyArrowModified.remove(fileName);
                    continue;
                }
                diyArrowCache.put(fileName, bitmap);
                diyArrowModified.put(fileName, modified);
                return bitmap;
            } catch (Throwable t) {
                Log.d(TAG, "load diy arrow failed: " + fileName, t);
            }
        }
        return null;
    }

    private String[] diyArrowFileNames(int dir) {
        String base = diyArrowBaseName(dir);
        return new String[]{base + ".png", base + ".webp", base + ".jpg", base + ".jpeg"};
    }

    private String diyArrowBaseName(int dir) {
        String base;
        if (dir == 1 || dir == 5 || dir == 6) {
            base = "cruise_arrow_left";
        } else if (dir == 2 || dir == 3 || dir == 7 || dir == 8) {
            base = "cruise_arrow_right";
        } else if (dir == 4) {
            base = "cruise_arrow_straight";
        } else if (dir == 0) {
            base = "cruise_arrow_uturn";
        } else {
            base = "cruise_arrow_default";
        }
        return base;
    }

    private String turnSymbol(int icon, int roundAboutNum) {
        if (icon == 2) {
            return "\u2190";
        }
        if (icon == 3) {
            return "\u21b1";
        }
        if (icon == 4) {
            return "\u2196";
        }
        if (icon == 5) {
            return "\u2197";
        }
        if (icon == 6) {
            return "\u2199";
        }
        if (icon == 7) {
            return "\u2198";
        }
        if (icon == 8 || icon == 10 || icon == 11 || icon == 12) {
            return "\u21b6";
        }
        if (icon == 13 || icon == 14 || icon == 17 || icon == 18) {
            return roundAboutNum > 0 ? ("\u25ef" + roundAboutNum) : "\u25ef";
        }
        if (icon == 9 || icon == 1) {
            return "\u2191";
        }
        if (icon == 19) {
            return "\u21b7";
        }
        if (icon == 20) {
            return "\u2191";
        }
        return "\u2191";
    }

    private int turnIconToTrafficDir(int icon) {
        if (icon == 2 || icon == 4 || icon == 6) {
            return 1;
        }
        if (icon == 3 || icon == 5 || icon == 7 || icon == 19) {
            return 2;
        }
        if (icon == 8) {
            return 0;
        }
        return 4;
    }

    private String bearingToCompass(int bearing) {
        int normalized = ((bearing % 360) + 360) % 360;
        String[] labels = {"\u5317", "\u4e1c\u5317", "\u4e1c", "\u4e1c\u5357", "\u5357", "\u897f\u5357", "\u897f", "\u897f\u5317"};
        int index = Math.round(normalized / 45f) % labels.length;
        return labels[index];
    }

    private void updateEtaFromExtras(Bundle extras) {
        boolean compactMode = isAnyDynamicIslandUiEnabled();
        if (etaText == null && !compactMode) {
            return;
        }
        String distance = valueString(extras, "ROUTE_REMAIN_DIS_AUTO", "routeRemainDistanceAuto", "distance");
        String time = valueString(extras, "ROUTE_REMAIN_TIME_AUTO", "routeRemainTimeAuto", "remainTime");
        String eta = valueString(extras, "ETA_TEXT", "etaText", "eta", "arrivalTime", "arriveTime");
        String road = valueString(extras, "NEXT_ROAD_NAME", "CUR_ROAD_NAME", "roadName", "curRoadName");
        String destination = valueString(extras, "endPOIName", "END_POI_NAME", "END_POI",
                "DESTINATION_NAME", "DESTINATION", "EXTRA_DESTINATION_NAME", "POINAME");

        int remainMeters = -1;
        if (TextUtils.isEmpty(distance)) {
            int meters = intValue(extras, "ROUTE_REMAIN_DIS", -1);
            if (meters > 0) {
                remainMeters = meters;
                distance = (compactMode || AppPrefs.isCardUiEnabled(this)) ? formatDistanceCompact(meters) : formatDistance(meters);
            }
        }
        if (TextUtils.isEmpty(time)) {
            int seconds = intValue(extras, "ROUTE_REMAIN_TIME", -1);
            if (seconds > 0) {
                time = formatDuration(seconds);
            }
        }

        // Card UI: normalize distance/eta text
        if (!TextUtils.isEmpty(distance) && AppPrefs.isCardUiEnabled(this)) {
            distance = distance.replace("公里", "km").replace("米", "m");
        }

        StringBuilder routeText = new StringBuilder();
        if (!TextUtils.isEmpty(distance)) {
            routeText.append(distance);
        }
        if (!TextUtils.isEmpty(time) && !AppPrefs.isCardUiEnabled(this)) {
            if (routeText.length() > 0) {
                routeText.append(" \u00b7 ");
            }
            routeText.append(time);
        }
        if (!TextUtils.isEmpty(eta)) {
            String cleanEta = eta.replace("\u9884\u8ba1", "").replace("\u5230\u8fbe", "\u5230").trim();
            if (!TextUtils.isEmpty(cleanEta)) {
                if (routeText.length() > 0) {
                    routeText.append(" \u00b7 ");
                }
                routeText.append(cleanEta);
            }
        }
        StringBuilder text = new StringBuilder();
        if (AppPrefs.isEtaVisible(this) && routeText.length() > 0) {
            text.append(routeText);
        }
        if (AppPrefs.shouldShowDestination(this) && !TextUtils.isEmpty(destination)) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(destination);
        }

        if (text.length() > 0 || compactMode) {
            currentEtaSummary = text.toString();
            if (etaText != null) {
                etaText.setText(text.toString());
            }
            if (clusterEtaText != null) {
                clusterEtaText.setText(text.toString());
            }
            boolean roadIsEmptyDestination = !TextUtils.isEmpty(destination)
                    && ("\u76ee\u7684\u5730".equals(road) || road.equals(destination));
            if (!TextUtils.isEmpty(road) && !roadIsEmptyDestination) {
                currentRoadName = road;
            }
            if (!compactMode && !isAnyDynamicIslandUiEnabled()) {
                refreshRoadTitle();
                syncEtaVisibility();
                if (AppPrefs.isEtaVisible(this) || AppPrefs.shouldShowDestination(this)) {
                    showAnyPanel();
                }
            } else if (isAnyDynamicIslandUiEnabled()) {
                // Store ETA info for alternating display
                int remainSec = intValue(extras, "ROUTE_REMAIN_TIME", -1);
                if (remainSec <= 0) {
                    String timeStr = valueString(extras, "ROUTE_REMAIN_TIME_AUTO", "routeRemainTimeAuto", "remainTime");
                    if (!TextUtils.isEmpty(timeStr)) {
                        try {
                            remainSec = Integer.parseInt(timeStr.replaceAll("[^0-9]", ""));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                updateFullModeEtaInfo(distance, remainSec);
                if (!TextUtils.isEmpty(road) && !roadIsEmptyDestination) {
                    if (!inCruiseMode) {
                        updateCompactMarqueeText(compactNavTurnRoadText, road);
                        updateCompactMarqueeText(clusterCompactNavTurnRoadText, road);
                    }
                    if (compactCruiseRoadText != null && inCruiseMode) {
                        if (!TextUtils.equals(compactCruiseRoadText.getText(), road)) {
                            compactCruiseRoadText.setText(road);
                        }
                    }
                }
                updateDynamicIslandLayout();
                if (AppPrefs.isEtaVisible(this) || AppPrefs.shouldShowDestination(this)) {
                    showAnyPanel();
                }
            } else if (!TextUtils.isEmpty(road) && !roadIsEmptyDestination) {
                if (!AppPrefs.isCardUiEnabled(this) && compactCruiseRoadText != null && inCruiseMode) {
                    if (!TextUtils.equals(compactCruiseRoadText.getText(), road)) {
                        compactCruiseRoadText.setText(road);
                    }
                }
                updateDynamicIslandLayout();
            }
        }
    }

    private void updateProtocolDetails(Bundle extras) {
        updateAlertDetails(extras);
        updateServiceAreaDetails(extras);
        updateStatusDetails(extras);
    }

    private void updateServiceAreaDetails(Bundle extras) {
        if (!AppPrefs.OVERLAY_UI_OLD.equals(AppPrefs.getOverlayUiStyle(this))) {
            clearServiceAreaDetails();
            return;
        }
        if (serviceAreaText == null && clusterServiceAreaText == null) {
            return;
        }
        ServiceAreaParser.Result result = ServiceAreaParser.parse(extras);
        if (!result.handled) {
            return;
        }
        if (!result.hasEntries()) {
            clearServiceAreaDetails();
            return;
        }
        ArrayList<String> rows = new ArrayList<>();
        for (ServiceAreaParser.Entry entry : result.entries) {
            String row = entry.displayText();
            if (!TextUtils.isEmpty(row)) {
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            clearServiceAreaDetails();
            return;
        }
        setServiceAreaText(join(rows, "\n"));
        syncServiceAreaVisibility();
        if (AppPrefs.isServiceAreaVisible(this)) {
            showAnyPanel();
        }
    }

    private void setServiceAreaText(String text) {
        if (serviceAreaText != null) {
            serviceAreaText.setText(text);
        }
        if (clusterServiceAreaText != null) {
            clusterServiceAreaText.setText(text);
        }
    }

    private void clearServiceAreaDetails() {
        if (serviceAreaText != null) {
            serviceAreaText.setText("");
            serviceAreaText.setVisibility(View.GONE);
        }
        if (clusterServiceAreaText != null) {
            clusterServiceAreaText.setText("");
            clusterServiceAreaText.setVisibility(View.GONE);
        }
    }

    private void updateAlertDetails(Bundle extras) {
        boolean compactMode = isAnyDynamicIslandUiEnabled();
        if (alertText == null && !compactMode && !AppPrefs.isCardUiEnabled(this)) {
            return;
        }
        boolean alertPayload = hasAny(extras, "LIMITED_SPEED", "CAMERA_INDEX", "CAMERA_DIST",
                "CAMERA_SPEED", "CAMERA_TYPE", "SAPA_DIST", "SAPA_NAME", "TRAFFIC_LIGHT_NUM",
                "routeRemainTrafficLightNum");

        int cameraIndex = intValue(extras, "CAMERA_INDEX", 0);
        int cameraDist = intValue(extras, "CAMERA_DIST", -1);
        int cameraType = intValue(extras, "CAMERA_TYPE", -1);
        currentCameraType = cameraType;
        int cameraSpeed = intValue(extras, "CAMERA_SPEED", -1);
        int limitedSpeed = intValue(extras, "LIMITED_SPEED", -1);
        int displaySpeed = limitedSpeed > 0 ? limitedSpeed : cameraSpeed;

        int lightNum = intValue(extras, "routeRemainTrafficLightNum",
                intValue(extras, "TRAFFIC_LIGHT_NUM", -1));
        boolean updated = false;
        if (alertPayload && displaySpeed <= 0) {
            cachedEdogSpeed = -1;
            currentLimitSpeed = -1;
            updated = true;
        } else if (displaySpeed > 0) {
            cachedEdogSpeed = displaySpeed;
            updated = true;
        }
        if (alertPayload && (cameraIndex == -1 || cameraDist < 0)) {
            cachedCameraIndex = -1;
            cachedCameraDist = -1;
            cachedCameraType = -1;
            updated = true;
        } else if (cameraIndex != -1 && cameraDist >= 0) {
            cachedCameraIndex = cameraIndex;
            cachedCameraDist = cameraDist;
            cachedCameraType = cameraType;
            updated = true;
        }
        if (alertPayload && lightNum <= 0) {
            cachedLightNum = -1;
            updated = true;
        } else if (lightNum > 0) {
            cachedLightNum = lightNum;
            updated = true;
        }
        if (updated) {
            cachedEdogUpdatedAt = System.currentTimeMillis();
        } else if (alertPayload) {
            mainHandler.removeCallbacks(alertClear);
            mainHandler.postDelayed(alertClear, ALERT_TTL_MS + 200L);
        }

        if (!hasFreshEdogAlert()) {
            clearAlertDetails();
            return;
        }

        ArrayList<String> parts = new ArrayList<>();
        if (cachedEdogSpeed > 0) {
            parts.add("\u9650\u901f " + cachedEdogSpeed);
        }
        if (cachedCameraIndex != -1 && cachedCameraDist >= 0) {
            StringBuilder camera = new StringBuilder(cameraTypeName(cachedCameraType));
            camera.append(' ').append(formatDistance(cachedCameraDist));
            parts.add(camera.toString());
        }
        if (cachedLightNum > 0) {
            parts.add("\u7ea2\u7eff\u706f " + cachedLightNum + "\u4e2a");
        }
        currentLimitSpeed = cachedEdogSpeed;
        currentAlertSummary = join(parts, "  \u00b7  ");
        if (alertText != null) {
            alertText.setText(currentAlertSummary);
        }
        if (clusterAlertText != null && alertText != null) {
            clusterAlertText.setText(alertText.getText());
        }
        populateEdogAlertRow(alertRow, cachedEdogSpeed, cachedCameraIndex,
                cachedCameraDist, cachedCameraType, cachedLightNum);
        populateEdogAlertRow(clusterAlertRow, cachedEdogSpeed, cachedCameraIndex,
                cachedCameraDist, cachedCameraType, cachedLightNum);
        populateCompactWidgetRow(cachedEdogSpeed, cachedCameraIndex,
                cachedCameraDist, cachedCameraType, cachedLightNum);
        refreshAlertCard();
        alertUpdatedAt = cachedEdogUpdatedAt;
        mainHandler.removeCallbacks(alertClear);
        mainHandler.postDelayed(alertClear, ALERT_TTL_MS + 200L);
        syncAlertVisibility();
        if (AppPrefs.isAlertVisible(this)) {
            showAnyPanel();
        }
    }

    private void clearAlertDetails() {
        currentLimitSpeed = -1;
        currentCameraType = -1;
        currentAlertSummary = "";
        clearEdogCache();
        if (alertText != null) {
            alertText.setVisibility(View.GONE);
            alertText.setText("");
        }
        if (clusterAlertText != null) {
            clusterAlertText.setVisibility(View.GONE);
            clusterAlertText.setText("");
        }
        clearEdogAlertRow(alertRow);
        clearEdogAlertRow(clusterAlertRow);
        clearEdogAlertRow(cardCruiseEdogRow);
        clearEdogAlertRow(cardNavEdogRow);
        clearEdogAlertRow(clusterCardCruiseEdogRow);
        clearEdogAlertRow(clusterCardNavEdogRow);
        clearCompactWidgetRow();
        refreshAlertCard();
        syncAlertVisibility();
        mainHandler.removeCallbacks(alertClear);
    }

    private boolean hasFreshEdogAlert() {
        if (cachedEdogUpdatedAt <= 0
                || System.currentTimeMillis() - cachedEdogUpdatedAt > ALERT_TTL_MS + 200L) {
            return false;
        }
        return cachedEdogSpeed > 0
                || (cachedCameraIndex != -1 && cachedCameraDist >= 0)
                || cachedLightNum > 0;
    }

    private void clearEdogCache() {
        cachedEdogSpeed = -1;
        cachedCameraIndex = -1;
        cachedCameraDist = -1;
        cachedCameraType = -1;
        cachedLightNum = -1;
        cachedEdogUpdatedAt = 0L;
    }

    private void populateEdogAlertRow(LinearLayout row, int speed, int cameraIndex,
                                      int cameraDist, int cameraType, int lightNum) {
        if (row == null) {
            return;
        }
        boolean anyVisible = false;
        View speedBox = row.findViewWithTag("speed_box");
        if (speedBox instanceof FrameLayout) {
            FrameLayout frame = (FrameLayout) speedBox;
            if (speed > 0) {
                speedBox.setVisibility(View.VISIBLE);
                if (frame.getChildCount() > 1 && frame.getChildAt(1) instanceof TextView) {
                    String txt = String.valueOf(speed);
                    TextView tv = (TextView) frame.getChildAt(1);
                    tv.setText(txt);
                }
                anyVisible = true;
            } else {
                speedBox.setVisibility(View.GONE);
            }
        }

        View cameraBox = row.findViewWithTag("camera_box");
        if (cameraBox instanceof LinearLayout) {
            boolean hasCamera = cameraIndex != -1 && cameraDist >= 0;
            if (hasCamera) {
                cameraBox.setVisibility(View.VISIBLE);
                LinearLayout box = (LinearLayout) cameraBox;
                // First child is a FrameLayout (tag="camera_icon_frame") with icon + speed overlay
                View iconFrame = box.findViewWithTag("camera_icon_frame");
                if (iconFrame instanceof FrameLayout) {
                    FrameLayout frame = (FrameLayout) iconFrame;
                    boolean speedCamera = isSpeedCameraType(cameraType) && speed > 0;
                    if (frame.getChildCount() > 0 && frame.getChildAt(0) instanceof ImageView) {
                        applyEdogIcon((ImageView) frame.getChildAt(0), cameraType, speedCamera);
                    }
                    if (frame.getChildCount() > 1 && frame.getChildAt(1) instanceof TextView) {
                        TextView limit = (TextView) frame.getChildAt(1);
                        if (speedCamera && speed > 0) {
                            limit.setText(String.valueOf(speed));
                            limit.setVisibility(View.VISIBLE);
                        } else {
                            limit.setText("");
                            limit.setVisibility(View.GONE);
                        }
                    }
                } else if (box.getChildCount() > 0 && box.getChildAt(0) instanceof ImageView) {
                    // Old-style direct ImageView (backward compat)
                    applyEdogIcon((ImageView) box.getChildAt(0), cameraType, false);
                }
                if (box.getChildCount() > 1 && box.getChildAt(1) instanceof TextView) {
                    ((TextView) box.getChildAt(1)).setText(formatDistance(cameraDist));
                }
                anyVisible = true;
            } else {
                cameraBox.setVisibility(View.GONE);
            }
        }

        View lightBox = row.findViewWithTag("light_box");
        if (lightBox instanceof LinearLayout) {
            if (lightNum > 0) {
                lightBox.setVisibility(View.VISIBLE);
                LinearLayout box = (LinearLayout) lightBox;
                if (box.getChildCount() > 1 && box.getChildAt(1) instanceof TextView) {
                    ((TextView) box.getChildAt(1)).setText(lightNum + "\u4e2a");
                }
                anyVisible = true;
            } else {
                lightBox.setVisibility(View.GONE);
            }
        }
        row.setVisibility(anyVisible ? View.VISIBLE : View.GONE);
    }

    private void clearEdogAlertRow(LinearLayout row) {
        if (row == null) {
            return;
        }
        row.setVisibility(View.GONE);
        View speedBox = row.findViewWithTag("speed_box");
        if (speedBox != null) speedBox.setVisibility(View.GONE);
        View cameraBox = row.findViewWithTag("camera_box");
        if (cameraBox != null) cameraBox.setVisibility(View.GONE);
        View lightBox = row.findViewWithTag("light_box");
        if (lightBox != null) lightBox.setVisibility(View.GONE);
    }

    private void updateStatusDetails(Bundle extras) {
        boolean hasCompactHeadingTarget = compactCruiseDirText != null || clusterCompactCruiseDirText != null;
        boolean showStatusDetails = shouldShowStandbyStatusDetails();
        int roadType = intValue(extras, "ROAD_TYPE", -1);
        if (roadType >= 0) {
            currentRoadType = roadType;
        }
        if (showStatusDetails && roadType >= 0) {
            currentRoadTypeSummary = roadTypeName(roadType);
        } else if (!showStatusDetails || roadType >= 0) {
            currentRoadTypeSummary = "";
        }
        if (detailText == null && !hasCompactHeadingTarget) {
            return;
        }
        ArrayList<String> lines = new ArrayList<>();

        String locationJson = valueString(extras, "EXTRA_LOCATION_INFO");
        if (!TextUtils.isEmpty(locationJson)) {
            String parsed = locationSummary(locationJson);
            if (!TextUtils.isEmpty(parsed)) {
                lines.add(parsed);
            }
        }

        int direction = intValue(extras, "CAR_DIRECTION", -1);
        if (direction < 0 && !TextUtils.isEmpty(locationJson)) {
            try {
                direction = new JSONObject(locationJson).optInt("bearing", -1);
            } catch (Throwable ignored) {}
        }
        double lat = doubleValue(extras, "CAR_LATITUDE",
                doubleValue(extras, "LAT", doubleValue(extras, "LATITUDE", Double.NaN)));
        double lon = doubleValue(extras, "CAR_LONGITUDE",
                doubleValue(extras, "LON", doubleValue(extras, "LONGITUDE", Double.NaN)));
        if (showStatusDetails && (direction >= 0 || (!Double.isNaN(lat) && !Double.isNaN(lon) && !(lat == 0.0d && lon == 0.0d)))) {
            StringBuilder car = new StringBuilder();
            if (direction >= 0) {
                car.append("\u8f66\u5934 ").append(bearingToCompass(direction));
            }
            if (!TextUtils.isEmpty(currentRoadTypeSummary)) {
                if (car.length() > 0) {
                    car.append(" \u00b7 ");
                }
                car.append(currentRoadTypeSummary);
            }
            if (!Double.isNaN(lat) && !Double.isNaN(lon) && !(lat == 0.0d && lon == 0.0d)) {
                if (car.length() > 0) {
                    car.append("  ");
                }
                car.append(String.format(java.util.Locale.US, "%.5f, %.5f", lat, lon));
            }
            lines.add(car.toString());
        }
        if (direction >= 0) {
            currentHeadingSummary = bearingToCompass(direction);
        }
        if (isAnyDynamicIslandUiEnabled()) {
            updateDynamicIslandCruiseDirectionText(compactCruiseDirText);
            updateDynamicIslandCruiseDirectionText(clusterCompactCruiseDirText);
        } else {
            updateCompactCruiseDirectionText(compactCruiseDirText);
            updateCompactCruiseDirectionText(clusterCompactCruiseDirText);
        }

        String province = valueString(extras, "PROVINCE_NAME", "provinceName");
        String city = valueString(extras, "CITY_NAME", "cityName");
        String district = valueString(extras, "DISTRICT_NAME", "districtName");
        String areaCode = valueString(extras, "AREA_CODE", "areaCode");
        if (showStatusDetails && (!TextUtils.isEmpty(province) || !TextUtils.isEmpty(city) || !TextUtils.isEmpty(district))) {
            StringBuilder admin = new StringBuilder("\u884c\u653f\u533a ");
            if (!TextUtils.isEmpty(province)) {
                admin.append(province).append(' ');
            }
            if (!TextUtils.isEmpty(city)) {
                admin.append(city).append(' ');
            }
            if (!TextUtils.isEmpty(district)) {
                admin.append(district);
            }
            if (!TextUtils.isEmpty(areaCode)) {
                admin.append(" ").append(areaCode);
            }
            lines.add(admin.toString().trim());
        }

        String traffic = valueString(extras, "EXTRA_LOCATION_TRAFFIC_INFO",
                "EXTRA_TRAFFIC_CONDITION_RESULT_MESSAGE");
        if (showStatusDetails && !TextUtils.isEmpty(traffic)) {
            lines.add("\u524d\u65b9\u8def\u51b5 " + traffic);
        }

        if (showStatusDetails && (extras.containsKey("EXTRA_MUTE") || extras.containsKey("EXTRA_CASUAL_MUTE"))) {
            boolean mute = intValue(extras, "EXTRA_MUTE", 0) == 1;
            boolean casual = intValue(extras, "EXTRA_CASUAL_MUTE", 0) == 1;
            lines.add("\u64ad\u62a5 " + (mute ? "\u9759\u97f3" : "\u6709\u58f0")
                    + (casual ? " \u00b7 \u4e34\u65f6\u9759\u97f3" : ""));
        }

        if (showStatusDetails && (extras.containsKey("EXTRA_HOME_OR_COMPANY_WHAT")
                || extras.containsKey("EXTRA_HOME_OR_COMPANY_ETA"))) {
            boolean home = booleanValue(extras, "EXTRA_HOME_OR_COMPANY_WHAT", false);
            String eta = valueString(extras, "EXTRA_HOME_OR_COMPANY_ETA");
            lines.add((home ? "\u56de\u5bb6" : "\u53bb\u516c\u53f8")
                    + (TextUtils.isEmpty(eta) ? "" : " " + eta));
        }

        String favorite = valueString(extras, "EXTRA_FAVORITE_MY_LOCATION");
        if (showStatusDetails && !TextUtils.isEmpty(favorite)) {
            lines.add("\u6536\u85cf\u5f53\u524d\u70b9\u5df2\u8fd4\u56de");
        }

        refreshStatusSummary();

        currentDetailSummary = lines.isEmpty() ? "" : join(lines, "\n");
        if (lines.isEmpty() || detailText == null) {
            return;
        }
        detailText.setText(currentDetailSummary);
        if (clusterDetailText != null) {
            clusterDetailText.setText(detailText.getText());
        }
        syncDetailVisibility();
        if (AppPrefs.isDetailVisible(this)) {
            showAnyPanel();
        }
    }

    private void updateLaneFromExtras(Bundle extras) {
        if (laneBar == null) {
            return;
        }

        LaneInfoParser.LaneInfo laneInfo = LaneInfoParser.parse(extras);
        if (!laneInfo.isHandled()) {
            return;
        }
        if (laneInfo.shouldClear()) {
            hideLaneData();
            return;
        }
        if (laneInfo.hasLaneData()) {
            showLaneData(laneInfo.lanes, laneInfo.advised);
        }
    }

    private void showLaneData(int[] lanes, boolean[] advised) {
        cacheLaneData(lanes, advised);
        if (laneBar == null && clusterLaneBar == null) {
            return;
        }
        applyCachedLaneData();
        syncLaneVisibility();
        if (AppPrefs.isLaneVisible(this)) {
            showAnyPanel();
        }
    }

    private void hideLaneData() {
        lastLaneData = null;
        lastLaneAdvised = null;
        if (laneBar != null) {
            laneBar.hideLane();
        }
        if (clusterLaneBar != null) {
            clusterLaneBar.hideLane();
        }
        if (laneSection != null) {
            laneSection.setVisibility(View.GONE);
        }
        if (clusterLaneSection != null) {
            clusterLaneSection.setVisibility(View.GONE);
        }
    }

    private void cacheLaneData(int[] lanes, boolean[] advised) {
        if (lanes == null || lanes.length == 0) {
            lastLaneData = null;
            lastLaneAdvised = null;
            return;
        }
        lastLaneData = Arrays.copyOf(lanes, lanes.length);
        lastLaneAdvised = advised == null ? null : Arrays.copyOf(advised, advised.length);
    }

    private void applyCachedLaneData() {
        if (lastLaneData == null || lastLaneData.length == 0) {
            return;
        }
        if (laneBar != null) {
            laneBar.setLaneData(lastLaneData, lastLaneAdvised);
        }
        if (clusterLaneBar != null) {
            clusterLaneBar.setLaneData(lastLaneData, lastLaneAdvised);
        }
    }

    private void updateTmcData(String tmcJson) {
        if (TextUtils.isEmpty(tmcJson)) {
            return;
        }
        cachedTmcJson = tmcJson;
        tmcUpdatedAt = System.currentTimeMillis();
        if (!AppPrefs.isTmcBarVisible(this)) {
            clearTmcDrawablesOnly();
            return;
        }
        if (mainTmcProgressBar != null) {
            mainTmcProgressBar.updateTmcData(tmcJson);
        }
        if (clusterTmcProgressBar != null) {
            clusterTmcProgressBar.updateTmcData(tmcJson);
        }
        mainHandler.removeCallbacks(tmcClear);
        mainHandler.postDelayed(tmcClear, TMC_TTL_MS + 500L);
    }

    private void clearTmcData() {
        cachedTmcJson = "";
        tmcUpdatedAt = 0L;
        if (mainTmcProgressBar != null) {
            mainTmcProgressBar.clear();
        }
        if (clusterTmcProgressBar != null) {
            clusterTmcProgressBar.clear();
        }
        mainHandler.removeCallbacks(tmcClear);
    }

    private void clearTmcDrawablesOnly() {
        if (mainTmcProgressBar != null) {
            mainTmcProgressBar.clear();
        }
        if (clusterTmcProgressBar != null) {
            clusterTmcProgressBar.clear();
        }
        mainHandler.removeCallbacks(tmcClear);
    }

    private void requestLaneInfo() {
        try {
            Intent intent = new Intent(ACTION_RECV);
            intent.setPackage(AppPrefs.getTargetPackage(this));
            intent.putExtra("KEY_TYPE", 10062);
            sendBroadcast(intent);
            Log.d(TAG, "request lane info KEY_TYPE=10062");
        } catch (Throwable t) {
            Log.e(TAG, "request lane info failed", t);
        }
    }

    private void requestTrafficLightInfo() {
        try {
            Intent intent = new Intent(ACTION_RECV);
            intent.setPackage(AppPrefs.getTargetPackage(this));
            intent.putExtra("KEY_TYPE", AmapConstants.KEY_TYPE_TRAFFIC_LIGHT);
            sendBroadcast(intent);
            Log.d(TAG, "request traffic light info KEY_TYPE=" + AmapConstants.KEY_TYPE_TRAFFIC_LIGHT);
        } catch (Throwable t) {
            Log.e(TAG, "request traffic light info failed", t);
        }
    }

    private void requestTmcInfo() {
        try {
            Intent intent = new Intent(ACTION_RECV);
            intent.setPackage(AppPrefs.getTargetPackage(this));
            intent.putExtra("KEY_TYPE", 13011);
            sendBroadcast(intent);
            Log.d(TAG, "request tmc info KEY_TYPE=13011");
        } catch (Throwable t) {
            Log.e(TAG, "request tmc info failed", t);
        }
    }

    private void requestExitInfo() {
        try {
            Intent intent = new Intent(ACTION_RECV);
            intent.setPackage(AppPrefs.getTargetPackage(this));
            intent.putExtra("KEY_TYPE", 12011);
            intent.putExtra("EXIT_INFO_TYPE", 1);
            sendBroadcast(intent);
            Log.d(TAG, "request exit info KEY_TYPE=12011");
        } catch (Throwable t) {
            Log.e(TAG, "request exit info failed", t);
        }
    }

    private boolean hasAny(Bundle extras, String... keys) {
        for (String key : keys) {
            if (extras.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private Object safeExtra(Bundle extras, String key) {
        try {
            return extras.get(key);
        } catch (Throwable t) {
            Log.d(TAG, "skip unreadable extra " + key, t);
            return null;
        }
    }

    private int intValue(Bundle extras, String key, int fallback) {
        Object value = safeExtra(extras, key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private int[] intArrayValue(Bundle extras, String... keys) {
        for (String key : keys) {
            Object value = safeExtra(extras, key);
            int[] parsed = parseIntArray(value);
            if (parsed != null && parsed.length > 0) {
                return parsed;
            }
        }
        return null;
    }

    private int lengthOf(int[] values) {
        return values == null ? 0 : values.length;
    }

    private int valueAt(int[] values, int index, int fallback) {
        if (values == null || values.length == 0) {
            return fallback;
        }
        if (index < values.length) {
            return values[index];
        }
        return values[values.length - 1];
    }

    private boolean[] booleanArrayValue(Bundle extras, String... keys) {
        for (String key : keys) {
            Object value = safeExtra(extras, key);
            boolean[] parsed = parseBooleanArray(value);
            if (parsed != null && parsed.length > 0) {
                return parsed;
            }
        }
        return null;
    }

    private int[] parseIntArray(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof int[]) {
            return (int[]) value;
        }
        if (value instanceof Integer) {
            return new int[]{(Integer) value};
        }
        Class<?> cls = value.getClass();
        if (cls.isArray()) {
            int length = Array.getLength(value);
            int[] out = new int[length];
            for (int i = 0; i < length; i++) {
                Object item = Array.get(value, i);
                out[i] = item instanceof Number ? ((Number) item).intValue() : parseInt(String.valueOf(item), 1);
            }
            return out;
        }
        String s = String.valueOf(value).replace('[', ' ').replace(']', ' ').trim();
        if (TextUtils.isEmpty(s)) {
            return null;
        }
        String[] parts = s.split("[,;| ]+");
        int[] out = new int[parts.length];
        int count = 0;
        for (String part : parts) {
            if (!TextUtils.isEmpty(part)) {
                out[count++] = parseInt(part, 1);
            }
        }
        if (count == 0) {
            return null;
        }
        int[] compact = new int[count];
        System.arraycopy(out, 0, compact, 0, count);
        return compact;
    }

    private boolean[] parseBooleanArray(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof boolean[]) {
            return (boolean[]) value;
        }
        if (value instanceof Boolean) {
            return new boolean[]{(Boolean) value};
        }
        Class<?> cls = value.getClass();
        if (cls.isArray()) {
            int length = Array.getLength(value);
            boolean[] out = new boolean[length];
            for (int i = 0; i < length; i++) {
                out[i] = parseBoolean(Array.get(value, i));
            }
            return out;
        }
        String s = String.valueOf(value).replace('[', ' ').replace(']', ' ').trim();
        if (TextUtils.isEmpty(s)) {
            return null;
        }
        String[] parts = s.split("[,;| ]+");
        boolean[] out = new boolean[parts.length];
        int count = 0;
        for (String part : parts) {
            if (!TextUtils.isEmpty(part)) {
                out[count++] = parseBoolean(part);
            }
        }
        if (count == 0) {
            return null;
        }
        boolean[] compact = new boolean[count];
        System.arraycopy(out, 0, compact, 0, count);
        return compact;
    }

    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String s = String.valueOf(value);
        return "1".equals(s) || "true".equalsIgnoreCase(s) || "\u662f".equals(s);
    }

    private boolean booleanValue(Bundle extras, String key, boolean fallback) {
        Object value = safeExtra(extras, key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String s = String.valueOf(value);
        if ("1".equals(s) || "true".equalsIgnoreCase(s) || "\u662f".equals(s)) {
            return true;
        }
        if ("0".equals(s) || "false".equalsIgnoreCase(s) || "\u5426".equals(s)) {
            return false;
        }
        return fallback;
    }

    private double doubleValue(Bundle extras, String key, double fallback) {
        Object value = safeExtra(extras, key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private String valueString(Bundle extras, String... keys) {
        for (String key : keys) {
            Object value = safeExtra(extras, key);
            if (value == null) {
                continue;
            }
            String s = String.valueOf(value);
            if (!TextUtils.isEmpty(s) && !"0".equals(s) && !"null".equals(s)) {
                return s;
            }
        }
        return null;
    }

    // --- Dynamic island mode helpers ---


    private void updateCardLayout() {
        if (!AppPrefs.isCardUiEnabled(this)) return;
        boolean isNav = !inCruiseMode && currentTurnIcon > 0;
        boolean isCruise = inCruiseMode;
        boolean isEmpty = !isNav && !isCruise;

        // Toggle main panel containers
        if (cardCruiseRow1 != null) cardCruiseRow1.setVisibility(isCruise ? View.VISIBLE : View.GONE);
        if (cardCruiseRow2 != null) cardCruiseRow2.setVisibility(isCruise ? View.VISIBLE : View.GONE);
        if (cardNavArea != null) cardNavArea.setVisibility(isNav ? View.VISIBLE : View.GONE);
        if (modeText != null) {
            ViewGroup.LayoutParams lp = modeText.getLayoutParams();
            lp.width = isEmpty ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
            lp.height = isEmpty ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
            modeText.setLayoutParams(lp);
            modeText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (TextUtils.isEmpty(modeText.getText())) {
                modeText.setText("待接收导航/巡航信息");
            }
        }

        // Swap shared field references based on active mode
        if (isCruise) {
            if (cardCruiseLaneSection != null) laneSection = cardCruiseLaneSection;
            if (cardCruiseLaneBar != null) laneBar = cardCruiseLaneBar;
            if (cardCruiseLightRow != null) lightRow = cardCruiseLightRow;
            if (cardCruiseEdogRow != null) alertRow = cardCruiseEdogRow;
            // Update cruise direction and road text
            if (compactCruiseRoadText != null) {
                String road = currentRoadName;
                if (!TextUtils.isEmpty(road) && !TextUtils.equals(compactCruiseRoadText.getText(), road)) {
                    compactCruiseRoadText.setText(road);
                }
            }
            if (compactCruiseDirText != null) {
                updateCompactCruiseDirectionText(compactCruiseDirText);
            }
        } else {
            if (cardNavLaneSection != null) laneSection = cardNavLaneSection;
            if (cardNavLaneBar != null) laneBar = cardNavLaneBar;
            if (cardNavLightRow != null) lightRow = cardNavLightRow;
            if (cardNavEdogRow != null) alertRow = cardNavEdogRow;
        }

        // Handle empty state: show the same standby text used by the classic UI.
        if (panel != null) {
            if (isEmpty) {
                int normalPadH = scaledDp(10, overlayScale);
                int normalPadV = scaledDp(8, overlayScale);
                panel.setPadding(normalPadH, normalPadV, normalPadH, normalPadV);
                panel.setMinimumWidth(0);
                panel.setMinimumHeight(0);
                mainPanelHeldMinWidth = 0;
                mainPanelHeldMinHeight = 0;
                panel.requestLayout();
                updateMainPanelLayoutIfAttached();
            } else {
                int normalPadH = scaledDp(5, overlayScale);
                int normalPadTop = scaledDp(4, overlayScale);
                int normalPadBottom = scaledDp(2, overlayScale);
                panel.setPadding(normalPadH, normalPadTop, normalPadH, normalPadBottom);
                if (isCruise) {
                    // Height: full=76dp (4+32+3+35+2), single-row=38dp (4+32+2)
                    boolean row2HasContent = (cardCruiseLaneBar != null && cardCruiseLaneBar.getVisibility() == View.VISIBLE)
                            || (cardCruiseLightRow != null && cardCruiseLightRow.getChildCount() > 0);
                    if (cardCruiseRow2 != null) {
                        cardCruiseRow2.setVisibility(row2HasContent ? View.VISIBLE : View.GONE);
                    }
                    int minH = row2HasContent ? scaledDp(76, overlayScale) : scaledDp(38, overlayScale);
                    panel.setMinimumHeight(minH);
                    // Sync held minimum to allow immediate shrink when mode changes
                    mainPanelHeldMinHeight = Math.min(mainPanelHeldMinHeight, minH);
                    panel.setVisibility(View.VISIBLE);
                    panel.requestLayout();
                    updateMainPanelLayoutIfAttached();
                } else {
                    // Nav mode: fixed 76dp height matching cruise
                    int minH = scaledDp(76, overlayScale);
                    panel.setMinimumHeight(minH);
                    mainPanelHeldMinHeight = Math.min(mainPanelHeldMinHeight, minH);
                    panel.setVisibility(View.VISIBLE);
                    panel.requestLayout();
                    updateMainPanelLayoutIfAttached();
                }
            }
        }

        // Toggle cluster containers
        if (clusterCardCruiseRow1 != null) clusterCardCruiseRow1.setVisibility(isCruise ? View.VISIBLE : View.GONE);
        if (clusterCardCruiseRow2 != null) clusterCardCruiseRow2.setVisibility(isCruise ? View.VISIBLE : View.GONE);
        if (clusterCardNavArea != null) clusterCardNavArea.setVisibility(isNav ? View.VISIBLE : View.GONE);
        if (clusterModeText != null) {
            ViewGroup.LayoutParams lp = clusterModeText.getLayoutParams();
            lp.width = isEmpty ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
            lp.height = isEmpty ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
            clusterModeText.setLayoutParams(lp);
            clusterModeText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (TextUtils.isEmpty(clusterModeText.getText())) {
                clusterModeText.setText("待接收导航/巡航信息");
            }
        }

        // Swap cluster shared field references
        if (isCruise) {
            if (clusterCardCruiseLaneSection != null) clusterLaneSection = clusterCardCruiseLaneSection;
            if (clusterCardCruiseLaneBar != null) clusterLaneBar = clusterCardCruiseLaneBar;
            if (clusterCardCruiseLightRow != null) clusterLightRow = clusterCardCruiseLightRow;
            if (clusterCardCruiseEdogRow != null) clusterAlertRow = clusterCardCruiseEdogRow;
            // Update cluster cruise text to match main
            if (clusterCompactCruiseRoadText != null) {
                String road = currentRoadName;
                if (!TextUtils.isEmpty(road) && !TextUtils.equals(clusterCompactCruiseRoadText.getText(), road)) {
                    clusterCompactCruiseRoadText.setText(road);
                }
            }
            if (clusterCompactCruiseDirText != null) {
                updateCompactCruiseDirectionText(clusterCompactCruiseDirText);
            }
        } else {
            if (clusterCardNavLaneSection != null) clusterLaneSection = clusterCardNavLaneSection;
            if (clusterCardNavLaneBar != null) clusterLaneBar = clusterCardNavLaneBar;
            if (clusterCardNavLightRow != null) clusterLightRow = clusterCardNavLightRow;
            if (clusterCardNavEdogRow != null) clusterAlertRow = clusterCardNavEdogRow;
            // Update cluster nav turn text to match main
            if (clusterCompactNavTurnRoadText != null && compactNavTurnRoadText != null) {
                String navRoad = compactNavTurnRoadText.getText().toString();
                updateCompactMarqueeText(clusterCompactNavTurnRoadText, navRoad);
            }
            if (clusterNavTurnDistText != null && navTurnDistText != null) {
                String dist = navTurnDistText.getText().toString();
                if (!TextUtils.equals(clusterNavTurnDistText.getText(), dist)) {
                    clusterNavTurnDistText.setText(dist);
                }
            }
        }

        // Handle cluster empty state
        if (clusterPanel != null) {
            if (isEmpty) {
                float cs = clusterScale > 0 ? clusterScale : overlayScale;
                int normalPadH = scaledDp(10, cs);
                int normalPadV = scaledDp(8, cs);
                clusterPanel.setPadding(normalPadH, normalPadV, normalPadH, normalPadV);
                clusterPanel.setMinimumWidth(0);
                clusterPanel.setMinimumHeight(0);
                clusterPanelHeldMinWidth = 0;
                clusterPanelHeldMinHeight = 0;
                clusterPanel.requestLayout();
                updateClusterPanelLayoutIfAttached();
            } else {
                int normalPadH = scaledDp(5, clusterScale > 0 ? clusterScale : overlayScale);
                int normalPadTop = scaledDp(4, clusterScale > 0 ? clusterScale : overlayScale);
                int normalPadBottom = scaledDp(2, clusterScale > 0 ? clusterScale : overlayScale);
                clusterPanel.setPadding(normalPadH, normalPadTop, normalPadH, normalPadBottom);
                if (isCruise) {
                    float cs = clusterScale > 0 ? clusterScale : overlayScale;
                    boolean cRow2HasContent = (clusterCardCruiseLaneBar != null && clusterCardCruiseLaneBar.getVisibility() == View.VISIBLE)
                            || (clusterCardCruiseLightRow != null && clusterCardCruiseLightRow.getChildCount() > 0);
                    if (clusterCardCruiseRow2 != null) {
                        clusterCardCruiseRow2.setVisibility(cRow2HasContent ? View.VISIBLE : View.GONE);
                    }
                    int minH = cRow2HasContent ? scaledDp(76, cs) : scaledDp(38, cs);
                    clusterPanel.setMinimumHeight(minH);
                    clusterPanelHeldMinHeight = Math.min(clusterPanelHeldMinHeight, minH);
                    clusterPanel.requestLayout();
                    updateClusterPanelLayoutIfAttached();
                } else {
                    float cs = clusterScale > 0 ? clusterScale : overlayScale;
                    int minH = scaledDp(76, cs);
                    clusterPanel.setMinimumHeight(minH);
                    clusterPanelHeldMinHeight = Math.min(clusterPanelHeldMinHeight, minH);
                    clusterPanel.requestLayout();
                    updateClusterPanelLayoutIfAttached();
                }
            }
        }
    }

    private void updateMainPanelLayoutIfAttached() {
        if (windowManager == null || panel == null || params == null || panel.getParent() == null) {
            return;
        }
        try {
            windowManager.updateViewLayout(panel, params);
        } catch (Throwable t) {
            Log.e(TAG, "card main layout update failed", t);
        }
    }

    private void updateClusterPanelLayoutIfAttached() {
        if (clusterWindowManager == null || clusterPanel == null || clusterParams == null || clusterPanel.getParent() == null) {
            return;
        }
        try {
            clusterWindowManager.updateViewLayout(clusterPanel, clusterParams);
        } catch (Throwable t) {
            Log.e(TAG, "card cluster layout update failed", t);
        }
    }

    // --- Overspeed warning (all UI styles) ---

    private void updateOverspeedWarning() {
        int v = currentVehicleSpeed;
        int l = currentLimitSpeed;
        if (l <= 0 || v < 0 || v <= l) {
            if (overspeedLevel != OVERSPEED_NONE) {
                stopOverspeedBlink();
            }
            return;
        }
        float ratio = (float) v / l;
        int level;
        if (ratio >= 1.20f) {
            level = OVERSPEED_HIGH;
        } else if (ratio >= 1.10f) {
            level = AppPrefs.isOverspeedMediumWarningEnabled(this) ? OVERSPEED_MEDIUM : OVERSPEED_NONE;
        } else {
            level = AppPrefs.isOverspeedMildWarningEnabled(this) ? OVERSPEED_MILD : OVERSPEED_NONE;
        }
        if (level == OVERSPEED_NONE) {
            if (overspeedLevel != OVERSPEED_NONE) {
                stopOverspeedBlink();
            }
            return;
        }
        int color = overspeedColorForLevel(level);
        // Already running at same level — no change needed
        if (overspeedBlinks != null && overspeedLevel == level && overspeedColor == color) {
            return;
        }
        // Level changed or first start — restart without restoring TMC in between
        if (overspeedBlinks != null) {
            mainHandler.removeCallbacks(overspeedBlinks);
            overspeedBlinks = null;
        }
        overspeedLevel = level;
        overspeedColor = color;
        overspeedBlinkPhase = true;
        overspeedPhaseStartedAt = System.currentTimeMillis();
        overspeedBlinkOn = true;
        applyOverspeedBorder();
        overspeedBlinks = new Runnable() {
            @Override
            public void run() {
                tickOverspeedWarning();
                mainHandler.postDelayed(this, 250);
            }
        };
        mainHandler.postDelayed(overspeedBlinks, 250);
    }

    private int overspeedColorForLevel(int level) {
        if (level == OVERSPEED_HIGH) {
            return 0xFFFF0000;
        }
        if (level == OVERSPEED_MEDIUM) {
            return 0xFFFFEB3B;
        }
        return 0xFF00C2A8;
    }

    private long overspeedRestMsForLevel(int level) {
        return level == OVERSPEED_MEDIUM ? OVERSPEED_MEDIUM_REST_MS : OVERSPEED_MILD_REST_MS;
    }

    private void startOverspeedBlink(int level, int color) {
        stopOverspeedBlink();
        overspeedLevel = level;
        overspeedColor = color;
        overspeedBlinkPhase = true;
        overspeedPhaseStartedAt = System.currentTimeMillis();
        overspeedBlinks = new Runnable() {
            @Override
            public void run() {
                tickOverspeedWarning();
                mainHandler.postDelayed(this, 250);
            }
        };
        overspeedBlinkOn = true;
        applyOverspeedBorder();
        mainHandler.postDelayed(overspeedBlinks, 250);
    }

    private void tickOverspeedWarning() {
        if (overspeedLevel == OVERSPEED_NONE || overspeedColor == 0) {
            stopOverspeedBlink();
            return;
        }
        long now = System.currentTimeMillis();
        if (overspeedLevel == OVERSPEED_HIGH) {
            overspeedBlinkOn = !overspeedBlinkOn;
            applyOverspeedBorder();
            return;
        }
        if (overspeedBlinkPhase) {
            if (now - overspeedPhaseStartedAt >= OVERSPEED_BLINK_MS) {
                overspeedBlinkPhase = false;
                overspeedPhaseStartedAt = now;
                overspeedBlinkOn = true;
            } else {
                overspeedBlinkOn = !overspeedBlinkOn;
            }
        } else {
            if (now - overspeedPhaseStartedAt >= overspeedRestMsForLevel(overspeedLevel)) {
                overspeedBlinkPhase = true;
                overspeedPhaseStartedAt = now;
                overspeedBlinkOn = true;
            } else {
                overspeedBlinkOn = true;
            }
        }
        applyOverspeedBorder();
    }

    private void stopOverspeedBlink() {
        if (overspeedBlinks != null) {
            mainHandler.removeCallbacks(overspeedBlinks);
            overspeedBlinks = null;
        }
        overspeedBlinkOn = false;
        overspeedBlinkPhase = false;
        overspeedColor = 0;
        overspeedLevel = OVERSPEED_NONE;
        overspeedPhaseStartedAt = 0L;
        restoreNormalBorder();
        // Overspeed fully ended — restore TMC progress bar
        setTmcBarVisible(true);
    }

    private void applyOverspeedBorder() {
        if (overspeedBlinkOn && overspeedColor != 0) {
            int strokeW = scaledDp(5, overlayScale);
            if (panelBackground != null) {
                panelBackground.setStroke(strokeW, overspeedColor);
                if (panel != null) panel.invalidate();
            }
            if (clusterPanelBackground != null && clusterPanel != null) {
                float cs = clusterScale > 0 ? clusterScale : overlayScale;
                clusterPanelBackground.setStroke(scaledDp(5, cs), overspeedColor);
                clusterPanel.invalidate();
            }
        } else {
            // Blink-off phase: hide the border visually but do NOT restore normal border
            // or show TMC bar — overspeed is still active
            int opacity = AppPrefs.getBackgroundOpacityPercent(this);
            int transparentStroke = withAlpha(0xFFFFFFFF, 0);
            if (panelBackground != null) {
                panelBackground.setStroke(scaledDp(5, overlayScale), transparentStroke);
                if (panel != null) panel.invalidate();
            }
            if (clusterPanelBackground != null && clusterPanel != null) {
                float cs = clusterScale > 0 ? clusterScale : overlayScale;
                clusterPanelBackground.setStroke(scaledDp(5, cs), transparentStroke);
                clusterPanel.invalidate();
            }
        }
        // TMC bar stays hidden for the entire overspeed session
        setTmcBarVisible(false);
    }

    // -- Turn blink (green arrow icon, distance-based) -----------
    private void startTurnBlink() {
        if (exitAlternatorActive) return; // exit alternation takes priority
        stopTurnBlink();
        if (currentTurnIcon <= 0 || currentTurnDistanceMeters <= 0 || currentTurnDistanceMeters >= 500) {
            turnBlinkOn = true;
            restoreTurnArrowAlpha();
            applyNavigationTextVisualState();
            return;
        }
        long intervalMs = 500L;
        turnBlink = new Runnable() {
            @Override public void run() {
                turnBlinkOn = !turnBlinkOn;
                applyTurnBlinkAlpha(turnBlinkOn ? 1f : 0.15f);
                mainHandler.postDelayed(this, intervalMs);
            }
        };
        turnBlinkOn = true;
        applyTurnBlinkAlpha(1f);
        mainHandler.postDelayed(turnBlink, intervalMs);
    }

    private boolean shouldSyncTurnTextWithArrow() {
        return currentTurnIcon > 0 && currentTurnDistanceMeters > 0 && currentTurnDistanceMeters < 200;
    }

    private boolean shouldPinFullModeTurnInfo() {
        return exitAlternatorActive || shouldSyncTurnTextWithArrow();
    }

    private void stopTurnBlink() {
        if (turnBlink != null) { mainHandler.removeCallbacks(turnBlink); turnBlink = null; }
        turnBlinkOn = true;
        restoreTurnArrowAlpha();
        applyNavigationTextVisualState();
    }

    private void restoreTurnArrowAlpha() {
        if (turnIconView != null) turnIconView.setAlpha(1f);
        if (clusterTurnIconView != null) clusterTurnIconView.setAlpha(1f);
        if (navTurnIconView != null) navTurnIconView.setAlpha(1f);
        if (clusterNavTurnIconView != null) clusterNavTurnIconView.setAlpha(1f);
        if (turnLeadIconView != null) turnLeadIconView.setAlpha(1f);
        if (clusterTurnLeadIconView != null) clusterTurnLeadIconView.setAlpha(1f);
    }

    private void applyTurnBlinkAlpha(float alpha) {
        if (turnIconView != null) turnIconView.setAlpha(alpha);
        if (clusterTurnIconView != null) clusterTurnIconView.setAlpha(alpha);
        if (navTurnIconView != null) navTurnIconView.setAlpha(alpha);
        if (clusterNavTurnIconView != null) clusterNavTurnIconView.setAlpha(alpha);
        if (turnLeadIconView != null) turnLeadIconView.setAlpha(alpha);
        if (clusterTurnLeadIconView != null) clusterTurnLeadIconView.setAlpha(alpha);
        applyNavigationTextVisualState();
    }

    private void applyNavigationTextVisualState() {
        if (exitAlternatorActive) {
            applyTurnTextBlinkAlpha(1f, exitShowNumber ? TURN_ARROW_GREEN : EXIT_LABEL_COLOR);
        } else if (shouldSyncTurnTextWithArrow()) {
            applyTurnTextBlinkAlpha(turnBlinkOn ? 1f : 0.15f, EXIT_LABEL_COLOR);
        } else {
            restoreNormalTurnTextState();
        }
    }

    private void restoreNormalTurnTextState() {
        int color = primaryTextColor();
        setBlinkTextState(turnText, 1f, color);
        setBlinkTextState(clusterTurnText, 1f, color);
        setBlinkTextState(turnDistanceText, 1f, color);
        setBlinkTextState(clusterTurnDistanceText, 1f, color);
        setBlinkTextState(navTurnDistText, 1f, color);
        setBlinkTextState(clusterNavTurnDistText, 1f, color);
        setBlinkTextState(compactNavTurnRoadText, 1f, color);
        setBlinkTextState(clusterCompactNavTurnRoadText, 1f, color);
    }

    private void applyTurnTextBlinkAlpha(float alpha, int color) {
        setBlinkTextState(turnText, alpha, color);
        setBlinkTextState(clusterTurnText, alpha, color);
        setBlinkTextState(turnDistanceText, alpha, color);
        setBlinkTextState(clusterTurnDistanceText, alpha, color);
        setBlinkTextState(navTurnDistText, alpha, color);
        setBlinkTextState(clusterNavTurnDistText, alpha, color);
        setBlinkTextState(compactNavTurnRoadText, alpha, color);
        setBlinkTextState(clusterCompactNavTurnRoadText, alpha, color);
    }

    private void applyExitAlternatorTextState(boolean showNumber) {
        applyTurnTextBlinkAlpha(1f, showNumber ? TURN_ARROW_GREEN : EXIT_LABEL_COLOR);
    }

    private void setBlinkTextState(TextView view, float alpha, int color) {
        if (view == null) {
            return;
        }
        if (view.getCurrentTextColor() != color) {
            view.setTextColor(color);
        }
        if (Math.abs(view.getAlpha() - alpha) > 0.01f) {
            view.setAlpha(alpha);
        }
    }

    // -- Exit-number overlay (wraps arrow ImageView in FrameLayout + TextView) --
    private TextView wrapArrowWithExitOverlay(ImageView arrow, float scale) {
        if (arrow == null) return null;
        ViewGroup parent = (ViewGroup) arrow.getParent();
        if (parent == null) return null;
        ViewGroup.LayoutParams originalLp = arrow.getLayoutParams();
        int idx = parent.indexOfChild(arrow);
        if (idx < 0) return null;
        parent.removeView(arrow);

        FrameLayout wrapper = new FrameLayout(parent.getContext());
        wrapper.setLayoutParams(originalLp);
        wrapper.setClipChildren(true);
        wrapper.setClipToPadding(true);

        FrameLayout.LayoutParams fillLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        arrow.setLayoutParams(fillLp);
        wrapper.addView(arrow);

        TextView exitText = new TextView(parent.getContext());
        exitText.setTextColor(EXIT_LABEL_COLOR);
        exitText.setTypeface(Typeface.DEFAULT_BOLD);
        exitText.setGravity(Gravity.CENTER);
        exitText.setIncludeFontPadding(false);
        exitText.setVisibility(View.GONE);
        exitText.setSingleLine(true);
        exitText.setEllipsize(null);
        exitText.setText(exitLabel);
        exitText.setTextSize(scaledSp(12f, scale));
        exitText.setBackground(null);
        exitText.setPadding(0, 0, 0, 0);
        FrameLayout.LayoutParams textLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        wrapper.addView(exitText, textLp);
        wrapper.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                fitExitTextToWrapper(exitText));

        parent.addView(wrapper, idx);
        fitExitTextToWrapper(exitText);
        applyPairImmediate(arrow, exitText, exitAlternatorActive && exitShowNumber);
        return exitText;
    }

    // -- Exit arrow/exit-number alternation ----------------------------------

    private static final long EXIT_SHOW_ARROW_MS = 2000L;
    private static final long EXIT_SHOW_NUMBER_MS = 2000L;
    private static final long EXIT_CROSSFADE_MS = 350L;
    private static final long EXIT_MARQUEE_GAP_MS = 450L;
    private boolean exitShowNumber;
    private int exitTransitionToken;

    // -- Exit toggle cycle (same pattern as fullModeAlternator) -----------

    /**
     * Start the toggle cycle. Like fullModeAlternator, uses a Runnable that
     * flips a boolean and re-posts itself. Arrow and exit number share the
     * same FrameLayout position; we crossfade with View.animate().alpha().
     */
    private void startExitToggleCycle() {
        if (!exitAlternatorActive) return;
        applyAllPairsImmediate(false);
        exitArrowTick = new Runnable() {
            @Override public void run() {
                if (!exitAlternatorActive) return;
                exitShowNumber = !exitShowNumber;
                applyArrowExitState(exitShowNumber);
                long delay = exitShowNumber ? exitNumberDisplayDelayMs() : EXIT_SHOW_ARROW_MS;
                mainHandler.postDelayed(this, delay);
            }
        };
        mainHandler.postDelayed(exitArrowTick, EXIT_SHOW_ARROW_MS);
    }

    private void applyAllPairsImmediate(boolean showNumber) {
        ++exitTransitionToken;
        applyPairImmediate(turnIconView, turnExitText, showNumber);
        applyPairImmediate(clusterTurnIconView, clusterTurnExitText, showNumber);
        applyPairImmediate(turnLeadIconView, turnLeadExitText, showNumber);
        applyPairImmediate(clusterTurnLeadIconView, clusterTurnLeadExitText, showNumber);
        applyPairImmediate(navTurnIconView, navExitText, showNumber);
        applyPairImmediate(clusterNavTurnIconView, clusterNavExitText, showNumber);
        applyExitAlternatorTextState(showNumber);
    }

    private void restoreExitAlternatorVisualState() {
        if (!exitAlternatorActive) {
            return;
        }
        applyAllPairsImmediate(exitShowNumber);
    }

    /**
     * Crossfade to arrow (showNumber=false) or exit number (showNumber=true).
     * Animates all 6 arrow+exit pairs simultaneously via ViewPropertyAnimator.
     */
    private void applyArrowExitState(boolean showNumber) {
        int token = ++exitTransitionToken;
        transitionPair(turnIconView, turnExitText, showNumber, token);
        transitionPair(clusterTurnIconView, clusterTurnExitText, showNumber, token);
        transitionPair(turnLeadIconView, turnLeadExitText, showNumber, token);
        transitionPair(clusterTurnLeadIconView, clusterTurnLeadExitText, showNumber, token);
        transitionPair(navTurnIconView, navExitText, showNumber, token);
        transitionPair(clusterNavTurnIconView, clusterNavExitText, showNumber, token);
        applyExitAlternatorTextState(showNumber);
    }

    private void transitionPair(ImageView iv, TextView tv, boolean showNumber, int token) {
        if (tv != null) {
            tv.setText(exitLabel);
            fitExitTextToWrapper(tv);
        }
        View to = showNumber ? tv : iv;
        cancelPairAnimation(iv, tv);
        hideViewForExitTransition(showNumber ? iv : tv);
        hideViewForExitTransition(to);
        mainHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (token != exitTransitionToken || !exitAlternatorActive) {
                    return;
                }
                showViewForExitTransition(to, token);
            }
        }, 40L);
    }

    private void showViewForExitTransition(View view, int token) {
        if (view == null || token != exitTransitionToken || !exitAlternatorActive) {
            return;
        }
        if (view instanceof TextView) {
            prepareExitTextMarqueeStart((TextView) view);
        }
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(1f).setDuration(EXIT_CROSSFADE_MS).withEndAction(new Runnable() {
            @Override public void run() {
                if (token == exitTransitionToken && exitAlternatorActive && view instanceof TextView) {
                    startExitTextMarquee((TextView) view);
                }
            }
        }).start();
    }

    private void hideViewForExitTransition(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setTranslationX(0f);
        view.setAlpha(0f);
        view.setVisibility(View.INVISIBLE);
    }

    private void cancelPairAnimation(ImageView iv, TextView tv) {
        if (iv != null) {
            iv.animate().cancel();
        }
        if (tv != null) {
            tv.animate().cancel();
        }
    }

    private int[] measurePanelContent(LinearLayout target, int baseMinWidth, int baseMinHeight) {
        int heldMinWidth = target.getMinimumWidth();
        int heldMinHeight = target.getMinimumHeight();
        try {
            // The stabilizer itself raises the root minimum size while content is visible.
            // Measure against the style's original minimum, otherwise the held size masks
            // every later content shrink and the delayed unlock can never observe it.
            target.setMinimumWidth(Math.max(0, baseMinWidth));
            target.setMinimumHeight(Math.max(0, baseMinHeight));
            int wSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            target.measure(wSpec, hSpec);
            return new int[]{
                    Math.max(0, target.getMeasuredWidth()),
                    Math.max(0, target.getMeasuredHeight())
            };
        } finally {
            target.setMinimumWidth(heldMinWidth);
            target.setMinimumHeight(heldMinHeight);
        }
    }

    private int panelShrinkThresholdPx(boolean cluster) {
        float scale = cluster && clusterScale > 0 ? clusterScale : overlayScale;
        return Math.max(1, scaledDp(2, scale));
    }

    private void applyPairImmediate(ImageView iv, TextView tv, boolean showNumber) {
        cancelPairAnimation(iv, tv);
        if (iv != null) {
            iv.setAlpha(showNumber ? 0f : 1f);
            iv.setVisibility(showNumber ? View.INVISIBLE : View.VISIBLE);
        }
        if (tv != null) {
            tv.setText(exitLabel);
            fitExitTextToWrapper(tv);
            tv.animate().cancel();
            tv.setAlpha(showNumber ? 1f : 0f);
            tv.setVisibility(showNumber ? View.VISIBLE : View.INVISIBLE);
            if (showNumber) {
                startExitTextMarquee(tv);
            } else if (!showNumber) {
                tv.setTranslationX(0f);
            }
        }
    }

    private void disableAncestorClipping(View view, int maxDepth) {
        View current = view;
        for (int i = 0; i < maxDepth && current instanceof ViewGroup; i++) {
            ViewGroup group = (ViewGroup) current;
            group.setClipChildren(false);
            group.setClipToPadding(false);
            if (!(group.getParent() instanceof View)) {
                return;
            }
            current = (View) group.getParent();
        }
    }

    private long exitNumberDisplayDelayMs() {
        long delay = EXIT_SHOW_NUMBER_MS;
        delay = Math.max(delay, exitTextMarqueeDurationMs(navExitText));
        delay = Math.max(delay, exitTextMarqueeDurationMs(clusterNavExitText));
        delay = Math.max(delay, exitTextMarqueeDurationMs(turnExitText));
        delay = Math.max(delay, exitTextMarqueeDurationMs(clusterTurnExitText));
        delay = Math.max(delay, exitTextMarqueeDurationMs(turnLeadExitText));
        delay = Math.max(delay, exitTextMarqueeDurationMs(clusterTurnLeadExitText));
        return delay + EXIT_MARQUEE_GAP_MS;
    }

    private long exitTextMarqueeDurationMs(TextView tv) {
        if (!needsExitTextMarquee(tv)) {
            return EXIT_SHOW_NUMBER_MS;
        }
        View parent = (View) tv.getParent();
        float distance = measuredExitTextWidth(tv) + parent.getWidth();
        return Math.max(2600L, Math.min(7000L, (long) (distance * 18f)));
    }

    private void startExitTextMarquee(TextView tv) {
        if (!needsExitTextMarquee(tv)) {
            tv.setTranslationX(0f);
            return;
        }
        float start = exitTextMarqueeStartX(tv);
        float end = exitTextMarqueeEndX(tv);
        tv.animate().cancel();
        tv.setTranslationX(start);
        tv.animate()
                .translationX(end)
                .setDuration(exitTextMarqueeDurationMs(tv))
                .start();
    }

    private void prepareExitTextMarqueeStart(TextView tv) {
        if (needsExitTextMarquee(tv)) {
            tv.setTranslationX(exitTextMarqueeStartX(tv));
        } else {
            tv.setTranslationX(0f);
        }
    }

    private float exitTextMarqueeStartX(TextView tv) {
        View parent = (View) tv.getParent();
        float textWidth = measuredExitTextWidth(tv);
        float parentWidth = parent.getWidth();
        float centeredLeft = (parentWidth - textWidth) * 0.5f;
        return parentWidth - centeredLeft;
    }

    private float exitTextMarqueeEndX(TextView tv) {
        View parent = (View) tv.getParent();
        float textWidth = measuredExitTextWidth(tv);
        float parentWidth = parent.getWidth();
        float centeredLeft = (parentWidth - textWidth) * 0.5f;
        return -textWidth - centeredLeft;
    }

    private boolean needsExitTextMarquee(TextView tv) {
        if (tv == null || !(tv.getParent() instanceof View) || TextUtils.isEmpty(tv.getText())) {
            return false;
        }
        View parent = (View) tv.getParent();
        return parent.getWidth() > 0 && measuredExitTextWidth(tv) > parent.getWidth() * 0.92f;
    }

    private float measuredExitTextWidth(TextView tv) {
        fitPaint.setTypeface(Typeface.DEFAULT_BOLD);
        fitPaint.setAntiAlias(true);
        fitPaint.setTextSize(tv.getTextSize());
        return fitPaint.measureText(String.valueOf(tv.getText()));
    }

    private void fitExitTextToWrapper(TextView tv) {
        if (tv == null || TextUtils.isEmpty(tv.getText())) {
            return;
        }
        View parent = tv.getParent() instanceof View ? (View) tv.getParent() : null;
        int width = parent != null ? parent.getWidth() : tv.getWidth();
        int height = parent != null ? parent.getHeight() : tv.getHeight();
        if (height <= 0 && parent != null && parent.getLayoutParams() != null) {
            height = parent.getLayoutParams().height;
        }
        if (height <= 0) {
            return;
        }
        String text = String.valueOf(tv.getText());
        float maxHeight = Math.max(1f, height * 0.58f);
        float lo = Math.max(3f, height * 0.08f);
        float hi = Math.max(lo, maxHeight);
        fitPaint.setTypeface(Typeface.DEFAULT_BOLD);
        fitPaint.setAntiAlias(true);
        float maxStillWidth = width > 0 ? width * 0.9f : Float.MAX_VALUE;
        for (int i = 0; i < 14; i++) {
            float mid = (lo + hi) * 0.5f;
            fitPaint.setTextSize(mid);
            Paint.FontMetrics fm = fitPaint.getFontMetrics();
            float textHeight = fm.descent - fm.ascent;
            float textWidth = fitPaint.measureText(text);
            if (textHeight <= maxHeight && (textWidth <= maxStillWidth || mid <= height * 0.34f)) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, lo);
        tv.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    }

    private void ensureExitAlternator() {
        // 0=高速 1=国道 2=省道 6=城市快速路 — all may have numbered exits
        boolean onHighwayOrExpressway = currentRoadType == 0 || currentRoadType == 1
                || currentRoadType == 2 || currentRoadType == 6 || currentRoadType == 7;
        boolean shouldAlt = exitResultState == 1 && !TextUtils.isEmpty(exitLabel)
                && !inCruiseMode && currentTurnIcon > 0
                && (onHighwayOrExpressway || routeGuidanceExitSupported);
        if (shouldAlt) {
            // Cancel any pending deferred stop
            if (exitStopPending != null) {
                mainHandler.removeCallbacks(exitStopPending);
                exitStopPending = null;
            }
            if (!exitAlternatorActive) {
                exitAlternatorActive = true;
                stopTurnBlink();
                exitShowNumber = false;
                startExitToggleCycle();
            }
        } else {
            // Deferred stop: wait 5s before stopping to avoid flapping
            if (exitStopPending == null && exitAlternatorActive) {
                exitStopPending = new Runnable() {
                    @Override public void run() {
                        stopExitAlternator();
                        exitStopPending = null;
                    }
                };
                mainHandler.postDelayed(exitStopPending, 5000L);
            }
        }
    }

    private void stopExitAlternator() {
        exitAlternatorActive = false;
        ++exitTransitionToken;
        if (exitArrowTick != null) {
            mainHandler.removeCallbacks(exitArrowTick);
            exitArrowTick = null;
        }
        if (exitStopPending != null) {
            mainHandler.removeCallbacks(exitStopPending);
            exitStopPending = null;
        }
        // Cancel animations, restore arrows, hide exit texts
        restoreAllPairs();
        startTurnBlink();
    }

    private void restoreAllPairs() {
        restorePair(turnIconView, turnExitText);
        restorePair(clusterTurnIconView, clusterTurnExitText);
        restorePair(turnLeadIconView, turnLeadExitText);
        restorePair(clusterTurnLeadIconView, clusterTurnLeadExitText);
        restorePair(navTurnIconView, navExitText);
        restorePair(clusterNavTurnIconView, clusterNavExitText);
        restoreTurnArrowAlpha();
    }

    private void restorePair(ImageView iv, TextView tv) {
        if (iv != null) { iv.animate().cancel(); iv.setAlpha(1f); }
        if (tv != null) { tv.animate().cancel(); tv.setAlpha(0f); tv.setVisibility(View.GONE); }
    }

    // -- 10001 / 12011 exit info handlers ------------------------------------

    private void updateRouteGuidanceExitInfo(Bundle extras) {
        if (intValue(extras, "KEY_TYPE", -1) != AmapConstants.KEY_TYPE_ROUTE_GUIDANCE) {
            return;
        }
        boolean hasExitPayload = hasAny(extras,
                "EXIT_NAME_INFO", "exit_name_info", "exitNameInfo", "exitName",
                "EXIT_DIRECTION_INFO", "exit_direction_info", "exitDirectionInfo", "exitDirection");
        if (!hasExitPayload) {
            return;
        }
        String nameInfo = valueString(extras,
                "EXIT_NAME_INFO", "exit_name_info", "exitNameInfo", "exitName");
        String directionInfo = valueString(extras,
                "EXIT_DIRECTION_INFO", "exit_direction_info", "exitDirectionInfo", "exitDirection");
        routeGuidanceExitSupported = true;
        exitDirection = cleanExitInfoText(directionInfo);
        routeGuidanceExitLabel = buildRouteGuidanceExitLabel(nameInfo, exitDirection);
        exitLabel = routeGuidanceExitLabel;
        exitNameNum = parsePositiveInt(exitLabel, -1);
        exitResultState = TextUtils.isEmpty(exitLabel) ? -1 : 1;
        updateExitTextViews(exitLabel);
        ensureExitAlternator();
    }

    private void handleExitInfo(Bundle extras) {
        int fallbackNameNum = intValue(extras, "EXIT_INFO_EXIT_NAME_NUM", -1);
        String fallbackDirection = cleanExitInfoText(valueString(extras, "EXIT_INFO_DIRECTION", ""));
        String fallbackLabel = buildExitLabelFrom12011(fallbackNameNum, fallbackDirection);
        if (!TextUtils.isEmpty(routeGuidanceExitLabel) && !TextUtils.isEmpty(fallbackLabel)
                && !TextUtils.equals(routeGuidanceExitLabel, fallbackLabel)) {
            Log.d(TAG, "exit info mismatch 10001=" + routeGuidanceExitLabel + " 12011=" + fallbackLabel);
        }
        if (routeGuidanceExitSupported) {
            ensureExitAlternator();
            return;
        }

        exitNameNum = fallbackNameNum;
        exitDirection = fallbackDirection;
        exitDistance = intValue(extras, "EXIT_INFO_DISTANCE", -1);
        exitTime = intValue(extras, "EXIT_INFO_TIME", -1);
        exitResultState = intValue(extras, "EXIT_INFO_RESULT_STATE", -1);
        exitLabel = fallbackLabel;
        if (TextUtils.isEmpty(exitLabel)) {
            exitResultState = -1;
        }
        updateExitTextViews(exitLabel);
        ensureExitAlternator();
    }

    private String buildExitLabelFrom12011(int nameNum, String direction) {
        // Try to extract the real exit number from the direction string first.
        // Amap format: "<num><name>,<num><name>,..." e.g. "2影视城,2码头国际健康城"
        if (!TextUtils.isEmpty(direction)) {
            String num = parseFirstExitNumber(direction);
            if (!TextUtils.isEmpty(num)) return num;
        }
        if (nameNum > 0) return String.valueOf(nameNum);
        return "";
    }

    private String buildRouteGuidanceExitLabel(String nameInfo, String directionInfo) {
        String name = compactExitInfoText(nameInfo);
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        String direction = compactExitInfoText(directionInfo);
        if (TextUtils.isEmpty(direction)) {
            return "";
        }
        String num = parseFirstExitNumber(direction);
        return TextUtils.isEmpty(num) ? direction : num;
    }

    private String cleanExitInfoText(String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        String s = text.replace('\r', ' ').replace('\n', ' ').trim();
        if (TextUtils.isEmpty(s) || "0".equals(s) || "null".equalsIgnoreCase(s)) {
            return "";
        }
        try {
            if (s.startsWith("{")) {
                JSONObject object = new JSONObject(s);
                String nested = valueFromExitJson(object,
                        "EXIT_NAME_INFO", "exit_name_info", "exitNameInfo", "exitName",
                        "name", "text", "value");
                if (!TextUtils.isEmpty(nested)) {
                    return cleanExitInfoText(nested);
                }
            } else if (s.startsWith("[")) {
                JSONArray array = new JSONArray(s);
                for (int i = 0; i < array.length(); i++) {
                    Object item = array.opt(i);
                    String nested = item instanceof JSONObject
                            ? valueFromExitJson((JSONObject) item, "name", "text", "value", "exitName", "exit_name_info")
                            : String.valueOf(item);
                    nested = cleanExitInfoText(nested);
                    if (!TextUtils.isEmpty(nested)) {
                        return nested;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return s.replaceAll("\\s+", " ");
    }

    private String valueFromExitJson(JSONObject object, String... keys) {
        if (object == null) {
            return "";
        }
        for (String key : keys) {
            String value = object.optString(key, "");
            if (!TextUtils.isEmpty(value) && !"0".equals(value) && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return "";
    }

    private String compactExitInfoText(String text) {
        String cleaned = cleanExitInfoText(text);
        if (TextUtils.isEmpty(cleaned)) {
            return "";
        }
        String[] parts = cleaned.split("[,\\uFF0C\\u3001;\\uFF1B]+");
        for (String part : parts) {
            String candidate = part.trim();
            if (TextUtils.isEmpty(candidate)) {
                continue;
            }
            candidate = candidate.replace("\u51fa\u53e3\u7f16\u53f7", "")
                    .replace("\u5165\u53e3\u7f16\u53f7", "")
                    .replace("\u51fa\u53e3", "")
                    .replace("\u5165\u53e3", "")
                    .trim();
            if (!TextUtils.isEmpty(candidate)) {
                return candidate;
            }
        }
        return cleaned;
    }

    private int parsePositiveInt(String value, int fallback) {
        if (TextUtils.isEmpty(value)) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    /** Parse the leading digits from the first segment of the direction string. */
    private String parseFirstExitNumber(String dir) {
        if (TextUtils.isEmpty(dir)) return "";
        String first = dir.split("[,\\uFF0C\\u3001;\\uFF1B]+")[0].trim();
        Matcher leading = Pattern.compile("^\\D*?([0-9]{1,4}[A-Za-z]?)").matcher(first);
        if (leading.find()) {
            return leading.group(1);
        }
        Matcher any = Pattern.compile("([0-9]{1,4}[A-Za-z]?)").matcher(first);
        if (any.find()) {
            return any.group(1);
        }
        return "";
    }

    private void updateExitTextViews(String label) {
        setExitText(navExitText, label);
        setExitText(clusterNavExitText, label);
        setExitText(turnExitText, label);
        setExitText(clusterTurnExitText, label);
        setExitText(turnLeadExitText, label);
        setExitText(clusterTurnLeadExitText, label);
        restoreExitAlternatorVisualState();
        applyNavigationTextVisualState();
    }

    private void setExitText(TextView view, String label) {
        if (view == null) {
            return;
        }
        view.setText(label);
        fitExitTextToWrapper(view);
    }

    private void clearExitInfoState() {
        exitNameNum = -1;
        exitDirection = "";
        exitLabel = "";
        routeGuidanceExitSupported = false;
        routeGuidanceExitLabel = "";
        exitDistance = -1;
        exitTime = -1;
        exitResultState = -1;
        updateExitTextViews("");
        stopExitAlternator();
    }

    private void restoreNormalBorder() {
        int opacity = AppPrefs.getBackgroundOpacityPercent(this);
        if (panelBackground != null) {
            panelBackground.setStroke(scaledDp(1, overlayScale),
                    withAlpha(0xFFFFFFFF, AppPrefs.strokeOpacityForBackground(opacity)));
            if (panel != null) panel.invalidate();
        }
        if (clusterPanelBackground != null && clusterPanel != null) {
            float cs = clusterScale > 0 ? clusterScale : overlayScale;
            clusterPanelBackground.setStroke(scaledDp(1, cs),
                    withAlpha(0xFFFFFFFF, AppPrefs.strokeOpacityForBackground(opacity)));
            clusterPanel.invalidate();
        }
    }

    private void setTmcBarVisible(boolean visible) {
        boolean show = visible && AppPrefs.isTmcBarVisible(this);
        if (mainTmcProgressBar != null) {
            mainTmcProgressBar.setDrawEnabled(show);
        }
        if (clusterTmcProgressBar != null) {
            clusterTmcProgressBar.setDrawEnabled(show);
        }
    }

    private void updateDynamicIslandLayout() {
        if (AppPrefs.isCardUiEnabled(this)) {
            updateCardLayout();
            return;
        }
        if (!isDynamicIslandOrCard()) {
            return;
        }
        boolean isNav = AppPrefs.isTurnVisible(this) && !inCruiseMode && currentTurnIcon > 0;
        boolean isCruise = inCruiseMode;
        boolean isEmpty = !isNav && !isCruise;
        renderDynamicIslandView(mainDynamicIslandViews, isNav, isCruise, isEmpty, overlayScale);
        renderDynamicIslandView(clusterDynamicIslandViews, isNav, isCruise, isEmpty,
                clusterScale > 0 ? clusterScale : overlayScale);
        if (isNav) {
            ensureFullModeAlternator();
            updateFullModeAlternatingDisplay();
        }
        applyNavigationTextVisualState();
    }

    private void renderDynamicIslandView(DynamicIslandViews views, boolean isNav, boolean isCruise,
                                         boolean isEmpty, float scale) {
        if (views == null) {
            return;
        }
        updateDynamicStandbyMode(views.mode, isEmpty, scale);
        int navVis = isNav ? View.VISIBLE : View.GONE;
        if (views.navTurnBox != null && views.navTurnBox.getVisibility() != navVis) {
            views.navTurnBox.setVisibility(navVis);
        }
        if (isNav) {
            updateNavTurn(views.navTurnBox, views.navIcon, views.navDistance);
            String roadName = currentTurnRoad;
            if (TextUtils.isEmpty(roadName) || "\u4e0b\u4e00\u8def\u53e3".equals(roadName)) {
                roadName = currentRoadName;
            }
            updateCompactMarqueeText(views.navRoad, roadName);
        }
        int cruiseVis = isCruise ? View.VISIBLE : View.GONE;
        if (views.cruiseLeft != null && views.cruiseLeft.getVisibility() != cruiseVis) {
            views.cruiseLeft.setVisibility(cruiseVis);
        }
        if (isCruise) {
            String road = !TextUtils.isEmpty(currentRoadName) ? currentRoadName : "";
            if (views.cruiseRoad != null && !TextUtils.isEmpty(road)
                    && !TextUtils.equals(views.cruiseRoad.getText(), road)) {
                views.cruiseRoad.setText(road);
            }
            updateDynamicIslandCruiseDirectionText(views.cruiseDirection);
        }
    }

    private void updateDynamicStandbyMode(TextView view, boolean visible) {
        updateDynamicStandbyMode(view, visible, overlayScale);
    }

    private void updateDynamicStandbyMode(TextView view, boolean visible, float scale) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = visible ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
        lp.height = visible ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
        view.setLayoutParams(lp);
        if (visible) {
            view.setText("待接收导航/巡航信息");
            view.setTextColor(primaryTextColor());
            view.setTextSize(scaledSp(13f, scale));
            view.setGravity(Gravity.CENTER);
            view.setSingleLine(true);
        }
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateDynamicIslandCruiseDirectionText(TextView view) {
        if (view == null) {
            return;
        }
        String text;
        if (!TextUtils.isEmpty(currentHeadingSummary)) {
            text = "【 " + currentHeadingSummary + " 】";
        } else if (TextUtils.isEmpty(view.getText())) {
            text = "【 -- 】";
        } else {
            text = null;
        }
        if (text != null && !TextUtils.equals(view.getText(), text)) {
            view.setText(text);
        }
        if (view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private void startCompactBreathing() {
        // No-op: removed compact breathing
    }

    private void stopCompactBreathing() {
        if (navTurnIconView != null) {
            navTurnIconView.setAlpha(1f);
        }
        if (clusterNavTurnIconView != null) {
            clusterNavTurnIconView.setAlpha(1f);
        }
    }

    // --- Dynamic island alternator helpers ---

    private void ensureFullModeAlternator() {
        if (fullModeAlternator != null) {
            return; // Already running, don't restart
        }
        if (!AppPrefs.isDynamicIslandUiEnabled(this)) {
            return;
        }
        if (navTurnBox == null || navTurnBox.getVisibility() != View.VISIBLE) {
            return;
        }
        startFullModeAlternator();
    }

    private void startFullModeAlternator() {
        stopFullModeAlternator();
        if (!AppPrefs.isDynamicIslandUiEnabled(this)) {
            return;
        }
        if (navTurnBox == null || navTurnBox.getVisibility() != View.VISIBLE) {
            return;
        }
        fullModeShowEta = false;
        updateFullModeAlternatingDisplay();
        fullModeAlternator = new Runnable() {
            @Override
            public void run() {
                fullModeShowEta = !fullModeShowEta;
                updateFullModeAlternatingDisplay();
                long delay = fullModeShowEta ? FULL_MODE_ETA_MS : FULL_MODE_TURN_MS;
                mainHandler.postDelayed(this, delay);
            }
        };
        mainHandler.postDelayed(fullModeAlternator, FULL_MODE_TURN_MS);
    }

    private void stopFullModeAlternator() {
        if (fullModeAlternator != null) {
            mainHandler.removeCallbacks(fullModeAlternator);
            fullModeAlternator = null;
        }
        fullModeShowEta = false;
    }

    private void updateFullModeAlternatingDisplay() {
        if (fullModeTurnInfoCol == null || fullModeEtaInfoCol == null) {
            return;
        }
        if (navTurnBox == null || navTurnBox.getVisibility() != View.VISIBLE) {
            return;
        }

        boolean showEta = !shouldPinFullModeTurnInfo()
                && fullModeShowEta
                && hasFullModeEtaContent();
        fullModeTurnInfoCol.setVisibility(showEta ? View.GONE : View.VISIBLE);
        fullModeEtaInfoCol.setVisibility(showEta ? View.VISIBLE : View.GONE);

        // Update cluster too
        if (fullModeClusterTurnInfoCol != null && fullModeClusterEtaInfoCol != null
                && clusterNavTurnBox != null && clusterNavTurnBox.getVisibility() == View.VISIBLE) {
            fullModeClusterTurnInfoCol.setVisibility(showEta ? View.GONE : View.VISIBLE);
            fullModeClusterEtaInfoCol.setVisibility(showEta ? View.VISIBLE : View.GONE);
        }
    }

    private boolean hasFullModeEtaContent() {
        return hasText(fullModeEtaRemainDist)
                || hasText(fullModeEtaArriveTime)
                || hasText(fullModeClusterEtaRemainDist)
                || hasText(fullModeClusterEtaArriveTime);
    }

    private boolean hasText(TextView view) {
        return view != null && !TextUtils.isEmpty(view.getText());
    }

    private void updateFullModeEtaInfo(String remainDistText, int remainSeconds) {
        if (!AppPrefs.isDynamicIslandUiEnabled(this)) {
            return;
        }
        // Only update when valid data exists - don't overwrite with empty
        boolean hasDist = !TextUtils.isEmpty(remainDistText);
        boolean hasTime = remainSeconds > 0;

        if (hasDist) {
            String dist = remainDistText.replace("\u5343\u7c73", "km")
                    .replace("\u516c\u91cc", "km")
                    .replace("\u7c73", "m");
            SpannableString sp = new SpannableString("\u4f59 " + dist);
            sp.setSpan(new RelativeSizeSpan(0.65f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (fullModeEtaRemainDist != null) {
                fullModeEtaRemainDist.setText(sp);
            }
            if (fullModeClusterEtaRemainDist != null) {
                fullModeClusterEtaRemainDist.setText(sp);
            }
        }
        if (hasTime) {
            long arriveMillis = System.currentTimeMillis() + remainSeconds * 1000L;
            java.util.Calendar arrive = java.util.Calendar.getInstance();
            arrive.setTimeInMillis(arriveMillis);
            int hour = arrive.get(java.util.Calendar.HOUR_OF_DAY);
            int minute = arrive.get(java.util.Calendar.MINUTE);
            String text = String.format(java.util.Locale.US, "%d:%02d\u5230\u8fbe", hour, minute);
            if (fullModeEtaArriveTime != null) {
                fullModeEtaArriveTime.setText(text);
            }
            if (fullModeClusterEtaArriveTime != null) {
                fullModeClusterEtaArriveTime.setText(text);
            }
        }
        // Ensure alternator is running when ETA data arrives during navigation
        if ((hasDist || hasTime) && !inCruiseMode && currentTurnIcon > 0
                && navTurnBox != null && navTurnBox.getVisibility() == View.VISIBLE) {
            ensureFullModeAlternator();
        }
        updateFullModeAlternatingDisplay();
    }

    private void populateCompactWidgetRow(int speed, int cameraIndex,
                                          int cameraDist, int cameraType, int lightNum) {
        populateOneCompactWidgetRow(compactWidgetRow, this, overlayScale, speed, cameraIndex,
                cameraDist, cameraType, lightNum);
        if (clusterContext != null) {
            populateOneCompactWidgetRow(clusterCompactWidgetRow, clusterContext, clusterScale, speed,
                    cameraIndex, cameraDist, cameraType, lightNum);
        }
    }

    private void populateOneCompactWidgetRow(LinearLayout row, Context context, float scale, int speed,
                                             int cameraIndex, int cameraDist, int cameraType, int lightNum) {
        if (row == null) {
            return;
        }
        if (!AppPrefs.isAlertVisible(this)) {
            row.setVisibility(View.GONE);
            return;
        }
        ensureCompactWidgetChildren(row, context, scale);
        boolean anyVisible = false;

        View speedBox = row.findViewWithTag("speed_box");
        if (speedBox instanceof FrameLayout) {
            if (speed > 0) {
                speedBox.setVisibility(View.VISIBLE);
                FrameLayout frame = (FrameLayout) speedBox;
                if (frame.getChildCount() > 1 && frame.getChildAt(1) instanceof TextView) {
                    ((TextView) frame.getChildAt(1)).setText(String.valueOf(speed));
                }
                anyVisible = true;
            } else {
                speedBox.setVisibility(View.GONE);
            }
        }

        View cameraBox = row.findViewWithTag("camera_box");
        if (cameraBox instanceof LinearLayout) {
            boolean hasCamera = cameraIndex != -1 && cameraDist >= 0;
            if (hasCamera) {
                cameraBox.setVisibility(View.VISIBLE);
                LinearLayout box = (LinearLayout) cameraBox;
                View iconFrame = box.findViewWithTag("camera_icon_frame");
                if (iconFrame instanceof FrameLayout) {
                    FrameLayout frame = (FrameLayout) iconFrame;
                    boolean speedCamera = isSpeedCameraType(cameraType) && speed > 0;
                    if (frame.getChildCount() > 0 && frame.getChildAt(0) instanceof ImageView) {
                        applyEdogIcon((ImageView) frame.getChildAt(0), cameraType, speedCamera);
                    }
                    if (frame.getChildCount() > 1 && frame.getChildAt(1) instanceof TextView) {
                        TextView limit = (TextView) frame.getChildAt(1);
                        limit.setText(speedCamera ? String.valueOf(speed) : "");
                        limit.setVisibility(speedCamera ? View.VISIBLE : View.GONE);
                    }
                }
                if (box.getChildCount() > 1 && box.getChildAt(1) instanceof TextView) {
                    ((TextView) box.getChildAt(1)).setText(formatDistanceCompact(cameraDist));
                }
                anyVisible = true;
            } else {
                cameraBox.setVisibility(View.GONE);
            }
        }

        View lightBox = row.findViewWithTag("light_box");
        if (lightBox instanceof LinearLayout) {
            if (lightNum > 0) {
                lightBox.setVisibility(View.VISIBLE);
                LinearLayout box = (LinearLayout) lightBox;
                if (box.getChildCount() > 1 && box.getChildAt(1) instanceof TextView) {
                    ((TextView) box.getChildAt(1)).setText(lightNum + "\u4e2a");
                }
                anyVisible = true;
            } else {
                lightBox.setVisibility(View.GONE);
            }
        }

        row.setVisibility(anyVisible ? View.VISIBLE : View.GONE);
    }

    private void ensureCompactWidgetChildren(LinearLayout row, Context context, float scale) {
        if (row == null || row.getChildCount() > 0) {
            return;
        }
        int iconSize = scaledDp(24, scale);

        FrameLayout speedBox = new FrameLayout(context);
        speedBox.setTag("speed_box");
        ImageView speedIcon = new ImageView(context);
        applyEdogIcon(speedIcon, -1, true);
        speedIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        speedBox.addView(speedIcon, new FrameLayout.LayoutParams(iconSize, iconSize));
        TextView speedText = new TextView(context);
        speedText.setTextColor(0xFFDC2626);
        speedText.setTextSize(scaledSp(11f, scale));
        speedText.setTypeface(Typeface.DEFAULT_BOLD);
        speedText.setGravity(Gravity.CENTER);
        speedText.setIncludeFontPadding(false);
        speedBox.addView(speedText, new FrameLayout.LayoutParams(iconSize, iconSize));
        speedBox.setVisibility(View.GONE);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, -2);
        slp.setMargins(0, 0, scaledDp(3, scale), 0);
        row.addView(speedBox, slp);

        LinearLayout cameraBox = new LinearLayout(context);
        cameraBox.setTag("camera_box");
        cameraBox.setOrientation(LinearLayout.HORIZONTAL);
        cameraBox.setGravity(Gravity.CENTER_VERTICAL);
        cameraBox.setVisibility(View.GONE);
        FrameLayout cameraIconFrame = new FrameLayout(context);
        cameraIconFrame.setTag("camera_icon_frame");
        ImageView cameraIcon = new ImageView(context);
        applyEdogIcon(cameraIcon, -1, false);
        cameraIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cameraIconFrame.addView(cameraIcon, new FrameLayout.LayoutParams(iconSize, iconSize));
        TextView cameraSpeed = new TextView(context);
        cameraSpeed.setTextColor(0xFFDC2626);
        cameraSpeed.setTextSize(scaledSp(11f, scale));
        cameraSpeed.setTypeface(Typeface.DEFAULT_BOLD);
        cameraSpeed.setGravity(Gravity.CENTER);
        cameraSpeed.setIncludeFontPadding(false);
        cameraSpeed.setVisibility(View.GONE);
        cameraIconFrame.addView(cameraSpeed, new FrameLayout.LayoutParams(iconSize, iconSize));
        cameraBox.addView(cameraIconFrame, new LinearLayout.LayoutParams(iconSize, iconSize));
        TextView distText = new TextView(context);
        distText.setTextColor(primaryTextColor());
        distText.setTextSize(scaledSp(9f, scale));
        distText.setTypeface(Typeface.DEFAULT_BOLD);
        distText.setSingleLine(true);
        cameraBox.addView(distText, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
        clp.setMargins(0, 0, scaledDp(4, scale), 0);
        row.addView(cameraBox, clp);

        LinearLayout lightBox = new LinearLayout(context);
        lightBox.setTag("light_box");
        lightBox.setOrientation(LinearLayout.HORIZONTAL);
        lightBox.setGravity(Gravity.CENTER_VERTICAL);
        lightBox.setVisibility(View.GONE);
        ImageView lightIcon = new ImageView(context);
        applyTrafficLightEdogIcon(lightIcon);
        lightIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        lightBox.addView(lightIcon, new LinearLayout.LayoutParams(iconSize, iconSize));
        TextView lightText = new TextView(context);
        lightText.setTextColor(primaryTextColor());
        lightText.setTextSize(scaledSp(9f, scale));
        lightText.setTypeface(Typeface.DEFAULT_BOLD);
        lightText.setSingleLine(true);
        lightBox.addView(lightText, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-2, -2);
        llp.setMargins(0, 0, scaledDp(4, scale), 0);
        row.addView(lightBox, llp);

        FontManager.applyToViewTree(context, row);
    }

    private boolean isSpeedCameraType(int type) {
        return type == 0 || type == 2 || type == 3 || type == 7 || type == 10 || type == 11;
    }

    private void clearCompactWidgetRow() {
        if (compactWidgetRow != null) {
            compactWidgetRow.removeAllViews();
            compactWidgetRow.setVisibility(View.GONE);
        }
        if (clusterCompactWidgetRow != null) {
            clusterCompactWidgetRow.removeAllViews();
            clusterCompactWidgetRow.setVisibility(View.GONE);
        }
    }

    private String formatDistance(int meters) {
        if (meters >= 1000) {
            float km = meters / 1000f;
            return String.format(java.util.Locale.US, "%.1fkm", km);
        }
        return meters + "m";
    }

    private String formatDistanceCompact(int meters) {
        if (meters >= 1000) {
            float km = meters / 1000f;
            if (km >= 10f) {
                return String.format(java.util.Locale.US, "%.0fkm", km);
            }
            return String.format(java.util.Locale.US, "%.1fkm", km);
        }
        return meters + "m";
    }

    /** Parse meters from a formatted distance string like "1.2公里" or "500米". */
    private int parseDistanceMeters(String s) {
        if (TextUtils.isEmpty(s)) return -1;
        try {
            boolean isKm = s.contains("km") || s.contains("公里");
            String num = s.replace("km", "").replace("m", "").replace("公里", "").replace("米", "").trim();
            float val = Float.parseFloat(num);
            return Math.round(isKm ? val * 1000f : val);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private String formatDuration(int seconds) {
        int minutes = Math.max(1, Math.round(seconds / 60f));
        return minutes + "\u5206\u949f";
    }

    private String locationSummary(String json) {
        try {
            JSONObject object = new JSONObject(json);
            String provider = object.optString("provider", "");
            double speed = object.optDouble("speed", Double.NaN);
            int bearing = object.optInt("bearing", -1);
            int accuracy = object.optInt("accuracy", -1);
            StringBuilder sb = new StringBuilder("\u5b9a\u4f4d");
            if (!TextUtils.isEmpty(provider)) {
                sb.append(' ').append(provider.toUpperCase(java.util.Locale.US));
            }
            if (!Double.isNaN(speed)) {
                int kmh = speed < 45 ? Math.round((float) speed * 3.6f) : Math.round((float) speed);
                sb.append(' ').append(kmh).append("km/h");
            }
            if (bearing >= 0) {
                sb.append(' ').append(bearing).append('\u00b0');
            }
            if (accuracy >= 0) {
                sb.append(" \u00b1").append(accuracy).append('m');
            }
            return sb.toString();
        } catch (Throwable t) {
            Log.e(TAG, "parse location json failed: " + json, t);
            return null;
        }
    }

    private String cameraTypeName(int type) {
        switch (type) {
            case 0:
                return "\u6d4b\u901f";
            case 1:
                return "\u76d1\u63a7";
            case 2:
                return "\u95ef\u7ea2\u706f";
            case 3:
                return "\u8fdd\u7ae0";
            case 4:
                return "\u516c\u4ea4\u9053";
            case 5:
                return "\u5e94\u6025\u8f66\u9053";
            case 6:
                return "\u975e\u673a\u52a8\u8f66\u9053";
            case 11:
                return "ETC\u6d4b\u901f";
            case 12:
                return "\u538b\u7ebf";
            case 13:
                return "\u4eba\u884c\u9053";
            case 14:
                return "\u76d1\u63a7";
            case 15:
                return "\u95ef\u7ea2\u706f";
            case 16:
                return "\u516c\u4ea4\u9053";
            case 17:
                return "\u5e94\u6025\u8f66\u9053";
            case 18:
                return "\u5b89\u5168\u5e26";
            case 19:
                return "\u624b\u673a";
            case 20:
                return "\u975e\u673a\u52a8\u8f66\u9053";
            case 21:
                return "\u8fdd\u505c";
            case 22:
                return "\u706f\u5149";
            case 23:
                return "\u76d1\u63a7";
            case 24:
                return "\u9e23\u7b1b";
            case 25:
                return "\u9006\u884c";
            case 26:
                return "\u94c1\u8def";
            case 27:
                return "\u4e0d\u6309\u5bfc\u5411\u8f66\u9053";
            case 28:
                return "\u8f66\u8ddd";
            case 29:
                return "HOV";
            case 30:
                return "\u8fdd\u7ae0\u6293\u62cd";
            default:
                return "\u7535\u5b50\u773c";
        }
    }

    private void applyTrafficLightEdogIcon(ImageView view) {
        if (view == null) {
            return;
        }
        Bitmap bitmap = PluginAssets.activeIconBitmap(this,
                "edog_traffic_light",
                "traffic_light",
                "widget_drawable_auto_ic_edog_traffic_loading");
        if (bitmap != null) {
            view.setImageBitmap(bitmap);
            return;
        }
        view.setImageResource(R.drawable.widget_drawable_auto_ic_edog_traffic_loading);
    }

    private void applyEdogIcon(ImageView view, int type, boolean speedCamera) {
        if (view == null) {
            return;
        }
        ArrayList<String> names = new ArrayList<>();
        if (speedCamera) {
            names.add("edog_limit_speed");
            names.add("limit_speed");
            names.add("widget_drawable_auto_ic_edog_limit_speed_loading");
        } else if (type >= 0) {
            names.add("edog_type_" + type);
            names.add("camera_type_" + type);
        }
        names.add(edogPluginIconName(type));
        names.add("edog_camera");
        names.add("widget_drawable_auto_ic_edog_camera_loading");
        Bitmap bitmap = PluginAssets.activeIconBitmap(this, names.toArray(new String[0]));
        if (bitmap != null) {
            view.setImageBitmap(bitmap);
            return;
        }
        view.setImageResource(speedCamera
                ? R.drawable.widget_drawable_auto_ic_edog_limit_speed_loading
                : edogIconResource(type));
    }

    private String edogPluginIconName(int type) {
        switch (type) {
            case 0:
                return "edog_limit_speed";
            case 2:
            case 15:
                return "edog_traffic_light";
            case 4:
            case 16:
                return "edog_bus";
            case 5:
            case 17:
                return "edog_emergency_line";
            case 6:
            case 20:
                return "edog_bicycle_lane";
            case 11:
                return "edog_speed_etc";
            case 12:
                return "edog_line";
            case 13:
                return "edog_sidewalk";
            case 18:
                return "edog_seatbelt";
            case 19:
                return "edog_phone";
            case 21:
                return "edog_parking";
            case 22:
                return "edog_lamp";
            case 24:
                return "edog_speaker";
            case 25:
                return "edog_reverse";
            case 26:
                return "edog_railway";
            case 27:
                return "edog_tail";
            case 28:
                return "edog_space";
            case 29:
                return "edog_hov";
            case 30:
                return "edog_recycle";
            case 1:
            case 3:
            case 14:
            case 23:
            default:
                return "edog_camera";
        }
    }

    private int edogIconResource(int type) {
        switch (type) {
            case 0:
                return R.drawable.widget_drawable_auto_ic_edog_limit_speed_loading;
            case 2:
            case 15:
                return R.drawable.widget_drawable_auto_ic_edog_traffic_loading;
            case 4:
            case 16:
                return R.drawable.widget_drawable_auto_ic_edog_bus_loading;
            case 5:
            case 17:
                return R.drawable.widget_drawable_auto_ic_edog_emergency_line_loading;
            case 6:
            case 20:
                return R.drawable.widget_drawable_auto_ic_edog_bicycle_lane_loading;
            case 11:
                return R.drawable.widget_drawable_auto_ic_edog_speed_etc_loading;
            case 12:
                return R.drawable.widget_drawable_auto_ic_edog_line_loading;
            case 13:
                return R.drawable.widget_drawable_auto_ic_edog_sidewalk_loading;
            case 18:
                return R.drawable.widget_drawable_auto_ic_edog_seatbelt_loading;
            case 19:
                return R.drawable.widget_drawable_auto_ic_edog_phone_loading;
            case 21:
                return R.drawable.widget_drawable_auto_ic_edog_parking_loading;
            case 22:
                return R.drawable.widget_drawable_auto_ic_edog_lamp;
            case 24:
                return R.drawable.widget_drawable_auto_ic_edog_speaker_loading;
            case 25:
                return R.drawable.widget_drawable_auto_ic_edog_reverse;
            case 26:
                return R.drawable.widget_drawable_auto_ic_edog_railway;
            case 27:
                return R.drawable.widget_drawable_auto_ic_edog_tail;
            case 28:
                return R.drawable.widget_drawable_auto_ic_edog_space;
            case 29:
                return R.drawable.widget_drawable_auto_ic_edog_hov;
            case 30:
                return R.drawable.widget_drawable_auto_ic_edog_recycle;
            case 1:
            case 3:
            case 14:
            case 23:
            default:
                return R.drawable.widget_drawable_auto_ic_edog_camera_loading;
        }
    }

    private String roadTypeName(int type) {
        switch (type) {
            case 0:
                return "\u9ad8\u901f";
            case 1:
                return "\u56fd\u9053";
            case 2:
                return "\u7701\u9053";
            case 3:
                return "\u53bf\u9053";
            case 4:
                return "\u4e61\u516c\u8def";
            case 5:
                return "\u53bf\u4e61\u6751\u5185\u90e8\u8def";
            case 6:
                return "\u57ce\u5e02\u5feb\u901f\u8def";
            case 7:
                return "\u4e3b\u8981\u9053\u8def";
            case 8:
                return "\u6b21\u8981\u9053\u8def";
            case 9:
                return "\u666e\u901a\u9053\u8def";
            case 10:
                return "\u975e\u5bfc\u822a\u9053\u8def";
            default:
                return "Type " + type;
        }
    }

    private String join(ArrayList<String> values, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(delimiter);
            }
            sb.append(value);
        }
        return sb.toString();
    }

    private String describeExtras(Bundle extras) {
        if (extras == null) {
            return "{}";
        }
        ArrayList<String> keys = new ArrayList<>(extras.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object value = safeExtra(extras, key);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(key).append('=');
            sb.append(value);
            if (value != null) {
                sb.append('(').append(value.getClass().getName()).append(')');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private Notification buildNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                ensureNotificationChannel(nm);
            }
            builder = createNotificationBuilderWithChannel();
        } else {
            builder = new Notification.Builder(this);
        }
        return builder
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("AMap Companion")
                .setContentText("\u76d1\u542c\u9ad8\u5fb7\u5bfc\u822a/\u5de1\u822a\u5e7f\u64ad")
                .setOngoing(true)
                .build();
    }

    private void ensureNotificationChannel(NotificationManager notificationManager) {
        try {
            Class<?> channelClass = Class.forName("android.app.NotificationChannel");
            Constructor<?> ctor = channelClass.getConstructor(String.class, CharSequence.class, int.class);
            Object channel = ctor.newInstance(CHANNEL_ID, "AMap Companion", 2);
            notificationManager.getClass()
                    .getMethod("createNotificationChannel", channelClass)
                    .invoke(notificationManager, channel);
        } catch (Throwable ignored) {
        }
    }

    private Notification.Builder createNotificationBuilderWithChannel() {
        try {
            Constructor<Notification.Builder> ctor =
                    Notification.Builder.class.getConstructor(Context.class, String.class);
            return ctor.newInstance(this, CHANNEL_ID);
        } catch (Throwable ignored) {
            return new Notification.Builder(this);
        }
    }

    private int dp(int value) {
        return dp((float) value);
    }

    private int dp(float value) {
        return scaledDp(value, overlayScale);
    }

    private int rawDp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private float sp(float value) {
        return scaledSp(value, overlayScale);
    }

    private int clusterDp(float value) {
        return scaledDp(value, clusterScale);
    }

    private float clusterSp(float value) {
        return scaledSp(value, clusterScale);
    }

    /**
     * Replace a FrameLayout placeholder (from XML) with a new LaneBarView.
     * Returns the LaneBarView instance.
     */
    private LaneBarView installLaneBar(View root, int placeholderId, float scale,
                                        float laneScale, int customHeightDp, int laneSpacingDp,
                                        boolean compactSpacing, boolean useCrop, int minCellCount) {
        return installLaneBar(this, root, placeholderId, scale, laneScale, customHeightDp,
                laneSpacingDp, compactSpacing, useCrop, minCellCount);
    }

    private LaneBarView installLaneBar(Context ctx, View root, int placeholderId, float scale,
                                        float laneScale, int customHeightDp, int laneSpacingDp,
                                        boolean compactSpacing, boolean useCrop, int minCellCount) {
        FrameLayout placeholder = (FrameLayout) root.findViewById(placeholderId);
        if (placeholder == null) return null;
        placeholder.removeAllViews();
        LaneBarView lane = new LaneBarView(ctx);
        lane.setFrameScaleMultiplier(scale);
        lane.setScaleMultiplier(laneScale);
        if (customHeightDp > 0) lane.setCustomHeightDp(customHeightDp);
        if (laneSpacingDp > 0) lane.setLaneSpacingDp(laneSpacingDp);
        if (compactSpacing) lane.setCompactSpacing(true);
        if (useCrop) lane.setUseCommonBitmapCrop(true);
        if (minCellCount > 0) lane.setMinCellCount(minCellCount);
        // Compact lane bars hide background and dividers
        if (customHeightDp > 0) {
            lane.setShowBackground(false);
            lane.setShowDividers(false);
        }
        placeholder.addView(lane, new LinearLayout.LayoutParams(-2, -2));
        return lane;
    }

    /** Overload: simple LaneBarView (classic/dashboard style) */
    private LaneBarView installLaneBarSimple(View root, int placeholderId, float scale, float laneScale) {
        return installLaneBarSimple(this, root, placeholderId, scale, laneScale);
    }

    private LaneBarView installLaneBarSimple(Context ctx, View root, int placeholderId, float scale, float laneScale) {
        FrameLayout placeholder = (FrameLayout) root.findViewById(placeholderId);
        if (placeholder == null) return null;
        placeholder.removeAllViews();
        LaneBarView lane = new LaneBarView(ctx);
        lane.setFrameScaleMultiplier(scale);
        lane.setScaleMultiplier(laneScale);
        placeholder.addView(lane, new LinearLayout.LayoutParams(-2, -2));
        return lane;
    }

    private int scaledDp(float value, float scale) {
        // Always use main display density so elements have identical proportions
        // on both main and cluster screens. The scale parameter controls overall size.
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * scale * density + 0.5f);
    }

    private float scaledSp(float value, float scale) {
        return value * scale;
    }

}
