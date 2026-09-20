package com.yagay.NotifyLens;

import android.app.Application;
import android.content.SharedPreferences;

import com.yagay.NotifyLens.util.HookAuth;

import java.util.List;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class NotifyLensApp extends Application implements XposedServiceHelper.OnServiceListener {
    public static final String REMOTE_GROUP = "notifylens_runtime";
    public static final String KEY_SECRET = "event_secret";
    public static final String KEY_HOOK_HEARTBEAT = "hook_heartbeat";
    public static final String KEY_HOOK_VERSION = "hook_version";
    public static final String KEY_HOOK_PACKAGE = "hook_package";
    public static final String KEY_HOOK_PROCESS = "hook_process";

    private static volatile XposedService service;
    private static volatile SharedPreferences remote;
    private static volatile String frameworkStatus = "LSPosed/API 102 服务未连接";

    @Override
    public void onCreate() {
        super.onCreate();
        HookAuth.ensureLocalSecret(this);
        XposedServiceHelper.registerListener(this);
    }

    @Override
    public void onServiceBind(XposedService bound) {
        try {
            if (bound.getApiVersion() < 102) {
                frameworkStatus = "框架 API " + bound.getApiVersion() + "，需要 API 102";
                return;
            }
            if ((bound.getFrameworkProperties() & XposedService.PROP_CAP_REMOTE) == 0) {
                frameworkStatus = bound.getFrameworkName() + " 不支持 Remote Preferences";
                return;
            }
            SharedPreferences prefs = bound.getRemotePreferences(REMOTE_GROUP);
            String secret = HookAuth.ensureLocalSecret(this);
            prefs.edit()
                    .putString(KEY_SECRET, secret)
                    .putString("app_version", BuildConfig.VERSION_NAME)
                    .putLong("app_sync_at", System.currentTimeMillis())
                    .commit();

            service = bound;
            remote = prefs;
            List<String> scope = bound.getScope();
            frameworkStatus = bound.getFrameworkName() + " " + bound.getFrameworkVersion()
                    + " · API " + bound.getApiVersion()
                    + " · Scope " + (scope == null ? 0 : scope.size());
        } catch (Throwable t) {
            frameworkStatus = "LSPosed 服务连接失败：" + t.getClass().getSimpleName();
        }
    }

    @Override
    public void onServiceDied(XposedService dead) {
        if (service == dead) {
            service = null;
            remote = null;
            frameworkStatus = "LSPosed/API 102 服务已断开";
        }
    }

    public static String runtimeStatus() {
        StringBuilder out = new StringBuilder(frameworkStatus);
        SharedPreferences prefs = remote;
        if (prefs != null) {
            long heartbeat = prefs.getLong(KEY_HOOK_HEARTBEAT, 0L);
            String version = prefs.getString(KEY_HOOK_VERSION, "");
            String pkg = prefs.getString(KEY_HOOK_PACKAGE, "");
            if (heartbeat > 0) {
                long age = Math.max(0L, System.currentTimeMillis() - heartbeat);
                out.append("\nHook：")
                        .append(age < 120_000L ? "运行中" : "已加载但心跳较旧")
                        .append(" · v").append(version == null ? "?" : version);
                if (pkg != null && !pkg.isEmpty()) out.append(" · ").append(pkg);
                out.append("\n最后心跳：").append(age / 1000L).append(" 秒前");
            } else {
                out.append("\nHook：尚未收到运行心跳");
            }
        }
        return out.toString();
    }
}
