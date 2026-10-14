package pe.animetv;
import android.os.Looper;
import android.app.Dialog;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.lang.reflect.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
import static pe.animetv.Models.*;
import pe.animetv.Models.Filter;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w960dp-h540dp-land-xhdpi")
public class TvScreenTest {
    public static class FakeActivity extends MainActivity {
        int recentCalls,catalogCalls,calendarCalls;
        protected Source createSource() { return new Source() {
            public String id(){return "fake";} public String name(){return "Fake";} public String baseUrl(){return "https://example.org";}
            public List<Anime> catalog(String q,int p,Map<String,String> f){catalogCalls++;return List.of();}
            public List<Filter> filters(){return List.of();}
            public Detail detail(Anime a){return new Detail(a,"Una historia de prueba",List.of(new Episode("Episodio 1",a.url()+"/1"),new Episode("Episodio 2",a.url()+"/2")),List.of(),"TV Anime","2026","En emisión");}
            public List<Server> servers(Episode e){return List.of();} public Stream resolve(Server s,String url){return null;}
            public List<RecentEpisode> recent(){recentCalls++;return List.of();} public List<CalendarEntry> calendar(){calendarCalls++;return List.of();}
        }; }
    }
    private ActivityController<FakeActivity> controller;
    private FakeActivity activity;
    @Before public void start(){controller=Robolectric.buildActivity(FakeActivity.class).setup(); activity=controller.get();}
    @Test public void sourcePickerSelectsTioanimeAndPersistsPreference()throws Exception{
        Method method=MainActivity.class.getDeclaredMethod("sourcesInfo");method.setAccessible(true);method.invoke(activity);
        Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();
        ViewGroup panel=(ViewGroup)((ViewGroup)dialog.getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0);
        ScrollView scroll=(ScrollView)panel.getChildAt(1);LinearLayout choices=(LinearLayout)scroll.getChildAt(0);
        choices.getChildAt(1).performClick();assertEquals("tioanime",new Store(activity).source());assertFalse(dialog.isShowing());new Store(activity).source("animeav1");
    }
    @Test public void sourcePickerIncludesLatanimeAndPersistsSelection()throws Exception{
        Method method=MainActivity.class.getDeclaredMethod("sourcesInfo");method.setAccessible(true);method.invoke(activity);
        Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();ViewGroup panel=(ViewGroup)((ViewGroup)dialog.getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0);
        LinearLayout choices=(LinearLayout)((ScrollView)panel.getChildAt(1)).getChildAt(0);assertEquals(Sources.ids().size(),choices.getChildCount());choices.getChildAt(2).performClick();assertEquals("latanime",new Store(activity).source());assertFalse(dialog.isShowing());new Store(activity).source("animeav1");
    }
    @After public void stop(){controller.pause().stop().destroy();}
    private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
    @Test public void topSourceButtonOpensPickerAndSettingsHaveIndependentServerChoices()throws Exception{
        settle();TvRowsView rows=(TvRowsView)field("catalogView");View picker=rows.model.items.get(0).findViewWithTag("source-picker");assertNotNull(picker);assertTrue(picker.isFocusable());picker.performClick();Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertTrue(dialog.isShowing());dialog.dismiss();
        invoke("settings");settle();rows=(TvRowsView)field("catalogView");
        LinearLayout dashboard=(LinearLayout)rows.model.items.get(1);dashboard.findViewWithTag("settings-category:Fuentes").performClick();settle();TvRowsView preferences=(TvRowsView)dashboard.getChildAt(1);View preference=preferences.model.items.stream().filter(v->String.valueOf(v.getContentDescription()).startsWith("Servidor de AnimeAV1,")).findFirst().orElseThrow();preference.performClick();
        dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();ViewGroup panel=(ViewGroup)((ViewGroup)dialog.getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0);LinearLayout options=(LinearLayout)((ScrollView)panel.getChildAt(1)).getChildAt(0);
        options.getChildAt(2).performClick();Store store=new Store(activity);assertEquals("Voe",store.serverDefault("animeav1"));assertEquals("auto",store.serverDefault("tioanime"));store.serverDefault("animeav1","auto");
        assertFalse(rows.model.items.stream().anyMatch(v->String.valueOf(v.getContentDescription()).startsWith("Fuente,")));
    }
    @Test public void continueWatchingLabelsOriginalSourcesAfterSwitch()throws Exception{
        LinearLayout body=new LinearLayout(activity);org.json.JSONArray history=new org.json.JSONArray();
        history.put(new org.json.JSONObject().put("title","Uno").put("url","https://animeav1.com/media/uno").put("image",""));history.put(new org.json.JSONObject().put("title","Dos").put("url","https://tioanime.com/anime/dos").put("image",""));
        Method method=MainActivity.class.getDeclaredMethod("continueWatching",LinearLayout.class,org.json.JSONArray.class);method.setAccessible(true);method.invoke(activity,body,history);
        TvRowsView.Rail rail=(TvRowsView.Rail)body.getChildAt(0);FrameLayout first=(FrameLayout)rail.model.items.get(0),second=(FrameLayout)rail.model.items.get(1);assertEquals("AnimeAV1",((TextView)((LinearLayout)first.getChildAt(4)).getChildAt(1)).getText());assertEquals("Tioanime",((TextView)((LinearLayout)second.getChildAt(4)).getChildAt(1)).getText());
    }
    @Test public void detailSourceBadgeReflectsAnimeUrlAndFitsMetadataRow()throws Exception{
        LinearLayout panel=new LinearLayout(activity);Method render=MainActivity.class.getDeclaredMethod("renderDetail",LinearLayout.class,Detail.class,int.class);render.setAccessible(true);
        render.invoke(activity,panel,new Detail(new Anime("Título","https://tioanime.com/anime/test",""),"Descripción",List.of()),(Integer)field("requestGeneration"));
        LinearLayout side=(LinearLayout)((LinearLayout)panel.getChildAt(0)).getChildAt(2);LinearLayout origin=(LinearLayout)side.getChildAt(1);TextView badge=(TextView)origin.getChildAt(1);assertTrue(origin.getChildAt(0) instanceof ImageView);assertEquals("Tioanime",badge.getText());
        panel.measure(View.MeasureSpec.makeMeasureSpec(1800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.AT_MOST));panel.layout(0,0,1800,panel.getMeasuredHeight());assertTrue(badge.getBaseline()+badge.getPaint().getFontMetrics().descent<=badge.getHeight());
    }
    private void invoke(String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}
    private void settle(){controller.visible().windowFocusChanged(true);for(int n=0;n<3;n++){Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(150));View decor=activity.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));decor.layout(0,0,1920,1080);}}
    @Test public void returningToSectionReusesLoadedRowsAndDoesNotFetchAgain()throws Exception{settle();Object original=field("catalogView");LinearLayout menu=(LinearLayout)field("header");menu.getChildAt(2).performClick();settle();Object catalog=field("catalogView");menu.getChildAt(1).performClick();settle();assertSame(original,field("catalogView"));menu.getChildAt(2).performClick();settle();assertSame(catalog,field("catalogView"));assertEquals(1,activity.recentCalls);assertEquals(1,activity.catalogCalls);}
    @Test public void compactEpisodesAndCollapsibleBlocksPreserveEpisodeNumbers()throws Exception{Method render=MainActivity.class.getDeclaredMethod("renderDetail",LinearLayout.class,Detail.class,int.class);render.setAccessible(true);List<Episode> list=new ArrayList<>();for(int n=1;n<=101;n++)list.add(new Episode("Episodio "+n,"https://example.org/compact/"+n));LinearLayout panel=new LinearLayout(activity);render.invoke(activity,panel,new Detail(new Anime("Prueba","test",""),"",list),(Integer)field("requestGeneration"));TvRowsView.Rail ranges=(TvRowsView.Rail)panel.getChildAt(2);assertEquals(2,ranges.model.items.size());ranges.model.items.get(0).performClick();assertEquals(0,countEpisodes(panel));ranges.model.items.get(1).performClick();assertEquals(1,countEpisodes(panel));((LinearLayout)panel.getChildAt(1)).getChildAt(2).performClick();View episode=panel.getChildAt(3);assertEquals("Episodio 101",episode.getContentDescription());episode.performClick();assertEquals(100,Shadows.shadowOf(activity).getNextStartedActivity().getIntExtra("index",-1));}
    @Test public void sectionRestoresItsSelectedRowAfterSwitching()throws Exception{settle();TvRowsView original=(TvRowsView)field("catalogView");for(int n=0;n<10;n++)original.model.addView(TvUi.button(activity,"Fila "+n,null,()->{}),new LinearLayout.LayoutParams(-1,180));settle();View last=original.model.items.get(original.model.items.size()-1);original.restoreFocus(last);settle();int selected=original.grid.getSelectedPosition();LinearLayout menu=(LinearLayout)field("header");menu.getChildAt(2).performClick();settle();menu.getChildAt(1).performClick();settle();assertSame(original,field("catalogView"));assertEquals(selected,original.grid.getSelectedPosition());menu.getChildAt(1).dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT));settle();assertSame(last,((View)field("root")).findFocus());}
    @Test public void sectionHeadersScrollAwayAndViewportHasNoOuterMargins()throws Exception{
        invoke("applySafeArea");for(String screen:new String[]{"home","calendar","settings","catalog"}){if(screen.equals("catalog")){Method load=MainActivity.class.getDeclaredMethod("loadCatalog",int.class);load.setAccessible(true);load.invoke(activity,1);}else invoke(screen);settle();View banner=(View)field("sectionBanner");TvRowsView rows=(TvRowsView)field("catalogView");assertSame(banner,rows.model.items.get(0));assertEquals(1,((LinearLayout)field("content")).getChildCount());for(int n=0;n<6;n++)rows.model.addView(TvUi.button(activity,"Prueba "+n,null,()->{}),new LinearLayout.LayoutParams(-1,180));settle();rows.restoreFocus(rows.model.items.get(rows.model.items.size()-1));settle();assertTrue("Header must scroll out: "+screen,rows.grid.findViewHolderForAdapterPosition(0)==null||rows.grid.findViewHolderForAdapterPosition(0).itemView.getBottom()<=0);View root=(View)field("root");assertEquals(0,root.getPaddingTop());assertEquals(0,root.getPaddingBottom());assertEquals(0,root.getPaddingLeft());assertEquals(0,root.getPaddingRight());assertEquals(TvUi.dp(activity,32),((View)field("header")).getPaddingLeft());assertEquals(0,rows.grid.getPaddingBottom());}
    }
    @Test public void episodeCaptionIsInsideImageAndPlayAppearsOnFocus()throws Exception{
        Method make=MainActivity.class.getDeclaredMethod("landscape",String.class,String.class,String.class,Runnable.class);make.setAccessible(true);FrameLayout card=(FrameLayout)make.invoke(activity,"Episodio 5","","",(Runnable)()->{});card.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));card.layout(0,0,400,card.getMeasuredHeight());assertEquals(225,card.getHeight());assertTrue(card.getClipToOutline());TextView caption=(TextView)card.getChildAt(2);assertEquals("Episodio 5",caption.getText());assertTrue(caption.getBottom()<=card.getHeight());View play=card.getChildAt(3);assertEquals(View.GONE,play.getVisibility());card.getOnFocusChangeListener().onFocusChange(card,true);assertEquals(View.VISIBLE,play.getVisibility());card.getOnFocusChangeListener().onFocusChange(card,false);assertEquals(View.GONE,play.getVisibility());
    }
    @Test public void leftFromFirstCardReturnsToMenuButMiddleCardDoesNot()throws Exception{
        settle();TvRowsView rows=(TvRowsView)field("catalogView");TvRowsView.Rail rail=new TvRowsView.Rail(activity);rows.model.addView(rail,new LinearLayout.LayoutParams(-1,-2));for(int n=0;n<3;n++)rail.model.addView(TvUi.button(activity,"Card "+n,null,()->{}),new LinearLayout.LayoutParams(280,350));settle();rows.restoreFocus(rail.model.items.get(1));settle();activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT));assertFalse(((View)field("header")).hasFocus());rows.restoreFocus(rail.model.items.get(0));settle();rail.model.items.get(0).requestFocus();assertSame("First card owns focus",rail.model.items.get(0),((View)field("root")).findFocus());assertTrue("Left handled",activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT)));assertTrue("Menu receives focus",((View)field("header")).hasFocus());
    }
    @Test public void compactCardsKeepTitleAndBadgesInsidePoster()throws Exception{
        Anime anime=new Anime("T�tulo largo que debe seguir dentro de la portada","https://example.org/media/compact","","Serie");Store store=new Store(activity);store.status(anime.url(),"Finalizado");Method portrait=MainActivity.class.getDeclaredMethod("portrait",Anime.class,String.class,Runnable.class);portrait.setAccessible(true);FrameLayout card=(FrameLayout)portrait.invoke(activity,anime,"09/10/2026 � 09:08",(Runnable)()->{});int width=TvUi.dp(activity,140);card.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));card.layout(0,0,width,card.getMeasuredHeight());assertEquals(Math.round(width*1.5f),card.getHeight());LinearLayout badges=(LinearLayout)card.getChildAt(2);assertEquals("Serie",((TextView)badges.getChildAt(0)).getText());assertEquals("Finalizado",((TextView)badges.getChildAt(1)).getText());assertEquals(-2,badges.getChildAt(1).getLayoutParams().width);assertTrue(card.getClipToOutline());TextView title=(TextView)card.getChildAt(3);assertTrue(title.getBottom()<=card.getHeight());assertTrue(title.getTop()>badges.getBottom());assertEquals("09/10/2026 � 09:08",store.publication(anime.url()));
    }
    @Test public void rightFromCatalogMenuGoesToFirstAnimeAndUpCanReachSearch()throws Exception{
        Method load=MainActivity.class.getDeclaredMethod("loadCatalog",int.class);load.setAccessible(true);load.invoke(activity,1);settle();TvRowsView rows=(TvRowsView)field("catalogView");Method display=MainActivity.class.getDeclaredMethod("displayGrid",LinearLayout.class,List.class);display.setAccessible(true);display.invoke(activity,rows.model,List.of(new Anime("Uno","https://example.org/uno","","Serie"),new Anime("Dos","https://example.org/dos","","Serie")));settle();LinearLayout header=(LinearLayout)field("header");View menu=header.getChildAt(2);menu.requestFocus();menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT));settle();View first=rows.model.items.stream().filter(v->v instanceof LinearLayout&&((LinearLayout)v).getChildCount()>0&&((LinearLayout)v).getChildAt(0) instanceof TvUi.PosterCard).map(v->((LinearLayout)v).getChildAt(0)).findFirst().get();assertSame(first,((View)field("root")).findFocus());View banner=(View)field("sectionBanner");int[] bannerAt=new int[2],rowsAt=new int[2];banner.getLocationOnScreen(bannerAt);rows.getLocationOnScreen(rowsAt);assertTrue("Full header remains above first catalog row",bannerAt[1]>=rowsAt[1]);assertFalse(((View)field("finder")).hasFocus());activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_UP));settle();assertTrue(((View)field("finder")).hasFocus());activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));settle();assertSame(first,((View)field("root")).findFocus());
    }
    @Test public void longAnimeUsesWindowsAndDescendingPlaysCorrectOriginalEpisode()throws Exception{
        Method render=MainActivity.class.getDeclaredMethod("renderDetail",LinearLayout.class,Detail.class,int.class);render.setAccessible(true);List<Episode> list=new ArrayList<>();for(int n=1;n<=205;n++)list.add(new Episode("Episodio "+n,"https://example.org/show/"+n));Detail detail=new Detail(new Anime("Largo","https://example.org/show",""),"",list);LinearLayout panel=new LinearLayout(activity);render.invoke(activity,panel,detail,(Integer)field("requestGeneration"));TvRowsView.Rail ranges=(TvRowsView.Rail)panel.getChildAt(2);assertEquals(3,ranges.model.items.size());assertEquals(100,countEpisodes(panel));ranges.model.items.get(2).performClick();assertEquals(5,countEpisodes(panel));FrameLayout first=(FrameLayout)((LinearLayout)panel.getChildAt(3)).getChildAt(0);assertEquals("Episodio 201",first.getContentDescription());((LinearLayout)panel.getChildAt(1)).getChildAt(1).performClick();first=(FrameLayout)((LinearLayout)panel.getChildAt(3)).getChildAt(0);assertEquals("Episodio 205",first.getContentDescription());first.performClick();android.content.Intent intent=Shadows.shadowOf(activity).getNextStartedActivity();assertEquals(204,intent.getIntExtra("index",-1));assertEquals(205,intent.getStringArrayListExtra("episodes").size());
    }
    @Test public void exactlyOneHundredEpisodesHasNoBlockSelector()throws Exception{
        Method render=MainActivity.class.getDeclaredMethod("renderDetail",LinearLayout.class,Detail.class,int.class);render.setAccessible(true);List<Episode> list=new ArrayList<>();for(int n=1;n<=100;n++)list.add(new Episode("Episodio "+n,"test/"+n));LinearLayout panel=new LinearLayout(activity);render.invoke(activity,panel,new Detail(new Anime("Cien","test",""),"",list),(Integer)field("requestGeneration"));assertEquals(100,countEpisodes(panel));for(int n=0;n<panel.getChildCount();n++)assertFalse(panel.getChildAt(n) instanceof TvRowsView.Rail);
    }
    private int countEpisodes(View view){if(view instanceof TvUi.EpisodeCard)return 1;int count=0;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++)count+=countEpisodes(group.getChildAt(n));}return count;}
    @Test public void navigationContainsFiveRequestedSections()throws Exception{
        LinearLayout header=(LinearLayout)field("header"); assertEquals(6,header.getChildCount());
        String[] names={"Inicio","Catálogo","Calendario","Favoritos","Ajustes"};
        for(int n=0;n<names.length;n++)assertEquals(names[n],header.getChildAt(n+1).getTag());
        assertEquals(View.GONE,((View)field("finder")).getVisibility());
    }
    @Test public void detailHidesGlobalNavigationAndBackRestoresCatalogView()throws Exception{
        View original=(View)field("catalogView"); Method m=MainActivity.class.getDeclaredMethod("showDetail",Anime.class);m.setAccessible(true);m.invoke(activity,new Anime("Prueba","https://example.org/media/test",""));
        assertEquals(View.GONE,((View)field("header")).getVisibility()); assertEquals(View.GONE,((View)field("finder")).getVisibility());
        activity.onBackPressed(); assertNull(field("detailView")); assertEquals(View.VISIBLE,((View)field("header")).getVisibility()); assertSame(original,((LinearLayout)field("content")).getChildAt(0));
    }
    @Test public void detailHasPosterHeartAndSpacedEpisodeCards()throws Exception{
        Method m=MainActivity.class.getDeclaredMethod("renderDetail",LinearLayout.class,Detail.class,int.class);m.setAccessible(true); LinearLayout panel=new LinearLayout(activity);
        Anime anime=new Anime("Prueba","https://example.org/media/test",""); Detail detail=new Detail(anime,"Descripción",List.of(new Episode("Episodio 1","test/1"),new Episode("Episodio 2","test/2")));
        m.invoke(activity,panel,detail,(Integer)field("requestGeneration")); LinearLayout hero=(LinearLayout)panel.getChildAt(0); assertTrue(hero.getChildAt(0) instanceof ImageView);
        LinearLayout identity=(LinearLayout)hero.getChildAt(2),heart=(LinearLayout)identity.getChildAt(0);
        assertEquals("Guardar anime en favoritos",heart.getChildAt(0).getContentDescription()); LinearLayout episodes=(LinearLayout)panel.getChildAt(2); LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)episodes.getChildAt(0).getLayoutParams(); assertTrue(p.leftMargin>0 && p.bottomMargin>0);
        heart.getChildAt(0).performClick(); assertTrue(new Store(activity).isFavorite(anime.url()));
    }
    @Test public void catalogAloneShowsSearchAndFilters()throws Exception{Method m=MainActivity.class.getDeclaredMethod("loadCatalog",int.class);m.setAccessible(true);m.invoke(activity,1);assertEquals(View.VISIBLE,((View)field("finder")).getVisibility());invoke("settings");assertEquals(View.GONE,((View)field("finder")).getVisibility());}
    @Test public void keyboardNextStartsSearchAndMovesFocus()throws Exception{EditText input=(EditText)field("search");input.setText("Naruto");input.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);assertEquals("Naruto",field("query"));assertEquals("Catálogo",field("section"));assertTrue(((View)field("searchButton")).hasFocus());}
    @Test public void rootHasSafeMarginsAndPortraitTypes()throws Exception{LinearLayout root=(LinearLayout)field("root");assertEquals(0,root.getPaddingLeft());assertEquals(LinearLayout.HORIZONTAL,root.getOrientation());assertEquals(LinearLayout.VERTICAL,((LinearLayout)field("header")).getOrientation());Method m=MainActivity.class.getDeclaredMethod("portrait",Anime.class,String.class,Runnable.class);m.setAccessible(true);FrameLayout card=(FrameLayout)m.invoke(activity,new Anime("Título largo que debe poder ocupar dos líneas","https://example.org/media/test","","OVA"),"Episodio 2",(Runnable)()->{});assertEquals("OVA",((TextView)((LinearLayout)card.getChildAt(2)).getChildAt(0)).getText());assertEquals(Gravity.BOTTOM,((FrameLayout.LayoutParams)card.getChildAt(3).getLayoutParams()).gravity);}
    @Test public void enlargedTextDoesNotPushNavigationOutsideTvViewport()throws Exception{
        activity.getResources().getDisplayMetrics().scaledDensity=activity.getResources().getDisplayMetrics().density*1.3f;
        LinearLayout root=(LinearLayout)field("root");int width=activity.getResources().getDisplayMetrics().widthPixels,height=activity.getResources().getDisplayMetrics().heightPixels;root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);
        LinearLayout header=(LinearLayout)field("header");View last=header.getChildAt(header.getChildCount()-1);assertTrue(last.getRight()<=header.getWidth());assertTrue(header.getWidth()<=width-root.getPaddingLeft()-root.getPaddingRight());
    }
    @Test public void recentCardsReserveEqualHeightForOneOrTwoLineDates()throws Exception{
        Method m=MainActivity.class.getDeclaredMethod("portrait",Anime.class,String.class,Runnable.class);m.setAccessible(true);
        Anime anime=new Anime("Título","https://example.org/media/typed","","Serie");
        View shortCard=(View)m.invoke(activity,anime,"Episodio 1 · hoy",(Runnable)()->{}),longCard=(View)m.invoke(activity,anime,"Episodio 14 · hace 21 horas",(Runnable)()->{});
        int width=TvUi.dp(activity,150);shortCard.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));longCard.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));assertEquals(shortCard.getMeasuredHeight(),longCard.getMeasuredHeight());
    }
    @Test public void scrollRevealsCompleteEpisodeCardIncludingCaption(){
        TvScrollView scroll=new TvScrollView(activity);scroll.setPadding(0,0,0,20);LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);View spacer=new View(activity);body.addView(spacer,new LinearLayout.LayoutParams(400,500));LinearLayout card=new LinearLayout(activity);card.setOrientation(LinearLayout.VERTICAL);card.setFocusable(true);body.addView(card,new LinearLayout.LayoutParams(400,200));body.setPadding(0,0,0,40);
        scroll.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));scroll.layout(0,0,400,300);scroll.requestChildRectangleOnScreen(card,new android.graphics.Rect(0,0,400,1),true);assertTrue(card.getBottom()-scroll.getScrollY()<=scroll.getHeight()-scroll.getPaddingBottom());assertTrue(card.getTop()-scroll.getScrollY()>=0);
    }
    @Test public void propagatedFocusRequestsDoNotJumpBackToTop(){
        TvScrollView scroll=new TvScrollView(activity);LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,0,0,40);scroll.addView(body);
        List<View> cards=new ArrayList<>();for(int n=0;n<8;n++){View card=new View(activity);card.setFocusable(true);body.addView(card,new LinearLayout.LayoutParams(400,160));cards.add(card);}
        scroll.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));scroll.layout(0,0,400,300);
        int previous=0;for(View card:cards){card.requestFocus();scroll.requestChildRectangleOnScreen(body,new android.graphics.Rect(0,card.getTop(),400,card.getBottom()),true);assertTrue(scroll.getScrollY()>=previous);assertTrue(card.getBottom()-scroll.getScrollY()<=300);previous=scroll.getScrollY();scroll.requestChildRectangleOnScreen(body,new android.graphics.Rect(0,card.getTop(),400,card.getBottom()),true);assertEquals(previous,scroll.getScrollY());}
    }
    @Test public void leavingDetailCancelsItsOutstandingWork()throws Exception{
        java.util.concurrent.FutureTask<Void> foreground=new java.util.concurrent.FutureTask<>(()->null),suggestions=new java.util.concurrent.FutureTask<>(()->null);
        for(var entry:Map.of("foregroundTask",foreground,"suggestionsTask",suggestions).entrySet()){Field field=MainActivity.class.getDeclaredField(entry.getKey());field.setAccessible(true);field.set(activity,entry.getValue());}
        Method back=MainActivity.class.getDeclaredMethod("returnCatalog");back.setAccessible(true);back.invoke(activity);assertTrue(foreground.isCancelled());assertTrue(suggestions.isCancelled());
    }
}




