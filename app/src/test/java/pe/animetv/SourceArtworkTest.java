package pe.animetv;
import android.app.*;import android.graphics.*;import android.os.Looper;import android.view.*;import android.widget.*;
import java.util.*;import java.util.concurrent.*;import java.lang.reflect.*;import java.io.*;
import okhttp3.mockwebserver.*;import okio.Buffer;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
import static org.junit.Assert.*;import static pe.animetv.Models.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="w960dp-h540dp-land-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SourceArtworkTest {
    @Test public void bannerReachesAllViewportEdgesWhileContentKeepsSafeInsets()throws Exception{
        var controller=Robolectric.buildActivity(TvScreenTest.FakeActivity.class).setup().visible();MainActivity activity=controller.get();
        try{
            TvRowsView rows=new TvRowsView(activity);rows.grid.setPadding(TvUi.dp(activity,24),TvUi.dp(activity,24),TvUi.dp(activity,24),TvUi.dp(activity,24));activity.setContentView(rows);
            Detail d=new Detail(new Anime("Kanata kara","test/banner",""),"Sinopsis",List.of(new Episode("1","test/1")),List.of(),"Serie","2026","En emisión","https://example.test/banner.png");call(activity,"renderDetail",new Class<?>[]{LinearLayout.class,Detail.class,int.class},rows.model,d,0);
            for(int n=0;n<3;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();layout(rows,1920,1080);}assertEquals(0,rows.grid.getPaddingTop());assertEquals(0,rows.grid.getPaddingLeft());assertEquals(0,rows.grid.getPaddingRight());
            FrameLayout background=(FrameLayout)rows.model.items.get(0);assertEquals(0,background.getLeft());assertEquals(1920,background.getWidth());LinearLayout hero=(LinearLayout)background.getChildAt(3);assertEquals(TvUi.dp(activity,24),hero.getPaddingLeft());assertEquals(TvUi.dp(activity,24),hero.getPaddingTop());ViewGroup.MarginLayoutParams margin=(ViewGroup.MarginLayoutParams)rows.model.items.get(1).getLayoutParams();assertEquals(TvUi.dp(activity,24),margin.leftMargin);assertEquals(TvUi.dp(activity,24),margin.rightMargin);
            Bitmap art=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);art.eraseColor(0xFF307060);((ImageView)background.getChildAt(0)).setImageBitmap(art);Bitmap preview=Bitmap.createBitmap(1920,1080,Bitmap.Config.ARGB_8888);rows.draw(new Canvas(preview));File folder=new File(System.getProperty("animetv.previewDir"));folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,"ficha-fondo-026.png"))){preview.compress(Bitmap.CompressFormat.PNG,100,out);}preview.recycle();art.recycle();
        }finally{controller.pause().stop().destroy();}
    }
    public static class LatActivity extends TvScreenTest.FakeActivity {
        static String poster;static int metadataCalls;
        protected String sourceIdentity(){return "latanime";}
        protected Source createSource(){return new Source(){
            public String id(){return sourceIdentity();}public String name(){return "Latanime";}public String baseUrl(){return "http://localhost";}
            public List<Anime> catalog(String q,int p,Map<String,String> f){return List.of();}public List<Models.Filter> filters(){return List.of();}
            public Detail detail(Anime a){metadataCalls++;return new Detail(new Anime(a.title(),a.url(),poster,"Serie","Finalizado"),"",List.of());}
            public List<Server> servers(Episode e){return List.of();}public Stream resolve(Server s,String e){return null;}
            public List<RecentEpisode> recent(){return List.of();}public List<CalendarEntry> calendar(){return List.of();}
        };}
    }
    public static class HdActivity extends LatActivity {protected String sourceIdentity(){return "animelatinohd";}}
    @Test public void hdMissingRecentPosterStillLoadsRealPortraitWithKnownState()throws Exception{
        try(MockWebServer server=new MockWebServer()){
            server.start();Bitmap b=Bitmap.createBitmap(80,120,Bitmap.Config.ARGB_8888);ByteArrayOutputStream data=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,data);b.recycle();server.enqueue(new MockResponse().setBody(new Buffer().write(data.toByteArray())));LatActivity.poster=server.url("/hd-portrait.png").toString();LatActivity.metadataCalls=0;
            var controller=Robolectric.buildActivity(HdActivity.class).setup().visible();HdActivity activity=controller.get();try{
                Anime anime=new Anime("Kanata kara",server.url("/anime/kanata-kara").toString(),"","Serie","En emisión");FrameLayout card=(FrameLayout)call(activity,"portrait",new Class<?>[]{Anime.class,String.class,Runnable.class},anime,"",(Runnable)()->{});FrameLayout root=new FrameLayout(activity);root.addView(card,new FrameLayout.LayoutParams(280,420));activity.setContentView(root);layout(root,400,500);
                for(int n=0;n<120;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(server.getRequestCount()>0)break;Thread.sleep(25);}assertEquals(1,LatActivity.metadataCalls);assertEquals(LatActivity.poster,new Store(activity).poster(anime.url()));assertEquals("/hd-portrait.png",server.takeRequest(3,TimeUnit.SECONDS).getPath());
            }finally{controller.pause().stop().destroy();}
        }
    }
    private Object call(MainActivity activity,String method,Class<?>[] args,Object...values)throws Exception{Method m=MainActivity.class.getDeclaredMethod(method,args);m.setAccessible(true);return m.invoke(activity,values);}
    private void layout(View view,int w,int h){view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);}
    @Test public void commonHorizontalArtIsEligibleButIndividualEpisodeFramesAreNot(){
        String common="https://latanime.org/thumbs/portada/mushoku.webp";
        assertEquals(common,LatanimeSource.sharedArtwork(List.of(new Episode("1","/1",common),new Episode("2","/2",common))));
        assertEquals("",LatanimeSource.sharedArtwork(List.of(new Episode("1","/1",common),new Episode("2","/2",common+"2"))));
        assertEquals("",LatanimeSource.sharedArtwork(List.of(new Episode("1","/1",common))));
        assertEquals("",LatanimeSource.sharedArtwork(List.of(new Episode("1","/1","https://latanime.org/screenshots/1.jpg"),new Episode("2","/2","https://latanime.org/screenshots/1.jpg"))));
    }
    @Test public void horizontalRecentGetsRealPosterEvenWhenTypeAndStateAreKnown()throws Exception{
        try(MockWebServer server=new MockWebServer()){
            server.start();Bitmap image=Bitmap.createBitmap(80,120,Bitmap.Config.ARGB_8888);image.eraseColor(Color.BLUE);ByteArrayOutputStream png=new ByteArrayOutputStream();image.compress(Bitmap.CompressFormat.PNG,100,png);image.recycle();server.enqueue(new MockResponse().setBody(new Buffer().write(png.toByteArray())));
            LatActivity.poster=server.url("/real-vertical.png").toString();LatActivity.metadataCalls=0;
            var controller=Robolectric.buildActivity(LatActivity.class).setup().visible();LatActivity activity=controller.get();
            try{
                Anime anime=new Anime("Mushoku",server.url("/anime/mushoku").toString(),server.url("/thumbs/portada/horizontal.png").toString(),"Serie","Finalizado");
                FrameLayout card=(FrameLayout)call(activity,"portrait",new Class<?>[]{Anime.class,String.class,Runnable.class},anime,"",(Runnable)()->{});FrameLayout root=new FrameLayout(activity);root.addView(card,new FrameLayout.LayoutParams(280,420));activity.setContentView(root);layout(root,400,500);
                for(int n=0;n<120;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(server.getRequestCount()>0)break;Thread.sleep(25);}
                Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(LatActivity.poster,new Store(activity).poster(anime.url()));assertEquals(1,LatActivity.metadataCalls);
                RecordedRequest downloaded=server.takeRequest(3,TimeUnit.SECONDS);assertNotNull("Real vertical artwork must be downloaded",downloaded);assertEquals("/real-vertical.png",downloaded.getPath());
                // A cached/re-attached card must restore its portrait without another metadata lookup.
                root.removeAllViews();((ImageView)card.getChildAt(0)).setImageDrawable(null);root.addView(card,new FrameLayout.LayoutParams(280,420));layout(root,400,500);
                for(int n=0;n<80&&((ImageView)card.getChildAt(0)).getDrawable()==null;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(25);}assertNotNull(((ImageView)card.getChildAt(0)).getDrawable());assertEquals(1,LatActivity.metadataCalls);
                // Recreating the card uses the saved portrait, not the episode thumbnail.
                FrameLayout second=(FrameLayout)call(activity,"portrait",new Class<?>[]{Anime.class,String.class,Runnable.class},anime,"",(Runnable)()->{});root.removeAllViews();root.addView(second,new FrameLayout.LayoutParams(280,420));layout(root,400,500);Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(1,LatActivity.metadataCalls);
            }finally{controller.pause().stop().destroy();}
        }
    }
    @Test public void cancelIsCenteredHasCloseIconAndDoesNotChangeSource()throws Exception{
        var controller=Robolectric.buildActivity(TvScreenTest.FakeActivity.class).setup();MainActivity activity=controller.get();
        try{String source=new Store(activity).source();call(activity,"sourcesInfo",new Class<?>[]{});Shadows.shadowOf(Looper.getMainLooper()).idle();Dialog picker=org.robolectric.shadows.ShadowDialog.getLatestDialog();LinearLayout cancel=picker.findViewById(android.R.id.content).findViewWithTag("source-cancel");assertEquals(Gravity.CENTER,cancel.getGravity());assertTrue(cancel.getChildAt(0) instanceof TvUi.Icon);assertEquals("Cancelar",((TextView)cancel.getChildAt(1)).getText());cancel.performClick();assertFalse(picker.isShowing());assertEquals(source,new Store(activity).source());}finally{controller.pause().stop().destroy();}
    }
    @Test public void logosUseOriginalMarksLargerThanTinyFavicons(){
        android.content.Context context=RuntimeEnvironment.getApplication();
        assertTrue(SourceBrand.icon(context,"animeav1",40).getDrawable() instanceof android.graphics.drawable.VectorDrawable);
        for(String id:List.of("tioanime","jkanime","latanime","animelatinohd")){
            android.graphics.drawable.BitmapDrawable drawable=(android.graphics.drawable.BitmapDrawable)SourceBrand.icon(context,id,40).getDrawable();assertTrue(id,drawable.getBitmap().getWidth()>=50);assertTrue(id,drawable.getBitmap().getHeight()>=50);
        }
    }
    @Test public void backdropHasIndependentImageAndDoesNotChangePosterSizeOrUseEpisodeOne()throws Exception{
        var controller=Robolectric.buildActivity(TvScreenTest.FakeActivity.class).setup();MainActivity activity=controller.get();
        try{
            LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);Detail detail=new Detail(new Anime("Mushoku Tensei","test/artwork",""),"Sinopsis de prueba",List.of(new Episode("1","test/ep","https://example.test/frame1.jpg")),List.of(),"Serie","2026","Finalizado","https://example.test/common-art.jpg");
            call(activity,"renderDetail",new Class<?>[]{LinearLayout.class,Detail.class,int.class},panel,detail,0);layout(panel,1800,900);FrameLayout background=(FrameLayout)panel.getChildAt(0);assertEquals("detail-background",background.getTag());assertEquals(4,background.getChildCount());assertTrue(background.getChildAt(0) instanceof ImageView);
            LinearLayout hero=(LinearLayout)background.getChildAt(3);assertEquals(TvUi.dp(activity,164),hero.getChildAt(0).getWidth());assertEquals(TvUi.dp(activity,246),hero.getChildAt(0).getHeight());assertEquals(ImageView.ScaleType.CENTER_CROP,((ImageView)background.getChildAt(0)).getScaleType());
            Bitmap art=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);art.eraseColor(0xFF3080B0);((ImageView)background.getChildAt(0)).setImageBitmap(art);
            Bitmap preview=Bitmap.createBitmap(1800,900,Bitmap.Config.ARGB_8888);panel.draw(new Canvas(preview));File folder=new File(System.getProperty("animetv.previewDir"));folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,"ficha-fondo-023.png"))){preview.compress(Bitmap.CompressFormat.PNG,100,out);}preview.recycle();art.recycle();
            LinearLayout empty=new LinearLayout(activity);call(activity,"addDetailHeader",new Class<?>[]{LinearLayout.class,LinearLayout.class,String.class},empty,new LinearLayout(activity),"");assertTrue(empty.getChildAt(0) instanceof LinearLayout);
        }finally{controller.pause().stop().destroy();}
    }
}
