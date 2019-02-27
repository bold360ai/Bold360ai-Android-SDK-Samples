package com.conversation.demo.forms

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nanorep.convesationui.structure.history.FetchDirection
import com.nanorep.convesationui.structure.history.HistoryListener
import com.nanorep.convesationui.structure.history.HistoryProvider
import com.nanorep.nanoengine.chatelement.ChatElement
import com.nanorep.nanoengine.chatelement.StorableChatElement
import com.nanorep.sdkcore.model.StatementScope
import com.nanorep.sdkcore.model.StatementStatus
import com.nanorep.sdkcore.model.StatusPending
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

const val HistoryPageSize = 8;
const val DEMO_FORM_TAG = "demo_form_fragment";


internal class MyHistoryProvider : HistoryProvider {

    internal var accountId: String? = null
    private val historySync = Any() // in use to block multi access to history from different actions.
    private var handler: Handler = Handler(Looper.getMainLooper())

    private val chatHistory = ConcurrentHashMap<String, List<HistoryElement>>()
    private var hasHistory = false


    override fun fetch(from: Int, @FetchDirection direction: Int, listener: HistoryListener?) {

        Thread(Runnable {
            val history: List<StorableChatElement>

            synchronized(historySync) {
                history = Collections.unmodifiableList<HistoryElement>(getHistoryForAccount(accountId, from, direction))
            }

            if (history.isNotEmpty()) {
                try {
                    Thread.sleep(800) // simulate async history fetching
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
            if (handler.looper != null) {
                handler.post {
                    Log.d("History", "passing history list to listener, from = " + from + ", size = " + history.size)
                    hasHistory = history.isNotEmpty()
                    listener!!.onReady(from, direction, history)
                }
            }
        }).start()
    }

    override fun store(item: StorableChatElement) {
       // if(item == null || item.getStatus() != StatusOk) return;

        synchronized(historySync) {
             accountId?.run {
                 val convHistory = getAccountHistory(this)
                 convHistory.add(HistoryElement(item))
             }
        }
    }

    override fun remove(timestampId: Long) {
        synchronized(historySync) {
            accountId?.run {
                val convHistory = getAccountHistory(this)

                val iterator = convHistory.listIterator()
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    if (item.getTimestamp() == timestampId) {
                        iterator.remove()
                        break
                    }
                }
            }
        }
    }

    override fun update(timestampId: Long, item: StorableChatElement) {
        synchronized(historySync) {
            accountId?.run {
                val convHistory = getAccountHistory(this)
                for (i in convHistory.indices.reversed()) {
                    if (convHistory[i].getTimestamp() == timestampId) {
                        convHistory[i] = HistoryElement(item)
                        break
                    }
                }
            }
        }
    }

    private fun getAccountHistory(accountId: String): ArrayList<HistoryElement> {
        val convHistory: ArrayList<HistoryElement>
        if (chatHistory.containsKey(accountId)) {
            convHistory = (chatHistory[accountId] as? ArrayList<HistoryElement>)?: arrayListOf()
        } else {
            convHistory = ArrayList<HistoryElement>()
            chatHistory.put(accountId,convHistory)
        }
        return convHistory
    }

    private fun getHistoryForAccount(account: String?, fromIdx: Int, direction: Int): List<HistoryElement> {
        var fromIdx = fromIdx

        val accountChatHistory = chatHistory[account] ?: return ArrayList<HistoryElement>()

        val fetchOlder = direction == HistoryProvider.Older

        // to prevent Concurrent exception
        val accountHistory = CopyOnWriteArrayList<HistoryElement>(accountChatHistory)

        val historySize = accountHistory.size

        if (fromIdx == -1) {
            fromIdx = if (fetchOlder) historySize - 1 else 0
        } else if (fetchOlder) {
            fromIdx = historySize - fromIdx
        }

        val toIndex = if (fetchOlder)
            Math.max(0, fromIdx - HistoryPageSize)
        else
            Math.min(fromIdx + HistoryPageSize, historySize - 1)

        return try {
            Log.d("History", "fetching history items ($historySize) from $toIndex to $fromIdx")

            accountHistory.subList(toIndex, fromIdx)

        } catch (ex: Exception) {
            ArrayList<HistoryElement>()
        }

    }

}

/**
 * [StorableChatElement] implementing class
 * sample class for app usage
 */
internal class HistoryElement(var key:ByteArray = byteArrayOf()) : StorableChatElement {

    private var timestamp: Long = 0

    override var scope: StatementScope = StatementScope.UnknownScope

    @ChatElement.Companion.ChatElementType
    private var type: Int = 0

    @StatementStatus
    private var status = StatusPending

    override val isStorageReady = true

    constructor(type: Int, timestamp: Long) : this() {
        this.type = type
        this.timestamp = timestamp
    }

    constructor(storable: StorableChatElement) :this(storable.getStorageKey()) {
        type = storable.getType()
        timestamp = storable.getTimestamp()
        status = storable.getStatus()
        scope = storable.scope
    }

    override fun getStorageKey(): ByteArray {
        return key
    }

    override fun getStorableContent(): String {
        return String(key)
    }

    override fun getType(): Int {
        return type
    }

    override fun getTimestamp(): Long {
        return timestamp
    }

    override fun getStatus(): Int {
        return status
    }
}

