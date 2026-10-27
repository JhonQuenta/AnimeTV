package pe.animetv;

import android.os.Looper;
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
public class LibraryRefreshTest {
    private ActivityController<TvScreenTest.FakeActivity> controller;private MainActivity activity;private Store store;
    @Before public void start()throws Exception{controller=Robolectric.buildActivity(TvScreenTest.FakeActivity.class).setup().visible().windowFocusChanged(true);activity=controller.get();store=new Store(activity);settle();}
    @After public void stop(){controller.pause().stop().destroy();}
    private void settle()throws Exception{for(int n=0;n<10;n++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();}}
    private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
    private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
    private Object call(String name,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,args);}
    private View find(View v,Object tag){if(tag.equals(v.getTag()))return v;if(v instanceof TvRowsView){for(View item:((TvRowsView)v).model.items){View found=find(item,tag);if(found!=null)return found;}}else if(v instanceof TvRowsView.Rail){for(View item:((TvRowsView.Rail)v).model.items){View found=find(item,tag);if(found!=null)return found;}}else if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int n=0;n<g.getChildCount();n++){View found=find(g.getChildAt(n),tag);if(found!=null)return found;}}return null;}
    @Test public void returningFromDetailAddsContinueCardWithoutNavigatingAway()throws Exception{Anime anime=new Anime("Visto","https://example.org/seen","");View before=(View)field("catalogView");store.rememberAnime(anime);store.lastEpisode(anime.url(),"episode-2");store.progress("episode-2",60000,180000);set("detailView",new FrameLayout(activity));call("returnCatalog",new Class<?>[]{});settle();View after=(View)field("catalogView");assertNotSame(before,after);assertNotNull(find(after,"continue:"+anime.url()));assertEquals("Inicio",field("section"));}
    @Test public void favoriteRemovalIsVisibleWhenReturningFromDetail()throws Exception{Anime one=new Anime("Uno","https://example.org/one",""),two=new Anime("Dos","https://example.org/two","");store.favorite(one,"Por defecto",true);store.favorite(two,"Por defecto",true);call("showFavorites",new Class<?>[]{});View before=(View)field("catalogView");assertNotNull(find(before,one.url()));store.favorite(one,"Por defecto",false);set("detailView",new FrameLayout(activity));call("returnCatalog",new Class<?>[]{});settle();View after=(View)field("catalogView");assertNotSame(before,after);assertNull(find(after,one.url()));assertNotNull(find(after,two.url()));assertEquals("Favoritos",field("section"));}
    @Test public void onResumeRefreshesHomeAndDoesNotRebuildUnchangedFavorites()throws Exception{Anime anime=new Anime("Nuevo","https://example.org/new","");store.rememberAnime(anime);controller.pause().resume();settle();assertNotNull(find((View)field("catalogView"),"continue:"+anime.url()));call("showFavorites",new Class<?>[]{});View before=(View)field("catalogView");controller.pause().resume();assertSame(before,field("catalogView"));}
    @Test public void sameContinueCardUpdatesRemainingTimeOnResume()throws Exception{Anime anime=new Anime("Continuar","https://example.org/resume","");store.rememberAnime(anime);store.lastEpisode(anime.url(),"progress-episode");store.progress("progress-episode",60000,180000);call("home",new Class<?>[]{});settle();FrameLayout card=(FrameLayout)find((View)field("catalogView"),"continue:"+anime.url());View old=(View)field("catalogView");TvRowsView rows=(TvRowsView)old;rows.restoreFocus(card);settle();old.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));old.layout(0,0,1920,1080);store.progress("progress-episode",120000,180000);controller.pause().resume();settle();assertSame(old,field("catalogView"));assertTrue(((TextView)card.getChildAt(2)).getText().toString().contains("1 min restantes"));}
}
