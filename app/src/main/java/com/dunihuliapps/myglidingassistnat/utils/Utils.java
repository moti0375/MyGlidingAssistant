package com.dunihuliapps.myglidingassistnat.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.location.Address;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

public class Utils {
	
	private static final String LOG_TAG = Utils.class.getSimpleName();
	private static final int GPS_ERROR_DIALOG_REQUEST = 9001;
	List<Address> list;
	
	public static boolean servicesOk(final Activity activity) {
		int isAvailable = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(activity.getApplicationContext());

		if (isAvailable == ConnectionResult.SUCCESS) {
//			Log.i(LOG_TAG, "Google Play Services are available");
//			Toast.makeText(context , "Google Play Services are available",
//					Toast.LENGTH_SHORT).show();
			return true;
		} else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable,
					activity, GPS_ERROR_DIALOG_REQUEST);
			dialog.show();
			dialog.setOnDismissListener(dialog1 -> {
                Toast.makeText(activity.getApplicationContext(), "Update Google Play Services and try again..",
                        Toast.LENGTH_SHORT).show();
                activity.finish();
            });
			
		} else {
			Toast.makeText(activity.getApplicationContext(), "Can't connect to Google Play Services",
					Toast.LENGTH_SHORT).show();
		}

		return false;
	}
	
	public static boolean checkExternalStorageState(){
		String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

	public static boolean isPackageInstalled(String packageName, Context context) {
		PackageManager pm = context.getPackageManager();
		try {
			Log.i(LOG_TAG, "About to get info for package: " + packageName);
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			return true;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void deleteCache(Activity activity) {
	      try {
	         File dir = activity.getApplicationContext().getCacheDir();
	         if (dir != null && dir.isDirectory()) {
	            deleteDir(dir);
	         }
	      } catch (Exception e) {
	         // TODO: handle exception
	      }
	   }

	   public static boolean deleteDir(File dir) {
	      if (dir != null && dir.isDirectory()) {
	         String[] children = dir.list();
	         for (int i = 0; i < children.length; i++) {
	            boolean success = deleteDir(new File(dir, children[i]));
	            if (!success) {
	               return false;
	            }
	         }
	      }

	      // The directory is now empty so delete it
	      return dir.delete();
	   }
	   
	   public static String getApplicationVersion(Activity activity){
		   PackageManager manager = activity.getApplicationContext().getPackageManager();
		   String Version = new String();
		   
			   	PackageInfo info;
				try {
					info = manager.getPackageInfo(activity.getApplicationContext().getPackageName(), 0);
			        Version = info.versionName;
				} catch (NameNotFoundException e) {
					e.printStackTrace();
					Version = null;
				}
		   return Version;
	   }
	   
//	   public static int mergeTrips(Trip tripA, Trip tripB, Activity activity, com.bartovapps.gpstriprec.domain.db.TripsDataSource datasource){
//		   int status = 1;
//		   ArrayList<LatLng> latLngList = new ArrayList<>();
//
//		   com.bartovapps.gpstriprec.domain.files.kml.KmlManager kmlManager = new com.bartovapps.gpstriprec.domain.files.kml.KmlManager(activity);
//		   kmlManager.openRawDocument();
//		   com.bartovapps.gpstriprec.core.trip_manager.com.bartovapps.gpstriprec.domain.trip_manager.KmlParser tripAParser = new com.bartovapps.gpstriprec.core.trip_manager.com.bartovapps.gpstriprec.domain.trip_manager.KmlParser(tripA.getKml());
//		   com.bartovapps.gpstriprec.core.trip_manager.com.bartovapps.gpstriprec.domain.trip_manager.KmlParser tripBParser = new com.bartovapps.gpstriprec.core.trip_manager.com.bartovapps.gpstriprec.domain.trip_manager.KmlParser(tripB.getKml());
//		   tripAParser.openTripKml();
//		   tripBParser.openTripKml();
//
//		   latLngList.addAll(tripAParser.getTripLocations());
//		   latLngList.addAll(tripBParser.getTripLocations());
//		   tripAParser.closeKml();
//		   tripBParser.closeKml();
//
//		   String mapFile = kmlManager.updateTripLatLng(latLngList);
//
//		   tripAParser.getLastLocation();
//		   long tripsDuration = tripA.getDuration() + tripB.getDuration();
//		   float tripsDistance = tripA.getDistance() + tripB.getDistance();
//		   double averageSpeed = tripsDistance  / (int) (tripsDuration / 1000); // m/sec
//		   SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy 'at' HH:mm");
//		   String date = sdf.format(new Date(System.currentTimeMillis()));
//
//		   double maxSpeed = Math.max(tripA.getMaxSpeed(), tripB.getMaxSpeed());
//
//		   //Todo uncomment this part when ready
////			Trip trip = new Trip(mapFile, date, tripsDistance, averageSpeed);
////			trip.setDuration(tripsDuration);
////			trip.setMaxSpeed(maxSpeed);
////			datasource.open();
////			datasource.create(trip);
////			datasource.close();
//
//
//		   return status;
//
//	   }


    public static boolean isFileExists(@Nullable  String fileName){
        if(fileName == null) return false;
        return new File(fileName).exists();
    }

    public static void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    public static Bitmap getRoundedShape(Bitmap scaleBitmapImage,int width) {
        // TODO Auto-generated method stub
        int targetWidth = width;
        int targetHeight = width;
        Bitmap targetBitmap = Bitmap.createBitmap(targetWidth,
                targetHeight,Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(targetBitmap);
        Path path = new Path();
        path.addCircle(((float) targetWidth - 1) / 2,
                ((float) targetHeight - 1) / 2,
                (Math.min(((float) targetWidth),
                        ((float) targetHeight)) / 2),
                Path.Direction.CCW);
        canvas.clipPath(path);
        Bitmap sourceBitmap = scaleBitmapImage;
        canvas.drawBitmap(sourceBitmap,
                new Rect(0, 0, sourceBitmap.getWidth(),
                        sourceBitmap.getHeight()),
                new Rect(0, 0, targetWidth,
                        targetHeight), null);
        return targetBitmap;
    }

    public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2, String text)
    {
        try
        {
            int maxWidth = (bmp1.getWidth() > bmp2.getWidth() ? bmp1.getWidth() : bmp2.getWidth());
            int maxHeight = (bmp1.getHeight() > bmp2.getHeight() ? bmp1.getHeight() : bmp2.getHeight());
            Bitmap bmOverlay = Bitmap.createBitmap(maxWidth, maxHeight,  bmp1.getConfig());
            Canvas canvas = new Canvas(bmOverlay);
            canvas.drawBitmap(bmp1, 0, 0, null);
            canvas.drawBitmap(bmp2, 0, 0, null);

            Paint paint = new Paint();
            paint.setColor(Color.BLACK); // Text Color
            Log.i(LOG_TAG, "Logo width: " + bmp2.getWidth()+ ", Logo height: " + bmp2.getHeight());
            Log.i(LOG_TAG, "Logo scaled width: " + bmp2.getScaledWidth(canvas) + ", scaled height: " + bmp2.getScaledHeight(canvas));
            paint.setStrokeWidth(bmp2.getWidth() * 4); // Text Size
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern
            // some more settings...

            canvas.drawBitmap(bmOverlay, 0, 0, paint);
            canvas.drawText(text, (float)(bmp2.getWidth()*0.7), (float)(bmp2.getWidth()*0.7), paint);


            return bmOverlay;

        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

	public static Bitmap RotateBitmap(Bitmap source, float angle)
	{
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

}
	  
