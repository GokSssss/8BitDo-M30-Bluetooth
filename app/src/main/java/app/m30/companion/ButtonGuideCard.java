package app.m30.companion;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/** Fits the complete reference card to a landscape viewport, keeping its proportions. */
public final class ButtonGuideCard extends LinearLayout {
    private final int viewportHeight;

    public ButtonGuideCard(Context context){this(context,0);}

    public ButtonGuideCard(Context context,int viewportHeight){
        super(context);setOrientation(VERTICAL);this.viewportHeight=viewportHeight;
    }

    @Override protected void onMeasure(int widthSpec,int heightSpec){
        super.onMeasure(widthSpec,heightSpec);
        if(viewportHeight<=0)return;
        View diagram=null;
        for(int i=0;i<getChildCount();i++)if(getChildAt(i) instanceof ButtonGuideView)diagram=getChildAt(i);
        if(diagram==null)return;
        // Re-measure the real heading/profile/footer at the narrower width as well.
        // Never force the card's height: exceptionally large fonts can still scroll.
        for(int pass=0;pass<4&&getMeasuredHeight()>viewportHeight;pass++){
            int surroundingHeight=getMeasuredHeight()-diagram.getMeasuredHeight();
            int drawingHeight=Math.max(1,viewportHeight-surroundingHeight);
            int width=getPaddingLeft()+getPaddingRight()+(int)Math.floor(drawingHeight*RadialGuideGeometry.WIDTH/RadialGuideGeometry.HEIGHT);
            if(width>=getMeasuredWidth())break;
            int previousWidth=getMeasuredWidth(),previousHeight=getMeasuredHeight();
            super.onMeasure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),heightSpec);
            if(getMeasuredHeight()>=previousHeight){
                super.onMeasure(MeasureSpec.makeMeasureSpec(previousWidth,MeasureSpec.EXACTLY),heightSpec);break;
            }
        }
    }
}
