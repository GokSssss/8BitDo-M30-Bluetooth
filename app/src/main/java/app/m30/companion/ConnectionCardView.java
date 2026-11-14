package app.m30.companion;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** The same compact, mode-specific instructions on single and paired layouts. */
final class ConnectionCardView extends LinearLayout {
    interface Select {void mode(int value);}
    private static final int TEXT=0xfff6f3e8,MUTED=0xffbbb7a8;
    private final int accent;
    private final String prefix;
    private final TextView profile;
    private final LinearLayout[] choices=new LinearLayout[4];
    private final TextView[] instructions=new TextView[3];
    ConnectionCardView(Context context,int mode,int accent,String prefix,Select select,Runnable help){
        super(context);this.accent=accent;this.prefix=prefix;setOrientation(VERTICAL);setPadding(dp(9),dp(5),dp(9),dp(7));setBackground(shape(0xff181818,false));setTag(R.id.navigation_key,prefix+"card");
        LinearLayout heading=new LinearLayout(context);heading.setGravity(Gravity.CENTER_VERTICAL);addView(heading,new LayoutParams(-1,-2));
        TextView title=text("Подключение M30",13,TEXT);heading.addView(title,new LayoutParams(-2,-2));profile=text("",11,accent);profile.setSingleLine(true);profile.setEllipsize(android.text.TextUtils.TruncateAt.END);profile.setPadding(dp(4),0,dp(3),0);heading.addView(profile,new LayoutParams(0,-2,1));
        if(help!=null){TextView info=text("ⓘ",18,accent);info.setGravity(Gravity.CENTER);info.setMinimumHeight(dp(32));info.setMinimumWidth(dp(28));info.setTag(R.id.navigation_key,prefix+"help");info.setContentDescription("Режимы и огоньки подключения. Открыть справку");info.setOnClickListener(v->help.run());heading.addView(info,new LayoutParams(-2,-2));}
        LinearLayout row=new LinearLayout(context);row.setBaselineAligned(false);addView(row,new LayoutParams(-1,-2));String[] names={"Android","Windows","macOS","Switch"};int[] order={0,1,3,2};
        for(int n=0;n<4;n++){final int m=order[n];LinearLayout choice=new LinearLayout(context);choice.setOrientation(VERTICAL);choice.setGravity(Gravity.CENTER);choice.setPadding(dp(2),dp(3),dp(2),dp(3));choice.setMinimumHeight(dp(52));choice.setTag(R.id.navigation_key,prefix+"mode:"+m);choice.setOnClickListener(v->select.mode(m));
            TextView label=text(names[n],13,MUTED);label.setSingleLine(true);label.setGravity(Gravity.CENTER);label.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);choice.addView(label,new LayoutParams(-1,-2));TextView chord=text(ConnectionGuide.shortKey(m),10,MUTED);chord.setSingleLine(true);chord.setGravity(Gravity.CENTER);choice.addView(chord,new LayoutParams(-1,-2));ModeLedsView leds=new ModeLedsView(context,m);leds.setAccent(accent);choice.addView(leds,new LayoutParams(-1,dp(14)));LayoutParams lp=new LayoutParams(0,-1,1);if(n>0)lp.leftMargin=dp(3);row.addView(choice,lp);choices[m]=choice;
        }
        TextView before=text("Огоньки при подключении · до PAIR — режим",12,MUTED);before.setGravity(Gravity.CENTER);before.setPadding(0,dp(3),0,dp(3));addView(before,new LayoutParams(-1,-2));
        for(int n=0;n<3;n++){instructions[n]=text("",13,TEXT);instructions[n].setPadding(0,dp(2),0,dp(2));addView(instructions[n],new LayoutParams(-1,-2));}
        TextView after=text(ConnectionGuide.connectedLeds(),12,MUTED);after.setPadding(0,dp(4),0,0);addView(after,new LayoutParams(-1,-2));setMode(mode);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView t=new TextView(getContext());t.setText(InstructionText.format(value));t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);return t;}
    private GradientDrawable shape(int color,boolean border){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(10));if(border)g.setStroke(dp(1.3f),accent);return g;}
    View mode(int mode){return choices[mode];}
    void setMode(int mode){profile.setText("· "+ConnectionGuide.names()[mode]);profile.setContentDescription("Инструкция выбранного режима: "+ConnectionGuide.names()[mode]+". Выбор не переключает режим джойстика.");for(int m=0;m<4;m++){boolean selected=m==mode;LinearLayout c=choices[m];c.setSelected(selected);c.setBackground(shape(selected?0xff2d2b22:0xff181818,selected));c.setContentDescription(ConnectionGuide.names()[m]+(selected?", выбран":", выбрать")+". "+ConnectionGuide.shortKey(m)+". "+ConnectionGuide.ledDescription(m));for(int child=0;child<2;child++){TextView t=(TextView)c.getChildAt(child);t.setTextColor(selected?accent:MUTED);t.setTypeface(null,child==0&&selected?Typeface.BOLD:Typeface.NORMAL);}}String[] steps=ConnectionGuide.compactSteps(mode);for(int n=0;n<3;n++)instructions[n].setText(InstructionText.format(steps[n]));}
}
