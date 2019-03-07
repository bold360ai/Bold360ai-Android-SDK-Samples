package com.conversation.demo.forms

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ScrollView
import com.conversation.demo.ChatType
import com.conversation.demo.R
import com.nanorep.convesationui.bold.model.BoldAccount
import com.nanorep.nanoengine.Account
import com.nanorep.nanoengine.BotAccount
import com.nanorep.sdkcore.utils.px
import kotlinx.android.synthetic.main.account_form.*
import kotlinx.android.synthetic.main.bot_context_view.view.*
import kotlin.math.max


/*interface AccountListener {
    fun onReady(account: Account)
}*/


class AccountForm : Fragment(), ContextAdapter {

    companion object {
        @JvmStatic
        fun create(@ChatType chatType: String, listener: AccountListener? = null): AccountForm {
            return AccountForm().apply {
                this.chatType = chatType
                this.accountListener = listener
            }
        }
    }

    private lateinit var chatType: String

    private var accountListener: AccountListener? = null
    private var account: Account? = null
    private val prevDataHandler = PrevDataHandler()

    private var contextHandler: ContextHandler? = null
    private var contextAdapter: ContextAdapter = this


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.account_form, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // scrolls to the focused view bottom
        form_root.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft,
                                              oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                val focused = activity?.currentFocus
                focused?.run { scroller.scrollTo(this.left, this.bottom + 10) }
            }
        }

        if (chatType == ChatType.LiveChat) {
            prepareLiveChatForm()

        } else {
            prepareBotChatForm()
        }

        start_chat.setOnClickListener {
            it.startAnimation(AlphaAnimation(1f, 0.8f).also { it.duration = 150 })

            account = createAccount()
            account?.run {
                it.isEnabled = false
                accountListener?.onReady(this)
            }
        }

        fillFields()

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }


    private fun prepareBotChatForm() {
        bot_fields.visibility = View.VISIBLE
        bot_context.visibility = View.VISIBLE
        api_key_layout.hint = getString(R.string.api_key_hint)

        bot_context.scroller = scroller

        add_context.setOnClickListener {
            try {
                val lastContext = contextHandler?.container?.getLast()
                if (lastContext == null || !lastContext.isEmpty()) {
                    if (lastContext == null) {
                        context_title.visibility = View.VISIBLE
                    }

                    contextHandler?.addContext()
                }
            } catch (ast: AssertionError) {
                Log.w("AccountForm", "got assertion error")
            }
        }

        contextHandler = ContextHandler(bot_context, contextAdapter).apply {
            onDelete = { view ->
                if(bot_context.childCount == 1){
                    context_title.visibility = View.GONE
                }
            }
        }
    }

    private fun prepareLiveChatForm() {
        bot_fields.visibility = View.GONE
        bot_context.visibility = View.GONE
        api_key_layout.hint = getString(R.string.access_key_hint)

        bot_context.scroller = null
        add_context.setOnClickListener(null)
    }

    override fun onStop() {
        super.onStop()

        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.takeIf { view != null }?.run { imm.hideSoftInputFromWindow(view!!.windowToken, 0) }
    }


    fun Pair<String, String>.isEmpty(): Boolean {
        return first.isBlank() || second.isBlank()
    }

    override fun createView(botContext: Pair<String, String>?,
                            onDelete: ((ContextViewHolder) -> Unit)?): View {
        return ContextViewLinear(context!!).apply {
            this.onDelete = onDelete
            botContext?.run {
                this@apply.setBotContext(botContext)
            }
        }
    }

    private fun createAccount(): Account? {
        return when (chatType) {
            ChatType.LiveChat -> {
                api_key_edit_text.text.toString().takeUnless { it.isEmpty() }?.let {
                    val account = BoldAccount(it)
                    context?.run {
                        prevDataHandler.saveChatData(this, account)
                    }
                    account
                }
            }

            ChatType.BotChat -> {
                getBotAccount()
            }

            else -> null
        }
    }

    /**
     * retrieve last saved form data from shared preferences and fill the relevant fields
     */
    private fun fillFields() {
        if (context == null) return

        val data = prevDataHandler.getFormData(context!!, chatType)

        account_name_edit_text.setText(data[PrevDataHandler.Account_key] as? String ?: "")

        knowledgebase_edit_text.setText(data[PrevDataHandler.Kb_key] as? String ?: "")

        server_edit_text.setText(data[PrevDataHandler.Server_key] as? String ?: "")

        api_key_edit_text.setText(
            (data[PrevDataHandler.ApiKey_key] as? String) ?: (data[PrevDataHandler.Access_key] as? String) ?: ""
        )

        bot_context.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int,
                                        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                scroller.scrollTo(0, 0)
                bot_context.removeOnLayoutChangeListener(this)
            }

        })

        (data[PrevDataHandler.Context_key] as? Set<String>)?.run {
            val contextPairs = this.map { contextString ->
                val seq = contextString.split(":", "key= ", " value= ")
                seq[0] to Pair(seq[2], seq.last())
            }
                .sortedBy { k -> k.first }
                .map { t -> t.second }

            context_title.visibility = if (contextPairs.isEmpty()) View.GONE else View.VISIBLE

            contextHandler?.setContexts(contextPairs)
        }
    }

    private fun getBotAccount(): BotAccount? {
        try {
            val accountName = account_name_edit_text.text.toString()
            val kb = knowledgebase_edit_text.text.toString()
            val apiKey = api_key_edit_text.text.toString()
            val server = server_edit_text.text.toString()

            if (accountName.isBlank() || kb.isBlank() || server.isBlank()) {
                throw AssertionError("Missing bot fields")
            }

            val contexts = contextHandler?.getContext()
            val botAccount = BotAccount(apiKey, accountName, kb, server, contexts)
            context?.run {
                prevDataHandler.saveChatData(this, botAccount)
            }

            return botAccount
        } catch (ast: AssertionError) {
            return null
        }
     }

    override fun onResume() {
        super.onResume()
        start_chat.isEnabled = true
    }
}

///////////////////////////////////////////////////


class ContextHandler(var container: ContextContainer, val contextsAdapter: ContextAdapter) {

    var onDelete: ((ContextViewHolder) -> Unit)? = {
        container.removeContext(it)
    }
    set(value) {
        field = {
            container.removeContext(it)
            value?.invoke(it)
        }
    }



    fun addContext(botContext: Pair<String, String>? = null) {
        val view = contextsAdapter.createView(botContext, onDelete)
        container.addContextView(view)
    }

    fun setContexts(contextPairs: List<Pair<String, String>>) {
        container.clear()
        contextPairs.forEach { pair ->
            addContext(pair)
        }
    }

    @Throws(AssertionError::class)
    fun getContext(): Map<String, String>? {
        return container.getContextList()
    }
}

///////////////////////////////////////////////////

interface ContextContainer {
    fun addContextView(view: View)
    fun clear()
    @Throws(AssertionError::class)
    fun getContextList(): Map<String, String>?

    fun getLast(): Pair<String, String>?
    fun removeContext(contextView: ContextViewHolder)
}

///////////////////////////////////////////////////

interface ContextAdapter {
    fun createView(botContext: Pair<String, String>? = null,
                   onDelete: ((ContextViewHolder) -> Unit)? = null): View
}

///////////////////////////////////////////////////

interface ContextViewHolder {
    @Throws(AssertionError::class)
    fun getBotContext(): Pair<String, String>
    fun setBotContext(context: Pair<String, String>)
    fun getView(): View
}



///////////////////////////////////////////////////

class LinearContext @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ContextContainer {

    var scroller: ScrollView? = null

    override fun addContextView(view: View) {
        addView(view, max(childCount - 1, 0))
    }

    override fun removeContext(contextView: ContextViewHolder) {
        removeView(contextView.getView())
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (changed) {
            scroller?.scrollTo(l, b)
        }
    }

    override fun clear() {
        removeViews(0, childCount - 1)
    }

    override fun getContextList(): Map<String, String>? {
        return (0 until childCount - 1).map { idx ->
            val entry = (getChildAt(idx) as? ContextViewHolder)?.getBotContext() ?: Pair("", "")
            entry.first to entry.second
        }.toMap()
    }

    override fun getLast(): Pair<String, String>? {
        return (takeIf { childCount > 1 }?.getChildAt(childCount - 2) as? ContextViewHolder)?.getBotContext()
    }

}

///////////////////////////////////////////////////

class ContextViewLinear @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ContextViewHolder {

    init {

        if (layoutParams == null) {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = 5.px
                marginStart = margin
                marginEnd = margin
                weightSum = 1f
                setPadding(margin, margin, margin, margin)
            }
        }
        orientation = LinearLayout.HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.bot_context_view, this, true)

        delete_context.setOnClickListener {view ->
            /*(it.parent?.parent as? ViewGroup)?.let { root ->
                root.removeView(it.parent as View)
                root.requestLayout()
            }*/
            onDelete?.invoke(this)
        }
    }

    var onDelete: ((ContextViewHolder) -> Unit)? = null

    @Throws(AssertionError::class)
    override fun getBotContext(): Pair<String, String> {
        val key = context_key.text.toString()
        val value = context_value.text.toString()
        if (key.isBlank() || value.isBlank()) {
            throw AssertionError()
        }
        return key to value
    }

    override fun setBotContext(context: Pair<String, String>) {
        context_key.setText(context.first)
        context_value.setText(context.second)
    }

    override fun getView(): View {
        return this
    }
}


///////////////////////////////////////////

/**
 * Handles the shared preference interaction to save and retrieve last applied data to forms.
 *
 */
class PrevDataHandler {

    companion object {
        const val BotSharedName = "ChatDataPref.bot"
        const val BoldSharedName = "ChatDataPref.bold"

        const val ApiKey_key = "apiKey"
        const val Account_key = "accountKey"
        const val Kb_key = "kbKey"
        const val Server_key = "serverKey"
        const val Access_key = "accessKey"
        const val Context_key = "contextKey"
    }

    fun saveChatData(context: Context, data: Map<String, Any>, chatType: String) {
        when (chatType) {
            ChatType.BotChat -> saveData(context, BotSharedName, data)
            ChatType.LiveChat -> saveData(context, BoldSharedName, data)
        }
    }

    fun getFormData(context: Context, chatType: String): Map<String, Any> {
        return when (chatType) {
            ChatType.BotChat -> getBotData(context)
            ChatType.LiveChat -> getBoldData(context)
            else -> mapOf()
        }
    }


    private fun getBotData(context: Context): Map<String, Any> {
        val shared = context.getSharedPreferences(BotSharedName, 0)
        return mapOf(
            Account_key to shared.getString(Account_key, ""),
            Kb_key to shared.getString(Kb_key, ""),
            Server_key to shared.getString(Server_key, ""),
            ApiKey_key to shared.getString(ApiKey_key, ""),
            Context_key to shared.getStringSet(Context_key, mutableSetOf())
        )
    }

    private fun getBoldData(context: Context): Map<String, String> {
        val shared = context.getSharedPreferences(BoldSharedName, 0)
        return mapOf(Access_key to shared.getString(Access_key, ""))
    }

    private fun saveData(context: Context, sharedName: String, data: Map<String, Any>) {
        val shared = context.getSharedPreferences(sharedName, 0)
        val editor = shared.edit()
        data.forEach { detail ->
            (detail.value as? String)?.run { editor.putString(detail.key, this) }
            (detail.value as? Set<String>)?.run { editor.putStringSet(detail.key, this) }
        }
        editor.apply() //commit()
    }

    fun saveChatData(context: Context, data: BotAccount) {
        saveChatData(
            context, mapOf<String, Any>(
                Account_key to (data.account ?: ""),
                Kb_key to (data.knowledgeBase ?: ""),
                Server_key to (data.domain ?: ""),
                ApiKey_key to data.apiKey,
                Context_key to (data.contexts?.takeIf { it.isNotEmpty() }?.map { entry ->
                    "key= ${entry.key} value= ${entry.value}"
                }?.mapIndexed { index, str -> "$index:$str" }?.toHashSet() ?: setOf<String>())

            ), ChatType.BotChat
        )
    }

    fun saveChatData(context: Context, data: BoldAccount) {
        saveChatData(context, mapOf(Access_key to data.apiKey), ChatType.LiveChat)
    }

}
