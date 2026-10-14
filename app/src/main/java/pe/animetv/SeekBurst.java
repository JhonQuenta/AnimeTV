package pe.animetv;
/** On-screen total for consecutive seeks in the same direction; actual seeking stays immediate. */
final class SeekBurst {
    private long last=Long.MIN_VALUE;private int total;
    int add(int seconds,long now){boolean fresh=last==Long.MIN_VALUE||now-last>1200||Integer.signum(seconds)!=Integer.signum(total);total=fresh?seconds:total+seconds;last=now;return total;}
    void reset(){last=Long.MIN_VALUE;total=0;}
}
