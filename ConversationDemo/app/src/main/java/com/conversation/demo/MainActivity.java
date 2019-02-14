package com.conversation.demo;

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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.nanorep.convesationui.fragments.NRConversationFragment;
import com.nanorep.convesationui.structure.ChatEventListener;
import com.nanorep.convesationui.structure.ChatServicesInjector;
import com.nanorep.convesationui.structure.FriendlyDatestampFormatFactory;
import com.nanorep.convesationui.structure.OptionActionListener;
import com.nanorep.convesationui.structure.UiConfigurations;
import com.nanorep.convesationui.structure.history.HistoryListener;
import com.nanorep.convesationui.structure.history.HistoryProvider;
import com.nanorep.convesationui.viewholder.base.ChatElementViewHolder;
import com.nanorep.convesationui.viewholder.controllers.ChatElementController;
import com.nanorep.convesationui.views.AgentTypingSettingsProvider;
import com.nanorep.convesationui.views.carousel.CarouselInfoContainer;
import com.nanorep.convesationui.views.carousel.CarouselItemConfiguration;
import com.nanorep.convesationui.views.carousel.CarouselItemsContainer;
import com.nanorep.convesationui.views.carousel.CarouselView;
import com.nanorep.convesationui.views.carousel.CarouselViewHolder;
import com.nanorep.convesationui.views.providers.CarouselViewsProvider;
import com.nanorep.convesationui.views.providers.ConversationViewsProvider;
import com.nanorep.convesationui.views.providers.FeedbackUIProvider;
import com.nanorep.nanoengine.ConversationStateListener;
import com.nanorep.nanoengine.Entity;
import com.nanorep.nanoengine.NRConversationEngine;
import com.nanorep.nanoengine.NRConversationMissingEntities;
import com.nanorep.nanoengine.NRPrivateInfo;
import com.nanorep.nanoengine.NanoAccess;
import com.nanorep.nanoengine.NanoRep;
import com.nanorep.nanoengine.Property;
import com.nanorep.nanoengine.chatelement.ChatElement;
import com.nanorep.nanoengine.chatelement.StorableChatElement;
import com.nanorep.nanoengine.model.AgentType;
import com.nanorep.nanoengine.model.CarouselElement;
import com.nanorep.nanoengine.model.NRAccount;
import com.nanorep.nanoengine.model.configuration.ConversationSettings;
import com.nanorep.nanoengine.model.configuration.TimestampStyle;
import com.nanorep.nanoengine.model.conversation.Conversation;
import com.nanorep.nanoengine.model.conversation.statement.OnStatementResponse;
import com.nanorep.nanoengine.model.conversation.statement.StatementRequest;
import com.nanorep.nanoengine.model.conversation.statement.StatementResponse;
import com.nanorep.nanoengine.model.conversation.statement.StatementScope;
import com.nanorep.sdkcore.model.StatementStatus;
import com.nanorep.sdkcore.types.NRError;
import com.nanorep.sdkcore.types.ViewHolder;
import com.nanorep.sdkcore.utils.UtilsKt;
import com.nanorep.sdkcore.utils.network.ConnectivityReceiver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static com.nanorep.nanoengine.model.Channels.Chat;
import static com.nanorep.nanoengine.model.Channels.PhoneNumber;
import static com.nanorep.nanoengine.model.Channels.Ticket;
import static com.nanorep.sdkcore.model.CommunicationModelsKt.StatusOk;
import static com.nanorep.sdkcore.model.CommunicationModelsKt.StatusPending;
import static com.nanorep.sdkcore.utils.UtilsKt.getPx;
import static com.nanorep.sdkcore.utils.UtilsKt.snack;
import static com.nanorep.sdkcore.utils.UtilsKt.toast;
import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity implements
        ConnectivityReceiver.ConnectivityListener,
        ConversationStateListener, HistoryProvider, ChatEventListener {

    private static final String TAG = "MainActivity";

    private static final String CONVERSATION_FRAGMENT_TAG = "conversation_fragment";
    public static final int HistoryPageSize = 8;
    public static final String END_HANDOVER_SESSION = "bye bye handover";

    private ImageButton startButton;
    private ProgressBar progressBar;

    private NRAccount account;
    private String conversationId;
    private NanoAccess nanoAccess;

    private ConcurrentHashMap<String, List<HistoryElement>> chatHistory = new ConcurrentHashMap<>();
    final Object historySync = new Object(); // in use to block multi access to history from different actions.

    private Map<String, OpenConversation> openConversations = new HashMap<>();

    private int handoverReplyCount = 0;
    private ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
    private ConcurrentLinkedQueue<StatementRequest> failedStatements = new ConcurrentLinkedQueue<>();
    private SoftReference<NRConversationFragment> conversationFragment;

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
    private OnStatementResponse onStatementResponse = new OnStatementResponse.Defaults() {

        @Override
        public void onResponse(@NotNull StatementResponse response) {
            if (conversationFragment.get() != null) {
                conversationFragment.get().onResponse(response);

            } else {
                // in case re-post of failed statements was done while chat was not loaded,
                // the app is responsible to update requests status and insert responses
                // to the history storage

                // update request history item with the new status.
                StatementRequest statementRequest = response.getStatementRequest();
                if (statementRequest != null) {
                    update(statementRequest.getTimestamp(), statementRequest.getTimestamp(), StatusOk);
                }
                // add response history item to the history storage
                store(StorableChatElement.StorableFactory.create(response));
            }
        }

        @Override
        public void onError(@NotNull NRError error) {
            Log.e("MainFragment", "failed to re-post request: " + error.getErrorCode() + ", " + error.getReason());
            MainActivity.this.onError(error);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);
        progressBar = findViewById(R.id.progressBar);

        startButton.setOnClickListener(view -> {
            setLoadingDisplay(true);
            initNanorep();
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setLoadingDisplay(false);
                if(conversationFragment != null) {
                    conversationFragment.clear();
                }
            }
        });
    }

    private void setLoadingDisplay(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        startButton.setEnabled(!isLoading);
    }

    @Override
    protected void onStart() {
        super.onStart();

        connectivityReceiver.register(this, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        connectivityReceiver.unregister(this);

        if(conversationFragment != null) {
            conversationFragment.clear();
        }

        try {
            nanoAccess.setConversationListener(null);
        } catch (Exception NRInitilizationException) {
            Log.e(TAG, "NanoRep was not initialized");
        }
    }

    public void initNanorep() {

        Map<String, String> conversationContext = new HashMap<>();

        account = getAccount();

        if (account.getAccount().isEmpty() || account.getKnowledgeBase().isEmpty()) {
            setLoadingDisplay(false);
            Toast.makeText(this, "Please provide valid account values", Toast.LENGTH_LONG).show();
            return;
        }

        account.setContext(conversationContext);
        account.setEntities(new String[]{"SUBSCRIBER"});

        ConversationSettings settings = new ConversationSettings()
                .speechEnable(true).enableMultiRequestsOnLiveAgent(true).setReadMoreThreshold(320)
                .timestampConfig(true, new TimestampStyle("hh:mm aaa",
                        getPx(11), Color.parseColor("#33aa33"), null))
                .enableOfflineMultiRequests(true) // defaults to true
                .enableLiveAgent(true)
                .datestamp(true, new FriendlyDatestampFormatFactory(this));

        //-> to configure the default text configuration to all bubbles text add something like the following:
        //settings.textStyleConfig(new StyleConfig(getPx(23), Color.BLUE, null));

        this.nanoAccess = NanoRep.initializeConversation(account, this,
                fetchAccountConversationData(account.getAccount()), settings);
    }

    @NonNull
    private NRAccount getAccount() {
        String accountName = "", apiKey = "", kb = "", server = "";
        try {
            accountName = ((EditText) findViewById(R.id.account_name_edit_text)).getText().toString();
            apiKey = ((EditText) findViewById(R.id.api_key_edit_text)).getText().toString();
            kb = ((EditText) findViewById(R.id.knowledgebase_edit_text)).getText().toString();
            server = ((EditText) findViewById(R.id.server_edit_text)).getText().toString();
        }catch (Exception ignored){}

        return new NRAccount( accountName, apiKey, kb, server, null);
    }

    private Conversation fetchAccountConversationData(String account) {

        Conversation conversation = null;

        if (openConversations.containsKey(account)) {
            OpenConversation openConversation = openConversations.get(account);

            if (openConversation != null) {
                conversation = new Conversation(openConversation.conversationId, openConversation.lastAgent);
            }
        }

        if (conversation == null) {
            conversation = new Conversation();
        }

        return conversation;
    }

    private void openConversationFragment() {

        FragmentManager fragmentManager = getSupportFragmentManager();

        // if activity is no longer up or fragment was already created :
        if(fragmentManager == null || fragmentManager.findFragmentByTag(CONVERSATION_FRAGMENT_TAG) != null){
            return;
        }

        this.conversationFragment = new SoftReference<>(
                NRConversationFragment.newInstance(new ChatServicesInjector() {

                    @Override
                    public HistoryProvider getHistoryProvider() {
                        return MainActivity.this;
                    }

                    @Override
                    public ConversationViewsProvider getUiProvider() {
                        return new MyConversationUiProvider();
                    }

                }, MainActivity.this)
        );

        fragmentManager
                .beginTransaction()
                .replace(R.id.content_main, conversationFragment.get(), CONVERSATION_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void connectionChanged(boolean isConnected) {
        this.connectionOk = isConnected;
        if(isConnected){
            if(pendingConversationCreation){
                nanoAccess.createConversation(this.account);

            } else {
                //!! there is no need to activate refresh on NRConversationFragment, it's done internally
                /*if(conversationFragment != null && conversationFragment.get() != null) {
                    conversationFragment.get().refresh();
                }*/

                // post pending statements until disconnection
                postFailedStatements();
            }
        }
    }

    private void postFailedStatements() {

        for(StatementRequest request : failedStatements){
            if(!connectionOk) break; // no point of trying to re-send

            Log.d("MainFragment", "re-sending previously failed request: "+request.getStatement());

            if(request.getScope() instanceof StatementScope.LiveHandoverScope){
                onChatHandoverInput(request);

            } else {
                //!! a MUST, otherwise requests will be sent with previous or 0 id and will fail
                request.conversationId(conversationId);

                nanoAccess.postStatement(request, onStatementResponse);
            }

            failedStatements.remove(request);
        }
    }


//<editor-fold desc=">>>>>>>>>  ConversationStateListener Impl  <<<<<<<<<">

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

    @Override
    public void onConversationTermination(@NotNull Conversation conversation) {

       OpenConversation openConversation;
        String accountId = this.account.getAccount();
        if (openConversations.containsKey(accountId)) {
            openConversation = openConversations.get(accountId);
            openConversation.conversationId = conversation.getId();
            openConversation.lastAgent = conversation.getAgentType();
        } else {
            openConversation = new OpenConversation(accountId, conversation.getId(), conversation.getAgentType());
            openConversations.put(accountId, openConversation);
        }
    }

    /**
     * Missing entity contains extra data - "Properties" that are being provided by the app (which can bring the data from external resources).
     * For example: If the provider contains Missing entity called "SUBSCRIBER", the app is being notified here and returns the extra data.
     *
     * @param response - The response from nanorep that contains the Missing Entities
     * @param missingEntitiesHandler - Would be informed when the missing entities are ready to be injected to the provider
     */
    @Override
    public void onMissingEntities(@NotNull StatementResponse response, @NotNull NRConversationEngine.MissingEntitiesHandler missingEntitiesHandler) {
        NRConversationMissingEntities missingEntities = new NRConversationMissingEntities(response.getStatement());
        for (String missingEntity : response.getMissingEntities()) {
            if (missingEntity.equals("SUBSCRIBER")) {
                missingEntities.addEntity(createEntity(missingEntity));
            }
        }

        missingEntitiesHandler.onMissingEntitiesReady(missingEntities);
    }

    // For this example, the app returns random data:
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
    public void handlePersonalInformation(@NotNull String id, @NotNull NRPrivateInfo privateInfo) {
        switch (id) {
            case "balance":
                String balance = String.format("%10.2f$", Math.random() * 10000);
                privateInfo.getValueReady().privateDataCallback(balance, null);
                return;
        }

        privateInfo.getValueReady().privateDataCallback("1,000$", null);
    }

    @Override
    public void onInitializeChatHandover() {
        handoverReplyCount = 0;
        if (!connectionOk) {
            nanoAccess.endHandover();
            // response scope should be provided indicate the chat agent status while statement were injected.
            // (see StatementScope for available scopes)
            conversationFragment.get().injectSystem("Failed to initiate live chat, " +
                    "please check your internet connection");
        } else {
            passDelayedResponse("Hey, my name is Bob, how can I help?", 1);
        }
    }

    @Override
    public void onChatHandoverInput(@NonNull StatementRequest statementRequest) {
        if(nanoAccess == null) return;

        OnStatementResponse inputCallback = statementRequest.getCallback();

        boolean endHandover = isEndHandover(statementRequest.getText());

        // in case request to live agent can't be delivered (in our demo- connection failure)
        if(!connectionOk){
            // pass live agent request error indication to the chat SDK
            if(inputCallback != null) {
                inputCallback.onError(new NRError(NRError.LiveStatementError, NRError.ConnectionException, statementRequest));
            }
            failedStatements.add(statementRequest);

        } else {

            passHandoverResponse(statementRequest, endHandover);
        }

        // verify handover is still on to prevent requests from being handled as handover.
        if(endHandover) {
            nanoAccess.endHandover();
        }
    }

    private void passDelayedResponse(final String responseText, final int count) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int random = count != -1 ? count-1  : new Random().nextInt(3);
                for (int i = 0; i <= random; i++) {
                    passHandoverResponse(responseText + (i == 0 ? "" : "_" + i), 1000);
                }
            }
        }).start();
    }

    private void passHandoverResponse(@NonNull StatementRequest statementRequest, boolean endHandover) {

        // pass live agent response to the chat SDK
        nanoAccess.updateRequestStatus(statementRequest, StatusOk);

        if (endHandover) {
            passHandoverResponse("Bye - Handover complete", 0);

        } else {
            //!- for long text test:
            String longText = "John Perry did two things on his 75th birthday. First, he visited his wife's grave. Then he joined the army.\n" +
                    "The good news is that humanity finally made it into interstellar space. The bad news is that planets fit to live on are scarce - and alien races willing to fight us for them are common. So, we fight, to defend Earth and to stake our own claim to planetary real estate. Far from Earth, the war has been going on for decades: brutal, bloody, unyielding.\n" +
                    "\n" +
                    "Earth itself is a backwater. The bulk of humanity's resources are in the hands of the Colonial Defense Force. Everybody knows that when you reach retirement age, you can join the CDF. They don't want young people; they want people who carry the knowledge and skills of decades of living. You'll be taken off Earth and never allowed to return. You'll serve two years at the front. And if you survive, you'll be given a generous homestead stake of your own, on one of our hard-won colony planets. ";
            String postText = handoverReplyCount == 1 ? longText : "";
            final String responseText = postText + "Response (" + String.valueOf(++handoverReplyCount) + ")";

            passDelayedResponse(responseText, postText.isEmpty() ? -1 : 1);
        }
    }

    private void passHandoverResponse(final String response, int delay) {

        // Present typing indication
        UtilsKt.runInMain(nanoAccess, new Function1<NanoAccess, Unit>() {
            @Override
            public Unit invoke(NanoAccess nanoAccess) {
                nanoAccess.updateTypingState(true);
                return null;
            }
        });

        if (delay != 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Hide typing indication
        UtilsKt.runInMain(nanoAccess, new Function1<NanoAccess, Unit>() {
            @Override
            public Unit invoke(NanoAccess nanoAccess) {
                nanoAccess.updateTypingState(false);

                if (conversationFragment.get() != null) {
                    conversationFragment.get().injectResponse(response, StatementScope.HandoverScope());
                }
                return null;
            }
        });
    }

    private boolean isEndHandover(String inputText) {
        return inputText != null && inputText.equalsIgnoreCase(END_HANDOVER_SESSION);
    }

//</editor-fold>


//<editor-fold desc=">>>>>>>>> Error handling and ErrorListener Impl  <<<<<<<<<">

    @SuppressLint("ResourceType")
    @Override
    public void onError(@NotNull NRError error) {
        progressBar.setVisibility(View.INVISIBLE);

        switch (error.getErrorCode()) {

            case NRError.ConversationCreationError:

                notifyConversationError(error);

                pendingConversationCreation = true;

                if (NRError.ConnectionException.equals(error.getReason())) {
                    if (conversationFragment == null || conversationFragment.get() == null) {
                        openConversationFragment();

                        //!! request injection is not allowed right after fragment committing to backstack
                        // that will result with StatementError. wait to the onChatLoaded event
                        // (see onChatLoaded)
                    }
                }

                View accountBtn = getConversationStartBtn();
                if (accountBtn != null) {
                    accountBtn.setEnabled(true);
                }
                break;

            case NRError.StatementError:

                if (conversationFragment == null || conversationFragment.get() == null) return;

                if (error.isConversationError()) {
                    notifyConversationError(error);
                    pendingConversationCreation = true;

                } else {
                    notifyStatementError(error);
                }
                StatementRequest statementRequest = (StatementRequest) error.getData();
                if (statementRequest != null) {
                    //Log.d("MainFragment", "removing failed request " + statementRequest.getStatement() + " from History");
                    //removeFromHistory(statementRequest.getTimestamp());

                    Log.d("MainFragment", "adding " + statementRequest.getStatement() + " to app pending");
                    failedStatements.add(statementRequest);
                }
                break;

            default:
                /*all other errors will be handled here. Demo implementation, displays a toast and
                  writes to the log.
                 if needed the error.getErrorCode() and sometimes the error.getReason() can provide
                 the details regarding the error
                 */
                if (getContext() != null) {
                    Log.e("App-ERROR", error.toString());
                    toast(this, error.toString(), 2500);
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
        View snackView = conversationFragment == null || conversationFragment.get() == null || conversationFragment.get().getView() == null ?
                getWindow().getDecorView() : conversationFragment.get().getView();

        try {
            snack(snackView,
                    s + error.getReason(),
                    1000, -1, Gravity.CENTER, new int[]{}, backgroundColor);
        } catch (Exception ignored) {
        }
    }

    View getConversationStartBtn() {
        return startButton;
    }

//</editor-fold>

//<editor-fold desc=">>>>>>>>>  ChatEventListener impl  <<<<<<<<<">

    @Override
    public void onChatLoaded() {
        // now the chat is ready to receive requests injection
        if(getHistorySize() <= 1) { // the welcome message was already inserted to the history
            // example: injecting a request on each return to the conversation.
//            conversationFragment.get().injectResponse("Hello, this is a sample text injection after load", StatementScope.SystemScope());
        }
    }

    @Override
    public void onInAppNavigation(String navTo) {
        Toast.makeText(this, navTo, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onUrlNavigation(String url) {
        // sample url handling:
        if(connectionOk){
            final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
            this.startActivity(intent);
        } else {
            Toast.makeText(this, "Connection is not available for opening url: "+url, Toast.LENGTH_SHORT).show();
        }

        return connectionOk; // returns if the link could be activated
    }

    @Override
    public void onPhoneNumberNavigation(@NonNull String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {}
    }

//</editor-fold>


//<editor-fold desc=">>>>>>>>>  History handling and HistoryProvider Impl  <<<<<<<<<">

    private int getHistorySize() {
        String accountId = this.account != null ? this.account.getAccount() : null;
        return accountId != null && chatHistory.containsKey(accountId) ? chatHistory.get(accountId).size() : 0;
    }

    @Override
    public void onFileUploadClicked() {
        Toast.makeText(this, "File upload clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void fetch(int from, int direction, @Nullable HistoryListener listener) {

        new Thread(() -> {
            final List<? extends StorableChatElement> history;

            synchronized (historySync) {
                history = Collections.unmodifiableList(getHistoryForAccount(account.getAccount(), from, direction));
            }

            if (history.size() > 0) {
                try {
                    Thread.sleep(800); // simulate async history fetching
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(() -> {
                Log.d("History", "passing history list to listener, from = " + from + ", size = " + history.size());
                if (listener != null) {
                    listener.onReady(from, direction, history);
                }
            });
        }).start();
    }

    private List<HistoryElement> getHistoryForAccount(String account, int fromIdx, int direction) {

        List<HistoryElement> accountChatHistory = chatHistory.get(account);

        if (accountChatHistory == null)
            return new ArrayList<>();

        boolean fetchOlder = direction == Older;

        // to prevent Concurrent exception
        CopyOnWriteArrayList<HistoryElement> accountHistory = new CopyOnWriteArrayList<>(accountChatHistory);

        int historySize = accountHistory.size();

        if (fromIdx == -1) {
            fromIdx = fetchOlder ? historySize - 1 : 0;
        } else if (fetchOlder) {
            fromIdx = historySize - fromIdx;
        }

        int toIndex = fetchOlder ? Math.max(0, fromIdx - HistoryPageSize) :
                Math.min(fromIdx + HistoryPageSize, historySize - 1);

        try {
            Log.d("History", "fetching history items (" + historySize + ") from " + toIndex + " to " + fromIdx);

            return accountHistory.subList(toIndex, fromIdx);

        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    @NonNull
    private ArrayList<HistoryElement> getAccountHistory() {
        ArrayList<HistoryElement> convHistory;
        String account = this.account.getAccount();

        if (chatHistory != null && chatHistory.containsKey(account)) {
            convHistory = (ArrayList<HistoryElement>) chatHistory.get(account);
        } else {
            convHistory = new ArrayList<>();
            chatHistory.put(account, convHistory);
        }
        return convHistory;
    }

    @Override
    public void store(@NotNull StorableChatElement item) {
        //if(item == null || item.getStatus() != StatusOk) return;

        synchronized (historySync) {
            ArrayList<HistoryElement> convHistory = getAccountHistory();
            convHistory.add(new HistoryElement(item));
        }
    }

    @Override
    public void remove(long timestampId) {
        synchronized (historySync) {
            ArrayList<HistoryElement> convHistory = getAccountHistory();

            Iterator<HistoryElement> iterator = convHistory.listIterator();
            while (iterator.hasNext()) {
                HistoryElement item = iterator.next();
                if (item.getTimestamp() == timestampId) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    @Override
    public void update(long timestampId, long newTimestamp, int status) {

        synchronized (historySync) {
            ArrayList<HistoryElement> convHistory = getAccountHistory();

            for (HistoryElement item : convHistory) {
                if (item.getTimestamp() == timestampId) {
                    item.timestamp = newTimestamp;
                    item.status = status;
                    break;
                }
            }
        }
    }

    @Override
    public void update(long timestampId, @NotNull StorableChatElement item) {
        synchronized (historySync) {
            ArrayList<HistoryElement> convHistory = getAccountHistory();
            for (int i = convHistory.size() - 1; i >= 0; i--) {
                if (convHistory.get(i).getTimestamp() == timestampId) {
                    convHistory.set(i, new HistoryElement(item));
                    break;
                }
            }
                /* -> Another option:
                for (HistoryElement historyElement: convHistory) {
                    if (historyElement.getTimestamp() == timestampId) {
                        historyElement.scope = item.getScope();
                        historyElement.status = item.getStatus();
                        historyElement.key = item.getStorageKey();
                        historyElement.type = item.getType();
                        break;
                    }
                }*/
        }
    }

//</editor-fold>

//<editor-fold desc=">>>>>>>>>  Carousel overriding implementations:  <<<<<<<<<">

class MyCarouselViewsProvider extends CarouselViewsProvider {
    @Nullable
    @Override
    public ViewHolder getCarouselInfoHolder(@NotNull CarouselElement carouselData, @Nullable CarouselInfoContainer carouselInfoContainer) {
        DemoCarouselInfoViewHolder demoCarouselInfoViewHolder = new DemoCarouselInfoViewHolder(carouselInfoContainer);
        demoCarouselInfoViewHolder.update(carouselData);
        return demoCarouselInfoViewHolder;
    }

    @NotNull
    @Override
    public CarouselInfoContainer getCarouselInfoView(@NotNull Context context) {
        CarouselInfoContainer carouselInfoContainer = super.getCarouselInfoView(context);
        carouselInfoContainer.setTextBackground(context.getResources().getDrawable(R.drawable.bg_inbox_normal));
        carouselInfoContainer.setMargins(0, 0, getPx(30), 0);
        carouselInfoContainer.setIconAlignment(UiConfigurations.Alignment.AlignTop);
        carouselInfoContainer.setIconTextGap(getPx(4)); // change the margin between the bot icon and the bubble text
        // change the text style of the carousel info section (in bubble)
        // carouselInfoContainer.setTextStyle(new StyleConfig(getPx(20), Color.BLUE, getTypeface(getContext(), "great_vibes.otf")));
        // change the carousel timestamp display (if not set, will use the stype provided by ConversationSettings or default if not available)
        // carouselInfoContainer.setTimestampStyle(new TimestampStyle("hh:mm", getPx(11), Color.parseColor("#a8a8a8"), null));

        return carouselInfoContainer;
    }

    @NotNull
    @Override
    public CarouselViewHolder injectCarousel(@NotNull Context context, @NotNull ChatElementController controller, @Nullable CarouselElement carouselData, @Nullable OptionActionListener optionListener) {
        CarouselView carouselView = (CarouselView) super.injectCustomCarousel(context, controller, carouselData, null);
        ((CarouselView.LayoutParams) carouselView.getLayoutParams()).setMargins(0, getPx(16), 0, 0);
        return carouselView;
    }

    /**
     * example to override carousel items list properties.
     *
     * @param context
     * @param optionListener
     * @return
     */
    @NotNull
    @Override
    public CarouselItemsContainer getCarouselItemsContainer(@NotNull Context context, @Nullable OptionActionListener optionListener) {
        CarouselItemsContainer itemsContainer = super.getCarouselItemsContainer(context, optionListener);

        itemsContainer.setOptionsHorizontalAlignment(Gravity.END);
        itemsContainer.setOptionsVerticalAlignment(Gravity.BOTTOM);

        itemsContainer.setOptionsBackground(/*new ColorDrawable(Color.TRANSPARENT));*/ getResources().getDrawable(R.drawable.c_item_option_back));
        itemsContainer.setOptionsTextStyle(Color.parseColor("#5EC4B6"), null);
        itemsContainer.setOptionsTextSize(16);

        itemsContainer.setItemBackground(getResources().getDrawable(R.drawable.bkg_bots));//new ColorDrawable(Color.parseColor("#f1f8ff"))/*getResources().getDrawable(R.drawable.c_item_back)*/);
        itemsContainer.setCardStyle(getPx(10), getPx(5));// set the carousel items to have a "card" look. [roundCornersRadius, elevation]
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

//</editor-fold>

///////////////////////////////


    class MyConversationUiProvider extends ConversationViewsProvider {

        DynamicBubbleBind dynamicBubbleBind = new DynamicBubbleBind();

        MyConversationUiProvider() {
            super(new MyCarouselViewsProvider(), new FeedbackUIProvider());
                /* !! uncomment this to feedback view override
                    {
                @NotNull
                @Override
                public FeedbackUIAdapter getFeedbackUIAdapter(@NotNull Context context, int feedbackDisplayType) {
                    return new FeedbackViewDummy(context);
                }
            }*/
        }

        /**
         * provide resources for quick options
         */
        @Override
        protected String getChannelResourceName(int optionChannelType) {
            String resourceName = null;
            switch (optionChannelType) {
                case PhoneNumber:
                    resourceName = "R.drawable.call";
                    break;
                case Chat:
                    resourceName = "R.drawable.chat";
                    break;
                case Ticket:
                    resourceName = "R.drawable.email";
                    break;
            }
            return resourceName;
        }

        @Override
        public int getQuickOptionsLayout() {
            return R.layout.quick_option_layout_custom;
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
            return new DemoRemoteViewHolder(view, controller, dynamicBubbleBind);
        }

        @Override
        public ChatElementViewHolder getLocalBubbleViewHolder(View view, ChatElementController controller) {
            return new DemoLocalViewHolder(view, controller, dynamicBubbleBind);
        }

        @Override
        public int getFragmentBackground() {
            return R.drawable.bold_form_bg;
        }

        @Override
        public int getUserInputLayout() {
            return R.layout.user_input_view_holder_custom;
        }

        @Override
        public AgentTypingSettingsProvider getAgentTypingSettingsProvider() {
            AgentTypingSettingsProvider agentTypingSettingsProvider = new AgentTypingSettingsProvider();
            agentTypingSettingsProvider.setMargins(0, 0, 0, 49);
            agentTypingSettingsProvider.setGravity(Gravity.BOTTOM);
            agentTypingSettingsProvider.setCompoundText("please wait");
            agentTypingSettingsProvider.setCompoundDrawable(R.drawable.ic_more_horiz_black_24dp);
            return agentTypingSettingsProvider;
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

/////////////////////////////////////

    /**
     * {@link StorableChatElement} implementing class
     * sample class for app usage
     */
    static class HistoryElement implements StorableChatElement {
        byte[] key;
        long timestamp = 0;
        StatementScope scope;
        @ChatElement.Companion.ChatElementType
        int type;
        @StatementStatus
        int status = StatusPending;

        protected HistoryElement(int type, long timestamp) {
            this.type = type;
            this.timestamp = timestamp;
        }

        HistoryElement(StorableChatElement storable) {
            key = storable.getStorageKey();
            type = storable.getType();
            timestamp = storable.getTimestamp();
            status = storable.getStatus();
            scope = storable.getScope();
        }

        @NotNull
        @Override
        public byte[] getStorageKey() {
            return key;
        }

        @NotNull
        @Override
        public String getStorableContent() {
            return new String(key);
        }

        @Override
        public int getType() {
            return type;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public StatementScope getScope() {
            return scope;
        }

        @Override
        public boolean isStorageReady() {
            return true;
        }
    }

/////////////////////////////////////

}
