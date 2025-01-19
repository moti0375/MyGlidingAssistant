//package com.bartovapps.gpstriprec.maphelper;
//
//import android.app.Activity;
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Matrix;
//import android.graphics.Paint;
//import android.graphics.Rect;
//import android.graphics.Typeface;
//import android.location.Location;
//import android.media.ExifInterface;
//import android.net.Uri;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.ImageView;
//
//import com.bartovapps.gpstriprec.R;
//import com.bartovapps.gpstriprec.core.db.TripsDBOpenHelper;
//import com.bartovapps.gpstriprec.core.db.TripsDataSource;
//import com.bartovapps.gpstriprec.core.di.QMainThread;
//import com.bartovapps.gpstriprec.utils.Utils;
//import com.google.android.gms.maps.CameraUpdate;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback;
//import com.google.android.gms.maps.model.BitmapDescriptorFactory;
//import com.google.android.gms.maps.model.CameraPosition;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.LatLngBounds;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.model.Polyline;
//import com.google.android.gms.maps.model.PolylineOptions;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//
//import javax.inject.Inject;
//
//import dagger.hilt.android.qualifiers.ApplicationContext;
//import data.model.Trip;
//
////import android.util.Log;
//
//public class com.bartovapps.gpstriprec.core.map_helper.MapHelper {
//    private static final double EARTHRADIUS = 6366198;
//    private static final float CAMERA_LONGSHOT_RAT = 2;
//    private static final float CAMERA_LONGSHOT_TILT = 65;
//    private static final String LOG_TAG = "com.bartovapps.gpstriprec.core.map_helper.MapHelper";
//    private static final int MAP_PADDING = 30;
//
//    // private static final String LOG_TAG = "MAP_HELPER";
//    private float lineWidth = 5;
//    private static final float NORTH = 360;
//    private GoogleMap mMap;
//    private float zoom = 5;
//    private Marker marker;
//    private Marker startMarker;
//    private Marker lastMarker;
//    private Polyline line;
//    private int lineColor = Color.RED;
//    private int mapType = GoogleMap.MAP_TYPE_NORMAL;
//    private float tilt = 0;
//    private LatLng latLng;
//    private float bearing;
//    private Handler handler;
//    private final Context context;
//
//    private final Map<String, ImageMarker> markersIdsMap;
//    private final TripsDataSource tripsDataSource;
//    @Inject
//    public com.bartovapps.gpstriprec.core.map_helper.MapHelper(@QMainThread  Handler handler,
//                     @ApplicationContext  Context context,
//                     TripsDataSource tripsDatasource) {
//        this.handler = handler;
//        this.tripsDataSource = tripsDatasource;
//        //this.mMap = map;
//
//        // this.zoom = mapZoom;
//        //setZoom(mapZoom);
//        //setLineColor(color);
//        //setMapType(mapType);
//       // this.handler = handler;
//        this.context = context;
//        markersIdsMap = new LinkedHashMap<>();
//        initMap();
//
//    }
//
//    private void initMap() {
//        clearEverything();
//        CameraUpdate update = CameraUpdateFactory.zoomBy(this.zoom);
//        mMap.moveCamera(update);
//        //mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(null));
//
//    }
//
//    public void setLocation(Location location) {
//        // this.zoom = zoom;
//        latLng = new LatLng(location.getLatitude(), location.getLongitude());
//        moveCamera(latLng, NORTH);
//    }
//
//    public void goToLocation(Location location) {
//        latLng = new LatLng(location.getLatitude(), location.getLongitude());
//        this.bearing = location.getBearing();
//        moveCamera(latLng, this.bearing);
//        addMarker(latLng);
//        drawLine(latLng);
//    }
//
//
//    private void drawLine(LatLng ll) {
//        if (lastMarker != null) {
//            PolylineOptions options = new PolylineOptions()
//                    .add(lastMarker.getPosition()).add(marker.getPosition())
//                    .width(lineWidth).color(lineColor);
//
//            line = mMap.addPolyline(options);
//        }
//    }
//
//    public void addMarker(LatLng ll) {
//        if (marker != null) {
//            lastMarker = marker;
//            marker.remove();
//            marker = null;
//            System.gc();
//        }
//        if (startMarker == null) {
//            MarkerOptions startOptions = new MarkerOptions()
//                    .position(ll)
//                    .draggable(false)
//                    .title("Start Point")
//                    .snippet(ll.latitude + "," + ll.longitude)
//                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//            startMarker = mMap.addMarker(startOptions);
//            System.gc();
//        }
//        MarkerOptions options = new MarkerOptions()
//                .position(ll)
//                .draggable(false)
//                .title("End Point")
//                .snippet(ll.latitude + "," + ll.longitude)
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
//        marker = mMap.addMarker(options);
//        System.gc();
//    }
//
//
//    public void addImageMarker(final ImageMarker imageMarker, final Context context) {
//        handler.post(() -> {
//            LatLng ll = new LatLng(imageMarker.getLatitude(), imageMarker.getLongitude());
//            Marker marker = mMap.addMarker(new MarkerOptions().position(ll)
//                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).snippet("" + ll.latitude + "," + ll.longitude).title(context.getString(R.string.app_name)));
//            markersIdsMap.put(marker.getId(), imageMarker);
//        });
//    }
//
//    public void clearMarkers() {
//
//        this.lastMarker = null;
//        this.marker = null;
//        this.line = null;
//        this.markersIdsMap.clear();
//
//    }
//
//    private void moveCamera(LatLng ll, float bearing) {
//
//        // CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll,
//        // this.zoom);
//
//        CameraPosition cameraPosition = new CameraPosition.Builder().target(ll)
//                .zoom(this.zoom).tilt(this.tilt) // Sets the zoom
//                .bearing(bearing) // Sets the tilt of the camera to 30 degrees
//                .build();
//
//        // CameraUpdate update = CameraUpdateFactory.newCameraPosition(new
//        // CameraPosition(ll, this.zoom, 0, bearing));
//        mMap.animateCamera(CameraUpdateFactory
//                .newCameraPosition(cameraPosition));
//
//    }
//
//    public void clearEverything() {
//        handler.post(() -> {
//            if (marker != null) {
//                marker.remove();
//            }
//            if (startMarker != null) {
//                startMarker.remove();
//                startMarker = null;
//            }
//            if (lastMarker != null) {
//                lastMarker.remove();
//                lastMarker = null;
//            }
//
//            if (line != null) {
//                line.remove();
//                line = null;
//            }
//            if (markersIdsMap != null) {
//                markersIdsMap.clear();
//            }
//            mMap.clear();
//        });
//
//    }
//
//    //This method is used when uploading a trip in the details Activity
//    public void overlayRoute(final List<LatLng> list) {
//
//        final PolylineOptions options = new PolylineOptions()
//                .width(lineWidth).color(com.bartovapps.gpstriprec.core.map_helper.MapHelper.this.lineColor);
//        final LatLngBounds.Builder builder = new LatLngBounds.Builder();
//        for (int i = 0; i < list.size() - 1; i++) {
////                    Log.i(LOG_TAG, "Add polyline");
//            options.add(list.get(i)).add(list.get(i + 1));
//            builder.include(list.get(i));
//        }
//
//        final LatLngBounds tmpBounds = builder.build();
//        final CameraUpdate update = CameraUpdateFactory.newLatLngBounds(
//                tmpBounds, MAP_PADDING);
//
//        Log.i(LOG_TAG, "About to overlay route..");
//        handler.post(() -> {
//            addMarker(list.get(0));
//            addMarker(list.get(list.size() - 1));
//            line = mMap.addPolyline(options);
//            mMap.moveCamera(update);
//        });
//    }
//
//    //This method is called when upload a trip to main screen, color is given as
//    public void overlayRoute(final List<LatLng> list, final float zoom, final int color) {
//        handler.post(() -> {
//            PolylineOptions options = new PolylineOptions()
//                    .width(lineWidth).color(color);
//            addMarker(list.get(0));
//            LatLngBounds.Builder builder = new LatLngBounds.Builder();
//
//            for (int i = 0; i < list.size() - 1; i++) {
//                options.add(list.get(i)).add(list.get(i + 1));
//                builder.include(list.get(i));
//
//                // Log.i(LOG_TAG, "Add polyline");
//            }
//            line = mMap.addPolyline(options);
//            addMarker(list.get(list.size() - 1));
//            LatLngBounds tmpBounds = builder.build();
//
//            CameraUpdate update = CameraUpdateFactory.newLatLngBounds(
//                    tmpBounds, 100);
//
//            mMap.moveCamera(update);
//        });
//
//    }
//
//
//    /**
//     * Create a new LatLng which lies toNorth meters north and toEast meters
//     * east of startLL
//     */
//    public static LatLng move(LatLng startLL, double toNorth, double toEast) {
//        double lonDiff = meterToLongitude(toEast, startLL.latitude);
//        double latDiff = meterToLatitude(toNorth);
//        return new LatLng(startLL.latitude + latDiff, startLL.longitude
//                + lonDiff);
//    }
//
//    private static double meterToLongitude(double meterToEast, double latitude) {
//        double latArc = Math.toRadians(latitude);
//        double radius = Math.cos(latArc) * EARTHRADIUS;
//        double rad = meterToEast / radius;
//        return Math.toDegrees(rad);
//    }
//
//    private static double meterToLatitude(double meterToNorth) {
//        double rad = meterToNorth / EARTHRADIUS;
//        return Math.toDegrees(rad);
//    }
//
//    public void setLineColor(int color) {
//        this.lineColor = color;
//    }
//
//    public void setZoom(float zoom) {
//        this.zoom = zoom;
//        CameraUpdate update = CameraUpdateFactory.zoomTo(zoom);
//        mMap.animateCamera(update);
//    }
//
//    public float getZoom() {
//        return this.zoom;
//    }
//
//    public void setCameraTilt(float tilt) {
//        this.tilt = tilt;
//    }
//
//    public void setMapType(int type) {
//        this.mapType = type;
//        mMap.setMapType(this.mapType);
//    }
//
//    public void setLineWidth(float width) {
//        lineWidth = width;
//    }
//
//    public void mapCameraLongshot() {
//        setCameraTilt(CAMERA_LONGSHOT_TILT);
//        setZoom(this.zoom - CAMERA_LONGSHOT_RAT);
//    }
//
//    public void mapCameraCloseup() {
//        setCameraTilt(0);
//        setZoom(this.zoom + CAMERA_LONGSHOT_RAT);
//    }
//
//    public void viewRoute(final List<LatLng> list) {
//        LatLngBounds.Builder builder = new LatLngBounds.Builder();
//
//        for (LatLng location : list) {
//            builder.include(location);
//        }
//        LatLngBounds tmpBounds = builder.build();
//        final CameraUpdate update = CameraUpdateFactory.newLatLngBounds(
//                tmpBounds, 100);
//
//       handler.post(() -> mMap.moveCamera(update));
//
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            Thread.interrupted();
//        }
//
//    }
//
//    public void saveMapAsImage(final Activity activity, final long tripId) {
//
//        Log.i("com.bartovapps.gpstriprec.core.map_helper.MapHelper", "saveMapAsImage: ");
//        long timestamp = System.currentTimeMillis();
//
//        String root = Environment.getExternalStorageDirectory().toString();
//        String projectDir = activity.getApplicationContext().getResources()
//                .getString(com.bartovapps.gpstriprec.R.string.projectRootDir);
//
//        if (Utils.checkExternalStorageState()) {
//            final File fileDir = new File(root + "/" + projectDir
//                    + "/mapImages");
//
//            if (!fileDir.exists()) {
//                fileDir.mkdirs();
//            }
//
//            final String fileName = fileDir + "/trip_" + timestamp + ".jpeg";
//
//            SnapshotReadyCallback callback = snapshot -> {
//                try {
//                    FileOutputStream out = new FileOutputStream(fileName);
//                    snapshot = Bitmap.createScaledBitmap(snapshot, 500,
//                            500, false);
//                    snapshot.compress(Bitmap.CompressFormat.JPEG, 50, out);
//                    snapshot.recycle();
//
//                    tripsDataSource.open();
//                    tripsDataSource.updateTripData(tripId,
//                            TripsDBOpenHelper.COLUMN_MAP_IMAGE, fileName);
//                    tripsDataSource.close();
//                    out.flush();
//                    out.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Log.e("com.bartovapps.gpstriprec.core.map_helper.MapHelper", "There was an exception: " +e.getMessage());
//                }
//            };
//
//            mMap.snapshot(callback);
//        } else {
//            Log.i("com.bartovapps.gpstriprec.core.map_helper.MapHelper", "checkExternalStorageState false");
//        }
//    }
//
//    public void saveMapAsImage(final String mapFileName) {
//
//        if (Utils.checkExternalStorageState() == true) {
//            // final File fileDir = new File(root +
//            // context.getResources().getString(com.bartovapps.gpstriprec.R.string.projectRootDir)
//            // + "/mapsImages");
//            final File fileName = new File(mapFileName);
//            final File fileDir = new File(fileName.getParentFile().toString());
//            if (!fileDir.exists()) {
//                fileDir.mkdirs();
//            }
//
//            // final String fileName = fileDir + "/" + mapFileName;
//
//            SnapshotReadyCallback callback = new SnapshotReadyCallback() {
//
//                @Override
//                public void onSnapshotReady(Bitmap snapshot) {
//                    FileOutputStream out = null;
//                    try {
//                         out = new FileOutputStream(fileName);
//                        snapshot = Bitmap.createScaledBitmap(snapshot, 500,
//                                500, false);
//                        snapshot.compress(Bitmap.CompressFormat.JPEG, 50, out);
//                        snapshot.recycle();
//                        snapshot = null;
//                        System.gc();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//
//                        try {
//                            if(out != null){
//                                out.flush();
//                                out.close();
//                            }
//                        }catch (Exception finallyException){
//                            finallyException.printStackTrace();
//                        }
//                    }
//                }
//
//            };
//
//            mMap.snapshot(callback);
//        }
//    }
//    // private class LongOperation extends AsyncTask<String, Void, String> {
//    //
//    // @Override
//    // protected String doInBackground(String... params) {
//    // for (int i = 0; i < 5; i++) {
//    // try {
//    // Thread.sleep(1000);
//    // } catch (InterruptedException e) {
//    // Thread.interrupted();
//    // }
//    // }
//    // return "Executed";
//    // }
//    //
//    // @Override
//    // protected void onPostExecute(String result) {
//    // // might want to change "executed" for the returned string passed
//    // // into onPostExecute() but that is upto you
//    // }
//    //
//    // @Override
//    // protected void onPreExecute() {}
//    //
//    // @Override
//    // protected void onProgressUpdate(Void... values) {}
//    // }
//
//    private Bitmap writeTextOnDrawable(int drawableId, String text, Context context) {
//
//        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), drawableId)
//                .copy(Bitmap.Config.ARGB_8888, true);
//
//        Typeface tf = Typeface.create("Helvetica", Typeface.BOLD);
//
//        Paint paint = new Paint();
//        paint.setStyle(Paint.Style.FILL);
//        paint.setColor(Color.WHITE);
//        paint.setTypeface(tf);
//        paint.setTextAlign(Paint.Align.CENTER);
//        paint.setTextSize(convertToPixels(context, 11));
//
//        Rect textRect = new Rect();
//        paint.getTextBounds(text, 0, text.length(), textRect);
//
//        Canvas canvas = new Canvas(bm);
//
//        //If the text is bigger than the canvas , reduce the font size
//        if (textRect.width() >= (canvas.getWidth() - 4))     //the padding on either sides is considered as 4, so as to appropriately fit in the text
//            paint.setTextSize(convertToPixels(context, 7));        //Scaling needs to be used for different dpi's
//
//        //Calculate the positions
//        int xPos = (canvas.getWidth() / 2) - 2;     //-2 is for regulating the x position offset
//
//        //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
//        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));
//
//        canvas.drawText(text, xPos, yPos, paint);
//
//        return bm;
//    }
//
//
//    public static int convertToPixels(Context context, int nDP) {
//        final float conversionScale = context.getResources().getDisplayMetrics().density;
//
//        return (int) ((nDP * conversionScale) + 0.5f);
//
//    }
//
//    public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
//
//        // These a both viewgroups containing an ImageView with id "badge" and two TextViews with id
//        // "title" and "snippet".
//        private final View mWindow;
//
//        CustomInfoWindowAdapter(LayoutInflater inflater) {
//            mWindow = inflater.inflate(R.layout.custom_info_window, null);
//        }
//
//        @Override
//        public View getInfoWindow(Marker marker) {
//            render(marker, mWindow);
//            return mWindow;
//        }
//
//        @Override
//        public View getInfoContents(Marker marker) {
//            return null;
//        }
//
//        private void render(Marker marker, View view) {
//
//            Log.i(LOG_TAG, "render function was called. marker id: " + marker.getId());
//            ImageView imageView = (ImageView)view.findViewById(R.id.markerImage);
//
//                if (markersIdsMap.get(marker.getId()) == null) {  //This means it's the a marker with no image.. (like trip start and end locations)
//                    imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_launcher));
//                } else {
//
//                    Uri imageUri = markersIdsMap.get(marker.getId()).getImageUri();
//                    String imagePath = imageUri.getPath();
//                    File imgFile = new File(imagePath);
//
//                    if(!imgFile.exists()) {  //This can happen if user erased the image from gallery
//                        imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.image_broken));
////                        Picasso.with(activity).load(R.drawable.ic_launcher).into(imageView);
//                    }else{
//                        int rotation = getImageRotation(imagePath);
////                        Picasso.with(activity).load(imageUri).rotate(rotation).into(imageView);
//                        Bitmap reducedSizeImage = getReducedImage(imageUri.getPath());
//                        imageView.setImageBitmap(rotateImage(reducedSizeImage, imageUri.getPath()));
////                        Picasso.with(activity).load(imageUri.getPath()).fit().centerInside().into(imageView);
//                    }
//
////                    imageView.setImageBitmap(reducedSizeImage);
//
////                    if(imgFile.exists()){
////                    }else{
////                    }
//                }
//
////            String title = marker.getTitle();
////            TextView titleUi = ((TextView) view.findViewById(R.id.title));
////            if (title != null) {
////                // Spannable string allows us to edit the formatting of the text.
////                SpannableString titleText = new SpannableString(title);
////                titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
////                titleUi.setText(titleText);
////            } else {
////                titleUi.setText("");
////            }
////
////            String snippet = marker.getSnippet();
////            TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
////            if (snippet != null && snippet.length() > 12) {
////                SpannableString snippetText = new SpannableString(snippet);
////                snippetText.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, snippet.length()/2, 0);
////                snippetText.setSpan(new ForegroundColorSpan(Color.BLUE), (snippet.length()/2)+1, snippet.length(), 0);
////                snippetUi.setText(snippetText);
////            } else {
////                snippetUi.setText("");
////            }
//        }
//    }
//
//
//    Bitmap getReducedImage(String imageFileLocation){
//        Log.i(LOG_TAG, "image file path: " + imageFileLocation);
//        int targetImageWidth = (int) context.getResources().getDimension(R.dimen.image_marker_width);
//        int targetImageHeight =(int) context.getResources().getDimension(R.dimen.image_marker_height);
//
//        Log.i(LOG_TAG, "image view sizes: " + targetImageWidth + ", " + targetImageHeight);
//
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(imageFileLocation, options);
//        int cameraImageWidth = options.outWidth;
//        int cameraImageHeight = options.outHeight;
//
//        int scaleFactor = Math.min(cameraImageWidth/targetImageWidth, cameraImageHeight/targetImageHeight);
//        options.inSampleSize = scaleFactor;
//        options.inJustDecodeBounds = false;
//
//        Bitmap reducedSizeImage = BitmapFactory.decodeFile(imageFileLocation, options);
//        return  reducedSizeImage;
//
//    }
//
//    private Bitmap rotateImage(Bitmap bitmap, String imageFileLocation){
//
//        Matrix matrix = new Matrix();
//
//        int rotation = getImageRotation(imageFileLocation);
//        matrix.setRotate(rotation);
//
//        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//        return rotatedBitmap;
//
//    }
//
//    private int getImageRotation(String imageFileLocation) {
//        ExifInterface exifInterface = null;
//
//        try {
//            exifInterface = new ExifInterface(imageFileLocation);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
//        Log.i(LOG_TAG, "image orientation: " + orientation);
//
//
//        switch (orientation) {
//            case ExifInterface.ORIENTATION_ROTATE_90:
//                return 90;
//            case ExifInterface.ORIENTATION_ROTATE_180:
//                return 180;
//            case ExifInterface.ORIENTATION_ROTATE_270:
//                return 270;
//            default:
//                return 0;
//
//        }
//    }
//
//    public ImageMarker getImageMarkerUri(String markerId){
//        if(markerId == null){
//            return null;
//        }
//        return markersIdsMap.get(markerId);
//
//    }
//
//}
