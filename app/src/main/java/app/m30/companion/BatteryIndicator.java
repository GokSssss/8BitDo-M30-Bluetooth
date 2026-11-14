package app.m30.companion;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Battery of the selected controller; a dash represents missing telemetry. */
public final class BatteryIndicator extends View {
    private int accent=0xffb6a45e;
    public void setAccent(int color){accent=color;invalidate();}
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float capacity=-1;
    private boolean neutral;
    public void setNeutral(boolean gray){neutral=gray;invalidate();}
    public BatteryIndicator(Context context){super(context);}
    public void setCapacity(float value){
        float next=Float.isNaN(value)||value<0||value>1?-1:value;
        if(capacity!=next){capacity=next;invalidate();}
        setContentDescription(capacity<0?"Батарея геймпада: нет данных":"Батарея геймпада: "+Math.round(capacity*100)+"%");
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float scale=Math.min(getWidth()/28f,getHeight()/16f);
        canvas.save();canvas.translate((getWidth()-28*scale)/2,(getHeight()-16*scale)/2);canvas.scale(scale,scale);
        paint.setColor(capacity<0?(neutral?0xffbfbfbf:0xffbbb7a8):accent);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(1,2,23,14,2,2,paint);paint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(24,5,27,11,1,1,paint);
        if(capacity>=0){if(capacity>0)canvas.drawRoundRect(3.5f,4.5f,3.5f+17*capacity,11.5f,1,1,paint);}
        else canvas.drawRoundRect(9,7,16,9,1,1,paint);
        canvas.restore();
    }
}
