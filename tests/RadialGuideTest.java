import app.m30.companion.*;
import java.util.*;
public class RadialGuideTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        Set<String> names=new HashSet<>();
        for(int i=0;i<RadialGuideGeometry.LABELS.length;i++){
            check(names.add(RadialGuideGeometry.LABELS[i]),"No duplicate physical controls");float[] a=RadialGuideGeometry.start(i),b=RadialGuideGeometry.end(i);
            for(int j=0;j<i;j++)check(!RadialGuideGeometry.crosses(a,b,RadialGuideGeometry.start(j),RadialGuideGeometry.end(j)),"Radial leaders must not cross");
            check(b[0]>35&&b[0]<325&&b[1]>20&&b[1]<282,"Action captions stay within the drawing including their side margins");
        }
        check(names.size()==14&&names.containsAll(List.of("L","R","PAIR","START","SELECT","HOME","★","↑ ↓ ← →","A","B","C","X","Y","Z")),"All 14 callouts remain separate");
        for(int mode=0;mode<4;mode++)check(ButtonGuide.caption(new String[]{"L"},ButtonGuide.defaultBindings(mode),mode).equals("—"),"No invented system action for game-only keys");
        check(RadialGuideGeometry.start(0)[0]<RadialGuideGeometry.start(1)[0],"Shoulder callouts anchor to separate left and right keys");
        System.out.println("PASS: approved radial reference, 14 distinct callouts, no crossing leaders, action margins, separate shoulder anchors, game-only dashes");
    }
}
