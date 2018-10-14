package com.conversation.demo;

import android.content.Context;
import android.graphics.Color;

import com.nanorep.convesationui.structure.providers.CarouselElementUIProvider;
import com.nanorep.convesationui.structure.providers.ChatElementsUIProvider;
import com.nanorep.convesationui.structure.providers.ChatUIProvider;
import com.nanorep.convesationui.structure.providers.ConversationUIProvider;
import com.nanorep.convesationui.structure.providers.IncomingElementUIProvider;
import com.nanorep.convesationui.structure.providers.OutgoingElementUIProvider;
import com.nanorep.convesationui.structure.providers.PersistentOptionsUIProvider;
import com.nanorep.convesationui.structure.providers.QuickOptionUIProvider;
import com.nanorep.convesationui.views.chatelement.BubbleContentContainer;
import com.nanorep.convesationui.views.chatelement.ExtendedBubbleContentContainer;
import com.nanorep.convesationui.views.chatelement.ExtendedBubbleContentHolder;
import com.nanorep.nanoengine.model.Channels;
import com.nanorep.nanoengine.model.configuration.StyleConfig;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import app.com.nanoconversationdemo.R;
import app.com.nanoconversationdemo.viewholder.JioBubbleContentHolder;

import static com.nanorep.sdkcore.utils.UtilityMethodsKt.getPx;

class AppChatUIProvider extends ConversationUIProvider.Defaults /*ConversationViewsProvider*/ {

    private CarouselElementUIProvider myCarouselViewsProvider = new AppCarouselViewsProvider();

    @NotNull
    @Override
    public ChatUIProvider getChatUIProvider() {
        return new ChatUIProvider.Defaults() {

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

                           /* @Override
                            public int getOutgoingElementLayout() {
                                return R.layout.bubbleoutgoing;
                            }*/

                            /*@NotNull
                            @Override
                            public ChatElementViewHolder getOutgoingViewHolder(@NotNull Context context, @NotNull UIElementController controller) {
                                return new JioLocalViewHolder(context, controller);
                            }
*/
                        };
                    }

                    @NotNull
                    @Override
                    public IncomingElementUIProvider getIncomingUIProvider() {
                        return new IncomingElementUIProvider.Defaults() {

                            /*@Override
                            public int getIncomingElementLayout() {
                                return R.layout.bubbleincoming;
                            }
*/

                            @NotNull
                            @Override
                            public ExtendedBubbleContentHolder getIncomingBubbleHolder(@NotNull BubbleContentContainer bubbleContentContainer) {
                                return new JioBubbleContentHolder(bubbleContentContainer);
                            }

                            @NotNull
                            @Override
                            public ExtendedBubbleContentContainer getIncomingExtendedBubbleContentContainer(@NotNull Context context) {
                                ExtendedBubbleContentContainer bubbleContentContainer = super.getIncomingExtendedBubbleContentContainer(context);
                                // [exp: bubbleContentContainer.setTextBackground(new ColorDrawable(Color.parseColor("#aaaaaa")));]
                                // bubbleContentContainer.setMargins(0, 0, getPx(30), 0);
                                // in case we need to change default: bubbleContentContainer.setContentAlignment(UiConfigurations.Alignment.AlignTopLTR);

                                // avatar can be configured : 1.by UI provider 2.here 3.in BubbleContentHolder extensions (see JioBubbleContentHolder)
                                // the last dynamic change if any should be prioritized ( UI provider is base configuration - static)
                                bubbleContentContainer.setIcon(context.getResources().getDrawable(R.drawable.mr_chatbot));

                                // change the text style of the carousel info section (in bubble)
                                // bubbleContentContainer.setTextStyle(new StyleConfig(getPx(20), Color.BLUE, getTypeface(getContext(), "great_vibes.otf")));
                                // change the carousel timestamp display (if not set, will use the stype provided by ConversationSettings or default if not available)
                                // bubbleContentContainer.setTimestampStyle(new TimestampStyle("hh:mm", getPx(11), Color.parseColor("#a8a8a8"), null));

                                return bubbleContentContainer;
                            }

                            @NotNull
                            @Override
                            public CarouselElementUIProvider getCarouselUIProvider() {
                                return myCarouselViewsProvider;
                            }

                            @NotNull
                            @Override
                            public PersistentOptionsUIProvider getPersistentOptionsUIProvider() {
                                return new PersistentOptionsUIProvider.Defaults() {
                                    @NotNull
                                    @Override
                                    public StyleConfig getOptionsStyleConfig() {
                                        return new StyleConfig(getPx(6), Color.parseColor("#8888bb"), null);
                                    }

                                    @Override
                                    public int getPersistentOptionLayout() {
                                        return R.layout.persistent_option;
                                    }

                                    @Override
                                    public int getPersistentOptionTextViewId() {
                                        return R.id.my_persistent_text;
                                    }
                                };

                            }

                            @NotNull
                            @Override
                            public QuickOptionUIProvider getQuickOptionsUIProvider() {
                                return new QuickOptionUIProvider.Defaults() {
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

            ;
        };
    }
}
