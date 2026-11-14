package app.m30.companion;

import java.util.Locale;
/** Independently encoded read-only M30 version query, documented from official
 * web.8bitdo.com connectDevice-CqNxlsyD.js: De -> Ie, BOOT_READ_DATA_CMD=0xC2,
 * default version field OH=0x01017040. Only used after model 0x5006. */
public final class BootVersionProtocol {
    public static final long ADDRESS=0x01017040L;
    private BootVersionProtocol(){}
    public static byte[] request(){
        byte[] packet=new byte[64];packet[0]=5;packet[1]=(byte)0xc2;packet[5]=4;packet[9]=4;
        for(int i=0;i<4;i++)packet[13+i]=(byte)(ADDRESS>>>(8*i));return packet;
    }
    private static int u16(byte[] p,int i){return (p[i]&255)|((p[i+1]&255)<<8);}
    private static long u32(byte[] p,int i){return (long)u16(p,i)|((long)u16(p,i+2)<<16);}
    public static byte[] decode(byte[] reply){
        if(reply.length<6||reply[0]!=0||reply[1]!=5||u16(reply,4)!=0xc2)return null;
        if(u16(reply,2)!=0)throw new IllegalArgumentException("Загрузчик отклонил чтение версии: "+u16(reply,2));
        if(reply.length!=64||u16(reply,6)!=4||u32(reply,10)!=4||u32(reply,14)!=ADDRESS)throw new IllegalArgumentException("Не совпадают размер или адрес ответа версии");
        if(u16(reply,8)!=BootModelProtocol.crc16(reply,18,4))throw new IllegalArgumentException("CRC ответа версии не совпала");
        return java.util.Arrays.copyOfRange(reply,18,22);
    }
    public static String version(byte[] bytes){
        if(bytes.length!=4)throw new IllegalArgumentException("Ожидаются 4 байта версии");
        int raw=u16(bytes,0);if(raw==0||raw>9999)return "Не распознана (поле "+raw+")";
        return String.format(Locale.ROOT,"%d.%02d",raw/100,raw%100);
    }
}
