import app.m30.companion.*;
import java.util.*;
import java.nio.file.*;
public class M30FirmwareTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void put(byte[] b,int o,long v){for(int i=0;i<4;i++)b[o+i]=(byte)(v>>>(8*i));}
    private static byte[] fixture(){byte[] b=new byte[56+16+32];put(b,0,201);put(b,4,0x08003400);put(b,8,16);put(b,44,201);put(b,48,0x01018000);put(b,52,32);put(b,56,0x5006);for(int i=28;i<44;i++)b[i]=(byte)i;for(int i=72;i<b.length;i++)b[i]=(byte)(i+1);return b;}
    private static void rejects(byte[] b,String message){try{M30Firmware.parse(b);throw new AssertionError(message);}catch(IllegalArgumentException expected){}}
    public static void main(String[] args)throws Exception{
        byte[] file=fixture();M30Firmware image=M30Firmware.parse(file);
        check(image.versionText().equals("2.01")&&image.first.length==16&&image.second.length==32&&image.second.headerOffset==44,"Both opaque payloads and decimal version are preserved");
        check(image.matchesCatalog(file.length,M30Firmware.digest("MD5",file).toUpperCase(Locale.ROOT)),"Catalog comparison accepts exact bytes and size");
        check(!image.matchesCatalog(file.length+1,image.md5)&&!image.matchesCatalog(file.length,"00000000000000000000000000000000")&&!image.matchesCatalog(file.length,null),"A structure/model match must not hide missing or mismatched catalog integrity");
        byte[] payload=image.second.data();payload[0]^=1;file[72]^=1;check(image.second.data()[0]==73,"Input and returned arrays cannot mutate a parsed image");
        byte[] bad=fixture();put(bad,56,0x6007);rejects(bad,"Another model must not pass as M30");
        bad=fixture();put(bad,8,0xfffffff0L);rejects(bad,"Unsigned lengths cannot overflow");
        bad=fixture();put(bad,52,0);rejects(bad,"Zero-length image rejected");
        bad=fixture();put(bad,48,0x01017040);rejects(bad,"Firmware cannot target the bootloader version area");
        bad=fixture();put(bad,44,200);rejects(bad,"Mixed section versions rejected");
        bad=fixture();put(bad,60,1);rejects(bad,"Unknown header variants fail closed");
        rejects(Arrays.copyOf(fixture(),43),"Truncated first image rejected");rejects(Arrays.copyOf(fixture(),60),"Truncated second image rejected");rejects(Arrays.copyOf(fixture(),105),"Trailing bytes rejected");rejects(new byte[M30Firmware.MAX_FILE_SIZE+1],"Oversize file rejected");
        if(args.length>0){M30Firmware real=M30Firmware.parse(Files.readAllBytes(Path.of(args[0])));check(real.first.length==80384&&real.second.length==71168&&real.version==201,"Actual official DAT layout");check(real.sha256.equals("158c4b39757048204eba76cd94e33faf087408a6ad79f027b6253ab0a69d5287"),"Observed HTTPS file is reproducible");check(!real.matchesCatalog(151608,"424780ACCB0317D8CB9AAA6128C50DE2"),"Actual catalog MD5 discrepancy remains visible");}
        System.out.println("PASS: two-section M30 DAT, immutable bytes, model/address/version bounds, truncation/overflow rejection, catalog integrity mismatch");
    }
}
