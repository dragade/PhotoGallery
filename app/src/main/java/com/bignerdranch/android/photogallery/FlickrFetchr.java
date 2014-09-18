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
  private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
  private static final String API_KEY = "b0a574d9e231935b3dd06f6186a10ab7";
  private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
  private static final String PARAM_EXTRAS = "extras";
  private static final String EXTRA_SMALL_URL = "url_s";

  private static final String XML_PHOTO = "photo";

  byte[] getUrlBytes(String urlSpec) throws IOException {
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

  public List<GalleryItem> fetchItems() {
    try {
      String url = Uri.parse(ENDPOINT).buildUpon()
          .appendQueryParameter("method", METHOD_GET_RECENT)
          .appendQueryParameter("api_key", API_KEY)
          .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
          .build().toString();
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
    return Collections.emptyList();
  }

  ArrayList<GalleryItem> parseItems(XmlPullParser parser)
      throws XmlPullParserException, IOException {
    ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
    int eventType = parser.next();
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG &&
          XML_PHOTO.equals(parser.getName())) {
        String id = parser.getAttributeValue(null, "id");
        String caption = parser.getAttributeValue(null, "title");
        String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);
        GalleryItem item = new GalleryItem();
        item.setId(id);
        item.setCaption(caption);
        item.setUrl(smallUrl);
        items.add(item);
      }
      eventType = parser.next();
    }
    return items;
  }
}
