package com.bignerdranch.android.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {
  GridView mGridView;
  List<GalleryItem> mItems;
  private static final String TAG = "PhotoGalleryFragment";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
    new FetchItemsTask().execute();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
    mGridView = (GridView) v.findViewById(R.id.gridView);
    setupAdapter();
    return v;

  }

  void setupAdapter() {
    if (getActivity() == null || mGridView == null) return;
    if (mItems != null) {
      ArrayAdapter<GalleryItem> arrayAdapter = new ArrayAdapter<GalleryItem>(getActivity(),
          android.R.layout.simple_gallery_item, mItems);
      mGridView.setAdapter(arrayAdapter);
    } else {
      mGridView.setAdapter(null);
    }
  }

  private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> {
    @Override
    protected List<GalleryItem> doInBackground(Void... params) {
      return new FlickrFetchr().fetchItems();
    }

    @Override
    protected void onPostExecute(List<GalleryItem> items) {
      mItems = items;
      setupAdapter();
    }
  }
}
