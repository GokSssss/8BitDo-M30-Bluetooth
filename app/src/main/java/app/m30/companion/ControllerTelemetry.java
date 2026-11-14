package app.m30.companion;

import java.util.*;

/** Fresh observations from logical sources already assigned to ONE physical controller. */
public final class ControllerTelemetry {
    private ControllerTelemetry(){}
    public static final class Source {
        public final int id,vendor,product;
        public final String name;
        public final boolean batteryPresent;
        public final float capacity;
        public final String batteryError;
        public Source(int id,String name,int vendor,int product,boolean present,float capacity){this(id,name,vendor,product,present,capacity,null);}
        public Source(int id,String name,int vendor,int product,boolean present,float capacity,String error){this.id=id;this.name=name;this.vendor=vendor;this.product=product;this.batteryPresent=present;this.capacity=capacity;this.batteryError=error;}
        public boolean hasBattery(){return id>=0&&batteryPresent&&Float.isFinite(capacity)&&capacity>=0&&capacity<=1;}
    }
    public static Source battery(int primary,List<Source> owned){
        Source fallback=null;
        for(Source source:owned)if(source.hasBattery()){
            if(source.id==primary)return source;
            if(fallback==null||source.id<fallback.id)fallback=source;
        }
        return fallback;
    }
    public static ModeDetector.Result mode(int primary,List<Source> owned){
        ModeDetector.Result best=null,unknown=null;int candidate=-1;
        StringBuilder evidence=new StringBuilder();boolean conflict=false;
        for(Source source:owned){
            if(source.id<0)continue;
            ModeDetector.Result detected=ModeDetector.detect(source.name,source.vendor,source.product);
            if(unknown==null||source.id==primary)unknown=detected;
            if(evidence.length()>0)evidence.append("\n");evidence.append("ID ").append(source.id).append(": ").append(detected.evidence);
            if(detected.candidate>=0){
                if(candidate>=0&&candidate!=detected.candidate)conflict=true;
                candidate=detected.candidate;if(best==null||source.id==primary)best=detected;
            }
        }
        if(conflict)return new ModeDetector.Result(-1,"Признаки режима противоречат друг другу",evidence.toString());
        if(best!=null)return new ModeDetector.Result(best.candidate,best.title,evidence.toString());
        if(unknown!=null)return new ModeDetector.Result(-1,unknown.title,evidence.toString());
        return ModeDetector.detect(null,0,0);
    }
}
