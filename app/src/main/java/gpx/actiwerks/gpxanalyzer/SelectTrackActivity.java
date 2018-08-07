package gpx.actiwerks.gpxanalyzer;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class SelectTrackActivity extends AppCompatActivity {

    private static final String TAG = SelectTrackActivity.class.getSimpleName();

    public static final String SELECT_TRACK_URL = "select.track.url";

    private WebView webView;
    private TextView loadingLabel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_track);
        webView = findViewById(R.id.web);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new ClickWebViewClient());
        webView.loadUrl(Configuration.TRACKS_URL);
        loadingLabel = findViewById(R.id.loading);
    }

    private void trackSelected(String trackUrl) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(SELECT_TRACK_URL,trackUrl);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }


    private class ClickWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            //https://www.openstreetmap.org/trace/2762763/data
            //https://www.openstreetmap.org/user/Micocoulier/traces/2762763

            if (url.contains("/traces/")) {
                int index = url.indexOf("/traces/");
                String endPart = url.substring(index+8); // Length of traces part
                try {
                    int traceId = Integer.parseInt(endPart);
                    String downloadURl = Configuration.SINGLE_TRACK_BASE_URL+traceId+"/data";
                    //String downloadURl = url+"/data";
                    trackSelected(downloadURl);
                    return true;
                } catch (NumberFormatException ex) {
                    // Not a number, proceed with url load
                    view.loadUrl(url);
                    return false;
                }
            } else { // Ignore all other links
                return true;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            SelectTrackActivity.this.runOnUiThread(() -> loadingLabel.setVisibility(View.GONE));
        }

    }
}
