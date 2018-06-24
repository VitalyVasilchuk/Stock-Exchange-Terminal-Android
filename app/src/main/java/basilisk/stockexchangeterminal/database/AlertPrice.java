package basilisk.stockexchangeterminal.database;

public class AlertPrice {
    // Таблица
    static final String TABLE = "alert_price";
    // Колонки таблицы
    public static final String COL_ID = "_id";
    public static final String COL_ACTIVE = "active";
    public static final String COL_ALERT = "alert";
    public static final String COL_CURR_PAIR = "currency_pair";
    public static final String COL_LOWER = "lower";
    public static final String COL_HIGHER = "higher";

    private long id;
    private boolean active;
    private boolean alert;
    private String currencyPair;
    private float lower;
    private float higher;

    public AlertPrice(long id, boolean active, boolean alert, String currencyPair, float lower, float higher) {
        this.id = id;
        this.active = active;
        this.alert = alert;
        this.currencyPair = currencyPair;
        this.lower = lower;
        this.higher = higher;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAlert() {
        return alert;
    }

    public void setAlert(boolean alert) {
        this.alert = alert;
    }

    public String getCurrencyPair() {
        return currencyPair;
    }

    public void setCurrencyPair(String currencyPair) {
        this.currencyPair = currencyPair;
    }

    public float getLower() {
        return lower;
    }

    public void setLower(float lower) {
        this.lower = lower;
    }

    public float getHigher() {
        return higher;
    }

    public void setHigher(float higher) {
        this.higher = higher;
    }

    @Override
    public String toString() {
        return "AlertPrice{" +
                "id=" + id +
                ", active=" + active +
                ", alert=" + alert +
                ", currencyPair='" + currencyPair + '\'' +
                ", lower=" + lower +
                ", higher=" + higher +
                '}';
    }
}
