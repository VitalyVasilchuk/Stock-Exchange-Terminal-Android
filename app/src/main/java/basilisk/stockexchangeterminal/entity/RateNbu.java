package basilisk.stockexchangeterminal.entity;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RateNbu {

    @SerializedName("r030")
    @Expose
    private Integer codeDigital;

    @SerializedName("txt")
    @Expose
    private String name;

    @SerializedName("rate")
    @Expose
    private float rate;

    @SerializedName("cc")
    @Expose
    private String codeLetter;

    @SerializedName("exchangedate")
    @Expose
    private String exchangedate;

    public Integer getCodeDigital() {
        return codeDigital;
    }

    public void setCodeDigital(Integer codeDigital) {
        this.codeDigital = codeDigital;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getRate() {
        return rate;
    }

    public void setRate(float rate) {
        this.rate = rate;
    }

    public String getCodeLetter() {
        return codeLetter;
    }

    public void setCodeLetter(String codeLetter) {
        this.codeLetter = codeLetter;
    }

    public String getExchangedate() {
        return exchangedate;
    }

    public void setExchangedate(String exchangedate) {
        this.exchangedate = exchangedate;
    }

    @Override
    public String toString() {
        return "RateNbu{" +
                "name='" + name + '\'' +
                ", codeDigital=" + codeDigital +
                ", codeLetter='" + codeLetter + '\'' +
                ", exchangedate='" + exchangedate + '\'' +
                ", rate=" + rate +
                '}';
    }
}