import app.m30.companion.ConnectionGuide;
public class ConnectionGuideTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        String[] shortKeys={"START + B","START + X","START + Y","START + A"};
        for(int mode=0;mode<4;mode++)check(ConnectionGuide.shortKey(mode).equals(shortKeys[mode]),"Mode tile startup chord");
        String[] expected={"B + START","X + START","Y + START","A + START"};
        for(int mode=0;mode<4;mode++){
            String body=String.join("\n",ConnectionGuide.steps(mode));
            check(body.contains(expected[mode]),"selected mode must determine startup combination");
            for(int other=0;other<4;other++)if(other!=mode)check(!body.contains(expected[other]),"instructions must not retain another mode's combination");
            String compact=String.join("\n",ConnectionGuide.compactSteps(mode));
            check(compact.contains(expected[mode])&&compact.contains("выключенном M30")&&compact.contains("Отпустить")&&compact.contains("Bluetooth"),"Compact instructions preserve startup, release and pairing destination");
            for(int other=0;other<4;other++)if(other!=mode)check(!compact.contains(expected[other]),"Compact instructions must not mix profiles");
            check(ConnectionGuide.full(mode).contains(expected[mode]),"short and detailed instructions must agree");
        }
        check(ConnectionGuide.steps(1)[1].contains("X + START"),"Windows must never show Android B + START instructions");
        check(ConnectionGuide.steps(2)[1].contains("бегущих огней"),"Switch must show its own startup signal");
        check(ConnectionGuide.steps(3)[1].contains("LED 1, LED 2 и LED 3"),"macOS must show its own startup signal");
        System.out.println("PASS: selected-mode instructions, no stale combinations, Windows X+START, matching short/full guides, per-mode signals");
    }
}
