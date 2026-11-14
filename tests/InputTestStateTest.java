import app.m30.companion.*;

public class InputTestStateTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static InputMonitor.Key key(int device,int code,boolean down,int repeat){return new InputMonitor.Key(device,"Keyboard "+device,code,"KEYCODE_"+code,code,down,repeat,0x101);}
    public static void main(String[] args){
        InputTestState view=new InputTestState();ControllerBank bank=new ControllerBank(0);
        check(!view.hasEvent(),"no invented input before first event");
        view.record(key(7,29,true,0));view.record(key(7,29,true,1));
        check(view.presses==1&&view.lastKey.repeat==1&&view.heldText().contains("29"),"repeat is visible but does not inflate press count");
        view.record(key(8,29,true,0));view.record(key(7,29,false,0));
        check(view.heldText().contains("ID 8")&&!view.heldText().contains("ID 7"),"same key on two devices releases independently");
        check(view.lastKey.code==29&&!view.lastKey.down,"release retains readable raw key");
        check(!view.axis(9,"Stick",1,"AXIS_Y",0),"idle axes cannot overwrite keyboard event");
        check(view.axis(9,"Stick",1,"AXIS_Y",-1)&&view.lastWasAxis&&view.deviceId()==9,"foreign controller axis has its own source");
        check(view.axis(9,"Stick",1,"AXIS_Y",0)&&view.axisValue==0,"return to neutral remains readable");
        check(!view.axis(9,"Stick",1,"AXIS_Y",0)&&!view.axis(9,"Stick",1,"AXIS_Y",Float.NaN),"unchanged/invalid axes do not replace last event");
        view.removeDevice(8);check(view.heldText().equals("Нет удерживаемых клавиш"),"unplug clears held keys from that device");
        view.record(key(-1,66,true,0));check(view.deviceId()==-1&&view.lastKey.code==66,"virtual events are displayed without assigning a controller");
        view.release();check(view.heldText().equals("Нет удерживаемых клавиш")&&view.hasEvent(),"focus loss clears held state but keeps last code readable");
        check(!bank.slots[0].assigned&&!bank.slots[1].assigned&&bank.slots[0].state.presses==0&&bank.slots[1].state.presses==0,"universal test cannot assign devices or contaminate controller counters");
        for(int n=0;n<300;n++)view.record(key(7,n,true,0));check(view.heldText().split("ID 7",-1).length-1==128,"lost releases cannot grow held display indefinitely");
        System.out.println("PASS: universal keyboard/controller input, source isolation, repeats, neutral axes, virtual keys, unplug/focus release and bounded held state");
    }
}
