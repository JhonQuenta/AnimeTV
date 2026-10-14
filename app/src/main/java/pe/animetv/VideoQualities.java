package pe.animetv;
import androidx.media3.common.*;
import java.util.*;

/** One playable full-video choice per actual resolution; skip HLS seek thumbnails. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class VideoQualities {
    record Choice(String label,Format format,TrackSelectionOverride override){}
    static List<Choice> from(Tracks tracks){
        Map<Integer,Choice> heights=new TreeMap<>(Comparator.reverseOrder());
        for(Tracks.Group group:tracks.getGroups())if(group.getType()==C.TRACK_TYPE_VIDEO)for(int n=0;n<group.length;n++){
            Format f=group.getTrackFormat(n);if(!group.isTrackSupported(n)||(f.roleFlags&C.ROLE_FLAG_TRICK_PLAY)!=0)continue;
            Choice previous=heights.get(f.height);if(previous==null||f.frameRate>previous.format.frameRate||(f.frameRate==previous.format.frameRate&&f.averageBitrate>previous.format.averageBitrate))heights.put(f.height,new Choice(f.height>0?f.height+"p":"Original",f,new TrackSelectionOverride(group.getMediaTrackGroup(),n)));
        }
        return List.copyOf(heights.values());
    }
}
