package app.m30.companion;

import java.util.Locale;

/** Identifies compatibility hints, never asserts the controller's firmware mode. */
public final class ModeDetector {
    /** A saved per-device choice wins; an unseen peer gets its own initial estimate. */
    public static int profileOnConnect(Integer saved,int fallback,String name,int vendor,int product){
        if(saved!=null&&saved>=0&&saved<4)return saved;
        int candidate=detect(name,vendor,product).candidate;
        return candidate>=0&&candidate<4?candidate:fallback>=0&&fallback<4?fallback:0;
    }
    public static final class Result {
        public final int candidate;
        public final String title, evidence;
        Result(int candidate, String title, String evidence) {
            this.candidate=candidate; this.title=title; this.evidence=evidence;
        }
    }
    public static Result detect(String name, int vendor, int product) {
        if(name==null) return new Result(-1,"Нет подключённого устройства", "Подключите M30, чтобы проверить имя и VID/PID, доступные Android.");
        String n=name.toLowerCase(Locale.ROOT);
        String id=String.format(Locale.ROOT,"VID:PID %04X:%04X",vendor,product);
        if((vendor==0x057e && product==0x2009) || n.contains("pro controller"))
            return new Result(2,"Вероятно: Switch", "Признак: "+(vendor==0x057e&&product==0x2009?id:"имя «"+name+"»")+". Эти признаки есть и у других контроллеров; они не доказывают режим прошивки M30.");
        if((vendor==0x045e && (product==0x028e||product==0x02e0||product==0x02fd||product==0x0b13)) || n.contains("x-input") || n.contains("xinput") || n.contains("xbox"))
            return new Result(1,"Вероятно: Windows · X-input", "Признак: "+id+", имя «"+name+"». Android показывает совместимое устройство, а не положение переключателя режима M30.");
        if(n.contains("d-input") || n.contains("dinput"))
            return new Result(0,"Вероятно: Android · D-input", "Признак: режим указан в имени «"+name+"». Имя зависит от прошивки и само по себе не является подтверждением.");
        if(n.contains("macos") || n.contains("mac os"))
            return new Result(3,"Вероятно: macOS", "Признак: имя «"+name+"». Требуется сверить с сочетанием при включении контроллера.");
        // Known M30 Bluetooth Android/D-input identity, also observed in the user's report.
        // This is a compatibility estimate, not a firmware mode register read.
        if(vendor==0x2dc8&&product==0x0651&&n.contains("m30"))
            return new Result(0,"Вероятно: Android · D-input", "Признак: M30 с "+id+" — известный вариант Bluetooth Android / D-input. Это предположение по идентификатору подключения, не чтение режима из прошивки.");
        return new Result(-1,"Режим не определяется однозначно", "Android сообщает «"+name+"», "+id+". Имя M30, коды кнопок и оси не различают все режимы и прошивки. Проверьте сочетание при включении; выбранный ниже профиль — ваша отметка.");
    }
}
