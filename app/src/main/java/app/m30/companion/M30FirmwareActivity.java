package app.m30.companion;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.usb.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.Locale;

/** Official-file update screen. Opening it never erases, writes or resets a controller. */
public final class M30FirmwareActivity extends Activity {
    private static final int BG=0xff000000,CARD=0xff181818,TEXT=0xfff6f3e8,MUTED=0xffbbb7a8,ACCENT=0xffb6a45e,SUCCESS=0xff73d39b;
    private static final String USB_ACTION="app.m30.companion.FIRMWARE_USB_PERMISSION";
    private M30Firmware image;private M30UpdateEngine.Probe probe;private String probedName,probeReport="",pendingName;
    private boolean busy,showUpdateResult=true;private LinearLayout body;private TextView status,installed,fileInfo,explanation;private ProgressBar progress;
    private Button query,pick,download,install,share;private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){public void run(){refresh();handler.postDelayed(this,1000);}};
    private final BroadcastReceiver permission=new BroadcastReceiver(){public void onReceive(Context context,Intent intent){
        if(!USB_ACTION.equals(intent.getAction())||pendingName==null)return;
        String name=pendingName;pendingName=null;UsbManager manager=usb();UsbDevice d=manager.getDeviceList().get(name);
        if(d==null||!manager.hasPermission(d)){busy=false;status.setText("USB-доступ не предоставлен или M30 отключён");refresh();return;}readController(d);
    }};
    private UsbManager usb(){return (UsbManager)getSystemService(USB_SERVICE);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;}
    private TextView label(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(7),0,dp(7));body.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
    private Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(48));lp.topMargin=dp(8);body.addView(b,lp);return b;}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(BG);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(12),dp(18),dp(20));scroll.addView(body);setContentView(scroll);
        if(Build.VERSION.SDK_INT>=30)body.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(18)+i.left,dp(12)+i.top,dp(18)+i.right,dp(20)+i.bottom);return insets;});
        label("Прошивка M30",22,TEXT).setTypeface(null,Typeface.BOLD);
        label("Официальные обновления · USB",14,ACCENT);
        label("Выключите M30. Удерживайте L + R + START до красного мигания верхнего индикатора рядом с PAIR и подключите кабель к телефону.",14,MUTED);
        label("Экспериментальное обновление по USB. Оставьте кабель подключённым до завершения.",12,MUTED);
        status=label("Подключите 8BitDo Boot и нажмите «Проверить M30».",14,TEXT);
        installed=label("Установленная версия: —",16,ACCENT);
        query=button("Проверить M30 · модель и версия",this::checkController);
        download=button("Скачать официальный файл 2.01",()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(OfficialM30Files.url(201))));}catch(ActivityNotFoundException error){status.setText("Нет приложения для загрузки файла");}});
        pick=button("Выбрать официальный DAT",this::chooseFile);
        fileInfo=label("Файл обновления: —",14,TEXT);explanation=label("",12,MUTED);
        install=button("Установить обновление",this::confirmInstall);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);body.addView(progress,new LinearLayout.LayoutParams(-1,dp(16)));
        share=button("Поделиться отчётом",this::shareReport);button("Назад",this::finish);
        IntentFilter filter=new IntentFilter(USB_ACTION);int receiverFlags=Build.VERSION.SDK_INT>=33?Context.RECEIVER_NOT_EXPORTED:0;registerReceiver(permission,filter,receiverFlags);
        File stored=new File(getFilesDir(),"m30-official.dat");if(stored.isFile())try(InputStream in=new FileInputStream(stored)){image=M30Firmware.parse(readBounded(in));}catch(Exception ignored){}
        probeReport=getSharedPreferences("firmware",0).getString("report","");refresh();FocusNavigation.prepare(scroll);
    }
    @Override protected void onResume(){super.onResume();handler.removeCallbacks(ticker);handler.post(ticker);}
    @Override protected void onPause(){handler.removeCallbacks(ticker);super.onPause();}
    @Override protected void onDestroy(){unregisterReceiver(permission);super.onDestroy();}
    private void chooseFile(){startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),30);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=30||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();busy=true;refresh();new Thread(()->{
            try(InputStream in=getContentResolver().openInputStream(uri)){
                byte[] bytes=readBounded(in);M30Firmware selected=M30Firmware.parse(bytes);
                if(OfficialM30Files.recognized(selected)){File tmp=new File(getFilesDir(),"m30-official.tmp");try(OutputStream out=new FileOutputStream(tmp)){out.write(bytes);}if(!tmp.renameTo(new File(getFilesDir(),"m30-official.dat")))throw new IOException("Не удалось сохранить файл");}
                runOnUiThread(()->{image=selected;showUpdateResult=false;busy=false;status.setText("Выбран файл M30 "+selected.versionText()+".");refresh();});
            }catch(Exception error){runOnUiThread(()->{image=null;showUpdateResult=false;busy=false;probeReport="Файл отклонён: "+error.getMessage();status.setText(probeReport);refresh();});}
        },"M30-file-check").start();
    }
    static byte[] readBounded(InputStream in)throws IOException{
        if(in==null)throw new IOException("Файл не открыт");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;
        while((n=in.read(chunk))!=-1){if(out.size()+n>M30Firmware.MAX_FILE_SIZE)throw new IOException("Файл больше 2 МБ");out.write(chunk,0,n);}return out.toByteArray();
    }
    private void checkController(){
        if(busy||M30UpdateService.running)return;showUpdateResult=false;probe=null;probedName=null;M30UpdateService.report="";M30UpdateService.percent=0;
        try{UsbDevice d=UsbM30Link.singleBoot(usb());busy=true;status.setText("Чтение модели и версии…");refresh();
            if(usb().hasPermission(d))readController(d);
            else{pendingName=d.getDeviceName();PendingIntent reply=PendingIntent.getBroadcast(this,31,new Intent(USB_ACTION).setPackage(getPackageName()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);usb().requestPermission(d,reply);}
        }catch(Exception error){busy=false;status.setText(error.getMessage());refresh();}
    }
    private void readController(UsbDevice d){new Thread(()->{
        try(UsbM30Link link=new UsbM30Link(usb(),d)){
            M30UpdateEngine.Probe found=M30UpdateEngine.probe(link);
            String text=String.format(Locale.ROOT,"M30 Companion 1.52 · официальный протокол Windows\nЗагрузчик 2DC8:3208 ответил.\nМодель: 0x%08X · M30 Bluetooth\nВерсия: %d.%02d\nКоманда чтения: 5 · адрес 0x01017040, CRC и поля ответа проверены.\nЗапись, стирание и перезапуск не выполнялись.",found.model,found.version/100,found.version%100);
            runOnUiThread(()->{probe=found;probedName=d.getDeviceName();probeReport=text;busy=false;status.setText("M30 определён. Можно выбрать официальный файл.");getSharedPreferences("firmware",0).edit().putString("report",text).apply();refresh();});
        }catch(Exception error){runOnUiThread(()->{probe=null;probedName=null;probeReport="Чтение модели/версии не завершено: "+error.getClass().getSimpleName()+" · "+error.getMessage()+"\nЗапись, стирание и перезапуск не выполнялись.";busy=false;status.setText(probeReport);refresh();});}
    },"M30-native-probe").start();}
    private FirmwareUpdateResult savedResult(){
        if(M30UpdateService.result!=null)return M30UpdateService.result;
        android.content.SharedPreferences prefs=getSharedPreferences("firmware",0);
        if(prefs.contains("result_kind"))return FirmwareUpdateResult.restore(prefs.getInt("result_kind",0),prefs.getInt("result_version",0),prefs.getString("result_report",""),prefs.getLong("result_time",0));
        FirmwareUpdateResult old=FirmwareUpdateResult.fromLegacyReport(prefs.getString("report",""));
        if(old.kind==FirmwareUpdateResult.SUCCESS)prefs.edit().putInt("result_kind",old.kind).putInt("result_version",old.version).putString("result_report",old.report).putLong("result_time",old.time).apply();
        return old;
    }
    private void refresh(){
        if(query==null)return;boolean running=M30UpdateService.running,enabled=!busy&&!running;FirmwareUpdateResult outcome=savedResult();
        boolean success=!running&&showUpdateResult&&outcome.kind==FirmwareUpdateResult.SUCCESS;
        if(probedName!=null){UsbDevice current=usb().getDeviceList().get(probedName);if(current==null||!UsbBootModel.supported(current)){probe=null;probedName=null;}}
        boolean sameVersion=probe!=null&&image!=null&&OfficialM30Files.recognized(image)&&image.version==probe.version;
        installed.setText(probe!=null?String.format(Locale.ROOT,"Установленная версия: %d.%02d",probe.version/100,probe.version%100):success?"Версия последнего обновления: "+outcome.versionText():running&&image!=null?"Устанавливается версия: "+image.versionText():"Установленная версия: —");
        installed.setTextColor(success||sameVersion?SUCCESS:ACCENT);
        fileInfo.setText(image==null?"Файл обновления: —":"Файл: M30 "+image.versionText()+" · "+image.size+" байт");
        explanation.setText(image==null?"Выберите DAT, скачанный с сервера 8BitDo.":OfficialM30Files.integrityNote(image));
        install.setText(sameVersion?"Уже установлена "+image.versionText():image==null?"Установить обновление":"Установить M30 "+image.versionText());
        query.setEnabled(enabled);pick.setEnabled(enabled);download.setEnabled(enabled);
        install.setEnabled(enabled&&probe!=null&&image!=null&&OfficialM30Files.recognized(image)&&image.version>probe.version);
        for(Button b:new Button[]{query,pick,download,install,share}){b.setBackground(shape(b.isEnabled()?CARD:0xff111111));b.setTextColor(b.isEnabled()?TEXT:0xff777777);}
        if(install.isEnabled()){install.setBackground(shape(ACCENT));install.setTextColor(BG);}
        progress.setVisibility(running||showUpdateResult&&outcome.kind!=FirmwareUpdateResult.NONE?View.VISIBLE:View.GONE);progress.setProgress(success?100:M30UpdateService.percent);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(success?SUCCESS:ACCENT));
        status.setTextColor(TEXT);status.setTextSize(14);status.setTypeface(null,Typeface.NORMAL);status.setBackgroundColor(android.graphics.Color.TRANSPARENT);status.setPadding(0,dp(7),0,dp(7));
        if(running)status.setText(M30UpdateService.message);
        else if(showUpdateResult&&outcome.kind!=FirmwareUpdateResult.NONE){
            status.setText(outcome.title());status.setTextSize(success?18:14);status.setTypeface(null,Typeface.BOLD);status.setPadding(dp(12),dp(12),dp(12),dp(12));
            status.setTextColor(success?SUCCESS:outcome.kind==FirmwareUpdateResult.ERROR?0xfff36b65:ACCENT);
            status.setBackground(shape(success?0xff11281a:outcome.kind==FirmwareUpdateResult.ERROR?0xff2a1515:0xff2d2b22));probeReport=outcome.report;
        }else if(sameVersion){
            status.setText("✓ На M30 установлена "+image.versionText());status.setTextSize(18);status.setTypeface(null,Typeface.BOLD);status.setTextColor(SUCCESS);status.setPadding(dp(12),dp(12),dp(12),dp(12));status.setBackground(shape(0xff11281a));
        }
    }
    private void confirmInstall(){
        if(image==null||probe==null||!install.isEnabled())return;
        new FocusDialogBuilder(this).setTitle("Установить M30 "+image.versionText()+"?")
            .setMessage("Официальный неизменённый файл, модель M30 Bluetooth.\n\n"+OfficialM30Files.integrityNote(image)+"\n\nВо время записи оставьте кабель подключённым. После завершения появится подтверждение установленной версии.")
            .setPositiveButton("Установить",(dialog,which)->{
                Intent job=new Intent(this,M30UpdateService.class).putExtra("usb_name",probedName).putExtra("sha256",image.sha256);
                showUpdateResult=true;try{startForegroundService(job);probe=null;probedName=null;handler.postDelayed(this::refresh,200);}catch(RuntimeException error){showUpdateResult=false;probeReport="Не удалось начать: "+error.getMessage();status.setText(probeReport);refresh();}
            }).setNegativeButton("Отмена",null).show();
    }
    private void shareReport(){String report=M30UpdateService.running?M30UpdateService.message:showUpdateResult&&savedResult().kind!=FirmwareUpdateResult.NONE?savedResult().report:probeReport;
        if(image!=null)report+="\n\nФайл: M30 "+image.versionText()+"\nSHA-256: "+image.sha256+"\n"+OfficialM30Files.integrityNote(image);
        try{startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,report.isEmpty()?UsbFirmwareProbe.inspect(this):report),"Отчёт обновления M30"));}catch(ActivityNotFoundException error){status.setText("Нет приложения для отправки отчёта");}
    }
}
