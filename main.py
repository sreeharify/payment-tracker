import os, sqlite3, datetime
from kivy.app import App
from kivy.clock import Clock
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.gridlayout import GridLayout
from kivy.utils import platform

if platform == "android":
    from jnius import autoclass
    from android.permissions import request_permissions, Permission
    PA = autoclass("org.kivy.android.PythonActivity")
    Settings = autoclass("android.provider.Settings")
    Uri = autoclass("android.net.Uri")
    Intent = autoclass("android.content.Intent")

def db_path():
    if platform == "android":
        return PA.mActivity.getFilesDir().getAbsolutePath() + "/expenses.db"
    return "expenses.db"

def rows():
    con = sqlite3.connect(db_path())
    con.execute("CREATE TABLE IF NOT EXISTS expenses(id INTEGER PRIMARY KEY AUTOINCREMENT, amount REAL, label TEXT, body TEXT, ts INTEGER)")
    r = con.execute("SELECT amount,label,ts FROM expenses ORDER BY id DESC").fetchall()
    con.close()
    return r

class Root(BoxLayout):
    def __init__(self, **kw):
        super().__init__(orientation="vertical", padding=12, spacing=8, **kw)
        self.total = Label(text="", size_hint_y=None, height=60, font_size="22sp")
        self.add_widget(self.total)
        b = Button(text="Grant 'Display over other apps'", size_hint_y=None, height=50, on_release=self.ask_overlay)
        self.add_widget(b)
        self.grid = GridLayout(cols=1, size_hint_y=None, spacing=6)
        self.grid.bind(minimum_height=self.grid.setter("height"))
        sv = ScrollView(); sv.add_widget(self.grid); self.add_widget(sv)
        Clock.schedule_interval(lambda dt: self.refresh(), 2)
        self.refresh()

    def ask_overlay(self, *a):
        if platform != "android": return
        i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                   Uri.parse("package:" + PA.mActivity.getPackageName()))
        PA.mActivity.startActivity(i)

    def refresh(self):
        data = rows()
        self.total.text = "Total spent: Rs %.2f" % sum(r[0] for r in data)
        self.grid.clear_widgets()
        for amt, label, ts in data:
            d = datetime.datetime.fromtimestamp(ts / 1000).strftime("%d %b %H:%M")
            l = Label(text="%s   Rs %.2f   [%s]" % (label, amt, d), size_hint_y=None, height=40)
            self.grid.add_widget(l)

class TrackerApp(App):
    def build(self):
        if platform == "android":
            request_permissions([Permission.RECEIVE_SMS, Permission.READ_SMS, "android.permission.POST_NOTIFICATIONS"])
            if not Settings.canDrawOverlays(PA.mActivity):
                Clock.schedule_once(lambda dt: self.root.ask_overlay(), 2)
        return Root()

TrackerApp().run()
