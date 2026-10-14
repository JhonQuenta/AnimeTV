package pe.animetv;

import android.content.Context;
import java.io.File;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

/** Shared disk cache only for artwork, respecting the origin's HTTP cache policy. */
final class ArtworkHttp {
    private static OkHttpClient client;
    private ArtworkHttp() {}
    static okhttp3.Request request(String url) {
        okhttp3.HttpUrl address=okhttp3.HttpUrl.get(url);
        return new okhttp3.Request.Builder().url(address).header("User-Agent",SourceNetwork.USER_AGENT)
                .header("Referer",(address.host().equals("cdn.jkdesa.com")?"https://jkanime.net/":address.scheme()+"://"+address.host()+"/")).build();
    }
    static synchronized OkHttpClient client(Context context) {
        if(client==null)client=SourceNetwork.HTTP.newBuilder()
                .cache(new Cache(new File(context.getApplicationContext().getCacheDir(),"artwork-http"),16L*1024*1024))
                .build();
        return client;
    }
}
