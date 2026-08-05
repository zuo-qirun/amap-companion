package com.autonavi.companion;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts traffic light state from various AMap broadcast Bundle formats.
 * All methods are static and stateless — the caller owns the LightState map
 * and the inCruiseMode flag.
 */
public final class TrafficLightParser {

    private static final String TAG = "AmapCompanion";
    private static final Pattern CAMERA_LIGHT_PATTERN = Pattern.compile(
            "CameraLightInfo\\{([^}]*)\\}");

    private TrafficLightParser() {}

    // ── LightState ───────────────────────────────────────────────────────
    static final class LightState {
        int dir;
        int status;
        int seconds;
        int color;
        long updatedAt;
        long ttlMs;
    }

    // ── Result ───────────────────────────────────────────────────────────
    static final class Result {
        final HashMap<Integer, LightState> lights;
        final boolean changed;
        final boolean setInCruiseMode;

        Result(HashMap<Integer, LightState> lights, boolean changed, boolean setInCruiseMode) {
            this.lights = lights;
            this.changed = changed;
            this.setInCruiseMode = setInCruiseMode;
        }

        static final Result EMPTY = new Result(new HashMap<>(), false, false);
    }

    // ── Main entry point ─────────────────────────────────────────────────

    /**
     * Parse traffic light data from a broadcast Bundle.
     *
     * @param extras             the broadcast Bundle
     * @param inCruiseMode       current cruise mode state
     * @param navigationTurnDir  current navigation turn direction (-1 if unknown)
     * @param currentTurnIcon    current turn icon code (0 if unknown)
     * @param existingLights     existing traffic lights map (for TTL-aware replacement)
     * @return Result with parsed lights, changed flag, and cruise mode hint
     */
    static Result parse(Bundle extras, boolean inCruiseMode, int navigationTurnDir,
                        int currentTurnIcon, HashMap<Integer, LightState> existingLights) {
        int keyType = intValue(extras, "KEY_TYPE", -1);
        if (booleanValue(extras, "clearLights", false)
                || booleanValue(extras, "EXTRA_CLEAR_LIGHTS", false)
                || isExplicitEmptyPayload(extras)) {
            return new Result(new HashMap<>(), true, inCruiseMode);
        }
        if (keyType != AmapConstants.KEY_TYPE_TRAFFIC_LIGHT
                && intValue(extras, "TRAFFIC_LIGHT_NUM", -1) == 0
                && intValue(extras, "routeRemainTrafficLightNum", -1) == 0
                && !hasCountdownPayload(extras)) {
            return new Result(new HashMap<>(), true, inCruiseMode);
        }

        HashMap<Integer, LightState> nextLights = new HashMap<>();
        boolean setInCruiseMode = inCruiseMode;

        // 1) lightsData format (cruise mode)
        if (hasLightsDataPayload(extras)) {
            setInCruiseMode = true;
            if (updateLightsDataTrafficLights(extras, nextLights)) {
                return new Result(nextLights, true, true);
            } else if (!hasSingleLightPayload(extras)) {
                return new Result(new HashMap<>(), true, true);
            }
        }

        // 2) cruiseCamera format
        if (updateCruiseCameraTrafficLights(extras, nextLights)) {
            return new Result(nextLights, true, setInCruiseMode);
        }

        // 3) JSON format
        if (updateTrafficLightsFromJson(extras, nextLights)) {
            return new Result(nextLights, true, setInCruiseMode);
        }

        // 4) directional format
        if (updateDirectionalTrafficLights(extras, nextLights)) {
            return new Result(nextLights, true, setInCruiseMode);
        }

        // 5) generic array format
        int[] dirs = intArrayValue(extras, "dir", "DIR", "dirs", "DIRECTIONS", "direction",
                "directions", "trafficLightDir", "trafficLightDirs", "trafficLightDirection",
                "trafficLightDirections", "TRAFFIC_LIGHT_DIR", "TRAFFIC_LIGHT_DIRECTION");
        int[] statuses = intArrayValue(extras, "trafficLightStatus", "TRAFFIC_LIGHT_STATUS",
                "trafficLightStatuses", "traffic_light_status", "trafficLightState",
                "trafficLightStates", "TRAFFIC_LIGHT_STATE");
        int[] reds = intArrayValue(extras, "redLightCountDownSeconds", "redLightCountDownSecond",
                "redLightCountdownSeconds", "redSeconds", "redCountDown", "redCountdown",
                "RED_LIGHT_COUNT_DOWN_SECONDS", "red_light_count_down_seconds");
        int[] greens = intArrayValue(extras, "greenLightLastSecond", "greenLightCountDownSeconds",
                "greenLightCountdownSeconds", "greenSeconds", "greenCountDown", "greenCountdown",
                "GREEN_LIGHT_LAST_SECOND", "green_light_last_second");
        int count = Math.max(Math.max(lengthOf(dirs), lengthOf(statuses)),
                Math.max(lengthOf(reds), lengthOf(greens)));
        if (count == 0) {
            count = 1;
        }
        for (int i = 0; i < count; i++) {
            int dir = valueAt(dirs, i, intValue(extras, "dir", intValue(extras, "direction", -1)));
            int status = valueAt(statuses, i, intValue(extras, "trafficLightStatus", -1));
            int red = valueAt(reds, i, intValue(extras, "redLightCountDownSeconds", 0));
            int green = valueAt(greens, i, intValue(extras, "greenLightLastSecond", 0));
            int seconds = secondsForLight(status, red, green);
            int key = dir >= 0 ? dir : 999;
            if (seconds > 0) {
                putLightState(nextLights, key, dir, status, red, green, seconds,
                        setInCruiseMode, navigationTurnDir, currentTurnIcon);
            }
        }
        return new Result(nextLights, !nextLights.isEmpty(), setInCruiseMode);
    }

    // ── lightsData parsing ───────────────────────────────────────────────

    private static boolean hasLightsDataPayload(Bundle extras) {
        return extras.containsKey("lightsData") || extras.containsKey("LIGHTS_DATA");
    }

    private static boolean hasSingleLightPayload(Bundle extras) {
        return hasAny(extras, "trafficLightStatus", "TRAFFIC_LIGHT_STATUS", "traffic_light_status",
                "redLightCountDownSeconds", "redLightCountDownSecond", "redLightCountdownSeconds",
                "greenLightLastSecond", "greenLightCountDownSeconds", "greenLightCountdownSeconds",
                "dir", "direction", "trafficLightDir", "trafficLightDirection");
    }

    private static boolean updateLightsDataTrafficLights(Bundle extras,
                                                          HashMap<Integer, LightState> target) {
        Object value = safeExtra(extras, "lightsData");
        if (value == null) {
            value = safeExtra(extras, "LIGHTS_DATA");
        }
        return parseLightsDataValue(value, target);
    }

    private static boolean parseLightsDataValue(Object value, HashMap<Integer, LightState> target) {
        if (value == null) {
            return false;
        }
        if (value instanceof Bundle) {
            return parseLightsDataBundle((Bundle) value, target);
        }
        if (value instanceof Iterable) {
            boolean handled = false;
            for (Object item : (Iterable<?>) value) {
                handled |= parseLightsDataValue(item, target);
            }
            return handled;
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            boolean handled = false;
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                handled |= parseLightsDataValue(Array.get(value, i), target);
            }
            return handled;
        }
        return parseLightsDataText(String.valueOf(value), target);
    }

    private static boolean parseLightsDataBundle(Bundle bundle, HashMap<Integer, LightState> target) {
        return putLightsDataInfo(
                intValue(bundle, "dir", intValue(bundle, "direction", intValue(bundle, "c", -1))),
                intValue(bundle, "status", intValue(bundle, "trafficLightStatus", intValue(bundle, "d", -1))),
                intValue(bundle, "countdown", intValue(bundle, "countDown",
                        intValue(bundle, "redLightCountDownSeconds", intValue(bundle, "e", 0)))),
                intValue(bundle, "redLightCountDownSeconds", intValue(bundle, "redLightCountdownSeconds",
                        intValue(bundle, "redSeconds", intValue(bundle, "redCountDown", 0)))),
                intValue(bundle, "greenLightLastSecond", intValue(bundle, "greenLightCountDownSeconds",
                        intValue(bundle, "greenLightCountdownSeconds", intValue(bundle, "greenSeconds", 0)))),
                target);
    }

    private static boolean parseLightsDataText(String text, HashMap<Integer, LightState> target) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        try {
            String trimmed = text.trim();
            if (trimmed.startsWith("[")) {
                JSONArray array = new JSONArray(trimmed);
                boolean handled = false;
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item != null) {
                        handled |= parseLightsDataObject(item, target);
                    }
                }
                return handled;
            }
            if (trimmed.startsWith("{")) {
                return parseLightsDataObject(new JSONObject(trimmed), target);
            }
        } catch (Throwable t) {
            Log.d(TAG, "lightsData parse skipped: " + text);
        }
        return false;
    }

    private static boolean parseLightsDataObject(JSONObject object,
                                                  HashMap<Integer, LightState> target) {
        boolean handled = false;
        JSONArray array = object.optJSONArray("lightsData");
        if (array == null) {
            array = object.optJSONArray("lights");
        }
        if (array == null) {
            array = object.optJSONArray("trafficLights");
        }
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    handled |= parseLightsDataObject(item, target);
                }
            }
        }
        handled |= putLightsDataInfo(
                object.optInt("dir", object.optInt("direction", object.optInt("c", -1))),
                object.optInt("status", object.optInt("trafficLightStatus", object.optInt("d", -1))),
                object.optInt("countdown", object.optInt("countDown",
                        object.optInt("redLightCountDownSeconds", object.optInt("e", 0)))),
                object.optInt("redLightCountDownSeconds", object.optInt("redLightCountdownSeconds",
                        object.optInt("redSeconds", object.optInt("redCountDown", 0)))),
                object.optInt("greenLightLastSecond", object.optInt("greenLightCountDownSeconds",
                        object.optInt("greenLightCountdownSeconds", object.optInt("greenSeconds", 0)))),
                target);
        return handled;
    }

    private static boolean putLightsDataInfo(int rawDir, int rawStatus, int countDown,
                                              int red, int green,
                                              HashMap<Integer, LightState> target) {
        if (rawDir < 0) {
            return false;
        }
        if (countDown <= 0) {
            countDown = Math.max(red, green);
        }
        int dir = normalizeCruiseCameraDirection(rawDir);
        int status = normalizeCruiseCameraStatus(rawStatus);
        if (red <= 0 && green <= 0) {
            if (isGreenLightStatus(status)) {
                green = countDown;
            } else {
                red = countDown;
            }
        }
        int seconds = secondsForLight(status, red, green);
        if (seconds <= 0) {
            return false;
        }
        putLightState(target, dir >= 0 ? dir : 900 + target.size(), dir, status,
                red, green, seconds, true, -1, 0);
        Log.d(TAG, "lightsData light rawDir=" + rawDir + " rawStatus=" + rawStatus
                + " countdown=" + countDown + " red=" + red + " green=" + green
                + " => dir=" + dir + " status=" + status + " seconds=" + seconds);
        return true;
    }

    // ── cruiseCamera parsing ─────────────────────────────────────────────

    private static boolean updateCruiseCameraTrafficLights(Bundle extras,
                                                            HashMap<Integer, LightState> target) {
        boolean handled = false;
        for (String key : extras.keySet()) {
            Object value = safeExtra(extras, key);
            if (value == null) {
                continue;
            }
            String lowerKey = key == null ? "" : key.toLowerCase(java.util.Locale.US);
            if (lowerKey.contains("cameralight") || lowerKey.contains("camera_light")
                    || lowerKey.contains("lightinfos") || lowerKey.contains("light_infos")
                    || String.valueOf(value).contains("CameraLightInfo{")) {
                handled |= parseCameraLightValue(value, target);
            }
        }
        return handled;
    }

    private static boolean parseCameraLightValue(Object value, HashMap<Integer, LightState> target) {
        if (value == null) {
            return false;
        }
        if (value instanceof Bundle) {
            return parseCameraLightBundle((Bundle) value, target);
        }
        if (value instanceof Iterable) {
            boolean handled = false;
            for (Object item : (Iterable<?>) value) {
                handled |= parseCameraLightValue(item, target);
            }
            return handled;
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            boolean handled = false;
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                handled |= parseCameraLightValue(Array.get(value, i), target);
            }
            return handled;
        }
        return parseCameraLightText(String.valueOf(value), target);
    }

    private static boolean parseCameraLightBundle(Bundle bundle, HashMap<Integer, LightState> target) {
        boolean handled = putCameraLightInfo(
                intValue(bundle, "direction", intValue(bundle, "dir", intValue(bundle, "c", -1))),
                intValue(bundle, "status", intValue(bundle, "trafficLightStatus", intValue(bundle, "d", -1))),
                intValue(bundle, "countDown", intValue(bundle, "countdown",
                        intValue(bundle, "redLightCountDownSeconds", intValue(bundle, "e", 0)))),
                target);
        for (String key : bundle.keySet()) {
            Object value = safeExtra(bundle, key);
            if (value != null && value != bundle) {
                handled |= parseCameraLightValue(value, target);
            }
        }
        return handled;
    }

    private static boolean parseCameraLightText(String text, HashMap<Integer, LightState> target) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        boolean handled = false;
        Matcher matcher = CAMERA_LIGHT_PATTERN.matcher(text);
        while (matcher.find()) {
            handled |= parseCameraLightFields(matcher.group(1), target);
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                if (trimmed.startsWith("[")) {
                    JSONArray array = new JSONArray(trimmed);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.optJSONObject(i);
                        if (item != null) {
                            handled |= parseCameraLightObject(item, target);
                        }
                    }
                } else {
                    handled |= parseCameraLightObject(new JSONObject(trimmed), target);
                }
            } catch (Throwable t) {
                Log.d(TAG, "camera light json parse skipped: " + text);
            }
        }
        return handled;
    }

    private static boolean parseCameraLightFields(String fields, HashMap<Integer, LightState> target) {
        int dir = -1;
        int status = -1;
        int countDown = 0;
        String[] parts = fields.split(",");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            String name = pair[0].trim();
            int value = parseInt(pair[1].trim(), Integer.MIN_VALUE);
            if (value == Integer.MIN_VALUE) {
                continue;
            }
            if ("direction".equals(name) || "dir".equals(name) || "c".equals(name)) {
                dir = value;
            } else if ("status".equals(name) || "trafficLightStatus".equals(name) || "d".equals(name)) {
                status = value;
            } else if ("countDown".equals(name) || "countdown".equals(name)
                    || "redLightCountDownSeconds".equals(name) || "e".equals(name)) {
                countDown = value;
            }
        }
        return putCameraLightInfo(dir, status, countDown, target);
    }

    private static boolean parseCameraLightObject(JSONObject object,
                                                   HashMap<Integer, LightState> target) {
        boolean handled = false;
        JSONArray array = object.optJSONArray("cameraLightInfos");
        if (array == null) {
            array = object.optJSONArray("cameraLights");
        }
        if (array == null) {
            array = object.optJSONArray("trafficLights");
        }
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    handled |= parseCameraLightObject(item, target);
                }
            }
        }
        handled |= putCameraLightInfo(
                object.optInt("direction", object.optInt("dir", object.optInt("c", -1))),
                object.optInt("status", object.optInt("trafficLightStatus", object.optInt("d", -1))),
                object.optInt("countDown", object.optInt("countdown",
                        object.optInt("redLightCountDownSeconds", object.optInt("e", 0)))),
                target);
        return handled;
    }

    private static boolean putCameraLightInfo(int cameraDir, int cameraStatus, int countDown,
                                               HashMap<Integer, LightState> target) {
        if (cameraDir < 0 || countDown <= 0) {
            return false;
        }
        int dir = normalizeCruiseCameraDirection(cameraDir);
        int status = normalizeCruiseCameraStatus(cameraStatus);
        putLightState(target, dir >= 0 ? dir : 999, dir, status,
                isRedLightStatus(status) ? countDown : 0,
                isGreenLightStatus(status) ? countDown : 0, countDown,
                true, -1, 0);
        return true;
    }

    private static int normalizeCruiseCameraStatus(int cameraStatus) {
        if (cameraStatus == 0) {
            return 1;
        }
        if (cameraStatus == 1 || cameraStatus == 2 || cameraStatus == 4) {
            return 4;
        }
        return cameraStatus;
    }

    private static int normalizeCruiseCameraDirection(int cameraDir) {
        if (cameraDir == 0) {
            return 0;
        }
        if (cameraDir == 1) {
            return 1;
        }
        if (cameraDir == 2) {
            return 4;
        }
        if (cameraDir == 3) {
            return 2;
        }
        return cameraDir;
    }

    // ── JSON parsing ─────────────────────────────────────────────────────

    private static boolean updateTrafficLightsFromJson(Bundle extras,
                                                        HashMap<Integer, LightState> target) {
        boolean handled = false;
        for (String key : extras.keySet()) {
            Object value = safeExtra(extras, key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value);
            if (TextUtils.isEmpty(text) || !looksLikeTrafficLightPayload(key, text)) {
                continue;
            }
            handled |= parseTrafficLightPayload(text, target);
        }
        return handled;
    }

    private static boolean looksLikeTrafficLightPayload(String key, String text) {
        String lowerKey = key == null ? "" : key.toLowerCase(java.util.Locale.US);
        String lowerText = text.toLowerCase(java.util.Locale.US);
        return lowerKey.contains("trafficlight") || lowerKey.contains("traffic_light")
                || lowerKey.contains("redlight") || lowerKey.contains("greenlight")
                || lowerKey.contains("lightsdata")
                || lowerText.contains("redlightcountdownseconds")
                || lowerText.contains("greenlightlastsecond")
                || lowerText.contains("trafficlightstatus")
                || lowerText.contains("\"countdown\"")
                || lowerText.contains("\"showtype\"");
    }

    private static boolean parseTrafficLightPayload(String text, HashMap<Integer, LightState> target) {
        try {
            String trimmed = text.trim();
            if (trimmed.startsWith("[")) {
                JSONArray array = new JSONArray(trimmed);
                boolean handled = false;
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item != null) {
                        handled |= parseTrafficLightObject(item, target);
                    }
                }
                return handled;
            }
            if (trimmed.startsWith("{")) {
                return parseTrafficLightObject(new JSONObject(trimmed), target);
            }
        } catch (Throwable t) {
            Log.d(TAG, "traffic light payload parse skipped: " + text);
        }
        return false;
    }

    private static boolean parseTrafficLightObject(JSONObject object,
                                                    HashMap<Integer, LightState> target) {
        boolean handled = false;
        JSONArray lights = object.optJSONArray("trafficLights");
        if (lights == null) {
            lights = object.optJSONArray("trafficLight");
        }
        if (lights == null) {
            lights = object.optJSONArray("lights");
        }
        if (lights != null) {
            for (int i = 0; i < lights.length(); i++) {
                JSONObject item = lights.optJSONObject(i);
                if (item != null) {
                    handled |= parseTrafficLightObject(item, target);
                }
            }
        }
        int dir = object.optInt("dir", object.optInt("direction",
                object.optInt("trafficLightDir", object.optInt("trafficLightDirection", -1))));
        int status = object.optInt("trafficLightStatus", object.optInt("status",
                object.optInt("trafficLightState", -1)));
        int red = object.optInt("redLightCountDownSeconds", object.optInt("redLightCountdownSeconds",
                object.optInt("redSeconds", object.optInt("redCountDown", 0))));
        int green = object.optInt("greenLightLastSecond", object.optInt("greenLightCountDownSeconds",
                object.optInt("greenLightCountdownSeconds", object.optInt("greenSeconds", 0))));
        int seconds = secondsForLight(status, red, green);
        if (seconds > 0) {
            int key = dir >= 0 ? dir : 999;
            putLightState(target, key, dir, status, red, green, seconds, false, -1, 0);
            handled = true;
        }
        return handled;
    }

    // ── directional parsing ──────────────────────────────────────────────

    private static boolean updateDirectionalTrafficLights(Bundle extras,
                                                          HashMap<Integer, LightState> target) {
        boolean handled = false;
        handled |= putDirectionalLight(target, extras, 1, "leftRedLightCountDownSeconds",
                "LEFT_RED_LIGHT_COUNT_DOWN_SECONDS", "leftRedSeconds");
        handled |= putDirectionalLight(target, extras, 4, "straightRedLightCountDownSeconds",
                "STRAIGHT_RED_LIGHT_COUNT_DOWN_SECONDS", "straightRedSeconds", "frontRedSeconds");
        handled |= putDirectionalLight(target, extras, 2, "rightRedLightCountDownSeconds",
                "RIGHT_RED_LIGHT_COUNT_DOWN_SECONDS", "rightRedSeconds");
        handled |= putDirectionalLight(target, extras, 1, "leftGreenLightLastSecond",
                "LEFT_GREEN_LIGHT_LAST_SECOND", "leftGreenSeconds");
        handled |= putDirectionalLight(target, extras, 4, "straightGreenLightLastSecond",
                "STRAIGHT_GREEN_LIGHT_LAST_SECOND", "straightGreenSeconds", "frontGreenSeconds");
        handled |= putDirectionalLight(target, extras, 2, "rightGreenLightLastSecond",
                "RIGHT_GREEN_LIGHT_LAST_SECOND", "rightGreenSeconds");
        return handled;
    }

    private static boolean putDirectionalLight(HashMap<Integer, LightState> target, Bundle extras,
                                                int dir, String... keys) {
        int seconds = intValue(extras, keys[0], 0);
        for (int i = 1; i < keys.length && seconds <= 0; i++) {
            seconds = intValue(extras, keys[i], 0);
        }
        if (seconds <= 0) {
            return false;
        }
        int status = keys[0].toLowerCase(java.util.Locale.US).contains("green") ? 4 : 1;
        putLightState(target, dir >= 0 ? dir : 999, dir, status, status == 1 ? seconds : 0,
                status == 4 ? seconds : 0, seconds, false, -1, 0);
        return true;
    }

    // ── LightState construction ──────────────────────────────────────────

    static void putLightState(HashMap<Integer, LightState> target, int key, int dir,
                               int status, int red, int green, int seconds,
                               boolean inCruiseMode, int navigationTurnDir, int currentTurnIcon) {
        LightState state = new LightState();
        state.dir = normalizeLightDirectionForDisplay(dir, status, red, green,
                inCruiseMode, navigationTurnDir, currentTurnIcon);
        state.status = status;
        state.seconds = seconds;
        state.color = colorForStatus(status, red, green);
        state.updatedAt = System.currentTimeMillis();
        state.ttlMs = inCruiseMode ? seconds * 1000L + 2000L : 4500L;
        LightState old = target.get(key);
        if (old == null || preferLightState(state, old)) {
            target.put(key, state);
        }
    }

    // ── Traffic light status utilities ────────────────────────────────────

    static int colorForStatus(int status, int red, int green) {
        if (isYellowTailCountdown(red, green)) {
            return AmapConstants.COLOR_YELLOW;
        }
        if (isYellowLightStatus(status)) {
            return AmapConstants.COLOR_YELLOW;
        }
        if (isGreenLightStatus(status)) {
            return AmapConstants.COLOR_GREEN;
        }
        if (isRedLightStatus(status)) {
            return AmapConstants.COLOR_RED;
        }
        if (green > 0) {
            return AmapConstants.COLOR_GREEN;
        }
        return AmapConstants.COLOR_YELLOW;
    }

    static int secondsForLight(int status, int red, int green) {
        if (isGreenLightStatus(status)) {
            return red > 0 ? red : green;
        }
        if (isRedLightStatus(status)) {
            return red > 0 ? red : green;
        }
        if (isYellowLightStatus(status)) {
            return red > 0 ? red : green;
        }
        return red > 0 ? red : green;
    }

    static boolean isRedLightStatus(int status) {
        return status == AmapConstants.LIGHT_STATUS_RED;
    }

    static boolean isGreenLightStatus(int status) {
        return status == AmapConstants.LIGHT_STATUS_GREEN;
    }

    static boolean isYellowLightStatus(int status) {
        return status == AmapConstants.LIGHT_STATUS_YELLOW_0
                || status == AmapConstants.LIGHT_STATUS_YELLOW_2
                || status == AmapConstants.LIGHT_STATUS_YELLOW_3
                || status == AmapConstants.LIGHT_STATUS_YELLOW_5
                || status == AmapConstants.LIGHT_STATUS_YELLOW_6;
    }

    private static boolean isYellowTailCountdown(int red, int green) {
        return (green == 3 && red == 0) || (green > 0 && green < 3);
    }

    static boolean isDisplayedYellowLight(int status, int red, int green) {
        return colorForStatus(status, red, green) == AmapConstants.COLOR_YELLOW;
    }

    static int currentLightSeconds(LightState state, long now) {
        if (state == null) {
            return 0;
        }
        long elapsedSeconds = Math.max(0L, (now - state.updatedAt) / 1000L);
        return Math.max(0, state.seconds - (int) elapsedSeconds);
    }

    static String directionLabel(int dir) {
        if (dir == AmapConstants.DIR_UTURN) {
            return "↶";
        }
        if (dir == AmapConstants.DIR_LEFT) {
            return "←";
        }
        if (dir == AmapConstants.DIR_RIGHT || dir == AmapConstants.DIR_RIGHT_ALT) {
            return "→";
        }
        if (dir == AmapConstants.DIR_STRAIGHT) {
            return "↑";
        }
        if (dir == AmapConstants.DIR_DIAGONAL_LEFT_1 || dir == AmapConstants.DIR_DIAGONAL_LEFT_2) {
            return "↖";
        }
        if (dir == AmapConstants.DIR_DIAGONAL_RIGHT_1 || dir == AmapConstants.DIR_DIAGONAL_RIGHT_2) {
            return "↗";
        }
        return dir >= 0 ? ("D" + dir) : "前";
    }

    static int directionPriority(int dir, boolean inCruiseMode) {
        if (inCruiseMode) {
            if (dir == AmapConstants.DIR_LEFT || dir == AmapConstants.DIR_DIAGONAL_LEFT_1
                    || dir == AmapConstants.DIR_DIAGONAL_LEFT_2) {
                return 10;
            }
            if (dir == AmapConstants.DIR_STRAIGHT) {
                return 20;
            }
            if (dir == AmapConstants.DIR_RIGHT || dir == AmapConstants.DIR_RIGHT_ALT
                    || dir == AmapConstants.DIR_DIAGONAL_RIGHT_1
                    || dir == AmapConstants.DIR_DIAGONAL_RIGHT_2) {
                return 30;
            }
            if (dir == AmapConstants.DIR_UTURN) {
                return 40;
            }
        }
        return 99;
    }

    private static int normalizeLightDirectionForDisplay(int dir, int status, int red, int green,
                                                          boolean inCruiseMode,
                                                          int navigationTurnDir,
                                                          int currentTurnIcon) {
        if (!inCruiseMode && isDisplayedYellowLight(status, red, green)) {
            return -1;
        }
        int effectiveDir = navigationTurnDir >= 0 ? navigationTurnDir
                : (currentTurnIcon > 0 ? turnIconToTrafficDir(currentTurnIcon) : -1);
        if (!inCruiseMode && dir == AmapConstants.DIR_RIGHT_ALT && effectiveDir == AmapConstants.DIR_UTURN) {
            return AmapConstants.DIR_UTURN;
        }
        if (!inCruiseMode && dir == AmapConstants.DIR_UTURN) {
            return AmapConstants.DIR_STRAIGHT;
        }
        return dir;
    }

    private static int turnIconToTrafficDir(int icon) {
        if (icon == 2 || icon == 4 || icon == 6) {
            return AmapConstants.DIR_LEFT;
        }
        if (icon == 3 || icon == 5 || icon == 7 || icon == 19) {
            return AmapConstants.DIR_RIGHT;
        }
        if (icon == 8) {
            return AmapConstants.DIR_UTURN;
        }
        return AmapConstants.DIR_STRAIGHT;
    }

    private static boolean preferLightState(LightState candidate, LightState old) {
        if (isRedLightStatus(old.status) && !isRedLightStatus(candidate.status)) {
            return false;
        }
        if (isRedLightStatus(candidate.status) && !isRedLightStatus(old.status)) {
            return true;
        }
        return candidate.seconds > 0;
    }

    // ── Bundle utility methods (duplicated from OverlayService) ──────────

    static boolean hasAny(Bundle extras, String... keys) {
        for (String key : keys) {
            if (extras.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasCountdownPayload(Bundle extras) {
        return hasAny(extras, "trafficLightStatus", "TRAFFIC_LIGHT_STATUS", "traffic_light_status",
                "redLightCountDownSeconds", "redLightCountDownSecond", "redLightCountdownSeconds",
                "redSeconds", "redCountDown", "redCountdown", "RED_LIGHT_COUNT_DOWN_SECONDS",
                "greenLightLastSecond", "greenLightCountDownSeconds", "greenLightCountdownSeconds",
                "greenSeconds", "greenCountDown", "greenCountdown", "GREEN_LIGHT_LAST_SECOND",
                "leftRedLightCountDownSeconds", "straightRedLightCountDownSeconds",
                "rightRedLightCountDownSeconds",
                "dir", "direction", "trafficLightDir", "trafficLightDirection", "trafficLights",
                "trafficLight", "trafficLightInfo", "trafficLightsCountdownInfo", "lightsData",
                "LIGHTS_DATA", "cameraLightInfo",
                "cameraLightInfos", "cameraLightInfoWrapper", "cameraLights", "lightInfos");
    }

    static boolean hasTrafficLightPayload(Bundle extras) {
        if (extras == null) {
            return false;
        }
        return intValue(extras, "KEY_TYPE", -1) == AmapConstants.KEY_TYPE_TRAFFIC_LIGHT
                || hasCountdownPayload(extras)
                || hasAny(extras, "lightsCount", "LIGHTS_COUNT",
                "clearLights", "EXTRA_CLEAR_LIGHTS");
    }

    private static boolean isExplicitEmptyPayload(Bundle extras) {
        int count = intValue(extras, "lightsCount",
                intValue(extras, "LIGHTS_COUNT", -1));
        return count == 0 && !hasSingleLightPayload(extras);
    }

    private static Object safeExtra(Bundle extras, String key) {
        try {
            return extras.get(key);
        } catch (Throwable t) {
            Log.d(TAG, "skip unreadable extra " + key, t);
            return null;
        }
    }

    private static int intValue(Bundle extras, String key, int fallback) {
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

    private static int[] intArrayValue(Bundle extras, String... keys) {
        for (String key : keys) {
            Object value = safeExtra(extras, key);
            int[] parsed = parseIntArray(value);
            if (parsed != null && parsed.length > 0) {
                return parsed;
            }
        }
        return null;
    }

    private static int lengthOf(int[] values) {
        return values == null ? 0 : values.length;
    }

    private static int valueAt(int[] values, int index, int fallback) {
        if (values == null || values.length == 0) {
            return fallback;
        }
        if (index < values.length) {
            return values[index];
        }
        return values[values.length - 1];
    }

    private static boolean booleanValue(Bundle extras, String key, boolean fallback) {
        Object value = safeExtra(extras, key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String s = String.valueOf(value);
        if ("1".equals(s) || "true".equalsIgnoreCase(s) || "是".equals(s)) {
            return true;
        }
        if ("0".equals(s) || "false".equalsIgnoreCase(s) || "否".equals(s)) {
            return false;
        }
        return fallback;
    }

    private static int[] parseIntArray(Object value) {
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
                out[i] = item instanceof Number ? ((Number) item).intValue()
                        : parseInt(String.valueOf(item), 1);
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

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
