package com.ucoon.hezjmyimageloader.Activity;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.ucoon.hezjmyimageloader.R;
import com.ucoon.hezjmyimageloader.util.ImageLoader;
import com.ucoon.hezjmyimageloader.util.Images;

/**
 * Created by ZongJie on 2017/1/21.
 */

public class ListImgsFragment extends Fragment {

    private GridView gridView;
    private String[] mUrlStrs = Images.imageThumbUrls;
    private ImageLoader imageLoader;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageLoader = ImageLoader.getInstance(3, ImageLoader.Type.LIFO);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_imgs,container,false);
        gridView = (GridView) view.findViewById(R.id.id_gridview);
        setUpAdapter();
        return view;
    }

    private void setUpAdapter() {
        if (getActivity() == null || gridView == null){
            return;
        }
        if (mUrlStrs != null){
            gridView.setAdapter(new ListImgItemAdapter(getActivity(), 0, mUrlStrs));
        }else {
            gridView.setAdapter(null);
        }
    }

    private class ListImgItemAdapter extends ArrayAdapter<String>{

        public ListImgItemAdapter(Context context, int resource, String[] datas) {
            super(getActivity(), 0, datas);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.item_fragment_list_imgs,parent,false);
            }
            ImageView imageView = (ImageView) convertView.findViewById(R.id.id_img);
            imageView.setImageResource(R.drawable.pictures_no);
            imageLoader.loadImage(getItem(position),imageView,true);
            return convertView;
        }
    }
}
