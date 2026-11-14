package app.m30.companion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Illustration of the startup LEDs, not a reading of the connected controller. */
public final class ModeLedsView extends View {
    private int accent=0xffb6a45e;
    public void setAccent(int color){accent=color;invalidate();}
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mode;
    public ModeLedsView(Context context,int mode){
        super(context);this.mode=mode;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
    @Override protected void onMeasure(int width,int height){
        setMeasuredDimension(resolveSize((int)dp(52),width),resolveSize((int)dp(16),height));
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        // A static left-to-right fade illustrates Switch without moving the dots.
        for(int i=0;i<4;i++){
            float brightness=i<(mode==0?1:mode==1?2:mode==3?3:0)?1:0;
            if(mode==2)brightness=1-i*.3f;
            float x=getWidth()/2f+(i-1.5f)*dp(11),y=getHeight()/2f,r=dp(2.7f);
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xff302e23);canvas.drawCircle(x,y,r,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(.8f));paint.setColor(0xff706a47);canvas.drawCircle(x,y,r,paint);
            if(brightness>0){paint.setStyle(Paint.Style.FILL);paint.setColor(accent);paint.setAlpha(Math.round(brightness*255));canvas.drawCircle(x,y,r,paint);paint.setAlpha(255);}
        }
    }
}
