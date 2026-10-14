package pe.animetv;
import androidx.media3.common.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class VideoQualitiesTest {
    private Tracks.Group group(Format... formats){int[] support=new int[formats.length];Arrays.fill(support,C.FORMAT_HANDLED);return new Tracks.Group(new TrackGroup(formats),true,support,new boolean[formats.length]);}
    private Format video(int height,int bitrate,int flags){return new Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).setHeight(height).setWidth(height*16/9).setAverageBitrate(bitrate).setFrameRate(24).setRoleFlags(flags).build();}
    @Test public void deduplicates1080AndSkipsTrickPlay(){Tracks tracks=new Tracks(List.of(group(video(1080,3000000,0),video(1080,5000000,0),video(1080,300000,C.ROLE_FLAG_TRICK_PLAY),video(720,1800000,0))));List<VideoQualities.Choice> choices=VideoQualities.from(tracks);assertEquals(List.of("1080p","720p"),choices.stream().map(VideoQualities.Choice::label).toList());assertEquals(5000000,choices.get(0).format().averageBitrate);}
    @Test public void neverInvents720WhenProviderOnlyHas1080(){List<VideoQualities.Choice> choices=VideoQualities.from(new Tracks(List.of(group(video(1080,3000000,0),video(1080,300000,C.ROLE_FLAG_TRICK_PLAY)))));assertEquals(1,choices.size());assertEquals("1080p",choices.get(0).label());}
}
