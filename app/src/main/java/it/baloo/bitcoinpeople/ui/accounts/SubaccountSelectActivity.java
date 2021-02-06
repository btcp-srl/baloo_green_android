package it.baloo.bitcoinpeople.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.Conversion;
import it.baloo.bitcoinpeople.AuthenticationHandler;
import it.baloo.bitcoinpeople.ui.LoggedActivity;
import it.baloo.bitcoinpeople.ui.R;
import it.baloo.bitcoinpeople.ui.ThemeUtils;
import it.baloo.bitcoinpeople.ui.UI;
import it.baloo.bitcoinpeople.ui.components.BottomOffsetDecoration;
import it.baloo.bitcoinpeople.ui.preferences.PrefKeys;

import java.util.ArrayList;
import java.util.List;

import static com.greenaddress.greenapi.Session.getSession;

public class SubaccountSelectActivity extends LoggedActivity implements AccountAdapter.OnAccountSelected {

    private TextView mTotalAmountBtc;
    private TextView mTotalAmountFiat;
    private RecyclerView mRecyclerView;
    private Disposable mRefreshDisposable;
    private final List<SubaccountData> mSubaccountList = new ArrayList<>();
    private static final int REQUEST_CREATE_SUBACCOUNT = 101;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subaccount_select);

        mTotalAmountBtc = findViewById(R.id.total_amount_btc);
        mTotalAmountFiat = findViewById(R.id.total_amount_fiat);

        mRecyclerView = UI.find(this, R.id.accountsList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final float offsetPx = getResources().getDimension(R.dimen.adapter_bar);
        final BottomOffsetDecoration bottomOffsetDecoration = new BottomOffsetDecoration((int) offsetPx);
        final boolean isWatchonly = getSession().isWatchOnly();
        final AccountAdapter accountsAdapter = new AccountAdapter(mSubaccountList, this, !isWatchonly);
        mRecyclerView.addItemDecoration(bottomOffsetDecoration);
        mRecyclerView.setAdapter(accountsAdapter);
        accountsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRefreshDisposable != null)
            mRefreshDisposable.dispose();
    }

    private void refresh() {
        startLoading();
        mRefreshDisposable = Observable.just(getSession())
                             .subscribeOn(Schedulers.computation())
                             .observeOn(AndroidSchedulers.mainThread())
                             .map((session) -> {
            return getSession().getSubAccounts(this);
        }).subscribe((subaccounts) -> {
            stopLoading();
            mSubaccountList.clear();
            mSubaccountList.addAll(subaccounts);
            mRecyclerView.getAdapter().notifyDataSetChanged();
            updateBalance(mSubaccountList);
        }, (final Throwable e) -> {
            stopLoading();
        });
    }

    private void updateBalance(final List<SubaccountData> subaccounts) {
        long totalSatoshi = 0L;
        for (int i = 0; i < subaccounts.size(); i++) {
            final long satoshi = subaccounts.get(i).getSatoshi().get("btc");
            totalSatoshi += satoshi;
        }
        final BalanceData balanceReq = new BalanceData();
        balanceReq.setSatoshi(totalSatoshi);
        try {
            final BalanceData total = getSession().convertBalance(balanceReq);
            final String btcString = Conversion.getBtc(total, true);
            final String fiatString = Conversion.getFiat(total, true);
            mTotalAmountBtc.setTextColor(ThemeUtils.resolveColorAccent(this));
            mTotalAmountBtc.setText(btcString);
            mTotalAmountFiat.setText(" ≈ " + fiatString);
        } catch (final Exception e) {
            e.printStackTrace();
            UI.toast(this, R.string.id_you_are_not_connected_please, Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onAccountSelected(final int subaccount) {
        setActiveAccount(subaccount);
        if (AuthenticationHandler.hasPin(this)) {
            cfg().edit().putInt(PrefKeys.ACTIVE_SUBACCOUNT, subaccount).apply();
        }
        setResult(RESULT_OK);
        finishOnUiThread();
        overridePendingTransition(0,0);
    }

    @Override
    public void onNewSubaccount() {
        startActivityForResult(new Intent(this, SubaccountAddActivity.class), REQUEST_CREATE_SUBACCOUNT);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_SUBACCOUNT) {
            refresh();
        }
    }
}
