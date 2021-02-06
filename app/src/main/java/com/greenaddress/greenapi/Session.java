package com.greenaddress.greenapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.data.TransactionData;
import it.baloo.bitcoinpeople.ui.GaActivity;
import it.baloo.bitcoinpeople.wallets.HardwareCodeResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Session extends GDKSession {
    private static final ObjectMapper mObjectMapper = new ObjectMapper();
    private static Session instance = new Session();

    private String mWatchOnlyUsername;
    private boolean pinJustSaved = false;
    private HWWallet mHWWallet = null;
    private SettingsData mSettings;
    private Boolean mTwoFAReset = false;
    private String mNetwork;

    private Session() {
        super();
    }

    public static Session getSession() {
        return instance;
    }

    public boolean isWatchOnly() {
        return null != mWatchOnlyUsername;
    }

    public boolean isPinJustSaved() {
        return pinJustSaved;
    }

    public void setPinJustSaved(final boolean pinJustSaved) {
        this.pinJustSaved = pinJustSaved;
    }

    public HWWallet getHWWallet() {
        return mHWWallet;
    }

    public void setHWWallet(final HWWallet hwWallet) {
        mHWWallet = hwWallet;
    }

    public void setTwoFAReset(final boolean m2FAReset) {
        this.mTwoFAReset = m2FAReset;
    }

    public boolean isTwoFAReset() {
        return mTwoFAReset;
    }

    @Override
    public void connect(final String network, final boolean isDebug) throws Exception {
        mNetwork = network;
        super.connect(network, isDebug);
    }

    @Override
    public void connectWithProxy(final String network, final String proxyAsString, final boolean useTor, final boolean isDebug) throws Exception {
        mNetwork = network;
        super.connectWithProxy(network, proxyAsString, useTor, isDebug);
    }

    public void disconnect() throws Exception {
        super.disconnect();

        if (mHWWallet != null) {
            mHWWallet.disconnect();
            mHWWallet = null;
        }
        mWatchOnlyUsername = null;
        mSettings = null;
        mTwoFAReset = false;
        pinJustSaved = false;
    }

    public void loginWatchOnly(final String username, final String password) throws Exception {
        mWatchOnlyUsername = username;
        super.loginWatchOnly(username, password);
    }

    public SettingsData refreshSettings() {
        try {
            final ObjectNode settings = getGDKSettings();
            final SettingsData settingsData = mObjectMapper.convertValue(settings, SettingsData.class);
            mSettings = settingsData;
            return settingsData;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public SettingsData getSettings() {
        if (mSettings != null)
            return mSettings;
        return refreshSettings();
    }

    public SubaccountData getSubAccount(final GaActivity activity, final long subaccount) throws Exception {
        final GDKTwoFactorCall call = getSubAccount(subaccount);
        final ObjectNode account = call.resolve(null, new HardwareCodeResolver(activity, getHWWallet()));
        final SubaccountData subAccount = mObjectMapper.readValue(account.toString(), SubaccountData.class);
        return subAccount;
    }

    public Map<String, Long> getBalance(final GaActivity activity, final Integer subaccount) throws Exception {
        final GDKTwoFactorCall call = getBalance(subaccount, 0);
        final ObjectNode balanceData = call.resolve(null, new HardwareCodeResolver(activity, getHWWallet()));
        final Map<String, Long> map = new HashMap<>();
        final Iterator<String> iterator = balanceData.fieldNames();
        while (iterator.hasNext()) {
            final String key = iterator.next();
            map.put(key, balanceData.get(key).asLong(0));
        }
        return map;
    }

    public List<TransactionData> getTransactions(final GaActivity activity, final int subaccount, final int first, final int size) throws Exception {
        final GDKTwoFactorCall call =
                getTransactionsRaw(subaccount, first, size);
        final ObjectNode txListObject = call.resolve(null, new HardwareCodeResolver(activity, getHWWallet()));
        final List<TransactionData> transactions =
                parseTransactions((ArrayNode) txListObject.get("transactions"));
        return transactions;
    }

    public List<SubaccountData> getSubAccounts(final GaActivity activity) throws Exception {
        final GDKTwoFactorCall call = getSubAccounts();
        final ObjectNode accounts = call.resolve(null, new HardwareCodeResolver(activity, getHWWallet()));
        final List<SubaccountData> subAccounts =
                mObjectMapper.readValue(mObjectMapper.treeAsTokens(accounts.get("subaccounts")),
                        new TypeReference<List<SubaccountData>>() {});
        return subAccounts;
    }

    public List<Long> getFees() {
        if (!getNotificationModel().getFees().isEmpty())
            return getNotificationModel().getFees();
        try {
            return getFeeEstimates();
        } catch (final Exception e) {
            return new ArrayList<Long>(0);
        }
    }

    public NetworkData getNetworkData() {
        final List<NetworkData> networks = getNetworks();
        for (final NetworkData n : networks) {
            if (n.getNetwork().equals(mNetwork)) {
                return n;
            }
        }
        return null;
    }

    public void setSettings(final SettingsData settings) {
        mSettings = settings;
    }
}
