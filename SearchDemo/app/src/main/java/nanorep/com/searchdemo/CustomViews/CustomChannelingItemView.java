package nanorep.com.searchdemo.CustomViews;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nanorep.nanoclient.Channeling.NRChanneling;

import nanorep.com.searchdemo.R;

import static com.nanorep.nanoclient.Channeling.NRChanneling.NRChannelingType.PhoneNumber;

/**
 * Created by obenoved on 20/03/2018.
 */

public class CustomChannelingItemView extends LinearLayout {
    ImageView imageView;
    TextView textView;
    NRChanneling channeling;
    ChannelListener listener;

    interface ChannelListener {
        void onChannelPressed(NRChanneling channeling);
    }

    public CustomChannelingItemView(Context context, NRChanneling channeling, ChannelListener listener) {
        super(context);
        this.listener = listener;
        this.channeling = channeling;
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.custom_channel_item, this);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        imageView = findViewById(R.id.channelIcon);
        textView = findViewById(R.id.channelName);

        if (channeling != null) {
            if (channeling.getButtonText() != null) {
                textView.setText(channeling.getButtonText());
            }

            if (channeling.getType() == PhoneNumber) {
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.phone));
            } else {
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.mail));
            }
        }

        LayoutParams params = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.weight = 1;
        this.setLayoutParams(params);

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onChannelPressed(channeling);
            }
        });
    }

}
