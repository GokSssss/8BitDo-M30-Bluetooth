package app.m30.companion;

/** Read-only command 8 of the 2DC8:3208 boot protocol.
 * Wire-format reference: alphaorderly/ultimate-bluetooth-updater protocol.py (MIT).
 * No erase, flash read/write, reboot or firmware commands are implemented. */
public final class BootModelProtocol {
    private BootModelProtocol(){}
    public static byte[] request(){byte[] packet=new byte[64];packet[0]=5;packet[1]=8;return packet;}
    public static int crc16(byte[] bytes,int offset,int length){
        int crc=0xffff;for(int i=offset;i<offset+length;i++){crc^=bytes[i]&255;for(int bit=0;bit<8;bit++)crc=(crc>>>1)^((crc&1)!=0?0xa001:0);}return crc;
    }
    private static int u16(byte[] b,int offset){return (b[offset]&255)|((b[offset+1]&255)<<8);}
    private static long u32(byte[] b,int offset){return (long)u16(b,offset)|((long)u16(b,offset+2)<<16);}
    /** Null means unrelated report; malformed command-8 replies fail explicitly. */
    public static Long decode(byte[] reply){
        if(reply.length<6||reply[0]!=0||reply[1]!=5||u16(reply,4)!=8)return null;
        if(u16(reply,2)!=0)throw new IllegalArgumentException("Загрузчик отклонил запрос модели: "+u16(reply,2));
        if(reply.length!=64||u16(reply,6)!=4||u32(reply,10)!=4||u32(reply,14)!=0)throw new IllegalArgumentException("Некорректная длина или адрес ответа модели");
        if(u16(reply,8)!=crc16(reply,18,4))throw new IllegalArgumentException("CRC ответа модели не совпала");
        return u32(reply,18);
    }
}
