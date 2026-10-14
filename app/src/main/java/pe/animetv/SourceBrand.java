package pe.animetv;
import android.content.Context;
import android.view.Gravity;
import android.widget.*;
import static pe.animetv.Models.*;

/** Official site logo marks, stored locally so source identity never waits for the network. */
final class SourceBrand {
    static ImageView icon(Context context,String id,int size){
        ImageView image=new ImageView(context);image.setImageResource(switch(id){case "jkanime"->R.drawable.source_jk_mark;case "tioanime"->R.drawable.source_tio_mark;case "latanime"->R.drawable.source_lat_mark;case "animelatinohd"->R.drawable.source_hd;default->R.drawable.source_av1_mark;});image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setLayoutParams(new LinearLayout.LayoutParams(TvUi.dp(context,size),TvUi.dp(context,size)));image.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);return image;
    }
    static LinearLayout badge(Context context,Source source,int size){
        LinearLayout badge=new LinearLayout(context);badge.setGravity(Gravity.CENTER_VERTICAL);badge.setPadding(TvUi.dp(context,5),TvUi.dp(context,3),TvUi.dp(context,5),TvUi.dp(context,3));badge.setBackground(TvUi.background(context,0xE018273B,0));badge.addView(icon(context,source.id(),14));TextView label=TvUi.text(context,source.name(),size,TvUi.TEXT);label.setSingleLine(true);LinearLayout.LayoutParams text=new LinearLayout.LayoutParams(-2,-2);text.leftMargin=TvUi.dp(context,4);badge.addView(label,text);badge.setContentDescription("Fuente del anime: "+source.name());return badge;
    }
}
