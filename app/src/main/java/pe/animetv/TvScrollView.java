package pe.animetv;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/** Keep the complete focused card inside the visible viewport, including its caption. */
public final class TvScrollView extends ScrollView {
    public TvScrollView(Context context){super(context);setFillViewport(true);}
    @Override public boolean requestChildRectangleOnScreen(View child,Rect rectangle,boolean immediate){
        // Ancestors propagate the request with the content container as `child`.
        // Scrolling that entire container back to its top caused the TV focus loop.
        View card=findFocus();
        if(card==null || card==this)card=child;
        if(!card.isFocusable())return super.requestChildRectangleOnScreen(child,rectangle,immediate);
        Rect bounds=new Rect(0,0,card.getWidth(),card.getHeight());
        ViewGroup body=(ViewGroup)getChildAt(0);
        body.offsetDescendantRectToMyCoords(card,bounds);
        bounds.offset(body.getLeft()-getScrollX(),body.getTop()-getScrollY());
        int top=getPaddingTop(),bottom=getHeight()-getPaddingBottom(),target=getScrollY();
        int gap=TvUi.dp(getContext(),8);
        if(bounds.height()+gap*2>bottom-top){target+=bounds.top-top-gap;}
        else if(bounds.bottom+gap>bottom){target+=bounds.bottom+gap-bottom;}
        else if(bounds.top-gap<top){target+=bounds.top-gap-top;}
        target=Math.max(0,target);if(target==getScrollY())return false;
        // Focus changes must settle before the next DPAD event; no competing animations.
        scrollTo(0,target);return true;
    }
}
