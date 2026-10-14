package pe.animetv;

import android.net.Uri;
import androidx.media3.datasource.*;
import java.io.IOException;
import java.util.*;
import javax.crypto.Cipher;

/** Random-access AES-CTR decryption; opens the requested HTTP range rather than a full download. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class MegaDataSource implements DataSource {
    private final DataSource upstream;private final String key;private Cipher cipher;
    MegaDataSource(DataSource upstream,String key){this.upstream=upstream;this.key=key;}
    public long open(DataSpec spec)throws IOException{cipher=StreamExtractors.megaCipher(key,spec.position);return upstream.open(spec);}
    public int read(byte[] buffer,int offset,int length)throws IOException{
        int count=upstream.read(buffer,offset,length);if(count<=0)return count;
        try{int written=cipher.update(buffer,offset,count,buffer,offset);if(written!=count)throw new IOException("Mega: lectura descifrada incompleta");return written;}
        catch(IOException e){throw e;}catch(Exception e){throw new IOException("Mega: error al descifrar el video",e);}
    }
    public Uri getUri(){return upstream.getUri();}
    public Map<String,List<String>> getResponseHeaders(){return upstream.getResponseHeaders();}
    public void addTransferListener(TransferListener listener){upstream.addTransferListener(listener);}
    public void close()throws IOException{cipher=null;upstream.close();}
}
