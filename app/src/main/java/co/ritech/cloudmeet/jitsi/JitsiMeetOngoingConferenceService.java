package co.ritech.cloudmeet.jitsi;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Build.VERSION;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.jitsi.meet.sdk.BroadcastEvent.Type;
import org.jitsi.meet.sdk.BroadcastIntentHelper;
import org.jitsi.meet.sdk.log.JitsiMeetLogger;

public class JitsiMeetOngoingConferenceService extends Service implements OngoingConferenceTracker.OngoingConferenceListener {
    private static final String TAG = org.jitsi.meet.sdk.JitsiMeetOngoingConferenceService.class.getSimpleName();
    private final JitsiMeetOngoingConferenceService.BroadcastReceiver broadcastReceiver = new JitsiMeetOngoingConferenceService.BroadcastReceiver();
    private boolean isAudioMuted;

    public JitsiMeetOngoingConferenceService() {
    }

    public static void launch(Context context) {
        OngoingNotification.createOngoingConferenceNotificationChannel();
        Intent intent = new Intent(context, JitsiMeetOngoingConferenceService.class);
        intent.setAction(JitsiMeetOngoingConferenceService.Action.START.getName());
        ComponentName componentName;
        if (VERSION.SDK_INT >= 26) {
            componentName = context.startForegroundService(intent);
        } else {
            componentName = context.startService(intent);
        }

        if (componentName == null) {
            JitsiMeetLogger.w(TAG + " Ongoing conference service not started", new Object[0]);
        }

    }

    public static void abort(Context context) {
        Intent intent = new Intent(context, JitsiMeetOngoingConferenceService.class);
        context.stopService(intent);
    }

    public void onCreate() {
        super.onCreate();
        OngoingConferenceTracker.getInstance().addListener(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Type.AUDIO_MUTED_CHANGED.getAction());
        LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(this.broadcastReceiver, intentFilter);
    }

    public void onDestroy() {
        OngoingConferenceTracker.getInstance().removeListener(this);
        LocalBroadcastManager.getInstance(this.getApplicationContext()).unregisterReceiver(this.broadcastReceiver);
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("WrongConstant")
    public int onStartCommand(Intent intent, int flags, int startId) {
        String actionName = intent.getAction();
        JitsiMeetOngoingConferenceService.Action action = JitsiMeetOngoingConferenceService.Action.fromName(actionName);
        switch(action) {
            case UNMUTE:
            case MUTE:
                Intent muteBroadcastIntent = BroadcastIntentHelper.buildSetAudioMutedIntent(action == JitsiMeetOngoingConferenceService.Action.MUTE);
                LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(muteBroadcastIntent);
                break;
            case START:
                Notification notification = OngoingNotification.buildOngoingConferenceNotification(this.isAudioMuted);
                if (notification == null) {
                    this.stopSelf();
                    JitsiMeetLogger.w(TAG + " Couldn't start service, notification is null", new Object[0]);
                } else {
                    this.startForeground(OngoingNotification.NOTIFICATION_ID, notification);
                    JitsiMeetLogger.i(TAG + " Service started", new Object[0]);
                }
                break;
            case HANGUP:
                JitsiMeetLogger.i(TAG + " Hangup requested", new Object[0]);
                Intent hangupBroadcastIntent = BroadcastIntentHelper.buildHangUpIntent();
                LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(hangupBroadcastIntent);
                this.stopSelf();
                break;
            default:
                JitsiMeetLogger.w(TAG + " Unknown action received: " + action, new Object[0]);
                this.stopSelf();
        }

        return 2;
    }

    public void onCurrentConferenceChanged(String conferenceUrl) {
        if (conferenceUrl == null) {
            this.stopSelf();
            OngoingNotification.resetStartingtime();
            JitsiMeetLogger.i(TAG + "Service stopped", new Object[0]);
        }

    }

    private class BroadcastReceiver extends android.content.BroadcastReceiver {
        private BroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            JitsiMeetOngoingConferenceService.this.isAudioMuted = Boolean.parseBoolean(intent.getStringExtra("muted"));
            Notification notification = OngoingNotification.buildOngoingConferenceNotification(JitsiMeetOngoingConferenceService.this.isAudioMuted);
            if (notification == null) {
                JitsiMeetOngoingConferenceService.this.stopSelf();
                JitsiMeetLogger.w(JitsiMeetOngoingConferenceService.TAG + " Couldn't start service, notification is null", new Object[0]);
            } else {
                JitsiMeetOngoingConferenceService.this.startForeground(OngoingNotification.NOTIFICATION_ID, notification);
                JitsiMeetLogger.i(JitsiMeetOngoingConferenceService.TAG + " Service started", new Object[0]);
            }

        }
    }

    public enum Action {
        START(JitsiMeetOngoingConferenceService.TAG + ":START"),
        HANGUP(JitsiMeetOngoingConferenceService.TAG + ":HANGUP"),
        MUTE(JitsiMeetOngoingConferenceService.TAG + ":MUTE"),
        UNMUTE(JitsiMeetOngoingConferenceService.TAG + ":UNMUTE");

        private final String name;

        private Action(String name) {
            this.name = name;
        }

        public static JitsiMeetOngoingConferenceService.Action fromName(String name) {
            JitsiMeetOngoingConferenceService.Action[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                JitsiMeetOngoingConferenceService.Action action = var1[var3];
                if (action.name.equalsIgnoreCase(name)) {
                    return action;
                }
            }

            return null;
        }

        public String getName() {
            return this.name;
        }
    }
}
