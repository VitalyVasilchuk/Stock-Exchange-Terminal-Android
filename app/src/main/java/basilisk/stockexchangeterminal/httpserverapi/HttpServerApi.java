package basilisk.stockexchangeterminal.httpserverapi;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import basilisk.stockexchangeterminal.BuildConfig;
import basilisk.stockexchangeterminal.SingletonSession;
import basilisk.stockexchangeterminal.entity.account.AccountList;
import basilisk.stockexchangeterminal.entity.candlestick.CandleStick;
import basilisk.stockexchangeterminal.entity.deal.Deal;
import basilisk.stockexchangeterminal.entity.offerbuy.OfferBuy;
import basilisk.stockexchangeterminal.entity.offersell.OfferSell;
import basilisk.stockexchangeterminal.entity.order.OrderList;
import basilisk.stockexchangeterminal.entity.price.PriceList;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface HttpServerApi {
    String ENDPOINT = "https://btc-trade.com.ua/api/";
    String TAG = "HttpServerApi";

    /*************************************************************
     * Public API
     *************************************************************/

    // запрос данных для построения диаграммы японских свечей по валютной паре
    @POST("japan_stat/high/{currencyPair}")
    Call<CandleStick> getCandleStickData(@Path("currencyPair") String currencyPair);

    // запрос списка предложений на покупку по валютной паре
    @POST("trades/buy/{currencyPair}")
    Call<OfferBuy> getOffersBuy(@Path("currencyPair") String currencyPair);

    // запрос списка предложений на продажу по валютной паре
    @POST("trades/sell/{currencyPair}")
    Call<OfferSell> getOffersSell(@Path("currencyPair") String currencyPair);

    // запрос списка совершенных сделок по валютной паре
    @POST("deals/{currencyPair}")
    Call<Deal[]> getDeals(@Path("currencyPair") String currencyPair);

    // запрос цен по валютным парам торгуемых
    @POST("market_prices")
    Call<PriceList> getMarketPrices();

    // запрос тикеров всех рынков
    @POST("ticker")
    Call<Map<String, Object>> getTickerList();

    // запрос тикера валютной пары
    @POST("ticker/{currencyPair}")
    Call<Map<String, Object>> getTicker(@Path("currencyPair") String currencyPair);

    /*************************************************************
     * Private API
     *************************************************************/

    // запрос авторизации
    @FormUrlEncoded
    @POST("auth")
    Call<Map<String, Object>> auth(
            @Header("public-key") String public_key,
            @Header("api-sign") String api_sign,
            @Field("out_order_id") Long out_order_id,
            @Field("nonce") Long nonce
    );

    // запрос остатков по имеющимся счетам
    @FormUrlEncoded
    @POST("balance")
    Call<AccountList> getBalance(
            @Header("public-key") String public_key,
            @Header("api-sign") String api_sign,
            @Field("out_order_id") Long out_order_id,
            @Field("nonce") Long nonce
    );

    // запрос списка открытых ордеров
    @FormUrlEncoded
    @POST("my_orders/{currencyPair}")
    Call<OrderList> getOrder(
            @Path("currencyPair") String currencyPair,
            @Header("public-key") String public_key,
            @Header("api-sign") String api_sign,
            @Field("out_order_id") Long out_order_id,
            @Field("nonce") Long nonce
    );

    // удаление открытого ордера по идентификатору
    @FormUrlEncoded
    //@POST("remove/order/{currencyPair}/{id}")
    @POST("remove/order/{id}")
    Call<Map<String, Object>> removeOrder(
            //@Path("currencyPair") String currencyPair,
            @Path("id") String id,
            @Header("public-key") String public_key,
            @Header("api-sign") String api_sign,
            @Field("out_order_id") Long out_order_id,
            @Field("nonce") Long nonce
    );

    // запрос на создание ордера
    @FormUrlEncoded
    @POST("{operation}/{currencyPair}")
    Call<Map<String, Object>> addOrder(
            @Path("operation") String operation,
            @Path("currencyPair") String currencyPair,
            @Header("public-key") String public_key,
            @Header("api-sign") String api_sign,
            @Field("out_order_id") Long out_order_id,
            @Field("nonce") Long nonce,
            @Field("price") String price,
            @Field("count") String count,
            @Field("currency1") String currency1,
            @Field("currency") String currency
    );

    // запрос списка совершенных сделок владельца по валютной паре
    @FormUrlEncoded
    @POST("my_deals/{currencyPair}")
    Call<Deal[]> getDealsOwner(
            @Path("currencyPair") String currencyPair,
            @Header("public-key") String public_key,
            @Header("api-sign") String api_sign,
            @Field("out_order_id") Long out_order_id,
            @Field("nonce") Long nonce
            //@Field("ts") String ts,
            //@Field("ts1") String ts1
            //@Field("_") Long _
    );

    /*************************************************************
     * Фабрика, возвращающая экземпляр, реализующий текущий API
     *************************************************************/
    class Factory {
        public static HttpServerApi create() {
            // перехватчики кеширования
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
                    .readTimeout(10, TimeUnit.SECONDS)
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .addInterceptor(new Interceptors.repeatInterceptor())
                    //.addInterceptor(new Interceptors.OfflineCacheInterceptor())
                    //.addNetworkInterceptor(new Interceptors.NetworkCacheInterceptor())
                    //.cache(Interceptors.provideCache())
                    ;

            // перехватчик для логгирования
            if (BuildConfig.DEBUG)
                builder.addInterceptor(Interceptors.loggingInterceptor(TAG));

            // перехватчик для повторной отправки "неуспешных" запросов - ВЫНЕС в соответсвующий класс
/*
            builder.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    // try the request
                    Response response = chain.proceed(request);
                    int tryCount = 0;
                    while (!response.isSuccessful() && tryCount < 3) {
                        Log.d("intercept", "Request is not successful - " + tryCount);
                        tryCount++;
                        // retry the request
                        response = chain.proceed(request);
                    }
                    // otherwise just pass the original response on
                    return response;
                }

            });
*/

            //Extra Headers
            OkHttpClient client = builder.build();
            Retrofit retrofit =
                    new Retrofit.Builder()
                            .baseUrl(HttpServerApi.ENDPOINT)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

            return retrofit.create(HttpServerApi.class);
        }

        /*************************************************************
         * Public API
         *************************************************************/
        // получение Call-интерфейса для запроса данных для графика по валютной паре
        public static Call<CandleStick> candleStickData(String currencyPair) {
            HttpServerApi api = HttpServerApi.Factory.create();
            Call<CandleStick> call = api.getCandleStickData(currencyPair);
            return call;
        }

        // получение Call-интерфейса для запроса списка котировок
        public static Call<Map<String, Object>> tickerList() {
            HttpServerApi api = HttpServerApi.Factory.create();
            //Call<TickerList> call = api.getTickerList();
            Call<Map<String, Object>> call = api.getTickerList();
            return call;
        }

        // получение Call-интерфейса для запроса тикера по валютной паре
        public static Call<Map<String, Object>> ticker(String currencyPair) {
            HttpServerApi api = HttpServerApi.Factory.create();
            Call<Map<String, Object>> call = api.getTicker(currencyPair);
            return call;
        }

        /*************************************************************
         * Private API
         *************************************************************/
        // получение Call-интерфейса для запроса авторизации
        public static Call<Map<String, Object>> auth() {
            Long outOrderId = SingletonSession.Instance().getOutOrderId();
            Long nonce = SingletonSession.Instance().getNonce();
            HttpServerApi api = HttpServerApi.Factory.create();
            Call<Map<String, Object>> call = api.auth(
                    SingletonSession.Instance().getPublicKey(),
                    SingletonSession.Instance().getApiSign("out_order_id=" + outOrderId + "&nonce=" + nonce),
                    outOrderId,
                    nonce);
            return call;
        }

        // получение Call-интерфейса для запроса остатков по валютам
        public static Call<AccountList> balance() {
            Long outOrderId = SingletonSession.Instance().getOutOrderId();
            Long nonce = SingletonSession.Instance().getNonce();
            HttpServerApi api = HttpServerApi.Factory.create();
            Call<AccountList> call = api.getBalance(
                    SingletonSession.Instance().getPublicKey(),
                    SingletonSession.Instance().getApiSign("out_order_id=" + outOrderId + "&nonce=" + nonce),
                    outOrderId,
                    nonce);
            return call;
        }

        // получение Call-интерфейса для запроса создания нового ордера покупки/продажи
        public static Call<Map<String, Object>> addOrder(
                String operation, String price, String count, String currency1, String currency) {
            String currencyPair = currency.toLowerCase() + "_" + currency1.toLowerCase();
            Long outOrderId = SingletonSession.Instance().getOutOrderId();
            Long nonce = SingletonSession.Instance().getNonce();
            String params = "out_order_id=" + outOrderId + "&nonce=" + nonce +
                    "&price=" + price + "&count=" + count +
                    "&currency1=" + currency1 + "&currency=" + currency;

            HttpServerApi api = HttpServerApi.Factory.create();
            Call<Map<String, Object>> call = api.addOrder(
                    operation,
                    currencyPair,
                    SingletonSession.Instance().getPublicKey(),
                    SingletonSession.Instance().getApiSign(params),
                    outOrderId,
                    nonce,
                    price,
                    count,
                    currency1,
                    currency);
            return call;
        }
    }
}