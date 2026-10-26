package com.custom.tvhomeshell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Домашний экран: док снизу, сетка приложений, часы и пилюли сверху. */
public class MainActivity extends Activity implements GridAdapter.Callbacks {

    private static final String PREFS = "tvhome";
    private static final String KEY_LAST = "last_pkg";

    private FrameLayout root;
    private HighlightOverlay highlight;
    private View glow;
    private LinearLayout dockRow;
    private LinearLayout dockBand;
    private TextView greeting;
    private TextView clock;
    private TextView date;
    private TextView bigName;
    private TextView pillInputs;
    private TextView pillWifi;
    private TextView pillSettings;
    private RecyclerView grid;
    private GridAdapter adapter;
    private PopupWindow inputsPopup;

    private final List<AppEntry> apps = new ArrayList<>();
    private int columns = 5;
    private int dockSlots = 4;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, 30_000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        columns = getResources().getInteger(R.integer.columns);
        dockSlots = getResources().getInteger(R.integer.dock_slots);

        buildUi();

        apps.addAll(AppRepository.load(this));
        bindGrid();
        bindDock();
        updateClock();
    }

    // ---------------------------------------------------------------- UI

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        glow = new View(this);
        root.addView(glow, match());

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        column.setPadding(Ui.dp(this, 44), Ui.dp(this, 30),
                Ui.dp(this, 44), Ui.dp(this, 28));
        root.addView(column, match());

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.TOP);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);

        greeting = new TextView(this);
        greeting.setTextColor(0xFF9A9AA6);
        greeting.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);

        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 46f);
        clock.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        clock.setIncludeFontPadding(false);
        clock.setPadding(0, Ui.dp(this, 2), 0, 0);

        date = new TextView(this);
        date.setTextColor(0xFF8A8A96);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

        left.addView(greeting);
        left.addView(clock);
        left.addView(date);

        top.addView(left, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        top.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.HORIZONTAL);
        right.setGravity(Gravity.TOP);

        pillInputs = pill("Входы");
        pillWifi = pill(ssid());
        pillSettings = pill("Настройки");

        right.addView(pillInputs, pillParams(0));
        right.addView(pillWifi, pillParams(8));
        right.addView(pillSettings, pillParams(8));

        top.addView(right, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        column.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        grid = new RecyclerView(this);
        grid.setClipToPadding(false);
        grid.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 6));
        grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
        grid.setItemAnimator(null);
        grid.setFocusable(false);
        grid.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        column.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        bigName = new TextView(this);
        bigName.setTextColor(0xFFEDEDF2);
        bigName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f);
        bigName.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        bigName.setGravity(Gravity.CENTER_HORIZONTAL);
        bigName.setSingleLine(true);
        bigName.setVisibility(View.INVISIBLE);
        column.addView(bigName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46)));

        dockBand = new LinearLayout(this);
        dockBand.setOrientation(LinearLayout.HORIZONTAL);
        dockBand.setBackground(Ui.roundStroke(0x12FFFFFF, 0x1FFFFFFF,
                Ui.dp(this, 1), Ui.dp(this, 26)));
        dockBand.setPadding(Ui.dp(this, 10), Ui.dp(this, 10),
                Ui.dp(this, 10), Ui.dp(this, 10));
        dockBand.setClipChildren(false);
        dockBand.setClipToPadding(false);

        dockRow = new LinearLayout(this);
        dockRow.setOrientation(LinearLayout.HORIZONTAL);
        dockRow.setGravity(Gravity.CENTER_VERTICAL);
        dockRow.setClipChildren(false);
        dockBand.addView(dockRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        column.addView(dockBand, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        highlight = new HighlightOverlay(this);
        root.addView(highlight, match());

        pillInputs.setOnClickListener(v -> showInputs());
        pillWifi.setOnClickListener(v -> openSettings());
        pillSettings.setOnClickListener(v -> openSettings());

        pillInputs.setOnKeyListener((v, keyCode, event) -> pillKey(keyCode, event, true, false));
        pillWifi.setOnKeyListener((v, keyCode, event) -> pillKey(keyCode, event, false, false));
        pillSettings.setOnKeyListener((v, keyCode, event) -> pillKey(keyCode, event, false, true));

        setContentView(root);
    }

    private FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private TextView pill(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 8));
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused},
                Ui.roundStroke(0x38FFFFFF, 0x66FFFFFF, Ui.dp(this, 1), Ui.dp(this, 22)));
        bg.addState(new int[0],
                Ui.roundStroke(0x1FFFFFFF, 0x26FFFFFF, Ui.dp(this, 1), Ui.dp(this, 22)));
        tv.setBackground(bg);
        tv.setFocusable(true);
        tv.setFocusableInTouchMode(true);
        tv.setClickable(true);
        return tv;
    }

    private LinearLayout.LayoutParams pillParams(int startMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(Ui.dp(this, startMargin));
        return lp;
    }

    // ------------------------------------------------------------- grid

    private void bindGrid() {
        adapter = new GridAdapter(apps, 60, this);
        grid.setAdapter(adapter);
        grid.setLayoutManager(new GridLayoutManager(this, columns));
    }

    private void bindDock() {
        dockRow.removeAllViews();
        String[] ids = getResources().getStringArray(R.array.dock_apps);
        int used = 0;
        for (int i = 0; i < ids.length && used < dockSlots; i++) {
            AppEntry entry = resolve(ids[i]);
            if (entry == null) {
                continue;
            }
            TileView tile = new TileView(this, 54, true);
            tile.bind(entry);
            final AppEntry bound = entry;
            tile.setFocusListener((t, focused) -> {
                if (focused) {
                    onTileFocused(t, bound, true);
                }
            });
            tile.setOnClickListener(v -> open(bound));
            tile.setOnKeyListener((v, keyCode, event) -> dockKey(keyCode, event));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
            dockRow.addView(tile, lp);
            used++;
        }
        for (int i = used; i < dockSlots; i++) {
            View spacer = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1f);
            lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
            dockRow.addView(spacer, lp);
        }
    }

    private AppEntry resolve(String id) {
        if ("settings".equals(id)) {
            Drawable icon;
            try {
                icon = getPackageManager().getApplicationIcon("com.cvte.tv.setting");
            } catch (Exception e) {
                icon = getPackageManager().getDefaultActivityIcon();
            }
            return new AppEntry("settings", "Настройки", icon, false);
        }
        for (AppEntry e : apps) {
            if (e.pkg.equals(id)) {
                return e;
            }
        }
        try {
            Intent launch = getPackageManager().getLeanbackLaunchIntentForPackage(id);
            if (launch == null) {
                launch = getPackageManager().getLaunchIntentForPackage(id);
            }
            if (launch != null) {
                String label = getPackageManager()
                        .getApplicationLabel(getPackageManager().getApplicationInfo(id, 0))
                        .toString();
                return new AppEntry(id, label, getPackageManager().getApplicationIcon(id), true);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ------------------------------------------------------------ focus

    private void onTileFocused(TileView tile, AppEntry entry, boolean dock) {
        moveHighlight(tile);
        // Первый показ: размеры плитки могут быть ещё нулевыми — повторим после разметки.
        tile.post(() -> moveHighlight(tile));
        setGlow(Ui.dominant(entry.icon));
        if (dock) {
            bigName.setText(entry.label);
            bigName.setVisibility(View.VISIBLE);
        } else {
            bigName.setVisibility(View.INVISIBLE);
        }
        saveLast(entry.pkg);
    }

    private void moveHighlight(View target) {
        if (target == null || target.getWidth() == 0) {
            return;
        }
        int[] t = new int[2];
        int[] r = new int[2];
        target.getLocationInWindow(t);
        root.getLocationInWindow(r);
        float left = t[0] - r[0] - Ui.dp(this, 4);
        float top = t[1] - r[1] - Ui.dp(this, 4);
        float right = left + target.getWidth() + Ui.dp(this, 8);
        float bottom = top + target.getHeight() + Ui.dp(this, 8);
        highlight.moveTo(new RectF(left, top, right, bottom), Ui.dp(this, 20));
    }

    private void setGlow(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        d.setGradientRadius(getResources().getDisplayMetrics().heightPixels * 0.9f);
        d.setGradientCenter(0.5f, 0.92f);
        d.setColors(new int[]{Ui.withAlpha(color, 60), Color.TRANSPARENT});
        glow.setBackground(d);
    }

    private void restoreFocus() {
        String last = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_LAST, null);
        if (last != null) {
            for (int i = 0; i < dockRow.getChildCount(); i++) {
                View v = dockRow.getChildAt(i);
                if (v instanceof TileView) {
                    TileView t = (TileView) v;
                    if (t.entry != null && last.equals(t.entry.pkg)) {
                        t.requestFocus();
                        return;
                    }
                }
            }
            for (int i = 0; i < apps.size(); i++) {
                if (apps.get(i).pkg.equals(last)) {
                    final int pos = i;
                    grid.scrollToPosition(pos);
                    grid.post(() -> {
                        RecyclerView.ViewHolder vh = grid.findViewHolderForAdapterPosition(pos);
                        if (vh != null) {
                            vh.itemView.requestFocus();
                        }
                    });
                    return;
                }
            }
        }
        focusDockFirst();
    }

    private void saveLast(String pkg) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_LAST, pkg).apply();
    }

    private void focusDockFirst() {
        for (int i = 0; i < dockRow.getChildCount(); i++) {
            View v = dockRow.getChildAt(i);
            if (v instanceof TileView && v.requestFocus()) {
                return;
            }
        }
        focusGridFirst();
    }

    private void focusGridFirst() {
        if (apps.isEmpty()) {
            pillInputs.requestFocus();
            return;
        }
        grid.scrollToPosition(0);
        grid.post(() -> {
            RecyclerView.ViewHolder vh = grid.findViewHolderForAdapterPosition(0);
            if (vh != null) {
                vh.itemView.requestFocus();
            } else {
                pillInputs.requestFocus();
            }
        });
    }

    private boolean pillKey(int keyCode, KeyEvent event, boolean first, boolean last) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            focusGridFirst();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && first) {
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && last) {
            return true;
        }
        return false;
    }

    private boolean dockKey(int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            pillInputs.requestFocus();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            return true;
        }
        return false;
    }

    // --------------------------------------------------- grid callbacks

    @Override
    public void onTileFocus(TileView tile, AppEntry entry, int position) {
        onTileFocused(tile, entry, false);
    }

    @Override
    public void onOpen(AppEntry entry) {
        open(entry);
    }

    @Override
    public boolean onTileKey(TileView tile, int keyCode, int position) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && position < columns) {
            pillInputs.requestFocus();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && position >= apps.size() - columns) {
            focusDockFirst();
            return true;
        }
        return false;
    }

    // --------------------------------------------------------- actions

    private void open(AppEntry entry) {
        if ("settings".equals(entry.pkg)) {
            openSettings();
            return;
        }
        Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(entry.pkg);
        if (intent == null) {
            intent = getPackageManager().getLaunchIntentForPackage(entry.pkg);
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }

    private void openSettings() {
        try {
            Intent i = new Intent(android.provider.Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage("com.cvte.tv.setting");
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    return;
                }
            } catch (Exception ignored) {
            }
            Toast.makeText(this, "Настройки не найдены", Toast.LENGTH_SHORT).show();
        }
    }

    private void showInputs() {
        List<TvInputInfo> inputs = new ArrayList<>();
        try {
            TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
            if (tim != null) {
                inputs.addAll(tim.getTvInputList());
            }
        } catch (Exception ignored) {
        }

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackground(Ui.roundStroke(0xF2101016, 0x33FFFFFF,
                Ui.dp(this, 1), Ui.dp(this, 16)));
        column.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));

        if (inputs.isEmpty()) {
            column.addView(inputRow("Нет доступных входов"));
        } else {
            for (TvInputInfo info : inputs) {
                String label;
                try {
                    label = info.loadLabel(this).toString();
                } catch (Exception e) {
                    label = info.getId();
                }
                TextView row = inputRow(label);
                final TvInputInfo bound = info;
                row.setOnClickListener(v -> {
                    openInput(bound);
                    dismissInputs();
                });
                column.addView(row);
            }
        }

        inputsPopup = new PopupWindow(column, Ui.dp(this, 300),
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        inputsPopup.setBackgroundDrawable(null);
        inputsPopup.setOutsideTouchable(true);
        inputsPopup.setFocusable(true);
        inputsPopup.showAsDropDown(pillInputs, 0, Ui.dp(this, 8));
    }

    private TextView inputRow(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFEDEDF2);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        tv.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 10));
        tv.setFocusable(true);
        tv.setFocusableInTouchMode(true);
        tv.setClickable(true);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, Ui.round(0x33FFFFFF, Ui.dp(this, 10)));
        bg.addState(new int[0], Ui.round(0x00000000, Ui.dp(this, 10)));
        tv.setBackground(bg);
        return tv;
    }

    private void openInput(TvInputInfo info) {
        try {
            ServiceInfo si = info.getServiceInfo();
            if (si != null) {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setPackage(si.packageName);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        } catch (Exception ignored) {
        }
    }

    private void dismissInputs() {
        if (inputsPopup != null) {
            inputsPopup.dismiss();
            inputsPopup = null;
        }
    }

    // ------------------------------------------------------------ misc

    private String ssid() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null) {
                String s = wm.getConnectionInfo().getSSID();
                if (s != null) {
                    s = s.replace("\"", "");
                    if (!s.isEmpty() && !"<unknown>".equals(s) && !"0x".equals(s)) {
                        return s;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "Wi-Fi";
    }

    private void updateClock() {
        Date now = new Date();
        clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        date.setText(new SimpleDateFormat("EEEE, d MMMM", new Locale("ru", "RU")).format(now));
        greeting.setText(greetingText());
    }

    private String greetingText() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String base;
        if (hour < 5) {
            base = "Доброй ночи";
        } else if (hour < 12) {
            base = "Доброе утро";
        } else if (hour < 18) {
            base = "Добрый день";
        } else {
            base = "Добрый вечер";
        }
        String name = getString(R.string.greeting_name);
        return (name == null || name.isEmpty()) ? base : base + ", " + name;
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        root.postDelayed(this::restoreFocus, 250L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
        dismissInputs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
        // Лаунчер не выходит по «Назад»: остаётся домашним экраном.
    }
}
