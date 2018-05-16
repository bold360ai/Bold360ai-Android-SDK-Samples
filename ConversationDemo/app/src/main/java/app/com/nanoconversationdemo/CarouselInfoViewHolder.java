package app.com.nanoconversationdemo;

import com.nanorep.convesationui.views.carousel.CarouselInfoContainer;
import com.nanorep.convesationui.views.carousel.CarouselInfoHolder;
import com.nanorep.nanoengine.model.AgentType;
import com.nanorep.nanoengine.model.CarouselData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CarouselInfoViewHolder extends CarouselInfoHolder {

    public CarouselInfoViewHolder(@Nullable CarouselInfoContainer carouselInfoContainer) {
        super(carouselInfoContainer);
    }

    @NotNull
    @Override
    public CarouselInfoHolder update(@Nullable Object data) {

        super.update(data);

        if (data instanceof CarouselData) {
            CarouselData carouselData = (CarouselData) data;

            int iconId = carouselData.getAgentType() == AgentType.Live ? R.drawable.jioagentlive : R.drawable.jioagentbot;
            if (getView() != null && getView().getContext() != null) {
                this.setIcon(getView().getContext().getResources().getDrawable(iconId));
            }
        }
        return this;
    }

}
