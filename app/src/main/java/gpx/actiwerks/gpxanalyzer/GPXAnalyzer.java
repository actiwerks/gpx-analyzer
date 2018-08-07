package gpx.actiwerks.gpxanalyzer;


import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.codebutchery.androidgpx.data.GPXDocument;
import com.codebutchery.androidgpx.data.GPXRoute;
import com.codebutchery.androidgpx.data.GPXRoutePoint;
import com.codebutchery.androidgpx.data.GPXSegment;
import com.codebutchery.androidgpx.data.GPXTrack;
import com.codebutchery.androidgpx.data.GPXTrackPoint;
import com.codebutchery.androidgpx.data.GPXWayPoint;
import com.codebutchery.androidgpx.xml.GPXListeners;
import com.codebutchery.androidgpx.xml.GPXParser;

import org.osmdroid.util.GeoPoint;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import actiwerks.networking.InputStreamRequest;

import static gpx.actiwerks.gpxanalyzer.Configuration.BEARING_TURN_TRESHOLD;


public class GPXAnalyzer implements Response.Listener<byte[]>, Response.ErrorListener,
        GPXListeners.GPXParserListener, GPXListeners.GPXParserProgressListener {

    private static final String TAG = GPXAnalyzer.class.getSimpleName();

    //private GPX gpx;
    private RequestQueue queue;
    private GPXDocument document;
    private GPXAnalyzedListener listener;

    private String info = "";
    private double totalDistance;
    private List<GeoPoint> geoPoints = new ArrayList<>();

    private GPXTrackPoint lastPoint = null;
    private GPXTrackPoint firstPoint = null;

    private double minLat = -1;
    private double minLong = -1;
    private double maxLat = -1;
    private double maxLong = -1;

    private GPXTrackPoint lastButOnePoint = null; // For turn calculation
    private int leftTurns;
    private int rightTurns;

    private GPXTrackPoint stopStartCandidate = null; // For stop detection
    private GPXTrackPoint stopEndCandidate = null;
    private int stopCount;

    private double altitudeUp;
    private double altitudeDown;


    public GPXAnalyzer(Context context) {
        queue = Volley.newRequestQueue(context);
    }

    public void read(String path, GPXAnalyzedListener listener) {
        if (queue != null) {
            this.listener = listener;
            InputStreamRequest request = new InputStreamRequest(Request.Method.GET, path, this,
                    this);
            queue.add(request);
        } else {
            Log.i(TAG, "Queue closed");
        }
    }

    public void stop() {
        if (queue != null) {
            queue.stop();
            queue = null;
        }
        listener = null;
    }

    private void analyze() {
        if (document == null) {
            info = "Parsed Document not available";
            return;
        }
        List<GPXTrack> tracks = document.getTracks();
        for (GPXTrack track : tracks) {
            info += "Track : " + track.getSegments().size() + " segment(s)\n";
            for (GPXSegment segment : track.getSegments()) {
                info += "Segment : " + segment.getTrackPoints().size() + " point(s)\n";
                for (GPXTrackPoint trackpoint : segment.getTrackPoints()) {
                    double distance = checkDistance(trackpoint);
                    checkStops(trackpoint, distance);
                    checkTurn(trackpoint);
                    checkAltitude(trackpoint);
                    checkBounds(trackpoint);
                    lastButOnePoint = lastPoint;
                    lastPoint = trackpoint;
                    GeoPoint geoPoint = new GeoPoint(trackpoint.getLatitude(), trackpoint.getLongitude());
                    geoPoints.add(geoPoint);
                }
            }
        }
        info += "Total distance: " + GeoTools.round(totalDistance, 3)  + " km\n";
        if (firstPoint != null && lastPoint != null && firstPoint.getTimeStamp() != null &&lastPoint.getTimeStamp() != null) {
            long diffInMillies = Math.abs(lastPoint.getTimeStamp().getTime() - firstPoint.getTimeStamp().getTime());
            String duration = String.format("%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes(diffInMillies),
                    TimeUnit.MILLISECONDS.toSeconds(diffInMillies) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(diffInMillies))
            );
            info += "Duration: " + duration+"\n";
            BigDecimal diffInHours = new BigDecimal(diffInMillies).divide(new BigDecimal(1000 * 3600), 20, RoundingMode.HALF_UP);
            Log.i(TAG, "Diff in hours : " + diffInHours + " from millis : " + diffInMillies);
            if (diffInHours.floatValue() > 0f) {
                BigDecimal bigTotalDistance = new BigDecimal(totalDistance);
                BigDecimal averageSpeed = bigTotalDistance.divide(diffInHours, 20, RoundingMode.HALF_UP);
                info += "Average Speed: " + GeoTools.round(averageSpeed.doubleValue(), 1) + " km/h\n";
            }
        }
        info += "Left turns: " + getLeftTurns() + "\n";
        info += "Right turns: " + getRightTurns() + "\n";
        if (getStopCount() > 0) {
            info += "Stops: " + getStopCount() + "\n";
        }
        if (getAltitudeUp() > 0) {
            info += "Altitude climbed: " + GeoTools.round(getAltitudeUp(), 1) + " m\n";
        }
        if (getAltitudeDown() > 0) {
            info += "Altitude descended: " + GeoTools.round(getAltitudeDown(), 1) + " m\n";
        }
    }

    private double checkDistance(GPXTrackPoint current) {
        if (lastPoint != null) {
            double distance = GeoTools.distance(lastPoint.getLatitude().doubleValue(), lastPoint.getLongitude().doubleValue(),
                    current.getLatitude().doubleValue(), current.getLongitude().doubleValue());
            totalDistance += distance;
            return distance;
        } else {
            firstPoint = current;
            return 0.0;
        }
    }

    private void checkStops(GPXTrackPoint current, double distance) {
        if (distance == 0.0) {
            if (stopStartCandidate == null) {
                stopStartCandidate = lastPoint;
            } else {
                stopEndCandidate = current;
            }
        } else {
            if (stopStartCandidate != null && stopEndCandidate != null) {
                long diffInMillies = Math.abs(stopStartCandidate.getTimeStamp().getTime() - stopEndCandidate.getTimeStamp().getTime());
                if (diffInMillies > Configuration.TIME_TO_PAUSE) {
                    Log.i(TAG, "Recorded stop for time :  " + diffInMillies);
                    stopCount++;
                }
            }
            stopStartCandidate = null;
            stopEndCandidate = null;
        }
    }

    private void checkBounds(GPXTrackPoint trackpoint) {
        if (minLat == -1 || minLat > trackpoint.getLatitude()) {
            minLat = trackpoint.getLatitude();
        }
        if (minLong == -1 || minLong > trackpoint.getLongitude()) {
            minLong = trackpoint.getLongitude();
        }
        if (maxLat == -1 || maxLat < trackpoint.getLatitude()) {
            maxLat = trackpoint.getLatitude();
        }
        if (maxLong == -1 || maxLong < trackpoint.getLongitude()) {
            maxLong = trackpoint.getLongitude();
        }
    }

    private void checkTurn(GPXTrackPoint current) {
        if (lastButOnePoint == null || lastPoint == null || current == null) {
            return;
        }
        double bearing1 = GeoTools.bearing(lastButOnePoint.getLatitude(), lastButOnePoint.getLongitude(), lastPoint.getLatitude(), lastPoint.getLongitude());
        double bearing2 = GeoTools.bearing(lastPoint.getLatitude(), lastPoint.getLongitude(), current.getLatitude(), current.getLongitude());
        //Log.i(TAG, "Step turn : " + bearing1 + " to " + bearing2 + " diff: " + (bearing2 - bearing1));
        if (bearing2 - bearing1 > BEARING_TURN_TRESHOLD) {
            leftTurns++;
        }
        if (bearing2 - bearing1 < -BEARING_TURN_TRESHOLD) {
            rightTurns++;
        }
    }

    private void checkAltitude(GPXTrackPoint current) {
        if (lastPoint != null) {
            if(lastPoint.getElevation() > current.getElevation()) {
                altitudeDown += lastPoint.getElevation() - current.getElevation();
            } else {
                altitudeUp += current.getElevation() - lastPoint.getElevation();
            }
        }
    }

    public double getNorth() {
        return maxLat;
    }

    public double getEast() {
        return maxLong;
    }

    public double getSouth() {
        return minLat;
    }

    public double getWest() {
        return minLong;
    }

    public int getLeftTurns() {
        return leftTurns;
    }

    public int getRightTurns() {
        return rightTurns;
    }

    public int getStopCount() {
        return stopCount;
    }

    public double getAltitudeUp() {
        return altitudeUp;
    }

    public double getAltitudeDown() {
        return altitudeDown;
    }

    public String getInfo() {
        return info;
    }

    public List<GeoPoint> getPoints() {
        return geoPoints;
    }

    // Response.Listener<byte[]> implementation

    @Override
    public void onResponse(byte[] response) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(response);
        try {
            GPXParser parser = new GPXParser(this, this);
            parser.parse(byteArrayInputStream);
        } catch (Throwable ex) {
            Log.e(TAG, "Error handling raw response:" + ex);
            if (listener != null) {
                listener.gpxAnalyzed(false);
            }
        }
    }

    // Response.ErrorListener implementation

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.i(TAG, "Got network error : " + error);
        if (listener != null) {
            listener.gpxAnalyzed(false);
        }
    }


    // GPXListeners.GPXParserListener implementation

    @Override
    public void onGpxParseStarted() {
    }

    @Override
    public void onGpxParseCompleted(GPXDocument document) {
        this.document = document;
        if (listener == null) {
            Log.i(TAG, "Listener not set, no reason to further process the document");
            return;
        }
        try {
            analyze();
            listener.gpxAnalyzed(true);
        } catch (Exception ex) {
            Log.e(TAG, "Exception while analyzing the GPX document: " + ex);
            listener.gpxAnalyzed(false);
        }
    }

    @Override
    public void onGpxParseError(String type, String message, int lineNumber, int columnNumber) {
        Log.i(TAG, "Parse Error : "+ type +" " + message + " line : " + lineNumber );
        if (listener != null) {
            listener.gpxAnalyzed(false);
        }
    }


    // GPXListeners.GPXParserProgressListener implementation

    @Override
    public void onGpxNewTrackParsed(int count, GPXTrack track) {
    }

    @Override
    public void onGpxNewRouteParsed(int count, GPXRoute track) {

    }

    @Override
    public void onGpxNewSegmentParsed(int count, GPXSegment segment) {

    }

    @Override
    public void onGpxNewTrackPointParsed(int count, GPXTrackPoint trackPoint) {

    }

    @Override
    public void onGpxNewRoutePointParsed(int count, GPXRoutePoint routePoint) {

    }

    @Override
    public void onGpxNewWayPointParsed(int count, GPXWayPoint wayPoint) {

    }

    public interface GPXAnalyzedListener {
        void gpxAnalyzed(boolean success);
    }
}
