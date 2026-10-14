package pe.animetv;
import android.app.Dialog;import android.os.Looper;import android.view.*;import android.widget.*;import android.graphics.*;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.lang.reflect.*;import java.io.*;
import okhttp3.*;import okhttp3.mockwebserver.*;import okio.Buffer;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
import static org.junit.Assert.*;import static pe.animetv.Models.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="w960dp-h540dp-land-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class RemoteFlowOptimizationTest {
    public static class FilterActivity extends TvScreenTest.FakeActivity {
        static CountDownLatch entered,release,cancelled;static AtomicInteger calls;
        protected Source createSource(){return new Source(){
            public String id(){return "fake";}public String name(){return "Fake";}public String baseUrl(){return "https://example.test";}
            public List<Anime> catalog(String q,int p,Map<String,String> f){return List.of();}
            public List<Models.Filter> filters()throws Exception{calls.incrementAndGet();entered.countDown();try{release.await();}catch(InterruptedException e){cancelled.countDown();throw e;}return List.of(new Models.Filter("type[]","Tipo",List.of("","0","1"),List.of("Todos","Serie","Película")));}
            public Detail detail(Anime a){return new Detail(a,"",List.of());}public List<Server> servers(Episode e){return List.of();}public Stream resolve(Server s,String e){return null;}public List<RecentEpisode> recent(){return List.of();}public List<CalendarEntry> calendar(){return List.of();}
        };}
    }
    private Object call(MainActivity a,String method,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(method,types);m.setAccessible(true);return m.invoke(a,args);}
    private Object field(MainActivity a,String key)throws Exception{Field f=MainActivity.class.getDeclaredField(key);f.setAccessible(true);return f.get(a);}
    private void key(Dialog d,int code){d.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));d.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));Shadows.shadowOf(Looper.getMainLooper()).idle();}
    private void reset(){FilterActivity.calls=new AtomicInteger();FilterActivity.entered=new CountDownLatch(1);FilterActivity.release=new CountDownLatch(1);FilterActivity.cancelled=new CountDownLatch(1);}
    @Test public void dpadTraversesSourceChoicesAndFooterThenBackRestoresOpener()throws Exception{
        var controller=Robolectric.buildActivity(TvScreenTest.FakeActivity.class).setup().visible().windowFocusChanged(true);MainActivity a=controller.get();
        try{
            Shadows.shadowOf(Looper.getMainLooper()).idle();View root=a.getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));root.layout(0,0,1920,1080);Shadows.shadowOf(Looper.getMainLooper()).idle();
            View opener=((View)field(a,"sectionBanner")).findViewWithTag("source-picker");assertTrue("Opener is attached="+opener.isAttachedToWindow(),opener.requestFocus());call(a,"sourcesInfo",new Class<?>[]{});Shadows.shadowOf(Looper.getMainLooper()).idle();Dialog d=org.robolectric.shadows.ShadowDialog.getLatestDialog();View decor=d.getWindow().getDecorView();View first=decor.findViewWithTag("source-choice:animeav1"),last=decor.findViewWithTag("source-choice:jkanime"),cancel=decor.findViewWithTag("source-cancel");assertTrue(first.requestFocus());
            for(String id:Sources.ids().subList(1,Sources.ids().size())){key(d,KeyEvent.KEYCODE_DPAD_DOWN);assertTrue(id,decor.findViewWithTag("source-choice:"+id).hasFocus());}
            key(d,KeyEvent.KEYCODE_DPAD_DOWN);assertTrue("Down from last choice must reach footer",cancel.hasFocus());key(d,KeyEvent.KEYCODE_DPAD_UP);assertTrue("Up from footer must reach last source",last.hasFocus());key(d,KeyEvent.KEYCODE_DPAD_DOWN);assertTrue("Footer remains reachable",cancel.hasFocus());key(d,KeyEvent.KEYCODE_BACK);assertFalse(d.isShowing());assertTrue("Dismiss restores original source button",opener.hasFocus());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void repeatedFilterPressesShareRequestAndCannotOpenStackedDrawers()throws Exception{
        reset();var controller=Robolectric.buildActivity(FilterActivity.class).setup().visible();MainActivity a=controller.get();
        try{call(a,"showFilters",new Class<?>[]{});assertTrue(FilterActivity.entered.await(2,TimeUnit.SECONDS));for(int n=0;n<4;n++)call(a,"showFilters",new Class<?>[]{});assertEquals(1,FilterActivity.calls.get());FilterActivity.release.countDown();
            for(int n=0;n<80;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(field(a,"filtersDialog")!=null)break;Thread.sleep(25);}Dialog first=(Dialog)field(a,"filtersDialog");assertNotNull(first);call(a,"showFilters",new Class<?>[]{});assertSame(first,field(a,"filtersDialog"));assertEquals(1,FilterActivity.calls.get());key(first,KeyEvent.KEYCODE_BACK);assertFalse(first.isShowing());assertNull(field(a,"filtersDialog"));
        }finally{FilterActivity.release.countDown();controller.pause().stop().destroy();}
    }
    @Test public void navigationCancelsPendingFiltersAndNoLatePopupAppears()throws Exception{
        reset();var controller=Robolectric.buildActivity(FilterActivity.class).setup().visible();MainActivity a=controller.get();
        try{call(a,"showFilters",new Class<?>[]{});assertTrue(FilterActivity.entered.await(2,TimeUnit.SECONDS));call(a,"settings",new Class<?>[]{});assertTrue(FilterActivity.cancelled.await(2,TimeUnit.SECONDS));Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(field(a,"filtersDialog"));assertFalse((Boolean)field(a,"filtersLoading"));assertEquals("Ajustes",field(a,"section"));}
        finally{FilterActivity.release.countDown();controller.pause().stop().destroy();}
    }
    @Test public void originalImageBytesArePreservedExactly()throws Exception{
        try(MockWebServer server=new MockWebServer()){byte[] input=new byte[1024];new Random(42).nextBytes(input);server.enqueue(new MockResponse().setBody(new Buffer().write(input)));server.start();assertArrayEquals(input,ArtworkBytes.get(new OkHttpClient(),new Request.Builder().url(server.url("/art")).build()));}
    }
    @Test public void preloadTargetsSavedEpisodeInsteadOfAlwaysEpisodeOne()throws Exception{
        try(MockWebServer server=new MockWebServer()){server.enqueue(new MockResponse().setBody("<script>var videos=[];</script>"));server.start();var controller=Robolectric.buildActivity(TvScreenTest.FakeActivity.class).setup().visible();MainActivity a=controller.get();
            try{TioAnimeSource source=new TioAnimeSource(server.url("/").toString().replaceAll("/$",""));Field f=MainActivity.class.getDeclaredField("source");f.setAccessible(true);f.set(a,source);Anime anime=new Anime("Test",server.url("/anime/test").toString(),"");List<Episode> episodes=List.of(new Episode("1",server.url("/ver/test-1").toString()),new Episode("3",server.url("/ver/test-3").toString()));Store store=new Store(a);store.serverDefault("tioanime","auto");store.lastEpisode(anime.url(),episodes.get(1).url());call(a,"warmEpisode",new Class<?>[]{Detail.class,String.class,int.class},new Detail(anime,"",episodes),null,(Integer)field(a,"requestGeneration"));RecordedRequest request=server.takeRequest(3,TimeUnit.SECONDS);assertNotNull(request);assertEquals("/ver/test-3",request.getPath());}
            finally{new Store(a).serverDefault("tioanime","ask");controller.pause().stop().destroy();}
        }
    }
    @Test public void interruptedImageDownloadCancelsItsActualCall()throws Exception{
        try(MockWebServer server=new MockWebServer()){server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));server.start();AtomicBoolean cancelled=new AtomicBoolean();Thread worker=new Thread(()->{try{ArtworkBytes.get(new OkHttpClient(),new Request.Builder().url(server.url("/art")).build());}catch(IOException e){cancelled.set(e.getCause() instanceof InterruptedException);}});worker.start();assertNotNull(server.takeRequest(3,TimeUnit.SECONDS));worker.interrupt();worker.join(2000);assertFalse(worker.isAlive());assertTrue(cancelled.get());}
    }
    @Test public void replacingArtworkUrlCancelsOldImageWithoutOverwritingNewPortrait()throws Exception{
        try(MockWebServer server=new MockWebServer()){server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));server.start();var controller=Robolectric.buildActivity(TvScreenTest.FakeActivity.class).setup().visible();MainActivity a=controller.get();
            try{ImageView view=new ImageView(a);a.setContentView(view);call(a,"image",new Class<?>[]{ImageView.class,String.class},view,server.url("/old.png").toString());assertNotNull(server.takeRequest(3,TimeUnit.SECONDS));Future<?> old=(Future<?>)((Map<?,?>)field(a,"artworkTasks")).values().iterator().next();Bitmap image=Bitmap.createBitmap(20,30,Bitmap.Config.ARGB_8888);image.eraseColor(Color.RED);ByteArrayOutputStream out=new ByteArrayOutputStream();image.compress(Bitmap.CompressFormat.PNG,100,out);image.recycle();server.enqueue(new MockResponse().setBody(new Buffer().write(out.toByteArray())));call(a,"image",new Class<?>[]{ImageView.class,String.class},view,server.url("/new.png").toString());assertTrue(old.isCancelled());for(int n=0;n<120&&view.getDrawable()==null;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(25);}assertNotNull(view.getDrawable());assertEquals(Color.RED,((android.graphics.drawable.BitmapDrawable)view.getDrawable()).getBitmap().getPixel(0,0));}
            finally{controller.pause().stop().destroy();}
        }
    }
    @Test public void sharedArtworkCancelsOnlyAfterLastSubscriberAndLateFailureCannotRemoveReplacement()throws Exception{
        try(MockWebServer server=new MockWebServer()){
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));server.start();var controller=Robolectric.buildActivity(TvScreenTest.FakeActivity.class).setup().visible();MainActivity a=controller.get();
            try{
                LinearLayout root=new LinearLayout(a);ImageView one=new ImageView(a),two=new ImageView(a),replacement=new ImageView(a);root.addView(one);root.addView(two);root.addView(replacement);a.setContentView(root);String url=server.url("/same.png").toString();call(a,"image",new Class<?>[]{ImageView.class,String.class},one,url);call(a,"image",new Class<?>[]{ImageView.class,String.class},two,url);assertNotNull(server.takeRequest(3,TimeUnit.SECONDS));Map<?,?> tasks=(Map<?,?>)field(a,"artworkTasks");assertEquals(1,tasks.size());Future<?> shared=(Future<?>)tasks.values().iterator().next();call(a,"detachArtwork",new Class<?>[]{ImageView.class},one);assertFalse(shared.isCancelled());call(a,"detachArtwork",new Class<?>[]{ImageView.class},two);assertTrue(shared.isCancelled());
                Bitmap bitmap=Bitmap.createBitmap(20,30,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.GREEN);ByteArrayOutputStream bytes=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.PNG,100,bytes);bitmap.recycle();server.enqueue(new MockResponse().setBody(new Buffer().write(bytes.toByteArray())));call(a,"image",new Class<?>[]{ImageView.class,String.class},replacement,url);
                for(int n=0;n<120&&replacement.getDrawable()==null;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(25);}assertNotNull(replacement.getDrawable());Bitmap actual=((android.graphics.drawable.BitmapDrawable)replacement.getDrawable()).getBitmap();assertEquals(Color.GREEN,actual.getPixel(0,0));assertEquals(2,server.getRequestCount());
            }finally{controller.pause().stop().destroy();}
        }
    }
}
