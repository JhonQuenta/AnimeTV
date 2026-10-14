package pe.animetv;

import java.net.*;
import java.util.*;
import okhttp3.*;
import org.json.*;

/** Fallback only for Latanime when the device resolver returns UnknownHostException. */
final class LatanimeDns implements Dns {
    private static final OkHttpClient RESOLVER=new OkHttpClient.Builder().connectTimeout(3,java.util.concurrent.TimeUnit.SECONDS).readTimeout(3,java.util.concurrent.TimeUnit.SECONDS).callTimeout(5,java.util.concurrent.TimeUnit.SECONDS).build();
    private final TimedCache<List<InetAddress>> cache=new TimedCache<>(2,300000);
    public List<InetAddress> lookup(String host)throws UnknownHostException {
        try{return Dns.SYSTEM.lookup(host);}catch(UnknownHostException initial){
            if(!host.equals("latanime.org")&&!host.equals("www.latanime.org"))throw initial;
            try{return cache.get(host,()->{
                HttpUrl url=HttpUrl.get("https://dns.google/resolve").newBuilder().addQueryParameter("name",host).addQueryParameter("type","A").build();
                try(Response response=RESOLVER.newCall(new Request.Builder().url(url).build()).execute()){
                    if(!response.isSuccessful()||response.body()==null)throw new UnknownHostException(host);
                    return addresses(host,new JSONObject(response.body().string()));
                }
            });}catch(Exception failure){initial.addSuppressed(failure);throw initial;}
        }
    }
    static List<InetAddress> addresses(String host,JSONObject result)throws Exception {
        List<InetAddress> out=new ArrayList<>();JSONArray answers=result.optJSONArray("Answer");
        if(result.optInt("Status",-1)==0&&answers!=null)for(int i=0;i<answers.length();i++){
            JSONObject answer=answers.getJSONObject(i);String value=answer.optString("data");
            if(answer.optInt("type")==1&&value.matches("[0-9]{1,3}(?:\\.[0-9]{1,3}){3}"))out.add(InetAddress.getByAddress(host,InetAddress.getByName(value).getAddress()));
        }
        if(out.isEmpty())throw new UnknownHostException(host);return List.copyOf(out);
    }
}
