package basilisk.stockexchangeterminal.entity;

import java.io.Serializable;
import java.util.List;

public class PriceList implements Serializable{
    private List<Price> prices;

    public List<Price> getPrices() {
        return prices;
    }

    public List<Price> getList() {
/*
        prices.add(new Price("1.20", "ADA_uah_top_price"));
        prices.add(new Price("1.30", "NEO_uah_top_price"));
        prices.add(new Price("1.40", "IOTA_uah_top_price"));
        prices.add(new Price("1.50", "XLM_uah_top_price"));
        prices.add(new Price("1.60", "FNO_uah_top_price"));
*/
        return prices;
    }

    public void setPrices(List<Price> prices) {
        this.prices = prices;
    }

    public class Price implements Serializable {
        private String price;
        private String type;

        public Price(String price, String type) {
            this.price = price;
            this.type = type;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getCurrencyTrade() {
            String s =type.toUpperCase();
            String[] a = s.split("_");
            s = a.length > 0 ? a[0] : "";
            return s;
        }

        public String getCurrencyBase() {
            String s =type.toUpperCase();
            String[] a = s.split("_");
            s = a.length > 0 ? a[1] : "";
            return s;
        }

        public String getCurrencyPair() {
            String s =type.toUpperCase();
            String[] a = s.split("_");
            s = a.length > 0 ? a[0] + "/" + a[1] : "";
            return s;
        }

        @Override
        public String toString() {
            return "Price{" +
                    "price='" + price + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}
