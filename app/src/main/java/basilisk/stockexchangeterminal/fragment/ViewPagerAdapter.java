package basilisk.stockexchangeterminal.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import basilisk.stockexchangeterminal.R;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {
    CharSequence Titles[]; // This will Store the Titles of the Tabs which are Going to be passed when ViewPagerAdapter is created
    int NumbOfTabs; // Store the number of tabs, this will also be passed when the ViewPagerAdapter is created

    private String currencyPair;
    private int iconResource;

    public ViewPagerAdapter(FragmentManager fm, CharSequence mTitles[], int mNumbOfTabs) {
        super(fm);

        this.Titles = mTitles;
        this.NumbOfTabs = mNumbOfTabs;
        this.currencyPair = "";
        iconResource = R.drawable.unknown;
    }

    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        bundle.putString("currencyPair", getCurrencyPair());
        bundle.putInt("iconResource", getIconResource());

        switch (position) {
            case 0:
                ChartFragment cf = new ChartFragment();
                cf.setArguments(bundle);
                return cf;

            case 1:
                OfferBuyFragment obf = new OfferBuyFragment();
                obf.setArguments(bundle);
                return obf;

            case 2:
                OfferSellFragment osf = new OfferSellFragment();
                osf.setArguments(bundle);
                return osf;

            case 3:
                DealFragment df = new DealFragment();
                df.setArguments(bundle);
                return df;

            case 4:
                OrderFragment of = new OrderFragment();
                of.setArguments(bundle);
                return of;

            case 5:
                DealOwnerFragment dof = new DealOwnerFragment();
                dof.setArguments(bundle);
                return dof;

            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return NumbOfTabs;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return Titles[position];
    }

    public String getCurrencyPair() {
        return currencyPair;
    }

    public void setCurrencyPair(String currencyPair) {
        this.currencyPair = currencyPair;
    }

    public int getIconResource() {
        return iconResource;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }
}
