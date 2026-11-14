package app.m30.companion;

import java.util.ArrayDeque;

/** Foreground diagnostics kept separate from the selected controller's test state. */
public final class InputMonitor {
    public static final class Key {
        public final int deviceId, code, scan, repeat, source;
        public final String deviceName, name;
        public final boolean down;
        public Key(int deviceId,String deviceName,int code,String name,int scan,boolean down,int repeat,int source){
            this.deviceId=deviceId;this.deviceName=deviceName;this.code=code;this.name=name;this.scan=scan;this.down=down;this.repeat=repeat;this.source=source;
        }
        public String summary(){return (down?"↓ ":"↑ ")+code+" · "+name+" · "+deviceName+" [ID "+deviceId+"] · scan "+scan+(repeat>0?" · повтор "+repeat:"");}
    }
    public Key lastSelected;
    public String lastOther;
    public final ArrayDeque<String> otherEvents=new ArrayDeque<>();
    public boolean otherEnabled;
    public boolean record(Key key,int selectedId){
        if(selectedId>=0 && key.deviceId==selectedId){lastSelected=key;return true;}
        if(otherEnabled)other(key.summary());
        return false;
    }
    public void other(String summary){
        if(!otherEnabled)return;
        lastOther=summary;otherEvents.addFirst(summary);while(otherEvents.size()>20)otherEvents.removeLast();
    }
    public void setOtherEnabled(boolean enabled){otherEnabled=enabled;if(!enabled){lastOther=null;otherEvents.clear();}}
    public void clearSelected(){lastSelected=null;}
}
