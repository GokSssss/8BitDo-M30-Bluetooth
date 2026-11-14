import app.m30.companion.BootModelProtocol;
import java.util.*;
public class BootModelTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void reject(byte[] packet,String message){try{BootModelProtocol.decode(packet);throw new AssertionError(message);}catch(IllegalArgumentException expected){}}
    public static void main(String[] args){
        byte[] physicalReply=new byte[64];
        byte[] prefix=java.util.HexFormat.of().parseHex("000500000800040000bd040000000000000006500000");
        System.arraycopy(prefix,0,physicalReply,0,prefix.length);
        check(BootModelProtocol.decode(physicalReply)==0x5006L,"real M30 1.23 report: command-8 model 5006 and CRC BD00");
        byte[] request=BootModelProtocol.request();check(request.length==64&&request[0]==5&&request[1]==8,"only model-query command 8 is emitted");for(int i=2;i<64;i++)check(request[i]==0,"model query contains no flash address or payload");
        check(BootModelProtocol.crc16("123456789".getBytes(java.nio.charset.StandardCharsets.US_ASCII),0,9)==0x4b37,"standard CRC16/MODBUS vector");
        byte[] reply=new byte[64];reply[1]=5;reply[4]=8;reply[6]=4;reply[10]=4;reply[18]=6;reply[19]=0x50;
        int crc=BootModelProtocol.crc16(reply,18,4);reply[8]=(byte)crc;reply[9]=(byte)(crc>>>8);
        check(BootModelProtocol.decode(reply)==0x5006L,"generic model decoding must not hard-code Ultimate PID 6007");
        byte[] corrupt=reply.clone();corrupt[18]^=1;reject(corrupt,"corrupt model cannot pass CRC");
        corrupt=reply.clone();corrupt[2]=1;reject(corrupt,"rejected commands must not look successful");
        corrupt=reply.clone();corrupt[10]=5;reject(corrupt,"mismatched total length");
        corrupt=reply.clone();corrupt[14]=1;reject(corrupt,"mismatched address");
        reject(Arrays.copyOf(reply,63),"truncated model response");
        check(BootModelProtocol.decode(new byte[0])==null&&BootModelProtocol.decode(new byte[64])==null,"unrelated HID reports ignored");
        corrupt=reply.clone();corrupt[4]=7;check(BootModelProtocol.decode(corrupt)==null,"another command's response must not be used as model");
        System.out.println("PASS: model-only request, CRC reference vector, model decode, rejected/corrupt/truncated/mismatched responses");
    }
}
