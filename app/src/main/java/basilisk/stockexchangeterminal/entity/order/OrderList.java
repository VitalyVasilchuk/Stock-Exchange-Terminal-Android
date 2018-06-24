package basilisk.stockexchangeterminal.entity.order;

import java.io.Serializable;
import java.util.List;

public class OrderList {

    private boolean auth;
    private String balance_buy;
    private String balance_sell;
    private String description;
    private List<Order> your_open_orders;

    public List<Order> getYour_open_orders() {
        return your_open_orders;
    }

    public List<Order> getList() {
        return getYour_open_orders();
    }

    public String getBalance_buy() {
        return balance_buy;
    }

    public boolean isAuth() {
        return auth;
    }

    public String getBalance_sell() {
        return balance_sell;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "OrderList{" + "balance_buy=" + balance_buy + ", auth=" + auth + ", balance_sell=" + balance_sell + ", description=" + description + "}";
    }

    public static class Order implements Serializable {

        private String id; // идентификатор заявки
        private String type; // типа покупка - buy, продажа - sell
        private String amnt_trade; // сумма в валюте торга
        private String amnt_base; // сумма в базовой валюте
        private String price; // цена из расчета за одну единицу валюты торга

        public Order(String type, String price, String volume, String amount) {
            this.type = type;
            this.amnt_trade = volume;
            this.amnt_base = amount;
            this.price = price;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getAmnt_trade() {
            return amnt_trade;
        }

        public String getAmnt_base() {
            return amnt_base;
        }

        public String getPrice() {
            return price;
        }

        @Override
        public String toString() {
            return "Order{" + "id=" + id + ", type=" + type + ", amnt_trade=" + amnt_trade + ", amnt_base=" + amnt_base + ", price=" + price + '}';
        }

    }

}
