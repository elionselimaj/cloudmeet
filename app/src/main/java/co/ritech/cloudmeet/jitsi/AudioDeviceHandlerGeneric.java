package co.ritech.cloudmeet.jitsi;


import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioFocusRequest.Builder;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Handler;
import android.os.Build.VERSION;
import java.util.HashSet;
import java.util.Set;

import org.jitsi.meet.sdk.log.JitsiMeetLogger;

class AudioDeviceHandlerGeneric implements AudioModeModule.AudioDeviceHandlerInterface, OnAudioFocusChangeListener {
    private static final String TAG = AudioDeviceHandlerGeneric.class.getSimpleName();
    private AudioModeModule module;
    private static final int TYPE_USB_HEADSET = 22;
    private boolean audioFocusLost = false;
    private AudioManager audioManager;
    private final Runnable onAudioDeviceChangeRunner = new Runnable() {
        public void run() {
            Set<String> devices = new HashSet();
            AudioDeviceInfo[] deviceInfos = AudioDeviceHandlerGeneric.this.audioManager.getDevices(3);
            AudioDeviceInfo[] var3 = deviceInfos;
            int var4 = deviceInfos.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                AudioDeviceInfo info = var3[var5];
                switch(info.getType()) {
                    case 1:
                        devices.add("EARPIECE");
                        break;
                    case 2:
                        devices.add("SPEAKER");
                        break;
                    case 3:
                    case 4:
                    case 22:
                        devices.add("HEADPHONES");
                        break;
                    case 7:
                        devices.add("BLUETOOTH");
                }
            }

            AudioDeviceHandlerGeneric.this.module.replaceDevices(devices);
            JitsiMeetLogger.i(AudioDeviceHandlerGeneric.TAG + " Available audio devices: " + devices.toString(), new Object[0]);
            AudioDeviceHandlerGeneric.this.module.updateAudioRoute();
        }
    };
    private final AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            JitsiMeetLogger.d(AudioDeviceHandlerGeneric.TAG + " Audio devices added", new Object[0]);
            AudioDeviceHandlerGeneric.this.onAudioDeviceChange();
        }

        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            JitsiMeetLogger.d(AudioDeviceHandlerGeneric.TAG + " Audio devices removed", new Object[0]);
            AudioDeviceHandlerGeneric.this.onAudioDeviceChange();
        }
    };

    public AudioDeviceHandlerGeneric(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    private void onAudioDeviceChange() {
        this.module.runInAudioThread(this.onAudioDeviceChangeRunner);
    }

    public void onAudioFocusChange(final int focusChange) {
        this.module.runInAudioThread(new Runnable() {
            public void run() {
                switch(focusChange) {
                    case -3:
                    case -2:
                    case -1:
                        JitsiMeetLogger.d(AudioDeviceHandlerGeneric.TAG + " Audio focus lost", new Object[0]);
                        AudioDeviceHandlerGeneric.this.audioFocusLost = true;
                    case 0:
                    default:
                        break;
                    case 1:
                        JitsiMeetLogger.d(AudioDeviceHandlerGeneric.TAG + " Audio focus gained", new Object[0]);
                        if (AudioDeviceHandlerGeneric.this.audioFocusLost) {
                            AudioDeviceHandlerGeneric.this.module.resetAudioRoute();
                        }

                        AudioDeviceHandlerGeneric.this.audioFocusLost = false;
                }

            }
        });
    }

    private void setBluetoothAudioRoute(boolean enabled) {
        if (enabled) {
            this.audioManager.startBluetoothSco();
            this.audioManager.setBluetoothScoOn(true);
        } else {
            this.audioManager.setBluetoothScoOn(false);
            this.audioManager.stopBluetoothSco();
        }

    }

    public void start(AudioModeModule audioModeModule) {
        JitsiMeetLogger.i("Using " + TAG + " as the audio device handler", new Object[0]);
        this.module = audioModeModule;
        this.audioManager.registerAudioDeviceCallback(this.audioDeviceCallback, (Handler)null);
        this.onAudioDeviceChange();
    }

    public void stop() {
        this.audioManager.unregisterAudioDeviceCallback(this.audioDeviceCallback);
    }

    public void setAudioRoute(String device) {
        this.audioManager.setSpeakerphoneOn(device.equals("SPEAKER"));
        this.setBluetoothAudioRoute(device.equals("BLUETOOTH"));
    }

    public boolean setMode(int mode) {
        if (mode == 0) {
            this.audioFocusLost = false;
            this.audioManager.setMode(0);
            this.audioManager.abandonAudioFocus(this);
            this.audioManager.setSpeakerphoneOn(false);
            this.setBluetoothAudioRoute(false);
            return true;
        } else {
            this.audioManager.setMode(3);
            this.audioManager.setMicrophoneMute(false);
            int gotFocus;
            if (VERSION.SDK_INT >= 26) {
                gotFocus = this.audioManager.requestAudioFocus((new Builder(1)).setAudioAttributes((new android.media.AudioAttributes.Builder()).setUsage(2).setContentType(1).build()).setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(this).build());
            } else {
                gotFocus = this.audioManager.requestAudioFocus(this, 0, 1);
            }

            if (gotFocus == 0) {
                JitsiMeetLogger.w(TAG + " Audio focus request failed", new Object[0]);
                return false;
            } else {
                return true;
            }
        }
    }
}

