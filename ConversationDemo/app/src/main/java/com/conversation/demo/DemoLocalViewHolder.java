package com.conversation.demo;

import android.support.annotation.NonNull;
import android.view.View;

import com.nanorep.convesationui.viewholder.BubbleLocalViewHolder;
import com.nanorep.convesationui.viewholder.controllers.ChatElementController;
import com.nanorep.convesationui.views.ChatTextView;
import com.nanorep.nanoengine.chatelement.ChatElement;
import com.nanorep.nanoengine.chatelement.LocalChatElement;
import com.nanorep.nanoengine.model.configuration.StyleConfig;

public class DemoLocalViewHolder extends BubbleLocalViewHolder {

    private DynamicBubbleBind dynamicBubbleBind;

    public DemoLocalViewHolder(View itemView, ChatElementController controller, DynamicBubbleBind dynamicBubbleBind) {
        super(itemView, controller, (ChatTextView) itemView.findViewById(R.id.demo_local_bubble_message_textview));

          /*Typeface fontface = getTypeface(itemView.getContext(), "great_vibes.otf");
        setTextStyles(new StyleConfig(getPx(26)
                        , itemView.getResources().getColor(android.R.color.white), fontface),
                new TimestampStyle("hh:mm:ss", getPx(24), Color.RED, fontface));*/


        this.dynamicBubbleBind = dynamicBubbleBind;

    }

    @Override
    public void bind(@NonNull ChatElement element, int position, int totalCount) {
        super.bind(element, position, totalCount);

        DynamicBubbleBind.BubbleData bubbleData = dynamicBubbleBind.onBind(itemView.getContext(),
                (LocalChatElement)element, position, totalCount);
        int color = bubbleData.getTextColor();
        if(color != -1){
            getBubbleText().setStyle(new StyleConfig(null, color, null), null);
        }
    }

}
