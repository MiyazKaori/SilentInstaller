package io.github.miyazkaori.silentinstaller.utils;

import android.app.*;
import android.content.pm.*;
import androidx.annotation.*;
import rikka.shizuku.*;

public final class ShizukuPermissionHelper {

    private ShizukuPermissionHelper() {}

    public interface PermissionCallback {
        void onGranted();
        void onDenied();
    }

    private static final int REQUEST_CODE = 1010;

    public static void requestPermission(@NonNull PermissionCallback callback) {
        if (Shizuku.isPreV11() || !Shizuku.pingBinder()) {
            callback.onDenied();
            return;
        }
        
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                callback.onGranted();
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                callback.onDenied();
            } else {
                Shizuku.addRequestPermissionResultListener(new OnRequestPermissionResultListener(callback));
                Shizuku.requestPermission(REQUEST_CODE);
            }
        } catch (Throwable e) {
            callback.onDenied();
            e.printStackTrace();
        }
    }

    private static class OnRequestPermissionResultListener implements Shizuku.OnRequestPermissionResultListener {
    
        private PermissionCallback callback;
        
        OnRequestPermissionResultListener(PermissionCallback callback) {
            this.callback = callback;
        }
        
        @Override
        public void onRequestPermissionResult(int requestCode, int grantResult) {
            Shizuku.removeRequestPermissionResultListener(this);
            if (requestCode == REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    callback.onGranted();
                } else {
                    callback.onDenied();
                }
            }
        }
        
    }
}