import app.m30.companion.*;
import java.util.*;

public class ControllerConnectionRequestTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static SourceSelector.Source pad(int id,String descriptor){return new SourceSelector.Source(id,descriptor,"8BitDo M30",true,true);}
    public static void main(String[] args){
        ControllerConnectionRequest request=new ControllerConnectionRequest();long now=1000;
        request.begin("AA:BB:CC:DD:EE:FF",1,now);
        ControllerBank bank=new ControllerBank(0);SourceSelector.Source a=pad(23,"pad-a");
        bank.reconcile(List.of(a),"",!request.active(now));
        check(!bank.slots[0].connected()&&!bank.slots[1].connected(),"enumeration alone must not put a slot-2 request into slot 1");
        check(!request.confirms(false,true,0,now),"remote cannot confirm a gamepad identity");
        check(!request.confirms(true,false,0,now)&&!request.confirms(true,true,1,now),"release and repeat cannot confirm");
        check(request.confirms(true,true,0,now),"first physical gamepad press confirms the chosen slot");
        bank.moveTo(request.slot,a);request.clear();bank.reconcile(List.of(a),"");
        check(bank.indexOf(23)==1&&!bank.slots[0].connected(),"only the requested slot receives the confirmed source");
        ControllerSession moving=bank.slots[1];moving.mode=3;moving.state.bind("A","K:96");moving.state.set("K:96",true);
        SourceSelector.Source b=pad(24,"pad-b");bank.reconcile(List.of(a,b),"");ControllerSession other=bank.slots[0];other.mode=1;
        bank.moveTo(0,a);
        check(bank.slots[0]==moving&&bank.slots[1]==other&&bank.indexOf(23)==0&&bank.indexOf(24)==1,"explicit reassignment swaps sessions without losing a connected peer");
        check(moving.mode==3&&moving.state.active("A")&&other.mode==1,"bindings, first press and profiles survive a move");
        bank.reconcile(List.of(pad(40,"pad-a"),pad(41,"pad-b")),"",false);
        check(bank.indexOf(40)==0&&bank.indexOf(41)==1,"known descriptors reconnect even while unrelated automatic assignment is held");
        request.begin("AA:BB:CC:DD:EE:01",0,now);check(!request.active(now+120_000)&&request.slot==-1,"expired request cannot later capture input");
        request.begin("AA:BB:CC:DD:EE:01",0,now);request.clear();check(!request.confirms(true,true,0,now),"closing connection cancels confirmation");
        boolean rejected=false;try{request.begin("same name",1,now);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"a device name cannot replace an exact Bluetooth address");
        System.out.println("PASS: requested slot 2, physical confirmation, remote/repeat/release isolation, session swap, reconnect and cancellation");
    }
}
