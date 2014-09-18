package com.bignerdranch.android.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class PollService extends IntentService{
  private static final String TAG = "PollService";
  private static final int POLL_INTERVAL = 1000 * 15; // 15 seconds

  public PollService() {
    super(TAG);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    ConnectivityManager cm = (ConnectivityManager)
        getSystemService(Context.CONNECTIVITY_SERVICE);
    @SuppressWarnings("deprecation")
    boolean isNetworkAvailable = cm.getBackgroundDataSetting() && cm.getActiveNetworkInfo() != null;

    if (!isNetworkAvailable) return;

    Log.i(TAG, "Received an intent: " + intent);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String query = prefs.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
    String lastResultId = prefs.getString(FlickrFetchr.PREF_LAST_RESULT_ID, null);
    final FlickrFetchr.Results results;
    if (query != null) {
      results = new FlickrFetchr().search(query);
    } else {
      results = new FlickrFetchr().fetchItems();
    }
    if (results.items.size() == 0)
      return;
    String resultId = results.items.get(0).getId();
    if (!resultId.equals(lastResultId)) {
      Log.i(TAG, "Got a new result: " + resultId);
    } else {
      Log.i(TAG, "Got an old result: " + resultId);
    }
    prefs.edit()
        .putString(FlickrFetchr.PREF_LAST_RESULT_ID, resultId)
        .commit();
  }

  public static void setServiceAlarm(Context context, boolean isOn) {
    Intent intent = new Intent(context, PollService.class);
    PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (isOn) {
      alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), POLL_INTERVAL, pendingIntent);
    } else {
      alarmManager.cancel(pendingIntent);
      pendingIntent.cancel();
    }
  }

  public static boolean isServiceAlarmOn(Context context) {
    Intent i = new Intent(context, PollService.class);
    PendingIntent pi = PendingIntent.getService(
        context, 0, i, PendingIntent.FLAG_NO_CREATE);
    return pi != null;
  }
}
