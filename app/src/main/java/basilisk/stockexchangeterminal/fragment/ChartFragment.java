package basilisk.stockexchangeterminal.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.entity.CandleStick;
import basilisk.stockexchangeterminal.api.HttpServerApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ChartFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private Context context;
    private ArrayList dataList;
    private CombinedChart combinedChart;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currencyPair;
    private int period = 2;
    private float chartVol;
    private float chartLow;
    private float chartHigh;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chart, container, false);
        context = view.getContext();

        combinedChart = view.findViewById(R.id.chart);
        combinedChart.setNoDataText(getString(R.string.chart_empty));
        combinedChart.setNoDataTextColor(getResources().getColor(R.color.secondary_text));

        swipeRefreshLayout = view.findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        if (savedInstanceState == null) {
            dataList = new ArrayList();
            currencyPair = (String) getArguments().get("currencyPair");
            loadDataFromHTTPServer(currencyPair);
        } else {
            dataList = savedInstanceState.getParcelableArrayList("DATA_LIST");
            currencyPair = savedInstanceState.getString("CURRENCY_PAIR");
            if (dataList.size() > 0) prepareCombinedChart();
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelableArrayList("DATA_LIST", dataList);
        savedInstanceState.putString("CURRENCY_PAIR", currencyPair);
    }

    private void prepareCombinedChart() {
        // настройка параметров диаграммы
        combinedChart.getDescription().setEnabled(true);
        //combinedChart.setBackgroundColor(getResources().getColor(R.color.primary_light));
        combinedChart.setDrawGridBackground(false);
        combinedChart.setDrawBarShadow(false);
        combinedChart.setHighlightFullBarEnabled(false);
        combinedChart.setDrawBorders(true);
        combinedChart.setBorderColor(getResources().getColor(R.color.divider));
        combinedChart.setScaleEnabled(false);

        // обработка касания элементов диаграммы
        combinedChart.setTouchEnabled(false); // отключил, пока не разберусь с отображением маркера
        combinedChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e == null) return;
                ArrayList a = (ArrayList) dataList.get(Math.round(e.getX()));
                if (a != null) {
                    String textMessage = "" +
                            "H: " + a.get(2).toString() + "\n" +
                            "L: " + a.get(3).toString() + "\n" +
                            "O: " + a.get(1).toString() + "\n" +
                            "C: " + a.get(4).toString() + "\n" +
                            "V: " + a.get(5).toString();
                    Toast.makeText(context, textMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {

            }
        });

        // настройка легенды, связанной с диаграммой
        Legend l = combinedChart.getLegend();
        l.setWordWrapEnabled(true);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);

        // формирование данных для каждого графика (24 часа)
        CandleData dataCS = generateCandleData(24 * period);
        BarData dataVolume = generateVolumeData(24 * period);

        CombinedData dataComb = new CombinedData();
        dataComb.setData(dataCS);
        dataComb.setData(dataVolume);

        // настройка отображения оси X внизу
        XAxis xAxis = combinedChart.getXAxis();
        xAxis.setAxisMinimum(dataComb.getXMin() - 1f);
        xAxis.setAxisMaximum(dataComb.getXMax() + 1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                String patternM30 = "HH:mm";
                String patternH1 = "HH:mm";
                String patternD1 = "HH:mm";
                String patternW1 = "dd MMM yyyy";

                ArrayList a = (ArrayList) dataList.get(Math.round(value));
                long l = Math.round((Double) a.get(0));
                //long l = Math.round(value);
                Date d = new Date(l);
                String s = new SimpleDateFormat(patternM30).format(d);
                return s;
            }
        });

        // настройка отображения оси Y справа
        YAxis rightAxis = combinedChart.getAxisRight();
        rightAxis.setDrawGridLines(true);
        rightAxis.setAxisMaximum((float) (dataCS.getYMax() + dataCS.getYMax() * 0.01));
        rightAxis.setAxisMinimum((float) (dataCS.getYMin() - dataCS.getYMin() * 0.02));
        rightAxis.setEnabled(true);

        // настройка отображения оси Y слева
        YAxis leftAxis = combinedChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum((float) (dataVolume.getYMax() * 4));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setEnabled(true);

        // загрузка данных всех диаграмм и отрисовка
        combinedChart.setData(dataComb);
        combinedChart.animateXY(1000, 1000);
        combinedChart.invalidate();

        // формирование и размещение описания
        String chartDesc = "24h V: " +
                String.format("%f", chartVol) + "; L: " +
                String.format("%f", chartLow) + "; H: " +
                String.format("%f", chartHigh)+ ";";
        Point size = new Point(); getActivity().getWindowManager().getDefaultDisplay().getSize(size);
        float x = size.x - combinedChart.getViewPortHandler().offsetRight();
        float y = combinedChart.getViewPortHandler().offsetTop();
        combinedChart.getDescription().setPosition(x, y - Utils.calcTextHeight(new Paint(), chartDesc));
        combinedChart.getDescription().setTextSize(10f);
        combinedChart.getDescription().setTypeface(Typeface.DEFAULT_BOLD);
        combinedChart.getDescription().setText(chartDesc);
    }

    private void loadDataFromJson() {
        // чтение JSON из файла-ресурса
        InputStream is = getResources().openRawResource(R.raw.candlestick);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
            is.close();
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            System.out.println(e.toString());
        } finally {
        }

        // преобразование полученого JSON в экземпляр класса и получение списка данных
        String jsonString = writer.toString();
        Gson gson = new Gson();
        CandleStick cs = gson.fromJson(jsonString, CandleStick.class);
        dataList.clear();
        dataList.addAll(cs.getList());
    }

    private void loadDataFromHTTPServer(String currencyPair) {
        swipeRefreshLayout.setRefreshing(true);
        Call<CandleStick> candleStickCall = HttpServerApi.Factory.candleStickData(currencyPair);
        candleStickCall.enqueue(new Callback<CandleStick>() {
            @Override
            public void onResponse(Call<CandleStick> call, Response<CandleStick> response) {
                Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    if (response.isSuccessful()) {
                        if (response.body().getList().size() > 0) {
                            dataList.clear();
                            dataList.addAll(response.body().getList());
                            prepareCombinedChart();
                        }
                    } else {
                        Toast.makeText(context, getString(R.string.server_request_error) +
                                " #" + response.code() + "\n" + response.message(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<CandleStick> call, Throwable t) {
                Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(context,
                            getString(R.string.server_request_error) + "\n" + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private CandleData generateCandleData(int shift) {
        // формирование списка данных для диаграммы свечей
        ArrayList<CandleEntry> candleList = new ArrayList<CandleEntry>();

        if (candleList.size() == 0 && dataList.size() > 0) {
            chartLow = 0f;
            chartHigh = 0f;
            for (int i = dataList.size() - shift; i < dataList.size(); i++) {
                ArrayList a = (ArrayList) dataList.get(i);
                float low = (float) (double) a.get(3);
                float high = (float) (double) a.get(2);
                candleList.add(new CandleEntry(
                        //(float) (double) a.get(0),
                        i,
                        high,
                        low,
                        (float) (double) a.get(1),
                        (float) (double) a.get(4),
                        getResources().getDrawable(R.drawable.btc)));
                if (low < chartLow || chartLow == 0) chartLow = low;
                if (high > chartHigh || chartHigh == 0) chartHigh = high;
            }
        }

        // настройка отображения набора данных по свечам
        CandleDataSet dataSet = new CandleDataSet(candleList, "price");
        dataSet.setDrawIcons(false);
        dataSet.setDrawValues(false);
        dataSet.setShadowColor(Color.BLACK);
        dataSet.setShadowWidth(0.7f);
        dataSet.setDecreasingColor(0xFFD32F2F);
        dataSet.setDecreasingPaintStyle(Paint.Style.FILL);
        dataSet.setIncreasingColor(0xFF388E3C);
        dataSet.setIncreasingPaintStyle(Paint.Style.FILL);
        dataSet.setNeutralColor(0xFF303F9F);
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);

        return new CandleData(dataSet);
    }

    private BarData generateVolumeData(int shift) {
        ArrayList<BarEntry> barList = new ArrayList<BarEntry>();

        if (barList.size() == 0 && dataList.size() > 0) {
            chartVol = 0f;
            for (int i = dataList.size() - shift; i < dataList.size(); i++) {
                ArrayList a = (ArrayList) dataList.get(i);
                barList.add(new BarEntry(
                        //(float) (double) a.get(0),
                        i,
                        (float) (double) a.get(5)
                ));
                chartVol += (float) (double) a.get(5);
            }
        }

        BarDataSet dataSet = new BarDataSet(barList, "volume");
        dataSet.setDrawValues(false);
        dataSet.setColor(getResources().getColor(R.color.divider));

        return new BarData(dataSet);
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                loadDataFromHTTPServer(currencyPair);
            }
        });
    }
}
