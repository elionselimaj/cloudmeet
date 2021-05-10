package co.ritech.cloudmeet.jitsi;


import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccount.Builder;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableNativeMap;

import org.jitsi.meet.sdk.log.JitsiMeetLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiresApi(
        api = 26
)
public class ConnectionService extends android.telecom.ConnectionService {
    static final String TAG = "JitsiConnectionService";
    static final String EXTRA_PHONE_ACCOUNT_HANDLE = "connection_service.PHONE_ACCOUNT_HANDLE";
    private static final Map<String, ConnectionService.ConnectionImpl> connections = new HashMap();
    private static final HashMap<String, Promise> startCallPromises = new HashMap();

    public ConnectionService() {
    }

    public static void abortConnections() {
        Iterator var0 = getConnections().iterator();

        while(var0.hasNext()) {
            ConnectionService.ConnectionImpl connection = (ConnectionService.ConnectionImpl)var0.next();
            connection.onAbort();
        }

    }

    static void addConnection(ConnectionService.ConnectionImpl connection) {
        connections.put(connection.getCallUUID(), connection);
    }

    static List<ConnectionService.ConnectionImpl> getConnections() {
        return new ArrayList(connections.values());
    }

    static boolean isSamsungDevice() {
        return Build.MANUFACTURER.toLowerCase().contains("samsung");
    }

    static void registerStartCallPromise(String uuid, Promise promise) {
        startCallPromises.put(uuid, promise);
    }

    static void removeConnection(ConnectionService.ConnectionImpl connection) {
        connections.remove(connection.getCallUUID());
    }

    static boolean setConnectionActive(String callUUID) {
        ConnectionService.ConnectionImpl connection = (ConnectionService.ConnectionImpl)connections.get(callUUID);
        if (connection != null) {
            connection.setActive();
            return true;
        } else {
            JitsiMeetLogger.w("%s setConnectionActive - no connection for UUID: %s", new Object[]{"JitsiConnectionService", callUUID});
            return false;
        }
    }

    static void setConnectionDisconnected(String callUUID, DisconnectCause cause) {
        ConnectionService.ConnectionImpl connection = (ConnectionService.ConnectionImpl)connections.get(callUUID);
        if (connection != null) {
            if (isSamsungDevice()) {
                connection.setOnHold();
                connection.setConnectionProperties(144);
            }

            connection.setDisconnected(cause);
            connection.destroy();
        } else {
            JitsiMeetLogger.e("JitsiConnectionService endCall no connection for UUID: " + callUUID, new Object[0]);
        }

    }

    static Promise unregisterStartCallPromise(String uuid) {
        return (Promise)startCallPromises.remove(uuid);
    }

    static void updateCall(String callUUID, ReadableMap callState) {
        ConnectionService.ConnectionImpl connection = (ConnectionService.ConnectionImpl)connections.get(callUUID);
        if (connection != null) {
            if (callState.hasKey("hasVideo")) {
                boolean hasVideo = callState.getBoolean("hasVideo");
                JitsiMeetLogger.i(" %s updateCall: %s hasVideo: %s", new Object[]{"JitsiConnectionService", callUUID, hasVideo});
                connection.setVideoState(hasVideo ? 3 : 0);
            }
        } else {
            JitsiMeetLogger.e("JitsiConnectionService updateCall no connection for UUID: " + callUUID, new Object[0]);
        }

    }

    public Connection onCreateOutgoingConnection(PhoneAccountHandle accountHandle, ConnectionRequest request) {
        ConnectionService.ConnectionImpl connection = new ConnectionService.ConnectionImpl();
        connection.setConnectionProperties(128);
        connection.setAddress(request.getAddress(), 3);
        connection.setExtras(request.getExtras());
        connection.setAudioModeIsVoip(true);
        connection.setVideoState(request.getVideoState());
        Bundle moreExtras = new Bundle();
        moreExtras.putParcelable("connection_service.PHONE_ACCOUNT_HANDLE", (Parcelable)Objects.requireNonNull(request.getAccountHandle(), "accountHandle"));
        connection.putExtras(moreExtras);
        addConnection(connection);
        Promise startCallPromise = unregisterStartCallPromise(connection.getCallUUID());
        if (startCallPromise != null) {
            JitsiMeetLogger.d("JitsiConnectionService onCreateOutgoingConnection " + connection.getCallUUID(), new Object[0]);
            startCallPromise.resolve((Object)null);
        } else {
            JitsiMeetLogger.e("JitsiConnectionService onCreateOutgoingConnection: no start call Promise for " + connection.getCallUUID(), new Object[0]);
        }

        return connection;
    }

    public Connection onCreateIncomingConnection(PhoneAccountHandle accountHandle, ConnectionRequest request) {
        throw new RuntimeException("Not implemented");
    }

    public void onCreateIncomingConnectionFailed(PhoneAccountHandle accountHandle, ConnectionRequest request) {
        throw new RuntimeException("Not implemented");
    }

    public void onCreateOutgoingConnectionFailed(PhoneAccountHandle accountHandle, ConnectionRequest request) {
        PhoneAccountHandle theAccountHandle = request.getAccountHandle();
        String callUUID = theAccountHandle.getId();
        JitsiMeetLogger.e("JitsiConnectionService onCreateOutgoingConnectionFailed " + callUUID, new Object[0]);
        if (callUUID != null) {
            Promise startCallPromise = unregisterStartCallPromise(callUUID);
            if (startCallPromise != null) {
                startCallPromise.reject("CREATE_OUTGOING_CALL_FAILED", "The request has been denied by the system");
            } else {
                JitsiMeetLogger.e("JitsiConnectionService startCallFailed - no start call Promise for UUID: " + callUUID, new Object[0]);
            }
        } else {
            JitsiMeetLogger.e("JitsiConnectionService onCreateOutgoingConnectionFailed - no call UUID", new Object[0]);
        }

        this.unregisterPhoneAccount(theAccountHandle);
    }

    private void unregisterPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        TelecomManager telecom = (TelecomManager)this.getSystemService(TelecomManager.class);
        if (telecom != null) {
            if (phoneAccountHandle != null) {
                telecom.unregisterPhoneAccount(phoneAccountHandle);
            } else {
                JitsiMeetLogger.e("JitsiConnectionService unregisterPhoneAccount - account handle is null", new Object[0]);
            }
        } else {
            JitsiMeetLogger.e("JitsiConnectionService unregisterPhoneAccount - telecom is null", new Object[0]);
        }

    }

    static PhoneAccountHandle registerPhoneAccount(Context context, Uri address, String callUUID) {
        PhoneAccountHandle phoneAccountHandle = new PhoneAccountHandle(new ComponentName(context, ConnectionService.class), callUUID);
        Builder builder = PhoneAccount.builder(phoneAccountHandle, address.toString()).setAddress(address).setCapabilities(3080).addSupportedUriScheme("sip");
        PhoneAccount account = builder.build();
        TelecomManager telecomManager = (TelecomManager)context.getSystemService(TelecomManager.class);
        telecomManager.registerPhoneAccount(account);
        return phoneAccountHandle;
    }

    class ConnectionImpl extends Connection {
        static final String KEY_HAS_VIDEO = "hasVideo";

        ConnectionImpl() {
        }

        public void onDisconnect() {
            JitsiMeetLogger.i("JitsiConnectionService onDisconnect " + this.getCallUUID(), new Object[0]);
            WritableNativeMap data = new WritableNativeMap();
            data.putString("callUUID", this.getCallUUID());
            ReactInstanceManagerHolder.emitEvent("org.jitsi.meet:features/connection_service#disconnect", data);
            ConnectionService.setConnectionDisconnected(this.getCallUUID(), new DisconnectCause(2));
        }

        public void onAbort() {
            JitsiMeetLogger.i("JitsiConnectionService onAbort " + this.getCallUUID(), new Object[0]);
            WritableNativeMap data = new WritableNativeMap();
            data.putString("callUUID", this.getCallUUID());
            ReactInstanceManagerHolder.emitEvent("org.jitsi.meet:features/connection_service#abort", data);
            ConnectionService.setConnectionDisconnected(this.getCallUUID(), new DisconnectCause(4));
        }

        public void onHold() {
            JitsiMeetLogger.w("JitsiConnectionService onHold %s - HOLD is not supported, aborting the call...", new Object[]{this.getCallUUID()});
            this.onAbort();
        }

        public void onCallAudioStateChanged(CallAudioState state) {
            JitsiMeetLogger.d("JitsiConnectionService onCallAudioStateChanged: " + state, new Object[0]);
            RNConnectionService module = (RNConnectionService) ReactInstanceManagerHolder.getNativeModule(RNConnectionService.class);
            if (module != null) {
                module.onCallAudioStateChange(state);
            }

        }

        public void onStateChanged(int state) {
            JitsiMeetLogger.d("%s onStateChanged: %s %s", new Object[]{"JitsiConnectionService", Connection.stateToString(state), this.getCallUUID()});
            if (state == 6) {
                ConnectionService.removeConnection(this);
                ConnectionService.this.unregisterPhoneAccount(this.getPhoneAccountHandle());
            }

        }

        String getCallUUID() {
            return this.getPhoneAccountHandle().getId();
        }

        private PhoneAccountHandle getPhoneAccountHandle() {
            return (PhoneAccountHandle)this.getExtras().getParcelable("connection_service.PHONE_ACCOUNT_HANDLE");
        }

        public String toString() {
            return String.format("ConnectionImpl[address=%s, uuid=%s]@%d", this.getAddress(), this.getCallUUID(), this.hashCode());
        }
    }
}
