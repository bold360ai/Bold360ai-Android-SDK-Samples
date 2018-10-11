package com.conversation.demo;

import android.view.View;

import com.conversation.demo.viewholder.DemoLocalViewHolder;
import com.conversation.demo.viewholder.DemoRemoteViewHolder;
import com.nanorep.convesationui.structure.providers.CarouselViewsProvider;
import com.nanorep.convesationui.structure.providers.ChatElementsUIProvider;
import com.nanorep.convesationui.structure.providers.ChatUIProvider;
import com.nanorep.convesationui.structure.providers.ConversationUIProvider;
import com.nanorep.convesationui.structure.providers.IncomingElementUIProvider;
import com.nanorep.convesationui.structure.providers.OutgoingElementUIProvider;
import com.nanorep.convesationui.viewholder.base.ChatElementViewHolder;
import com.nanorep.convesationui.viewholder.controllers.UIElementController;
import com.nanorep.convesationui.views.QuickOptionUIProvider;
import com.nanorep.nanoengine.model.Channels;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class AppChatUIProvider extends ConversationUIProvider.Defaults /*ConversationViewsProvider*/ {

    private CarouselViewsProvider myCarouselViewsProvider = new AppCarouselViewsProvider();

    @NotNull
    @Override
    public ChatUIProvider getChatUIProvider() {
        return new ChatUIProvider.Defaults() {

            @Override
            public int getUserInputLayout() {
                return R.layout.user_input_view_holder_custom;
            }

            @Override
            public int getFragmentBackground() {
                return R.drawable.bold_form_bg;
            }

            @NotNull
            @Override
            public ChatElementsUIProvider getChatElementsUIProvider() {
                return new ChatElementsUIProvider.Defaults() {

                    @NotNull
                    @Override
                    public OutgoingElementUIProvider getOutgoingUIProvider() {
                        return new OutgoingElementUIProvider.Defaults() {

                            @Override
                            public int getOutgoingElementLayout() {
                                return R.layout.bubbleoutgoing;
                            }

                            @NotNull
                            @Override
                            public ChatElementViewHolder getOutgoingViewHolder(@NotNull View view, @NotNull UIElementController controller) {
                                return new DemoLocalViewHolder(view, controller);
                            }

                        };
                    }

                    @NotNull
                    @Override
                    public IncomingElementUIProvider getIncomingUIProvider() {
                        return new IncomingElementUIProvider.Defaults() {

                            @Override
                            public int getIncomingElementLayout() {
                                return R.layout.bubbleincoming;
                            }

                            @NotNull
                            @Override
                            public ChatElementViewHolder getIncomingViewHolder(@NotNull View view, @NotNull UIElementController controller) {
                                return new DemoRemoteViewHolder(view, controller);
                            }

                            @NotNull
                            @Override
                            public CarouselViewsProvider getCarouselUIProvider() {
                                return myCarouselViewsProvider;
                            }

                            @Override
                            public int getPersistentOptionTextViewId() {
                                return R.id.my_persistent_text;
                            }

                            @Override
                            public int getPersistentOptionLayout() {
                                return R.layout.persistent_option;
                            }
                        };
                    }

                    @NotNull
                    @Override
                    public QuickOptionUIProvider getQuickOptionUIProvider() {

                        return new QuickOptionUIProvider.Defaults() {

                            @Override
                            public int getQuickOptionLayoutId() {
                                return R.layout.quick_option_layout_custom;
                            }

                            @Nullable
                            @Override
                            public String getChannelResourceName(int optionChannelType) {

                                String resourceName = null;
                                switch (optionChannelType) {
                                    case Channels.PhoneNumber:
                                        resourceName = "R.drawable.call_channel";
                                        break;
                                    case Channels.Chat:
                                        resourceName = "R.drawable.chat_channel";
                                        break;
                                    case Channels.Ticket:
                                        resourceName = "R.drawable.mail_channel";
                                        break;
                                }
                                return resourceName;
                            }
                        };
                    }

                };
            }
        };
    }
}
