# FFmpeg accesses these fields and methods through JNI, outside Java call graphs.
-keep class io.github.anilbeesetti.nextlib.media3ext.ffdecoder.** { *; }
-keep class androidx.media3.decoder.VideoDecoderOutputBuffer { *; }
