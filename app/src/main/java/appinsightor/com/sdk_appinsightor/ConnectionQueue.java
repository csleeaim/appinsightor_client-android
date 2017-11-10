package appinsightor.com.sdk_appinsightor;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ConnectionQueue Class
 */
public class ConnectionQueue {
    private AppInsightorStore store_;
    private ExecutorService executor_;
    private String appName_;
    private String appVersion_;
    private Context context_;
    private String serverURL_;
    private Future<?> connectionProcessorFuture_;
    private String deviceId_;

    String getAppName() {
        return appName_;
    }
    void setAppName(final String appName) {
        appName_ = appName;
    }
    String getAppVersion() {
        return appVersion_;
    }
    void setAppVersion(final String appVersion) {
        appVersion_ = appVersion;
    }

    Context getContext() {
        return context_;
    }
    void setContext(final Context context) {
        context_ = context;
    }

    String getServerURL() {
        return serverURL_;
    }
    void setServerURL(final String serverURL) {
        serverURL_ = serverURL;
    }

    AppInsightorStore getAppInsightorStore() {
        return store_;
    }
    void setAppInsightorStore(final AppInsightorStore appinsightorStore) {
        store_ = appinsightorStore;
    }

    String getDeviceId() {
        return deviceId_;
    }
    public void setDeviceId(String deviceId) {
        deviceId_ = deviceId;
    }


    /**
     * ok-
     * Report를 생성하기 위해 내부 상태 등 여러가지 요소를 Check하여 시작할 수 없을 경우 IllegalStateException 처리
     * @throws IllegalStateException if context, app name, store, or server URL have not been set
     */
    void checkInternalState() {
        if (context_ == null) {
            throw new IllegalStateException("context has not been set");
        }
        if (appName_ == null || appName_.length() == 0) {
            throw new IllegalStateException("app name has not been set");
        }
        if (store_ == null) {
            throw new IllegalStateException("appinsightor store has not been set");
        }
        if (serverURL_ == null || !AppInsightor.isValidURL(serverURL_)) {
            throw new IllegalStateException("server URL is not valid");
        }
    }


    /**
     * session 시작 이벤트를 기록하고 AppInsightor 서버에 전송
     */
    void beginSession() {
        AppInsightor.sharedInstance().logShowPrinting("start");
        checkInternalState();
        final String data = "t=" + AppInsightor.APPLICATION_SERVER_NAME
                          + "&category=" + AppInsightor.APPINSIGHTOR_SAVE_CATEGORY
                          + "&app_name=" + appName_
                          + "&app_version=" + appVersion_
                          + "&device_id=" + AppInsightor.getDeviceId()
                          + "&timestamp=" + AppInsightor.currentTimestampMs()
                          + "&sdk_name=" + AppInsightor.APPINSIGHTOR_SDK_NAME_STRING
                          + "&sdk_version=" + AppInsightor.APPINSIGHTOR_SDK_VERSION_STRING
                          + "&session_status=active"
                          + "&metrics=" + DeviceInfo.getMetrics(context_);

        store_.addConnection(data);

        tick();
        AppInsightor.sharedInstance().logShowPrinting("end");
    }

    /**
     * session 종료 이벤트를 기록하고 AppInsightor 서버에 전송
     */
    void endSession() {
        AppInsightor.sharedInstance().logShowPrinting("start");
        checkInternalState();
        String data = "t=" + AppInsightor.APPLICATION_SERVER_NAME
                    + "&category=" + AppInsightor.APPINSIGHTOR_SAVE_CATEGORY
                    + "&app_name=" + appName_
                    + "&app_version=" + appVersion_
                    + "&device_id=" + AppInsightor.getDeviceId()
                    + "&timestamp=" + AppInsightor.currentTimestampMs()
                    + "&sdk_name=" + AppInsightor.APPINSIGHTOR_SDK_NAME_STRING
                    + "&sdk_version=" + AppInsightor.APPINSIGHTOR_SDK_VERSION_STRING
                    + "&session_status=inactive"
        + "&metrics=" + DeviceInfo.getMetrics(context_);

        store_.addConnection(data);

        tick();
        AppInsightor.sharedInstance().logShowPrinting("end");
    }


    /**
     * Send user data to the server.
     */
    //    void sendUserData() {
    //        checkInternalState();
    //        String userdata = UserData.getDataForRequest();
    //
    //        if(!userdata.equals("")){
    //            String data = "app_name=" + appName_
    //                    + "&timestamp=" + AppInsightor.currentTimestampMs()
    //                    + "&year=" + AppInsightor.currentYear()
    //                    + "&month=" + AppInsightor.currentMonth()
    //                    + "&hour=" + AppInsightor.currentHour()
    //                    + "&week=" + AppInsightor.currentDayOfWeek()
    //                    + userdata;
    //            store_.addConnection(data);
    //
    //            tick();
    //        }
    //    }


    /**
     * ok-
     * Crash Report 데이터 입력 with device data
     * @param error Exception error 내용
     * @param nonfatal 치명적오류 유무, 즉 예외처리가 된 상태에서의 error인가? 되지 않는 상태에서의 error인가
     * @throws IllegalStateException if context, app name, store, or server URL have not been set
     * called checkInternalState() - error check
     * AppInsightorStore에 data 저장
     */
    void sendCrashReport(String error, boolean nonfatal) {
        AppInsightor.sharedInstance().logShowPrinting("start");
        checkInternalState();
        final String data = "t=" + AppInsightor.APPLICATION_SERVER_NAME
                + "&category=" + AppInsightor.APPINSIGHTOR_SAVE_CATEGORY
                + "&app_name=" + appName_
                + "&app_version=" + appVersion_
                + "&device_id=" + AppInsightor.getDeviceId()
                + "&timestamp=" + AppInsightor.currentTimestampMs()
                + "&sdk_name=" + AppInsightor.APPINSIGHTOR_SDK_NAME_STRING
                + "&sdk_version=" + AppInsightor.APPINSIGHTOR_SDK_VERSION_STRING
                + "&metrics=" + DeviceInfo.getMetrics(context_)
                + "&crash=" + CrashDetails.getCrashData(context_, error, nonfatal);

        //local store add & connection
        store_.addConnection(data);

        tick();
        AppInsightor.sharedInstance().logShowPrinting("end");
    }

    /**
     * 임의로 지정한 이벤트를 기록하고 서버로 전송
     */
    void recordEvents(final String events) {
        AppInsightor.sharedInstance().logShowPrinting("start");
        checkInternalState();
        final String data = "t=" + AppInsightor.APPLICATION_SERVER_NAME
                            + "&category=" + AppInsightor.APPINSIGHTOR_SAVE_CATEGORY
                            + "&app_name=" + appName_
                            + "&app_version=" + appVersion_
                            + "&device_id=" + AppInsightor.getDeviceId()
                            + "&timestamp=" + AppInsightor.currentTimestampMs()
                            + "&sdk_name=" + AppInsightor.APPINSIGHTOR_SDK_NAME_STRING
                            + "&sdk_version=" + AppInsightor.APPINSIGHTOR_SDK_VERSION_STRING
                            + "&metrics=" + DeviceInfo.getMetrics(context_)
                            + "&session_status=" + (CrashDetails.isInBackground() == "false" ? "active" : "inactive")
                            + "&events=" + events;

        store_.addConnection(data);
        tick();
        AppInsightor.sharedInstance().logShowPrinting("end");
    }

    /**
     * ok-
     * ConnectionProcessor instances 가 생성되었는지 확인
     *
     * Executors 클래스는 Executor 인터페이스 등을 구현한 인스턴스를 리턴하는 메소드를 제공하는 클래스이다.
     * Executors.newSingleThreadExecutor() 메소드로 하나의 스레드로 태스크를 실행시키는 Executor 를 취득한다.
     */
    void ensureExecutor() {
        AppInsightor.sharedInstance().logShowPrinting("");
        if (executor_ == null) {
            executor_ = Executors.newSingleThreadExecutor();
        }
    }

    /**
     * ok-
     * ConnectionProcessor instances를 백그라운드로 실행하고(Runnable), queue data를 처리한다.
     * 전송할 데이터가 없거나 ConnectionProcessor가 이미 실행중인 경우에는 수행하지 않는다.
     *
     * 지정된 태스크를 submit, 실행 결과를 Future 형태로 리턴한다.
     */
    void tick() {
        AppInsightor.sharedInstance().logShowPrinting("");
        if (!store_.isEmptyConnections() && (connectionProcessorFuture_ == null || connectionProcessorFuture_.isDone())) {
            ensureExecutor();
            connectionProcessorFuture_ = executor_.submit(new ConnectionProcessor(serverURL_, store_, deviceId_));
        }
    }

}
