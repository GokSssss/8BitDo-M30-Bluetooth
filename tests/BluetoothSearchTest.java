import app.m30.companion.BluetoothGamepadFilter;
import app.m30.companion.BluetoothPairingState;

public final class BluetoothSearchTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void filter(String name,int cls,int expected){check(BluetoothGamepadFilter.classify(name,cls)==expected,"Filter: "+name+" class "+cls);}
    public static void main(String[] args){
        filter("8BitDo M30 gamepad",0,BluetoothGamepadFilter.M30);
        filter("M30",0x0508,BluetoothGamepadFilter.M30);
        filter("8bitdo m30",0x0500,BluetoothGamepadFilter.M30);
        filter("Pro Controller",0,BluetoothGamepadFilter.GAMEPAD);
        filter("Xbox Wireless Controller",0,BluetoothGamepadFilter.GAMEPAD);
        filter("Wireless Controller",0,BluetoothGamepadFilter.GAMEPAD);
        filter("8BitDo Ultimate",0,BluetoothGamepadFilter.GAMEPAD);
        filter(null,0x0508,BluetoothGamepadFilter.GAMEPAD);
        filter("Без имени",0x0504,BluetoothGamepadFilter.GAMEPAD);
        filter("Hybrid",0x05c8,BluetoothGamepadFilter.GAMEPAD);
        filter("8BitDo Retro Keyboard",0x0540,BluetoothGamepadFilter.NONE);
        filter("8BitDo mouse",0x0580,BluetoothGamepadFilter.NONE);
        filter("Xbox Headset",0x0404,BluetoothGamepadFilter.NONE);
        filter("M30 Speaker",0x0414,BluetoothGamepadFilter.NONE);
        filter("My Phone",0x020c,BluetoothGamepadFilter.NONE);
        filter("Laptop",0x010c,BluetoothGamepadFilter.NONE);
        filter("Samsung TV",0x043c,BluetoothGamepadFilter.NONE);
        filter("Remote",0x050c,BluetoothGamepadFilter.NONE);
        filter("8BitDo",0x0500,BluetoothGamepadFilter.NONE);
        filter(null,0,BluetoothGamepadFilter.NONE);
        check(BluetoothGamepadFilter.cleanName("\nM30\tgamepad\u0000").equals("M30 gamepad"),"Control chars sanitized");
        check(BluetoothGamepadFilter.cleanName("x".repeat(200)).length()==100,"Bounded display name");
        BluetoothPairingState p=new BluetoothPairingState();String a="11:22:33:44:55:66",b="11:22:33:44:55:77";
        p.select(a,1000);check(!p.bondEvent(b,12)&&p.phase()==1,"Another device cannot complete selected pairing");
        check(!p.poll(a,10)&&p.phase()==1,"Async createBond does not fail from initial NONE poll");
        check(p.bondEvent(a,11)&&p.sawBonding(),"Observed pairing");check(p.bondEvent(a,12)&&p.phase()==2,"Bonded is paired only");
        check(!p.expire(200000)&&!p.bondEvent(a,10)&&p.phase()==2,"Old timeout/broadcast cannot overwrite completion");
        p.select(a,0);check(p.bondEvent(a,10)&&p.phase()==3,"Cancellation reported even after missed BONDING event");
        p.select(a,0);p.poll(a,11);check(p.poll(a,10)&&p.phase()==3,"Resume notices cancellation after known bonding");
        p.select(a,500);check(!p.expire(90499)&&p.expire(90500),"Bounded pairing timeout");
        p.restore(a,1,1000000,false,100);check(p.deadline()==90100,"Restored timeout bounded");p.expire(90100);check(p.phase()==3,"Expired resume is unknown, not success");
        p.select(a,0);p.fail();check(p.phase()==3,"Permission loss releases pending state");
        System.out.println("PASS: gamepad-only discovery hints, explicit M30, generic candidates, keyboard/audio/phone exclusion, sanitized names, pairing correlation/cancel/resume/timeout; bonding never claims HID ready");
    }
}
