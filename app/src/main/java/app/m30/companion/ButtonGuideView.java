package app.m30.companion;

import android.content.Context;
import android.graphics.*;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import java.util.*;

/** Static radial reference, following the approved M30-radial-clean-draft. */
public final class ButtonGuideView extends View {
    private final PadView miniature;
    private final Paint line=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint badge=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Caption> captions=new ArrayList<>();
    private final int accent;
    private static final int BACKGROUND=0xff181818;
    private static final class Caption {
        final String label;final float[] start,center;final StaticLayout action;
        final float[] end;RectF badgeBox;float badgeX,badgeY;
        Caption(String label,float[] start,float[] center,StaticLayout action){
            this.label=label;this.start=start;this.center=center;this.action=action;
            float vx=start[0]-center[0],vy=start[1]-center[1];
            float factor=Math.min(vx==0?999:(action.getWidth()/2f+1)/Math.abs(vx),vy==0?999:action.getHeight()/2f/Math.abs(vy));
            end=new float[]{center[0]+vx*factor,center[1]+vy*factor};
        }
    }
    public ButtonGuideView(Context context,Map<String,String> bindings,int mode,int slot){
        super(context);accent=slot==1?0xffb66f45:0xffb6a45e;
        miniature=new PadView(context,new PadState(),null);miniature.setControllerSlot(slot);miniature.setModelName("M30");
        miniature.measure(MeasureSpec.makeMeasureSpec(1000,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(540,MeasureSpec.EXACTLY));miniature.layout(0,0,1000,540);
        badge.setTextSize(10);badge.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));badge.setTextAlign(Paint.Align.CENTER);badge.setColor(accent);
        TextPaint body=new TextPaint(Paint.ANTI_ALIAS_FLAG);body.setTextSize(11);body.setColor(0xfff6f3e8);body.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        StringBuilder description=new StringBuilder("Действия кнопок по профилю "+ConnectionGuide.names()[mode]+". ");
        for(int i=0;i<RadialGuideGeometry.LABELS.length;i++){
            String label=RadialGuideGeometry.LABELS[i];String[] buttons=label.equals("↑ ↓ ← →")?new String[]{"↑","↓","←","→"}:new String[]{label};
            String action=ButtonGuide.caption(buttons,bindings,mode);
            // The direction caption needs room for the whole word «Перемещение».
            int captionWidth=label.equals("↑ ↓ ← →")?80:70;
            StaticLayout text=StaticLayout.Builder.obtain(action,0,action.length(),body,captionWidth).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setLineSpacing(1,1).build();
            captions.add(new Caption(label,RadialGuideGeometry.start(i),RadialGuideGeometry.end(i),text));
            description.append(label).append(": ").append(action).append(". ");
        }
        placeBadges();setContentDescription(description.toString());setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    private boolean hitsRay(RectF box,Caption ray){
        if(box.contains(ray.start[0],ray.start[1])||box.contains(ray.end[0],ray.end[1]))return true;
        float[][] corners={{box.left,box.top},{box.right,box.top},{box.right,box.bottom},{box.left,box.bottom}};
        for(int k=0;k<4;k++)if(RadialGuideGeometry.crosses(ray.start,ray.end,corners[k],corners[(k+1)%4]))return true;
        return false;
    }
    private void placeBadges(){
        ArrayList<RectF> placed=new ArrayList<>();
        for(Caption c:captions){
            float half=badge.measureText(c.label)/2+3;int best=Integer.MAX_VALUE;
            for(float t:new float[]{.68f,.75f,.82f,.60f,.53f,.9f}){
                float x=c.start[0]+(c.end[0]-c.start[0])*t,y=c.start[1]+(c.end[1]-c.start[1])*t;
                RectF box=new RectF(x-half,y-8,x+half,y+8),expanded=new RectF(box);expanded.inset(-2,-2);int collisions=0;
                for(RectF other:placed)if(RectF.intersects(expanded,other))collisions++;
                for(Caption ray:captions)if(ray!=c&&hitsRay(box,ray))collisions++;
                if(collisions<best){best=collisions;c.badgeBox=box;c.badgeX=x;c.badgeY=y;}
                if(collisions==0)break;
            }
            placed.add(c.badgeBox);
        }
    }
    @Override protected void onMeasure(int width,int height){
        int w=MeasureSpec.getSize(width);setMeasuredDimension(w,resolveSize((int)Math.ceil(w*RadialGuideGeometry.HEIGHT/RadialGuideGeometry.WIDTH),height));
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.save();float scale=getWidth()/RadialGuideGeometry.WIDTH;canvas.scale(scale,scale);
        canvas.save();canvas.translate(RadialGuideGeometry.PAD_X,RadialGuideGeometry.PAD_Y);canvas.scale(RadialGuideGeometry.PAD_WIDTH/1000,RadialGuideGeometry.PAD_WIDTH/1000);miniature.draw(canvas);canvas.restore();
        line.setColor(accent);line.setAlpha(155);line.setStrokeWidth(.75f);line.setStyle(Paint.Style.STROKE);
        for(Caption c:captions){canvas.drawLine(c.start[0],c.start[1],c.end[0],c.end[1],line);canvas.save();canvas.translate(c.center[0]-c.action.getWidth()/2f,c.center[1]-c.action.getHeight()/2f);c.action.draw(canvas);canvas.restore();}
        line.setStyle(Paint.Style.FILL);line.setColor(BACKGROUND);line.setAlpha(255);
        for(Caption c:captions){canvas.drawRoundRect(c.badgeBox,4,4,line);canvas.drawText(c.label,c.badgeX,c.badgeY-(badge.ascent()+badge.descent())/2,badge);}
        canvas.restore();
    }
}
