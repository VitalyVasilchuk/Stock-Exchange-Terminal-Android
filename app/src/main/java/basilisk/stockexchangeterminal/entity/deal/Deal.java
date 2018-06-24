/*
Описание атрибутов сущности Сделка (Deal)
*/
package basilisk.stockexchangeterminal.entity.deal;

import java.io.Serializable;

public class Deal implements Serializable{

    private Long id; // идентификатор
    private String pub_date; // дата сделки
    private String type; // тип операции sell/buy  
    private String price; // цена
    private String amnt_base; // сумма сделки в базовой валюте,  
    private String amnt_trade; // сумма сделки в валюте торга
    private String user; // участник сделки 

    public Deal(Long id, String type, String date, String price, String volume, String amount, String user) {
        this.id = id;
        this.pub_date = date;
        this.type = type;
        this.amnt_base = amount;
        this.amnt_trade = volume;
        this.price = price;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPub_date() {
        return pub_date;
    }

    public void setPub_date(String pub_date) {
        this.pub_date = pub_date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAmnt_base() {
        return amnt_base;
    }

    public void setAmnt_base(String amnt_base) {
        this.amnt_base = amnt_base;
    }

    public String getAmnt_trade() {
        return amnt_trade;
    }

    public void setAmnt_trade(String amnt_trade) {
        this.amnt_trade = amnt_trade;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Deal{" + "id=" + id + ", pub_date=" + pub_date + ", type=" + type + ", amnt_base=" + amnt_base + ", amnt_trade=" + amnt_trade + ", price=" + price + ", user=" + user + '}';
    }

}
