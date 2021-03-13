package com.meiresearch.android.plotprojects;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
// import android.support.v4.app.NotificationCompat;
import androidx.core.app.NotificationCompat;
import org.appcelerator.kroll.common.Log;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiRHelper;
import android.graphics.Color;

// PlotProjects required imports
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.plotprojects.retail.android.NotificationTrigger;
import com.plotprojects.retail.android.FilterableNotification;
import com.plotprojects.retail.android.Geotrigger;
import com.plotprojects.retail.android.OpenUriReceiver;
import com.plotprojects.retail.android.Plot;
import com.plotprojects.retail.android.PlotConfiguration;
import com.meiresearch.android.plotprojects.GeotriggerBatches.GeotriggersAndId;


public final class LocationForegroundService extends Service
{
	/** The tag name to be used by the Log() method. Will prefix log message with this tag name. */
	private static final String TAG = "EMALocationService";

	/** Stores a reference to this service if running. Set to null if service never started or not currently running. */
	private static LocationForegroundService instance;

	/** Binder object providing access to this service via the onBind() method call. Created dynamically. */
	private LocationForegroundService.Binder binder;

    // Plot specific properties
    	private static Boolean isEnabled = false;
    	private static String version = "0.00.00";
    	private static Boolean isGeoTriggerHandlerEnabled = false;

	/** Called when the Android OS creates this service. */
	@Override
	public void onCreate()
	{
		Log.i(TAG, "onCreate() called.");
        super.onCreate();

		// Keep a reference to this service instance to be made available via the static getInstance() method.
		LocationForegroundService.instance = this;

        // Put this service into the foreground.
		int notiIcon = android.R.drawable.btn_star;
		int notiTranIcon = android.R.drawable.btn_star;
		try{

			notiIcon = TiRHelper.getApplicationResource("drawable.twotone_near_me_black");
			notiTranIcon = TiRHelper.getApplicationResource("drawable.twotone_near_me_black");

		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}

        try {
//TODO: *** Configure the notification here. ***
            // Set up the foreground service's status bar notification.
            final int NOTIFICATION_ID = 1235;	// <- Set this to a unique ID.
            final String NOTIFICATION_CHANNEL_NAME = "location_service_channel";
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel(
                        NOTIFICATION_CHANNEL_NAME, "EMA Location Service",
                        NotificationManager.IMPORTANCE_LOW);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder notificationBuilder;
            notificationBuilder = new NotificationCompat.Builder(TiApplication.getInstance(), NOTIFICATION_CHANNEL_NAME);
			notificationBuilder.setGroup(NOTIFICATION_CHANNEL_NAME);
            notificationBuilder.setContentTitle("PiLR Location Services");
            notificationBuilder.setContentText("Location Services are running");
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    			notificationBuilder.setSmallIcon(notiTranIcon);
    			notificationBuilder.setColor(Color.parseColor("#1A66A4"));
			} else {
    			notificationBuilder.setSmallIcon(notiIcon);
			}
            // Set up an intent to launch/resume the app when the above notification is tapped on.
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            launchIntent.setPackage(null);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, launchIntent, 0));

            // Enable this service's foreground state.
            startForeground(NOTIFICATION_ID, notificationBuilder.build());

            //TODO: *** Put your code "here" now that the service has started. ***
//			new Handler().postDelayed( new Runnable() {
//				@Override
//				public void run() {
//					// Magic here
//					refresh();
//				}
//			}, 5000); // Millisecond 5000 = 5 sec

        } catch (Exception ex) {
            Log.e(TAG, "Failed to put service into the foreground.", ex);
        }
	}

	/** Called when the Android OS is about to destroy this service. */
	@Override
	public void onDestroy()
	{
		Log.i(TAG, "onDestroy() called.");

		// Remove reference to this service now that it's destroyed. This means static getInstance() will return null.
		LocationForegroundService.instance = null;

//TODO: *** The service has stopped. Do your code cleanup here. ***
	}

	/**
	 * Called when the service has just been started.
	 * @param intent The intent used to start this service. Can be null.
	 * @param flags Provides additional flags such as START_FLAG_REDELIVERY, START START_FLAG_RETRY, etc.
	 * @param startId Unique integer ID to be used by stopSelfResult() method, if needed.
	 * @return Returns a "START_*" constant indicating how the Android OS should handle the started service.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		return Service.START_STICKY;
	}

	/**
	 * Called when a client of this service wants direct access to this service via a binder object.
	 * @param intent The intent that was used to bind this service.
	 * @return Returns a binder object used to access this service.
	 */
	@Override
	public IBinder onBind(Intent intent)
	{
		if (this.binder == null) {
			this.binder = new LocationForegroundService.Binder(this);
		}
		return this.binder;
	}

	/**
	 * Starts this service, if not already running.
	 * @return
	 * Returns true if service was started or already running.
	 * Returns false if a startup error occurred which can happen if "FOREGROUND_SERVICE" permission is missing.
	 */
	public static boolean start()
	{
        Log.d(TAG, "start() called");

		// Do not continue if this service is already running.
		if (LocationForegroundService.instance != null) {
            Log.d(TAG, "instance != null");
			return true;
		}

		// Start the service.
		boolean wasStarted = false;
		try {
			TiApplication context = TiApplication.getInstance();
			Intent serviceIntent = new Intent(context, LocationForegroundService.class);
			if (Build.VERSION.SDK_INT >= 26) {
                Log.d(TAG, "sdk >= 26");
				context.startForegroundService(serviceIntent);
			} else {
                Log.d(TAG, "startService");
				context.startService(serviceIntent);
			}
			wasStarted = true;
		} catch (Exception ex) {
			Log.e(TAG, "Failed to start service.", ex);
		}
		return wasStarted;
	}

	/** Stop this service, if currently running. */
	public static void stop()
	{
		// Fetch the service instance.
		LocationForegroundService thisService = LocationForegroundService.instance;
		if (thisService == null) {
			return;
		}

		// Stop this service.
		try {
			thisService.stopSelf();

		} catch (Exception ex) {
			Log.e(TAG, "Failed to stop service.", ex);
		}
	}

	/**
	 * Gets a reference to the one and only LocationForegroundService instance created by the Android OS, if running.
	 * @return Returns a reference to this service if running. Returns null if not running.
	 */
	public static LocationForegroundService getInstance()
	{
		return LocationForegroundService.instance;
	}

    // Plot Projects Service Methods
	public static void initPlot() {
        Log.i(TAG, "initPlot() called");

        if(isRunning()) {
    		// put module init code that needs to run when the application is created
    		TiApplication appContext = TiApplication.getInstance();

    		Activity activity = appContext.getCurrentActivity();

    		SettingsUtil.setGeotriggerHandlerEnabled(true);
    		Plot.init(activity);

    		isEnabled = Plot.isEnabled();
    		isGeoTriggerHandlerEnabled = SettingsUtil.isGeotriggerHandlerEnabled();
    		version = Plot.getVersion();


    		Log.d(TAG, "Plot Version is - " + version);
    		Log.d(TAG, "Is Plot Enabled? - " + isEnabled.toString());
    		Log.d(TAG, "Is GeotriggerHandler Enabled? - " + isGeoTriggerHandlerEnabled.toString());
        }
	}

	public static void enable() {
		Plot.enable();
	}

	public static void disable() {
		Plot.disable();
	}

	public static boolean isEnabled() {
		return Plot.isEnabled();
	}

	public static String getVersion() {
		return Plot.getVersion();
	}

	public static void mailDebugLog() {
		Plot.mailDebugLog();
	}

//	public static HashMap popFilterableNotifications() {
//		NotificationsAndId notificationsAndId = NotificationBatches.popBatch();
//
//		HashMap<String, Object> result = new HashMap<String, Object>();
//		result.put("filterId", notificationsAndId.getId());
//		result.put("notifications", JsonUtil.notificationsToMap(notificationsAndId.getNotifications()));
//		return result;
//	}

	public static void sendNotifications(HashMap batch) {
		Log.d(TAG, "sendNotifications");
		String filterId = (String) batch.get("filterId");
		List<FilterableNotification> notifications = NotificationBatches.getBatch(filterId);

		Object[] jsonNotifications = (Object[]) batch.get("notifications");
		List<FilterableNotification> notificationsToSend = JsonUtil.getNotifications(jsonNotifications, notifications);
		NotificationBatches.sendBatch(filterId, notificationsToSend);
	}

	public static HashMap popGeotriggers() {
		GeotriggersAndId geotriggersAndId = GeotriggerBatches.popBatch();

		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("handlerId", geotriggersAndId.getId());
		result.put("geotriggers", JsonUtil.geotriggersToMap(geotriggersAndId.getGeotriggers()));
		return result;
	}

	public static void markGeotriggersHandled(HashMap batch) {
		Log.d(TAG, "markGeotriggersHandled - A");

		String handlerId = (String) batch.get("handlerId");
		List<Geotrigger> geotriggers = GeotriggerBatches.getBatch(handlerId);

		Log.d(TAG, "markGeotriggersHandled - B");

		Object[] jsonGeotriggers = (Object[]) batch.get("geotriggers");
		List<Geotrigger> geotriggersHandled = JsonUtil.getGeotriggers(jsonGeotriggers, geotriggers);
		GeotriggerBatches.sendBatch(handlerId, geotriggersHandled);

		Log.d(TAG, "markGeotriggersHandled - C");
	}

	public static void setStringSegmentationProperty(String property, String value) {
		Plot.setStringSegmentationProperty(property, value);
	}

	public static void setBooleanSegmentationProperty(String property, boolean value) {
		Plot.setBooleanSegmentationProperty(property, value);
	}

	public static void setIntegerSegmentationProperty(String property, int value) {
		Plot.setLongSegmentationProperty(property, value);
	}

	public static void setDoubleSegmentationProperty(String property, double value) {
		Plot.setDoubleSegmentationProperty(property, value);
	}

	public static void setDateSegmentationProperty(String property, Date value) {
		Plot.setDateSegmentationProperty(property, value.getTime() / 1000);
	}

	public static HashMap[] getLoadedNotifications() {
		return JsonUtil.notificationTriggersToMap(new ArrayList(Plot.getLoadedNotifications()));
	}

	public static HashMap[] getLoadedGeotriggers() {
		Log.d(TAG, "getLoadedGeotriggers");
		return JsonUtil.geotriggersToMap(new ArrayList(Plot.getLoadedGeotriggers()));
	}

	public static HashMap[] getSentNotifications() {
		return JsonUtil.sentNotificationsToMap(new ArrayList(Plot.getSentNotifications()));
	}

	public static HashMap[] getSentGeotriggers() {
		Log.d(TAG, "getSentGeotriggers");
		return JsonUtil.sentGeotriggersToMap(new ArrayList(Plot.getSentGeotriggers()));
	}

	public static void clearSentNotifications() {
		Plot.clearSentNotifications();
	}

	public static void clearSentGeotriggers() {
		Plot.clearSentGeotriggers();
	}

	/** Binder providing external access to the LocationForegroundService object. */
	private static class Binder extends android.os.Binder
	{
		/** Reference to the service this binder provides access to. */
		private LocationForegroundService service;

		/**
		 * Creates a new binder providing access to the given service.
		 * @param service Reference to the service this binder will provide access to.
		 */
		public Binder(LocationForegroundService service)
		{
			this.service = service;
		}

		/**
		 * Gets the service this binder provides access to.
		 * @return Returns the service this binder provides access to.
		 */
		Service getService()
		{
			return this.service;
		}
	}

    /**
     * is the foreground service running?
     * @return boolean
     */
	public static boolean isRunning(){
        if (LocationForegroundService.instance != null) {
            return true;
        }
        return false;
    }
}
