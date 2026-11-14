package app.m30.companion;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.text.*;

/** Dependency-free on-device checks. Run on a disposable emulator. */
public final class SmokeInstrumentation extends Instrumentation {
    private Activity activity;
    private View find(View view,String text,boolean clickable){
        if(view instanceof TextView&&((TextView)view).getText().toString().contains(text)&&(!clickable||view.isClickable()))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),text,clickable);if(found!=null)return found;}}return null;
    }
    private void checkPage(int page){View tab=FocusNavigation.find(activity.getWindow().getDecorView(),"tab:"+page);if(tab==null||!tab.isSelected())throw new AssertionError("Unexpected selected page "+page);}
    private void check(String text){if(find(activity.getWindow().getDecorView(),text,false)==null)throw new AssertionError("Missing UI: "+text);}
    private void click(String text){View v=find(activity.getWindow().getDecorView(),text,true);if(v==null)throw new AssertionError("Missing action: "+text);v.performClick();}
    private PadView header(View view){
        if(view instanceof PadView)return (PadView)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){PadView pad=header(group.getChildAt(i));if(pad!=null)return pad;}}return null;
    }
    private void checkInstructionLayout(){
        String[] commands={"B + START","X + START","Y + START","A + START","SELECT + ↑","SELECT + ←","SELECT + →","L + R + START"};
        TextPaint paint=new TextPaint();paint.setTextSize(20);
        for(String command:commands){String text="Нажмите "+command+" и отпустите обе кнопки.";CharSequence formatted=InstructionText.format(text);StaticLayout layout=StaticLayout.Builder.obtain(formatted,0,formatted.length(),paint,155).build();int start=text.indexOf(command),end=start+command.length()-1;if(layout.getLineForOffset(start)!=layout.getLineForOffset(end))throw new AssertionError("Command wrapped: "+command);}
        BatteryIndicator battery=new BatteryIndicator(getTargetContext());battery.setCapacity(Float.NaN);if(!battery.getContentDescription().toString().contains("нет данных"))throw new AssertionError("Missing battery must remain unknown");battery.setCapacity(0);if(!battery.getContentDescription().toString().contains("0%"))throw new AssertionError("Empty battery is a real zero");
    }
    private void chooseMode(int mode){View choice=FocusNavigation.find(activity.getWindow().getDecorView(),"mode:"+mode);if(choice==null)throw new AssertionError("Missing mode choice");choice.performClick();}
    private void checkModeOnOpen(){
        try{
            java.lang.reflect.Field field=MainActivity.class.getDeclaredField("session");field.setAccessible(true);ControllerSession session=(ControllerSession)field.get(activity);
            java.lang.reflect.Method detect=MainActivity.class.getDeclaredMethod("modeResult");detect.setAccessible(true);int candidate=((ModeDetector.Result)detect.invoke(activity)).candidate;
            int manual=candidate==0?1:0;chooseMode(manual);
            java.lang.reflect.Method refresh=MainActivity.class.getDeclaredMethod("refreshLive");refresh.setAccessible(true);refresh.invoke(activity);
            if(session.mode!=manual)throw new AssertionError("Live refresh overwrote manual profile choice");
            click("Тест кнопок");click("Джойстик 1");
            if(session.mode!=(candidate>=0?candidate:manual))throw new AssertionError("Opening the controller page must select its inferred mode; unknown mode must preserve manual selection");
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private void checkHeader(){
        View decor=activity.getWindow().getDecorView();if(header(decor)==null)throw new AssertionError("Missing M30 header");
        boolean landscape=activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        View scroller=FocusNavigation.find(decor,"page:scroll-all");
        if(landscape){if(!(scroller instanceof ScrollView)||FocusNavigation.find(scroller,"header:pad")==null||FocusNavigation.find(scroller,"tab:0")==null)throw new AssertionError("Landscape header and tabs must share full-page scroll");}
        else if(scroller!=null)throw new AssertionError("Portrait header must remain fixed");
        if(!(field(activity,"joinedFrame") instanceof JoinedTabFrame))throw new AssertionError("Missing joined tab frame");Button settings=(Button)FocusNavigation.find(decor,"header:options");if(settings==null||settings.getCompoundDrawablesRelative()[0]==null)throw new AssertionError("Options must use the outlined vector icon");
        ViewGroup row=(ViewGroup)field(activity,"tabs");View region=(View)field(activity,"topRegion");
        if(region!=null&&screenBounds(row).bottom>screenBounds(region).top)throw new AssertionError("Tabs must precede controller header");
        for(int n=0;n<row.getChildCount();n++){View child=row.getChildAt(n);if(child.getTop()<0||child.getBottom()>row.getHeight())throw new AssertionError("Tab clipped by row");}
    }
    private void checkControllerPageAlignment(){
        if(activity.getResources().getConfiguration().orientation!=android.content.res.Configuration.ORIENTATION_LANDSCAPE)return;
        final int[] firstTop={0};runOnMainSync(()->{click("Джойстик 1");firstTop[0]=screenBounds((View)field(activity,"tabs")).top;((ScrollView)field(activity,"contentScroll")).scrollTo(0,48);click("Джойстик 2");});waitForIdleSync();
        runOnMainSync(()->{checkPage(2);if(((ScrollView)field(activity,"contentScroll")).getScrollY()!=0||screenBounds((View)field(activity,"tabs")).top!=firstTop[0])throw new AssertionError("Opening the peer must restore the top tabs");click("Джойстик 1");});waitForIdleSync();
        runOnMainSync(this::checkHeader);
    }
    private ButtonGuideView guide(View view){if(view instanceof ButtonGuideView)return (ButtonGuideView)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){ButtonGuideView found=guide(group.getChildAt(i));if(found!=null)return found;}}return null;}
    private ButtonGuideView openGuide(){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("showButtonGuide");m.setAccessible(true);m.invoke(activity);return guide(((Dialog)field(activity,"guideDialog")).getWindow().getDecorView());}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private void checkGuide(String text){ButtonGuideView diagram=openGuide();try{if(diagram==null||!diagram.getContentDescription().toString().contains(text))throw new AssertionError("Missing static guide: "+text);}finally{((Dialog)field(activity,"guideDialog")).dismiss();}}
    private void checkGuideSizing(){
        float density=activity.getResources().getDisplayMetrics().density;
        try{
            ButtonGuideView diagram=openGuide();
            java.lang.reflect.Field field=ButtonGuideView.class.getDeclaredField("captions");field.setAccessible(true);
            boolean found=false;
            for(Object caption:(java.util.List<?>)field.get(diagram)){
                java.lang.reflect.Field action=caption.getClass().getDeclaredField("action");action.setAccessible(true);
                StaticLayout layout=(StaticLayout)action.get(caption);
                if(layout.getText().toString().equals("Перемещение")){
                    found=true;if(layout.getLineCount()!=1)throw new AssertionError("Direction word must not lose its final letter to a second line");
                }
            }
            if(!found)throw new AssertionError("Direction caption missing");
            ((Dialog)field(activity,"guideDialog")).dismiss();
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        for(int[] viewport:new int[][]{{780,320},{1080,360},{1920,1080}}){
            int availableWidth=Math.round(viewport[0]*density),availableHeight=Math.round((viewport[1]-16)*density);
            ButtonGuideCard card=new ButtonGuideCard(activity,availableHeight);card.setPadding(Math.round(14*density),Math.round(12*density),Math.round(14*density),Math.round(12*density));
            String[] labels={"Кнопки в Android","По профилю: Windows · X-input","Статичная справка · назначения по профилю"};
            for(int i=0;i<3;i++){
                if(i==2)card.addView(new ButtonGuideView(activity,ButtonGuide.defaultBindings(1),1,0),new LinearLayout.LayoutParams(-1,-2));
                TextView label=new TextView(activity);label.setText(labels[i]);label.setTextSize(i==0?18:i==1?13:10);label.setPadding(0,Math.round(4*density),0,Math.round(4*density));card.addView(label,new LinearLayout.LayoutParams(-1,-2));
            }
            card.measure(View.MeasureSpec.makeMeasureSpec(availableWidth,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            if(card.getMeasuredHeight()>availableHeight||card.getMeasuredWidth()>=availableWidth)throw new AssertionError("Whole guide card must fit the landscape height with side margins");
        }
    }
    private boolean headerOnline(int slot){String description=header(activity.getWindow().getDecorView()).getContentDescription().toString();return description.startsWith("Геймпад "+slot+". ")&&description.contains(" · ID ");}
    private int headerDevice(int slot){String description=header(activity.getWindow().getDecorView()).getContentDescription().toString();java.util.regex.Matcher id=java.util.regex.Pattern.compile(" · ID (\\d+)\\.").matcher(description);if(!headerOnline(slot)||!id.find())throw new AssertionError("Missing connected controller "+slot);return Integer.parseInt(id.group(1));}
    private void checkDualControllers(){
        click("Джойстик 1");chooseMode(0);int first=headerDevice(1);key(first,KeyEvent.ACTION_DOWN,96);checkHeld("Нажато: A");
        click("Джойстик 2");chooseMode(0);int second=headerDevice(2);if(first==second)throw new AssertionError("Slots share the same source");key(second,KeyEvent.ACTION_DOWN,97);checkHeld("Нажато: B");
        key(first,KeyEvent.ACTION_UP,96);checkHeld("Нажато: B");click("Джойстик 1");checkHeld("Нет удерживаемых кнопок");click("Тест кнопок");check("Джойстик 1");check("Проверено 1 / 16");
        swipe(false);check("Подключение M30");checkHeld("Нажато: B");swipe(false);checkHeld("Нет удерживаемых кнопок");swipe(true);checkHeld("Нажато: B");swipe(true);check("Джойстик 2");check("Проверено 1 / 16");key(second,KeyEvent.ACTION_UP,97);checkHeld("Нет удерживаемых кнопок");
        try{
            java.lang.reflect.Field enabled=MainActivity.class.getDeclaredField("autoFollowControllers");enabled.setAccessible(true);boolean prior=enabled.getBoolean(activity);enabled.setBoolean(activity,true);
            ControllerSession secondSession=((ControllerBank)field(activity,"controllers")).slots[1];int priorProfile=secondSession.mode;
            try{
                int manualProfile=secondSession.mode==1?0:1;secondSession.mode=manualProfile;
                key(first,KeyEvent.ACTION_DOWN,99);checkPage(0);checkHeld("Нажато: X");
                key(second,KeyEvent.ACTION_DOWN,100);checkPage(2);checkHeld("Нажато: Y");
                if(secondSession.mode!=manualProfile)throw new AssertionError("Automatic page following must not replace the manually selected profile or clear its first press");
                key(first,KeyEvent.ACTION_UP,99);checkPage(2);
                hat(first,1,0);checkPage(0);hat(first,0,0);key(second,KeyEvent.ACTION_UP,100);checkPage(0);
                click("Тест кнопок");key(second,KeyEvent.ACTION_DOWN,97);check("Тест кнопок");key(second,KeyEvent.ACTION_UP,97);
            }finally{secondSession.mode=priorProfile;enabled.setBoolean(activity,prior);}
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private View description(View view,String prefix){
        if(view.getContentDescription()!=null&&view.getContentDescription().toString().startsWith(prefix))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=description(group.getChildAt(i),prefix);if(found!=null)return found;}}return null;
    }
    private void checkHeld(String labels){
        View root=activity.getWindow().getDecorView();View display=header(root);if(display==null)display=description(root,"Панель кнопок.");
        if(display==null||!display.getContentDescription().toString().contains(labels))throw new AssertionError("Selected controller input display does not show held keys: "+labels);
    }
    private void checkControllerCount(int expected){
        try{java.lang.reflect.Field field=MainActivity.class.getDeclaredField("session");field.setAccessible(true);ControllerSession current=(ControllerSession)field.get(activity);if(current.state.testedCount()!=expected)throw new AssertionError("Foreign event changed controller test count");}catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private void checkControllerTools(){
        check("Опции");check("Клавиши");checkHeader();click("Опции");Dialog menu=(Dialog)field(activity,"optionsDialog");if(menu==null)throw new AssertionError("Missing Options");View root=menu.getWindow().getDecorView();
        if(find(root,"Дополнительно",true)!=null)throw new AssertionError("Options cannot contain another Additional menu");
        for(String label:new String[]{"Исправить подсветку кнопок","Сбросить проверку","Выбрать M30","Найти M30 по нажатию","Диагностика","Справка M30","Инструкция выбранного режима","Поиск геймпадов Bluetooth","Автопереход по нажатию:","Прошивка · обновление USB"})if(find(root,label,false)==null)throw new AssertionError("Missing direct option: "+label);
        menu.dismiss();
    }
    private void checkButtonsPage(){
        View root=activity.getWindow().getDecorView();if(header(root)!=null||guide(root)!=null)throw new AssertionError("Buttons page must not contain a controller drawing");
        if(find(root,"Опции",false)!=null||find(root,"Прошивка ·",false)!=null)throw new AssertionError("Controller tools must only be on joystick pages");
        if(description(root,"Панель кнопок.")==null)throw new AssertionError("Missing independent physical button panel");
    }
    private void checkOverlays(){
        try{
            View root=activity.getWindow().getDecorView();ScrollView mainScroll;
            java.lang.reflect.Field scrollField=MainActivity.class.getDeclaredField("contentScroll");scrollField.setAccessible(true);mainScroll=(ScrollView)scrollField.get(activity);int y=mainScroll.getScrollY();
            if(header(root)==null)throw new AssertionError("Header missing before D-pad popup");FocusNavigation.find(root,"header:dpad-info").performClick();
            java.lang.reflect.Field field=MainActivity.class.getDeclaredField("dpadDialog");field.setAccessible(true);Dialog dpad=(Dialog)field.get(activity);
            if(dpad==null||!dpad.isShowing()||find(dpad.getWindow().getDecorView(),"SELECT + ↑",false)==null)throw new AssertionError("D-pad caption must open combinations in an overlay");
            if(find(dpad.getWindow().getDecorView(),"На включённом M30",false)==null||find(dpad.getWindow().getDecorView(),"рядом с PAIR",false)==null)throw new AssertionError("D-pad help needs power state and exact confirmation LED");
            if(find(root,"Крестовина M30",false)!=null||mainScroll.getScrollY()!=y)throw new AssertionError("D-pad popup changed main page content or scroll");dpad.dismiss();
            PadView diagram=header(root);float scale=Math.min(diagram.getWidth()/1000f,diagram.getHeight()/540f);float x=(diagram.getWidth()-1000*scale)/2+220*scale,yTap=(diagram.getHeight()-540*scale)/2+436*scale;long now=SystemClock.uptimeMillis();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent tap=MotionEvent.obtain(now,now,action,x,yTap,0);diagram.onTouchEvent(tap);tap.recycle();}
            dpad=(Dialog)field.get(activity);if(dpad==null||!dpad.isShowing())throw new AssertionError("Touching physical D-pad caption must open the combinations");dpad.dismiss();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_UP}){MotionEvent tap=MotionEvent.obtain(now,now,action,x,yTap,0);diagram.onTouchEvent(tap);tap.recycle();}
            if(field.get(activity)!=null)throw new AssertionError("Cancelled swipe must not open D-pad combinations");
            java.lang.reflect.Method open=MainActivity.class.getDeclaredMethod("showDiagnostics");open.setAccessible(true);open.invoke(activity);
            field=MainActivity.class.getDeclaredField("diagnosticsDialog");field.setAccessible(true);Dialog diagnostics=(Dialog)field.get(activity);
            if(diagnostics==null||!diagnostics.isShowing()||find(diagnostics.getWindow().getDecorView(),"Поделиться отчётом",true)==null)throw new AssertionError("Diagnostics must have its own window and report action");
            if(find(root,"Блокировать навигацию кнопками",false)!=null||mainScroll.getScrollY()!=y)throw new AssertionError("Diagnostics must not be added below the main page");
            for(int id:InputDevice.getDeviceIds()){InputDevice input=InputDevice.getDevice(id);if(input==null||!"M30 Virtual Test".equals(input.getName()))continue;
                java.lang.reflect.Field selected=MainActivity.class.getDeclaredField("controllers");selected.setAccessible(true);ControllerSession target=((ControllerBank)selected.get(activity)).forDevice(id);if(target==null)continue;
                long t=SystemClock.uptimeMillis();diagnostics.dispatchKeyEvent(new KeyEvent(t,t,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BUTTON_16,0,0,id,0,0,InputDevice.SOURCE_GAMEPAD));
                if(target.monitor.lastSelected==null||target.monitor.lastSelected.code!=KeyEvent.KEYCODE_BUTTON_16)throw new AssertionError("Modal window swallowed live controller input");
                diagnostics.dispatchKeyEvent(new KeyEvent(t,t,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BUTTON_16,0,0,id,0,0,InputDevice.SOURCE_GAMEPAD));
                if(target.state.down.contains("K:"+KeyEvent.KEYCODE_BUTTON_16))throw new AssertionError("Modal window swallowed controller release");
            }
            diagnostics.dismiss();if(field.get(activity)!=null)throw new AssertionError("Dismiss must release the diagnostic window");
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private void remoteKey(int code){sendKeyDownUpSync(code);waitForIdleSync();}
    private void checkRemoteNavigation(){
        runOnMainSync(()->{
            click("Тест кнопок");View tab=FocusNavigation.find(activity.getWindow().getDecorView(),"tab:0");
            if(tab==null||!tab.isFocusable()||tab.getForeground()==null)throw new AssertionError("Tab missing TV focus treatment");
            tab.requestFocusFromTouch();
        });waitForIdleSync();remoteKey(KeyEvent.KEYCODE_DPAD_CENTER);
        runOnMainSync(()->{check("Подключение M30");FocusNavigation.find(activity.getWindow().getDecorView(),"mode:1").requestFocusFromTouch();});
        waitForIdleSync();remoteKey(KeyEvent.KEYCODE_DPAD_CENTER);
        runOnMainSync(()->{
            check("Windows · X-input");if(!"mode:1".equals(FocusNavigation.key(activity.getCurrentFocus())))throw new AssertionError("Mode change lost remote focus");
            click("Тест кнопок");key(-42,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_F1);checkLargeCode(KeyEvent.KEYCODE_F1);key(-42,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_F1);FocusNavigation.find(activity.getWindow().getDecorView(),"tab:1").requestFocusFromTouch();
        });waitForIdleSync();remoteKey(KeyEvent.KEYCODE_DPAD_LEFT);
        runOnMainSync(()->{
            checkPage(2);click("Тест кнопок");checkLargeCode(KeyEvent.KEYCODE_DPAD_LEFT);click("Джойстик 1");check("После PAIR: один постоянный огонёк");
            try{
                java.lang.reflect.Method open=MainActivity.class.getDeclaredMethod("showDpadMenu");open.setAccessible(true);open.invoke(activity);
                java.lang.reflect.Field field=MainActivity.class.getDeclaredField("dpadDialog");field.setAccessible(true);Dialog dialog=(Dialog)field.get(activity);
                View close=find(dialog.getWindow().getDecorView(),"Закрыть",true);close.requestFocusFromTouch();long now=SystemClock.uptimeMillis();
                for(int action:new int[]{KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP})dialog.dispatchKeyEvent(new KeyEvent(now,now,action,KeyEvent.KEYCODE_DPAD_CENTER,0,0,-42,0,0,InputDevice.SOURCE_DPAD));
            }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        });waitForIdleSync();
        runOnMainSync(()->{try{java.lang.reflect.Field field=MainActivity.class.getDeclaredField("dpadDialog");field.setAccessible(true);if(field.get(activity)!=null)throw new AssertionError("Remote OK could not close popup");chooseMode(0);}catch(ReflectiveOperationException error){throw new AssertionError(error);}});
    }
    private android.graphics.Rect bluetoothBounds,tabBounds;
    private ScrollView originalScroll;
    private int originalScrollY;
    private BluetoothSearchController originalBluetooth;
    private void checkBluetoothToggleAndCount(BluetoothDeviceList list){
        try{
            View toggle=FocusNavigation.find(originalBluetooth.view(),"bt:search");
            if(toggle==null||FocusNavigation.find(originalBluetooth.view(),"bt:stop")!=null)throw new AssertionError("Search/stop must be one stable control");
            java.lang.reflect.Field owns=BluetoothSearchController.class.getDeclaredField("ownsDiscovery");owns.setAccessible(true);
            java.lang.reflect.Method update=BluetoothSearchController.class.getDeclaredMethod("updateControls");update.setAccessible(true);
            try{
                owns.setBoolean(originalBluetooth,true);update.invoke(originalBluetooth);
                if(!((TextView)toggle).getText().toString().equals("Остановить поиск")||FocusNavigation.find(originalBluetooth.view(),"bt:search")!=toggle)throw new AssertionError("Same search button must become Stop");
            }finally{owns.setBoolean(originalBluetooth,false);update.invoke(originalBluetooth);}
            if(!((TextView)toggle).getText().toString().equals("Начать поиск"))throw new AssertionError("Stopped discovery must restore Start text");
            TextView heading=(TextView)field(originalBluetooth,"savedHeading");if(!heading.getText().toString().equals("Ранее подключённые ("+list.section(true).size()+")"))throw new AssertionError("Saved count must count saved rows, including found bond only once");
            ScrollView scroll=(ScrollView)FocusNavigation.find(originalBluetooth.view(),"bt:saved-scroll");
            if(scroll==null||!scroll.isVerticalScrollBarEnabled()||scroll.getVerticalScrollbarPosition()!=View.SCROLLBAR_POSITION_LEFT||scroll.isScrollbarFadingEnabled())throw new AssertionError("Saved list needs persistent left position scrollbar");
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private void checkBluetoothDeviceMenu(BluetoothDeviceList.Entry entry){
        ActivityMonitor monitor=addMonitor(new android.content.IntentFilter(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS),null,true);
        try{
            java.lang.reflect.Method show=BluetoothSearchController.class.getDeclaredMethod("showDeviceMenu",BluetoothDeviceList.Entry.class,android.bluetooth.BluetoothDevice.class,boolean.class);show.setAccessible(true);
            for(String command:new String[]{"Отключить","Забыть устройство"}){
                show.invoke(originalBluetooth,entry,null,true);AlertDialog dialog=(AlertDialog)field(originalBluetooth,"deviceMenu");View menu=dialog.getWindow().getDecorView();
                for(String label:new String[]{"Подключить устройство","Переименовать","Отключить","Забыть устройство"}){
                    View action=find(menu,label,true);if(action==null||!action.isFocusable()||action.getForeground()==null)throw new AssertionError("Missing framed device action: "+label);
                }
                int before=monitor.getHits();find(menu,command,true).performClick();
                if(monitor.getHits()!=before+1||dialog.isShowing())throw new AssertionError("Device action must close menu and open Android Bluetooth");
                TextView status=(TextView)FocusNavigation.find(originalBluetooth.view(),"bt:status");
                if(!entry.saved()||!status.getText().toString().contains(command))throw new AssertionError("Delegated action cannot pretend the system pair was removed/disconnected");
            }
            show.invoke(originalBluetooth,entry,null,true);
            AlertDialog menu=(AlertDialog)field(originalBluetooth,"deviceMenu");String systemName=entry.displayName();
            java.lang.reflect.Method localEditor=BluetoothSearchController.class.getDeclaredMethod("showLabelEditor",BluetoothDeviceList.Entry.class);localEditor.setAccessible(true);localEditor.invoke(originalBluetooth,entry);AlertDialog editor=(AlertDialog)field(originalBluetooth,"labelEditor");
            ((EditText)FocusNavigation.find(editor.getWindow().getDecorView(),"bt:label-title")).setText("Windows TV");((EditText)FocusNavigation.find(editor.getWindow().getDecorView(),"bt:label-number")).setText("3");find(editor.getWindow().getDecorView(),"Сохранить",true).performClick();
            TextView row=(TextView)FocusNavigation.find(originalBluetooth.view(),"bt:saved|"+entry.address);
            if(!row.getText().toString().startsWith("[3] Windows TV")||!entry.displayName().equals(systemName))throw new AssertionError("App label must update row without changing Android alias");
            show.invoke(originalBluetooth,entry,null,true);menu=(AlertDialog)field(originalBluetooth,"deviceMenu");localEditor.invoke(originalBluetooth,entry);editor=(AlertDialog)field(originalBluetooth,"labelEditor");
            ((EditText)FocusNavigation.find(editor.getWindow().getDecorView(),"bt:label-number")).setText("42");find(editor.getWindow().getDecorView(),"Сохранить",true).performClick();
            if(!row.getText().toString().startsWith("[42] Windows TV"))throw new AssertionError("Custom badge number was not saved");
            ((BluetoothControllerNames)field(originalBluetooth,"controllerNames")).saveLabel(entry.address,"",0);
            show.invoke(originalBluetooth,entry,null,true);
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        finally{removeMonitor(monitor);}
    }
    private android.graphics.Rect screenBounds(View v){int[] xy=new int[2];v.getLocationOnScreen(xy);return new android.graphics.Rect(xy[0],xy[1],xy[0]+v.getWidth(),xy[1]+v.getHeight());}
    private Object field(Object owner,String name){try{java.lang.reflect.Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private void unchangedOverlayGeometry(){
        View root=activity.getWindow().getDecorView();
        if(!bluetoothBounds.equals(screenBounds(FocusNavigation.find(root,"bluetooth:toggle")))||!tabBounds.equals(screenBounds(FocusNavigation.find(root,"tab:0"))))throw new AssertionError("Bluetooth overlay moved anchor or lower tabs");
        if(field(activity,"contentScroll")!=originalScroll||originalScrollY!=originalScroll.getScrollY())throw new AssertionError("Bluetooth overlay recreated or moved lower scroll");
    }
    private void checkInlineBluetooth(){
        runOnMainSync(()->{click("Джойстик 1");});waitForIdleSync();
        runOnMainSync(()->{
            checkPage(0);check("START + B");check("START + X");check("START + A");check("START + Y");
            View root=activity.getWindow().getDecorView();View bt=FocusNavigation.find(root,"bluetooth:toggle"),choose=FocusNavigation.find(root,"header:choose"),battery=FocusNavigation.find(root,"header:battery"),pad=FocusNavigation.find(root,"header:pad");
            for(View action:new View[]{bt,choose,battery,pad})if(action==null||!action.isFocusable()||action.getForeground()==null)throw new AssertionError("Header action unreachable/unframed");
            if(choose.getNextFocusUpId()!=bt.getId()||battery.getNextFocusDownId()!=pad.getId()||bt.getNextFocusRightId()!=pad.getId()||pad.getNextFocusLeftId()!=bt.getId())throw new AssertionError("Header focus routes missing");
            originalScroll=(ScrollView)field(activity,"contentScroll");originalScrollY=originalScroll.getScrollY();bluetoothBounds=screenBounds(bt);tabBounds=screenBounds(FocusNavigation.find(root,"tab:0"));bt.performClick();
        });waitForIdleSync();
        runOnMainSync(()->{
            unchangedOverlayGeometry();check("Начать поиск");check("Системные настройки Bluetooth");check("Ранее подключённые");check("Найдены рядом");
            originalBluetooth=(BluetoothSearchController)field(activity,"inlineBluetooth");
            View close=FocusNavigation.find(activity.getWindow().getDecorView(),"single:connection:close");if(!(close instanceof ImageButton)||close.getLayoutParams().width!=close.getLayoutParams().height||!"Закрыть подключение".contentEquals(close.getContentDescription()))throw new AssertionError("Connection close must be a centred square accessible icon");if(FocusNavigation.find(originalBluetooth.view(),"bt:compatibility")==null)throw new AssertionError("Compatibility controls missing");
            android.content.SharedPreferences mainPrefs=activity.getPreferences(0),linkedPrefs=(android.content.SharedPreferences)field(field(originalBluetooth,"controllerNames"),"slots");String probe="instrumentation.preview.prefs";mainPrefs.edit().putString(probe,"same-main-store").commit();if(!linkedPrefs.getString(probe,"").equals("same-main-store"))throw new AssertionError("Controller labels must use the actual MainActivity preferences in either package");mainPrefs.edit().remove(probe).commit();
            TextView btStatus=(TextView)FocusNavigation.find(originalBluetooth.view(),"bt:status");if(btStatus.getEllipsize()!=null||btStatus.getMaxLines()<=2||btStatus.getTextSize()<14*activity.getResources().getDisplayMetrics().scaledDensity)throw new AssertionError("Bluetooth status must wrap without clipping or tiny text");
            if(originalBluetooth==null||originalBluetooth.searching())throw new AssertionError("Overlay must open without automatic discovery");
            if(originalBluetooth.view().getParent()!=field(activity,"singleBluetoothHost")||!header(activity.getWindow().getDecorView()).isShown()||((View)field(activity,"content")).getVisibility()!=View.VISIBLE)throw new AssertionError("Portrait/single Connection must append below all existing blocks");if(((View)field(activity,"singleBluetoothHost")).getTop()<((View)field(activity,"content")).getBottom())throw new AssertionError("Bluetooth must be below the instruction card");
            LinearLayout columns=(LinearLayout)FocusNavigation.find(originalBluetooth.view(),"bt:columns");if(columns==null||columns.getOrientation()!=LinearLayout.HORIZONTAL||columns.getChildCount()!=2)throw new AssertionError("Need two independent columns");
            BluetoothDeviceList list=(BluetoothDeviceList)field(originalBluetooth,"devices");String address="AA:BB:CC:DD:EE:FF";list.offer(address,"M30 test",BluetoothGamepadFilter.M30,12,true);
            try{java.lang.reflect.Method refresh=BluetoothSearchController.class.getDeclaredMethod("refreshRows");refresh.setAccessible(true);refresh.invoke(originalBluetooth);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
            View savedRow=FocusNavigation.find(originalBluetooth.view(),"bt:saved|"+address),foundRow=FocusNavigation.find(originalBluetooth.view(),"bt:found|"+address);
            if(savedRow==null||foundRow==null||!savedRow.isLongClickable()||!foundRow.isLongClickable())throw new AssertionError("Both columns must keep discovered bond and support long press");
            checkBluetoothToggleAndCount(list);
            TextView renameHint=(TextView)FocusNavigation.find(originalBluetooth.view(),"bt:rename-hint");if(renameHint==null||renameHint.getEllipsize()!=null||!renameHint.getText().toString().contains("переименовать"))throw new AssertionError("Missing complete rename hint in saved column");
            checkBluetoothDeviceMenu(list.get(address));AlertDialog deviceMenu=(AlertDialog)field(originalBluetooth,"deviceMenu");
            activity.onBackPressed();if(field(activity,"inlineBluetooth")!=null||deviceMenu.isShowing())throw new AssertionError("Back must close overlay and its device menu");
        });waitForIdleSync();
        runOnMainSync(()->{
            unchangedOverlayGeometry();FocusNavigation.find(activity.getWindow().getDecorView(),"bluetooth:toggle").performClick();originalBluetooth=(BluetoothSearchController)field(activity,"inlineBluetooth");
            try{java.lang.reflect.Method render=MainActivity.class.getDeclaredMethod("render");render.setAccessible(true);render.invoke(activity);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        });waitForIdleSync();
        runOnMainSync(()->{
            if(field(activity,"inlineBluetooth")!=originalBluetooth||originalBluetooth.view().getParent()==null)throw new AssertionError("Render replaced or lost Bluetooth controller");
            activity.onBackPressed();FocusNavigation.find(activity.getWindow().getDecorView(),"tab:0").requestFocusFromTouch();
        });waitForIdleSync();remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        runOnMainSync(()->checkPage(2));remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        runOnMainSync(()->check("Тест кнопок"));remoteKey(KeyEvent.KEYCODE_DPAD_LEFT);
        runOnMainSync(()->checkPage(2));remoteKey(KeyEvent.KEYCODE_DPAD_LEFT);
        runOnMainSync(()->{checkPage(0);FocusNavigation.find(activity.getWindow().getDecorView(),"header:choose").requestFocusFromTouch();});
        remoteKey(KeyEvent.KEYCODE_DPAD_UP);
        runOnMainSync(()->{if(!"bluetooth:toggle".equals(FocusNavigation.key(activity.getCurrentFocus())))throw new AssertionError("Choose UP must reach Connection");checkPage(0);});
        remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        runOnMainSync(()->{if(!"header:pad".equals(FocusNavigation.key(activity.getCurrentFocus())))throw new AssertionError("Header RIGHT must focus pad, not change page");checkPage(0);});
        remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        runOnMainSync(()->{checkPage(2);FocusNavigation.find(activity.getWindow().getDecorView(),"header:choose").requestFocusFromTouch();});
        remoteKey(KeyEvent.KEYCODE_DPAD_LEFT);
        runOnMainSync(()->{checkPage(0);FocusNavigation.find(activity.getWindow().getDecorView(),"mode:0").requestFocusFromTouch();});
        remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        runOnMainSync(()->{
            checkPage(0);if(!"mode:1".equals(FocusNavigation.key(activity.getCurrentFocus())))throw new AssertionError("Interior profile arrow must move focus before paging");
            FocusNavigation.find(activity.getWindow().getDecorView(),"bluetooth:toggle").performClick();
        });waitForIdleSync();
        runOnMainSync(()->{FocusNavigation.find(activity.getWindow().getDecorView(),"bt:settings").requestFocusFromTouch();});
        remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        runOnMainSync(()->{
            checkPage(2);if(field(activity,"inlineBluetooth")!=null)throw new AssertionError("Edge paging must clean up Bluetooth overlay");
            long now=SystemClock.uptimeMillis();
            activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT,0,0,-42,0,0,InputDevice.SOURCE_DPAD));
            for(int repeat=1;repeat<4;repeat++)activity.dispatchKeyEvent(new KeyEvent(now,now+repeat,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT,repeat,0,-42,0,0,InputDevice.SOURCE_DPAD));
            activity.dispatchKeyEvent(new KeyEvent(now,now+4,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_RIGHT,0,0,-42,0,0,InputDevice.SOURCE_DPAD));check("Тест кнопок");
            click("Джойстик 1");
        });waitForIdleSync();
    }
    private boolean largeCode(View view,int code){
        if(view instanceof TextView&&((TextView)view).getText().toString().equals(String.valueOf(code))&&((TextView)view).getTextSize()>=40*getTargetContext().getResources().getDisplayMetrics().scaledDensity)return true;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(largeCode(group.getChildAt(i),code))return true;}return false;
    }
    private void checkLargeCode(int code){if(!largeCode(activity.getWindow().getDecorView(),code))throw new AssertionError("Missing prominent code: "+code);}

    private void key(int id,int action,int code){long t=SystemClock.uptimeMillis();activity.dispatchKeyEvent(new KeyEvent(t,t,action,code,0,0,id,0,0,InputDevice.SOURCE_GAMEPAD));}
    private void swipe(boolean left){
        float width=getTargetContext().getResources().getDisplayMetrics().widthPixels,y=100*getTargetContext().getResources().getDisplayMetrics().density;
        long t=SystemClock.uptimeMillis();float from=width*(left?.85f:.15f),to=width*(left?.15f:.85f);
        int[] actions={MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP};float[] x={from,to,to};
        for(int i=0;i<3;i++){MotionEvent e=MotionEvent.obtain(t,t+i*30,actions[i],x[i],y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);activity.dispatchTouchEvent(e);e.recycle();}
    }
    private void hat(int id,float x,float y){
        long t=SystemClock.uptimeMillis();MotionEvent.PointerProperties p=new MotionEvent.PointerProperties();p.id=0;MotionEvent.PointerCoords c=new MotionEvent.PointerCoords();c.setAxisValue(MotionEvent.AXIS_HAT_X,x);c.setAxisValue(MotionEvent.AXIS_HAT_Y,y);
        MotionEvent e=MotionEvent.obtain(t,t,MotionEvent.ACTION_MOVE,1,new MotionEvent.PointerProperties[]{p},new MotionEvent.PointerCoords[]{c},0,0,1,1,id,0,InputDevice.SOURCE_JOYSTICK,0);activity.dispatchGenericMotionEvent(e);e.recycle();
    }
    private void checkWideWorkspace(Bundle report){
        final ControllerPanelView[] panes=(ControllerPanelView[])field(activity,"widePanels");
        if(panes==null||panes.length!=2)throw new AssertionError("Missing paired controller panes");
        runOnMainSync(()->{for(int slot=0;slot<2;slot++){if(panes[slot].body.getVisibility()!=View.VISIBLE)throw new AssertionError("Both controllers must initially be visible");if(panes[slot].pad==panes[1-slot].pad)throw new AssertionError("Shared controller view");}panes[0].connect.performClick();});waitForIdleSync();
        runOnMainSync(()->{ControllerPanelView[] now=(ControllerPanelView[])field(activity,"widePanels");View connection=FocusNavigation.find(activity.getWindow().getDecorView(),"wide:connection");if(connection==null||connection.getParent()!=now[1]||now[0].body.getVisibility()!=View.VISIBLE||now[1].body.getVisibility()!=View.INVISIBLE)throw new AssertionError("First Connection must cover second panel only");now[0].connect.performClick();});waitForIdleSync();
        runOnMainSync(()->{ControllerPanelView[] now=(ControllerPanelView[])field(activity,"widePanels");if(now[1].body.getVisibility()!=View.VISIBLE)throw new AssertionError("Peer did not return after close");now[1].connect.performClick();});waitForIdleSync();
        runOnMainSync(()->{ControllerPanelView[] now=(ControllerPanelView[])field(activity,"widePanels");View connection=FocusNavigation.find(activity.getWindow().getDecorView(),"wide:connection");if(connection==null||connection.getParent()!=now[0]||now[1].body.getVisibility()!=View.VISIBLE)throw new AssertionError("Second Connection must cover first panel only");FocusNavigation.find(activity.getWindow().getDecorView(),"wide:connection:close").performClick();checkControllerToolsWide();});
        report.putString("wide_layout","PASS: two independent panes, opposite Connection in both directions, close restoration and tools");
    }
    private void checkControllerToolsWide(){ControllerPanelView[] panes=(ControllerPanelView[])field(activity,"widePanels");panes[1].options.performClick();Dialog options=(Dialog)field(activity,"optionsDialog");if(options==null||!options.isShowing())throw new AssertionError("Wide Options inaccessible");options.dismiss();panes=((ControllerPanelView[])field(activity,"widePanels"));panes[1].keys.performClick();Dialog keys=(Dialog)field(activity,"guideDialog");if(keys==null||guide(keys.getWindow().getDecorView())==null)throw new AssertionError("Wide button reference inaccessible");keys.dismiss();}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle report=new Bundle();try{
        activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        if(field(activity,"widePanels")!=null){checkWideWorkspace(report);return;}
        runOnMainSync(()->{check("Версия ");check("Подключение M30");if(header(activity.getWindow().getDecorView()).getContentDescription().toString().contains("ID -1"))throw new AssertionError("Virtual Android source shown as a connected controller");checkHeader();checkInstructionLayout();check("Android");check("Windows");check("Switch");chooseMode(1);});waitForIdleSync();
        runOnMainSync(()->{check("X + START");checkGuide("C: —");checkGuideSizing();if(find(activity.getWindow().getDecorView(),"B + START",false)!=null)throw new AssertionError("Windows instructions show stale Android combination");chooseMode(2);checkGuide("★: Снимок на Switch");chooseMode(0);checkGuide("★: Turbo");});waitForIdleSync();
        runOnMainSync(()->{
            check("B + START");checkModeOnOpen();
            click("Джойстик 1");checkControllerTools();checkOverlays();boolean firstOnline=headerOnline(1);
            click("Джойстик 2");checkControllerTools();boolean secondOnline=headerOnline(2);
            if(firstOnline&&secondOnline){checkDualControllers();report.putString("virtual_input","PASS: separate controller state, hidden release, both controller pages and Buttons via swipes");return;}
            if(!secondOnline){
                check("Подключение M30");check("Android");check("Windows");check("macOS");check("Switch");check("START + B");check("Клавиши");
            }
            click("Джойстик 1");chooseMode(0);click("Тест кнопок");checkButtonsPage();check("Проверено 0 / 16");
            swipe(true);checkButtonsPage();check("Проверено 0 / 16");
            key(-42,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BUTTON_A);checkLargeCode(KeyEvent.KEYCODE_BUTTON_A);checkControllerCount(0);key(-42,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BUTTON_A);
            InputDevice virtual=null;for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null&&d.getName().equals("M30 Virtual Test"))virtual=d;}
            if(virtual!=null){
                int id=virtual.getId();key(id,KeyEvent.ACTION_DOWN,96);key(id,KeyEvent.ACTION_DOWN,97);checkHeld("A + B");checkLargeCode(97);check("Код 97 · BUTTON_B = 1");check("Обычно в Android:");check("Назад / отмена");
                key(-42,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_F1);checkLargeCode(KeyEvent.KEYCODE_F1);check("Нажатий на этом экране:");checkControllerCount(2);key(-42,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_F1);
                click("Джойстик 1");check("Подключение M30");checkHeader();checkHeld("A + B");checkGuide("B: Назад");key(id,KeyEvent.ACTION_UP,96);checkHeld("Нет удерживаемых кнопок");
                click("Тест кнопок");checkLargeCode(KeyEvent.KEYCODE_F1);key(id,KeyEvent.ACTION_UP,97);checkHeld("Нет удерживаемых кнопок");
                key(id,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BUTTON_16);checkLargeCode(KeyEvent.KEYCODE_BUTTON_16);key(id,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BUTTON_16);checkLargeCode(KeyEvent.KEYCODE_BUTTON_16);
                hat(id,-1,-1);checkHeld("↑ + ←");hat(id,0,0);checkHeld("Нет удерживаемых кнопок");
                report.putString("virtual_input","PASS: controller header on joystick pages, independent button panel, live keys across swipes, universal foreign key display with controller counter isolation, retained codes, HAT diagonals");
            }else report.putString("virtual_input","SKIPPED: M30 Virtual Test input source not present");
        });
        checkInlineBluetooth();
        checkControllerPageAlignment();
        checkRemoteNavigation();
        Activity beforeSearch=activity;activity=startActivitySync(new Intent(getTargetContext(),BluetoothControllerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        runOnMainSync(()->{check("Bluetooth · геймпады");check("Ранее подключённые");check("Найдены рядом");check("Начать поиск");check("Системные настройки Bluetooth");check("Проверить кнопки");
            try{java.lang.reflect.Field field=BluetoothControllerActivity.class.getDeclaredField("search");field.setAccessible(true);if(((BluetoothSearchController)field.get(activity)).searching())throw new AssertionError("Opening the screen must not start discovery");}catch(ReflectiveOperationException error){throw new AssertionError(error);}
            activity.finish();});activity=beforeSearch;
        Activity main=activity;activity=startActivitySync(new Intent(getTargetContext(),M30FirmwareActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        runOnMainSync(()->{check("Прошивка M30");check("Проверить M30");check("Выбрать официальный DAT");View install=find(activity.getWindow().getDecorView(),"Установить",true);if(install==null||install.isEnabled())throw new AssertionError("Update must be disabled before current USB model/version and exact file checks");if(M30UpdateService.running)throw new AssertionError("Opening firmware screen must never start an update");activity.finish();});activity=main;
        report.putString("stream","PASS: launch, fixed header, atomic instruction combinations, missing/zero battery, left/right swipes, profile selection\n"+report.getString("virtual_input"));finish(Activity.RESULT_OK,report);
    }catch(Throwable e){report.putString("stream","FAIL: "+e);finish(Activity.RESULT_CANCELED,report);}}
}
