package app.m30.companion;

/** Exact unchanged files retrieved repeatedly over HTTPS from 8BitDo support.
 * SHA-256 pins attest to those observed bytes, not a vendor digital signature.
 * Catalog MD5 discrepancy is explicitly reported and is never called a match. */
public final class OfficialM30Files {
    private OfficialM30Files(){}
    public static boolean recognized(M30Firmware file){
        return file.version==201&&file.size==151608&&file.sha256.equals("158c4b39757048204eba76cd94e33faf087408a6ad79f027b6253ab0a69d5287")
            ||file.version==200&&file.size==149560&&file.sha256.equals("46c06f9ef51f11833bc7d94aab917e061273c16cc5ffabdc364acc11f512bde9");
    }
    public static String url(int version){return version==201?"https://support.8bitdo.com/bd-uploads/files/old/9bf8ddad-4f37-4297-9fa1-f48a48c5dbc6.dat":version==200?"https://support.8bitdo.com/bd-uploads/files/old/0d6ccc95-a085-4978-aea6-c7641b88fb93.dat":null;}
    public static String integrityNote(M30Firmware file){
        return recognized(file)?"SHA-256 совпадает с неизменённым файлом, полученным с HTTPS-сервера 8BitDo. MD5 этого файла отличается от MD5 каталога производителя; причина расхождения не установлена.":"Файл не совпадает с известными официальными пакетами M30. Запись недоступна.";
    }
}
