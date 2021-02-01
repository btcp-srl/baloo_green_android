package it.baloo.bitcoinpeople;

import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import it.baloo.bitcoinpeople.ui.GaActivity;
import it.baloo.bitcoinpeople.ui.R;

public class LinksActivity extends GaActivity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_links);
        setTitleBackTransparent();
        setTitle("Informazioni");

        TextView t = findViewById(R.id.links_text);
        t.setText(Html.fromHtml("Acquista sul sito <a href=\"https://bitmoon.bitcoinpeople.it/\">bitcoinpeople.it</a><br>Contattaci a <a href=\"mailto:info@bitcoinpeople.it\">info@bitcoinpeople.it</a><br><br><br>Visita i nostri social"));
        t.setMovementMethod(LinkMovementMethod.getInstance());

        findViewById(R.id.linksFacebookImg).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://www.facebook.com/bitcoinpeople.it/"));
            startActivity(intent);
        });

        findViewById(R.id.linksInstagramImg).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://www.instagram.com/bitcoinpeople.it/"));
            startActivity(intent);
        });

        findViewById(R.id.linksTelegramImg).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://t.me/btcpsrl"));
            startActivity(intent);
        });
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            ImageView bg = findViewById(R.id.linksBackground);
            Matrix bgMatrix = new Matrix();

            Drawable d = bg.getDrawable();

            float scaleY = (float)bg.getHeight() / (float)d.getIntrinsicHeight();
            bgMatrix.setScale(scaleY, scaleY);

            float translateX = Math.max((d.getIntrinsicWidth() * scaleY - bg.getWidth()), 0) + 0.2f * bg.getWidth();
            bgMatrix.postTranslate(-translateX, 0);

            bg.setImageMatrix(bgMatrix);
        }
    }

}
