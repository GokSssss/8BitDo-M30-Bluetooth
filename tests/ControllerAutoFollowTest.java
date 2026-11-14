import app.m30.companion.*;

public final class ControllerAutoFollowTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static int target(int page,int slot,boolean activation){return ControllerAutoFollow.page(page,slot,true,true,true,activation);}
    public static void main(String[] args){
        check(target(0,1,ControllerAutoFollow.key(true,0,false))==2,"Second pad press opens its own page");
        check(target(2,0,ControllerAutoFollow.key(true,0,false))==0,"First pad press returns to first page");
        check(target(2,0,ControllerAutoFollow.key(false,0,true))==2,"Releasing previously held first pad cannot steal second page");
        check(!ControllerAutoFollow.key(true,1,true)&&!ControllerAutoFollow.key(true,0,true),"Repeat or duplicate DOWN cannot bounce pages");
        check(target(1,0,true)==1&&target(1,1,true)==1,"Keyboard remains a universal input screen");
        check(ControllerAutoFollow.page(0,1,false,true,true,true)==0,"User can disable auto-follow");
        check(ControllerAutoFollow.page(0,1,true,false,true,true)==0,"Menus, overlays, calibration or missing window focus protect current UI");
        check(ControllerAutoFollow.page(0,1,true,true,false,true)==0&&target(0,-1,true)==0,"Remote, keyboard and unassigned input cannot select controller page");
        check(!ControllerAutoFollow.axis(1f,0)&&!ControllerAutoFollow.axis(null,.03f)&&!ControllerAutoFollow.axis(.8f,.81f),"Neutral, noise and held-axis updates do not switch");
        check(ControllerAutoFollow.axis(0f,-1)&&ControllerAutoFollow.axis(-1f,1)&&ControllerAutoFollow.axis(.6f,.8f),"Fresh direction and threshold crossings can follow D-pad/trigger");
        check(!ControllerAutoFollow.axis(null,Float.NaN)&&!ControllerAutoFollow.axis(0f,Float.POSITIVE_INFINITY),"Malformed axes cannot change pages");
        ControllerSession first=new ControllerSession(0),second=new ControllerSession(1);first.state.set("K:96",true);second.state.set("K:97",true);
        check(target(0,1,true)==2&&first.state.down.contains("K:96")&&second.state.down.contains("K:97"),"Page choice does not clear either pad's held input");
        System.out.println("PASS: controller auto-follow, reverse press, release/repeat/noise guards, raw tab, modal and source isolation");
    }
}
