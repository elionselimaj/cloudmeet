package co.ritech.cloudmeet.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jitsi.meet.sdk.BroadcastEvent;
import org.jitsi.meet.sdk.BroadcastIntentHelper;
import co.ritech.cloudmeet.activity.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.URL;

import co.ritech.cloudmeet.R;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    public static final String roomName = "vpaas-magic-cookie-ab26e54bea154ba8aaf6a4e7e32edfd6/SampleAppIncredibleProofsUrgePainfully";
    public static final String token = "eyJraWQiOiJ2cGFhcy1tYWdpYy1jb29raWUtYWIyNmU1NGJlYTE1NGJhOGFhZjZhNGU3ZTMyZWRmZDYvMjZkZjE4LVNBTVBMRV9BUFAiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiJqaXRzaSIsImV4cCI6MTYyMDY4Nzk2NywibmJmIjoxNjIwNjgwNzYyLCJpc3MiOiJjaGF0Iiwicm9vbSI6IioiLCJzdWIiOiJ2cGFhcy1tYWdpYy1jb29raWUtYWIyNmU1NGJlYTE1NGJhOGFhZjZhNGU3ZTMyZWRmZDYiLCJjb250ZXh0Ijp7ImZlYXR1cmVzIjp7ImxpdmVzdHJlYW1pbmciOnRydWUsIm91dGJvdW5kLWNhbGwiOnRydWUsInRyYW5zY3JpcHRpb24iOnRydWUsInJlY29yZGluZyI6dHJ1ZX0sInVzZXIiOnsibW9kZXJhdG9yIjp0cnVlLCJuYW1lIjoiZXNlbGltYWoiLCJpZCI6ImF1dGgwfDYwOGFhNjlkYmFkMDJkMDA2OWYwNjRhOSIsImF2YXRhciI6IiIsImVtYWlsIjoiZXNlbGltYWpAcml0ZWNoLmNvIn19fQ.TS8-Fzpa6fb82nyH9eCzWyyWMghPH8K-CSUPhb2MzIdSbbc7eGdlvwDtj06SoAtMG_ZITwlnp0mqfBahN3_EmsO6x1S5O3qrqLQkFEnAsv5b859BppEBJH6JS0DExyhZuAKKUmQwBQ-EnDe4gCD45z7fkeRZ1j6P0QijSHg_cCA5Df4RJD4ju-iaPiXbE_nznLHDMNuU0RfwRcJ42JQeus96FIxh3BptA6mPy4ezM-3S6GyYAcy63Miuc7dWuDZQs8mHOGNa2WLS0jPNW5MHW2VmOC96b94aipZGwEp3ma14uT07Jp8Ihkn-KBhlQZvyPP3qa_qJfcZHPbCr2jh-Aw";
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onBroadcastReceived(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize default options for Jitsi Meet conferences.
        URL serverURL;
        try {
            // When using JaaS, replace "https://meet.jit.si" with the proper serverURL
            serverURL = new URL("https://8x8.vc");
            org.jitsi.meet.sdk.JitsiMeetConferenceOptions.Builder builder
                    = new org.jitsi.meet.sdk.JitsiMeetConferenceOptions.Builder();
            builder.setServerURL(serverURL);
            builder.setToken(token);
            builder.setWelcomePageEnabled(false);
            builder.setFeatureFlag("chat.enabled",false);
            builder.setRoom(roomName);

            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverURL)
                    .setToken(token)
                    .setWelcomePageEnabled(false)
                    .setFeatureFlag("chat.enabled",false)
                    .setRoom(roomName)
                    .build();

            JitsiMeetActivity.launch(this, options);

        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException("Invalid server URL!");
        }

        registerForBroadcastMessages();

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        super.onDestroy();
    }

    private void registerForBroadcastMessages() {
        IntentFilter intentFilter = new IntentFilter();

        /* This registers for every possible event sent from JitsiMeetSDK
           If only some of the events are needed, the for loop can be replaced
           with individual statements:
           ex:  intentFilter.addAction(BroadcastEvent.Type.AUDIO_MUTED_CHANGED.getAction());
                intentFilter.addAction(BroadcastEvent.Type.CONFERENCE_TERMINATED.getAction());
                ... other events
         */
        for (BroadcastEvent.Type type : BroadcastEvent.Type.values()) {
            intentFilter.addAction(type.getAction());
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    // Example for handling different JitsiMeetSDK events
    private void onBroadcastReceived(Intent intent) {
        if (intent != null) {
            BroadcastEvent event = new BroadcastEvent(intent);

            switch (event.getType()) {
                case CONFERENCE_JOINED:
                    Timber.i("Conference Joined with url%s", event.getData().get("url"));
                    break;
                case PARTICIPANT_JOINED:
                    Timber.i("Participant joined%s", event.getData().get("name"));
                    break;
            }
        }
    }

    // Example for sending actions to JitsiMeetSDK
    private void hangUp() {
        Intent hangupBroadcastIntent = BroadcastIntentHelper.buildHangUpIntent();
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(hangupBroadcastIntent);
    }
}
