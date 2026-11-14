package app.m30.companion;
import java.util.Locale;
/** Eligibility for the USB diagnostics entry, not proof of a flashable model. */
public final class UsbDeviceIdentity {
    private UsbDeviceIdentity(){}
    public static boolean firmwareCandidate(int vendor,int product,String name){
        if(vendor==0x2dc8&&(product==0x5006||product==0x5750||product==0x3208))return true;
        boolean m30=name!=null&&name.toLowerCase(Locale.ROOT).matches(".*\\bm30\\b.*");
        return m30&&(vendor==0x2dc8||vendor==0x045e&&product==0x028e);
    }
}
