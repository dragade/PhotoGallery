package com.bignerdranch.android.photogallery;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ThumbnailDownloader<Token> extends HandlerThread {
  private static final String TAG = "ThumbnailDownloader";
  private static final int MESSAGE_DOWNLOAD = 0;

  Handler mHandler;
  Map<Token, String> requestMap =
      Collections.synchronizedMap(new HashMap<Token, String>());

  Handler mResponseHandler;
  Listener<Token> mListener;

  public interface Listener<Token> {
    void onThumbnailDownloaded(Token token, Bitmap thumbnail);
  }

  private LruCache<String,Bitmap> mImageCache;


  public void setListener(Listener<Token> listener) {
    mListener = listener;
  }

  public ThumbnailDownloader(Handler responseHandler) {
    super(TAG);
    mResponseHandler = responseHandler;
    mImageCache = new LruCache<String, Bitmap>(100);
  }

  @SuppressLint("HandlerLeak")
  @Override
  protected void onLooperPrepared() {
    mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        if (msg.what == MESSAGE_DOWNLOAD) {
          @SuppressWarnings("unchecked")
          Token token = (Token) msg.obj;
          if (requestMap.containsKey(token)) {
//            Log.d(TAG, "Got a request for url: " + requestMap.get(token));
            handleRequest(token);
          }
        }
      }
    };
  }

  public void queueThumbnail(Token token, String url) {
//    Log.i(TAG, "Got a URL: " + url);
    requestMap.put(token, url);
    mHandler
        .obtainMessage(MESSAGE_DOWNLOAD, token)
        .sendToTarget();
  }

  private void handleRequest(final Token token) {
    try {
      final String url = requestMap.get(token);
      if (url == null)
        return;

      //check the cache
      final Bitmap bitmap;
      Bitmap bitmapFromCache = mImageCache.get(url);
      if (bitmapFromCache == null) {
        byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
        bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        mImageCache.put(url, bitmap);
      } else {
        bitmap = bitmapFromCache;
      }

      mResponseHandler.post(new Runnable() {
        public void run() {
          if (!url.equals(requestMap.get(token)))
            return;
          requestMap.remove(token);
          mListener.onThumbnailDownloaded(token, bitmap);
        }
      });
    } catch (IOException ioe) {
      Log.e(TAG, "Error downloading image", ioe);
    }
  }

  public void clearQueue() {
    mHandler.removeMessages(MESSAGE_DOWNLOAD);
    requestMap.clear();
  }
}