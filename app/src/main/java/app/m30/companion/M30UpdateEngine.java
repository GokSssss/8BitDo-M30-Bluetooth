package app.m30.companion;

/** Sequential M30 3208 transaction. First DAT section is intentionally not flashed on this path. */
public final class M30UpdateEngine {
    public interface Link {byte[] exchange(BootUpdateProtocol.Request request,int timeout)throws Exception;void reset(BootUpdateProtocol.Request request)throws Exception;}
    public interface Progress {void update(int percent,String message);}
    public static final class Probe {public final long model;public final int version;Probe(long model,int version){this.model=model;this.version=version;}}
    private M30UpdateEngine(){}
    public static Probe probe(Link link)throws Exception{
        long model=BootUpdateProtocol.u32(link.exchange(BootUpdateProtocol.model(),3000),0);
        if(model!=M30Firmware.MODEL)throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,"Другой контроллер: модель 0x%08X",model));
        int version=BootUpdateProtocol.u16(link.exchange(BootUpdateProtocol.version(),3000),0);
        if(version<1||version>9999)throw new IllegalArgumentException("Текущая версия не распознана; запись не начата");return new Probe(model,version);
    }
    public static void install(M30Firmware image,Link link,Progress progress)throws Exception{
        if(!OfficialM30Files.recognized(image))throw new IllegalArgumentException("Неизвестный официальный пакет. Запись не начата");
        Probe found=probe(link);if(found.version==image.version)throw new IllegalArgumentException("Эта версия уже установлена; запись не начата");if(found.version>image.version)throw new IllegalArgumentException("Выбранная версия ниже установленной; понижение не поддерживается");runVerified(image,link,progress);
    }
    // Package-private for simulator tests. Production entry always checks the exact official file.
    static void runVerified(M30Firmware image,Link link,Progress progress)throws Exception{
        progress.update(0,"Подготовка памяти M30");link.exchange(BootUpdateProtocol.eraseImage(image),15000);
        link.exchange(BootUpdateProtocol.eraseVersion(),3000);
        for(int offset=0;offset<image.second.length;offset+=46){
            BootUpdateProtocol.Request chunk=BootUpdateProtocol.writeChunk(image,offset);link.exchange(chunk,3000);
            progress.update(Math.min(94,(offset+chunk.length)*94/image.second.length),"Запись M30");
        }
        progress.update(95,"Сохранение версии");link.exchange(BootUpdateProtocol.eraseVersion(),3000);link.exchange(BootUpdateProtocol.saveVersion(image),3000);
        int observed=BootUpdateProtocol.u16(link.exchange(BootUpdateProtocol.version(),3000),0);
        if(observed!=image.version)throw new IllegalStateException("Запись версии не подтверждена; перезапуск не отправлен");
        progress.update(98,"Загрузчик подтвердил запись. Перезапуск M30");link.reset(BootUpdateProtocol.reset());
        // Caller confirms disappearance of the same boot USB device before reporting completion.
    }
}
