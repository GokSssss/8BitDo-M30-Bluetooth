package app.m30.companion;

import android.hardware.usb.*;
import android.os.SystemClock;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** Exclusive USB transport for the observed M30 3208 HID endpoints. */
public final class UsbM30Link implements M30UpdateEngine.Link,AutoCloseable {
    private static final java.util.concurrent.atomic.AtomicBoolean BUSY=new java.util.concurrent.atomic.AtomicBoolean();
    private boolean closed;
    private final UsbDeviceConnection connection;private final UsbInterface iface;private final UsbEndpoint in,out;
    public UsbM30Link(UsbManager manager,UsbDevice device){
        if(!UsbBootModel.supported(device)||!manager.hasPermission(device))throw new IllegalArgumentException("Нет доступа к загрузчику 2DC8:3208");
        UsbInterface found=null;UsbEndpoint input=null,output=null;
        for(int i=0;i<device.getInterfaceCount();i++){
            UsbInterface f=device.getInterface(i);if(f.getInterfaceClass()!=UsbConstants.USB_CLASS_HID)continue;UsbEndpoint fi=null,fo=null;
            for(int j=0;j<f.getEndpointCount();j++){UsbEndpoint e=f.getEndpoint(j);if(e.getType()!=UsbConstants.USB_ENDPOINT_XFER_INT||e.getMaxPacketSize()!=64)continue;if(e.getAddress()==0x81)fi=e;if(e.getAddress()==0x02)fo=e;}
            if(fi!=null&&fo!=null){if(found!=null)throw new IllegalArgumentException("Несколько HID-интерфейсов загрузчика");found=f;input=fi;output=fo;}
        }
        if(found==null)throw new IllegalArgumentException("Нет HID 81 IN / 02 OUT по 64 байта");
        if(!BUSY.compareAndSet(false,true))throw new IllegalStateException("USB уже занят проверкой или обновлением M30");
        UsbDeviceConnection c=null;
        try{c=manager.openDevice(device);if(c==null)throw new IllegalStateException("Не удалось открыть USB");
            if(!c.claimInterface(found,true))throw new IllegalStateException("Не удалось занять HID-интерфейс");
            connection=c;iface=found;in=input;out=output;
        }catch(RuntimeException error){if(c!=null)c.close();BUSY.set(false);throw error;}
    }
    private byte[] transfer(UsbEndpoint endpoint,byte[] bytes,int timeout)throws Exception{
        UsbRequest request=new UsbRequest();boolean initialized=false;try{
            if(!request.initialize(connection,endpoint))throw new IllegalStateException("USB endpoint недоступен");initialized=true;
            ByteBuffer buffer=bytes==null?ByteBuffer.allocate(64):ByteBuffer.wrap(bytes);
            if(!request.queue(buffer))throw new IllegalStateException("USB запрос не поставлен в очередь");
            if(connection.requestWait(timeout)!=request)throw new IllegalStateException("USB запрос не завершён");
            int size=buffer.position();if(bytes!=null&&size!=64)throw new IllegalStateException("Неполная USB-передача");return Arrays.copyOf(buffer.array(),size);
        }finally{if(initialized){request.cancel();request.close();}}
    }
    @Override public byte[] exchange(BootUpdateProtocol.Request request,int timeout)throws Exception{
        transfer(out,request.packet(),3000);long end=SystemClock.elapsedRealtime()+timeout;
        while(SystemClock.elapsedRealtime()<end){
            byte[] raw=transfer(in,null,(int)Math.max(1,end-SystemClock.elapsedRealtime()));
            try{byte[] result=request.decode(raw);if(result!=null)return result;}
            catch(IllegalArgumentException error){StringBuilder hex=new StringBuilder();for(byte b:raw)hex.append(String.format(java.util.Locale.ROOT,"%02X ",b&255));throw new IllegalArgumentException(error.getMessage()+" · ответ "+hex,error);}
        }
        throw new java.util.concurrent.TimeoutException("Нет ответа на команду "+request.command);
    }
    @Override public void reset(BootUpdateProtocol.Request request)throws Exception{transfer(out,request.packet(),3000);}
    @Override public void close(){if(closed)return;closed=true;try{connection.releaseInterface(iface);}finally{try{connection.close();}finally{BUSY.set(false);}}}
    public static UsbDevice singleBoot(UsbManager manager){
        UsbDevice chosen=null;for(UsbDevice d:manager.getDeviceList().values())if(UsbBootModel.supported(d)){if(chosen!=null)throw new IllegalArgumentException("Подключите один загрузчик M30");chosen=d;}
        if(chosen==null)throw new IllegalArgumentException("Подключите M30 в режиме 8BitDo Boot");return chosen;
    }
}
