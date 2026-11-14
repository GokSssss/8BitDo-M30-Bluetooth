package app.m30.companion;

import android.content.*;
import android.view.*;
import android.widget.*;

/** Host preferences are changed only by an explicit checkbox/confirmation, never by an error. */
final class BluetoothCompatibility extends LinearLayout implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final String STORE="bluetooth_compatibility",LABELS="local_labels",EXTERNAL="external_connect";
    static SharedPreferences prefs(Context context){return context.getSharedPreferences(STORE,0);}
    static boolean labels(Context context){return prefs(context).getBoolean(LABELS,false);}
    static boolean external(Context context){return prefs(context).getBoolean(EXTERNAL,false);}
    static void set(Context context,String key,boolean value){prefs(context).edit().putBoolean(key,value).apply();}
    private final TextView heading;
    private final LinearLayout choices;
    private final CheckBox labels,external;
    private boolean updating;
    private final Runnable changed;
    private View up,down;
    View entry(){return heading;}
    View last(){return choices.getVisibility()==VISIBLE?external:heading;}
    void boundaries(View up,View down){this.up=up;this.down=down;routes();}
    private void routes(){FocusNavigation.route(heading,View.FOCUS_UP,up);FocusNavigation.route(heading,View.FOCUS_DOWN,choices.getVisibility()==VISIBLE?labels:down);FocusNavigation.route(labels,View.FOCUS_UP,heading);FocusNavigation.route(labels,View.FOCUS_DOWN,external);FocusNavigation.route(external,View.FOCUS_UP,labels);FocusNavigation.route(external,View.FOCUS_DOWN,down);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    BluetoothCompatibility(Context context,int accent){this(context,accent,null);}
    BluetoothCompatibility(Context context,int accent,Runnable changed){
        super(context);this.changed=changed;setOrientation(VERTICAL);choices=new LinearLayout(context);heading=new TextView(context);heading.setTextColor(accent);heading.setTextSize(12);heading.setGravity(Gravity.CENTER_VERTICAL);heading.setPadding(dp(4),0,dp(4),0);heading.setMinHeight(dp(36));heading.setTag(R.id.navigation_key,"bt:compatibility");heading.setOnClickListener(v->{choices.setVisibility(choices.getVisibility()==VISIBLE?GONE:VISIBLE);sync();});addView(heading,new LayoutParams(-1,-2));
        choices.setOrientation(VERTICAL);choices.setVisibility(GONE);addView(choices,new LayoutParams(-1,-2));labels=choice("Переименование: метки в приложении",LABELS);external=choice("Подключение и поиск: настройки Bluetooth",EXTERNAL);sync();
    }
    private CheckBox choice(String title,String key){CheckBox box=new CheckBox(getContext());box.setText(title);box.setTextSize(13);box.setTextColor(0xfff6f3e8);box.setMinHeight(dp(48));box.setPadding(dp(4),dp(3),dp(4),dp(3));box.setTag(R.id.navigation_key,"bt:compat:"+key);box.setOnCheckedChangeListener((b,value)->{if(!updating)set(getContext(),key,value);});choices.addView(box,new LayoutParams(-1,-2));return box;}
    private void sync(){updating=true;labels.setChecked(labels(getContext()));external.setChecked(external(getContext()));updating=false;heading.setText("Совместимость"+(labels.isChecked()||external.isChecked()?" · включена":"")+(choices.getVisibility()==VISIBLE?"  ▴":"  ▾"));heading.setContentDescription("Совместимость со старыми устройствами. "+(choices.getVisibility()==VISIBLE?"Свернуть":"Показать настройки"));FocusNavigation.prepare(this);routes();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();prefs(getContext()).registerOnSharedPreferenceChangeListener(this);sync();}
    @Override protected void onDetachedFromWindow(){prefs(getContext()).unregisterOnSharedPreferenceChangeListener(this);super.onDetachedFromWindow();}
    @Override public void onSharedPreferenceChanged(SharedPreferences p,String key){sync();if(changed!=null)changed.run();}
}
