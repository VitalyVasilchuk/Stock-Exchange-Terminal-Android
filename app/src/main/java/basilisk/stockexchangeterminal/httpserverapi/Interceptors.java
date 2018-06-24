package basilisk.stockexchangeterminal.httpserverapi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import basilisk.stockexchangeterminal.App;
import basilisk.stockexchangeterminal.BuildConfig;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class Interceptors {
    private static final int MAX_AGE = 120;
    private static final long MAX_STALE = 86400;

    public static class OfflineCacheInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            if (isNetworkAvailable()) {
                CacheControl cacheControl = new CacheControl.Builder()
                        .maxStale(1, TimeUnit.DAYS)
                        .build();
                request = request.newBuilder()
                        .header("Cache-Control", cacheControl.toString())
                        .build();
            }
            return chain.proceed(request);
        }
    }


    public static class NetworkCacheInterceptor implements Interceptor {
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();

            String cacheHeaderValue = isNetworkAvailable()
                    ? "public, max-age=" + MAX_AGE
                    : "public, only-if-cached, max-stale=" + MAX_STALE;
            Request request = originalRequest.newBuilder().build();
            Response response = chain.proceed(request);
            return response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", cacheHeaderValue)
                    .build();
        }
    }

    public static HttpLoggingInterceptor loggingInterceptor(final String tag) {
        return new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Log.d(tag, message);
            }
        })
                .setLevel(HttpLoggingInterceptor.Level.BODY);
    }

    public static Cache provideCache() {
        Cache cache = null;
        try {
            File dir = App.getAppContext().getCacheDir();
            cache = new Cache(new File(dir, "http-cache"), 10 * 1024 * 1024); // 10 MB
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cache;
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivity = (ConnectivityManager) App.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // перехватчик для повторной отправки "неуспешных" запросов
    public static class repeatInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);
            boolean responseOK = response.isSuccessful();
            int tryCount = 0;

            while (!responseOK && tryCount < 3) {
                try {
                    response = chain.proceed(request);
                    responseOK = response.isSuccessful();
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) Log.d("intercept", "Request is not successful - " + tryCount);
                } finally {
                    tryCount++;
                }
            }
            return response;
        }
    }
}
