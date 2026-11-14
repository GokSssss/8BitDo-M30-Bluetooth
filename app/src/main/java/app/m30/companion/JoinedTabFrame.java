package app.m30.companion;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;

/** Draws a shared page outline with an opening below the selected folder tab. */
final class JoinedTabFrame extends FrameLayout {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private View selected;
    private int accent=0xffaaa99e;
    JoinedTabFrame(Context context){super(context);setWillNotDraw(false);setBackgroundColor(Color.BLACK);int p=dp(7);setPadding(p,p,p,p);}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    void select(View tab,int accent){selected=tab;this.accent=accent;invalidate();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float edge=dp(1)/2f,w=getWidth()-edge,h=getHeight()-edge,r=dp(14),start=edge,end=edge;
        if(selected!=null){int[] tab=new int[2],frame=new int[2];selected.getLocationOnScreen(tab);getLocationOnScreen(frame);start=Math.max(edge,tab[0]-frame[0]);end=Math.min(w,start+selected.getWidth());}
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,dp(1)));paint.setColor(accent);
        Path path=new Path();path.moveTo(start,edge);path.lineTo(edge,edge);path.lineTo(edge,h-r);path.quadTo(edge,h,edge+r,h);path.lineTo(w-r,h);path.quadTo(w,h,w,h-r);path.lineTo(w,edge);path.lineTo(end,edge);canvas.drawPath(path,paint);
    }
    static Drawable tab(Context context,boolean selected,int accent){return new Drawable(){
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final float density=context.getResources().getDisplayMetrics().density;
        public void draw(Canvas c){Rect b=getBounds();float inset=density*.5f,r=13*density,left=b.left+inset,right=b.right-inset,top=b.top+inset,bottom=b.bottom;
            Path shape=new Path();shape.moveTo(left,bottom);shape.lineTo(left,top+r);shape.quadTo(left,top,left+r,top);shape.lineTo(right-r,top);shape.quadTo(right,top,right,top+r);shape.lineTo(right,bottom);
            p.setColor(Color.BLACK);p.setStyle(Paint.Style.FILL);c.drawPath(shape,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(density);p.setColor(selected?accent:0xff60605a);c.drawPath(shape,p);
        }
        public void setAlpha(int a){} public void setColorFilter(ColorFilter f){} public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    };}
}
