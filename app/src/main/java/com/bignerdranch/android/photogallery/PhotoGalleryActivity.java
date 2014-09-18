package com.bignerdranch.android.photogallery;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class PhotoGalleryActivity extends SingleFragmentActivity {

  @Override
  protected Fragment createFragment() {
    return new PhotoGalleryFragment();
  }
}
