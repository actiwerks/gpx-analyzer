package gpx.actiwerks.gpxanalyzer;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.osmdroid.views.MapView;

public class AnalyzerActivity extends AppCompatActivity {

    private static final String TAG = AnalyzerActivity.class.getSimpleName();

    private static final int SELECT_ID = 1111;

    private MapWrapper map = null;

    private TextView infoText = null;

    private GPXAnalyzer analyzer;
    private boolean trackAnalyzeInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i (TAG, "External storage :" + Environment.getExternalStorageDirectory() + " state: " + Environment.getExternalStorageState());
        setContentView(R.layout.activity_analyzer);
        map = new MapWrapper(this, (MapView) findViewById(R.id.map));
        infoText = findViewById(R.id.info);
        infoText.setText("Use menu to pick a track");
        infoText.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public void onResume(){
        super.onResume();
        map.getMapView().onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause(){
        super.onPause();
        map.getMapView().onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onStop() {
        super.onStop();
        if (analyzer != null) {
            analyzer.stop();
        }
    }


    @Override
    public void onConfigurationChanged(android.content.res.Configuration configuration) {
        super.onConfigurationChanged(configuration);
        map.refreshMap(this);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_ID) {
            if(resultCode == Activity.RESULT_OK) {
                infoText.setText("Analyzing selected track...");
                String result=data.getStringExtra(SelectTrackActivity.SELECT_TRACK_URL);
                analyzer = new GPXAnalyzer(this);
                analyzer.read(result, (success) -> gpxAnalyzed(success));
                trackAnalyzeInProgress = true;
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
                infoText.setText("Track selection cancelled");
            }
        }
    }

    public void gpxAnalyzed(boolean success) {
        if (!trackAnalyzeInProgress) { // No need to handle the callback again (in case of multiple errors)
            return;
        }
        trackAnalyzeInProgress = false;
        if (!success) {
            infoText.setText("Failed to read or analyze the selected track");
            return;
        }
        infoText.setText(analyzer.getInfo());
        map.setTrack(analyzer, (info) -> trackClicked(info));
    }

    public void trackClicked(String info) {
        infoText.setText(info);
    }

    // Menu

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        int menuId = Menu.FIRST;
        menu.add(0, menuId , Menu.NONE, "Select");
        menu.add(0, menuId+1 , Menu.NONE, "Record");
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (trackAnalyzeInProgress) { // Ignore new track selection request until the current analysis is done
            return false;
        }
        switch (item.getItemId()) {
            case Menu.FIRST:
                selectTrack();
                return true;
            case Menu.FIRST+1:
                recordTrack();
                return true;
            default:
                return false;
        }
    }

    private void selectTrack() {
        Intent selectTrackIntent = new Intent(this, SelectTrackActivity.class);
        startActivityForResult(selectTrackIntent, SELECT_ID);
    }

    private void recordTrack() {
        infoText.setText("Recording not implemented yet");
    }


}
