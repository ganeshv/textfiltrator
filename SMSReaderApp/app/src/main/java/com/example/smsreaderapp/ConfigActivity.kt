package com.example.smsreaderapp

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

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

    private var isEditing : Boolean = false

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

        // Load previously saved settings
        loadSettings()

        editButton = findViewById(R.id.editButton)
        saveButton = findViewById(R.id.saveButton)
        resetButton = findViewById(R.id.resetButton)
        configStatusTextView = findViewById(R.id.configStatusTextView)

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

            val smtpServer = smtpServerEditText.text.toString()
            val portString = portEditText.text.toString()
            val senderEmail = senderEmailEditText.text.toString()
            val senderPassword = senderPasswordEditText.text.toString()
            val recipientEmails = recipientEmailsEditText.text.toString().split(" ")
            val subjectLine = subjectLineEditText.text.toString()

            // Validations
            if (smtpServer.isEmpty()) {
                configStatusTextView.text = "SMTP Server cannot be empty!"
                return@setOnClickListener
            }
            val port = portString.toIntOrNull()
            if (port == null || port <= 0 || port > 65535) {
                configStatusTextView.text = "Invalid port number!"
                return@setOnClickListener
            }
            if (senderEmail.isEmpty() || !isValidEmail(senderEmail)) {
                configStatusTextView.text = "Invalid sender email!"
                return@setOnClickListener
            }

            if (senderPassword.isBlank()) {
                configStatusTextView.text = "Sender password cannot be empty!"
                return@setOnClickListener
            }

            if (recipientEmails.isEmpty() || !recipientEmails.all { isValidEmail(it) }) {
                configStatusTextView.text = "Invalid recipient email(s)!"
                return@setOnClickListener
            }

            if (subjectLine.isBlank()) {
                configStatusTextView.text = "Subject line cannot be empty!"
                return@setOnClickListener
            }

            // Save to encrypted shared preferences
            saveSettings(smtpServer, portString, senderEmail, senderPassword, recipientEmails.joinToString(" "), subjectLine)
            configStatusTextView.text = "Configuration saved successfully!"
            LogManager.log("Configuration saved successfully!")
            // Make all the fields uneditable until the Edit button is pressed
            disableEdit()
        }

        resetButton.setOnClickListener {
            // Wipe all preferences
            saveSettings("", "", "", "", "", "")
            loadSettings()
            LogManager.log("Configuration reset successfully!")
        }
    }

    private fun loadSettings() {
        val smtpServer = EncryptedPreferencesUtil.getString(this, "SMTP_SERVER") ?: ""
        val port = EncryptedPreferencesUtil.getString(this, "PORT")
        val senderEmail = EncryptedPreferencesUtil.getString(this, "SENDER_EMAIL") ?: ""
        val senderPassword = EncryptedPreferencesUtil.getString(this, "SENDER_PASSWORD") ?: ""
        val recipientEmails = EncryptedPreferencesUtil.getString(this, "RECIPIENT_EMAILS") ?: ""
        val subjectLine = EncryptedPreferencesUtil.getString(this, "SUBJECT_LINE") ?: ""

        // Populate the fields with the loaded settings
        smtpServerEditText.setText(smtpServer)
        portEditText.setText(port)
        senderEmailEditText.setText(senderEmail)
        senderPasswordEditText.setText(senderPassword)
        recipientEmailsEditText.setText(recipientEmails)
        subjectLineEditText.setText(subjectLine)
    }

    private fun saveSettings(smtpServer: String, port: String, senderEmail: String, senderPassword: String, recipientEmails: String, subjectLine: String) {
        EncryptedPreferencesUtil.putString(this, "SMTP_SERVER", smtpServer)
        EncryptedPreferencesUtil.putString(this, "PORT", port)
        EncryptedPreferencesUtil.putString(this, "SENDER_EMAIL", senderEmail)
        EncryptedPreferencesUtil.putString(this, "SENDER_PASSWORD", senderPassword)
        EncryptedPreferencesUtil.putString(this, "RECIPIENT_EMAILS", recipientEmails)
        EncryptedPreferencesUtil.putString(this, "SUBJECT_LINE", subjectLine)
    }

    private fun disableEdit() {
        smtpServerEditText.isEnabled = false
        portEditText.isEnabled = false
        senderEmailEditText.isEnabled = false
        senderPasswordEditText.isEnabled = false
        recipientEmailsEditText.isEnabled = false
        subjectLineEditText.isEnabled = false
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
        editButton.text = "Cancel"
        isEditing = true
        saveButton.isEnabled = true
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
