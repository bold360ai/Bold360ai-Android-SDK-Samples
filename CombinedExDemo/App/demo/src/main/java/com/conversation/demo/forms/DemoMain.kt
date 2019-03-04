package com.conversation.demo.forms

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.util.Log
import android.view.*
import android.widget.Toast
import com.conversation.demo.ChatType
import com.conversation.demo.R
import com.integration.bold.boldchat.core.FormData
import com.integration.bold.boldchat.core.PostChatData
import com.integration.core.FormResults
import com.integration.core.StateEvent
import com.integration.core.StateEvent.Companion.ChatWindowDetached
import com.integration.core.StateEvent.Companion.ChatWindowLoaded
import com.integration.core.StateEvent.Companion.Ended
import com.integration.core.StateEvent.Companion.Preparing
import com.integration.core.StateEvent.Companion.Started
import com.integration.core.StateEvent.Companion.Unavailable
import com.nanorep.convesationui.bold.ui.FormListener
import com.nanorep.convesationui.structure.FriendlyDatestampFormatFactory
import com.nanorep.convesationui.structure.UiConfigurations
import com.nanorep.convesationui.structure.controller.*
import com.nanorep.convesationui.structure.feedback.FeedbackUIAdapter
import com.nanorep.convesationui.structure.feedback.FeedbackViewDummy
import com.nanorep.convesationui.structure.handlers.AccountInfoProvider
import com.nanorep.convesationui.structure.providers.ChatUIProvider
import com.nanorep.convesationui.structure.providers.FeedbackUIProvider
import com.nanorep.convesationui.views.carousel.CarouselItemsUIAdapter
import com.nanorep.convesationui.views.chatelement.BubbleContentUIAdapter
import com.nanorep.nanoengine.Account
import com.nanorep.nanoengine.AccountInfo
import com.nanorep.nanoengine.chatelement.IncomingElementModel
import com.nanorep.nanoengine.chatelement.OutgoingElementModel
import com.nanorep.nanoengine.model.configuration.ConversationSettings
import com.nanorep.nanoengine.model.configuration.StyleConfig
import com.nanorep.nanoengine.model.configuration.TimestampStyle
import com.nanorep.sdkcore.model.StatementScope
import com.nanorep.sdkcore.model.isLive
import com.nanorep.sdkcore.utils.*
import kotlinx.android.synthetic.main.fragment_main.*
import org.jetbrains.anko.toast
import java.lang.ref.WeakReference
import java.util.*

internal const val DemoMain_TAG = "DemoMain"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [DemoMain.FragmentInteraction] interface
 * to handle interaction events.
 * Use the [DemoMain.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class DemoMain : Fragment(), ChatEventListener, AccountListener {
    val CONVERSATION_FRAGMENT_TAG = "conversation_fragment"
    val DEMO_FORM_TAG = "demo_form_fragment"

    internal var menu: Menu? = null

    private val notificationsReceiver = NotificationsReceiver()

    private val accounts = HashMap<String, AccountInfo>()

    private var chatController: ChatController? = null
    private var historyProvider: MyHistoryProvider? = null

    /*!! ChatController's providers kept as members to make sure their object will be kept alive
       (chatController handles those listeners and providers as weak references, which means they
       may be released otherwise) */
    private var accountInfoProvider: MyAccountInfoProvider? = null
    private var formProvider: MyFormProvider? = null

    private var outerInteraction: FragmentInteraction? = null

    private val boldChatMenu: MenuItem?
        get() {
            return menu?.getItem(0)
        }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bold_chat.setOnClickListener { onChatClick(it) }
        bot_chat.setOnClickListener { onChatClick(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountInfoProvider = MyAccountInfoProvider()
        formProvider = MyFormProvider(this)
        historyProvider = MyHistoryProvider()
    }

    fun showWaiting(show: Boolean) {
        outerInteraction?.enableWaiting(show)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentInteraction) {
            outerInteraction = context
        } else {
            throw RuntimeException("$context must implement FragmentInteraction")
        }
    }

    override fun onDetach() {
        super.onDetach()
        outerInteraction = null
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        this.menu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.getItemId()) {
            R.id.action_settings -> return false
            R.id.bold_end -> {
                chatController?.endChat(false)
                return true
            }
            else -> {
            }
        }

        return false
    }

    private fun clearsAllResources() {
        try {
            chatController?.run {
                unsubscribeNotifications(notificationsReceiver)
                terminateChat()
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    override fun onStop() {
        if (isRemoving) {
            clearsAllResources()
        }

        super.onStop()
    }

    override fun onReady(account: Account) {
        outerInteraction?.enableWaiting(true)

        if (!accounts.containsKey(account.apiKey)) {
            accounts[account.apiKey] = account
        }

        historyProvider?.accountId = account.getApiKey()

        this.chatController = createChat(account)?.apply {
            subscribeNotifications(
                notificationsReceiver, ChatNotifications.PostChatFormSubmissionResults,
                ChatNotifications.UnavailabilityFormSubmissionResults
            )
        }
    }

    private fun onChatClick(view: View) {
        val chatType = if (view.id == R.id.bold_chat) ChatType.LiveChat else ChatType.BotChat

        val accountForm = AccountForm.create(chatType, this)
        fragmentManager?.beginTransaction()?.apply {
            replace(R.id.content_main, accountForm)
            addToBackStack(null)
            commit()
        }
    }

    private fun createChat(account: Account): ChatController? {
        if (context == null) return null

        val settings = ConversationSettings()
            .speechEnable(true)
            .enableMultiRequestsOnLiveAgent(true)
            .timestampConfig(
                true, TimestampStyle(
                    "dd.MM hh:mm:ss",
                    10, Color.parseColor("#33aa33"), null
                )
            )
            .datestamp(true, FriendlyDatestampFormatFactory(context))

        return ChatController.Builder(context!!).apply {
            conversationSettings(settings)
            chatEventListener(this@DemoMain)
            chatUIProvider(getCustomisedChatUI(context!!))

            historyProvider?.run { historyProvider(this) }
            accountInfoProvider?.run { accountProvider(this) }
            formProvider?.run { formProvider(this) }
        }
            .build(account, object : ChatLoadedListener {

                override fun onComplete(result: ChatLoadResponse) {

                    val error = result.error

                    if (error != null) {
                        onError(error)
                        if (!(error.isConversationError() || error.isServerConnectionNotAvailable())) {
                            openConversationFragment(result.fragment)
                        }

                    } else {
                        openConversationFragment(result.fragment)
                    }
                    outerInteraction?.enableWaiting(false)
                }
            })
    }


    override fun onAccountUpdate(accountInfo: AccountInfo) {
        val savedAccount = getAccountInfo(accountInfo.getApiKey())
        if (savedAccount != null) {
            savedAccount.updateInfo(accountInfo.getInfo())
        } else {
            Log.w(
                CONVERSATION_FRAGMENT_TAG,
                "Got account update for account that is currently not " + "in accounts list\nadding account to saved accounts list"
            )
            accounts[accountInfo.getApiKey()] = accountInfo
        }
    }

    private fun openConversationFragment(fragment: Fragment) {
        val fragmentManager = getFragmentManager()

        if (fragmentManager == null || fragmentManager.isStateSaved() ||
            fragmentManager.findFragmentByTag(CONVERSATION_FRAGMENT_TAG) != null
        )
            return

        fragmentManager.beginTransaction()
            .replace(R.id.content_main, fragment, CONVERSATION_FRAGMENT_TAG)
            .addToBackStack(CONVERSATION_FRAGMENT_TAG)
            .commit()
    }

    private fun getAccountInfo(apiKey: String): AccountInfo? {
        return accounts[apiKey]
    }

    @SuppressLint("ResourceType")
    override fun onError(error: NRError) {

        val reason = error.reason

        when (error.errorCode) {

            NRError.ConversationCreationError -> {

                notifyConversationError(error)

                if (reason != null && reason == NRError.ConnectionException) {
                    notifyConnectionError()
                }
            }

            NRError.StatementError ->

                if (error.isConversationError()) {
                    notifyConversationError(error)

                } else {
                    notifyStatementError(error)
                }

            else -> {
                /*all other errors will be handled here. Demo implementation, displays a toast and
                  writes to the log.
                 if needed the error.getErrorCode() and sometimes the error.getReason() can provide
                 the details regarding the error
                 */
                Log.e("App-ERROR", error.toString())

                if (reason != null && reason == NRError.ConnectionException) {
                    notifyConnectionError()
                } else {
                    notifyError(error, "general error: ", Color.DKGRAY)
                }
            }
        }
    }

    private fun notifyConnectionError() {
        context?.toast("Connection failure.\nPlease check your connection.")
    }

    private fun notifyConversationError(error: NRError) {
        notifyError(error, "Conversation is not available: ", Color.parseColor("#6666aa"))
    }

    private fun notifyStatementError(error: NRError) {
        notifyError(error, "statement failure - ", Color.RED)
    }

    @SuppressLint("ResourceType")
    private fun notifyError(error: NRError, s: String, i: Int) {

        try {
            val fragmentList = fragmentManager?.fragments
            val snackView = fragmentList?.get(fragmentList.size - 1)?.view

            snackView?.snack(
                s + error.reason + ": " + error.description,
                4000, -1, Gravity.CENTER, intArrayOf(), i
            )

        } catch (ignored: Exception) {
            context?.toast(s + error.reason + ": " + error.description)
        }
    }

    override fun onUrlLinkSelected(url: String) {
        // sample code for handling given link
        try {
            val intent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(CONVERSATION_FRAGMENT_TAG, "failed to activate link on default app: " + e.message)
            Toast.makeText(context, "activating: $url", Toast.LENGTH_SHORT).show()
        }

        Log.w(CONVERSATION_FRAGMENT_TAG, "got link activation while activity is no longer available.\n($url)")
    }

    override fun onChatStateChanged(stateEvent: StateEvent) {

        Log.d(DemoMain_TAG, "onChatStateChanged: state " + stateEvent.state + ", scope = " + stateEvent.scope)

        when (stateEvent.state) {

            Preparing -> {
            }

            Started ->
                // display end chat button
                if (stateEvent.scope == StatementScope.BoldScope) {
                    menu?.getItem(0)?.isVisible = true
                }

            ChatWindowLoaded -> {
            }

            Unavailable -> {
                context?.toast("Chat unavailable due to " + stateEvent.data!!)
                // hide end chat button
                if (stateEvent.scope.isLive()) {
                    menu?.getItem(0)?.isVisible = false
                }
            }

            Ended -> if (stateEvent.scope.isLive()) {
                menu?.getItem(0)?.isVisible = false
            }

            ChatWindowDetached -> {
                fragmentManager?.popBackStack(CONVERSATION_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        }
    }

    //-> previous listener method signature @Override onPhoneNumberNavigation(@NonNull String phoneNumber) {
    override fun onPhoneNumberSelected(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phoneNumber")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {

        }

    }

    // How can we change default look of chat elements UI:
    private fun getCustomisedChatUI(context: Context): ChatUIProvider {

        val uiProvider = ChatUIProvider(context)

        uiProvider.chatBackground = resources.getDrawable(R.drawable.bkg_bots)

        val incomingUIProvider = uiProvider.chatElementsUIProvider.incomingUIProvider

        incomingUIProvider
            .configure = fun(adapter: BubbleContentUIAdapter): BubbleContentUIAdapter {
            adapter.setBackground(resources.getDrawable(R.drawable.in_bubble))
            adapter.setAvatar(resources.getDrawable(R.drawable.bot_avatar))

            return adapter
        }

        incomingUIProvider
            .customize = fun(adapter: BubbleContentUIAdapter, element: IncomingElementModel?): BubbleContentUIAdapter {
            if (element != null) {
                val scope = element.elemScope
                if (scope.isLive()) {
                    adapter.setAvatar(resources.getDrawable(R.drawable.agent))
                    adapter.setTextStyle(StyleConfig(16, Color.DKGRAY))
                    adapter.setBackground(resources.getDrawable(R.drawable.live_in_back))
                }
            }
            return adapter
        }

        incomingUIProvider.carouselUIProvider.configure = fun(adapter: CarouselItemsUIAdapter): CarouselItemsUIAdapter {
            adapter.setCardStyle(4f, 10f)
            adapter.setOptionsTextStyle(StyleConfig(14, Color.parseColor("#4a4a4a")))
            return adapter
        }


        /*incomingUIProvider.carouselUIProvider.customize =
            fun(adapter: CarouselItemsUIAdapter, element: CarouselElementModel?): CarouselItemsUIAdapter {

                val elemCarouselItems = element?.elemCarouselItems
                val size = elemCarouselItems?.size ?: 0
                if (size >= 3) {
                    adapter.setOptionsTextStyle(StyleConfig(14, Color.RED, null))
                }
                return adapter
            }*/

        //incomingUIProvider.feedbackUIProvider.overrideFactory = MyFeedbackFactory()

        val outgoingUIProvider = uiProvider.chatElementsUIProvider.outgoingUIProvider

        outgoingUIProvider.configure = fun(adapter: BubbleContentUIAdapter): BubbleContentUIAdapter {
            adapter.setTextStyle(StyleConfig(17, Color.WHITE, Typeface.SANS_SERIF))
            adapter.setBackground(resources.getDrawable(R.drawable.out_bubble))
            adapter.setTextAlignment(
                UiConfigurations.Alignment.AlignEnd,
                UiConfigurations.Alignment.AlignTop
            )
            return adapter
        }

        outgoingUIProvider.customize =
            fun(adapter: BubbleContentUIAdapter, element: OutgoingElementModel?): BubbleContentUIAdapter {
                /*if (element != null && element.elemContent.toLowerCase().contains("the")) {
                    adapter.setTimestampStyle(TimestampStyle("E, HH:mm:ss", 11, Color.MAGENTA, null))
                    adapter.setTextStyle(StyleConfig(null, Color.BLUE, Typeface.SERIF))
                    adapter.setBackground(null)
                }*/

                if (element != null) {
                    val scope = element.elemScope
                    if (scope.isLive()) {
                        //adapter.setAvatar(resources.getDrawable(R.drawable.bold_360))
                        adapter.setTextStyle(StyleConfig(16, Color.RED))
                        adapter.setBackground(resources.getDrawable(R.drawable.live_out_back))
                    }
                }
                return adapter
            }

        return uiProvider
    }

    internal inner class MyFeedbackFactory : FeedbackUIProvider.FeedbackFactory {

        override fun create(context: Context, feedbackDisplayType: Int): FeedbackUIAdapter {
            return FeedbackViewDummy(context)
        }
    }

    internal inner class MyAccountInfoProvider : AccountInfoProvider {

        override fun updateAccountInfo(account: AccountInfo) {
            val savedAccount = getAccountInfo(account.getApiKey())
            savedAccount?.updateInfo(account.getInfo())

        }

        override fun provide(info: String, callback: Completion<AccountInfo?>) {
            val savedAccount = getAccountInfo(info)
            callback.onComplete(savedAccount)
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface FragmentInteraction {
        fun enableWaiting(enable: Boolean)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment.
         *
         * @return A new instance of fragment DemoMain.
         */
        @JvmStatic
        fun newInstance() = DemoMain()
    }
}

internal class MyFormProvider(parent: DemoMain) : FormProvider {

    private val parent: WeakReference<DemoMain> = WeakReference(parent)

    override fun presentForm(formData: FormData, callback: FormListener) {
        if (parent.get() == null) {
            Log.w("DemoMain", "presentForm: form can't be presented, activity reference is lost")
            callback.onCancel(formData.formType)
            return
        }

        // Demo implementation that presents present a dummy form :
        val fragment = DemoForm.create(formData, callback)

        val fragmentManager = parent.get()?.fragmentManager

        fragmentManager?.beginTransaction()?.add(R.id.content_main, fragment, DEMO_FORM_TAG)
            ?.addToBackStack(DEMO_FORM_TAG)
            ?.commit()

        Log.v("formData", "form type: " + formData.formType + " form data:" + formData.logFormBrandings())

        if (formData is PostChatData) {
            parent.get()?.menu?.getItem(0)?.isVisible = false
        }
    }

}

internal class NotificationsReceiver : Notifiable {

    override fun onNotify(notification: Notification, dispatcher: DispatchContinuation) {
        when (notification.notification) {
            ChatNotifications.PostChatFormSubmissionResults, ChatNotifications.UnavailabilityFormSubmissionResults -> {
                val results = notification.data as FormResults?
                if (results != null) {
                    Log.i(
                        DemoMain_TAG, "Got notified for form results for form: " +
                                results.data +
                                if (results.error != null) ", with error: " + results.error!! else ""
                    )

                } else {
                    Log.w(DemoMain_TAG, "Got notified for form results but results are null")
                }
            }
        }
    }
}