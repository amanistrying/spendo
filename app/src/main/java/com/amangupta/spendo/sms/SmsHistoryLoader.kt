package com.amangupta.spendo.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.amangupta.spendo.data.AppDatabase
import com.amangupta.spendo.data.CategorizationEngine
import com.amangupta.spendo.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsHistoryLoader {
    suspend fun loadHistory(context: Context, startTime: Long, endTime: Long) {
        val db = AppDatabase.getDatabase(context)
        val transactionDao = db.transactionDao()
        val categoryRuleDao = db.categoryRuleDao()

        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.ADDRESS
            )
            val selection = "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?"
            val selectionArgs = arrayOf(startTime.toString(), endTime.toString())

            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val body = cursor.getString(bodyIndex)
                    val timestamp = cursor.getLong(dateIndex)

                    val parsed = SmsParser.parse(body) ?: continue
                    
                    if (transactionDao.countDuplicates(parsed.merchantVpa, parsed.amount, timestamp) > 0) {
                        Log.d("SmsHistoryLoader", "Duplicate skipped: ${parsed.merchantVpa}")
                        continue
                    }

                    val result = CategorizationEngine.categorize(
                        parsed.merchantVpa, parsed.merchantName, categoryRuleDao
                    )
                    transactionDao.insert(
                        Transaction(
                            amount = parsed.amount,
                            merchantVpa = parsed.merchantVpa,
                            merchant = result.name,
                            category = result.category,
                            timestamp = timestamp,
                            rawMessage = body,
                            bankRef = parsed.bankRef
                        )
                    )
                    Log.d("SmsHistoryLoader", "Restored: ${parsed.amount} at $timestamp")
                }
            }
        }
    }
}
