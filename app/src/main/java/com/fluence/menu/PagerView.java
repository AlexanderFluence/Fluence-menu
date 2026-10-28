package com.fluence.menu;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/** Desplazamiento horizontal que se detiene en páginas completas, como el menú de CarPlay. */
public class PagerView extends HorizontalScrollView {

    public interface Listener {
        void onPageChanged(int page);
        void onSizeReady(int width, int height);
    }

    private Listener listener;
    private boolean flung;
    private int pageCount = 1;
    private int currentPage = 0;

    public PagerView(Context context) {
        super(context);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setFillViewport(true);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setPageCount(int n) {
        pageCount = Math.max(1, n);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (listener != null && w > 0 && h > 0 && (w != oldw || h != oldh)) {
            final int fw = w;
            final int fh = h;
            post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) listener.onSizeReady(fw, fh);
                }
            });
        }
    }

    @Override
    public void fling(int velocityX) {
        int w = getWidth();
        if (w <= 0) {
            super.fling(velocityX);
            return;
        }
        float pos = (float) getScrollX() / w;
        int target = velocityX > 0 ? (int) Math.floor(pos) + 1 : (int) Math.ceil(pos) - 1;
        flung = true;
        goToPage(target, true);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) flung = false;
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) flung = false;
        boolean handled = super.onTouchEvent(ev);
        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && !flung) {
            int w = getWidth();
            if (w > 0) goToPage(Math.round((float) getScrollX() / w), true);
        }
        return handled;
    }

    public void goToPage(int page, boolean smooth) {
        int p = Math.max(0, Math.min(pageCount - 1, page));
        int x = p * getWidth();
        if (smooth) {
            smoothScrollTo(x, 0);
        } else {
            scrollTo(x, 0);
        }
        setPage(p);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        int w = getWidth();
        if (w > 0) setPage(Math.round((float) l / w));
    }

    private void setPage(int p) {
        if (p != currentPage) {
            currentPage = p;
            if (listener != null) listener.onPageChanged(p);
        }
    }
}
