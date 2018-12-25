package com.conversation.demo.viewholder;

import android.view.View;

import com.conversation.demo.R;
import com.nanorep.convesationui.viewholder.BubbleLocalViewHolder;
import com.nanorep.convesationui.viewholder.controllers.UIElementController;
import com.nanorep.convesationui.views.ChatTextView;


public class DemoLocalViewHolder extends BubbleLocalViewHolder {

    public DemoLocalViewHolder(View itemView, UIElementController controller) {
        super(itemView, controller, (ChatTextView) itemView.findViewById(R.id.demo_local_bubble_message_textview));
    }
}
