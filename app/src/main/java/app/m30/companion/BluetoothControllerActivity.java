package app.m30.companion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** The full search screen remains available from Additional. */
public final class BluetoothControllerActivity extends Activity {
    private BluetoothSearchController search;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);getWindow().setStatusBarColor(0xff000000);getWindow().setNavigationBarColor(0xff000000);
        search=new BluetoothSearchController(this,saved,false,0xffb6a45e,()->{setResult(RESULT_OK);finish();});setContentView(search.view());}
    @Override protected void onResume(){super.onResume();search.resume();}
    @Override protected void onPause(){search.pause();super.onPause();}
    @Override protected void onDestroy(){search.destroy();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putAll(search.save());}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);search.permissionResult(request);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);search.activityResult(request,result,data);}
}
