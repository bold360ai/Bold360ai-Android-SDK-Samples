package com.conversation.demo;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;

import com.nanorep.convesationui.viewholder.BubbleRemoteViewHolder;
import com.nanorep.convesationui.viewholder.RemoteResourcesProvider;
import com.nanorep.convesationui.viewholder.controllers.ChatElementController;
import com.nanorep.nanoengine.chatelement.ChatElement;
import com.nanorep.nanoengine.chatelement.ContentChatElement;
import com.nanorep.nanoengine.chatelement.OptionsChatElement;
import com.nanorep.nanoengine.model.AgentType;
import com.nanorep.nanoengine.model.configuration.StyleConfig;

public class DemoRemoteViewHolder extends BubbleRemoteViewHolder {

    private final ImageView avatarImageView;
    private DynamicBubbleBind dynamicBubbleBind;

    public DemoRemoteViewHolder(View view, ChatElementController controller, DynamicBubbleBind dynamicBubbleBind) {
        super(view, new RemoteResources(), controller);

        // should configured the style to use for the bubbled text and the timestamp
        // if font was configured on console for the remote text it will be overridden
         /*Typeface fontface = getTypeface(view.getContext(), "great_vibes.otf");
        setTextStyles(new StyleConfig(getPx(26), Color.GREEN, fontface),
                new TimestampStyle("hh:mm:ss", getPx(24), Color.RED, fontface));*/

        this.avatarImageView = itemView.findViewById(R.id.demo_agent_avatar);

        this.dynamicBubbleBind = dynamicBubbleBind;
    }


    @Override
    public void bind(@NonNull ChatElement element, int position, int totalCount) {
        super.bind(element, position, totalCount);

        if (!(element instanceof OptionsChatElement) || avatarImageView == null) {
            return;
        }

        ContentChatElement remoteChatElement = (ContentChatElement) element;

        DynamicBubbleBind.BubbleData bubbleData = dynamicBubbleBind.onBind(remoteChatElement, position, totalCount);

        if(bubbleData.displayAvatar()) {
            avatarImageView.setImageResource(remoteChatElement.getAgentType().equals(AgentType.Live) ?
                R.drawable.mr_chatbot :
                R.drawable.bold_360);
            avatarImageView.setVisibility(View.VISIBLE);
        } else {
            avatarImageView.setVisibility(View.INVISIBLE);
        }

        int color = bubbleData.getTextColor();
        if(color != -1){
            getBubbleText().setStyle(new StyleConfig(null, color, null), null);
        }
    }

    static class RemoteResources extends RemoteResourcesProvider {
        @Override
        public int getPersistentOptionTextViewId() {
            return R.id.my_persistent_text;
        }

        @Override
        public int getPersistentOptionLayout() {
            return R.layout.persistent_option;
        }
    }
}
