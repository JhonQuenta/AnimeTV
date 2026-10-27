package pe.animetv;

import android.app.Dialog;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static pe.animetv.Models.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w960dp-h540dp-land-xhdpi")
public class EpisodeWatchedTest {
    private ActivityController<TvScreenTest.FakeActivity> controller;
    private MainActivity activity;private Store store;
    @Before public void start(){controller=Robolectric.buildActivity(TvScreenTest.FakeActivity.class).setup().visible().windowFocusChanged(true);activity=controller.get();store=new Store(activity);}
    @After public void stop(){controller.pause().stop().destroy();}
    private Object call(String name,Class<?>[] types,Object...args)throws Exception{Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(activity,args);}
    private Detail detail(int count){List<Episode> episodes=new ArrayList<>();for(int n=1;n<=count;n++)episodes.add(new Episode("Episodio "+n,"https://example.org/watched/"+n));return new Detail(new Anime("Prueba","https://example.org/watched",""),"",episodes);}
    private void showMenu(Detail d,int index,View origin,boolean held)throws Exception{call("episodeMenu",new Class<?>[]{Detail.class,int.class,View.class,boolean.class},d,index,origin,held);}
    private View option(String tag){return org.robolectric.shadows.ShadowDialog.getLatestDialog().getWindow().getDecorView().findViewWithTag(tag);}
    @Test public void explicitWatchAndUnwatchAlwaysPreserveResume(){String url="manual";store.progress(url,30000,600000);store.watched(url,true);assertTrue(store.watched(url));assertEquals(30000,store.position(url));store.watched(url,false);assertFalse(store.watched(url));assertEquals(30000,store.position(url));store.progress(url,599000,600000);assertTrue(store.watched(url));store.watched(url,false);assertFalse(new Store(activity).watched(url));assertEquals(599000,store.position(url));}
    private TvRowsView episodeRows(boolean compact)throws Exception{store.episodeList(compact);TvRowsView rows=new TvRowsView(activity);activity.setContentView(rows);Class<?> browser=Arrays.stream(MainActivity.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("EpisodeBrowser")).findFirst().orElseThrow();Constructor<?> constructor=browser.getDeclaredConstructor(MainActivity.class,LinearLayout.class,Detail.class);constructor.setAccessible(true);Object instance=constructor.newInstance(activity,rows.model,detail(3));Method create=browser.getDeclaredMethod("create");create.setAccessible(true);create.invoke(instance);Shadows.shadowOf(Looper.getMainLooper()).idle();rows.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));rows.layout(0,0,1920,1080);return rows;}
    @Test public void compactRowsExposeMenuAndKeepArrowNavigation()throws Exception{TvRowsView rows=episodeRows(true);View first=rows.model.items.get(1),second=rows.model.items.get(2);assertTrue(first.isLongClickable());assertTrue(first.performLongClick());assertNotNull(option("episode-toggle-watched"));option("episode-menu-cancel").performClick();rows.restoreFocus(first);Shadows.shadowOf(Looper.getMainLooper()).idle();first.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(second.hasFocus());}
    @Test public void galleryCardsExposeSameEpisodeMenu()throws Exception{TvRowsView rows=episodeRows(false);LinearLayout gallery=(LinearLayout)rows.model.items.get(1);View card=gallery.getChildAt(1);assertTrue(card.isLongClickable());card.performLongClick();assertNotNull(option("episode-mark-previous"));option("episode-toggle-watched").performClick();assertTrue(store.watched(detail(3).episodes().get(1).url()));}
    @Test public void previousMeansOneToFiftyAndDoesNotChangeFiftyOneOrHistory(){Detail d=detail(150);String current=d.episodes().get(50).url();store.progress(current,3000,60000);store.markPreviousEpisodes(d.episodes(),50);for(int n=0;n<50;n++)assertTrue(store.watched(d.episodes().get(n).url()));assertFalse(store.watched(current));assertFalse(store.watched(d.episodes().get(51).url()));assertEquals(3000,store.position(current));assertEquals(0,store.history().length());}
    @Test public void menuTogglesAndDoesNotOfferPreviousForFirstEpisode()throws Exception{Detail d=detail(2);View row=new View(activity);showMenu(d,0,row,false);assertNull(option("episode-mark-previous"));option("episode-toggle-watched").performClick();assertTrue(store.watched(d.episodes().get(0).url()));showMenu(d,0,row,false);assertTrue(option("episode-toggle-watched").getContentDescription().toString().contains("Desmarcar"));option("episode-toggle-watched").performClick();assertFalse(store.watched(d.episodes().get(0).url()));}
    @Test public void chapterFiftyOneMenuMarksPreviousEvenAcrossDisplayGroups()throws Exception{Detail d=detail(150);showMenu(d,50,new View(activity),false);option("episode-mark-previous").performClick();assertTrue(store.watched(d.episodes().get(49).url()));assertFalse(store.watched(d.episodes().get(50).url()));showMenu(d,100,new View(activity),false);option("episode-mark-previous").performClick();assertTrue(store.watched(d.episodes().get(99).url()));assertFalse(store.watched(d.episodes().get(100).url()));}
    @Test public void originalHeldReleaseCannotAccidentallyMarkEpisode()throws Exception{Detail d=detail(2);showMenu(d,0,new View(activity),true);Dialog menu=org.robolectric.shadows.ShadowDialog.getLatestDialog();option("episode-toggle-watched").requestFocus();menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER));assertFalse(store.watched(d.episodes().get(0).url()));assertTrue(menu.isShowing());option("episode-menu-cancel").performClick();}
    @Test public void compactCheckIsAtRightAndCaptionRemainsUnmodified()throws Exception{Episode episode=detail(1).episodes().get(0);store.watched(episode.url(),true);LinearLayout row=(LinearLayout)call("compactEpisode",new Class<?>[]{Episode.class,Runnable.class},episode,(Runnable)()->{});row.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(88,View.MeasureSpec.EXACTLY));row.layout(0,0,1000,88);TextView caption=(TextView)row.getChildAt(0);View check=row.findViewWithTag("episode-watched-check");assertEquals(episode.name(),caption.getText());assertEquals(View.VISIBLE,check.getVisibility());assertTrue(check.getLeft()>800);assertEquals(1000-row.getPaddingRight(),check.getRight());}
    @Test public void galleryDesaturatesAndRestoresImageWhenUnmarked()throws Exception{Episode episode=detail(1).episodes().get(0);FrameLayout card=new TvUi.EpisodeCard(activity);ImageView image=new ImageView(activity);card.addView(image);store.watched(episode.url(),true);call("addProgress",new Class<?>[]{FrameLayout.class,String.class},card,episode.url());FrameLayout host=new FrameLayout(activity);host.addView(card);activity.setContentView(host);assertNotNull(image.getColorFilter());assertEquals(.65f,image.getAlpha(),.001f);store.watched(episode.url(),false);call("refreshProgress",new Class<?>[]{});assertNull(image.getColorFilter());assertEquals(1f,image.getAlpha(),.001f);}
}
