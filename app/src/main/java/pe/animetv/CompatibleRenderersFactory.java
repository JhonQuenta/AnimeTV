package pe.animetv;

import android.content.Context;
import android.os.Handler;
import java.util.ArrayList;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.video.VideoRendererEventListener;
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FfmpegVideoRenderer;

/** Android decoders remain first; HLS and MP4Upload can fall back to dav1d for AV1. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class CompatibleRenderersFactory extends DefaultRenderersFactory {
    CompatibleRenderersFactory(Context context){super(context);}
    static boolean needsSoftwareFallback(String serverName) {
        return "HLS".equals(serverName) || "MP4Upload".equals(serverName);
    }
    static DefaultRenderersFactory forServer(Context context,String serverName) {
        return (needsSoftwareFallback(serverName)?new CompatibleRenderersFactory(context):new DefaultRenderersFactory(context)).setEnableDecoderFallback(true);
    }
    @Override protected void buildVideoRenderers(Context context,int extensionRendererMode,MediaCodecSelector codecSelector,
            boolean enableDecoderFallback,Handler handler,VideoRendererEventListener listener,long joiningTimeMs,ArrayList<Renderer> out) {
        super.buildVideoRenderers(context,extensionRendererMode,codecSelector,enableDecoderFallback,handler,listener,joiningTimeMs,out);
        out.add(new FfmpegVideoRenderer(joiningTimeMs,handler,listener,50,Math.min(4,Runtime.getRuntime().availableProcessors()),4,4));
    }
}
