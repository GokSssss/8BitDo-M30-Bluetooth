package app.m30.companion;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.Gravity;
import android.widget.Button;

/** Explicit metrics avoid OEM Button insets, transforms and clipped TV labels. */
final class ActionButton extends Button {
    private boolean infoMode,adjustingInfo;
    private CharSequence infoTitle;
    ActionButton(Context c,String title,int color){
        super(c,null,0);setText(title);setAllCaps(false);setTransformationMethod(null);setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));setTextSize(13);setLetterSpacing(0);setIncludeFontPadding(false);setSingleLine(true);setHorizontallyScrolling(false);setEllipsize(null);setGravity(Gravity.START|Gravity.CENTER_VERTICAL);setTextColor(color);setMinWidth(0);setMinimumWidth(0);setMinHeight(dp(44));setMinimumHeight(dp(44));setPadding(dp(12),dp(6),dp(12),dp(6));setStateListAnimator(null);GradientDrawable bg=new GradientDrawable();bg.setColor(0xff181818);bg.setCornerRadius(dp(10));setBackground(bg);
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    void icon(int id,int color){Drawable image=getContext().getDrawable(id).mutate();image.setTint(color);image.setBounds(0,0,dp(16),dp(16));setCompoundDrawablesRelative(image,null,null,null);setCompoundDrawablePadding(dp(6));}
    int desiredWidth(){Drawable icon=getCompoundDrawablesRelative()[0];return (int)Math.ceil(getPaint().measureText(getText().toString()))+getPaddingLeft()+getPaddingRight()+(icon==null?0:icon.getBounds().width()+getCompoundDrawablePadding())+dp(8);}
    static String dpadTitle(DpadDetector detector){return switch(detector.kind()){case DPAD->"D-pad";case LEFT->"Левый стик";case RIGHT->"Правый стик";default->"Крестовина";};}
    @Override public void setText(CharSequence text,BufferType type){if(infoMode&&!adjustingInfo)infoTitle=text;super.setText(text,type);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        if(infoMode&&infoTitle!=null){int width=android.view.View.MeasureSpec.getSize(widthSpec);Drawable icon=getCompoundDrawablesRelative()[0];int needed=(int)Math.ceil(getPaint().measureText(infoTitle.toString()))+dp(12)+(icon==null?0:icon.getBounds().width()+dp(6));boolean compact=android.view.View.MeasureSpec.getMode(widthSpec)!=android.view.View.MeasureSpec.UNSPECIFIED&&needed>width;CharSequence shown=compact?"":infoTitle;if(!getText().toString().contentEquals(shown)){adjustingInfo=true;super.setText(shown,BufferType.NORMAL);adjustingInfo=false;}int horizontal=compact&&icon!=null?Math.max(dp(6),(width-icon.getBounds().width())/2):dp(6);setPadding(horizontal,dp(5),horizontal,dp(5));setCompoundDrawablePadding(compact?0:dp(6));setTooltipText(infoTitle);}
        super.onMeasure(widthSpec,heightSpec);
    }
    void info(){infoTitle=getText();infoMode=true;setTextSize(12);setPadding(dp(6),dp(5),dp(6),dp(5));setGravity(Gravity.CENTER);}
}
