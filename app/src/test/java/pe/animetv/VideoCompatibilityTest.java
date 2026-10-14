package pe.animetv;
import androidx.media3.common.*;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class VideoCompatibilityTest {
 private Tracks tracks(String mime,int support){Format f=new Format.Builder().setSampleMimeType(mime).setHeight(1080).build();return new Tracks(List.of(new Tracks.Group(new TrackGroup(f),false,new int[]{support},new boolean[]{support==C.FORMAT_HANDLED})));}
 @Test public void unsupportedAV1HasSpecificExplanation(){String reason=PlayerActivity.incompatibleVideo(tracks(MimeTypes.VIDEO_AV1,C.FORMAT_UNSUPPORTED_TYPE),"MP4Upload");assertTrue(reason.contains("MP4Upload"));assertTrue(reason.contains("AV1"));assertTrue(reason.contains("decodificador"));}
 @Test public void AV1IsAllowedWhenDeviceReportsSupport(){assertEquals("",PlayerActivity.incompatibleVideo(tracks(MimeTypes.VIDEO_AV1,C.FORMAT_HANDLED),"MP4Upload"));}
 @Test public void EmptyTracksDoNotPrematurelyRejectLoading(){assertEquals("",PlayerActivity.incompatibleVideo(Tracks.EMPTY,"MP4Upload"));}
}
