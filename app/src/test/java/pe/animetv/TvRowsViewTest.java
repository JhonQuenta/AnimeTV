package pe.animetv;
import android.app.Activity;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w960dp-h540dp-land-xhdpi")
public class TvRowsViewTest {
    private ActivityController<Activity> controller;private Activity activity;private TvRowsView rows;
    @Before public void start(){controller=Robolectric.buildActivity(Activity.class).setup();activity=controller.get();rows=new TvRowsView(activity);activity.setContentView(rows);controller.visible().windowFocusChanged(true);}
    @After public void stop(){controller.pause().stop().destroy();}
    private void settle(){for(int n=0;n<3;n++){Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(300));activity.getWindow().getDecorView().measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));activity.getWindow().getDecorView().layout(0,0,1920,1080);}}
    private void fill(){for(int n=0;n<8;n++){TvRowsView.Rail rail=new TvRowsView.Rail(activity);rows.model.addView(rail,new LinearLayout.LayoutParams(-1,-2));for(int k=0;k<6;k++){View card=TvUi.button(activity,"Fila "+n+", anime "+k,null,()->{});rail.model.addView(card,new LinearLayout.LayoutParams(280,520));}}settle();rows.focusContent();settle();}
    @Test public void downFocusNavigatesNativeRowsAndKeepsFocusedCardVisible(){
        fill();assertNotNull(rows.findFocus());int previous=rows.grid.getSelectedPosition();
        for(int n=0;n<6;n++){View next=rows.findFocus().focusSearch(View.FOCUS_DOWN);assertNotNull("A lower row must be reachable",next);next.requestFocus();settle();assertTrue("DPAD must move to a later row: step="+n+" previous="+previous+" current="+rows.grid.getSelectedPosition()+" focus="+rows.findFocus(),rows.grid.getSelectedPosition()>previous);previous=rows.grid.getSelectedPosition();View focused=rows.findFocus();assertNotNull(focused);android.graphics.Rect visible=new android.graphics.Rect();assertTrue(focused.getGlobalVisibleRect(visible));assertEquals("Full focused card must be visible",focused.getHeight(),visible.height());}
    }
    @Test public void recyclingCanReturnToEarlierFocusedCard(){fill();TvRowsView.Rail first=(TvRowsView.Rail)rows.model.items.get(0);View target=first.model.items.get(2);first.restoreFocus(target);settle();rows.grid.setSelectedPosition(7);settle();rows.restoreFocus(target);settle();assertEquals(0,rows.grid.getSelectedPosition());assertTrue(target.hasFocus());}
    @Test public void headingsDoNotTrapDownFocus(){fill();rows.model.addView(TvUi.text(activity,"Más capítulos",21,TvUi.TEXT),1,new LinearLayout.LayoutParams(-1,-2));settle();TvRowsView.Rail first=(TvRowsView.Rail)rows.model.items.get(0);rows.restoreFocus(first.model.items.get(0));settle();View next=rows.findFocus().focusSearch(View.FOCUS_DOWN);assertNotNull(next);next.requestFocus();settle();assertEquals(2,rows.grid.getSelectedPosition());}
    @Test public void onlyVisibleRowsAreAttached(){fill();assertEquals(8,rows.model.items.size());assertTrue(rows.grid.getChildCount()<8);}
}





