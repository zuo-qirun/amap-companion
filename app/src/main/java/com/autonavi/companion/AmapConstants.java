package com.autonavi.companion;

/**
 * Constants for the AMap Auto broadcast protocol.
 * Extracted from OverlayService to eliminate magic numbers.
 */
public final class AmapConstants {

    private AmapConstants() {}

    // ── KeyType values (broadcast Bundle key "keyType") ──────────────────
    public static final int KEY_TYPE_NAVIGATION_STATE  = 10019;
    public static final int KEY_TYPE_ROUTE_GUIDANCE    = 10001;
    public static final int KEY_TYPE_CRUISE            = 60021;
    public static final int KEY_TYPE_LANE_INFO         = 13012;
    public static final int KEY_TYPE_TRAFFIC_LIGHT     = 60073;
    public static final int KEY_TYPE_CLUSTER_ACTIVATE  = 13014;
    public static final int KEY_TYPE_REQUEST_LANE      = 10062;

    // ── Navigation state values (Bundle key "state") ─────────────────────
    public static final int NAV_STATE_SIM_NAV     = 5;
    public static final int NAV_STATE_6           = 6;
    public static final int NAV_STATE_NAVIGATING  = 8;
    public static final int NAV_STATE_NAV_EXIT    = 9;
    public static final int NAV_STATE_10          = 10;
    public static final int NAV_STATE_11          = 11;
    public static final int NAV_STATE_12          = 12;
    public static final int NAV_STATE_CRUISE      = 24;
    public static final int NAV_STATE_CRUISE_EXIT = 25;
    public static final int MAP_STATE_DAY          = 37;
    public static final int MAP_STATE_NIGHT        = 38;
    public static final int APP_STATE_FOREGROUND  = 3;
    public static final int APP_STATE_BACKGROUND  = 4;

    // ── Traffic light status values ──────────────────────────────────────
    public static final int LIGHT_STATUS_YELLOW_0 = 0;
    public static final int LIGHT_STATUS_RED      = 1;
    public static final int LIGHT_STATUS_YELLOW_2 = 2;
    public static final int LIGHT_STATUS_YELLOW_3 = 3;
    public static final int LIGHT_STATUS_GREEN    = 4;
    public static final int LIGHT_STATUS_YELLOW_5 = 5;
    public static final int LIGHT_STATUS_YELLOW_6 = 6;

    // ── Direction codes (Bundle key "dir") ───────────────────────────────
    public static final int DIR_UTURN         = 0;
    public static final int DIR_LEFT          = 1;
    public static final int DIR_RIGHT         = 2;
    public static final int DIR_RIGHT_ALT     = 3;
    public static final int DIR_STRAIGHT      = 4;
    public static final int DIR_DIAGONAL_LEFT_1  = 5;
    public static final int DIR_DIAGONAL_LEFT_2  = 6;
    public static final int DIR_DIAGONAL_RIGHT_1 = 7;
    public static final int DIR_DIAGONAL_RIGHT_2 = 8;
    public static final int DIR_UNSPECIFIED   = 999;

    // ── Route type values ────────────────────────────────────────────────
    public static final int ROUTE_TYPE_SIMULATED = 1;
    public static final int ROUTE_TYPE_CRUISE    = 2;

    // ── Traffic light colors ─────────────────────────────────────────────
    public static final int COLOR_RED    = 0xFFC62828;
    public static final int COLOR_GREEN  = 0xFF1F8F45;
    public static final int COLOR_YELLOW = 0xFFD49A00;
}
