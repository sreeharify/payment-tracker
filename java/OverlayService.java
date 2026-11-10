package org.pay.tracker;

import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.File;

public class OverlayService extends Service {
    WindowManager wm;
    View view;
    String curAmount, curBody;
    EditText input;

    @Override public IBinder onBind(Intent i) { return null; }

    static void save(Context c, String amount, String label, String body) {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(c.getFilesDir(), "expenses.db"), null);
        db.execSQL("CREATE TABLE IF NOT EXISTS expenses(id INTEGER PRIMARY KEY AUTOINCREMENT, amount REAL, label TEXT, body TEXT, ts INTEGER)");
        db.execSQL("INSERT INTO expenses(amount,label,body,ts) VALUES(?,?,?,?)",
                new Object[]{Double.parseDouble(amount), label, body, System.currentTimeMillis()});
        db.close();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(new NotificationChannel("pt", "Expense prompt", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, "pt")
                .setContentTitle("Payment detected").setContentText("Tell me what it was for")
                .setSmallIcon(android.R.drawable.ic_dialog_info).build();
        startForeground(1, n);

        if (intent == null || !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY; }
        if (view != null) { save(this, curAmount, "Unlabeled", curBody); remove(); }
        curAmount = intent.getStringExtra("amount");
        curBody = intent.getStringExtra("body");
        show();
        return START_NOT_STICKY;
    }

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    void show() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(20));
        box.setBackground(bg);

        TextView t = new TextView(this);
        t.setText("You spent ₹" + curAmount + ". What was it for?");
        t.setTextColor(Color.BLACK); t.setTextSize(18); t.setTypeface(null, Typeface.BOLD);
        box.addView(t);

        input = new EditText(this);
        input.setHint("e.g. Food, Fuel, Rent");
        input.setTextColor(Color.BLACK); input.setHintTextColor(Color.GRAY);
        box.addView(input);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        Button skip = new Button(this); skip.setText("Skip");
        Button ok = new Button(this); ok.setText("Save");
        row.addView(skip); row.addView(ok);
        box.addView(row);

        skip.setOnClickListener(v -> { save(this, curAmount, "Unlabeled", curBody); finishUp(); });
        ok.setOnClickListener(v -> {
            String l = input.getText().toString().trim();
            save(this, curAmount, l.isEmpty() ? "Unlabeled" : l, curBody);
            finishUp();
        });

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        p.dimAmount = 0.5f;
        p.gravity = Gravity.CENTER;
        p.horizontalMargin = 0.05f;
        p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        view = box;
        wm.addView(view, p);
    }

    void remove() { try { if (view != null) wm.removeView(view); } catch (Exception e) {} view = null; }
    void finishUp() { remove(); stopForeground(true); stopSelf(); }
    @Override public void onDestroy() { remove(); super.onDestroy(); }
}
