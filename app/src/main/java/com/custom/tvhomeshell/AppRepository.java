package com.custom.tvhomeshell;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Собирает запускаемые приложения: сначала ТВ-приложения, затем остальные. */
public final class AppRepository {

    private AppRepository() {
    }

    public static List<AppEntry> load(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Set<String> seen = new HashSet<>();
        List<AppEntry> tv = new ArrayList<>();
        List<AppEntry> other = new ArrayList<>();

        collect(ctx, pm, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
                seen, tv, true);
        collect(ctx, pm, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                seen, other, false);

        Collator collator = Collator.getInstance(new Locale("ru", "RU"));
        Collections.sort(tv, (a, b) -> collator.compare(a.label, b.label));
        Collections.sort(other, (a, b) -> collator.compare(a.label, b.label));

        List<AppEntry> all = new ArrayList<>(tv.size() + other.size());
        all.addAll(tv);
        all.addAll(other);
        return all;
    }

    private static void collect(Context ctx, PackageManager pm, Intent intent,
                                Set<String> seen, List<AppEntry> out, boolean tv) {
        List<ResolveInfo> found = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo ri : found) {
            if (ri.activityInfo == null) {
                continue;
            }
            String pkg = ri.activityInfo.packageName;
            if (pkg == null || pkg.equals(ctx.getPackageName()) || seen.contains(pkg)) {
                continue;
            }
            Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (launch == null) {
                launch = pm.getLaunchIntentForPackage(pkg);
            }
            if (launch == null) {
                continue;
            }
            Drawable icon;
            CharSequence label;
            try {
                icon = ri.loadIcon(pm);
                label = ri.loadLabel(pm);
            } catch (Exception e) {
                continue;
            }
            if (icon == null || label == null) {
                continue;
            }
            seen.add(pkg);
            out.add(new AppEntry(pkg, label.toString(), icon, tv));
        }
    }
}
