package pe.animetv;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.leanback.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;

/** TV focus and scrolling belong to Leanback, not nested phone ScrollViews. */
final class TvRowsView extends FrameLayout {
    final VerticalGridView grid;
    final RowModel model;
    TvRowsView(Context context){
        super(context);setClipChildren(true);setClipToPadding(false);
        grid=new VerticalGridView(context);configure(grid);grid.setNumColumns(1);
        grid.setPadding(0,0,0,0);grid.setWindowAlignmentOffsetPercent(65);
        addView(grid,new FrameLayout.LayoutParams(-1,-1));model=new RowModel(context,grid,false);
    }
    static void configure(BaseGridView grid){
        grid.setClipChildren(false);grid.setClipToPadding(false);grid.setItemAnimator(null);
        grid.setWindowAlignment(BaseGridView.WINDOW_ALIGN_BOTH_EDGE);
        grid.setWindowAlignmentOffsetPercent(50);grid.setItemAlignmentOffsetPercent(50);
        // Leanback uses aligned focus scrolling by default.
        grid.setFocusable(true);grid.setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    }
    void focusContent(){grid.requestFocus();}
    void restoreFocus(View target){
        int index=model.indexContaining(target);if(index<0){focusContent();return;}
        grid.setSelectedPosition(index,new ViewHolderTask(){public void run(RecyclerView.ViewHolder holder){
            View row=model.items.get(index);if(row instanceof Rail)((Rail)row).restoreFocus(target);else target.requestFocus();
        }});
    }
    static final class Rail extends FrameLayout {
        final HorizontalGridView grid;final RowModel model;private int measuredWidth=-1,measuredRevision=-1,measuredHeight;private float measuredDensity;
        Rail(Context context){
            super(context);setClipChildren(false);setClipToPadding(false);
            grid=new HorizontalGridView(context);configure(grid);grid.setNumRows(1);
            grid.setWindowAlignmentOffsetPercent(0);grid.setItemAlignmentOffsetPercent(0);
            grid.setPadding(TvUi.dp(context,8),TvUi.dp(context,8),TvUi.dp(context,8),TvUi.dp(context,12));
            grid.setHorizontalSpacing(TvUi.dp(context,16));addView(grid,new FrameLayout.LayoutParams(-1,-2));
            model=new RowModel(context,grid,true);
        }
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            int height=0,available=Math.max(0,MeasureSpec.getSize(widthSpec)-grid.getPaddingLeft()-grid.getPaddingRight());
            if(available!=measuredWidth||model.revision!=measuredRevision||measuredDensity!=getResources().getDisplayMetrics().scaledDensity){for(View item:model.items){ViewGroup.LayoutParams p=item.getLayoutParams();int w=p!=null&&p.width>0?p.width:available;int h=p!=null&&p.height>0?p.height:0;item.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,h>0?MeasureSpec.EXACTLY:MeasureSpec.UNSPECIFIED));height=Math.max(height,item.getMeasuredHeight());}
            measuredDensity=getResources().getDisplayMetrics().scaledDensity;measuredWidth=available;measuredRevision=model.revision;measuredHeight=height;}else height=measuredHeight;grid.getLayoutParams().height=height+grid.getPaddingTop()+grid.getPaddingBottom();super.onMeasure(widthSpec,heightSpec);
        }
        void restoreFocus(View target){int index=model.indexContaining(target);if(index>=0)grid.setSelectedPosition(index,new ViewHolderTask(){public void run(RecyclerView.ViewHolder holder){target.requestFocus();}});}
    }
    /** Construction model keeps row identities while RecyclerView attaches only visible rows. */
    static final class RowModel extends LinearLayout {
        final List<View> items=new ArrayList<>();final BaseGridView grid;final boolean horizontal;
        private final RowsAdapter adapter;private boolean pending;int revision;int contentInsets;
        RowModel(Context context,BaseGridView grid,boolean horizontal){super(context);this.grid=grid;this.horizontal=horizontal;adapter=new RowsAdapter();adapter.setHasStableIds(true);grid.setAdapter(adapter);}
        @Override public void addView(View view,int index,ViewGroup.LayoutParams params){
            if(contentInsets>0&&!"detail-background".equals(view.getTag())){LinearLayout.LayoutParams inset=params instanceof LinearLayout.LayoutParams?new LinearLayout.LayoutParams((LinearLayout.LayoutParams)params):params instanceof ViewGroup.MarginLayoutParams?new LinearLayout.LayoutParams((ViewGroup.MarginLayoutParams)params):new LinearLayout.LayoutParams(params);inset.leftMargin=contentInsets;inset.rightMargin=contentInsets;params=inset;}
            view.setLayoutParams(params);items.add(index<0?items.size():index,view);changed();
        }
        @Override public void removeView(View view){if(items.remove(view))changed();}
        @Override public void removeAllViews(){items.clear();changed();}
        private void changed(){revision++;if(!pending){pending=true;grid.post(()->{pending=false;adapter.notifyDataSetChanged();});}}
        int indexContaining(View target){for(int n=0;n<items.size();n++)if(contains(items.get(n),target))return n;return -1;}
        private boolean contains(View view,View target){if(view==target)return true;if(view instanceof Rail&&((Rail)view).model.indexContaining(target)>=0)return true;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++)if(contains(group.getChildAt(n),target))return true;}return false;}
        private final class RowsAdapter extends RecyclerView.Adapter<RowHolder>{
            public int getItemCount(){return items.size();}
            public long getItemId(int position){return System.identityHashCode(items.get(position));}
            public RowHolder onCreateViewHolder(ViewGroup parent,int type){FrameLayout frame=new FrameLayout(getContext());frame.setClipChildren(false);frame.setClipToPadding(false);frame.setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);frame.setLayoutParams(new RecyclerView.LayoutParams(horizontal?-2:-1,-2));return new RowHolder(frame);}
            public void onBindViewHolder(RowHolder holder,int position){View view=items.get(position);if(view.getParent() instanceof ViewGroup)((ViewGroup)view.getParent()).removeView(view);holder.frame.removeAllViews();ViewGroup.LayoutParams p=view.getLayoutParams();FrameLayout.LayoutParams size=new FrameLayout.LayoutParams(horizontal&&p!=null?p.width:-1,p!=null&&p.height>0?p.height:-2);if(!horizontal&&p instanceof ViewGroup.MarginLayoutParams){ViewGroup.MarginLayoutParams margins=(ViewGroup.MarginLayoutParams)p;size.setMargins(margins.leftMargin,margins.topMargin,margins.rightMargin,margins.bottomMargin);}holder.frame.addView(view,size);}
            public void onViewRecycled(RowHolder holder){holder.frame.removeAllViews();}
        }
    }
    private static final class RowHolder extends RecyclerView.ViewHolder {final FrameLayout frame;RowHolder(FrameLayout frame){super(frame);this.frame=frame;}}
}




