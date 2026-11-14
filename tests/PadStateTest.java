import app.m30.companion.PadState;

public class PadStateTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        PadState s=new PadState();s.bind("A","K:96");s.bind("B","K:97");
        check(s.set("K:96",true),"first press changes state");
        check(!s.set("K:96",true),"repeat must not inflate counters");
        check(s.presses==1&&s.testedCount()==1,"first press counted once");
        s.set("K:97",true);check(s.active("A")&&s.active("B"),"simultaneous keys retained");
        s.set("K:96",false);check(!s.active("A")&&s.tested("A"),"release preserves coverage");
        s.release();check(s.down.isEmpty()&&s.testedCount()==2,"focus loss clears held buttons only");
        s.set("K:96",true);check(s.counts.get("K:96")==2,"second physical press counted");
        s.bind("C","K:96");check(!s.bindings.containsKey("A")&&s.active("C"),"binding reassignment cannot light two physical buttons");
        s.bind("↑","A:16:-");s.bind("←","A:15:-");s.set("A:16:-",true);s.set("A:15:-",true);
        check(s.active("↑")&&s.active("←"),"axis diagonal retains both directions");
        s.bind("↑","D:19");s.bind("←","D:21");
        check(s.active("↑")&&s.active("←"),"automatic D-pad aliases follow HAT");
        s.set("K:19",true);s.set("A:16:-",false);check(s.active("↑"),"one input source cannot release another held source");
        s.set("K:19",false);check(!s.active("↑"),"last source release clears direction");
        s.set("A:14:-",true);check(s.active("↑"),"switching D-pad to right stick needs no recalibration");
        s.set("A:14:-",false);s.set("T:14:+",true);check(!s.active("↑"),"unipolar triggers cannot become D-pad input");
        check(s.rawDown().stream().noneMatch(t->t.startsWith("D:")),"raw input excludes synthesized direction aliases");
        s.bind("A","K:21");check(!s.bindings.containsKey("←")&&s.active("A"),"direction calibration shares canonical identity with automatic D-pad");
        check(PadState.normalize(.10f,.05f)==0,"small axis noise filtered");
        check(PadState.normalize(.30f,.40f)==0,"device deadzone honored");
        check(PadState.normalize(-1,0)==-1,"negative direction retained");
        s.reset();check(s.seen.isEmpty()&&s.down.isEmpty()&&s.counts.isEmpty()&&s.presses==0,"reset clears session");
        check(s.bindings.containsKey("C"),"reset preserves calibration");
        System.out.println("PASS: press/release, repeats, simultaneous keys, focus loss, calibration conflicts, diagonal axes, deadzones, reset");
    }
}
