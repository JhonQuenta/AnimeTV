package pe.animetv;
/** More runway for high-bitrate JK segments and slow progressive downloads, bounded in RAM. */
record PlaybackBufferPolicy(int minimum,int maximum,int start,int rebuffer,int bytes,int backBuffer) {
    static PlaybackBufferPolicy forStream(String server,String mime){
        if(JkCdnInterceptor.ownHls(server,mime))return new PlaybackBufferPolicy(25000,45000,12000,15000,48*1024*1024,2000);
        if(server.equals("MP4Upload")||server.equals("HLS"))return new PlaybackBufferPolicy(20000,40000,8000,10000,32*1024*1024,2000);
        return new PlaybackBufferPolicy(5000,30000,1000,2000,24*1024*1024,5000);
    }
}
