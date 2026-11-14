package app.m30.companion;

/** One source of truth for the selected mode and its connection instructions. */
public final class ConnectionGuide {
    private static final String[] NAMES={"Android · D-input","Windows · X-input","Switch","macOS"};
    private static final String[] KEYS={"B + START","X + START","Y + START","A + START"};
    private static final String[] SIGNAL={"мигания LED 1","мигания LED 1 и LED 2","бегущих огней","мигания LED 1, LED 2 и LED 3"};
    private static final String[] DESCRIPTION={"D-input: Android, Android TV и эмуляторы Sega. ★ — Turbo.","X-input: может работать и на Android. C/R могут приходить осями RT/LT. ★ — Turbo.","Nintendo Switch. ★ — снимок экрана; Turbo нет.","Для macOS. На Android могут отличаться коды и оси."};
    private static void validate(int mode){if(mode<0||mode>=NAMES.length)throw new IllegalArgumentException("Unknown mode");}
    public static String[] names(){return NAMES.clone();}
    public static String shortKey(int mode){validate(mode);return new String[]{"START + B","START + X","START + Y","START + A"}[mode];}
    public static String[] keys(){return KEYS.clone();}
    public static String description(int mode){validate(mode);return DESCRIPTION[mode];}
    public static String ledDescription(int mode){validate(mode);return new String[]{"1 из 4: мигает LED 1","2 из 4: мигают LED 1 и LED 2","4 LED: бегущие огни слева направо","3 из 4: мигают LED 1, LED 2 и LED 3"}[mode];}
    public static String[] steps(int mode){
        validate(mode);
        return new String[]{"1. START — 3 секунды, до погасания LED.","2. На выключенном M30 держите "+KEYS[mode]+" до включения и "+SIGNAL[mode]+". Отпустите обе кнопки.","3. PAIR — 2 секунды, до бегущей индикации. Отпустите и выберите M30 в Bluetooth."};
    }
    public static String[] compactSteps(int mode){
        validate(mode);
        return new String[]{"1. START · 3 с — выключить M30.","2. "+KEYS[mode]+" — держать на выключенном M30 до "+SIGNAL[mode]+". Отпустить обе кнопки.","3. PAIR · 2 с — до бегущих огней. Отпустить и выбрать M30 в Bluetooth."};
    }
    public static String connectedLeds(){return "После PAIR: один постоянный огонёк — статус подключения, не режим.";}
    public static String full(int mode){return String.join("\n\n",steps(mode))+"\n\n"+connectedLeds()+"\n\n4. Откройте «Кнопки» и нажмите A или B. Если M30 не выбран — «Найти M30 нажатием».";}
}
