
package io.github.miyazkaori.silentinstaller.demo;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import io.github.miyazkaori.silentinstaller.SilentInstaller;
import io.github.miyazkaori.silentinstaller.demo.databinding.ActivityMainBinding;
import io.github.miyazkaori.silentinstaller.utils.ShizukuPermissionHelper;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        SilentInstaller.init(getApplication());
            
        this.binding.btnRequestShizuku.setOnClickListener(v -> {
            ShizukuPermissionHelper.requestPermission(new ShizukuPermissionHelper.PermissionCallback() {
                @Override
                public void onGranted() {
                    showToast("已获取Shizuku权限");
                }
                
                @Override
                public void onDenied() {
                    showToast("获取Shizuku权限失败");
                }
            });
        });
        
        this.binding.btnSelectApp.setOnClickListener(v -> {
            openApkFilePicker();
        });
    }
    
    private static final int REQUEST_CODE_PICK_APK = 100;

    private void openApkFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        startActivityForResult(intent, REQUEST_CODE_PICK_APK);
    }
    
    private SilentInstaller.InstallCallback installCallback = new SilentInstaller.InstallCallback() {
        @Override
        public void onPermissionDenied() {
            // TODO: Implement this method
            showToast("Permission denied");
        }
        
        @Override
        public void onSuccess(int status, String message) {
            // TODO: Implement this method
            showToast("Install success: " + message);
        }
        
        @Override
        public void onFailure(int status, String message, Throwable tr) {
            // TODO: Implement this method
            showToast("Install failure: " + message);
        }
        
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_APK && resultCode == RESULT_OK) {
            if (data != null) {
                Uri apkUri = data.getData();
                if (apkUri != null) {
           //         showToast("Install for: " + apkUri);
                    SilentInstaller.install(apkUri, installCallback);
                }
            }
        }
    }

    
    private void showToast(String msg) {
    	runOnUiThread(()->{
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}
