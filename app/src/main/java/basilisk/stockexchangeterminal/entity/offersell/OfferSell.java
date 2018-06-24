package basilisk.stockexchangeterminal.entity.offersell;

import java.io.Serializable;
import java.util.List;

public class OfferSell {

    public class Offer implements Serializable {

	private String currency_trade; // сумма на продажу в валюте торга 
	private String price; // цена из расчета за одну целую единицу валюты торга
	private String currency_base; // сумма  сделки в базовой валюте

	public String getCurrency_trade() {
	    return currency_trade;
	}

	public String getPrice() {
	    return price;
	}

	public String getCurrency_base() {
	    return currency_base;
	}

	@Override
	public String toString() {
	    return "Offer{" + "currency_trade=" + currency_trade + ", price=" + price + ", currency_base=" + currency_base + '}';
	}

    }

    private String min_price; // минимальная цена в списке заявок
    private List<Offer> list; // список заявок на продажу содежащий объекты заявок.
    private String orders_sum; // сумма всех заявок на продажу в валюте торга 
    private String max_price; // максимальная  цена в списке заявок

    public OfferSell() {
    }

    public String getMin_price() {
	return min_price;
    }

    public List<Offer> getList() {
	return list;
    }

    public String getOrders_sum() {
	return orders_sum;
    }

    public String getMax_price() {
	return max_price;
    }

    @Override
    public String toString() {
	return "OfferBuy{" + "min_price=" + min_price + ", orders_sum=" + orders_sum + ", max_price=" + max_price + '}';
    }



}
