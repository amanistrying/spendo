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
    companion object { private const val TAG = "SmsReceiver" }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullSms = messages.joinToString("") { it.messageBody }

        if (!SmsParser.isUpiDebit(fullSms)) return

        val parsed = SmsParser.parse(fullSms) ?: run {
            Log.w(TAG, "Parse failed for UPI SMS")
            return
        }

        // goAsync keeps the receiver alive until finish() is called
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val ts = System.currentTimeMillis()

                // Duplicate check
                if (db.transactionDao().countDuplicates(parsed.merchantVpa, parsed.amount, ts) > 0) {
                    Log.d(TAG, "Duplicate skipped: ${parsed.merchantVpa}")
                    return@launch
                }

                val result = CategorizationEngine.categorize(
                    parsed.merchantVpa, parsed.merchantName, db.categoryRuleDao()
                )
                db.transactionDao().insert(
                    Transaction(
                        amount = parsed.amount,
                        merchantVpa = parsed.merchantVpa,
                        merchant = result.name,
                        category = result.category,
                        timestamp = ts,
                        rawMessage = fullSms,
                        bankRef = parsed.bankRef
                    )
                )
                Log.d(TAG, "Saved ₹${parsed.amount} → ${result.name} [${result.category}]")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transaction", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
