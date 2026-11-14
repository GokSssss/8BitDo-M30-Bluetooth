package app.m30.companion;

import android.Manifest;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.os.*;
import android.view.InputDevice;
import java.util.*;

/** Foreground identity labels; no HID control, polling, name matching or inferred MACs. */
final class BluetoothControllerNames implements InputManager.InputDeviceListener,SharedPreferences.OnSharedPreferenceChangeListener {
    private final Context context;
    private final Runnable changed;
    private final SharedPreferences preferences,slots,labels,legacyModes;
    private final BluetoothSlotLinks links=new BluetoothSlotLinks();
    private final Map<String,String> names=new HashMap<>();
    private final Set<String> disconnected=new HashSet<>();
    private boolean started,receiverRegistered;
    private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){
        BluetoothDevice d=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if(d!=null){if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction()))disconnected.add(d.getAddress());else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction()))disconnected.remove(d.getAddress());}
        reload();changed.run();
    }};
    BluetoothControllerNames(Context context,Runnable changed){this.context=context;this.changed=changed;preferences=context.getSharedPreferences("bluetooth_controller_links",0);String mainClass=new ComponentName(context,MainActivity.class).getShortClassName();slots=context.getSharedPreferences(mainClass.startsWith(".")?mainClass.substring(1):mainClass,0);labels=context.getSharedPreferences("bluetooth_device_labels",0);legacyModes=context.getSharedPreferences("bluetooth_modes",0);reload();}
    BluetoothDeviceLabel label(String address){
        String title=labels.getString("title:"+address,null);int old=legacyModes.getInt(address,-1);
        if(title==null)title=old>=0&&old<4?new String[]{"Android","Windows","macOS","Switch"}[old]:"";
        return new BluetoothDeviceLabel(title,labels.getInt("badge:"+address,0));
    }
    void saveLabel(String address,String title,int badge){
        BluetoothDeviceLabel value=new BluetoothDeviceLabel(title,badge);legacyModes.edit().remove(address).apply();
        labels.edit().putString("title:"+address,value.title).putInt("badge:"+address,value.badge).apply();changed.run();
    }
    private boolean allowed(){return Build.VERSION.SDK_INT<31||context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
    private void reload(){
        for(String a:new ArrayList<>(links.snapshot().keySet()))links.remove(a);
        for(Map.Entry<String,?> e:preferences.getAll().entrySet())if(e.getValue() instanceof String)links.restore(e.getKey(),(String)e.getValue());
        names.clear();if(!allowed())return;
        try{BluetoothManager m=context.getSystemService(BluetoothManager.class);BluetoothAdapter a=m==null?null:m.getAdapter();if(a==null||!a.isEnabled())return;
            for(BluetoothDevice d:a.getBondedDevices()){String name=Build.VERSION.SDK_INT>=30?d.getAlias():null;if(name==null||name.isEmpty())name=d.getName();names.put(d.getAddress(),BluetoothGamepadFilter.cleanName(name==null?"Геймпад":name));}
        }catch(SecurityException ignored){names.clear();}
    }
    InputDevice source(int slot){
        String descriptor=slots.getString("controller."+slot,slot==0?slots.getString("device",""):"");if(descriptor.isEmpty())return null;
        for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null&&id>=0&&!SourceSelector.systemInputName(d.getName())&&descriptor.equals(d.getDescriptor()))return d;}return null;
    }
    String slotName(int slot){InputDevice d=source(slot);if(d==null)return "Не подключён";String a=links.linkedAddress(d.getDescriptor());return a!=null&&activeSlot(a)==slot?label(a).displayName(names.get(a)):BluetoothGamepadFilter.cleanName(d.getName());}
    int activeSlot(String address){
        if(!allowed()||!names.containsKey(address)||disconnected.contains(address))return -1;
        String[] active={"",""};for(int n=0;n<2;n++){InputDevice d=source(n);if(d!=null)active[n]=d.getDescriptor();}return links.activeSlot(address,active);
    }
    String linkedDescriptor(String address){reload();String value=links.snapshot().get(address);return value==null?"":value;}
    boolean linked(String address){return links.snapshot().containsKey(address);}
    boolean bind(String address,int slot){
        reload();InputDevice d=source(slot);if(d==null||!names.containsKey(address)||!links.bind(address,d.getDescriptor()))return false;
        SharedPreferences.Editor editor=preferences.edit();for(String old:preferences.getAll().keySet())editor.remove(old);for(Map.Entry<String,String> e:links.snapshot().entrySet())editor.putString(e.getKey(),e.getValue());editor.apply();changed.run();return true;
    }
    void unlink(String address){preferences.edit().remove(address).apply();}
    void start(){if(started)return;started=true;disconnected.clear();preferences.registerOnSharedPreferenceChangeListener(this);slots.registerOnSharedPreferenceChangeListener(this);labels.registerOnSharedPreferenceChangeListener(this);legacyModes.registerOnSharedPreferenceChangeListener(this);InputManager m=context.getSystemService(InputManager.class);if(m!=null)m.registerInputDeviceListener(this,new Handler(Looper.getMainLooper()));
        IntentFilter f=new IntentFilter();f.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);f.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);f.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);f.addAction(BluetoothDevice.ACTION_NAME_CHANGED);f.addAction(BluetoothDevice.ACTION_ALIAS_CHANGED);f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        if(allowed())try{context.registerReceiver(receiver,f,Build.VERSION.SDK_INT>=33?Context.RECEIVER_EXPORTED:0);receiverRegistered=true;}catch(SecurityException ignored){}
        reload();changed.run();
    }
    void stop(){if(!started)return;started=false;preferences.unregisterOnSharedPreferenceChangeListener(this);slots.unregisterOnSharedPreferenceChangeListener(this);labels.unregisterOnSharedPreferenceChangeListener(this);legacyModes.unregisterOnSharedPreferenceChangeListener(this);InputManager m=context.getSystemService(InputManager.class);if(m!=null)m.unregisterInputDeviceListener(this);if(receiverRegistered){context.unregisterReceiver(receiver);receiverRegistered=false;}}
    @Override public void onSharedPreferenceChanged(SharedPreferences p,String key){if(p==slots&&key!=null&&!key.startsWith("controller.")&&!key.equals("device"))return;reload();changed.run();}
    @Override public void onInputDeviceAdded(int id){changed.run();}
    @Override public void onInputDeviceRemoved(int id){changed.run();}
    @Override public void onInputDeviceChanged(int id){changed.run();}
}
