package app.m30.companion;

import android.annotation.TargetApi;
import android.companion.CompanionDeviceManager;

/** Public Android16 API; compiled with the existing official SDK37 bridge classpath. */
@TargetApi(36)
public final class BluetoothForget36 {
    private BluetoothForget36(){}
    public static boolean start(CompanionDeviceManager manager,int associationId){return manager.removeBond(associationId);}
}
