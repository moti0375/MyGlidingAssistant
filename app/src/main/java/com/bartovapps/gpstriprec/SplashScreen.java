//package com.bartovapps.gpstriprec;
//
//import android.content.Intent;
//import android.net.Uri;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.Looper;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.bartovapps.gpstriprec.presentation.screens.main_screen.MainScreen;
//import com.bartovapps.gpstriprec.utils.Utils;
//import com.squareup.picasso.Picasso;
//
//import java.io.BufferedInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//public class SplashScreen extends AppCompatActivity {
//
//    private static final String LOG_TAG = SplashScreen.class.getSimpleName();
//    private static final long SPLASH_TIMEOUT = 3500;
//
//    private String version;
//    private ImageView iv;
//    String root = Environment.getExternalStorageDirectory().toString();
//    String projectDir;
//    String attachmentFile;
//    Intent i;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.splash_screen);
//        i = new Intent(SplashScreen.this, MainScreen.class);
//        attachmentFile = getExternalFilesDir(null) + "/" + "attachment.kml";
//        version = Utils.getApplicationVersion(SplashScreen.this);
//        iv = findViewById(R.id.splashImage);
//
//
//        Picasso.with(iv.getContext())
//                .load(R.drawable.splash)
//                .fit()
//                .centerInside()
//                .into(iv);
//
//        new Handler(Looper.getMainLooper()).postDelayed(this::startMainActivity, SPLASH_TIMEOUT);
//    }
//
//
//
//    private void startMainActivity() {
//        Intent launcherIntent = getIntent();
//        String path = null;
//
//        if (getIntent().getScheme() == null) {
//            startActivity(i);
//            finish();
//            return;
//        }
//
//        if (getIntent().getScheme().equals("file")) {
//            path = launcherIntent.getData().getPath();
////            Log.i(LOG_TAG, "Started from a local file..");
////            Log.i(LOG_TAG, "type is: " + launcherIntent.getType());
////            Log.i(LOG_TAG, "Host is: " + getIntent().getData().getHost());
//            if (path != null) {
//                i.putExtra("kml_path", path);
//            }
//            startActivity(i);
//            finish();
//            return;
//        }
//
//        if (getIntent().getScheme().equals("content")) {
////            Log.i(LOG_TAG, "Started from a email attachment..");
////            Log.i(LOG_TAG, "Type is: " + getIntent().getType());
////            Log.i(LOG_TAG, "Host is: " + getIntent().getData().getHost());
//            Uri uri = launcherIntent.getData();
//            if (uri != null) {
//                try {
//                    InputStream attachment = getContentResolver().openInputStream(uri);
//                    if (attachment == null) {
////                        Log.i(LOG_TAG, "not from mail attachment.. getting path from data..");
//                        startActivity(i);
//                    } else {
////                        Log.i(LOG_TAG, "Received file from attachment");
//                        UploadAttachmentTask uploadAttachmentTask = new UploadAttachmentTask();
//                        Container container = new Container();
//                        container.fileName = attachmentFile;
//                        container.uri = uri;
//                        uploadAttachmentTask.execute(container);
//                        //path = getFileFromAttachment(uri);
//                    }
////                    Log.i(LOG_TAG, "Got path: " + path);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
////                    Log.i(LOG_TAG, "File not found exception..");
//                } catch (SecurityException e){
//                    Toast.makeText(SplashScreen.this, getString(R.string.error_access_attachment), Toast.LENGTH_SHORT).show();
//                    startActivity(i);
//                    finish();
//                }
//            }
//        }
//
//
//
//    }
//
//
//
//
//    private class UploadAttachmentTask extends AsyncTask<Container, Void, Container> {
//
//        @Override
//        protected Container doInBackground(Container... params) {
//            Container container = params[0];
//
//            try {
//                InputStream attachment = getContentResolver().openInputStream(container.uri);
//                FileOutputStream tmp = new FileOutputStream(container.fileName);
//
//                BufferedInputStream bis = new BufferedInputStream(attachment);
//                StringBuffer b = new StringBuffer();
//
//                while (bis.available() != 0) {
//                    char c = (char) bis.read();
//                    b.append(c);
//                }
//
//                bis.close();
//                attachment.close();
//                tmp.write(String.valueOf(b).getBytes());
//                tmp.close();
//                container.status = true;
//            } catch (FileNotFoundException e) {
//                container.status = false;
////                Log.i(LOG_TAG, "File not found exception..");
//                e.printStackTrace();
//            } catch (IOException e) {
//                container.status = false;
////                Log.i(LOG_TAG, "IOException..");
//                e.printStackTrace();
//            }
//            return container;
//        }
//
//        @Override
//        protected void onPostExecute(Container container) {
////            Log.i(LOG_TAG, "file load status = " + container.status);
////            Log.i(LOG_TAG, "kml file name: " + container.fileName);
//            if(container.status == true){
//                if (container.fileName != null) {
//                    i.putExtra("kml_path", container.fileName);
//                }
//            }
//            startActivity(i);
//            finish();
//
//            return;
//        }
//
//        @Override
//        protected void onPreExecute() {
//        }
//
//        @Override
//        protected void onProgressUpdate(Void... values) {
//        }
//    }
//
//    class Container{
//        Uri uri;
//        String fileName;
//        boolean status;
//    }
//
//}
