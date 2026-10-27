package pe.animetv;
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FfmpegLibrary;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import java.util.*;
import java.util.concurrent.*;
import static pe.animetv.Models.*;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class PlayerActivity extends Activity {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newFixedThreadPool(3);
    private Future<?> extractionTask;
    private void cancelExtraction(){if(extractionTask!=null){extractionTask.cancel(true);extractionTask=null;}}
    private Source source;
    protected Source createSource() { return Sources.forUrl(getIntent().getStringExtra("animeUrl"),Sources.create(getIntent().getStringExtra("source"))); }
    private final List<Server> attempted=new ArrayList<>();
    private List<Server> servers=List.of();
    private Store store;
    private ExoPlayer player;
    private PlayerView surface;
    private LinearLayout controls,actions;
    private TextView status,clock,totalClock,episodeHeading,episodeNumber;
    private final Runnable hideControls=()->showControls(false);
    private String episodeLanguage="";
    private SeekBar seek;
    private FrameLayout root;
    private ArrayList<String> episodes,names;
    private int index,generation=0;
    private final SeekBurst seekBurst=new SeekBurst();
    private long requestedPosition=0,startedAt;
    private boolean suggested=false,firstFrame=false,controlsVisible=false,foreground=true;
    private Server activeServer;
    private Server returningServer;
    private Stream activeStream;
    private long resolvedAt;
    private boolean returningPaused;
    private LinearLayout playButton;
    private TextView actionHint;
    private final Set<Server> retriedServers=new HashSet<>();
    private boolean selectedVideo=false; private String lastFailure="";
    private final Runnable timeout=()-> { if(foreground && player!=null && player.getPlayWhenReady()) failed((activeServer==null?"El servidor":activeServer.name())+": se agotó el tiempo de carga del video."); };
    private final Runnable videoTimeout=()-> {
        if(player!=null && foreground && PlaybackHealth.awaitingVideo(firstFrame,!player.getPlayWhenReady(),true)) {
            store.diagnostic("Sin fotograma. videoSeleccionado="+selectedVideo+" estado="+player.getPlaybackState());
            failed((activeServer==null?"El servidor":activeServer.name())+": no llegó una imagen. Puede ser un fallo de carga o un formato incompatible.");
        }
    };
    private final Runnable ticker=new Runnable() {
        @Override public void run() {
            if(player!=null) {
                long duration=player.getDuration(),position=player.getCurrentPosition();
                if(firstFrame && !player.getCurrentTimeline().isEmpty()) store.progress(episodes.get(index),position,duration);
                updateTimeline();
            }
            main.postDelayed(this,1000);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); ProtectedPages.attach(this); store=new Store(this);source=createSource();
        episodes=getIntent().getStringArrayListExtra("episodes"); names=getIntent().getStringArrayListExtra("names"); index=state==null?getIntent().getIntExtra("index",0):state.getInt("index",0);
        if(episodes==null || names==null || index<0 || index>=episodes.size()) { finish(); return; }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        root=new FrameLayout(this); root.setBackgroundColor(Color.BLACK); root.setFocusable(true); setContentView(root);
        surface=(PlayerView)getLayoutInflater().inflate(store.textureSurface()?R.layout.video_texture:R.layout.video_surface,root,false);
        surface.setUseController(false); surface.setShutterBackgroundColor(Color.TRANSPARENT); surface.setFocusable(false); surface.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS); root.addView(surface,new FrameLayout.LayoutParams(-1,-1));
        store.diagnostic("Inicio Android="+Build.VERSION.SDK_INT+" salida="+(store.textureSurface()?"TextureView":"SurfaceView"));
        status=new TextView(this); status.setTextColor(Color.WHITE); status.setTextSize(19); status.setPadding(24,16,24,16); status.setBackgroundColor(0xB0101524); status.setMaxLines(3); FrameLayout.LayoutParams notice=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL); notice.topMargin=dp(27); notice.leftMargin=dp(48); notice.rightMargin=dp(48); root.addView(status,notice);
        controls=new LinearLayout(this); controls.setOrientation(LinearLayout.VERTICAL); controls.setPadding(dp(32),dp(8),dp(32),dp(12)); controls.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0xE6000000})); FrameLayout.LayoutParams panel=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM); root.addView(controls,panel);
        episodeNumber=TvUi.text(this,"",18,TvUi.TEXT);episodeNumber.setSingleLine(true);episodeNumber.setEllipsize(android.text.TextUtils.TruncateAt.END);episodeNumber.setPadding(dp(10),dp(6),dp(10),dp(6));episodeNumber.setBackground(TvUi.background(this,0x99000000,0));FrameLayout.LayoutParams numberPosition=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT);numberPosition.setMargins(dp(32),dp(18),dp(32),0);root.addView(episodeNumber,numberPosition);
        episodeHeading=TvUi.text(this,"",20,TvUi.TEXT);episodeHeading.setSingleLine(true);episodeHeading.setEllipsize(android.text.TextUtils.TruncateAt.END);controls.addView(episodeHeading);updateEpisodeHeading();
        LinearLayout times=new LinearLayout(this);times.setGravity(Gravity.CENTER_VERTICAL);clock=TvUi.text(this,"0:00",12,TvUi.MUTED);totalClock=TvUi.text(this,"--:--",12,TvUi.MUTED);totalClock.setGravity(Gravity.RIGHT);times.addView(clock,new LinearLayout.LayoutParams(0,-2,1));times.addView(totalClock,new LinearLayout.LayoutParams(-2,-2));controls.addView(times);
        seek=new SeekBar(this); seek.setMax(1000);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(TvUi.ACCENT));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(TvUi.ACCENT)); controls.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
            public void onProgressChanged(SeekBar s,int value,boolean user) { if(user && player!=null && player.getDuration()>0 && player.isCurrentMediaItemSeekable()) player.seekTo(player.getDuration()*value/1000); }
        });
        actions=new LinearLayout(this); actions.setGravity(Gravity.CENTER); controls.addView(actions,new LinearLayout.LayoutParams(-1,-2)); actionHint=TvUi.text(this,"",13,TvUi.MUTED); actionHint.setGravity(Gravity.CENTER); actionHint.setVisibility(View.GONE);controls.addView(actionHint); showControls(false);
        requestedPosition=store.position(episodes.get(index)); rebuildActions(); loadEpisode(); main.post(ticker);
    }
    private void updateEpisodeHeading(){
        if(episodeHeading!=null)episodeHeading.setText(getIntent().getStringExtra("title"));
        if(episodeNumber!=null){String name=names.get(index);episodeNumber.setText(name.matches("\\d+(?:\\.\\d+)?")?"Capítulo "+name:name);}
    }
    private void loadEpisode() {
        cancelExtraction();
        updateEpisodeHeading();
        int token=++generation; startedAt=SystemClock.elapsedRealtime(); suggested=false; lastFailure=""; attempted.clear();retriedServers.clear(); activeServer=null; activeStream=null; servers=List.of(); firstFrame=false;
        releasePlayer(); notice("Buscando servidores…");
        Episode episode=new Episode(names.get(index),episodes.get(index));
        extractionTask=io.submit(()-> { try { List<Server> result=source.servers(episode);
            main.post(()-> { if(token!=generation || !foreground) return; servers=result.stream().filter(s->!blockedVideo(s,episode.url())).toList();
                if(servers.isEmpty()) { error("La fuente no devolvió servidores."); return; }
                if(returningServer!=null){Server previous=returningServer;returningServer=null;Server match=servers.stream().filter(s->s.name().equals(previous.name())&&s.language().equals(previous.language())).findFirst().orElse(null);if(match!=null){selectServer(match);return;}}
                chooseLanguage(this::applyServerPreference);

            });
        } catch(Exception e) { main.post(()-> { if(token==generation) error("No se pudieron obtener servidores: "+e.getMessage()); }); } });
    }
    private void nextCandidate() {
        cancelExtraction();
        releasePlayer();
        List<Server> remaining=new ArrayList<>(); for(Server s:servers) if(!attempted.contains(s)) remaining.add(s);
        String language=episodeLanguage.isBlank()?store.language():episodeLanguage; if(!episodeLanguage.isBlank())remaining.removeIf(s->!s.language().equals(language));else if(servers.stream().anyMatch(s->PlaybackLanguages.matches(s.language(),language)))remaining.removeIf(s->!PlaybackLanguages.matches(s.language(),language));
        remaining.sort(Comparator.comparingInt(s->PlaybackPolicy.serverRank(s.name())));
        if(remaining.isEmpty()) { error("No se pudo reproducir con las alternativas de "+PlaybackLanguages.label(store.language()).toLowerCase(Locale.ROOT)+"."+(lastFailure.isBlank()?"":"\n"+lastFailure)); return; }
        int token=++generation; String episodeUrl=episodes.get(index); notice(attempted.isEmpty()?"Preparando video…":"Probando otro servidor…");
        Server candidate=remaining.get(0);attempted.add(candidate);activeServer=candidate;activeStream=null;
        extractionTask=io.submit(()-> {try {Stream stream=source.resolve(candidate,episodeUrl);main.post(()-> {if(token==generation&&foreground)startStream(candidate,stream);});}catch(Exception e){main.post(()-> {if(token!=generation||!foreground)return;lastFailure=candidate.name()+": "+readable(e);store.diagnostic(lastFailure);nextCandidate();});}});
    }
    private List<String> episodeLanguages(){return List.of("SUB","LAT","CAST","DUB").stream().filter(language->servers.stream().anyMatch(s->s.language().equals(language))).toList();}
    private void chooseLanguage(Runnable next){
        List<String> languages=episodeLanguages();if(languages.isEmpty()){error("No hay idiomas disponibles para este episodio.");return;}if(languages.size()==1){episodeLanguage=languages.get(0);next.run();return;}
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.addView(TvUi.text(this,getIntent().getStringExtra("title")+" · "+names.get(index),13,TvUi.MUTED));Dialog dialog=TvUi.dialog(this,"Elegir idioma",body,false);
        for(String language:languages){LinearLayout option=TvUi.setting(this,PlaybackLanguages.label(language),PlaybackLanguages.matches(language,store.language())?"Preferido":"",()->{episodeLanguage=language;store.language(language);dialog.dismiss();next.run();});option.setTag("episode-language:"+language);body.addView(option);}body.addView(TvUi.button(this,"Cerrar","close",()->{dialog.dismiss();showControls(true);}));dialog.setOnCancelListener(d->showControls(true));
    }
    private void applyServerPreference(){String preference=store.serverDefault(source.id());if(preference.equals("ask"))showServerOptions();else if(preference.equals("auto"))nextCandidate();else {Server preferred=servers.stream().filter(s->s.name().equals(preference)&&s.language().equals(episodeLanguage)).findFirst().orElse(null);if(preferred==null){notice(preference+" no está disponible en "+PlaybackLanguages.label(episodeLanguage)+". Elige otro servidor.");showServerOptions();}else selectServer(preferred);}}
    private void chooseServer(){if(servers.isEmpty()){loadEpisode();return;}chooseLanguage(this::showServerOptions);}
    private void showServerOptions(){
        android.app.Dialog dialog=new android.app.Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(16),dp(20),dp(16));box.setBackground(TvUi.background(this,TvUi.BG,0));
        box.addView(TvUi.text(this,"Elegir servidor · "+PlaybackLanguages.label(episodeLanguage),23,TvUi.TEXT));box.addView(TvUi.text(this,getIntent().getStringExtra("title")+" · "+names.get(index),13,TvUi.MUTED));ScrollView scroll=new TvScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        List<Server> options=servers.stream().filter(s->s.language().equals(episodeLanguage)).sorted(Comparator.comparingInt(s->PlaybackPolicy.serverRank(s.name()))).toList();for(Server server:options){View option=TvUi.setting(this,serverCaption(server,source.id()),"",()->{dialog.dismiss();selectServer(server);});option.setTag("episode-server:"+server.name());LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(-1,-2);size.bottomMargin=dp(6);list.addView(option,size);}
        if(episodeLanguages().size()>1)box.addView(TvUi.button(this,"Cambiar idioma","audio",()->{dialog.dismiss();chooseServer();}));dialog.setOnCancelListener(d->{if(episodeLanguages().size()>1)chooseServer();else showControls(true);});box.addView(TvUi.button(this,"Cerrar","close",()->{dialog.dismiss();if(player==null)notice("Selecciona un servidor para reproducir.");showControls(true);}));dialog.setContentView(box);dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.show();dialog.getWindow().setLayout(Math.min(dp(460),getResources().getDisplayMetrics().widthPixels-dp(96)),Math.min(dp(350),getResources().getDisplayMetrics().heightPixels-dp(40)));
    }
    private void selectServer(Server selected) {
        cancelExtraction();
        rememberPosition();releasePlayer();activeServer=selected;activeStream=null;int token=++generation;String url=episodes.get(index);notice("Preparando "+selected.name()+"…");
        extractionTask=io.submit(()-> { try { Stream stream=source.resolve(selected,url);main.post(()-> {if(token==generation&&foreground)startStream(selected,stream);}); }catch(Exception e){main.post(()-> {if(token==generation&&foreground){store.diagnostic("Extracción "+selected.name()+": "+e.getClass().getSimpleName()+" · "+readable(e));error(selected.name()+": "+readable(e));}});} });
    }
    static String serverCaption(Server server){return server.name()+switch(server.name().toLowerCase(Locale.ROOT)){case "voe","yourupload"->" - 720p";case "byse","mp4upload","hls"->" - 1080p";default->"";};}
    static String serverCaption(Server server,String source){return (source.equals("latanime")||source.equals("jkanime"))?server.name():serverCaption(server);}
    private long startupTimeout(){if(activeServer==null)return 12000;return switch(activeServer.name()){case "MP4Upload","HLS","Desu","Magi"->45000;case "Mega"->25000;case "Byse"->20000;default->12000;};}
    static String incompatibleVideo(Tracks tracks,String server){boolean video=false,supported=false;String codec="";for(Tracks.Group group:tracks.getGroups())if(group.getType()==C.TRACK_TYPE_VIDEO){video=true;supported|=group.isSupported();for(int n=0;n<group.length;n++)if(MimeTypes.VIDEO_AV1.equals(group.getTrackFormat(n).sampleMimeType))codec="AV1";}return video&&!supported?server+": la TV no ofrece un decodificador compatible con el video"+(codec.isBlank()?"":" "+codec)+" de este archivo.":"";}
    private boolean blockedVideo(Server server,String episode){return new VideoExclusions(this).blocked(server,episode,System.currentTimeMillis());}
    private void blockVideo(Server server,String episode){new VideoExclusions(this).block(server,episode,System.currentTimeMillis());servers=servers.stream().filter(s->!s.equals(server)).toList();store.diagnostic("AV1 sin hardware: alternativa ocultada para este episodio");}
    private String readable(Exception e) { if(e instanceof javax.net.ssl.SSLException)return "No se pudo validar el certificado del servidor. Comprueba fecha y hora de la TV.";return e.getMessage()==null?"Fallo de conexión con el servidor":e.getMessage(); }
    private void startStream(Server server,Stream stream) {
        releasePlayer(); surface.setVisibility(View.VISIBLE); activeServer=server; activeStream=stream; resolvedAt=SystemClock.elapsedRealtime(); firstFrame=false;
        OkHttpDataSource.Factory http=PlaybackDataSources.upstream(server,stream);
        PlaybackBufferPolicy buffer=PlaybackBufferPolicy.forStream(server.name(),stream.mimeType());
        DefaultLoadControl loadControl=new DefaultLoadControl.Builder().setBufferDurationsMs(buffer.minimum(),buffer.maximum(),buffer.start(),buffer.rebuffer()).setBackBuffer(buffer.backBuffer(),true).setTargetBufferBytes(buffer.bytes()).setPrioritizeTimeOverSizeThresholds(false).build();
        DefaultRenderersFactory renderers=CompatibleRenderersFactory.forServer(this,server.name());
        androidx.media3.exoplayer.trackselection.DefaultTrackSelector selector=new androidx.media3.exoplayer.trackselection.DefaultTrackSelector(this);
        selector.setParameters(selector.buildUponParameters().setExceedRendererCapabilitiesIfNecessary(false).setMaxVideoSize(1920,1080).setPreferredVideoMimeTypes(MimeTypes.VIDEO_H264));
        androidx.media3.datasource.DataSource.Factory upstream=stream.megaKey().isEmpty()?http:()->new MegaDataSource(http.createDataSource(),stream.megaKey());
        player=new ExoPlayer.Builder(this,renderers).setTrackSelector(selector).setLoadControl(loadControl).setMediaSourceFactory(new DefaultMediaSourceFactory(PlaybackDataSources.cached(this,upstream)).setLoadErrorHandlingPolicy(new androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy(4))).build();
        final ExoPlayer current=player; final int streamGeneration=++generation; selectedVideo=false;
        player.setPlaybackSpeed(store.speed());
        store.diagnostic("Servidor="+server.name()+" idioma="+server.language());
        if(CompatibleRenderersFactory.needsSoftwareFallback(server.name()))store.diagnostic(server.name()+" ABI="+Arrays.toString(Build.SUPPORTED_ABIS)+" AV1 software="+FfmpegLibrary.supportsFormat(MimeTypes.VIDEO_AV1));
        surface.setPlayer(current); current.addListener(new Player.Listener() {
            private boolean active() { return foreground && player==current && generation==streamGeneration; }
            @Override public void onRenderedFirstFrame() { if(!active())return; firstFrame=true; main.removeCallbacks(videoTimeout); store.preferredHost(server.name()); long latency=SystemClock.elapsedRealtime()-startedAt; store.diagnostic("Primer fotograma ms="+latency); android.util.Log.i("AnimeTV","first_frame_ms="+latency+" server="+server.name()); notice(server.name()+" · "+server.language()); hideNoticeLater(); }
            @Override public void onPlayerError(PlaybackException e) { if(!active())return; store.diagnostic("Error="+e.getErrorCodeName()); main.post(()-> { if(active())failed("Error de reproducción: "+e.getErrorCodeName(),PlaybackRecovery.refreshFor(e.errorCode)); }); }
            @Override public void onPlaybackStateChanged(int state) {
                if(!active())return;
                main.removeCallbacks(timeout);
                if(state==Player.STATE_BUFFERING && player!=null && player.getPlayWhenReady()) main.postDelayed(timeout,firstFrame?25000:startupTimeout());
                if(state==Player.STATE_READY) { if(!firstFrame)notice("Preparando imagen…"); else hideNoticeLater(); }
                if(state==Player.STATE_ENDED) { store.progress(episodes.get(index),player.getDuration(),player.getDuration()); suggestNext(); }
            }
            @Override public void onTracksChanged(Tracks tracks) {
                if(!active())return; boolean hasVideo=false,supported=false; selectedVideo=false;
                if(VideoCompatibility.unsupportedAv1(tracks,format->VideoCompatibility.hardware(PlayerActivity.this,format))){blockVideo(server,episodes.get(index));main.post(()->{if(active())failed("Este archivo usa AV1 y la TV no tiene un decodificador por hardware compatible. Se ocultó esta alternativa para este episodio.",false);});return;}
                String incompatible=incompatibleVideo(tracks,server.name());if(!incompatible.isBlank()){store.diagnostic(incompatible);main.post(()->{if(active())failed(incompatible,false);});return;}
                for(Tracks.Group group:tracks.getGroups()) if(group.getType()==C.TRACK_TYPE_VIDEO) {
                    hasVideo=true; supported|=group.isSupported(); selectedVideo|=group.isSelected();
                    for(int n=0;n<group.length;n++) if(group.isTrackSelected(n))store.diagnostic("Video="+group.getTrackFormat(n).sampleMimeType+" "+group.getTrackFormat(n).width+"x"+group.getTrackFormat(n).height);
                }
                if(current.getPlaybackState()==Player.STATE_READY && !tracks.getGroups().isEmpty() && (!hasVideo || !supported || !selectedVideo)) {
                    store.diagnostic("Video no compatible: presente="+hasVideo+" soportado="+supported+" seleccionado="+selectedVideo);
                    main.post(()-> { if(active())failed("Esta alternativa no ofrece video compatible con la TV.",false); }); return;
                }
                rebuildActions();
            }
        });
        player.addAnalyticsListener(new androidx.media3.exoplayer.analytics.AnalyticsListener(){
            @Override public void onVideoDecoderInitialized(EventTime time,String name,long initialized,long duration){store.diagnostic("Decodificador="+name+" inicio ms="+duration);}
            @Override public void onDroppedVideoFrames(EventTime time,int count,long elapsed){if(count>=20)store.diagnostic("Fotogramas descartados="+count+" intervalo ms="+elapsed);}
            @Override public void onLoadCompleted(EventTime time,androidx.media3.exoplayer.source.LoadEventInfo info,androidx.media3.exoplayer.source.MediaLoadData data){if(info.loadDurationMs>=3000&&data.dataType==C.DATA_TYPE_MEDIA)store.diagnostic("Segmento bytes="+info.bytesLoaded+" ms="+info.loadDurationMs);}
        });
        player.setMediaItem(new MediaItem.Builder().setUri(stream.url()).setMimeType(stream.mimeType()).build()); if(requestedPosition>0) player.seekTo(requestedPosition); player.prepare(); if(returningPaused)player.pause();else player.play();returningPaused=false;
        main.postDelayed(timeout,startupTimeout()); main.postDelayed(videoTimeout,startupTimeout()+3000); rebuildActions();
    }
    private void failed(String reason) { failed(reason,true); }
    private void failed(String reason,boolean refresh) {
        if(refresh && activeServer!=null)source.invalidate(activeServer,episodes.get(index));
        lastFailure=reason;store.diagnostic(reason); rememberPosition(); main.removeCallbacks(timeout);
        
        if(refresh&&activeServer!=null&&retriedServers.add(activeServer)){Server retry=activeServer;selectServer(retry);return;}
        if(store.serverDefault(source.id()).equals("auto")) nextCandidate(); else { releasePlayer(); error(reason); }
    }
    private void error(String reason) {
        notice(reason); rebuildActions(); showControls(true);
    }
    private void rememberPosition() { if(player!=null && firstFrame && !player.getCurrentTimeline().isEmpty()) { requestedPosition=player.getCurrentPosition(); store.progress(episodes.get(index),requestedPosition,player.getDuration()); } }
    private void rebuildActions() {
        Object tag=getCurrentFocus()==null?null:getCurrentFocus().getTag();actions.removeAllViews();
        View spacer=new View(this);actions.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        add("Retroceder "+store.seconds("backward")+" segundos","rewind",()->jump(-store.seconds("backward"),false));
        playButton=add("Pausar / Reanudar",player!=null&&player.getPlayWhenReady()?"pause":"play",this::toggle);playButton.getLayoutParams().width=dp(60);playButton.getLayoutParams().height=dp(60);
        add("Adelantar "+store.seconds("forward")+" segundos","forward",()->jump(store.seconds("forward"),false));
        LinearLayout tools=new LinearLayout(this);tools.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);actions.addView(tools,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout song=TvUi.button(this,"+"+store.seconds("opening")+" seg",null,()->jump(store.seconds("opening"),false));song.setContentDescription("Saltar canción, +"+store.seconds("opening")+" segundos");tools.addView(song);song.setTag("song");
        LinearLayout episodesButton=TvUi.button(this,"Episodios",null,this::chooseEpisode);LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-2,-2);gap.leftMargin=dp(10);tools.addView(episodesButton,gap);episodesButton.setTag("Episodios");
        LinearLayout speedButton=add("Velocidad · "+store.speed()+"×","speed",this::chooseSpeed);actions.removeView(speedButton);tools.addView(speedButton);
        LinearLayout settingsButton=add("Calidad y opciones","settings",this::playerOptions);actions.removeView(settingsButton);tools.addView(settingsButton);
        if(controlsVisible){View restore=tag==null?null:actions.findViewWithTag(tag);if(restore!=null&&restore.isFocusable())restore.requestFocus();else playButton.requestFocus();}
    }
    private void chooseEpisode(){new AlertDialog.Builder(this).setTitle("Episodios").setSingleChoiceItems(names.toArray(new String[0]),index,(dialog,n)->{dialog.dismiss();if(n!=index){if(store.position(episodes.get(n))>0)new AlertDialog.Builder(this).setTitle("Continuar episodio").setPositiveButton("Reanudar",(d,w)->changeEpisode(n)).setNegativeButton("Empezar de cero",(d,w)->{store.position(episodes.get(n),0);changeEpisode(n);}).setNeutralButton("Cancelar",null).show();else changeEpisode(n);}}).show();}
    private void chooseSpeed(){float[] speeds={.25f,.5f,.75f,1f,1.25f,1.5f,1.75f,2f};String[] labels=new String[speeds.length];int selected=3;for(int n=0;n<speeds.length;n++){labels[n]=speeds[n]==1?"Normal · 1×":speeds[n]+"×";if(speeds[n]==store.speed())selected=n;}new AlertDialog.Builder(this).setTitle("Velocidad de reproducción").setSingleChoiceItems(labels,selected,(dialog,n)->{store.speed(speeds[n]);if(player!=null)player.setPlaybackSpeed(speeds[n]);dialog.dismiss();rebuildActions();showControls(true);}).show();}
    private void playerOptions() {
        List<String> labels=new ArrayList<>();List<Runnable> commands=new ArrayList<>();
        labels.add("Servidor · "+(activeServer==null?"Elegir":activeServer.name()));commands.add(this::chooseServer);
        if(player!=null)for(int type:new int[]{C.TRACK_TYPE_VIDEO,C.TRACK_TYPE_AUDIO,C.TRACK_TYPE_TEXT}){int count=0,height=0;for(Tracks.Group g:player.getCurrentTracks().getGroups())if(g.getType()==type)for(int n=0;n<g.length;n++)if(g.isTrackSupported(n)){count++;if(g.isTrackSelected(n))height=g.getTrackFormat(n).height;}
            if(type==C.TRACK_TYPE_VIDEO)count=VideoQualities.from(player.getCurrentTracks()).size();if(count>0&&(type==C.TRACK_TYPE_VIDEO||count>1||type==C.TRACK_TYPE_TEXT)){final int trackType=type;String name=type==C.TRACK_TYPE_VIDEO?"Calidad":type==C.TRACK_TYPE_AUDIO?"Audio":"Subtítulos";labels.add(name+(type==C.TRACK_TYPE_VIDEO&&height>0?" · "+height+"p":"")+(type==C.TRACK_TYPE_VIDEO&&count==1?" · única disponible":""));commands.add(()->chooseTrack(trackType,name));}}
        labels.add("Reintentar reproducción");commands.add(()->{rememberPosition();loadEpisode();});labels.add("Cerrar controles");commands.add(()->showControls(false));
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);android.app.Dialog drawer=TvUi.dialog(this,"Reproducción",body,true);for(int n=0;n<labels.size();n++){final int chosen=n;String label=labels.get(n);int split=label.indexOf(" · ");body.addView(TvUi.setting(this,split<0?label:label.substring(0,split),split<0?"":label.substring(split+3),()->{drawer.dismiss();commands.get(chosen).run();}));}body.addView(TvUi.button(this,"Volver","close",drawer::dismiss));
    }
    private void chooseTrack(int type,String title) {
        if(player==null)return; List<String> labels=new ArrayList<>(); List<TrackSelectionOverride> choices=new ArrayList<>();
        if(type==C.TRACK_TYPE_VIDEO){List<VideoQualities.Choice> qualities=VideoQualities.from(player.getCurrentTracks());if(qualities.isEmpty())return;if(qualities.size()>1){labels.add("Automático");choices.add(null);}for(VideoQualities.Choice quality:qualities){labels.add(quality.label());choices.add(quality.override());}if(qualities.size()==1)title="Calidad · única disponible en este servidor";}
        else { labels.add(type==C.TRACK_TYPE_TEXT?"Desactivar":"Automático"); choices.add(null);
        for(Tracks.Group g:player.getCurrentTracks().getGroups()) if(g.getType()==type) for(int n=0;n<g.length;n++) if(g.isTrackSupported(n)) {
            Format f=g.getTrackFormat(n); String label=type==C.TRACK_TYPE_VIDEO?(f.height>0?f.height+"p":"Video "+(n+1)):(f.label!=null?f.label:(f.language!=null?f.language:"Pista "+(n+1)));
            labels.add(label); choices.add(new TrackSelectionOverride(g.getMediaTrackGroup(),n));
        }}
        new AlertDialog.Builder(this).setTitle(title).setItems(labels.toArray(new String[0]),(d,n)-> {
            if(player==null)return; TrackSelectionParameters.Builder b=player.getTrackSelectionParameters().buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type,type==C.TRACK_TYPE_TEXT && n==0);
            if(choices.get(n)!=null)b.setOverrideForType(choices.get(n)); player.setTrackSelectionParameters(b.build());
        }).show();
    }
    private void toggle() { if(player==null)return; if(player.getPlayWhenReady()) { player.pause(); notice("Pausado"); } else { player.play(); if(!firstFrame) { main.removeCallbacks(videoTimeout); main.postDelayed(videoTimeout,10000); } notice("Reproduciendo"); } rebuildActions(); hideNoticeLater(); }
    private void jump(int seconds,boolean ending) {
        if(player==null) { notice("Todavía no hay video preparado."); hideNoticeLater(); return; }
        if(!PlaybackHealth.canSeek(!player.getCurrentTimeline().isEmpty(),player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))) { notice("El video todavía no permite saltar. Prueba cuando termine de cargar."); hideNoticeLater(); return; }
        long duration=player.getDuration(); long target=PlaybackPolicy.seek(player.getCurrentPosition(),seconds*1000L,duration); player.setSeekParameters(Math.abs(seconds)>=30?SeekParameters.CLOSEST_SYNC:SeekParameters.EXACT); player.seekTo(target); int accumulated=seekBurst.add(seconds,SystemClock.elapsedRealtime());notice((accumulated>0?"+":"")+accumulated+" s"); hideNoticeLater();
        if(ending && PlaybackPolicy.reachedEnd(target,duration)) suggestNext();
    }
    private void suggestNext() {
        if(suggested)return; suggested=true;
        if(index+1>=episodes.size()) { notice("Has llegado al último episodio disponible."); return; }
        new AlertDialog.Builder(this).setTitle("Episodio terminado").setMessage("¿Quieres ver el siguiente episodio?").setNegativeButton("Quedarme aquí",null).setPositiveButton("Ver siguiente",(d,n)->nextEpisode()).show();
    }
    private void nextEpisode() { changeEpisode(index+1); }
    private void changeEpisode(int target) { if(target<0 || target>=episodes.size())return; rememberPosition(); index=target;store.lastEpisode(getIntent().getStringExtra("animeUrl"),episodes.get(index)); requestedPosition=store.position(episodes.get(index)); showControls(false); loadEpisode(); }
    private void updateTimeline() { if(player==null||clock==null||seek==null)return;long d=player.getDuration(),p=player.getCurrentPosition();clock.setText(format(p));if(totalClock!=null)totalClock.setText(format(d));if(!seek.isPressed())seek.setProgress(d>0?(int)Math.min(1000,p*1000/d):0); }
    private void scheduleControlsHide(){main.removeCallbacks(hideControls);if(controlsVisible&&foreground&&getWindow().getDecorView().hasWindowFocus())main.postDelayed(hideControls,5000);}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)scheduleControlsHide();else main.removeCallbacks(hideControls);}
    @Override public void onUserInteraction(){super.onUserInteraction();scheduleControlsHide();}
    private void showControls(boolean visible) { if(visible)updateTimeline();controlsVisible=visible; controls.setVisibility(visible?View.VISIBLE:View.GONE);episodeNumber.setVisibility(visible?View.VISIBLE:View.GONE);FrameLayout.LayoutParams noticePosition=(FrameLayout.LayoutParams)status.getLayoutParams();noticePosition.topMargin=dp(visible?72:27);status.setLayoutParams(noticePosition);main.removeCallbacks(hideControls);if(visible){scheduleControlsHide();if(playButton!=null)playButton.requestFocus();}else root.requestFocus(); }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if(controlsVisible)scheduleControlsHide();
        if(controlsVisible&&event.getKeyCode()==KeyEvent.KEYCODE_DPAD_UP){
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){if(seek.hasFocus())showControls(false);else seek.requestFocus();}return true;
        }
        if(controlsVisible&&seek.hasFocus()&&event.getKeyCode()==KeyEvent.KEYCODE_DPAD_DOWN){if(event.getAction()==KeyEvent.ACTION_DOWN&&playButton!=null)playButton.requestFocus();return true;}
        if(controlsVisible&&seek.hasFocus()&&event.getAction()==KeyEvent.ACTION_DOWN&&(event.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT||event.getKeyCode()==KeyEvent.KEYCODE_DPAD_RIGHT)){jump((event.getKeyCode()==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)*store.seconds("timeline"),false);updateTimeline();return true;}
        if(!controlsVisible && event.getAction()==KeyEvent.ACTION_DOWN) {
            int key=event.getKeyCode();
            if(key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER) { if(event.getRepeatCount()==0)toggle(); return true; }
            if(key==KeyEvent.KEYCODE_DPAD_LEFT) { jump(-store.seconds("backward"),false); return true; }
            if(key==KeyEvent.KEYCODE_DPAD_RIGHT) { jump(store.seconds("forward"),false); return true; }
            if(key==KeyEvent.KEYCODE_DPAD_UP) { if(event.getRepeatCount()==0)showControls(true); return true; }
            if(key==KeyEvent.KEYCODE_DPAD_DOWN) return true;
        }
        if(event.getAction()==KeyEvent.ACTION_DOWN && event.getKeyCode()==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) { if(event.getRepeatCount()==0)toggle(); return true; }
        return super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed() { if(controlsVisible) showControls(false); else super.onBackPressed(); }
    private int dp(int n) { return TvUi.dp(this,n); }
    private LinearLayout add(String label,String icon,Runnable action) { LinearLayout b=TvUi.button(this,"",icon,action);android.graphics.drawable.GradientDrawable idleCircle=TvUi.background(this,0x332C3442,0);idleCircle.setCornerRadius(dp(40));b.setBackground(idleCircle);b.setContentDescription(label);b.setTag(label);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(48),dp(48));p.setMargins(dp(5),dp(4),dp(5),dp(4));actions.addView(b,p);b.setOnFocusChangeListener((v,focused)->{android.graphics.drawable.GradientDrawable circle=TvUi.background(this,focused?0xFF344354:0x332C3442,0);circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);if(focused)circle.setStroke(dp(2),TvUi.ACCENT);v.setBackground(circle);if(focused)actionHint.setText(label);});return b; }
    private void notice(String message) { main.removeCallbacks(hideNotice); status.setText(message); status.setVisibility(View.VISIBLE); }
    private final Runnable hideNotice=()->status.setVisibility(View.GONE);
    private void hideNoticeLater() { main.removeCallbacks(hideNotice); main.postDelayed(hideNotice,1800); }
    private String format(long ms) { if(ms<0)return "--:--"; long sec=ms/1000; return String.format(Locale.ROOT,"%d:%02d",sec/60,sec%60); }
    private void releasePlayer() { seekBurst.reset(); main.removeCallbacks(timeout); main.removeCallbacks(videoTimeout); if(player!=null) { ExoPlayer old=player; player=null; surface.setPlayer(null); old.release(); } }
    @Override protected void onPause() { returningServer=activeServer;returningPaused=player!=null&&!player.getPlayWhenReady();foreground=false; main.removeCallbacks(hideControls);rememberPosition();++generation;cancelExtraction();releasePlayer(); if(surface!=null)surface.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if(surface!=null)surface.onResume(); if(!foreground) { foreground=true; startedAt=SystemClock.elapsedRealtime(); Server previous=returningServer;returningServer=null; if(previous!=null && activeStream!=null && PlaybackRecovery.reuse(resolvedAt,startedAt)){long originalResolution=resolvedAt;startStream(previous,activeStream);resolvedAt=originalResolution;}else if(previous!=null){source.invalidate(previous,episodes.get(index));selectServer(previous);}else loadEpisode(); }scheduleControlsHide(); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putInt("index",index); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() { ProtectedPages.detach(this); ++generation; main.removeCallbacksAndMessages(null); releasePlayer(); io.shutdownNow(); super.onDestroy(); }
}







