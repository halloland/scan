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
public class  Scan extends CordovaPlugin {
    public CallbackContext callbackContext;
    protected final static String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
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
                && PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                && PermissionHelper.hasPermission(this, Manifest.permission.CAMERA);

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
            String[] paths = intent.getStringArrayExtra("paths");
            JSONArray uris = new JSONArray();

            for (int i = 0; i < paths.length; i ++){
                File source = new File(paths[i]);

                Uri resultUri = Uri.fromFile(source);
                uris.put(resultUri);
            }

            callbackContext.success(uris);



        } else {
            callbackContext.error("CENCELED");
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
