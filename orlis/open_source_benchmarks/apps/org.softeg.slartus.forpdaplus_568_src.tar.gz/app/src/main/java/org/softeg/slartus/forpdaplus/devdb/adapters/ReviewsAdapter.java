package org.softeg.slartus.forpdaplus.devdb.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.softeg.slartus.forpdaplus.R;
import org.softeg.slartus.forpdaplus.devdb.adapters.base.BaseRecyclerViewHolder;
import org.softeg.slartus.forpdaplus.devdb.helpers.DevDbUtils;
import org.softeg.slartus.forpdaplus.devdb.model.ReviewsModel;

import java.util.List;

import butterknife.Bind;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ViewHolder> {

    private static final int LAYOUT = R.layout.dev_db_reviews_item;
    private LayoutInflater mLayoutInflater;
    private Context mContext;
    private List<ReviewsModel> mModels;
    private ImageLoader imageLoader;

    public ReviewsAdapter(Context context, List<ReviewsModel> models, ImageLoader imageLoader) {
        super();
        this.mContext = context;
        this.mModels = models;
        this.imageLoader = imageLoader;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(LAYOUT, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final ReviewsModel obj = mModels.get(position);
        holder.textBody.setText(obj.getReviewTitle());
        holder.date.setText(obj.getReviewDate());

        holder.textBody.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DevDbUtils.showUrl(mContext, obj.getReviewLink());
            }
        });

        //Picasso.with(mContext).load(obj.getReviewImgLink()).into(holder.image);
        imageLoader.displayImage(obj.getReviewImgLink(), holder.image, new ImageLoadingListener() {

            @Override
            public void onLoadingStarted(String p1, View p2) {
                p2.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onLoadingFailed(String p1, View p2, FailReason p3) {
            }

            @Override
            public void onLoadingComplete(String p1, View p2, Bitmap p3) {
                p2.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadingCancelled(String p1, View p2) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return mModels.size();
    }

    public static class ViewHolder extends BaseRecyclerViewHolder {

        @Bind(R.id.devDbReviewsText)
        TextView textBody;
        @Bind(R.id.devDbReviewsDate)
        TextView date;
        @Bind(R.id.devDbReviewsIV)
        ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
