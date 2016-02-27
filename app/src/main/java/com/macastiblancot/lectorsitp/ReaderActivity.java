package com.macastiblancot.lectorsitp;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.macastiblancot.lectorsitp.manager.AnalyticsManager;
import com.macastiblancot.lectorsitp.sinpolib.Iso7816;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ReaderActivity extends AppCompatActivity {

    private NumberFormat nf = NumberFormat.getInstance(Locale.US);
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;

    InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("See logcat").build();
        mAdView.loadAd(adRequest);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("adunit");

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
            }
        });

        requestNewInterstitial();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("See logcat")
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();

        AnalyticsManager.INSTANCE.trackScreen(AnalyticsManager.Screen.READ_TAG);

        mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction())) {
            readTag(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent){
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            readTag(intent);
        }
    }

    private void readTag(Intent intent){

        AnalyticsManager.INSTANCE.trackAction(AnalyticsManager.Action.READ_TAG);

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        new ReaderTask().execute(tag);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    private class ReaderTask extends AsyncTask<Tag, Void, Integer> {

        @Override
        protected Integer doInBackground(Tag... params) {
            Tag tag = params[0];
            int balance = -1;
            IsoDep isoDep = IsoDep.get(tag);

            try {
                if (isoDep != null) {
                    Iso7816.StdTag stdTag = new Iso7816.StdTag(isoDep);
                    isoDep.connect();

                    if (stdTag.selectSITP().isOkay()){
                        balance = stdTag.getBalance();
                        AnalyticsManager.INSTANCE.trackAction(AnalyticsManager.Action.SUCCESSFUL_READ_TAG);
                    }

                    isoDep.close();
                }
            } catch (IOException e){
                AnalyticsManager.INSTANCE.trackAction(AnalyticsManager.Action.FAILED_READ_TAG);

                try {
                    isoDep.close();
                } catch (IOException e1){
                }

            }

            return balance;
        }

        @Override
        protected void onPostExecute(final Integer result) {
            if (result != null && result > -1) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView)findViewById(R.id.balance)).setText(nf.format(result));
                            }
                        });
                    }
                }, 2000);

            } else {
                Snackbar.make(findViewById(R.id.reader_layout),
                        getString(R.string.unable_to_read),
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
