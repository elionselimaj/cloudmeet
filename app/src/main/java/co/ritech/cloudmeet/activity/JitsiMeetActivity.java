package co.ritech.cloudmeet.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.facebook.react.modules.core.PermissionListener;
import java.util.HashMap;

import org.jitsi.meet.sdk.BroadcastEvent;
import org.jitsi.meet.sdk.BroadcastEvent.Type;
import org.jitsi.meet.sdk.JitsiMeetActivityDelegate;
import org.jitsi.meet.sdk.JitsiMeetActivityInterface;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions.Builder;
import org.jitsi.meet.sdk.JitsiMeetFragment;

import co.ritech.cloudmeet.jitsi.AudioModeModule;
import co.ritech.cloudmeet.jitsi.ConnectionService;
import co.ritech.cloudmeet.jitsi.JitsiMeetOngoingConferenceService;
import org.jitsi.meet.sdk.JitsiMeetView;
import org.jitsi.meet.sdk.R.id;
import org.jitsi.meet.sdk.log.JitsiMeetLogger;

import co.ritech.cloudmeet.R;

public class JitsiMeetActivity extends FragmentActivity implements JitsiMeetActivityInterface {
    protected static final String TAG = JitsiMeetActivity.class.getSimpleName();
    private static final String ACTION_JITSI_MEET_CONFERENCE = "org.jitsi.meet.CONFERENCE";
    private static final String JITSI_MEET_CONFERENCE_OPTIONS = "JitsiMeetConferenceOptions";
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            JitsiMeetActivity.this.onBroadcastReceived(intent);
        }
    };

    public JitsiMeetActivity() {
    }

    public static void launch(Context context, JitsiMeetConferenceOptions options) {
        Intent intent = new Intent(context, JitsiMeetActivity.class);
        intent.setAction("org.jitsi.meet.CONFERENCE");
        intent.putExtra("JitsiMeetConferenceOptions", options);
        if (!(context instanceof Activity)) {
            intent.setFlags(268435456);
        }

        context.startActivity(intent);
    }

    public static void launch(Context context, String url) {
        JitsiMeetConferenceOptions options = (new Builder()).setRoom(url).build();
        launch(context, options);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_jitsi_meet);
        this.registerForBroadcastMessages();
        if (!this.extraInitialize()) {
            this.initialize();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onDestroy() {
        this.leave();
        if (AudioModeModule.useConnectionService()) {
            ConnectionService.abortConnections();
        }

        JitsiMeetOngoingConferenceService.abort(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.broadcastReceiver);
        super.onDestroy();
    }

    public void finish() {
        this.leave();
        super.finish();
    }

    protected JitsiMeetView getJitsiView() {
        JitsiMeetFragment fragment = (JitsiMeetFragment)this.getSupportFragmentManager().findFragmentById(id.jitsiFragment);
        return fragment != null ? fragment.getJitsiView() : null;
    }

    public void join(@Nullable String url) {
        JitsiMeetConferenceOptions options = (new Builder()).setRoom(url).build();
        this.join(options);
    }

    public void join(JitsiMeetConferenceOptions options) {
        JitsiMeetView view = this.getJitsiView();
        if (view != null) {
            view.join(options);
        } else {
            JitsiMeetLogger.w("Cannot join, view is null", new Object[0]);
        }

    }

    public void leave() {
        JitsiMeetView view = this.getJitsiView();
        if (view != null) {
            view.leave();
        } else {
            JitsiMeetLogger.w("Cannot leave, view is null", new Object[0]);
        }

    }

    @Nullable
    private JitsiMeetConferenceOptions getConferenceOptions(Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.VIEW".equals(action)) {
            Uri uri = intent.getData();
            if (uri != null) {
                return (new Builder()).setRoom(uri.toString()).build();
            }
        } else if ("org.jitsi.meet.CONFERENCE".equals(action)) {
            return (JitsiMeetConferenceOptions)intent.getParcelableExtra("JitsiMeetConferenceOptions");
        }

        return null;
    }

    protected boolean extraInitialize() {
        return false;
    }

    protected void initialize() {
        this.join(this.getConferenceOptions(this.getIntent()));
    }

    protected void onConferenceJoined(HashMap<String, Object> extraData) {
        JitsiMeetLogger.i("Conference joined: " + extraData, new Object[0]);
        JitsiMeetOngoingConferenceService.launch(this);
    }

    protected void onConferenceTerminated(HashMap<String, Object> extraData) {
        JitsiMeetLogger.i("Conference terminated: " + extraData, new Object[0]);
        this.finish();
    }

    protected void onConferenceWillJoin(HashMap<String, Object> extraData) {
        JitsiMeetLogger.i("Conference will join: " + extraData, new Object[0]);
    }

    protected void onParticipantJoined(HashMap<String, Object> extraData) {
        try {
            JitsiMeetLogger.i("Participant joined: ", new Object[]{extraData});
        } catch (Exception var3) {
            JitsiMeetLogger.w("Invalid participant joined extraData", new Object[]{var3});
        }

    }

    protected void onParticipantLeft(HashMap<String, Object> extraData) {
        try {
            JitsiMeetLogger.i("Participant left: ", new Object[]{extraData});
        } catch (Exception var3) {
            JitsiMeetLogger.w("Invalid participant left extraData", new Object[]{var3});
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        JitsiMeetActivityDelegate.onActivityResult(this, requestCode, resultCode, data);
    }

    public void onBackPressed() {
        JitsiMeetActivityDelegate.onBackPressed();
    }

    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        JitsiMeetConferenceOptions options;
        if ((options = this.getConferenceOptions(intent)) != null) {
            this.join(options);
        } else {
            JitsiMeetActivityDelegate.onNewIntent(intent);
        }
    }

    protected void onUserLeaveHint() {
        JitsiMeetView view = this.getJitsiView();
        if (view != null) {
            view.enterPictureInPicture();
        }

    }

    public void requestPermissions(String[] permissions, int requestCode, PermissionListener listener) {
        JitsiMeetActivityDelegate.requestPermissions(this, permissions, requestCode, listener);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void registerForBroadcastMessages() {
        IntentFilter intentFilter = new IntentFilter();
        Type[] var2 = Type.values();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Type type = var2[var4];
            intentFilter.addAction(type.getAction());
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(this.broadcastReceiver, intentFilter);
    }

    private void onBroadcastReceived(Intent intent) {
        if (intent != null) {
            BroadcastEvent event = new BroadcastEvent(intent);
            switch(event.getType()) {
                case CONFERENCE_JOINED:
                    this.onConferenceJoined(event.getData());
                    break;
                case CONFERENCE_WILL_JOIN:
                    this.onConferenceWillJoin(event.getData());
                    break;
                case CONFERENCE_TERMINATED:
                    this.onConferenceTerminated(event.getData());
                    break;
                case PARTICIPANT_JOINED:
                    this.onParticipantJoined(event.getData());
                    break;
                case PARTICIPANT_LEFT:
                    this.onParticipantLeft(event.getData());
            }
        }

    }
}
