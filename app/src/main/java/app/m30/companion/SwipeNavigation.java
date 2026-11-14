package app.m30.companion;

/** With two controllers, split Connection into stable first/second slot pages. */
public final class SwipeNavigation {
    public static final int[] ORDER={0,1};
    private static final int[] DUAL_ORDER={0,2,1};
    public static int[] order(boolean dual){return (dual?DUAL_ORDER:ORDER).clone();}
    public static boolean horizontal(float dx,float dy,float threshold){return Math.abs(dx)>=threshold&&Math.abs(dx)>Math.abs(dy)*1.5f;}
    public static int target(int current,float dx,float dy,float threshold){
        return target(current,dx,dy,threshold,false);
    }
    public static int target(int current,float dx,float dy,float threshold,boolean dual){
        if(!horizontal(dx,dy,threshold))return current;
        int[] pages=dual?DUAL_ORDER:ORDER;int index=0;for(int n=0;n<pages.length;n++)if(pages[n]==current)index=n;
        index=Math.max(0,Math.min(pages.length-1,index+(dx<0?1:-1)));return pages[index];
    }
}
