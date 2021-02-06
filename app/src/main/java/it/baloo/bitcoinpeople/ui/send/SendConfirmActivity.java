package it.baloo.bitcoinpeople.ui.send;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.model.Conversion;
import it.baloo.bitcoinpeople.ui.GaActivity;
import it.baloo.bitcoinpeople.ui.LoggedActivity;
import it.baloo.bitcoinpeople.ui.R;
import it.baloo.bitcoinpeople.ui.UI;
import it.baloo.bitcoinpeople.ui.assets.AssetsAdapter;
import it.baloo.bitcoinpeople.ui.components.CharInputFilter;
import it.baloo.bitcoinpeople.ui.preferences.PrefKeys;
import it.baloo.bitcoinpeople.ui.twofactor.PopupCodeResolver;
import it.baloo.bitcoinpeople.ui.twofactor.PopupMethodResolver;
import it.baloo.bitcoinpeople.wallets.HardwareCodeResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.greenaddress.greenapi.Session.getSession;

public class SendConfirmActivity extends LoggedActivity implements SwipeButton.OnActiveListener {
    private static final String TAG = SendConfirmActivity.class.getSimpleName();
    private final ObjectMapper mObjectMapper = new ObjectMapper();

    private HWDeviceData mHwData;
    private ObjectNode mTxJson;
    private SwipeButton mSwipeButton;

    private Disposable setupDisposable;
    private Disposable sendDisposable;

    private PopupMethodResolver popupMethodResolver;
    private PopupCodeResolver popupCodeResolver;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_confirm);
        setTitleBackTransparent();
        mSwipeButton = UI.find(this, R.id.swipeButton);

        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final boolean isSweep = getIntent().getBooleanExtra(PrefKeys.SWEEP, false);
        final String hwwJson = getIntent().getStringExtra("hww");

        popupMethodResolver = new PopupMethodResolver(this);
        popupCodeResolver = new PopupCodeResolver(this);

        setTitle(isSweep ? R.string.id_sweep : R.string.id_send);

        startLoading();
        setupDisposable = Observable.just(getSession())
                          .observeOn(AndroidSchedulers.mainThread())
                          .map((session) -> {
            if (hwwJson != null)
                mHwData = mObjectMapper.readValue(hwwJson, HWDeviceData.class);
            return mObjectMapper.readValue(getIntent().getStringExtra(PrefKeys.INTENT_STRING_TX), ObjectNode.class);
        })
                          .observeOn(Schedulers.computation())
                          .map((tx) -> {
            // FIXME: If we didn't pass in the full transaction (with utxos)
            // then this call will go to the server. So, we should do it in
            // the background and display a wait icon until it returns
            return getSession().createTransactionRaw(this, tx)
            .resolve(null, new HardwareCodeResolver(this));
        })
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe((tx) -> {
            mTxJson = tx;
            stopLoading();
            setup();
        }, (e) -> {
            e.printStackTrace();
            stopLoading();
            UI.toast(this, e.getLocalizedMessage(), Toast.LENGTH_LONG);
            setResult(Activity.RESULT_CANCELED);
            finishOnUiThread();
        });
    }

    private void setup() {
        // Setup views fields
        final TextView noteTextTitle = UI.find(this, R.id.sendMemoTitle);
        final TextView noteText = UI.find(this, R.id.noteText);
        final TextView addressText = UI.find(this, R.id.addressText);

        // Use only 1st addressee
        final JsonNode addressee = mTxJson.withArray("addressees").get(0);
        final String tag = addressee.has("asset_tag") ? addressee.get("asset_tag").asText() : "btc";
        final String address = addressee.get("address").asText();
        final long amount = mTxJson.get("satoshi").get(tag).asLong(0);
        final boolean isSweeping = mTxJson.get("is_sweep").asBoolean();
        UI.hideIf(isSweeping, noteTextTitle);
        UI.hideIf(isSweeping, noteText);

        addressText.setText(address);
        noteText.setText(mTxJson.get("memo") == null ? "" : mTxJson.get("memo").asText());
        CharInputFilter.setIfNecessary(noteText);

        // Set currency & amount
        final long fee = mTxJson.get("fee").asLong();
        final TextView sendAmount = UI.find(this, R.id.sendAmount);
        final TextView sendFee = UI.find(this, R.id.sendFee);
        if (getSession().getNetworkData().getLiquid()) {
            sendAmount.setVisibility(View.GONE);
            UI.find(this, R.id.amountWordSending).setVisibility(View.GONE);
            final Map<String, Long> balances = new HashMap<>();
            balances.put(tag, amount);
            final RecyclerView assetsList = findViewById(R.id.assetsList);
            assetsList.setLayoutManager(new LinearLayoutManager(this));
            final AssetsAdapter adapter = new AssetsAdapter(balances, getNetwork(), null);
            assetsList.setAdapter(adapter);
            assetsList.setVisibility(View.VISIBLE);
        } else {
            sendAmount.setText(getFormatAmount(amount));
        }
        sendFee.setText(getFormatAmount(fee));

        if (mHwData != null && mTxJson.has("transaction_outputs")) {
            UI.show(UI.find(this, R.id.changeLayout));
            final TextView view = UI.find(this, R.id.changeAddressText);
            final Collection<String> changesList = new ArrayList<>();
            for (final JsonNode output : mTxJson.get("transaction_outputs")) {
                if (output.get("is_change").asBoolean() && !output.get("is_fee").asBoolean() && output.has("address")) {
                    changesList.add("- " + output.get("address").asText());
                }
            }
            view.setText(TextUtils.join("\n", changesList));
        }

        mSwipeButton.setOnActiveListener(this);
    }

    private String getFormatAmount(final long amount) {
        try {
            return String.format("%s / %s",
                                 Conversion.getBtc(amount, true),
                                 Conversion.getFiat(amount, true));
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (setupDisposable != null)
            setupDisposable.dispose();
        if (sendDisposable != null)
            sendDisposable.dispose();
        if (popupMethodResolver != null)
            popupMethodResolver.dismiss();
        if (popupCodeResolver != null)
            popupCodeResolver.dismiss();
    }

    @Override
    public void onActive() {
        startLoading();
        mSwipeButton.setEnabled(false);

        final GaActivity activity = this;
        final TextView noteText = UI.find(this, R.id.noteText);
        final String memo = noteText.getText().toString();
        mTxJson.put("memo", memo);

        sendDisposable = Observable.just(getSession())
                         .observeOn(Schedulers.computation())
                         .map((session) -> {
            return session.signTransactionRaw(mTxJson).resolve(null, new HardwareCodeResolver(activity));
        })
                         .map((tx) -> {
            final boolean isSweep = tx.get("is_sweep").asBoolean();
            if (isSweep) {
                getSession().broadcastTransactionRaw(tx.get("transaction").asText());
            } else {
                getSession().sendTransactionRaw(activity, tx).resolve(popupMethodResolver, popupCodeResolver);
            }
            return tx;
        })
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe((tx) -> {
            mTxJson = tx;
            UI.toast(activity, R.string.id_transaction_sent, Toast.LENGTH_LONG);
            stopLoading();
            activity.setResult(Activity.RESULT_OK);
            activity.finishOnUiThread();
        }, (e) -> {
            e.printStackTrace();
            stopLoading();
            final Resources res = getResources();
            final String msg = UI.i18n(res, e.getMessage());
            UI.toast(activity, msg, Toast.LENGTH_LONG);
            if (msg.equals(res.getString(R.string.id_transaction_already_confirmed))) {
                activity.setResult(Activity.RESULT_OK);
                activity.finishOnUiThread();
            } else {
                mSwipeButton.setEnabled(true);
                mSwipeButton.moveButtonBack();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
