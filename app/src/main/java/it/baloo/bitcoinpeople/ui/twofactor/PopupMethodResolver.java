package it.baloo.bitcoinpeople.ui.twofactor;

import android.app.Activity;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.gdk.MethodResolver;
import it.baloo.bitcoinpeople.ui.R;
import it.baloo.bitcoinpeople.ui.UI;

import java.util.List;

public class PopupMethodResolver implements MethodResolver {
    private Activity activity;
    private MaterialDialog dialog;

    public PopupMethodResolver(final Activity activity) {
        this.activity = activity;
    }

    @Override
    public SettableFuture<String> method(List<String> methods) {
        final SettableFuture<String> future = SettableFuture.create();
        if (methods.size() == 1) {
            future.set(methods.get(0));
        } else {
            final MaterialDialog.Builder builder = UI.popup(activity, R.string.id_choose_method_to_authorize_the)
                                                   .cancelable(false)
                                                   .items(methods)
                                                   .itemsCallbackSingleChoice(0, (dialog, v, which, text) -> {
                Log.d("RSV", "PopupMethodResolver CHOOSE callback");
                future.set(methods.get(which));
                return true;
            })
                                                   .onNegative((dialog, which) -> {
                Log.d("RSV", "PopupMethodResolver CANCEL callback");
                future.set(null);
            });
            activity.runOnUiThread(() -> {
                Log.d("RSV", "PopupMethodResolver dialog show");
                dialog = builder.show();
            });
        }
        return future;
    }

    @Override
    public void dismiss() {
        if (dialog != null) {
            activity.runOnUiThread(() -> {
                dialog.dismiss();
            });
        }
    }
}
