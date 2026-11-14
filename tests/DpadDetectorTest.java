import app.m30.companion.*;
import java.util.*;

public class DpadDetectorTest {
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    private static void axes(DpadDetector d,long time,Map<Integer,Float> values,Integer... centered){d.axes(values,new HashSet<>(Arrays.asList(centered)),false,time);}
    public static void main(String[] args){
        DpadDetector d=new DpadDetector();check(d.kind()==DpadDetector.Kind.UNKNOWN,"no firmware setting is invented before input");
        axes(d,1,Map.of(0,0f,1,.1f,17,1f,18,1f,22,1f,23,1f),0,1);
        check(d.kind()==DpadDetector.Kind.UNKNOWN,"noise, trigger and brake/gas axes cannot identify a direction mode");
        d.key(19,false,0,false,2);d.key(19,true,1,false,3);d.key(96,true,0,false,4);
        check(d.kind()==DpadDetector.Kind.UNKNOWN,"release, repeat and face buttons are not direction evidence");
        d.key(19,true,0,false,10);check(d.kind()==DpadDetector.Kind.DPAD&&d.evidence().contains("DPAD_UP"),"raw direction key estimates D-pad");
        axes(d,20,Map.of(15,-1f,16,-1f),15,16);check(d.kind()==DpadDetector.Kind.DPAD,"hat plus key is the same direction family");
        axes(d,500,Map.of(0,-1f,1,1f),0,1);check(d.kind()==DpadDetector.Kind.LEFT&&d.caption().contains("ЛЕВЫЙ"),"new XY press estimates left stick and updates caption");
        axes(d,550,Map.of(0,0f,1,0f),0,1);check(d.kind()==DpadDetector.Kind.LEFT,"neutral retains the last observation for reading");
        axes(d,1000,Map.of(11,-1f,14,1f),11,14);check(d.kind()==DpadDetector.Kind.RIGHT,"centered Z/RZ pair can carry right stick");
        d.reset();axes(d,1100,Map.of(11,-1f,14,-1f,12,0f,13,0f),11,14,12,13);
        check(d.kind()==DpadDetector.Kind.UNKNOWN,"resting Z/RZ trigger axes are ignored when RX/RY is the stick pair");
        axes(d,1200,Map.of(11,-1f,14,-1f,12,1f,13,0f),11,14,12,13);
        check(d.kind()==DpadDetector.Kind.RIGHT&&d.evidence().equals("AXIS_RX"),"RX/RY are recognized without misreading Z/RZ triggers");
        d.reset();axes(d,2000,Map.of(0,1f,15,1f),0,15);check(d.kind()==DpadDetector.Kind.MIXED,"mirrored hat and stick in one event must remain ambiguous");
        d.reset();d.key(22,true,0,false,2200);axes(d,2210,Map.of(0,1f),0);
        check(d.kind()==DpadDetector.Kind.MIXED,"nearby key and stick reports merge instead of flickering into a false certainty");
        axes(d,3000,Map.of(0,1f),0);check(d.kind()==DpadDetector.Kind.LEFT,"a later unambiguous direction can replace mixed evidence");
        d.key(21,true,0,true,3100);check(d.kind()==DpadDetector.Kind.UNKNOWN,"SELECT plus direction invalidates an old observation rather than claiming a setting changed");
        d.axes(Map.of(0,-1f),Set.of(0),true,3200);check(d.kind()==DpadDetector.Kind.UNKNOWN,"holding a settings chord is not a firmware readout");
        axes(d,4000,Map.of(0,-1f),0);check(d.kind()==DpadDetector.Kind.LEFT,"fresh direction after the chord can be tested again");
        d.endBurst();d.key(20,true,0,false,4010);check(d.kind()==DpadDetector.Kind.DPAD,"focus change ends the old report burst without merging unrelated windows");
        ControllerSession a=new ControllerSession(0),b=new ControllerSession(0);
        a.connect(new SourceSelector.Source(8,"a","8BitDo M30",true,true));b.connect(new SourceSelector.Source(9,"b","8BitDo M30",true,true));
        a.dpad.key(19,true,0,false,1);axes(b.dpad,1,Map.of(0,1f),0);
        check(a.dpad.kind()==DpadDetector.Kind.DPAD&&b.dpad.kind()==DpadDetector.Kind.LEFT,"two controllers own independent observations");
        a.release();check(a.dpad.kind()==DpadDetector.Kind.DPAD,"opening an in-app popup keeps readable observation");
        a.connect(null);check(a.dpad.kind()==DpadDetector.Kind.UNKNOWN&&b.dpad.kind()==DpadDetector.Kind.LEFT,"disconnect clears only its own observation");
        b.clearTest();check(b.dpad.kind()==DpadDetector.Kind.UNKNOWN,"reset clears old direction estimate");
        System.out.println("PASS: direction inference, hat/XY/right-axis pairs, trigger exclusion, mirrored-signal ambiguity, chords, neutral/focus/reconnect and per-controller isolation");
    }
}
