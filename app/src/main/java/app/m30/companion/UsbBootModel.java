package app.m30.companion;

import android.hardware.usb.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

/** User-initiated read-only model and M30 version query, limited to 2DC8:3208 HID. */
public final class UsbBootModel {
    private UsbBootModel(){}
    public static boolean supported(UsbDevice device){return device!=null&&device.getVendorId()==0x2dc8&&device.getProductId()==0x3208;}
    private static byte[] transfer(UsbDeviceConnection connection,UsbEndpoint endpoint,byte[] output,int timeout)throws Exception{
        UsbRequest request=new UsbRequest();
        try{
            if(!request.initialize(connection,endpoint))throw new IllegalStateException("Не удалось открыть endpoint");
            ByteBuffer buffer=output==null?ByteBuffer.allocate(64):ByteBuffer.wrap(output);
            if(!request.queue(buffer))throw new IllegalStateException("Не удалось поставить USB-запрос в очередь");
            if(connection.requestWait(timeout)!=request)throw new IllegalStateException("USB-запрос не завершён");
            int length=buffer.position();
            if(output!=null&&length!=64)throw new IllegalStateException("Запрос модели отправлен не полностью");
            return Arrays.copyOf(buffer.array(),length);
        }finally{request.cancel();request.close();}
    }
    public static String inspect(UsbManager manager,UsbDevice device){
        if(!supported(device))return "Этот запрос поддерживает только загрузчик 2DC8:3208.";
        if(!manager.hasPermission(device))return "Нет разрешения USB. Разрешите доступ и повторите проверку.";
        UsbDeviceConnection connection=null;UsbInterface claimed=null;
        try{
            UsbInterface target=null;UsbEndpoint in=null,out=null;
            for(int i=0;i<device.getInterfaceCount();i++){
                UsbInterface iface=device.getInterface(i);if(iface.getInterfaceClass()!=UsbConstants.USB_CLASS_HID)continue;
                UsbEndpoint candidateIn=null,candidateOut=null;
                for(int j=0;j<iface.getEndpointCount();j++){UsbEndpoint endpoint=iface.getEndpoint(j);if(endpoint.getType()!=UsbConstants.USB_ENDPOINT_XFER_INT||endpoint.getMaxPacketSize()!=64)continue;if(endpoint.getAddress()==0x81)candidateIn=endpoint;if(endpoint.getAddress()==0x02)candidateOut=endpoint;}
                if(candidateIn!=null&&candidateOut!=null){if(target!=null)return "Несколько подходящих HID-интерфейсов: выбор неоднозначен.";target=iface;in=candidateIn;out=candidateOut;}
            }
            if(target==null)return "Не найден ожидаемый HID-интерфейс 81 IN / 02 OUT, 64 байта.";
            connection=manager.openDevice(device);if(connection==null)return "Android не открыл USB-устройство.";
            if(!connection.claimInterface(target,true))return "Не удалось получить доступ к HID-интерфейсу.";claimed=target;
            transfer(connection,out,BootModelProtocol.request(),3000);
            long deadline=android.os.SystemClock.elapsedRealtime()+3000;
            while(android.os.SystemClock.elapsedRealtime()<deadline){
                int timeout=(int)Math.max(1,deadline-android.os.SystemClock.elapsedRealtime());
                byte[] reply=transfer(connection,in,null,timeout);Long model=BootModelProtocol.decode(reply);
                if(model!=null){
                    String report=String.format(Locale.ROOT,"Загрузчик 2DC8:3208 ответил.\nКод модели: 0x%08X\nОтвет модели: %s\nCRC, команда, длина и адрес проверены.\n",model,hex(reply));
                    if(model!=0x5006L)return report+"\nЭто другая модель; запрос версии M30 не отправлен.";
                    report+="Модель: 8BitDo M30 Bluetooth\n";
                    try{
                        transfer(connection,out,BootVersionProtocol.request(),3000);
                        long versionDeadline=android.os.SystemClock.elapsedRealtime()+3000;
                        while(android.os.SystemClock.elapsedRealtime()<versionDeadline){
                            byte[] versionReply=transfer(connection,in,null,(int)Math.max(1,versionDeadline-android.os.SystemClock.elapsedRealtime()));
                            byte[] payload=BootVersionProtocol.decode(versionReply);
                            if(payload!=null)return report+"\nВерсия прошивки M30: "+BootVersionProtocol.version(payload)+"\nПоле версии: "+hex(payload)+"\nОтвет версии: "+hex(versionReply)+"\n\nЧтение 0xC2 по адресу 0x01017040; CRC и поля ответа проверены.\nВерсия не получена из bcdDevice. Запись, стирание и перезапуск не выполнялись.";
                        }
                        return report+"\nНе получен ответ на чтение версии. Запись и стирание не выполнялись.";
                    }catch(Exception error){return report+"\nВерсию прочитать не удалось: "+error.getClass().getSimpleName()+" · "+error.getMessage()+"\nЗапись, стирание и перезапуск не выполнялись.";}
                }
            }
            return "Загрузчик не вернул ответ на запрос модели. Совместимость протокола не подтверждена.";
        }catch(Exception error){return "Запрос модели не завершён: "+error.getClass().getSimpleName()+" · "+error.getMessage()+"\nСовместимость протокола не подтверждена. Запись и стирание не выполнялись.";}
        finally{if(connection!=null){try{if(claimed!=null)connection.releaseInterface(claimed);}finally{connection.close();}}}
    }
    private static String hex(byte[] bytes){StringBuilder result=new StringBuilder();for(byte b:bytes){if(result.length()>0)result.append(' ');result.append(String.format(Locale.ROOT,"%02X",b&255));}return result.toString();}
}
