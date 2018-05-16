package app.com.nanoconversationdemo;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nanorep.convesationui.ConversationViewsProvider;
import com.nanorep.convesationui.adapter.ConversationInjector;
import com.nanorep.convesationui.fragments.NRConversationFragment;
import com.nanorep.convesationui.structure.FriendlyDatestampFormatFactory;
import com.nanorep.convesationui.structure.OptionActionListener;
import com.nanorep.convesationui.structure.UiConfigurations;
import com.nanorep.convesationui.structure.history.HistoryHandler;
import com.nanorep.convesationui.structure.history.HistoryItem;
import com.nanorep.convesationui.structure.history.HistoryListener;
import com.nanorep.convesationui.structure.history.HistoryProvider;
import com.nanorep.convesationui.viewholder.base.ChatElementViewHolder;
import com.nanorep.convesationui.viewholder.controllers.ChatElementController;
import com.nanorep.convesationui.views.carousel.CarouselInfoContainer;
import com.nanorep.convesationui.views.carousel.CarouselItemConfiguration;
import com.nanorep.convesationui.views.carousel.CarouselItemsContainer;
import com.nanorep.convesationui.views.carousel.CarouselView;
import com.nanorep.convesationui.views.carousel.CarouselViewHolder;
import com.nanorep.convesationui.views.carousel.CarouselViewsProvider;
import com.nanorep.nanoengine.ConversationListener;
import com.nanorep.nanoengine.Entity;
import com.nanorep.nanoengine.NRConversationEngine;
import com.nanorep.nanoengine.NRConversationMissingEntities;
import com.nanorep.nanoengine.NRPrivateInfo;
import com.nanorep.nanoengine.NanoAccess;
import com.nanorep.nanoengine.NanoRep;
import com.nanorep.nanoengine.Property;
import com.nanorep.nanoengine.model.AgentType;
import com.nanorep.nanoengine.model.CarouselData;
import com.nanorep.nanoengine.model.NRAccount;
import com.nanorep.nanoengine.model.configuration.ConversationSettings;
import com.nanorep.nanoengine.model.configuration.TimestampStyle;
import com.nanorep.nanoengine.model.conversation.Conversation;
import com.nanorep.nanoengine.model.conversation.statement.OnStatementResponse;
import com.nanorep.nanoengine.model.conversation.statement.StatementRequest;
import com.nanorep.nanoengine.model.conversation.statement.StatementResponse;
import com.nanorep.sdkcore.types.NRError;
import com.nanorep.sdkcore.types.ViewHolder;
import com.nanorep.sdkcore.utils.network.ConnectivityReceiver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.nanorep.nanoengine.model.NRChannel.ChannelType.Chat;
import static com.nanorep.nanoengine.model.NRChannel.ChannelType.PhoneNumber;
import static com.nanorep.nanoengine.model.NRChannel.ChannelType.Ticket;
import static com.nanorep.nanoengine.model.ResponseModelsKt.isConversationError;
import static com.nanorep.sdkcore.utils.UtilsKt.getPx;
import static com.nanorep.sdkcore.utils.UtilsKt.snack;
import static com.nanorep.sdkcore.utils.UtilsKt.toast;

public class MainActivity extends AppCompatActivity implements ConversationListener, HistoryProvider, ConnectivityReceiver.ConnectivityListener {

    private static final String TAG = "MainActivity";

    /* Account parameters */
    private static final String ACCOUNT_NAME = "";
    private static final String API_KEY = "";
    private static final String KNOWLEDGE_BASE = "";
    private static final String SERVER = "";

    private static final String CONVERSATION_FRAGMENT_TAG = "conversation_fragment";
    public static final int HistoryPageSize = 8;

    private Button startButton;
    private ProgressBar progressBar;
    private TextView historySizeTextView;

    private NRAccount account;
    private String conversationId;
    private NanoAccess nanoAccess;

    private Map<String, List<HistoryItem>> chatHistory = new HashMap<>();
    private Map<String, OpenConversation> openConversations = new HashMap<>();

    private int handoverReplyCount = 0;
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
    private ConcurrentLinkedQueue<StatementRequest> failedStatements = new ConcurrentLinkedQueue<>();
    private SoftReference<NRConversationFragment> conversationFragment;
    private HistoryHandler historyHandler;

    /**
     * in use when previously failed statements are posted to indicate if the connection is done again
     * to stop the re-posting.
     */
    private boolean connectionOk = true;

    /**
     * indicates if we wait for conversation creation.
     * statements at this time are collected on the SDK, engine, side.
     * once a connection established, the app should activate the
     * createConversation API.
     * while requests are posted if the engine indicates that the conversation is not available, it
     * tries to create it. once creation succeeded the app will get a call to "onConversationIdUpdated"
     * and this field will be cleared.
     */
    private boolean pendingConversationCreation = false;

    /**
     * will be used when posting failed statements
     */
    private OnStatementResponse onStatementResponse = new OnStatementResponse() {
        @Override
        public void onResponse(@NotNull StatementResponse response) {

            /* -> we're not removing from history anymore, so no need to insert it
            // re-inserting failed request to history, since we removed it from the history when it got failed
            StatementRequest statementRequest = response.getStatementRequest();
            if(statementRequest != null) {
                historyHandler.addToHistory(new HistoryItem(statementRequest.getStatement(),
                        statementRequest.getTimestamp(), HistoryItemOrigin.OUTGOING, StatusOk));

            }*/

            if(conversationFragment.get() != null){
                conversationFragment.get().onResponse(response);
            }
        }

        @Override
        public void onError(@NotNull NRError error) {
            Log.e("MainFragment","failed to re-post request: "+error.getErrorCode()+", "+error.getReason() );
            MainActivity.this.onError(error);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);
        historySizeTextView = findViewById(R.id.historySizeTextView);
        progressBar = findViewById(R.id.progressBar);
        updateHistorySizeIndication();

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startButton.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                initNanorep();
            }
        });

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    progressBar.setVisibility(View.INVISIBLE);
                    startButton.setEnabled(true);
                    updateHistorySizeIndication();
                    if(conversationFragment != null) {
                        conversationFragment.clear();
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        connectivityReceiver.register(this, this);
    }

    private void updateHistorySizeIndication() {
        historySizeTextView.setText(getString(R.string.history_size_format, chatHistory.size()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        connectivityReceiver.unregister(this);

        conversationFragment.clear();

        try {
            nanoAccess.setConversationListener(null);
        } catch (Exception NRInitilizationException) {
            Log.e(TAG, "NanoRep was not initialized");
        }
    }

    public void initNanorep() {
        Map<String, String> conversationContext = new HashMap<>();
        account = new NRAccount(ACCOUNT_NAME, API_KEY,
                KNOWLEDGE_BASE, SERVER, conversationContext);
        account.setEntities(new String[]{"SUBSCRIBERS"});

        ConversationSettings settings = new ConversationSettings().disableFeedback()
                .speechEnable(true).enableMultiRequestsOnLiveAgent(false)
                .enableOfflineMultiRequests(true)
                .timestampConfig(true, new TimestampStyle("EEE, HH:mm",
                    getPx(10), Color.parseColor("#a8a8a8"), null) ).
                datestamp(true, new FriendlyDatestampFormatFactory(this));

        //-> to configure the default text configuration to all bubbles text add something like the following:
        //settings.textStyleConfig(new StyleConfig(getPx(23), Color.BLUE, null));

        this.nanoAccess = NanoRep.initializeConversation(account, this,
                                    fetchAccountConversationData(account.getAccount()), settings);
    }

    private Conversation fetchAccountConversationData(String account) {

        Conversation conversation = null;

        if (openConversations.containsKey(account)) {
            OpenConversation openConversation = openConversations.get(account);
            conversation = new Conversation(openConversation.conversationId, openConversation.lastAgent);
        }

        if (conversation == null) {
            conversation = new Conversation();
        }

        return conversation;
    }

    /**
     * called when conversationId is updated.
     * this may happen more than once in case the conversation became invalid, so in order
     * to prevent multiple "NRConversationFragment" we tagged the fragment and we look for it
     * when we get this event.
     *
     * @param conversationId
     */
    @Override
    public void onConversationIdUpdated(String conversationId) {
        Log.i("conversationId", conversationId);
        this.conversationId = conversationId;

        if(pendingConversationCreation){
            pendingConversationCreation = false;
            // in case conversation was created, the fragment needs a refresh to update the styles
            // as defined in the configurations
            if(conversationFragment != null && conversationFragment.get() != null) {
                conversationFragment.get().refresh();
            }
            postFailedStatements();
        }

        String accountId = this.account.getAccount();
        if(!openConversations.containsKey(accountId)){
            openConversations.put(accountId, new OpenConversation(accountId, conversationId, AgentType.Bot));
        }

        openConversationFragment();
    }

    private void openConversationFragment() {

        FragmentManager fragmentManager = getSupportFragmentManager();

        // if activity is no longer up or fragment was already created :
        if(fragmentManager == null || fragmentManager.findFragmentByTag(CONVERSATION_FRAGMENT_TAG) != null){
            return;
        }

        this.conversationFragment = new SoftReference<>(NRConversationFragment.newInstance(new ConversationInjector() {
            @Override
            public ConversationListener getListener() {
                return MainActivity.this;
            }

            @Override
            public ConversationViewsProvider getUiProvider() {
                return new MyConversationUiProvider();
            }


            @Override
            public HistoryHandler getHistoryHandler() {
                historyHandler = new HistoryHandler(MainActivity.this, true);
                return historyHandler;
                // return new ConversationInjector.DefaultsInjector().getHistoryHandler() - if history is not needed
            }
        }));

        fragmentManager
                .beginTransaction()
                .replace(R.id.content_main, conversationFragment.get(), CONVERSATION_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onConversationTermination(Conversation conversation) {

        OpenConversation openConversation;
        String accountId = this.account.getAccount();
        if(openConversations.containsKey(accountId)){
            openConversation = openConversations.get(accountId);
            openConversation.conversationId = conversation.getId();
            openConversation.lastAgent = conversation.getAgentType();
        } else {
            openConversation = new OpenConversation(accountId, conversation.getId(), conversation.getAgentType());
            openConversations.put(accountId, openConversation);
        }

    }



    @Override
    public void onMissingEntities(StatementResponse response, NRConversationEngine.MissingEntitiesHandler handler) {
        NRConversationMissingEntities missingEntities = new NRConversationMissingEntities(response.getStatement());
        for (String missingEntity : response.getMissingEntities()) {
            if (missingEntity.equals("SUBSCRIBERS")) {
                missingEntities.addEntity(createEntity(missingEntity));
            }
        }

        handler.onMissingEntitiesReady(missingEntities);
    }


    @Override
    public void handlePersonalInformation(String id, NRPrivateInfo privateInfo) {
        switch (id) {
            case "balance":
                String balance = String.format("%10.2f$", Math.random() * 10000);
                privateInfo.getValueReady().privateDataCallback(balance, null);
                return;
        }

        privateInfo.getValueReady().privateDataCallback("1,000$", null);
    }


    private Entity createEntity(String entityName) {
        Entity entity = new Entity(Entity.PERSISTENT, Entity.NUMBER, "3", entityName, "1");
        for (int i = 0; i < 3; i++) {
            Property property = new Property(Entity.NUMBER, String.valueOf(i) + "234", "SUBSCRIBER");
            property.setName(property.getValue());
            property.addProperty(new Property(Entity.NUMBER, String.valueOf(i) + "234", "ID"));
            entity.addProperty(property);
        }
        return entity;
    }

    @Override
    public void onChatLoaded() {
        // now the chat is ready to receive requests injection
        // example: injecting a request on each return to the conversation.
        conversationFragment.get().injectRequest("Hello, this is a sample request injection after load");
    }

    @Override
    public void onError(NRError error) {
        progressBar.setVisibility(View.INVISIBLE);

         switch (error.getErrorCode()){

            case NRError.ConversationCreationError:

                notifyConversationError(error);

                pendingConversationCreation = true;

                if(NRError.ConnectionException.equals(error.getReason())) {
                    if(conversationFragment == null || conversationFragment.get() == null) {
                        openConversationFragment();
                        /*!! after opening the fragment, we need to wait for
                          !! onChatLoaded event before activating anything on it.*/
                    }
                }

                if(startButton!=null){
                    startButton.setEnabled(true);
                }
                break;

            case NRError.StatementError:

                if(conversationFragment == null || conversationFragment.get() == null) return;

                if(isConversationError(error)){
                    notifyConversationError(error);
                    pendingConversationCreation = true;

                } else {
                    notifyStatementError(error);
                }
                StatementRequest statementRequest = (StatementRequest) error.getData();

                /*
                Log.d("MainFragment", "removing failed request "+statementRequest.getStatement() +" from History");
                historyHandler.removeFromHistory(statementRequest.getTimestamp());
                 */

                Log.d("MainFragment", "adding "+statementRequest.getStatement() +" to app pending");
                failedStatements.add(statementRequest);
                break;

            default:
                /*all other errors will be handled here. Demo implementation, displays a toast and
                  writes to the log.
                 if needed the error.getErrorCode() and sometimes the error.getReason() can provide
                 the details regarding the error
                 */
                if(!isFinishing()){
                    Log.e("App-ERROR", error.toString());
                    toast(this, error.toString(), 5000 );
                }
        }
    }

    private void notifyConversationError(@NotNull NRError error) {
        notifyError(error, "failed to create conversation: ", Color.parseColor("#dddd88"));
    }

    private void notifyStatementError(@NotNull NRError error) {
        notifyError(error, "request failed - ", Color.RED);
    }

    @SuppressLint("ResourceType")
    private void notifyError(@NotNull NRError error, String s, int backgroundColor) {
        try {
            snack(conversationFragment.get().getView(),
                    s + error.getReason(),
                    1000, -1, Gravity.CENTER, new int[]{}, backgroundColor);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInitializeChatHandover() {
        handoverReplyCount = 0;
        nanoAccess.sendHandover("Hey, my name is Bob, how can I help?");
    }

    @Override
    public void onChatHandoverInput(String input) {
        if(nanoAccess == null) return;

        if (handoverReplyCount > 2 || input.equals("end handover")) {
            nanoAccess.sendHandover("Bye - Handover complete");
            nanoAccess.endHandover();
        } else {
            nanoAccess.sendHandover("Response (" + String.valueOf(++handoverReplyCount) + ")");
        }
    }

    @Override
    public void onInAppNavigation(String navTo) {
        Toast.makeText(this, navTo, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onUrlNavigation(String url) {
        Toast.makeText(this, url, Toast.LENGTH_SHORT).show();
        return connectionOk; // returns if the link could be activated
    }

    @Override
    public void fetchHistory(final int from, final boolean older, final HistoryListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<HistoryItem> history = getHistoryForAccount(account.getAccount(), from, older);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onReady(from, older, history);
                    }
                });
            }
        }).start();
    }

    private List<HistoryItem> getHistoryForAccount(String account, int fromIdx, boolean older) {
        ArrayList<HistoryItem> accountHistory = (ArrayList<HistoryItem>) chatHistory.get(account);
        if(accountHistory == null) return new ArrayList<>();

        if(fromIdx == -1) {
            fromIdx = accountHistory.size() - 1;
        } else {
            fromIdx = accountHistory.size() - fromIdx;
        }

        int toIndex = Math.max(0, fromIdx-HistoryPageSize);

        try {
            return accountHistory.subList(toIndex, fromIdx);

        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }


    @Override
    public void storeToHistory(HistoryItem item) {
        //if(item == null || item.getStatus() != StatusOk) return;

        ArrayList<HistoryItem> convHistory = getAccountHistory();
        convHistory.add(item);
    }

    @Override
    public void removeFromHistory(long timestampId) {
        ArrayList<HistoryItem> convHistory = getAccountHistory();

        Iterator<HistoryItem> iterator = convHistory.listIterator();
        while (iterator.hasNext()) {
            HistoryItem item = iterator.next();
            if (item.getTimestamp() == timestampId) {
                iterator.remove();
                break;
            }
        }
    }

    @Override
    public void updateHistory(long timestampId, long newTimestamp, int status) {
        ArrayList<HistoryItem> convHistory = getAccountHistory();

        Iterator<HistoryItem> iterator = convHistory.listIterator();
        while (iterator.hasNext()) {
            HistoryItem item = iterator.next();
            if(item.getTimestamp() == timestampId){
                item.setTimestamp(newTimestamp);
                item.setStatus(status);
                break;
            }
        }
    }

    @NonNull
    private ArrayList<HistoryItem> getAccountHistory() {
        ArrayList<HistoryItem> convHistory;
        String account = this.account.getAccount();
        if(chatHistory.containsKey(account)) {
            convHistory = (ArrayList<HistoryItem>) chatHistory.get(account);
        } else {
            convHistory = new ArrayList<HistoryItem>();
            chatHistory.put(account,  convHistory);
        }
        return convHistory;
    }

    @Override
    public void onPhoneNumberNavigation(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {

        }
    }

    @Override
    public void connectionChanged(boolean isConnected) {
        this.connectionOk = isConnected;
        if(isConnected){
            if(pendingConversationCreation){
                nanoAccess.createConversation(this.account);

            } else {
                // needed to refresh previously chat items (exp: carousel images)
                if(conversationFragment != null && conversationFragment.get() != null) {
                    conversationFragment.get().refresh();
                }

                // post pending statements until disconnection
                postFailedStatements();
            }
        }
    }

    private void postFailedStatements() {

        for(StatementRequest request : failedStatements){
            if(!connectionOk) break;

            Log.d("MainFragment", "adding previously failed request "+request.getStatement() +" to History");

            //!! a MUST, otherwise requests will be sent with previous or 0 id and will fail
            request.conversationId(conversationId);

            nanoAccess.postStatement(request, onStatementResponse);

            failedStatements.remove(request);
        }
    }

//////////////////////////////////////////

    class MyCarouselUiProvider extends CarouselViewsProvider {
        @Nullable
        @Override
        public ViewHolder getCarouselInfoHolder(@NotNull CarouselData carouselData, @Nullable CarouselInfoContainer carouselInfoContainer) {
            CarouselInfoViewHolder carouselInfoViewHolder = new CarouselInfoViewHolder(carouselInfoContainer);
            carouselInfoViewHolder.update(carouselData);
            return carouselInfoViewHolder;
        }

        @NotNull
        @Override
        public CarouselInfoContainer getCarouselInfoView(@NotNull Context context) {
            CarouselInfoContainer carouselInfoContainer = super.getCarouselInfoView(context);
            carouselInfoContainer.setTextBackground(context.getResources().getDrawable(R.drawable.bg_inbox_normal));
            carouselInfoContainer.setMargins( 0, 0, getPx(30),0);
            carouselInfoContainer.setIconAlignment(UiConfigurations.Alignment.AlignTop);

            // in case carousel timestamp needs to be changed:
            // carouselInfoContainer.setTimestampStyle(new TimestampStyle("hh:mm", getPx(12), Color.parseColor("#a8a8a8"), null));

           // carouselInfoContainer.setIconTextGap(getPx(8)); // change the margin between the bot icon and the bubble text

            return carouselInfoContainer;
        }

        @NotNull
        @Override
        public CarouselViewHolder injectCarousel(@NotNull Context context, @Nullable CarouselData carouselData, @Nullable OptionActionListener optionListener) {
            CarouselView carouselView = (CarouselView) super.injectCustomCarousel(context, carouselData, null);
            ((CarouselView.LayoutParams) carouselView.getLayoutParams()).setMargins(0, getPx(12), 0, 0);
            return carouselView;
        }

        @NotNull
        @Override
        public CarouselItemsContainer getCarouselItemsContainer(@NotNull Context context, @Nullable OptionActionListener optionListener) {

            CarouselItemsContainer itemsContainer = super.getCarouselItemsContainer(context, optionListener);
            itemsContainer.setOptionsHorizontalAlignment(Gravity.END);
            itemsContainer.setOptionsVerticalAlignment(Gravity.BOTTOM);
            itemsContainer.setOptionsBackground(new ColorDrawable(Color.TRANSPARENT));
            itemsContainer.setOptionsTextStyle(Color.parseColor("#5EC4B6"), null);
            itemsContainer.setOptionsTextSize(16);

            itemsContainer.setItemBackground(new ColorDrawable(Color.parseColor("#F5F4F4")));
            itemsContainer.setCardStyle(getPx(10), getPx(4) );// set the carousel items to have a "card" look. [roundCornersRadius, elevation]
            itemsContainer.setInfoTextAlignment(CarouselItemConfiguration.ItemInfoAlignment.AlignBelowThumb);
            itemsContainer.setInfoTextStyle(Color.GRAY, null);
            itemsContainer.setInfoTextSize(16);
            itemsContainer.setInfoTextBackground(new ColorDrawable(Color.parseColor("#00000000")));
            itemsContainer.setInfoSubTitleMinLines(1);
            return itemsContainer;
        }
    }

///////////////////////////////


    class MyConversationUiProvider extends ConversationViewsProvider {

        MyConversationUiProvider() {
            super(new MyCarouselUiProvider());
        }

        /**
         * provide resources for quick options
         */
        @Override
        protected String getChannelResourceName(int optionChannelType) {
            String resourceName = null;
            switch (optionChannelType) {
                case PhoneNumber:
                    resourceName = "R.drawable.call_channel";
                    break;
                case Chat:
                    resourceName = "R.drawable.chat_channel";
                    break;
                case Ticket:
                    resourceName = "R.drawable.mail_channel";
                    break;
            }
            return resourceName;
        }

        @Override
        public int getFragmentBackground() {
            return R.drawable.jioback;
        }

        @Override
        public int getLocalBubbleLayout() {
            return R.layout.bubbleoutgoing;
        }

        @Override
        public int getRemoteBubbleLayout() {
            return R.layout.bubbleincoming;
        }

        @Override
        public ChatElementViewHolder getRemoteBubbleViewHolder(View view, ChatElementController controller) {
            return new RemoteViewHolder(view, controller);
        }

        @Override
        public ChatElementViewHolder getLocalBubbleViewHolder(View view, ChatElementController controller) {
            return new LocalViewHolder(view, controller);
        }
    }

//////////////////////////////////////

    static class OpenConversation{
        String accountId;
        String conversationId;
        AgentType lastAgent;

        OpenConversation(String accountId, String conversationId, AgentType lastAgent) {
            this.accountId = accountId;
            this.conversationId = conversationId;
            this.lastAgent = lastAgent;
        }
    }
}