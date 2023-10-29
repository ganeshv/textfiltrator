package com.textfiltrator.textfiltrator

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
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.let {
            if (it.isNotEmpty()) {
                val originatingAddress = it[0].displayOriginatingAddress
                val messageBody = it.joinToString(separator = "") {message -> message.messageBody}
                //Log.d("SmsReceiver: message from:", originatingAddress)
                //Log.d("SmsReceiver: message body:", messageBody)

                //LogManager.log("Received SMS from $originatingAddress", context)
                // format a message string including the sender and the message body
                val message = "SMS from: $originatingAddress\n$messageBody"
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        if (context != null) {
                            val fwdState = EncryptedPreferencesUtil.getBoolean(context, "FORWARDING_STATE", false)
                            if (!fwdState) {
                                return@launch
                            }
                            val matchWords = EncryptedPreferencesUtil.getString(context, "MATCH_WORDS", "") ?: ""
                            if (matchWords.isNotBlank()) {
                                val regex = generateRegexPattern(matchWords)
                                if (!regex.containsMatchIn(messageBody)) {
                                    LogManager.log("Received SMS from $originatingAddress, keywords not found", context)
                                    return@launch
                                }
                            }
                            val smtpSettings = SMTPSettings.load(context)
                            sendEmailSSL(
                                content = message,
                                settings = smtpSettings
                            )
                            LogManager.log(
                                "Emailed SMS from $originatingAddress",
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
