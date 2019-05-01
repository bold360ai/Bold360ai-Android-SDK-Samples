package com.conversation.demo;

import android.content.Context;
import android.graphics.Color;

import com.nanorep.nanoengine.chatelement.ChatElement;
import com.nanorep.nanoengine.chatelement.ContentChatElement;
import com.nanorep.nanoengine.model.conversation.statement.StatementScope;


/**
 * This class enables dynamic control on the ChatElement according to content and position during it's ViewHolder's binding
 */

class DynamicBubbleBind {

    public static final int NoColor = -2;

    private StatementScope lastScope;
    @ChatElement.Companion.ChatElementType
    private int lastType;

    BubbleData onBind(Context context, ContentChatElement element, int position, int total) {
        boolean displayAvatar = true;
        if (position != total-1 && lastScope == element.getScope() && lastType == element.getType()) {
            displayAvatar = false;
        }

        lastType = element.getType();
        lastScope = element.getScope();

        boolean isLive = element.getScope().isLive();
        int textColor = isLive ? Color.RED :
                (context.getResources().getColor(lastType == ChatElement.OutgoingElement ?
                        R.color.outgoing_text : R.color.incoming_text));

        return new BubbleData().displayAvatar(displayAvatar).textColor(textColor);
    }


    static class BubbleData {
        boolean displayAvatar = false;
        int textColor = NoColor;

        BubbleData displayAvatar(boolean display) {
            displayAvatar = display;
            return this;
        }

        BubbleData textColor(int color) {
            textColor = color;
            return this;
        }

        boolean displayAvatar() {
            return this.displayAvatar;
        }

        int getTextColor(){
            return this.textColor;
        }
    }
}
