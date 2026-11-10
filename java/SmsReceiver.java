package org.pay.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import java.util.regex.*;

public class SmsReceiver extends BroadcastReceiver {
    static final Pattern AMT = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s?([0-9,]+(?:\\.[0-9]+)?)");
    static final Pattern DEBIT = Pattern.compile("(?i)debited|spent|paid|purchase|withdrawn|sent|payment");

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (msgs == null || msgs.length == 0) return;
        StringBuilder body = new StringBuilder();
        String sender = msgs[0].getOriginatingAddress();
        for (SmsMessage m : msgs) body.append(m.getMessageBody());
        String text = body.toString();
        // bank senders are alphanumeric (e.g. HDFCBK), not plain phone numbers
        if (sender == null || !sender.matches(".*[A-Za-z].*")) return;
        if (!DEBIT.matcher(text).find()) return;
        Matcher m = AMT.matcher(text);
        if (!m.find()) return;
        String amount = m.group(1).replace(",", "");

        Intent i = new Intent(ctx, OverlayService.class);
        i.putExtra("amount", amount);
        i.putExtra("body", text);
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i);
        else ctx.startService(i);
    }
}
