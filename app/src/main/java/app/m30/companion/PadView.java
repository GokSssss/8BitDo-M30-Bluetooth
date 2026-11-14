package app.m30.companion;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import java.util.*;

/** Scalable physical M30 layout. Touch selects a binding; it never simulates input. */
public final class PadView extends View {
    // Physical M30 proportions and controls follow the supplied front-view sketch.
    private static final int WIDTH=1000,HEIGHT=540;
    private Drawable shell;
    private int accent=0xffb6a45e;
    private boolean neutral;
    private final Paint p = new Paint(3);
    private final PadState state;
    private final Map<String, RectF> targets = new LinkedHashMap<>();
    public interface Pick { void select(String label); }
    private final Pick pick;
    private float drawScale=1,offsetX,offsetY;
    private String modelName="—",dpadCaption="D-PAD";
    private Runnable dpadAction;
    private boolean captionHidden;
    public void hideDpadCaption(boolean hidden){captionHidden=hidden;invalidate();}
    private boolean dpadTap;
    private float tapX,tapY;
    public String waiting;
    public PadView(Context context, PadState state, Pick pick) {
        super(context); this.state = state; this.pick = pick;
        shell=getResources().getDrawable(R.drawable.pad_shell,context.getTheme());shell.setBounds(0,0,WIDTH,HEIGHT);
        setContentDescription("Схема M30. Нажатия на подключённом геймпаде подсвечиваются.");
    }
    public void setControllerSlot(int slot){setDisplayStyle(slot,false);}
    public void setDisplayStyle(int slot,boolean gray){
        neutral=gray;accent=gray?0xffa6a6a6:slot==1?0xffb66f45:0xffb6a45e;
        int resource=gray?R.drawable.pad_shell_gray:slot==1?R.drawable.pad_shell_orange:R.drawable.pad_shell;
        shell=getResources().getDrawable(resource,getContext().getTheme());shell.setBounds(0,0,WIDTH,HEIGHT);invalidate();
    }
    public void setModelName(String name){
        String value=name==null?"—":name.replaceAll("\\s+"," ").trim();
        if(value.matches("(?i).*\\bM30\\b.*"))value="M30";
        else value=value.replaceFirst("(?i)^8bitdo\\s*","").replaceFirst("(?i)\\s*gamepad$","").trim();
        if(value.isEmpty())value="—";
        if(!modelName.equals(value)){modelName=value;invalidate();}
    }
    public void setDpadInfo(String caption,Runnable action){
        if(!dpadCaption.equals(caption)){dpadCaption=caption;invalidate();}
        dpadAction=action;setClickable(action!=null);setFocusable(action!=null);setTooltipText(action==null?null:"Как переключить D-pad / левый / правый стик");
    }
    public PointF buttonCenter(String label){
        if("PAIR".equals(label))return new PointF(430,80);
        RectF target=targets.get(label);return target==null?null:new PointF(target.centerX(),target.centerY());
    }
    @Override protected void onMeasure(int w,int h){
        int width=MeasureSpec.getSize(w),ideal=(int)(width*(float)HEIGHT/WIDTH);
        int height=MeasureSpec.getMode(h)==MeasureSpec.EXACTLY?MeasureSpec.getSize(h):MeasureSpec.getMode(h)==MeasureSpec.AT_MOST?Math.min(ideal,MeasureSpec.getSize(h)):ideal;
        setMeasuredDimension(width,height);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawScale=Math.min(getWidth()/(float)WIDTH,getHeight()/(float)HEIGHT);offsetX=(getWidth()-WIDTH*drawScale)/2;offsetY=(getHeight()-HEIGHT*drawScale)/2;
        canvas.save();canvas.translate(offsetX,offsetY);canvas.scale(drawScale,drawScale);targets.clear();
        shell.draw(canvas);
        shoulder(canvas,"L",false);shoulder(canvas,"R",true);
        button(canvas,"↑",260,198,27,27);button(canvas,"↓",260,302,27,27);
        button(canvas,"←",208,250,27,27);button(canvas,"→",312,250,27,27);
        p.setStyle(Paint.Style.FILL);p.setColor((neutral?0xff2f2f2f:Color.rgb(47,47,39)));canvas.drawCircle(260,250,10,p);
        button(canvas,"X",630,230,33,33);button(canvas,"Y",714,188,33,33);button(canvas,"Z",795,165,33,33);
        button(canvas,"A",668,323,43,43);button(canvas,"B",750,280,43,43);button(canvas,"C",843,254,43,43);
        button(canvas,"START",500,250,44,15);
        button(canvas,"★",442,350,22,22);button(canvas,"SELECT",500,350,22,22);button(canvas,"HOME",558,350,22,22);
        // PAIR is a physical top-edge button, without a normal Android gamepad event.
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(21,21,21));canvas.drawRoundRect(398,70,462,90,8,8,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(accent);canvas.drawRoundRect(398,70,462,90,8,8,p);
        label(canvas,"PAIR",430,80,16,(neutral?0xffeeeeee:Color.rgb(255,244,180)),false);
        label(canvas,"8BitDo",500,120,40,(neutral?0xff171717:Color.rgb(23,23,16)),true);
        modelLabel(canvas);
        if(!captionHidden){if(dpadAction!=null)dpadCaptionBadge(canvas);else label(canvas,dpadCaption,260,397,18,(neutral?0xff303030:Color.rgb(48,42,11)),false);}
        label(canvas,"STAR",442,389,16,(neutral?0xff303030:Color.rgb(48,42,11)),false);
        label(canvas,"SELECT",500,389,16,(neutral?0xff303030:Color.rgb(48,42,11)),false);
        label(canvas,"HOME",558,389,16,(neutral?0xff303030:Color.rgb(48,42,11)),false);
        canvas.restore();
    }
    private void dpadCaptionBadge(Canvas canvas){
        p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));float size=28;p.setTextSize(size);
        while(p.measureText(dpadCaption)>198&&size>18){size--;p.setTextSize(size);}
        float half=Math.max(65,Math.min(140,p.measureText(dpadCaption)/2+28));
        RectF badge=new RectF(220-half,416,220+half,456);
        p.setStyle(Paint.Style.FILL);p.setColor(0xff151515);canvas.drawRoundRect(badge,12,12,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(neutral?0xffc0c0c0:accent);canvas.drawRoundRect(badge,12,12,p);
        label(canvas,dpadCaption,208,436,size,Color.WHITE,true);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(accent);canvas.drawCircle(badge.right-15,436,9,p);label(canvas,"i",badge.right-15,436,15,accent,true);
    }
    private int keyColor(String label){return state.active(label)?(neutral?0xfff0f0f0:Color.rgb(255,251,220)):state.tested(label)?Color.rgb(Math.round(Color.red(accent)*.55f),Math.round(Color.green(accent)*.55f),Math.round(Color.blue(accent)*.55f)):Color.rgb(21,21,21);}
    private void modelLabel(Canvas canvas){
        String text=modelName;float size=26;
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(size);
        while(p.measureText(text)>160&&size>18){size--;p.setTextSize(size);}
        if(p.measureText(text)>160){while(text.length()>1&&p.measureText(text+"…")>160)text=text.substring(0,text.length()-1);text+="…";}
        label(canvas,text,500,164,size,(neutral?0xff303030:Color.rgb(48,42,11)),false);
    }
    private void label(Canvas canvas,String text,float x,float y,float size,int color,boolean bold){
        p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);canvas.drawText(text,x,y-(p.ascent()+p.descent())/2,p);
    }
    private void shoulder(Canvas canvas,String label,boolean right){
        // One opaque capsule per shoulder: cover shell seams instead of subdividing the key.
        float x=right?745:255;RectF key=new RectF(x-105,38,x+105,96);targets.put(label,key);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.BLACK);canvas.drawRoundRect(x-111,32,x+111,102,34,34,p);
        p.setColor(keyColor(label));canvas.drawRoundRect(key,28,28,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(label.equals(waiting)?6:5);p.setColor(label.equals(waiting)?Color.WHITE:accent);canvas.drawRoundRect(key,28,28,p);
        label(canvas,label,x,67,34,state.active(label)?Color.rgb(21,21,21):(neutral?0xffeeeeee:Color.rgb(255,244,180)),true);
    }
    private void button(Canvas c, String label, float x, float y, float rx, float ry) {
        RectF r=new RectF(x-rx,y-ry,x+rx,y+ry); targets.put(label,r);
        p.setStyle(Paint.Style.FILL);
        boolean active=state.active(label),tested=state.tested(label);
        // Pale light for a held button remains obvious against the yellow shell.
        // A released, tested button stays amber; the D-pad remains one cross-shaped piece.
        boolean direction=label.equals("↑")||label.equals("↓")||label.equals("←")||label.equals("→");
        p.setColor(keyColor(label));
        if(!direction||active||tested)c.drawRoundRect(r,Math.min(rx,ry),Math.min(rx,ry),p);
        if(!direction){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor((neutral?0xff3a3a3a:Color.rgb(58,52,17)));c.drawRoundRect(x-rx-4,y-ry-4,x+rx+4,y+ry+4,Math.min(rx,ry)+4,Math.min(rx,ry)+4,p);p.setStyle(Paint.Style.FILL);}
        if(label.equals(waiting)) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4); p.setColor(Color.WHITE); c.drawRoundRect(x-rx-5,y-ry-5,x+rx+5,y+ry+5,ry,ry,p); p.setStyle(Paint.Style.FILL); }
        p.setColor(active?Color.rgb(20,20,20):(neutral?0xffeeeeee:Color.rgb(255,244,180)));
        // Vector symbols cannot be replaced by coloured emoji fonts on Android.
        if(label.equals("HOME")){
            Path heart=new Path();heart.moveTo(x,y+11);heart.cubicTo(x-4,y+7,x-14,y-2,x-11,y-9);heart.cubicTo(x-7,y-14,x,y-11,x,y-5);heart.cubicTo(x,y-11,x+7,y-14,x+11,y-9);heart.cubicTo(x+14,y-2,x+4,y+7,x,y+11);heart.close();c.drawPath(heart,p);
        }else if(label.equals("★")){
            Path star=new Path();for(int i=0;i<10;i++){double angle=Math.toRadians(-90+i*36);float radius=i%2==0?13:5.5f;float px=x+radius*(float)Math.cos(angle),py=y+radius*(float)Math.sin(angle);if(i==0)star.moveTo(px,py);else star.lineTo(px,py);}star.close();c.drawPath(star,p);
        }else if(label.equals("SELECT"))c.drawRoundRect(x-9,y-2,x+9,y+2,2,2,p);
        else{p.setTextSize(label.equals("START")?22:direction?30:rx>35?40:38);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);c.drawText(label,x,y-(p.ascent()+p.descent())/2,p);}
    }
    private boolean dpadHit(float x,float y){
        float scale=Math.min(getWidth()/(float)WIDTH,getHeight()/(float)HEIGHT);
        if(scale<=0)return false;
        float px=(x-(getWidth()-WIDTH*scale)/2)/scale,py=(y-(getHeight()-HEIGHT*scale)/2)/scale;
        return px>=65&&px<=400&&py>=145&&py<=480;
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){tapX=e.getX();tapY=e.getY();dpadTap=dpadAction!=null&&dpadHit(tapX,tapY);return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&Math.hypot(e.getX()-tapX,e.getY()-tapY)>ViewConfiguration.get(getContext()).getScaledTouchSlop())dpadTap=false;
        if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){dpadTap=false;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){
            boolean open=dpadTap&&dpadHit(e.getX(),e.getY());dpadTap=false;
            if(open){performClick();return true;}
            if(pick!=null){float x=(e.getX()-offsetX)/drawScale,y=(e.getY()-offsetY)/drawScale;
                for(Map.Entry<String,RectF> t:targets.entrySet()){RectF hit=new RectF(t.getValue());hit.inset(-5,-5);if(hit.contains(x,y)){super.performClick();pick.select(t.getKey());break;}}
            }
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();if(dpadAction!=null)dpadAction.run();return true;}
}
