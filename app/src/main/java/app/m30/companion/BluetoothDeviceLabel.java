package app.m30.companion;

/** App-only labels do not rename Android devices, merge identities or change modes. */
public final class BluetoothDeviceLabel {
    public static final int MAX_BADGE=99;
    public final String title;
    public final int badge;
    public BluetoothDeviceLabel(String title,int badge){
        String clean=BluetoothGamepadFilter.cleanName(title);
        this.title=clean.substring(0,clean.offsetByCodePoints(0,Math.min(32,clean.codePointCount(0,clean.length()))));
        this.badge=badge>=1&&badge<=MAX_BADGE?badge:0;
    }
    public static int number(String text){try{int value=Integer.parseInt(text.trim());return value>=1&&value<=MAX_BADGE?value:0;}catch(NumberFormatException|NullPointerException ignored){return 0;}}
    public String displayName(String systemName){String prefix=badge==0?"":"["+badge+"] ";String name=title.isEmpty()?BluetoothGamepadFilter.cleanName(systemName):title;return title.isEmpty()&&!prefix.isEmpty()&&name.startsWith(prefix)?name:prefix+name;}
    public boolean empty(){return title.isEmpty()&&badge==0;}
}
