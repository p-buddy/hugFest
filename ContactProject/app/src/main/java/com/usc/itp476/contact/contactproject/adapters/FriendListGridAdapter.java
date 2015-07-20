package com.usc.itp476.contact.contactproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.usc.itp476.contact.contactproject.R;

public class FriendListGridAdapter extends BaseAdapter {

    //private Context mContext;
    String[] points;
    Context context;
    int[] imageId;
    boolean mDisplayCheckBox = false;
    private static LayoutInflater inflater = null;
    private static int staticPosition = 0;

    public FriendListGridAdapter(Context mainActivity, String[] prgmNameList, int[] prgmImages, String[] score, Boolean displayCheckBox){
        // TODO Auto-generated constructor stub
        context=mainActivity;
        imageId=prgmImages;
        inflater=(LayoutInflater)context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        points = score;
        mDisplayCheckBox = displayCheckBox;

    }
    @Override
    public int getCount() {
        return points.length;
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public class Holder {
        ImageView img;
        TextView points;
        CheckBox invited;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        staticPosition = position;
        Holder holder = new Holder();
        View rowView;
        rowView = inflater.inflate(R.layout.grid_tile, null);

        holder.img = (ImageView) rowView.findViewById(R.id.imageViewFriend);
        holder.points = (TextView) rowView.findViewById(R.id.imageViewScore);
        holder.invited = (CheckBox) rowView.findViewById(R.id.ckbxInvite);

        holder.img.setImageResource(imageId[position]);

        if(mDisplayCheckBox){
            holder.points.setVisibility(View.GONE);
        }else{
            holder.points.setText( points[position]);
            holder.invited.setVisibility(View.GONE);
        }

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox c = (CheckBox) view.findViewById(R.id.ckbxInvite);
                c.setChecked( !c.isChecked() );
            }
        });

        return rowView;
    }
}