import app.m30.companion.BluetoothScanPolicy;
import static app.m30.companion.BluetoothScanPolicy.*;

public class BluetoothScanPolicyTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(path(26,false,false)==OFFER_EXTERNAL&&path(30,false,false)==OFFER_EXTERNAL,"Old hosts without location must offer usable system search before location permission");
        check(path(28,false,true)==NATIVE,"Old hosts with location can use explicit Classic discovery");
        check(path(31,false,false)==NATIVE&&path(37,false,false)==NATIVE,"Nearby-device discovery must not be gated by old location rules");
        check(path(26,true,false)==EXTERNAL&&path(37,true,true)==EXTERNAL,"Saved user choice bypasses native scan and location access on every host");
        System.out.println("PASS: old-TV scan fallback, modern Nearby permissions and persistent external-search choice");
    }
}
