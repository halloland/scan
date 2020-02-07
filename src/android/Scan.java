package halloland.scan;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


import static android.app.Activity.RESULT_OK;


/**
 * This class echoes a string called from JavaScript.
 */
public class Scan extends CordovaPlugin {
    public CallbackContext callbackContext;
    protected final static String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private Context context;
    private JSONArray options;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scan")) {
            this.callbackContext = callbackContext;
            this.context = cordova.getActivity().getApplicationContext();

            if (this.checkPermissions()) {
                if(!args.isNull(0)){
                    this.options = args;
                }

                this.openNewActivity();
            } else {
                this.requestPermissions();
            }


            return true;
        }
        return false;
    }

    private void requestPermissions() {
        PermissionHelper.requestPermissions(this, 0, permissions);
    }

    private boolean checkPermissions() {

        return PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                && PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                this.callbackContext.error("PERMISSION_DENIED_ERROR");
                return;
            }
        }

        openNewActivity();
    }

    private void openNewActivity() {
        Scan that = this;
        File[] files = this.cordova.getActivity().getApplicationContext().getExternalFilesDirs(null);
        String sdCardPath = null;
        for (File file : files){
            String rootPath = file.getPath().split("/Android")[0];
            File rootFile = new File(rootPath);
            if(Environment.isExternalStorageRemovable(rootFile)){
                sdCardPath = rootFile.getAbsolutePath();
            }
        }
        Intent intent = new Intent(context, MainScreen.class);
        if(this.options != null){
            intent.putExtra("options", this.options.toString());
        }
        intent.putExtra("cachePath", this.getTempDirectoryPath());




        cordova.setActivityResultCallback(that);
        cordova.getActivity().startActivityForResult(intent, 2);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            String path = intent.getStringExtra("path");
            File source = new File(path);

            Uri resultUri = Uri.fromFile(source);

            callbackContext.success(resultUri.toString());



        } else {
            callbackContext.error(intent.getStringExtra("error"));
        }
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    private String getTempDirectoryPath() {
        File cache = null;

        // SD Card Mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cache = cordova.getActivity().getExternalCacheDir();
        }
        // Use internal storage
        else {
            cache = cordova.getActivity().getCacheDir();
        }

        // Create the cache directory if it doesn't exist
        cache.mkdirs();
        return cache.getAbsolutePath();
    }
}
