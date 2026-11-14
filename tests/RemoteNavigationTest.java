import app.m30.companion.RemoteNavigation;
public final class RemoteNavigationTest {
    public static void main(String[] args){
        for(int code:new int[]{19,20,21,22,23,66,160,4,111,61}){
            if(!RemoteNavigation.routesToUi(code,false))throw new AssertionError("Remote key trapped: "+code);
            if(RemoteNavigation.routesToUi(code,true))throw new AssertionError("Gamepad escaped tester: "+code);
        }
        for(int code:new int[]{96,97,98,99,24,25,131,29,108,110})if(RemoteNavigation.routesToUi(code,false))throw new AssertionError("Non-menu input escaped tester: "+code);
        app.m30.companion.SourceSelector.Source remote=new app.m30.companion.SourceSelector.Source(7,"remote","TV Remote",true,true);
        if(app.m30.companion.SourceSelector.namedController(remote))throw new AssertionError("Remote capabilities mistaken for named controller");
        if(!app.m30.companion.SourceSelector.namedController(new app.m30.companion.SourceSelector.Source(8,"m30","8BitDo M30 gamepad",false,true)))throw new AssertionError("M30 identity lost on DPAD/keyboard event");
        for(String key:new String[]{"bluetooth:toggle","header:choose","header:battery","header:pad","mode:1","bt:search","bt:AA:BB:CC:DD:EE:FF","root"}){
            if(RemoteNavigation.pagesFrom(key,false,false))throw new AssertionError("Interior horizontal focus hijacked: "+key);
            if(!RemoteNavigation.pagesFrom(key,false,true))throw new AssertionError("Remote cannot page from screen edge: "+key);
            if(RemoteNavigation.pagesFrom(key,true,true))throw new AssertionError("Gamepad escaped tester at edge: "+key);
        }
        if(RemoteNavigation.pagesFrom(null,false,false)||!RemoteNavigation.pagesFrom(null,false,true))throw new AssertionError("Untagged focused views must still support boundary paging");
        for(String key:new String[]{"tab:0","tab:2","tab:1"}){
            if(!RemoteNavigation.pagesFrom(key,false,false))throw new AssertionError("Tab paging lost");
            if(RemoteNavigation.pagesFrom(key,true,true))throw new AssertionError("Gamepad paged");
        }
        int[] pages={0,2,1};
        for(int n=0;n<pages.length;n++){
            if(RemoteNavigation.pageTarget(pages[n],22,true)!=pages[Math.min(2,n+1)])throw new AssertionError("Right page order");
            if(RemoteNavigation.pageTarget(pages[n],21,true)!=pages[Math.max(0,n-1)])throw new AssertionError("Left page order");
            if(RemoteNavigation.pageTarget(pages[n],22,false)!=pages[n])throw new AssertionError("Local horizontal navigation escaped into page switch");
        }
        if(!RemoteNavigation.pageCaption(1).equals("Тест кнопок")||!RemoteNavigation.pageCaption(2).equals("Джойстик 2"))throw new AssertionError("Page caption mapping");
        System.out.println("PASS: remote/keyboard menu keys remain navigable; gamepad input and test keys stay separate");
    }
}
