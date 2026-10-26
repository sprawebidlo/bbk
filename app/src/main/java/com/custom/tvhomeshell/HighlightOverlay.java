package com.custom.tvhomeshell;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/** Единая «стеклянная» подсветка, переезжающая от плитки к плитке. */
public class HighlightOverlay extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF from = new RectF();
    private final RectF to = new RectF();
    private final RectF cur = new RectF();
    private float radius;
    private ValueAnimator animator;

    public HighlightOverlay(Context c) {
        super(c);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(30, 255, 255, 255));
    }

    public void moveTo(RectF target, float cornerRadius) {
        from.set(cur);
        to.set(target);
        radius = cornerRadius;
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(190L);
        animator.setInterpolator(new OvershootInterpolator(0.8f));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float f = (float) a.getAnimatedValue();
                cur.left = from.left + (to.left - from.left) * f;
                cur.top = from.top + (to.top - from.top) * f;
                cur.right = from.right + (to.right - from.right) * f;
                cur.bottom = from.bottom + (to.bottom - from.bottom) * f;
                invalidate();
            }
        });
        animator.start();
    }

    /** Мгновенно поставить подсветку на место (первый показ, без анимации из нуля). */
    public void jumpTo(RectF target, float cornerRadius) {
        from.set(target);
        to.set(target);
        cur.set(target);
        radius = cornerRadius;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (cur.width() > 0 && cur.height() > 0) {
            canvas.drawRoundRect(cur, radius, radius, paint);
        }
    }
}
