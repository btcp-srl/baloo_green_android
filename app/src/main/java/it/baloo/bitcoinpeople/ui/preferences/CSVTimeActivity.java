package it.baloo.bitcoinpeople.ui.preferences;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.greenaddress.greenapi.data.SettingsData;
import it.baloo.bitcoinpeople.ui.GaActivity;
import it.baloo.bitcoinpeople.ui.R;
import it.baloo.bitcoinpeople.ui.UI;
import it.baloo.bitcoinpeople.ui.components.RadioBoxAdapter;
import it.baloo.bitcoinpeople.ui.twofactor.PopupCodeResolver;
import it.baloo.bitcoinpeople.ui.twofactor.PopupMethodResolver;

import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.greenaddress.greenapi.Session.getSession;

public class CSVTimeActivity extends GaActivity {

    private PopupMethodResolver popupMethodResolver;
    private PopupCodeResolver popupCodeResolver;
    private Disposable disposable;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_csv_time);
        setTitle(getString(R.string.id_set_2fa_expiry));
        setTitleBackTransparent();

        final Integer csv = getSession().getSettings().getCsvtime();
        final int index = getNetwork().getCsvBuckets().indexOf(csv);
        final String titles[] = getResources().getStringArray(R.array.csv_titles);
        final String subtitles[] = getResources().getStringArray(R.array.csv_subtitles);
        final RadioBoxAdapter adapter = new RadioBoxAdapter(titles, subtitles, index);

        recyclerView = UI.find(this, R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        final Button button = UI.find(this, R.id.button);
        button.setOnClickListener(this::setCsvTime);
    }

    private void setCsvTime(final View view) {
        final int selected = ((RadioBoxAdapter) recyclerView.getAdapter()).getSelected();
        final int csvTime = getNetwork().getCsvBuckets().get(selected);
        startLoading();
        popupMethodResolver = new PopupMethodResolver(CSVTimeActivity.this);
        popupCodeResolver = new PopupCodeResolver(CSVTimeActivity.this);
        disposable = Observable.just(getSession())
                .observeOn(Schedulers.computation())
                .map((session) -> session.setCsvTime(csvTime))
                .map((res) -> res.resolve(popupMethodResolver, popupCodeResolver))
                .map((res) -> getSession().refreshSettings())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(x -> {
                    stopLoading();
                    finishOnUiThread();
                }, (e) -> {
                    stopLoading();
                    e.printStackTrace();
                    UI.toast(CSVTimeActivity.this, e.getMessage(), Toast.LENGTH_LONG);
                    finishOnUiThread();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null)
            disposable.dispose();
        if (popupMethodResolver != null)
            popupMethodResolver.dismiss();
        if (popupCodeResolver != null)
            popupCodeResolver.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
