package pe.animetv;
import android.content.Context;import androidx.media3.common.*;import androidx.media3.exoplayer.mediacodec.*;import java.util.function.Predicate;
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class VideoCompatibility {
    static boolean hardware(Context context,Format format){try{return MediaCodecUtil.getDecoderInfos(format.sampleMimeType,false,false).stream().anyMatch(codec->codec.hardwareAccelerated&&codec.isFormatSupported(context,format));}catch(Exception unavailable){return false;}}
    static boolean unsupportedAv1(Tracks tracks,Predicate<Format> hardware){
        for(Tracks.Group group:tracks.getGroups())if(group.getType()==C.TRACK_TYPE_VIDEO)for(int n=0;n<group.length;n++)if(group.isTrackSelected(n)){
            Format format=group.getTrackFormat(n);if(MimeTypes.VIDEO_AV1.equals(format.sampleMimeType)&&!hardware.test(format))return true;
        }return false;
    }
}
