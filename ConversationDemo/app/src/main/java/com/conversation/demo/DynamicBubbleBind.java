package com.conversation.demo;

import android.graphics.Color;

import com.nanorep.nanoengine.chatelement.ChatElement;
import com.nanorep.nanoengine.chatelement.ContentChatElement;
import com.nanorep.nanoengine.model.conversation.statement.StatementScope;

/**
 * This class enables dynamic control on the ChatElement according to content and position during it's ViewHolder's binding
 */

class DynamicBubbleBind {

    private StatementScope lastScope;
    @ChatElement.Companion.ChatElementType
    private int lastType;

    BubbleData onBind(ContentChatElement element, int position, int total) {
        boolean displayAvatar = true;
        if (position != total-1 && lastScope == element.getScope() && lastType == element.getType()) {
            displayAvatar = false;
        }

        lastType = element.getType();
        lastScope = element.getScope();

        return new BubbleData().displayAvatar(displayAvatar).textColor(element.getScope().isLive()? Color.RED:-1);
    }


    static class BubbleData {
        boolean displayAvatar = false;
        int textColor = -1;

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
