package com.conversation.demo.viewholder;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;

import com.conversation.demo.R;
import com.nanorep.convesationui.viewholder.BubbleRemoteViewHolder;
import com.nanorep.convesationui.viewholder.controllers.UIElementController;
import com.nanorep.nanoengine.chatelement.ChatElement;
import com.nanorep.nanoengine.chatelement.ContentChatElement;

import static com.nanorep.sdkcore.model.StatementModels.isLive;

public class DemoRemoteViewHolder extends BubbleRemoteViewHolder {
    private final ImageView avatarImageView;

    public DemoRemoteViewHolder(View view, UIElementController controller) {
        super(view, controller);

        this.avatarImageView = itemView.findViewById(R.id.demo_agent_avatar);
    }

    @Override
    public void bind(@NonNull ChatElement element, int position, int totalCount) {
        super.bind(element, position, totalCount);

        ContentChatElement remoteChatElement = (ContentChatElement) element;

        avatarImageView.setImageResource(isLive(remoteChatElement.getScope()) ?
                R.drawable.mr_chatbot :
                R.drawable.bold_360);
    }
}
