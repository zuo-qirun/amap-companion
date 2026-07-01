package com.autonavi.companion;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;

/**
 * Centralized access to all SharedPreferences keys, broadcast action constants,
 * UI style constants, and preference accessor methods.
 *
 * Extracted from MainActivity to decouple OverlayService and other components
 * from the Activity class.
 */
public final class AppPrefs {

    private AppPrefs() {}

    // ── SharedPreferences name ───────────────────────────────────────────
    public static final String PREFS = "amap_companion";

    // ── Preference keys ──────────────────────────────────────────────────
    public static final String KEY_TARGET_PACKAGE               = "target_package";
    public static final String KEY_OVERLAY_SCALE_PERCENT        = "overlay_scale_percent";
    public static final String KEY_MAIN_OVERLAY_ENABLED         = "main_overlay_enabled";
    public static final String KEY_CLUSTER_MIRROR_ENABLED       = "cluster_mirror_enabled";
    public static final String KEY_OVERLAY_X                    = "overlay_x";
    public static final String KEY_OVERLAY_Y                    = "overlay_y";
    public static final String KEY_CLUSTER_X                    = "cluster_x";
    public static final String KEY_CLUSTER_Y                    = "cluster_y";
    public static final String KEY_CLUSTER_SCALE_PERCENT        = "cluster_scale_percent";
    public static final String KEY_CLUSTER_DISPLAY_ID           = "cluster_display_id";
    public static final String KEY_SHOW_MODE                    = "show_mode";
    public static final String KEY_SHOW_TURN                    = "show_turn";
    public static final String KEY_SHOW_LANE                    = "show_lane";
    public static final String KEY_SHOW_LIGHT                   = "show_light";
    public static final String KEY_SHOW_SERVICE_AREA            = "show_service_area";
    public static final String KEY_SHOW_ETA                     = "show_eta";
    public static final String KEY_SHOW_DESTINATION             = "show_destination";
    public static final String KEY_SHOW_ALERT                   = "show_alert";
    public static final String KEY_SHOW_DETAIL                  = "show_detail";
    public static final String KEY_SHOW_TMC_BAR                 = "show_tmc_bar";
    public static final String KEY_TRANSPARENT_BACKGROUND       = "transparent_background";
    public static final String KEY_BACKGROUND_OPACITY_PERCENT   = "background_opacity_percent";
    public static final String KEY_BACKGROUND_COLOR             = "background_color";
    public static final String KEY_TEXT_MODE                    = "text_mode";
    public static final String KEY_CUSTOM_TEXT_COLOR_ENABLED    = "custom_text_color_enabled";
    public static final String KEY_TEXT_COLOR                   = "text_color";
    public static final String KEY_DAY_NIGHT_STATE              = "day_night_state";
    public static final String KEY_OVERLAY_UI_STYLE             = "overlay_ui_style";
    public static final String KEY_AUTO_START_ENABLED           = "auto_start_enabled";
    public static final String KEY_START_SERVICE_ON_APP_OPEN    = "start_service_on_app_open";
    public static final String KEY_LAUNCH_TARGET_FROM_DESKTOP   = "launch_target_from_desktop";
    public static final String KEY_SHOW_MAIN_WHEN_TARGET_FOREGROUND  = "show_main_when_target_foreground";
    public static final String KEY_HIDE_MAIN_WHEN_TARGET_FOREGROUND  = "hide_main_when_target_foreground";
    public static final String KEY_HIDE_CLUSTER_WHEN_INACTIVE   = "hide_cluster_when_inactive";
    public static final String KEY_OVERSPEED_MILD_WARNING       = "overspeed_mild_warning";
    public static final String KEY_OVERSPEED_MEDIUM_WARNING     = "overspeed_medium_warning";
    public static final String KEY_PLUGIN_FONT_ID               = "plugin_font_id";
    public static final String KEY_PLUGIN_ICONS_ID              = "plugin_icons_id";
    public static final String KEY_PLUGIN_UI_ID                 = "plugin_ui_id";

    // ── Broadcast actions ────────────────────────────────────────────────
    public static final String ACTION_MAIN_OVERLAY_CHANGED      = "com.autonavi.companion.MAIN_OVERLAY_CHANGED";
    public static final String ACTION_OVERLAY_SCALE_CHANGED     = "com.autonavi.companion.OVERLAY_SCALE_CHANGED";
    public static final String ACTION_CLUSTER_MIRROR_CHANGED    = "com.autonavi.companion.CLUSTER_MIRROR_CHANGED";
    public static final String ACTION_CLUSTER_POSITION_CHANGED  = "com.autonavi.companion.CLUSTER_POSITION_CHANGED";
    public static final String ACTION_OVERLAY_CONTENT_CHANGED   = "com.autonavi.companion.OVERLAY_CONTENT_CHANGED";
    public static final String ACTION_OVERLAY_STYLE_CHANGED     = "com.autonavi.companion.OVERLAY_STYLE_CHANGED";
    public static final String ACTION_DISPLAY_POLICY_CHANGED    = "com.autonavi.companion.DISPLAY_POLICY_CHANGED";
    public static final String ACTION_PLUGINS_CHANGED           = "com.autonavi.companion.PLUGINS_CHANGED";
    public static final String ACTION_DIAGNOSTIC_REPLAY         = "com.autonavi.companion.DIAGNOSTIC_REPLAY";
    public static final String EXTRA_DIAGNOSTIC_EVENT_JSON      = "diagnostic_event_json";
    public static final String EXTRA_DIAGNOSTIC_REPLAY          = "diagnostic_replay";

    // ── UI style values ──────────────────────────────────────────────────
    public static final String DEFAULT_TARGET_PACKAGE           = "com.autonavi.amapauto";
    public static final String TEXT_MODE_LIGHT                  = "light";
    public static final String TEXT_MODE_AUTO                   = "auto";
    public static final String OVERLAY_UI_OLD                   = OverlayUiStyles.OLD;
    public static final String OVERLAY_UI_NEW                   = OverlayUiStyles.NEW;
    public static final String OVERLAY_UI_DYNAMIC_ISLAND        = OverlayUiStyles.DYNAMIC_ISLAND_FULL;
    public static final String OVERLAY_UI_CARD                  = OverlayUiStyles.CARD;

    // ── Scale / opacity bounds ───────────────────────────────────────────
    public static final int MIN_BACKGROUND_OPACITY_PERCENT      = 0;
    public static final int MAX_BACKGROUND_OPACITY_PERCENT      = 100;
    public static final int DEFAULT_BACKGROUND_OPACITY_PERCENT  = 90;
    public static final int DEFAULT_BACKGROUND_COLOR            = 0xFF111827;
    public static final int DEFAULT_TEXT_COLOR                  = 0xFFE8EAED;
    public static final int[] BACKGROUND_COLOR_PRESETS = {
            0xFF111827,
            0xFF1E293B,
            0xFF0F3D3E,
            0xFF064E3B,
            0xFF1F3A5F,
            0xFF172554,
            0xFF312E41,
            0xFF3A3328
    };
    public static final int MIN_OVERLAY_SCALE_PERCENT           = 30;
    public static final int MAX_OVERLAY_SCALE_PERCENT           = 300;
    public static final int DEFAULT_OVERLAY_SCALE_PERCENT       = 200;

    // ═══════════════════════════════════════════════════════════════════════
    //  Static preference accessors
    // ═══════════════════════════════════════════════════════════════════════

    public static int getOverlayScalePercent(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return clampOverlayScalePercent(prefs.getInt(KEY_OVERLAY_SCALE_PERCENT, DEFAULT_OVERLAY_SCALE_PERCENT));
    }

    public static float getOverlayScale(Context context) {
        return getOverlayScalePercent(context) / 100f;
    }

    public static boolean isMainOverlayEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_MAIN_OVERLAY_ENABLED, false);
    }

    public static boolean isClusterMirrorEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_CLUSTER_MIRROR_ENABLED, false);
    }

    public static boolean isAutoStartEnabled(Context context) {
        return isBehaviorEnabled(context, KEY_AUTO_START_ENABLED);
    }

    public static boolean isStartServiceOnAppOpenEnabled(Context context) {
        return isBehaviorEnabled(context, KEY_START_SERVICE_ON_APP_OPEN);
    }

    public static boolean isLaunchTargetFromDesktopEnabled(Context context) {
        return isBehaviorEnabled(context, KEY_LAUNCH_TARGET_FROM_DESKTOP);
    }

    public static boolean isHideMainWhenTargetForegroundEnabled(Context context) {
        return isBehaviorEnabled(context, KEY_HIDE_MAIN_WHEN_TARGET_FOREGROUND);
    }

    public static boolean isShowMainWhenTargetForegroundEnabled(Context context) {
        return isBehaviorEnabled(context, KEY_SHOW_MAIN_WHEN_TARGET_FOREGROUND);
    }

    public static boolean isHideClusterWhenInactiveEnabled(Context context) {
        return isBehaviorEnabled(context, KEY_HIDE_CLUSTER_WHEN_INACTIVE);
    }

    public static boolean isOverspeedMildWarningEnabled(Context context) {
        return isBehaviorEnabled(context, KEY_OVERSPEED_MILD_WARNING);
    }

    public static boolean isOverspeedMediumWarningEnabled(Context context) {
        return isBehaviorEnabled(context, KEY_OVERSPEED_MEDIUM_WARNING);
    }

    public static boolean hasUsageStatsAccess(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) {
                return false;
            }
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int getClusterScalePercent(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return clampOverlayScalePercent(prefs.getInt(KEY_CLUSTER_SCALE_PERCENT, DEFAULT_OVERLAY_SCALE_PERCENT));
    }

    public static float getClusterScale(Context context) {
        return getClusterScalePercent(context) / 100f;
    }

    public static int getClusterDisplayId(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_CLUSTER_DISPLAY_ID, -1);
    }

    public static int getClusterX(Context context, int defaultValue) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_CLUSTER_X, defaultValue);
    }

    public static int getClusterY(Context context, int defaultValue) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_CLUSTER_Y, defaultValue);
    }

    public static boolean isModeVisible(Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_MODE);
    }

    public static boolean isTurnVisible(Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_TURN);
    }

    public static boolean isLaneVisible(Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_LANE);
    }

    public static boolean isLightVisible(Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_LIGHT);
    }

    public static boolean isServiceAreaVisible(Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_SERVICE_AREA);
    }

    public static boolean isEtaVisible(Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_ETA);
    }

    public static boolean isDestinationVisible(Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_DESTINATION);
    }

    public static boolean isDestinationSupportedByUiStyle(Context context) {
        String style = getOverlayUiStyle(context);
        return OVERLAY_UI_OLD.equals(style) || OVERLAY_UI_NEW.equals(style);
    }

    public static boolean shouldShowDestination(Context context) {
        return isDestinationVisible(context) && isDestinationSupportedByUiStyle(context);
    }

    public static boolean isAlertVisible(Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_ALERT);
    }

    public static boolean isDetailVisible(Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_DETAIL);
    }

    public static boolean isTmcBarVisible(Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_TMC_BAR);
    }

    public static boolean isTransparentBackground(Context context) {
        return getBackgroundOpacityPercent(context) <= MIN_BACKGROUND_OPACITY_PERCENT;
    }

    public static boolean isAutoTextMode(Context context) {
        return TEXT_MODE_AUTO.equals(getOverlayTextMode(context));
    }

    public static boolean isCardUiEnabled(Context context) {
        return OVERLAY_UI_CARD.equals(getOverlayUiStyle(context));
    }

    public static boolean isCardLikeOverlayUiEnabled(Context context) {
        return OverlayUiStyles.isCardLike(getOverlayUiStyle(context));
    }

    public static boolean isNewOverlayUiEnabled(Context context) {
        return isCardLikeOverlayUiEnabled(context);
    }

    public static boolean isDynamicIslandUiEnabled(Context context) {
        return OVERLAY_UI_DYNAMIC_ISLAND.equals(getOverlayUiStyle(context));
    }

    public static boolean usesDarkTextPalette(Context context) {
        if (!isAutoTextMode(context)) {
            return false;
        }
        // Opaque dark panels must retain light text for contrast. Day/night
        // following is useful when the map shows through a transparent panel.
        if (getBackgroundOpacityPercent(context) > 55) {
            return false;
        }
        int dayNightState = getDayNightState(context);
        return dayNightState != AmapConstants.MAP_STATE_NIGHT;
    }

    public static int getDayNightState(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_DAY_NIGHT_STATE, -1);
    }

    public static boolean updateDayNightState(Context context, int state) {
        if (state != AmapConstants.MAP_STATE_DAY && state != AmapConstants.MAP_STATE_NIGHT) {
            return false;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getInt(KEY_DAY_NIGHT_STATE, -1) == state) {
            return false;
        }
        prefs.edit().putInt(KEY_DAY_NIGHT_STATE, state).apply();
        return true;
    }

    public static boolean isCustomTextColorEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_CUSTOM_TEXT_COLOR_ENABLED, false);
    }

    public static int getTextColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return normalizeBackgroundColor(prefs.getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR));
    }

    public static int getBackgroundOpacityPercent(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_BACKGROUND_OPACITY_PERCENT)) {
            return clampBackgroundOpacityPercent(
                    prefs.getInt(KEY_BACKGROUND_OPACITY_PERCENT, DEFAULT_BACKGROUND_OPACITY_PERCENT));
        }
        return prefs.getBoolean(KEY_TRANSPARENT_BACKGROUND, false)
                ? MIN_BACKGROUND_OPACITY_PERCENT
                : DEFAULT_BACKGROUND_OPACITY_PERCENT;
    }

    public static int getBackgroundColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return normalizeBackgroundColor(prefs.getInt(KEY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR));
    }

    public static String getOverlayTextMode(Context context) {
        String mode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_TEXT_MODE, TEXT_MODE_AUTO);
        return TEXT_MODE_LIGHT.equals(mode) ? TEXT_MODE_LIGHT : TEXT_MODE_AUTO;
    }

    public static String getOverlayUiStyle(Context context) {
        String style = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_OVERLAY_UI_STYLE, OVERLAY_UI_OLD);
        return OverlayUiStyles.normalize(style);
    }

    public static boolean isOverlayContentEnabled(Context context, String key) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key, true);
    }

    static boolean isBehaviorEnabled(Context context, String key) {
        boolean defaultValue = KEY_AUTO_START_ENABLED.equals(key)
                || KEY_OVERSPEED_MILD_WARNING.equals(key)
                || KEY_OVERSPEED_MEDIUM_WARNING.equals(key);
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key, defaultValue);
    }

    static int clampOverlayScalePercent(int percent) {
        return Math.max(MIN_OVERLAY_SCALE_PERCENT, Math.min(MAX_OVERLAY_SCALE_PERCENT, percent));
    }

    static int clampBackgroundOpacityPercent(int percent) {
        return Math.max(MIN_BACKGROUND_OPACITY_PERCENT, Math.min(MAX_BACKGROUND_OPACITY_PERCENT, percent));
    }

    static int normalizeBackgroundColor(int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }

    public static int strokeOpacityForBackground(int opacityPercent) {
        return opacityPercent <= 0 ? 0 : Math.max(8, Math.round(opacityPercent * 0.18f));
    }

    static int withAlpha(int color, int alphaPercent) {
        int alpha = Math.max(0, Math.min(255, Math.round(clampBackgroundOpacityPercent(alphaPercent) * 255f / 100f)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static String getTargetPackage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String value = prefs.getString(KEY_TARGET_PACKAGE, DEFAULT_TARGET_PACKAGE);
        return value == null || value.length() == 0 ? DEFAULT_TARGET_PACKAGE : value;
    }
}
