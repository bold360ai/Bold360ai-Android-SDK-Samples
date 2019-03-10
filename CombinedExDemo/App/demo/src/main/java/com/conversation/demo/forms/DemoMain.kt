package com.conversation.demo.forms

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.conversation.demo.ChatFlowHandler
import com.conversation.demo.ChatType
import com.conversation.demo.R
import kotlinx.android.synthetic.main.fragment_main.*

internal const val DemoMain_TAG = "DemoMain"


class DemoMain : Fragment() {

    private var chatFlowHandler: ChatFlowHandler? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bold_chat.setOnClickListener { onChatClick(it) }
        bot_chat.setOnClickListener { onChatClick(it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        chatFlowHandler = (context as? ChatFlowHandler) ?: kotlin.run {
            Log.e(TAG, "$context must implement ChatFlowHandler")
            null
        }
    }

    override fun onDetach() {
        super.onDetach()
        chatFlowHandler = null
    }

    private fun onChatClick(view: View) {
        val chatType = if (view.id == R.id.bold_chat) ChatType.LiveChat else ChatType.BotChat

        val accountForm = AccountForm.create(chatType)
        fragmentManager?.beginTransaction()?.apply {
            replace(R.id.content_main, accountForm)
            addToBackStack(null)
            commit()
        }
    }

    companion object {
        const val TAG = "DemoMain"
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
