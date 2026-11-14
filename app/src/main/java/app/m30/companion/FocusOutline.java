package app.m30.companion;

import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;

/** Thin, transparent outline follows each background; it never paints over the caption. */
final class FocusOutline extends Drawable {
    private final View view;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    FocusOutline(View view){this.view=view;}
    @Override public void draw(Canvas canvas){
        float density=view.getResources().getDisplayMetrics().density,stroke=2*density,inset=stroke/2;Rect b=getBounds();RectF rect=new RectF(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);String key=FocusNavigation.key(view);
        if(key!=null&&key.startsWith("tab:")){float r=Math.min(13*density,rect.height()/2);Path path=new Path();path.moveTo(rect.left,rect.bottom);path.lineTo(rect.left,rect.top+r);path.quadTo(rect.left,rect.top,rect.left+r,rect.top);path.lineTo(rect.right-r,rect.top);path.quadTo(rect.right,rect.top,rect.right,rect.top+r);path.lineTo(rect.right,rect.bottom);canvas.drawPath(path,paint);return;}
        Drawable background=view.getBackground();if(background!=null)background=background.getCurrent();while(background instanceof InsetDrawable)background=((InsetDrawable)background).getDrawable();float radius=6*density;
        if(background instanceof GradientDrawable){GradientDrawable shape=(GradientDrawable)background;radius=Math.max(0,shape.getCornerRadius()-inset/2);}
        canvas.drawRoundRect(rect,radius,radius,paint);
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
