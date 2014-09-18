package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {
  private static final String TAG = "PhotoGalleryFragment";

  GridView mGridView;
  List<GalleryItem> mItems;
  ThumbnailDownloader<ImageView> mThumbnailThread;
  private FlickrFetchr mFlickrFetchr;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
    setHasOptionsMenu(true);
    updateItems();

    mFlickrFetchr = new FlickrFetchr();
    mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
    mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
      public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
        if (isVisible()) {
          imageView.setImageBitmap(thumbnail);
        }
      } });
    mThumbnailThread.start();
    mThumbnailThread.getLooper();
    Log.i(TAG, "Background thread started");
  }

  public void updateItems() {
    new FetchItemsTask().execute();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
    mGridView = (GridView) v.findViewById(R.id.gridView);
    setupAdapter();
    return v;

  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mThumbnailThread.quit();
    Log.i(TAG, "Background thread destroyed");
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    mThumbnailThread.clearQueue();
  }

  void setupAdapter() {
    if (getActivity() == null || mGridView == null) return;
    if (mItems != null) {
      mGridView.setAdapter(new GalleryItemAdapter(mItems));
    } else {
      mGridView.setAdapter(null);
    }
  }

  private class FetchItemsTask extends AsyncTask<Void,Void,FlickrFetchr.Results> {
    @Override
    protected FlickrFetchr.Results doInBackground(Void... params) {
      Activity activity = getActivity();
      if (activity == null)
        return new FlickrFetchr.Results(Collections.<GalleryItem>emptyList(), 0);

      String query = PreferenceManager.getDefaultSharedPreferences(activity)
          .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
      if (query != null) {
        return mFlickrFetchr.search(query);
      } else {
        return mFlickrFetchr.fetchItems();
      }
    }

    @Override
    protected void onPostExecute(FlickrFetchr.Results results) {
      mItems = results.items;
      setupAdapter();

      //display the toast of num results found
      String msg = String.format("Found %d results", results.total);
      Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }
  }

  private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
    public GalleryItemAdapter(List<GalleryItem> items) {
      super(getActivity(), 0, items);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item, parent, false);
      }
      ImageView imageView = (ImageView)convertView.findViewById(R.id.gallery_item_imageView);
      imageView.setImageResource(R.drawable.izan_and_liani);

      GalleryItem item = getItem(position);
      mThumbnailThread.queueThumbnail(imageView, item.getUrl());
      return convertView;
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.fragment_photo_gallery, menu);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      // Pull out the SearchView
      MenuItem searchItem = menu.findItem(R.id.menu_item_search);
      SearchView searchView = (SearchView)searchItem.getActionView();
      // Get the data from our searchable.xml as a SearchableInfo
      SearchManager searchManager = (SearchManager)getActivity()
          .getSystemService(Context.SEARCH_SERVICE);
      ComponentName name = getActivity().getComponentName();
      SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
      searchView.setSearchableInfo(searchInfo);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    switch (item.getItemId()) {
      case R.id.menu_item_search:
        String query = prefs.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
        getActivity().startSearch(query, true, null, false);

//        getActivity().onSearchRequested();
        return true;
      case R.id.menu_item_clear:
        prefs
            .edit().remove(FlickrFetchr.PREF_SEARCH_QUERY)
//            .putString(FlickrFetchr.PREF_SEARCH_QUERY, null) ??
            .commit();
        updateItems();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
}
