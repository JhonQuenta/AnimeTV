package pe.animetv;

import android.content.Context;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.cache.*;
import androidx.media3.database.StandaloneDatabaseProvider;
import java.io.File;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import java.util.*;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import static pe.animetv.Models.*;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class PlaybackDataSources {
    static final OkHttpClient HTTP=SourceNetwork.HTTP.newBuilder().connectTimeout(8,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(0,TimeUnit.SECONDS).retryOnConnectionFailure(true).addInterceptor(new JkCdnInterceptor()).build();
    private static SimpleCache cache;
    private PlaybackDataSources() {}
    static OkHttpDataSource.Factory upstream(Server server,Stream stream) {
        Map<String,String> headers=new HashMap<>(stream.headers());headers.put("Referer",stream.referer());
        return new OkHttpDataSource.Factory(HTTP).setUserAgent(StreamExtractors.userAgent(server.name())).setDefaultRequestProperties(headers);
    }
    static synchronized DataSource.Factory cached(Context context,DataSource.Factory upstream) {
        try {
            if(cache==null) {
                Context app=context.getApplicationContext();
                cache=new SimpleCache(new File(app.getCacheDir(),"video"),new LeastRecentlyUsedCacheEvictor(64L*1024*1024),new StandaloneDatabaseProvider(app));
            }
            return new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(upstream).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        } catch(RuntimeException e) { return upstream; }
    }
}
