package app.m30.companion;

/** Coordinates of the approved 360 x 302 informational sketch. No input or animation. */
public final class RadialGuideGeometry {
    private RadialGuideGeometry(){}
    public static final float WIDTH=360,HEIGHT=302,PAD_WIDTH=160,PAD_X=100,PAD_Y=(HEIGHT-PAD_WIDTH*.54f)/2;
    public static final String[] LABELS={"L","R","PAIR","Z","Y","X","C","B","A","HOME","SELECT","★","↑ ↓ ← →","START"};
    private static final float[][] ANCHORS={{255,67},{745,67},{430,80},{795,165},{714,188},{630,230},{843,254},{750,280},{668,323},{558,350},{500,350},{442,350},{260,250},{500,250}};
    // Minimum-distance matching to the outer ring, verified to have no crossing rays.
    private static final int[] SLOTS={12,1,13,3,2,0,4,5,6,7,8,9,11,10};
    public static float[] start(int index){return new float[]{PAD_X+ANCHORS[index][0]*PAD_WIDTH/1000,PAD_Y+ANCHORS[index][1]*PAD_WIDTH/1000};}
    public static float[] end(int index){double angle=Math.toRadians(-90+SLOTS[index]*360.0/14);return new float[]{180+138*(float)Math.cos(angle),151+124*(float)Math.sin(angle)};}
    private static float orient(float[] a,float[] b,float[] c){return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);}
    public static boolean crosses(float[] a,float[] b,float[] c,float[] d){return orient(a,b,c)*orient(a,b,d)<-.000001f&&orient(c,d,a)*orient(c,d,b)<-.000001f;}
}
