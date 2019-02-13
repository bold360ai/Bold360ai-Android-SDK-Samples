package com.conversation.demo;

import com.nanorep.convesationui.views.carousel.CarouselInfoContainer;
import com.nanorep.convesationui.views.carousel.CarouselInfoHolder;
import com.nanorep.nanoengine.model.AgentType;
import com.nanorep.nanoengine.model.CarouselElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class DemoCarouselInfoViewHolder extends CarouselInfoHolder {

    public DemoCarouselInfoViewHolder(@Nullable CarouselInfoContainer carouselInfoContainer) {
        super(carouselInfoContainer);
    }

    @NotNull
    @Override
    public CarouselInfoHolder update(@Nullable Object data) {

        super.update(data);

        if (data instanceof CarouselElement) {
            CarouselElement carouselData = (CarouselElement) data;

            int iconId = carouselData.getAgentType() == AgentType.Live ? R.drawable.mr_livechat : R.drawable.mr_chatbot;
            if (getView() != null && getView().getContext() != null) {
                this.setIcon(getView().getContext().getResources().getDrawable(iconId));
            }
        }
        return this;
    }

}
