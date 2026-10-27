package pe.animetv;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.speech.RecognizerIntent;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import static pe.animetv.Models.*;
import pe.animetv.Models.Filter;

public class MainActivity extends Activity {
    private final ExecutorService io=Executors.newFixedThreadPool(2),art=Executors.newFixedThreadPool(2),metadata=Executors.newFixedThreadPool(2);
    private final Map<String,List<TextView[]>> pendingTags=new HashMap<>();
    private final Map<String,List<ImageView>> pendingImages=new HashMap<>();private final Map<String,Future<?>> artworkTasks=new HashMap<>();private final Map<ImageView,String> artworkKeys=new IdentityHashMap<>(); private final Set<String> typeRequests=new HashSet<>(); private android.speech.SpeechRecognizer speech; private boolean voicePending,voiceActive;
    private final List<Runnable> progressRefresh=new ArrayList<>();
    @Override protected void onResume(){super.onResume();refreshProgress();refreshLibrarySections();}
    private void refreshProgress(){for(Runnable refresh:List.copyOf(progressRefresh))refresh.run();}
    private void refreshLibrarySections(){
        if(store==null||detailView!=null)return;
        boolean homeChanged=section.equals("Inicio")&&!homeHistory.equals(store.history().toString()),favoritesChanged=section.equals("Favoritos")&&!favoriteState.equals(store.favorites().toString());
        if(!homeChanged&&!favoritesChanged)return;
        View focused=root.findFocus();Object tag=focused==null?null:focused.getTag();boolean restore=!header.hasFocus();
        if(homeChanged)home();else showFavorites();
        if(restore&&catalogView instanceof TvRowsView){TvRowsView rows=(TvRowsView)catalogView;main.post(()->{if(detailView!=null||catalogView!=rows)return;View target=libraryCard(rows,tag);if(target==null)target=libraryCard(rows,null);if(target!=null)rows.restoreFocus(target);else focusMenu();});}
    }
    private View libraryCard(View view,Object tag){
        if((view instanceof TvUi.PosterCard||view instanceof TvUi.EpisodeCard)&&(tag==null||tag.equals(view.getTag())))return view;
        if(view instanceof TvRowsView){for(View child:((TvRowsView)view).model.items){View found=libraryCard(child,tag);if(found!=null)return found;}}
        else if(view instanceof TvRowsView.Rail){for(View child:((TvRowsView.Rail)view).model.items){View found=libraryCard(child,tag);if(found!=null)return found;}}
        else if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++){View found=libraryCard(group.getChildAt(n),tag);if(found!=null)return found;}}
        return null;
    }
    private void observeProgress(View view,Runnable refresh){refresh.run();view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewDetachedFromWindow(View v){progressRefresh.remove(refresh);}public void onViewAttachedToWindow(View v){if(!progressRefresh.contains(refresh))progressRefresh.add(refresh);refresh.run();}});}
    private Recommendations recommendationLoader;
    private final ExecutorService suggestions=Executors.newSingleThreadExecutor();
    private Future<?> suggestionsTask,foregroundTask,filtersTask,warmTask;
    private final ExecutorService controls=Executors.newSingleThreadExecutor(),preload=Executors.newSingleThreadExecutor();
    private boolean filtersLoading;private int filtersGeneration;private Dialog filtersDialog;
    private void cancelForeground(){if(foregroundTask!=null){foregroundTask.cancel(true);foregroundTask=null;}if(filtersTask!=null){filtersTask.cancel(true);filtersTask=null;}filtersLoading=false;filtersGeneration++;if(warmTask!=null){warmTask.cancel(true);warmTask=null;}if(filtersDialog!=null){filtersDialog.dismiss();filtersDialog=null;}}
    private void cancelSuggestions(){if(suggestionsTask!=null){suggestionsTask.cancel(true);suggestionsTask=null;}}
    private final TimedCache<List<RecentEpisode>> recentCache=new TimedCache<>(1,300000);
    private final TimedCache<List<CalendarEntry>> calendarCache=new TimedCache<>(1,300000);
    private final TimedCache<List<Anime>> catalogCache=new TimedCache<>(6,300000);
    private record SectionSnapshot(View view,View banner,int page,long time,String history){}
    private final LinkedHashMap<String,SectionSnapshot> sections=new LinkedHashMap<>(4,.75f,true);
    private boolean sectionReady;private String homeHistory="",favoriteState="";private long sectionBorn;
    private void navigate(String name,Runnable loader){
        if(name.equals(section)&&detailView==null&&sectionReady&&android.os.SystemClock.elapsedRealtime()-sectionBorn<300000)return;
        if(detailView==null&&sectionReady&&List.of("Inicio","Catálogo","Calendario").contains(section)){
            sections.put(section,new SectionSnapshot(catalogView,sectionBanner,page,sectionBorn,section.equals("Inicio")?homeHistory:""));
            while(sections.size()>3)sections.remove(sections.keySet().iterator().next());
        }
        SectionSnapshot saved=sections.remove(name);
        if(saved!=null&&android.os.SystemClock.elapsedRealtime()-saved.time()<300000&&(!name.equals("Inicio")||saved.history().equals(store.history().toString()))){
            begin(name);content.removeAllViews();if(saved.view().getParent() instanceof ViewGroup)((ViewGroup)saved.view().getParent()).removeView(saved.view());catalogView=saved.view();sectionBanner=saved.banner();page=saved.page();sectionBorn=saved.time();if(name.equals("Inicio"))homeHistory=saved.history();content.addView(catalogView,new LinearLayout.LayoutParams(-1,0,1));sectionReady=true;if(name.equals("Catálogo"))resumeCatalogPagination();
        }else loader.run();
    }
    @Override public void onTrimMemory(int level){super.onTrimMemory(level);if(level>=android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW){sections.clear();images.evictAll();}}
    private final Handler main=new Handler(Looper.getMainLooper());
    private Source source;
    protected Source createSource() { return Sources.create(store.source()); }
    private final Map<String,String> filters=new LinkedHashMap<>();
    private Store store;
    private LinearLayout root,content,header,finder;
    private EditText search; private View searchButton;
    private View catalogView,detailView,returnFocus,sectionBanner;
    private int requestGeneration=0,page=1; private boolean pendingCatalogFocus;
    private String query="",section="Inicio",favoriteCategory="Por defecto";
    private final android.util.LruCache<String,android.graphics.Bitmap> images=new android.util.LruCache<>(8*1024*1024) {
        protected int sizeOf(String key,android.graphics.Bitmap bitmap) { return bitmap.getByteCount(); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); ProtectedPages.attach(this); store=new Store(this); source=createSource();recommendationLoader=new Recommendations(source);getSharedPreferences("catalog-ratings-jikan-v1",MODE_PRIVATE).edit().clear().apply();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        root=row(); root.setBackgroundColor(TvUi.BG); applySafeArea(); setContentView(root);
        header=column(); header.setPadding(dp(32),dp(8),dp(8),0); root.addView(header,new LinearLayout.LayoutParams(dp(164)+dp(32),-1));
        ImageView brand=new ImageView(this);brand.setImageResource(R.drawable.cat_vector);brand.setScaleType(ImageView.ScaleType.FIT_CENTER);brand.setContentDescription("AnimeTV");LinearLayout branding=row();branding.setGravity(Gravity.CENTER);branding.addView(brand,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView wordmark=text("AnimeTV",16);wordmark.setTypeface(null,android.graphics.Typeface.BOLD);android.text.SpannableString brandName=new android.text.SpannableString("AnimeTV");brandName.setSpan(new android.text.style.ForegroundColorSpan(TvUi.ACCENT),5,7,0);wordmark.setText(brandName);branding.addView(wordmark);header.addView(branding,new LinearLayout.LayoutParams(-1,dp(58)));root.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus,newFocus)->{if(header!=null&&header.getVisibility()==View.VISIBLE){boolean menuFocus=false;for(View v=newFocus;v!=null;v=v.getParent() instanceof View?(View)v.getParent():null)if(v==header){menuFocus=true;break;}menuExpanded(menuFocus);}});
        nav("Inicio","home",this::home); nav("Catálogo","grid",()->loadCatalog(1)); nav("Calendario","calendar",this::calendar); nav("Favoritos","heart",this::showFavorites); nav("Ajustes","settings",this::settings);
        LinearLayout mainArea=column();root.addView(mainArea,new LinearLayout.LayoutParams(0,-1,1));
        finder=row();finder.setGravity(Gravity.CENTER_VERTICAL); finder.setPadding(0,dp(10),dp(8),dp(8)); mainArea.addView(finder);
        search=new EditText(this); search.setSingleLine(true); search.setTextColor(TvUi.TEXT); search.setHintTextColor(TvUi.MUTED); search.setHint("Buscar anime…"); search.setTextSize(17); search.setPadding(dp(16),0,dp(16),0); search.setBackground(TvUi.background(this,TvUi.PANEL,0));
        finder.addView(search,new LinearLayout.LayoutParams(0,dp(46),1));
        searchButton=catalogAction("Buscar","search",this::submitSearch);finder.addView(searchButton,searchActionSize());search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setOnEditorActionListener((v,action,event)-> {if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH||action==android.view.inputmethod.EditorInfo.IME_ACTION_NEXT||action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE||(event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER&&event.getAction()==KeyEvent.ACTION_DOWN)){submitSearch();return true;}return false;});search.setNextFocusForwardId(View.generateViewId());searchButton.setId(search.getNextFocusForwardId());
        finder.addView(catalogAction("Buscar por voz","voice",this::voice),searchActionSize());finder.addView(catalogAction("Filtros","filter",this::showFilters),searchActionSize());
        content=column(); mainArea.addView(content,new LinearLayout.LayoutParams(-1,0,1));menuExpanded(header.hasFocus());if(getIntent().getBooleanExtra("return-catalog",false))loadCatalog(1);else home();
    }
    private void nav(String label,String icon,Runnable action) { LinearLayout b=button(label,icon,()->navigate(label,action));b.setGravity(Gravity.CENTER_VERTICAL);b.setPadding(dp(12),dp(12),dp(8),dp(12));b.setTag(label);b.setOnFocusChangeListener((v,focused)->{TvUi.refreshFocus(v);if(focused)menuExpanded(true);else main.post(()->menuExpanded(header.hasFocus()));});b.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&event.getAction()==KeyEvent.ACTION_DOWN){if(section.equals("Catálogo")){pendingCatalogFocus=!focusFirstCatalogAnime();}else {View target=firstFocusable(content);if(target!=null)target.requestFocus();}return true;}return false;});TextView caption=(TextView)b.getChildAt(1);caption.setSingleLine(true);caption.setTextSize(14);caption.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(5));header.addView(b,p); }
    private LinearLayout begin(String name) {
        cancelForeground();cancelSuggestions();++requestGeneration;sectionReady=false;sectionBorn=android.os.SystemClock.elapsedRealtime(); pendingCatalogFocus=false;section=name; detailView=null; returnFocus=null; header.setVisibility(View.VISIBLE); finder.setVisibility(name.equals("Catálogo")?View.VISIBLE:View.GONE);
        for(int i=1;i<header.getChildCount();i++) { View v=header.getChildAt(i); v.setSelected(name.equals(v.getTag())); TvUi.refreshFocus(v); }
        content.removeAllViews();TvRowsView rows=new TvRowsView(this);content.addView(rows,new LinearLayout.LayoutParams(-1,0,1));catalogView=rows;sectionBanner=null;return rows.model;
    }
    private void menuExpanded(boolean expanded){
        int width=dp(expanded?190:104);if(header.getLayoutParams().width!=width){header.getLayoutParams().width=width;header.requestLayout();}
        ((LinearLayout)header.getChildAt(0)).getChildAt(1).setVisibility(expanded?View.VISIBLE:View.GONE);for(int n=1;n<header.getChildCount();n++){LinearLayout item=(LinearLayout)header.getChildAt(n);item.setGravity(expanded?Gravity.CENTER_VERTICAL:Gravity.CENTER);item.getChildAt(1).setVisibility(expanded?View.VISIBLE:View.GONE);((LinearLayout.LayoutParams)item.getChildAt(0).getLayoutParams()).rightMargin=expanded?dp(7):0;}
    }
    private void title(LinearLayout body,String title,String subtitle){
        LinearLayout banner=column();banner.setPadding(dp(8),dp(10),dp(8),dp(6));
        LinearLayout top=row();top.setGravity(Gravity.CENTER_VERTICAL);TextView heading=text(section.equals("Catálogo")&&!query.isBlank()?"Resultados: "+query:section,25);heading.setTypeface(null,android.graphics.Typeface.BOLD);top.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout sourceBadge=TvUi.button(this,source.name(),null,this::sourcesInfo);sourceBadge.setFocusableInTouchMode(true);sourceBadge.setMinimumHeight(dp(28));sourceBadge.setPadding(dp(7),dp(4),dp(7),dp(4));((TextView)sourceBadge.getChildAt(0)).setTextSize(11);sourceBadge.addView(SourceBrand.icon(this,source.id(),14),0);((LinearLayout.LayoutParams)sourceBadge.getChildAt(0).getLayoutParams()).rightMargin=dp(8);sourceBadge.setTag("source-picker");sourceBadge.setContentDescription("Cambiar fuente. Fuente activa: "+source.name());top.addView(sourceBadge);banner.addView(top);
        TextView sub=text(subtitle,13);sub.setTextColor(TvUi.MUTED);banner.addView(sub);
        if(body instanceof TvRowsView.RowModel){sectionBanner=banner;body.addView(banner,0,new LinearLayout.LayoutParams(-1,-2));}else body.addView(banner);
    }
    private void continueWatching(LinearLayout body,JSONArray history){
        LinearLayout line=shelf(body);for(int n=0;n<Math.min(10,history.length());n++){
            JSONObject a=history.optJSONObject(n);Anime anime=new Anime(a.optString("title"),a.optString("url"),a.optString("image"));
            String last=store.lastEpisode(anime.url()),artwork=store.backdrop(anime.url());
            FrameLayout card=(FrameLayout)landscape(anime.title(),"",artwork.isBlank()?anime.image():artwork,()->{returnFocus=root.findFocus();showDetail(anime);});
            final int historyIndex=n;card.setTag("continue:"+anime.url());LongOk.bind(card,held->removeContinueMenu(anime,historyIndex,held));
            TextView label=(TextView)card.getChildAt(2);label.setMaxLines(2);observeProgress(card,()->{String current=store.lastEpisode(anime.url());long duration=store.duration(current),position=store.position(current);label.setText(anime.title()+(duration>0?"\n"+Math.max(0,(duration-position)/60000)+" min restantes":""));});
            LinearLayout origin=SourceBrand.badge(this,Sources.forUrl(anime.url(),source),10);FrameLayout.LayoutParams badge=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT);badge.setMargins(dp(9),dp(9),dp(9),0);card.addView(origin,badge);
            addProgress(card,()->store.lastEpisode(anime.url()));LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(245),-2);size.rightMargin=dp(16);line.addView(card,size);
        }
    }

    private void removeContinueMenu(Anime anime,int index,boolean held){
        LinearLayout body=column();Dialog menu=dialog("Seguir viendo",body);
        body.addView(text("¿Quieres eliminar este episodio de Seguir viendo?",17));body.addView(text(anime.title(),15));body.addView(text("Tu progreso queda guardado.",12));
        View remove=button("Eliminar de Seguir viendo","close",()->{store.removeContinueWatching(anime.url());menu.dismiss();refreshContinueHome(index);});remove.setTag("continue-remove");body.addView(remove);
        View cancel=button("Cancelar",null,menu::dismiss);cancel.setTag("continue-cancel");body.addView(cancel);
        LongOk.consumeHeldRelease(menu,held);
    }
    private void refreshContinueHome(int index){
        home();TvRowsView rows=(TvRowsView)catalogView;JSONArray history=store.history();
        if(history.length()==0){View nav=header.findViewWithTag("Inicio");if(nav!=null)nav.requestFocus();return;}
        String tag="continue:"+history.optJSONObject(Math.min(index,Math.min(9,history.length()-1))).optString("url");
        main.post(()->{if(!section.equals("Inicio")||catalogView!=rows)return;for(View item:rows.model.items)if(item instanceof TvRowsView.Rail)for(View card:((TvRowsView.Rail)item).model.items)if(tag.equals(card.getTag())){rows.restoreFocus(card);return;}});
    }
    private void clearContinueMenu(LinearLayout field){
        LinearLayout body=column();Dialog menu=dialog("Limpiar Seguir viendo",body);
        body.addView(text("¿Quieres eliminar todos los episodios de Seguir viendo?",17));body.addView(text("Tu progreso queda guardado.",12));
        View clear=button("Limpiar Seguir viendo","close",()->{store.clearContinueWatching();((TextView)field.getChildAt(1)).setText("Sin episodios");field.setContentDescription("Limpiar Seguir viendo, Sin episodios");menu.dismiss();});clear.setTag("continue-clear-confirm");body.addView(clear);
        View cancel=button("Cancelar",null,menu::dismiss);cancel.setTag("continue-cancel");body.addView(cancel);
    }
    private void home() {
        homeHistory=store.history().toString();
        LinearLayout body=begin("Inicio"); title(body,"Hoy hay algo nuevo para ver","Últimos capítulos publicados");
        JSONArray history=store.history(); if(history.length()>0) { body.addView(text("Seguir viendo",19)); continueWatching(body,history); }
        body.addView(text("Capítulos recientes",21)); TextView loading=text("Consultando estrenos…",17); body.addView(loading); int token=requestGeneration;
        foregroundTask=io.submit(()-> { try { List<RecentEpisode> feed=recentCache.get("recent",()->List.copyOf(source.recent())); main.post(()-> { if(!valid(token))return; sectionReady=true;body.removeView(loading); LinearLayout line=null; for(int n=0;n<feed.size();n++) { RecentEpisode item=feed.get(n);store.backdrop(item.anime().url(),item.episode().image()); if(n%10==0) { if(n>0)body.addView(text("Más capítulos recientes",21));line=shelf(body); } View card=portrait(item.anime(),item.episode().name()+(item.published().isBlank()?"":" · "+item.published()),()->showDetail(item.anime(),item.episode().url())); addShelfCard(line,card); } if(feed.isEmpty())body.addView(text("La fuente no devolvió capítulos recientes.",17)); }); } catch(Exception e) { main.post(()-> { if(valid(token)) { loading.setText("No se pudieron cargar los estrenos: "+e.getMessage()); body.addView(button("Reintentar","retry",this::home)); } }); } });
    }
    private int calendarDay=java.time.LocalDate.now().getDayOfWeek().getValue();
    private void calendar(){
        LinearLayout body=begin("Calendario");title(body,"Calendario","Tu semana de anime · horario local referencial");TextView loading=text("Consultando horario…",17);body.addView(loading);int token=requestGeneration;
        foregroundTask=io.submit(()->{try{List<CalendarEntry> entries=calendarCache.get("calendar",()->List.copyOf(source.calendar()));main.post(()->{if(!valid(token))return;sectionReady=true;body.removeView(loading);renderCalendar(body,entries);});}catch(Exception e){main.post(()->{if(valid(token)){loading.setText("No se pudo consultar el calendario.");body.addView(button("Reintentar","retry",this::calendar));}});}});
    }
    private void renderCalendar(LinearLayout body,List<CalendarEntry> entries){
        List<View> daysContent=new ArrayList<>();String[] days={"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"};LinearLayout tabs=shelf(body);
        Runnable show=()->{for(View old:daysContent)body.removeView(old);daysContent.clear();List<CalendarEntry> selected=new ArrayList<>();for(CalendarEntry e:entries)if(e.day()==calendarDay)selected.add(e);
            TextView heading=text(days[calendarDay-1]+" · "+selected.size()+" animes",20);body.addView(heading);daysContent.add(heading);
            TvRowsView.Rail rail=new TvRowsView.Rail(this);body.addView(rail);daysContent.add(rail);for(CalendarEntry e:selected)addShelfCard(rail.model,portrait(e.anime(),e.publication().isBlank()?e.label():e.publication(),()->showDetail(e.anime())));
            if(selected.isEmpty()){TextView empty=text("Sin publicaciones en este día.",15);body.addView(empty);daysContent.add(empty);}
            for(View v:((TvRowsView.RowModel)tabs).items){v.setSelected(Objects.equals(v.getTag(),calendarDay));TvUi.refreshFocus(v);}
        };
        for(int n=1;n<=7;n++){final int day=n;View tab=button(days[n-1],null,()->{calendarDay=day;show.run();});tab.setTag(day);tabs.addView(tab,new LinearLayout.LayoutParams(dp(104),dp(44)));}show.run();
    }

    private boolean valid(int token) { return token==requestGeneration && !isDestroyed(); }
    private void submitSearch() { query=search.getText().toString().trim();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();loadCatalog(1);searchButton.requestFocus(); }
    private void keyboardFallback(String message) { message(message);search.requestFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT); }
    private void voice() {
        if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){voicePending=true;requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},41);return;}
        if(!android.speech.SpeechRecognizer.isRecognitionAvailable(this)){keyboardFallback("No hay servicio de voz disponible. Puedes usar el micrófono de Gboard.");return;}
        if(speech==null){speech=android.speech.SpeechRecognizer.createSpeechRecognizer(this);speech.setRecognitionListener(new android.speech.RecognitionListener(){
            public void onReadyForSpeech(Bundle b){message("Escuchando…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float rms){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){}public void onEvent(int type,Bundle b){}
            private void words(Bundle b,boolean finish){ArrayList<String> matches=b.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);if(matches!=null&&!matches.isEmpty()){search.setText(matches.get(0));if(finish)submitSearch();}else if(finish)keyboardFallback("No se recibió texto. Puedes usar el micrófono del teclado.");}
            public void onResults(Bundle b){if(!voiceActive)return;voiceActive=false;words(b,true);}public void onPartialResults(Bundle b){if(voiceActive)words(b,false);}public void onError(int code){if(!voiceActive)return;voiceActive=false;keyboardFallback("La búsqueda por voz no pudo completarse ("+code+"). Usa el micrófono de Gboard o escribe el título.");}
        });}
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-PE").putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);voiceActive=true;speech.startListening(intent);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==41&&voicePending){voicePending=false;if(results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)voice();else keyboardFallback("Puedes buscar con el micrófono de Gboard o escribiendo.");}}
    @Override protected void onActivityResult(int request,int result,Intent data) { super.onActivityResult(request,result,data); if(request==40 && result==RESULT_OK && data!=null) { ArrayList<String> words=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS); if(words!=null && !words.isEmpty()) { search.setText(words.get(0)); query=words.get(0); loadCatalog(1); } } }
    private void loadCatalog(int selectedPage) {
        if(selectedPage<1)return;
        boolean navigating=section.equals("Catálogo")&&selectedPage!=page;page=selectedPage;
        LinearLayout body=begin("Catálogo");title(body,query.isEmpty()?"Encuentra tu próximo anime":"Resultados: "+query,"Página "+page+(filters.values().stream().anyMatch(v->!v.isEmpty())?" · filtros activos":" · todas las historias"));
        TextView loading=text("Cargando catálogo…",18);body.addView(loading);int token=requestGeneration;String capturedQuery=query;Map<String,String> capturedFilters=Map.copyOf(filters);LinearLayout pages=row();pages.setTag("catalog-pager");
        foregroundTask=io.submit(()->{
            try{
                List<Anime> items=catalogPage(capturedQuery,selectedPage,capturedFilters);
                main.post(()->{
                    if(!valid(token))return;sectionReady=true;body.removeView(loading);pendingCatalogFocus=navigating&&!items.isEmpty();displayGrid(body,items);body.addView(pages);
                    View previous=addPreviousPage(pages,selectedPage);
                    if(items.isEmpty()){body.addView(text("No hay resultados. Prueba otra búsqueda o limpia los filtros.",18));if(previous!=null)restorePageControl(previous,token);}
                });
                // Display this page immediately; the look-ahead never blocks its cards.
                if(!items.isEmpty())try{
                    List<Anime> next=catalogPage(capturedQuery,selectedPage+1,capturedFilters);
                    if(CatalogPagination.hasMore(items,next))main.post(()->{if(valid(token)){View button=button("Siguiente página","next",()->loadCatalog(selectedPage+1));button.setFocusableInTouchMode(true);button.setTag("catalog-next");addAction(pages,button);}});
                }catch(Exception unavailable){/* Keep current page usable; never invent a next page. */}
            }catch(Exception failure){main.post(()->{
                if(!valid(token))return;loading.setText("No se pudo cargar: "+failure.getMessage());body.addView(pages);View previous=addPreviousPage(pages,selectedPage);
                body.addView(button("Reintentar","retry",()->loadCatalog(selectedPage)));if(previous!=null)restorePageControl(previous,token);
            });}
        });
    }
    private List<Anime> catalogPage(String query,int page,Map<String,String> selected)throws Exception{
        return catalogCache.get(query+"|"+page+"|"+new TreeMap<>(selected),()->List.copyOf(source.catalog(query,page,selected)));
    }
    private View addPreviousPage(LinearLayout parent,int selected){
        if(selected<=1)return null;View previous=button("Anterior","previous",()->loadCatalog(selected-1));previous.setFocusableInTouchMode(true);previous.setTag("catalog-previous");addAction(parent,previous);return previous;
    }
    private void restorePageControl(View target,int token){main.post(()->{if(valid(token)&&detailView==null&&section.equals("Catálogo")&&catalogView instanceof TvRowsView)((TvRowsView)catalogView).restoreFocus(target);});}
    private void resumeCatalogPagination(){
        if(!section.equals("Catálogo")||!(catalogView instanceof TvRowsView))return;
        for(View item:((TvRowsView)catalogView).model.items)if("catalog-pager".equals(item.getTag())&&item instanceof LinearLayout&&item.findViewWithTag("catalog-next")==null){
            LinearLayout pages=(LinearLayout)item;int selected=page,token=requestGeneration;String searchQuery=query;Map<String,String> values=Map.copyOf(filters);
            foregroundTask=io.submit(()->{try{List<Anime> current=catalogPage(searchQuery,selected,values),next=current.isEmpty()?List.of():catalogPage(searchQuery,selected+1,values);if(CatalogPagination.hasMore(current,next))main.post(()->{if(valid(token)&&pages.findViewWithTag("catalog-next")==null){View advance=button("Siguiente página","next",()->loadCatalog(selected+1));advance.setFocusableInTouchMode(true);advance.setTag("catalog-next");addAction(pages,advance);}});}catch(Exception unavailable){/* Keep the original cards and their focus on return. */}});return;
        }
    }
    private void displayGrid(LinearLayout parent,List<Anime> items) {
        if(!section.equals("Catálogo") || detailView!=null){LinearLayout shelf=shelf(parent);for(Anime anime:items)addShelfCard(shelf,portrait(anime,"",()->showDetail(anime)));return;}
        int columns=Math.max(3,Math.min(6,(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density-104)/140));LinearLayout line=null;for(int n=0;n<items.size();n++){Anime anime=items.get(n);if(n%columns==0){line=row();parent.addView(line);}addCard(line,portrait(anime,"",()->showDetail(anime),true));}pad(line,items.size(),columns);if(pendingCatalogFocus)main.post(()->{if(detailView==null&&section.equals("Catálogo"))pendingCatalogFocus=!focusFirstCatalogAnime();});
    }
    private View findPoster(View view){
        if(view instanceof TvUi.PosterCard)return view;
        if(view instanceof TvRowsView){for(View child:((TvRowsView)view).model.items){View found=findPoster(child);if(found!=null)return found;}}
        else if(view instanceof TvRowsView.Rail){for(View child:((TvRowsView.Rail)view).model.items){View found=findPoster(child);if(found!=null)return found;}}
        else if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++){View found=findPoster(group.getChildAt(n));if(found!=null)return found;}}
        return null;
    }
    private boolean focusFirstCatalogAnime(){if(!(catalogView instanceof TvRowsView))return false;View first=findPoster(catalogView);if(first==null)return false;((TvRowsView)catalogView).restoreFocus(first);return true;}
    private LinearLayout shelf(LinearLayout parent) { TvRowsView.Rail rail=new TvRowsView.Rail(this);parent.addView(rail,new LinearLayout.LayoutParams(-1,-2));return rail.model; }
    private View firstFocusable(View view){if(view instanceof TvRowsView)return ((TvRowsView)view).grid;if(view.getVisibility()!=View.VISIBLE)return null;if(view.isFocusable()&&!(view instanceof ScrollView)&&!(view instanceof HorizontalScrollView))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++){View found=firstFocusable(group.getChildAt(n));if(found!=null)return found;}}return null;}
    private void addShelfCard(LinearLayout line,View card){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(140),-2);p.setMargins(0,0,dp(16),0);line.addView(card,p);}
    private View portrait(Anime anime,String subtitle,Runnable action){
        return portrait(anime,subtitle,action,false);
    }
    private View portrait(Anime anime,String subtitle,Runnable action,boolean stackedTags){
        if(!anime.status().isBlank())store.status(anime.url(),anime.status());FrameLayout card=new TvUi.PosterCard(this);card.setPadding(dp(4),dp(4),dp(4),dp(4));TvUi.focus(card);card.setContentDescription(anime.title());card.setTag(anime.url());String sourceId=Sources.forUrl(anime.url(),source).id();boolean horizontalRecent=(sourceId.equals("latanime")&&anime.image().contains("/thumbs/portada/"))||(sourceId.equals("animelatinohd")&&(anime.image().isBlank()||anime.image().contains("/w780/")));String knownPoster=store.poster(anime.url());card.addView(poster(!knownPoster.isBlank()?knownPoster:horizontalRecent?"":anime.image()),new FrameLayout.LayoutParams(-1,-1));
        View fade=new View(this);fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x000C0F14,0xF50C0F14}));FrameLayout.LayoutParams fadeSize=new FrameLayout.LayoutParams(-1,dp(100),Gravity.BOTTOM);card.addView(fade,fadeSize);
        LinearLayout tags=stackedTags?column():row();tags.setPadding(dp(5),dp(5),dp(5),0);tags.setGravity(Gravity.RIGHT);TextView type=cardTag(anime.type().isBlank()?store.type(anime.url()):anime.type(),0xD928303B);TextView state=cardTag(store.status(anime.url()),0xD9236B50);state.setTag("status");updateTag(state,store.status(anime.url()));tags.addView(type,new LinearLayout.LayoutParams(-2,-2));LinearLayout.LayoutParams stateSize=new LinearLayout.LayoutParams(-2,-2);if(stackedTags)stateSize.topMargin=dp(3);else stateSize.leftMargin=dp(3);tags.addView(state,stateSize);card.addView(tags,new FrameLayout.LayoutParams(stackedTags?-2:-1,-2,Gravity.TOP|Gravity.RIGHT));
        TextView label=text(anime.title(),14);label.setTypeface(null,android.graphics.Typeface.BOLD);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setPadding(dp(8),dp(8),dp(8),dp(9));card.addView(label,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        if(!anime.type().isBlank())store.type(anime.url(),anime.type());if(!subtitle.isBlank())store.publication(anime.url(),subtitle);
        TextView[] pair={type,state};card.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewDetachedFromWindow(View v){List<TextView[]> waiting=pendingTags.get(anime.url());if(waiting!=null){waiting.remove(pair);if(waiting.isEmpty())pendingTags.remove(anime.url());}}
            public void onViewAttachedToWindow(View v){String knownType=anime.type().isBlank()?store.type(anime.url()):anime.type(),knownStatus=store.status(anime.url());updateTag(type,knownType);updateTag(state,knownStatus);boolean missingPoster=horizontalRecent&&store.poster(anime.url()).isBlank();if(!store.poster(anime.url()).isBlank())image((ImageView)card.getChildAt(0),store.poster(anime.url()));if(!knownType.isBlank()&&!knownStatus.isBlank()&&!missingPoster)return;List<TextView[]> waiting=pendingTags.computeIfAbsent(anime.url(),key->new ArrayList<>());if(!waiting.contains(pair))waiting.add(pair);if(typeRequests.add(anime.url()))metadata.execute(()->{try{Detail resolved=Sources.forUrl(anime.url(),source).metadata(anime);store.type(anime.url(),resolved.anime().type());store.status(anime.url(),resolved.status());store.poster(anime.url(),resolved.anime().image());main.post(()->{typeRequests.remove(anime.url());List<TextView[]> targets=pendingTags.remove(anime.url());if(!isDestroyed()&&targets!=null)for(TextView[] target:targets){updateTag(target[0],store.type(anime.url()));updateTag(target[1],resolved.status());if(target[0].getParent() instanceof View&&((View)target[0].getParent()).getParent() instanceof FrameLayout){FrameLayout targetCard=(FrameLayout)((View)target[0].getParent()).getParent();image((ImageView)targetCard.getChildAt(0),store.poster(anime.url()));}}});}catch(Exception ignored){main.post(()->{typeRequests.remove(anime.url());pendingTags.remove(anime.url());});}});}
        });
        addProgress(card,()->store.lastEpisode(anime.url()));card.setOnClickListener(v->{if(detailView==null)returnFocus=card;action.run();});return card;
    }
    private TextView cardTag(String value,int color){TextView tag=text(value,9);tag.setSingleLine(true);tag.setEllipsize(android.text.TextUtils.TruncateAt.END);tag.setGravity(Gravity.CENTER);tag.setPadding(dp(5),dp(3),dp(5),dp(3));tag.setBackground(TvUi.background(this,color,0));updateTag(tag,value);return tag;}
    private void updateTag(TextView tag,String value){if("status".equals(tag.getTag()))tag.setBackground(TvUi.background(this,value.equalsIgnoreCase("En emisión")?0xE6236B50:(value.equalsIgnoreCase("Próximamente")||value.equalsIgnoreCase("Estreno"))?0xE66E5928:value.equalsIgnoreCase("Finalizado")?0xE69F303C:0xE638414E,0));tag.setText(value);tag.setVisibility(value.isBlank()?View.GONE:View.VISIBLE);}
    private ImageView poster(String url) { ImageView view=new ImageView(this); view.setScaleType(ImageView.ScaleType.CENTER_CROP); view.setBackground(TvUi.background(this,0xFF293348,0)); view.setClipToOutline(true);view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){image(view,url);}public void onViewDetachedFromWindow(View v){detachArtwork(view);}}); return view; }
    private View landscape(String title,String subtitle,String url,Runnable action) {
        FrameLayout card=new TvUi.EpisodeCard(this);TvUi.focus(card);card.setPadding(dp(4),dp(4),dp(4),dp(4));card.addView(poster(url),new FrameLayout.LayoutParams(-1,-1));
        View fade=new View(this);fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x000C0F14,0xEE0C0F14}));card.addView(fade,new FrameLayout.LayoutParams(-1,dp(48),Gravity.BOTTOM));
        TextView label=text(title,15);label.setMaxLines(1);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setPadding(dp(8),dp(5),dp(8),dp(6));card.addView(label,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        TvUi.Icon play=new TvUi.Icon(this,"play");play.setPadding(dp(8),dp(8),dp(8),dp(8));android.graphics.drawable.GradientDrawable playCircle=TvUi.background(this,0xCC101318,0);playCircle.setShape(android.graphics.drawable.GradientDrawable.OVAL);play.setBackground(playCircle);play.setVisibility(View.GONE);card.addView(play,new FrameLayout.LayoutParams(dp(40),dp(40),Gravity.CENTER));
        card.setOnFocusChangeListener((v,focused)->{v.setBackground(TvUi.background(this,focused?0xFF29384C:TvUi.PANEL,focused?TvUi.ACCENT:0));play.setVisibility(focused?View.VISIBLE:View.GONE);});card.setContentDescription(title+(subtitle.isBlank()?"":", "+subtitle));card.setOnClickListener(v->action.run());return card;
    }
    private void addProgress(FrameLayout card,String episode) {addProgress(card,()->episode,true);}
    private void addProgress(FrameLayout card,java.util.function.Supplier<String> episodeUrl) {
        addProgress(card,episodeUrl,false);
    }
    private void addProgress(FrameLayout card,java.util.function.Supplier<String> episodeUrl,boolean fadeWatched) {
        String episode=episodeUrl.get();
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setProgress(store.progress(episode));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(TvUi.ACCENT));card.addView(bar,new FrameLayout.LayoutParams(-1,dp(4),Gravity.BOTTOM));
        TextView check=text("✓",18);check.setPadding(dp(8),dp(4),dp(8),dp(4));check.setBackground(TvUi.background(this,0xCC101318,0));check.setVisibility(store.watched(episode)?View.VISIBLE:View.GONE);card.addView(check,new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.RIGHT));
        ImageView artwork=fadeWatched&&card.getChildAt(0) instanceof ImageView?(ImageView)card.getChildAt(0):null;
        android.graphics.ColorMatrix grayscale=new android.graphics.ColorMatrix();grayscale.setSaturation(0.15f);android.graphics.ColorMatrixColorFilter watchedFilter=new android.graphics.ColorMatrixColorFilter(grayscale);
        observeProgress(card,()->{String current=episodeUrl.get();boolean watched=store.watched(current);bar.setProgress(watched?1000:store.progress(current));bar.setVisibility(watched||store.duration(current)>0?View.VISIBLE:View.GONE);check.setVisibility(watched?View.VISIBLE:View.GONE);if(artwork!=null){artwork.setColorFilter(watched?watchedFilter:null);artwork.setAlpha(watched?0.65f:1f);}});
    }
    private void showRecommendations(LinearLayout panel,List<Anime> items){
        if(items.equals(panel.getTag()))return;
        View focused=panel.hasFocus()?root.findFocus():null;Object oldTag=focused==null?null:focused.getTag();
        panel.removeAllViews();panel.setTag(items);displayGrid(panel,items);
        if(items.isEmpty())panel.addView(text("No hay sugerencias disponibles en esta fuente.",15));
        if(oldTag!=null&&detailView instanceof TvRowsView){for(int n=0;n<panel.getChildCount();n++){View row=panel.getChildAt(n);if(row instanceof TvRowsView.Rail)for(View card:((TvRowsView.Rail)row).model.items)if(oldTag.equals(card.getTag())){TvRowsView current=(TvRowsView)detailView;main.post(()->{if(detailView==current)current.restoreFocus(card);});return;}}}
    }
    private LinearLayout compactEpisode(Episode episode,Runnable action){
        TvUi.ProgressRow item=new TvUi.ProgressRow(this);item.setContentDescription(episode.name());item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(12),dp(6),dp(12),dp(6));item.setMinimumHeight(dp(44));TvUi.focus(item);item.setOnClickListener(v->action.run());
        TextView caption=text(episode.name(),14);caption.setSingleLine(true);caption.setEllipsize(android.text.TextUtils.TruncateAt.END);item.addView(caption,new LinearLayout.LayoutParams(0,-2,1));TextView check=text("✓",18);check.setTag("episode-watched-check");check.setPadding(dp(12),0,0,0);item.addView(check,new LinearLayout.LayoutParams(-2,-2));
        observeProgress(item,()->{boolean watched=store.watched(episode.url());check.setVisibility(watched?View.VISIBLE:View.GONE);item.setContentDescription(episode.name()+(watched?", visto":""));item.setProgress(watched?1000:store.progress(episode.url()));});return item;
    }
    private void addCard(LinearLayout line,View card) { LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-2,1); size.setMargins(dp(6),dp(8),dp(6),dp(8)); line.addView(card,size); }
    private void pad(LinearLayout line,int count,int columns) { if(line!=null && count%columns!=0)for(int n=count%columns;n<columns;n++)line.addView(new View(this),new LinearLayout.LayoutParams(0,1,1)); }
    private void detachArtwork(ImageView view){
        artworkKeys.remove(view);Iterator<Map.Entry<String,List<ImageView>>> iterator=pendingImages.entrySet().iterator();
        while(iterator.hasNext()){var entry=iterator.next();entry.getValue().remove(view);if(entry.getValue().isEmpty()){Future<?> task=artworkTasks.remove(entry.getKey());if(task!=null)task.cancel(true);iterator.remove();}}
    }
    private void image(ImageView view,String url){image(view,url,dp(180));}
    private void image(ImageView view,String url,int targetWidth){
        if(isDestroyed()||url==null||url.isBlank())return;String key=url+"|"+targetWidth;if(!key.equals(artworkKeys.get(view))){detachArtwork(view);artworkKeys.put(view,key);}android.graphics.Bitmap cached=images.get(key);if(cached!=null){view.setImageBitmap(cached);return;}
        List<ImageView> waiting=pendingImages.get(key);if(waiting!=null){if(!waiting.contains(view))waiting.add(view);return;}List<ImageView> targets=new ArrayList<>(List.of(view));pendingImages.put(key,targets);
        artworkTasks.put(key,art.submit(()->{android.graphics.Bitmap result=null;try{
            {byte[] data=ArtworkBytes.get(ArtworkHttp.client(this),ArtworkHttp.request(url));BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);o.inSampleSize=1;while(o.outWidth/(o.inSampleSize*2)>=targetWidth)o.inSampleSize*=2;o.inJustDecodeBounds=false;result=BitmapFactory.decodeByteArray(data,0,data.length,o);if(result!=null)images.put(key,result);}
        }catch(Exception ignored){}android.graphics.Bitmap bitmap=result;main.post(()->{if(pendingImages.get(key)!=targets)return;pendingImages.remove(key);artworkTasks.remove(key);if(!isDestroyed()&&bitmap!=null)for(ImageView target:targets)target.postOnAnimation(()->{if(!isDestroyed()&&target.isAttachedToWindow()&&key.equals(artworkKeys.get(target)))target.setImageBitmap(bitmap);});});}));
    }
    private void showDetail(Anime anime) { showDetail(anime,null); }
    private void showDetail(Anime anime,String episodeToPlay) {
        cancelForeground();cancelSuggestions();int token=++requestGeneration; header.setVisibility(View.GONE); finder.setVisibility(View.GONE); content.removeAllViews();TvRowsView rows=new TvRowsView(this);LinearLayout panel=rows.model;detailView=rows;rows.setBackgroundColor(TvUi.BG);rows.grid.setPadding(dp(24),dp(24),dp(24),dp(24));rows.grid.setClipToPadding(false);content.addView(rows,new LinearLayout.LayoutParams(-1,-1)); TextView loading=text("Cargando "+anime.title()+"…",22); panel.addView(loading);
        foregroundTask=io.submit(()-> { try { Detail detail=Sources.forUrl(anime.url(),source).detail(anime); main.post(()-> { if(!valid(token))return; panel.removeAllViews(); renderDetail(panel,detail,token);if(episodeToPlay==null)warmEpisode(detail,null,token); if(episodeToPlay!=null) { for(int n=0;n<detail.episodes().size();n++)if(detail.episodes().get(n).url().equals(episodeToPlay)) { confirmRecentEpisode(detail,n); return; } message("El capítulo no aparece todavía en la ficha de la fuente."); } }); } catch(Exception e) { main.post(()-> { if(valid(token)) { loading.setText("No se pudo cargar: "+e.getMessage()); panel.addView(button("Reintentar","retry",()->showDetail(anime,episodeToPlay))); } }); } });
    }
    private void addDetailHeader(LinearLayout panel,LinearLayout hero,String backdrop){
        if(backdrop.isBlank()){panel.addView(hero);return;}
        FrameLayout background=new FrameLayout(this);background.setTag("detail-background");background.setBackgroundColor(TvUi.BG);
        ImageView art=new ImageView(this);art.setScaleType(ImageView.ScaleType.CENTER_CROP);art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);background.addView(art,new FrameLayout.LayoutParams(-1,-1));
        art.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){image(art,backdrop,Math.min(1280,getResources().getDisplayMetrics().widthPixels));}public void onViewDetachedFromWindow(View v){detachArtwork(art);}});
        View horizontal=new View(this);horizontal.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xF20C0F14,0xB30C0F14}));background.addView(horizontal,new FrameLayout.LayoutParams(-1,-1));
        View vertical=new View(this);vertical.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x000C0F14,0xFF0C0F14}));background.addView(vertical,new FrameLayout.LayoutParams(-1,-1));
        background.addView(hero,new FrameLayout.LayoutParams(-1,-2));panel.addView(background);
    }
    private void renderDetail(LinearLayout panel,Detail detail,int token) {
        Anime anime=detail.anime();store.poster(anime.url(),anime.image());if(!detail.backdrop().isBlank()&&panel instanceof TvRowsView.RowModel){TvRowsView.RowModel model=(TvRowsView.RowModel)panel;model.contentInsets=dp(24);model.grid.setPadding(0,0,0,dp(24));}LinearLayout hero=row();hero.setGravity(Gravity.TOP);hero.setPadding(detail.backdrop().isBlank()?0:dp(24),detail.backdrop().isBlank()?0:dp(24),detail.backdrop().isBlank()?0:dp(24),dp(20));addDetailHeader(panel,hero,detail.backdrop());hero.addView(poster(anime.image()),new LinearLayout.LayoutParams(dp(164),dp(246)));
        LinearLayout info=column();info.setPadding(dp(24),0,dp(12),0);hero.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        TextView name=text(anime.title(),30);name.setTypeface(null,android.graphics.Typeface.BOLD);name.setPadding(dp(4),0,dp(4),dp(3));name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(name,new LinearLayout.LayoutParams(-1,-2));
        String metadata=String.join("  ·  ",Arrays.asList(detail.type(),detail.year(),detail.status()).stream().filter(value->!value.isBlank()).toArray(String[]::new));TextView meta=text(metadata,13);meta.setTextColor(TvUi.ACCENT);meta.setSingleLine(true);meta.setVisibility(metadata.isBlank()?View.GONE:View.VISIBLE);info.addView(meta);store.status(anime.url(),detail.status());
        String published=PublicationTime.display(store.publication(anime.url()));TextView date=text("Última publicación · "+published,12);date.setTextColor(TvUi.MUTED);date.setSingleLine(true);date.setEllipsize(android.text.TextUtils.TruncateAt.END);date.setVisibility(published.isBlank()?View.GONE:View.VISIBLE);info.addView(date);
        LinearLayout genres=row();genres.setGravity(Gravity.CENTER_VERTICAL);genres.setVisibility(detail.genres().isEmpty()?View.GONE:View.VISIBLE);info.addView(genres,new LinearLayout.LayoutParams(-1,dp(26)));for(Genre genre:detail.genres()){TextView chip=text(genre.name(),12);chip.setPadding(dp(4),0,dp(4),0);chip.setSingleLine(true);chip.setGravity(Gravity.CENTER_VERTICAL);chip.setTextColor(TvUi.MUTED);LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(-2,-1);size.setMargins(dp(4),0,dp(4),0);genres.addView(chip,size);}
        TextView synopsis=text(detail.description(),16);synopsis.setMaxLines(2);synopsis.setEllipsize(android.text.TextUtils.TruncateAt.END);synopsis.setVisibility(detail.description().isBlank()?View.GONE:View.VISIBLE);info.addView(synopsis,new LinearLayout.LayoutParams(-1,-2));synopsis.setContentDescription("Sinopsis: "+detail.description());LinearLayout detailActions=row();detailActions.setPadding(0,dp(6),0,0);info.addView(detailActions);
        LinearLayout identity=column();identity.setGravity(Gravity.TOP|Gravity.RIGHT);identity.setPadding(0,0,dp(2),0);hero.addView(identity,new LinearLayout.LayoutParams(dp(118),-2));LinearLayout heart=column();refreshHeart(heart,anime);identity.addView(heart,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout origin=SourceBrand.badge(this,Sources.forUrl(anime.url(),source),10);origin.setOrientation(LinearLayout.HORIZONTAL);origin.setGravity(Gravity.CENTER);LinearLayout.LayoutParams badgeSize=new LinearLayout.LayoutParams(-2,-2);badgeSize.topMargin=dp(10);identity.addView(origin,badgeSize);
        if(!detail.episodes().isEmpty()){String last=store.lastEpisode(anime.url());int resume=0;for(int n=0;n<detail.episodes().size();n++)if(detail.episodes().get(n).url().equals(last))resume=n;final int selected=resume;View primary=detailButton((last.isBlank()?"Reproducir · ":"Reanudar · ")+detail.episodes().get(selected).name(),"play",()->play(detail,selected));primary.setSelected(true);TvUi.refreshFocus(primary);addAction(detailActions,primary);}
        addAction(detailActions,detailButton("Ver más",null,()->new AlertDialog.Builder(this).setTitle(anime.title()).setMessage(detail.description()).setPositiveButton("Cerrar",null).show()));new EpisodeBrowser(panel,detail).create();
        if(detail.episodes().isEmpty())panel.addView(text("La fuente no devolvió capítulos compatibles.",18));
        panel.addView(text("Saga y sugerencias",22)); LinearLayout recommendations=column();recommendations.setClipChildren(false);recommendations.setClipToPadding(false);panel.addView(recommendations);TextView waiting=text("Buscando títulos de la saga y sugerencias…",15);recommendations.addView(waiting);
        suggestionsTask=suggestions.submit(()-> {try {List<Anime> related=(Sources.forUrl(detail.anime().url(),source).id().equals(source.id())?recommendationLoader:new Recommendations(Sources.forUrl(detail.anime().url(),source))).load(detail,partial->main.post(()->{if(!valid(token))return;showRecommendations(recommendations,partial);}));main.post(()->{if(!valid(token))return;showRecommendations(recommendations,related);});}catch(Exception e){main.post(()->{if(valid(token))waiting.setText("Las sugerencias no están disponibles ahora.");});}});
        panel.addView(text("Pulsa Atrás para regresar",12));
    }
    private final class EpisodeBrowser {
        final LinearLayout panel;final Detail detail;final List<View> episodeRows=new ArrayList<>();
        boolean descending,compact=store.episodeList();int group;View anchor;LinearLayout order,mode;TvRowsView.Rail ranges;
        EpisodeBrowser(LinearLayout panel,Detail detail){this.panel=panel;this.detail=detail;}
        void create(){
            LinearLayout toolbar=row();toolbar.setTag("episode-toolbar");toolbar.setPadding(0,0,0,dp(12));toolbar.setGravity(Gravity.CENTER_VERTICAL);TextView label=text("Capítulos · "+detail.episodes().size(),20);label.setTypeface(null,android.graphics.Typeface.BOLD);toolbar.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            if(detail.episodes().isEmpty()){panel.addView(toolbar);return;}
            order=button("","ascending",()->{descending=!descending;group=descending?EpisodeWindow.groups(detail.episodes().size())-1:0;updateToolbar();renderRanges();renderEpisodes();});toolbar.addView(order);LinearLayout.LayoutParams modeSize=new LinearLayout.LayoutParams(-2,-2);modeSize.leftMargin=dp(8);
            mode=button("",compact?"thumbnail":"list",()->{compact=!compact;store.episodeList(compact);updateToolbar();renderEpisodes();});toolbar.addView(mode,modeSize);updateToolbar();panel.addView(toolbar);anchor=toolbar;
            if(detail.episodes().size()>100){ranges=new TvRowsView.Rail(MainActivity.this);panel.addView(ranges);anchor=ranges;renderRanges();}
            renderEpisodes();
        }
        void updateToolbar(){order.removeAllViews();order.addView(new TvUi.Icon(MainActivity.this,descending?"descending":"ascending"),new LinearLayout.LayoutParams(dp(22),dp(22)));order.setContentDescription(descending?"Orden descendente; cambiar a ascendente":"Orden ascendente; cambiar a descendente");mode.removeAllViews();mode.addView(new TvUi.Icon(MainActivity.this,compact?"thumbnail":"list"),new LinearLayout.LayoutParams(dp(22),dp(22)));mode.setContentDescription(compact?"Mostrar miniaturas":"Mostrar lista compacta");}
        void renderRanges(){if(ranges==null)return;ranges.model.removeAllViews();int count=EpisodeWindow.groups(detail.episodes().size());for(int n=0;n<count;n++){final int selected=descending?count-1-n:n;View button=button(EpisodeWindow.label(detail.episodes().size(),selected),null,()->{group=group==selected?-1:selected;markRange();renderEpisodes();});button.setTag(selected);button.setContentDescription("Bloque de capítulos "+EpisodeWindow.label(detail.episodes().size(),selected));button.setMinimumHeight(dp(36));button.setPadding(dp(12),dp(6),dp(12),dp(6));ranges.model.addView(button,new LinearLayout.LayoutParams(dp(92),dp(36)));}markRange();}
        void markRange(){if(ranges==null)return;for(View v:ranges.model.items){boolean selected=Objects.equals(v.getTag(),group);v.setSelected(selected);TextView caption=(TextView)((LinearLayout)v).getChildAt(0);caption.setText(EpisodeWindow.label(detail.episodes().size(),(Integer)v.getTag()));caption.setTextColor(selected?TvUi.TEXT:TvUi.MUTED);TvUi.refreshFocus(v);v.setContentDescription((selected?"Contraer bloque ":"Expandir bloque ")+EpisodeWindow.label(detail.episodes().size(),(Integer)v.getTag()));}}
        View.OnKeyListener compactNavigation(View item){return (v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN||(key!=KeyEvent.KEYCODE_DPAD_DOWN&&key!=KeyEvent.KEYCODE_DPAD_UP))return false;int target=episodeRows.indexOf(item)+(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1);if(target<0||target>=episodeRows.size())return false;View next=episodeRows.get(target);if(panel instanceof TvRowsView.RowModel&&((TvRowsView.RowModel)panel).grid.getParent() instanceof TvRowsView)((TvRowsView)((TvRowsView.RowModel)panel).grid.getParent()).restoreFocus(next);else next.requestFocus();return true;};}
        void renderEpisodes(){
            for(View old:episodeRows)panel.removeView(old);episodeRows.clear();
            int insert=panel instanceof TvRowsView.RowModel?((TvRowsView.RowModel)panel).items.indexOf(anchor)+1:panel.indexOfChild(anchor)+1;
            if(group<0)return;List<Integer> indices=EpisodeWindow.indices(detail.episodes().size(),group,descending);LinearLayout line=null;
            for(int n=0;n<indices.size();n++){final int index=indices.get(n);Episode episode=detail.episodes().get(index);if(compact){LinearLayout item=compactEpisode(episode,()->play(detail,index));item.setTag("episode-row");episodeRows.add(item);LongOk.bind(item,held->episodeMenu(detail,index,item,held),compactNavigation(item));LinearLayout.LayoutParams compactSize=new LinearLayout.LayoutParams(-1,dp(44));compactSize.bottomMargin=dp(8);panel.addView(item,insert++,compactSize);continue;}if(n%4==0){line=row();line.setTag("episode-row");episodeRows.add(line);panel.addView(line,insert++,new LinearLayout.LayoutParams(-1,-2));}long position=store.position(episode.url());FrameLayout episodeCard=(FrameLayout)landscape(episode.name(),position>0?"Continuar · "+position/60000+" min":"",episode.image().isBlank()?detail.anime().image():episode.image(),()->play(detail,index));addProgress(episodeCard,episode.url());LongOk.bind(episodeCard,held->episodeMenu(detail,index,episodeCard,held));addCard(line,episodeCard);}pad(line,indices.size(),4);
        }
    }
    private void episodeMenu(Detail detail,int index,View origin,boolean held){
        Episode episode=detail.episodes().get(index);boolean watched=store.watched(episode.url());
        LinearLayout body=column();Dialog menu=dialog(episode.name(),body);
        View toggle=button(watched?"Desmarcar como visto":"Marcar como visto",null,()->{store.watched(episode.url(),!watched);refreshProgress();menu.dismiss();});toggle.setTag("episode-toggle-watched");body.addView(toggle);
        if(index>0){View previous=button("Marcar anteriores como vistos",null,()->{store.markPreviousEpisodes(detail.episodes(),index);refreshProgress();menu.dismiss();});previous.setTag("episode-mark-previous");body.addView(previous);body.addView(text("Hasta "+detail.episodes().get(index-1).name()+". El capítulo seleccionado no se modifica.",13));}
        View cancel=button("Cancelar",null,menu::dismiss);cancel.setTag("episode-menu-cancel");body.addView(cancel);
        menu.setOnDismissListener(d->{if(detailView instanceof TvRowsView)((TvRowsView)detailView).restoreFocus(origin);else origin.requestFocus();});LongOk.consumeHeldRelease(menu,held);
    }
    private void confirmRecentEpisode(Detail detail,int index){
        new AlertDialog.Builder(this).setTitle("Último capítulo publicado").setMessage(detail.anime().title()+"\n¿Quieres ver "+detail.episodes().get(index).name()+"?")
            .setPositiveButton("Ver capítulo",(dialog,which)->play(detail,index))
            .setNegativeButton("Cerrar",null).show();
    }
    private void warmEpisode(Detail detail,String requested,int token){Source playbackSource=Sources.forUrl(detail.anime().url(),source);String preference=store.serverDefault(playbackSource.id());if(preference.equals("ask")||detail.episodes().isEmpty()||!playbackSource.supportsPreload())return;Episode episode=detail.episodes().get(0);String wanted=requested==null?store.lastEpisode(detail.anime().url()):requested;for(Episode item:detail.episodes())if(item.url().equals(wanted))episode=item;Episode target=episode;String language=store.language();if(warmTask!=null)warmTask.cancel(true);warmTask=preload.submit(()->{try{if(!valid(token))return;List<Server> options=playbackSource.servers(target);if(options.stream().map(Server::language).distinct().count()>1)return;Server choice=options.stream().filter(s->PlaybackLanguages.matches(s.language(),language)&&(preference.equals("auto")||s.name().equals(preference))).findFirst().orElse(null);if(choice!=null&&valid(token))playbackSource.resolve(choice,target.url());}catch(Exception ignored){}});}
    private void refreshHeart(LinearLayout holder,Anime anime) { holder.removeAllViews(); boolean saved=store.isFavorite(anime.url()); View heart=button("",saved?"heart-filled":"heart",()-> { if(store.isFavorite(anime.url())) { new AlertDialog.Builder(this).setTitle("Favoritos").setItems(new String[]{"Cambiar categoría","Quitar de favoritos"},(d,n)-> { if(n==0)chooseCategory(anime,holder); else { store.favorite(anime,"Por defecto",false); refreshHeart(holder,anime); } }).show(); } else chooseCategory(anime,holder); }); heart.setContentDescription(saved?"Anime guardado: gestionar favorito":"Guardar anime en favoritos"); holder.addView(heart); }
    private void chooseCategory(Anime anime,LinearLayout heart) { List<String> names=store.categories(); if(names.size()==1) { store.favorite(anime,"Por defecto",true); refreshHeart(heart,anime); return; } new AlertDialog.Builder(this).setTitle("Guardar en categoría").setItems(names.toArray(new String[0]),(d,n)-> { store.favorite(anime,names.get(n),true); refreshHeart(heart,anime); }).show(); }
    private void play(Detail detail,int index) { String episode=detail.episodes().get(index).url();if(store.position(episode)>0){new AlertDialog.Builder(this).setTitle("Continuar episodio").setMessage("Guardado en "+store.position(episode)/60000+" min").setPositiveButton("Reanudar",(d,w)->launchEpisode(detail,index)).setNegativeButton("Empezar de cero",(d,w)->{store.position(episode,0);launchEpisode(detail,index);}).setNeutralButton("Cancelar",null).show();}else launchEpisode(detail,index); }
    private void launchEpisode(Detail detail,int index) { store.lastEpisode(detail.anime().url(),detail.episodes().get(index).url());store.rememberAnime(detail.anime()); ArrayList<String> urls=new ArrayList<>(),names=new ArrayList<>(); for(Episode e:detail.episodes()) { urls.add(e.url()); names.add(e.name()); } startActivity(new Intent(this,PlayerActivity.class).putExtra("source",Sources.forUrl(detail.anime().url(),source).id()).putExtra("animeUrl",detail.anime().url()).putExtra("title",detail.anime().title()).putStringArrayListExtra("episodes",urls).putStringArrayListExtra("names",names).putExtra("index",index)); }
    private void returnCatalog() { cancelForeground();cancelSuggestions();++requestGeneration; detailView=null; header.setVisibility(View.VISIBLE); finder.setVisibility(section.equals("Catálogo")?View.VISIBLE:View.GONE); content.removeAllViews(); if(catalogView!=null) { if(catalogView.getParent()!=null)((ViewGroup)catalogView.getParent()).removeView(catalogView); content.addView(catalogView,new LinearLayout.LayoutParams(-1,0,1)); if(returnFocus!=null&&catalogView instanceof TvRowsView)((TvRowsView)catalogView).restoreFocus(returnFocus);resumeCatalogPagination();refreshLibrarySections(); } else home(); }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(event.getAction()==KeyEvent.ACTION_DOWN&&detailView==null&&section.equals("Catálogo")&&catalogView instanceof TvRowsView){
            TvRowsView rows=(TvRowsView)catalogView;View focused=root.findFocus(),first=findPoster(rows);
            if(event.getKeyCode()==KeyEvent.KEYCODE_DPAD_UP&&focused instanceof TvUi.PosterCard&&first!=null&&rows.model.indexContaining(focused)==rows.model.indexContaining(first)){search.requestFocus();return true;}
            if(event.getKeyCode()==KeyEvent.KEYCODE_DPAD_DOWN&&finder.hasFocus()){if(focused!=search&&sectionBanner!=null){View picker=sectionBanner.findViewWithTag("source-picker");if(picker!=null){if(picker.isAttachedToWindow())picker.requestFocus();else rows.restoreFocus(picker);return true;}}if(focusFirstCatalogAnime())return true;}
        }

        if(event.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT&&event.getAction()==KeyEvent.ACTION_DOWN&&detailView==null&&header!=null&&!header.hasFocus()){
            View focused=root.findFocus();boolean first=focused==search;
            for(View v=focused;v!=null&&v.getParent() instanceof View;v=(View)v.getParent())if(v instanceof TvRowsView.Rail){TvRowsView.Rail rail=(TvRowsView.Rail)v;first=rail.model.indexContaining(focused)==0;break;}
            if(!first&&catalogView instanceof TvRowsView&&focused!=null){TvRowsView rows=(TvRowsView)catalogView;int index=rows.model.indexContaining(focused);if(index>=0&&!(rows.model.items.get(index) instanceof TvRowsView.Rail))first=firstFocusable(rows.model.items.get(index))==focused;}
            if(first){focusMenu();return true;}
        }return super.dispatchKeyEvent(event);
    }
    private void focusMenu(){for(int n=1;n<header.getChildCount();n++)if(section.equals(header.getChildAt(n).getTag())){header.getChildAt(n).requestFocus();return;}}
    @Override public void onBackPressed() { if(detailView!=null)returnCatalog(); else if(section.equals("Catálogo")&&page>1)loadCatalog(page-1);else if(!header.hasFocus()){for(int n=1;n<header.getChildCount();n++)if(section.equals(header.getChildAt(n).getTag())){header.getChildAt(n).requestFocus();return;}}else super.onBackPressed(); }
    private void showFavorites(){
        favoriteState=store.favorites().toString();
        LinearLayout body=begin("Favoritos");title(body,"Favoritos","Tu colección");LinearLayout categories=shelf(body);
        List<String> names=new ArrayList<>();names.add("Todos");names.addAll(store.categories());for(String category:names){View tab=button(category,null,()->{favoriteCategory=category;showFavorites();});tab.setSelected(category.equals(favoriteCategory));TvUi.refreshFocus(tab);addAction(categories,tab);}
        addAction(categories,button("Nueva categoría","heart",()->categoryInput(null)));addAction(categories,button("Gestionar","settings",this::manageCategories));
        List<Anime> items=new ArrayList<>();JSONArray saved=store.favorites();for(int n=0;n<saved.length();n++){JSONObject f=saved.optJSONObject(n);if(favoriteCategory.equals("Todos")||f.optString("category").equals(favoriteCategory))items.add(new Anime(f.optString("title"),f.optString("url"),f.optString("image")));}
        body.addView(text(items.size()+" animes",13));displayGrid(body,items);if(items.isEmpty())body.addView(text("Guarda un anime con el corazón para verlo aquí.",18));
    }

    private void categoryInput(String oldName) { EditText field=new EditText(this); field.setSingleLine(true); if(oldName!=null)field.setText(oldName); new AlertDialog.Builder(this).setTitle(oldName==null?"Nueva categoría":"Renombrar categoría").setView(field).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,n)-> { if(oldName==null)store.addCategory(field.getText().toString()); else store.renameCategory(oldName,field.getText().toString()); favoriteCategory="Por defecto"; showFavorites(); }).show(); }
    private void manageCategories() { List<String> names=store.categories(); names.remove(0); if(names.isEmpty()) { categoryInput(null); return; } new AlertDialog.Builder(this).setTitle("Categorías").setItems(names.toArray(new String[0]),(d,n)->new AlertDialog.Builder(this).setTitle(names.get(n)).setItems(new String[]{"Renombrar","Eliminar y mover a Por defecto"},(x,k)-> { if(k==0)categoryInput(names.get(n)); else { store.deleteCategory(names.get(n)); favoriteCategory="Por defecto"; showFavorites(); } }).show()).show(); }
    private Dialog dialog(String title,LinearLayout body){return TvUi.dialog(this,title,body,false);}
    private void showFilters(){if(filtersLoading||(filtersDialog!=null&&filtersDialog.isShowing()))return;filtersLoading=true;int token=requestGeneration,ticket=++filtersGeneration;filtersTask=controls.submit(()->{try{List<Filter> available=source.filters();main.post(()->{if(!valid(token)||ticket!=filtersGeneration)return;filtersLoading=false;Map<String,String> pending=new LinkedHashMap<>(filters);LinearLayout body=column();Dialog drawer=TvUi.dialog(this,"Filtros",body,true);filtersDialog=drawer;drawer.setOnDismissListener(d->{if(filtersDialog==drawer)filtersDialog=null;});
        for(Filter f:available){int index=f.values().indexOf(pending.getOrDefault(f.key(),""));LinearLayout field=TvUi.setting(this,f.label(),index<0?"Todos":f.labels().get(index),()->{});field.setOnClickListener(v->{LinearLayout options=column();Dialog picker=dialog(f.label(),options);for(int k=0;k<f.values().size();k++){final int chosen=k;options.addView(button(f.labels().get(k),null,()->{pending.put(f.key(),f.values().get(chosen));((TextView)field.getChildAt(1)).setText(f.labels().get(chosen));picker.dismiss();}));}});body.addView(field);}
        body.addView(button("Aplicar filtros","filter",()->{filters.clear();filters.putAll(pending);drawer.dismiss();loadCatalog(1);}));body.addView(button("Limpiar","retry",()->{filters.clear();drawer.dismiss();loadCatalog(1);}));body.addView(button("Cancelar","close",drawer::dismiss));
    });}catch(Exception e){main.post(()->{if(valid(token)&&ticket==filtersGeneration){filtersLoading=false;message("No se pudieron cargar los filtros.");}});}});}
    private String settingsCategory="Reproducción";
    private LinearLayout settingsTabs;
    private void settings(){
        LinearLayout body=begin("Ajustes");title(body,"Ajustes","Personaliza la reproducción · guardado automático");LinearLayout layout=row();layout.setPadding(dp(8),dp(8),dp(8),dp(8));body.addView(layout,new LinearLayout.LayoutParams(-1,dp(370)));
        LinearLayout categories=column();settingsTabs=categories;layout.addView(categories,new LinearLayout.LayoutParams(dp(172),-1));TvRowsView options=new TvRowsView(this);LinearLayout.LayoutParams area=new LinearLayout.LayoutParams(0,-1,1);area.leftMargin=dp(20);layout.addView(options,area);
        String[] names={"Reproducción","Control remoto","Fuentes","Avanzado"},icons={"play","remote","server","settings"};
        for(int n=0;n<names.length;n++){String category=names[n];LinearLayout tab=button(category,icons[n],()->{renderSettingsCategory(category,options.model);options.focusContent();});tab.setId(View.generateViewId());tab.setTag("settings-category:"+category);tab.setGravity(Gravity.CENTER_VERTICAL);tab.setOnFocusChangeListener((v,focused)->{if(focused){settingsCategory=category;for(int i=0;i<categories.getChildCount();i++){View t=categories.getChildAt(i);t.setSelected(t==tab);TvUi.refreshFocus(t);}renderSettingsCategory(category,options.model);}TvUi.refreshFocus(v);});tab.setSelected(category.equals(settingsCategory));TvUi.refreshFocus(tab);tab.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&event.getAction()==KeyEvent.ACTION_DOWN){options.focusContent();return true;}return false;});LinearLayout.LayoutParams spacing=new LinearLayout.LayoutParams(-1,-2);spacing.bottomMargin=dp(10);categories.addView(tab,spacing);}
        options.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_DPAD_LEFT&&event.getAction()==KeyEvent.ACTION_DOWN){View tab=categories.findViewWithTag("settings-category:"+settingsCategory);if(tab!=null)tab.requestFocus();return true;}return false;});renderSettingsCategory(settingsCategory,options.model);
    }
    private LinearLayout settingWithIcon(String label,String value,String icon){LinearLayout field=TvUi.setting(this,label,value,()->{});TextView name=(TextView)field.getChildAt(0);field.removeView(name);LinearLayout caption=row();caption.setGravity(Gravity.CENTER_VERTICAL);caption.addView(new TvUi.Icon(this,icon),new LinearLayout.LayoutParams(dp(22),dp(22)));name.setPadding(dp(10),0,dp(6),0);name.setMaxLines(2);caption.addView(name,new LinearLayout.LayoutParams(0,-2,1));field.addView(caption,0,new LinearLayout.LayoutParams(0,-2,1));return field;}
    private void renderSettingsCategory(String category,LinearLayout body){
        settingsCategory=category;if(settingsTabs!=null)for(int n=0;n<settingsTabs.getChildCount();n++){View tab=settingsTabs.getChildAt(n);tab.setSelected(("settings-category:"+category).equals(tab.getTag()));TvUi.refreshFocus(tab);}body.removeAllViews();TextView heading=text(category,22);heading.setTypeface(null,android.graphics.Typeface.BOLD);body.addView(heading);
        if(category.equals("Control remoto")){String[] keys={"backward","forward","opening","timeline"},labels={"Retroceder","Adelantar","Saltar canción","Salto en la barra"},icons={"rewind","forward","opening","speed"};for(int n=0;n<keys.length;n++){String key=keys[n],label=labels[n];LinearLayout field=settingWithIcon(label,store.seconds(key)+" seg",icons[n]);field.setOnClickListener(v->secondsPicker(key,label,field));body.addView(field);}body.addView(text("Saltar canción avanza el tiempo configurado; no detecta el opening.",12));}
        else if(category.equals("Fuentes")){body.addView(text("Servidor predeterminado por fuente",13));for(String id:Sources.ids()){LinearLayout field=settingWithIcon("Servidor de "+Sources.create(id).name(),serverPreferenceLabel(store.serverDefault(id)),"server");LinearLayout caption=(LinearLayout)field.getChildAt(0);caption.removeViewAt(0);caption.addView(SourceBrand.icon(this,id,24),0);field.setOnClickListener(v->serverPreferencePicker(id,field));body.addView(field);}}
        else if(category.equals("Avanzado")){LinearLayout texture=settingWithIcon("Salida de video alternativa",store.textureSurface()?"Activada":"Desactivada","thumbnail");texture.setOnClickListener(v->{store.textureSurface(!store.textureSurface());((TextView)texture.getChildAt(1)).setText(store.textureSurface()?"Activada":"Desactivada");});body.addView(texture);body.addView(text("Si escuchas audio pero no ves imagen, cambia la salida y vuelve a abrir el episodio.",13));LinearLayout diagnostic=settingWithIcon("Informe de reproducción","Ver diagnóstico","list");diagnostic.setOnClickListener(v->{LinearLayout report=column();report.addView(text(store.diagnostics().isBlank()?"Sin datos todavía.":store.diagnostics(),13));dialog("Diagnóstico",report);});body.addView(diagnostic);}
        else {LinearLayout language=settingWithIcon("Audio preferido",PlaybackLanguages.label(store.language()),"audio");language.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Audio preferido").setItems(new String[]{"Subtitulado","Doblado","Latino","Castellano"},(d,n)->{store.language(new String[]{"SUB","DUB","LAT","CAST"}[n]);((TextView)language.getChildAt(1)).setText(PlaybackLanguages.label(store.language()));}).show());body.addView(language);LinearLayout speed=settingWithIcon("Velocidad",store.speed()+"×","speed");speed.setOnClickListener(v->{float[] speeds={.25f,.5f,.75f,1,1.25f,1.5f,1.75f,2};String[] labels=new String[speeds.length];for(int n=0;n<speeds.length;n++)labels[n]=speeds[n]+"×";new AlertDialog.Builder(this).setTitle("Velocidad de reproducción").setItems(labels,(d,n)->{store.speed(speeds[n]);((TextView)speed.getChildAt(1)).setText(store.speed()+"×");}).show();});body.addView(speed);LinearLayout clear=settingWithIcon("Limpiar Seguir viendo",store.history().length()==0?"Sin episodios":"Eliminar todos","close");clear.setTag("continue-clear-setting");clear.setOnClickListener(v->clearContinueMenu(clear));body.addView(clear);body.addView(text("En episodios con varios idiomas, primero podrás elegir el audio.",13));}
        if(settingsTabs!=null){View tab=settingsTabs.findViewWithTag("settings-category:"+category);if(tab!=null){List<View> items=body instanceof TvRowsView.RowModel?((TvRowsView.RowModel)body).items:new ArrayList<>();for(View item:items)if(item.isFocusable())item.setNextFocusLeftId(tab.getId());}}
    }
    private void sourcesInfo(){
        if(filtersTask!=null){filtersTask.cancel(true);filtersTask=null;}filtersLoading=false;filtersGeneration++;View opener=root.findFocus();LinearLayout body=column();Dialog picker=dialog("FUENTES DE ANIME",body);picker.setOnDismissListener(d->{if(opener!=null&&opener.isAttachedToWindow())opener.requestFocus();});

        for(String id:Sources.ids()){
            Source available=Sources.create(id);boolean active=source.id().equals(id);LinearLayout choice=row();choice.setGravity(Gravity.CENTER_VERTICAL);choice.setPadding(dp(10),dp(8),dp(10),dp(8));choice.setMinimumHeight(dp(66));
            ImageView icon=SourceBrand.icon(this,id,40);LinearLayout logoBox=row();logoBox.setGravity(Gravity.CENTER);logoBox.setBackground(TvUi.background(this,0xFF10131A,0));logoBox.addView(icon);choice.addView(logoBox,new LinearLayout.LayoutParams(dp(48),dp(48)));
            LinearLayout labels=column();labels.setPadding(dp(14),0,dp(10),0);TextView name=text(available.name(),18);name.setTypeface(null,android.graphics.Typeface.BOLD);labels.addView(name);labels.addView(TvUi.text(this,id.equals("latanime")?"Latino y castellano":id.equals("tioanime")?"Subtitulado":"Subtitulado y doblado",12,TvUi.MUTED));choice.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
            TextView selected=TvUi.text(this,active?"✓":"",21,TvUi.ACCENT);choice.addView(selected,new LinearLayout.LayoutParams(dp(26),-2));TvUi.focus(choice);choice.setFocusableInTouchMode(true);choice.setSelected(active);TvUi.refreshFocus(choice);choice.setContentDescription(available.name()+(active?", fuente activa":""));choice.setTag("source-choice:"+id);
            choice.setOnClickListener(v->{picker.dismiss();if(!active){store.source(id);getIntent().putExtra("return-catalog",section.equals("Catálogo"));recreate();}});LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-1,-2);gap.bottomMargin=dp(5);body.addView(choice,gap);
        }
        LinearLayout cancel=button("Cancelar","close",picker::dismiss);cancel.setGravity(Gravity.CENTER);cancel.setFocusableInTouchMode(true);cancel.setTag("source-cancel");cancel.setId(View.generateViewId());
        for(int n=0;n<body.getChildCount();n++)body.getChildAt(n).setId(View.generateViewId());
        for(int n=0;n<body.getChildCount();n++){View choice=body.getChildAt(n);choice.setNextFocusUpId((n==0?choice:body.getChildAt(n-1)).getId());choice.setNextFocusDownId((n+1<body.getChildCount()?body.getChildAt(n+1):cancel).getId());choice.setNextFocusLeftId(choice.getId());choice.setNextFocusRightId(choice.getId());}
        cancel.setNextFocusUpId(body.getChildAt(body.getChildCount()-1).getId());cancel.setNextFocusDownId(cancel.getId());
        // ScrollView's internal focus search cannot reach a footer outside its subtree.
        picker.setOnKeyListener((dialog,key,event)->{
            if(event.getAction()!=KeyEvent.ACTION_DOWN||(key!=KeyEvent.KEYCODE_DPAD_UP&&key!=KeyEvent.KEYCODE_DPAD_DOWN))return false;
            View focused=picker.getCurrentFocus();int index=focused==cancel?body.getChildCount():-1;for(int n=0;n<body.getChildCount();n++)if(focused==body.getChildAt(n))index=n;
            if(index<0)return false;int next=Math.max(0,Math.min(body.getChildCount(),index+(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1)));(next==body.getChildCount()?cancel:body.getChildAt(next)).requestFocus();return true;
        });
        LinearLayout panel=(LinearLayout)((ViewGroup)picker.getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0);GradientDrawable rounded=TvUi.background(this,0xFF252831,0);rounded.setCornerRadius(dp(28));panel.setBackground(rounded);LinearLayout.LayoutParams cancelSize=new LinearLayout.LayoutParams(-1,-2);cancelSize.topMargin=dp(8);panel.addView(cancel,cancelSize);TextView heading=(TextView)panel.getChildAt(0);panel.removeView(heading);LinearLayout headingBox=column();headingBox.setGravity(Gravity.CENTER);TvUi.Icon puzzle=new TvUi.Icon(this,"puzzle");headingBox.addView(puzzle,new LinearLayout.LayoutParams(dp(28),dp(28)));heading.setGravity(Gravity.CENTER);heading.setTextSize(16);heading.setPadding(0,dp(10),0,dp(18));headingBox.addView(heading);panel.addView(headingBox,0);picker.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);picker.getWindow().setDimAmount(.7f);panel.post(()->{picker.getWindow().setLayout(Math.min(dp(480),getResources().getDisplayMetrics().widthPixels-dp(40)),Math.min(body.getMeasuredHeight()+dp(160),getResources().getDisplayMetrics().heightPixels-dp(40)));View active=body.findViewWithTag("source-choice:"+source.id());if(active!=null)active.requestFocus();});
    }
    private String serverPreferenceLabel(String selected){return selected.equals("auto")?"Automático":selected.equals("ask")?"Elegir al reproducir":selected;}
    private void serverPreferencePicker(String id,LinearLayout field){
        LinearLayout options=column();Dialog picker=TvUi.dialog(this,"Servidor de "+Sources.create(id).name(),options,true);
        List<String> values=new ArrayList<>(List.of("auto","ask"));values.addAll(Sources.servers(id));
        for(String value:values){String caption=value.equals("auto")||value.equals("ask")?serverPreferenceLabel(value):PlayerActivity.serverCaption(new Server(value,"","SUB"),id);options.addView(button(caption,null,()->{store.serverDefault(id,value);((TextView)field.getChildAt(1)).setText(serverPreferenceLabel(value));picker.dismiss();}));}
        options.addView(button("Cerrar","close",picker::dismiss));
    }
    private void secondsPicker(String key,String label,LinearLayout field){
        LinearLayout body=column();Dialog picker=dialog(label,body);final int[] value={store.seconds(key)};TextView display=text(value[0]+" seg",25);display.setGravity(Gravity.CENTER);body.addView(display);LinearLayout actions=row();body.addView(actions);addAction(actions,button("− 5",null,()->{value[0]=Math.max(1,value[0]-5);display.setText(value[0]+" seg");}));addAction(actions,button("+ 5",null,()->{value[0]=Math.min(600,value[0]+5);display.setText(value[0]+" seg");}));
        LinearLayout precise=row();body.addView(precise);addAction(precise,button("− 1",null,()->{value[0]=Math.max(1,value[0]-1);display.setText(value[0]+" seg");}));addAction(precise,button("+ 1",null,()->{value[0]=Math.min(600,value[0]+1);display.setText(value[0]+" seg");}));addAction(actions,button("Guardar",null,()->{store.seconds(key,value[0]);((TextView)field.getChildAt(1)).setText(value[0]+" seg");picker.dismiss();}));body.addView(button("Cancelar","close",picker::dismiss));
    }

    private void message(String value) { Toast.makeText(this,value,Toast.LENGTH_LONG).show(); }
    private void applySafeArea(){root.setPadding(0,0,0,0);if(header!=null){header.setPadding(dp(32),dp(8),dp(8),0);menuExpanded(header.hasFocus());}}
    private int dp(int value) { return TvUi.dp(this,value); }
    private LinearLayout column() { LinearLayout view=new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private LinearLayout row() { LinearLayout view=new LinearLayout(this); view.setGravity(Gravity.TOP); return view; }
    private TextView text(String value,int size) { TextView view=TvUi.text(this,value,size,TvUi.TEXT); view.setPadding(dp(4),dp(5),dp(4),dp(5)); return view; }
    private LinearLayout.LayoutParams searchActionSize(){LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(46),dp(46));size.leftMargin=dp(8);return size;}
    private LinearLayout catalogAction(String label,String icon,Runnable action){LinearLayout button=button("",icon,action);button.setContentDescription(label);button.setFocusableInTouchMode(true);button.setMinimumHeight(dp(46));button.setMinimumWidth(dp(46));return button;}
    private LinearLayout detailButton(String value,String icon,Runnable action){LinearLayout view=button(value,icon,action);view.setMinimumHeight(dp(36));view.setPadding(dp(10),dp(6),dp(10),dp(6));return view;}
    private LinearLayout button(String value,String icon,Runnable action) { return TvUi.button(this,value,icon,action); }
    private void addAction(LinearLayout parent,View child) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2); p.setMargins(dp(4),dp(4),dp(4),dp(4)); parent.addView(child,p); }
    @Override protected void onPause(){voiceActive=false;if(speech!=null)speech.cancel();super.onPause();}
    @Override protected void onDestroy() { ProtectedPages.detach(this); ++requestGeneration; cancelForeground();cancelSuggestions();suggestions.shutdownNow();controls.shutdownNow();preload.shutdownNow();io.shutdownNow();for(Future<?> task:artworkTasks.values())task.cancel(true);artworkTasks.clear();pendingImages.clear();artworkKeys.clear();art.shutdownNow();metadata.shutdownNow();if(speech!=null)speech.destroy(); main.removeCallbacksAndMessages(null); super.onDestroy(); }
}









