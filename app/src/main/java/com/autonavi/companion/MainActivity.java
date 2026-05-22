package com.autonavi.companion;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.display.DisplayManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    static final String PREFS = "amap_companion";
    static final String KEY_TARGET_PACKAGE = "target_package";
    static final String KEY_UPDATE_URL = "update_url";
    static final String KEY_UPDATE_CHANNEL = "update_channel";
    static final String KEY_OVERLAY_SCALE_PERCENT = "overlay_scale_percent";
    static final String KEY_MAIN_OVERLAY_ENABLED = "main_overlay_enabled";
    static final String KEY_CLUSTER_MIRROR_ENABLED = "cluster_mirror_enabled";
    static final String KEY_OVERLAY_X = "overlay_x";
    static final String KEY_OVERLAY_Y = "overlay_y";
    static final String KEY_CLUSTER_X = "cluster_x";
    static final String KEY_CLUSTER_Y = "cluster_y";
    static final String KEY_CLUSTER_SCALE_PERCENT = "cluster_scale_percent";
    static final String KEY_CLUSTER_DISPLAY_ID = "cluster_display_id";
    static final String KEY_SHOW_MODE = "show_mode";
    static final String KEY_SHOW_TURN = "show_turn";
    static final String KEY_SHOW_LANE = "show_lane";
    static final String KEY_SHOW_LIGHT = "show_light";
    static final String KEY_SHOW_ETA = "show_eta";
    static final String KEY_SHOW_ALERT = "show_alert";
    static final String KEY_SHOW_DETAIL = "show_detail";
    static final String KEY_TRANSPARENT_BACKGROUND = "transparent_background";
    static final String KEY_BACKGROUND_OPACITY_PERCENT = "background_opacity_percent";
    static final String KEY_TEXT_MODE = "text_mode";
    static final String KEY_OVERLAY_UI_STYLE = "overlay_ui_style";
    static final String KEY_AUTO_START_ENABLED = "auto_start_enabled";
    static final String KEY_HIDE_MAIN_WHEN_TARGET_FOREGROUND = "hide_main_when_target_foreground";
    static final String KEY_HIDE_CLUSTER_WHEN_INACTIVE = "hide_cluster_when_inactive";
    static final String ACTION_MAIN_OVERLAY_CHANGED = "com.autonavi.companion.MAIN_OVERLAY_CHANGED";
    static final String ACTION_OVERLAY_SCALE_CHANGED = "com.autonavi.companion.OVERLAY_SCALE_CHANGED";
    static final String ACTION_CLUSTER_MIRROR_CHANGED = "com.autonavi.companion.CLUSTER_MIRROR_CHANGED";
    static final String ACTION_OVERLAY_CONTENT_CHANGED = "com.autonavi.companion.OVERLAY_CONTENT_CHANGED";
    static final String ACTION_OVERLAY_STYLE_CHANGED = "com.autonavi.companion.OVERLAY_STYLE_CHANGED";
    static final String ACTION_DISPLAY_POLICY_CHANGED = "com.autonavi.companion.DISPLAY_POLICY_CHANGED";
    static final String DEFAULT_TARGET_PACKAGE = "com.autonavi.amapClone";
    static final String UPDATE_CHANNEL_SERVER = "server";
    static final String UPDATE_CHANNEL_GITHUB = "github";
    static final String DEFAULT_UPDATE_CHANNEL = UPDATE_CHANNEL_SERVER;
    static final String SERVER_UPDATE_URL = "https://amap-companion.zuoqirun.top/update.json";
    static final String GITHUB_UPDATE_URL = "https://amap-companion.zuoqirun.top/update-github.json";
    static final String REPOSITORY_URL = "https://github.com/zuo-qirun/amap-companion";
    static final String LICENSE_URL = "https://github.com/zuo-qirun/amap-companion/blob/master/LICENSE";
    static final String CUSTOM_MAP_SKILL_URL = "https://github.com/zuo-qirun/amap-cruise-wrapper-skill";
    static final String CUSTOM_MAP_APK_URL = "https://github.com/zuo-qirun/amap-cruise-wrapper-skill/releases/download/v20260519-cruise-wrapper/_9.1.0.600087_cruise_lightsdata_clear_signed.apk";
    static final String CUSTOM_MAP_SKILL_MIRROR_URL = "https://gh-proxy.com/https://github.com/zuo-qirun/amap-cruise-wrapper-skill/archive/refs/heads/master.zip";
    static final String CUSTOM_MAP_APK_MIRROR_URL = "https://gh.llkk.cc/https://github.com/zuo-qirun/amap-cruise-wrapper-skill/releases/download/v20260519-cruise-wrapper/_9.1.0.600087_cruise_lightsdata_clear_signed.apk";
    static final String DEFAULT_UPDATE_URL = SERVER_UPDATE_URL;
    static final String TEXT_MODE_LIGHT = "light";
    static final String TEXT_MODE_AUTO = "auto";
    static final String OVERLAY_UI_OLD = "old";
    static final String OVERLAY_UI_NEW = "new";
    static final int MIN_BACKGROUND_OPACITY_PERCENT = 0;
    static final int MAX_BACKGROUND_OPACITY_PERCENT = 90;
    static final int DEFAULT_BACKGROUND_OPACITY_PERCENT = 90;
    static final int MIN_OVERLAY_SCALE_PERCENT = 80;
    static final int MAX_OVERLAY_SCALE_PERCENT = 300;
    static final int DEFAULT_OVERLAY_SCALE_PERCENT = 200;
    private static final String TARGET_PACKAGE_PREFIX = "com.autonavi.";
    private static final int REQUEST_LOG_PERMISSIONS = 7001;

    private TextView targetText;
    private TextView updateText;
    private TextView overlayScaleText;
    private TextView clusterScaleText;
    private TextView clusterDisplayText;
    private TextView overlayBackgroundOpacityText;
    private FrameLayout overlayPreviewStage;
    private LinearLayout overlayPreviewPanel;
    private Button overlayTextModeButton;
    private Button overlayUiStyleButton;
    private TextView previewModeText;
    private TextView previewTurnText;
    private LinearLayout previewLightRow;
    private LinearLayout previewLaneSection;
    private TextView previewEtaText;
    private TextView previewAlertText;
    private TextView previewDetailText;
    private TextView previewLaneTitleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        persistDefaultUpdateUrl();
        migrateOverlayStylePrefs();
        setContentView(buildContent());
        startOverlayService();
        targetText.postDelayed(() -> {
            checkForUpdates(false);
        }, 2000L);
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFFF3F6FA);
        boolean wideLayout = isWideLayout();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout hero = card(0xFF111827);
        root.addView(hero, new LinearLayout.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("AMap Companion");
        title.setTextSize(28f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        hero.addView(title, new LinearLayout.LayoutParams(-1, -2));

        targetText = new TextView(this);
        targetText.setTextSize(14f);
        targetText.setTextColor(0xFFD1D5DB);
        targetText.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams targetLp = new LinearLayout.LayoutParams(-1, -2);
        targetLp.setMargins(0, dp(8), 0, 0);
        hero.addView(targetText, targetLp);
        updateTargetText();

        updateText = new TextView(this);
        updateText.setTextSize(13f);
        updateText.setTextColor(0xFFA7F3D0);
        updateText.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams updateLp = new LinearLayout.LayoutParams(-1, -2);
        updateLp.setMargins(0, dp(8), 0, 0);
        hero.addView(updateText, updateLp);
        updateUpdateText("\u66f4\u65b0\u6e20\u9053\n" + displayUpdateUrl());

        addAnnouncementSection(root);

        LinearLayout contentArea = new LinearLayout(this);
        contentArea.setOrientation(wideLayout ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(-1, -2);
        contentLp.setMargins(0, dp(14), 0, 0);
        root.addView(contentArea, contentLp);

        LinearLayout leftColumn = new LinearLayout(this);
        leftColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(-1, -2);
        if (wideLayout) {
            leftLp = new LinearLayout.LayoutParams(0, -2, 1f);
            leftLp.setMargins(0, 0, dp(7), 0);
        }
        contentArea.addView(leftColumn, leftLp);

        LinearLayout rightColumn = new LinearLayout(this);
        rightColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(-1, -2);
        if (wideLayout) {
            rightLp = new LinearLayout.LayoutParams(0, -2, 1f);
            rightLp.setMargins(dp(7), 0, 0, 0);
        } else {
            rightLp.setMargins(0, dp(14), 0, 0);
        }
        contentArea.addView(rightColumn, rightLp);

        LinearLayout actions = card(Color.WHITE);
        leftColumn.addView(actions, new LinearLayout.LayoutParams(-1, -2));
        addActionButtons(actions, wideLayout);

        LinearLayout settings = card(Color.WHITE);
        rightColumn.addView(settings, new LinearLayout.LayoutParams(-1, -2));
        addOverlayScaleControls(settings);
        addClusterMirrorControls(settings);
        addOverlayContentControls(settings);
        addBehaviorControls(settings);
        addOpenSourceSection(wideLayout ? leftColumn : rightColumn, wideLayout);

        return scroll;
    }

    private void addActionButtons(LinearLayout parent, boolean wideLayout) {
        if (wideLayout) {
            addButtonPair(parent,
                    button("\u9009\u62e9\u76ee\u6807\u5e94\u7528", v -> chooseTargetApp(), 0xFF2563EB),
                    button("\u6388\u6743\u60ac\u6d6e\u7a97", v -> requestOverlayPermission(), 0xFF475569));
            addButtonPair(parent,
                    button("\u542f\u52a8\u60ac\u6d6e\u7a97", v -> enableMainOverlay(), 0xFF0F766E),
                    button("\u5173\u95ed\u60ac\u6d6e\u7a97", v -> stopOverlayService(), 0xFFB45309));
            addButtonPair(parent,
                    button(clusterMirrorButtonText(), v -> toggleClusterMirror((Button) v), 0xFF7C3AED),
                    button("\u6253\u5f00\u76ee\u6807\u5e94\u7528", v -> openTargetApp(), 0xFF111827));
            addButtonPair(parent,
                    button("\u9009\u62e9\u4e0b\u8f7d\u6e20\u9053", v -> chooseUpdateChannel(), 0xFF334155),
                    button("\u68c0\u67e5\u66f4\u65b0", v -> checkForUpdates(true), 0xFF059669));
            addButtonPair(parent,
                    button("\u67e5\u770b/\u4fdd\u5b58\u65e5\u5fd7", v -> showLogcatDialog(), 0xFF4F46E5),
                    null);
            return;
        }
        parent.addView(button("\u9009\u62e9\u76ee\u6807\u5e94\u7528", v -> chooseTargetApp(), 0xFF2563EB));
        parent.addView(button("\u6388\u6743\u60ac\u6d6e\u7a97", v -> requestOverlayPermission(), 0xFF475569));
        parent.addView(button("\u542f\u52a8\u60ac\u6d6e\u7a97", v -> enableMainOverlay(), 0xFF0F766E));
        parent.addView(button("\u5173\u95ed\u60ac\u6d6e\u7a97", v -> stopOverlayService(), 0xFFB45309));
        parent.addView(button(clusterMirrorButtonText(), v -> toggleClusterMirror((Button) v), 0xFF7C3AED));
        parent.addView(button("\u6253\u5f00\u76ee\u6807\u5e94\u7528", v -> openTargetApp(), 0xFF111827));
        parent.addView(button("\u9009\u62e9\u4e0b\u8f7d\u6e20\u9053", v -> chooseUpdateChannel(), 0xFF334155));
        parent.addView(button("\u68c0\u67e5\u66f4\u65b0", v -> checkForUpdates(true), 0xFF059669));
        parent.addView(button("\u67e5\u770b/\u4fdd\u5b58\u65e5\u5fd7", v -> showLogcatDialog(), 0xFF4F46E5));
    }

    private void addAnnouncementSection(LinearLayout root) {
        LinearLayout section = card(Color.WHITE);
        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(-1, -2);
        sectionLp.setMargins(0, dp(14), 0, 0);
        root.addView(section, sectionLp);

        TextView title = new TextView(this);
        title.setText("\u516c\u544a");
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF111827);
        section.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView body = new TextView(this);
        body.setText("\u53cd\u9988/\u4ea4\u6d41\u7fa4 QQ\u7fa4\uff1a1106923186");
        body.setTextSize(14f);
        body.setTextColor(0xFF334155);
        body.setTextIsSelectable(true);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(-1, -2);
        bodyLp.setMargins(0, dp(8), 0, 0);
        section.addView(body, bodyLp);
    }

    private void addOpenSourceSection(LinearLayout root, boolean compactTopMargin) {
        LinearLayout section = card(Color.WHITE);
        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(-1, -2);
        sectionLp.setMargins(0, compactTopMargin ? dp(10) : dp(14), 0, 0);
        root.addView(section, sectionLp);

        TextView title = new TextView(this);
        title.setText("\u5f00\u6e90\u4fe1\u606f");
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF111827);
        section.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView repo = new TextView(this);
        repo.setText("\u5f00\u6e90\u5730\u5740\n" + REPOSITORY_URL);
        repo.setTextSize(13f);
        repo.setTextColor(0xFF334155);
        repo.setLineSpacing(dp(2), 1.0f);
        repo.setTextIsSelectable(true);
        LinearLayout.LayoutParams repoLp = new LinearLayout.LayoutParams(-1, -2);
        repoLp.setMargins(0, dp(8), 0, 0);
        section.addView(repo, repoLp);

        TextView license = new TextView(this);
        license.setText("\u5f00\u6e90\u8bb8\u53ef\u8bc1\nGNU GPL v3.0\n\u672c\u9879\u76ee\u6309 GPL v3.0 \u5f00\u6e90\u53d1\u5e03\uff0c\u53ef\u4ee5\u4f7f\u7528\u3001\u4fee\u6539\u548c\u5206\u53d1\uff0c\u4f46\u5206\u53d1\u4fee\u6539\u7248\u65f6\u9700\u7ee7\u7eed\u4ee5\u76f8\u540c\u8bb8\u53ef\u8bc1\u5f00\u6e90\uff0c\u5e76\u9644\u4e0a\u539f\u59cb\u8bb8\u53ef\u8bc1\u6587\u672c\u3002");
        license.setTextSize(13f);
        license.setTextColor(0xFF334155);
        license.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams licenseLp = new LinearLayout.LayoutParams(-1, -2);
        licenseLp.setMargins(0, dp(10), 0, 0);
        section.addView(license, licenseLp);

        TextView customMap = new TextView(this);
        customMap.setText("\u5de1\u822a\u7ea2\u7eff\u706f\u5b9a\u5236\u5730\u56fe\n\u5de1\u822a\u5de6\u8f6c/\u76f4\u884c\u591a\u65b9\u5411\u5012\u8ba1\u65f6\u9700\u914d\u5408\u5b9a\u5236\u9ad8\u5fb7\u5730\u56fe\uff1a\nGitHub: " + CUSTOM_MAP_SKILL_URL + "\n\u955c\u50cf ZIP: " + CUSTOM_MAP_SKILL_MIRROR_URL);
        customMap.setTextSize(13f);
        customMap.setTextColor(0xFF334155);
        customMap.setLineSpacing(dp(2), 1.0f);
        customMap.setTextIsSelectable(true);
        LinearLayout.LayoutParams customMapLp = new LinearLayout.LayoutParams(-1, -2);
        customMapLp.setMargins(0, dp(10), 0, 0);
        section.addView(customMap, customMapLp);

        if (isWideLayout()) {
            addButtonPair(section,
                    button("\u6253\u5f00\u5f00\u6e90\u4ed3\u5e93", v -> openUrl(REPOSITORY_URL), 0xFF1D4ED8),
                    button("\u67e5\u770b\u8bb8\u53ef\u8bc1", v -> openUrl(LICENSE_URL), 0xFF475569));
            addButtonPair(section,
                    button("\u5b9a\u5236\u5730\u56fe Skill", v -> chooseDownloadSource("\u5b9a\u5236\u5730\u56fe Skill", CUSTOM_MAP_SKILL_URL, CUSTOM_MAP_SKILL_MIRROR_URL), 0xFF0F766E),
                    button("\u4e0b\u8f7d\u5df2\u6539\u9ad8\u5fb7", v -> chooseDownloadSource("\u4e0b\u8f7d\u5df2\u6539\u9ad8\u5fb7", CUSTOM_MAP_APK_URL, CUSTOM_MAP_APK_MIRROR_URL), 0xFFB45309));
        } else {
            section.addView(button("\u6253\u5f00\u5f00\u6e90\u4ed3\u5e93", v -> openUrl(REPOSITORY_URL), 0xFF1D4ED8));
            section.addView(button("\u67e5\u770b\u8bb8\u53ef\u8bc1", v -> openUrl(LICENSE_URL), 0xFF475569));
            section.addView(button("\u5b9a\u5236\u5730\u56fe Skill", v -> chooseDownloadSource("\u5b9a\u5236\u5730\u56fe Skill", CUSTOM_MAP_SKILL_URL, CUSTOM_MAP_SKILL_MIRROR_URL), 0xFF0F766E));
            section.addView(button("\u4e0b\u8f7d\u5df2\u6539\u9ad8\u5fb7", v -> chooseDownloadSource("\u4e0b\u8f7d\u5df2\u6539\u9ad8\u5fb7", CUSTOM_MAP_APK_URL, CUSTOM_MAP_APK_MIRROR_URL), 0xFFB45309));
        }
    }

    private void addOverlayScaleControls(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(2), dp(10), dp(2), 0);

        overlayScaleText = new TextView(this);
        overlayScaleText.setTextSize(14f);
        overlayScaleText.setTextColor(0xFF111827);
        overlayScaleText.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(overlayScaleText, new LinearLayout.LayoutParams(-1, -2));
        addOverlayPreview(box);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(MAX_OVERLAY_SCALE_PERCENT - MIN_OVERLAY_SCALE_PERCENT);
        seekBar.setProgress(getOverlayScalePercent(this) - MIN_OVERLAY_SCALE_PERCENT);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int percent = MIN_OVERLAY_SCALE_PERCENT + progress;
                updateOverlayScaleText(percent);
                if (fromUser) {
                    saveOverlayScalePercent(percent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                int percent = MIN_OVERLAY_SCALE_PERCENT + bar.getProgress();
                saveOverlayScalePercent(percent);
                updateOverlayScaleText(percent);
            }
        });
        box.addView(seekBar, new LinearLayout.LayoutParams(-1, -2));
        updateOverlayScaleText(getOverlayScalePercent(this));
        box.addView(button("\u5e94\u7528\u5f53\u524d\u5927\u5c0f\u5230\u60ac\u6d6e\u7a97", v -> notifyOverlayScaleChanged(), 0xFF334155));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(10), 0, 0);
        parent.addView(box, lp);
    }

    private void addClusterMirrorControls(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(2), dp(12), dp(2), 0);

        TextView title = new TextView(this);
        title.setText("\u526f\u5c4f\u60ac\u6d6e\u7a97");
        title.setTextSize(14f);
        title.setTextColor(0xFF111827);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(title, new LinearLayout.LayoutParams(-1, -2));

        clusterDisplayText = new TextView(this);
        clusterDisplayText.setTextSize(13f);
        clusterDisplayText.setTextColor(0xFF334155);
        LinearLayout.LayoutParams displayTextLp = new LinearLayout.LayoutParams(-1, -2);
        displayTextLp.setMargins(0, dp(8), 0, 0);
        box.addView(clusterDisplayText, displayTextLp);
        updateClusterDisplayText();

        box.addView(button("\u9009\u62e9\u6295\u5c4f\u5c4f\u5e55", v -> chooseClusterDisplay(), 0xFF334155));

        clusterScaleText = new TextView(this);
        clusterScaleText.setTextSize(13f);
        clusterScaleText.setTextColor(0xFF334155);
        LinearLayout.LayoutParams scaleTextLp = new LinearLayout.LayoutParams(-1, -2);
        scaleTextLp.setMargins(0, dp(8), 0, 0);
        box.addView(clusterScaleText, scaleTextLp);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(MAX_OVERLAY_SCALE_PERCENT - MIN_OVERLAY_SCALE_PERCENT);
        seekBar.setProgress(getClusterScalePercent(this) - MIN_OVERLAY_SCALE_PERCENT);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int percent = MIN_OVERLAY_SCALE_PERCENT + progress;
                updateClusterScaleText(percent);
                if (fromUser) {
                    saveClusterScalePercent(percent);
                    notifyClusterMirrorChanged();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                int percent = MIN_OVERLAY_SCALE_PERCENT + bar.getProgress();
                saveClusterScalePercent(percent);
                updateClusterScaleText(percent);
                notifyClusterMirrorChanged();
            }
        });
        box.addView(seekBar, new LinearLayout.LayoutParams(-1, -2));
        updateClusterScaleText(getClusterScalePercent(this));

        LinearLayout upRow = new LinearLayout(this);
        upRow.setGravity(Gravity.CENTER);
        upRow.addView(directionButton("\u4e0a", v -> moveClusterBy(0, -dp(16))));
        box.addView(upRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout middleRow = new LinearLayout(this);
        middleRow.setGravity(Gravity.CENTER);
        middleRow.addView(directionButton("\u5de6", v -> moveClusterBy(-dp(16), 0)));
        middleRow.addView(directionButton("\u53f3", v -> moveClusterBy(dp(16), 0)));
        box.addView(middleRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout downRow = new LinearLayout(this);
        downRow.setGravity(Gravity.CENTER);
        downRow.addView(directionButton("\u4e0b", v -> moveClusterBy(0, dp(16))));
        box.addView(downRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, 0);
        parent.addView(box, lp);
    }

    private void addOverlayContentControls(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(2), dp(12), dp(2), 0);

        TextView title = new TextView(this);
        title.setText("\u81ea\u5b9a\u4e49\u60ac\u6d6e\u7a97\u5185\u5bb9");
        title.setTextSize(14f);
        title.setTextColor(0xFF111827);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView hint = new TextView(this);
        hint.setText("\u4e3b\u60ac\u6d6e\u7a97\u548c\u526f\u5c4f\u955c\u50cf\u4f1a\u540c\u6b65\u4f7f\u7528\u8fd9\u7ec4\u663e\u793a\u8bbe\u7f6e");
        hint.setTextSize(12f);
        hint.setTextColor(0xFF64748B);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-1, -2);
        hintLp.setMargins(0, dp(6), 0, 0);
        box.addView(hint, hintLp);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(-1, -2);
        gridLp.setMargins(0, dp(6), 0, 0);
        box.addView(grid, gridLp);

        if (isWideLayout()) {
            addTogglePair(grid,
                    contentToggle("\u9876\u90e8\u72b6\u6001", KEY_SHOW_MODE),
                    contentToggle("\u8def\u7ebf\u6307\u5f15", KEY_SHOW_TURN));
            addTogglePair(grid,
                    contentToggle("\u7ea2\u7eff\u706f\u5012\u8ba1\u65f6", KEY_SHOW_LIGHT),
                    contentToggle("\u8f66\u9053\u4fe1\u606f", KEY_SHOW_LANE));
            addTogglePair(grid,
                    contentToggle("\u5269\u4f59\u91cc\u7a0b\u4e0e\u76ee\u7684\u5730", KEY_SHOW_ETA),
                    contentToggle("\u9650\u901f/\u7535\u5b50\u773c/\u7ea2\u7eff\u706f\u4e2a\u6570", KEY_SHOW_ALERT));
            addTogglePair(grid,
                    contentToggle("\u8be6\u7ec6\u72b6\u6001", KEY_SHOW_DETAIL),
                    null);
        } else {
            grid.addView(contentToggle("\u9876\u90e8\u72b6\u6001", KEY_SHOW_MODE));
            grid.addView(contentToggle("\u8def\u7ebf\u6307\u5f15", KEY_SHOW_TURN));
            grid.addView(contentToggle("\u7ea2\u7eff\u706f\u5012\u8ba1\u65f6", KEY_SHOW_LIGHT));
            grid.addView(contentToggle("\u8f66\u9053\u4fe1\u606f", KEY_SHOW_LANE));
            grid.addView(contentToggle("\u5269\u4f59\u91cc\u7a0b\u4e0e\u76ee\u7684\u5730", KEY_SHOW_ETA));
            grid.addView(contentToggle("\u9650\u901f/\u7535\u5b50\u773c/\u7ea2\u7eff\u706f\u4e2a\u6570", KEY_SHOW_ALERT));
            grid.addView(contentToggle("\u8be6\u7ec6\u72b6\u6001", KEY_SHOW_DETAIL));
        }
        addBackgroundOpacityControls(box);
        overlayUiStyleButton = button(overlayUiStyleButtonText(), v -> chooseOverlayUiStyle(), 0xFF334155);
        LinearLayout.LayoutParams uiStyleLp = new LinearLayout.LayoutParams(-1, dp(42));
        uiStyleLp.setMargins(0, dp(8), 0, 0);
        overlayUiStyleButton.setLayoutParams(uiStyleLp);
        box.addView(overlayUiStyleButton);
        overlayTextModeButton = button(textModeButtonText(), v -> chooseTextMode(), 0xFF475569);
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(-1, dp(42));
        buttonLp.setMargins(0, dp(8), 0, 0);
        overlayTextModeButton.setLayoutParams(buttonLp);
        box.addView(overlayTextModeButton);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, 0);
        parent.addView(box, lp);
        updateOverlayPreviewContentVisibility();
        applyOverlayPreviewStyle();
    }

    private void addBehaviorControls(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(2), dp(12), dp(2), 0);

        TextView title = new TextView(this);
        title.setText("自动启动与显示策略");
        title.setTextSize(14f);
        title.setTextColor(0xFF111827);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView hint = new TextView(this);
        hint.setText("这些选项只控制本程序窗口，不会主动唤醒或启动目标高德应用。高德前台隐藏需要允许“使用情况访问权限”。");
        hint.setTextSize(12f);
        hint.setTextColor(0xFF64748B);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-1, -2);
        hintLp.setMargins(0, dp(6), 0, 0);
        box.addView(hint, hintLp);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(-1, -2);
        gridLp.setMargins(0, dp(6), 0, 0);
        box.addView(grid, gridLp);

        if (isWideLayout()) {
            addTogglePair(grid,
                    behaviorToggle("开机自动启动", KEY_AUTO_START_ENABLED),
                    behaviorToggle("高德前台隐藏中控悬浮窗", KEY_HIDE_MAIN_WHEN_TARGET_FOREGROUND));
            addTogglePair(grid,
                    behaviorToggle("导航/巡航退出隐藏仪表", KEY_HIDE_CLUSTER_WHEN_INACTIVE),
                    null);
        } else {
            grid.addView(behaviorToggle("开机自动启动", KEY_AUTO_START_ENABLED));
            grid.addView(behaviorToggle("高德前台隐藏中控悬浮窗", KEY_HIDE_MAIN_WHEN_TARGET_FOREGROUND));
            grid.addView(behaviorToggle("导航/巡航退出隐藏仪表", KEY_HIDE_CLUSTER_WHEN_INACTIVE));
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, 0);
        parent.addView(box, lp);
    }

    private void addBackgroundOpacityControls(LinearLayout parent) {
        overlayBackgroundOpacityText = new TextView(this);
        overlayBackgroundOpacityText.setTextSize(13f);
        overlayBackgroundOpacityText.setTextColor(0xFF334155);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(-1, -2);
        textLp.setMargins(0, dp(8), 0, 0);
        parent.addView(overlayBackgroundOpacityText, textLp);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(MAX_BACKGROUND_OPACITY_PERCENT - MIN_BACKGROUND_OPACITY_PERCENT);
        seekBar.setProgress(getBackgroundOpacityPercent(this) - MIN_BACKGROUND_OPACITY_PERCENT);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int percent = MIN_BACKGROUND_OPACITY_PERCENT + progress;
                updateBackgroundOpacityText(percent);
                if (fromUser) {
                    saveBackgroundOpacityPercent(percent);
                    applyOverlayPreviewStyle();
                    notifyOverlayStyleChanged();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                int percent = MIN_BACKGROUND_OPACITY_PERCENT + bar.getProgress();
                saveBackgroundOpacityPercent(percent);
                updateBackgroundOpacityText(percent);
                applyOverlayPreviewStyle();
                notifyOverlayStyleChanged();
            }
        });
        parent.addView(seekBar, new LinearLayout.LayoutParams(-1, -2));
        updateBackgroundOpacityText(getBackgroundOpacityPercent(this));
    }

    private void addOverlayPreview(LinearLayout parent) {
        overlayPreviewStage = new FrameLayout(this);
        overlayPreviewStage.setPadding(dp(10), dp(10), dp(10), dp(10));
        overlayPreviewStage.setBackground(navigationPreviewBackground());
        addPreviewRoads(overlayPreviewStage);

        LinearLayout topGuide = buildPreviewTopGuide();
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        topLp.setMargins(dp(6), dp(6), dp(6), 0);
        overlayPreviewStage.addView(topGuide, topLp);

        overlayPreviewPanel = buildOverlayPreviewPanel();
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER | Gravity.BOTTOM);
        panelLp.setMargins(0, dp(66), 0, dp(12));
        overlayPreviewStage.addView(overlayPreviewPanel, panelLp);

        LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(-1, dp(260));
        stageLp.setMargins(0, dp(8), 0, dp(2));
        parent.addView(overlayPreviewStage, stageLp);
    }

    private GradientDrawable navigationPreviewBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        bg.setColors(new int[]{0xFF182436, 0xFF0B1320});
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), 0xFF1F2A3A);
        return bg;
    }

    private void addPreviewRoads(FrameLayout stage) {
        stage.addView(previewRoad(dp(260), dp(16), -18f, 0xFF0F8F6D),
                roadLayout(dp(-28), dp(184), dp(360), dp(18)));
        stage.addView(previewRoad(dp(190), dp(11), -18f, 0xFF14B88A),
                roadLayout(dp(190), dp(204), dp(250), dp(13)));
        stage.addView(previewRoad(dp(210), dp(9), 28f, 0xFF24364C),
                roadLayout(dp(10), dp(116), dp(260), dp(12)));
        stage.addView(previewRoad(dp(180), dp(8), 28f, 0xFF24364C),
                roadLayout(dp(210), dp(98), dp(240), dp(10)));
    }

    private FrameLayout.LayoutParams roadLayout(int left, int top, int width, int height) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height, Gravity.TOP | Gravity.LEFT);
        lp.leftMargin = left;
        lp.topMargin = top;
        return lp;
    }

    private android.view.View previewRoad(int width, int height, float rotation, int color) {
        android.view.View road = new android.view.View(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(height / 2f);
        road.setBackground(bg);
        road.setRotation(rotation);
        road.setAlpha(0.92f);
        return road;
    }

    private LinearLayout buildPreviewTopGuide() {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(10), dp(8), dp(10), dp(7));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF0000000);
        bg.setCornerRadius(dp(8));
        top.setBackground(bg);

        TextView main = new TextView(this);
        main.setText("\u2190 669 \u7c73  \u8fdb\u5165 \u6986\u4e61\u8def\u8f85\u8def");
        main.setTextSize(15f);
        main.setTextColor(Color.WHITE);
        main.setTypeface(Typeface.DEFAULT_BOLD);
        main.setSingleLine(true);
        top.addView(main, new LinearLayout.LayoutParams(-1, -2));

        TextView sub = new TextView(this);
        sub.setText("5.3\u516c\u91cc \u00b7 10\u5206\u949f                                      05:42\u5230");
        sub.setTextSize(8.5f);
        sub.setTextColor(0xFFD1D5DB);
        sub.setSingleLine(true);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.setMargins(0, dp(5), 0, 0);
        top.addView(sub, subLp);
        return top;
    }

    private LinearLayout buildOverlayPreviewPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(6), dp(5), dp(6), dp(5));
        panel.setBackground(createPreviewPanelBackground());

        previewModeText = new TextView(this);
        previewModeText.setText("\u5bfc\u822a \u00b7 \u5357\u56db\u73af\u4e1c\u8def\u8f85\u8def \u00b7 39 km/h");
        previewModeText.setTextSize(6.5f);
        previewModeText.setTextColor(0xFFE8EAED);
        previewModeText.setSingleLine(true);
        panel.addView(previewModeText, new LinearLayout.LayoutParams(-2, -2));

        previewTurnText = new TextView(this);
        previewTurnText.setText("\u2190  669\u7c73\n\u8fdb\u5165 \u6986\u4e61\u8def\u8f85\u8def");
        previewTurnText.setTextSize(20f);
        previewTurnText.setTypeface(Typeface.DEFAULT_BOLD);
        previewTurnText.setGravity(Gravity.CENTER);
        previewTurnText.setTextColor(Color.WHITE);
        previewTurnText.setPadding(dp(12), dp(4), dp(12), dp(5));
        GradientDrawable turnBg = new GradientDrawable();
        turnBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        turnBg.setColors(new int[]{0xFF1D4ED8, 0xFF0891B2});
        turnBg.setCornerRadius(dp(5));
        previewTurnText.setBackground(turnBg);
        LinearLayout.LayoutParams turnLp = new LinearLayout.LayoutParams(-2, -2);
        turnLp.setMargins(0, dp(3), 0, dp(3));
        panel.addView(previewTurnText, turnLp);

        previewLightRow = new LinearLayout(this);
        previewLightRow.setOrientation(LinearLayout.HORIZONTAL);
        previewLightRow.setGravity(Gravity.CENTER);
        previewLightRow.addView(previewLight("\u2190 51s", 0xFFC62828));
        previewLightRow.addView(previewLight("\u2191 18s", 0xFFC62828));
        panel.addView(previewLightRow, new LinearLayout.LayoutParams(-2, -2));

        previewLaneSection = new LinearLayout(this);
        previewLaneSection.setOrientation(LinearLayout.VERTICAL);
        previewLaneSection.setGravity(Gravity.CENTER_HORIZONTAL);
        previewLaneSection.setPadding(dp(4), dp(3), dp(4), dp(4));
        GradientDrawable laneBg = new GradientDrawable();
        laneBg.setColor(0xCC0F172A);
        laneBg.setCornerRadius(dp(5));
        laneBg.setStroke(dp(1), 0x1FFFFFFF);
        previewLaneSection.setBackground(laneBg);

        previewLaneTitleText = new TextView(this);
        previewLaneTitleText.setText("\u8f66\u9053\u4fe1\u606f");
        previewLaneTitleText.setTextSize(5.5f);
        previewLaneTitleText.setTextColor(0xFFBAE6FD);
        previewLaneTitleText.setTypeface(Typeface.DEFAULT_BOLD);
        previewLaneSection.addView(previewLaneTitleText, new LinearLayout.LayoutParams(-2, -2));

        LaneBarView laneBar = new LaneBarView(this);
        laneBar.setFrameScaleMultiplier(1f);
        laneBar.setScaleMultiplier(1.5f);
        laneBar.setLaneData(new int[]{15, 31, 18}, new boolean[]{true, false, true});
        previewLaneSection.addView(laneBar, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams laneLp = new LinearLayout.LayoutParams(-2, -2);
        laneLp.setMargins(0, dp(3), 0, dp(2));
        panel.addView(previewLaneSection, laneLp);

        previewEtaText = new TextView(this);
        previewEtaText.setText("5.3\u516c\u91cc \u00b7 10\u5206\u949f\n\u9884\u8ba105:42\u5230\u8fbe\n\u76ee\u7684\u5730 \u5c0f\u7ea2\u95e8\u4e61\u515a\u7fa4\u670d\u52a1\u4e2d\u5fc3");
        previewEtaText.setTextSize(7.5f);
        previewEtaText.setTextColor(0xFFE8EAED);
        previewEtaText.setGravity(Gravity.CENTER);
        panel.addView(previewEtaText, new LinearLayout.LayoutParams(-2, -2));

        previewAlertText = new TextView(this);
        previewAlertText.setText("\u9650\u901f 50  \u00b7  \u7ea2\u7eff\u706f 2\u4e2a");
        previewAlertText.setTextSize(6.5f);
        previewAlertText.setTextColor(0xFFFFF7ED);
        previewAlertText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams alertLp = new LinearLayout.LayoutParams(-2, -2);
        alertLp.setMargins(0, dp(3), 0, 0);
        panel.addView(previewAlertText, alertLp);

        previewDetailText = new TextView(this);
        previewDetailText.setText("\u8f66\u5934 90\u00b0\n\u9053\u8def \u4e3b\u8981\u9053\u8def");
        previewDetailText.setTextSize(5.8f);
        previewDetailText.setTextColor(0xFFC7D2FE);
        previewDetailText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-2, -2);
        detailLp.setMargins(0, dp(2), 0, 0);
        panel.addView(previewDetailText, detailLp);
        return panel;
    }

    private TextView previewLight(String text, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(10f);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setMinWidth(dp(31));
        view.setMinHeight(dp(18));
        view.setPadding(dp(5), 0, dp(5), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(9));
        view.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(18));
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        view.setLayoutParams(lp);
        return view;
    }

    private LinearLayout card(int color) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(14), dp(12), dp(14), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(10));
        if (color == Color.WHITE) {
            bg.setStroke(dp(1), 0xFFE5E7EB);
        }
        layout.setBackground(bg);
        return layout;
    }

    private Button button(String text, android.view.View.OnClickListener listener, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15f);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(8));
        b.setBackground(bg);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
        lp.setMargins(0, dp(9), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private Button directionButton(String text, android.view.View.OnClickListener listener) {
        Button b = button(text, listener, 0xFF475569);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(86), dp(42));
        lp.setMargins(dp(5), dp(6), dp(5), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void addButtonPair(LinearLayout parent, Button left, Button right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(2f);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, dp(9), 0, 0);
        row.setLayoutParams(rowLp);

        row.addView(wideButton(left, 0, dp(5)));
        if (right != null) {
            row.addView(wideButton(right, dp(5), 0));
        } else {
            android.view.View spacer = new android.view.View(this);
            LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, 0, 1f);
            spacerLp.setMargins(dp(5), 0, 0, 0);
            row.addView(spacer, spacerLp);
        }
        parent.addView(row);
    }

    private Button wideButton(Button button, int leftMargin, int rightMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        lp.setMargins(leftMargin, 0, rightMargin, 0);
        button.setLayoutParams(lp);
        return button;
    }

    private void addTogglePair(LinearLayout parent, CheckBox left, CheckBox right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(2f);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, dp(2), 0, 0);
        row.setLayoutParams(rowLp);

        row.addView(wideToggle(left, 0, dp(8)));
        if (right != null) {
            row.addView(wideToggle(right, dp(8), 0));
        } else {
            android.view.View spacer = new android.view.View(this);
            LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, 0, 1f);
            spacerLp.setMargins(dp(8), 0, 0, 0);
            row.addView(spacer, spacerLp);
        }
        parent.addView(row);
    }

    private CheckBox wideToggle(CheckBox checkBox, int leftMargin, int rightMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(leftMargin, 0, rightMargin, 0);
        checkBox.setLayoutParams(lp);
        return checkBox;
    }

    private boolean isWideLayout() {
        return getResources().getDisplayMetrics().widthPixels >= getResources().getDisplayMetrics().heightPixels;
    }

    private void chooseTargetApp() {
        PackageManager pm = getPackageManager();
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PackageManager.MATCH_ALL : 0;
        HashSet<String> launcherPackages = new HashSet<>();
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(main, flags);
        HashSet<String> seen = new HashSet<>();
        ArrayList<AppChoice> allChoices = new ArrayList<>();
        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null || info.activityInfo.packageName == null) {
                continue;
            }
            String pkg = info.activityInfo.packageName;
            launcherPackages.add(pkg);
            if (pkg.equals(getPackageName())) {
                continue;
            }
            if (!seen.add(pkg)) {
                continue;
            }
            ApplicationInfo appInfo = info.activityInfo.applicationInfo;
            String label = String.valueOf(appInfo.loadLabel(pm));
            allChoices.add(new AppChoice(label, pkg, isSystemApp(appInfo), true, isMapNamedApp(label), isAmapPackage(pkg)));
        }
        for (ApplicationInfo appInfo : pm.getInstalledApplications(flags)) {
            String pkg = appInfo.packageName;
            if (pkg == null || pkg.equals(getPackageName()) || !seen.add(pkg)) {
                continue;
            }
            String label = String.valueOf(appInfo.loadLabel(pm));
            allChoices.add(new AppChoice(label, pkg, isSystemApp(appInfo), launcherPackages.contains(pkg), isMapNamedApp(label), isAmapPackage(pkg)));
        }
        sortAppChoices(allChoices);
        ArrayList<AppChoice> filteredChoices = new ArrayList<>();
        for (AppChoice choice : allChoices) {
            if (choice.mapNamed || choice.amapPackage) {
                filteredChoices.add(choice);
            }
        }
        boolean fallbackToAll = filteredChoices.isEmpty();
        ArrayList<AppChoice> choices = new ArrayList<>();
        if (fallbackToAll) {
            choices.addAll(allChoices);
        } else {
            choices.addAll(filteredChoices);
        }
        sortAppChoices(choices);
        if (choices.isEmpty()) {
            choices.add(new AppChoice(DEFAULT_TARGET_PACKAGE, DEFAULT_TARGET_PACKAGE, false, false, false, true));
        }
        LinearLayout dialogContent = new LinearLayout(this);
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setPadding(dp(8), 0, dp(8), 0);
        TextView hint = new TextView(this);
        hint.setText(fallbackToAll
                ? "\u672a\u627e\u5230 com.autonavi.* \u6216\u540d\u79f0\u5305\u542b\u201c\u5730\u56fe\u201d\u7684\u5e94\u7528\uff0c\u5df2\u663e\u793a\u6240\u6709\u53ef\u89c1\u5e94\u7528\u5305\u3002"
                : "\u4f18\u5148\u663e\u793a com.autonavi.* \u5305\u540d\u6216\u540d\u79f0\u5305\u542b\u201c\u5730\u56fe\u201d\u7684\u5e94\u7528\u3002");
        hint.setTextSize(13);
        hint.setTextColor(0xFF4B5563);
        hint.setPadding(dp(16), dp(6), dp(16), dp(10));
        dialogContent.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        ListView listView = new ListView(this);
        listView.setDivider(null);
        TargetAppAdapter adapter = new TargetAppAdapter(choices);
        listView.setAdapter(adapter);
        dialogContent.addView(listView, new LinearLayout.LayoutParams(-1, Math.min(dp(520), getResources().getDisplayMetrics().heightPixels / 2)));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("\u9009\u62e9\u76ee\u6807\u5e94\u7528")
                .setNegativeButton("\u663e\u793a\u6240\u6709\u5e94\u7528", null)
                .setView(dialogContent)
                .create();
        listView.setOnItemClickListener((parent, view, which, id) -> {
            saveTargetPackage(choices.get(which).packageName);
            updateTargetText();
            startOverlayService();
            dialog.dismiss();
        });
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            choices.clear();
            choices.addAll(allChoices);
            if (choices.isEmpty()) {
                choices.add(new AppChoice(DEFAULT_TARGET_PACKAGE, DEFAULT_TARGET_PACKAGE, false, false, false, true));
            }
            hint.setText("\u5df2\u663e\u793a\u6240\u6709\u53ef\u89c1\u5e94\u7528\u5305\u3002");
            adapter.notifyDataSetChanged();
        });
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        return (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    private void sortAppChoices(ArrayList<AppChoice> choices) {
        Collections.sort(choices, Comparator
                .comparing((AppChoice a) -> !a.amapPackage)
                .thenComparing(a -> !a.mapNamed)
                .thenComparing(a -> a.system)
                .thenComparing(a -> a.label.toLowerCase(java.util.Locale.CHINA))
                .thenComparing(a -> a.packageName));
    }

    private boolean isAmapPackage(String packageName) {
        return packageName != null && packageName.startsWith(TARGET_PACKAGE_PREFIX);
    }

    private boolean isMapNamedApp(String label) {
        return label != null && label.contains("\u5730\u56fe");
    }

    private void startOverlayService() {
        startOverlayService(this);
    }

    static void startOverlayService(Context context) {
        Intent intent = new Intent(context, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Context.class.getMethod("startForegroundService", Intent.class).invoke(context, intent);
            } catch (Throwable ignored) {
                context.startService(intent);
            }
        } else {
            context.startService(intent);
        }
    }

    private void stopOverlayService() {
        saveMainOverlayEnabled(false);
        notifyMainOverlayChanged();
        stopServiceIfNoVisuals();
    }

    private void enableMainOverlay() {
        saveMainOverlayEnabled(true);
        startOverlayService();
        notifyMainOverlayChanged();
    }

    private String clusterMirrorButtonText() {
        return isClusterMirrorEnabled(this) ? "\u5173\u95ed\u4eea\u8868\u76d8\u955c\u50cf" : "\u5f00\u542f\u4eea\u8868\u76d8\u955c\u50cf";
    }

    private void toggleClusterMirror(Button button) {
        boolean enabled = !isClusterMirrorEnabled(this);
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_CLUSTER_MIRROR_ENABLED, enabled)
                .apply();
        button.setText(clusterMirrorButtonText());
        startOverlayService();
        notifyClusterMirrorChanged();
        stopServiceIfNoVisuals();
        Toast.makeText(this,
                enabled ? "\u5df2\u5f00\u542f\u4eea\u8868\u76d8\u955c\u50cf" : "\u5df2\u5173\u95ed\u4eea\u8868\u76d8\u955c\u50cf",
                Toast.LENGTH_SHORT).show();
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void openTargetApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(getTargetPackage(this));
        if (launch != null) {
            startActivity(launch);
        }
    }

    private void chooseClusterDisplay() {
        ArrayList<DisplayChoice> choices = getClusterDisplayChoices();
        String[] labels = new String[choices.size() + 1];
        labels[0] = "\u81ea\u52a8\u9009\u62e9\n\u4f18\u5148\u4f7f\u7528\u7cfb\u7edf\u8ba4\u5b9a\u7684\u526f\u5c4f";
        for (int i = 0; i < choices.size(); i++) {
            DisplayChoice choice = choices.get(i);
            labels[i + 1] = choice.label + "\nID " + choice.displayId;
        }
        int currentId = getClusterDisplayId(this);
        int checked = 0;
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).displayId == currentId) {
                checked = i + 1;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("\u9009\u62e9\u6295\u5c4f\u5c4f\u5e55")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    saveClusterDisplayId(which == 0 ? -1 : choices.get(which - 1).displayId);
                    updateClusterDisplayText();
                    startOverlayService();
                    notifyClusterMirrorChanged();
                    dialog.dismiss();
                })
                .setNegativeButton("\u53d6\u6d88", null)
                .show();
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Throwable t) {
            Toast.makeText(this, "\u65e0\u6cd5\u6253\u5f00\u94fe\u63a5", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLogcatDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), 0, dp(8), 0);

        TextView hint = new TextView(this);
        hint.setText("\u53cd\u9988 bug \u65f6\u53ef\u63d0\u4ea4\u65e5\u5fd7\u3002\u4f18\u5148\u4fdd\u5b58\u5230 /sdcard/amap_log\uff1b\u82e5\u7cfb\u7edf\u4e0d\u6388\u6743\uff0c\u4f1a\u81ea\u52a8\u56de\u9000\u5230\u5e94\u7528\u79c1\u6709\u65e5\u5fd7\u76ee\u5f55\u3002");
        hint.setTextSize(13);
        hint.setTextColor(0xFF4B5563);
        hint.setPadding(dp(16), dp(6), dp(16), dp(10));
        content.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        TextView logText = new TextView(this);
        logText.setTextSize(11);
        logText.setTextColor(0xFF111827);
        logText.setTypeface(Typeface.MONOSPACE);
        logText.setTextIsSelectable(true);
        logText.setLineSpacing(0, 1.05f);
        logText.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawable logBg = new GradientDrawable();
        logBg.setColor(0xFFF8FAFC);
        logBg.setStroke(dp(1), 0xFFE2E8F0);
        logBg.setCornerRadius(dp(8));
        logText.setBackground(logBg);
        ScrollView logScroll = new ScrollView(this);
        logScroll.addView(logText, new ScrollView.LayoutParams(-1, -2));
        content.addView(logScroll, new LinearLayout.LayoutParams(-1, Math.min(dp(520), getResources().getDisplayMetrics().heightPixels / 2)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setWeightSum(4f);
        content.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        Button grant = compactDialogButton("\u6388\u6743");
        Button refresh = compactDialogButton("\u5237\u65b0");
        Button save = compactDialogButton("\u4fdd\u5b58");
        Button copy = compactDialogButton("\u590d\u5236");
        actions.addView(grant, new LinearLayout.LayoutParams(0, dp(42), 1f));
        actions.addView(refresh, new LinearLayout.LayoutParams(0, dp(42), 1f));
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(42), 1f));
        actions.addView(copy, new LinearLayout.LayoutParams(0, dp(42), 1f));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("\u65e5\u5fd7\u4e0e\u8c03\u8bd5")
                .setView(content)
                .setPositiveButton("\u5173\u95ed", null)
                .create();

        grant.setOnClickListener(v -> requestLogAndStoragePermissions(true));
        refresh.setOnClickListener(v -> refreshLogcat(logText, logScroll));
        save.setOnClickListener(v -> saveLogText(String.valueOf(logText.getText())));
        copy.setOnClickListener(v -> copyLogText(String.valueOf(logText.getText())));
        dialog.setOnShowListener(d -> {
            requestLogAndStoragePermissions(false);
            refreshLogcat(logText, logScroll);
        });
        dialog.show();
    }

    private Button compactDialogButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(0xFF1F2937);
        return b;
    }

    private void requestLogAndStoragePermissions(boolean openSettings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.READ_LOGS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_LOGS);
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (Build.VERSION.SDK_INT <= 32
                    && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (!permissions.isEmpty()) {
                try {
                    requestPermissions(permissions.toArray(new String[0]), REQUEST_LOG_PERMISSIONS);
                } catch (Throwable ignored) {
                }
            }
        }
        if (openSettings && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Throwable t) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                } catch (Throwable ignored) {
                    Toast.makeText(this, "\u65e0\u6cd5\u6253\u5f00\u6240\u6709\u6587\u4ef6\u8bbf\u95ee\u6743\u9650\u8bbe\u7f6e", Toast.LENGTH_SHORT).show();
                }
            }
            Toast.makeText(this, "\u8bf7\u5f00\u542f\u201c\u6240\u6709\u6587\u4ef6\u8bbf\u95ee\u6743\u9650\u201d\uff0c\u7528\u4e8e\u4fdd\u5b58\u5230 /sdcard/amap_log", Toast.LENGTH_LONG).show();
        } else if (openSettings) {
            Toast.makeText(this, logPermissionSummary(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshLogcat(TextView logText, ScrollView logScroll) {
        logText.setText("\u6b63\u5728\u8bfb\u53d6 logcat...");
        new Thread(() -> {
            String text = collectLogcat();
            runOnUiThread(() -> {
                logText.setText(text);
                logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
            });
        }, "amap-logcat-reader").start();
    }

    private String collectLogcat() {
        StringBuilder sb = new StringBuilder();
        sb.append("AMap Companion log report\n");
        sb.append("time=").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date())).append('\n');
        sb.append("package=").append(getPackageName()).append('\n');
        sb.append("version=").append(currentVersionName()).append(" (").append(currentVersionCode()).append(")\n");
        sb.append("targetPackage=").append(getTargetPackage(this)).append('\n');
        sb.append("android=").append(Build.VERSION.RELEASE).append(" sdk=").append(Build.VERSION.SDK_INT).append('\n');
        sb.append("readLogsPermission=").append(hasPermission(Manifest.permission.READ_LOGS)).append('\n');
        sb.append("publicLogDirWritable=").append(canWritePublicLogDir()).append('\n');
        sb.append("preferredLogDir=/sdcard/amap_log\n");
        sb.append("note=Android may restrict third-party apps to their own logs only.\n\n");
        int lines = appendLogcatCommand(sb, "filtered", new String[]{
                "logcat", "-d", "-v", "time", "-t", "1000",
                "AmapCompanion:D", "AndroidRuntime:E", "System.err:W", "*:S"
        });
        if (lines == 0) {
            lines = appendLogcatCommand(sb, "recent", new String[]{
                    "logcat", "-d", "-v", "time", "-t", "300"
            });
        }
        if (lines == 0) {
            sb.append("\n(no logcat output; system may restrict log access or logs may be empty)\n");
        }
        return sb.toString();
    }

    private String currentVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private long currentVersionCode() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return info.getLongVersionCode();
            }
            return info.versionCode;
        } catch (Throwable t) {
            return -1;
        }
    }

    private int appendLogcatCommand(StringBuilder sb, String label, String[] command) {
        sb.append("---- logcat ").append(label).append(" ----\n");
        java.lang.Process process = null;
        int lines = 0;
        try {
            process = Runtime.getRuntime().exec(command);
            lines = appendStream(sb, process.getInputStream());
            int exit = process.waitFor();
            StringBuilder err = new StringBuilder();
            appendStream(err, process.getErrorStream());
            if (err.length() > 0) {
                sb.append("\n---- logcat stderr ----\n").append(err);
            }
            sb.append("\n---- logcat ").append(label).append(" exit=").append(exit).append(" lines=").append(lines).append(" ----\n");
        } catch (Throwable t) {
            sb.append("\nlogcat failed: ").append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append('\n');
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return lines;
    }

    private int appendStream(StringBuilder sb, InputStream stream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        int lines = 0;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
            lines++;
        }
        return lines;
    }

    private void saveLogText(String text) {
        try {
            File dir = resolveWritableLogDir();
            String name = "amap_companion_log_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".txt";
            File out = new File(dir, name);
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(out), "UTF-8");
            try {
                writer.write(text);
            } finally {
                writer.close();
            }
            Toast.makeText(this, "\u65e5\u5fd7\u5df2\u4fdd\u5b58\uff1a" + out.getAbsolutePath() + "\n\u53cd\u9988 bug \u65f6\u53ef\u63d0\u4ea4\u8be5\u6587\u4ef6", Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Toast.makeText(this, "\u4fdd\u5b58\u65e5\u5fd7\u5931\u8d25\uff1a" + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File resolveWritableLogDir() throws Exception {
        File primary = new File(Environment.getExternalStorageDirectory(), "amap_log");
        if (ensureWritableDir(primary)) {
            return primary;
        }
        File fallback = getExternalFilesDir("logs");
        if (fallback == null) {
            fallback = new File(getCacheDir(), "logs");
        }
        if (ensureWritableDir(fallback)) {
            Toast.makeText(this, "\u65e0\u6cd5\u5199\u5165 /sdcard/amap_log\uff0c\u5df2\u56de\u9000\u5230\uff1a" + fallback.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return fallback;
        }
        throw new IllegalStateException("no writable log dir");
    }

    private boolean ensureWritableDir(File dir) {
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                return false;
            }
            File probe = new File(dir, ".write_test");
            FileOutputStream out = new FileOutputStream(probe);
            try {
                out.write(1);
            } finally {
                out.close();
            }
            if (probe.exists()) {
                probe.delete();
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean canWritePublicLogDir() {
        return ensureWritableDir(new File(Environment.getExternalStorageDirectory(), "amap_log"));
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        try {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    private String logPermissionSummary() {
        String storage = canWritePublicLogDir() ? "/sdcard/amap_log \u53ef\u5199" : "/sdcard/amap_log \u4e0d\u53ef\u5199\uff0c\u4f1a\u56de\u9000\u5230\u5e94\u7528\u79c1\u6709\u76ee\u5f55";
        String logs = hasPermission(Manifest.permission.READ_LOGS) ? "READ_LOGS \u5df2\u6388\u6743" : "READ_LOGS \u672a\u6388\u6743\uff0c\u666e\u901a\u7cfb\u7edf\u53ef\u80fd\u4ec5\u8fd4\u56de\u672c\u5e94\u7528\u65e5\u5fd7";
        return storage + "\n" + logs;
    }

    private void copyLogText(String text) {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null) {
            Toast.makeText(this, "\u590d\u5236\u5931\u8d25", Toast.LENGTH_SHORT).show();
            return;
        }
        manager.setPrimaryClip(ClipData.newPlainText("AMap Companion log", text));
        Toast.makeText(this, "\u65e5\u5fd7\u5df2\u590d\u5236", Toast.LENGTH_SHORT).show();
    }

    private void chooseDownloadSource(String title, String githubUrl, String mirrorUrl) {
        String[] labels = {
                "\u955c\u50cf\u7ad9\uff08\u4e0b\u8f7d ZIP\uff0c\u5feb\uff09\n" + mirrorUrl,
                "GitHub \u539f\u7ad9\uff08\u53ef\u80fd\u8f83\u6162\uff09\n" + githubUrl
        };
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(labels, (dialog, which) -> {
                    if (which == 0) {
                        openUrl(mirrorUrl);
                    } else {
                        openUrl(githubUrl);
                    }
                })
                .show();
    }

    private void chooseUpdateChannel() {
        String[] labels = {
                "\u670d\u52a1\u5668\u5206\u53d1\uff08\u63a8\u8350\uff09\n" + SERVER_UPDATE_URL,
                "GitHub \u76f4\u8fde\n" + GITHUB_UPDATE_URL
        };
        int checked = UPDATE_CHANNEL_GITHUB.equals(getUpdateChannel()) ? 1 : 0;
        new AlertDialog.Builder(this)
                .setTitle("\u9009\u62e9\u4e0b\u8f7d\u6e20\u9053")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    saveUpdateChannel(which == 1 ? UPDATE_CHANNEL_GITHUB : UPDATE_CHANNEL_SERVER);
                    updateUpdateText("\u66f4\u65b0\u6e20\u9053\n" + displayUpdateUrl());
                    dialog.dismiss();
                })
                .setNegativeButton("\u53d6\u6d88", null)
                .show();
    }

    private void updateTargetText() {
        if (targetText != null) {
            targetText.setText("\u76ee\u6807\u5e94\u7528\n" + getTargetPackage(this));
        }
    }

    private void checkForUpdates(boolean manual) {
        String url = getUpdateUrl();
        if (TextUtils.isEmpty(url)) {
            if (manual) {
                Toast.makeText(this, "\u66f4\u65b0\u5730\u5740\u672a\u914d\u7f6e", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        updateUpdateText("\u6b63\u5728\u68c0\u67e5\u66f4\u65b0...\n" + url);
        new Thread(() -> {
            try {
                Updater.UpdateInfo info = Updater.check(this, url);
                runOnUiThread(() -> handleUpdateInfo(info, manual));
            } catch (Throwable t) {
                runOnUiThread(() -> updateUpdateText("\u66f4\u65b0\u5931\u8d25: " + t.getMessage()));
            }
        }).start();
    }

    private void handleUpdateInfo(Updater.UpdateInfo info, boolean manual) {
        if (!info.hasUpdate()) {
            updateUpdateText("\u5df2\u662f\u6700\u65b0\u7248\n" + info.localVersionName + " (" + info.localVersionCode + ")");
            if (manual) {
                Toast.makeText(this, "\u5df2\u662f\u6700\u65b0\u7248", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        updateUpdateText("\u53d1\u73b0\u65b0\u7248\n" + info.remoteVersionName + " (" + info.remoteVersionCode + ")");
        showUpdateDetail(info);
    }

    private void showUpdateDetail(Updater.UpdateInfo info) {
        new AlertDialog.Builder(this)
                .setTitle("\u53d1\u73b0\u65b0\u7248")
                .setMessage(info.detailText())
                .setPositiveButton("\u66f4\u65b0", (dialog, which) -> installUpdate(info))
                .setNegativeButton("\u53d6\u6d88", null)
                .show();
    }

    private void installUpdate(Updater.UpdateInfo info) {
        updateUpdateText("\u51c6\u5907\u66f4\u65b0...\n" + info.remoteVersionName + " (" + info.remoteVersionCode + ")");
        new Thread(() -> Updater.install(this, info,
                message -> runOnUiThread(() -> updateUpdateText(message)))).start();
    }

    private void updateUpdateText(String text) {
        if (updateText != null) {
            updateText.setText(text);
        }
    }

    private void saveTargetPackage(String packageName) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_TARGET_PACKAGE, packageName)
                .apply();
    }

    private void saveUpdateUrl(String url) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_UPDATE_URL, TextUtils.isEmpty(url) ? DEFAULT_UPDATE_URL : url)
                .apply();
    }

    private void saveUpdateChannel(String channel) {
        String normalized = UPDATE_CHANNEL_GITHUB.equals(channel) ? UPDATE_CHANNEL_GITHUB : UPDATE_CHANNEL_SERVER;
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_UPDATE_CHANNEL, normalized)
                .putString(KEY_UPDATE_URL, channelToUpdateUrl(normalized))
                .apply();
    }

    private void persistDefaultUpdateUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String channel = prefs.getString(KEY_UPDATE_CHANNEL, DEFAULT_UPDATE_CHANNEL);
        if (!UPDATE_CHANNEL_GITHUB.equals(channel)) {
            channel = UPDATE_CHANNEL_SERVER;
        }
        prefs.edit()
                .putString(KEY_UPDATE_CHANNEL, channel)
                .putString(KEY_UPDATE_URL, channelToUpdateUrl(channel))
                .apply();
    }

    private String getUpdateUrl() {
        return channelToUpdateUrl(getUpdateChannel());
    }

    private String getUpdateChannel() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String channel = prefs.getString(KEY_UPDATE_CHANNEL, DEFAULT_UPDATE_CHANNEL);
        if (UPDATE_CHANNEL_GITHUB.equals(channel)) {
            return UPDATE_CHANNEL_GITHUB;
        }
        String legacyUrl = prefs.getString(KEY_UPDATE_URL, DEFAULT_UPDATE_URL);
        if (GITHUB_UPDATE_URL.equals(legacyUrl)) {
            return UPDATE_CHANNEL_GITHUB;
        }
        return UPDATE_CHANNEL_SERVER;
    }

    private String channelToUpdateUrl(String channel) {
        return UPDATE_CHANNEL_GITHUB.equals(channel) ? GITHUB_UPDATE_URL : SERVER_UPDATE_URL;
    }

    private String displayUpdateUrl() {
        String url = getUpdateUrl();
        if (TextUtils.isEmpty(url)) {
            return "\u672a\u8bbe\u7f6e";
        }
        String channelName = UPDATE_CHANNEL_GITHUB.equals(getUpdateChannel()) ? "GitHub \u76f4\u8fde" : "\u670d\u52a1\u5668\u5206\u53d1";
        return channelName + "\n" + url;
    }

    private void saveOverlayScalePercent(int percent) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt(KEY_OVERLAY_SCALE_PERCENT, clampOverlayScalePercent(percent))
                .apply();
    }

    private void updateOverlayScaleText(int percent) {
        if (overlayScaleText != null) {
            overlayScaleText.setText("\u60ac\u6d6e\u7a97\u5927\u5c0f " + clampOverlayScalePercent(percent) + "%");
        }
        updateOverlayPreviewScale(percent);
    }

    private void updateOverlayPreviewScale(int percent) {
        if (overlayPreviewPanel == null || overlayPreviewStage == null) {
            return;
        }
        float scale = clampOverlayScalePercent(percent) / 100f;
        overlayPreviewPanel.setScaleX(scale);
        overlayPreviewPanel.setScaleY(scale);
        FrameLayout.LayoutParams panelLp = (FrameLayout.LayoutParams) overlayPreviewPanel.getLayoutParams();
        panelLp.gravity = Gravity.CENTER;
        overlayPreviewPanel.setLayoutParams(panelLp);

        LinearLayout.LayoutParams stageLp = (LinearLayout.LayoutParams) overlayPreviewStage.getLayoutParams();
        stageLp.height = Math.max(dp(210), Math.round(dp(260) * scale));
        overlayPreviewStage.setLayoutParams(stageLp);
    }

    private CheckBox contentToggle(String text, String key) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        checkBox.setChecked(isOverlayContentEnabled(this, key));
        checkBox.setTextSize(14f);
        checkBox.setTextColor(0xFF0F172A);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF2563EB));
        }
        checkBox.setPadding(0, dp(2), 0, dp(2));
        checkBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            saveOverlayContentEnabled(key, isChecked);
            updateOverlayPreviewContentVisibility();
            notifyOverlayContentChanged();
        });
        return checkBox;
    }

    private CheckBox behaviorToggle(String text, String key) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        checkBox.setChecked(isBehaviorEnabled(this, key));
        checkBox.setTextSize(14f);
        checkBox.setTextColor(0xFF0F172A);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF2563EB));
        }
        checkBox.setPadding(0, dp(2), 0, dp(2));
        checkBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            saveBehaviorEnabled(key, isChecked);
            if (KEY_HIDE_MAIN_WHEN_TARGET_FOREGROUND.equals(key) && isChecked && !hasUsageStatsAccess(this)) {
                Toast.makeText(this, "请为 AMap Companion 开启使用情况访问权限", Toast.LENGTH_LONG).show();
                openUsageAccessSettings();
            }
            startOverlayService();
            notifyDisplayPolicyChanged();
        });
        return checkBox;
    }

    private void openUsageAccessSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } catch (Throwable t) {
            Toast.makeText(this, "无法打开使用情况访问设置", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveOverlayContentEnabled(String key, boolean enabled) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(key, enabled)
                .apply();
    }

    private void saveBehaviorEnabled(String key, boolean enabled) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(key, enabled)
                .apply();
    }

    private void updateOverlayPreviewContentVisibility() {
        setPreviewVisibility(previewModeText, isOverlayContentEnabled(this, KEY_SHOW_MODE));
        setPreviewVisibility(previewTurnText, isOverlayContentEnabled(this, KEY_SHOW_TURN));
        setPreviewVisibility(previewLightRow, isOverlayContentEnabled(this, KEY_SHOW_LIGHT));
        setPreviewVisibility(previewLaneSection, isOverlayContentEnabled(this, KEY_SHOW_LANE));
        setPreviewVisibility(previewEtaText, isOverlayContentEnabled(this, KEY_SHOW_ETA));
        setPreviewVisibility(previewAlertText, isOverlayContentEnabled(this, KEY_SHOW_ALERT));
        setPreviewVisibility(previewDetailText, isOverlayContentEnabled(this, KEY_SHOW_DETAIL));
    }

    private void applyOverlayPreviewStyle() {
        applyOverlayPreviewPanelStyle();
        applyOverlayPreviewTextStyle();
        if (overlayUiStyleButton != null) {
            overlayUiStyleButton.setText(overlayUiStyleButtonText());
        }
        if (overlayTextModeButton != null) {
            overlayTextModeButton.setText(textModeButtonText());
        }
        updateBackgroundOpacityText(getBackgroundOpacityPercent(this));
    }

    private void applyOverlayPreviewPanelStyle() {
        if (overlayPreviewPanel != null) {
            overlayPreviewPanel.setBackground(createPreviewPanelBackground());
        }
    }

    private void applyOverlayPreviewTextStyle() {
        int primary = previewPrimaryTextColor();
        int alert = previewAlertTextColor();
        int detail = previewDetailTextColor();
        if (previewModeText != null) {
            previewModeText.setTextColor(primary);
        }
        if (previewEtaText != null) {
            previewEtaText.setTextColor(primary);
        }
        if (previewAlertText != null) {
            previewAlertText.setTextColor(alert);
        }
        if (previewDetailText != null) {
            previewDetailText.setTextColor(detail);
        }
    }

    private GradientDrawable createPreviewPanelBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(7));
        int opacity = getBackgroundOpacityPercent(this);
        bg.setColor(withAlpha(0xFF111827, opacity));
        bg.setStroke(dp(1), withAlpha(0xFFFFFFFF, strokeOpacityForBackground(opacity)));
        return bg;
    }

    private String textModeButtonText() {
        return isAutoTextMode(this)
                ? "\u6587\u5b57\u6a21\u5f0f\uff1a\u81ea\u52a8\uff08\u6839\u636e\u900f\u660e\u5ea6\u81ea\u52a8\u5207\u6362\uff09"
                : "\u6587\u5b57\u6a21\u5f0f\uff1a\u6d45\u8272";
    }

    private String overlayUiStyleButtonText() {
        return isNewOverlayUiEnabled(this)
                ? "\u60ac\u6d6e\u7a97\u6837\u5f0f\uff1a\u65b0 UI\uff08\u6d4b\u8bd5\u4e2d\uff09"
                : "\u60ac\u6d6e\u7a97\u6837\u5f0f\uff1a\u65e7 UI";
    }

    private void chooseOverlayUiStyle() {
        String[] labels = {
                "\u65e7 UI\uff08\u9ed8\u8ba4\uff09",
                "\u65b0 UI\uff08\u5361\u7247\u6837\u5f0f\uff0c\u6d4b\u8bd5\u4e2d\uff09"
        };
        int checked = isNewOverlayUiEnabled(this) ? 1 : 0;
        new AlertDialog.Builder(this)
                .setTitle("\u9009\u62e9\u60ac\u6d6e\u7a97\u6837\u5f0f")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    saveOverlayUiStyle(which == 1 ? OVERLAY_UI_NEW : OVERLAY_UI_OLD);
                    applyOverlayPreviewStyle();
                    notifyOverlayStyleChanged();
                    dialog.dismiss();
                })
                .setNegativeButton("\u53d6\u6d88", null)
                .show();
    }

    private void chooseTextMode() {
        String[] labels = {
                "\u81ea\u52a8\u6a21\u5f0f\uff08\u6839\u636e\u80cc\u666f\u900f\u660e\u5ea6\u81ea\u52a8\u66f4\u6539\u6587\u5b57\u989c\u8272\uff09",
                "\u6d45\u8272\u6a21\u5f0f\uff08\u59cb\u7ec8\u4f7f\u7528\u6d45\u8272\u6587\u5b57\uff09"
        };
        int checked = isAutoTextMode(this) ? 0 : 1;
        new AlertDialog.Builder(this)
                .setTitle("\u9009\u62e9\u6587\u5b57\u6a21\u5f0f")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    saveOverlayTextMode(which == 0 ? TEXT_MODE_AUTO : TEXT_MODE_LIGHT);
                    applyOverlayPreviewStyle();
                    notifyOverlayStyleChanged();
                    dialog.dismiss();
                })
                .setNegativeButton("\u53d6\u6d88", null)
                .show();
    }

    private void saveOverlayTextMode(String mode) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_TEXT_MODE, TEXT_MODE_AUTO.equals(mode) ? TEXT_MODE_AUTO : TEXT_MODE_LIGHT)
                .apply();
    }

    private void saveOverlayUiStyle(String style) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_OVERLAY_UI_STYLE, OVERLAY_UI_NEW.equals(style) ? OVERLAY_UI_NEW : OVERLAY_UI_OLD)
                .apply();
    }

    private int previewPrimaryTextColor() {
        return usesDarkTextPalette(this) ? 0xFF0F172A : 0xFFE8EAED;
    }

    private int previewAlertTextColor() {
        return usesDarkTextPalette(this) ? 0xFF7C2D12 : 0xFFFFF7ED;
    }

    private int previewDetailTextColor() {
        return usesDarkTextPalette(this) ? 0xFF1E3A8A : 0xFFC7D2FE;
    }

    private void updateBackgroundOpacityText(int percent) {
        if (overlayBackgroundOpacityText != null) {
            overlayBackgroundOpacityText.setText("\u4e3b\u80cc\u666f\u900f\u660e\u5ea6 " + clampBackgroundOpacityPercent(percent) + "%");
        }
    }

    private void saveBackgroundOpacityPercent(int percent) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt(KEY_BACKGROUND_OPACITY_PERCENT, clampBackgroundOpacityPercent(percent))
                .apply();
    }

    private void migrateOverlayStylePrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.contains(KEY_BACKGROUND_OPACITY_PERCENT)) {
            return;
        }
        int opacity = prefs.getBoolean(KEY_TRANSPARENT_BACKGROUND, false)
                ? MIN_BACKGROUND_OPACITY_PERCENT
                : DEFAULT_BACKGROUND_OPACITY_PERCENT;
        prefs.edit().putInt(KEY_BACKGROUND_OPACITY_PERCENT, opacity).apply();
    }

    private void setPreviewVisibility(android.view.View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }

    private void notifyOverlayScaleChanged() {
        startOverlayService();
        Intent intent = new Intent(ACTION_OVERLAY_SCALE_CHANGED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void saveMainOverlayEnabled(boolean enabled) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MAIN_OVERLAY_ENABLED, enabled)
                .apply();
    }

    private void notifyMainOverlayChanged() {
        Intent intent = new Intent(ACTION_MAIN_OVERLAY_CHANGED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void notifyClusterMirrorChanged() {
        Intent intent = new Intent(ACTION_CLUSTER_MIRROR_CHANGED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void notifyOverlayContentChanged() {
        Intent intent = new Intent(ACTION_OVERLAY_CONTENT_CHANGED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void notifyOverlayStyleChanged() {
        Intent intent = new Intent(ACTION_OVERLAY_STYLE_CHANGED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void notifyDisplayPolicyChanged() {
        Intent intent = new Intent(ACTION_DISPLAY_POLICY_CHANGED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void stopServiceIfNoVisuals() {
        if (!isMainOverlayEnabled(this) && !isClusterMirrorEnabled(this)) {
            stopService(new Intent(this, OverlayService.class));
        }
    }

    private void saveClusterScalePercent(int percent) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt(KEY_CLUSTER_SCALE_PERCENT, clampOverlayScalePercent(percent))
                .apply();
    }

    private void updateClusterScaleText(int percent) {
        if (clusterScaleText != null) {
            clusterScaleText.setText("\u526f\u5c4f\u5927\u5c0f " + clampOverlayScalePercent(percent) + "%");
        }
    }

    private void updateClusterDisplayText() {
        if (clusterDisplayText == null) {
            return;
        }
        int selectedId = getClusterDisplayId(this);
        if (selectedId < 0) {
            clusterDisplayText.setText("\u6295\u5c4f\u5c4f\u5e55 \u00b7 \u81ea\u52a8\u9009\u62e9");
            return;
        }
        DisplayChoice selected = null;
        ArrayList<DisplayChoice> choices = getClusterDisplayChoices();
        for (DisplayChoice choice : choices) {
            if (choice.displayId == selectedId) {
                selected = choice;
                break;
            }
        }
        if (selected != null) {
            clusterDisplayText.setText("\u6295\u5c4f\u5c4f\u5e55 \u00b7 " + selected.label + " (ID " + selected.displayId + ")");
        } else {
            clusterDisplayText.setText("\u6295\u5c4f\u5c4f\u5e55 \u00b7 \u5df2\u6307\u5b9a ID " + selectedId + "\uff08\u5f53\u524d\u672a\u68c0\u6d4b\u5230\uff09");
        }
    }

    private void saveClusterDisplayId(int displayId) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt(KEY_CLUSTER_DISPLAY_ID, displayId)
                .apply();
    }

    private ArrayList<DisplayChoice> getClusterDisplayChoices() {
        ArrayList<DisplayChoice> choices = new ArrayList<>();
        DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (manager == null) {
            return choices;
        }
        Display[] displays = manager.getDisplays();
        for (Display display : displays) {
            if (display == null || display.getDisplayId() == Display.DEFAULT_DISPLAY) {
                continue;
            }
            String name = display.getName();
            if (TextUtils.isEmpty(name)) {
                name = "\u526f\u5c4f";
            }
            choices.add(new DisplayChoice(display.getDisplayId(), name));
        }
        Collections.sort(choices, Comparator.comparingInt(choice -> choice.displayId));
        return choices;
    }

    private void moveClusterBy(int dx, int dy) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int x = Math.max(0, prefs.getInt(KEY_CLUSTER_X, dp(24)) + dx);
        int y = Math.max(0, prefs.getInt(KEY_CLUSTER_Y, dp(120)) + dy);
        prefs.edit()
                .putInt(KEY_CLUSTER_X, x)
                .putInt(KEY_CLUSTER_Y, y)
                .apply();
        startOverlayService();
        notifyClusterMirrorChanged();
    }

    static int getOverlayScalePercent(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        return clampOverlayScalePercent(prefs.getInt(KEY_OVERLAY_SCALE_PERCENT, DEFAULT_OVERLAY_SCALE_PERCENT));
    }

    static float getOverlayScale(android.content.Context context) {
        return getOverlayScalePercent(context) / 100f;
    }

    static boolean isMainOverlayEnabled(android.content.Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(KEY_MAIN_OVERLAY_ENABLED, true);
    }

    static boolean isClusterMirrorEnabled(android.content.Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(KEY_CLUSTER_MIRROR_ENABLED, false);
    }

    static boolean isAutoStartEnabled(android.content.Context context) {
        return isBehaviorEnabled(context, KEY_AUTO_START_ENABLED);
    }

    static boolean isHideMainWhenTargetForegroundEnabled(android.content.Context context) {
        return isBehaviorEnabled(context, KEY_HIDE_MAIN_WHEN_TARGET_FOREGROUND);
    }

    static boolean isHideClusterWhenInactiveEnabled(android.content.Context context) {
        return isBehaviorEnabled(context, KEY_HIDE_CLUSTER_WHEN_INACTIVE);
    }

    static boolean hasUsageStatsAccess(android.content.Context context) {
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

    static int getClusterScalePercent(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        return clampOverlayScalePercent(prefs.getInt(KEY_CLUSTER_SCALE_PERCENT, DEFAULT_OVERLAY_SCALE_PERCENT));
    }

    static float getClusterScale(android.content.Context context) {
        return getClusterScalePercent(context) / 100f;
    }

    static int getClusterDisplayId(android.content.Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_CLUSTER_DISPLAY_ID, -1);
    }

    static int getClusterX(android.content.Context context, int defaultValue) {
        return Math.max(0, context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_CLUSTER_X, defaultValue));
    }

    static int getClusterY(android.content.Context context, int defaultValue) {
        return Math.max(0, context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_CLUSTER_Y, defaultValue));
    }

    static boolean isModeVisible(android.content.Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_MODE);
    }

    static boolean isTurnVisible(android.content.Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_TURN);
    }

    static boolean isLaneVisible(android.content.Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_LANE);
    }

    static boolean isLightVisible(android.content.Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_LIGHT);
    }

    static boolean isEtaVisible(android.content.Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_ETA);
    }

    static boolean isAlertVisible(android.content.Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_ALERT);
    }

    static boolean isDetailVisible(android.content.Context context) {
        return isOverlayContentEnabled(context, KEY_SHOW_DETAIL);
    }

    static boolean isTransparentBackground(android.content.Context context) {
        return getBackgroundOpacityPercent(context) <= MIN_BACKGROUND_OPACITY_PERCENT;
    }

    static boolean isAutoTextMode(android.content.Context context) {
        return TEXT_MODE_AUTO.equals(getOverlayTextMode(context));
    }

    static boolean isNewOverlayUiEnabled(android.content.Context context) {
        return OVERLAY_UI_NEW.equals(getOverlayUiStyle(context));
    }

    static boolean usesDarkTextPalette(android.content.Context context) {
        return getBackgroundOpacityPercent(context) <= 55 && isAutoTextMode(context);
    }

    static int getBackgroundOpacityPercent(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.contains(KEY_BACKGROUND_OPACITY_PERCENT)) {
            return clampBackgroundOpacityPercent(
                    prefs.getInt(KEY_BACKGROUND_OPACITY_PERCENT, DEFAULT_BACKGROUND_OPACITY_PERCENT));
        }
        return prefs.getBoolean(KEY_TRANSPARENT_BACKGROUND, false)
                ? MIN_BACKGROUND_OPACITY_PERCENT
                : DEFAULT_BACKGROUND_OPACITY_PERCENT;
    }

    static String getOverlayTextMode(android.content.Context context) {
        String mode = context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_TEXT_MODE, TEXT_MODE_AUTO);
        return TEXT_MODE_LIGHT.equals(mode) ? TEXT_MODE_LIGHT : TEXT_MODE_AUTO;
    }

    static String getOverlayUiStyle(android.content.Context context) {
        String style = context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_OVERLAY_UI_STYLE, OVERLAY_UI_OLD);
        return OVERLAY_UI_NEW.equals(style) ? OVERLAY_UI_NEW : OVERLAY_UI_OLD;
    }

    static boolean isOverlayContentEnabled(android.content.Context context, String key) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(key, true);
    }

    private static boolean isBehaviorEnabled(android.content.Context context, String key) {
        boolean defaultValue = KEY_AUTO_START_ENABLED.equals(key);
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(key, defaultValue);
    }

    private static int clampOverlayScalePercent(int percent) {
        return Math.max(MIN_OVERLAY_SCALE_PERCENT, Math.min(MAX_OVERLAY_SCALE_PERCENT, percent));
    }

    private static int clampBackgroundOpacityPercent(int percent) {
        return Math.max(MIN_BACKGROUND_OPACITY_PERCENT, Math.min(MAX_BACKGROUND_OPACITY_PERCENT, percent));
    }

    static int strokeOpacityForBackground(int opacityPercent) {
        return opacityPercent <= 0 ? 0 : Math.max(8, Math.round(opacityPercent * 0.18f));
    }

    private static int withAlpha(int color, int alphaPercent) {
        int alpha = Math.max(0, Math.min(255, Math.round(clampBackgroundOpacityPercent(alphaPercent) * 255f / 100f)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    static String getTargetPackage(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        String value = prefs.getString(KEY_TARGET_PACKAGE, DEFAULT_TARGET_PACKAGE);
        return value == null || value.length() == 0 ? DEFAULT_TARGET_PACKAGE : value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private final class TargetAppAdapter extends BaseAdapter {
        private final ArrayList<AppChoice> choices;

        TargetAppAdapter(ArrayList<AppChoice> choices) {
            this.choices = choices;
        }

        @Override
        public int getCount() {
            return choices.size();
        }

        @Override
        public Object getItem(int position) {
            return choices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppChoice choice = choices.get(position);
            LinearLayout root = new LinearLayout(MainActivity.this);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(18), dp(12), dp(18), dp(12));

            ImageView icon = new ImageView(MainActivity.this);
            icon.setImageDrawable(loadAppIcon(choice.packageName));
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(46), dp(46));
            iconLp.setMargins(0, 0, dp(14), 0);
            root.addView(icon, iconLp);

            LinearLayout content = new LinearLayout(MainActivity.this);
            content.setOrientation(LinearLayout.VERTICAL);
            root.addView(content, new LinearLayout.LayoutParams(0, -2, 1f));

            TextView title = new TextView(MainActivity.this);
            title.setText(choice.label);
            title.setTextSize(16);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextColor(0xFF111827);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            content.addView(title, new LinearLayout.LayoutParams(-1, -2));

            TextView packageView = new TextView(MainActivity.this);
            packageView.setText(choice.packageName);
            packageView.setTextSize(12);
            packageView.setTextColor(0xFF6B7280);
            packageView.setSingleLine(true);
            packageView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            LinearLayout.LayoutParams pkgLp = new LinearLayout.LayoutParams(-1, -2);
            pkgLp.setMargins(0, dp(4), 0, 0);
            content.addView(packageView, pkgLp);

            LinearLayout tags = new LinearLayout(MainActivity.this);
            tags.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams tagsLp = new LinearLayout.LayoutParams(-1, -2);
            tagsLp.setMargins(0, dp(8), 0, 0);
            content.addView(tags, tagsLp);

            tags.addView(appTag(choice.amapPackage ? "\u9ad8\u5fb7\u5305\u540d" : (choice.mapNamed ? "\u5730\u56fe\u5339\u914d" : "\u5168\u90e8\u5217\u8868"),
                    choice.amapPackage ? 0xFFEFF6FF : (choice.mapNamed ? 0xFFECFDF5 : 0xFFF3F4F6),
                    choice.amapPackage ? 0xFF1D4ED8 : (choice.mapNamed ? 0xFF047857 : 0xFF4B5563)));
            tags.addView(appTag(choice.system ? "\u7cfb\u7edf\u5e94\u7528" : "\u7528\u6237\u5e94\u7528",
                    choice.system ? 0xFFFFF7ED : 0xFFEFF6FF,
                    choice.system ? 0xFFC2410C : 0xFF1D4ED8));
            tags.addView(appTag(choice.launchable ? "\u53ef\u6253\u5f00" : "\u65e0\u684c\u9762\u56fe\u6807",
                    choice.launchable ? 0xFFF0FDFA : 0xFFFEF2F2,
                    choice.launchable ? 0xFF0F766E : 0xFFB91C1C));
            return root;
        }

        private Drawable loadAppIcon(String packageName) {
            try {
                return getPackageManager().getApplicationIcon(packageName);
            } catch (Exception ignored) {
                return getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            }
        }

        private TextView appTag(String text, int backgroundColor, int textColor) {
            TextView tag = new TextView(MainActivity.this);
            tag.setText(text);
            tag.setTextSize(11);
            tag.setTextColor(textColor);
            tag.setTypeface(Typeface.DEFAULT_BOLD);
            tag.setPadding(dp(8), dp(3), dp(8), dp(3));
            GradientDrawable background = new GradientDrawable();
            background.setColor(backgroundColor);
            background.setCornerRadius(dp(999));
            tag.setBackground(background);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(0, 0, dp(6), 0);
            tag.setLayoutParams(lp);
            return tag;
        }
    }

    private static final class AppChoice {
        final String label;
        final String packageName;
        final boolean system;
        final boolean launchable;
        final boolean mapNamed;
        final boolean amapPackage;

        AppChoice(String label, String packageName, boolean system, boolean launchable, boolean mapNamed, boolean amapPackage) {
            this.label = label;
            this.packageName = packageName;
            this.system = system;
            this.launchable = launchable;
            this.mapNamed = mapNamed;
            this.amapPackage = amapPackage;
        }
    }

    private static final class DisplayChoice {
        final int displayId;
        final String label;

        DisplayChoice(int displayId, String label) {
            this.displayId = displayId;
            this.label = label;
        }
    }
}
