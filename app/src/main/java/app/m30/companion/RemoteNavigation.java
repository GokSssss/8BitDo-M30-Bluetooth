package app.m30.companion;

/** Only remote/keyboard menu keys bypass the input tester. Gamepad events stay testable. */
public final class RemoteNavigation {
    private RemoteNavigation(){}
    public static String pageCaption(int page){return page==0?"Джойстик 1":page==2?"Джойстик 2":"Тест кнопок";}
    public static boolean pagesFrom(String focusedKey,boolean gamepad,boolean horizontalEdge){
        return !gamepad&&(horizontalEdge||(focusedKey!=null&&focusedKey.startsWith("tab:")));
    }
    public static int pageTarget(int current,int keycode,boolean onTabs){
        if(!onTabs||(keycode!=21&&keycode!=22))return current;
        int[] order=SwipeNavigation.order(true);int index=-1;for(int n=0;n<order.length;n++)if(order[n]==current)index=n;
        if(index<0)return current;return order[Math.max(0,Math.min(order.length-1,index+(keycode==22?1:-1)))];
    }
    public static boolean routesToUi(int keycode,boolean gamepad){
        if(gamepad)return false;
        return keycode==19||keycode==20||keycode==21||keycode==22||keycode==23
            ||keycode==66||keycode==160||keycode==4||keycode==111||keycode==61;
    }
}
