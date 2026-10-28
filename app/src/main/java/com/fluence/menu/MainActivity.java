package com.fluence.menu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity implements PagerView.Listener {

    private static final String PREFS = "menu";
    private static final String KEY_DOCK = "dock";
    private static final String KEY_DOCK_INIT = "dock_init";
    private static final String KEY_HIDDEN = "hidden";
    private static final int MAX_DOCK = 4;

    /** Apps que se fijan en la barra la primera vez, si están instaladas. */
    private static final String[] DEFAULT_DOCK = {
            "com.spotify.music", "zlink", "com.google.android.apps.maps", "com.waze"
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private List<AppItem> allApps = new ArrayList<>();

    private PagerView pager;
    private LinearLayout pagesContainer;
    private LinearLayout dots;
    private LinearLayout dockApps;
    private TextView clock;

    private float density;
    private int pageW;
    private int pageH;
    private boolean loading;
    private boolean reloadAgain;

    private final Runnable reloadRunnable = new Runnable() {
        @Override
        public void run() {
            loadApps();
        }
    };

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.removeCallbacks(reloadRunnable);
            handler.postDelayed(reloadRunnable, 600);
        }
    };

    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateClock();
        }
    };

    // ---------------------------------------------------------------- ciclo de vida

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(buildUi());
        registerReceivers();
        updateClock();
        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateClock();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (pager != null) pager.goToPage(0, true);
    }

    @Override
    public void onBackPressed() {
        if (pager != null && pager.getCurrentPage() > 0) {
            pager.goToPage(0, true);
            return;
        }
        Intent i = getIntent();
        boolean isHome = i != null && i.hasCategory(Intent.CATEGORY_HOME);
        if (!isHome) super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(packageReceiver);
        } catch (Exception ignored) {
        }
        try {
            unregisterReceiver(timeReceiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    private void registerReceivers() {
        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pf.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pf.addAction(Intent.ACTION_PACKAGE_REPLACED);
        pf.addDataScheme("package");
        registerReceiver(packageReceiver, pf);

        IntentFilter tf = new IntentFilter();
        tf.addAction(Intent.ACTION_TIME_TICK);
        tf.addAction(Intent.ACTION_TIME_CHANGED);
        tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeReceiver, tf);
    }

    // ---------------------------------------------------------------- interfaz

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF14213D, 0xFF070B14, 0xFF000000}));

        // Barra lateral: hora, apps fijadas y botón de inicio
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.VERTICAL);
        dock.setGravity(Gravity.CENTER_HORIZONTAL);
        dock.setBackgroundColor(0xB3000000);
        dock.setPadding(0, px(14), 0, px(14));
        root.addView(dock, new LinearLayout.LayoutParams(px(96), ViewGroup.LayoutParams.MATCH_PARENT));

        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        clock.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        clock.setGravity(Gravity.CENTER);
        dock.addView(clock, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dockApps = new LinearLayout(this);
        dockApps.setOrientation(LinearLayout.VERTICAL);
        dockApps.setGravity(Gravity.CENTER);
        dock.addView(dockApps, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        ImageView home = new ImageView(this);
        home.setImageDrawable(IconFactory.gridGlyph());
        home.setPadding(px(15), px(15), px(15), px(15));
        GradientDrawable homeBg = new GradientDrawable();
        homeBg.setColor(0x33FFFFFF);
        homeBg.setCornerRadius(px(14));
        home.setBackground(homeBg);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.goToPage(0, true);
            }
        });
        home.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showSettings();
                return true;
            }
        });
        addPressEffect(home);
        dock.addView(home, new LinearLayout.LayoutParams(px(56), px(56)));

        // Zona principal: páginas de íconos y puntos indicadores
        FrameLayout main = new FrameLayout(this);
        root.addView(main, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        pager = new PagerView(this);
        pager.setListener(this);
        pagesContainer = new LinearLayout(this);
        pagesContainer.setOrientation(LinearLayout.HORIZONTAL);
        pager.addView(pagesContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        main.addView(pager, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, px(20), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dlp.bottomMargin = px(8);
        main.addView(dots, dlp);

        return root;
    }

    @Override
    public void onSizeReady(int width, int height) {
        pageW = width;
        pageH = height;
        rebuildPages();
        refreshDock();
    }

    @Override
    public void onPageChanged(int page) {
        updateDots(page);
    }

    private void rebuildPages() {
        if (pageW <= 0 || pageH <= 0 || pagesContainer == null) return;

        List<AppItem> apps = visibleApps();
        int usableH = pageH - px(44);
        int cols = clamp(pageW / px(150), 3, 6);
        int rows = clamp(usableH / px(140), 2, 4);
        int perPage = cols * rows;
        int pages = Math.max(1, (apps.size() + perPage - 1) / perPage);
        int cellH = usableH / rows;
        int cellW = pageW / cols;
        int iconSize = Math.max(px(40), Math.min(Math.round(cellW * 0.62f), cellH - px(34)));

        int keepPage = pager.getCurrentPage();
        pagesContainer.removeAllViews();

        for (int p = 0; p < pages; p++) {
            LinearLayout page = new LinearLayout(this);
            page.setOrientation(LinearLayout.VERTICAL);
            page.setPadding(0, px(10), 0, 0);
            for (int r = 0; r < rows; r++) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                for (int c = 0; c < cols; c++) {
                    int idx = p * perPage + r * cols + c;
                    View cell = idx < apps.size() ? makeCell(apps.get(idx), iconSize) : new View(this);
                    row.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
                }
                page.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellH));
            }
            pagesContainer.addView(page, new LinearLayout.LayoutParams(pageW, pageH));
        }

        pager.setPageCount(pages);
        buildDots(pages);
        final int target = Math.min(keepPage, pages - 1);
        pager.post(new Runnable() {
            @Override
            public void run() {
                pager.goToPage(target, false);
            }
        });
        updateDots(target);
    }

    private View makeCell(final AppItem app, int iconSize) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);

        ImageView iv = new ImageView(this);
        iv.setImageBitmap(app.icon);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cell.addView(iv, new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView tv = new TextView(this);
        tv.setText(app.label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(px(6), 0, px(6), 0);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = px(6);
        cell.addView(tv, tlp);

        cell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launch(app);
            }
        });
        cell.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showAppMenu(v, app, false);
                return true;
            }
        });
        addPressEffect(cell);
        return cell;
    }

    private void refreshDock() {
        if (dockApps == null) return;
        dockApps.removeAllViews();
        int size = px(58);
        int slot = size + px(16);
        int maxFit = MAX_DOCK;
        if (pageH > 0) {
            int avail = pageH - px(28) - px(32) - px(56) - px(8);
            maxFit = Math.max(1, Math.min(MAX_DOCK, avail / slot));
        }
        int shown = 0;
        for (String key : getDock()) {
            if (shown >= maxFit) break;
            final AppItem app = findApp(key);
            if (app == null) continue;
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(app.icon);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launch(app);
                }
            });
            iv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showAppMenu(v, app, true);
                    return true;
                }
            });
            addPressEffect(iv);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.topMargin = px(8);
            lp.bottomMargin = px(8);
            dockApps.addView(iv, lp);
            shown++;
        }
    }

    private void buildDots(int pages) {
        dots.removeAllViews();
        dots.setVisibility(pages > 1 ? View.VISIBLE : View.INVISIBLE);
        for (int i = 0; i < pages; i++) {
            View d = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(0x55FFFFFF);
            d.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(7), px(7));
            lp.leftMargin = px(4);
            lp.rightMargin = px(4);
            dots.addView(d, lp);
        }
    }

    private void updateDots(int page) {
        if (dots == null) return;
        for (int i = 0; i < dots.getChildCount(); i++) {
            Drawable bg = dots.getChildAt(i).getBackground();
            if (bg instanceof GradientDrawable) {
                ((GradientDrawable) bg).setColor(i == page ? 0xFFFFFFFF : 0x55FFFFFF);
            }
        }
    }

    private void addPressEffect(View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private void updateClock() {
        if (clock != null) {
            clock.setText(android.text.format.DateFormat.getTimeFormat(this).format(new Date()));
        }
    }

    // ---------------------------------------------------------------- apps

    private void loadApps() {
        if (loading) {
            reloadAgain = true;
            return;
        }
        loading = true;
        final int iconPx = px(96);
        final String myPackage = getPackageName();
        final PackageManager pm = getPackageManager();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent query = new Intent(Intent.ACTION_MAIN);
                query.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> infos;
                try {
                    infos = pm.queryIntentActivities(query, 0);
                } catch (Exception e) {
                    infos = new ArrayList<>();
                }

                final List<AppItem> list = new ArrayList<>();
                for (ResolveInfo ri : infos) {
                    if (ri.activityInfo == null) continue;
                    String pkg = ri.activityInfo.packageName;
                    if (myPackage.equals(pkg)) continue;
                    try {
                        String label = String.valueOf(ri.loadLabel(pm));
                        Drawable icon = null;
                        try {
                            icon = ri.loadIcon(pm);
                        } catch (Exception ignored) {
                        }
                        Bitmap bmp = IconFactory.tile(icon, iconPx);
                        list.add(new AppItem(label, new ComponentName(pkg, ri.activityInfo.name), bmp));
                    } catch (Exception ignored) {
                        // una app con ícono dañado no debe tumbar el menú
                    }
                }

                final Collator collator = Collator.getInstance();
                collator.setStrength(Collator.PRIMARY);
                Collections.sort(list, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem a, AppItem b) {
                        return collator.compare(a.label, b.label);
                    }
                });

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;
                        allApps = list;
                        loading = false;
                        ensureDefaultDock();
                        rebuildPages();
                        refreshDock();
                        if (reloadAgain) {
                            reloadAgain = false;
                            loadApps();
                        }
                    }
                });
            }
        }).start();
    }

    private void launch(AppItem app) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setComponent(app.component);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(i);
        } catch (Exception e) {
            toast("No se pudo abrir " + app.label);
        }
    }

    private List<AppItem> visibleApps() {
        Set<String> hidden = getHidden();
        List<AppItem> out = new ArrayList<>();
        for (AppItem a : allApps) {
            if (!hidden.contains(a.key())) out.add(a);
        }
        return out;
    }

    private AppItem findApp(String key) {
        for (AppItem a : allApps) {
            if (a.key().equals(key)) return a;
        }
        return null;
    }

    // ---------------------------------------------------------------- menús

    private void showAppMenu(View anchor, final AppItem app, boolean inDock) {
        PopupMenu popup = new PopupMenu(this, anchor);
        Menu m = popup.getMenu();
        boolean pinned = getDock().contains(app.key());
        m.add(0, 1, 0, pinned ? "Quitar de la barra" : "Fijar en la barra");
        if (!inDock) m.add(0, 2, 1, "Ocultar del menú");
        m.add(0, 3, 2, "Información de la app");
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 1:
                        togglePin(app);
                        return true;
                    case 2:
                        hide(app);
                        return true;
                    case 3:
                        openInfo(app);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    private void showSettings() {
        String[] items = {"Mostrar apps ocultas", "Elegir pantalla de inicio predeterminada"};
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Fluence Menu")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showHiddenDialog();
                        } else {
                            openHomeSettings();
                        }
                    }
                })
                .show();
    }

    private void showHiddenDialog() {
        Set<String> hidden = getHidden();
        final List<AppItem> hiddenApps = new ArrayList<>();
        for (AppItem a : allApps) {
            if (hidden.contains(a.key())) hiddenApps.add(a);
        }
        if (hiddenApps.isEmpty()) {
            toast("No hay apps ocultas");
            return;
        }
        String[] labels = new String[hiddenApps.size()];
        final boolean[] checked = new boolean[hiddenApps.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = hiddenApps.get(i).label;

        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Marca las apps que quieres volver a mostrar")
                .setMultiChoiceItems(labels, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checked[which] = isChecked;
                    }
                })
                .setPositiveButton("Mostrar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Set<String> h = new HashSet<>(getHidden());
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) h.remove(hiddenApps.get(i).key());
                        }
                        prefs.edit().putStringSet(KEY_HIDDEN, h).apply();
                        rebuildPages();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void openHomeSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
                toast("No se pudieron abrir los ajustes");
            }
        }
    }

    private void openInfo(AppItem app) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", app.packageName(), null));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            toast("No se pudo abrir la información");
        }
    }

    // ---------------------------------------------------------------- preferencias

    private List<String> getDock() {
        List<String> out = new ArrayList<>();
        String s = prefs.getString(KEY_DOCK, "");
        if (s == null || s.isEmpty()) return out;
        for (String part : s.split("\\|")) {
            if (!part.isEmpty()) out.add(part);
        }
        return out;
    }

    private void saveDock(List<String> dock) {
        prefs.edit().putString(KEY_DOCK, TextUtils.join("|", dock)).apply();
    }

    private void togglePin(AppItem app) {
        List<String> dock = getDock();
        if (dock.contains(app.key())) {
            dock.remove(app.key());
        } else {
            if (dock.size() >= MAX_DOCK) {
                toast("La barra admite hasta " + MAX_DOCK + " apps");
                return;
            }
            dock.add(app.key());
        }
        saveDock(dock);
        refreshDock();
    }

    private Set<String> getHidden() {
        Set<String> s = prefs.getStringSet(KEY_HIDDEN, null);
        return s == null ? new HashSet<String>() : s;
    }

    private void hide(AppItem app) {
        Set<String> h = new HashSet<>(getHidden());
        h.add(app.key());
        prefs.edit().putStringSet(KEY_HIDDEN, h).apply();
        rebuildPages();
        toast(app.label + " oculta. Mantén presionado el botón de inicio para volver a mostrarla.");
    }

    private void ensureDefaultDock() {
        if (prefs.getBoolean(KEY_DOCK_INIT, false) || allApps.isEmpty()) return;
        List<String> dock = new ArrayList<>();
        for (String wanted : DEFAULT_DOCK) {
            if (dock.size() >= MAX_DOCK) break;
            for (AppItem a : allApps) {
                String pkg = a.packageName();
                boolean match = wanted.contains(".")
                        ? pkg.equals(wanted)
                        : pkg.toLowerCase().contains(wanted);
                if (match && !dock.contains(a.key())) {
                    dock.add(a.key());
                    break;
                }
            }
        }
        saveDock(dock);
        prefs.edit().putBoolean(KEY_DOCK_INIT, true).apply();
    }

    // ---------------------------------------------------------------- utilidades

    private int px(float dp) {
        return Math.round(dp * density);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
