package app.m30.companion;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;

/** Compiled against the official API37 SDK; called only on API37+ after companion consent. */
@TargetApi(37)
public final class BluetoothConnect37 {
    private BluetoothConnect37(){}
    public static int start(BluetoothDevice device){return device.connect();}
}
