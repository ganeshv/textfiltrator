# Try out SMTP server settings

import smtplib
from email.message import EmailMessage

def send_email(smtp_server, port, sender_email, sender_password, recipient_email, subject, body):
    if sender_password == 'abcdefghijklmnop':
        raise Exception('Edit this file and change credentials!')

    # Create the email message
    msg = EmailMessage()
    msg.set_content(body)
    msg['From'] = sender_email
    msg['To'] = recipient_email
    msg['Subject'] = subject

    try:
        with smtplib.SMTP_SSL(smtp_server, port) as server:
            server.login(sender_email, sender_password)
            server.send_message(msg)
        print("Email sent successfully!")
    except Exception as e:
        print(f"Error sending email: {e}")

# Example usage:
smtp_server = 'smtp.gmail.com'              # SMTP server address, e.g., 'smtp.gmail.com' for Gmail
port = 465                                  # Default port for SSL
sender_email = 'mythrowaway@gmail.com'      # Your email
sender_password = 'abcdefghijklmnop'        # If Gmail, this is the "App password" (NOT google account password)
recipient_email = 'myusername@gmail.com'    # Recipient's email
subject = 'Hello from Python'
body = 'This is a test email sent from a Python script!'

send_email(smtp_server, port, sender_email, sender_password, recipient_email, subject, body)

