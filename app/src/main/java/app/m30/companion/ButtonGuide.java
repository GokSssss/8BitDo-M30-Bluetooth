package app.m30.companion;

import java.util.LinkedHashMap;
import java.util.Map;

/** Typical Android menu behavior, not a measurement of another app's actions.
 * Fallback reference: AOSP data/keyboards/Generic.kcm. Physical correspondence
 * remains the selected diagram profile, which can differ on another firmware. */
public final class ButtonGuide {
    private ButtonGuide(){}
    public static String[][] leftLabels(){return new String[][]{{"L","R"},{"↑","↓","←","→"},{"START"},{"★"},{"SELECT"}};}
    public static String[][] rightLabels(){return new String[][]{{"PAIR"},{"Z"},{"Y"},{"X"},{"C"},{"B"},{"A"},{"HOME"}};}

    public static String caption(String[] labels,Map<String,String> bindings,int mode){
        String first=compact(forButton(labels[0],bindings.get(labels[0]),mode));boolean same=true,directions=true;
        StringBuilder individual=new StringBuilder();
        for(String label:labels){String action=forButton(label,bindings.get(label),mode);same&=first.equals(compact(action));directions&=action.endsWith("по меню");if(individual.length()>0)individual.append('\n');individual.append(label).append(" · ").append(compact(action));}
        return directions?"Перемещение":same?first:individual.toString();
    }
    private static String compact(String action){
        if(action.startsWith("Игровая ")||action.startsWith("Игровой курок "))return "—";
        if(action.startsWith("Код не определён"))return "Проверить код";
        if(action.startsWith("Turbo:"))return "Turbo: кнопка + ★";
        if(action.startsWith("Снимок экрана на Switch"))return "Снимок на Switch";
        if(action.startsWith("Сопряжение:"))return "Поиск пары · 2 с";
        return switch(action){
            case "Выбрать / открыть выделенное" -> "Выбрать / открыть";
            case "Назад / отмена" -> "Назад";
            case "Удалить символ в поле ввода" -> "Стереть символ";
            case "Пробел; в меню — выбор" -> "Пробел / выбор";
            case "Выбрать / открыть; в игре — START" -> "Выбрать / START";
            case "Меню приложения, если поддерживается" -> "Меню";
            case "Главный экран Android","Главный экран — зависит от Android" -> "Главный экран";
            case "Действие задаёт приложение" -> "В приложении";
            default -> action;
        };
    }

    public static Map<String,String> defaultBindings(int mode){
        if(mode<0||mode>3)throw new IllegalArgumentException("Unknown mode");
        LinkedHashMap<String,String> bindings=new LinkedHashMap<>();
        int[] keys={19,20,21,22,96,97,103,99,100,102,104,105,108,109,110};
        for(int i=0;i<keys.length;i++)bindings.put(PadState.LABELS[i],(i<4?"D:":"K:")+keys[i]);
        bindings.put("L","B:L2");bindings.put("R","B:R2");
        if(mode==1){bindings.put("C","B:R2");bindings.put("Z","K:103");bindings.put("L","K:102");bindings.put("R","B:L2");}
        if(mode==2){bindings.put("A","K:97");bindings.put("B","K:96");bindings.put("X","K:100");bindings.put("Y","K:99");}
        return bindings;
    }

    /** User-confirmed M30 heart -> BUTTON_C compatibility fallback, only after that raw code arrives.
     * Custom calibration and any existing physical owner of K:98 must be preserved. */
    public static boolean observeM30Home(PadState state,String name,int vendor,int code){
        String n=name==null?"":name.toLowerCase(java.util.Locale.ROOT);
        if(code!=98||!n.contains("m30")||!(n.contains("8bitdo")||vendor==0x2dc8)||!"K:110".equals(state.bindings.get("HOME"))||state.bindings.containsValue("K:98"))return false;
        state.bind("HOME","K:98");return true;
    }

    public static String action(String token){
        if(token==null)return "Код не определён — проверьте во вкладке «Кнопки»";
        if(token.equals("B:L2")||token.equals("T:17:+")||token.equals("T:23:+"))return "Игровой курок LT / L2";
        if(token.equals("B:R2")||token.equals("T:18:+")||token.equals("T:22:+"))return "Игровой курок RT / R2";
        if(token.startsWith("A:"))return token.startsWith("A:0:")||token.startsWith("A:1:")||token.startsWith("A:15:")||token.startsWith("A:16:")?"Перемещение по меню":"Ось стика — действие задаёт приложение";
        if(token.startsWith("K:")||token.startsWith("D:")){
            try{return keyAction(Integer.parseInt(token.substring(2)));}catch(NumberFormatException ignored){}
        }
        return "Действие задаёт приложение";
    }

    public static String keyAction(int code){
        return switch(code){
            case 3 -> "Главный экран Android";
            case 4,97 -> "Назад / отмена";
            case 19 -> "Вверх по меню";
            case 20 -> "Вниз по меню";
            case 21 -> "Влево по меню";
            case 22 -> "Вправо по меню";
            case 23,66,96,98,101,106,107 -> "Выбрать / открыть выделенное";
            case 62,100 -> "Пробел; в меню — выбор";
            case 67,99 -> "Удалить символ в поле ввода";
            case 82,109 -> "Меню приложения, если поддерживается";
            case 102 -> "Игровая L1 — действие задаёт приложение";
            case 103 -> "Игровая R1 — действие задаёт приложение";
            case 104 -> "Игровой курок LT / L2";
            case 105 -> "Игровой курок RT / R2";
            case 108 -> "Выбрать / открыть; в игре — START";
            case 110 -> "Главный экран — зависит от Android";
            case 111 -> "Escape / отмена — зависит от приложения";
            case 120 -> "Снимок экрана, если приложение не обработало код";
            case 187 -> "Недавние приложения";
            default -> "Действие задаёт приложение";
        };
    }

    public static String forButton(String label,String token,int mode){
        if("PAIR".equals(label))return "Сопряжение: держать 2 секунды";
        if("★".equals(label)&&token==null)return mode==2?"Снимок экрана на Switch; на Android зависит от раскладки":"Turbo: держите игровую кнопку и нажмите ★";
        return action(token);
    }
}
