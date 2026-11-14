package app.m30.companion;

import java.util.*;

/** Observes direction signals only; never reads or changes a firmware setting.
 * Numeric axes/keycodes are Android constants, kept here without an Android dependency. */
public final class DpadDetector {
    public enum Kind { UNKNOWN, DPAD, LEFT, RIGHT, MIXED }
    private int mask;
    private long lastSignal=-1;
    private final Set<String> signals=new TreeSet<>();
    public Kind kind(){return mask==1?Kind.DPAD:mask==2?Kind.LEFT:mask==4?Kind.RIGHT:mask==0?Kind.UNKNOWN:Kind.MIXED;}
    public void key(int code,boolean down,int repeat,boolean selectHeld,long now){
        if(!down||repeat!=0||code<19||code>22)return;
        if(selectHeld){reset();return;}
        accept(1,Collections.singleton("KEYCODE_DPAD_"+new String[]{"UP","DOWN","LEFT","RIGHT"}[code-19]),now);
    }
    public void axes(Map<Integer,Float> values,Set<Integer> centered,boolean selectHeld,long now){
        int observed=0;Set<String> evidence=new TreeSet<>();
        boolean hasRxRy=centered.contains(12)||centered.contains(13);
        for(Map.Entry<Integer,Float> entry:values.entrySet()){
            int axis=entry.getKey();float value=entry.getValue();if(!Float.isFinite(value)||Math.abs(value)<=.5f)continue;
            int family=axis==15||axis==16?1:axis==0||axis==1?2:((axis==12||axis==13)||!hasRxRy&&(axis==11||axis==14))&&centered.contains(axis)?4:0;
            if(family!=0){observed|=family;evidence.add(axisName(axis));}
        }
        if(observed==0)return;
        if(selectHeld){reset();return;}
        accept(observed,evidence,now);
    }
    private void accept(int observed,Set<String> evidence,long now){
        // Android may mirror a single press as both a key and an axis report.
        if(lastSignal<0||now<lastSignal||now-lastSignal>150){mask=0;signals.clear();}
        mask|=observed;signals.addAll(evidence);lastSignal=now;
    }
    private static String axisName(int axis){return switch(axis){case 0->"AXIS_X";case 1->"AXIS_Y";case 11->"AXIS_Z";case 12->"AXIS_RX";case 13->"AXIS_RY";case 14->"AXIS_RZ";case 15->"AXIS_HAT_X";case 16->"AXIS_HAT_Y";default->"AXIS_"+axis;};}
    public String summary(){return switch(kind()){
        case UNKNOWN -> "Крестовина: не определено";
        case DPAD -> "Крестовина: предположительно D-pad";
        case LEFT -> "Крестовина: предположительно левый стик";
        case RIGHT -> "Крестовина: предположительно правый стик";
        case MIXED -> "Крестовина: неоднозначные сигналы";
    };}
    public String caption(){return switch(kind()){
        case UNKNOWN -> "НЕ ОПРЕДЕЛЕНО";
        case DPAD -> "D-PAD";
        case LEFT -> "ЛЕВЫЙ СТИК";
        case RIGHT -> "ПРАВЫЙ СТИК";
        case MIXED -> "НЕОДНОЗНАЧНО";
    };}
    public String evidence(){return signals.isEmpty()?"Нажмите направление на крестовине":String.join(", ",signals);}
    public void endBurst(){lastSignal=-1;}
    public void reset(){mask=0;signals.clear();lastSignal=-1;}
}
