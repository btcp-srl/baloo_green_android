package it.baloo.bitcoinpeople.ui.accounts;

import com.greenaddress.greenapi.data.NetworkData;

public interface NetworkSwitchListener {
    void onNetworkClick(final NetworkData networkData);
}
