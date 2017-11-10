package appinsightor.com.sdk_appinsightor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


/**
 * SharePreferences 생성하여 local store를 구성한다.
 * 즉, 요청 데이터에 대해 저장할 대기열 Queue를 생성한다.
 *
 * 간단한 값 저장에 DB를 사용하기에는 복잡하기 때문에 SharedPreferences를 사용하면 적합하다.
 * 보통 초기 설정값이나 자동로그인 여부 등 간단한 값을 저장하기 위해 사용한다.
 * 어플리케이션에 파일 형태로 데이터를 저장한다.
 * e.g. data/data/패키지명/shared_prefs/SharedPreference이름.xml 위치에 저장
 * 어플리케이션이 삭제되기 전까지 보존된다.
 *
 * getPreferences(int mode)
 * 하나의 액티비티에서만 사용하는 SharedPreferences를 생성한다.
 * 생성되는 SharedPreferences 파일은 해당 액티비티이름으로 생선된다.
 * 하나의 액티비티에서만 사용할 수 있지만 getSharedPreferences()를 사용하면 다른 액티비티에서도 사용가능하다.
 *
 * getSharedPreferences(String name, int mode)
 * 특정 이름을 가진 SharedPreferences를 생성한다.
 * 주로 애플리케이션 전체에서 사용한다.
 *
 * SharedPreferences에 데이터 불러오기
 * 데이터를 불러오기 위해서 getInt()나 getString() 메서드를 사용하여 불러와야 한다.
 * 첫번째 인자는 데이터의 키, 두번째 인자는 해당값이 없을경우 반환할 값을 넣어준다.
 * e.g. int firstData = test.getInt("First", 0);
 */
public class AppInsightorStore {
    private static final String PREFERENCES = "APPINSIGHTOR_STORE";//SharePreferences xml name
    private static final String DELIMITER = ":::";//Queue에 쌓여 있는 요청 항목들간의 문자열 전환시 분리자
    private static final String CONNECTIONS_PREFERENCE = "CONNECTIONS";//SharedPreferences, 전송할 Queue
    private static final String EVENTS_PREFERENCE = "EVENTS";//SharedPreferences, 이벤트를 담고 있을 Queue
    private static final int MAX_EVENTS = 10;//SharedPreferences Queue에 쌓을 최대 이벤트 갯수
    private static final int MAX_REQUESTS = 10;//SharedPreferences Queue에 쌓을 최대 요청 갯수(이벤트 제외)
    private final SharedPreferences preferences_;


    /**
     * ok-
     * 특정 이름을 가진 SharedPreferences를 생성한다.(read+write mode)
     * @param context
     * @throws IllegalArgumentException if context is null
     */
    AppInsightorStore(final Context context) {
        if (context == null) {
            throw new IllegalArgumentException("valid context");
        }
        preferences_ = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);//0x0000(read+write)
    }

    /**
     * ok-
     * 현재 SharedPreferences에 의해 저장된 connections의 배열 값을 반환한다.<br />
     * 수신 서버에 전송하기 위한 Queue(local store)
     * @return unsorted data array
     */
    public String[] connections() {
        AppInsightor.sharedInstance().logShowPrinting("");
        final String joinedConnStr = preferences_.getString(CONNECTIONS_PREFERENCE, "");
        return joinedConnStr.length() == 0 ? new String[0] : joinedConnStr.split(DELIMITER);
    }

    /**
     * ok-
     * 현재 SharedPreferences에 의해 저장된 events의 JSON 문자열의 배열 값을 반환한다.<br />
     * 이벤트 데이터를 저장하기 위한 Queue(local store)
     * @return unsorted data array
     */
    public String[] events() {
        AppInsightor.sharedInstance().logShowPrinting("");
        final String joinedEventsStr = preferences_.getString(EVENTS_PREFERENCE, "");
        return joinedEventsStr.length() == 0 ? new String[0] : joinedEventsStr.split(DELIMITER);
    }

    /**
     * Returns a list of the current stored events, sorted by timestamp from oldest to newest.
     */
    public List<Event> eventsList() {
        AppInsightor.sharedInstance().logShowPrinting("");
        final String[] array = events();
        final List<Event> events = new ArrayList<>(array.length);
        for (String s : array) {
            try {
                final Event event = Event.fromJSON(new JSONObject(s));
                if (event != null) {
                    events.add(event);
                }
            } catch (JSONException ignored) {}
        }
        // order the events from least to most recent
        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(final Event e1, final Event e2) {
                return (int)(e1.timestamp - e2.timestamp);
            }
        });
        return events;
    }

    /**
     * ok-
     * 현재 SarhedPreferences connections의 전송할 요청데이터가 없다면 true, 있다면 false를 리턴한다.
     * 즉, connection ConnectionProcessor를 실행하기 위해서는 data가 있어야 한다.
     * @return boolean
     */
    public boolean isEmptyConnections() {
        AppInsightor.sharedInstance().logShowPrinting("");
        return preferences_.getString(CONNECTIONS_PREFERENCE, "").length() == 0;
    }

    /**
     * ok-
     * 요청한 메시지를 SharedPreferences(local store)에 저장한다.
     * 이때 기존의 데이터를 배열 형태로 읽어들어 최대 Queue Size를 체크하고,
     * 조건이 맞다면 배열에 추가하고, local store에 분리자를 이용하여 하나의 String으로 기록한다.
     * @param str 요청 메시지
     */
    public synchronized void addConnection(final String str) {
        AppInsightor.sharedInstance().logShowPrinting("");
        if (str != null && str.length() > 0) {
            final List<String> connections = new ArrayList<>(Arrays.asList(connections()));
            if (connections.size() < MAX_REQUESTS) {
                connections.add(str);
                preferences_.edit().putString(CONNECTIONS_PREFERENCE, join(connections, DELIMITER)).commit();
            }
        }
    }

    /**
     * Removes a connection from the local store.
     */
    public synchronized void removeConnection(final String str) {
        AppInsightor.sharedInstance().logShowPrinting("");
        if (str != null && str.length() > 0) {
            final List<String> connections = new ArrayList<>(Arrays.asList(connections()));
            if (connections.remove(str)) {
                preferences_.edit().putString(CONNECTIONS_PREFERENCE, join(connections, DELIMITER)).commit();
            }
        }
    }

    /**
     * Adds a custom event to the local store.
     */
    void addEvent(final Event event) {
        AppInsightor.sharedInstance().logShowPrinting("");
        final List<Event> events = eventsList();
        if (events.size() < MAX_EVENTS) {
            events.add(event);
            preferences_.edit().putString(EVENTS_PREFERENCE, joinEvents(events, DELIMITER)).commit();
        }
    }
    public synchronized void addEvent(final String key, final Map<String, String> segmentation, final long timestamp, final double dur) {
        AppInsightor.sharedInstance().logShowPrinting("");
        final Event event = new Event();
        event.key = key;
        event.segmentation = segmentation;
        event.timestamp = timestamp;
        event.count = 1;
        event.dur = dur;

        addEvent(event);
    }

    /**
     * Removes the specified events from the local store.
     */
    public synchronized void removeEvents(final Collection<Event> eventsToRemove) {
        AppInsightor.sharedInstance().logShowPrinting("");
        if (eventsToRemove != null && eventsToRemove.size() > 0) {
            final List<Event> events = eventsList();
            if (events.removeAll(eventsToRemove)) {
                preferences_.edit().putString(EVENTS_PREFERENCE, joinEvents(events, DELIMITER)).commit();
            }
        }
    }

    /**
     * Converts a collection of Event objects to URL-encoded JSON to a string, with each
     * event JSON string delimited by the specified delimiter.
     */
    static String joinEvents(final Collection<Event> collection, final String delimiter) {
        AppInsightor.sharedInstance().logShowPrinting("");
        final List<String> strings = new ArrayList<>();
        for (Event e : collection) {
            strings.add(e.toJSON().toString());
        }
        return join(strings, delimiter);
    }

    /**
     * ok-
     * List, Set, ArrayList 등의 순서나 집합적인 저장데이터 Collection의 모든 문자열을
     * 지정된 분리자로 연결, 단일 문자열로 조인한다.
     * @param collection List, Set 등의 collection type data
     * @param delimiter 분리자
     * @return string
     */
    static String join(final Collection<String> collection, final String delimiter) {
        AppInsightor.sharedInstance().logShowPrinting("");
        final StringBuilder builder = new StringBuilder();

        int i = 0;
        for (String s : collection) {
            builder.append(s);
            if (++i < collection.size()) {
                builder.append(delimiter);
            }
        }
        return builder.toString();
    }


    public synchronized String getPreference(final String key) {
        return preferences_.getString(key, null);
    }
    public synchronized void setPreference(final String key, final String value) {
        if (value == null) {
            preferences_.edit().remove(key).commit();
        } else {
            preferences_.edit().putString(key, value).commit();
        }
    }

    // preferences_ clear. test....
    synchronized void clear() {
        AppInsightor.sharedInstance().logShowPrinting("");
        final SharedPreferences.Editor prefsEditor = preferences_.edit();
        prefsEditor.remove(EVENTS_PREFERENCE);
        prefsEditor.remove(CONNECTIONS_PREFERENCE);
        prefsEditor.clear();
        prefsEditor.commit();
    }
}
