package com.conversation.demo

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.conversation.demo.forms.*
import com.integration.bold.boldchat.core.FormData
import com.integration.core.FormResults
import com.integration.core.StateEvent
import com.integration.core.annotations.FormType
import com.nanorep.convesationui.bold.ui.FormListener
import com.nanorep.convesationui.structure.FriendlyDatestampFormatFactory
import com.nanorep.convesationui.structure.controller.*
import com.nanorep.convesationui.structure.handlers.AccountInfoProvider
import com.nanorep.nanoengine.Account
import com.nanorep.nanoengine.AccountInfo
import com.nanorep.nanoengine.model.configuration.ConversationSettings
import com.nanorep.nanoengine.model.configuration.TimestampStyle
import com.nanorep.sdkcore.model.StatementScope
import com.nanorep.sdkcore.model.isLive
import com.nanorep.sdkcore.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast



interface ChatFlowHandler : ChatEventListener {
    fun waitingVisibility(visible: Boolean)
    fun onAccountReady(account: Account)
}


class ChatActivity : AppCompatActivity(), ChatFlowHandler {

    private var chatController: ChatController? = null
    private val notificationsReceiver = NotificationsReceiver()

    private var historyProvider: MyHistoryProvider? = null
    private var accountProvider: AccountHandler? = null
    private var formProvider: FormProvider? = null

    private var menu: Menu? = null


    init {
        formProvider = ChatFormProvider(
            fragmentManager = {
                supportFragmentManager
            },
            onFormPresent = { formType ->
                if (formType == FormType.PostChatForm) {
                    hideEndBoldMenu()
                }
            })

        accountProvider = AccountHandler()

        historyProvider = MyHistoryProvider()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.content_main, DemoMain.newInstance(), DemoMain_TAG)
        fragmentTransaction.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        this.menu = menu;
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> return true

            R.id.bold_end -> {
                chatController?.endChat(false)
                return true
            }
        }

        return false
    }

    private fun getBoldMenu(): MenuItem? {
        return menu?.findItem(R.id.bold_end)
    }


    override fun onStop() {
        if (isFinishing) {
            clearAllResources();
        }

        super.onStop();
    }

    override fun onBackPressed() {
        waitingVisibility(false)
        super.onBackPressed()
    }

    private fun clearAllResources() {
        try {
            chatController?.run {
                unsubscribeNotifications(notificationsReceiver)
                terminateChat()
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    override fun onDestroy() {
        clearAllResources()
        super.onDestroy()
    }

    override fun waitingVisibility(visible: Boolean) {
        progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }


    override fun onAccountReady(account: Account) {
        waitingVisibility(true)

        accountProvider?.updateAccountInfo(account)

        historyProvider?.accountId = account.getApiKey()

        if (isFinishing) return

        /*TBC: once will be supported by the SDK
        this.chatController?.run {
            activateChat(account)
        }*/
        chatController = createChat(account)?.apply {
            subscribeNotifications(
                notificationsReceiver, ChatNotifications.PostChatFormSubmissionResults,
                ChatNotifications.UnavailabilityFormSubmissionResults
            )
        }

    }

    override fun onAccountUpdate(accountInfo: AccountInfo) {
        accountProvider?.updateAccountInfo(accountInfo)
    }


    private fun createChat(account: Account): ChatController? {
        if (isFinishing) return null

        val settings = ConversationSettings()
            .speechEnable(true)
            .enableMultiRequestsOnLiveAgent(true)
            .timestampConfig(
                true, TimestampStyle(
                    "dd.MM hh:mm:ss",
                    10, Color.parseColor("#33aa33"), null
                )
            )
            .datestamp(true, FriendlyDatestampFormatFactory(this))

        return ChatController.Builder(this).apply {
            conversationSettings(settings)
            chatEventListener(this@ChatActivity)

            historyProvider?.run { historyProvider(this) }
            accountProvider?.run { accountProvider(this) }
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

                    waitingVisibility(false)
                }
            })
    }

    override fun onChatStateChanged(stateEvent: StateEvent) {

        Log.d(DemoMain_TAG, "onChatStateChanged: state " + stateEvent.state + ", scope = " + stateEvent.scope)

        when (stateEvent.state) {

            StateEvent.Preparing -> {
            }

            StateEvent.Started -> {
                // display end chat button
                if (stateEvent.scope == StatementScope.BoldScope) {
                    getBoldMenu()?.isVisible = true
                }
            }

            StateEvent.ChatWindowLoaded -> {
            }

            StateEvent.Unavailable -> {
                toast("Chat unavailable due to " + stateEvent.data!!)
                // hide end chat button
                if (stateEvent.scope.isLive()) {
                    hideEndBoldMenu()
                }
            }

            StateEvent.Ending, StateEvent.Ended -> {
                if (stateEvent.scope.isLive()) {
                    hideEndBoldMenu()
                }
            }

            StateEvent.ChatWindowDetached -> {
                supportFragmentManager?.popBackStack(
                    CONVERSATION_FRAGMENT_TAG,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            }
        }
    }

    private fun hideEndBoldMenu() {
        getBoldMenu()?.isVisible = false
    }

    private fun openConversationFragment(fragment: Fragment) {
        if (isFinishing || supportFragmentManager.isStateSaved ||
            supportFragmentManager.findFragmentByTag(CONVERSATION_FRAGMENT_TAG) != null
        ) {
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_main, fragment, CONVERSATION_FRAGMENT_TAG)
            .addToBackStack(CONVERSATION_FRAGMENT_TAG)
            .commit()
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
        toast("Connection failure.\nPlease check your connection.")
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
            supportFragmentManager.fragments.lastOrNull()?.view?.run {
                snack(
                    s + error.reason + ": " + error.description,
                    4000, -1, Gravity.CENTER, intArrayOf(), i
                )
            }

        } catch (ignored: Exception) {
            toast(s + error.reason + ": " + error.description)
        }
    }

    override fun onUrlLinkSelected(url: String) {
        // sample code for handling given link
        try {
            val intent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(CONVERSATION_FRAGMENT_TAG, ">> Failed to activate link on default app: " + e.message)
           // toast(this, "activating: $url", Toast.LENGTH_SHORT)
        }
    }

    //-> previous listener method signature @Override onPhoneNumberNavigation(@NonNull String phoneNumber) {
    override fun onPhoneNumberSelected(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phoneNumber")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(CONVERSATION_FRAGMENT_TAG, ">> Failed to activate phone dialer default app: " + e.message)
        }
    }

    companion object {
        const val My_TAG = "ChatActivity"
        const val CONVERSATION_FRAGMENT_TAG = "conversation_fragment"
    }

}


////////////////////////////////////////

class AccountHandler : AccountInfoProvider {

    private val accounts: MutableMap<String, AccountInfo> = mutableMapOf()

    override fun provide(info: String, callback: Completion<AccountInfo?>) {
        callback.onComplete(accounts[info])
    }

    override fun updateAccountInfo(account: AccountInfo) {
        accounts[account.getApiKey()]?.run {
            updateInfo(account.getInfo())
        } ?: kotlin.run {
            accounts[account.getApiKey()] = account
        }
    }
}

////////////////////////////////////////

internal class ChatFormProvider(
    val fragmentManager: () -> FragmentManager?,
    private val onFormPresent: ((formType: String) -> Unit)? = null
) : FormProvider {

    override fun presentForm(formData: FormData, callback: FormListener) {
        val fragmentManager = fragmentManager()

        fragmentManager?.run {
            // Demo implementation that presents present a dummy form :
            val fragment = DemoForm.create(formData, callback)
            beginTransaction().add(R.id.content_main, fragment, DEMO_FORM_TAG)
                .addToBackStack(DEMO_FORM_TAG)
                .commit()

            Log.v("formData", "form type: " + formData.formType + " form data:" + formData.logFormBrandings())

            onFormPresent?.invoke(formData.formType)

        } ?: kotlin.run {
            Log.w("DemoMain", "presentForm: form can't be presented, activity reference is lost")
            callback.onCancel(formData.formType)
            return
        }

    }

}

////////////////////////////////////////

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