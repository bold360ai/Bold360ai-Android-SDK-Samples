package com.conversation.demo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.view.Gravity;

import com.conversation.demo.viewholder.DemoCarouselInfoViewHolder;
import com.nanorep.convesationui.structure.UiConfigurations;
import com.nanorep.convesationui.structure.providers.CarouselElementUIProvider;
import com.nanorep.convesationui.structure.providers.CarouselViewsProvider;
import com.nanorep.convesationui.views.OptionActionListener;
import com.nanorep.convesationui.views.carousel.CarouselInfoContainer;
import com.nanorep.convesationui.views.carousel.CarouselItemConfiguration;
import com.nanorep.convesationui.views.carousel.CarouselItemsContainer;
import com.nanorep.convesationui.views.carousel.CarouselView;
import com.nanorep.convesationui.views.carousel.CarouselViewHolder;
import com.nanorep.nanoengine.model.CarouselElement;
import com.nanorep.sdkcore.utils.ViewHolder;

import org.jetbrains.annotations.Nullable;

import static com.nanorep.sdkcore.utils.UtilityMethodsKt.getPx;

class AppCarouselViewsProvider extends CarouselElementUIProvider.Defaults {

    @Nullable
    @Override
    public ViewHolder getCarouselInfoHolder(@NonNull CarouselElement carouselData, @Nullable CarouselInfoContainer carouselInfoContainer) {
        DemoCarouselInfoViewHolder demoCarouselInfoViewHolder = new DemoCarouselInfoViewHolder(carouselInfoContainer);
        demoCarouselInfoViewHolder.update(carouselData);
        return demoCarouselInfoViewHolder;
    }

    @NonNull
    @Override
    public CarouselInfoContainer getCarouselInfoView(@NonNull Context context) {
        CarouselInfoContainer carouselInfoContainer = super.getCarouselInfoView(context);
        carouselInfoContainer.setTextBackground(context.getResources().getDrawable(R.drawable.bg_inbox_normal));
        carouselInfoContainer.setMargins( 0, 0, getPx(30),0);
        carouselInfoContainer.setIconAlignment(UiConfigurations.Alignment.AlignTop);
        carouselInfoContainer.setIconTextGap(getPx(4)); // change the margin between the bot icon and the bubble text
        // change the text style of the carousel info section (in bubble)
       // carouselInfoContainer.setTextStyle(new StyleConfig(getPx(20), Color.BLUE, getTypeface(getContext(), "great_vibes.otf")));
        // change the carousel timestamp display (if not set, will use the stype provided by ConversationSettings or default if not available)
       // carouselInfoContainer.setTimestampStyle(new TimestampStyle("hh:mm", getPx(11), Color.parseColor("#a8a8a8"), null));

        return carouselInfoContainer;
    }

    @NonNull
    @Override
    public CarouselViewHolder injectCarousel(@NonNull Context context, @Nullable CarouselElement carouselData, @Nullable OptionActionListener optionListener) {
        CarouselView carouselView = (CarouselView) super.injectCustomCarousel(context, carouselData, null);
        ((CarouselView.LayoutParams) carouselView.getLayoutParams()).setMargins(0, getPx(12), 0, 0);
        return carouselView;
    }

    /**
     * example to override carousel items list properties.
     *
     * @param context
     * @param optionListener
     * @return
     */
    @NonNull
    @Override
    public CarouselItemsContainer getCarouselItemsContainer(@NonNull Context context, @Nullable OptionActionListener optionListener) {
        CarouselItemsContainer itemsContainer = super.getCarouselItemsContainer(context, optionListener);

        itemsContainer.setOptionsHorizontalAlignment(Gravity.END);
        itemsContainer.setOptionsVerticalAlignment(Gravity.BOTTOM);

        itemsContainer.setOptionsBackground(/*new ColorDrawable(Color.TRANSPARENT));*/ context.getResources().getDrawable(R.drawable.c_item_option_back));
        itemsContainer.setOptionsTextStyle(Color.parseColor("#5EC4B6"), null);
        itemsContainer.setOptionsTextSize(16);

        itemsContainer.setItemBackground(context.getResources().getDrawable( R.drawable.bkg_bots));//new ColorDrawable(Color.parseColor("#f1f8ff"))/*getResources().getDrawable(R.drawable.c_item_back)*/);
        itemsContainer.setCardStyle(getPx(10), getPx(5) );// set the carousel items to have a "card" look. [roundCornersRadius, elevation]
       // itemsContainer.setOptionsTextStyle(Color.parseColor("#00AAFF"), "great_vibes.otf");
        itemsContainer.setInfoTextAlignment(CarouselItemConfiguration.ItemInfoAlignment.AlignBelowThumb);
        itemsContainer.setInfoTextStyle(Color.DKGRAY, null);
        itemsContainer.setInfoTextSize(16);
        itemsContainer.setInfoTextBackground(new ColorDrawable(Color.parseColor("#00000000")));

        // if the text not occupies the minimum lines, empty lines will be displayed
        itemsContainer.setInfoTitleMinLines(1); // configure the MINIMUM lines the title will be displayed on
        itemsContainer.setInfoSubTitleMinLines(2);// configure the MINIMUM lines the title will be displayed on
        return itemsContainer;
    }
}
