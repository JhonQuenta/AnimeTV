package pe.animetv;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;

/** Interrupting a superseded source lookup also cancels its real HTTP call. */
final class SourceHttp {
    record Page(int code,String body){}
    static Page page(OkHttpClient client,Request request)throws IOException {
        Call call=client.newCall(request);CountDownLatch done=new CountDownLatch(1);AtomicReference<Page> result=new AtomicReference<>();AtomicReference<IOException> failure=new AtomicReference<>();
        if(Thread.currentThread().isInterrupted())throw new IOException("Consulta cancelada");
        call.enqueue(new Callback(){
            public void onFailure(Call c,IOException e){failure.set(e);done.countDown();}
            public void onResponse(Call c,Response r){try(r){if(r.body()==null)throw new IOException("Respuesta vacía");result.set(new Page(r.code(),r.body().string()));}catch(IOException e){failure.set(e);}finally{done.countDown();}}
        });
        try{done.await();}catch(InterruptedException cancelled){call.cancel();Thread.currentThread().interrupt();throw new IOException("Consulta cancelada",cancelled);}
        if(failure.get()!=null)throw failure.get();return result.get();
    }
    static String get(OkHttpClient client,Request request,String error)throws IOException {Page p=page(client,request);if(p.code()<200||p.code()>=300)throw new IOException(error+" HTTP "+p.code());return p.body();}
}
