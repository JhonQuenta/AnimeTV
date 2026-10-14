package pe.animetv;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;

/** Interrupting abandoned artwork work cancels the socket, preserving the original image bytes. */
final class ArtworkBytes {
    static byte[] get(OkHttpClient http,Request request)throws IOException{
        if(Thread.currentThread().isInterrupted())throw new IOException("Imagen cancelada");
        Call call=http.newCall(request);CountDownLatch complete=new CountDownLatch(1);
        AtomicReference<byte[]> bytes=new AtomicReference<>();AtomicReference<IOException> failure=new AtomicReference<>();
        call.enqueue(new Callback(){
            public void onFailure(Call c,IOException e){failure.set(e);complete.countDown();}
            public void onResponse(Call c,Response response){try(response){if(!response.isSuccessful()||response.body()==null)throw new IOException("Imagen HTTP "+response.code());bytes.set(response.body().bytes());}catch(IOException e){failure.set(e);}finally{complete.countDown();}}
        });
        try{complete.await();}catch(InterruptedException e){call.cancel();Thread.currentThread().interrupt();throw new IOException("Imagen cancelada",e);}
        if(failure.get()!=null)throw failure.get();return bytes.get();
    }
}
