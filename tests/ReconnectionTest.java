import app.m30.companion.*;
import java.util.*;

public class ReconnectionTest {
    private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args){
        SourceSelector.Source dinput=new SourceSelector.Source(9,"android-connection","8BitDo M30 gamepad",true,true);
        SourceSelector.Source xinputKeyboard=new SourceSelector.Source(12,"windows-connection","8BitDo M30 gamepad",false,true);
        SourceSelector.Source home=new SourceSelector.Source(3,"power","pmic_pwrkey",false,false);
        SourceSelector.Source keyboard=new SourceSelector.Source(5,"keyboard","Bluetooth Keyboard",false,true);
        check(SourceSelector.pick(List.of(home,dinput),"")==dinput,"first connection selects M30");
        check(SourceSelector.pick(List.of(home,dinput),home.descriptor)==dinput,"stale selection of phone buttons cannot hide connected M30");
        check(SourceSelector.pick(List.of(home,xinputKeyboard),dinput.descriptor)==xinputKeyboard,"mode change with new descriptor and keyboard-only source can still select named M30");
        check(SourceSelector.pick(List.of(home,keyboard),dinput.descriptor)==null,"missing M30 must not select phone buttons or random keyboard");
        check(SourceSelector.pick(List.of(home,keyboard,xinputKeyboard),keyboard.descriptor)==keyboard,"explicit diagnostics source remains selectable");
        check(SourceSelector.discoverable(xinputKeyboard),"source discovery can accept M30 without GAMEPAD flag");
        check(!SourceSelector.discoverable(home),"phone power source cannot win gamepad discovery");
        check(SourceSelector.discoverable(keyboard),"explicit first-key discovery can identify an external keyboard-style source");
        check(SourceSelector.blockNavigation(xinputKeyboard,false,true),"keyboard-style M30 must not navigate UI during a test");
        check(SourceSelector.blockNavigation(null,true,true),"unselected gamepad events must not escape to Android focus navigation");
        check(SourceSelector.blockNavigation(keyboard,false,true),"external keyboard-style fallback source is contained in test mode");
        check(!SourceSelector.blockNavigation(home,false,true),"phone power/volume sources remain outside external-key navigation lock");
        check(!SourceSelector.blockNavigation(xinputKeyboard,true,false),"navigation lock has an explicit off state");
        ModeConfirmation c=new ModeConfirmation();
        c.confirm(1,"windows-connection:12:1");c.observe("windows-connection:12:1");check(c.current()==1,"user confirmation survives unchanged connection");
        c.observe(null);check(c.current()==-1,"disconnect clears user confirmation");
        c.confirm(1,"windows-connection:12:1");c.observe("windows-connection:13:2");check(c.current()==-1,"new connection ID requires fresh confirmation even when name is unchanged");
        c.confirm(1,"windows-connection:12:1");c.observe("android-connection:9:1");check(c.current()==-1,"other source cannot inherit mode confirmation");
        check(ModeDetector.detect("8BitDo M30 gamepad",0x2dc8,0x0651).candidate==0&&c.current()==-1,"identity can estimate D-input without inventing a user confirmation");
        PadState pad=new PadState();pad.bind("C","K:105");pad.bind("R","K:104");
        pad.set("T:18:+",true);check(pad.active("C")&&!pad.active("R"),"Windows RT physical C can arrive as a trigger axis");
        pad.set("K:105",true);pad.set("T:18:+",false);check(pad.active("C"),"trigger axis neutral cannot release a still-held trigger key");
        pad.set("K:105",false);check(!pad.active("C")&&pad.tested("C"),"trigger release clears highlight and retains coverage");
        pad.set("T:23:+",true);check(pad.active("R"),"BRAKE is accepted as a left-trigger alias");
        check(!pad.rawDown().contains("B:L2"),"logical trigger aliases are excluded from raw diagnostics");
        System.out.println("PASS: keyboard-style M30, changed descriptor, explicit source, phone-source exclusion, connection-scoped mode confirmation");
    }
}
