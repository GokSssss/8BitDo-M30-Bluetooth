package app.m30.companion;
import java.util.*;
import java.nio.file.*;
import java.io.*;
public class BootUpdateTest {
    private static void check(boolean v,String s){if(!v)throw new AssertionError(s);}
    private static void put(byte[] b,int o,long v,int count){for(int i=0;i<count;i++)b[o+i]=(byte)(v>>>(8*i));}
    private static M30Firmware fixture(){byte[] b=new byte[56+16+100];put(b,0,201,4);put(b,4,0x08003400,4);put(b,8,16,4);put(b,44,201,4);put(b,48,0x01018000,4);put(b,52,100,4);put(b,56,0x5006,4);for(int i=72;i<b.length;i++)b[i]=(byte)i;return M30Firmware.parse(b);}
    private static byte[] reply(BootUpdateProtocol.Request r,byte[] data){byte[] b=new byte[64];b[1]=5;put(b,4,r.command,2);if(data!=null){put(b,6,data.length,2);put(b,8,BootModelProtocol.crc16(data,0,data.length),2);put(b,10,data.length,4);put(b,14,r.address,4);System.arraycopy(data,0,b,18,data.length);}return b;}
    private static final class Sim implements M30UpdateEngine.Link {
        ArrayList<Integer> cmds=new ArrayList<>();ByteArrayOutputStream written=new ByteArrayOutputStream();int version=200,fail=-1,expectedVersion=201;long model=0x5006;boolean reset,dropVersion;
        public byte[] exchange(BootUpdateProtocol.Request r,int timeout)throws Exception{
            cmds.add(r.command);if(cmds.size()==fail)throw new IOException("injected disconnect");byte[] p=r.packet();
            check(p.length==64&&p[0]==5,"Exact HID report size");check(r.command==8||BootUpdateProtocol.u16(p,7)==BootModelProtocol.crc16(p,17,r.command==5?0:r.length),"Native M30 CRC covers actual request payload, empty requests use FFFF");
            if(r.command==8){byte[] id=new byte[4];put(id,0,model,4);return r.decode(reply(r,id));}
            if(r.command==5)return r.decode(reply(r,new byte[]{(byte)version,(byte)(version>>>8),0,0}));
            if(r.command==3){check(r.address==0x01018000L+written.size(),"Only the PID-5006 image is written, contiguously");written.write(p,17,r.length);}
            if(r.command==2){check(r.address==0x01017040&&r.length==4,"Version-only final plain write");if(!dropVersion)version=BootUpdateProtocol.u16(p,17);}
            return r.decode(reply(r,null));
        }
        public void reset(BootUpdateProtocol.Request r){check(r.command==7&&version==expectedVersion,"Reset only after version is saved and verified");cmds.add(7);reset=true;}
    }
    private static void exercise(M30Firmware image,boolean publicPath)throws Exception{
        Sim s=new Sim();s.version=image.version-1;s.expectedVersion=image.version;ArrayList<Integer> progress=new ArrayList<>();
        if(publicPath)M30UpdateEngine.install(image,s,(n,m)->progress.add(n));else M30UpdateEngine.runVerified(image,s,(n,m)->progress.add(n));
        check(s.reset&&Arrays.equals(s.written.toByteArray(),image.second.data()),"Exact official encrypted section preserved without modifying payloads");
        check(s.cmds.subList(publicPath?2:0,(publicPath?2:0)+2).equals(List.of(4,4)),"Image and version area prepared in native order");
        check(s.cmds.subList(s.cmds.size()-4,s.cmds.size()).equals(List.of(4,2,5,7)),"Erase metadata, save version, read back, then reset");
        check(progress.get(progress.size()-1)==98,"Engine cannot claim 100% before USB reconnect verification");
        for(int i=1;i<progress.size();i++)check(progress.get(i)>=progress.get(i-1),"Monotonic progress");
    }
    public static void main(String[] args)throws Exception{
        M30Firmware image=fixture();exercise(image,false);
        Sim untouched=new Sim();try{M30UpdateEngine.install(image,untouched,(n,m)->{});throw new AssertionError("Unknown file accepted");}catch(IllegalArgumentException expected){}check(untouched.cmds.isEmpty(),"Unknown bytes rejected before any USB command");
        for(int fail=1;fail<=5;fail++){Sim s=new Sim();s.fail=fail;try{M30UpdateEngine.runVerified(image,s,(n,m)->{});throw new AssertionError("Failure not propagated");}catch(IOException expected){}check(s.cmds.size()==fail&&!s.reset,"Disconnect/rejection stops transaction without retries or reset");}
        BootUpdateProtocol.Request r=BootUpdateProtocol.version();byte[] good=reply(r,new byte[]{(byte)201,0,0,0});check(r.decode(good)[0]==(byte)201,"Native command-5 version reply");
        byte[] bad=good.clone();bad[18]^=1;try{r.decode(bad);throw new AssertionError("Bad CRC accepted");}catch(IllegalArgumentException expected){}
        bad=good.clone();bad[14]^=1;try{r.decode(bad);throw new AssertionError("Wrong address accepted");}catch(IllegalArgumentException expected){}
        bad=reply(BootUpdateProtocol.eraseImage(image),null);bad[2]=1;try{BootUpdateProtocol.eraseImage(image).decode(bad);throw new AssertionError("Rejected erase accepted");}catch(IllegalArgumentException expected){}
        check(r.decode(reply(BootUpdateProtocol.model(),new byte[]{6,0x50,0,0}))==null,"Unrelated asynchronous report ignored");
                Sim wrong=new Sim();wrong.model=0x6007;
        try{M30UpdateEngine.probe(wrong);throw new AssertionError("Another model passed preflight");}catch(IllegalArgumentException expected){}check(wrong.cmds.equals(List.of(8)),"Wrong model stops before version and erase");
        Sim lostVersion=new Sim();lostVersion.dropVersion=true;
        try{M30UpdateEngine.runVerified(image,lostVersion,(n,m)->{});throw new AssertionError("Failed metadata write accepted");}catch(IllegalStateException expected){}check(!lostVersion.reset,"No reset if version readback mismatches");
        for(String path:args){M30Firmware real=M30Firmware.parse(Files.readAllBytes(Path.of(path)));exercise(real,true);Sim newer=new Sim();newer.version=real.version+1;
            try{M30UpdateEngine.install(real,newer,(n,m)->{});throw new AssertionError("Downgrade accepted");}catch(IllegalArgumentException expected){}check(newer.cmds.equals(List.of(8,5)),"Downgrade stops before any erase/write");
            Sim same=new Sim();same.version=real.version;try{M30UpdateEngine.install(real,same,(n,m)->{});throw new AssertionError("Unnecessary same-version reinstall accepted");}catch(IllegalArgumentException expected){}check(same.cmds.equals(List.of(8,5)),"Already installed version stops before any erase/write");
        }
        System.out.println("PASS: native command-5 read, M30 CRC, exact encrypted section, native erase/write/version/reset order, source pin, ACK validation, disconnect stops, progress never announces premature success");
    }
}
