package nanorep.com.searchdemo.CustomViews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.nanorep.nanoclient.Channeling.NRChanneling;

import java.util.List;

import nanorep.com.searchdemo.R;
import nanorep.nanowidget.Components.AbstractViews.NRCustomChannelView;
import nanorep.nanowidget.SearchViewsProvider;

/**
 * Created by obenoved on 20/03/2018.
 */

@SuppressLint("ViewConstructor")
public class CustomChannelingView extends NRCustomChannelView implements CustomChannelingItemView.ChannelListener {

    private LinearLayout channelsContainerLayout;
    private boolean hasDivider;


    public CustomChannelingView(Context context, SearchViewsProvider viewsProvider) {
        super(context, viewsProvider);
        LayoutInflater.from(context).inflate(viewsProvider.getChannelingLayout(), this);
    }

    @Override
    public void setChanneling(List<NRChanneling> channelings) {
        mChannelings = channelings;
        if (channelings.size() > 1) {
            hasDivider = true;
        } else {
            channelsContainerLayout.setShowDividers(SHOW_DIVIDER_NONE);
        }
        channelsContainerLayout.removeAllViews();
        setWeightSum(2);

        for (int i = 0; i < channelings.size(); i++) {
            CustomChannelingItemView item = new CustomChannelingItemView(getContext(), channelings.get(i), CustomChannelingView.this);
            channelsContainerLayout.addView(item);
        }

        if (!hasDivider) {
            View dummyView = new View(getContext());
            LayoutParams dummyParams = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
            dummyParams.weight = 1;
            dummyView.setLayoutParams(dummyParams);
            channelsContainerLayout.addView(dummyView);
        }
    }

    public void onViewAdded(View child) {
        super.onViewAdded(child);
        channelsContainerLayout = findViewById(R.id.channelsLayout);
    }

    @Override
    public void onChannelSelected(NRChanneling channeling) {
        mListener.onChannelSelected(channeling);
    }

    @Override
    public void onChannelPressed(NRChanneling channeling) {
        onChannelSelected(channeling);
    }
}
