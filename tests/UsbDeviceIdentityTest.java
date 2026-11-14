import app.m30.companion.UsbDeviceIdentity;
public class UsbDeviceIdentityTest {
    private static void check(boolean b){if(!b)throw new AssertionError("USB eligibility mismatch");}
    public static void main(String[] args){
        check(UsbDeviceIdentity.firmwareCandidate(0x2dc8,0x3208,"8BitDo Boot"));
        check(UsbDeviceIdentity.firmwareCandidate(0x2dc8,0x5750,null));
        check(UsbDeviceIdentity.firmwareCandidate(0x2dc8,0x5006,null));
        check(UsbDeviceIdentity.firmwareCandidate(0x045e,0x028e,"8BitDo M30 gamepad"));
        check(!UsbDeviceIdentity.firmwareCandidate(0x045e,0x028e,"Xbox 360 Controller"));
        check(!UsbDeviceIdentity.firmwareCandidate(0x046d,0xc534,"USB Receiver"));
        check(!UsbDeviceIdentity.firmwareCandidate(0x2dc8,0x9999,"Other controller"));
        check(!UsbDeviceIdentity.firmwareCandidate(0x045e,0x028e,null));
        System.out.println("PASS: USB runtime/boot eligibility, observed M30 Xbox identity, unrelated USB excluded");
    }
}
