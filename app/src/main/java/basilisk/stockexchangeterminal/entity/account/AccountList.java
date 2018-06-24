package basilisk.stockexchangeterminal.entity.account;

import java.io.Serializable;
import java.util.List;

public class AccountList {
    // счет
    public class Account implements Serializable {
        private String currency;    // валюта счета
        private String balance;     // балас счета

        public String getCurrency() {
            return currency;
        }

        public String getBalance() {
            return balance;
        }

        @Override
        public String toString() {
            return "Account{" + "currency=" + currency + ", balance=" + balance + '}';
        }

    }

    // поля баланса и список счетов
    private int msg_count;
    private List<Account> accounts;
    private boolean use_f2a;
    private int notify_count;
    private String description;
    private boolean status;

    public int getMsg_count() {
        return msg_count;
    }

    public List<Account> getList() {
        return accounts;
    }

    public boolean isUse_f2a() {
        return use_f2a;
    }

    public int getNotify_count() {
        return notify_count;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Balance{" + "msg_count=" + msg_count + ", use_f2a=" + use_f2a + ", notify_count=" + notify_count + '}';
    }

}
