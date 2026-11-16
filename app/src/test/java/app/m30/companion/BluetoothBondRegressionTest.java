package app.m30.companion;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.os.Looper;
import android.widget.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class BluetoothBondRegressionTest {
    private static final String A="AA:BB:CC:DD:EE:01",B="AA:BB:CC:DD:EE:02";
    private ActivityController<Activity> activity;
    private Activity context;
    private BluetoothAdapter adapter;
    private BluetoothDevice first,second;
    private BluetoothSearchController search;
    private Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    private void idle(){shadowOf(Looper.getMainLooper()).idle();}
    @Before public void prepare(){activity=Robolectric.buildActivity(Activity.class).setup();context=activity.get();shadowOf(context.getApplication()).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.ACCESS_FINE_LOCATION);adapter=BluetoothAdapter.getDefaultAdapter();shadowOf(adapter).setEnabled(true);first=adapter.getRemoteDevice(A);second=adapter.getRemoteDevice(B);shadowOf(first).setName("8BitDo M30 old");shadowOf(second).setName("8BitDo M30 second");shadowOf(first).setBondState(BluetoothDevice.BOND_BONDED);shadowOf(second).setBondState(BluetoothDevice.BOND_BONDED);shadowOf(adapter).setBondedDevices(new HashSet<>(Arrays.asList(first,second)));}
    @After public void finish(){if(search!=null)search.destroy();activity.pause().stop().destroy();}
    private void start(){search=new BluetoothSearchController(context,null,true,0xffb6a45e,()->{});search.resume();}
    private void label(String address,String title,int badge){context.getSharedPreferences("bluetooth_device_labels",0).edit().putString("title:"+address,title).putInt("badge:"+address,badge).commit();context.getSharedPreferences("bluetooth_controller_links",0).edit().putString(address,"descriptor-"+address).commit();}
    private void unpairFirst(){shadowOf(adapter).setBondedDevices(Collections.singleton(second));context.sendBroadcast(new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED).putExtra(BluetoothDevice.EXTRA_DEVICE,first).putExtra(BluetoothDevice.EXTRA_BOND_STATE,BluetoothDevice.BOND_NONE));idle();}
    private void discoverFirst(String name)throws Exception{Field f=search.getClass().getDeclaredField("ownsDiscovery");f.setAccessible(true);f.setBoolean(search,true);context.sendBroadcast(new Intent(BluetoothDevice.ACTION_FOUND).putExtra(BluetoothDevice.EXTRA_DEVICE,first).putExtra(BluetoothDevice.EXTRA_NAME,name));idle();}
    @Test public void bondRemovalUpdatesIdlePanelAndOnlyRemovedIdentityIsPurged()throws Exception{
        label(A,"White",2);label(B,"Black",7);start();assertNotNull(FocusNavigation.find(search.view(),"bt:saved|"+A));assertFalse(search.searching());unpairFirst();assertNull(FocusNavigation.find(search.view(),"bt:saved|"+A));assertNotNull(FocusNavigation.find(search.view(),"bt:saved|"+B));assertFalse(context.getSharedPreferences("bluetooth_device_labels",0).contains("title:"+A));assertFalse(context.getSharedPreferences("bluetooth_controller_links",0).contains(A));assertEquals("Black",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+B,""));assertEquals(7,context.getSharedPreferences("bluetooth_device_labels",0).getInt("badge:"+B,0));
    }
    @Test public void removalWhilePausedIsReconciledFromSnapshotOnResume(){
        label(A,"Old label",3);start();search.pause();shadowOf(adapter).setBondedDevices(Collections.singleton(second));search.resume();assertNull(FocusNavigation.find(search.view(),"bt:saved|"+A));assertFalse(context.getSharedPreferences("bluetooth_device_labels",0).contains("title:"+A));
    }
    @Test public void bluetoothOffIsUnknownAndNeverDeletesRememberedMetadata(){
        label(A,"Keep",4);BluetoothBondMemory.read(context,adapter);shadowOf(adapter).setEnabled(false);shadowOf(adapter).setBondedDevices(Collections.emptySet());assertNull(BluetoothBondMemory.read(context,adapter));assertEquals("Keep",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));assertTrue(context.getSharedPreferences(BluetoothBondMemory.STORE,0).getStringSet(BluetoothBondMemory.KEY,Collections.emptySet()).contains(A));
    }
    @Test public void neverPairedDiscoveryLabelSurvivesUnrelatedSnapshot(){
        shadowOf(adapter).setBondedDevices(Collections.singleton(second));context.getSharedPreferences("bluetooth_device_labels",0).edit().putString("title:"+A,"Not paired yet").commit();BluetoothBondMemory.read(context,adapter);assertEquals("Not paired yet",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));
    }
    @Test public void staleDeviceBondAndCachedNameCannotBlockNewPairing()throws Exception{
        label(A,"Old local name",2);start();unpairFirst();assertEquals(BluetoothDevice.BOND_BONDED,first.getBondState());discoverFirst("8BitDo M30 fresh");Button found=(Button)FocusNavigation.find(search.view(),"bt:found|"+A);assertTrue(found.getText().toString().contains("fresh"));found.performClick();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();BluetoothDeviceList list=(BluetoothDeviceList)field(search,"devices");assertEquals("8BitDo M30 fresh",list.get(A).displayName());assertEquals(BluetoothDeviceList.PAIR,list.get(A).action());shadowOf(first).setCreatedBond(true);FocusNavigation.find(dialog.getWindow().getDecorView(),"bt:device-action:0").performClick();assertEquals("Stale BONDED must not skip createBond or claim paired",BluetoothPairingState.PAIRING,((BluetoothPairingState)field(search,"pairing")).phase());assertNull(shadowOf(context).getNextStartedActivity());
    }
    @Test public void staleDisconnectMenuDoesNotOpenSettingsForMissingPair(){
        start();FocusNavigation.find(search.view(),"bt:saved|"+A).performClick();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();shadowOf(adapter).setBondedDevices(Collections.singleton(second));FocusNavigation.find(dialog.getWindow().getDecorView(),"bt:disconnect").performClick();assertNull(shadowOf(context).getNextStartedActivity());assertNull(FocusNavigation.find(search.view(),"bt:saved|"+A));
    }
    private AlertDialog askForget(String rowKey){
        FocusNavigation.find(search.view(),rowKey).performClick();AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();assertNull(FocusNavigation.find(menu.getWindow().getDecorView(),"bt:forget-local"));assertNull(FocusNavigation.find(menu.getWindow().getDecorView(),"bt:forget-system"));Button forget=(Button)FocusNavigation.find(menu.getWindow().getDecorView(),"bt:forget");assertEquals("Забыть устройство",forget.getText().toString());forget.performClick();return ShadowAlertDialog.getLatestAlertDialog();
    }
    private void goToSystemForget(AlertDialog confirm){confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();Intent intent=shadowOf(context).getNextStartedActivity();assertNotNull(intent);assertEquals(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,intent.getAction());}
    @Test public void forgetPairedDeviceOnlyClearsAfterAndroidDeletion()throws Exception{
        label(A,"White",2);label(B,"Black",7);start();FocusNavigation.find(search.view(),"bt:saved|"+A).performClick();AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();capture(menu,"device-menu.png");FocusNavigation.find(menu.getWindow().getDecorView(),"bt:forget").performClick();AlertDialog confirm=ShadowAlertDialog.getLatestAlertDialog();capture(confirm,"forget-device.png");assertEquals("White",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));goToSystemForget(confirm);assertEquals("White",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));assertTrue(context.getSharedPreferences("bluetooth_controller_links",0).contains(A));search.pause();shadowOf(adapter).setBondedDevices(Collections.singleton(second));search.resume();assertFalse(context.getSharedPreferences("bluetooth_device_labels",0).contains("title:"+A));assertNull(FocusNavigation.find(search.view(),"bt:saved|"+A));assertEquals("Black",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+B,""));assertEquals("",search.save().getString("forget_address"));
    }
    @Test public void cancelledSystemForgetKeepsSavedEntryReconnectsWithoutSearch(){
        label(A,"White",2);start();goToSystemForget(askForget("bt:saved|"+A));search.pause();search.resume();assertEquals("White",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));assertTrue(context.getSharedPreferences("bluetooth_controller_links",0).contains(A));assertNotNull(FocusNavigation.find(search.view(),"bt:saved|"+A));assertFalse(search.searching());FocusNavigation.find(search.view(),"bt:saved|"+A).performClick();AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();FocusNavigation.find(menu.getWindow().getDecorView(),"bt:device-action:0").performClick();Intent connect=shadowOf(context).getNextStartedActivity();assertNotNull(connect);assertEquals(BluetoothAliasActivity.CONNECT,connect.getStringExtra(BluetoothAliasActivity.OPERATION));assertEquals(A,connect.getStringExtra("address"));assertFalse(search.searching());
    }
    @Test public void forgetUnpairedDiscoveryClearsLabelAndEntry()throws Exception{
        shadowOf(adapter).setBondedDevices(Collections.singleton(second));start();context.getSharedPreferences("bluetooth_device_labels",0).edit().putString("title:"+A,"Unpaired label").putInt("badge:"+A,4).commit();discoverFirst("8BitDo M30 fresh");AlertDialog confirm=askForget("bt:found|"+A);assertTrue(context.getSharedPreferences("bluetooth_device_labels",0).contains("title:"+A));confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();assertFalse(context.getSharedPreferences("bluetooth_device_labels",0).contains("title:"+A));assertNull(FocusNavigation.find(search.view(),"bt:found|"+A));assertNull(shadowOf(context).getNextStartedActivity());assertEquals(Collections.singleton(second),adapter.getBondedDevices());
    }
    @Test public void newlyCreatedPairDuringConfirmationCannotBeHiddenLocally()throws Exception{
        shadowOf(adapter).setBondedDevices(Collections.singleton(second));start();context.getSharedPreferences("bluetooth_device_labels",0).edit().putString("title:"+A,"Keep new pair").commit();discoverFirst("8BitDo M30 fresh");AlertDialog confirm=askForget("bt:found|"+A);shadowOf(adapter).setBondedDevices(new HashSet<>(Arrays.asList(first,second)));goToSystemForget(confirm);assertEquals("Keep new pair",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));assertTrue(adapter.getBondedDevices().contains(first));
    }
    @Test @Config(sdk=35) public void pendingForgetSurvivesRecreationAndWaitsForReadableSnapshot(){
        label(A,"Keep until verified",5);start();goToSystemForget(askForget("bt:saved|"+A));android.os.Bundle state=search.save();search.destroy();shadowOf(adapter).setBondedDevices(Collections.singleton(second));shadowOf(context.getApplication()).denyPermissions(Manifest.permission.BLUETOOTH_CONNECT);search=new BluetoothSearchController(context,state,true,0xffb6a45e,()->{});search.resume();assertEquals(A,search.save().getString("forget_address"));assertEquals("Keep until verified",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));search.pause();shadowOf(context.getApplication()).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT);search.resume();assertEquals("",search.save().getString("forget_address"));assertFalse(context.getSharedPreferences("bluetooth_device_labels",0).contains("title:"+A));assertNull(FocusNavigation.find(search.view(),"bt:saved|"+A));
    }
    private void capture(AlertDialog dialog,String name)throws Exception{
        idle();android.view.View decor=dialog.getWindow().getDecorView();decor.measure(android.view.View.MeasureSpec.makeMeasureSpec(600,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(700,android.view.View.MeasureSpec.AT_MOST));decor.layout(0,0,600,decor.getMeasuredHeight());android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);decor.draw(new android.graphics.Canvas(bitmap));java.io.File dir=new java.io.File("build/reports/bluetooth-1.62");assertTrue(dir.isDirectory()||dir.mkdirs());try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,name))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
    @Test public void cancelledNewPairKeepsIntentionalLabelAndRemainsRetryable()throws Exception{
        shadowOf(adapter).setBondedDevices(Collections.singleton(second));context.getSharedPreferences("bluetooth_device_labels",0).edit().putString("title:"+A,"Keep unpaired label").commit();start();discoverFirst("8BitDo M30 fresh");FocusNavigation.find(search.view(),"bt:found|"+A).performClick();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();shadowOf(first).setCreatedBond(true);FocusNavigation.find(dialog.getWindow().getDecorView(),"bt:device-action:0").performClick();context.sendBroadcast(new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED).putExtra(BluetoothDevice.EXTRA_DEVICE,first).putExtra(BluetoothDevice.EXTRA_BOND_STATE,BluetoothDevice.BOND_NONE).putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,BluetoothDevice.BOND_BONDING));idle();assertEquals(BluetoothPairingState.ERROR,((BluetoothPairingState)field(search,"pairing")).phase());assertEquals("Keep unpaired label",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));assertTrue(FocusNavigation.find(search.view(),"bt:found|"+A).isEnabled());
    }
    @Test @Config(sdk=35) public void missingPermissionCannotBeMisreadAsRemovedBond(){
        label(A,"Keep permission",6);start();search.pause();shadowOf(adapter).setBondedDevices(Collections.singleton(second));shadowOf(context.getApplication()).denyPermissions(Manifest.permission.BLUETOOTH_CONNECT);search.resume();assertEquals("Keep permission",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));search.pause();shadowOf(context.getApplication()).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT);search.resume();assertFalse(context.getSharedPreferences("bluetooth_device_labels",0).contains("title:"+A));
    }
    @Test @Config(sdk=35) public void unpairedDiscoveryDoesNotReuseAndroidCachedAlias()throws Exception{
        shadowOf(first).setAlias("Old Android alias");start();unpairFirst();discoverFirst("8BitDo M30 new broadcast");BluetoothDeviceList list=(BluetoothDeviceList)field(search,"devices");assertEquals("8BitDo M30 new broadcast",list.get(A).displayName());assertTrue(list.get(A).alias.isEmpty());
    }
}
