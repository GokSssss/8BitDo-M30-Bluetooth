import app.m30.companion.BluetoothDeviceList;
import app.m30.companion.BluetoothGamepadFilter;
import java.util.*;

public final class BluetoothDeviceListTest {
    static void check(boolean pass,String reason){if(!pass)throw new AssertionError(reason);}
    public static void main(String[] args){
        BluetoothDeviceList list=new BluetoothDeviceList();
        String a="AA:BB:CC:DD:EE:01",b="AA:BB:CC:DD:EE:02";
        BluetoothDeviceList.Entry saved=list.offer(a,"8BitDo M30 gamepad",BluetoothGamepadFilter.M30,12,false);
        saved.mode=1;
        check(list.section(true).size()==1&&list.section(false).isEmpty(),"Cached bond is not a search result");
        check(saved.action()==BluetoothDeviceList.SYSTEM_CONNECT,"Saved entry must expose repeat connection action");
        check(list.offer(a.toLowerCase(Locale.ROOT),saved.name,saved.kind,12,true)==saved,"Discovery must update same full-address entry");
        check(saved.found&&list.section(true).size()==1&&list.section(false).size()==1&&list.section(false).get(0)==saved,"Saved discovery must appear in BOTH independent columns with same identity");
        saved.alias="M30 Windows";check(saved.displayName().equals("M30 Windows")&&saved.name.equals("8BitDo M30 gamepad"),"System alias must not overwrite original identity/filter name");
        check(saved.action()==BluetoothDeviceList.SYSTEM_CONNECT,"Finding a paired device cannot make it unselectable");
        BluetoothDeviceList.Entry other=list.offer(b,saved.name,saved.kind,10,true);
        check(other!=saved&&other.mode==-1&&other.action()==BluetoothDeviceList.PAIR,"Same name must remain independent, unknown mode and pairable");
        check(saved.modeCaption().contains("Windows")&&saved.modeCaption().contains("ваша отметка"),"Mode provenance must be visible");
        check(other.modeCaption().equals("Режим не указан"),"No mode guesses from same M30 name");
        list.offer(b,other.name,other.kind,11,false);check(other.action()==BluetoothDeviceList.WAIT,"In-flight pairing cannot be restarted");
        list.offer(b,other.name,other.kind,12,false);check(list.section(true).size()==2&&list.section(false).size()==2,"Pair completion adds saved without hiding a discovered entry");
        list.newSearch();check(!saved.found&&!other.found&&saved.mode==1&&list.section(false).isEmpty(),"New search clears presence, retains bonds and mode");
        list.offer(b,other.name,other.kind,12,true);list.syncSaved(Collections.singleton(a));
        check(other.bond==10&&other.found&&other.action()==BluetoothDeviceList.PAIR&&list.section(false).size()==1,"Unpaired system entry found nearby remains pairable");
        list.newSearch();check(list.get(b)==null&&list.get(a)==saved,"Unseen unpaired entries cannot survive new scan");
        check(list.offer("invalid","fake",1,12,true)==null,"Invalid identity must be rejected");
        saved.mode=8;check(saved.modeCaption().equals("Режим не указан"),"Invalid mode cannot become a claim");
        list.syncSaved(Collections.emptySet());check(list.section(true).isEmpty()&&list.get(a)==null,"Forgotten unseen pair must disappear");
        for(int n=0;n<70;n++)list.offer(String.format(Locale.ROOT,"00:11:22:33:44:%02X",n),"Controller",1,10,true);
        check(list.section(false).size()==64,"Bound candidate storage");
        System.out.println("PASS: system-like Bluetooth sections, immediate dedup, repeat selection, mode provenance, bond refresh and scan reset");
    }
}
