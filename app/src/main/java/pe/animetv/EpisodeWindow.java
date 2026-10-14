package pe.animetv;

import java.util.*;

/** Windows contain at most 100 episodes; indices always refer to the original playlist. */
final class EpisodeWindow {
    static int groups(int count){return (count+99)/100;}
    static List<Integer> indices(int count,int group,boolean descending){
        if(count<=0)return List.of();
        int selected=Math.max(0,Math.min(group,groups(count)-1));
        int start=selected*100,end=Math.min(count,start+100);
        List<Integer> result=new ArrayList<>();
        for(int n=start;n<end;n++)result.add(n);
        if(descending)Collections.reverse(result);
        return result;
    }
    static String label(int count,int group){return (group*100+1)+"-"+Math.min(count,(group+1)*100);}
}
