package basilisk.stockexchangeterminal.api;

import java.util.concurrent.TimeUnit;

import basilisk.stockexchangeterminal.BuildConfig;
import basilisk.stockexchangeterminal.entity.RateNbu;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NbuService {
    String ENDPOINT = "https://bank.gov.ua/NBUStatService/v1/statdirectory/";
    String TAG = "NbuService";

    @GET("exchange/")
    Call<RateNbu[]> getExchange(@Query("valcode") String valcode, @Query("json") String json);

    class Factory {
        private static NbuService create() {
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
                    .readTimeout(10, TimeUnit.SECONDS)
                    .connectTimeout(15, TimeUnit.SECONDS);

            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
                interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(interceptor).build();
            }

            OkHttpClient client = builder.build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(NbuService.ENDPOINT)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            return retrofit.create(NbuService.class);
        }

        public static Call<RateNbu[]> getExchangeRate(String valcode) {
            return create().getExchange(valcode, "");
        }
    }
}
