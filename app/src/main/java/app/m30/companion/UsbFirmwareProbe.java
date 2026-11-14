package app.m30.companion;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.*;
import java.util.*;

/** USB descriptor inspection only. Does not open, claim or write to any device. */
public final class UsbFirmwareProbe {
    private UsbFirmwareProbe(){}
    /** Bit 1: candidate connected; bit 2: exactly one command-8 bootloader. */
    public static int connectionState(Context context){
        UsbManager manager=(UsbManager)context.getSystemService(Context.USB_SERVICE);if(manager==null)return 0;
        int state=0,bootCount=0;
        try{for(UsbDevice device:manager.getDeviceList().values()){
            if(UsbDeviceIdentity.firmwareCandidate(device.getVendorId(),device.getProductId(),device.getProductName()))state|=1;
            if(UsbBootModel.supported(device))bootCount++;
        }}catch(RuntimeException error){return 0;}
        return state|(bootCount==1?2:0);
    }
    public static String inspect(Context context){
        if(!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
            return "Этот телефон не заявляет поддержку USB Host / OTG.";
        UsbManager manager=(UsbManager)context.getSystemService(Context.USB_SERVICE);
        if(manager==null)return "Служба USB недоступна.";
        try{
            ArrayList<UsbDevice> devices=new ArrayList<>(manager.getDeviceList().values());
            if(devices.isEmpty())return "USB Host / OTG поддерживается.\nUSB-устройства не обнаружены. Нужен кабель с передачей данных; кабель только для зарядки не подойдёт.";
            devices.sort(Comparator.comparingInt(UsbDevice::getDeviceId));
            StringBuilder report=new StringBuilder("USB Host / OTG поддерживается.\n");
            for(UsbDevice device:devices){
                int vendor=device.getVendorId(),product=device.getProductId();
                String kind=vendor==0x2dc8&&product==0x5006?"M30 Bluetooth · обычный USB-режим":vendor==0x2dc8&&(product==0x5750||product==0x3208)?"Загрузчик 8BitDo · модель по этому ID не определяется":vendor==0x2dc8?"Устройство 8BitDo · модель не подтверждена":"Другое USB-устройство";
                report.append('\n').append(kind).append('\n');
                String name=device.getProductName();if(name!=null&&!name.isEmpty())report.append(name).append('\n');
                report.append(String.format(Locale.ROOT,"VID:PID %04X:%04X",vendor,product)).append('\n');
                // bcdDevice is a USB descriptor, not a verified firmware version.
                report.append("USB bcdDevice: ").append(device.getVersion()).append(" (не версия прошивки)\n");
                for(int i=0;i<device.getInterfaceCount();i++){
                    UsbInterface iface=device.getInterface(i);
                    report.append("Интерфейс ").append(iface.getId()).append(" · class ").append(iface.getInterfaceClass()).append('\n');
                    for(int j=0;j<iface.getEndpointCount();j++){
                        UsbEndpoint endpoint=iface.getEndpoint(j);
                        report.append(String.format(Locale.ROOT,"  EP %02X · %s · type %d · %d B\n",endpoint.getAddress(),endpoint.getDirection()==UsbConstants.USB_DIR_IN?"IN":"OUT",endpoint.getType(),endpoint.getMaxPacketSize()));
                    }
                }
            }
            return report.toString().trim();
        }catch(RuntimeException error){return "Не удалось прочитать список USB-устройств: "+error.getClass().getSimpleName();}
    }
}
