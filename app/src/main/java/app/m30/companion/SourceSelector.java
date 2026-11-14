package app.m30.companion;

import java.util.List;
import java.util.Locale;

/** Source classification is separate from the controller's firmware mode. */
public final class SourceSelector {
    public static final class Source {
        public final int id;
        public final String descriptor,name;
        public final boolean gamepad,external;
        public Source(int id,String descriptor,String name,boolean gamepad,boolean external){
            this.id=id;this.descriptor=descriptor;this.name=name;this.gamepad=gamepad;this.external=external;
        }
    }
    public static boolean systemInputName(String name){
        String n=name==null?"":name.toLowerCase(Locale.ROOT).trim();
        return n.equals("virtual")||n.equals("virtual-search")||n.equals("virtual-remote")||n.equals("mtk tv keypad")||n.equals("mstar smart tv ir receiver")||n.startsWith("gpio-")||n.startsWith("pmic_")||n.startsWith("sec_touch")||n.equals("hall");
    }
    public static boolean controller(Source s){
        if(s==null||s.id<0||systemInputName(s.name))return false;
        return s.gamepad||namedController(s);
    }
    public static boolean namedController(Source s){
        if(s==null||s.id<0||systemInputName(s.name))return false;
        String n=s.name.toLowerCase(Locale.ROOT);
        return n.contains("m30")||n.contains("8bitdo")||n.contains("xbox")||n.contains("x-input")||n.contains("pro controller")||n.contains("wireless controller");
    }
    public static boolean discoverable(Source s){
        if(s==null||s.id<0||systemInputName(s.name))return false;
        String n=s.name.toLowerCase(Locale.ROOT);
        if(n.startsWith("gpio-")||n.startsWith("pmic_")||n.startsWith("sec_touch")||n.equals("hall"))return false;
        return controller(s)||s.external;
    }
    public static Source pick(List<Source> sources,String preferred){
        for(Source s:sources)if(!preferred.isEmpty()&&s.descriptor.equals(preferred)&&discoverable(s))return s;
        Source best=null;int rank=-1;
        for(Source s:sources){if(!controller(s))continue;String n=s.name.toLowerCase(Locale.ROOT);int r=n.contains("m30")?3:n.contains("8bitdo")?2:1;if(r>rank){rank=r;best=s;}}
        return best;
    }
    public static boolean blockNavigation(Source s,boolean controllerEvent,boolean enabled){return enabled&&(s==null||!systemInputName(s.name))&&(controllerEvent||discoverable(s));}
}
