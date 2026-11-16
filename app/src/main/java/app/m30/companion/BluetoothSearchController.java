package app.m30.companion;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

/** User-started Classic discovery and public createBond; no hidden HID connect API. */
final class BluetoothSearchController {
    private final Activity activity;
    private final boolean compact;
    private final int accent;
    private final Runnable checkButtons;
    interface ConnectionChoice {void select(String address,int slot);}
    private final int owner;
    private final Runnable cancelConnection;
    private BluetoothCompatibility compatibility;
    private final ConnectionChoice connectionChoice;
    private String waitingAddress="";
    private long connectionDeadline;
    private boolean connectionActionPending;
    private Button fallback;
    private final Runnable connectionTimeout=()->checkConnection();
    private View view;
    private Button savedButton;
    private static final int BG=0xff000000,CARD=0xff181818,TEXT=0xfff6f3e8,MUTED=0xffbbb7a8,ACCENT=0xffb6a45e;
    static final int PERMISSIONS=51,ENABLE=52,READ_SAVED=53,DEVICE_ACTION=54;
    private BluetoothAdapter adapter;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Set<String> savedAddresses=new HashSet<>();
    private final Map<String,BluetoothDevice> candidates=new LinkedHashMap<>();
    private final Map<String,Button> rows=new LinkedHashMap<>();
    private final BluetoothPairingState pairing=new BluetoothPairingState();
    private final BluetoothDeviceList devices=new BluetoothDeviceList();
    private LinearLayout body,results,savedResults,foundResults,actions;
    private TextView status,savedEmpty,foundEmpty,savedHeading;
    private View topBoundary,bottomBoundary;
    private Button search,settings;
    private Space headerReserve;
    private AlertDialog deviceMenu,labelEditor,labelPresets,scanDialog;
    private final BluetoothControllerNames controllerNames;
    private boolean registered,ownsDiscovery,resumed,savedSnapshotValid;
    private final Runnable scanTimeout=()->finishScan("Поиск завершён. Если M30 не найден, включите PAIR и повторите.");
    private final Runnable pairTimeout=()->reconcilePair();
    private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){
        if(!resumed||!canReadSaved())return;
        String action=intent.getAction();
        if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
            if(ownsDiscovery){ownsDiscovery=false;handler.removeCallbacks(scanTimeout);finishScan("Поиск завершён. Выберите геймпад или повторите поиск.");}return;
        }
        if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
            if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR)==BluetoothAdapter.STATE_OFF){finishScan("Bluetooth выключен.");pairing.fail();updateControls();}return;
        }
        BluetoothDevice found=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);if(found==null)return;
        try{
            if(BluetoothDevice.ACTION_FOUND.equals(action)&&ownsDiscovery){String name=intent.getStringExtra(BluetoothDevice.EXTRA_NAME);offer(found,name,true,false);}
            else if((BluetoothDevice.ACTION_NAME_CHANGED.equals(action)||BluetoothDevice.ACTION_ALIAS_CHANGED.equals(action))&&(ownsDiscovery||candidates.containsKey(found.getAddress())))offer(found,intent.getStringExtra(BluetoothDevice.EXTRA_NAME));
            else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                int bond=intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,BluetoothDevice.ERROR);String address=found.getAddress();
                if(bond==BluetoothDevice.BOND_NONE){boolean wasSaved=savedAddresses.contains(address)||intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,BluetoothDevice.ERROR)==BluetoothDevice.BOND_BONDED;devices.bondChanged(address,bond);if(wasSaved){BluetoothBondMemory.removed(activity,address);savedAddresses.remove(address);clearWaiting(address);pairing.forget(address);handler.removeCallbacks(pairTimeout);closeDeviceMenu();status.setText("Пара удалена в Android. Включите PAIR на M30 и найдите его заново.");}else if(pairing.bondEvent(address,bond))showPairState();refreshRows();updateControls();}
                else{if(bond==BluetoothDevice.BOND_BONDED)savedAddresses.add(address);if(candidates.containsKey(address))offer(found,null,false,bond==BluetoothDevice.BOND_BONDED);devices.bondChanged(address,bond);if(pairing.bondEvent(address,bond))showPairState();refreshRows();}

            }
        }catch(SecurityException error){accessLost();}
    }};
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    private boolean landscape(){return activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;}
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;}
    private TextView label(String value,int size,int color){TextView v=new TextView(activity);v.setText(value);v.setTextColor(color);v.setTextSize(size);v.setPadding(0,dp(7),0,dp(7));return v;}
    private Button button(String text,Runnable action){Button b=new Button(activity);b.setText(text);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(TEXT);b.setBackground(shape(CARD));b.setPadding(dp(12),dp(10),dp(12),dp(10));b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(7);b.setLayoutParams(lp);return b;}
    private Button menuButton(String text,Runnable action){
        Button b=button(text,action);android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
        GradientDrawable disabled=shape(0xff222222);disabled.setStroke(dp(1),0xff41413d);states.addState(new int[]{-android.R.attr.state_enabled},disabled);
        GradientDrawable pressed=shape(0xff454545);pressed.setStroke(dp(1),0xffa6a69c);states.addState(new int[]{android.R.attr.state_pressed},pressed);
        GradientDrawable normal=shape(0xff303030);normal.setStroke(dp(1),0xff686861);states.addState(new int[]{},normal);b.setBackground(states);
        b.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff9a9a91,TEXT}));return b;
    }
    BluetoothSearchController(Activity activity,Bundle saved,boolean compact,int accent,Runnable checkButtons){
        this(activity,saved,compact,accent,checkButtons,-1,null,null);
    }
    BluetoothSearchController(Activity activity,Bundle saved,boolean compact,int accent,Runnable checkButtons,int owner,ConnectionChoice connectionChoice){
        this(activity,saved,compact,accent,checkButtons,owner,connectionChoice,null);
    }
    BluetoothSearchController(Activity activity,Bundle saved,boolean compact,int accent,Runnable checkButtons,int owner,ConnectionChoice connectionChoice,Runnable cancelConnection){
        this.cancelConnection=cancelConnection;this.activity=activity;this.compact=compact;this.accent=accent;this.checkButtons=checkButtons;this.owner=owner;this.connectionChoice=connectionChoice;
        if(saved!=null){waitingAddress=saved.getString("connection_address","");connectionDeadline=saved.getLong("connection_deadline");connectionActionPending=saved.getBoolean("connection_action_pending");}
        controllerNames=new BluetoothControllerNames(activity,()->{if(savedResults!=null){refreshRows();checkConnection();}});
        BluetoothManager manager=activity.getSystemService(BluetoothManager.class);adapter=manager==null?null:manager.getAdapter();
        if(saved!=null)pairing.restore(saved.getString("address"),saved.getInt("phase"),saved.getLong("deadline"),saved.getBoolean("sawBonding"),SystemClock.elapsedRealtime());
        if(compact)buildCompact();else buildFull();
        FocusNavigation.prepare(view);if(pairing.phase()==BluetoothPairingState.PAIRED||pairing.phase()==BluetoothPairingState.ERROR)showPairState();updateControls();
        if(canReadSaved()&&pairing.phase()!=BluetoothPairingState.PAIRING)loadSaved(false);
    }
    View view(){return view;}
    boolean searching(){return ownsDiscovery;}
    View firstAction(){return settings;}
    void boundaries(View top,View bottom){topBoundary=top;bottomBoundary=bottom;wireFocus();}
    void reserveHeader(int top,int right){
        if(!compact)return;
        int reserve=Math.max(0,right);ViewGroup.LayoutParams lp=headerReserve.getLayoutParams();
        if(lp.width!=reserve){lp.width=reserve;headerReserve.setLayoutParams(lp);}
        if(view.getPaddingTop()!=top)view.setPadding(0,top,0,0);
    }
    private void buildFull(){
        ScrollView scroll=new ScrollView(activity);scroll.setBackgroundColor(BG);body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(12),dp(18),dp(20));scroll.addView(body);view=scroll;
        if(Build.VERSION.SDK_INT>=30)body.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(18)+i.left,dp(12)+i.top,dp(18)+i.right,dp(20)+i.bottom);return insets;});
        body.addView(label("Bluetooth · геймпады",22,TEXT));body.addView(label("Включите нужный режим M30. Для поиска держите PAIR 2 секунды.",14,accent));buildActions(body);buildStatus(body);buildLists(body,dp(Math.max(220,activity.getResources().getConfiguration().screenHeightDp/2)));
        body.addView(label("Слева сохранённые пары Android; справа ответы текущего поиска. Зелёный цвет означает совпадение адреса, а не активное подключение.",12,MUTED));
        Button check=button("Проверить кнопки",checkButtons);body.addView(check);bottomBoundary=check;wireFocus();
    }
    private void buildCompact(){
        LinearLayout shell=new LinearLayout(activity);shell.setOrientation(LinearLayout.VERTICAL);shell.setPadding(0,dp(8),0,0);shell.setBackgroundColor(BG);view=shell;
        buildActions(shell);buildStatus(shell);buildLists(shell,0);
    }
    private void buildActions(LinearLayout target){
        if(compact){
            actions=new LinearLayout(activity);actions.setGravity(Gravity.CENTER_VERTICAL);target.addView(actions,new LinearLayout.LayoutParams(-1,-2));headerReserve=new Space(activity);actions.addView(headerReserve,new LinearLayout.LayoutParams(0,1));
            settings=button("Системные настройки Bluetooth",()->openSettings(Settings.ACTION_BLUETOOTH_SETTINGS));settings.setTag(R.id.navigation_key,"bt:settings");settings.setTextSize(12);settings.setMaxLines(2);settings.setPadding(dp(5),dp(5),dp(5),dp(5));settings.setMinHeight(dp(44));LinearLayout.LayoutParams settingsLp=new LinearLayout.LayoutParams(0,-2,1.4f);actions.addView(settings,settingsLp);
            search=button("Начать поиск",this::toggleSearch);search.setTag(R.id.navigation_key,"bt:search");search.setTextSize(12);search.setPadding(dp(5),dp(5),dp(5),dp(5));search.setMinHeight(dp(44));LinearLayout.LayoutParams searchLp=new LinearLayout.LayoutParams(0,-2,1);searchLp.leftMargin=dp(5);actions.addView(search,searchLp);return;
        }
        int topHeight=dp(56),bottomHeight=dp(48);
        actions=new LinearLayout(activity);actions.setOrientation(LinearLayout.VERTICAL);target.addView(actions,new LinearLayout.LayoutParams(-1,topHeight+dp(8)+bottomHeight));
        LinearLayout top=new LinearLayout(activity);top.setGravity(Gravity.CENTER_VERTICAL);actions.addView(top,new LinearLayout.LayoutParams(-1,topHeight));
        if(compact){headerReserve=new Space(activity);top.addView(headerReserve,new LinearLayout.LayoutParams(dp(104),1));}
        settings=button("Системные настройки Bluetooth",()->openSettings(Settings.ACTION_BLUETOOTH_SETTINGS));settings.setContentDescription("Системные настройки Bluetooth");settings.setTag(R.id.navigation_key,"bt:settings");settings.setTextSize(14);settings.setPadding(dp(6),dp(4),dp(6),dp(4));settings.setMaxLines(2);LinearLayout.LayoutParams set=new LinearLayout.LayoutParams(0,topHeight,1);set.leftMargin=compact?dp(8):0;top.addView(settings,set);
        Space gap=new Space(activity);actions.addView(gap,new LinearLayout.LayoutParams(1,dp(8)));
        LinearLayout searchRow=new LinearLayout(activity);actions.addView(searchRow,new LinearLayout.LayoutParams(-1,bottomHeight));
        search=button("Начать поиск",this::toggleSearch);search.setTag(R.id.navigation_key,"bt:search");search.setMinHeight(0);search.setMinimumHeight(0);search.setPadding(dp(8),dp(4),dp(8),dp(4));searchRow.addView(search,new LinearLayout.LayoutParams(-1,bottomHeight));
    }
    private void buildStatus(LinearLayout target){
        compatibility=new BluetoothCompatibility(activity,accent,()->{if(BluetoothCompatibility.external(activity)&&ownsDiscovery)finishScan("Выбран поиск в настройках Android.");else updateControls();});target.addView(compatibility,new LinearLayout.LayoutParams(-1,-2));
        status=label(adapter==null?"Bluetooth недоступен.":"Выберите устройство или начните поиск.",14,TEXT);status.setPadding(dp(2),dp(3),dp(2),dp(3));status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);status.setTag(R.id.navigation_key,"bt:status");
        fallback=button("Другой способ подключения",()->{BluetoothDeviceList.Entry entry=devices.get(waitingAddress);if(entry!=null)launchDeviceAction(entry,true,true);});fallback.setTag(R.id.navigation_key,"bt:fallback");fallback.setVisibility(View.GONE);
        status.setOnClickListener(v->new FocusDialogBuilder(activity).setTitle("Bluetooth").setMessage(status.getText()).setPositiveButton("Закрыть",null).show());target.addView(status);target.addView(fallback);
    }
    private LinearLayout listColumn(LinearLayout target,boolean saved){
        LinearLayout column=new LinearLayout(activity);column.setOrientation(LinearLayout.VERTICAL);column.setBackground(shape(CARD));column.setPadding(dp(8),dp(4),dp(8),dp(8));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);if(!saved)cp.leftMargin=dp(8);target.addView(column,cp);
        LinearLayout heading=new LinearLayout(activity);heading.setGravity(Gravity.CENTER_VERTICAL);heading.setMinimumHeight(dp(40));column.addView(heading,new LinearLayout.LayoutParams(-1,-2));
        TextView caption=label(saved?"Ранее подключённые (0)":"Найдены рядом",14,accent);caption.setPadding(dp(2),dp(2),dp(4),dp(2));caption.setGravity(Gravity.CENTER_VERTICAL);heading.addView(caption,new LinearLayout.LayoutParams(0,-2,1));if(saved)savedHeading=caption;
        if(saved){savedButton=button("↻",()->loadSaved(true));savedButton.setTextSize(18);savedButton.setPadding(0,0,0,0);savedButton.setMinHeight(0);savedButton.setMinimumHeight(0);savedButton.setContentDescription("Обновить сохранённые пары Android");savedButton.setTag(R.id.navigation_key,"bt:saved");heading.addView(savedButton,new LinearLayout.LayoutParams(dp(40),dp(40)));}
        ScrollView scroll=new ScrollView(activity);scroll.setFocusable(false);
        if(saved){scroll.setTag(R.id.navigation_key,"bt:saved-scroll");scroll.setVerticalScrollBarEnabled(true);scroll.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);scroll.setScrollbarFadingEnabled(false);scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);scroll.setScrollBarSize(dp(4));
            if(Build.VERSION.SDK_INT>=29){GradientDrawable thumb=shape(MUTED);scroll.setVerticalScrollbarThumbDrawable(thumb);scroll.setVerticalScrollbarTrackDrawable(shape(0xff333333));}
        }
        column.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout inside=new LinearLayout(activity);inside.setOrientation(LinearLayout.VERTICAL);scroll.addView(inside);
        TextView empty=label(saved?(canReadSaved()?"Сохранённых геймпадов нет.":"Нажмите ↻, чтобы загрузить пары Android."):"Нажмите «Начать поиск».",13,MUTED);empty.setPadding(0,dp(3),0,dp(3));inside.addView(empty);if(saved)savedEmpty=empty;else foundEmpty=empty;
        LinearLayout group=new LinearLayout(activity);group.setOrientation(LinearLayout.VERTICAL);inside.addView(group);return group;
    }
    private void buildLists(LinearLayout target,int height){
        results=new LinearLayout(activity);results.setOrientation(LinearLayout.HORIZONTAL);results.setTag(R.id.navigation_key,"bt:columns");target.addView(results,new LinearLayout.LayoutParams(-1,height,height==0?1:0));
        savedResults=listColumn(results,true);foundResults=listColumn(results,false);
        TextView hint=label("Одинаковые джойстики лучше переименовать в Bluetooth.",12,MUTED);hint.setPadding(dp(2),dp(4),dp(2),0);hint.setTag(R.id.navigation_key,"bt:rename-hint");target.addView(hint,new LinearLayout.LayoutParams(-1,-2));
    }
    private View firstRow(LinearLayout group){return group.getChildCount()>0?group.getChildAt(0):null;}
    private void wireFocus(){
        if(savedResults==null)return;
        View first=firstRow(savedResults);if(first==null)first=firstRow(foundResults);if(first==null)first=savedButton;
        Button[] bar={settings,search};
        for(int n=0;n<bar.length;n++){
            Button b=bar[n];View left=topBoundary==null?b:topBoundary,right=b;
            for(int i=n-1;i>=0;i--)if(bar[i].isEnabled()){left=bar[i];break;}
            for(int i=n+1;i<bar.length;i++)if(bar[i].isEnabled()){right=bar[i];break;}
            FocusNavigation.route(b,View.FOCUS_LEFT,left);FocusNavigation.route(b,View.FOCUS_RIGHT,right);FocusNavigation.route(b,View.FOCUS_UP,topBoundary==null?b:topBoundary);FocusNavigation.route(b,View.FOCUS_DOWN,compatibility==null?status:compatibility.entry());
        }
        if(compatibility!=null)compatibility.boundaries(search,status);FocusNavigation.route(status,View.FOCUS_UP,compatibility==null?search:compatibility.last());FocusNavigation.route(status,View.FOCUS_DOWN,fallback!=null&&fallback.getVisibility()==View.VISIBLE?fallback:first);if(fallback!=null){FocusNavigation.route(fallback,View.FOCUS_UP,status);FocusNavigation.route(fallback,View.FOCUS_DOWN,first);}
        FocusNavigation.route(savedButton,View.FOCUS_UP,status);FocusNavigation.route(savedButton,View.FOCUS_DOWN,firstRow(savedResults)==null?(bottomBoundary==null?status:bottomBoundary):firstRow(savedResults));FocusNavigation.route(savedButton,View.FOCUS_RIGHT,firstRow(foundResults)==null?savedButton:firstRow(foundResults));
        for(boolean saved:new boolean[]{true,false}){
            LinearLayout group=saved?savedResults:foundResults,other=saved?foundResults:savedResults;
            for(int n=0;n<group.getChildCount();n++){
                View row=group.getChildAt(n);String key=FocusNavigation.key(row),address=key.substring(key.lastIndexOf('|')+1);View peer=rows.get((saved?"found|":"saved|")+address);if(peer==null&&other.getChildCount()>0)peer=other.getChildAt(Math.min(n,other.getChildCount()-1));if(peer==null)peer=saved?savedButton:row;
                FocusNavigation.route(row,saved?View.FOCUS_RIGHT:View.FOCUS_LEFT,peer);FocusNavigation.route(row,saved?View.FOCUS_LEFT:View.FOCUS_RIGHT,row);
                FocusNavigation.route(row,View.FOCUS_UP,n==0?(saved?savedButton:status):group.getChildAt(n-1));FocusNavigation.route(row,View.FOCUS_DOWN,n+1<group.getChildCount()?group.getChildAt(n+1):(bottomBoundary==null?row:bottomBoundary));
            }
        }
    }
    private boolean canReadSaved(){return Build.VERSION.SDK_INT<31||activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
    private void loadSaved(boolean request){
        savedSnapshotValid=false;
        if(adapter==null||ownsDiscovery||pairing.phase()==BluetoothPairingState.PAIRING)return;
        if(!canReadSaved()){if(request)activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},READ_SAVED);return;}
        try{
            if(!adapter.isEnabled()){status.setText("Bluetooth выключен. Включите его через системные настройки или запустите поиск.");return;}
            Set<BluetoothDevice> snapshot=BluetoothBondMemory.read(activity,adapter);if(snapshot==null)return;savedAddresses.clear();for(BluetoothDevice device:snapshot)savedAddresses.add(device.getAddress());devices.syncSaved(savedAddresses);savedSnapshotValid=true;for(BluetoothDevice device:snapshot)offer(device,null,false,true);if(resumed)registerBluetooth();refreshRows();savedEmpty.setText("Сохранённых геймпадов нет.");
            if(request)status.setText("Список сохранённых устройств обновлён.");updateControls();
        }catch(SecurityException error){accessLost();}
    }
    private boolean hasPermissions(){
        if(Build.VERSION.SDK_INT>=31)return activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED&&activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;
        return activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private void toggleSearch(){if(ownsDiscovery)finishScan("Поиск остановлен.");else beginSearch();}
    private void beginSearch(){
        if(adapter==null||pairing.phase()==BluetoothPairingState.PAIRING||ownsDiscovery)return;
        int path=BluetoothScanPolicy.path(Build.VERSION.SDK_INT,BluetoothCompatibility.external(activity),locationEnabled());
        if(path==BluetoothScanPolicy.EXTERNAL){externalSearch();return;}
        if(path==BluetoothScanPolicy.OFFER_EXTERNAL){offerSystemSearch("На этом Android поиск новых Bluetooth-устройств из приложения зависит от включённой геолокации. Сохранённые пары доступны без неё.");return;}
        if(!hasPermissions()){
            activity.requestPermissions(Build.VERSION.SDK_INT>=31?new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT}:new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS);return;
        }
        try{
            if(!adapter.isEnabled()){activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),ENABLE);return;}
            registerBluetooth();
            if(adapter.isDiscovering()){status.setText("Уже идёт другой поиск Bluetooth. Дождитесь его завершения и повторите.");return;}
            devices.newSearch();refreshRows();
            loadSaved(false);if(!savedSnapshotValid)return;refreshRows();
            ownsDiscovery=adapter.startDiscovery();
            if(ownsDiscovery){status.setText("Поиск… Устройства появляются по мере обнаружения.");foundEmpty.setText("Идёт поиск геймпадов рядом…");handler.postDelayed(scanTimeout,25_000);}
            else offerSystemSearch("Android не запустил поиск из приложения. Можно использовать системный Bluetooth.");
            updateControls();if(ownsDiscovery)search.requestFocus();
        }catch(SecurityException error){accessLost();}catch(RuntimeException error){finishScan("Не удалось запустить поиск: "+error.getClass().getSimpleName());}
    }
    private void externalSearch(){finishScan(null);devices.newSearch();refreshRows();status.setText("Найдите и подключите M30 в настройках Bluetooth Android. При возвращении список пар обновится.");openSettings(Settings.ACTION_BLUETOOTH_SETTINGS);}
    private void offerSystemSearch(String reason){
        if(scanDialog!=null)scanDialog.dismiss();status.setText(reason);LinearLayout form=new LinearLayout(activity);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),0,dp(16),dp(4));form.addView(label(reason,14,TEXT));CheckBox use=new CheckBox(activity);use.setTag(R.id.navigation_key,"bt:external-search");use.setText("Использовать настройки Bluetooth для подключения и поиска");use.setTextColor(TEXT);use.setTextSize(14);use.setPadding(dp(6),dp(4),dp(8),dp(4));use.setMinHeight(dp(48));form.addView(use);
        FocusDialogBuilder builder=new FocusDialogBuilder(activity);builder.setTitle("Поиск устройств").setView(form).setPositiveButton("Продолжить",(d,w)->{BluetoothCompatibility.set(activity,BluetoothCompatibility.EXTERNAL,true);externalSearch();}).setNegativeButton("Отмена",null);
        boolean tv=(activity.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_TYPE_MASK)==android.content.res.Configuration.UI_MODE_TYPE_TELEVISION;
        Intent location=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);if(!tv&&activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION)&&location.resolveActivity(activity.getPackageManager())!=null)builder.setNeutralButton("Геолокация",(d,w)->openSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        AlertDialog dialog=builder.show();scanDialog=dialog;FocusDialogBuilder.confirmation(dialog,use);dialog.setOnDismissListener(d->{if(scanDialog==dialog)scanDialog=null;});
    }
    private boolean locationEnabled(){LocationManager m=(LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);if(m==null)return false;if(Build.VERSION.SDK_INT>=28)return m.isLocationEnabled();return m.isProviderEnabled(LocationManager.GPS_PROVIDER)||m.isProviderEnabled(LocationManager.NETWORK_PROVIDER);}
    private void registerBluetooth(){
        if(registered)return;IntentFilter filter=new IntentFilter();
        if(Build.VERSION.SDK_INT>=30)filter.addAction(BluetoothDevice.ACTION_ALIAS_CHANGED);filter.addAction(BluetoothDevice.ACTION_FOUND);filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        // These are protected platform actions, delivered by the privileged Bluetooth process.
        activity.registerReceiver(receiver,filter,Build.VERSION.SDK_INT>=33?Context.RECEIVER_EXPORTED:0);registered=true;
    }
    private void offer(BluetoothDevice device,String announcedName){offer(device,announcedName,false,false);}
    private void offer(BluetoothDevice device,String announcedName,boolean discovered,boolean savedSnapshot){
        try{
            String address=device.getAddress(),name=announcedName==null?device.getName():announcedName;
            BluetoothClass type=device.getBluetoothClass();int kind=BluetoothGamepadFilter.classify(name,type==null?0:type.getDeviceClass());
            BluetoothDeviceList.Entry existing=devices.get(address);
            if(kind==BluetoothGamepadFilter.NONE&&existing!=null&&(name==null||name.isEmpty()))kind=existing.kind;
            if(kind==BluetoothGamepadFilter.NONE){devices.remove(address);refreshRows();return;}
            String clean=BluetoothGamepadFilter.cleanName(name);if(clean.isEmpty())clean=existing==null?"Геймпад без имени":existing.name;
            int bond=savedSnapshot||savedAddresses.contains(address)?BluetoothDevice.BOND_BONDED:device.getBondState()==BluetoothDevice.BOND_BONDING?BluetoothDevice.BOND_BONDING:BluetoothDevice.BOND_NONE;
            BluetoothDeviceList.Entry entry=devices.offer(address,clean,kind,bond,discovered);if(entry==null)return;
            if(announcedName!=null&&!announcedName.isEmpty())entry.name=clean;
            if(Build.VERSION.SDK_INT>=30&&entry.saved()){String alias=device.getAlias();entry.alias=alias==null?"":BluetoothGamepadFilter.cleanName(alias);}
            entry.mode=activity.getSharedPreferences("bluetooth_modes",0).getInt(entry.address,-1);
            candidates.put(entry.address,device);refreshRows();
        }catch(SecurityException error){accessLost();}
    }
    private CharSequence deviceRowText(String name,String detail,String suffix){
        boolean horizontal=landscape();String separator=horizontal?" · ":"\n";
        android.text.SpannableString text=new android.text.SpannableString(name+separator+detail+"\n"+suffix);
        if(horizontal){int start=name.length()+separator.length();text.setSpan(new android.text.style.RelativeSizeSpan(12f/14f),start,text.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
        java.util.regex.Matcher modes=java.util.regex.Pattern.compile("(?iu)(?<![\\p{L}\\p{N}])(?:android|windows(?:[ -]*xbox)?|xbox|macos|switch|[xd][ -]?input)(?![\\p{L}\\p{N}])").matcher(name);
        while(modes.find())text.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),modes.start(),modes.end(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }
    private void refreshRows(){
        if(savedHeading!=null)savedHeading.setText("Ранее подключённые ("+devices.section(true).size()+")");
        Set<String> wanted=new HashSet<>();for(boolean saved:new boolean[]{true,false})for(BluetoothDeviceList.Entry e:devices.section(saved))wanted.add((saved?"saved|":"found|")+e.address);
        for(String key:new ArrayList<>(rows.keySet()))if(!wanted.contains(key)){Button row=rows.remove(key);boolean focused=row.hasFocus();if(row.getParent() instanceof ViewGroup)((ViewGroup)row.getParent()).removeView(row);if(focused)settings.requestFocus();}
        for(String address:new ArrayList<>(candidates.keySet()))if(devices.get(address)==null)candidates.remove(address);
        for(boolean saved:new boolean[]{true,false}){
            LinearLayout group=saved?savedResults:foundResults;List<BluetoothDeviceList.Entry> entries=devices.section(saved);
            for(int n=0;n<entries.size();n++){
                BluetoothDeviceList.Entry e=entries.get(n);String key=(saved?"saved|":"found|")+e.address;Button row=rows.get(key);
                if(row==null){row=button("",()->select(candidates.get(e.address)));row.setOnLongClickListener(v->{select(candidates.get(e.address));return true;});row.setTag(R.id.navigation_key,"bt:"+key);row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);rows.put(key,row);row.setTextSize(14);row.setPadding(dp(7),dp(4),dp(7),dp(4));LinearLayout.LayoutParams rp=(LinearLayout.LayoutParams)row.getLayoutParams();rp.topMargin=dp(4);FocusNavigation.prepare(row);}
                if(row.getParent()!=group||group.indexOfChild(row)!=n){boolean focused=row.hasFocus();if(row.getParent() instanceof ViewGroup)((ViewGroup)row.getParent()).removeView(row);group.addView(row,Math.min(n,group.getChildCount()));if(focused)row.requestFocus();}
                boolean match=e.saved()&&e.found;int slot=controllerNames.activeSlot(e.address);boolean connected=slot>=0;GradientDrawable bg=shape(connected?0xff18363c:match?0xff203125:0xff242424);if(connected||match)bg.setStroke(dp(connected?2:1),connected?0xff77dce5:0xff73d39b);row.setBackground(bg);
                String detail=connected?"Подключён · Джойстик "+(slot+1):e.bond==BluetoothDevice.BOND_BONDING?"Сопряжение…":match?"Совпадение · пара сохранена":saved?"Пара сохранена":"Найден в поиске";
                String displayName=e.saved()?controllerNames.label(e.address).displayName(e.displayName()):e.displayName();
                row.setText(deviceRowText(displayName,detail,e.address.substring(9)));row.setEnabled(pairing.phase()!=BluetoothPairingState.PAIRING);
                row.setContentDescription(displayName+". Имя в Android: "+e.displayName()+". "+detail+". "+e.address+(connected?". По вашей привязке к активному источнику ввода":""));
            }
            (saved?savedEmpty:foundEmpty).setVisibility(entries.isEmpty()?View.VISIBLE:View.GONE);
        }
        wireFocus();
    }
    private void select(BluetoothDevice device){
        if(device==null||pairing.phase()==BluetoothPairingState.PAIRING)return;
        finishScan(null);
        try{
            loadSaved(false);if(!savedSnapshotValid)return;offer(device,null);BluetoothDeviceList.Entry e=devices.get(device.getAddress());if(e==null)return;
            if(e.action()==BluetoothDeviceList.WAIT){pairing.select(e.address,SystemClock.elapsedRealtime());pairing.poll(e.address,device.getBondState());registerBluetooth();showPairState();return;}
            boolean saved=e.action()==BluetoothDeviceList.SYSTEM_CONNECT;
            showDeviceMenu(e,device,saved);
        }catch(SecurityException error){accessLost();}
    }
    private void showDeviceMenu(BluetoothDeviceList.Entry entry,BluetoothDevice device,boolean saved){
        closeDeviceMenu();ScrollView scroll=new ScrollView(activity);LinearLayout menu=new LinearLayout(activity);menu.setOrientation(LinearLayout.VERTICAL);menu.setPadding(dp(14),0,dp(14),dp(8));scroll.addView(menu);
        TextView identity=label(entry.displayName()+" · "+entry.address,12,MUTED);identity.setPadding(0,dp(3),0,dp(3));menu.addView(identity);
        Button connect=menuButton("Подключить устройство",()->{closeDeviceMenu();connectDevice(entry,device);});connect.setTag(R.id.navigation_key,"bt:device-action:0");menu.addView(connect);
        Button name=menuButton("Переименовать",()->{closeDeviceMenu();launchDeviceAction(entry,false,false);});name.setTag(R.id.navigation_key,"bt:rename");menu.addView(name);
        if(!controllerNames.label(entry.address).empty()){Button local=menuButton("Метка в приложении",()->showLabelEditor(entry));local.setTag(R.id.navigation_key,"bt:local-label");menu.addView(local);}
        if(saved){
            LinearLayout row=new LinearLayout(activity);row.setBaselineAligned(false);menu.addView(row);
            Button disconnect=menuButton("Отключить",()->{closeDeviceMenu();disconnectDevice(entry);});disconnect.setTag(R.id.navigation_key,"bt:disconnect");Button forget=menuButton("Забыть в Android",()->{closeDeviceMenu();forgetInAndroid(entry);});forget.setTag(R.id.navigation_key,"bt:forget-system");for(Button action:new Button[]{disconnect,forget}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(0,dp(5),dp(4),0);row.addView(action,lp);}
            Button link=menuButton("Привязка к джойстику…",()->{closeDeviceMenu();String[] options=new String[controllerNames.linked(entry.address)?3:2];for(int n=0;n<2;n++){android.view.InputDevice source=controllerNames.source(n);options[n]="Это Джойстик "+(n+1)+" · "+(source==null?"нет источника":source.getName());}if(options.length==3)options[2]="Убрать привязку";
                deviceMenu=new FocusDialogBuilder(activity).setTitle("Источник кнопок этого M30").setItems(options,(d,n)->{if(n==2)controllerNames.unlink(entry.address);else if(!controllerNames.bind(entry.address,n))Toast.makeText(activity,"Сначала подключите и выберите источник кнопок этого джойстика.",Toast.LENGTH_LONG).show();refreshRows();}).setNegativeButton("Отмена",null).show();});menu.addView(link);
        }
        Button clear=menuButton("Удалить из приложения",()->confirmLocalRemoval(entry));clear.setTag(R.id.navigation_key,"bt:forget-local");menu.addView(clear);
        AlertDialog dialog=new FocusDialogBuilder(activity).setTitle(entry.saved()?controllerNames.label(entry.address).displayName(entry.displayName()):entry.displayName()).setView(scroll).setNegativeButton("Закрыть",null).show();deviceMenu=dialog;dialog.setOnDismissListener(d->{if(deviceMenu==dialog)deviceMenu=null;});
    }
    private void showLabelEditor(BluetoothDeviceList.Entry entry){
        closeDeviceMenu();closeLabelEditor();labelEditor=DeviceLabelEditor.show(activity,entry.address,owner,"",()->{status.setText("Метка сохранена в приложении.");refreshRows();});AlertDialog current=labelEditor;current.setOnDismissListener(d->{if(labelEditor==current)labelEditor=null;});
    }
    private void launchDeviceAction(BluetoothDeviceList.Entry entry,boolean connect,boolean alternative){
        if(connect)connectionActionPending=true;
        activity.startActivityForResult(new Intent(activity,BluetoothAliasActivity.class).putExtra("address",entry.address).putExtra("name",entry.displayName()).putExtra("owner",owner).putExtra(BluetoothAliasActivity.OPERATION,connect?BluetoothAliasActivity.CONNECT:"rename").putExtra("alternative",alternative),DEVICE_ACTION);
    }
    private void connectDevice(BluetoothDeviceList.Entry entry,BluetoothDevice device){
        if(device==null){status.setText("Устройство уже недоступно. Обновите список.");return;}
        loadSaved(false);if(!savedSnapshotValid)return;entry.bond=savedAddresses.contains(entry.address)?BluetoothDevice.BOND_BONDED:BluetoothDevice.BOND_NONE;if(!entry.saved())entry.alias="";
        waitingAddress=entry.address;connectionDeadline=SystemClock.elapsedRealtime()+15_000;fallback.setVisibility(View.GONE);
        if(connectionChoice!=null)connectionChoice.select(entry.address,owner);
        int assigned=controllerNames.activeSlot(entry.address);boolean confirmed=entry.saved()&&assigned>=0&&(owner<0||assigned==owner);
        int action=BluetoothActionPolicy.connect(Build.VERSION.SDK_INT,activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP),entry.saved(),BluetoothCompatibility.external(activity),confirmed);
        if(action==BluetoothActionPolicy.CONNECTED)checkConnection();else if(action==BluetoothActionPolicy.PAIR)pair(device);else launchDeviceAction(entry,true,false);
    }
    private void checkConnection(){
        handler.removeCallbacks(connectionTimeout);if(status==null||waitingAddress.isEmpty()||!resumed||connectionActionPending)return;
        int slot=controllerNames.activeSlot(waitingAddress);
        if(slot>=0&&(owner<0||slot==owner)){status.setText("Подключён Джойстик "+(slot+1)+" · "+controllerNames.slotName(slot));waitingAddress="";fallback.setVisibility(View.GONE);return;}
        if(pairing.phase()==BluetoothPairingState.PAIRING)return;
        String target=owner<0?"джойстике":"Джойстике "+(owner+1);
        if(SystemClock.elapsedRealtime()<connectionDeadline){status.setText("Включите выбранный M30. Ожидаем подключения Android; затем нажмите кнопку на "+target+", чтобы подтвердить источник.");handler.postDelayed(connectionTimeout,Math.max(1,connectionDeadline-SystemClock.elapsedRealtime()));}
        else{status.setText("Источник кнопок пока не подтверждён. Нажмите кнопку на выбранном M30. Если он не подключился, выберите другой способ подключения.");fallback.setVisibility(View.VISIBLE);FocusNavigation.prepare(fallback);wireFocus();}
    }
    private void closeLabelEditor(){if(labelPresets!=null){AlertDialog previous=labelPresets;labelPresets=null;previous.dismiss();}if(labelEditor!=null){AlertDialog previous=labelEditor;labelEditor=null;previous.dismiss();}}
    private void closeDeviceMenu(){if(deviceMenu!=null){AlertDialog previous=deviceMenu;deviceMenu=null;previous.dismiss();}}
    private void clearWaiting(String address){if(waitingAddress.equals(address)){waitingAddress="";connectionActionPending=false;handler.removeCallbacks(connectionTimeout);if(fallback!=null)fallback.setVisibility(View.GONE);if(cancelConnection!=null)cancelConnection.run();}}
    private boolean stillSaved(BluetoothDeviceList.Entry entry){
        if(!canReadSaved()){status.setText("Разрешите доступ к Bluetooth и обновите список.");return false;}
        if(adapter==null||!adapter.isEnabled()){status.setText("Включите Bluetooth, чтобы проверить состояние пары.");return false;}
        loadSaved(false);if(!savedSnapshotValid)return false;if(savedAddresses.contains(entry.address))return true;
        BluetoothBondMemory.removed(activity,entry.address);clearWaiting(entry.address);pairing.forget(entry.address);devices.bondChanged(entry.address,BluetoothDevice.BOND_NONE);refreshRows();status.setText("Этой пары уже нет в Android. Включите PAIR на M30 и выполните новое сопряжение.");return false;
    }
    private void disconnectDevice(BluetoothDeviceList.Entry entry){
        if(!stillSaved(entry))return;clearWaiting(entry.address);
        int action=BluetoothActionPolicy.disconnect(Build.VERSION.SDK_INT,activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP),BluetoothCompatibility.external(activity));
        if(action==BluetoothActionPolicy.NATIVE)activity.startActivityForResult(new Intent(activity,BluetoothAliasActivity.class).putExtra("address",entry.address).putExtra("name",entry.displayName()).putExtra(BluetoothAliasActivity.OPERATION,BluetoothAliasActivity.DISCONNECT),DEVICE_ACTION);
        else deviceActionInSettings(entry,"Отключить");
    }
    private void forgetInAndroid(BluetoothDeviceList.Entry entry){
        if(!stillSaved(entry))return;
        deviceMenu=new FocusDialogBuilder(activity).setTitle("Забыть пару в Android").setMessage("Android разрешает удалить эту Bluetooth-пару только в системных настройках. После удаления метка и привязка в приложении очистятся автоматически.").setPositiveButton("Открыть настройки",(d,w)->{if(stillSaved(entry))deviceActionInSettings(entry,"Забыть устройство");}).setNegativeButton("Отмена",null).show();
    }
    private void confirmLocalRemoval(BluetoothDeviceList.Entry entry){
        closeDeviceMenu();deviceMenu=new FocusDialogBuilder(activity).setTitle("Удалить из приложения?").setMessage("Удалим метку, номер и привязку к слоту. Пара в настройках Android останется, если она ещё сохранена. Джойстик можно будет найти и подключить заново.").setPositiveButton("Удалить",(d,w)->{clearWaiting(entry.address);pairing.forget(entry.address);controllerNames.forget(entry.address);devices.remove(entry.address);candidates.remove(entry.address);loadSaved(false);refreshRows();status.setText("Метка и привязка удалены. Для нового поиска включите PAIR на M30.");}).setNegativeButton("Отмена",null).show();
    }
    private void deviceActionInSettings(BluetoothDeviceList.Entry entry,String command){
        String instruction="В настройках Bluetooth выберите «"+entry.displayName()+"» ("+entry.address.substring(9)+")";
        if(!command.equals("Системные настройки Bluetooth"))instruction+=" → «"+(command.equals("Подключить устройство")?"Подключить":command)+"»";
        instruction+=".";status.setText(instruction);Toast.makeText(activity,instruction,Toast.LENGTH_LONG).show();openSettings(Settings.ACTION_BLUETOOTH_SETTINGS);
    }
    private void pair(BluetoothDevice device){
        if(pairing.phase()==BluetoothPairingState.PAIRING)return;
        try{
            registerBluetooth();pairing.select(device.getAddress(),SystemClock.elapsedRealtime());
            int state=savedAddresses.contains(device.getAddress())?BluetoothDevice.BOND_BONDED:device.getBondState()==BluetoothDevice.BOND_BONDING?BluetoothDevice.BOND_BONDING:BluetoothDevice.BOND_NONE;
            if(state==BluetoothDevice.BOND_BONDED||state==BluetoothDevice.BOND_BONDING)pairing.poll(device.getAddress(),state);
            else if(!device.createBond()){pairing.fail();status.setText("Android не начал сопряжение. Можно выбрать другой способ подключения.");fallback.setVisibility(View.VISIBLE);FocusNavigation.prepare(fallback);updateControls();return;}
            showPairState();
        }catch(SecurityException error){accessLost();}catch(RuntimeException error){pairing.fail();status.setText("Сопряжение не началось: "+error.getClass().getSimpleName());updateControls();}
    }
    private void reconcilePair(){
        handler.removeCallbacks(pairTimeout);
        if(!resumed||pairing.phase()!=BluetoothPairingState.PAIRING)return;
        if(!canReadSaved()){accessLost();return;}
        if(adapter==null){pairing.fail();status.setText("Bluetooth недоступен. Результат сопряжения неизвестен.");updateControls();return;}
        try{BluetoothDevice selected=adapter.getRemoteDevice(pairing.address());Set<BluetoothDevice> snapshot=BluetoothBondMemory.read(activity,adapter);if(snapshot==null)return;boolean bonded=false;for(BluetoothDevice d:snapshot)if(d.getAddress().equals(pairing.address()))bonded=true;pairing.poll(pairing.address(),bonded?BluetoothDevice.BOND_BONDED:selected.getBondState()==BluetoothDevice.BOND_BONDING?BluetoothDevice.BOND_BONDING:BluetoothDevice.BOND_NONE);if(pairing.phase()==BluetoothPairingState.PAIRED)loadSaved(false);if(pairing.expire(SystemClock.elapsedRealtime())){status.setText("Время ожидания сопряжения истекло. Повторите PAIR.");updateControls();return;}showPairState();}catch(SecurityException error){accessLost();}
    }
    private void showPairState(){
        handler.removeCallbacks(pairTimeout);
        if(pairing.phase()==BluetoothPairingState.PAIRED){status.setText("Пара сохранена. Нажмите кнопку на выбранном геймпаде.");connectionDeadline=SystemClock.elapsedRealtime()+15_000;checkConnection();}
        else if(pairing.phase()==BluetoothPairingState.ERROR){status.setText("Android не завершил сопряжение. Повторите PAIR или выберите другой способ подключения.");if(!waitingAddress.isEmpty()){fallback.setVisibility(View.VISIBLE);FocusNavigation.prepare(fallback);wireFocus();}}
        else if(pairing.phase()==BluetoothPairingState.PAIRING){status.setText("Сопряжение… Подтвердите запрос Android, если он появится.");if(resumed)handler.postDelayed(pairTimeout,Math.max(1,Math.min(1000,pairing.deadline()-SystemClock.elapsedRealtime())));}
        updateControls();
    }
    private void finishScan(String message){
        handler.removeCallbacks(scanTimeout);
        if(ownsDiscovery){ownsDiscovery=false;try{if(adapter!=null)adapter.cancelDiscovery();}catch(SecurityException ignored){}}
        if(message!=null&&status!=null)status.setText(message);if(foundEmpty!=null&&!ownsDiscovery)foundEmpty.setText("Геймпады не найдены. Включите PAIR и повторите поиск.");updateControls();
    }
    private void accessLost(){finishScan(null);pairing.fail();devices.clear();refreshRows();status.setText("Нет доступа к Bluetooth. Разрешите доступ или откройте настройки.");updateControls();}
    private void updateControls(){if(search==null)return;boolean busy=pairing.phase()==BluetoothPairingState.PAIRING;
        search.setEnabled(adapter!=null&&!busy);search.setText(ownsDiscovery?"Остановить поиск":BluetoothCompatibility.external(activity)?"Поиск в Android":"Начать поиск");search.setContentDescription(ownsDiscovery?"Остановить поиск геймпадов":"Начать поиск геймпадов");search.setTextColor(search.isEnabled()?BG:MUTED);search.setBackground(shape(search.isEnabled()?accent:0xff242424));
        if(savedButton!=null)savedButton.setEnabled(adapter!=null&&!busy&&!ownsDiscovery);for(Button row:rows.values())row.setEnabled(!busy);wireFocus();
    }
    private void openSettings(String action){try{activity.startActivity(new Intent(action));}catch(ActivityNotFoundException error){try{activity.startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(ActivityNotFoundException ignored){status.setText("На устройстве нет приложения настроек.");}}}
    void permissionResult(int request){
        if(resumed&&(request==READ_SAVED||request==PERMISSIONS)){controllerNames.stop();controllerNames.start();}
        if(request==READ_SAVED){if(canReadSaved())loadSaved(true);else status.setText("Доступ к сохранённым парам не предоставлен. Доступны системные настройки.");return;}
        if(request!=PERMISSIONS)return;if(hasPermissions())beginSearch();else status.setText("Доступ к поиску не предоставлен. Можно открыть системные настройки Bluetooth; тест кнопок работает без сканирования.");
    }
    void activityResult(int request,int result){activityResult(request,result,null);}
    void activityResult(int request,int result,Intent data){
        if(request==DEVICE_ACTION&&data!=null&&BluetoothAliasActivity.DISCONNECT.equals(data.getStringExtra(BluetoothAliasActivity.OPERATION))){loadSaved(false);status.setText(result==Activity.RESULT_OK?"Запрос отключения передан Android. Состояние обновится после ответа системы.":"Отключение отменено.");}
        if(request==DEVICE_ACTION&&data!=null&&BluetoothAliasActivity.CONNECT.equals(data.getStringExtra(BluetoothAliasActivity.OPERATION))&&waitingAddress.equals(data.getStringExtra("address"))){
            connectionActionPending=false;
            if(result==Activity.RESULT_OK){connectionDeadline=SystemClock.elapsedRealtime()+15_000;checkConnection();}
            else{waitingAddress="";fallback.setVisibility(View.GONE);if(cancelConnection!=null)cancelConnection.run();status.setText("Подключение отменено. Выберите устройство или начните поиск.");}
        }
if(request==ENABLE){if(result==Activity.RESULT_OK)beginSearch();else status.setText("Bluetooth не включён. Повторите поиск, чтобы попробовать снова.");}}
    void resume(){resumed=true;controllerNames.start();if(canReadSaved()&&pairing.phase()!=BluetoothPairingState.PAIRING)loadSaved(false);if(pairing.phase()==BluetoothPairingState.PAIRING){if(!canReadSaved()){accessLost();return;}try{registerBluetooth();reconcilePair();}catch(SecurityException error){accessLost();}}checkConnection();}
    void pause(){resumed=false;if(scanDialog!=null){scanDialog.dismiss();scanDialog=null;}handler.removeCallbacks(connectionTimeout);controllerNames.stop();closeDeviceMenu();closeLabelEditor();boolean active=ownsDiscovery;finishScan(active?"Поиск остановлен при уходе с экрана.":null);handler.removeCallbacks(pairTimeout);if(registered){activity.unregisterReceiver(receiver);registered=false;}}
    void destroy(){pause();handler.removeCallbacksAndMessages(null);}
    Bundle save(){Bundle out=new Bundle();out.putBoolean("connection_action_pending",connectionActionPending);out.putString("connection_address",waitingAddress);out.putLong("connection_deadline",connectionDeadline);out.putString("address",pairing.address());out.putInt("phase",pairing.phase());out.putLong("deadline",pairing.deadline());out.putBoolean("sawBonding",pairing.sawBonding());return out;}
}
