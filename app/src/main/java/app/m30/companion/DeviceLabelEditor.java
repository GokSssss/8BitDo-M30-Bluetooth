package app.m30.companion;

import android.app.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

/** App-only title and number, opened only after choosing the local-label alternative. */
final class DeviceLabelEditor {
    private static int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
    private static EditText field(Activity a,String hint,String value,boolean number){EditText f=new EditText(a);f.setSingleLine(true);f.setTextSize(16);f.setHint(hint);f.setText(value);f.setPadding(dp(a,14),dp(a,10),dp(a,14),dp(a,10));f.setMinHeight(dp(a,48));f.setInputType(number?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);f.setFilters(new InputFilter[]{new InputFilter.LengthFilter(number?2:64)});f.setImeOptions(number?EditorInfo.IME_ACTION_DONE:EditorInfo.IME_ACTION_NEXT);return f;}
    static AlertDialog show(Activity a,String address,int owner,String suggested,Runnable changed){
        return show(a,address,owner,suggested,null,changed);
    }
    static AlertDialog show(Activity a,String address,int owner,String suggested,android.os.Bundle restored,Runnable changed){
        BluetoothControllerNames names=new BluetoothControllerNames(a,()->{});BluetoothDeviceLabel old=names.label(address);
        ScrollView scroll=new ScrollView(a);LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(a,14),0,dp(a,14),dp(a,4));scroll.addView(form);LinearLayout row=new LinearLayout(a);row.setBaselineAligned(false);form.addView(row);
        String initial=restored!=null?restored.getString("title",""):suggested==null||suggested.isEmpty()?old.title:suggested;EditText title=field(a,"Метка",initial,false);title.setTag(R.id.navigation_key,"bt:label-title");row.addView(title,new LinearLayout.LayoutParams(0,-2,1));int badge=old.badge==0&&owner>=0?owner+1:old.badge;EditText number=field(a,"№",restored!=null?restored.getString("number",""):badge==0?"":String.valueOf(badge),true);number.setTag(R.id.navigation_key,"bt:label-number");number.setContentDescription("Номер от 1 до 99, можно очистить");LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(dp(a,72),-2);np.leftMargin=dp(a,8);row.addView(number,np);
        TextView hint=new TextView(a);hint.setText("Название и номер будут видны только в этом приложении.");hint.setTextSize(12);hint.setTextColor(0xffbbb7a8);hint.setPadding(0,dp(a,6),0,0);form.addView(hint);
        AlertDialog dialog=new FocusDialogBuilder(a).setTitle("Метка в приложении").setView(scroll).setPositiveButton("Сохранить",(d,w)->{names.saveLabel(address,title.getText().toString(),BluetoothDeviceLabel.number(number.getText().toString()));changed.run();}).setNeutralButton("Сбросить",(d,w)->{names.saveLabel(address,"",0);changed.run();}).setNegativeButton("Отмена",null).show();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);number.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();return true;}return false;});return dialog;
    }
    static android.os.Bundle save(AlertDialog dialog){android.os.Bundle out=new android.os.Bundle();if(dialog!=null&&dialog.getWindow()!=null){View root=dialog.getWindow().getDecorView();View title=FocusNavigation.find(root,"bt:label-title"),number=FocusNavigation.find(root,"bt:label-number");if(title instanceof EditText)out.putString("title",((EditText)title).getText().toString());if(number instanceof EditText)out.putString("number",((EditText)number).getText().toString());}return out;}
}
