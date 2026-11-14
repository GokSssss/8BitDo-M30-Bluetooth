package app.m30.companion;

import java.util.Locale;
import java.util.regex.*;

/** Last update outcome, separate from current USB connection and selected file. */
public final class FirmwareUpdateResult {
    public static final int NONE=0,SUCCESS=1,UNCONFIRMED=2,ERROR=3;
    public final int kind,version;public final String report;public final long time;
    private FirmwareUpdateResult(int kind,int version,String report,long time){this.kind=kind;this.version=version;this.report=report==null?"":report;this.time=time;}
    public static FirmwareUpdateResult none(){return new FirmwareUpdateResult(NONE,0,"",0);}
    public static FirmwareUpdateResult completed(int version,boolean bootGone,boolean runtime,String report,long time){
        if(version<1||version>9999)throw new IllegalArgumentException("Verified firmware version required");
        return new FirmwareUpdateResult(bootGone&&runtime?SUCCESS:UNCONFIRMED,version,report,time);
    }
    public static FirmwareUpdateResult failed(String report,long time){return new FirmwareUpdateResult(ERROR,0,report,time);}
    public static FirmwareUpdateResult restore(int kind,int version,String report,long time){
        if((kind==SUCCESS||kind==UNCONFIRMED)&&version>0&&version<=9999)return new FirmwareUpdateResult(kind,version,report,time);
        return kind==ERROR?failed(report,time):none();
    }
    public String versionText(){return String.format(Locale.ROOT,"%d.%02d",version/100,version%100);}
    public String title(){return kind==SUCCESS?"✓ Прошивка "+versionText()+" установлена":kind==UNCONFIRMED?"Запись выполнена · переподключение не подтверждено":kind==ERROR?"Обновление остановлено":"";}
    /** Migrate only the exact success report emitted after runtime confirmation in 1.28/1.29. */
    public static FirmwareUpdateResult fromLegacyReport(String report){
        if(report==null||!report.startsWith("M30 Companion ")||!report.endsWith("\nОбновление M30 завершено\nВерсия прочитана после записи, команда перезапуска отправлена."))return none();
        Matcher match=Pattern.compile("(?m)^Официальный M30 ([0-9]{1,2})\\.([0-9]{2})$").matcher(report);
        if(!match.find())return none();int version=Integer.parseInt(match.group(1))*100+Integer.parseInt(match.group(2));
        return version>0?completed(version,true,true,report,0):none();
    }
}
