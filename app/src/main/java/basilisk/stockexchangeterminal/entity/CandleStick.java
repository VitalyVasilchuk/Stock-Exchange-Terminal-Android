package basilisk.stockexchangeterminal.entity;

import java.io.Serializable;
import java.util.List;

public class CandleStick {

    public class CandleItem  implements Serializable {
        private long datatime;  // 0 время
        private float open;     // 1 цена открытия торгов
        private float high;     // 2 максимальная цена интервала
        private float low;      // 3 минимальная цена интервала
        private float close;    // 4 цена  закрытия интервала
        private float volume;   // 5 объём  торгов интервала

        public long getDatatime() {
            return datatime;
        }

        public float getOpen() {
            return open;
        }

        public float getHigh() {
            return high;
        }

        public float getLow() {
            return low;
        }

        public float getClose() {
            return close;
        }

        public float getVolume() {
            return volume;
        }

        @Override
        public String toString() {
            return "CandleItem{" +
                    "datatime=" + datatime + ", open=" + open + ", high=" + high + "," +
                    " low=" + low + ", close=" + close + ", volume=" + volume + "}";
        }
    }

    private String volume_trade;
    private String volume_base;
    private int online;
    private List trades;

    public String getVolume_trade() {
        return volume_trade;
    }

    public String getVolume_base() {
        return volume_base;
    }

    public int getOnline() {
        return online;
    }

    public List getList() {
        return trades;
    }

    @Override
    public String toString() {
        return "CandleStick{" +
                "volume_trade='" + volume_trade + '\'' +
                ", volume_base='" + volume_base + '\'' +
                ", online=" + online +
                '}';
    }
}
