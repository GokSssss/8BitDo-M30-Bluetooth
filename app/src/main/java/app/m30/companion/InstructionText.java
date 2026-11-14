package app.m30.companion;

import android.graphics.*;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import java.util.regex.*;

/** Button commands form one bold, unbreakable layout run. */
public final class InstructionText {
    private static final Pattern COMMAND=Pattern.compile("(?<![\\p{L}\\p{N}_])(?:L\\s*\\+\\s*R\\s*\\+\\s*START|SELECT\\s*\\+\\s*[↑↓←→]|[ABXY]\\s*\\+\\s*START|START|PAIR)(?![\\p{L}\\p{N}_])");
    public static CharSequence format(String text){
        SpannableString result=new SpannableString(text);Matcher matches=COMMAND.matcher(text);
        while(matches.find())result.setSpan(new CommandSpan(),matches.start(),matches.end(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return result;
    }
    private static final class CommandSpan extends ReplacementSpan {
        private Paint bold(Paint original){Paint paint=new Paint(original);paint.setTypeface(Typeface.create(original.getTypeface(),Typeface.BOLD));return paint;}
        @Override public int getSize(Paint original,CharSequence text,int start,int end,Paint.FontMetricsInt metrics){
            Paint paint=bold(original);if(metrics!=null)paint.getFontMetricsInt(metrics);
            return (int)Math.ceil(paint.measureText(text,start,end));
        }
        @Override public void draw(Canvas canvas,CharSequence text,int start,int end,float x,int top,int baseline,int bottom,Paint paint){canvas.drawText(text,start,end,x,baseline,bold(paint));}
    }
}
