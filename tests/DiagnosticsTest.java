import app.m30.companion.*;

public class DiagnosticsTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static InputMonitor.Key key(int id,int code,int scan,boolean down){return new InputMonitor.Key(id,"Device "+id,code,"KEY_"+code,scan,down,0,0x401);}
    public static void main(String[] args){
        check(ModeDetector.profileOnConnect(null,0,"8BitDo M30 gamepad",0x045e,0x02e0)==1,"An unseen Windows peer must get its own initial profile before the first auto-follow press");
        check(ModeDetector.profileOnConnect(null,1,"8BitDo M30 gamepad",0x2dc8,0x0651)==0,"An unseen Android peer cannot inherit the first pad's Windows profile");
        check(ModeDetector.profileOnConnect(0,1,"8BitDo M30 gamepad",0x045e,0x02e0)==0,"Saved manual per-device profile overrides an estimate");
        check(ModeDetector.profileOnConnect(null,3,"Unknown",0,0)==3&&ModeDetector.profileOnConnect(99,99,null,0,0)==0,"Unknown identities retain a valid fallback; invalid settings cannot become a profile");
        check(ModeDetector.detect(null,0,0).candidate==-1,"no device means no mode estimate");
        check(ModeDetector.detect("8BitDo M30 gamepad",0x2dc8,0x0651).candidate==0,"observed Bluetooth M30 ID is an Android/D-input compatibility hint");
        check(ModeDetector.detect("8BitDo M30",0,0).candidate==-1,"missing VID/PID cannot identify a mode");
        check(ModeDetector.detect("Wireless Controller",0,0).candidate==-1,"generic Bluetooth names do not prove macOS mode");
        check(ModeDetector.detect("Pro Controller",0x057e,0x2009).candidate==2,"Nintendo protocol identity is a Switch compatibility hint");
        check(ModeDetector.detect("Controller",0x045e,0x028e).candidate==1,"Xbox ID produces a compatibility hint");
        check(ModeDetector.detect("M30 D-input",0,0).candidate==0,"explicit mode name is usable evidence");
        check(ModeDetector.detect("8BitDo M30",0x2dc8,0x5006).candidate==-1,"USB/model IDs are not blindly treated as Android mode");
        check(ModeDetector.detect("Other controller",0x2dc8,0x0651).candidate==-1,"new hint requires an M30 name as well as the exact identity");
        InputMonitor monitor=new InputMonitor();
        check(monitor.record(key(2,110,316,true),2),"selected source is accepted regardless of its keyboard/gamepad category");
        check(monitor.lastSelected.code==110,"unbound HOME still has visible diagnostics");
        monitor.record(key(2,110,316,false),2);check(!monitor.lastSelected.down&&monitor.lastSelected.code==110,"release preserves key code for reading");
        check(!monitor.record(key(3,203,729,true),2)&&monitor.lastOther==null,"other sources stay out of selected diagnostics with monitor off");
        check(monitor.lastSelected.code==110,"unrelated key cannot overwrite selected device code");
        monitor.setOtherEnabled(true);monitor.record(key(3,203,729,true),2);
        check(monitor.lastOther.contains("ID 3")&&monitor.lastOther.contains("203"),"other device events identify source and raw code");
        check(monitor.lastSelected.code==110,"other monitor cannot overwrite M30 diagnostics");
        monitor.record(key(-1,96,0,true),-1);check(monitor.lastSelected.code==110,"no selected device must not accept virtual keyboard as M30");
        for(int n=0;n<60;n++)monitor.record(key(3,n,n,true),2);check(monitor.otherEvents.size()==20,"monitor history is bounded");
        monitor.setOtherEnabled(false);check(monitor.otherEvents.isEmpty()&&monitor.lastOther==null,"disabling monitor clears unrelated device history");
        PadState state=new PadState();state.bind("★","S:364");state.bind("HOME","S:365");state.set("S:364",true);
        check(state.active("★")&&!state.active("HOME"),"different scancodes distinguish unknown Android keys");
        state.set("S:364",false);check(!state.active("★")&&state.tested("★"),"calibrated STAR has ordinary press/release behavior");
        monitor.clearSelected();check(monitor.lastSelected==null,"changing selected device clears stale last key");
        System.out.println("PASS: mode evidence, ambiguous IDs, selected/other source isolation, unbound keys, retained release, bounded monitoring, unknown scancodes");
    }
}
