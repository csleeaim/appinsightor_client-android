package appinsightor.com.sdk_appinsightor;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.UUID;

/**
 * Device 에 대한 정보를 획득하고 취급한다.
 * <ul>
 *     <li>OS Name</li>
 *     <li>OS Version</li>
 *     <li>Device Model</li>
 *     <li>해상도</li>
 *     <li>디스플레이 밀도(density)</li>
 *     <li>네트워크 운영자</li>
 *     <li>현재 지역(e.g "ko_KR")</li>
 *     <li>고유한 Device ID</li>
 *     <li>Application Version</li>
 * </ul>
 * Device에 대한 정보들을 JSON 형태로 변환한다.
 */
class DeviceInfo {

    /**
     * OS Name - "Android"
     */
    static String getOS() {
        return "Android";
    }

    /**
     * OS Version
     */
    static String getOSVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * Device Model - Build
     */
    static String getDevice() {
        return Build.MODEL;
    }

    /**
     * Get 해상도(current default display)
     */
    static String getResolution(final Context context) {
        String resolution = "";
        try {
            final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            final Display display = wm.getDefaultDisplay();
            final DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            resolution = metrics.widthPixels + "x" + metrics.heightPixels;
        } catch (Throwable t) {
            if (AppInsightor.sharedInstance().isLoggingEnabled()) {
                Log.i(AppInsightor.TAG, "Unable to verify resolution!");
            }
        }
        return resolution;
    }

    /**
     * 디스플레이 밀도 current display density
     */
    static String getDensity(final Context context) {
        String densityStr = "";
        final int density = context.getResources().getDisplayMetrics().densityDpi;
        switch (density) {
            case DisplayMetrics.DENSITY_LOW:
                densityStr = "LDPI";
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                densityStr = "MDPI";
                break;
            case DisplayMetrics.DENSITY_TV:
                densityStr = "TVDPI";
                break;
            case DisplayMetrics.DENSITY_HIGH:
                densityStr = "HDPI";
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                densityStr = "XHDPI";
                break;
            case DisplayMetrics.DENSITY_400:
                densityStr = "XMHDPI";
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                densityStr = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                densityStr = "XXXHDPI";
                break;
        }
        return densityStr;
    }

    /**
     * 네트워크 운영자 current network operator, TelephonyManager (default. "Android")
     */
    static String getCarrier(final Context context) {
        String carrier = "";
        final TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager != null) {
            carrier = manager.getNetworkOperatorName();
        }
        if (carrier == null || carrier.length() == 0) {
            carrier = "";
            if (AppInsightor.sharedInstance().isLoggingEnabled()) {
                Log.i(AppInsightor.TAG, "Unable to verify network operator!");
            }
        }
        return carrier;
    }

    /**
     * 현재 지역(e.g/ "ko_KR")
     */
    static String getLocale() {
        final Locale locale = Locale.getDefault();
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    /**
     *  GPS 정보 (위도 경도)
     */
    static JSONObject getGeolocation(Context context) {
        JSONObject jsonObject = new JSONObject();
        // GPS 권한 체크
        Location location;
        if (ContextCompat.checkSelfPermission( context, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED) {

            GPSTracker gpsTracker = new GPSTracker(context);
            location = gpsTracker.getLocation();

            try {
                jsonObject.put("latitude", location.getLatitude());
                jsonObject.put("longitude", location.getLongitude());
            } catch (JSONException ignored) {

            } catch (NullPointerException e) {
                try {
                    jsonObject.put("latitude", 0);
                    jsonObject.put("longitude", 0);
                } catch (JSONException ignored) {}
            }
        } else {
//            Log.d("getGeolocation", "not have permission");
            try {
                jsonObject.put("latitude", 0);
                jsonObject.put("longitude", 0);
            } catch (JSONException ignored) {}
        }

        return jsonObject;

    }

    /**
     * 고유한 Device ID 값을 생성한다.(UUID)
     * <p>다른방법</p>
     * 다른방법안드로이드 2.3 진저브레드 버전부터는 android.os.Build.SERIAL 값을 활용<br />
     * Settings.Secure.ANDROID_ID Constant 값을 활용(Android 4.2 이상)<br />
     * - 이 값은 64bit 크기의 고유 값으로 디바이스가 최초 부팅 될 때 생성되어 저장되며, 디바이스가 초기화 되는 경우에는 삭제된다.<br />
     * - 개별 디바이스를 식별하기 위해서 ANDROID_ID 값을 활용하는 것이 가장 적절한 선택
     *
     * @return 무작위 인 64 비트 숫자 (16 진수 문자열)
     */
    public synchronized static String initializeDeviceID(Context context) {
        String sID = null;
        final String INSTALLATION = "INSTALLATION";

        if (Build.SERIAL != null && Build.SERIAL.length() > 12) {
            sID = Build.SERIAL;
        } else if (Settings.Secure.ANDROID_ID != null && Settings.Secure.ANDROID_ID.length() > 12) {
            sID = Settings.Secure.ANDROID_ID;
        } else if(sID == null) {
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                if (!installation.exists())
                    writeInstallationFile(installation);
                sID = readInstallationFile(installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return sID;
    }

    /**
     * 고유한 Device ID 값을 생성하기 위한 호출 Method<br />
     * called initializeDeviceID()
     * @throws IOException
     */
    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    /**
     * 고유한 Device ID 값을 생성하기 위한 호출 Method<br />
     * called initializeDeviceID()
     * @throws IOException
     */
    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }

    /**
     * application version<br />
     * 구글스토어 패키지 등록 버전을 체크하여 없을 경우 사용자가 정의한 version을 사용한다.
     */
    static String getAppVersion(final Context context) {
        String result = AppInsightor.APPINSIGHTOR_DEFAULT_APP_VERSION;
        try {
            result = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        }
        catch (PackageManager.NameNotFoundException e) {
            if (AppInsightor.sharedInstance().isLoggingEnabled()) {
                Log.i(AppInsightor.TAG, "default app version not found!");
            }
        }
        return result;
    }

    /**
     * device key-value 값들에 대해 json 형태로 변환하고, 그 값들을 수신 서버에 전송하기 위해 URL endcoding.
     * @return URL-encoded JSON string, Device Information
     */
    static String getMetrics(final Context context) {
        final JSONObject json = new JSONObject();

        fillJSONIfValuesNotEmpty(json,
                "_device", getDevice(),
                "_os", getOS(),
                "_os_version", getOSVersion(),
                "_carrier", getCarrier(context),
                "_resolution", getResolution(context),
                "_density", getDensity(context),
                "_locale", getLocale()
                );

        // geolocation의 Value가 JSONObject 타입이라 별도로 추가
        fillJSONIfValuesNotEmpty(json, "_geolocation", getGeolocation(context));

        String result = json.toString();

        try {
            result = java.net.URLEncoder.encode(result, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {}

        return result;
    }

    /**
     * 문자열의 집합, 즉 "key1","value1","key2","value2",...의 값들을 입력 받아 JSON 형태로 변환, 리턴한다.<br />
     * 문자열은 항상 key-value 쌍을 이루며, 값이 없을 경우 key를 포함한 key-value 쌍 자체가 제외된다.<br />
     * called getMetrics()
     * @param json 변환된 값을 담아 리턴할 json 변수
     * @param objects 문자열의 집합 ("key1","value1","key2","value2", more)
     */
    static void fillJSONIfValuesNotEmpty(final JSONObject json, final String ... objects) {
        try {
            if (objects.length > 0 && objects.length % 2 == 0) {
                for (int i = 0; i < objects.length; i += 2) {
                    final String key = objects[i];
                    final String value = objects[i + 1];
                    if (value != null && value.length() > 0) {
                        json.put(key, value);
                    }
                }
            }
        } catch (JSONException ignored) {}
    }

    /**
     * Value가 JSONObject 타입일때는 이 함수를 이용한다 사전에 Value가 JSONObject타입이어야 한다.<br />
     * called getMetrics()
     * @param json 변환된 값을 담아 리턴할 json 변수
     * @param key key
     * @param value JSONObject
     */
    static void fillJSONIfValuesNotEmpty(final JSONObject json, final String key, final JSONObject value) {
        try {
            if (value != null && value.length() > 0) {
                json.put(key, value);
            }
        } catch (JSONException ignored) {}
    }
}
