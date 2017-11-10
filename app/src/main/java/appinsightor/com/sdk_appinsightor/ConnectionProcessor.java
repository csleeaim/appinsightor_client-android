package appinsightor.com.sdk_appinsightor;

import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static android.content.ContentValues.TAG;

/**
 * ConnectionProcessor는 백그라운드에서 실행되는 Runnable이다.
 * 요청메시지 데이터를 수신서버로 전송한다.
 * 요청메시지 데이터가 crash 데이터 또는 2048 길이보다 클 경우 POST 방식으로 전달하며 그 외에는 GET 방식으로 전달한다.
 * HttpPostOnlyRequest 값이 true 일경우 모든 요청 메시지를 강제적으로 POST 방식으로 전달한다.
 */
public class ConnectionProcessor implements Runnable {
    private static final int CONNECT_TIMEOUT_IN_MILLISECONDS = 30000;
    private static final int READ_TIMEOUT_IN_MILLISECONDS = 30000;

    private final AppInsightorStore store_;
    private final String deviceId_;
    private final String serverURL_;

    protected static String checksum;
    protected static Boolean HttpPostOnlyRequest = false;


    /**
     * FROYO(API 8 Level) 이전의 안드로이드에서 HttpURLConnection을 사용할 경우 readable input stream에서 close()를 호출하면
     * 전체 connection pool에 쓰레기값이 들어갈 수도 있는 현상이 발생하므로, connection pooling을 off 해주어야 한다.
     * e.g. if (Integer.parseInt(Build.VERSION_SDK) < Build.VERSION_CODES.FROYO) {
     *      System.setProperty("http.keepAlive", "false");
     *      }
     * @param serverURL 수신 서버
     * @param store 보낼 요청 메시지
     * @param deviceId
     */
    ConnectionProcessor(final String serverURL, final AppInsightorStore store, final String deviceId) {
        AppInsightor.sharedInstance().logShowPrinting("");
        serverURL_ = serverURL;
        store_ = store;
        deviceId_ = deviceId;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {//API 8 Level
            System.setProperty("http.keepAlive", "false");
        }
    }

    /**
     * 요청 메시지 데이타 전송
     * 메시지가 crash 데이타 또는 2048 길이보다 클 경우 POST 방식으로 전달하며 그 외에는 GET 방식으로 전달
     * HttpPostOnlyRequest 값이 true 일경우 모든 요청 메시지를 강제적으로 POST 방식으로 전달한다.
     * @param eventData 요청메시지 데이타
     * @return HttpURLConnection conn
     * @throws IOException
     */
    URLConnection urlConnectionForEventData(final String eventData) throws IOException {
        AppInsightor.sharedInstance().logShowPrinting("start");

//        String urlStr = serverURL_ + "/i.php?";
        String urlStr = serverURL_ + "/ne.nfl?";


        //별도로 checksum의 값만 GET 방식으로 전달
        if(!HttpPostOnlyRequest && !eventData.contains("&crash=") && eventData.length() < 2048) {
            urlStr += eventData;
            if (checksum != null) urlStr += "&checksum=" + sha1Hash(checksum);
        } else {
            if (checksum != null)urlStr += "checksum=" + sha1Hash(checksum);
        }

        final URL url = new URL(urlStr);

        HttpURLConnection conn = null;

            // https certificate throw
//            trustAllHosts();
//            httpsConn = (HttpsURLConnection)url.openConnection();
//            httpsConn.setHostnameVerifier(new HostnameVerifier() {
//                @Override
//                public boolean verify(String s, SSLSession sslSession) {
//                    return true;
//                }
//            });

        conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_IN_MILLISECONDS);
        conn.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
        conn.setUseCaches(false);
        conn.setDoInput(true);

        if(!HttpPostOnlyRequest && !eventData.contains("&crash=") && eventData.length() < 2048) {
            conn.setDoOutput(true);
        } else {
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(eventData);
            writer.flush();
            writer.close();
            os.close();
        }

        AppInsightor.sharedInstance().logShowPrinting("end");

        return conn;
    }

    /**
     * ok-
     * ConnectionProcessor runnable start
     * 200번대(성공) 클라이언트의 요구가 성공적으로 수신되어 처리되었음을 의미한다.
     * 300번대(리다이렉션) 해당 요구 사항을 처리하기 위해 사용자 에이전트에 의해 수행되어야 할 추가적인 동작이 있음을 의미한다.
     * 400번대(클라이언트 측 에러) 클라이언트에 오류가 발생한 경우 사용된다. 예를 들면 클라이언트가 서버에 보내는 요구 메시지를 완전히 처리하지 못한 경우 등이다.
     * 500번대(서버 측 에러) 서버 자체에서 발생된 오류 상황이나 요구 사항을 제대로 처리할 수 없을 때 사용된다.
     */
    @Override
    public void run() {
        AppInsightor.sharedInstance().logShowPrinting("start");
        final JSONObject json = new JSONObject();

        while(true) {
            final String[] storedEvents = store_.connections();
            if (storedEvents == null || storedEvents.length == 0) {
                //throw new IllegalStateException("error!");
                break;
            }


            //deviceID check
            if (deviceId_ == null) {
                if (AppInsightor.sharedInstance().isLoggingEnabled()) {
                    Log.w(AppInsightor.TAG, "No Device ID available yet, skipping request " + storedEvents[0]);
                }
                break;
            }



            //전체 데이터를 JSON으로 변환
            String[] values = storedEvents[0].split("&");
            String[][] tokens = new String[values.length][2];
            for (int i = 0; i < values.length; i++) {
                tokens[i] = values[i].split("=");
                try {
                    json.put(tokens[i][0], tokens[i][1]);
                } catch (JSONException ignored) {
                }
            }
            String eventData = "c=" + json.toString().replace("\"%7B","%7B")
                                                     .replace("%7D\"","%7D")
                                                     .replace("\"%5B","%5B")
                                                     .replace("%5D\"","%5D");

            //String eventData = storedEvents[0];
            //Log.e("Log.String: ","---");
            //Log.e("Log.String: ",store_.getPreference(""));
            //Log.e("Log.String: ",store_.getPreference("CONNECTIONS"));
            //Log.e("Log.String: ",store_.getPreference("EVENTS"));
            //위의 store_.getpreference(~~) 를 호출할때 데이터가 사라진다???



            URLConnection conn = null;
            try {
                //initialize and open connection
                conn = urlConnectionForEventData(eventData);
                conn.connect();

                //response code has to be 2xx to be considered a success
                boolean success = true;
                final int responseCode;
                if (conn instanceof HttpURLConnection) {
                    final HttpURLConnection httpConn = (HttpURLConnection) conn;
                    responseCode = httpConn.getResponseCode();
                    success = responseCode >= 200 && responseCode < 300;//success true or false set
                    if (!success && AppInsightor.sharedInstance().isLoggingEnabled()) {
                        Log.w(AppInsightor.TAG, "HTTP error response code was " + responseCode + " from submitting event data: " + eventData);
                    }
                } else {
                    responseCode = 0;
                }

                if (success) {
                    if (AppInsightor.sharedInstance().isLoggingEnabled()) {
                        Log.d(AppInsightor.TAG, "send ok ->" + eventData);
                    }

                    //정상적인 수신서버 전송 후 local store Queue 의 데이터를 제거한다.
                    store_.removeConnection(storedEvents[0]);

                    //response code 가 400번대로써 클라이언트 오류가 발생한 경우 response code 와 함께 Queue 에서도 제거한다.
                } else if (responseCode >= 400 && responseCode < 500) {
                    if (AppInsightor.sharedInstance().isLoggingEnabled()) {
                        Log.d(AppInsightor.TAG, "send fail " + responseCode + " ->" + eventData);
                    }
                    store_.removeConnection(storedEvents[0]);
                } else {
                    //그외 300번대(리다이렉션), 500번대(서버측에러)의 경우  처리를 중지하고 다음 틱에서 다시 시도하도록 한다.
                    //throw new IllegalStateException("error!");
                    break;
                }
            } catch (Exception e) {
                if (AppInsightor.sharedInstance().isLoggingEnabled()) {
                    Log.w(AppInsightor.TAG, "Got exception while trying to submit event data: " + eventData, e);
                    Log.w(AppInsightor.TAG, "AA" + e);
                }
                //예외가 발생하였다면 처리를 중지하고 다음 틱에서 다시 시도하도록 한다.
                //throw new IllegalStateException("error!");
                break;
            } finally {
                //free connection resources
                if (conn != null && conn instanceof HttpURLConnection) {
                    ((HttpURLConnection) conn).disconnect();
                }
            }
        }
        AppInsightor.sharedInstance().logShowPrinting("end");
    }



    /**
     * ok-
     * checksum 전송시 SHA-1 Hash 알고리즘으로 암호화
     * @param toHash 지정한 문자열
     * @return Hash value
     */
    private static String sha1Hash (String toHash) {
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance( "SHA-1" );// 이 부분을 SHA-256, MD5로만 바꿔주면 된다.
            byte[] bytes = toHash.getBytes("UTF-8");
            md.update(bytes, 0, bytes.length);//전달된 인자값 toHash를 SHA-1으로 변환 준비
            bytes = md.digest();

            StringBuffer sb = new StringBuffer();
            for(int i=0; i<bytes.length; i++) {
                sb.append(Integer.toString((bytes[i]&0xff) + 0x100, 16).substring(1));
            }

            hash = sb.toString();
        }
        catch( Throwable e ) {
            if (AppInsightor.sharedInstance().isLoggingEnabled()) {
                Log.d(AppInsightor.TAG, "Cannot executed sha1Hash", e);
            }
        }
        return hash;
    }

    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            @Override
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] chain,
                    String authType)
                    throws java.security.cert.CertificateException {
                Log.e("checkClientTrusted:", "client");
                // TODO Auto-generated method stub

            }

            @Override
            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] chain,
                    String authType)
                    throws java.security.cert.CertificateException {
                Log.e("checkClientTrusted:", "server");
                // TODO Auto-generated method stub

            }
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //String getServerURL() { return serverURL_; }
    //AppInsightorStore getAppInsightorStore() { return store_; }
    //DeviceId getDeviceId() { return deviceId_; }
}
