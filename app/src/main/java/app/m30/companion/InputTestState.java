package app.m30.companion;

import java.util.*;

/** Foreground-only universal input display. Never assigns a source to a controller slot. */
public final class InputTestState {
    public InputMonitor.Key lastKey;
    public boolean lastWasAxis;
    public int lastAxis,presses;
    public String axisName;
    public float axisValue;
    private int axisDevice;
    private String axisDeviceName;
    private final LinkedHashMap<String,InputMonitor.Key> held=new LinkedHashMap<>();
    private final Map<String,Float> axes=new HashMap<>();
    private String identity(InputMonitor.Key k){return k.deviceId+":"+k.source+":"+k.code+":"+k.scan;}
    public void record(InputMonitor.Key key){
        lastKey=key;lastWasAxis=false;String id=identity(key);
        if(key.down){if(!held.containsKey(id)&&key.repeat==0)presses++;held.put(id,key);while(held.size()>128)held.remove(held.keySet().iterator().next());}
        else held.remove(id);
    }
    public boolean axis(int device,String name,int axis,String axisLabel,float value){
        if(!Float.isFinite(value))return false;
        String id=device+":"+axis;Float previous=axes.put(id,value);
        if((previous==null&&value==0)||(previous!=null&&Float.compare(previous,value)==0))return false;
        // Hardware exposes a finite range set; cap device/range churn during a long foreground session.
        if(axes.size()>256){axes.clear();axes.put(id,value);}
        lastWasAxis=true;lastAxis=axis;axisName=axisLabel;axisValue=value;axisDevice=device;axisDeviceName=name;return true;
    }
    public boolean hasEvent(){return lastKey!=null||axisName!=null;}
    public int deviceId(){return lastWasAxis?axisDevice:lastKey==null?-1:lastKey.deviceId;}
    public String deviceName(){return lastWasAxis?axisDeviceName:lastKey==null?"—":lastKey.deviceName;}
    public String heldText(){
        if(held.isEmpty())return "Нет удерживаемых клавиш";
        ArrayList<String> labels=new ArrayList<>();for(InputMonitor.Key k:held.values())labels.add(k.name.replace("KEYCODE_","")+" [ID "+k.deviceId+"]");
        return "Нажато: "+String.join(" + ",labels);
    }
    public void release(){held.clear();axes.clear();}
    public void removeDevice(int id){held.values().removeIf(key->key.deviceId==id);axes.keySet().removeIf(key->key.startsWith(id+":"));}
}
