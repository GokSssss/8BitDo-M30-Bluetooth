package app.m30.companion;

import java.util.Arrays;

/** Independently encoded from the M30 branch of the official Windows updater. */
public final class BootUpdateProtocol {
    private BootUpdateProtocol(){}
    public static final long VERSION_ADDRESS=0x01017040L;
    public static final class Request {
        public final int command,length,total;public final long address;
        private final byte[] payload;
        private Request(int command,long address,int length,int total,byte[] payload){
            if(length<0||length>46||total<0||address<0||address>0xffffffffL||payload!=null&&payload.length!=length)throw new IllegalArgumentException("Invalid boot packet");
            this.command=command;this.address=address;this.length=length;this.total=total;this.payload=payload==null?null:payload.clone();
        }
        public byte[] packet(){
            if(command==8)return BootModelProtocol.request();
            byte[] packet=new byte[64];packet[0]=5;put16(packet,1,command);put16(packet,5,length);
            put16(packet,7,BootModelProtocol.crc16(payload==null?new byte[0]:payload,0,payload==null?0:payload.length));
            put32(packet,9,total);put32(packet,13,address);if(payload!=null)System.arraycopy(payload,0,packet,17,payload.length);return packet;
        }
        /** null is an unrelated report; rejection aborts without erasing or retrying. */
        public byte[] decode(byte[] reply){
            if(reply==null||reply.length<6||reply[0]!=0||reply[1]!=5||u16(reply,4)!=command)return null;
            if(u16(reply,2)!=0)throw new IllegalArgumentException("Загрузчик отклонил команду "+command+": "+u16(reply,2));
            if(command==8){Long model=BootModelProtocol.decode(reply);if(model==null)return null;return Arrays.copyOfRange(reply,18,22);}
            if(command==2||command==3||command==4||command==7){if(reply.length>64)throw new IllegalArgumentException("Неверный ACK загрузчика");return new byte[0];}
            if(reply.length!=64||u16(reply,6)!=length||u32(reply,10)!=total||u32(reply,14)!=address)throw new IllegalArgumentException("Неверный адрес или размер ответа загрузчика");
            byte[] data=Arrays.copyOfRange(reply,18,18+length);if(u16(reply,8)!=BootModelProtocol.crc16(data,0,data.length))throw new IllegalArgumentException("CRC ответа загрузчика не совпала");return data;
        }
    }
    public static Request model(){return new Request(8,0,4,4,null);}
    public static Request version(){return new Request(5,VERSION_ADDRESS,4,4,null);}
    public static Request eraseImage(M30Firmware image){return new Request(4,image.second.address,0,image.second.length,null);}
    public static Request eraseVersion(){return new Request(4,VERSION_ADDRESS,0,32,null);}
    public static Request writeChunk(M30Firmware image,int offset){
        if(offset<0||offset>=image.second.length||offset%46!=0)throw new IllegalArgumentException("Invalid M30 chunk offset");
        int length=Math.min(46,image.second.length-offset);
        return new Request(3,image.second.address+offset,length,image.second.length,image.second.chunk(offset,length));
    }
    public static Request saveVersion(M30Firmware image){byte[] value=new byte[4];put32(value,0,image.version);return new Request(2,VERSION_ADDRESS,4,4,value);}
    public static Request reset(){return new Request(7,0,0,0,null);}
    public static long u32(byte[] b,int o){return (b[o]&255L)|((b[o+1]&255L)<<8)|((b[o+2]&255L)<<16)|((b[o+3]&255L)<<24);}
    public static int u16(byte[] b,int o){return (b[o]&255)|((b[o+1]&255)<<8);}
    private static void put16(byte[] b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);}
    private static void put32(byte[] b,int o,long v){for(int i=0;i<4;i++)b[o+i]=(byte)(v>>>(8*i));}
}
