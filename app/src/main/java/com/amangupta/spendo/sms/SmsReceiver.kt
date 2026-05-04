package com.amangupta.spendo.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

import com.amangupta.spendo.data.AppDatabase
import com.amangupta.spendo.data.CategorizationEngine
import com.amangupta.spendo.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val body = message.messageBody
                val sender = message.displayOriginatingAddress
                Log.d("SmsReceiver", "From: $sender, Body: $body")
                
                val parsed = SmsParser.parse(body)
                if (parsed != null) {
                    Log.d("SmsReceiver", "Parsed: $parsed")
                    saveTransaction(context, parsed, body)
                }
            }
        }
    }

    private fun saveTransaction(context: Context, parsed: SmsParser.ParsedTransaction, rawBody: String) {
        val db = AppDatabase.getDatabase(context)
        scope.launch {
            val result = CategorizationEngine.categorize(parsed.merchant, db.categoryRuleDao())
            val transaction = Transaction(
                amount = parsed.amount,
                merchant = result.name,
                category = result.category,
                timestamp = System.currentTimeMillis(),
                rawMessage = rawBody
            )
            db.transactionDao().insert(transaction)
            Log.d("SmsReceiver", "Transaction saved: $transaction")
        }
    }
}
