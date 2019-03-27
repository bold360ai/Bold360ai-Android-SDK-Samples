package com.conversation.demo

import android.content.Context
import com.nanorep.nanoengine.model.NRAccount

class PrevDataHandler {

    companion object {
        const val BotSharedName = "ChatDataPref.bot"

        const val ApiKey_key = "apiKey"
        const val Account_key = "accountKey"
        const val Kb_key = "kbKey"
        const val Server_key = "serverKey"
        const val Access_key = "accessKey"
        const val Context_key = "contextKey"
    }

    private fun saveChatData(context: Context, data: Map<String, Any>) {
        saveData(context, BotSharedName, data)
    }

    fun getFormData(context: Context): Map<String, Any> {
        return getBotData(context)
    }

    private fun getBotData(context: Context): Map<String, Any> {
        val shared = context.getSharedPreferences(BotSharedName, 0)
        return mapOf(
                Account_key to shared.getString(Account_key, ""),
                Kb_key to shared.getString(Kb_key, ""),
                Server_key to shared.getString(Server_key, ""),
                ApiKey_key to shared.getString(ApiKey_key, ""),
                Context_key to shared.getString(Context_key, "")
        )
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

    fun saveChatData(context: Context, data: NRAccount) {
        saveChatData(
                context, mapOf<String, Any>(
                Account_key to (data.account ?: ""),
                Kb_key to (data.knowledgeBase ?: ""),
                Server_key to (data.server ?: ""),
                ApiKey_key to data.apiKey,
                Context_key to data.context
                /*(data.contexts?.takeIf { it.isNotEmpty() }?.map { entry ->
                    "key= ${entry.key} value= ${entry.value}"
                }?.mapIndexed { index, str -> "$index:$str" }?.toHashSet() ?: setOf<String>())*/
        ))
    }



}
