package app.m30.companion;

import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Looper;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="sw720dp-w1280dp-h720dp-land-television-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class TvUiRegressionTest {
    private Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private void layout(View decor,int width,int height){shadowOf(Looper.getMainLooper()).idle();decor.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));decor.layout(0,0,width,height);decor.getViewTreeObserver().dispatchOnPreDraw();}
    private void dialogLayout(View decor){shadowOf(Looper.getMainLooper()).idle();decor.measure(View.MeasureSpec.makeMeasureSpec(680,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.AT_MOST));decor.layout(0,0,680,decor.getMeasuredHeight());}
    private void image(View decor,String name)throws Exception{int width=decor.getWidth()>0?decor.getWidth():1280,height=decor.getHeight()>0?decor.getHeight():720;Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);decor.draw(new Canvas(bitmap));File directory=new File("build/reports/ui-1.60");assertTrue(directory.isDirectory()||directory.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(directory,name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
    @Test public void wideLabelsAndBothHelpButtonsRemainUsableWithLargeText()throws Exception{
        Configuration c=new Configuration(RuntimeEnvironment.getApplication().getResources().getConfiguration());c.fontScale=1.3f;RuntimeEnvironment.getApplication().getResources().updateConfiguration(c,null);
        ActivityController<MainActivity> control=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=control.get();try{
            View decor=a.getWindow().getDecorView();decor.requestFocusFromTouch();layout(decor,1280,720);ControllerPanelView[] panes=(ControllerPanelView[])field(a,"widePanels");assertNotNull("TV needs both controller panels",panes);
            for(ControllerPanelView pane:panes){Button connect=pane.connect;assertEquals("Подключение",connect.getText().toString());int icon=connect.getCompoundDrawablesRelative()[0].getBounds().width()+connect.getCompoundDrawablePadding();int available=connect.getWidth()-connect.getPaddingLeft()-connect.getPaddingRight()-icon;assertTrue("Full connection caption must fit at fontScale1.3",available>=connect.getPaint().measureText(connect.getText().toString()));assertTrue(pane.dpadHelp.isFocusable());assertTrue(pane.ledHelp.isFocusable());assertTrue(pane.dpadHelp.isShown());assertTrue(pane.ledHelp.isShown());assertSame(pane.dpadHelp,pane.keys.focusSearch(View.FOCUS_DOWN));assertSame(pane.ledHelp,pane.dpadHelp.focusSearch(View.FOCUS_RIGHT));int[] help=new int[2],pad=new int[2];pane.dpadHelp.getLocationOnScreen(help);pane.pad.getLocationOnScreen(pad);assertTrue("Help must be below the diagram",help[1]>=pad[1]+pane.pad.getHeight());assertFalse("Diagram must not steal remote focus from help",pane.pad.isFocusable());}
            panes[1].connect.requestFocus();image(decor,"tv-large-text-focused-connection.png");panes[1].dpadHelp.performClick();Dialog dialog=(Dialog)field(a,"dpadDialog");assertTrue(dialog.isShowing());dialogLayout(dialog.getWindow().getDecorView());image(dialog.getWindow().getDecorView(),"dpad-help.png");dialog.dismiss();
        }finally{control.pause().stop().destroy();}
    }
    @Test public void fallbackIsOpaqueAndCheckboxMovesFocusToContinue()throws Exception{
        RuntimeEnvironment.getApplication().getSharedPreferences(BluetoothCompatibility.STORE,0).edit().clear().commit();Intent intent=new Intent(RuntimeEnvironment.getApplication(),BluetoothAliasActivity.class).putExtra("address","AA:BB:CC:DD:EE:FF").putExtra("name","8BitDo M30").putExtra("owner",1).putExtra(BluetoothAliasActivity.OPERATION,BluetoothAliasActivity.CONNECT);
        ActivityController<BluetoothAliasActivity> control=Robolectric.buildActivity(BluetoothAliasActivity.class,intent).setup();BluetoothAliasActivity a=control.get();try{
            AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertTrue(dialog.isShowing());dialog.getWindow().getDecorView().requestFocusFromTouch();dialogLayout(dialog.getWindow().getDecorView());assertEquals(0xff181818,((GradientDrawable)dialog.getWindow().getDecorView().getBackground()).getColor().getDefaultColor());assertEquals(1f,dialog.getWindow().getAttributes().alpha,0);
            CheckBox checkbox=(CheckBox)FocusNavigation.find(dialog.getWindow().getDecorView(),"bt:enable-alternative");Button confirm=dialog.getButton(AlertDialog.BUTTON_POSITIVE);assertFalse(confirm.isEnabled());checkbox.setChecked(true);shadowOf(Looper.getMainLooper()).idle();assertTrue(confirm.isEnabled());assertTrue("Checking must move focus onto confirmation",confirm.hasFocus());assertFalse("Checking alone cannot commit compatibility mode",BluetoothCompatibility.external(a));image(dialog.getWindow().getDecorView(),"opaque-fallback-continue-focused.png");confirm.performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(BluetoothCompatibility.external(a));assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS,shadowOf(a).getNextStartedActivity().getAction());
        }finally{control.pause().stop().destroy();}
    }
    @Test @Config(qualifiers="sw360dp-w360dp-h780dp-port-mdpi") public void portraitCaptionAndHelpFitWithLargeText()throws Exception{
        Configuration c=new Configuration(RuntimeEnvironment.getApplication().getResources().getConfiguration());c.fontScale=1.3f;RuntimeEnvironment.getApplication().getResources().updateConfiguration(c,null);ActivityController<MainActivity> control=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=control.get();try{
            View decor=a.getWindow().getDecorView();decor.requestFocusFromTouch();layout(decor,360,780);image(decor,"phone-large-text.png");TextView text=(TextView)field(a,"headerConnection");assertTrue("Single connection caption must be complete",text.getWidth()-text.getPaddingLeft()-text.getPaddingRight()>=text.getPaint().measureText(text.getText().toString()));
            int[] keyLocation=new int[2];View keys=FocusNavigation.find(decor,"header:keys");keys.getLocationOnScreen(keyLocation);TextView version=(TextView)field(a,"headerVersions");int[] versionLocation=new int[2];version.getLocationOnScreen(versionLocation);
            for(String key:new String[]{"header:dpad-info","header:led-info"}){Button button=(Button)FocusNavigation.find(decor,key);assertTrue(button.isFocusable());int[] position=new int[2];button.getLocationOnScreen(position);assertEquals("Portrait help top aligns with Keys",keyLocation[1],position[1]);assertEquals("Portrait help bottom aligns with Keys",keyLocation[1]+keys.getHeight(),position[1]+button.getHeight());assertTrue("Version belongs above help",versionLocation[1]+version.getHeight()<=position[1]);int icon=button.getCompoundDrawablesRelative()[0].getBounds().width()+button.getCompoundDrawablePadding();assertTrue("Portrait help must fit: "+key,button.getWidth()-button.getPaddingLeft()-button.getPaddingRight()-icon>=button.getPaint().measureText(button.getText().toString()));}
        }finally{control.pause().stop().destroy();}
    }
    @Test @Config(qualifiers="sw320dp-w640dp-h320dp-land-mdpi") public void shortLandscapePhoneShowsWholeControllerAndHelp()throws Exception{
        Configuration c=new Configuration(RuntimeEnvironment.getApplication().getResources().getConfiguration());c.fontScale=1.3f;RuntimeEnvironment.getApplication().getResources().updateConfiguration(c,null);ActivityController<MainActivity> control=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=control.get();try{
            View decor=a.getWindow().getDecorView();layout(decor,640,320);assertNull("Phone stays single-pane",field(a,"widePanels"));View pad=(View)field(a,"pad");int[] location=new int[2];pad.getLocationOnScreen(location);assertTrue("Whole controller fits short viewport",location[1]+pad.getHeight()<=320);View help=FocusNavigation.find(decor,"header:led-info");help.getLocationOnScreen(location);assertTrue("Help remains visible without scrolling",location[1]+help.getHeight()<=320);image(decor,"phone-landscape-short.png");
        }finally{control.pause().stop().destroy();}
    }
    @Test @Config(qualifiers="sw360dp-w780dp-h360dp-land-mdpi") public void openingConnectionRevealsBluetoothForEitherSlotAndClosingReturns()throws Exception{
        ActivityController<MainActivity> control=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=control.get();try{
            View decor=a.getWindow().getDecorView();layout(decor,780,360);ScrollView scroll=(ScrollView)field(a,"contentScroll");
            for(int page:new int[]{0,2}){FocusNavigation.find(decor,"tab:"+page).performClick();layout(decor,780,360);scroll=(ScrollView)field(a,"contentScroll");int previous=page==0?0:48;scroll.scrollTo(0,previous);View toggle=FocusNavigation.find(decor,"bluetooth:toggle");toggle.performClick();layout(decor,780,360);View host=(View)field(a,"singleBluetoothHost");int[] location=new int[2];host.getLocationOnScreen(location);assertTrue("Opening must scroll down",scroll.getScrollY()>0);assertTrue("Bluetooth heading appears at viewport top",location[1]>=0&&location[1]<8);assertTrue("Bluetooth is actually visible",host.getGlobalVisibleRect(new Rect()));assertEquals("Opening keeps requested controller slot",page==0?0:1,field(a,"activeSlot"));image(decor,"phone-landscape-bluetooth-"+page+".png");FocusNavigation.find(decor,"single:connection:close").performClick();layout(decor,780,360);assertEquals("Close returns to previous scroll position",previous,scroll.getScrollY());assertNull(field(a,"singleBluetoothHost"));}
        }finally{control.pause().stop().destroy();}
    }
    @Test @Config(qualifiers="sw360dp-w780dp-h360dp-land-mdpi") public void restoredOpenConnectionRemainsVisibleAndOwnedBySecondSlot()throws Exception{
        ActivityController<MainActivity> control=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=control.get();View decor=a.getWindow().getDecorView();layout(decor,780,360);FocusNavigation.find(decor,"tab:2").performClick();layout(decor,780,360);FocusNavigation.find(decor,"bluetooth:toggle").performClick();layout(decor,780,360);android.os.Bundle saved=new android.os.Bundle();control.saveInstanceState(saved).pause().stop().destroy();ActivityController<MainActivity> restored=Robolectric.buildActivity(MainActivity.class).create(saved).start().restoreInstanceState(saved).resume().visible();a=restored.get();try{
            decor=a.getWindow().getDecorView();layout(decor,780,360);assertEquals(1,field(a,"activeSlot"));View host=(View)field(a,"singleBluetoothHost");assertNotNull(host);int[] position=new int[2];host.getLocationOnScreen(position);assertTrue("Restored Bluetooth must remain visible",position[1]>=0&&position[1]<8);FocusNavigation.find(decor,"single:connection:close").performClick();layout(decor,780,360);assertEquals(0,((ScrollView)field(a,"contentScroll")).getScrollY());
        }finally{restored.pause().stop().destroy();}
    }
    @Test public void oldTvWithLocationOffOffersSettingsAndNeverLocationGate()throws Exception{
        ActivityController<Activity> control=Robolectric.buildActivity(Activity.class).setup();Activity a=control.get();BluetoothCompatibility.set(a,BluetoothCompatibility.EXTERNAL,false);shadowOf((android.location.LocationManager)a.getSystemService(Context.LOCATION_SERVICE)).setLocationEnabled(false);BluetoothSearchController search=new BluetoothSearchController(a,null,true,0xffb6a45e,()->{});try{
            search.resume();((Button)FocusNavigation.find(search.view(),"bt:search")).performClick();AlertDialog dialog=(AlertDialog)field(search,"scanDialog");assertNotNull(dialog);assertTrue(dialog.isShowing());dialog.getWindow().getDecorView().requestFocusFromTouch();dialogLayout(dialog.getWindow().getDecorView());assertEquals(View.GONE,dialog.getButton(AlertDialog.BUTTON_NEUTRAL).getVisibility());assertEquals(0xff181818,((GradientDrawable)dialog.getWindow().getDecorView().getBackground()).getColor().getDefaultColor());assertFalse(search.searching());CheckBox choice=(CheckBox)FocusNavigation.find(dialog.getWindow().getDecorView(),"bt:external-search");choice.setChecked(true);shadowOf(Looper.getMainLooper()).idle();Button confirm=dialog.getButton(AlertDialog.BUTTON_POSITIVE);assertTrue(confirm.hasFocus());image(dialog.getWindow().getDecorView(),"tv-location-off-fallback.png");confirm.performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(BluetoothCompatibility.external(a));assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS,shadowOf(a).getNextStartedActivity().getAction());
        }finally{search.destroy();control.pause().stop().destroy();}
    }
    @Test public void chosenSystemSearchDoesNotAskForLocationOrStartClassicDiscovery(){
        ActivityController<Activity> control=Robolectric.buildActivity(Activity.class).setup();Activity a=control.get();BluetoothCompatibility.set(a,BluetoothCompatibility.EXTERNAL,true);BluetoothSearchController search=new BluetoothSearchController(a,null,true,0xffb6a45e,()->{});try{
            assertNotNull(BluetoothAdapter.getDefaultAdapter());search.resume();Button start=(Button)FocusNavigation.find(search.view(),"bt:search");assertEquals("Поиск в Android",start.getText().toString());start.performClick();assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS,shadowOf(a).getNextStartedActivity().getAction());assertFalse(search.searching());
        }finally{search.destroy();control.pause().stop().destroy();}
    }
}
