package basilisk.stockexchangeterminal.entity;
public class Ticker {

    private String currency_trade;
    private String currency_base;
    private String buy;
    private String buy_usd;
    private String sell;
    private String sell_usd;
    private String last;
    private String last_usd;
    private String vol_cur;
    private String vol_curUsd;
    private String low;
    private String high;
    private String avg;
    private String vol;
    private String usd_rate;
    private Integer updated;

    public String getCurrencyTrade() {
        return currency_trade;
    }

    public void setCurrencyTrade(String currency_trade) {
        this.currency_trade = currency_trade;
    }

    public String getCurrencyBase() {
        return currency_base;
    }

    public void setCurrencyBase(String currency_base) {
        this.currency_base = currency_base;
    }

    public String getBuy() {
        return buy;
    }

    public void setBuy(String buy) {
        this.buy = buy;
    }

    public String getBuyUsd() {
        return buy_usd;
    }

    public void setBuyUsd(String buy_usd) {
        this.buy_usd = buy_usd;
    }

    public String getSell() {
        return sell;
    }

    public void setSell(String sell) {
        this.sell = sell;
    }

    public String getSellUsd() {
        return sell_usd;
    }

    public void setSellUsd(String sell_usd) {
        this.sell_usd = sell_usd;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public String getLastUsd() {
        return last_usd;
    }

    public void setLastUsd(String last_usd) {
        this.last_usd = last_usd;
    }

    public String getVolCur() {
        return vol_cur;
    }

    public void setVolCur(String vol_cur) {
        this.vol_cur = vol_cur;
    }

    public String getVolCurUsd() {
        return vol_curUsd;
    }

    public void setVolCurUsd(String vol_curUsd) {
        this.vol_curUsd = vol_curUsd;
    }

    public String getLow() {
        return low;
    }

    public void setLow(String low) {
        this.low = low;
    }

    public String getHigh() {
        return high;
    }

    public void setHigh(String high) {
        this.high = high;
    }

    public String getAvg() {
        return avg;
    }

    public void setAvg(String avg) {
        this.avg = avg;
    }

    public String getVol() {
        return vol;
    }

    public void setVol(String vol) {
        this.vol = vol;
    }

    public String getUsdRate() {
        return usd_rate;
    }

    public void setUsdRate(String usd_rate) {
        this.usd_rate = usd_rate;
    }

    public Integer getUpdated() {
        return updated;
    }

    public void setUpdated(Integer updated) {
        this.updated = updated;
    }

    public String getFormattedBuy() {
        return String.format("%.5f", Float.parseFloat(buy)).replace(",", ".");
    }

    public String getFormattedSell() {
        return String.format("%.5f", Float.parseFloat(sell)).replace(",", ".");
    }

    @Override
    public String toString() {
        return "Ticker{" +
                "currency_trade='" + currency_trade + '\'' +
                ", currency_base='" + currency_base + '\'' +
                ", buy='" + buy + '\'' +
                ", buy_usd='" + buy_usd + '\'' +
                ", sell='" + sell + '\'' +
                ", sell_usd='" + sell_usd + '\'' +
                ", last='" + last + '\'' +
                ", last_usd='" + last_usd + '\'' +
                ", vol_cur='" + vol_cur + '\'' +
                ", vol_curUsd='" + vol_curUsd + '\'' +
                ", low='" + low + '\'' +
                ", high='" + high + '\'' +
                ", avg='" + avg + '\'' +
                ", vol='" + vol + '\'' +
                ", usd_rate='" + usd_rate + '\'' +
                ", updated=" + updated +
                '}';
    }
}