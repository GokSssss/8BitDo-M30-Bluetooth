package app.m30.companion;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

/** Independent controller state and controls in the approved wide workspace. */
final class ControllerPanelView extends FrameLayout {
    interface Actions {void run(int slot,String action);}
    private static final int TEXT=0xfff6f3e8,MUTED=0xffbbb7a8,CARD=0xff181818;
    final int slot,accent;
    final LinearLayout body;
    final PadView pad;
    final Button connect,choose,options,keys,dpadHelp,ledHelp;
    final TextView battery,name,code,event,version;
    final BatteryIndicator indicator;
    final ConnectionCardView connection;
    private final Actions actions;
    private final ControllerSession session;
    private final LinearLayout charge;
    private int shownMode=-1;

    ControllerPanelView(Context context,int slot,ControllerSession session,Actions actions){
        super(context);this.slot=slot;this.session=session;this.actions=actions;accent=slot==0?0xffb6a45e:0xffb66f45;
        setTag(R.id.navigation_key,"wide:"+slot);setPadding(dp(5),dp(4),dp(5),dp(5));body=column();addView(body,new LayoutParams(-1,-2));
        name=text("Джойстик "+(slot+1),13,accent);body.addView(name);
        LinearLayout hero=new LinearLayout(context);hero.setGravity(Gravity.TOP);body.addView(hero,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout rail=column();
        connect=action("Подключение","bluetooth",R.drawable.ic_bluetooth);choose=action("Выбрать","choose",R.drawable.ic_gamepad);options=action("Опции","options",R.drawable.ic_options_outline);keys=action("Клавиши","keys",R.drawable.ic_info_outline);for(Button b:new Button[]{connect,choose,options,keys})rail.addView(b);
        int width=Math.max(dp(128),((ActionButton)connect).desiredWidth());hero.addView(rail,new LinearLayout.LayoutParams(width,-2));
        LinearLayout drawing=column();LinearLayout.LayoutParams drawLp=new LinearLayout.LayoutParams(0,-2,1);drawLp.leftMargin=dp(6);hero.addView(drawing,drawLp);FrameLayout diagram=new FrameLayout(context);drawing.addView(diagram,new LinearLayout.LayoutParams(-1,-2));pad=new PadView(context,session.state,null);pad.setDisplayStyle(slot,false);pad.hideDpadCaption(true);pad.setTag(R.id.navigation_key,key("pad"));diagram.addView(pad,new LayoutParams(-1,-2));
        charge=new LinearLayout(context);charge.setGravity(Gravity.CENTER_VERTICAL);charge.setMinimumHeight(dp(20));charge.setTag(R.id.navigation_key,key("battery"));charge.setOnClickListener(v->actions.run(slot,"battery"));indicator=new BatteryIndicator(context);indicator.setAccent(accent);charge.addView(indicator,new LinearLayout.LayoutParams(dp(18),dp(12)));battery=text("—",11,accent);battery.setPadding(dp(4),0,0,0);charge.addView(battery);diagram.addView(charge,new LayoutParams(-2,dp(20),Gravity.TOP|Gravity.CENTER_HORIZONTAL));
        LinearLayout helpRow=new LinearLayout(context);helpRow.setBaselineAligned(false);LinearLayout.LayoutParams helpLp=new LinearLayout.LayoutParams(-1,-2);helpLp.topMargin=dp(2);drawing.addView(helpRow,helpLp);
        dpadHelp=info("Крестовина","dpad-info","dpad");ledHelp=info("Огоньки","led-info","led");helpRow.addView(dpadHelp,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams rightLp=new LinearLayout.LayoutParams(0,-2,1);rightLp.leftMargin=dp(4);helpRow.addView(ledHelp,rightLp);
        version=text("",10,MUTED);version.setGravity(Gravity.CENTER);version.setSingleLine(true);drawing.addView(version,new LinearLayout.LayoutParams(-1,dp(16)));
        LinearLayout summary=column();summary.setBackground(shape(CARD));summary.setPadding(dp(8),dp(4),dp(8),dp(4));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(4),0,dp(5));body.addView(summary,lp);code=text("Код — · значение —",13,TEXT);code.setTypeface(Typeface.MONOSPACE);code.setSingleLine(true);code.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);summary.addView(code,new LinearLayout.LayoutParams(-1,-2));event=text("Подключите M30",13,accent);event.setSingleLine(true);event.setEllipsize(android.text.TextUtils.TruncateAt.END);summary.addView(event,new LinearLayout.LayoutParams(-1,-2));
        connection=new ConnectionCardView(context,session.mode,accent,key(""),m->actions.run(slot,"mode:"+m),null);body.addView(connection,new LinearLayout.LayoutParams(-1,-2));
    }
    String key(String action){return "wide:"+slot+":"+action;}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(getContext());l.setOrientation(LinearLayout.VERTICAL);return l;}
    private GradientDrawable shape(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(10));return g;}
    private TextView text(String value,int size,int color){TextView t=new TextView(getContext());t.setText(InstructionText.format(value));t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setPadding(0,dp(2),0,dp(2));return t;}
    private void icon(Button button,int drawable,int tint){android.graphics.drawable.Drawable d=getContext().getDrawable(drawable).mutate();d.setTint(tint);d.setBounds(0,0,dp(13),dp(13));button.setCompoundDrawablesRelative(d,null,null,null);button.setCompoundDrawablePadding(dp(4));}
    private Button action(String label,String action,int image){ActionButton b=new ActionButton(getContext(),label,accent);b.setTag(R.id.navigation_key,key(action));b.setOnClickListener(v->actions.run(slot,action));b.icon(image,accent);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(3);b.setLayoutParams(lp);return b;}
    private Button info(String label,String key,String action){ActionButton b=new ActionButton(getContext(),label,accent);b.info();b.icon(R.drawable.ic_info_outline,accent);b.setTag(R.id.navigation_key,key(key));b.setContentDescription(action.equals("dpad")?"Как переключить крестовину, левый или правый стик":"Режимы подключения и индикаторы M30");b.setOnClickListener(v->actions.run(slot,action));return b;}
    void refresh(String alias,float capacity,ModeDetector.Result detected,String appVersion,boolean selected){
        GradientDrawable border=shape(0xff000000);border.setStroke(dp(1),selected?accent:0xff30302b);setBackground(border);name.setText("Джойстик "+(slot+1)+" · "+alias);int color=session.connected()?accent:0xfff36b65;connect.setTextColor(color);icon(connect,session.connected()?R.drawable.ic_bluetooth:R.drawable.ic_bluetooth_off,color);connect.setContentDescription("Джойстик "+(slot+1)+". "+(session.connected()?alias+" · ID "+session.deviceId:"Не подключён")+". Открыть подключение в соседнем поле");indicator.setCapacity(capacity);battery.setText(capacity<0?"—":Math.round(capacity*100)+"%");charge.setContentDescription(capacity<0?"Заряд: нет данных от Android":"Заряд: "+Math.round(capacity*100)+"%");
        pad.setModelName(session.deviceName);pad.waiting=session.waiting;pad.setDpadInfo(session.dpad.caption(),null);dpadHelp.setText(ActionButton.dpadTitle(session.dpad));pad.invalidate();ArrayList<String> held=new ArrayList<>();for(String label:PadState.LABELS)if(session.state.active(label))held.add(label);pad.setContentDescription("Джойстик "+(slot+1)+". "+alias+". "+(held.isEmpty()?"Нет удерживаемых кнопок":"Нажато: "+String.join(" + ",held)));
        InputMonitor.Key last=session.monitor.lastSelected;code.setText(session.lastWasAxis?String.format(Locale.ROOT,"AX %d · %s = %.3f",session.lastAxis,android.view.MotionEvent.axisToString(session.lastAxis),session.lastAxisValue):last==null?"Код — · значение —":"Код "+last.code+" · "+last.name.replace("KEYCODE_","")+" = "+(last.down?1:0));event.setText(session.lastWasAxis?"Ось · "+(Math.abs(session.lastAxisValue)<0.001f?"нейтраль":"активна"):last==null?(session.connected()?"Нажмите кнопку для проверки":"Подключите M30"):last.name.replace("KEYCODE_","")+" · "+(last.down?"нажато":"отпущено"));
        if(shownMode!=session.mode){shownMode=session.mode;connection.setMode(shownMode);}connection.setContentDescription("Инструкция "+ConnectionGuide.names()[session.mode]+". "+(!session.connected()?"Джойстик не подключён":detected.title)+". Режим по признакам Android, не чтение прошивки.");version.setText("Версия "+appVersion+" · прошивка —");
    }
    void routes(View tab){View[] rail={connect,choose,options,keys};for(int n=0;n<rail.length;n++){FocusNavigation.route(rail[n],View.FOCUS_UP,n==0?tab:rail[n-1]);FocusNavigation.route(rail[n],View.FOCUS_DOWN,n==3?dpadHelp:rail[n+1]);FocusNavigation.route(rail[n],View.FOCUS_RIGHT,n==0?charge:dpadHelp);}FocusNavigation.route(charge,View.FOCUS_DOWN,dpadHelp);FocusNavigation.route(charge,View.FOCUS_UP,tab);FocusNavigation.route(charge,View.FOCUS_LEFT,connect);FocusNavigation.route(dpadHelp,View.FOCUS_LEFT,keys);FocusNavigation.route(dpadHelp,View.FOCUS_RIGHT,ledHelp);FocusNavigation.route(ledHelp,View.FOCUS_LEFT,dpadHelp);FocusNavigation.route(dpadHelp,View.FOCUS_UP,charge);FocusNavigation.route(ledHelp,View.FOCUS_UP,charge);FocusNavigation.route(dpadHelp,View.FOCUS_DOWN,connection.mode(0));FocusNavigation.route(ledHelp,View.FOCUS_DOWN,connection.mode(2));for(int m=0;m<4;m++)FocusNavigation.route(connection.mode(m),View.FOCUS_UP,m==0||m==1?dpadHelp:ledHelp);}
}
