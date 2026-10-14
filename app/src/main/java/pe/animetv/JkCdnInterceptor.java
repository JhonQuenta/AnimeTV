package pe.animetv;

import java.io.IOException;
import java.util.*;
import java.util.function.*;
import okhttp3.*;
import okio.*;

/** JKAnime's replicated TS CDN: quarantine slow nodes, verify bytes before using a mirror. */
final class JkCdnInterceptor implements Interceptor {
    private final Map<String,Long> slow=new LinkedHashMap<>();
    private final Predicate<Request> accepts;
    private final Function<HttpUrl,HttpUrl> mirror;
    private final LongSupplier clock;
    JkCdnInterceptor(){this(JkCdnInterceptor::eligible,u->u.newBuilder().host(u.host().equals("cdn1.ducvomes.com")?"cdn3.ducvomes.com":"cdn1.ducvomes.com").build(),()->System.nanoTime()/1000000);}
    JkCdnInterceptor(Predicate<Request> accepts,Function<HttpUrl,HttpUrl> mirror,LongSupplier clock){this.accepts=accepts;this.mirror=mirror;this.clock=clock;}
    static boolean ownHls(String server,String mime){return (server.equals("Desu")||server.equals("Magi"))&&mime.equalsIgnoreCase("application/x-mpegURL");}
    static boolean eligible(Request request){HttpUrl ref=HttpUrl.parse(Objects.toString(request.header("Referer"),""));return request.method().equals("GET")&&request.url().isHttps()&&request.url().host().matches("cdn[1-6]\\.ducvomes\\.com")&&request.url().encodedPath().matches("/(?:ja|jp)/segs/.+\\.ts")&&ref!=null&&ref.host().equals("jkanime.net")&&ref.encodedPath().startsWith("/jkplayer/");}
    synchronized void markSlow(String host){slow.put(host,clock.getAsLong()+120000);while(slow.size()>8)slow.remove(slow.keySet().iterator().next());}
    private synchronized boolean slow(String host){return slow.getOrDefault(host,0L)>clock.getAsLong();}
    static boolean same(Response original,byte[] sample,Response alternative,byte[] other){return original.isSuccessful()&&alternative.isSuccessful()&&original.code()==alternative.code()&&original.body()!=null&&alternative.body()!=null&&original.body().contentLength()>0&&original.body().contentLength()==alternative.body().contentLength()&&Objects.equals(original.header("Content-Range"),alternative.header("Content-Range"))&&sample.length>=65536&&Arrays.equals(sample,other);}
    @Override public Response intercept(Chain chain)throws IOException {
        Request request=chain.request();if(!accepts.test(request))return chain.proceed(request);
        Response original=chain.proceed(request);
        if(original.isSuccessful()&&original.body()!=null&&slow(request.url().host())){
            byte[] first;try{first=original.peekBody(65536).bytes();}catch(IOException failed){original.close();throw failed;}
            if(first.length==65536){
                original.close();Response alternate=null;boolean accepted=false;
                try{
                    alternate=chain.proceed(request.newBuilder().url(mirror.apply(request.url())).build());byte[] other=alternate.peekBody(65536).bytes();
                    if(same(original,first,alternate,other)){accepted=true;return monitor(alternate);}
                }catch(IOException unavailable){if(chain.call().isCanceled())throw unavailable;}
                finally{if(alternate!=null&&!accepted)alternate.close();}
                original=chain.proceed(request);
            }
        }
        return monitor(original);
    }
    private Response monitor(Response response){
        ResponseBody body=response.body();if(body==null||!response.isSuccessful())return response;long started=clock.getAsLong();
        BufferedSource source=Okio.buffer(new ForwardingSource(body.source()){
            long read;
            @Override public long read(Buffer sink,long count)throws IOException{long n=super.read(sink,count);if(n>0){read+=n;long elapsed=clock.getAsLong()-started;if(elapsed>=6000&&read*1000/Math.max(1,elapsed)<250000){markSlow(response.request().url().host());throw new IOException("JKAnime: nodo CDN lento; reintentar con réplica verificada");}}return n;}
        });
        return response.newBuilder().body(new ResponseBody(){public MediaType contentType(){return body.contentType();}public long contentLength(){return body.contentLength();}public BufferedSource source(){return source;}}).build();
    }
}
