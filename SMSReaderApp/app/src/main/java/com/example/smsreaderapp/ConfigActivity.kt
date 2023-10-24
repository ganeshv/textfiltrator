package com.example.smsreaderapp

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SMTPSettings (
    val smtpServer: String,
    val port: String,
    val senderEmail: String,
    val senderPassword: String,
    val recipientEmails: String,
    val subjectLine: String
) {
    companion object {
        fun load(context: Context) : SMTPSettings {
            val smtpServer = EncryptedPreferencesUtil.getString(context, "SMTP_SERVER") ?: ""
            val port = EncryptedPreferencesUtil.getString(context, "PORT") ?: ""
            val senderEmail = EncryptedPreferencesUtil.getString(context, "SENDER_EMAIL") ?: ""
            val senderPassword = EncryptedPreferencesUtil.getString(context, "SENDER_PASSWORD") ?: ""
            val recipientEmails = EncryptedPreferencesUtil.getString(context, "RECIPIENT_EMAILS") ?: ""
            val subjectLine = EncryptedPreferencesUtil.getString(context, "SUBJECT_LINE") ?: ""
            return SMTPSettings(smtpServer, port, senderEmail, senderPassword, recipientEmails, subjectLine)
        }
    }
    fun store(context: Context) {
        EncryptedPreferencesUtil.putString(context, "SMTP_SERVER", smtpServer)
        EncryptedPreferencesUtil.putString(context, "PORT", port)
        EncryptedPreferencesUtil.putString(context, "SENDER_EMAIL", senderEmail)
        EncryptedPreferencesUtil.putString(context, "SENDER_PASSWORD", senderPassword)
        EncryptedPreferencesUtil.putString(context, "RECIPIENT_EMAILS", recipientEmails)
        EncryptedPreferencesUtil.putString(context, "SUBJECT_LINE", subjectLine)
    }
}

class ConfigActivity : Activity() {

    // UI Components
    private lateinit var smtpServerEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var senderEmailEditText: EditText
    private lateinit var senderPasswordEditText: EditText
    private lateinit var recipientEmailsEditText: EditText
    private lateinit var subjectLineEditText: EditText
    private lateinit var editButton: Button
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button
    private lateinit var configStatusTextView: TextView
    private lateinit var btnSendTestMail: Button
    private lateinit var checkBoxConfirmReceipt: CheckBox

    private var isEditing : Boolean = false
    private lateinit var smtpSettings : SMTPSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        // Initialize UI Components
        smtpServerEditText = findViewById(R.id.smtpServerEditText)
        portEditText = findViewById(R.id.portEditText)
        senderEmailEditText = findViewById(R.id.senderEmailEditText)
        senderPasswordEditText = findViewById(R.id.senderPasswordEditText)
        recipientEmailsEditText = findViewById(R.id.recipientEmailsEditText)
        subjectLineEditText = findViewById(R.id.subjectLineEditText)

        editButton = findViewById(R.id.editButton)
        saveButton = findViewById(R.id.saveButton)
        resetButton = findViewById(R.id.resetButton)
        configStatusTextView = findViewById(R.id.configStatusTextView)
        btnSendTestMail = findViewById(R.id.btnSendTestMail)
        checkBoxConfirmReceipt = findViewById(R.id.checkBoxConfirmReceipt)

        // Load previously saved settings
        smtpSettings = loadSettings()

        // Make all the fields uneditable until the Edit button is pressed
        disableEdit()

        // Set up listeners
        editButton.setOnClickListener {
            // toggle edit mode
            if (isEditing) {
                disableEdit()
            } else {
                enableEdit()
            }
        }

        saveButton.setOnClickListener {
            val settings: SMTPSettings = validateSettings() ?: return@setOnClickListener
            val confirm = checkBoxConfirmReceipt.isChecked
            if (!confirm) {
                configStatusTextView.text = "Please send test mail and confirm receipt"
                return@setOnClickListener
            }
            // Save to encrypted shared preferences
            saveSettings(settings, true)
            configStatusTextView.text = "Configuration saved successfully!"
            LogManager.log("Configuration saved successfully!")
            // Make all the fields uneditable until the Edit button is pressed
            disableEdit()
        }

        resetButton.setOnClickListener {
            // Wipe all preferences
            saveSettings(SMTPSettings("", "", "", "", "", ""), false)
            loadSettings()
            LogManager.log("Configuration reset successfully!")
        }

        btnSendTestMail.setOnClickListener {
            // Call your method to send a test mail.
            sendTestMail()
        }
    }

    private fun validateSettings() : SMTPSettings? {
        val smtpServer = smtpServerEditText.text.toString().trim()
        val portString = portEditText.text.toString().trim()
        val senderEmail = senderEmailEditText.text.toString().trim()
        val senderPassword = senderPasswordEditText.text.toString().trim()
        val recipientEmails = recipientEmailsEditText.text.toString().trim().split(" ")
        val subjectLine = subjectLineEditText.text.toString().trim()
        val errlist = mutableListOf<String>()

        // Validations
        if (smtpServer.isEmpty()) {
            errlist.add("SMTP server cannot be empty!")
        }
        val port = portString.toIntOrNull()
        if (port == null || port <= 0 || port > 65535) {
            errlist.add("Invalid port number!")
        }
        if (senderEmail.isEmpty() || !isValidEmail(senderEmail)) {
            errlist.add("Invalid sender email!")
        }
        if (senderPassword.isBlank()) {
            errlist.add("Sender password cannot be empty!")
        }
        if (recipientEmails.isEmpty() || !recipientEmails.all { isValidEmail(it) }) {
            errlist.add("Invalid recipient email!")
        }
        if (subjectLine.isBlank()) {
            errlist.add("Subject line cannot be empty!")
        }
        return if (errlist.isNotEmpty()) {
            configStatusTextView.text = errlist.joinToString("\n")
            null
        } else {
            configStatusTextView.text = ""
            SMTPSettings(smtpServer, portString, senderEmail, senderPassword, recipientEmails.joinToString(" "), subjectLine)
        }
    }

    private fun loadSettings() : SMTPSettings {
        val s = SMTPSettings.load(this)
        val confirmReceipt = EncryptedPreferencesUtil.getBoolean(this, "CONFIRM_RECEIPT", false)

        // Populate the fields with the loaded settings
        smtpServerEditText.setText(s.smtpServer)
        portEditText.setText(s.port)
        senderEmailEditText.setText(s.senderEmail)
        senderPasswordEditText.setText(s.senderPassword)
        recipientEmailsEditText.setText(s.recipientEmails)
        subjectLineEditText.setText(s.subjectLine)
        checkBoxConfirmReceipt.isChecked = confirmReceipt

        return s
    }

    private fun saveSettings(smtpSettings: SMTPSettings, confirmEmail: Boolean) {
        smtpSettings.store(this)
        EncryptedPreferencesUtil.putBoolean(this, "CONFIRM_RECEIPT", confirmEmail)
    }

    private fun disableEdit() {
        smtpServerEditText.isEnabled = false
        portEditText.isEnabled = false
        senderEmailEditText.isEnabled = false
        senderPasswordEditText.isEnabled = false
        recipientEmailsEditText.isEnabled = false
        subjectLineEditText.isEnabled = false
        btnSendTestMail.isEnabled = false
        checkBoxConfirmReceipt.isEnabled = false

        editButton.text = "Edit"
        isEditing = false
        saveButton.isEnabled = false
    }

    private fun enableEdit() {
        smtpServerEditText.isEnabled = true
        portEditText.isEnabled = true
        senderEmailEditText.isEnabled = true
        senderPasswordEditText.isEnabled = true
        recipientEmailsEditText.isEnabled = true
        subjectLineEditText.isEnabled = true
        btnSendTestMail.isEnabled = true
        checkBoxConfirmReceipt.isEnabled = true
        editButton.text = "Cancel"
        isEditing = true
        saveButton.isEnabled = true
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun sendTestMail() {
        val settings: SMTPSettings = validateSettings() ?: return
        val subject = "Textfiltrator test mail"
        val body = "This is a test mail sent from Textfiltrator. Please confirm receipt by checking the box in the app."
        val testSettings = SMTPSettings(settings.smtpServer, settings.port, settings.senderEmail, settings.senderPassword, settings.recipientEmails, subject)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                sendEmailSSL(body, testSettings)
                LogManager.log("Test mail sent!")
                configStatusTextView.text = "Test mail sent!"
            } catch (e: Exception) {
                LogManager.log("Failed to send test mail: ${e.message}")
                configStatusTextView.text = "Failed to send test mail: ${e.message}"
            }
        }
    }
}
