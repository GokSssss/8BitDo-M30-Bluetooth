package app.m30.companion;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.BatteryState;
import android.hardware.input.InputManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity implements InputManager.InputDeviceListener {
    private static final int BG=0xff000000, CARD=0xff181818, TEXT=0xfff6f3e8, MUTED=0xffbbb7a8, YELLOW=0xffb6a45e, ORANGE=0xffb66f45, GREEN=0xffdaca65;
    private boolean discoveringSource;
    private boolean bootPermissionReceiverRegistered,bootQueryRunning;
    private String pendingBootName;
    private final String bootPermissionAction="app.m30.companion.USB_BOOT_MODEL";
    private final BroadcastReceiver bootPermissionReceiver=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){
        if(!bootPermissionAction.equals(intent.getAction())||pendingBootName==null)return;
        String name=pendingBootName;pendingBootName=null;
        android.hardware.usb.UsbManager manager=(android.hardware.usb.UsbManager)getSystemService(USB_SERVICE);
        android.hardware.usb.UsbDevice current=manager.getDeviceList().get(name);
        if(!UsbBootModel.supported(current)||!manager.hasPermission(current)){toast("Доступ к загрузчику не предоставлен или устройство отключено");return;}
        runBootQuery(manager,current);
    }};

    private boolean navigationLocked=true;
    private boolean autoFollowControllers=true;
    private int pendingSourceId=-1;
    private TextView sourceHintView;
    private Button useSourceButton,firmwareEntry,bootAccessButton;
    private TextView firmwareCableHint;
    private long lastUsbStateCheck;
    private int usbConnectionState;

    private final Map<Integer,String> otherMotion=new HashMap<>();
    private final InputTestState inputTest=new InputTestState();
    private TextView inputSourceView;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private InputManager inputs;
    private InputDevice device;
    private LinearLayout root, content, tabs,buttonGrid,pageBody,singleBluetoothHost;
    private JoinedTabFrame joinedFrame;
    private ConnectionCardView connectionCard;
    private final Map<String,TextView> buttonCells=new LinkedHashMap<>();
    private ScrollView contentScroll;
    private boolean wholePageScroll,wideControllers;
    private ControllerPanelView[] widePanels;
    private LinearLayout wideOverlay;
    private TextView status, details, live, progress, logView, rawView, batteryView, bindHint;
    private TextView lastCodeView,lastNameView,lastDetailsView,otherLastView,otherLogView,modeResultView;
    private PadView pad;
    private BatteryIndicator headerBattery;
    private ImageView headerBluetooth;
    private Button headerDpadHelp,headerLedHelp;
    private TextView headerPercent,headerConnection,headerVersions,headerInputLine,headerInputAction;
    private String appVersion="—";
    private int page=0,activeSlot;
    private ControllerBank controllers;
    private ControllerSession session;
    private boolean registered,touchSwiping,touchVertical,mainResumed,inlineBluetoothOpen,inlineTouch;
    private BluetoothSearchController inlineBluetooth;
    private BluetoothControllerNames bluetoothNames;
    private final ControllerConnectionRequest connectionRequest=new ControllerConnectionRequest();
    private Bundle inlineSaved;
    private LinearLayout inputSummary,topBase;
    private FrameLayout topRegion;
    private View bluetoothButton,bluetoothPlaceholder;
    private int inlineHeight,inlineReturnScroll=-1;
    private int pagingKey=-1,pagingDevice;
    private boolean pagingRecorded;
    private Dialog diagnosticsDialog,dpadDialog,optionsDialog,guideDialog;
    private TextView diagnosticsDpadView,dpadDialogStatus;
    private float touchStartX,touchStartY;
    private final int[] pageScroll={0,0,0};
    private final String[] modeNames=ConnectionGuide.names();
    private final String[] modeKeys=ConnectionGuide.keys();
    private final Runnable ticker=new Runnable(){ public void run(){ refreshLive(); handler.postDelayed(this,1000); }};

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        bluetoothNames=new BluetoothControllerNames(this,this::refreshLive);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        try{String version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;if(version!=null&&!version.isEmpty())appVersion=version;}catch(PackageManager.NameNotFoundException ignored){}
        controllers=new ControllerBank(getPreferences(0).getInt("mode",0));
        autoFollowControllers=getPreferences(0).getBoolean("follow_controller_input",true);
        controllers.restore(0,getPreferences(0).getString("controller.0",getPreferences(0).getString("device","")));
        controllers.restore(1,getPreferences(0).getString("controller.1",""));
        activeSlot=controllers.dual()?getPreferences(0).getInt("active_controller",0):0;
        if(saved!=null&&saved.getBoolean("inline_open"))activeSlot=saved.getInt("inline_owner",activeSlot);
        if(activeSlot<0||activeSlot>1)activeSlot=0;
        session=controllers.slots[activeSlot];page=activeSlot==0?0:2;
        if(saved!=null){inlineBluetoothOpen=saved.getBoolean("inline_open");inlineSaved=saved.getBundle("inline_state");inlineReturnScroll=saved.getInt("inline_return_scroll",-1);}
        if(saved!=null&&saved.getInt("connect_slot",-1)>=0){connectionRequest.begin(saved.getString("connect_address"),saved.getInt("connect_slot"),SystemClock.elapsedRealtime());connectionRequest.deadline=saved.getLong("connect_deadline");}
        if(inlineSaved!=null&&inlineSaved.getBoolean("connection_action_pending")&&connectionRequest.slot>=0)connectionRequest.deadline=SystemClock.elapsedRealtime()+120_000;
        updateTaskIcon();
        inputs=(InputManager)getSystemService(INPUT_SERVICE);
        chooseDevice();alignModeOnOpen();render();if(inlineBluetoothOpen)revealInlineBluetooth();
    }
    @SuppressWarnings("deprecation")
    private void updateTaskIcon(){
        // Send fresh pixels rather than a resource ID that an OEM task list may cache.
        android.graphics.drawable.Drawable icon=getDrawable(R.mipmap.ic_m30_launcher);
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(192,192,android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);icon.setBounds(0,0,192,192);icon.draw(canvas);
        setTaskDescription(new ActivityManager.TaskDescription(getApplicationInfo().loadLabel(getPackageManager()).toString(),bitmap,BG));
    }
    private int slotAccent(){return activeSlot==1?ORANGE:YELLOW;}
    private int headerAccent(){return page==1?0xffbfbfbf:slotAccent();}
    private int accent(){return page==1||activeSlot==1&&!session.connected()?0xffbfbfbf:slotAccent();}
    private android.content.Context dialogContext(){return new android.view.ContextThemeWrapper(this,page==1?R.style.M30GrayDialogTheme:activeSlot==1?R.style.M30OrangeDialogTheme:R.style.M30DialogTheme);}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(InstructionText.format(value));v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(4),0,dp(4));return v;}
    private TextView title(String value,int size){TextView v=text(value,size,TEXT);v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return v;}
    private void gap(LinearLayout box,int h){Space s=new Space(this);box.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    private Button button(String label,Runnable action,boolean primary){
        Button b=new Button(this);b.setText(InstructionText.format(label));b.setTextSize(14);b.setAllCaps(false);b.setTextColor(primary?BG:TEXT);
        b.setBackground(shape(primary?accent():0xff2d2b22,14));b.setMinHeight(dp(48));b.setPadding(dp(12),dp(8),dp(12),dp(8));
        b.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(48));lp.topMargin=dp(6);b.setLayoutParams(lp);return b;
    }
    private LinearLayout card(String heading){return card(heading,column());}
    private LinearLayout card(String heading,LinearLayout c){c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(shape(CARD,22));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);content.addView(c,lp);if(heading!=null){TextView label=title(heading,18);int slotName=heading.indexOf("Джойстик 2");if(slotName>=0){android.text.SpannableString caption=new android.text.SpannableString(label.getText());caption.setSpan(new android.text.style.ForegroundColorSpan(ORANGE),slotName,slotName+"Джойстик 2".length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);label.setText(caption);}c.addView(label);}return c;}
    private void render(){
        int previousScroll=contentScroll==null?0:contentScroll.getScrollY();String focusedKey=FocusNavigation.key(getCurrentFocus());
        if(inlineBluetooth!=null&&inlineBluetooth.view().getParent() instanceof ViewGroup)((ViewGroup)inlineBluetooth.view().getParent()).removeView(inlineBluetooth.view());
        firmwareEntry=null;firmwareCableHint=null;lastUsbStateCheck=0;buttonGrid=null;buttonCells.clear();status=null;details=null;live=null;progress=null;batteryView=null;bindHint=null;pad=null;headerBattery=null;headerBluetooth=null;headerPercent=null;headerConnection=null;headerVersions=null;headerInputLine=null;headerInputAction=null;headerDpadHelp=null;headerLedHelp=null;lastCodeView=null;lastNameView=null;lastDetailsView=null;modeResultView=null;sourceHintView=null;useSourceButton=null;inputSourceView=null;connectionCard=null;singleBluetoothHost=null;bluetoothPlaceholder=null;widePanels=null;wideOverlay=null;
        wideControllers=wideControllerLayout();wholePageScroll=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        root=column();root.setBackgroundColor(BG);root.setPadding(dp(12),0,dp(12),0);
        if(wholePageScroll){ScrollView scroller=new ScrollView(this);scroller.setBackgroundColor(BG);scroller.setFillViewport(true);scroller.setSmoothScrollingEnabled(false);scroller.setTag(R.id.navigation_key,"page:scroll-all");scroller.addView(root,new ScrollView.LayoutParams(-1,-2));contentScroll=scroller;setContentView(scroller);if(Build.VERSION.SDK_INT>=30)scroller.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);return insets;});}
        else{setContentView(root);if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(dp(12)+i.left,i.top,dp(12)+i.right,i.bottom);return insets;});}
        tabs=new LinearLayout(this);tabs.setBaselineAligned(false);root.addView(tabs,new LinearLayout.LayoutParams(-1,-2));int[] order=SwipeNavigation.order(true);String[] names={"Джойстик 1","Джойстик 2","Тест кнопок"};View selectedTab=null;
        for(int n=0;n<order.length;n++){final int target=order[n];boolean selected=page==target;int color=selected?(target==0?YELLOW:target==2?ORANGE:TEXT):MUTED;TextView tab=text(names[n],14,color);tab.setIncludeFontPadding(false);tab.setGravity(Gravity.CENTER);tab.setPadding(dp(3),dp(6),dp(3),dp(6));tab.setMaxLines(2);tab.setEllipsize(android.text.TextUtils.TruncateAt.END);tab.setMinimumHeight(dp(52));tab.setBackground(JoinedTabFrame.tab(this,selected,target==0?YELLOW:target==2?ORANGE:TEXT));tab.setSelected(selected);tab.setFocusable(true);tab.setTag(R.id.navigation_key,"tab:"+target);tab.setOnClickListener(v->switchPage(target));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(n>0)lp.leftMargin=dp(3);tabs.addView(tab,lp);if(selected)selectedTab=tab;}
        joinedFrame=new JoinedTabFrame(this);joinedFrame.select(selectedTab,headerAccent());root.addView(joinedFrame,new LinearLayout.LayoutParams(-1,wholePageScroll?-2:0,wholePageScroll?0:1));root.setContentDescription(RemoteNavigation.pageCaption(page));pageBody=column();pageBody.setMinimumHeight(Math.max(dp(140),guideViewportHeight()-dp(70)));
        if(wholePageScroll)joinedFrame.addView(pageBody,new FrameLayout.LayoutParams(-1,-2));else{ScrollView scroller=new ScrollView(this);scroller.setFillViewport(true);scroller.setSmoothScrollingEnabled(false);scroller.setTag(R.id.navigation_key,"page:scroll-content");joinedFrame.addView(scroller,new FrameLayout.LayoutParams(-1,-1));scroller.addView(pageBody,new ScrollView.LayoutParams(-1,-2));contentScroll=scroller;}
        if(wideControllers&&page!=1){renderWideControllers(focusedKey,previousScroll);return;}
        topRegion=new FrameLayout(this);pageBody.addView(topRegion,new LinearLayout.LayoutParams(-1,-2));topBase=column();topRegion.addView(topBase,new FrameLayout.LayoutParams(-1,-2));
        if(page!=1){
            LinearLayout hero=new LinearLayout(this);hero.setGravity(Gravity.TOP);topBase.addView(hero,new LinearLayout.LayoutParams(-1,-2));LinearLayout rail=column();
            LinearLayout link=new LinearLayout(this);link.setGravity(Gravity.CENTER_VERTICAL);link.setBackground(shape(CARD,9));link.setPadding(dp(12),0,dp(12),0);link.setTag(R.id.navigation_key,"bluetooth:toggle");link.setContentDescription("Подключение M30. Открыть Bluetooth внизу страницы");link.setOnClickListener(v->toggleInlineBluetooth());bluetoothButton=link;
            headerBluetooth=new ImageView(this);link.addView(headerBluetooth,new LinearLayout.LayoutParams(dp(16),dp(18)));headerConnection=text("Подключение",12,headerAccent());headerConnection.setPadding(dp(6),0,0,0);headerConnection.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));headerConnection.setLetterSpacing(0);headerConnection.setIncludeFontPadding(false);headerConnection.setSingleLine(true);link.addView(headerConnection);
            int controlHeight=Math.max(dp(44),headerConnection.getLineHeight()+dp(12)),controlWidth=Math.max(dp(128),(int)Math.ceil(headerConnection.getPaint().measureText("Подключение"))+dp(54));hero.addView(rail,new LinearLayout.LayoutParams(controlWidth,-2));link.setMinimumHeight(controlHeight);rail.addView(link,new LinearLayout.LayoutParams(-1,controlHeight));
            gap(rail,3);rail.addView(headerAction("Выбрать","header:choose",this::devicePicker,controlWidth,controlHeight,R.drawable.ic_gamepad));gap(rail,3);rail.addView(headerAction("Опции","header:options",this::showOptions,controlWidth,controlHeight,R.drawable.ic_options_outline));gap(rail,3);rail.addView(headerAction("Клавиши","header:keys",this::showButtonGuide,controlWidth,controlHeight,R.drawable.ic_info_outline));
            int railHeight=controlHeight*4+dp(9),heroHeight=wholePageScroll?Math.max(railHeight,guideViewportHeight()-dp(128)):railHeight;
            LinearLayout drawing=column();LinearLayout.LayoutParams drawingLp=new LinearLayout.LayoutParams(0,heroHeight,1);drawingLp.leftMargin=dp(6);hero.addView(drawing,drawingLp);FrameLayout diagram=new FrameLayout(this);drawing.addView(diagram,new LinearLayout.LayoutParams(-1,0,1));pad=new PadView(this,session.state,null);pad.setTag(R.id.navigation_key,"header:pad");pad.setDisplayStyle(activeSlot,false);pad.hideDpadCaption(true);diagram.addView(pad,new FrameLayout.LayoutParams(-1,-1));
            LinearLayout charge=new LinearLayout(this);charge.setGravity(Gravity.CENTER_VERTICAL);charge.setTag(R.id.navigation_key,"header:battery");charge.setContentDescription("Заряд M30");charge.setOnClickListener(v->showBatteryInfo());headerBattery=new BatteryIndicator(this);headerBattery.setAccent(headerAccent());charge.addView(headerBattery,new LinearLayout.LayoutParams(dp(18),dp(12)));headerPercent=text("—",11,headerAccent());headerPercent.setPadding(dp(4),0,0,0);charge.addView(headerPercent);diagram.addView(charge,new FrameLayout.LayoutParams(-2,dp(20),Gravity.TOP|Gravity.CENTER_HORIZONTAL));
            headerVersions=text("Версия "+appVersion+" · прошивка —",10,MUTED);headerVersions.setGravity(Gravity.CENTER);headerVersions.setPadding(0,0,0,0);headerVersions.setSingleLine(true);drawing.addView(headerVersions,new LinearLayout.LayoutParams(-1,dp(16)));
            LinearLayout helpRow=new LinearLayout(this);helpRow.setBaselineAligned(false);LinearLayout.LayoutParams helpLp=new LinearLayout.LayoutParams(-1,controlHeight);helpLp.topMargin=dp(2);drawing.addView(helpRow,helpLp);headerDpadHelp=headerInfo("Крестовина","header:dpad-info",this::showDpadMenu);headerLedHelp=headerInfo("Огоньки","header:led-info",this::showLedGuide);helpRow.addView(headerDpadHelp,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams ledLp=new LinearLayout.LayoutParams(0,-2,1);ledLp.leftMargin=dp(4);helpRow.addView(headerLedHelp,ledLp);
        }
        inputSummary=column();inputSummary.setBackground(shape(CARD,9));inputSummary.setPadding(dp(8),dp(4),dp(8),dp(4));inputSummary.setMinimumHeight(dp(44));LinearLayout.LayoutParams summaryLp=new LinearLayout.LayoutParams(-1,-2);summaryLp.setMargins(0,dp(4),0,dp(5));topBase.addView(inputSummary,summaryLp);
        headerInputLine=text("",13,TEXT);headerInputLine.setIncludeFontPadding(false);headerInputLine.setTypeface(Typeface.MONOSPACE);headerInputLine.setSingleLine(true);headerInputLine.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);headerInputLine.setPadding(0,dp(1),0,dp(1));inputSummary.addView(headerInputLine,new LinearLayout.LayoutParams(-1,-2));headerInputAction=text("",13,accent());headerInputAction.setIncludeFontPadding(false);headerInputAction.setSingleLine(true);headerInputAction.setEllipsize(android.text.TextUtils.TruncateAt.END);headerInputAction.setPadding(0,dp(1),0,dp(1));inputSummary.addView(headerInputAction,new LinearLayout.LayoutParams(-1,-2));
        content=column();pageBody.addView(content,new LinearLayout.LayoutParams(-1,-2));if(page!=1)connectionPage();else testPage();refreshLive();FocusNavigation.prepare(root);disableModeLabelFocus(root);wireRemoteFocus();
        if(inlineBluetoothOpen&&page!=1)showInlineBluetooth();View restored=FocusNavigation.find(root,focusedKey);boolean tv=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_TYPE_MASK)==android.content.res.Configuration.UI_MODE_TYPE_TELEVISION;if(restored==null&&tv)restored=FocusNavigation.find(root,"tab:"+page);if(restored!=null&&restored.isShown())restored.requestFocus();restoreScrollBeforeDraw(contentScroll,previousScroll);
    }
    private boolean wideControllerLayout(){
        android.content.res.Configuration c=getResources().getConfiguration();
        return c.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE&&(c.smallestScreenWidthDp>=600||(c.uiMode&android.content.res.Configuration.UI_MODE_TYPE_MASK)==android.content.res.Configuration.UI_MODE_TYPE_TELEVISION);
    }
    private void renderWideControllers(String focusedKey,int previousScroll){
        topRegion=null;topBase=null;bluetoothButton=null;bluetoothPlaceholder=null;
        LinearLayout pair=new LinearLayout(this);pair.setBaselineAligned(false);pair.setTag(R.id.navigation_key,"wide:pair");pageBody.addView(pair,new LinearLayout.LayoutParams(-1,-2));content=pair;
        widePanels=new ControllerPanelView[2];
        for(int slot=0;slot<2;slot++){widePanels[slot]=new ControllerPanelView(this,slot,controllers.slots[slot],this::wideAction);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(slot>0)lp.leftMargin=dp(12);pair.addView(widePanels[slot],lp);}
        refreshLive();FocusNavigation.prepare(root);wireRemoteFocus();
        if(inlineBluetoothOpen){ControllerPanelView expected=widePanels[0];root.post(()->{if(widePanels!=null&&widePanels[0]==expected)showInlineBluetooth();});}
        boolean tv=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_TYPE_MASK)==android.content.res.Configuration.UI_MODE_TYPE_TELEVISION;
        View restored=FocusNavigation.find(root,focusedKey);if(restored==null&&tv)restored=FocusNavigation.find(root,"tab:"+page);if(restored!=null&&restored.isShown())restored.requestFocus();
        restoreScrollBeforeDraw(contentScroll,previousScroll);
    }
    private void wideAction(int slot,String action){
        int target=slot==0?0:2;if(page!=target)switchPage(target,false);
        if(action.equals("bluetooth"))toggleInlineBluetooth();else if(action.equals("choose"))devicePicker();else if(action.equals("options"))showOptions();else if(action.equals("keys"))showButtonGuide();else if(action.equals("battery"))showBatteryInfo();else if(action.equals("dpad"))showDpadMenu();else if(action.equals("led"))showLedGuide();else if(action.startsWith("mode:"))selectMode(Integer.parseInt(action.substring(5)));
    }
    private void refreshWideControllers(){
        if(widePanels==null)return;
        for(int slot=0;slot<2;slot++){ControllerSession observed=controllers.slots[slot];ArrayList<ControllerTelemetry.Source> sources=telemetrySources(observed,true);ControllerTelemetry.Source battery=ControllerTelemetry.battery(observed.deviceId,sources);widePanels[slot].refresh(bluetoothNames.slotName(slot),battery==null?-1:battery.capacity,ControllerTelemetry.mode(observed.deviceId,sources),appVersion,slot==activeSlot);}
    }
    private ImageButton connectionClose(String key){
        ImageButton close=new ImageButton(this);close.setImageResource(R.drawable.ic_close_outline);close.setImageTintList(android.content.res.ColorStateList.valueOf(slotAccent()));close.setScaleType(ImageView.ScaleType.CENTER_INSIDE);close.setPadding(dp(12),dp(12),dp(12),dp(12));close.setBackground(shape(CARD,9));close.setContentDescription("Закрыть подключение");close.setTooltipText("Закрыть");close.setTag(R.id.navigation_key,key);close.setOnClickListener(v->closeInlineBluetooth());close.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));return close;
    }
    private void showWideBluetooth(){
        if(widePanels==null)return;ControllerPanelView opposite=widePanels[1-activeSlot];
        if(inlineBluetooth==null){inlineBluetooth=new BluetoothSearchController(this,inlineSaved,true,slotAccent(),()->{closeInlineBluetooth();switchPage(1);},activeSlot,this::requestBluetoothController,connectionRequest::clear);inlineSaved=null;if(mainResumed)inlineBluetooth.resume();}
        inlineBluetooth.reserveHeader(0,0);
        if(wideOverlay==null){wideOverlay=column();wideOverlay.setPadding(dp(8),dp(8),dp(8),dp(8));wideOverlay.setBackgroundColor(BG);wideOverlay.setTag(R.id.navigation_key,"wide:connection");
            LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);wideOverlay.addView(titleRow,new LinearLayout.LayoutParams(-1,-2));TextView label=title("Подключение · Джойстик "+(activeSlot+1),14);titleRow.addView(label,new LinearLayout.LayoutParams(0,-2,1));ImageButton close=connectionClose("wide:connection:close");titleRow.addView(close);
            if(inlineBluetooth.view().getParent() instanceof ViewGroup)((ViewGroup)inlineBluetooth.view().getParent()).removeView(inlineBluetooth.view());wideOverlay.addView(inlineBluetooth.view(),new LinearLayout.LayoutParams(-1,0,1));opposite.addView(wideOverlay,new FrameLayout.LayoutParams(-1,-1));}
        opposite.body.setVisibility(View.INVISIBLE);wideOverlay.bringToFront();FocusNavigation.prepare(wideOverlay);wireRemoteFocus();
    }
    private void restoreScrollBeforeDraw(ScrollView scroll,int y){
        scroll.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){
            @Override public boolean onPreDraw(){
                if(scroll.getViewTreeObserver().isAlive())scroll.getViewTreeObserver().removeOnPreDrawListener(this);
                if(contentScroll==scroll)scroll.scrollTo(0,y);
                return true;
            }
        });
    }
    private void disableModeLabelFocus(View view){
        if(FocusNavigation.key(view)!=null&&FocusNavigation.key(view).startsWith("mode:")&&view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)group.getChildAt(i).setFocusable(false);return;}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)disableModeLabelFocus(group.getChildAt(i));}
    }
    private void wireRemoteFocus(){
        if(widePanels!=null){
            for(int slot=0;slot<2;slot++){View tab=FocusNavigation.find(root,"tab:"+(slot==0?0:2));widePanels[slot].routes(tab);FocusNavigation.route(tab,View.FOCUS_DOWN,inlineBluetoothOpen&&inlineBluetooth!=null&&slot!=activeSlot?inlineBluetooth.firstAction():widePanels[slot].connect);}
            if(inlineBluetoothOpen&&inlineBluetooth!=null){inlineBluetooth.boundaries(widePanels[activeSlot].connect,FocusNavigation.find(root,"wide:connection:close"));FocusNavigation.route(widePanels[activeSlot].connect,activeSlot==0?View.FOCUS_RIGHT:View.FOCUS_LEFT,inlineBluetooth.firstAction());}
            return;
        }
        View bluetooth=FocusNavigation.find(root,"bluetooth:toggle"),battery=FocusNavigation.find(root,"header:battery"),choose=FocusNavigation.find(root,"header:choose"),options=FocusNavigation.find(root,"header:options"),keys=FocusNavigation.find(root,"header:keys"),currentTab=FocusNavigation.find(root,"tab:"+page),dpadInfo=FocusNavigation.find(root,"header:dpad-info"),ledInfo=FocusNavigation.find(root,"header:led-info");View[] rail={bluetooth,choose,options,keys};
        for(int n=0;n<rail.length;n++){FocusNavigation.route(rail[n],View.FOCUS_UP,n==0?currentTab:rail[n-1]);FocusNavigation.route(rail[n],View.FOCUS_DOWN,n==3?dpadInfo:rail[n+1]);FocusNavigation.route(rail[n],View.FOCUS_RIGHT,n==0?battery:dpadInfo);}
        FocusNavigation.route(battery,View.FOCUS_DOWN,dpadInfo);FocusNavigation.route(battery,View.FOCUS_UP,currentTab);FocusNavigation.route(battery,View.FOCUS_LEFT,bluetooth);FocusNavigation.route(dpadInfo,View.FOCUS_LEFT,keys);FocusNavigation.route(dpadInfo,View.FOCUS_RIGHT,ledInfo);FocusNavigation.route(ledInfo,View.FOCUS_LEFT,dpadInfo);FocusNavigation.route(dpadInfo,View.FOCUS_UP,battery);FocusNavigation.route(ledInfo,View.FOCUS_UP,battery);FocusNavigation.route(dpadInfo,View.FOCUS_DOWN,FocusNavigation.find(root,"mode:0"));FocusNavigation.route(ledInfo,View.FOCUS_DOWN,FocusNavigation.find(root,"mode:2"));for(int tab:new int[]{0,2,1})FocusNavigation.route(FocusNavigation.find(root,"tab:"+tab),View.FOCUS_DOWN,bluetooth);
        View previous=null;for(int mode:new int[]{0,1,3,2}){View choice=FocusNavigation.find(root,"mode:"+mode);FocusNavigation.route(choice,View.FOCUS_UP,mode==0||mode==1?dpadInfo:ledInfo);if(previous!=null){FocusNavigation.route(previous,View.FOCUS_RIGHT,choice);FocusNavigation.route(choice,View.FOCUS_LEFT,previous);}if(inlineBluetoothOpen&&inlineBluetooth!=null)FocusNavigation.route(choice,View.FOCUS_DOWN,inlineBluetooth.firstAction());previous=choice;}
        if(inlineBluetoothOpen&&inlineBluetooth!=null)inlineBluetooth.boundaries(FocusNavigation.find(root,"mode:0"),FocusNavigation.find(root,"single:connection:close"));
    }
    private int connectionPageId(){return activeSlot==0?0:2;}
    private InputDevice controllerDevice(ControllerSession target){return target.connected()?InputDevice.getDevice(target.deviceId):null;}
    private void selectController(int index){
        if(index!=activeSlot)closeInlineBluetooth();
        activeSlot=index;session=controllers.slots[index];device=controllerDevice(session);
        getPreferences(0).edit().putInt("active_controller",index).apply();
    }
    private void switchPage(int target){switchPage(target,true);}
    private void switchPage(int target,boolean alignProfile){
        if(target==page)return;if(contentScroll!=null)pageScroll[page]=contentScroll.getScrollY();closeInlineBluetooth();
        closeDpadMenu();closeDiagnostics();if(session.monitor.otherEnabled){session.monitor.setOtherEnabled(false);otherMotion.clear();}
        session.waiting=null;inputTest.release();
        if(target!=1){int next=target==2?1:0;if(next!=activeSlot){discoveringSource=false;pendingSourceId=-1;}selectController(next);if(alignProfile)alignModeOnOpen();}
        page=target;render();int y=wholePageScroll&&page!=1?0:pageScroll[page];restoreScrollBeforeDraw(contentScroll,y);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN)inlineTouch=false;
        if(action==MotionEvent.ACTION_DOWN&&inlineBluetoothOpen&&topRegion!=null){int[] xy=new int[2];topRegion.getLocationOnScreen(xy);inlineTouch=e.getRawX()>=xy[0]&&e.getRawX()<=xy[0]+topRegion.getWidth()&&e.getRawY()>=xy[1]&&e.getRawY()<=xy[1]+topRegion.getHeight();}
        if(inlineTouch){boolean result=super.dispatchTouchEvent(e);if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)inlineTouch=false;return result;}
        float dx=e.getX()-touchStartX,dy=e.getY()-touchStartY;
        if(action==MotionEvent.ACTION_DOWN){touchStartX=e.getX();touchStartY=e.getY();touchSwiping=false;touchVertical=false;}
        if(action==MotionEvent.ACTION_POINTER_DOWN)touchVertical=true;
        if(action==MotionEvent.ACTION_MOVE&&!touchSwiping&&!touchVertical){
            if(Math.abs(dy)>ViewConfiguration.get(this).getScaledTouchSlop()&&Math.abs(dy)>Math.abs(dx))touchVertical=true;
            else if(SwipeNavigation.horizontal(dx,dy,dp(40))){touchSwiping=true;MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();}
        }
        if(touchSwiping){if(action==MotionEvent.ACTION_UP){int next=SwipeNavigation.target(page,dx,dy,dp(40),true);touchSwiping=false;switchPage(next);}else if(action==MotionEvent.ACTION_CANCEL)touchSwiping=false;return true;}
        return super.dispatchTouchEvent(e);
    }
    private void connectionPage(){
        connectionCard=new ConnectionCardView(this,session.mode,slotAccent(),"",mode->{if(session.mode!=mode)selectMode(mode);},null);content.addView(connectionCard,new LinearLayout.LayoutParams(-1,-2));bindHint=text("",13,accent());bindHint.setPadding(0,0,0,0);content.addView(bindHint);
    }
    private Button headerAction(String label,String key,Runnable action,int width,int height){return headerAction(label,key,action,width,height,0);}
    private Button headerAction(String label,String key,Runnable action,int width,int height,int image){ActionButton b=new ActionButton(this,label,headerAccent());b.setTextSize(12);b.setTag(R.id.navigation_key,key);b.setOnClickListener(v->action.run());b.setLayoutParams(new LinearLayout.LayoutParams(width,Math.max(dp(44),height)));if(image!=0)b.icon(image,headerAccent());return b;}
    private Button headerInfo(String label,String key,Runnable action){ActionButton b=new ActionButton(this,label,headerAccent());b.info();b.icon(R.drawable.ic_info_outline,headerAccent());b.setTag(R.id.navigation_key,key);b.setContentDescription(key.endsWith("dpad-info")?"Как переключить крестовину, левый или правый стик":"Режимы подключения и индикаторы M30");b.setOnClickListener(v->action.run());return b;}

    private ButtonGuideCard buttonGuideContent(){
        ButtonGuideCard c=new ButtonGuideCard(this,wholePageScroll?Math.max(1,guideViewportHeight()-dp(120)):0);c.setPadding(dp(14),dp(12),dp(14),dp(12));
        TextView heading=title("Кнопки в Android",18);heading.setGravity(Gravity.CENTER);c.addView(heading,new LinearLayout.LayoutParams(-1,-2));
        TextView profile=text("По профилю: "+modeNames[session.mode],13,accent());profile.setGravity(Gravity.CENTER);c.addView(profile,new LinearLayout.LayoutParams(-1,-2));
        c.addView(new ButtonGuideView(this,session.state.bindings,session.mode,activeSlot),new LinearLayout.LayoutParams(-1,-2));
        TextView note=text("Статичная справка · назначения по профилю",12,MUTED);note.setGravity(Gravity.CENTER);c.addView(note,new LinearLayout.LayoutParams(-1,-2));
        TextView system=text("Системное действие последнего ввода: "+lastInputAction()+".\nДействия зависят от режима M30 и приложения. HOME может обрабатываться Android до передачи события приложению.",14,TEXT);c.addView(system,new LinearLayout.LayoutParams(-1,-2));
        return c;
    }
    private void showButtonGuide(){
        if(guideDialog!=null)return;ScrollView scroll=new ScrollView(this);ButtonGuideCard card=buttonGuideContent();scroll.addView(card,new ScrollView.LayoutParams(-1,-2));card.setGravity(Gravity.CENTER_HORIZONTAL);
        AlertDialog dialog=new FocusDialogBuilder(dialogContext()).setTitle("Клавиши").setView(scroll).setNegativeButton("Закрыть",null).create();guideDialog=dialog;
        dialog.setOnDismissListener(d->{if(guideDialog==dialog)guideDialog=null;});dialog.show();FocusNavigation.prepare(dialog.getWindow().getDecorView());
    }
    private void showOptions(){
        if(optionsDialog!=null)return;LinearLayout body=column();body.setPadding(dp(16),0,dp(16),dp(12));ScrollView scroll=new ScrollView(this);scroll.addView(body);
        AlertDialog dialog=new FocusDialogBuilder(dialogContext()).setTitle("Опции").setView(scroll).setNegativeButton("Закрыть",null).create();optionsDialog=dialog;
        String[] items={"Исправить подсветку кнопок","Сбросить проверку","Выбрать M30","Найти M30 по нажатию","Диагностика","Справка M30","Инструкция выбранного режима","Поиск геймпадов Bluetooth","Автопереход по нажатию: "+(autoFollowControllers?"включён":"выключен")};
        for(int n=0;n<items.length;n++){final int action=n;Button item=button(items[n],()->{dialog.dismiss();runOption(action);},false);item.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);item.setBackground(shape(CARD,8));item.setTag(R.id.navigation_key,"options:"+n);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(2);item.setMinHeight(dp(44));item.setPadding(dp(12),dp(6),dp(12),dp(6));body.addView(item,lp);}firmwareSection(body);
        dialog.setOnDismissListener(d->{if(optionsDialog!=dialog)return;optionsDialog=null;firmwareEntry=null;firmwareCableHint=null;});dialog.show();FocusNavigation.prepare(dialog.getWindow().getDecorView());lastUsbStateCheck=0;refreshFirmwareEntry();
    }
    private int guideViewportHeight(){
        if(Build.VERSION.SDK_INT>=30){
            android.view.WindowMetrics metrics=getWindowManager().getCurrentWindowMetrics();
            android.graphics.Insets bars=metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            return metrics.getBounds().height()-bars.top-bars.bottom;
        }
        android.graphics.Rect visible=new android.graphics.Rect();getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);
        return visible.height()>0?visible.height():getResources().getDisplayMetrics().heightPixels;
    }
    private void showLedGuide(){
        LinearLayout body=column();body.setPadding(dp(20),dp(8),dp(20),dp(12));
        body.addView(text("Четыре нижних индикатора считаются слева направо. Смотрите, когда они мигают: при включении, во время PAIR или после подключения.",14,TEXT));
        body.addView(title("Выбор режима · M30 выключен",16));
        body.addView(text("Сначала START · 3 с — выключить. Затем удерживайте нужное сочетание до включения и отпустите:",14,TEXT));
        for(int mode:new int[]{0,1,3,2}){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);body.addView(row);
            ModeLedsView leds=new ModeLedsView(this,mode);leds.setAccent(accent());row.addView(leds,new LinearLayout.LayoutParams(dp(58),dp(32)));
            TextView label=text(modeNames[mode]+" · "+modeKeys[mode]+"\n"+ConnectionGuide.ledDescription(mode),13,TEXT);label.setLineSpacing(dp(2),1);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        }
        String[][] sections={
            {"Подключение · M30 включён","Для новой пары: держите PAIR 2 с, затем выберите M30 в Bluetooth. Бегущие нижние огоньки — поиск пары. После подключения постоянный нижний огонёк — статус связи, а не смена режима. В Switch нижние огоньки также показывают номер игрока."},
            {"Крестовина · M30 включён","Держите одновременно 5 с:\nSELECT + ↑ — D-pad (обычная крестовина).\nSELECT + ← — левый стик.\nSELECT + → — правый стик.\nВерхний индикатор рядом с PAIR коротко мигнёт красным — отпустите кнопки. Затем нажмите направление для проверки."},
            {"Обмен кнопок · M30 включён, режим Switch","SELECT + ↓ · 5 с — поменять местами A/B и X/Y. Короткое красное мигание верхнего индикатора рядом с PAIR подтверждает смену. Отпустите кнопки."},
            {"Прошивка · M30 выключен","Удерживайте L + R + START до красного мигания верхнего индикатора рядом с PAIR, затем подключите USB-кабель. Это режим обновления, не игровой."},
            {"Зарядка · верхний красный индикатор","Мигает во время обычной работы — низкий заряд. Горит при зарядке — заряжается; погас — зарядка завершена. Четыре нижних огонька не показывают процент батареи."}
        };
        for(String[] section:sections){gap(body,8);body.addView(title(section[0],16));TextView description=text(section[1],14,TEXT);description.setLineSpacing(dp(3),1);body.addView(description);}
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        new FocusDialogBuilder(dialogContext()).setTitle("Что означают огоньки M30").setView(scroll).setPositiveButton("Закрыть",null).setNeutralButton("Инструкция 8BitDo",(dialog,which)->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://download.8bitdo.com/Manual/Controller/M30/M30_Manual.pdf")));}catch(ActivityNotFoundException error){toast("Нет приложения для открытия инструкции");}}).show();
    }
    private void testPage(){
        LinearLayout c=card("Тест кнопок · все устройства");
        c.addView(text("Нажмите клавишу на клавиатуре, пульте или геймпаде. Видны события, которые Android передаёт этому экрану.",12,MUTED));
        inputSourceView=text("",14,TEXT);inputSourceView.setMinLines(2);c.addView(inputSourceView);
        lastCodeView=title("—",40);lastCodeView.setTextColor(accent());lastCodeView.setTypeface(Typeface.MONOSPACE);c.addView(lastCodeView);
        lastNameView=text("",12,TEXT);lastNameView.setTypeface(Typeface.MONOSPACE);lastNameView.setTextIsSelectable(true);c.addView(lastNameView);
        lastDetailsView=text("",11,MUTED);lastDetailsView.setTextIsSelectable(true);c.addView(lastDetailsView);
        live=text("",13,accent());live.setMinLines(2);c.addView(live);
        createButtonGrid(c);
        progress=text("",12,MUTED);c.addView(progress);
        if(discoveringSource||pendingSourceId>=0){sourceHintView=text("",12,accent());c.addView(sourceHintView);useSourceButton=button("Выбрать найденный M30",this::acceptPendingSource,true);c.addView(useSourceButton);}
        bindHint=text("",13,accent());c.addView(bindHint);
    }
    private void createButtonGrid(LinearLayout card){
        buttonGrid=column();card.addView(buttonGrid,new LinearLayout.LayoutParams(-1,-2));
        String[][] rows={{"↑","↓","←","→"},{"A","B","C","START"},{"X","Y","Z","SELECT"},{"L","R","★","HOME"}};
        for(String[] labels:rows){
            LinearLayout row=new LinearLayout(this);buttonGrid.addView(row,new LinearLayout.LayoutParams(-1,-2));
            for(int i=0;i<labels.length;i++){
                String label=labels[i];TextView cell=text("HOME".equals(label)?"♥\ufe0e HOME":label,12,TEXT);cell.setSingleLine(true);cell.setAutoSizeTextTypeUniformWithConfiguration(9,12,1,android.util.TypedValue.COMPLEX_UNIT_SP);cell.setGravity(Gravity.CENTER);cell.setPadding(dp(2),0,dp(2),0);cell.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(38),1);lp.topMargin=dp(5);if(i>0)lp.leftMargin=dp(5);row.addView(cell,lp);buttonCells.put(label,cell);
            }
        }
        gap(card,6);
    }
    private void refreshButtonGrid(){
        if(buttonGrid==null)return;ControllerSession shown=inputTest.hasEvent()?controllers.forDevice(inputTest.deviceId()):session;
        buttonGrid.setVisibility(shown==null?View.GONE:View.VISIBLE);if(shown==null)return;ArrayList<String> held=new ArrayList<>();
        for(Map.Entry<String,TextView> entry:buttonCells.entrySet()){
            String label=entry.getKey();boolean active=shown.state.active(label);if(active)held.add(label);int level=active?2:shown.state.tested(label)?1:0;TextView cell=entry.getValue();
            if(!Integer.valueOf(level).equals(cell.getTag())){
                cell.setTag(level);cell.setBackground(shape(active?0xffbfbfbf:level==1?0xff414141:0xff242424,9));cell.setTextColor(active?BG:level==1?TEXT:MUTED);cell.setSelected(active);cell.setContentDescription(label+": "+(active?"нажата":level==1?"проверена":"не проверена"));
            }
        }
        buttonGrid.setContentDescription("Панель кнопок. Геймпад "+(shown==controllers.slots[1]?2:1)+". "+(held.isEmpty()?"Нет удерживаемых кнопок":"Нажато: "+String.join(" + ",held)));
    }
    private void firmwareSection(LinearLayout parent){
        firmwareEntry=button("Прошивка · обновление USB",()->{lastUsbStateCheck=0;refreshFirmwareEntry();if((usbConnectionState&1)!=0){if(optionsDialog!=null)optionsDialog.dismiss();showUsbFirmwareProbe();}},true);
        firmwareEntry.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_usb_cable,0,0,0);firmwareEntry.setCompoundDrawablePadding(dp(6));parent.addView(firmwareEntry);
        firmwareCableHint=text("Подключите джойстик кабелем к телефону",12,MUTED);firmwareCableHint.setGravity(Gravity.CENTER);parent.addView(firmwareCableHint);gap(parent,8);
    }
    private void refreshFirmwareEntry(){
        if(firmwareEntry==null&&bootAccessButton==null)return;
        long now=SystemClock.elapsedRealtime();if(lastUsbStateCheck!=0&&now-lastUsbStateCheck<750)return;lastUsbStateCheck=now;
        usbConnectionState=UsbFirmwareProbe.connectionState(this);boolean connected=(usbConnectionState&1)!=0;
        if(firmwareEntry!=null){firmwareEntry.setEnabled(connected);firmwareEntry.setTextColor(connected?BG:MUTED);firmwareEntry.setBackground(shape(connected?accent():CARD,14));firmwareEntry.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(connected?BG:MUTED));firmwareCableHint.setVisibility(connected?View.GONE:View.VISIBLE);}
        if(bootAccessButton!=null){boolean available=(usbConnectionState&2)!=0;bootAccessButton.setEnabled(available);bootAccessButton.setTextColor(available?BG:MUTED);bootAccessButton.setBackground(shape(available?accent():CARD,14));}
    }
    private void resetTest(){session.dpad.reset();session.state.reset();session.axes.clear();session.log.clear();session.monitor.clearSelected();session.lastWasAxis=false;session.lastAxisToken=null;session.lastInput=0;refreshLive();}
    private void runOption(int n){
        if(n==0)showBindingPicker();else if(n==1)resetTest();else if(n==2)devicePicker();else if(n==3)beginSourceDiscovery();else if(n==4)showDiagnostics();else if(n==5)showHelp();else if(n==6)showModeInstructions();else if(n==7)openFullBluetoothSearch();else if(n==8)showAutoFollowSetting();
    }
    private void showAutoFollowSetting(){
        new FocusDialogBuilder(dialogContext()).setTitle("Автопереход по нажатию").setMessage("На страницах джойстиков новое нажатие кнопки или отклонение крестовины открывает страницу того геймпада, от которого пришёл ввод. Отпускание, нейтраль осей и повтор удержания не переключают вкладку. Во вкладке «Тест кнопок», меню, диагностике и во время назначения кнопки автопереход не действует.")
            .setPositiveButton(autoFollowControllers?"Выключить":"Включить",(d,w)->{autoFollowControllers=!autoFollowControllers;getPreferences(0).edit().putBoolean("follow_controller_input",autoFollowControllers).apply();toast(autoFollowControllers?"Автопереход включён":"Автопереход выключен");}).setNegativeButton("Отмена",null).show();
    }
    private boolean followControllerInput(ControllerSession target,boolean physicalGamepad,boolean freshActivity){
        if(widePanels!=null)return false; // Both sources are already visible; keep the Connection owner stable.
        int slot=target==controllers.slots[0]?0:target==controllers.slots[1]?1:-1;
        boolean ready=hasWindowFocus()&&!inlineBluetoothOpen&&diagnosticsDialog==null&&dpadDialog==null&&session.waiting==null&&!touchSwiping&&!discoveringSource;
        int next=ControllerAutoFollow.page(page,slot,autoFollowControllers,ready,physicalGamepad,freshActivity);
        if(next==page)return false;switchPage(next,false);return true;
    }
    private void queryBootModel(){
        if(bootQueryRunning||pendingBootName!=null){toast("Проверка загрузчика уже запущена");return;}
        android.hardware.usb.UsbManager manager=(android.hardware.usb.UsbManager)getSystemService(USB_SERVICE);
        ArrayList<android.hardware.usb.UsbDevice> found=new ArrayList<>();if(manager!=null)for(android.hardware.usb.UsbDevice candidate:manager.getDeviceList().values())if(UsbBootModel.supported(candidate))found.add(candidate);
        if(found.size()!=1){toast(found.isEmpty()?"Подключите загрузчик 8BitDo Boot · 2DC8:3208":"Подключите только один загрузчик 8BitDo");return;}
        android.hardware.usb.UsbDevice target=found.get(0);
        if(manager.hasPermission(target)){runBootQuery(manager,target);return;}
        if(!bootPermissionReceiverRegistered){IntentFilter filter=new IntentFilter(bootPermissionAction);registerReceiver(bootPermissionReceiver,filter,Build.VERSION.SDK_INT>=33?Context.RECEIVER_NOT_EXPORTED:0);bootPermissionReceiverRegistered=true;}
        pendingBootName=target.getDeviceName();Intent callback=new Intent(bootPermissionAction).setPackage(getPackageName());
        int flags=PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE;
        try{manager.requestPermission(target,PendingIntent.getBroadcast(this,240,callback,flags));}catch(RuntimeException error){pendingBootName=null;toast("Не удалось запросить USB-доступ: "+error.getClass().getSimpleName());}
    }
    private void runBootQuery(android.hardware.usb.UsbManager manager,android.hardware.usb.UsbDevice target){
        bootQueryRunning=true;toast("Читаю модель и версию M30…");
        new Thread(()->{String result=UsbBootModel.inspect(manager,target);handler.post(()->{bootQueryRunning=false;if(isFinishing()||isDestroyed())return;new FocusDialogBuilder(dialogContext()).setTitle("Модель и версия M30").setMessage(result).setPositiveButton("Поделиться",(dialog,which)->{Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"M30 Companion "+appVersion+" · Boot model\n"+result);try{startActivity(Intent.createChooser(share,"Отчёт загрузчика"));}catch(ActivityNotFoundException error){toast("Нет приложения для отправки");}}).setNegativeButton("Закрыть",null).show();});},"M30-boot-model").start();
    }
    @Override protected void onDestroy(){closeInlineBluetooth();closeDpadMenu();closeDiagnostics();if(bootPermissionReceiverRegistered)unregisterReceiver(bootPermissionReceiver);super.onDestroy();}
    private void showUsbFirmwareProbe(){startActivity(new Intent(this,M30FirmwareActivity.class));}
    private void showBindingPicker(){
        String[] labels=Arrays.copyOf(PadState.LABELS,PadState.LABELS.length+1);labels[labels.length-1]="Вернуть стандартную подсветку";
        new FocusDialogBuilder(dialogContext()).setTitle("Какая кнопка подсвечивается неверно?").setItems(labels,(d,n)->{
            if(n<PadState.LABELS.length)startBinding(labels[n]);else{getPreferences(0).edit().remove(bindingKey()).apply();loadBindings();resetTest();}
        }).setNegativeButton("Отмена",null).show();
    }
    private Dialog inputDialog(){return new Dialog(dialogContext()){
            @Override public boolean dispatchKeyEvent(KeyEvent event){return handleKeyInput(event)||super.dispatchKeyEvent(event);}
            @Override public boolean dispatchGenericMotionEvent(MotionEvent event){return handleMotionInput(event)||super.dispatchGenericMotionEvent(event);}
            @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused){controllers.releaseAll();inputTest.release();refreshLive();}}
        };
    }
    private void closeDpadMenu(){if(dpadDialog!=null)dpadDialog.dismiss();}
    private void showDpadMenu(){
        if(dpadDialog!=null)return;closeDiagnostics();
        Dialog dialog=inputDialog();dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout shell=column();shell.setPadding(dp(16),dp(12),dp(16),dp(12));shell.setBackground(shape(CARD,20));shell.addView(title("Крестовина M30",18));
        ScrollView scroll=new ScrollView(this);LinearLayout body=column();scroll.addView(body);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        dpadDialogStatus=text("",14,accent());body.addView(dpadDialogStatus);
        body.addView(text("На включённом M30 держите SELECT и нужное направление одновременно 5 секунд:",14,TEXT));
        for(String combination:new String[]{"SELECT + ↑ — D-pad (крестовина)","SELECT + ← — левый стик","SELECT + → — правый стик"})body.addView(text(combination,15,accent()));
        body.addView(text("Верхний индикатор рядом с PAIR коротко мигнёт красным — отпустите кнопки.",14,TEXT));
        body.addView(text("Нажмите направление ещё раз — приложение проверит, как работает крестовина. Режим Android / Windows при этом не меняется.",13,MUTED));
        shell.addView(button("Закрыть",dialog::dismiss,false));dialog.setContentView(shell);dialog.setCanceledOnTouchOutside(true);dpadDialog=dialog;
        dialog.setOnDismissListener(d->{if(dpadDialog!=dialog)return;dpadDialog=null;dpadDialogStatus=null;controllers.releaseAll();inputTest.release();if(!isFinishing()&&!isDestroyed())refreshLive();});
        showOverlay(dialog,380);refreshLive();
    }
    private void showOverlay(Dialog dialog,int maxHeight){
        dialog.show();FocusNavigation.prepare(dialog.getWindow().getDecorView());Window window=dialog.getWindow();if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(.65f);
            int available=root.getHeight()>0?root.getHeight():getResources().getDisplayMetrics().heightPixels;
            window.setLayout(getResources().getDisplayMetrics().widthPixels-dp(32),Math.min(dp(maxHeight),Math.round(available*.85f)));
        }
    }
    private void closeDiagnostics(){if(diagnosticsDialog!=null)diagnosticsDialog.dismiss();}
    private void showDiagnostics(){
        if(diagnosticsDialog!=null)return;closeDpadMenu();
        final ControllerSession observed=session;
        Dialog dialog=inputDialog();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout shell=column();shell.setPadding(dp(16),dp(12),dp(16),dp(12));shell.setBackground(shape(CARD,20));
        shell.addView(title("Диагностика · Джойстик "+(activeSlot+1),18));
        ScrollView scroll=new ScrollView(this);scroll.setContentDescription("Прокрутка диагностики");shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout c=column();scroll.addView(c);
        diagnosticsDpadView=text("",13,accent());c.addView(diagnosticsDpadView);
        c.addView(text("Крестовина определяется по нажатиям. Android может преобразовывать сигналы; сохранённая настройка прошивки не читается.",12,MUTED));
        Switch nav=new Switch(this);nav.setText("Блокировать навигацию кнопками");nav.setTextColor(TEXT);nav.setTextSize(14);nav.setMinHeight(dp(48));nav.setChecked(navigationLocked);c.addView(nav);nav.setOnCheckedChangeListener((v,on)->navigationLocked=on);
        Switch other=new Switch(this);other.setText("Показывать другие устройства");other.setTextColor(TEXT);other.setTextSize(14);other.setMinHeight(dp(48));other.setChecked(session.monitor.otherEnabled);c.addView(other);other.setOnCheckedChangeListener((v,on)->{session.monitor.setOtherEnabled(on);otherMotion.clear();refreshLive();});
        otherLastView=text("",14,accent());c.addView(otherLastView);otherLogView=text("",12,MUTED);otherLogView.setTypeface(Typeface.MONOSPACE);c.addView(otherLogView);
        c.addView(text("Видны только события, переданные приложению. Они не смешиваются с проверкой M30.",12,MUTED));
        c.addView(button("Устройства Android",this::showInputSources,false));
        rawView=text("",12,MUTED);rawView.setTypeface(Typeface.MONOSPACE);rawView.setTextIsSelectable(true);c.addView(rawView);
        logView=text("",12,MUTED);logView.setTypeface(Typeface.MONOSPACE);logView.setTextIsSelectable(true);c.addView(logView);c.addView(button("Поделиться отчётом",this::shareReport,false));
        shell.addView(button("Закрыть",dialog::dismiss,false));dialog.setContentView(shell);dialog.setCanceledOnTouchOutside(true);
        diagnosticsDialog=dialog;
        dialog.setOnDismissListener(d->{
            if(diagnosticsDialog!=dialog)return;
            diagnosticsDialog=null;observed.monitor.setOtherEnabled(false);otherMotion.clear();
            rawView=null;logView=null;otherLastView=null;otherLogView=null;diagnosticsDpadView=null;
            controllers.releaseAll();inputTest.release();if(!isFinishing()&&!isDestroyed())refreshLive();
        });
        showOverlay(dialog,620);
        refreshLive();
    }
    private void showInputSources(){
        ArrayList<String> rows=new ArrayList<>();for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null)rows.add(d.getName()+" · ID "+id);}
        new FocusDialogBuilder(dialogContext()).setTitle("Устройства Android").setMessage(String.join("\n\n",rows)+"\n\nВстроенные кнопки и сенсоры телефона тоже являются источниками ввода. Здесь они только перечислены; тест M30 не переключается на них.").setPositiveButton("Закрыть",null).show();
    }
    private void showHelp(){
        new FocusDialogBuilder(dialogContext()).setTitle("Справка M30").setMessage(InstructionText.format("Крестовина: на включённом M30 держите SELECT + ↑ — D-pad, SELECT + ← — левый стик или SELECT + → — правый стик 5 секунд. Верхний индикатор рядом с PAIR мигнёт красным — отпустите кнопки.\n\n"+session.dpad.summary()+". "+session.dpad.evidence()+". Определяется по сигналам Android после нажатия, не чтением настройки прошивки.\n\n★ управляет Turbo в режимах Android, Windows · X-input и macOS; в Switch это снимок экрана. HOME может обрабатываться Android до приложения. Если событие доходит, его код виден во вкладке «Тест кнопок».\n\nTurbo: держите игровую кнопку и нажмите ★. Повтор отключает Turbo; в Switch Turbo нет.\n\nКрасный мигающий LED — низкий заряд, постоянный — зарядка, погас — зарядка завершена. Процент доступен только если Android его передаёт.\n\nДля Sega назначайте физические A/B/C, X/Y/Z, START и SELECT (Mode) в самом эмуляторе. Companion не перенастраивает игры.\n\nM30 Companion · неофициальное приложение."))
            .setPositiveButton("Закрыть",null).setNeutralButton("Официальный PDF",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://download.8bitdo.com/Manual/Controller/M30/M30_Manual.pdf")));}catch(ActivityNotFoundException e){toast("Нет приложения для открытия ссылки");}}).show();
    }
    private ArrayList<ControllerTelemetry.Source> telemetrySources(boolean readBattery){return telemetrySources(session,readBattery);}
    private ArrayList<ControllerTelemetry.Source> telemetrySources(ControllerSession observed,boolean readBattery){
        ArrayList<ControllerTelemetry.Source> result=new ArrayList<>();if(!observed.connected())return result;
        TreeSet<Integer> ids=new TreeSet<>(observed.sourceIds);ids.add(observed.deviceId);
        for(int id:ids){
            if(id<0||controllers.forDevice(id)!=observed)continue;
            InputDevice input=InputDevice.getDevice(id);if(input==null)continue;
            if(id!=observed.deviceId&&(observed.descriptor.isEmpty()||!observed.descriptor.equals(input.getDescriptor())))continue;
            boolean present=false;float capacity=-1f;String batteryError=null;
            if(readBattery&&Build.VERSION.SDK_INT>=31)try{BatteryState state=input.getBatteryState();present=state.isPresent();capacity=state.getCapacity();}catch(RuntimeException error){batteryError=error.getClass().getSimpleName()+": "+error.getMessage();}
            result.add(new ControllerTelemetry.Source(id,input.getName(),input.getVendorId(),input.getProductId(),present,capacity,batteryError));
        }
        return result;
    }
    private ModeDetector.Result modeResult(){return ControllerTelemetry.mode(session.deviceId,telemetrySources(false));}
    private void alignModeOnOpen(){
        int candidate=modeResult().candidate;
        if(candidate>=0&&candidate!=session.mode)applyMode(candidate);
    }
    private void applyMode(int mode){
        session.mode=mode;getPreferences(0).edit().putInt("mode",mode).putInt("profile:"+session.descriptor,mode).apply();clearSession();loadBindings();
    }
    private void selectMode(int m){
        int scrollY=page!=1&&contentScroll!=null?contentScroll.getScrollY():0;
        applyMode(m);render();
        if(page!=1&&contentScroll!=null){ScrollView current=contentScroll;current.post(()->current.scrollTo(0,scrollY));}
    }
    private String modeSummaryText(){
        ModeDetector.Result detected=modeResult();
        return device==null?"Джойстик не подключён":detected.candidate<0?"Режим не определён · ⓘ":modeNames[detected.candidate];
    }
    private void showModeInstructions(){
        final int shown=session.mode;LinearLayout heading=column();heading.setPadding(dp(24),dp(18),dp(24),dp(8));heading.addView(title(modeNames[shown],18));
        TextView combination=text(modeKeys[shown],21,accent());combination.setSingleLine(true);heading.addView(combination);
        new FocusDialogBuilder(dialogContext()).setCustomTitle(heading)
            .setMessage(InstructionText.format(startupInstructions()))
            .setPositiveButton("Я включил этот режим",(dialog,which)->{
                if(device==null){toast("Сначала подключите M30 и выберите его источник ввода");return;}
                if(!SourceSelector.discoverable(sourceInfo(device))){toast("Сначала выберите M30: сейчас выбран встроенный источник телефона");return;}
                session.confirmation.confirm(shown,connectionKey());refreshLive();toast("Отмечено вами: "+modeNames[shown]);
            }).setNegativeButton("Закрыть",null).show();
    }
    private boolean gamepad(InputDevice d){return d!=null&&(((d.getSources()&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD)||((d.getSources()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK));}
    private SourceSelector.Source sourceInfo(InputDevice d){
        boolean external=Build.VERSION.SDK_INT>=29?d.isExternal():d.getVendorId()!=0||d.getProductId()!=0;
        return new SourceSelector.Source(d.getId(),d.getDescriptor(),d.getName(),gamepad(d),external);
    }
    private String connectionKey(){return device==null?null:device.getDescriptor()+":"+device.getId()+":"+device.getSources()+":"+device.getVendorId()+":"+device.getProductId();}
    private String modeStatusText(){
        int confirmed=session.confirmation.current();String result=confirmed<0?"Режим по вашему запуску: не подтверждён":"Режим по вашему запуску: "+modeNames[confirmed]+" (ваша отметка)";
        if(confirmed>=0&&confirmed!=session.mode)result+="\nПрофиль схемы отличается от вашей отметки. Выбор профиля не переключает M30.";
        return result+"\nПо признакам Android: "+modeResult().title;
    }
    private String startupInstructions(){return ConnectionGuide.full(session.mode);}
    private boolean chooseDevice(){
        ArrayList<SourceSelector.Source> list=new ArrayList<>();for(int id:InputDevice.getDeviceIds()){InputDevice input=InputDevice.getDevice(id);if(input!=null)list.add(sourceInfo(input));}
        boolean changed=controllers.reconcile(list,getPreferences(0).getString("device",""),!connectionRequest.active(SystemClock.elapsedRealtime()));
        android.content.SharedPreferences.Editor editor=getPreferences(0).edit();
        for(int i=0;i<controllers.slots.length;i++){
            ControllerSession target=controllers.slots[i];InputDevice input=controllerDevice(target);
            if(!target.bindingsLoaded){String profile="profile:"+target.descriptor;Integer saved=getPreferences(0).contains(profile)?getPreferences(0).getInt(profile,0):null;target.mode=ModeDetector.profileOnConnect(saved,getPreferences(0).getInt("mode",0),input==null?null:input.getName(),input==null?0:input.getVendorId(),input==null?0:input.getProductId());loadBindings(target);target.bindingsLoaded=true;}
            editor.putString("controller."+i,target.descriptor);
            if(!target.descriptor.isEmpty()){
                if(input!=null){target.deviceName=input.getName();editor.putString("device_name:"+target.descriptor,target.deviceName);}
                else if(target.deviceName==null)target.deviceName=getPreferences(0).getString("device_name:"+target.descriptor,null);
                editor.putString("controller."+i,target.descriptor).putInt("profile:"+target.descriptor,target.mode);
            }
            target.confirmation.observe(input==null?null:input.getDescriptor()+":"+input.getId()+":"+input.getSources()+":"+input.getVendorId()+":"+input.getProductId());
        }
        editor.apply();device=controllerDevice(session);return changed;
    }
    private void requestBluetoothController(String address,int slot){
        connectionRequest.begin(address,slot,SystemClock.elapsedRealtime());
        chooseDevice();String descriptor=bluetoothNames.linkedDescriptor(address);
        if(!descriptor.isEmpty())for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null&&descriptor.equals(d.getDescriptor())){confirmBluetoothController(d);return;}}
    }
    private void confirmBluetoothController(InputDevice input){
        if(!connectionRequest.active(SystemClock.elapsedRealtime())||input==null)return;
        String address=connectionRequest.address;int owner=connectionRequest.slot;
        controllers.moveTo(owner,sourceInfo(input));session=controllers.slots[activeSlot];
        android.content.SharedPreferences.Editor prefs=getPreferences(0).edit();for(int n=0;n<2;n++)prefs.putString("controller."+n,controllers.slots[n].descriptor);prefs.apply();
        bluetoothNames.bind(address,owner);connectionRequest.clear();chooseDevice();render();
        toast("Подключён Джойстик "+(owner+1)+". Источник подтверждён.");
    }
    private void beginSourceDiscovery(){
        discoveringSource=!discoveringSource;pendingSourceId=-1;session.waiting=null;
        if(pad!=null)pad.waiting=null;
        if(page!=1)switchPage(1);else render();
        toast(discoveringSource?"Нажмите A или B на M30. Клавиатуру пока не нажимайте.":"Поиск источника отменён");
    }
    private void useDevice(InputDevice d){
        if(d==null||SourceSelector.systemInputName(d.getName())){toast("Это системный источник или пульт. Его события видны в Тесте кнопок.");return;}
        int existing=controllers.indexOf(d.getId());
        if(existing>=0)selectController(existing);else controllers.assign(activeSlot,sourceInfo(d));
        getPreferences(0).edit().putString("device",d.getDescriptor()).apply();chooseDevice();
        discoveringSource=false;pendingSourceId=-1;if(page!=1){page=connectionPageId();alignModeOnOpen();}render();
    }
    private void acceptPendingSource(){InputDevice d=pendingSourceId<0?null:InputDevice.getDevice(pendingSourceId);if(d!=null)useDevice(d);else{pendingSourceId=-1;refreshLive();toast("Этот источник уже отключён. Запустите поиск заново.");}}
    private void observeIncomingSource(InputDevice d,boolean active){
        if(d==null||!active||!SourceSelector.discoverable(sourceInfo(d)))return;
        if(discoveringSource){useDevice(d);toast("Выбран источник: "+d.getName()+" · ID "+d.getId());}
        else if(!selected(d.getId())&&controllers.forDevice(d.getId())==null)pendingSourceId=d.getId();
    }
    private void devicePicker(){sourcePicker(false);}
    private void sourcePicker(boolean all){
        ArrayList<InputDevice> list=new ArrayList<>();for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null&&id>=0&&(all||SourceSelector.controller(sourceInfo(d))))list.add(d);}
        if(list.isEmpty()){new FocusDialogBuilder(dialogContext()).setTitle("Геймпад пока не найден").setMessage(InstructionText.format("Найдите M30 через поиск Bluetooth в приложении или подключите через USB OTG, затем вернитесь.")).setPositiveButton("Найти геймпад",(d,w)->openBluetoothSearch()).setNegativeButton("Закрыть",null).show();return;}
        String[] names=new String[list.size()];for(int i=0;i<names.length;i++){int slot=controllers.indexOf(list.get(i).getId());names[i]=(slot<0?"":"Джойстик "+(slot+1)+" · ")+list.get(i).getName()+" · ID "+list.get(i).getId();}
        new FocusDialogBuilder(dialogContext()).setTitle(all?"Все источники · выбор меняет тестируемое устройство":"Устройство ввода").setItems(names,(d,n)->{useDevice(list.get(n));}).setNeutralButton("Поиск Bluetooth",(d,w)->openBluetoothSearch()).setNegativeButton("Закрыть",null).show();
    }
    private String bindingKey(){return bindingKey(session);}
    private String bindingKey(ControllerSession target){return "bindings:"+(target.descriptor.isEmpty()?"none":target.descriptor)+":"+target.mode;}
    private void loadBindings(){loadBindings(session);}
    private void loadBindings(ControllerSession target){
        target.state.bindings.clear();target.state.bindings.putAll(ButtonGuide.defaultBindings(target.mode));
        try{JSONObject j=new JSONObject(getPreferences(0).getString(bindingKey(target),"{}"));if(j.length()>0){target.state.bindings.clear();Iterator<String> it=j.keys();while(it.hasNext()){String k=it.next();target.state.bindings.put(k,j.getString(k));}}}catch(JSONException e){toast("Назначения повреждены. Используется стандартная раскладка.");}
    }
    private void startBinding(String label){if(device==null){toast("Сначала подключите геймпад");return;}session.waiting=label;session.state.release();if(pad!=null)pad.waiting=label;if(bindHint!=null)bindHint.setText("Нажмите «"+label+"» на геймпаде. Назначение сохранится автоматически. Для отмены нажмите «Назад».");if(pad!=null)pad.invalidate();refreshLive();}
    private void token(String token,boolean down){token(session,token,down);}
    private void token(ControllerSession target,String token,boolean down){
        if(target==session&&target.waiting!=null&&down&&!target.state.down.contains(token)){
            target.state.bind(target.waiting,token);getPreferences(0).edit().putString(bindingKey(target),new JSONObject(target.state.bindings).toString()).apply();
            toast(target.waiting+" → "+token);target.waiting=null;if(pad!=null)pad.waiting=null;if(bindHint!=null)bindHint.setText("Сохранено. Нажмите другую кнопку на схеме, чтобы назначить её.");
        }
        if(target.state.set(token,down)){target.lastInput=SystemClock.elapsedRealtime();if(!token.startsWith("K:")&&!token.startsWith("S:"))addLog(target,(down?"↓ ":"↑ ")+token);}
    }
    private boolean selected(int id){return controllers.forDevice(id)==session;}
    private static String keyToken(int code,int scan){return code==KeyEvent.KEYCODE_UNKNOWN?"S:"+scan:"K:"+code;}
    private InputMonitor.Key diagnosticKey(KeyEvent e){InputDevice d=e.getDevice();return new InputMonitor.Key(e.getDeviceId(),d==null?"Источник без имени":d.getName(),e.getKeyCode(),KeyEvent.keyCodeToString(e.getKeyCode()),e.getScanCode(),e.getAction()==KeyEvent.ACTION_DOWN,e.getRepeatCount(),e.getSource());}
    private void bindLastKey(){
        InputMonitor.Key k=session.monitor.lastSelected;
        if((!session.lastWasAxis&&(k==null||!selected(k.deviceId)))||device==null){toast("Сначала нажмите нужную кнопку на выбранном устройстве");return;}
        final String token=session.lastWasAxis?session.lastAxisToken:keyToken(k.code,k.scan);
        final String label=session.lastWasAxis?MotionEvent.axisToString(session.lastAxis):k.name;
        new FocusDialogBuilder(dialogContext()).setTitle(label+" · кнопка на схеме…").setItems(PadState.LABELS,(d,n)->{
            session.state.bind(PadState.LABELS[n],token);getPreferences(0).edit().putString(bindingKey(),new JSONObject(session.state.bindings).toString()).apply();session.state.release();session.waiting=null;
            if(pad!=null)pad.waiting=null;if(bindHint!=null)bindHint.setText("На схеме: "+PadState.LABELS[n]+" → "+label);refreshLive();
        }).setNegativeButton("Отмена",null).show();
    }
    private void observeM30Home(ControllerSession target,InputDevice input,InputMonitor.Key key){
        if(input==null||!key.down||key.repeat!=0||target.waiting!=null)return;
        if(ButtonGuide.observeM30Home(target.state,input.getName(),input.getVendorId(),key.code)){
            getPreferences(0).edit().putString(bindingKey(target),new JSONObject(target.state.bindings).toString()).apply();
            // Rebuild the static action captions so they describe the newly observed raw code too.
            if(target==session&&page!=1){int y=contentScroll.getScrollY();render();ScrollView scroll=contentScroll;scroll.post(()->scroll.scrollTo(0,y));}
        }
    }
    private boolean controllerEvent(int source){return (source&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD||(source&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK||(source&InputDevice.SOURCE_DPAD)==InputDevice.SOURCE_DPAD;}
    private boolean suppressNavigation(InputDevice d,int source){return SourceSelector.blockNavigation(d==null?null:sourceInfo(d),controllerEvent(source),navigationLocked);}
    private boolean gamepadInput(KeyEvent e){if(e.getDevice()!=null&&SourceSelector.systemInputName(e.getDevice().getName()))return false;return (e.getSource()&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD||(e.getSource()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK||(e.getDevice()!=null&&SourceSelector.namedController(sourceInfo(e.getDevice())));}
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(pagingKey==e.getKeyCode()&&pagingDevice==e.getDeviceId()){if(e.getAction()==KeyEvent.ACTION_UP){if(pagingRecorded){inputTest.record(diagnosticKey(e));if(page==1)refreshLive();}pagingKey=-1;pagingRecorded=false;}return true;}
        if(handleKeyInput(e))return true;
        boolean horizontal=e.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT||e.getKeyCode()==KeyEvent.KEYCODE_DPAD_RIGHT;
        View focused=getCurrentFocus();
        if(horizontal&&RemoteNavigation.pagesFrom(FocusNavigation.key(focused),gamepadInput(e),FocusNavigation.horizontalEdge(focused,e.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT?View.FOCUS_LEFT:View.FOCUS_RIGHT))){
            if(e.getAction()==KeyEvent.ACTION_DOWN){if(e.getRepeatCount()==0){
                pagingKey=e.getKeyCode();pagingDevice=e.getDeviceId();pagingRecorded=page==1;
                int target=RemoteNavigation.pageTarget(page,e.getKeyCode(),true);
                if(target!=page){switchPage(target);View selected=FocusNavigation.find(root,"tab:"+page);if(selected!=null)selected.requestFocus();}
            }return true;}
            if(e.getAction()==KeyEvent.ACTION_UP)return true;
        }
        return super.dispatchKeyEvent(e);
    }
    private boolean handleKeyInput(KeyEvent e){
        // A TV remote can report SOURCE_DPAD: this is menu navigation, not a gamepad lock.
        boolean fromGamepad=gamepadInput(e);
        if(RemoteNavigation.routesToUi(e.getKeyCode(),fromGamepad)){
            if(e.getAction()==KeyEvent.ACTION_DOWN||e.getAction()==KeyEvent.ACTION_UP){
                InputMonitor.Key key=diagnosticKey(e);
                if(page==1)inputTest.record(key);
                if(session.monitor.otherEnabled)session.monitor.record(key,session.deviceId);
                if(page==1||session.monitor.otherEnabled)refreshLive();
            }
            return false; // Activity/Dialog handles arrows, OK, Back and Tab using standard focus.
        }
        if(connectionRequest.confirms(fromGamepad,e.getAction()==KeyEvent.ACTION_DOWN,e.getRepeatCount(),SystemClock.elapsedRealtime()))confirmBluetoothController(e.getDevice());
        boolean keyAction=e.getAction()==KeyEvent.ACTION_DOWN||e.getAction()==KeyEvent.ACTION_UP;
        if(keyAction){
            if(page==1&&discoveringSource)observeIncomingSource(e.getDevice(),e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0);
            ControllerSession target=controllers.forDevice(e.getDeviceId());InputMonitor.Key key=diagnosticKey(e);
            if(page==1)inputTest.record(key);
            if(target!=null){
                boolean freshActivity=ControllerAutoFollow.key(e.getAction()==KeyEvent.ACTION_DOWN,e.getRepeatCount(),target.state.down.contains(keyToken(e.getKeyCode(),e.getScanCode())));
                target.monitor.record(key,key.deviceId);target.lastWasAxis=false;
                observeM30Home(target,e.getDevice(),key);
                if(e.getRepeatCount()==0){token(target,keyToken(e.getKeyCode(),e.getScanCode()),e.getAction()==KeyEvent.ACTION_DOWN);addLog(target,key.summary());}
                target.dpad.key(key.code,key.down,key.repeat,target.state.active("SELECT"),e.getEventTime());
                if(followControllerInput(target,fromGamepad,freshActivity))return true;
                if(target!=session&&session.monitor.otherEnabled)session.monitor.other(key.summary());
                if(page==1||target==session||widePanels!=null||session.monitor.otherEnabled)refreshLive();return true;
            }
            if(page==1){session.monitor.record(key,session.deviceId);refreshLive();if(!e.isSystem()||suppressNavigation(e.getDevice(),e.getSource()))return true;}
            else if(session.monitor.otherEnabled){session.monitor.record(key,session.deviceId);refreshLive();}
        }
        if(controllers.forDevice(e.getDeviceId())!=null||(page==1&&suppressNavigation(e.getDevice(),e.getSource())))return true;
        return false;
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent e){return handleMotionInput(e)||super.dispatchGenericMotionEvent(e);}
    private boolean handleMotionInput(MotionEvent e){
        if(page==1&&e.isFromSource(InputDevice.SOURCE_JOYSTICK)&&e.getActionMasked()==MotionEvent.ACTION_MOVE){
            InputDevice source=e.getDevice();if(source!=null)for(InputDevice.MotionRange range:source.getMotionRanges())if((e.getSource()&range.getSource())==range.getSource())inputTest.axis(e.getDeviceId(),source.getName(),range.getAxis(),MotionEvent.axisToString(range.getAxis()),PadState.normalize(e.getAxisValue(range.getAxis()),range.getFlat()));
            refreshLive();
            InputDevice incoming=e.getDevice();boolean active=false;if(incoming!=null)for(InputDevice.MotionRange r:incoming.getMotionRanges())if(Math.abs(PadState.normalize(e.getAxisValue(r.getAxis()),r.getFlat()))>.5f){active=true;break;}
            if(discoveringSource)observeIncomingSource(incoming,active);if(pendingSourceId>=0)refreshLive();
        }
        ControllerSession target=controllers.forDevice(e.getDeviceId());
        if(target!=null&&e.getActionMasked()==MotionEvent.ACTION_MOVE){
            InputDevice input=e.getDevice();if(input==null)return true;
            boolean freshActivity=false;
            Map<Integer,Float> directionAxes=new HashMap<>();Set<Integer> centeredAxes=new HashSet<>();
            for(InputDevice.MotionRange r:input.getMotionRanges()){
                if((e.getSource()&r.getSource())!=r.getSource())continue;
                directionAxes.put(r.getAxis(),PadState.normalize(e.getAxisValue(r.getAxis()),r.getFlat()));if(r.getMin()<0)centeredAxes.add(r.getAxis());
                int axis=r.getAxis();float value=PadState.normalize(e.getAxisValue(axis),r.getFlat());Float previous=target.axes.put(MotionEvent.axisToString(axis),value);
                if((previous==null?value!=0:previous!=value)&&(value!=0||(previous!=null&&previous!=0))){target.lastWasAxis=true;target.lastAxis=axis;target.lastAxisValue=value;float direction=value!=0?value:previous;target.lastAxisToken=(r.getMin()>=0?"T:":"A:")+axis+":"+(direction<0?"-":"+");}
                boolean directional=r.getMin()<0&&(axis==MotionEvent.AXIS_HAT_X||axis==MotionEvent.AXIS_HAT_Y||axis==MotionEvent.AXIS_X||axis==MotionEvent.AXIS_Y||axis==MotionEvent.AXIS_Z||axis==MotionEvent.AXIS_RZ||axis==MotionEvent.AXIS_RX||axis==MotionEvent.AXIS_RY);
                if((directional||r.getMin()>=0&&value>0)&&ControllerAutoFollow.axis(previous,value))freshActivity=true;
                if(directional){
                    token(target,"A:"+axis+":-",value<-.5f);token(target,"A:"+axis+":+",value>.5f);
                }else if(r.getMin()>=0)token(target,"T:"+axis+":+",value>.5f);
            }
            target.dpad.axes(directionAxes,centeredAxes,target.state.active("SELECT"),e.getEventTime());
            if(followControllerInput(target,e.isFromSource(InputDevice.SOURCE_JOYSTICK),freshActivity))return true;
            if(target==session||widePanels!=null)refreshLive();return true;
        }
        if(session.monitor.otherEnabled&&!selected(e.getDeviceId())&&e.isFromSource(InputDevice.SOURCE_JOYSTICK)&&e.getActionMasked()==MotionEvent.ACTION_MOVE){
            InputDevice other=e.getDevice();if(other!=null){StringBuilder values=new StringBuilder();for(InputDevice.MotionRange r:other.getMotionRanges()){float v=PadState.normalize(e.getAxisValue(r.getAxis()),r.getFlat());if(v!=0)values.append(String.format(Locale.ROOT,"%s=%.2f ",MotionEvent.axisToString(r.getAxis()),v));}
                String signature=values.length()==0?"оси в нейтрали":values.toString();String previous=otherMotion.put(e.getDeviceId(),signature);
                if(!signature.equals(previous)){session.monitor.other(other.getName()+" [ID "+e.getDeviceId()+"] · "+signature);refreshLive();}
            }
        }
        if(page==1&&controllerEvent(e.getSource())&&suppressNavigation(e.getDevice(),e.getSource()))return true;
        return false;
    }
    private void addLog(String text){addLog(session,text);}
    private void addLog(ControllerSession target,String text){target.log.addFirst(String.format(Locale.ROOT,"%tT  %s",new Date(),text));while(target.log.size()>40)target.log.removeLast();}
    private String lastInputAction(){
        if(session.lastWasAxis){
            if(session.lastAxisValue==0)return "Ось в нейтрали";
            if(session.lastAxis==0||session.lastAxis==15)return session.lastAxisValue<0?"Влево по меню":"Вправо по меню";
            if(session.lastAxis==1||session.lastAxis==16)return session.lastAxisValue<0?"Вверх по меню":"Вниз по меню";
            return ButtonGuide.action(session.lastAxisToken);
        }
        InputMonitor.Key key=session.monitor.lastSelected;
        return key==null?"—":key.code==0?"Назначение неизвестно: Android не распознал код":ButtonGuide.keyAction(key.code);
    }
    private void refreshControllerTabNames(){
        if(tabs==null||bluetoothNames==null)return;
        for(int slot=0;slot<2;slot++){
            View view=FocusNavigation.find(tabs,"tab:"+(slot==0?0:2));if(!(view instanceof TextView))continue;TextView tab=(TextView)view;
            String title="Джойстик "+(slot+1),name=bluetoothNames.slotName(slot),value=title+"\n"+name;
            if(!value.contentEquals(tab.getText())){android.text.SpannableString label=new android.text.SpannableString(value);label.setSpan(new android.text.style.RelativeSizeSpan(12f/14f),title.length()+1,label.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);tab.setText(label);}
            tab.setContentDescription(title+". "+name+(tab.isSelected()?", выбрано":""));
        }
    }
    private void refreshLive(){
        refreshControllerTabNames();refreshWideControllers();
        refreshFirmwareEntry();refreshButtonGrid();if(page==1)refreshInputTest();
        if(dpadDialogStatus!=null)dpadDialogStatus.setText(session.dpad.summary());
        if(diagnosticsDpadView!=null)diagnosticsDpadView.setText(session.dpad.summary()+"\n"+session.dpad.evidence());
        if(page!=1&&headerInputLine!=null){
            InputMonitor.Key key=session.monitor.lastSelected;
            String code=session.lastWasAxis?String.format(Locale.ROOT,"AX %d · %s = %.3f",session.lastAxis,MotionEvent.axisToString(session.lastAxis),session.lastAxisValue):key==null?"Код — · значение —":"Код "+key.code+" · "+key.name.replace("KEYCODE_","")+" = "+(key.down?"1":"0");
            String action=key==null&&!session.lastWasAxis?(device==null?"Подключите M30":"Нажмите кнопку для проверки"):session.lastWasAxis?"Ось · "+(Math.abs(session.lastAxisValue)<0.001f?"нейтраль":"активна"):key.name.replace("KEYCODE_","")+" · "+(key.down?"нажато":"отпущено");
            if(!code.contentEquals(headerInputLine.getText()))headerInputLine.setText(code);
            if(!action.contentEquals(headerInputAction.getText()))headerInputAction.setText(action);
        }
        if(headerConnection!=null){boolean connected=device!=null;headerConnection.setText("Подключение");headerConnection.setTextColor(page==1?headerAccent():connected?headerAccent():0xfff36b65);headerBluetooth.setImageResource(connected?R.drawable.ic_bluetooth:R.drawable.ic_bluetooth_off);headerBluetooth.setImageTintList(android.content.res.ColorStateList.valueOf(page==1?headerAccent():connected?headerAccent():0xfff36b65));headerBluetooth.setContentDescription(connected?"M30 подключён. Bluetooth":"M30 не подключён. Bluetooth перечёркнут");headerConnection.setContentDescription("Геймпад "+(activeSlot+1)+". "+(connected?device.getName()+" · ID "+device.getId():"Не подключён"));}
        if(headerBattery!=null){float capacity=batteryCapacity();headerBattery.setCapacity(capacity);headerPercent.setText(capacity<0?"—":Math.round(capacity*100)+"%");headerPercent.setContentDescription(batteryText());}
        if(modeResultView!=null){modeResultView.setVisibility(page!=1||device!=null?View.VISIBLE:View.GONE);modeResultView.setText(page!=1?modeSummaryText():modeStatusText());modeResultView.setTextColor(page!=1?(modeResult().candidate>=0?accent():MUTED):session.confirmation.current()==session.mode?GREEN:MUTED);}
        if(sourceHintView!=null){InputDevice pending=pendingSourceId<0?null:InputDevice.getDevice(pendingSourceId);
            sourceHintView.setVisibility(discoveringSource||pending!=null?View.VISIBLE:View.GONE);
            sourceHintView.setText(discoveringSource?"Нажмите A или B на M30. Внешнюю клавиатуру пока не нажимайте.":pending!=null?"Нажатие от «"+pending.getName()+"» · ID "+pending.getId()+". Выбрать его для проверки?":"Подключите M30 и выберите его кнопкой «Выбрать» или найдите нажатием.");
            useSourceButton.setVisibility(!discoveringSource&&pending!=null?View.VISIBLE:View.GONE);
        }
        if(page!=1&&lastCodeView!=null){InputMonitor.Key k=session.monitor.lastSelected;
            int visibility=session.lastWasAxis||k!=null?View.VISIBLE:View.GONE;
            lastCodeView.setVisibility(visibility);lastNameView.setVisibility(visibility);lastDetailsView.setVisibility(visibility);
            lastCodeView.setText(session.lastWasAxis?"AX "+session.lastAxis:k==null?"—":String.valueOf(k.code));lastNameView.setText(session.lastWasAxis?MotionEvent.axisToString(session.lastAxis):k==null?"Ожидание нажатия":k.name);
            if(session.lastWasAxis)lastDetailsView.setText(String.format(Locale.ROOT,"Значение %.3f · %s",session.lastAxisValue,session.lastAxisValue==0?"НЕЙТРАЛЬ":"АКТИВНА"));
            else lastDetailsView.setText(k==null?"Нажмите кнопку на выбранном устройстве. Неназначенные кнопки тоже появятся здесь.":(k.down?"↓ НАЖАТИЕ":"↑ ОТПУСКАНИЕ")+(k.repeat>0?" · повтор "+k.repeat:"")+" · scanCode "+k.scan);
        }
        if(otherLastView!=null){otherLastView.setText(!session.monitor.otherEnabled?"Монитор выключен":session.monitor.lastOther==null?"Ожидание другого устройства…":session.monitor.lastOther);otherLogView.setText(String.join("\n",session.monitor.otherEvents));}
        if(status!=null){status.setText(device==null?"○  Ожидание геймпада":gamepad(device)?"●  Геймпад подключён":"●  Источник ввода подключён");status.setTextColor(device==null?accent():GREEN);}
        if(details!=null)details.setText(device==null?"M30 пока не выбран":device.getName()+" · ID "+device.getId());
        if(batteryView!=null)batteryView.setText(batteryText());
        if(page!=1&&live!=null){ArrayList<String> labels=new ArrayList<>();for(String s:PadState.LABELS)if(session.state.active(s))labels.add(s);live.setVisibility(session.waiting!=null||!labels.isEmpty()?View.VISIBLE:View.GONE);live.setText(session.waiting!=null?"Назначение: "+session.waiting:!labels.isEmpty()?"Нажато: "+String.join(" + ",labels):"Нажмите любую кнопку на M30");}
        if(page!=1&&progress!=null)progress.setText("Проверено "+session.state.testedCount()+" / 16   ·   Нажатий: "+session.state.presses);
        if(pad!=null){pad.waiting=session.waiting;pad.setModelName(device==null?session.deviceName:device.getName());pad.setDpadInfo(session.dpad.caption(),null);if(headerDpadHelp!=null)headerDpadHelp.setText(ActionButton.dpadTitle(session.dpad));pad.invalidate();ArrayList<String> held=new ArrayList<>();for(String label:PadState.LABELS)if(session.state.active(label))held.add(label);pad.setContentDescription("Геймпад "+(activeSlot+1)+". "+(device==null?"Не подключён":device.getName()+" · ID "+device.getId())+". "+(held.isEmpty()?"Нет удерживаемых кнопок":"Нажато: "+String.join(" + ",held))+". "+session.dpad.summary()+". Нажмите подпись под крестовиной, чтобы открыть инструкцию переключения.");}
        if(bindHint!=null){bindHint.setVisibility(session.waiting==null?View.GONE:View.VISIBLE);if(session.waiting!=null)bindHint.setText("Нажмите физическую кнопку «"+session.waiting+"» на M30. Меняется только подсветка схемы.");}
        if(logView!=null)logView.setText(session.log.isEmpty()?"Ожидание ввода…":String.join("\n",session.log));
        if(rawView!=null){StringBuilder s=new StringBuilder(device==null?"Устройство не подключено":device.getName()+"\nVID/PID: "+device.getVendorId()+"/"+device.getProductId()+"\nSources: 0x"+Integer.toHexString(device.getSources()));
            s.append("\n").append(session.dpad.summary()).append("\n").append(session.dpad.evidence());
            s.append("\n").append(batteryText()).append("\nПрофиль (вручную): ").append(modeNames[session.mode]).append("\n").append(modeResult().title).append("\n").append(modeResult().evidence);
            if(session.lastInput>0)s.append("\nПоследний переход: ").append((SystemClock.elapsedRealtime()-session.lastInput)/1000).append(" с назад");
            s.append("\nАктивные коды: ").append(session.state.rawDown().isEmpty()?"—":String.join(", ",session.state.rawDown()));
            for(Map.Entry<String,Float> a:session.axes.entrySet())s.append(String.format(Locale.ROOT,"\n%s: %.3f",a.getKey(),a.getValue()));s.append("\n\nНазначения:");for(Map.Entry<String,String> b:session.state.bindings.entrySet())s.append("\n").append(b.getKey()).append(" → ").append(b.getValue());rawView.setText(s);}
    }
    private void refreshInputTest(){
        if(inputSourceView==null)return;
        InputMonitor.Key key=inputTest.lastKey;boolean axis=inputTest.lastWasAxis;
        String code=!inputTest.hasEvent()?"Код — · значение —":axis?String.format(Locale.ROOT,"AX %d · %s = %.3f",inputTest.lastAxis,inputTest.axisName,inputTest.axisValue):"Код "+key.code+" · "+key.name.replace("KEYCODE_","")+" = "+(key.down?"1":"0");
        headerInputLine.setText(code);headerInputAction.setText(!inputTest.hasEvent()?"Нажмите любую клавишу для проверки":axis?"Ось: "+(inputTest.axisValue==0?"нейтраль":"активна"):"Обычно в Android: "+ButtonGuide.keyAction(key.code));
        inputSourceView.setText(inputTest.hasEvent()?inputTest.deviceName()+" · ID "+inputTest.deviceId():"Источник: ожидание нажатия");
        lastCodeView.setText(!inputTest.hasEvent()?"—":axis?"AX "+inputTest.lastAxis:String.valueOf(key.code));
        lastNameView.setText(!inputTest.hasEvent()?"":axis?inputTest.axisName:key.name);
        lastDetailsView.setText(!inputTest.hasEvent()?"":axis?String.format(Locale.ROOT,"Значение %.3f",inputTest.axisValue):(key.down?"↓ НАЖАТИЕ":"↑ ОТПУСКАНИЕ")+(key.repeat>0?" · повтор "+key.repeat:"")+" · scanCode "+key.scan+" · source 0x"+Integer.toHexString(key.source));
        live.setVisibility(View.VISIBLE);live.setText(inputTest.heldText());
        ControllerSession shown=inputTest.hasEvent()?controllers.forDevice(inputTest.deviceId()):session;
        progress.setText(shown==null?"Нажатий на этом экране: "+inputTest.presses:"Джойстик "+(shown==controllers.slots[1]?2:1)+" · Проверено "+shown.state.testedCount()+" / 16   ·   Нажатий: "+shown.state.presses);
    }
    private float batteryCapacity(){
        ControllerTelemetry.Source source=ControllerTelemetry.battery(session.deviceId,telemetrySources(true));
        return source==null?-1:source.capacity;
    }
    private void showBatteryInfo(){
        String info=batteryText();
        if(session.connected()&&batteryCapacity()<0)info+="\n\nВ текущем подключении приложение не получило процент заряда от Android. Данные могут отличаться между режимами.\n\nДля проверки Windows / X-input выключите M30 (START 3 секунды), включите через X + START и переподключите Bluetooth. Если потребуется новая пара — PAIR 2 секунды. Прошивку повторять не нужно.";
        new FocusDialogBuilder(dialogContext()).setTitle("Заряд M30").setMessage(InstructionText.format(info)).setPositiveButton("Закрыть",null).show();
    }
    private String batteryText(){float capacity=batteryCapacity();return capacity<0?(device==null?"M30 не подключён":"Батарея геймпада: нет данных от Android"):"Батарея геймпада: "+Math.round(capacity*100)+"%";}
    private void showInlineBluetooth(){
        if(!inlineBluetoothOpen||page==1)return;if(widePanels!=null){showWideBluetooth();return;}if(pageBody==null||singleBluetoothHost!=null)return;
        if(inlineBluetooth==null){inlineBluetooth=new BluetoothSearchController(this,inlineSaved,true,slotAccent(),()->{closeInlineBluetooth();switchPage(1);},activeSlot,this::requestBluetoothController,connectionRequest::clear);inlineSaved=null;if(mainResumed)inlineBluetooth.resume();}inlineBluetooth.reserveHeader(0,0);
        singleBluetoothHost=column();singleBluetoothHost.setTag(R.id.navigation_key,"single:connection");singleBluetoothHost.setPadding(0,dp(5),0,0);LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);singleBluetoothHost.addView(heading,new LinearLayout.LayoutParams(-1,-2));TextView title=title("Bluetooth · Джойстик "+(activeSlot+1),14);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));heading.addView(connectionClose("single:connection:close"));if(inlineBluetooth.view().getParent() instanceof ViewGroup)((ViewGroup)inlineBluetooth.view().getParent()).removeView(inlineBluetooth.view());singleBluetoothHost.addView(inlineBluetooth.view(),new LinearLayout.LayoutParams(-1,0,1));int panelHeight=wholePageScroll?Math.max(dp(280),guideViewportHeight()):dp(280);pageBody.addView(singleBluetoothHost,new LinearLayout.LayoutParams(-1,panelHeight,1));FocusNavigation.prepare(singleBluetoothHost);wireRemoteFocus();
    }
    private void closeInlineBluetooth(){
        connectionRequest.clear();int keepY=inlineReturnScroll>=0?inlineReturnScroll:contentScroll==null?0:contentScroll.getScrollY();inlineReturnScroll=-1;inlineBluetoothOpen=false;inlineSaved=null;inlineHeight=0;
        if(inlineBluetooth!=null){View panel=inlineBluetooth.view();if(panel.getParent() instanceof ViewGroup)((ViewGroup)panel.getParent()).removeView(panel);inlineBluetooth.destroy();inlineBluetooth=null;}
        if(singleBluetoothHost!=null&&singleBluetoothHost.getParent() instanceof ViewGroup)((ViewGroup)singleBluetoothHost.getParent()).removeView(singleBluetoothHost);singleBluetoothHost=null;
        if(widePanels!=null){if(wideOverlay!=null&&wideOverlay.getParent() instanceof ViewGroup)((ViewGroup)wideOverlay.getParent()).removeView(wideOverlay);wideOverlay=null;for(ControllerPanelView panel:widePanels)panel.body.setVisibility(View.VISIBLE);}
        if(root!=null)wireRemoteFocus();if(mainResumed&&!isFinishing()&&!isDestroyed()){View anchor=widePanels!=null?widePanels[activeSlot].connect:bluetoothButton;if(anchor!=null&&anchor.isShown())anchor.requestFocus();}
        if(contentScroll!=null){ScrollView expected=contentScroll;expected.post(()->{if(contentScroll==expected)expected.scrollTo(0,keepY);});}
    }
    private void revealInlineBluetooth(){
        if(singleBluetoothHost==null||contentScroll==null)return;View target=singleBluetoothHost;ScrollView scroll=contentScroll;
        scroll.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){
            @Override public boolean onPreDraw(){
                if(scroll.getViewTreeObserver().isAlive())scroll.getViewTreeObserver().removeOnPreDrawListener(this);
                if(!inlineBluetoothOpen||singleBluetoothHost!=target||contentScroll!=scroll)return true;
                int[] panelPosition=new int[2],viewportPosition=new int[2];target.getLocationOnScreen(panelPosition);scroll.getLocationOnScreen(viewportPosition);int top=viewportPosition[1]+scroll.getPaddingTop(),bottom=viewportPosition[1]+scroll.getHeight()-scroll.getPaddingBottom();
                if(panelPosition[1]<top||panelPosition[1]+target.getHeight()>bottom)scroll.scrollTo(0,Math.max(0,scroll.getScrollY()+panelPosition[1]-top));
                return true;
            }
        });scroll.requestLayout();
    }
    private void toggleInlineBluetooth(){if(inlineBluetoothOpen)closeInlineBluetooth();else{inlineReturnScroll=contentScroll==null?0:contentScroll.getScrollY();inlineBluetoothOpen=true;showInlineBluetooth();revealInlineBluetooth();}}
    private void openBluetoothSearch(){if(page==1)switchPage(connectionPageId());if(!inlineBluetoothOpen){inlineReturnScroll=contentScroll==null?0:contentScroll.getScrollY();inlineBluetoothOpen=true;showInlineBluetooth();}revealInlineBluetooth();}
    private void openFullBluetoothSearch(){closeInlineBluetooth();render();startActivityForResult(new Intent(this,BluetoothControllerActivity.class),61);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==BluetoothSearchController.DEVICE_ACTION&&result==RESULT_OK&&data!=null&&BluetoothAliasActivity.CONNECT.equals(data.getStringExtra(BluetoothAliasActivity.OPERATION))&&connectionRequest.slot>=0&&connectionRequest.address.equals(data.getStringExtra("address")))connectionRequest.deadline=SystemClock.elapsedRealtime()+120_000;if(inlineBluetooth!=null)inlineBluetooth.activityResult(request,result,data);if(request==61&&result==RESULT_OK)switchPage(1);}
    private void bluetoothSettings(){try{startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));}catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
    private void bonded(){
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},7);return;}
        try{BluetoothManager manager=getSystemService(BluetoothManager.class);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
            if(adapter==null){toast("На устройстве нет Bluetooth. Можно использовать USB OTG.");return;}
            if(!adapter.isEnabled()){toast("Включите Bluetooth в настройках");bluetoothSettings();return;}
            ArrayList<String> names=new ArrayList<>();for(BluetoothDevice b:adapter.getBondedDevices())names.add((b.getName()==null?"Без имени":b.getName())+"\n"+b.getAddress());Collections.sort(names);
            new FocusDialogBuilder(dialogContext()).setTitle("Сопряжённые устройства").setMessage(names.isEmpty()?"Список пуст. Добавьте M30 через настройки Bluetooth.":String.join("\n\n",names)+"\n\nЭто сохранённые пары, а не список активных подключений.").setPositiveButton("Настройки",(d,w)->bluetoothSettings()).setNegativeButton("Закрыть",null).show();
        }catch(SecurityException e){toast("Для списка пар нужно разрешение «Устройства поблизости»");}
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] results){super.onRequestPermissionsResult(r,p,results);if(inlineBluetooth!=null)inlineBluetooth.permissionResult(r);if(r==7){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)bonded();else toast("Список пар недоступен. Тест кнопок и системные настройки работают без разрешения.");}}
    private void shareReport(){
        try{JSONObject out=new JSONObject();out.put("app","M30 Companion 1.37");out.put("time",new Date().toString());out.put("android",Build.VERSION.RELEASE);out.put("device",device==null?JSONObject.NULL:device.getName());out.put("controller_slot",activeSlot+1);out.put("profile_manually_selected",modeNames[session.mode]);out.put("mode_estimate",modeResult().title);out.put("mode_estimate_evidence",modeResult().evidence);out.put("mode_estimate_is_firmware_readout",false);out.put("mode_user_confirmed",session.confirmation.current()<0?JSONObject.NULL:modeNames[session.confirmation.current()]);out.put("mode_confirmation_source","user_statement_not_firmware_readout");out.put("dpad_observation",session.dpad.kind().name());out.put("dpad_observation_evidence",session.dpad.evidence());out.put("dpad_is_firmware_readout",false);out.put("test_navigation_locked",navigationLocked);out.put("other_device_monitor_enabled",session.monitor.otherEnabled);if(session.lastWasAxis){JSONObject axis=new JSONObject();axis.put("axis",session.lastAxis);axis.put("name",MotionEvent.axisToString(session.lastAxis));axis.put("value",session.lastAxisValue);axis.put("binding",session.lastAxisToken);out.put("last_axis",axis);}out.put("other_device_events",new JSONArray(session.monitor.otherEvents));
            JSONArray available=new JSONArray();for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null){JSONObject src=new JSONObject();src.put("id",id);src.put("name",d.getName());src.put("sources",d.getSources());src.put("vendor_id",d.getVendorId());src.put("product_id",d.getProductId());src.put("controller_candidate",SourceSelector.controller(sourceInfo(d)));available.put(src);}}out.put("available_input_sources",available);
            JSONArray telemetry=new JSONArray();for(ControllerTelemetry.Source source:telemetrySources(true)){JSONObject row=new JSONObject();row.put("id",source.id);row.put("name",source.name);row.put("vendor_id",source.vendor);row.put("product_id",source.product);row.put("battery_present",source.batteryPresent);row.put("battery_read_error",source.batteryError==null?JSONObject.NULL:source.batteryError);row.put("battery_capacity",Float.isFinite(source.capacity)?source.capacity:JSONObject.NULL);telemetry.put(row);}out.put("selected_controller_sources",telemetry);
            if(device!=null){out.put("device_id",device.getId());out.put("vendor_id",device.getVendorId());out.put("product_id",device.getProductId());}
            if(session.monitor.lastSelected!=null){InputMonitor.Key k=session.monitor.lastSelected;JSONObject last=new JSONObject();last.put("keycode",k.code);last.put("key_name",k.name);last.put("scancode",k.scan);last.put("device_id",k.deviceId);last.put("device_name",k.deviceName);last.put("action",k.down?"DOWN":"UP");last.put("source",k.source);last.put("repeat",k.repeat);out.put("last_key",last);}out.put("battery",batteryText());out.put("tested",session.state.testedCount());out.put("presses",session.state.presses);out.put("bindings",new JSONObject(session.state.bindings));out.put("counts",new JSONObject(session.state.counts));out.put("axes",new JSONObject(session.axes));out.put("events",new JSONArray(session.log));Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT,"Проверка 8BitDo M30").putExtra(Intent.EXTRA_TEXT,out.toString(2));startActivity(Intent.createChooser(send,"Отчёт тестирования"));}catch(JSONException|ActivityNotFoundException e){toast("Не удалось открыть отправку отчёта");}
    }
    private void clearSession(){session.clearTest();}
    private void devicesChanged(){if(chooseDevice()){if(page!=1)alignModeOnOpen();render();}else refreshLive();}
    @Override public void onInputDeviceAdded(int id){devicesChanged();}
    @Override public void onInputDeviceRemoved(int id){inputTest.removeDevice(id);devicesChanged();}
    @Override public void onInputDeviceChanged(int id){if(selected(id)){session.confirmation.observe(null);device=InputDevice.getDevice(id);}devicesChanged();}
    @Override protected void onResume(){super.onResume();mainResumed=true;if(inlineBluetooth!=null)inlineBluetooth.resume();updateTaskIcon();inputs.registerInputDeviceListener(this,handler);registered=true;devicesChanged();bluetoothNames.start();handler.removeCallbacks(ticker);handler.post(ticker);}
    @Override protected void onPause(){mainResumed=false;pagingKey=-1;bluetoothNames.stop();if(inlineBluetooth!=null)inlineBluetooth.pause();if(optionsDialog!=null)optionsDialog.dismiss();if(guideDialog!=null)guideDialog.dismiss();super.onPause();if(registered)inputs.unregisterInputDeviceListener(this);registered=false;handler.removeCallbacks(ticker);controllers.releaseAll();inputTest.release();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(!focus){pagingKey=-1;controllers.releaseAll();inputTest.release();refreshLive();}}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("connect_slot",connectionRequest.slot);out.putString("connect_address",connectionRequest.address);out.putLong("connect_deadline",connectionRequest.deadline);out.putBoolean("inline_open",inlineBluetoothOpen);out.putInt("inline_return_scroll",inlineReturnScroll);out.putInt("inline_owner",activeSlot);if(inlineBluetooth!=null)out.putBundle("inline_state",inlineBluetooth.save());}
    @Override public void onBackPressed(){if(inlineBluetoothOpen){closeInlineBluetooth();}else if(session.waiting!=null){session.waiting=null;if(pad!=null)pad.waiting=null;if(bindHint!=null)bindHint.setText("Назначение отменено.");refreshLive();}else if(dpadDialog!=null){closeDpadMenu();}else if(diagnosticsDialog!=null){closeDiagnostics();}else if(page==1){switchPage(connectionPageId());}else if(page==2){switchPage(0);}else super.onBackPressed();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
