import app.m30.companion.BluetoothDeviceLabel;

public final class BluetoothDeviceLabelTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        String system="8BitDo M30 gamepad";
        BluetoothDeviceLabel label=new BluetoothDeviceLabel("  Android\nTV\t ",2);
        check(label.title.equals("Android TV")&&label.displayName(system).equals("[2] Android TV"),"Local label must clean whitespace without changing device identity");
        check(system.equals("8BitDo M30 gamepad"),"Android's name remains separate");
        check(new BluetoothDeviceLabel("",1).displayName(system).equals("[1] "+system),"Number-only label retains system name");
        check(new BluetoothDeviceLabel("",2).displayName("[2] Белый").equals("[2] Белый"),"Reading an OS alias cannot duplicate its already saved number");
        check(new BluetoothDeviceLabel("",0).displayName(system).equals(system),"Removing label restores system name");
        check(new BluetoothDeviceLabel(null,100).empty(),"Invalid badge and missing label are harmless");
        check(new BluetoothDeviceLabel("TV",99).displayName(system).equals("[99] TV"),"Custom two-digit numbers remain available");
        check(BluetoothDeviceLabel.number("42")==42&&BluetoothDeviceLabel.number(" 8 ")==8&&BluetoothDeviceLabel.number("")==0&&BluetoothDeviceLabel.number("100")==0&&BluetoothDeviceLabel.number("bad")==0,"Number entry handles empty, custom and invalid values");
        String emoji="😀".repeat(40);BluetoothDeviceLabel longName=new BluetoothDeviceLabel(emoji,3);
        check(longName.title.codePointCount(0,longName.title.length())==32&&!Character.isHighSurrogate(longName.title.charAt(longName.title.length()-1)),"Length limit cannot split Unicode characters");
        check(new BluetoothDeviceLabel("Windows",0).displayName(system).equals("Windows"),"A mode label is user text, not a new system name or firmware readout");
        System.out.println("PASS: app-only label/number formatting, reset, unknown values and Unicode limits");
    }
}
