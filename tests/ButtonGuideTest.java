import app.m30.companion.*;
import java.util.*;

public class ButtonGuideTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static String button(int mode,String label){return ButtonGuide.forButton(label,ButtonGuide.defaultBindings(mode).get(label),mode);}
    public static void main(String[] args){
        check(button(0,"B").contains("Назад")&&button(0,"A").contains("открыть"),"Android physical A selects and B goes back in the default diagram profile");
        check(button(2,"A").contains("Назад")&&button(2,"B").contains("открыть"),"Switch profile reverses the physical A/B roles on Android");
        check(button(0,"X").contains("Удалить")&&button(2,"Y").contains("Удалить"),"text deletion follows the actual Android BUTTON_X code, not its physical label");
        check(button(1,"C").contains("RT / R2")&&button(1,"R").contains("LT / L2"),"Windows trigger aliases are gaming input, not invented system navigation");
        check(ButtonGuide.keyAction(97).contains("Назад")&&ButtonGuide.keyAction(96).contains("открыть"),"live meaning uses the received Android code independently of the chosen physical profile");
        check(ButtonGuide.keyAction(3).contains("Главный экран")&&ButtonGuide.keyAction(187).contains("Недавние"),"HOME and recent-app keys remain distinct");
        check(ButtonGuide.keyAction(110).contains("зависит"),"MODE does not promise that Android performed HOME");
        check(ButtonGuide.forButton("★",null,2).contains("на Switch")&&ButtonGuide.forButton("★",null,0).contains("Turbo"),"hardware star description changes with the chosen mode without promising an Android screenshot");
        check(ButtonGuide.action(null).contains("не определён")&&ButtonGuide.action("S:786").equals(ButtonGuide.keyAction(0)),"unmapped/scan-only input cannot invent an OS shortcut");
        check(ButtonGuide.action("A:15:-").contains("меню")&&ButtonGuide.action("A:11:+").contains("приложение"),"d-pad and other stick axes are described separately");
        ControllerSession first=new ControllerSession(0),second=new ControllerSession(2);
        first.state.bindings.putAll(ButtonGuide.defaultBindings(0));second.state.bindings.putAll(ButtonGuide.defaultBindings(2));
        first.state.bind("HOME","K:4");
        check(ButtonGuide.forButton("HOME",first.state.bindings.get("HOME"),first.mode).contains("Назад"),"a corrected visual label describes its code instead of pretending the label remaps Android");
        check(ButtonGuide.forButton("HOME",second.state.bindings.get("HOME"),second.mode).contains("Главный экран"),"the other controller retains its own mapping");
        List<String[]> callouts=new ArrayList<>();callouts.addAll(Arrays.asList(ButtonGuide.leftLabels()));callouts.addAll(Arrays.asList(ButtonGuide.rightLabels()));
        Set<String> labels=new HashSet<>();for(String[] callout:callouts)for(String label:callout)check(labels.add(label),"each physical button is described exactly once");
        check(labels.containsAll(Arrays.asList(PadState.LABELS))&&labels.contains("PAIR"),"compact rows still describe all buttons including PAIR");
        first.state.bind("C","K:187");first.state.bind("↑","K:4");first.state.set("K:187",true);
        boolean recent=false,back=false;for(String[] callout:callouts){
            String caption=ButtonGuide.caption(callout,first.state.bindings,first.mode);
            if(Arrays.asList(callout).contains("C"))recent=caption.contains("Недавние");
            if(Arrays.asList(callout).contains("↑"))back=caption.contains("↑ · Назад");
        }
        check(recent&&back,"custom system actions cannot disappear inside grouped game buttons or directions");
        first.state.release();
        check(ButtonGuide.caption(new String[]{"A"},second.state.bindings,2).equals("Назад")&&ButtonGuide.caption(new String[]{"B"},second.state.bindings,2).contains("открыть"),"short callouts preserve the Switch A/B distinction");
        check(ButtonGuide.caption(new String[]{"L","R"},second.state.bindings,2).equals("—"),"ordinary game-only shoulders share one informational caption");
        check(first.state.down.isEmpty()&&second.state.down.isEmpty()&&first.state.presses==1&&second.state.presses==0,"looking up help never injects or counts a press");
        PadState observed=new PadState();observed.bindings.putAll(ButtonGuide.defaultBindings(3));
        check(!ButtonGuide.observeM30Home(observed,"USB Keyboard",0,98),"a keyboard BUTTON_C must not alter controller mapping");
        check(!ButtonGuide.observeM30Home(observed,"8BitDo M30 gamepad",0x2dc8,103),"no inferred correction without observed code 98");
        check(ButtonGuide.observeM30Home(observed,"8BitDo M30 gamepad",0x2dc8,98),"observed M30 heart code gets a physical visual binding");
        observed.set("K:98",true);check(observed.active("HOME")&&!observed.active("C"),"heart highlights on down without changing the received raw token");
        observed.set("K:98",false);check(!observed.active("HOME")&&observed.tested("HOME"),"heart releases and retains tested state");
        observed.bind("HOME","K:4");check(!ButtonGuide.observeM30Home(observed,"8BitDo M30",0x2dc8,98),"explicit alternate HOME mapping wins");
        observed.bind("HOME","K:110");observed.bind("C","K:98");check(!ButtonGuide.observeM30Home(observed,"8BitDo M30",0x2dc8,98),"code already owned by C must not move silently");
        System.out.println("PASS: mode-specific button help, raw-code semantics, trigger axes, conditional HOME, unknown keys, local mapping and controller isolation");
    }
}
