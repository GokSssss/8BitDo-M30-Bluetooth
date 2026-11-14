package app.m30.companion;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.*;
import android.widget.AbsListView;

/** A foreground ring keeps focus visible without replacing selected/pressed/live backgrounds. */
final class FocusNavigation {
    private FocusNavigation(){}
    static String key(View view){Object key=view==null?null:view.getTag(R.id.navigation_key);return key instanceof String?(String)key:null;}
    static boolean horizontalEdge(View focused,int direction){
        if(focused==null||(direction!=View.FOCUS_LEFT&&direction!=View.FOCUS_RIGHT))return false;
        View next=focused.focusSearch(direction);
        if(next==null||next==focused||!next.isShown()||!next.isEnabled()||!next.isFocusable())return true;
        int[] from=new int[2],to=new int[2];focused.getLocationOnScreen(from);next.getLocationOnScreen(to);
        float currentCenter=from[0]+focused.getWidth()/2f,nextCenter=to[0]+next.getWidth()/2f;
        // An explicit route to a lower row must not wrap focus back across the screen.
        return direction==View.FOCUS_RIGHT?nextCenter<=currentCenter:nextCenter>=currentCenter;
    }
    static void prepare(View view){prepare(view,"root");}
    private static Drawable ring(View view){return new FocusOutline(view);}
    private static void prepare(View view,String path){
        if(view instanceof AbsListView){
            AbsListView list=(AbsListView)view;StateListDrawable selector=new StateListDrawable();
            selector.addState(new int[]{android.R.attr.state_pressed},ring(view));
            selector.addState(new int[]{android.R.attr.state_focused},ring(view));
            selector.addState(new int[]{android.R.attr.state_selected},ring(view));
            selector.addState(new int[]{},new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            list.setSelector(selector);list.setDrawSelectorOnTop(true);
        }else if(view.isClickable()){
            if(view instanceof android.widget.CheckBox){int inset=Math.round(8*view.getResources().getDisplayMetrics().density);if(view.getPaddingRight()<inset)view.setPadding(view.getPaddingLeft(),view.getPaddingTop(),inset,view.getPaddingBottom());}
            view.setFocusable(true);
            if(view.getId()==View.NO_ID)view.setId(View.generateViewId());
            if(key(view)==null)view.setTag(R.id.navigation_key,path);
            StateListDrawable states=new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_focused,android.R.attr.state_enabled},ring(view));
            states.addState(new int[]{},new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            view.setForeground(states);
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)prepare(group.getChildAt(i),path+":"+i);}
    }
    static void route(View from,int direction,View to){
        if(from==null||to==null)return;
        if(to.getId()==View.NO_ID)to.setId(View.generateViewId());
        if(direction==View.FOCUS_LEFT)from.setNextFocusLeftId(to.getId());
        if(direction==View.FOCUS_RIGHT)from.setNextFocusRightId(to.getId());
        if(direction==View.FOCUS_UP)from.setNextFocusUpId(to.getId());
        if(direction==View.FOCUS_DOWN)from.setNextFocusDownId(to.getId());
    }
    static View find(View root,String key){
        if(key==null)return null;if(key.equals(key(root)))return root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),key);if(found!=null)return found;}}
        return null;
    }
}
