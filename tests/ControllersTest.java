import app.m30.companion.*;
import java.util.*;

public class ControllersTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static SourceSelector.Source pad(int id,String descriptor){return new SourceSelector.Source(id,descriptor,"8BitDo M30 gamepad",true,true);}
    public static void main(String[] args){
        ControllerBank empty=new ControllerBank(0);empty.reconcile(List.of(new SourceSelector.Source(-1,"virtual","Virtual",false,false)),"virtual");
        check(!empty.slots[0].connected()&&!empty.slots[1].connected()&&empty.forDevice(-1)==null,"Android virtual ID -1 is not a connected controller and must not be resolved through InputDevice.getDevice");
        ControllerBank bank=new ControllerBank(0);
        SourceSelector.Source a=pad(11,"m30-a"),b=pad(12,"m30-b"),phone=new SourceSelector.Source(3,"phone","gpio-keys",false,false);
        bank.reconcile(List.of(phone,b,a),a.descriptor);
        check(bank.slots[0].deviceId==11&&bank.slots[1].deviceId==12&&bank.dual(),"identical names with different descriptors occupy separate stable slots");
        ControllerSession first=bank.forDevice(11),second=bank.forDevice(12);
        first.state.bind("A","K:96");second.state.bind("B","K:96");first.mode=0;second.mode=1;
        bank.forDevice(11).state.set("K:96",true);bank.forDevice(12).state.set("K:96",true);
        check(first.state.active("A")&&second.state.active("B"),"simultaneous equal keycodes follow their own device bindings");
        bank.forDevice(11).state.set("K:96",false);
        check(!first.state.active("A")&&first.state.tested("A")&&second.state.active("B"),"release on a hidden controller does not release the selected controller");
        first.monitor.record(new InputMonitor.Key(11,"M30",96,"BUTTON_A",1,true,0,0),11);
        check(second.monitor.lastSelected==null,"last key and diagnostics are isolated");
        bank.reconcile(List.of(a,b,phone),"");
        check(bank.slots[0]==first&&bank.slots[1]==second&&first.mode==0&&second.mode==1,"enumeration order cannot swap slots or profiles");
        bank.reconcile(List.of(a),"");
        check(!second.connected()&&first.connected()&&!second.state.active("B")&&first.state.tested("A")&&bank.dual(),"disconnect releases only that controller and keeps its visible slot");
        bank.reconcile(List.of(pad(22,"m30-b"),a),"");
        check(bank.forDevice(22)==second&&bank.forDevice(12)==null&&second.state.tested("B"),"same descriptor reconnects to the same slot under a new ID with prior coverage");
        bank.releaseAll();check(first.state.down.isEmpty()&&second.state.down.isEmpty(),"focus loss releases both controllers");
        ControllerBank logical=new ControllerBank(0);
        SourceSelector.Source keys=new SourceSelector.Source(31,"same-m30","8BitDo M30 gamepad",false,true);
        logical.reconcile(List.of(keys,pad(32,"same-m30"),phone),"");
        check(!logical.dual()&&logical.forDevice(31)==logical.forDevice(32),"two logical sources of one controller do not become joystick 1 and 2");
        check(logical.slots[0].deviceId==32,"gamepad interface is preferred for status and battery");
        logical.reconcile(List.of(phone),"");check(logical.forDevice(31)==null&&logical.forDevice(32)==null,"removed logical sources cannot keep controlling a stale slot");
        ControllerBank restored=new ControllerBank(0);restored.restore(0,"m30-a");restored.restore(1,"m30-b");restored.reconcile(List.of(pad(50,"m30-b")),"");
        check(restored.slots[0].deviceId==-1&&restored.slots[1].deviceId==50,"reopening with only joystick 2 connected keeps its number");
        ControllerSession reused=new ControllerSession(0);reused.connect(a);reused.confirmation.confirm(0,"old");reused.state.set("K:96",true);reused.connect(pad(a.id,"new-device"));
        check(reused.state.seen.isEmpty()&&reused.confirmation.current()==-1,"reused device ID with a different identity cannot inherit the old test or confirmation");
        ControllerBank noDescriptor=new ControllerBank(0);noDescriptor.reconcile(List.of(pad(70,""),pad(71,"")),"");
        check(noDescriptor.dual()&&noDescriptor.forDevice(70)!=noDescriptor.forDevice(71),"missing descriptors cannot merge independent device IDs");
        check(SwipeNavigation.target(0,-120,0,40,true)==2&&SwipeNavigation.target(2,-120,0,40,true)==1,"dual swipe order is first controller, second controller, buttons");
        check(SwipeNavigation.target(1,120,0,40,true)==2&&SwipeNavigation.target(2,120,0,40,true)==0,"swiping back visits both controller pages");
        check(SwipeNavigation.target(0,120,0,40,true)==0&&SwipeNavigation.target(1,-120,0,40,true)==1,"dual pages do not wrap at their edges");
        check(SwipeNavigation.target(2,20,160,40,true)==2,"vertical scrolling on second controller does not change page");
        ControllerBank tv=new ControllerBank(0);tv.restore(1,"tv-helper");SourceSelector.Source helper=new SourceSelector.Source(4,"tv-helper","virtual-search",true,true);tv.reconcile(List.of(helper,a),"");
        check(tv.forDevice(4)==null&&tv.slots[1].descriptor.isEmpty()&&tv.forDevice(a.id)!=null,"TV virtual-search cannot occupy or restore a controller slot even with gamepad flags");
        check(!SourceSelector.controller(helper)&&!SourceSelector.discoverable(helper)&&!SourceSelector.blockNavigation(helper,true,true),"System TV helper must keep controlling menus, not the M30 lock");
        System.out.println("PASS: two identical controllers, stable numbering, independent simultaneous input, hidden release, reconnect, focus loss, logical-source grouping, dual swipes");
    }
}
