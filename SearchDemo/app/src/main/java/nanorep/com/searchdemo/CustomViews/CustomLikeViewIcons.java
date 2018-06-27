package nanorep.com.searchdemo.CustomViews;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

import com.nanorep.nanoclient.Nanorep;
import com.nanorep.nanoclient.Response.NRConfiguration;

import nanorep.com.searchdemo.R;
import nanorep.nanowidget.Components.AbstractViews.NRCustomLikeView;
import nanorep.nanowidget.SearchViewsProvider;

/**
 * Created by obenoved on 18/03/2018.
 */

public class CustomLikeViewIcons extends NRCustomLikeView {

    private ImageButton mLikeButton;
    private ImageButton mDislikeButton;
    private boolean mLikeSelection;

    public CustomLikeViewIcons(Context context, SearchViewsProvider viewsProvider) {
        super(context, viewsProvider);
        LayoutInflater.from(context).inflate(R.layout.custom_like_view_icons, this);
    }

    @Override
    public void updateLikeButton(boolean isLike) {
        resetLikeView();
        if (isLike) {
            mLikeButton.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.upvote_filled ));
            mLikeButton.setSelected(true);
        } else {
            mDislikeButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.downvote_filled));
            mDislikeButton.setSelected(true);
        }
        mDislikeButton.setClickable(false);
        mLikeButton.setClickable(false);
        mDislikeButton.setEnabled(false);
        mLikeButton.setEnabled(false);
        mLikeSelection = isLike;
    }

    @Override
    public void resetLikeView() {
        mLikeButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.upvote));
        mDislikeButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.downvote));

        mDislikeButton.setClickable(true);
        mLikeButton.setClickable(true);
        mDislikeButton.setEnabled(true);
        mLikeButton.setEnabled(true);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        mLikeButton = findViewById(R.id.fragment_article_positiveFeedback_button);
        mDislikeButton = findViewById(R.id.fragment_article_negativeFeedback_button);

        mLikeButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.upvote));
        mDislikeButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.downvote));

        mLikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSelection(true);
            }
        });
        mDislikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSelection(false);
            }
        });
    }

    private void sendSelection(boolean selection) {
        mLikeSelection = selection;
        updateLikeButton(mLikeSelection);
        mListener.onLikeClicked(CustomLikeViewIcons.this, null, mLikeSelection);
    }

    @Override
    public boolean getLikeSelection() {
        return mLikeSelection;
    }

    @Override
    public boolean shouldOpenDialog() {
        return Nanorep.getInstance().getNRConfiguration().getFeedbackDialogType() != NRConfiguration.NO_FEEDBACK_DIALOG_TYPE;
    }
}
