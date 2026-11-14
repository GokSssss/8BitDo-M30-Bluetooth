package app.m30.companion;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Solid dialog surface and consistent focus, including callers that use create().show(). */
final class FocusDialogBuilder extends AlertDialog.Builder {
    FocusDialogBuilder(Context context){super(context);}
    @Override public AlertDialog create(){AlertDialog dialog=super.create();dialog.setOnShowListener(d->style(dialog));return dialog;}
    @Override public AlertDialog show(){AlertDialog dialog=create();dialog.show();return dialog;}
    static void style(AlertDialog dialog){Window window=dialog.getWindow();if(window==null)return;GradientDrawable background=new GradientDrawable();background.setColor(0xff181818);background.setCornerRadius(14*dialog.getContext().getResources().getDisplayMetrics().density);window.setBackgroundDrawable(background);window.getDecorView().setBackground(background);WindowManager.LayoutParams params=window.getAttributes();params.alpha=1;params.dimAmount=.7f;window.setAttributes(params);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);FocusNavigation.prepare(window.getDecorView());}
    static void confirmation(AlertDialog dialog,CheckBox checkbox){Button confirm=dialog.getButton(AlertDialog.BUTTON_POSITIVE);confirm.setEnabled(checkbox.isChecked());FocusNavigation.route(checkbox,View.FOCUS_DOWN,confirm);FocusNavigation.route(confirm,View.FOCUS_UP,checkbox);checkbox.setOnCheckedChangeListener((v,on)->{confirm.setEnabled(on);if(on)confirm.post(()->{if(dialog.isShowing()&&checkbox.isChecked())confirm.requestFocusFromTouch();});});}
}
