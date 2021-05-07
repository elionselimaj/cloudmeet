package co.ritech.cloudmeet.jitsi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jitsi.meet.sdk.BroadcastEvent;
import org.jitsi.meet.sdk.BroadcastIntentHelper;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.URL;
import co.ritech.cloudmeet.R;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    public static final String roomName = "vpaas-magic-cookie-ab26e54bea154ba8aaf6a4e7e32edfd6/SampleAppDominantWinesBurnImmediately";
    public static final String token = "eyJraWQiOiJ2cGFhcy1tYWdpYy1jb29raWUtYWIyNmU1NGJlYTE1NGJhOGFhZjZhNGU3ZTMyZWRmZDYvMjZkZjE4LVNBTVBMRV9BUFAiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiJqaXRzaSIsImV4cCI6MTYyMDM5OTM3NCwibmJmIjoxNjIwMzkyMTY5LCJpc3MiOiJjaGF0Iiwicm9vbSI6IioiLCJzdWIiOiJ2cGFhcy1tYWdpYy1jb29raWUtYWIyNmU1NGJlYTE1NGJhOGFhZjZhNGU3ZTMyZWRmZDYiLCJjb250ZXh0Ijp7ImZlYXR1cmVzIjp7ImxpdmVzdHJlYW1pbmciOnRydWUsIm91dGJvdW5kLWNhbGwiOnRydWUsInRyYW5zY3JpcHRpb24iOnRydWUsInJlY29yZGluZyI6dHJ1ZX0sInVzZXIiOnsibW9kZXJhdG9yIjp0cnVlLCJuYW1lIjoiZXNlbGltYWoiLCJpZCI6ImF1dGgwfDYwOGFhNjlkYmFkMDJkMDA2OWYwNjRhOSIsImF2YXRhciI6IiIsImVtYWlsIjoiZXNlbGltYWpAcml0ZWNoLmNvIn19fQ.SDGbc7fFQFJLjpIdHDoLzcgR2tA4pW3vu1vF3-mhaGNWFW9rOZPpAX9MFRlQTvbJLFPq4t-mGhplOg84bXshxbjvk5k7swHJzB_nTLrZpRU5CCGjpVedqpJiwQKZon8uxdsJhea0XVVds8ntlZLbZwlSKa86Y_xt7tnm4M5lcFCvy-ivMA68SW4Ta1yC6ODJxjVktOay-Jw8SzyaJBGkLdYPMT5_FzKlF2wK9c7gyLi0ZxJ8dgF2CTLD4z-rjZy9G8QAVJ1gPNhABHp3Pj-_PGLDEfLVOzRT38uzWCfyPsbWn79DtZS05JFwBfFj-8MNaf09s6RlBRcHV_arPJDT4w";
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
