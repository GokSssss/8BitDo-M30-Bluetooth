package app.m30.companion;

import java.util.*;

/** System bonds and discovery are independent. Full address is the only list identity. */
public final class BluetoothDeviceList {
    public static final int PAIR=0,SYSTEM_CONNECT=1,WAIT=2;
    public static final class Entry {
        public final String address;
        public String name="",alias="";
        public int kind,bond=10,mode=-1;
        public boolean found;
        Entry(String address){this.address=address;}
        public String displayName(){return alias.isEmpty()?name:alias;}
        public boolean saved(){return bond==12;}
        public int action(){return bond==11?WAIT:saved()?SYSTEM_CONNECT:PAIR;}
        public String modeCaption(){return mode>=0&&mode<4?"Режим: "+new String[]{"Android","Windows","macOS","Switch"}[mode]+" · ваша отметка":"Режим не указан";}
        public String stateCaption(){return bond==11?"Сопряжение…":saved()?(found?"Сопряжено · найден в этом поиске":"Сопряжено"):(found?"Найден в этом поиске":"Не сопряжено");}
    }
    private final Map<String,Entry> entries=new LinkedHashMap<>();
    private static String key(String address){return address!=null&&address.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")?address.toUpperCase(Locale.ROOT):null;}
    public Entry get(String address){return entries.get(key(address));}
    public Entry offer(String address,String name,int kind,int bond,boolean found){
        String key=key(address);if(key==null)return null;
        Entry entry=entries.get(key);if(entry==null){if(entries.size()>=64)return null;entry=new Entry(key);entries.put(key,entry);}
        if(name!=null&&!name.isEmpty()&&(found||!entry.found||entry.name.isEmpty()))entry.name=name;
        entry.kind=kind;entry.bond=bond;if(bond!=12)entry.alias="";entry.found|=found;return entry;
    }
    public void bondChanged(String address,int bond){Entry e=get(address);if(e==null)return;e.bond=bond;if(bond!=12)e.alias="";if(bond==10&&!e.found)remove(address);}
    public void remove(String address){entries.remove(key(address));}
    public void clear(){entries.clear();}
    public void newSearch(){for(Entry e:new ArrayList<>(entries.values())){e.found=false;if(!e.saved()&&e.bond!=11)entries.remove(e.address);}}
    public void syncSaved(Set<String> addresses){
        Set<String> normalized=new HashSet<>();for(String address:addresses){String key=key(address);if(key!=null)normalized.add(key);}
        for(Entry entry:new ArrayList<>(entries.values()))if(entry.saved()&&!normalized.contains(entry.address)){entry.bond=10;entry.alias="";if(!entry.found)entries.remove(entry.address);}
    }
    public List<Entry> section(boolean saved){
        List<Entry> result=new ArrayList<>();for(Entry e:entries.values())if(saved?e.saved():e.found)result.add(e);
        result.sort(Comparator.comparingInt((Entry e)->e.kind==BluetoothGamepadFilter.M30?0:1).thenComparing(e->e.name,String.CASE_INSENSITIVE_ORDER).thenComparing(e->e.address));return result;
    }
}
