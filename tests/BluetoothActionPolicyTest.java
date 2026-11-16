import app.m30.companion.BluetoothActionPolicy;
import static app.m30.companion.BluetoothActionPolicy.*;

public class BluetoothActionPolicyTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(rename(30,true,false)==UNAVAILABLE,"Android11 cannot silently pretend to rename the OS device");
        check(rename(31,true,false)==NATIVE&&rename(37,true,false)==NATIVE,"Eligible host starts with public OS renaming");
        check(rename(35,false,false)==UNAVAILABLE,"TV without companion setup needs explicit label alternative");
        check(rename(26,false,true)==LOCAL_LABEL&&rename(37,true,true)==LOCAL_LABEL,"Chosen label alternative persists even on a capable host");
        check(connect(36,true,true,false,false)==UNAVAILABLE,"Saved HID host connection is unavailable before API37");
        check(connect(37,false,true,false,false)==UNAVAILABLE,"API version alone is not enough without companion setup");
        check(connect(37,true,true,false,false)==NATIVE,"API37 with companion setup uses public connect");
        check(connect(26,false,false,false,false)==PAIR,"New pairing remains native on older Android");
        check(connect(37,true,false,true,false)==EXTERNAL&&connect(26,false,true,true,false)==EXTERNAL,"Only explicit external choice bypasses native actions");
        check(connect(26,false,true,false,true)==CONNECTED,"Confirmed input must not be sent to settings just because its OS is old");
        check(connect(37,true,true,true,true)==CONNECTED,"Already connected input wins over the saved fallback choice");
        check(disconnect(36,true,false)==UNAVAILABLE&&disconnect(37,true,false)==NATIVE,"Disconnect requires the public API37 companion path");
        check(disconnect(37,false,false)==UNAVAILABLE&&disconnect(37,true,true)==EXTERNAL,"Disconnect respects capabilities and explicit host choice");
        System.out.println("PASS: OS capabilities, explicit alternatives, new pairing, native API37 connection and confirmed-input precedence");
    }
}
