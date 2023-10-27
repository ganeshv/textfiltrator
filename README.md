# TextFiltrator

Texts exfiltrated, OTPs forwarded, Googles thwarted

<img src="https://raw.githubusercontent.com/ganeshv/textfiltrator/master/tf_logo.png" width="200">

## Overview

TextFiltrator is an Android app which automatically forwards SMS messages to one or
more email addresses. A list of keywords may be specified, and only messages containing a
keyword will be forwarded.

This is useful for those who receive transactional messages and OTPs on multiple phones,
and don't want to carry all of them. Especially now that Google's Messages Web
has stopped showing OTPs, "for our security", making it completely useless.

You will need an account with an email provider who lets you send mail via SMTP.

It does not work for RCS messages, which are not SMS but a kind of Google WhatsApp, and
confusingly implemented by the same Messages app.

Forwarding (once enabled) will continue in the background, even if the app is closed 
or the phone reboots.

## Usage

Use the **Configure** button to set up and edit config details.

- Set up SMTP server details. 
    - **SMTP Server**: Hostname or IP address of the SMTP server.
    - **SMTP Port**: Use 465 for SMTP over SSL (other ports might work but not recommended).
    - **SMTP Username**: This is the username on the SMTP server. For most providers this looks
        like an email address.
    - **SMTP Password**: Password associated with the username. In case of Google, this is the
    "App Password" (explained later).
- **Recipient Emails:** Specify recipient email addresses, separated by spaces.
- **Subject Line:** Set to something like "SMS from ABC's Pixel 7", useful to distinguish which phone sent the email.
- **Keywords:** A list of words separated by spaces. If the SMS contains one or more
    of these words, it will be forwarded. Leave blank to forward all messages (default).
- Hit the **Save** button, this will perform some elementary validation on the fields.
- Hit the **Send test mail** button to verify SMTP settings. Once you **confirm receipt** by ticking
    the checkbox, hit **Save** again.
- Go back to the main screen and use the **Fwd SMS** button to start automatic forwarding.

## Getting an SMTP provider

Email is an open protocol, the oldest on the net, but there is no point in trying direct SMTP.
Your mails will be most likely rejected as spam. You need an "email lawyer", i.e. an SMTP relay run by a
reputed source, to present your mail in the best possible light to the recipient.

The recommended provider is Google, since as an Android user you already have a Google account.
Plus they already know everything about you, a few more OTPs won't hurt. However, Google makes it
difficult to use SMTP. Here's what works as of Oct 2023:

1. (Recommended) Create a new Gmail account, though you can use an existing account as well.
2. Go to https://myaccount.google.com, **Security** section.
3. **2-Step Verification**: You need to enable this, or the following steps won't work.
4. **App passwords**: Go to the search bar at the top of the page which says "Search Google Account"
(not the browser URL or search bar) and enter "app" to find the **App passwords** menu.
It's not visible otherwise. Create an app password with a name like "SMTP". You will get a 
popup with 16 characters, like "abcd efgh ijkl mnop". Your app password is "abcdefghijklmnop",
carefully note it before dismissing the popup.

Now you can use the following SMTP settings:
- **SMTP Server**: smtp.gmail.com
- **SMTP Port**: 465
- **SMTP Username**: myusername@gmail.com
- **SMTP Password**: abcdefghijklmnop

Hit **Send Test Mail** to verify that it works. If you get something like
"5.7.8 Username and Password not accepted." in the status, something didn't
go right with the previous steps, or Google changed things yet again.

Other SMTP providers include [Brevo](https://www.brevo.com/products/transactional-email/) and
[Mailgun](https://www.mailgun.com/). Both have free plans which don't require a credit card,
and the free plan has enough quota for this use case.

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
