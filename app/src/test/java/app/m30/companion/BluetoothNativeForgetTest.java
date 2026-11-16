package app.m30.companion;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.companion.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.widget.*;
import java.time.Duration;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=36)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class BluetoothNativeForgetTest {
    private static final String A="AA:BB:CC:DD:EE:01",B="AA:BB:CC:DD:EE:02";
    private ActivityController<Activity> host;
    private Activity context;
    private BluetoothAdapter adapter;
    private BluetoothDevice first,second;
    private CompanionDeviceManager manager;
    private BluetoothSearchController search;
    private ActivityController<BluetoothAliasActivity> action;
    @Before public void prepare(){
        host=Robolectric.buildActivity(Activity.class).setup();context=host.get();
        shadowOf(context.getApplication()).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN);
        shadowOf(context.getPackageManager()).setSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP,true);
        adapter=BluetoothAdapter.getDefaultAdapter();shadowOf(adapter).setEnabled(true);first=adapter.getRemoteDevice(A);second=adapter.getRemoteDevice(B);
        shadowOf(first).setName("8BitDo M30");shadowOf(second).setName("8BitDo M30");
        shadowOf(first).setBondState(BluetoothDevice.BOND_BONDED);shadowOf(second).setBondState(BluetoothDevice.BOND_BONDED);
        shadowOf(adapter).setBondedDevices(new HashSet<>(Arrays.asList(first,second)));
        manager=context.getSystemService(CompanionDeviceManager.class);
        label(A,"White",2);label(B,"Black",7);association(41,A);association(42,B);
    }
    @After public void finish(){if(search!=null)search.destroy();if(action!=null)action.destroy();host.pause().stop().destroy();}
    private void label(String address,String title,int badge){context.getSharedPreferences("bluetooth_device_labels",0).edit().putString("title:"+address,title).putInt("badge:"+address,badge).commit();context.getSharedPreferences("bluetooth_controller_links",0).edit().putString(address,"source-"+address).commit();context.getSharedPreferences("bluetooth_modes",0).edit().putInt(address,1).commit();}
    private void association(int id,String address){shadowOf(manager).addAssociation(AssociationInfoBuilder.newBuilder().setId(id).setUserId(0).setPackageName(context.getPackageName()).setDeviceMacAddress(address).build());}
    private void start(){search=new BluetoothSearchController(context,null,true,0xffb6a45e,()->{});search.resume();}
    private Intent request(){
        FocusNavigation.find(search.view(),"bt:saved|"+A).performClick();AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();FocusNavigation.find(menu.getWindow().getDecorView(),"bt:forget").performClick();ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();
        Intent intent=shadowOf(context).getNextStartedActivity();assertNotNull(intent);assertEquals(BluetoothAliasActivity.FORGET,intent.getStringExtra(BluetoothAliasActivity.OPERATION));assertEquals(A,intent.getStringExtra("address"));return intent;
    }
    private Intent accepted(){shadowOf(manager).markAssociationBondRemovable(41);action=Robolectric.buildActivity(BluetoothAliasActivity.class,request()).create();BluetoothAliasActivity a=action.get();assertTrue(a.isFinishing());assertEquals(Activity.RESULT_OK,shadowOf(a).getResultCode());Intent result=shadowOf(a).getResultIntent();assertTrue(result.getBooleanExtra("bond_removal_requested",false));return result;}
    private void result(Intent result){search.activityResult(BluetoothSearchController.DEVICE_ACTION,Activity.RESULT_OK,result);}
    private void kept(){assertEquals("White",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));assertEquals(2,context.getSharedPreferences("bluetooth_device_labels",0).getInt("badge:"+A,0));assertTrue(context.getSharedPreferences("bluetooth_controller_links",0).contains(A));assertEquals(41,BluetoothBondMemory.associationId(context,manager,A));}
    private void purged(){assertFalse(context.getSharedPreferences("bluetooth_device_labels",0).contains("title:"+A));assertFalse(context.getSharedPreferences("bluetooth_device_labels",0).contains("badge:"+A));assertFalse(context.getSharedPreferences("bluetooth_controller_links",0).contains(A));assertFalse(context.getSharedPreferences("bluetooth_modes",0).contains(A));assertEquals(-1,BluetoothBondMemory.associationId(context,manager,A));assertEquals("Black",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+B,""));assertEquals(42,BluetoothBondMemory.associationId(context,manager,B));assertNull(FocusNavigation.find(search.view(),"bt:saved|"+A));}
    @Test public void acceptedRequestUsesExactAssociationAndDoesNotPretendSuccess(){start();Intent response=accepted();assertEquals(41,shadowOf(manager).getLastRemoveBondAssociationId());result(response);kept();assertTrue(search.save().getBoolean("forget_native_pending"));assertFalse(FocusNavigation.find(search.view(),"bt:saved|"+A).isEnabled());}
    @Test public void confirmedBondEventPurgesOnlyRemovedController(){start();result(accepted());context.getSharedPreferences("controller_bindings",0).edit().putString("source-saved","keep buttons").commit();shadowOf(adapter).setBondedDevices(Collections.singleton(second));context.sendBroadcast(new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED).putExtra(BluetoothDevice.EXTRA_DEVICE,first).putExtra(BluetoothDevice.EXTRA_BOND_STATE,BluetoothDevice.BOND_NONE).putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,BluetoothDevice.BOND_BONDED));shadowOf(Looper.getMainLooper()).idle();purged();assertEquals("keep buttons",context.getSharedPreferences("controller_bindings",0).getString("source-saved",""));assertEquals("",search.save().getString("forget_address"));}
    @Test public void freshSnapshotCompletesWithoutBroadcast(){start();result(accepted());shadowOf(adapter).setBondedDevices(Collections.singleton(second));shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));purged();}
    @Test public void refusalPreservesPairAndOffersSettings(){start();Intent request=request();shadowOf(manager).markAssociationBondNotRemovable(41);action=Robolectric.buildActivity(BluetoothAliasActivity.class,request).create();assertFalse(action.get().isFinishing());kept();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Открыть настройки",dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();search.activityResult(BluetoothSearchController.DEVICE_ACTION,Activity.RESULT_CANCELED,shadowOf(action.get()).getResultIntent());kept();assertFalse(search.save().getBoolean("forget_native_pending"));assertTrue(FocusNavigation.find(search.view(),"bt:saved|"+A).isEnabled());}
    @Test public void lostPermissionRetainsMetadataUntilReadableSnapshot(){start();result(accepted());search.pause();shadowOf(adapter).setBondedDevices(Collections.singleton(second));shadowOf(context.getApplication()).denyPermissions(Manifest.permission.BLUETOOTH_CONNECT);search.resume();assertEquals("White",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));search.pause();shadowOf(context.getApplication()).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT);search.resume();purged();}
    @Test public void timeoutPreservesPairAndAllowsExplicitSystemFallback(){start();result(accepted());shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31));kept();assertFalse(search.save().getBoolean("forget_native_pending"));AlertDialog fallback=ShadowAlertDialog.getLatestAlertDialog();fallback.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();Intent settings=shadowOf(context).getNextStartedActivity();assertNotNull(settings);assertEquals(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,settings.getAction());kept();}
    @Test public void pendingRequestSurvivesRecreationWithoutRepeatingNativeCall(){start();result(accepted());Bundle saved=search.save();search.destroy();search=new BluetoothSearchController(context,saved,true,0xffb6a45e,()->{});search.resume();kept();assertTrue(search.save().getBoolean("forget_native_pending"));shadowOf(adapter).setBondedDevices(Collections.singleton(second));shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));purged();}
    @Test public void missingAssociationRequiresConsentForExactAddress()throws Exception{
        manager.disassociate(41);start();Intent intent=request();action=Robolectric.buildActivity(BluetoothAliasActivity.class,intent).create();assertFalse(action.get().isFinishing());assertEquals(-1,shadowOf(manager).getLastRemoveBondAssociationId());AssociationRequest req=shadowOf(manager).getLastAssociationRequest();assertNotNull(req);java.util.List<?> filters=org.robolectric.util.ReflectionHelpers.callInstanceMethod(req,"getDeviceFilters");assertEquals(A,org.robolectric.util.ReflectionHelpers.getField(filters.get(0),"mAddress"));
        association(51,A);shadowOf(manager).markAssociationBondRemovable(51);
        java.lang.reflect.Field field=BluetoothAliasActivity.class.getDeclaredField("screen");field.setAccessible(true);field.set(action.get(),"confirming");action.get().onActivityResult(1,Activity.RESULT_OK,new Intent());assertEquals(51,shadowOf(manager).getLastRemoveBondAssociationId());assertTrue(action.get().isFinishing());
    }
    @Test public void emptyCancelledActivityResultCannotLockPanel(){start();request();search.activityResult(BluetoothSearchController.DEVICE_ACTION,Activity.RESULT_CANCELED,null);kept();assertFalse(search.save().getBoolean("forget_action_pending"));assertTrue(FocusNavigation.find(search.view(),"bt:saved|"+A).isEnabled());}
    @Test public void hostWithoutCompanionSetupUsesSameForgetActionWithSettings(){shadowOf(context.getPackageManager()).setSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP,false);start();FocusNavigation.find(search.view(),"bt:saved|"+A).performClick();AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();FocusNavigation.find(menu.getWindow().getDecorView(),"bt:forget").performClick();ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,shadowOf(context).getNextStartedActivity().getAction());kept();}
    @Test public void cancelledCompanionConsentDoesNotRemovePairOrMetadata()throws Exception{manager.disassociate(41);start();action=Robolectric.buildActivity(BluetoothAliasActivity.class,request()).create();java.lang.reflect.Field screen=BluetoothAliasActivity.class.getDeclaredField("screen");screen.setAccessible(true);screen.set(action.get(),"confirming");action.get().onActivityResult(1,Activity.RESULT_CANCELED,null);assertEquals(-1,shadowOf(manager).getLastRemoveBondAssociationId());assertEquals("White",context.getSharedPreferences("bluetooth_device_labels",0).getString("title:"+A,""));assertTrue(adapter.getBondedDevices().contains(first));assertNotNull(ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE));}
    @Test public void permissionDeniedBeforeNativeActionCannotCallRemoveBond(){start();Intent intent=request();shadowOf(context.getApplication()).denyPermissions(Manifest.permission.BLUETOOTH_CONNECT);action=Robolectric.buildActivity(BluetoothAliasActivity.class,intent).create();action.get().onRequestPermissionsResult(2,new String[]{Manifest.permission.BLUETOOTH_CONNECT},new int[]{PackageManager.PERMISSION_DENIED});assertEquals(-1,shadowOf(manager).getLastRemoveBondAssociationId());kept();assertFalse(action.get().isFinishing());}

}
