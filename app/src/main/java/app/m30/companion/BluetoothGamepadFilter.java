package app.m30.companion;

import java.util.Locale;
import java.util.regex.Pattern;

/** Discovery hints only: name/class cannot authenticate the physical controller model. */
public final class BluetoothGamepadFilter {
    public static final int NONE=0,GAMEPAD=1,M30=2;
    private static final Pattern MODEL=Pattern.compile("(?i)\\bm30\\b");
    private static final Pattern OTHER_8BITDO=Pattern.compile("(?i)\\b(sn30|sf30|sfc30|fc30|n30|pro\\s*2|ultimate|lite|micro|zero\\s*2)\\b");
    private BluetoothGamepadFilter(){}
    public static int classify(String name,int deviceClass){
        String n=cleanName(name).toLowerCase(Locale.ROOT);
        if(n.matches(".*\\b(keyboard|mouse|headphones?|headset|speaker|earbuds?|soundbar|клавиатура|мышь|наушники|колонка)\\b.*"))return NONE;
        // Preserve the peripheral major class, mask the optional keyboard/pointing flags.
        boolean peripheral=(deviceClass&0x1f00)==0x0500;
        int minor=deviceClass&0x003c;
        boolean controllerClass=peripheral&&(minor==0x04||minor==0x08);
        if(MODEL.matcher(n).find()&&(n.contains("8bitdo")||n.contains("8bit do")||n.equals("m30")||n.contains("gamepad")))return M30;
        boolean controllerName=n.contains("gamepad")||n.contains("joystick")||n.contains("xbox")
            ||n.equals("pro controller")||n.equals("wireless controller")||n.contains("dualsense")||n.contains("dualshock")
            ||n.contains("8bitdo")&&OTHER_8BITDO.matcher(n).find();
        return controllerClass||controllerName?GAMEPAD:NONE;
    }
    public static String cleanName(String name){
        if(name==null)return "";String cleaned=name.replaceAll("[\\p{Cntrl}\\s]+"," ").trim();
        return cleaned.length()>100?cleaned.substring(0,100):cleaned;
    }
}
