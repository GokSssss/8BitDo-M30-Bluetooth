package app.m30.companion;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.hardware.usb.*;
import android.os.*;
import java.io.*;
import java.util.*;

/** User-started connected-device foreground service; no automatic flash on USB attachment. */
public final class M30UpdateService extends Service {
    public static final String ACTION="app.m30.companion.FIRMWARE_PROGRESS";
    public static volatile boolean running;public static volatile int percent;public static volatile String message="",report="";
    public static volatile FirmwareUpdateResult result;
    private static final String CHANNEL="m30_firmware";private static final int NOTICE=30;
    private static final Object LOCK=new Object();
    private PowerManager.WakeLock wake;private Thread worker;
    @Override public void onCreate(){super.onCreate();NotificationManager n=getSystemService(NotificationManager.class);n.createNotificationChannel(new NotificationChannel(CHANNEL,"Обновление M30",NotificationManager.IMPORTANCE_LOW));}
    private Notification notification(String text){
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,M30FirmwareActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_usb_cable).setContentTitle("Обновление M30").setContentText(text).setOngoing(true).setContentIntent(open).setProgress(100,percent,false).build();
    }
    private void update(int value,String text){int old=percent;String before=message;percent=value;message=text;
        // After reset, USB permission for the old device may already be gone; keep the existing foreground notification until stopping.
        if(old!=value||!text.equals(before)){if(value<=98)startForeground(NOTICE,notification(text));sendBroadcast(new Intent(ACTION).setPackage(getPackageName()));}
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        synchronized(LOCK){if(running)return START_NOT_STICKY;running=true;percent=0;message="Проверка M30";report="";result=null;}
        try{if(Build.VERSION.SDK_INT>=29)startForeground(NOTICE,notification(message),ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);else startForeground(NOTICE,notification(message));}
        catch(RuntimeException error){running=false;report="Не удалось запустить обновление: "+error.getMessage();message=report;result=FirmwareUpdateResult.failed(report,System.currentTimeMillis());persistResult();stopSelf();return START_NOT_STICKY;}
        final String name=intent==null?null:intent.getStringExtra("usb_name");final String approvedHash=intent==null?null:intent.getStringExtra("sha256");
        worker=new Thread(()->install(name,approvedHash),"M30-official-update");worker.start();return START_NOT_STICKY;
    }
    private void persistResult(){
        FirmwareUpdateResult saved=result;if(saved==null)return;
        getSharedPreferences("firmware",0).edit().putString("report",saved.report).putInt("result_kind",saved.kind).putInt("result_version",saved.version).putString("result_report",saved.report).putLong("result_time",saved.time).apply();
    }
    private void install(String usbName,String approvedHash){
        boolean complete=false;String detail="";
        try{
            wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"m30:firmware");wake.acquire(20*60*1000L);
            byte[] bytes;try(InputStream in=new FileInputStream(new File(getFilesDir(),"m30-official.dat"))){bytes=M30FirmwareActivity.readBounded(in);}
            M30Firmware image=M30Firmware.parse(bytes);
            if(!image.sha256.equals(approvedHash)||!OfficialM30Files.recognized(image))throw new IllegalArgumentException("Выбранный файл изменился или не распознан");
            UsbManager manager=(UsbManager)getSystemService(USB_SERVICE);UsbDevice boot=UsbM30Link.singleBoot(manager);
            if(!boot.getDeviceName().equals(usbName))throw new IllegalArgumentException("USB-устройство изменилось; выберите загрузчик заново");
            for(UsbDevice d:manager.getDeviceList().values())if(!d.getDeviceName().equals(usbName)&&UsbDeviceIdentity.firmwareCandidate(d.getVendorId(),d.getProductId(),d.getProductName()))throw new IllegalArgumentException("Оставьте один M30 по USB на время обновления");
            detail="M30 Companion 1.52 · USB update\nОфициальный M30 "+image.versionText()+"\nSHA-256: "+image.sha256+"\n"+OfficialM30Files.integrityNote(image)+"\nСекция 0x5006: "+image.second.length+" байт, адрес 0x01018000. Секция PID 0 не записывается в ветке загрузчика 3208.\n";
            try(UsbM30Link link=new UsbM30Link(manager,boot)){
                M30UpdateEngine.install(image,link,this::update);
            }
            update(99,"Проверка переподключения M30");long deadline=SystemClock.elapsedRealtime()+20000;boolean bootGone=false,runtime=false;
            while(SystemClock.elapsedRealtime()<deadline){
                Map<String,UsbDevice> devices=manager.getDeviceList();UsbDevice old=devices.get(usbName);bootGone=old==null||!UsbBootModel.supported(old);
                for(UsbDevice d:devices.values())if(!UsbBootModel.supported(d)&&UsbDeviceIdentity.firmwareCandidate(d.getVendorId(),d.getProductId(),d.getProductName()))runtime=true;
                if(bootGone&&runtime)break;Thread.sleep(250);
            }
            complete=bootGone&&runtime;
            message=complete?"Обновление M30 завершено":"Загрузчик подтвердил запись и версию; обычное USB-подключение после перезапуска не подтверждено";
            if(complete)percent=100;
            report=detail+"\n"+message+"\nВерсия прочитана после записи, команда перезапуска отправлена.";
            result=FirmwareUpdateResult.completed(image.version,bootGone,runtime,report,System.currentTimeMillis());message=result.title();
        }catch(Exception error){message="Обновление остановлено";report=detail+"\n"+message+": "+error.getClass().getSimpleName()+" · "+error.getMessage()+"\nАвтоматического повторного стирания или перезапуска после ошибки нет. Если запись уже началась, оставьте M30 в загрузчике для повторного обновления.";}
        finally{
            if(result==null)result=FirmwareUpdateResult.failed(report,System.currentTimeMillis());persistResult();running=false;sendBroadcast(new Intent(ACTION).setPackage(getPackageName()));
            if(wake!=null&&wake.isHeld())wake.release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
        }
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
