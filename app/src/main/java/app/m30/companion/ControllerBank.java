package app.m30.companion;

import java.util.*;

/** Stable slots: preserve existing assignments, do not confuse identical names or logical sources. */
public final class ControllerBank {
    public final ControllerSession[] slots;
    public ControllerBank(int mode){slots=new ControllerSession[]{new ControllerSession(mode),new ControllerSession(mode)};}
    public boolean dual(){return slots[1].assigned||!slots[1].descriptor.isEmpty();}
    public void restore(int index,String descriptor){slots[index].descriptor=descriptor==null?"":descriptor;}
    public ControllerSession forDevice(int id){if(id<0)return null;for(ControllerSession slot:slots)if(slot.deviceId==id||slot.sourceIds.contains(id))return slot;return null;}
    public int indexOf(int id){for(int i=0;i<slots.length;i++)if(id>=0&&(slots[i].deviceId==id||slots[i].sourceIds.contains(id)))return i;return -1;}
    public void releaseAll(){for(ControllerSession slot:slots)slot.release();}
    public void assign(int index,SourceSelector.Source source){
        int existing=indexOf(source.id);if(existing>=0&&existing!=index)throw new IllegalArgumentException("Controller already belongs to another slot");
        slots[index].connect(source);
    }
    /** Explicit user confirmation may move a source, preserving both sessions by swapping. */
    public void moveTo(int index,SourceSelector.Source source){
        int existing=indexOf(source.id);
        if(existing>=0&&existing!=index){ControllerSession other=slots[index];slots[index]=slots[existing];slots[existing]=other;}
        else assign(index,source);
    }
    public boolean reconcile(List<SourceSelector.Source> sources,String preferred){return reconcile(sources,preferred,true);}
    public boolean reconcile(List<SourceSelector.Source> sources,String preferred,boolean autoAssign){
        String before=signature();
        for(ControllerSession slot:slots)for(SourceSelector.Source source:sources)if(SourceSelector.systemInputName(source.name)&&!slot.descriptor.isEmpty()&&slot.descriptor.equals(source.descriptor)){slot.connect(null);slot.clearTest();slot.descriptor="";slot.deviceName=null;slot.assigned=false;slot.bindingsLoaded=false;}

        // Multiple logical Android sources with the same descriptor are one controller.
        LinkedHashMap<String,SourceSelector.Source> unique=new LinkedHashMap<>();
        for(SourceSelector.Source source:sources){
            boolean explicit=!source.descriptor.isEmpty()&&(source.descriptor.equals(preferred)||source.descriptor.equals(slots[0].descriptor)||source.descriptor.equals(slots[1].descriptor));
            if(!SourceSelector.controller(source)&&!(explicit&&SourceSelector.discoverable(source)))continue;
            String key=key(source);SourceSelector.Source previous=unique.get(key);
            if(previous==null||source.gamepad&&!previous.gamepad)unique.put(key,source);
        }
        ArrayList<SourceSelector.Source> available=new ArrayList<>(unique.values());
        HashSet<Integer> used=new HashSet<>();
        for(ControllerSession slot:slots){
            SourceSelector.Source match=null;
            for(SourceSelector.Source source:available)if(!used.contains(source.id)&&source.id==slot.deviceId&&source.descriptor.equals(slot.descriptor)){match=source;break;}
            if(match==null&&!slot.descriptor.isEmpty())for(SourceSelector.Source source:available)if(!used.contains(source.id)&&source.descriptor.equals(slot.descriptor)){match=source;break;}
            slot.connect(match);if(match!=null)used.add(match.id);
        }
        if(autoAssign)for(ControllerSession slot:slots){
            if(slot.deviceId>=0)continue;
            ArrayList<SourceSelector.Source> remaining=new ArrayList<>();for(SourceSelector.Source source:available)if(!used.contains(source.id))remaining.add(source);
            SourceSelector.Source chosen=SourceSelector.pick(remaining,preferred==null?"":preferred);
            if(chosen!=null){slot.connect(chosen);used.add(chosen.id);}
        }
        for(ControllerSession slot:slots)if(slot.deviceId>=0&&!slot.descriptor.isEmpty())for(SourceSelector.Source source:sources)if(SourceSelector.discoverable(source)&&source.descriptor.equals(slot.descriptor))slot.sourceIds.add(source.id);
        return !before.equals(signature());
    }
    private static String key(SourceSelector.Source source){return source.descriptor.isEmpty()?"id:"+source.id:source.descriptor;}
    private String signature(){StringBuilder value=new StringBuilder();for(ControllerSession slot:slots)value.append(slot.deviceId).append(':').append(slot.descriptor).append(';');return value.toString();}
}
