package com.example.smsreaderapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("SmsReceiver", "Received SMS intent")
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.let {
            if (it.isNotEmpty()) {
                val messageBody = it[0].messageBody
                // For simplicity, let's show a Toast with the SMS content
                Log.d("SmsReceiver", messageBody)  // Log the SMS details
                Log.d("SmsReceiver", it[0].displayOriginatingAddress)
                LogManager.log("Received SMS from ${it[0].displayOriginatingAddress}", context)
                // format a message string including the sender and the message body
                val message = "SMS from: ${it[0].displayOriginatingAddress}\n$messageBody"
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        if (context != null) {
                            val fwdState = EncryptedPreferencesUtil.getBoolean(context, "FORWARDING_STATE", false)
                            if (!fwdState) {
                                return@launch
                            }
                            val matchWords = EncryptedPreferencesUtil.getString(context, "MATCH_WORDS", "") ?: ""
                            if (!matchWords.isBlank()) {
                                val regex = generateRegexPattern(matchWords)
                                if (!regex.containsMatchIn(message)) {
                                    LogManager.log("SMS from ${it[0].displayOriginatingAddress} did not have match words", context)
                                    return@launch
                                }
                            }
                            val smtpSettings = SMTPSettings.load(context)
                            sendEmailSSL(
                                content = message,
                                settings = smtpSettings
                            )
                            LogManager.log(
                                "Emailed SMS from ${it[0].displayOriginatingAddress}",
                                context
                            )
                        }
                    } catch (e: Exception) {
                        LogManager.log("Failed to email SMS: ${e.message}", context)
                    }
                }

            }
        }
    }
}

suspend fun sendEmailSSL(content: String, settings: SMTPSettings) {
    withContext(Dispatchers.IO) {
        // Fetch credentials from EncryptedSharedPreferences
        val (host, port, user, password, to, subject) = settings

        val properties = System.getProperties()
        properties["mail.smtp.host"] = host
        properties["mail.smtp.socketFactory.port"] = port
        properties["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.port"] = port

        val session = Session.getDefaultInstance(properties)
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(user))
        val recipientEmails = to.replace(" ", ", ") // Convert space-separated list to comma-separated
        message.addRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(recipientEmails))
        message.subject = subject
        message.setText(content)

        val transport = session.getTransport("smtp")
        transport.connect(host, user, password)
        transport.sendMessage(message, message.allRecipients)
        transport.close()
    }
}
