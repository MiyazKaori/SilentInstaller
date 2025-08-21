package io.github.miyazkaori.silentinstaller;

import android.app.Application;
import android.content.pm.IPackageInstallerSession;
import java.io.File;
import android.net.Uri;
import java.io.InputStream;
import androidx.annotation.NonNull;
import org.lsposed.hiddenapibypass.HiddenApiBypass;
import io.github.miyazkaori.silentinstaller.logger.AppLog;
import io.github.miyazkaori.silentinstaller.utils.ApplicationUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import io.github.miyazkaori.silentinstaller.utils.ShizukuPermissionHelper;
import android.content.pm.PackageInstaller;
import android.content.ContentResolver;
import io.github.miyazkaori.silentinstaller.utils.ShizukuSystemServerApi;
import rikka.shizuku.Shizuku;
import io.github.miyazkaori.silentinstaller.utils.PackageInstallerUtils;
import rikka.shizuku.ShizukuBinderWrapper;
import java.io.OutputStream;
import java.io.IOException;
import android.content.Intent;
import java.util.concurrent.CountDownLatch;
import android.content.IntentSender;
import io.github.miyazkaori.silentinstaller.utils.IIntentSenderAdaptor;
import io.github.miyazkaori.silentinstaller.utils.IntentSenderUtils;
import android.os.Build;
import android.content.pm.IPackageInstaller;


public final class SilentInstaller {
    private SilentInstaller() {
    	
    }
    
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("L");
            AppLog.d("解除隐藏API反射限制");
        }
    }
    
    public static void init(@NonNull Application application) {
    	ApplicationUtils.setApplication(application);
        AppLog.d("Silent installer初始化完成");
    }
    
    public static void install(@NonNull String apkFilePath, @NonNull InstallCallback callback) {
    	install(new File(apkFilePath), callback);
    }
    
    public static void install(@NonNull File apkFile, @NonNull InstallCallback callback) {
        try {
            install(new FileInputStream(apkFile), callback);
        } catch (FileNotFoundException e) {
            callback.onFailure(-1, e.getMessage(), e);
        }
    }
    
    public static void install(@NonNull Uri apkFileUri, @NonNull InstallCallback callback) {
        try {
            install(ApplicationUtils.getApplication().getContentResolver().openInputStream(apkFileUri), callback);
        } catch (FileNotFoundException e) {
            callback.onFailure(-1, e.getMessage(), e);
        }
    }
    
    private synchronized static void install(InputStream inputStream, InstallCallback callback) {
    	ShizukuPermissionHelper.requestPermission(new ShizukuPermissionHelper.PermissionCallback() {
            public void onGranted() {
                new Thread(() -> {
                    doInstall(inputStream, callback);
                }).start();
            }
            
            public void onDenied() {
                callback.onPermissionDenied();
                AppLog.e("没有使用Shizuku的权限");
            }
        });
    }
    
    private static void doInstall(InputStream is, InstallCallback callback) {
        PackageInstaller packageInstaller;
        PackageInstaller.Session session = null;
        String installerPackageName;
        String installerAttributionTag = null;
        int userId;
        
        AppLog.d("安装开始");

        try {
            IPackageInstaller _packageInstaller = ShizukuSystemServerApi.PackageManager_getPackageInstaller();

            // the reason for use "com.android.shell" as installer package under adb is that getMySessions will check installer package's owner
            installerPackageName =  "com.android.shell";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                installerAttributionTag = ApplicationUtils.getApplication().getAttributionTag();
            }
            userId = 0;
            packageInstaller = PackageInstallerUtils.createPackageInstaller(_packageInstaller, installerPackageName, installerAttributionTag, userId);
            int sessionId;

            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int installFlags = PackageInstallerUtils.getInstallFlags(params);
            installFlags |= 0x00000004/*PackageManager.INSTALL_ALLOW_TEST*/ | 0x00000002/*PackageManager.INSTALL_REPLACE_EXISTING*/;
            PackageInstallerUtils.setInstallFlags(params, installFlags);

            sessionId = packageInstaller.createSession(params);
            AppLog.d("Create Session: %s", sessionId);

            IPackageInstallerSession _session = IPackageInstallerSession.Stub.asInterface(new ShizukuBinderWrapper(_packageInstaller.openSession(sessionId).asBinder()));
            session = PackageInstallerUtils.createSession(_session);

            String name = "base.apk";
            OutputStream os = session.openWrite(name, 0, -1);
            byte[] buf = new byte[8192];
            int len;
            try {
                while ((len = is.read(buf)) > 0) {
                    os.write(buf, 0, len);
                    os.flush();
                    session.fsync(os);
                }
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Thread.sleep(300);

            Intent[] results = new Intent[]{null};
            CountDownLatch countDownLatch = new CountDownLatch(1);
            IntentSender intentSender = IntentSenderUtils.newInstance(new IIntentSenderAdaptor() {
                @Override
                public void send(Intent intent) {
                    results[0] = intent;
                    countDownLatch.countDown();
                }
            });
            AppLog.d("Commit install");
            session.commit(intentSender);

            countDownLatch.await();
            Intent result = results[0];
            int status = result.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            String message = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            
            if(status == PackageInstaller.STATUS_SUCCESS) {
                callback.onSuccess(status, message);
                AppLog.d("安装成功, status: %s, msg: %s", status, message);
            } else {
                callback.onFailure(status, message, null);
                AppLog.e("安装失败, status: %s, msg: %s", status, message);
            }
        } catch (Throwable tr) {
            callback.onFailure(-1, tr.getMessage(), null);
            tr.printStackTrace();
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Throwable tr) {
                    tr.printStackTrace();
                }
            }
        }
    }

    public static interface InstallCallback {
        void onPermissionDenied();
        
        void onSuccess(int status, String message);
        
        void onFailure(int status, String message, Throwable tr);
    }
}
