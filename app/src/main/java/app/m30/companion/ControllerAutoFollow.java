package app.m30.companion;

/** Page selection follows fresh controller activity, never releases or a raw-input tab. */
public final class ControllerAutoFollow {
    private ControllerAutoFollow(){}
    public static boolean key(boolean down,int repeat,boolean alreadyHeld){return down&&repeat==0&&!alreadyHeld;}
    public static boolean axis(Float previous,float value){
        if(!Float.isFinite(value)||Math.abs(value)<.7f)return false;
        return previous==null||!Float.isFinite(previous)||Math.abs(previous)<.7f||Math.signum(previous)!=Math.signum(value);
    }
    public static int page(int current,int slot,boolean enabled,boolean mainWindowReady,boolean physicalGamepad,boolean freshActivity){
        if(!enabled||!mainWindowReady||!physicalGamepad||!freshActivity||(current!=0&&current!=2)||slot<0||slot>1)return current;
        return slot==0?0:2;
    }
}
