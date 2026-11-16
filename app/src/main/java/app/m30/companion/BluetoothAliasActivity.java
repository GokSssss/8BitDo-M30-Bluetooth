package app.m30.companion;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.companion.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

/** System action first; a persistent alternative is enabled only after explicit user consent. */
public final class BluetoothAliasActivity extends Activity {
    static final String OPERATION="operation",CONNECT="connect",DISCONNECT="disconnect",FORGET="forget";
    private static final int CONFIRM=1,PERMISSION=2;
    private String address,systemName,draft,screen="name",reason="";
    private int owner=-1;
    private boolean connect,disconnect,forget,done;
    private CompanionDeviceManager manager;
    private BluetoothDevice device;
    private AlertDialog prompt;
    private EditText nameField;
    private Bundle labelDraft;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable timeout=()->{if(operating())offerAlternative("Android не завершил запрос. Включите выбранный M30 и повторите либо используйте другой способ.");};
    static boolean supported(Context c){return Build.VERSION.SDK_INT>=31&&c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP);}
    static boolean connectSupported(Context c){return Build.VERSION.SDK_INT>=37&&c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP);}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);address=getIntent().getStringExtra("address");systemName=getIntent().getStringExtra("name");owner=getIntent().getIntExtra("owner",-1);connect=CONNECT.equals(getIntent().getStringExtra(OPERATION));disconnect=DISCONNECT.equals(getIntent().getStringExtra(OPERATION));forget=FORGET.equals(getIntent().getStringExtra(OPERATION));if(systemName==null)systemName="M30";draft=systemName;
        if(address==null||!BluetoothAdapter.checkBluetoothAddress(address)){new FocusDialogBuilder(this).setTitle("Устройство недоступно").setMessage("Обновите список Bluetooth и выберите устройство снова.").setPositiveButton("Закрыть",(d,w)->finish()).setOnCancelListener(d->finish()).show();return;}
        setResult(RESULT_CANCELED,new Intent().putExtra(OPERATION,operation()).putExtra("address",address));
        if(saved!=null){screen=saved.getString("screen","name");draft=saved.getString("draft",systemName);reason=saved.getString("reason","");labelDraft=saved.getBundle("local_draft");}
        if(screen.equals("local")){showLocalLabel();return;}
        if(screen.equals("offer")){offerAlternative(reason);return;}
        if((connect||disconnect)&&BluetoothCompatibility.external(this)){openExternal();return;}
        if(!connect&&!disconnect&&!forget&&BluetoothCompatibility.labels(this)){showLocalLabel();return;}
        if(saved==null&&getIntent().getBooleanExtra("alternative",false)){offerAlternative("Подключение выбранного M30 пока не подтверждено. Можно использовать системные настройки Bluetooth.");return;}
        if((forget?BluetoothActionPolicy.forget(Build.VERSION.SDK_INT,getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP)):disconnect?BluetoothActionPolicy.disconnect(Build.VERSION.SDK_INT,getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP),false):connect?BluetoothActionPolicy.connect(Build.VERSION.SDK_INT,getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP),true,false,false):BluetoothActionPolicy.rename(Build.VERSION.SDK_INT,getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP),false))!=BluetoothActionPolicy.NATIVE){offerAlternative(forget?"На этом Android прямое удаление пары недоступно.":disconnect?"На этом Android отключение геймпада из приложения недоступно.":connect?"На этом Android подключение сохранённого геймпада из приложения недоступно.":"На этом устройстве переименование в Bluetooth из приложения недоступно.");return;}
        if(screen.equals("permission"))return; // Android will return the outstanding permission result.
        if(screen.equals("confirming")){if(prepareDevice())handler.postDelayed(timeout,90_000);return;}
        if(screen.equals("associating")||connect||disconnect||forget){begin();return;}
        showName();
    }
    private String operation(){return forget?FORGET:disconnect?DISCONNECT:connect?CONNECT:"rename";}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void dismiss(){nameField=null;if(prompt!=null){prompt.dismiss();prompt=null;}}
    private void showName(){
        screen="name";LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),0,dp(16),dp(4));nameField=new EditText(this);nameField.setSingleLine(true);nameField.setTextSize(16);nameField.setText(draft);nameField.setPadding(dp(14),dp(10),dp(14),dp(10));nameField.setMinHeight(dp(48));nameField.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(64)});nameField.setImeOptions(EditorInfo.IME_ACTION_DONE);nameField.setTag(R.id.navigation_key,"bt:system-name");form.addView(nameField,new LinearLayout.LayoutParams(-1,-2));
        TextView info=new TextView(this);info.setText("Имя будет видно в настройках Bluetooth этого устройства.");info.setTextSize(12);info.setPadding(0,dp(6),0,0);form.addView(info);
        prompt=new FocusDialogBuilder(this).setTitle("Переименовать в Bluetooth").setView(form).setPositiveButton("Сохранить",null).setNegativeButton("Отмена",(d,w)->finish()).setOnCancelListener(d->finish()).show();prompt.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);prompt.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String text=nameField.getText().toString().trim();if(text.isEmpty()){nameField.setError("Введите имя");return;}draft=text;dismiss();begin();});nameField.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){prompt.getButton(AlertDialog.BUTTON_POSITIVE).performClick();return true;}return false;});
    }
    private boolean prepareDevice(){
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){offerAlternative("Android не предоставил доступ к Bluetooth.");return false;}
        try{BluetoothManager bluetooth=getSystemService(BluetoothManager.class);BluetoothAdapter adapter=bluetooth==null?null:bluetooth.getAdapter();manager=getSystemService(CompanionDeviceManager.class);if(adapter==null||!adapter.isEnabled()||manager==null){offerAlternative("Bluetooth выключен или системный доступ недоступен.");return false;}device=adapter.getRemoteDevice(address);java.util.Set<BluetoothDevice> saved=BluetoothBondMemory.read(this,adapter);if(saved==null){offerAlternative("Не удалось проверить пары Android. Проверьте доступ к Bluetooth.");return false;}boolean bonded=false;if(saved!=null)for(BluetoothDevice d:saved)if(address.equals(d.getAddress()))bonded=true;if(!bonded){if(forget){done=true;setResult(RESULT_OK,new Intent().putExtra(OPERATION,FORGET).putExtra("address",address));finish();}else if(connect||disconnect)missingPair();else offerAlternative("Сначала нужно подтвердить сопряжение с этим M30.");return false;}return true;}catch(SecurityException error){offerAlternative("Android не разрешил доступ к Bluetooth.");return false;}catch(RuntimeException error){offerAlternative("Не удалось обратиться к выбранному устройству Bluetooth.");return false;}
    }
    private void missingPair(){dismiss();screen="missing";prompt=new FocusDialogBuilder(this).setTitle("Пара больше не сохранена").setMessage("Устройства уже нет в парах Android. Включите PAIR на M30, найдите его и выполните новое сопряжение.").setPositiveButton("Закрыть",(d,w)->finish()).setOnCancelListener(d->finish()).show();}
    private void begin(){
        dismiss();screen="associating";
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){screen="permission";requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},PERMISSION);return;}
        if(!prepareDevice())return;
        try{if(associated()){performSystemAction();return;}handler.removeCallbacks(timeout);handler.postDelayed(timeout,90_000);AssociationRequest request=new AssociationRequest.Builder().addDeviceFilter(new BluetoothDeviceFilter.Builder().setAddress(address).build()).setSingleDevice(true).build();manager.associate(request,new CompanionDeviceManager.Callback(){
            @Override public void onDeviceFound(IntentSender sender){if(!operating())return;try{screen="confirming";startIntentSenderForResult(sender,CONFIRM,null,0,0,0);}catch(IntentSender.SendIntentException error){offerAlternative("Не удалось открыть подтверждение доступа Android.");}}
            @Override public void onFailure(CharSequence error){if(operating())offerAlternative("Android не подтвердил доступ к выбранному M30.");}
        },handler);}catch(RuntimeException error){offerAlternative("Android не разрешил это действие из приложения.");}
    }
    @SuppressWarnings("deprecation") private boolean associated(){if(forget)return BluetoothBondMemory.associationId(this,manager,address)>=0;for(String value:manager.getAssociations())if(address.equalsIgnoreCase(value))return true;return false;}
    private boolean operating(){return !done&&!isFinishing()&&!isDestroyed()&&(screen.equals("associating")||screen.equals("confirming"));}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] result){super.onRequestPermissionsResult(request,permissions,result);if(request!=PERMISSION||done)return;if(result.length>0&&result[0]==PackageManager.PERMISSION_GRANTED)begin();else offerAlternative("Доступ к Bluetooth не предоставлен.");}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=CONFIRM||!operating())return;handler.removeCallbacks(timeout);if(result!=RESULT_OK){offerAlternative("Вы не подтвердили доступ Android к выбранному M30.");return;}if(!prepareDevice())return;try{if(associated())performSystemAction();else offerAlternative("Android не подтвердил доступ к выбранному адресу.");}catch(RuntimeException error){offerAlternative("Не удалось проверить подтверждение Android.");}}
    private void performSystemAction(){
        if(!operating())return;
        if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){offerAlternative("Доступ к Bluetooth отозван.");return;}
        try{
            if(forget){if(Build.VERSION.SDK_INT<36){offerAlternative("На этом Android прямое удаление пары недоступно.");return;}int id=BluetoothBondMemory.associationId(this,manager,address);if(id<0){offerAlternative("Android не подтвердил доступ к выбранному M30.");return;}if(!BluetoothForget36.start(manager,id)){offerAlternative("Android не начал удаление пары. Пара и метка сохранены.");return;}done=true;setResult(RESULT_OK,new Intent().putExtra(OPERATION,FORGET).putExtra("address",address).putExtra("bond_removal_requested",true));Toast.makeText(this,"Android удаляет пару M30…",Toast.LENGTH_LONG).show();finish();}
            else if(disconnect){if(Build.VERSION.SDK_INT<37){offerAlternative("На этом Android отключение из приложения недоступно.");return;}int result=BluetoothConnect37.stop(device);if(result!=BluetoothStatusCodes.SUCCESS){offerAlternative("Android не начал отключение выбранного M30.");return;}done=true;setResult(RESULT_OK,new Intent().putExtra(OPERATION,operation()).putExtra("address",address));Toast.makeText(this,"Запрос отключения отправлен Android.",Toast.LENGTH_LONG).show();finish();}
            else if(connect){if(Build.VERSION.SDK_INT<37){offerAlternative("На этом Android подключение из приложения недоступно.");return;}int result=BluetoothConnect37.start(device);if(result!=BluetoothStatusCodes.SUCCESS){offerAlternative("Android не начал подключение выбранного M30.");return;}done=true;setResult(RESULT_OK,new Intent().putExtra(OPERATION,operation()).putExtra("address",address));Toast.makeText(this,"Запрос подключения отправлен. Нажмите кнопку на M30 для проверки.",Toast.LENGTH_LONG).show();finish();}
            else{if(Build.VERSION.SDK_INT<31){offerAlternative("На этом Android переименование в Bluetooth недоступно.");return;}int result=device.setAlias(draft);if(result==BluetoothStatusCodes.SUCCESS&&draft.equals(device.getAlias())){done=true;setResult(RESULT_OK,new Intent().putExtra(OPERATION,operation()).putExtra("address",address));Toast.makeText(this,"Имя в Bluetooth: "+draft,Toast.LENGTH_LONG).show();finish();}else offerAlternative("Android не подтвердил новое имя Bluetooth.");}
        }catch(SecurityException error){offerAlternative("Android запретил это действие из приложения.");}catch(RuntimeException error){offerAlternative("Не удалось выполнить действие через Bluetooth.");}catch(LinkageError unavailable){offerAlternative("Эта прошивка Android не поддерживает прямое действие.");}
    }
    private void offerAlternative(String message){
        if(done||isFinishing()||isDestroyed())return;handler.removeCallbacks(timeout);dismiss();screen="offer";reason=message;
        if(forget){prompt=new FocusDialogBuilder(this).setTitle("Удаление пары").setMessage(message+" Можно удалить пару через настройки Android; метка сохранится до подтверждения удаления.").setPositiveButton("Открыть настройки",(d,w)->openExternal()).setNegativeButton("Отмена",(d,w)->finish()).setOnCancelListener(d->finish()).show();return;}
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),0,dp(16),dp(4));TextView detail=new TextView(this);detail.setText(message);detail.setTextSize(14);form.addView(detail);CheckBox use=new CheckBox(this);use.setText((connect||disconnect)?"Использовать настройки Bluetooth":"Использовать метки в приложении");use.setTextSize(14);use.setMinHeight(dp(48));use.setTag(R.id.navigation_key,"bt:enable-alternative");form.addView(use);
        prompt=new FocusDialogBuilder(this).setTitle(disconnect?"Другой способ отключения":connect?"Другой способ подключения":"Другой способ переименования").setView(form).setPositiveButton("Продолжить",(d,w)->{BluetoothCompatibility.set(this,(connect||disconnect)?BluetoothCompatibility.EXTERNAL:BluetoothCompatibility.LABELS,true);if(connect||disconnect)openExternal();else showLocalLabel();}).setNegativeButton("Отмена",(d,w)->finish()).setOnCancelListener(d->finish()).show();FocusDialogBuilder.confirmation(prompt,use);
    }
    private void showLocalLabel(){
        dismiss();screen="local";String suggested=draft!=null&&!draft.equals(systemName)?draft:"";prompt=DeviceLabelEditor.show(this,address,owner,suggested,labelDraft,()->Toast.makeText(this,"Метка сохранена в приложении",Toast.LENGTH_SHORT).show());prompt.setOnDismissListener(d->{if(screen.equals("local")&&!isChangingConfigurations()){done=true;finish();}});
    }
    private void openExternal(){dismiss();screen="external";String text="В Bluetooth выберите «"+systemName+"» ("+address.substring(9)+") и нажмите «"+(forget?"Забыть устройство":disconnect?"Отключить":"Подключить")+"».";Toast.makeText(this,text,Toast.LENGTH_LONG).show();try{startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));setResult(RESULT_OK,new Intent().putExtra(OPERATION,operation()).putExtra("address",address));}catch(ActivityNotFoundException error){Toast.makeText(this,"Системные настройки Bluetooth недоступны",Toast.LENGTH_LONG).show();}done=true;finish();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(nameField!=null)draft=nameField.getText().toString();if(screen.equals("local"))out.putBundle("local_draft",DeviceLabelEditor.save(prompt));out.putString("screen",screen);out.putString("draft",draft);out.putString("reason",reason);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(prompt!=null)prompt.setOnDismissListener(null);dismiss();super.onDestroy();}
}
