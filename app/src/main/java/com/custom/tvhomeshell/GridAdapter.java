package com.custom.tvhomeshell;

import android.view.KeyEvent;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Сетка приложений. */
public class GridAdapter extends RecyclerView.Adapter<GridAdapter.Holder> {

    public interface Callbacks {
        void onTileFocus(TileView tile, AppEntry entry, int position);

        void onOpen(AppEntry entry);

        boolean onTileKey(TileView tile, int keyCode, int position);
    }

    private final List<AppEntry> items;
    private final int iconDp;
    private final Callbacks cb;

    public GridAdapter(List<AppEntry> items, int iconDp, Callbacks cb) {
        this.items = items;
        this.iconDp = iconDp;
        this.cb = cb;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TileView tile;

        Holder(TileView t) {
            super(t);
            tile = t;
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        TileView tile = new TileView(parent.getContext(), iconDp, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = Ui.dp(parent.getContext(), 6);
        lp.setMargins(m, m, m, m);
        tile.setLayoutParams(lp);
        return new Holder(tile);
    }

    @Override
    public void onBindViewHolder(Holder h, int position) {
        final AppEntry entry = items.get(position);
        final int pos = position;
        h.tile.bind(entry);
        h.tile.setFocusListener((tile, focused) -> {
            if (focused) {
                cb.onTileFocus(tile, entry, pos);
            }
        });
        h.tile.setOnClickListener(v -> cb.onOpen(entry));
        h.tile.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return cb.onTileKey(h.tile, keyCode, pos);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
