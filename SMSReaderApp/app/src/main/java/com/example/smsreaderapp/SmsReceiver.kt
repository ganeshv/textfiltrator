package com.example.smsreaderapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
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
                Toast.makeText(context, messageBody, Toast.LENGTH_LONG).show()
                LogManager.log("Received SMS from ${it[0].displayOriginatingAddress}", context)
                // format a message string including the sender and the message body
                val message = "SMS from ${it[0].displayOriginatingAddress}: $messageBody"
                CoroutineScope(Dispatchers.Main).launch {
                    if (context != null) {
                        sendEmailSSL(
                            context = context, // Pass the context
                            content = message
                        )
                        LogManager.log("Emailed SMS from ${it[0].displayOriginatingAddress}", context)
                    }
                }

            }
        }

    }
}

suspend fun sendEmailSSL(context: Context, content: String) {
    withContext(Dispatchers.IO) {
        // Fetch credentials from EncryptedSharedPreferences
        val host = EncryptedPreferencesUtil.getString(context, "SMTP_SERVER", "")
        val port = EncryptedPreferencesUtil.getString(context, "PORT", "")
        val user = EncryptedPreferencesUtil.getString(context, "SENDER_EMAIL", "")
        val password = EncryptedPreferencesUtil.getString(context, "SENDER_PASSWORD", "")
        val to = EncryptedPreferencesUtil.getString(context, "RECIPIENT_EMAILS", "")
        val subject = EncryptedPreferencesUtil.getString(context, "SUBJECT_LINE", "")

        val properties = System.getProperties()
        properties["mail.smtp.host"] = host
        properties["mail.smtp.socketFactory.port"] = port
        properties["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.port"] = port

        val session = Session.getDefaultInstance(properties)
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(user))
        message.addRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(to))
        message.subject = subject
        message.setText(content)

        val transport = session.getTransport("smtp")
        transport.connect(host, user, password)
        transport.sendMessage(message, message.allRecipients)
        transport.close()
    }
}
