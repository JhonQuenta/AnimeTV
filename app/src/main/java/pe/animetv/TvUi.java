package pe.animetv;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public final class TvUi {
    public static final int BG=0xFF0C0F14,PANEL=0xFF181D26,TEXT=0xFFF3F5FA,MUTED=0xFFADB9C8,ACCENT=0xFF2688FF;
    private TvUi() {}
    public static int dp(Context c,int n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    public static GradientDrawable background(Context c,int color,int stroke) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c,12)); if(stroke!=0)d.setStroke(dp(c,2),stroke); return d; }
    public static TextView text(Context c,String value,int size,int color) { TextView t=new TextView(c); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setFontFeatureSettings("kern"); return t; }
    public static void focus(View v) {
        v.setFocusable(true); v.setClickable(true); v.setBackground(background(v.getContext(),PANEL,0));
        v.setOnFocusChangeListener((view,focused)-> {
            refreshFocus(view);
        });
    }
    public static void refreshFocus(View view){if(view.getTag() instanceof Integer){GradientDrawable capsule=background(view.getContext(),view.isSelected()?0xFF174F86:view.hasFocus()?0xFF263445:PANEL,view.hasFocus()?ACCENT:0);capsule.setCornerRadius(dp(view.getContext(),24));view.setBackground(capsule);return;}if(view instanceof ProgressRow){((ProgressRow)view).refreshBackground();return;}boolean navigation=view.getTag() instanceof String&&java.util.List.of("Inicio","Catálogo","Calendario","Favoritos","Ajustes").contains(view.getTag());view.setBackground(background(view.getContext(),view.isSelected()?(navigation?0xFF14293F:0xFF174F86):view.hasFocus()?0xFF263445:navigation?BG:PANEL,view.hasFocus()?ACCENT:0));if(navigation&&view instanceof LinearLayout){LinearLayout item=(LinearLayout)view;if(item.getChildAt(0) instanceof Icon)((Icon)item.getChildAt(0)).tint(view.isSelected()?ACCENT:MUTED);if(item.getChildAt(1) instanceof TextView)((TextView)item.getChildAt(1)).setTextColor(view.isSelected()?ACCENT:TEXT);}}
    public static LinearLayout setting(Context c,String label,String value,Runnable action){
        LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(c,16),dp(c,10),dp(c,16),dp(c,10));row.setMinimumHeight(dp(c,52));LinearLayout.LayoutParams spacing=new LinearLayout.LayoutParams(-1,-2);spacing.bottomMargin=dp(c,4);row.setLayoutParams(spacing);
        row.addView(text(c,label,16,TEXT),new LinearLayout.LayoutParams(0,-2,1));row.addView(text(c,value,14,MUTED));Icon arrow=new Icon(c,"chevron");LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(c,16),dp(c,16));size.leftMargin=dp(c,14);row.addView(arrow,size);focus(row);row.setOnClickListener(v->action.run());row.setContentDescription(label+", "+value);return row;
    }
    public static android.app.Dialog dialog(Context c,String title,LinearLayout body,boolean drawer){
        android.app.Dialog d=new android.app.Dialog(c);LinearLayout panel=new LinearLayout(c);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(c,20),dp(c,16),dp(c,20),dp(c,16));panel.setBackground(background(c,PANEL,0));
        TextView heading=text(c,title,23,TEXT);heading.setPadding(0,0,0,dp(c,14));panel.addView(heading);ScrollView scroll=new TvScrollView(c);scroll.addView(body);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));d.setContentView(panel);d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);d.show();
        int width=Math.min(dp(c,drawer?360:480),c.getResources().getDisplayMetrics().widthPixels-dp(c,40));int height=c.getResources().getDisplayMetrics().heightPixels;
        d.getWindow().setGravity(drawer?Gravity.RIGHT:Gravity.CENTER);d.getWindow().setLayout(width,drawer?height:dp(c,340));if(!drawer)panel.post(()->{body.measure(View.MeasureSpec.makeMeasureSpec(width-dp(c,40),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));d.getWindow().setLayout(width,Math.min(body.getMeasuredHeight()+dp(c,84),height-dp(c,40)));});return d;
    }
    public static LinearLayout button(Context c,String caption,String icon,Runnable action) {
        LinearLayout b=new LinearLayout(c); b.setGravity(Gravity.CENTER); b.setPadding(dp(c,12),dp(c,10),dp(c,12),dp(c,10)); b.setMinimumHeight(dp(c,44));
        if(icon!=null) { Icon glyph=new Icon(c,icon); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(c,22),dp(c,22)); p.rightMargin=caption.isEmpty()?0:dp(c,7); b.addView(glyph,p); }
        if(!caption.isEmpty()) b.addView(text(c,caption,14,TEXT)); b.setContentDescription(caption); focus(b); b.setOnClickListener(v->action.run()); return b;
    }
    /** The entire episode row represents progress; the focus border stays above the fill. */
    public static final class ProgressRow extends LinearLayout {
        private int progress;
        public ProgressRow(Context context){super(context);}
        public void setProgress(int value){progress=Math.max(0,Math.min(1000,value));refreshBackground();}
        public int getProgress(){return progress;}
        void refreshBackground(){setBackground(new ProgressBackground(getContext(),progress,hasFocus()));}
    }
    private static final class ProgressBackground extends android.graphics.drawable.Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path clip=new Path();
        private final int progress;private final float radius,stroke;private final boolean focused;
        ProgressBackground(Context c,int progress,boolean focused){this.progress=progress;this.focused=focused;radius=dp(c,12);stroke=dp(c,2);}
        @Override public void draw(Canvas canvas){Rect b=getBounds();RectF bounds=new RectF(b);paint.setStyle(Paint.Style.FILL);paint.setColor(focused?0xFF263445:PANEL);canvas.drawRoundRect(bounds,radius,radius,paint);
            clip.reset();clip.addRoundRect(bounds,radius,radius,Path.Direction.CW);int save=canvas.save();canvas.clipPath(clip);paint.setColor(focused?0xFF235B92:0xFF174F86);canvas.drawRect(b.left,b.top,b.left+b.width()*progress/1000f,b.bottom,paint);canvas.restoreToCount(save);
            if(focused){bounds.inset(stroke/2,stroke/2);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setColor(ACCENT);canvas.drawRoundRect(bounds,radius,radius,paint);}
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    public static class RoundedCard extends FrameLayout {
        private final Path clip=new Path();
        public RoundedCard(Context context){super(context);}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);clip.reset();float radius=dp(getContext(),8);clip.addRoundRect(new RectF(getPaddingLeft(),getPaddingTop(),w-getPaddingRight(),h-getPaddingBottom()),radius,radius,Path.Direction.CW);}
        @Override protected void dispatchDraw(Canvas canvas){int saved=canvas.save();canvas.clipPath(clip);super.dispatchDraw(canvas);canvas.restoreToCount(saved);}
    }
    public static class PosterCard extends RoundedCard {
        public PosterCard(Context context){super(context);setClipToOutline(true);}
        @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec);int height=Math.round(width*1.5f);super.onMeasure(widthSpec,MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));}
    }
    public static class EpisodeCard extends RoundedCard {
        public EpisodeCard(Context context){super(context);setClipToOutline(true);}
        @Override protected void onMeasure(int widthSpec,int heightSpec){int height=Math.round(MeasureSpec.getSize(widthSpec)*9f/16f);super.onMeasure(widthSpec,MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));}
    }
    public static class Icon extends View {
        private final String kind;private int tint=TEXT;public void tint(int color){tint=color;invalidate();} private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        public Icon(Context c,String kind) { super(c); this.kind=kind; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        protected void onDraw(Canvas actual) {
            super.onDraw(actual); actual.save();float edge=Math.min(getWidth()-getPaddingLeft()-getPaddingRight(),getHeight()-getPaddingTop()-getPaddingBottom());actual.translate(getPaddingLeft()+(getWidth()-getPaddingLeft()-getPaddingRight()-edge)/2f,getPaddingTop()+(getHeight()-getPaddingTop()-getPaddingBottom()-edge)/2f);actual.scale(edge/24f,edge/24f); paint.setColor(tint); paint.setStrokeWidth(1.8f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); paint.setStyle(Paint.Style.STROKE);
            switch(kind) {
                case "audio": actual.drawLine(3,9,7,9,paint);actual.drawLine(3,15,7,15,paint);actual.drawLine(3,9,3,15,paint);actual.drawLine(7,9,12,5,paint);actual.drawLine(7,15,12,19,paint);actual.drawLine(12,5,12,19,paint);actual.drawArc(12,5,23,19,-65,130,false,paint);break;
                case "remote": actual.drawRoundRect(7,2,17,22,3,3,paint);actual.drawCircle(12,5,1,paint);actual.drawCircle(12,12,3,paint);actual.drawPoint(10,18,paint);actual.drawPoint(14,18,paint);break;
                case "puzzle": {Path p=new Path();p.moveTo(4,5);p.lineTo(9,5);p.cubicTo(7,0,16,0,14,5);p.lineTo(19,5);p.lineTo(19,10);p.cubicTo(24,8,24,17,19,15);p.lineTo(19,20);p.lineTo(14,20);p.cubicTo(16,15,7,15,9,20);p.lineTo(4,20);p.lineTo(4,15);p.cubicTo(9,17,9,8,4,10);p.close();actual.drawPath(p,paint);break;}
                case "speed": actual.drawArc(3,4,21,22,180,180,false,paint);actual.drawLine(12,14,17,8,paint);actual.drawCircle(12,14,1.5f,paint);actual.drawLine(5,19,19,19,paint);break;
                case "ascending": case "descending": {boolean up=kind.equals("ascending");actual.drawLine(12,4,12,20,paint);actual.drawLine(6,up?10:14,12,up?4:20,paint);actual.drawLine(18,up?10:14,12,up?4:20,paint);break;}
                case "list": for(int y=5;y<=19;y+=7){actual.drawPoint(3,y,paint);actual.drawLine(7,y,21,y,paint);}break;
                case "thumbnail": actual.drawRoundRect(3,4,21,20,2,2,paint);actual.drawLine(3,16,9,10,paint);actual.drawLine(9,10,15,16,paint);actual.drawLine(15,16,18,13,paint);actual.drawLine(18,13,21,16,paint);break;
                case "chevron": actual.drawLine(8,5,16,12,paint);actual.drawLine(16,12,8,19,paint);break;
                case "play": triangle(actual,7,4,20,12,7,20); break;
                case "pause": actual.drawLine(8,5,8,19,paint); actual.drawLine(16,5,16,19,paint); break;
                case "next": triangle(actual,4,5,15,12,4,19); actual.drawLine(19,5,19,19,paint); break;
                case "previous": triangle(actual,20,5,9,12,20,19); actual.drawLine(5,5,5,19,paint); break;
                case "forward": triangle(actual,3,6,11,12,3,18); triangle(actual,12,6,20,12,12,18); break;
                case "rewind": triangle(actual,21,6,13,12,21,18); triangle(actual,12,6,4,12,12,18); break;
                case "opening": triangle(actual,3,5,13,12,3,19); actual.drawLine(16,5,16,19,paint); actual.drawLine(20,5,20,19,paint); break;
                case "ending": actual.drawLine(4,5,4,19,paint); actual.drawLine(8,5,8,19,paint); triangle(actual,12,5,22,12,12,19); break;
                case "heart": case "heart-filled": {
                    if(kind.equals("heart-filled"))paint.setStyle(Paint.Style.FILL);
                    Path p=new Path(); p.moveTo(12,21); p.cubicTo(0,13,1,3,7,3); p.cubicTo(10,3,12,6,12,6); p.cubicTo(12,6,14,3,17,3); p.cubicTo(23,3,24,13,12,21); actual.drawPath(p,paint); break; }
                case "search": actual.drawCircle(10,10,6,paint); actual.drawLine(15,15,21,21,paint); break;
                case "voice": actual.drawRoundRect(9,3,15,14,3,3,paint); actual.drawArc(6,8,18,18,0,180,false,paint); actual.drawLine(12,18,12,22,paint); break;
                case "calendar": actual.drawRoundRect(3,5,21,21,2,2,paint); actual.drawLine(3,10,21,10,paint); actual.drawLine(8,2,8,7,paint); actual.drawLine(16,2,16,7,paint); actual.drawPoint(8,14,paint); actual.drawPoint(12,14,paint); actual.drawPoint(16,14,paint); break;
                case "home": { Path p=new Path(); p.moveTo(2,11); p.lineTo(12,3); p.lineTo(22,11); p.moveTo(5,9); p.lineTo(5,21); p.lineTo(19,21); p.lineTo(19,9); actual.drawPath(p,paint); break; }
                case "grid": for(int x=3;x<=14;x+=11)for(int y=3;y<=14;y+=11)actual.drawRoundRect(x,y,x+7,y+7,1,1,paint); break;
                case "filter": actual.drawLine(3,6,21,6,paint); actual.drawLine(6,12,18,12,paint); actual.drawLine(9,18,15,18,paint); break;
                case "close": actual.drawLine(6,6,18,18,paint); actual.drawLine(18,6,6,18,paint); break;
                case "retry": actual.drawArc(4,4,20,20,40,300,false,paint); actual.drawLine(20,4,20,10,paint); actual.drawLine(14,10,20,10,paint); break;
                case "server": actual.drawRoundRect(3,3,21,10,2,2,paint); actual.drawRoundRect(3,14,21,21,2,2,paint); actual.drawPoint(17,6,paint); actual.drawPoint(17,17,paint); break;
                case "settings": {Path gear=new Path();for(int n=0;n<48;n++){double a=n*Math.PI/24-Math.PI/2;float radius=(n%6==1||n%6==2||n%6==3)?10:7.8f;float x=12+(float)Math.cos(a)*radius,y=12+(float)Math.sin(a)*radius;if(n==0)gear.moveTo(x,y);else gear.lineTo(x,y);}gear.close();actual.drawPath(gear,paint);actual.drawCircle(12,12,3.2f,paint);break;}
                default: actual.drawCircle(12,12,8,paint); actual.drawLine(12,7,12,12,paint); actual.drawPoint(12,17,paint);
            }
            actual.restore();
        }
        private void triangle(Canvas c,float x1,float y1,float x2,float y2,float x3,float y3) { Path p=new Path(); p.moveTo(x1,y1); p.lineTo(x2,y2); p.lineTo(x3,y3); p.close(); c.drawPath(p,paint); }
    }
}


