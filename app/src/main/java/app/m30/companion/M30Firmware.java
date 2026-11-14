package app.m30.companion;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;

/** Unmodified, two-section M30 Bluetooth DAT. Structural checks do not authenticate a file. */
public final class M30Firmware {
    public static final int MAX_FILE_SIZE=2*1024*1024;
    public static final long MODEL=0x5006;
    public static final class Image {
        public final int headerOffset,length,version;
        public final long address,model;
        private final byte[] bytes;
        private Image(byte[] bytes,int offset){
            this.bytes=bytes;headerOffset=offset;version=(int)u32(bytes,offset);address=u32(bytes,offset+4);
            long n=u32(bytes,offset+8);model=u32(bytes,offset+12);
            if(n==0||n>MAX_FILE_SIZE||offset+28L+n>bytes.length)throw new IllegalArgumentException("Обрезанная или неверная секция DAT");length=(int)n;
            if(version<1||version>9999)throw new IllegalArgumentException("Неизвестное поле версии DAT");
            for(int i=16;i<28;i+=4)if(u32(bytes,offset+i)!=0)throw new IllegalArgumentException("Неподдерживаемый вариант заголовка DAT");
        }
        public byte[] data(){return Arrays.copyOfRange(bytes,headerOffset+28,headerOffset+28+length);}
        byte[] chunk(int offset,int count){if(offset<0||count<0||offset+count>length)throw new IllegalArgumentException("Invalid section slice");return Arrays.copyOfRange(bytes,headerOffset+28+offset,headerOffset+28+offset+count);}
        public byte[] header(){return Arrays.copyOfRange(bytes,headerOffset,headerOffset+28);}
    }
    public final Image first,second;
    public final int size,version;
    public final String sha256,md5;
    private M30Firmware(byte[] bytes){
        if(bytes.length<56||bytes.length>MAX_FILE_SIZE)throw new IllegalArgumentException("Неверный размер файла M30 DAT");
        first=new Image(bytes,0);int offset=28+first.length;
        if(offset+28>bytes.length)throw new IllegalArgumentException("В пакете M30 отсутствует вторая секция");
        second=new Image(bytes,offset);
        if(offset+28+second.length!=bytes.length)throw new IllegalArgumentException("Лишние или неполные данные после секций M30");
        if(first.model!=0||second.model!=MODEL)throw new IllegalArgumentException("Файл не соответствует M30 Bluetooth 0x5006");
        if(first.address!=0x08003400L||second.address!=0x01018000L)throw new IllegalArgumentException("Неожиданные адреса секций M30");
        if(first.version!=second.version)throw new IllegalArgumentException("Версии секций DAT различаются");
        size=bytes.length;version=first.version;sha256=digest("SHA-256",bytes);md5=digest("MD5",bytes);
    }
    public static M30Firmware parse(byte[] file){if(file==null)throw new IllegalArgumentException("Файл не выбран");if(file.length>MAX_FILE_SIZE)throw new IllegalArgumentException("Файл слишком большой");return new M30Firmware(file.clone());}
    public boolean matchesCatalog(long expectedSize,String expectedMd5){return size==expectedSize&&expectedMd5!=null&&expectedMd5.matches("(?i)[0-9a-f]{32}")&&md5.equalsIgnoreCase(expectedMd5);}
    public String versionText(){return String.format(Locale.ROOT,"%d.%02d",version/100,version%100);}
    public static String digest(String algorithm,byte[] bytes){
        try{byte[] hash=MessageDigest.getInstance(algorithm).digest(bytes);StringBuilder result=new StringBuilder();for(byte b:hash)result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();}
        catch(NoSuchAlgorithmException error){throw new IllegalStateException(error);}
    }
    private static long u32(byte[] b,int offset){return (b[offset]&255L)|((b[offset+1]&255L)<<8)|((b[offset+2]&255L)<<16)|((b[offset+3]&255L)<<24);}
}
