import app.m30.companion.*;
import java.util.*;

public class ControllerTelemetryTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static ControllerTelemetry.Source source(int id,String name,int vendor,int product,boolean present,float capacity){return new ControllerTelemetry.Source(id,name,vendor,product,present,capacity);}
    public static void main(String[] args){
        ControllerTelemetry.Source primary=source(8,"8BitDo M30 gamepad",0x2dc8,0x0651,false,Float.NaN);
        ControllerTelemetry.Source sibling=source(10,"M30 keyboard",0x045e,0x02e0,true,.52f);
        check(ControllerTelemetry.battery(8,List.of(primary,sibling))==sibling,"battery on a sibling logical source remains visible");
        ControllerTelemetry.Source zero=source(8,"M30",0x2dc8,0x0651,true,0);
        check(ControllerTelemetry.battery(8,List.of(sibling,zero))==zero,"real primary zero is valid and takes priority over sibling");
        check(ControllerTelemetry.battery(8,List.of(primary))==null,"missing battery does not invent a cached percent");
        for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-.1f,1.1f})check(ControllerTelemetry.battery(8,List.of(source(8,"M30",0,0,true,bad)))==null,"invalid battery capacity is unknown");
        check(ControllerTelemetry.battery(8,List.of(source(-1,"Virtual",0,0,true,.8f),source(8,"M30",0,0,false,.52f)))==null,"virtual source and absent battery cannot supply percent");
        ControllerTelemetry.Source generic=source(8,"8BitDo M30",0,0,false,Float.NaN);
        check(ControllerTelemetry.mode(8,List.of(generic,sibling)).candidate==1,"known sibling identity supplies estimate when primary is generic");
        check(ControllerTelemetry.mode(8,List.of(primary)).candidate==0,"user's actual 2DC8:0651 report selects Android/D-input");
        check(ControllerTelemetry.mode(8,List.of(primary,sibling)).candidate==-1,"contradictory logical identities are not silently resolved as certainty");
        check(ControllerTelemetry.mode(8,List.of(generic)).candidate==-1,"a name without identifiers remains unknown");
        check(ControllerTelemetry.mode(-1,List.of()).candidate==-1,"disconnected controller has no estimate");
        System.out.println("PASS: per-controller logical source telemetry, primary/zero battery, sibling fallback, invalid/absent battery and observed D-input identity");
    }
}
