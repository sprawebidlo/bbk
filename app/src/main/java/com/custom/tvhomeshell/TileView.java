package com.custom.tvhomeshell;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Плитка приложения: иконка + подпись. */
public class TileView extends FrameLayout {

    public interface FocusListener {
        void onFocus(TileView tile, boolean focused);
    }

    public final ImageView icon;
    public final TextView label;
    public AppEntry entry;

    private FocusListener focusListener;

    public TileView(Context c, int iconDp, boolean dock) {
        super(c);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClipToPadding(false);

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setClipChildren(false);

        icon = new ImageView(c);
        icon.setClipChildren(false);
        int size = Ui.dp(c, iconDp);
        box.addView(icon, new LinearLayout.LayoutParams(size, size));

        label = new TextView(c);
        label.setTextColor(Color.parseColor("#D6D6DD"));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, dock ? 12f : 11f);
        label.setSingleLine(true);
        label.setGravity(Gravity.CENTER);
        label.setAlpha(0.85f);
        label.setVisibility(dock ? View.GONE : View.VISIBLE);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(c, 6);
        box.addView(label, lp);

        addView(box, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.animate()
                        .scaleX(hasFocus ? 1.08f : 1f)
                        .scaleY(hasFocus ? 1.08f : 1f)
                        .setDuration(140)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                label.setAlpha(hasFocus ? 1f : 0.85f);
                if (focusListener != null) {
                    focusListener.onFocus(TileView.this, hasFocus);
                }
            }
        });
    }

    public void setFocusListener(FocusListener l) {
        focusListener = l;
    }

    public void bind(AppEntry e) {
        entry = e;
        icon.setImageDrawable(e.icon);
        label.setText(e.label);
    }
}
