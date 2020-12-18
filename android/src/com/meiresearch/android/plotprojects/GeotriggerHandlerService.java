/**
 * Copyright 2016 Floating Market B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meiresearch.android.plotprojects;

import com.plotprojects.retail.android.GeotriggerHandlerUtil;
import com.plotprojects.retail.android.GeotriggerHandlerUtil.Batch;
import com.plotprojects.retail.android.Geotrigger;
import com.plotprojects.retail.android.GeotriggerHandlerBroadcastReceiver;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiProperties;

import androidx.core.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.media.RingtoneManager;
import android.app.PendingIntent;
import android.net.Uri;
import android.content.Intent;
import android.content.Context;

import android.util.Log;
import ti.modules.titanium.android.TiBroadcastReceiver;
import android.content.BroadcastReceiver;
import android.app.AlarmManager;
import android.location.Location;

import android.widget.Toast;
import android.R;
import java.util.List;

import org.json.*;


public class GeotriggerHandlerService extends BroadcastReceiver {

    private static final String TAG = "GeotriggerHandlerService";

    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Geofence triggered!");

        if (GeotriggerHandlerUtil.isGeotriggerHandlerBroadcastReceiverIntent(context, intent)) {
            GeotriggerHandlerUtil.Batch batch = GeotriggerHandlerUtil.getBatch(intent, context);
            if (batch != null) {
                List<Geotrigger> triggers = batch.getGeotriggers();
                Geotrigger t;

                for(int i = 0; i < triggers.size(); i++){
                    t = triggers.get(i);
                    //Log.d("Handled Geotrigger", triggers[i].getGeofenceLatitude(), triggers[i].getGeofenceLongitude(), triggers[i].getName(), triggers[i].getTrigger());
                    Log.d(TAG, "Handled Geotrigger id" + t.getId());
                    Log.d(TAG, "Handled Geotrigger name" + t.getName());//t.getGeofenceLatitude());
                    //Log.d(TAG, "    ", t.getGeofenceLongitude());
                    //Log.d(TAG, "    ", t.getName());
                    Log.d(TAG, "   getTrigger " + t.getTrigger());
                    Log.d(TAG, "handled geotrigger --end--");

                    Long tsLong = System.currentTimeMillis()/1000;
                    String ts = tsLong.toString();
                    String geofenceName = t.getName();

                    if(EMAFilterRegion.regionAllowed(geofenceName)){
                        // for healthkick, we have to test regions and ensure consistent 'generic' naming of a region.
                        if(geofenceName.indexOf("generic,") == 0){
                            geofenceName = "generic";
                        }

                        savePersistentData(ts, geofenceName, t.getId(), t.getTrigger());
                        sendNotification(t.getTrigger());
                        break;
                    }
                }

                batch.markGeotriggersHandled(triggers);
            }
        }
    }

    // save to Ti.app.properties so the Titanium app can access this data later.
    private static void savePersistentData(String timestamp, String name, String id, String direction){
        TiProperties props = TiApplication.getInstance().getAppProperties();
        String propName = "plot.surveyTriggered";

        try{
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("detection_timestamp", timestamp);
            jsonObj.put("geotrigger_id", id);
            jsonObj.put("geotrigger_name", name);
            jsonObj.put("geotrigger_direction", direction);

            EMADataAccess.appendToJsonArray(propName, jsonObj);
        } catch(JSONException e){
            Log.e(TAG, "error getting notification details");
            e.printStackTrace();
        }
    }

    private static void sendNotification(String direction){

        String notificationTitle = EMADataAccess.getStringProperty("plot.notificationTitle." + direction);
        String notificationText = EMADataAccess.getStringProperty("plot.notificationText." + direction);
        int notificationId = 201;

        String notifyChannelName = "EMA Plot Location";
        String notifyChannelDesc = "Location based notifications";
        String groupName = "ema_plot_loc";

        long scheduleTime = System.currentTimeMillis() + 2 * 60 * 1000;
        Context context =  TiApplication.getInstance().getApplicationContext();

        //create notification channel
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("12", notifyChannelName, importance);
        channel.setDescription(notifyChannelDesc);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder
                (context, "12")
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setGroupSummary(true)
                .setGroup(groupName)
                .setStyle( new NotificationCompat.InboxStyle())
                .setSmallIcon(R.drawable.btn_star)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setShowWhen(true)
                .setWhen(scheduleTime);
                //.setTimeoutAfter(expireTime-scheduleTime)
                //.setAutoCancel(true);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(alarmSound);
/*
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "12")
                .setSmallIcon(R.drawable.btn_star)
                .setContentTitle("Plot Projects Geotrigger")
                .setContentText(t.getTrigger());
*/
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        launchIntent.setPackage(null);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        builder.setContentIntent(PendingIntent.getActivity(context, 0, launchIntent, 0));

        //notificationManager.notify(1, builder.build());

        //Creates the notification intent with extras
        Intent notificationIntent = new Intent(context, EMANotificationBroadcastReceiver.class);
        notificationIntent.putExtra(EMANotificationBroadcastReceiver.NOTIFICATION_ID, notificationId);
        notificationIntent.putExtra(EMANotificationBroadcastReceiver.NOTIFICATION, builder.build());

        //Creates the pending intent which includes the notificationIntent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, notificationIntent, 0);


        try{
            Log.d(TAG, context.toString());

            AlarmManager am = (AlarmManager)context.getSystemService(TiApplication.ALARM_SERVICE);
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduleTime, pendingIntent);

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    //This is code that can be adapted so that this broadcastreceiver can be managed from titanium.
    //Previously this was a service and not a broadcast receiver, so onStartCommand is irrelevant for instance.
    //I left in here if you desire to extend TiBroadcastReceiver and place the .js code in the platform/android/assets folder instead
//    public GeotriggerHandlerService() {
//        super("plotgeotriggerhandler.js");
//        Log.d(TAG, "GeotriggerHandlerService()");
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.d(TAG, "onStartCommand()");
//        if (GeotriggerHandlerUtil.isGeotriggerHandlerBroadcastReceiverIntent(this,intent)) {
//            GeotriggerHandlerUtil.Batch batch = GeotriggerHandlerUtil.getBatch(intent, this);
//            if (batch != null) {
//                if (SettingsUtil.isGeotriggerHandlerEnabled()) {
//                    GeotriggerBatches.addBatch(batch, this, startId);
//                    return super.onStartCommand(intent, flags, startId);
//                } else {
//                    batch.markGeotriggersHandled(batch.getGeotriggers());
//                }
//            } else {
//                Log.w(TAG, "Unable to obtain batch with geotriggers from intent");
//            }
//        } else {
//            Log.w(TAG, String.format("Received unexpected intent with action: %s", intent.getAction()));
//        }
//        stopSelf(startId);
//        return START_NOT_STICKY;
//    }
//
//    @Override
//    public void onTaskRemoved(Intent rootIntent) {
//
//    }

}
