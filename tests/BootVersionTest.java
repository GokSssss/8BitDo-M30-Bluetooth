import app.m30.companion.*;
import java.util.*;
public class BootVersionTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void reject(byte[] packet){try{BootVersionProtocol.decode(packet);throw new AssertionError("Bad response accepted");}catch(IllegalArgumentException expected){}}
    public static void main(String[] args){
        byte[] query=BootVersionProtocol.request();
        byte[] prefix=HexFormat.of().parseHex("05c2000000040000000400000040700101");
        check(query.length==64&&Arrays.equals(Arrays.copyOf(query,prefix.length),prefix),"official M30 read: cmd C2, 4 bytes, total 4, address 01017040");for(int i=17;i<64;i++)check(query[i]==0,"no write payload");
        byte[] r=new byte[64];r[1]=5;r[4]=(byte)0xc2;r[6]=4;r[10]=4;r[14]=0x40;r[15]=0x70;r[16]=1;r[17]=1;r[18]=(byte)201;
        int crc=BootModelProtocol.crc16(r,18,4);r[8]=(byte)crc;r[9]=(byte)(crc>>>8);
        check(BootVersionProtocol.version(BootVersionProtocol.decode(r)).equals("2.01"),"201 formats as 2.01, not 2.10");
        check(BootVersionProtocol.version(new byte[]{100,0,0,0}).equals("1.00"),"100 formats as 1.00");
        check(BootVersionProtocol.version(new byte[]{-1,-1,-1,-1}).contains("Не распознана"),"erased flash isn't a version");
        check(BootVersionProtocol.version(new byte[4]).contains("Не распознана"),"empty version isn't confirmation");
        byte[] bad=r.clone();bad[18]^=1;reject(bad);bad=r.clone();bad[14]^=1;reject(bad);bad=r.clone();bad[2]=1;reject(bad);reject(Arrays.copyOf(r,63));
        check(BootVersionProtocol.decode(new byte[64])==null,"unrelated report ignored");
        System.out.println("PASS: official M30 version read packet, decimal version, unknown fields, CRC/address/status/length guards");
    }
}
