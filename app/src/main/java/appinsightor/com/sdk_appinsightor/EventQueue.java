package appinsightor.com.sdk_appinsightor;

import android.util.Log;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;


/**
 * EventQueue Class
 * 이벤트 데이터, 큐 및 JSON으로 변환
 */
public class EventQueue {
    private final AppInsightorStore appinsightorStore_;


    EventQueue(final AppInsightorStore appinsightorStore) {
        AppInsightor.sharedInstance().logShowPrinting("");
        appinsightorStore_ = appinsightorStore;
    }

    /**
     * Returns the number of events in the local event queue.
     */
    int size() {
        return appinsightorStore_.events().length;
   }

    /**
     * Removes all current events from the local queue and returns them.
     */
    String events() {
        AppInsightor.sharedInstance().logShowPrinting("");
        String result;

        final List<Event> events = appinsightorStore_.eventsList();
        final JSONArray eventArray = new JSONArray();
        for (Event e : events) {
            eventArray.put(e.toJSON());
            //Log.e("Log.Test >",String.valueOf(e.toJSON()));
        }

        result = eventArray.toString();

        appinsightorStore_.removeEvents(events);

        try {
            result = java.net.URLEncoder.encode(result, "UTF-8");
        } catch (UnsupportedEncodingException e) {}

        return result;
    }

    /**
     * Records a custom event to the local event queue.
     */
    void recordEvent(final Event event) {
        AppInsightor.sharedInstance().logShowPrinting("");
        appinsightorStore_.addEvent(event);
    }
    void recordEvent(final String key, final Map<String, String> segmentation, final double dur) {
        AppInsightor.sharedInstance().logShowPrinting("");
        final long timestamp = AppInsightor.currentTimestampMs();
        appinsightorStore_.addEvent(key, segmentation, timestamp, dur);
    }


    //AppInsightorStore getAppInsightorStore() { return appinsightorStore_; }
}
