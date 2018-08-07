package gpx.actiwerks.gpxanalyzer;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay;

import java.util.List;

public class MapWrapper {

    private static final String TAG = MapWrapper.class.getSimpleName();

    private MapView mapView;

    private SimpleLocationOverlay locationOverlay;
    private ScaleBarOverlay scaleBarOverlay;
    private BoundingBox lastBoundingBox;

    public MapWrapper(Context context, MapView map) {
        this.mapView = map;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setTilesScaledToDpi(true);
        mapView.setMultiTouchControls(true);
        locationOverlay = new SimpleLocationOverlay(((BitmapDrawable)context.getResources().getDrawable(org.osmdroid.library.R.drawable.person)).getBitmap());
        mapView.getOverlays().add(locationOverlay);
        scaleBarOverlay = new ScaleBarOverlay(mapView);
        mapView.getOverlays().add(scaleBarOverlay);
        // Scale bar tries to draw as 1-inch, so to put it in the top center, set x offset to
        // half screen width, minus half an inch.
        scaleBarOverlay.setScaleBarOffset(
                (int) (context.getResources().getDisplayMetrics().widthPixels / 2 - context.getResources()
                        .getDisplayMetrics().xdpi / 2), 10);
        // Zoom to authors home address
        IMapController mapController = mapView.getController();
        mapController.setZoom(13.0);
        GeoPoint startPoint = new GeoPoint(50.218792, 15.842702);
        mapController.setCenter(startPoint);
        Log.i(TAG, "Initial Zoom " + mapView.getIntrinsicScreenRect(null).height());
    }

    public MapView getMapView() {
        return mapView;
    }

    public void refreshMap(Context context) {
        scaleBarOverlay.setScaleBarOffset(
                (int) (context.getResources().getDisplayMetrics().widthPixels / 2 - context.getResources()
                        .getDisplayMetrics().xdpi / 2), 10);
        mapView.invalidate();
        if (lastBoundingBox != null) {
            Log.i(TAG, "Zooming to last saved bounds " + mapView.getIntrinsicScreenRect(null).height());
            mapView.zoomToBoundingBox(lastBoundingBox, true);
        }
    }

    public void setTrack(GPXAnalyzer analyzer, TrackClickListener listener) {
        Polyline line = new Polyline();
        List<GeoPoint> points = analyzer.getPoints();
        line.setPoints(points);
        String info = analyzer.getInfo();
        mapView.getOverlayManager().add(line);
        line.setOnClickListener((polyline, mapView, eventPos) -> {
            listener.trackClicked(info);
            return true;
        });
        BoundingBox boundingBox = new BoundingBox(analyzer.getNorth(), analyzer.getEast(), analyzer.getSouth(), analyzer.getWest());
        lastBoundingBox = boundingBox.increaseByScale(1.2f);
        mapView.zoomToBoundingBox(lastBoundingBox, true);
    }


    public interface TrackClickListener {
        void trackClicked(String info);
    }

}
