# TextFiltrator

Texts exfiltrated, OTPs forwarded, Googles thwarted

![App Logo](https://raw.github.com/ganeshv/textfiltrator/master/tf_logo.png | width=240)

## Overview

TextFiltrator is an Android app which automatically forward SMS messages to one or
more email addresses. By default, all texts are forwarded, but if keywords are specified,
then only texts containing at least one of the words will be forwarded.

This is useful for those who need multiple phones, receive transactional messages
and OTPs, and don't want to carry all of them. Especially now that Google's Messages Web
has stopped showing OTPs, "for our security", making it completely useless.

You will need an account with an email provider who lets you send mail via SMTP.

It does not work for RCS messages, which are not SMS but a kind of "Google WhatsApp" and
confusingly implemented by the same Messages app.

Forwarding (once enabled) will continue in the background, even if the app is closed, or
your phone reboots and you didn't open the app after reboot.

## Usage

Go to the configuration screen using the **Configure** button.

- Set up SMTP server details. 
    - **SMTP Server**: Hostname or IP address of the SMTP server
    - **SMTP Port**: Use 465, for SMTP over SSL (other ports might work but not recommended)
    - **SMTP Username**: This is the username on the SMTP server. For most providers this looks
        like an emaila address
    - **SMTP Password**: Password associated with the username. In case of Google, this is the
    "App Password" (explained later)
- **Recipient Emails:** Specify recipient email addresses: If more than one, separate with spaces
- **Subject Line:** Useful to distinguish which phone sent the email
- **Match words:** Specify a list of words, separated by spaces. If the SMS contains any one or more
    of these words, it will be forwarded. Leave it blank to forward all messages.
- Hit the **Save** button, this will perform some elementary validation on the fields.
- Hit the **Send test mail** button to verify SMTP settings. Once you confirm receipt by ticking
    the checkbox, hit **Save**.
- Go back to the main screen and hit the **Fwd SMS** button to start forwarding.

## Getting an SMTP provider

Email is an open protocol, the oldest on the net, but no point in trying direct SMTP, your mails
will be rejected as spam. You need an "email lawyer", aka SMTP relay, to present your email in the best
possible light to the recipient.

The recommended provider is Google, since as an Android user you already have a Google account.
Plus they already know everything about you, a few more OTPs won't hurt. However, Google makes it
difficult to use SMTP. Here's what works as of Oct 2023:

1. (Recommended) Create a new Gmail account, though you can use an existing account as well.
2. Go to https://myaccount.google.com
3. **Two factor authentication**: You need to enable this, or the following steps won't work
4. **App passwords**: Go to the myaccount.google.com search bar and search for "App passwords".
It's not visible otherwise. Create an app password with any name, like "SMTP". You will get a 
popup with 16 characters, like "abcd efgh ijkl mnop". Your app password is "abcdefghijklmnop",
carefully copy it before dismissing the popup.

Now you can use the following SMTP settings:
- **SMTP Server**: smtp.gmail.com
- **SMTP Port**: 465
- **SMTP Username**: myusername@gmail.com
- **SMTP Password**: abcdefghijklmnop

Hit **Send Test Mail** to verify that it works. If you get something like
"5.7.8 Username and Password not accepted." in the status, something didn't
go right with the previous steps, or Google changed things yet again.

Other SMTP providers include [Brevo](https://www.brevo.com/products/transactional-email/) and
[Mailgun](https://www.mailgun.com/).

## Setup & Installation

Sorry, I'm not providing apks right now. You'll need to build from source, using
Android Studio.

TBD: Add steps to compile without Android Studio

## Permissions

The app requires the following permissions:

- **Read SMS**: To access and forward incoming SMS messages.
- **Internet**: To send emails using SMTP.

## License

This project is licensed under the MIT License. See `LICENSE` for more details.

## Acknowledgements

ChatGPT and Github Copilot were extensively used for the entire development
process. I did not know anything about Android, Kotlin, Android Studio, etc.
and learned everything using ChatGPT.
