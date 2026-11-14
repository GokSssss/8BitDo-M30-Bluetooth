import app.m30.companion.*;
public class FirmwareUpdateResultTest {
    private static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
    public static void main(String[] args){
        FirmwareUpdateResult success=FirmwareUpdateResult.completed(201,true,true,"report",123);
        check(success.kind==FirmwareUpdateResult.SUCCESS&&success.title().equals("✓ Прошивка 2.01 установлена"),"Verified update names exact installed version");
        check(FirmwareUpdateResult.completed(201,true,false,"",1).kind==FirmwareUpdateResult.UNCONFIRMED&&FirmwareUpdateResult.completed(201,false,true,"",1).kind==FirmwareUpdateResult.UNCONFIRMED,"Both boot disappearance and runtime must be confirmed for green success");
        check(FirmwareUpdateResult.failed("CRC failure",1).kind==FirmwareUpdateResult.ERROR,"A failed update cannot inherit the last success");
        check(FirmwareUpdateResult.restore(1,201,"report",123).version==201,"Version and success survive process restart independent of USB/probe or selected file");
        check(FirmwareUpdateResult.restore(1,0,"",123).kind==FirmwareUpdateResult.NONE&&FirmwareUpdateResult.restore(1,65535,"",123).kind==FirmwareUpdateResult.NONE,"Corrupt/missing cached version cannot show green success");
        String legacy="M30 Companion 1.29 · USB update\nОфициальный M30 2.01\nSHA-256: hash\n\nОбновление M30 завершено\nВерсия прочитана после записи, команда перезапуска отправлена.";
        FirmwareUpdateResult migrated=FirmwareUpdateResult.fromLegacyReport(legacy);
        check(migrated.kind==1&&migrated.version==201&&migrated.time==0,"Existing user's successful 1.29 update is restored without another flash or invented timestamp");
        check(FirmwareUpdateResult.fromLegacyReport(legacy.replace("Обновление M30 завершено","Обновление остановлено")).kind==0,"Failed legacy report must not migrate as success");
        check(FirmwareUpdateResult.fromLegacyReport("Файл: M30 2.01").kind==0&&FirmwareUpdateResult.fromLegacyReport("Версия: 2.01\nЗапись, стирание и перезапуск не выполнялись.").kind==0,"Selecting or reading version is not an installation");
        check(FirmwareUpdateResult.fromLegacyReport(legacy.replace("2.01","2.00")).version==200,"Migration reads the recorded version, not the selected latest file");
        System.out.println("PASS: firmware success requires verified reconnect, exact version persists, errors/unknown outcomes stay distinct, existing 1.29 report migration without reflashing");
    }
}
