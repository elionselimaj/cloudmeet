package co.ritech.cloudmeet.jitsi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import co.ritech.cloudmeet.R;

import org.jitsi.meet.sdk.log.JitsiMeetLogger;

import java.util.Random;

public class OngoingNotification {
    private static final String TAG = OngoingNotification.class.getSimpleName();
    private static final String CHANNEL_ID = "JitsiNotificationChannel";
    private static final String CHANNEL_NAME = "Ongoing Conference Notifications";
    public static final int NOTIFICATION_ID = (new Random()).nextInt(99999) + 10000;
    private static long startingTime = 0L;

    OngoingNotification() {
    }

    public static void createOngoingConferenceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            Context context = ReactInstanceManagerHolder.getCurrentActivity();
            if (context == null) {
                JitsiMeetLogger.w(TAG + " Cannot create notification channel: no current context", new Object[0]);
            } else {
                NotificationManager notificationManager = (NotificationManager)context.getSystemService("notification");
                NotificationChannel channel = notificationManager.getNotificationChannel("JitsiNotificationChannel");
                if (channel == null) {
                    channel = new NotificationChannel("JitsiNotificationChannel", "Ongoing Conference Notifications", 3);
                    channel.enableLights(false);
                    channel.enableVibration(false);
                    channel.setShowBadge(false);
                    notificationManager.createNotificationChannel(channel);
                }
            }
        }
    }

    public static Notification buildOngoingConferenceNotification(boolean isMuted) {
        Context context = ReactInstanceManagerHolder.getCurrentActivity();
        if (context == null) {
            JitsiMeetLogger.w(TAG + " Cannot create notification: no current context", new Object[0]);
            return null;
        } else {
            Intent notificationIntent = new Intent(context, context.getClass());
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "JitsiNotificationChannel");
            if (startingTime == 0L) {
                startingTime = System.currentTimeMillis();
            }

            builder.setCategory("call").setContentTitle("Ongoing meeting").setContentText(context.getString(R.string.ongoing_notification_text)).setPriority(0).setContentIntent(pendingIntent).setOngoing(true).setWhen(startingTime).setUsesChronometer(true).setAutoCancel(false).setVisibility(1).setOnlyAlertOnce(true).setSmallIcon(context.getResources().getIdentifier("ic_notification", "drawable", context.getPackageName()));
            NotificationCompat.Action hangupAction = createAction(context, JitsiMeetOngoingConferenceService.Action.HANGUP, R.string.ongoing_notification_action_hang_up);
            JitsiMeetOngoingConferenceService.Action toggleAudioAction = isMuted ? JitsiMeetOngoingConferenceService.Action.UNMUTE : JitsiMeetOngoingConferenceService.Action.MUTE;
            int toggleAudioTitle = isMuted ? R.string.ongoing_notification_action_unmute : R.string.ongoing_notification_action_mute;
            NotificationCompat.Action audioAction = createAction(context, toggleAudioAction, toggleAudioTitle);
            builder.addAction(hangupAction);
            builder.addAction(audioAction);
            return builder.build();
        }
    }

    public static void resetStartingtime() {
        startingTime = 0L;
    }

    private static NotificationCompat.Action createAction(Context context, JitsiMeetOngoingConferenceService.Action action, @StringRes int titleId) {
        Intent intent = new Intent(context, JitsiMeetOngoingConferenceService.class);
        intent.setAction(action.getName());
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 134217728);
        String title = context.getString(titleId);
        return new NotificationCompat.Action(0, title, pendingIntent);
    }
}

