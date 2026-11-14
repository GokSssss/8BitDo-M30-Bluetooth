package app.m30.companion;

import java.util.*;

/** Explicit owner links only. A bond, name or discovery result never creates a link. */
public final class BluetoothSlotLinks {
    private final Map<String,String> links=new LinkedHashMap<>();
    public static String address(String value){return value!=null&&value.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")?value.toUpperCase(Locale.ROOT):null;}
    public void restore(String address,String descriptor){String a=address(address);if(a!=null&&descriptor!=null&&!descriptor.isEmpty())links.put(a,descriptor);}
    public boolean bind(String address,String descriptor){String a=address(address);if(a==null||descriptor==null||descriptor.isEmpty())return false;links.entrySet().removeIf(e->e.getValue().equals(descriptor));links.put(a,descriptor);return true;}
    public void remove(String address){links.remove(address(address));}
    public Map<String,String> snapshot(){return new LinkedHashMap<>(links);}
    public String linkedAddress(String descriptor){
        String result=null;if(descriptor==null||descriptor.isEmpty())return null;
        for(Map.Entry<String,String> e:links.entrySet())if(e.getValue().equals(descriptor)){if(result!=null)return null;result=e.getKey();}
        return result;
    }
    public int activeSlot(String address,String[] activeDescriptors){
        String a=address(address),descriptor=links.get(a);if(descriptor==null||!a.equals(linkedAddress(descriptor)))return -1;
        int result=-1;for(int n=0;n<activeDescriptors.length;n++)if(descriptor.equals(activeDescriptors[n])){if(result>=0)return -1;result=n;}return result;
    }
}
