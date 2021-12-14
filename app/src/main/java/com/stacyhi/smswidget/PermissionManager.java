package com.stacyhi.smswidget;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager extends AppCompatActivity {
    public static final String TAG = "PermissionManager";
    public static final int REQUEST_ALL_PERMISSIONS = 1;
    public static final String[] permissions = new String[] {
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.RECEIVE_MMS
    };

    public static boolean checkAllPermissions(Context mContext) {
        for (String permission : permissions) {
            if(ActivityCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    public void requestAllPermissions() {
        List<String> remainingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                remainingPermissions.add(permission);
            }
        }
        if ( remainingPermissions.size() > 0 ) {
            requestPermissions(remainingPermissions.toArray(new String[remainingPermissions.size()]), PermissionManager.REQUEST_ALL_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.REQUEST_ALL_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(permissions[i])) {
                        requestAllPermissions();
                    } else {
                        openSettingsDialog();
                    }
                    return;
                }
            }
        }
    }

    private void openSettingsDialog(){
        //If User was asked permission before and denied
        new AlertDialog.Builder(this)
                .setTitle("Permission needed")
                .setMessage("Please enable permissions for SMS and Contacts")
                .setPositiveButton("Open Setting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", PermissionManager.this.getPackageName(),
                                null);
                        intent.setData(uri);
                        PermissionManager.this.startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}