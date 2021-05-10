package co.ritech.cloudmeet.jitsi;


import android.app.Activity;
import androidx.annotation.Nullable;
import com.calendarevents.CalendarEventsPackage;
import com.corbt.keepawake.KCKeepAwakePackage;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.common.LifecycleState;
import com.facebook.react.devsupport.DevInternalSettings;
import com.facebook.react.jscexecutor.JSCExecutorFactory;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.soloader.SoLoader;
import com.horcrux.svg.SvgPackage;
import com.kevinresol.react_native_default_preference.RNDefaultPreferencePackage;
import com.ocetnik.timer.BackgroundTimerPackage;
import com.oney.WebRTCModule.RTCVideoViewManager;
import com.oney.WebRTCModule.WebRTCModule;
import com.oney.WebRTCModule.WebRTCModule.Options;
import com.reactnativecommunity.asyncstorage.AsyncStoragePackage;
import com.reactnativecommunity.netinfo.NetInfoPackage;
import com.reactnativecommunity.webview.RNCWebViewPackage;
import com.rnimmersive.RNImmersivePackage;
import com.zmxv.RNSound.RNSoundPackage;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.devio.rn.splashscreen.SplashScreenModule;
import org.jitsi.meet.sdk.net.NAT64AddrInfoModule;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

public class ReactInstanceManagerHolder {
    private static ReactInstanceManager reactInstanceManager;

    ReactInstanceManagerHolder() {
    }


    private static List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Arrays.asList(new RTCVideoViewManager());
    }

    static void emitEvent(String eventName, @Nullable Object data) {
        ReactInstanceManager reactInstanceManager = getReactInstanceManager();
        if (reactInstanceManager != null) {
            ReactContext reactContext = reactInstanceManager.getCurrentReactContext();
            if (reactContext != null) {
                ((RCTDeviceEventEmitter)reactContext.getJSModule(RCTDeviceEventEmitter.class)).emit(eventName, data);
            }
        }

    }

    static <T extends NativeModule> T getNativeModule(Class<T> nativeModuleClass) {
        ReactContext reactContext = reactInstanceManager != null ? reactInstanceManager.getCurrentReactContext() : null;
        return reactContext != null ? reactContext.getNativeModule(nativeModuleClass) : null;
    }

    static Activity getCurrentActivity() {
        ReactContext reactContext = reactInstanceManager != null ? reactInstanceManager.getCurrentReactContext() : null;
        return reactContext != null ? reactContext.getCurrentActivity() : null;
    }

    static ReactInstanceManager getReactInstanceManager() {
        return reactInstanceManager;
    }

}

