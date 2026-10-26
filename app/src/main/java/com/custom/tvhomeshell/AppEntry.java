package com.custom.tvhomeshell;

import android.graphics.drawable.Drawable;

/** Одно приложение в сетке или доке. */
public class AppEntry {

    public final String pkg;
    public final String label;
    public final Drawable icon;
    public final boolean tv;

    public AppEntry(String pkg, String label, Drawable icon, boolean tv) {
        this.pkg = pkg;
        this.label = label;
        this.icon = icon;
        this.tv = tv;
    }
}
