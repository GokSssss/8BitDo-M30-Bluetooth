package app.m30.companion;

import java.util.*;

/** Foreground test data belongs to one controller slot, including while it is hidden. */
public final class ControllerSession {
    public final PadState state=new PadState();
    public final InputMonitor monitor=new InputMonitor();
    public final DpadDetector dpad=new DpadDetector();
    public final ModeConfirmation confirmation=new ModeConfirmation();
    public final Map<String,Float> axes=new LinkedHashMap<>();
    public final ArrayDeque<String> log=new ArrayDeque<>();
    public final Set<Integer> sourceIds=new HashSet<>();
    public int deviceId=-1,mode,lastAxis;
    public String descriptor="",deviceName,waiting,lastAxisToken;
    public boolean bindingsLoaded,lastWasAxis,assigned;
    public float lastAxisValue;
    public long lastInput;
    public ControllerSession(int mode){this.mode=mode;}
    public boolean connected(){return deviceId>=0;}
    public void connect(SourceSelector.Source source){
        if(source!=null){assigned=true;deviceName=source.name;}
        int next=source==null?-1:source.id;
        if(source!=null&&!source.descriptor.equals(descriptor)){
            clearTest();state.bindings.clear();bindingsLoaded=false;descriptor=source.descriptor;confirmation.observe(null);
        }
        if(next!=deviceId){dpad.reset();release();monitor.clearSelected();lastWasAxis=false;lastAxisToken=null;confirmation.observe(null);}
        deviceId=next;sourceIds.clear();if(next>=0)sourceIds.add(next);
    }
    public void release(){dpad.endBurst();state.release();axes.clear();waiting=null;}
    public void clearTest(){dpad.reset();state.reset();axes.clear();log.clear();monitor.clearSelected();waiting=null;lastWasAxis=false;lastAxisToken=null;lastInput=0;}
}
