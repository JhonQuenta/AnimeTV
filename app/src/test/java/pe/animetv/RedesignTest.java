package pe.animetv;

import android.app.Dialog;

import android.graphics.*;

import android.os.Looper;

import android.view.*;

import android.widget.*;

import java.io.*;

import java.lang.reflect.*;

import java.util.*;

import org.junit.*;

import org.junit.runner.RunWith;

import org.robolectric.*;

import org.robolectric.annotation.*;

import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.*;

import static pe.animetv.Models.*;

@RunWith(RobolectricTestRunner.class)

@Config(sdk=28,qualifiers="w960dp-h540dp-land-xhdpi")

@GraphicsMode(GraphicsMode.Mode.NATIVE)

public class RedesignTest {
    @Test public void catalogueAndDetailHaveNoRatingWidgetsOrLabels()throws Exception{
        Anime anime=new Anime("Sousou no Frieren","test/without-stars","","Serie","Finalizado");FrameLayout card=(FrameLayout)call("portrait",new Class<?>[]{Anime.class,String.class,Runnable.class,boolean.class},anime,"",(Runnable)()->{},true);FrameLayout screen=new FrameLayout(activity);screen.addView(card,new FrameLayout.LayoutParams(280,420));activity.setContentView(screen);settle();assertNull(card.findViewWithTag("catalog-rating"));assertEquals(anime.title(),card.getContentDescription());TextView state=card.findViewWithTag("status");TextView type=(TextView)((LinearLayout)state.getParent()).getChildAt(0);assertTrue(type.getWidth()<state.getWidth());snapshot("catalogo-sin-estrellas-028",screen);
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);activity.setContentView(panel);call("renderDetail",new Class<?>[]{LinearLayout.class,Detail.class,int.class},panel,new Detail(anime,"Una aventura",List.of(),List.of(),"Serie","2023","Finalizado"),(Integer)field("requestGeneration"));settle();assertNull(panel.findViewWithTag("detail-rating"));LinearLayout info=(LinearLayout)((LinearLayout)panel.getChildAt(0)).getChildAt(1);assertEquals("Serie  ·  2023  ·  Finalizado",((TextView)info.getChildAt(1)).getText().toString());snapshot("ficha-sin-estrellas-028",panel);
    }
    @Test public void sourcePickerStaysCompactAfterRemovingRatings()throws Exception{
        call("loadCatalog",new Class<?>[]{int.class},1);settle();TvRowsView rows=(TvRowsView)field("catalogView");LinearLayout picker=rows.model.items.get(0).findViewWithTag("source-picker");assertEquals(TvUi.dp(activity,28),picker.getMinimumHeight());assertTrue(picker.isFocusable());assertTrue(picker.getHeight()<=TvUi.dp(activity,32));assertTrue(picker.requestFocusFromTouch());
    }

    @Test public void compactEpisodesKeepVisibleGapsWithBannerAndRemoteFocus()throws Exception{
        new Store(activity).episodeList(true);TvRowsView rows=new TvRowsView(activity);activity.setContentView(rows);Detail detail=new Detail(new Anime("Anime","test/spacing",""),"Sinopsis",List.of(new Episode("Episodio 1","test/1"),new Episode("Episodio 2","test/2"),new Episode("Episodio 3","test/3")),List.of(),"Serie","2026","En emisión","https://example.test/background.jpg");call("renderDetail",new Class<?>[]{LinearLayout.class,Detail.class,int.class},rows.model,detail,(Integer)field("requestGeneration"));settle();List<View> episodes=rows.model.items.stream().filter(v->"episode-row".equals(v.getTag())).toList();assertEquals(3,episodes.size());
        for(int n=1;n<3;n++){int[] before=new int[2],after=new int[2];episodes.get(n-1).getLocationInWindow(before);episodes.get(n).getLocationInWindow(after);assertEquals(TvUi.dp(activity,8),after[1]-before[1]-episodes.get(n-1).getHeight());}
        Field currentDetail=MainActivity.class.getDeclaredField("detailView");currentDetail.setAccessible(true);currentDetail.set(activity,rows);
        // Direct Robolectric key dispatch does not switch the window out of touch mode.
        for(View episode:episodes)episode.setFocusableInTouchMode(true);rows.restoreFocus(episodes.get(0));settle();assertTrue("First episode focused before Down",episodes.get(0).hasFocus());activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));settle();assertTrue("After Down: "+activity.getCurrentFocus(),episodes.get(1).hasFocus());snapshot("capitulos-lista-027",activity.getWindow().getDecorView());
    }
    @Test public void catalogActionsAreAlignedIconsAndOnlyOneSourcePicker()throws Exception{
        call("loadCatalog",new Class<?>[]{int.class},1);settle();LinearLayout finder=(LinearLayout)field("finder");assertEquals(4,finder.getChildCount());View search=finder.getChildAt(0);for(int n=1;n<4;n++){LinearLayout button=(LinearLayout)finder.getChildAt(n);assertEquals(search.getHeight(),button.getHeight());assertEquals(search.getTop(),button.getTop());assertEquals(1,button.getChildCount());assertTrue(button.getChildAt(0) instanceof TvUi.Icon);}TvRowsView rows=(TvRowsView)field("catalogView");View source=rows.model.items.get(0).findViewWithTag("source-picker");assertNotNull(source);assertTrue(((LinearLayout)source).getChildAt(0) instanceof ImageView);finder.getChildAt(3).requestFocus();activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));settle();assertTrue("Focus after down: "+activity.getCurrentFocus()+" sourceAttached="+source.isAttachedToWindow(),source.hasFocus());snapshot("catalogo-019",activity.getWindow().getDecorView());
    }
    @Test public void settingsCategoriesShowOnlySelectedOptionsAndReturnFocusLeft()throws Exception{
        call("settings",new Class<?>[]{});settle();TvRowsView rows=(TvRowsView)field("catalogView");LinearLayout dashboard=(LinearLayout)rows.model.items.get(1),tabs=(LinearLayout)dashboard.getChildAt(0);TvRowsView options=(TvRowsView)dashboard.getChildAt(1);assertEquals(4,tabs.getChildCount());tabs.getChildAt(2).performClick();settle();assertTrue(tabs.getChildAt(2).isSelected());assertTrue(options.model.items.stream().anyMatch(v->String.valueOf(v.getContentDescription()).startsWith("Servidor de AnimeAV1,")));assertFalse(options.model.items.stream().anyMatch(v->String.valueOf(v.getContentDescription()).startsWith("Retroceder,")));snapshot("ajustes-fuentes-019",activity.getWindow().getDecorView());tabs.getChildAt(1).performClick();settle();View backward=options.model.items.stream().filter(v->String.valueOf(v.getContentDescription()).startsWith("Retroceder,")).findFirst().orElseThrow();assertEquals(tabs.getChildAt(1).getId(),backward.getNextFocusLeftId());snapshot("ajustes-mando-019",activity.getWindow().getDecorView());
    }
    @Test public void sourcePickerFitsTvAndHasFocusableChoices()throws Exception{
        call("sourcesInfo",new Class<?>[]{});settle();Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();View decor=dialog.getWindow().getDecorView();layout(decor,960,920);LinearLayout panel=(LinearLayout)((ViewGroup)decor.findViewById(android.R.id.content)).getChildAt(0);LinearLayout choices=(LinearLayout)((ScrollView)panel.getChildAt(1)).getChildAt(0);assertEquals(Sources.ids().size(),choices.getChildCount());for(int n=0;n<Sources.ids().size();n++){assertTrue(choices.getChildAt(n).isFocusable());assertTrue(choices.getChildAt(n).getHeight()>=TvUi.dp(activity,64));}choices.getChildAt(0).requestFocus();snapshot("selector-fuentes-018",decor);dialog.dismiss();
    }

    private ActivityController<TvScreenTest.FakeActivity> controller;

    private MainActivity activity;

    @Before public void start(){controller=Robolectric.buildActivity(TvScreenTest.FakeActivity.class).setup();activity=controller.get();controller.visible().windowFocusChanged(true);}

    @After public void stop(){controller.pause().stop().destroy();}

    private Object field(String key)throws Exception{Field f=MainActivity.class.getDeclaredField(key);f.setAccessible(true);return f.get(activity);}

    private Object call(String key,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(key,types);m.setAccessible(true);return m.invoke(activity,args);}

    private void settle(){for(int n=0;n<3;n++){Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(150));layout(activity.getWindow().getDecorView(),1920,1080);}}

    private void layout(View view,int width,int height){view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);}

    private void snapshot(String name,View view)throws Exception{File folder=new File(System.getProperty("animetv.previewDir"));folder.mkdirs();Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();}

    @Test public void detailPositionsStayFixedAcrossPosterAndSynopsisSizes()throws Exception {

        int[] reference=null;

        for(int n=0;n<3;n++) {

            Anime anime=new Anime(n==2?"Titulo de una temporada con un nombre suficientemente largo para dos lineas":"Anime", "test/stable/"+n, "");

            if(n!=1)new Store(activity).publication(anime.url(),"Episodio 2 - hoy");

            Detail detail=new Detail(anime,n==0?"Descripcion larga. ".repeat(100):"Descripcion breve",List.of(new Episode("Episodio 1","test/1")),List.of(new Genre("Sobrenatural","supernatural"),new Genre("Comedia","comedy")),"TV Anime","2026","En emision");

            LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);

            call("renderDetail",new Class<?>[]{LinearLayout.class,Detail.class,int.class},panel,detail,(Integer)field("requestGeneration"));

            layout(panel,1800,1080);LinearLayout hero=(LinearLayout)panel.getChildAt(0);LinearLayout info=(LinearLayout)hero.getChildAt(1);

            int[] positions={hero.getChildAt(0).getWidth(),hero.getChildAt(0).getHeight()};

            if(reference==null)reference=positions;else assertArrayEquals(reference,positions);

            assertTrue(info.getChildAt(5).getBottom()<=hero.getHeight());assertEquals(n==1?View.GONE:View.VISIBLE,info.getChildAt(2).getVisibility());assertEquals(info.getChildAt(4).getBottom(),info.getChildAt(5).getTop());
            LinearLayout genres=(LinearLayout)info.getChildAt(3);for(int g=0;g<genres.getChildCount();g++){TextView chip=(TextView)genres.getChildAt(g);assertTrue(chip.getBaseline()+chip.getPaint().getFontMetrics().descent<=chip.getHeight());assertTrue(chip.getBaseline()+chip.getPaint().getFontMetrics().ascent>=0);}
            TextView origin=(TextView)((LinearLayout)((LinearLayout)hero.getChildAt(2)).getChildAt(1)).getChildAt(1);assertEquals("Fake",origin.getText().toString());assertEquals("Source tag should not be ellipsized",0,origin.getLayout().getEllipsisCount(0));if(n==0){assertTrue(info.getChildAt(0).getHeight()<TvUi.dp(activity,76));snapshot("ficha-generos-completos",panel);}else if(n==2)snapshot("ficha-titulo-largo",panel);

        }

    }

    @Test public void collapsedMenuKeepsIconsSquareAndExpandsOnFocus()throws Exception{

        settle();LinearLayout header=(LinearLayout)field("header");View nav=header.getChildAt(1);assertTrue(nav.requestFocusFromTouch());settle();assertEquals(TvUi.dp(activity,190),header.getWidth());assertEquals(View.VISIBLE,((LinearLayout)nav).getChildAt(1).getVisibility());

        call("settings",new Class<?>[]{});settle();TvRowsView rows=(TvRowsView)field("catalogView");((LinearLayout)((LinearLayout)rows.model.items.get(1)).getChildAt(0)).getChildAt(0).requestFocus();settle();assertEquals(TvUi.dp(activity,104),header.getWidth());View icon=((LinearLayout)nav).getChildAt(0);assertEquals(icon.getWidth(),icon.getHeight());assertTrue(icon.getWidth()>0);snapshot("ajustes",activity.getWindow().getDecorView());

    }

    @Test public void catalogUsesSixColumnsAndRightAlignedTags()throws Exception{

        call("loadCatalog",new Class<?>[]{int.class},1);settle();List<Anime> items=new ArrayList<>();for(int n=1;n<=6;n++)items.add(new Anime("Anime "+n,"test/"+n,"","Serie","En emisión"));TvRowsView rows=(TvRowsView)field("catalogView");call("displayGrid",new Class<?>[]{LinearLayout.class,List.class},rows.model,items);settle();LinearLayout row=(LinearLayout)rows.model.items.get(rows.model.items.size()-1);assertEquals(6,row.getChildCount());FrameLayout card=(FrameLayout)row.getChildAt(0);layout(card,TvUi.dp(activity,140),TvUi.dp(activity,210));LinearLayout tags=(LinearLayout)card.getChildAt(2);assertEquals(Gravity.RIGHT,tags.getGravity()&Gravity.HORIZONTAL_GRAVITY_MASK);assertEquals("card="+card.getWidth()+" measured="+card.getMeasuredWidth()+" tags="+tags.getWidth()+" tag="+tags.getChildAt(1).getWidth(),tags.getWidth()-tags.getPaddingRight(),tags.getChildAt(1).getRight());snapshot("catalogo",activity.getWindow().getDecorView());

    }

    @Test public void calendarTabsChangeVisibleDayWithoutFetching()throws Exception{

        TvRowsView view=new TvRowsView(activity);List<CalendarEntry> entries=List.of(new CalendarEntry(new Anime("Lunes","test/l","","Serie","En emisión"),1,"",0),new CalendarEntry(new Anime("Martes","test/m","","Serie","En emisión"),2,"",0));call("renderCalendar",new Class<?>[]{LinearLayout.class,List.class},view.model,entries);TvRowsView.Rail tabs=(TvRowsView.Rail)view.model.items.get(0);tabs.model.items.get(0).performClick();assertTrue(tabs.model.items.get(0).isSelected());tabs.model.items.get(1).performClick();assertFalse(tabs.model.items.get(0).isSelected());assertTrue(tabs.model.items.get(1).isSelected());assertEquals("Martes · 1 animes",((TextView)view.model.items.get(1)).getText());

    }

    @Test public void numericSettingsSaveFineAdjustmentAndDialogScrolls()throws Exception{

        call("settings",new Class<?>[]{});settle();TvRowsView rows=(TvRowsView)field("catalogView");LinearLayout dashboard=(LinearLayout)rows.model.items.get(1);dashboard.findViewWithTag("settings-category:Control remoto").performClick();settle();TvRowsView options=(TvRowsView)dashboard.getChildAt(1);LinearLayout setting=(LinearLayout)options.model.items.stream().filter(v->String.valueOf(v.getContentDescription()).startsWith("Retroceder,")).findFirst().orElseThrow();setting.performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();LinearLayout panel=(LinearLayout)((ViewGroup)dialog.getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0);ScrollView scroll=(ScrollView)panel.getChildAt(1);assertEquals(1f,((LinearLayout.LayoutParams)scroll.getLayoutParams()).weight,0);LinearLayout body=(LinearLayout)scroll.getChildAt(0);((LinearLayout)body.getChildAt(2)).getChildAt(1).performClick();((LinearLayout)body.getChildAt(1)).getChildAt(2).performClick();assertEquals(6,new Store(activity).seconds("backward"));assertEquals("6 seg",((TextView)setting.getChildAt(1)).getText());assertFalse(dialog.isShowing());

    }

    @Test public void detailAndCompactListFitTvViewport()throws Exception{

        List<Episode> episodes=new ArrayList<>();for(int n=1;n<=13;n++)episodes.add(new Episode("Episodio "+n,"test/episode/"+n));Detail detail=new Detail(new Anime("Uma Musume: Cinderella Gray","test/anime",""),"Una historia sobre carreras, amistades y nuevos desafíos. ".repeat(8),episodes,List.of(),"TV Anime","2025","Finalizado");TvRowsView view=new TvRowsView(activity);view.setBackgroundColor(TvUi.BG);view.grid.setPadding(48,48,48,48);new Store(activity).progress("test/episode/1",5000,20000);call("renderDetail",new Class<?>[]{LinearLayout.class,Detail.class,int.class},view.model,detail,(Integer)field("requestGeneration"));LinearLayout hero=(LinearLayout)view.model.items.get(0);LinearLayout info=(LinearLayout)hero.getChildAt(1);TextView synopsis=(TextView)info.getChildAt(4);assertEquals(2,synopsis.getMaxLines());((LinearLayout)view.model.items.get(1)).getChildAt(2).performClick();assertEquals("Episodio 1",view.model.items.get(2).getContentDescription());layout(view,1920,1080);Shadows.shadowOf(Looper.getMainLooper()).idle();layout(view,1920,1080);snapshot("capitulos-lista",view);

    }

    @Test public void compactProgressFillsWholeRowAndControlsHaveSpace()throws Exception{

        Store store=new Store(activity);store.progress("test/progress",5000,20000);TvUi.ProgressRow row=(TvUi.ProgressRow)call("compactEpisode",new Class<?>[]{Episode.class,Runnable.class},new Episode("Episodio 40","test/progress"),(Runnable)()->{});

        assertEquals(250,row.getProgress());assertEquals(1,row.getChildCount());layout(row,800,88);Bitmap bitmap=Bitmap.createBitmap(800,88,Bitmap.Config.ARGB_8888);row.draw(new Canvas(bitmap));assertEquals(0xFF174F86,bitmap.getPixel(100,8));assertEquals(TvUi.PANEL,bitmap.getPixel(400,8));bitmap.recycle();snapshot("episodio-progreso",row);

        TvRowsView view=new TvRowsView(activity);call("renderDetail",new Class<?>[]{LinearLayout.class,Detail.class,int.class},view.model,new Detail(new Anime("Prueba","test",""),"",List.of(new Episode("Episodio 1","test/1"))),(Integer)field("requestGeneration"));View toolbar=view.model.items.get(1);assertTrue(toolbar.getPaddingBottom()>=TvUi.dp(activity,12));

    }

    @Test public void recentEpisodeRequiresChoiceBeforeLaunching()throws Exception{

        call("showDetail",new Class<?>[]{Anime.class,String.class},new Anime("Prueba","test/choice",""),"test/choice/2");settle();android.app.AlertDialog dialog=(android.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(dialog);assertTrue(dialog.isShowing());assertEquals("Cerrar",dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).getText().toString());assertNull(Shadows.shadowOf(activity).getNextStartedActivity());dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(dialog.isShowing());assertNull(Shadows.shadowOf(activity).getNextStartedActivity());

        call("confirmRecentEpisode",new Class<?>[]{Detail.class,int.class},new Detail(new Anime("Prueba","test/choice",""),"",List.of(new Episode("Episodio 1","test/choice/1"),new Episode("Episodio 2","test/choice/2"))),1);dialog=(android.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(1,Shadows.shadowOf(activity).getNextStartedActivity().getIntExtra("index",-1));

    }

    @Test public void drawerIsRightAlignedAndLongOptionsRemainScrollable()throws Exception{

        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);for(int n=0;n<30;n++)body.addView(TvUi.setting(activity,"Opción "+n,"Todos",()->{}));Dialog drawer=TvUi.dialog(activity,"Filtros",body,true);Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(Gravity.RIGHT,drawer.getWindow().getAttributes().gravity);assertEquals(TvUi.dp(activity,360),drawer.getWindow().getAttributes().width);assertEquals(1080,drawer.getWindow().getAttributes().height);drawer.dismiss();

    }

    @Test public void detailKeepsSafeStartWithoutClippingAndUsesApprovedBitmap()throws Exception{

        call("showDetail",new Class<?>[]{Anime.class},new Anime("Prueba","test/detail",""));settle();TvRowsView detail=(TvRowsView)field("detailView");assertEquals(TvUi.dp(activity,24),detail.grid.getPaddingLeft());assertEquals(TvUi.dp(activity,24),detail.grid.getPaddingRight());assertSame(detail,((LinearLayout)field("content")).getChildAt(0));assertFalse(detail.grid.getClipToPadding());

        ImageView banner=new ImageView(activity);banner.setImageResource(R.drawable.cat_banner);banner.setScaleType(ImageView.ScaleType.FIT_CENTER);assertTrue(banner.getDrawable() instanceof android.graphics.drawable.BitmapDrawable);assertTrue(((android.graphics.drawable.BitmapDrawable)banner.getDrawable()).isFilterBitmap());assertTrue(((android.graphics.drawable.BitmapDrawable)banner.getDrawable()).getBitmap().getWidth()==960);layout(banner,1280,720);snapshot("logo-banner-original",banner);

    }

    @Test public void continueWatchingUsesWideCardsAndFavoriteCategoriesRemainAvailable()throws Exception{

        Store store=new Store(activity);Anime anime=new Anime("One Piece","test/one-piece","","Serie","En emisión");store.rememberAnime(anime);store.lastEpisode(anime.url(),"test/episode/40");store.progress("test/episode/40",600000,1200000);call("home",new Class<?>[]{});settle();TvRowsView rows=(TvRowsView)field("catalogView");TvRowsView.Rail rail=(TvRowsView.Rail)rows.model.items.get(2);View card=rail.model.items.get(0);assertTrue(card instanceof TvUi.EpisodeCard);assertEquals(TvUi.dp(activity,245),card.getLayoutParams().width);snapshot("inicio",activity.getWindow().getDecorView());

        store.favorite(anime,"Por defecto",true);call("showFavorites",new Class<?>[]{});settle();rows=(TvRowsView)field("catalogView");assertTrue(rows.model.items.get(1) instanceof TvRowsView.Rail);snapshot("favoritos",activity.getWindow().getDecorView());

    }

}

