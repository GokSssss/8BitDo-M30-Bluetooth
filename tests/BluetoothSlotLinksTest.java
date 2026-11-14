import app.m30.companion.BluetoothSlotLinks;

public final class BluetoothSlotLinksTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        BluetoothSlotLinks links=new BluetoothSlotLinks();String a="AA:BB:CC:DD:EE:01",b="AA:BB:CC:DD:EE:02";
        check(links.activeSlot(a,new String[]{"same-name-pad",""})==-1,"Identical names or saved pairs cannot invent a Bluetooth address");
        check(!links.bind("invalid","pad")&&!links.bind(a,""),"Invalid address or absent source cannot create a link");
        links.bind(a.toLowerCase(),"descriptor1");check(links.activeSlot(a,new String[]{"descriptor1","descriptor2"})==0,"Explicit source link resolves correct full address and slot");
        check(links.activeSlot(b,new String[]{"descriptor1","descriptor2"})==-1,"Other same-name controller remains independent");
        check(links.activeSlot(a,new String[]{"","descriptor2"})==-1,"Disconnect clears connected indication without deleting link");
        check(links.activeSlot(a,new String[]{"descriptor2","descriptor1"})==1,"Link follows physical source when its slot changes");
        check(links.activeSlot(a,new String[]{"new-mode-descriptor",""})==-1,"Changed mode/descriptor must not inherit old identity");
        links.bind(b,"descriptor1");check(links.activeSlot(a,new String[]{"descriptor1",""})==-1&&links.activeSlot(b,new String[]{"descriptor1",""})==0,"Rebinding replaces stale claim rather than lighting two same-name rows");
        links.bind(a,"descriptor2");check(links.activeSlot(a,new String[]{"descriptor1","descriptor2"})==1,"Two controllers have independent labels");
        check(links.activeSlot(a,new String[]{"descriptor2","descriptor2"})==-1,"Ambiguous duplicate slots must not be labelled connected");
        links.restore(b,"descriptor2");check(links.linkedAddress("descriptor2")==null&&links.activeSlot(a,new String[]{"descriptor2",""})==-1,"Conflicting restored mappings cannot choose arbitrary alias");
        links.remove(b);check(links.linkedAddress("descriptor2").equals(a),"Unlink restores unique mapping");
        System.out.println("PASS: explicit Bluetooth/slot identity, duplicate names, disconnection, mode changes, rebinding and ambiguous mappings");
    }
}
