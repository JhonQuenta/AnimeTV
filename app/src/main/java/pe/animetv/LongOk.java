package pe.animetv;

import android.view.*;
import java.util.function.Consumer;

/** One menu per held OK; releasing after the menu opens must never trigger a click. */
final class LongOk implements View.OnKeyListener,View.OnAttachStateChangeListener {
    private final View view;
    private boolean held,triggered;
    private final Runnable timeout;
    static void bind(View view,Consumer<Boolean> openMenu){new LongOk(view,openMenu);}
    private LongOk(View view,Consumer<Boolean> openMenu){
        this.view=view;
        timeout=()->{if(held&&view.hasFocus()&&view.isAttachedToWindow()){triggered=true;view.performLongClick();}};
        view.setOnLongClickListener(v->{openMenu.accept(held);return true;});
        view.setOnKeyListener(this);view.addOnAttachStateChangeListener(this);
        View.OnFocusChangeListener previous=view.getOnFocusChangeListener();
        view.setOnFocusChangeListener((v,focused)->{if(!focused)cancel();if(previous!=null)previous.onFocusChange(v,focused);});
    }
    private static boolean ok(int key){return key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_NUMPAD_ENTER;}
    public boolean onKey(View v,int key,KeyEvent event){
        if(!ok(key))return false;
        if(event.getAction()==KeyEvent.ACTION_DOWN){
            if(event.getRepeatCount()==0){view.removeCallbacks(timeout);held=true;triggered=false;view.setPressed(true);view.postDelayed(timeout,ViewConfiguration.getLongPressTimeout());}
            return true;
        }
        if(event.getAction()==KeyEvent.ACTION_UP){
            boolean click=held&&!triggered&&!event.isCanceled();held=false;view.removeCallbacks(timeout);view.setPressed(false);
            if(click)view.performClick();return true;
        }
        return false;
    }
    private void cancel(){held=false;view.removeCallbacks(timeout);view.setPressed(false);}
    public void onViewDetachedFromWindow(View v){cancel();}
    public void onViewAttachedToWindow(View v){}
    static void consumeHeldRelease(android.app.Dialog dialog,boolean held){
        if(!held)return;final boolean[] pending={true};
        dialog.setOnKeyListener((d,key,event)->{if(!pending[0]||!ok(key))return false;if(event.getAction()==KeyEvent.ACTION_UP)pending[0]=false;return true;});
    }
}
