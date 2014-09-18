package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlickrFetchr {
  public static final String TAG = "FlickrFetchr";
  public static final String PREF_SEARCH_QUERY = "searchQuery";

  public static final String PREF_LAST_RESULT_ID = "lastResultId";

  private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
  private static final String API_KEY = "b0a574d9e231935b3dd06f6186a10ab7";
  private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
  private static final String PARAM_EXTRAS = "extras";
  private static final String EXTRA_SMALL_URL = "url_s";
  private static final String METHOD_SEARCH = "flickr.photos.search";
  private static final String PARAM_TEXT = "text";

  private static final String XML_PHOTO = "photo";
  private static final String XML_PHOTOS = "photos";
  private static final String XML_PHOTOS_TOTAL = "total";

  public static class Results {
    public final List<GalleryItem> items;
    public final int total;

    public Results(List<GalleryItem> items, int total) {
      this.items = items;
      this.total = total;
    }
  }

  public byte[] getUrlBytes(String urlSpec) throws IOException {
    URL url = new URL(urlSpec);
    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      InputStream in = connection.getInputStream();
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        return null;
      }
      int bytesRead = 0;
      byte[] buffer = new byte[1024];
      while ((bytesRead = in.read(buffer)) > 0) {
        out.write(buffer, 0, bytesRead);
      }
      out.close();
      return out.toByteArray();
    } finally {
      connection.disconnect();
    }
  }
  public String getUrl(String urlSpec) throws IOException {
    return new String(getUrlBytes(urlSpec));
  }

  public Results downloadGalleryItems(String  url) {
    try {
      String xmlString = getUrl(url);
      Log.i(TAG, "Received xml: " + xmlString);
      XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
      XmlPullParser parser = xmlPullParserFactory.newPullParser();
      parser.setInput(new StringReader(xmlString));
      return parseItems(parser);
    } catch (IOException ioe) {
      Log.e(TAG, "Failed to fetch items", ioe);
    } catch (XmlPullParserException e) {
      Log.e(TAG, "Failed to parse items", e);
    }
    return new Results(Collections.<GalleryItem>emptyList(), 0);
  }

  public Results fetchItems() {
    String url = Uri.parse(ENDPOINT).buildUpon()
        .appendQueryParameter("method", METHOD_GET_RECENT)
        .appendQueryParameter("api_key", API_KEY)
        .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
        .build().toString();
    Log.i(TAG, "Fetching for " + url);
    return downloadGalleryItems(url);
  }

  public Results search(String query) {
    String url = Uri.parse(ENDPOINT).buildUpon()
        .appendQueryParameter("method", METHOD_SEARCH)
        .appendQueryParameter("api_key", API_KEY)
        .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
        .appendQueryParameter(PARAM_TEXT, query)
        .build().toString();
    Log.i(TAG, "Searching for " + url);
    return downloadGalleryItems(url);
  }

  private Results parseItems(XmlPullParser parser)
      throws XmlPullParserException, IOException {
    ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
    int eventType = parser.next();
    int total = 0;
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        if (XML_PHOTOS.equals(parser.getName())) {
          String totalValue = parser.getAttributeValue(null, XML_PHOTOS_TOTAL);
          total = Integer.parseInt(totalValue);
          Log.d(TAG, "There are " + total + " images.");
        }
        else if (XML_PHOTO.equals(parser.getName())) {
          String id = parser.getAttributeValue(null, "id");
          String caption = parser.getAttributeValue(null, "title");
          String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);
          GalleryItem item = new GalleryItem();
          item.setId(id);
          item.setCaption(caption);
          item.setUrl(smallUrl);
          items.add(item);
        }
      }

      eventType = parser.next();
    }
    return new Results(items, total);
  }
}
