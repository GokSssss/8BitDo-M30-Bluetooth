import app.m30.companion.SwipeNavigation;
public class SwipeNavigationTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        check(SwipeNavigation.ORDER.length==2,"connection and mode are one page");
        check(SwipeNavigation.target(0,-120,4,40)==1,"left swipe opens Buttons");
        check(SwipeNavigation.target(1,120,4,40)==0,"right swipe returns to Connection and Modes");
        check(SwipeNavigation.target(0,120,4,40)==0&&SwipeNavigation.target(1,-120,4,40)==1,"edges do not wrap");
        check(SwipeNavigation.target(0,12,200,40)==0,"vertical scrolling never changes a tab");
        check(!SwipeNavigation.horizontal(100,90,40),"diagonal scroll stays a scroll");
        check(SwipeNavigation.target(0,12,0,40)==0,"small movement stays a tap");
        System.out.println("PASS: two combined pages, both swipe directions, boundaries, vertical/diagonal scroll, taps");
    }
}
