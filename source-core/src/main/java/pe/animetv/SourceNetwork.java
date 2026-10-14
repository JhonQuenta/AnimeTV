package pe.animetv;
import java.util.concurrent.TimeUnit;import okhttp3.OkHttpClient;
/** Shared source/extractor transport, independent of any particular website. */
public final class SourceNetwork {
    private SourceNetwork(){}
    public static final String USER_AGENT="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    public static final OkHttpClient HTTP=new OkHttpClient.Builder().dns(new LatanimeDns()).connectTimeout(5,TimeUnit.SECONDS).readTimeout(7,TimeUnit.SECONDS).callTimeout(10,TimeUnit.SECONDS).build();
}
