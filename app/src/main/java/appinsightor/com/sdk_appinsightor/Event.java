package appinsightor.com.sdk_appinsightor;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Event Class
 * 단일 유저 이벤트 data를 holds
 */
class Event {
    private static final String SEGMENTATION_KEY = "segmentation";
    private static final String KEY_KEY = "key";
    private static final String COUNT_KEY = "count";
    private static final String DUR_KEY = "dur";
    private static final String TIMESTAMP_KEY = "timestamp";

    public String key;
    public Map<String, String> segmentation;
    public int count;
    public double dur;
    public long timestamp;


    Event () {}

    public Event (String key) {
        AppInsightor.sharedInstance().logShowPrinting("");
        this.key = key;
        this.timestamp = AppInsightor.currentTimestampMs();
    }

    JSONObject toJSON() {
        AppInsightor.sharedInstance().logShowPrinting("");
        final JSONObject json = new JSONObject();

        try {
            json.put(KEY_KEY, key);
            json.put(COUNT_KEY, count);
            json.put(TIMESTAMP_KEY, timestamp);

            if (segmentation != null) {
                json.put(SEGMENTATION_KEY, new JSONObject(segmentation));
            }

            if (dur > 0) {
                json.put(DUR_KEY, dur);
            }
        }
        catch (JSONException e) {
            if (AppInsightor.sharedInstance().isLoggingEnabled()) {
                Log.w(AppInsightor.TAG, "JSON 객체 반환 Error", e);
            }
        }

        return json;
    }

    static Event fromJSON(final JSONObject json) {
        AppInsightor.sharedInstance().logShowPrinting("");
        Event event = new Event();

        try {
            if (!json.isNull(KEY_KEY)) {
                event.key = json.getString(KEY_KEY);
            }
            event.count = json.optInt(COUNT_KEY);
            event.dur = json.optDouble(DUR_KEY, 0.0d);
            event.timestamp = json.optLong(TIMESTAMP_KEY);

            if (!json.isNull(SEGMENTATION_KEY)) {
                final JSONObject segm = json.getJSONObject(SEGMENTATION_KEY);
                final HashMap<String, String> segmentation = new HashMap<String, String>(segm.length());
                final Iterator nameItr = segm.keys();
                while (nameItr.hasNext()) {
                    final String key = (String) nameItr.next();
                    if (!segm.isNull(key)) {
                        segmentation.put(key, segm.getString(key));
                    }
                }
                event.segmentation = segmentation;
            }
        }
        catch (JSONException e) {
            if (AppInsightor.sharedInstance().isLoggingEnabled()) {
                Log.w(AppInsightor.TAG, "Event 데이터 JSON 객체 반환 Error", e);
            }
            event = null;
        }

        return (event != null && event.key != null && event.key.length() > 0) ? event : null;
    }

    @Override
    public boolean equals(final Object o) {
        AppInsightor.sharedInstance().logShowPrinting("");
        if (o == null || !(o instanceof Event)) {
            return false;
        }

        final Event e = (Event) o;

        return (key == null ? e.key == null : key.equals(e.key)) &&
               timestamp == e.timestamp &&
               (segmentation == null ? e.segmentation == null : segmentation.equals(e.segmentation));
    }

    @Override
    public int hashCode() {
        AppInsightor.sharedInstance().logShowPrinting("");
        return (key != null ? key.hashCode() : 1) ^
               (segmentation != null ? segmentation.hashCode() : 1) ^
               (timestamp != 0 ? (int)timestamp : 1);
    }
}
