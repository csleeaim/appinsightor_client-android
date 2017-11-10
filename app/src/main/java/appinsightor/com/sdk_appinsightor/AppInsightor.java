package appinsightor.com.sdk_appinsightor;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;

/**
 * AppInsightor SDK 시작시 호출(SDK 동작)<br />
 * 기본 환경설정과 값들을 지정하며, AppInsightor SDK에서 동작하는 Method 호출을 담당한다.
 *
 * <p><strong>Context 클래스의 역할</strong></p>
 *
 * 안드로이드 시스템은 여러가지 형태의 실행 형식( Activity, Service, ... )을 가지고 있다. 그렇기 때문에 클래스 구현상 중복된 실행 정보를 가질 수 밖에 없다.
 * 따라서 이런 중복된 정보를 줄이고, 메소드를 일원화시키기 위하여 실행 클래스들의 상위클래스로써 Context 클래스를 제공하고 있다.<br/>
 *
 * 안드로이드 시스템에서 모든 실행 형식을 가지는 클래스들은 Context 클래스에서 상속을 받아 설계되었기 때문에 다형성을 적용하여 코드를 통일성있게 표현할 수 있다.<br />
 * 예를 들어 액티비티에서 정보를 전달하기위하여 Intent 클래스를 사용하는 경우 해당 액티비티를 Intent 클래스가 참조할 수 있도록 Intent 클래스의 생성자는 아래와 같이 선언되어야 한다.<br />
 * Intent(Activity activity, ... );<br />
 *
 * 하지만 서비스도 Intent 클래스를 사용할 수 있기때문에 Intent 클래스는 서비스를 위하여 아래와 같은 생성자가 추가로 필요할 것이다.<br />
 * Intent(Service service, ... );<br/>
 *
 * 그리고 또 다른 형태의 실행 형식을 가지는 클래스가 추가된다면 Intent 클래스는 그 클래스를 위하여 객체생성자가 추가되어야하는 문제점이 생기게되어 시스템 입장에서는 유지보수가 곤란해진다.
 * 따라서 안드로이드 시스템은 모든 실행 형식과 관련된 클래스들을 Context 클래스에서 계승받았기 때문에 Intent 클래스의 객체 생성자를 아래와 같이 선언할 수 있다.<br />
 * Intent(Context context, ... );<br />
 *
 * 이렇게 되면 이제 이 생성자는 Activity 클래스든 Service 클래스든 둘 다 사용할 수 있을 뿐만 아니라 나중에 또 다른 실행 형식의 클래스가 추가되어도 수정없이 위 생성자를 사용할 수 있다.<br />
 *
 * 예를들어, Activity 클래스에서 Intent 클래스를 사용할때는 Intent 클래스의 생성자에 this 를 명시해서 사용하면 된다.
 * this 는 Activity 객체를 의미하지만 Activity 클래스가 Context 클래스의 자식 클래스이기 때문에 다형성에 의해서 사용이 가능하다.
 */
public class AppInsightor {

    /**
     * AppInsightor Application ID
     */
    public static String APPLICATION_SERVER_NAME;
    /**
     * 서버에 저장하기 위한 category name 설정
     */
    public static final String APPINSIGHTOR_SAVE_CATEGORY = "native";
    /**
     * AppInsightor SDK Name 설정
     */
    public static final String APPINSIGHTOR_SDK_NAME_STRING = "native-android";
    /**
     * AppInsightor SDK Version 설정
     */
    public static final String APPINSIGHTOR_SDK_VERSION_STRING = "16.10.22";
    /**
     * 사용자 App Version 설정
     * 만약 사용자가 정의하지 않거나 구글스토어에서 가져오지 못했을 경우 App Version이 지정된다.
     */
    public static final String APPINSIGHTOR_DEFAULT_APP_VERSION = "1.10";
    /**
     * 디버깅 처리시 TAG
     */
    public static final String TAG = "AppInsightor";
    /**
     * 사용자 설정 DeviceID<br/>
     * 만약 시스템에서 가져오지 못했을 경우 사용자 설정 Device ID 가 지정된다.
     */
    private static final String USER_DEVICE_ID = "APP-USER-DEVICE-ID";
    //private static final String DEVICEID_PREFERENCE = "DEVICEID";
    //SharedPreferences, Device ID를 담고 있을 Queue, deviceId 영속적인 저장 처리

    /**
     * Event Queue에 담긴 요청 메시지들에 대한 수신서버 전송 제한 갯수(<b>1:즉시전송</b>)
     */
    private static int EVENT_QUEUE_SIZE_THRESHOLD = 1;
    /**
     * star-end Event 시 key 비교를 위한 변수
     */
    protected static final Map<String, Event> timedEvents = new HashMap<>();

    /**
     * 디버깅을 위한 boolean 변수
     */
    private boolean enableLogging_ = false;
    /**
     * 디버깅을 위한 변수 - 현재 Class Name 과 Method Name 출력
     */
    private boolean enableLoggingStep_ = false;
    /**
     * release 시 사용할 로그를 위한 변수 - 현재 Class Name 과 Method Name 출력
     */
    private boolean enableLogRelease_ = true;


    private ConnectionQueue connectionQueue_;
    private EventQueue eventQueue_;
    private int activityCount_;
    private Context context_;
    private static String deviceId_;


    /**
     * Thread 를 Singleton 으로 생성한다.
     */
    private static class SingletonHolder {
        static final AppInsightor instance = new AppInsightor();
    }

    /**
     * Singleton 생성 호출
     * @return Singleton instance
     */
    public static AppInsightor sharedInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 생성자로부터 local store Queue 를 생성<br />
     * 향후 User Data 등 수신서버에 전송할 필요 내용을 처리할 수 있다.
     */
    AppInsightor() {
        connectionQueue_ = new ConnectionQueue();
        //Other Contents
    }

    /**
     * AppInsightor init - Device ID 값 지정(시스템 또는 사용자)
     * @param context
     * @param serverURL 수신서버 URL
     * @param appName App Name
     * @return Device ID 값을 사용자가 지정하지 않을 경우 고유한 Device ID 값을 시스템으로부터 생성하여 재정의된 init() 호출
     */
    public AppInsightor init(final Context context, final String serverURL, final String appName, String appVersion, String serverAppName) {
        String deviceIdTemp = DeviceInfo.initializeDeviceID(context);
        if(appVersion == null || appVersion.length() < 1) {
            appVersion = DeviceInfo.getAppVersion(context);
        }
        if (deviceIdTemp == null || deviceIdTemp.length() < 14) {
            return init(context, serverURL, appName, appVersion, USER_DEVICE_ID, serverAppName);
        } else {
            return init(context, serverURL, appName, appVersion, deviceIdTemp, serverAppName);
        }
    }

    /**
     * AppInsightor init - once time<br />
     * 요청 메시지를 처리 및 수신서버에 전송하기 위한 local store(Queue)를 초기화 하고<br />
     * deviceID 등 초기 데이터를 입력한다.
     * @param context
     * @param serverURL 설정된 수신 Server URL
     * @param appName App Name
     * @param deviceID 시스템 및 사용자 정의 device Id
     * @throws IllegalArgumentException context, serverURL, appName, DeviceID 의 값이 옳바르지 않을 경우
     * @return this
     */
    public AppInsightor init(final Context context, final String serverURL, final String appName, final String appVersion, final String deviceID, String serverAppName) {
        AppInsightor.sharedInstance().logShowPrinting("start");
        if (context == null) {
            throw new IllegalArgumentException("valid context is required");
        }
        if (!isValidURL(serverURL)) {
            throw new IllegalArgumentException("valid serverURL is required");
        }
        if (appName == null || appName.length() == 0) {
            throw new IllegalArgumentException("valid appName is required");
        }
        if (deviceID != null && deviceID.length() == 0) {
            throw new IllegalArgumentException("valid DeviceID is required");
        }
        if (serverAppName != null && serverAppName.length() == 0) {
            throw new IllegalArgumentException("valid serverAppName is required");
        }
        //halt();
        if (eventQueue_ == null) {
            final AppInsightorStore appinsightorStore = new AppInsightorStore(context);

            deviceId_ = deviceID;
            AppInsightor.APPLICATION_SERVER_NAME = serverAppName;
            //appinsightorStore.setPreference(DEVICEID_PREFERENCE, deviceId_);//deviceId 영속적인 저장 처리
            connectionQueue_.setServerURL(serverURL);
            connectionQueue_.setAppName(appName);
            connectionQueue_.setAppVersion(appVersion);
            connectionQueue_.setAppInsightorStore(appinsightorStore);
            connectionQueue_.setDeviceId(deviceId_);
            eventQueue_ = new EventQueue(appinsightorStore);
        }
        context_ = context;
        connectionQueue_.setContext(context);


        AppInsightor.sharedInstance().logShowPrinting("end");
        return this;
    }

    /**
     * 이벤트큐(event Queue)가 생성되었는지 Check - 요청 메시지 데이터를 담아둘 local store
     * @return eventQueue_ 이벤트큐
     */
    public synchronized boolean isInitialized() {
        AppInsightor.sharedInstance().logShowPrinting("start");
        return eventQueue_ != null;
    }

    /**
     * Get DeviceId
     * @return deviceID
     */
    static String getDeviceId() {
        return deviceId_;
    }

    /**
     * AppInsightor SDK 프로그래밍 디버깅을 위한 로그 출력 유무의 boolean 값
     * @return boolean 로그출력 유무
     */
    public synchronized boolean isLoggingEnabled() {
        return enableLogging_;
    }

    /**
     * AppInsightor SDK 프로그래밍 디버깅을 위해 현재 Class Name 과 Method Name 을 출력하기 위함
     * @param str 사용자 지정 문자열 (start, end etc.)
     */
    public synchronized void logShowPrinting(String str) {
        if(enableLoggingStep_) {
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            StackTraceElement currentStack = stacks[1];
            if (str == "start") {
                System.out.println("===" + currentStack.getClassName() + " " + currentStack.getMethodName() + " start");
            } else if (str == "end") {
                System.out.println("===" + currentStack.getClassName() + " " + currentStack.getMethodName() + " end");
            } else {
                System.out.println("===" + currentStack.getClassName() + " " + currentStack.getMethodName());
            }
        }
    }

    /**
     * AppInsightor SDK 실제 API 적용시 현재 ClassName과 MethodName을 출력하기 위함
     * @param str 사용자 지정 문자열 (start, end etc.)
     */
    public synchronized void showClassMethodLog(String str) {
        if(enableLogRelease_) {
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            StackTraceElement currentStack = stacks[1];
            if (str == "start") {
                Log.i(currentStack.getClassName() + " ", currentStack.getMethodName() + " start");
            } else if (str == "end") {
                Log.i(currentStack.getClassName() + " ", currentStack.getMethodName() + " end");
            } else {
                Log.i(currentStack.getClassName() + " ", currentStack.getMethodName());
            }
        }
    }

    /**
     * checksum 검사를 하기 위한 설정(서버에서 확인하여 유효성 검사 가능)<br />
     * 프로그램 고정 또는 사용자가 직접 지정 가능<br />
     * call ConnectionProcessor.checksum
     * @param checksum 검사를 필요로 하는 문자열
     * @return this
     */
    public synchronized AppInsightor enableSecretChecksum(String checksum) {
        ConnectionProcessor.checksum = checksum;
        return this;
    }

    /**
     * 모든 Queue 와 관련한 데이터와 변수값들을 초기화 한다.<br />
     * @deprecated Currently SDK
     */
    public synchronized void halt() {
        eventQueue_ = null;
        final AppInsightorStore appinsightorStore = connectionQueue_.getAppInsightorStore();
        if (appinsightorStore != null) {
            appinsightorStore.clear();
        }
        connectionQueue_.setContext(null);
        connectionQueue_.setServerURL(null);
        connectionQueue_.setAppName(null);
        connectionQueue_.setAppVersion(null);
        connectionQueue_.setAppInsightorStore(null);
        activityCount_ = 0;
    }

    /**
     * AppInsightor SDK Start (Auto call)<br />
     * App 시작시 beginSession() 호출 및 App 의 foreground 실행으로 설정(이는 Crash 발생시 App 의 현재 상태정보중 하나)
     * @param activity
     */
    public synchronized void onStart(Activity activity) {
        AppInsightor.sharedInstance().logShowPrinting("start");
        AppInsightor.sharedInstance().showClassMethodLog("start");

        if (eventQueue_ == null) {
            throw new IllegalStateException("eventQueue_는 not null 이어야 하며, onStart 호출 전에 초기화 되어 한다.");
        }
        ++activityCount_;
        if (activityCount_ == 1) {//beginSession() 호출, 값이 0일 경우 호출하지 않음
            onStartHelper();
        }
        CrashDetails.inForeground();//Foreground 실행 설정

        AppInsightor.sharedInstance().logShowPrinting("end");
        AppInsightor.sharedInstance().showClassMethodLog("end");
    }

    /**
     * App 구동시, 백그라운드에서 포그라운드 전환시 실행<br />
     * APP 실행에 따른 beginSession 요청메시지 데이터 수신서버 전송 진행
     * Call connectionQueue_.beginSession()
     */
    void onStartHelper() {
        AppInsightor.sharedInstance().logShowPrinting("");
        connectionQueue_.beginSession();
    }

    /**
     * AppInsightor SDK Stop (Auto call)<br />
     * App 종료시 endSession() 호출 및  App 의 background 실행으로 설정(이는 Crash 발생시 App 의 현재 상태정보중 하나)
     */
    public synchronized void onStop() {
        AppInsightor.sharedInstance().logShowPrinting("");
        AppInsightor.sharedInstance().showClassMethodLog("start");
        if (eventQueue_ == null) {
            throw new IllegalStateException("onStop 호출 전에 진행된 값이 있어야 한다.");
        }
        if (activityCount_ == 0) {
            throw new IllegalStateException("onStop 호출 전에 onStart 를 진행하였으면 값이 카운트 되어 있다.");
        }

        --activityCount_;
        if (activityCount_ == 0) {//endSession() 호출
            onStopHelper();
        }
        CrashDetails.inBackground();//Background 실행 설정

        AppInsightor.sharedInstance().logShowPrinting("end");
        AppInsightor.sharedInstance().showClassMethodLog("end");
    }

    /**
     * App 종료시, 포그라운드에서 백그라운드 전환시 실행<br />
     * APP 종료에 따른 endSession 요청메시지 데이터 수신서버 전송 진행<br />
     *
     * <p>Addition</p>
     * 멀티 처리의 경우, 즉 이벤트 요청 메시지를 Queue 에 쌓아두고 설정한 카운트가 되면 수신서버에 전송시<br />
     * endSession 이 발생하면 endSession 메시지 데이터 전송후 eventQueue 에 이벤트 요청메시지들이 있을 경우 별도로 전송 처리한다.<br />
     * 즉시 전송을 설정한 EVENT_QUEUE_SIZE_THRESHOLD 변수의 값이 1 이상의 값을 지정한 경우 조건이 수행 될 수 있다.<br />
     * call connectionQueue_.endSession()
     */
    void onStopHelper() {
        AppInsightor.sharedInstance().logShowPrinting("");
        connectionQueue_.endSession();

        // 멀티 이벤트 요청메시지 처리시
        if (eventQueue_.size() > 0) {
            connectionQueue_.recordEvents(eventQueue_.events());//이벤트 요청메시지 데이터 수신서버 전송 처리
        }
    }

    /**
     * 이벤트 기록 with given key, segmentation, count
     * call 재정의된 recordEvent()
     * @param key 설정된 키
     */
    public synchronized void recordEvent(final String key) {
        recordEvent(key, null, 0);
    }

    public synchronized void recordEvent(final String key, final Map<String, String> segmentation) {
        recordEvent(key, segmentation, 0);
    }

    /**
     * 이벤트 기록 with given key, segmentation, count, duration<br/>
     * 이벤트 요청메시지 데이터를 기록하고 수신서버에 전송한다.<br/>
     * call eventQueue_.recordEvent()<br/>
     * call connectionQueue_.recordEvents()
     *
     * @param key 설정된 키
     * @param segmentation 사용자가 입력한 key-value 값
     * @param dur 이벤트 처리 시간(second)
     * @throws IllegalStateException AppInsightor SDK 초기화가 되지 않았을 경우
     * @throws IllegalArgumentException key 값이 없거나, count 가 1 이하인경우, 사용자 입력 segmentation key-value 쌍이 올바르지 않은 경우
     */
    public synchronized void recordEvent(final String key, final Map<String, String> segmentation, final double dur) {
        AppInsightor.sharedInstance().logShowPrinting("start");
        AppInsightor.sharedInstance().showClassMethodLog("start");

        if (!isInitialized()) {
            throw new IllegalStateException("AppInsightor.sharedInstance().init 처리를 통한 eventQueue_ 초기화 체크");
        }
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("Event 이름 필요");
        }
        if (segmentation != null) {
            for (String k : segmentation.keySet()) {
                if (k == null || k.length() == 0) {
                    throw new IllegalArgumentException("NULL이 아닌 Map, segmentation의 key가 null 또는 비어 있어 있으면 안됨");
                } else {
                    if (enableLogRelease_) {
                        Log.i("recordEvent", "key : " + k);
                    }
                }
                if (segmentation.get(k) == null || segmentation.get(k).length() == 0) {
                    throw new IllegalArgumentException("NULL이 아닌 Map, segmentation의 값이 null 또는 비어 있어 있으면 안됨");
                } else {
                    if (enableLogRelease_) {
                        Log.i("recordEvent", "value : " + segmentation.get(k));
                    }
                }
            }
        }

        //이벤트 요청메시지 데이터를 event Queue 에 기록
        eventQueue_.recordEvent(key, segmentation, dur);

        //event Queue 사이즈를 체크하여 설정한 값보다 같거나 클경우 connection Queue 에 저장 후 수신서버에 전송
        if (eventQueue_.size() >= EVENT_QUEUE_SIZE_THRESHOLD) {
            connectionQueue_.recordEvents(eventQueue_.events());
        }
        AppInsightor.sharedInstance().logShowPrinting("end");
        AppInsightor.sharedInstance().showClassMethodLog("end");
    }

    /**
     * 사용자 Crash Report segments 설정<br/>
     * segments 값이 null이 아닌경우 CrashDetails.setCustomSegments() 설정 Method 호출
     * @param segments Map&lt;String, String&gt; key segments and their values
     * @return this
     */
    public synchronized AppInsightor setCustomCrashSegments(Map<String, String> segments) {
        AppInsightor.sharedInstance().logShowPrinting("");
        if(segments != null)
            CrashDetails.setCustomSegments(segments);//사용자 정의 Crash 설정 값 저장
        return this;
    }

    /**
     * Crash Report 제출시 "Crash 명칭", 로그정보, Crash 경로 등을 추가할 수 있다.
     * @param record String Crash Report 추가 정보
     * @return this
     */
    public synchronized AppInsightor addCrashLog(String record) {
        AppInsightor.sharedInstance().logShowPrinting("");
        CrashDetails.addLog(record);
        return this;
    }

    /**
     * 예외처리가 된 Crash 로그 정보를 전송한다.
     * 즉, 예외처리 루틴에서 호출하여 예외처리 정보를 전송한다.
     * @param exception Exception to log
     * @return this
     */
    public synchronized AppInsightor logException(Exception exception) {
        AppInsightor.sharedInstance().logShowPrinting("");
        AppInsightor.sharedInstance().showClassMethodLog("");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        connectionQueue_.sendCrashReport(sw.toString(), true);
        return this;
    }

    /**
     * Crash Report를 시작한다.
     * 즉 예외처리가 되지 않은 갑작스런 충돌에 대하여 별도 Handler 생성하여 Crash Report를 담당
     * Exception error 가 발생되면 error 정보와 boolean nonfatal(치명적인 error 유무) 값과 함께 sendCrashReport() 호출
     * @return this
     */
    public synchronized AppInsightor enableCrashReporting() {
        AppInsightor.sharedInstance().logShowPrinting("");
         final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();//get default handler

        Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                AppInsightor.sharedInstance().connectionQueue_.sendCrashReport(sw.toString(), false);

                //if there was another handler before
                if(oldHandler != null){
                    //notify it also
                    oldHandler.uncaughtException(t,e);
                }
            }
        };

        Thread.setDefaultUncaughtExceptionHandler(handler);
        return this;
    }


    /**
     * 이벤트 시작 지정<br/>
     * timeEvents 변수에 star-end Event 시 비교를 위한 key 를 저장
     * @param key start-end Event 에 설정된 키(이벤트 키)
     * @return boolean
     * @throws IllegalStateException AppInsightor SDK 초기화가 되지 않았을 경우
     * @throws IllegalArgumentException Start-End Event Key is null
     */
    public synchronized boolean startEvent(final String key) {
        AppInsightor.sharedInstance().logShowPrinting("");
        AppInsightor.sharedInstance().showClassMethodLog("");
        if (!isInitialized()) {
            throw new IllegalStateException("AppInsightor.sharedInstance().init must be called before recordEvent");
        }
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("Valid AppInsightor event key is required");
        }

        if (timedEvents.containsKey(key)) {
            return false;
        }
        timedEvents.put(key, new Event(key));
        return true;
    }

    /**
     * 이벤트 종료 지정
     * @param key start-end Event 에 설정된 키
     * @return boolean, 재정의된 endEvent() Method 호출
     */
    public synchronized boolean endEvent(final String key) {
        return endEvent(key, null);
    }

    /**
     * 이벤트 종료 지정
     * @param key start-end Event 에 설정된 키
     * @param segmentation 사용자가 지정한 key-value 값
     * @return boolean
     * @throws IllegalStateException AppInsightor SDK 초기화가 되지 않았을 경우
     * @throws IllegalArgumentException key 값이 없거나, count 가 1 이하인경우, 사용자 입력 segmentation key-value 쌍이 올바르지 않은 경우
     */
    public synchronized boolean endEvent(final String key, final Map<String, String> segmentation) {
        AppInsightor.sharedInstance().logShowPrinting("");

        Event event = timedEvents.remove(key);//start-end Event 에 설정된 키값을 clear
        if (event != null) {
            if (!isInitialized()) {
                throw new IllegalStateException("AppInsightor.sharedInstance().init 처리를 통한 eventQueue_ 값 체크");
            }
            if (key == null || key.length() == 0) {
                throw new IllegalArgumentException("Event 이름 필요");
            }
            if (segmentation != null) {
                for (String k : segmentation.keySet()) {
                    if (k == null || k.length() == 0) {
                        throw new IllegalArgumentException("NULL이 아닌 Map, segmentation의 key가 null 또는 비어 있어 있으면 안됨");
                    } else {
                        if (enableLogRelease_) {
                            Log.i("endEvent", "key : " + k);
                        }
                    }
                    if (segmentation.get(k) == null || segmentation.get(k).length() == 0) {
                        throw new IllegalArgumentException("NULL이 아닌 Map, segmentation의 값이 null 또는 비어 있어 있으면 안됨");
                    } else {
                        if (enableLogRelease_) {
                            Log.i("endEvent", "value : " + segmentation.get(k));
                        }
                    }
                }
            }

            AppInsightor.sharedInstance().showClassMethodLog("");
            event.segmentation = segmentation;
            event.dur = AppInsightor.currentTimestampMs() - event.timestamp; //변경
            event.count = 1;
            eventQueue_.recordEvent(event);
            if (eventQueue_.size() >= EVENT_QUEUE_SIZE_THRESHOLD) {
                connectionQueue_.recordEvents(eventQueue_.events());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 시스템 timestamp 요청에 따른 직전 시간 저장 변수
     */
    private static long lastTsMs;

    /**
     * 시스템 현재의 시간 timestamp<br/>
     * 요청에 따른 직전 시간을 변수에 저장해 두고 다음 요청시 직전 시간보다 큰 값을 돌려준다.<br/>
     * 동기화 처리(멀티 스레드 접근 허용)를 위해 순서가 뒤에 있는 요청이 빠른 시간을 가질 수 없도록 한다.
     * @return timestamp
     */
    static synchronized long currentTimestampMs() {
        long ms = System.currentTimeMillis();
        while (lastTsMs >= ms) {
            ms += 1;
        }
        lastTsMs = ms;
        return ms;
    }

    /**
     * 시스템 시간 Year
     * @return int year
     */
    static int currentYear(){
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /**
     * 시스템 시간 Month
     * @return int Month(0~11) + 1
     */
    static int currentMonth(){
        return (Calendar.getInstance().get(Calendar.MONTH) + 1);
    }

    /**
     * 시스템 시간 Day
     * @return int Day
     */
    static int currentDay(){
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 시스템 시간 Hour
     * @return int Hour
     */
    static int currentHour(){
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 시스템 시간 Week
     * @return int Week(1:일요일)
     */
    static int currentDayOfWeek(){ return Calendar.getInstance().get(Calendar.DAY_OF_WEEK); }//1(일요일)

    /**
     * 시스템 시간 timezone
     * @return int timezone(분), 대한민국(+540분(9시간))
     */
    static int currentTimeZone(){ return (Calendar.getInstance().get(Calendar.ZONE_OFFSET) / (60*1000)); }

    /**
     * URL 유효성 검사
     * @param urlStr
     * @return boolean
     */
    static boolean isValidURL(final String urlStr) {
        boolean validURL = false;
        if (urlStr != null && urlStr.length() > 0) {
            try {
                new URL(urlStr);
                validURL = true;
            }
            catch (MalformedURLException e) {
                validURL = false;
            }
        }
        return validURL;
    }


    /**
     * Crash Report Enabled.<br/>
     * 사용자 Crash Report segments 설정 가능<br/>
     * e.g/ data.put("key1","value1"); 형식으로 사용자 key-value 입력<br/>
     *
     * called setCustomCrashSegments(data)<br/>
     * called enableCrashReporting()<br/>
     */
    public AppInsightor enableCrashTracking(){
        AppInsightor.sharedInstance().logShowPrinting("start");

        //사용자 정의 세그먼트 추가
//        HashMap<String, String> data = new HashMap<>();
//        data.put("key1","value1");
//        data.put("key2","value2");
//        setCustomCrashSegments(data);

        //Crash Report Start
        enableCrashReporting();
        AppInsightor.sharedInstance().logShowPrinting("end");

        return this;
    }

    //Crash Test Function
    public void stackOverflow() {
        this.stackOverflow();
    }
    public synchronized AppInsightor crashTest(int crashNumber) {
        AppInsightor.sharedInstance().logShowPrinting("start");

        if (crashNumber == 2){//
            throw new RuntimeException("This is a runtime crash");
        } else if (crashNumber == 3){//
            int test = 10/0;
        } else if (crashNumber == 4) {//
            stackOverflow();
        }
        else{
            String test = null;
            test.charAt(1);
        }
        AppInsightor.sharedInstance().logShowPrinting("end");
        return AppInsightor.sharedInstance();
    }
}
