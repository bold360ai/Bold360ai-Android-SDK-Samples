package com.conversation.demo;

import android.view.View;

import com.nanorep.convesationui.viewholder.BubbleLocalViewHolder;
import com.nanorep.convesationui.viewholder.controllers.ChatElementController;
import com.nanorep.convesationui.views.ChatTextView;

/**
 * Created by aviran on 10/24/17.
 */

public class DemoLocalViewHolder extends BubbleLocalViewHolder {

    public DemoLocalViewHolder(View itemView, ChatElementController controller) {
        super(itemView, controller, (ChatTextView) itemView.findViewById(R.id.jio_local_bubble_message_textview));

    }

}
