package com.einmalfel.podlisten;


import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashSet;
import java.util.Set;

public class PodcastListAdapter extends CursorRecyclerAdapter<PodcastViewHolder> {
  public interface ItemClickListener {
    /**
     * @return true if event was consumed
     */
    boolean onLongTap(long id, String title);

    void onButtonTap(long id, String title);
  }

  private static final String TAG = "PLA";
  private final ItemClickListener listener;
  private final Set<Long> expandedElements = new HashSet<>(5);

  public PodcastListAdapter(Cursor cursor, ItemClickListener listener) {
    super(cursor);
    setHasStableIds(true);
    this.listener = listener;
  }

  void setExpanded(long id, boolean expanded, final int position) {
    if (!expandedElements.contains(id) && expanded) {
      expandedElements.add(id);
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          notifyItemChanged(position);
        }
      });
    } else if (expandedElements.contains(id) && !expanded) {
      expandedElements.remove(id);
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          notifyItemChanged(position);
        }
      });
    }
  }

  @Override
  public void onBindViewHolderCursor(PodcastViewHolder holder, Cursor cursor) {
    long id = cursor.getLong(cursor.getColumnIndexOrThrow(Provider.K_ID));
    holder.bind(cursor.getInt(cursor.getColumnIndexOrThrow(Provider.K_PSTATE)),
                cursor.getString(cursor.getColumnIndexOrThrow(Provider.K_PNAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(Provider.K_PDESCR)),
                cursor.getString(cursor.getColumnIndexOrThrow(Provider.K_PFURL)),
                cursor.getString(cursor.getColumnIndexOrThrow(Provider.K_PURL)),
                id,
                cursor.getString(cursor.getColumnIndexOrThrow(Provider.K_PSDESCR)),
                cursor.getString(cursor.getColumnIndexOrThrow(Provider.K_PERROR)),
                cursor.getLong(cursor.getColumnIndexOrThrow(Provider.K_PTSTAMP)),
                expandedElements.contains(id));
  }

  @Override
  public PodcastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext())
                           .inflate(R.layout.podcast_list_element, parent, false);
    return new PodcastViewHolder(v, listener, this);
  }
}
