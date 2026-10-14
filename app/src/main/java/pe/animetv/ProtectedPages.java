package pe.animetv;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;
import okhttp3.*;
import org.json.JSONTokener;

/** User-operated verification, confined to the requested source. Never solves a challenge. */
final class ProtectedPages {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static WeakReference<Activity> host=new WeakReference<>(null);
    private static volatile String userAgent;
    private static BrowserRequest active;
    private static final Object QUEUE=new Object();
    static void attach(Activity activity){host=new WeakReference<>(activity);SourcePageAccess.install(new SourcePageAccess.Host(){public String get(String url,boolean interactive)throws Exception{return ProtectedPages.get(url,interactive);}public String catalog(String url)throws Exception{return ProtectedPages.catalog(url);}});}
    static void detach(Activity activity){if(active!=null&&active.activity==activity)active.close();if(host.get()==activity)host=new WeakReference<>(null);}
    static boolean allowed(String url){HttpUrl u=HttpUrl.parse(url);return u!=null&&u.isHttps()&&(u.host().equals("www.animelatinohd.com")||u.host().equals("animelatinohd.com"));}
    static boolean challenge(int status,String body){return (status==403||status==503)&&(body.contains("cf-chl")||body.contains("challenge-platform")||body.toLowerCase(java.util.Locale.ROOT).contains("just a moment"));}
    static boolean ready(String body){return body.contains("<main")&&body.contains("self.__next_f.push")&&!body.contains("cf-chl-opt");}
    private static String agent()throws Exception {
        if(userAgent!=null)return userAgent;Activity activity=host.get();if(activity==null)throw new IOException("Abre AnimeLatinoHD desde la app para verificar el acceso");
        FutureTask<String> result=new FutureTask<>(()->{String value=WebSettings.getDefaultUserAgent(activity);userAgent=value==null?SourceNetwork.USER_AGENT:value;return userAgent;});MAIN.post(result);return result.get(10,TimeUnit.SECONDS);
    }
    static String get(String url)throws Exception {
        return get(url,true);
    }
    static String get(String url,boolean interactive)throws Exception {
        if(!allowed(url))throw new IOException("Destino de verificación no permitido");String ua=agent();String cookies=CookieManager.getInstance().getCookie(url);
        Request.Builder request=new Request.Builder().url(url).header("User-Agent",ua).header("Referer","https://www.animelatinohd.com/");if(cookies!=null&&!cookies.isBlank())request.header("Cookie",cookies);
        SourceHttp.Page response=SourceHttp.page(SourceNetwork.HTTP,request.build());String body=response.body();
        if(response.code()>=200&&response.code()<300)return body;
        if(!challenge(response.code(),body))throw new IOException("AnimeLatinoHD: HTTP "+response.code());
        return browser(url,ua,false,interactive);
    }
    static String catalog(String url)throws Exception{return browser(url,agent(),true,true);}
    private static String browser(String url,String ua,boolean rendered,boolean interactive)throws Exception {
        if(!allowed(url))throw new IOException("Destino de verificación no permitido");
        synchronized(QUEUE){
            BrowserRequest requestView=new BrowserRequest(url,ua,rendered,interactive);MAIN.post(requestView::open);
            try{if(!requestView.done.await(90,TimeUnit.SECONDS))throw new IOException("La verificación no terminó. Actualiza Android System WebView y vuelve a intentarlo.");if(requestView.html==null)throw new IOException("Verificación cancelada");return requestView.html;}
            catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new IOException("Verificación cancelada",cancelled);}
            finally{MAIN.post(requestView::close);}
        }
    }
    static final class Pointer {
        float x,y;Pointer(float x,float y){this.x=x;this.y=y;}
        void move(float dx,float dy,float width,float height){x=Math.max(0,Math.min(Math.max(0,width-1),x+dx));y=Math.max(0,Math.min(Math.max(0,height-1),y+dy));}
    }
    private static final class BrowserRequest {
        final String url,ua;final boolean rendered,interactive;final CountDownLatch done=new CountDownLatch(1);String html;Activity activity;Dialog dialog;WebView web;View cursor;FrameLayout frame;boolean closed,shown;long started;Pointer pointer=new Pointer(100,100);
        BrowserRequest(String url,String ua,boolean rendered,boolean interactive){this.url=url;this.ua=ua;this.rendered=rendered;this.interactive=interactive;}
        void open(){
            activity=host.get();if(activity==null||activity.isFinishing()||activity.isDestroyed()){close();return;}
            try{
                active=this;dialog=new Dialog(activity);LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(16,12,16,12);box.setBackgroundColor(TvUi.BG);
                box.addView(TvUi.text(activity,"Verificar AnimeLatinoHD",20,TvUi.TEXT));box.addView(TvUi.text(activity,"Flechas: mover puntero · OK: clic · Atrás: cancelar",14,TvUi.MUTED));
                frame=new FrameLayout(activity);box.addView(frame,new LinearLayout.LayoutParams(-1,0,1));web=new WebView(activity);frame.addView(web,new FrameLayout.LayoutParams(-1,-1));
                web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setMediaPlaybackRequiresUserGesture(true);web.getSettings().setUserAgentString(ua);CookieManager.getInstance().setAcceptCookie(true);
                cursor=new View(activity){final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);@Override protected void onDraw(Canvas canvas){paint.setColor(TvUi.ACCENT);canvas.drawCircle(12,12,10,paint);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);canvas.drawCircle(12,12,10,paint);paint.setStyle(Paint.Style.FILL);}};
                frame.addView(cursor,new FrameLayout.LayoutParams(24,24));cursor.setClickable(false);frame.post(()->{pointer=new Pointer(frame.getWidth()/2f,frame.getHeight()/2f);place();});
                web.setWebViewClient(new WebViewClient(){
                    @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return request.isForMainFrame()&&!allowed(request.getUrl().toString());}
                    @Override public boolean shouldOverrideUrlLoading(WebView view,String target){return !allowed(target);}
                    @Override public void onPageFinished(WebView view,String target){if(allowed(target))inspect();}
                });
                box.addView(TvUi.button(activity,"Cancelar","close",this::close));dialog.setContentView(box);dialog.setOnCancelListener(d->close());dialog.setOnKeyListener((d,key,event)->{
                    if(key==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)close();return true;}
                    if(event.getAction()!=KeyEvent.ACTION_DOWN)return key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER;
                    float step=event.getRepeatCount()>0?28:14;
                    if(key==KeyEvent.KEYCODE_DPAD_LEFT)pointer.move(-step,0,frame.getWidth(),frame.getHeight());else if(key==KeyEvent.KEYCODE_DPAD_RIGHT)pointer.move(step,0,frame.getWidth(),frame.getHeight());else if(key==KeyEvent.KEYCODE_DPAD_UP)pointer.move(0,-step,frame.getWidth(),frame.getHeight());else if(key==KeyEvent.KEYCODE_DPAD_DOWN)pointer.move(0,step,frame.getWidth(),frame.getHeight());else if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){click();return true;}else return false;place();return true;
                });
                started=SystemClock.elapsedRealtime();web.layout(0,0,960,540);web.loadUrl(url);
            }catch(Exception failure){close();}
        }
        void place(){cursor.setTranslationX(pointer.x-12);cursor.setTranslationY(pointer.y-12);}
        void click(){long now=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(now,now,action,pointer.x,pointer.y,0);web.dispatchTouchEvent(event);event.recycle();}MAIN.postDelayed(this::inspect,1000);}
        void inspect(){if(closed||web==null)return;web.evaluateJavascript("document.documentElement.outerHTML",result->{if(closed)return;try{Object decoded=new JSONTokener(result).nextValue();if(decoded instanceof String){String body=(String)decoded;
            if(body.contains("cf-chl")||body.toLowerCase(java.util.Locale.ROOT).contains("just a moment")){if(!interactive){if(SystemClock.elapsedRealtime()-started>=8000){close();return;}}else if(!shown){shown=true;dialog.show();dialog.getWindow().setLayout(activity.getResources().getDisplayMetrics().widthPixels-32,activity.getResources().getDisplayMetrics().heightPixels-32);}}
            else if(ready(body)&&(!rendered||SystemClock.elapsedRealtime()-started>=2500&&!body.contains("BUSCANDO..."))){html=rendered?"<!-- animetv-rendered-catalog -->"+body:body;CookieManager.getInstance().flush();close();return;}
        }}catch(Exception ignored){}MAIN.postDelayed(this::inspect,750);});}
        void close(){if(closed)return;closed=true;done.countDown();if(dialog!=null)dialog.dismiss();if(web!=null){web.stopLoading();frame.removeView(web);web.destroy();web=null;}if(active==this)active=null;}
    }
}
