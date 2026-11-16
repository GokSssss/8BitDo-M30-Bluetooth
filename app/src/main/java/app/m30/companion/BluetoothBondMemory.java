package app.m30.companion;

import android.bluetooth.*;
import android.content.*;
import android.Manifest;
import android.os.Build;
import android.content.pm.PackageManager;
import java.util.*;

/** A successful adapter snapshot, never a scan/cache/name, is the saved-pair authority. */
final class BluetoothBondMemory {
    static final String STORE="bluetooth_known_bonds",KEY="addresses";
    static Set<BluetoothDevice> read(Context context,BluetoothAdapter adapter){
        if(Build.VERSION.SDK_INT>=31&&context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return null;
        Set<BluetoothDevice> snapshot;Set<String> current=new HashSet<>();
        try{if(adapter==null||!adapter.isEnabled())return null;snapshot=adapter.getBondedDevices();if(snapshot==null)return null;for(BluetoothDevice d:snapshot)current.add(d.getAddress());}catch(SecurityException unavailable){return null;}
        SharedPreferences known=context.getSharedPreferences(STORE,0),links=context.getSharedPreferences("bluetooth_controller_links",0);
        Set<String> previous=new HashSet<>(known.getStringSet(KEY,Collections.emptySet()));
        // Existing links could only be made for a saved pair; migrate them without erasing labels on never-paired discoveries.
        for(String address:links.getAll().keySet())if(BluetoothSlotLinks.address(address)!=null)previous.add(address);
        previous.removeAll(current);for(String address:previous)clear(context,address);
        if(!current.equals(known.getStringSet(KEY,Collections.emptySet())))known.edit().putStringSet(KEY,current).apply();
        return new HashSet<>(snapshot);
    }
    static void removed(Context context,String address){
        clear(context,address);SharedPreferences known=context.getSharedPreferences(STORE,0);Set<String> values=new HashSet<>(known.getStringSet(KEY,Collections.emptySet()));if(values.remove(address))known.edit().putStringSet(KEY,values).apply();
    }
    static void clear(Context context,String address){
        if(BluetoothSlotLinks.address(address)==null)return;
        context.getSharedPreferences("bluetooth_device_labels",0).edit().remove("title:"+address).remove("badge:"+address).apply();
        context.getSharedPreferences("bluetooth_modes",0).edit().remove(address).apply();
        context.getSharedPreferences("bluetooth_controller_links",0).edit().remove(address).apply();
    }
}
