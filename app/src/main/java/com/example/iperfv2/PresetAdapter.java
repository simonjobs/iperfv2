package com.example.iperfv2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PresetAdapter extends RecyclerView.Adapter {

    private final String TAG = getClass().getSimpleName();

    public PresetAdapter(ListItemClickListener mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
    }

    interface ListItemClickListener{
        void onListItemClick(int position);
    }

    private List<String> stringList = Collections.synchronizedList(new ArrayList<String>());
    final private ListItemClickListener mOnClickListener;

    public void addString(String string){
        int currentPosition = stringList.size();
        stringList.add(currentPosition, string);
        notifyItemChanged(currentPosition);
    }

    public List<String> getStringList(){
        return stringList;
    }

    public void clear(){
        stringList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.preset_item,parent,false);
        return new ItemView(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ItemView)holder).mTvText.setText(stringList.get(position));

    }

    @Override
    public int getItemCount() {
        return stringList.size();
    }

    public class ItemView extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView mTvText;
        RelativeLayout parentLayout;

        public ItemView(@NonNull View itemView) {
            super(itemView);
            mTvText = itemView.findViewById(R.id.pr_item);
            parentLayout = itemView.findViewById(R.id.parent_layout);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            mOnClickListener.onListItemClick(position);
        }
    }
}
