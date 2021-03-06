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

package com.plotprojects.titanium;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.plotprojects.retail.android.NotificationTrigger;
import com.plotprojects.retail.android.FilterableNotification;
import com.plotprojects.retail.android.Geotrigger;
import com.plotprojects.retail.android.OpenUriReceiver;
import com.plotprojects.retail.android.Plot;
import com.plotprojects.retail.android.PlotConfiguration;
import com.plotprojects.titanium.NotificationBatches.NotificationsAndId;
import com.plotprojects.titanium.GeotriggerBatches.GeotriggersAndId;

@Kroll.module(name="PlotAndroidModule", id="com.plotprojects.ti")
@SuppressWarnings("rawtypes") //required for Kroll
public class PlotAndroidModule extends KrollModule implements NotificationQueue.NewNotificationListener {
	private static final String ENABLE_ON_FIRST_RUN_FIELD = "enableOnFirstRun";
	private static final String COOLDOWN_PERIOD_FIELD = "cooldownPeriod";
	private static final String PUBLIC_TOKEN_FIELD = "publicToken";
	private static final String NOTICATION_FILTER_ENABLED = "notificationFilterEnabled";
	private static final String GEOTRIGGER_HANDLER_ENABLED = "geotriggerHandlerEnabled";
	private static final String NOTIFICATION_RECEIVED_EVENT = "plotNotificationReceived";

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app) {
		
	}
	
	public void newNotification() {
		TiApplication app = TiApplication.getInstance();
		if (app == null) {
			Log.i("PlotAndroidModule", "TiApplication not intialized");
			return;
		}
		if (!KrollRuntime.isInitialized()) {
			Log.i("PlotAndroidModule", "KrollRuntime not intialized");
			return;
		}
		
		handleNotifications();
	}
	
	private void handleNotifications() {
		while (true) {
			FilterableNotification notification = NotificationQueue.getNextNotification();
			if (notification == null) {
				break;
			}
			handleNotification(notification);
		}
	}
  
	private void handleNotification(FilterableNotification notification) {
		if (hasListeners(NOTIFICATION_RECEIVED_EVENT)) {
			Log.i("PlotAndroidModule", "Opening for listener");
			// Convert notification to map
			Map<String, Object> jsonNotification = JsonUtil.notificationToMap(notification);
			fireEvent(NOTIFICATION_RECEIVED_EVENT, jsonNotification);
		} else {
			if (notification.getData() == null) {
				return;
			}
			try {
				Context appContext = TiApplication.getInstance().getApplicationContext();
				Intent openBrowserIntent = new Intent(appContext, OpenUriReceiver.class);
				openBrowserIntent.putExtra("notification", notification);
				
				appContext.sendBroadcast(openBrowserIntent);
			} catch (Throwable e) {
				Log.e("PlotAndroidModule", "Error opening URI: " + notification.getData(), e);
			}
		}
	}

	@Kroll.method
	public void initPlot(HashMap configuration) {	
		TiApplication appContext = TiApplication.getInstance();

		Activity activity = appContext.getCurrentActivity();

		if (configuration == null) {
			throw new IllegalArgumentException("No configuration object provided.");
		}

		if (configuration.containsKey(NOTICATION_FILTER_ENABLED) && !(configuration.get(NOTICATION_FILTER_ENABLED) instanceof Boolean)) {
			throw new IllegalArgumentException("NotificationFilterEnabled not specified correctly.");
		} 
		if (configuration.containsKey(NOTICATION_FILTER_ENABLED)) {
			SettingsUtil.setNotificationFilterEnabled((Boolean) configuration.get(NOTICATION_FILTER_ENABLED)); 
		}

		if (configuration.containsKey(GEOTRIGGER_HANDLER_ENABLED) && !(configuration.get(GEOTRIGGER_HANDLER_ENABLED) instanceof Boolean)) {
			throw new IllegalArgumentException("GeotriggerHandlerEnabled not specified correctly.");
		}
		if (configuration.containsKey(GEOTRIGGER_HANDLER_ENABLED)) {
			SettingsUtil.setGeotriggerHandlerEnabled((Boolean) configuration.get(GEOTRIGGER_HANDLER_ENABLED));
		}

    NotificationQueue.setListener(this);

		if (!configuration.containsKey(PUBLIC_TOKEN_FIELD)) {
			Plot.init(activity);
		} else {
      initPlotWithConfiguration(configuration, activity);
    }
		
		handleNotifications();
	}

  @SuppressWarnings("deprecation") // Plot.init with a config is deprecated... we know this
  private void initPlotWithConfiguration(HashMap configuration, Activity activity) {
    if (!(configuration.get(PUBLIC_TOKEN_FIELD) instanceof String)) {
			throw new IllegalArgumentException("Public key not specified correctly.");
		}
		
 		PlotConfiguration config = new PlotConfiguration((String) configuration.get(PUBLIC_TOKEN_FIELD));

		if (configuration.containsKey(COOLDOWN_PERIOD_FIELD) && !(configuration.get(COOLDOWN_PERIOD_FIELD) instanceof Integer)) {
			throw new IllegalArgumentException("Cooldown period not specified correctly.");
		}
		else if (configuration.containsKey(COOLDOWN_PERIOD_FIELD)) {
			config.setCooldownPeriod((Integer) configuration.get(COOLDOWN_PERIOD_FIELD));
		}

		if (configuration.containsKey(ENABLE_ON_FIRST_RUN_FIELD) && !(configuration.get(ENABLE_ON_FIRST_RUN_FIELD) instanceof Boolean)) {
			throw new IllegalArgumentException("Enable on first run not specified correctly.");
		}
		else if (configuration.containsKey(ENABLE_ON_FIRST_RUN_FIELD)) {
			config.setEnableOnFirstRun((Boolean) configuration.get(ENABLE_ON_FIRST_RUN_FIELD));
		}
		
		Plot.init(activity, config);
  }

	@Kroll.method
	public void enable() {
		Plot.enable();
	}

	@Kroll.method
	public void disable() {
		Plot.disable();
	}

	@Kroll.getProperty @Kroll.method
	public boolean getEnabled() {
		return Plot.isEnabled();
	}

	@Kroll.method
	public void setCooldownPeriod(int cooldownSeconds) {
		Plot.setCooldownPeriod(cooldownSeconds);
	}

	@Kroll.getProperty @Kroll.method
	public String getVersion() {
		return Plot.getVersion();
	}	

	@Kroll.method
	public void mailDebugLog() {
		Plot.mailDebugLog();
	}
	
	@Kroll.method
	public HashMap popFilterableNotifications() {
		NotificationsAndId notificationsAndId = NotificationBatches.popBatch();

		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("filterId", notificationsAndId.getId());
		result.put("notifications", JsonUtil.notificationsToMap(notificationsAndId.getNotifications()));
		return result;
	}
	
	@Kroll.method
	public void sendNotifications(HashMap batch) {
		String filterId = (String) batch.get("filterId");
		List<FilterableNotification> notifications = NotificationBatches.getBatch(filterId);
		
		Object[] jsonNotifications = (Object[]) batch.get("notifications");
		List<FilterableNotification> notificationsToSend = JsonUtil.getNotifications(jsonNotifications, notifications);
		NotificationBatches.sendBatch(filterId, notificationsToSend);
	}

	@Kroll.method
	public HashMap popGeotriggers() {
		GeotriggersAndId geotriggersAndId = GeotriggerBatches.popBatch();

		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("handlerId", geotriggersAndId.getId());
		result.put("geotriggers", JsonUtil.geotriggersToMap(geotriggersAndId.getGeotriggers()));
		return result;
	}

	@Kroll.method
	public void markGeotriggersHandled(HashMap batch) {
		String handlerId = (String) batch.get("handlerId");
		List<Geotrigger> geotriggers = GeotriggerBatches.getBatch(handlerId);

		Object[] jsonGeotriggers = (Object[]) batch.get("geotriggers");
		List<Geotrigger> geotriggersHandled = JsonUtil.getGeotriggers(jsonGeotriggers, geotriggers);
		GeotriggerBatches.sendBatch(handlerId, geotriggersHandled);
	}

  @Kroll.method
  public void setStringSegmentationProperty(String property, String value) {
    Plot.setStringSegmentationProperty(property, value);
  }

  @Kroll.method
  public void setBooleanSegmentationProperty(String property, boolean value) {
    Plot.setBooleanSegmentationProperty(property, value);
  }

  @Kroll.method
  public void setIntegerSegmentationProperty(String property, int value) {
    Plot.setLongSegmentationProperty(property, value);
  }

  @Kroll.method
  public void setDoubleSegmentationProperty(String property, double value) {
    Plot.setDoubleSegmentationProperty(property, value);
  }

  @Kroll.method
  public void setDateSegmentationProperty(String property, Date value) {
    Plot.setDateSegmentationProperty(property, value.getTime() / 1000);
  }
  
  @Kroll.getProperty @Kroll.method
  public HashMap[] getLoadedNotifications() {
  	return JsonUtil.notificationTriggersToMap(new ArrayList(Plot.getLoadedNotifications()));
  }
  
  @Kroll.getProperty @Kroll.method
  public HashMap[] getLoadedGeotriggers() {
  	return JsonUtil.geotriggersToMap(new ArrayList(Plot.getLoadedGeotriggers()));
  }
  
  @Kroll.getProperty @Kroll.method
  public HashMap[] getSentNotifications() {
  	return JsonUtil.sentNotificationsToMap(new ArrayList(Plot.getSentNotifications()));
  }
    
  @Kroll.getProperty @Kroll.method
  public HashMap[] getSentGeotriggers() {
  	return JsonUtil.sentGeotriggersToMap(new ArrayList(Plot.getSentGeotriggers()));
  }
  
  @Kroll.method
  public void clearSentNotifications() {
  	Plot.clearSentNotifications();
  }
  
	@Kroll.method
  public void clearSentGeotriggers() {
  	Plot.clearSentGeotriggers();
  }
  

}
