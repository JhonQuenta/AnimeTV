package pe.animetv;
import android.os.*;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.video.*;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FfmpegVideoRenderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class CompatibleRenderersFactoryTest {
 @Test public void hlsHasSoftwareVideoFallbackAfterAndroidRenderer(){
  assertHardwareFirstWithSoftware("HLS");
 }
 @Test public void mp4UploadHasSoftwareFallbackAfterAndroidRenderer(){assertHardwareFirstWithSoftware("MP4Upload");}
 @Test public void otherServersKeepAndroidOnly(){
  for(String server:new String[]{"Voe","Byse"}){
   for(Renderer renderer:renderers(server))assertFalse(renderer instanceof FfmpegVideoRenderer);
  }
 }
 private Renderer[] renderers(String server){
  return CompatibleRenderersFactory.forServer(RuntimeEnvironment.getApplication(),server).createRenderers(new Handler(Looper.getMainLooper()),new VideoRendererEventListener(){},new AudioRendererEventListener(){},cues->{},metadata->{});
 }
 private void assertHardwareFirstWithSoftware(String server){
  Renderer[] renderers=renderers(server);
  int hardware=-1,software=-1;
  for(int n=0;n<renderers.length;n++){if(renderers[n] instanceof MediaCodecVideoRenderer)hardware=n;if(renderers[n] instanceof FfmpegVideoRenderer)software=n;}
  assertTrue(hardware>=0);assertTrue(software>hardware);
 }
}
