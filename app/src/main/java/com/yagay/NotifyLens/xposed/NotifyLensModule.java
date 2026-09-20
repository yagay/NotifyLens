package com.yagay.NotifyLens.xposed;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.yagay.NotifyLens.BuildConfig;
import com.yagay.NotifyLens.NotifyLensApp;
import com.yagay.NotifyLens.collector.XposedEventReceiver;
import com.yagay.NotifyLens.data.EventTypes;
import com.yagay.NotifyLens.util.HookAuth;
import com.yagay.NotifyLens.util.TextUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class NotifyLensModule extends XposedModule {
    private static final String TAG = "NotifyLens-Xposed";
    private static final Map<Object, PendingUiEvent> PENDING =
            Collections.synchronizedMap(new WeakHashMap<>());

    private volatile SharedPreferences runtimePrefs;

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        try {
            runtimePrefs = getRemotePreferences(NotifyLensApp.REMOTE_GROUP);
            markHeartbeat(param.getProcessName(), "module");
        } catch (Throwable ignored) {}
        log(Log.INFO, TAG, "module loaded");
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        String pkg = param.getPackageName();
        if (!param.isFirstPackage() || pkg == null || pkg.equals("com.yagay.NotifyLens")) return;
        try {
            installFrameworkHooks(pkg);
            installSnackbarHooks(param.getClassLoader(), pkg);
            if ("com.android.systemui".equals(pkg)) {
                installHeadsUpHooks(param.getClassLoader());
            }
            markHeartbeat(param.getProcessName(), pkg);
            log(Log.INFO, TAG, "enhanced capture ready: " + pkg);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "hook setup failed: " + pkg, t);
        }
    }

    private void installFrameworkHooks(String pkg) {
        hookNamed(Toast.class, "makeText", chain -> {
            Context context = argContext(chain);
            String text = textArg(context, chain);
            Object result = chain.proceed();
            if (result instanceof Toast && context != null && text != null && !text.isBlank()) {
                PENDING.put(result, new PendingUiEvent(context, EventTypes.TOAST, text, Toast.class.getName()));
            }
            return result;
        });

        hookNamed(Toast.class, "show", chain -> {
            Object self = chain.getThisObject();
            Object result = chain.proceed();
            if (self instanceof Toast) {
                PendingUiEvent pending = PENDING.remove(self);
                if (pending != null) {
                    emitUi(pending.context, pkg, pending.type, pending.text, pending.className);
                } else {
                    Toast toast = (Toast) self;
                    View view = toast.getView();
                    emitUi(view == null ? null : view.getContext(), pkg, EventTypes.TOAST,
                            TextUtil.collectText(view), toast.getClass().getName());
                }
            }
            return result;
        });

        hookNamed(Dialog.class, "show", chain -> {
            Object result = chain.proceed();
            Object self = chain.getThisObject();
            if (self instanceof Dialog) {
                Dialog d = (Dialog) self;
                View root = d.getWindow() == null ? null : d.getWindow().getDecorView();
                emitUi(d.getContext(), pkg, EventTypes.DIALOG, TextUtil.collectText(root), d.getClass().getName());
            }
            return result;
        });

        XposedInterface.Hooker popupHook = chain -> {
            Object result = chain.proceed();
            Object self = chain.getThisObject();
            if (self instanceof PopupWindow) {
                PopupWindow p = (PopupWindow) self;
                View content = p.getContentView();
                emitUi(content == null ? null : content.getContext(), pkg, EventTypes.POPUP,
                        TextUtil.collectText(content), p.getClass().getName());
            }
            return result;
        };
        hookNamed(PopupWindow.class, "showAsDropDown", popupHook);
        hookNamed(PopupWindow.class, "showAtLocation", popupHook);
    }

    private void installSnackbarHooks(ClassLoader cl, String pkg) {
        try {
            Class<?> snackbar = Class.forName("com.google.android.material.snackbar.Snackbar", false, cl);
            hookNamed(snackbar, "make", chain -> {
                Context context = null;
                String text = null;
                for (Object arg : chain.getArgs()) {
                    if (arg instanceof View && context == null) context = ((View) arg).getContext();
                    if (arg instanceof CharSequence) text = arg.toString();
                }
                Object result = chain.proceed();
                if (result != null && context != null && text != null && !text.isBlank()) {
                    PENDING.put(result, new PendingUiEvent(context, EventTypes.SNACKBAR, text, snackbar.getName()));
                }
                return result;
            });
            hookNamed(snackbar, "show", chain -> {
                Object self = chain.getThisObject();
                Object result = chain.proceed();
                PendingUiEvent pending = self == null ? null : PENDING.remove(self);
                if (pending != null) emitUi(pending.context, pkg, pending.type, pending.text, pending.className);
                return result;
            });
        } catch (Throwable ignored) {}
    }

    private void installHeadsUpHooks(ClassLoader cl) {
        String[] classes = {
                "com.android.systemui.statusbar.notification.headsup.HeadsUpManagerImpl",
                "com.android.systemui.statusbar.policy.HeadsUpManager",
                "com.android.systemui.statusbar.phone.HeadsUpManagerPhone"
        };
        for (String name : classes) {
            try {
                Class<?> c = Class.forName(name, false, cl);
                hookNamed(c, "showNotification", chain -> {
                    Object result = chain.proceed();
                    Object first = chain.getArgs().isEmpty() ? null : chain.getArg(0);
                    StatusBarNotification sbn = extractSbn(first);
                    String key = sbn != null ? sbn.getKey() : extractKey(first);
                    String pkg = sbn != null ? sbn.getPackageName() : "com.android.systemui";
                    emitHeadsUp(currentApplicationContext(), pkg, key, c.getName());
                    return result;
                });
            } catch (Throwable ignored) {}
        }
    }

    private void hookNamed(Class<?> clazz, String name, XposedInterface.Hooker hooker) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (!m.getName().equals(name)) continue;
            try {
                m.setAccessible(true);
                hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(hooker);
            } catch (Throwable t) {
                log(Log.WARN, TAG, "skip " + clazz.getName() + "#" + name + ": " + t);
            }
        }
    }

    private void emitUi(Context context, String pkg, String type, String text, String className) {
        emit(context, pkg, XposedEventReceiver.KIND_UI, type, text, className, "");
    }

    private void emitHeadsUp(Context context, String pkg, String notificationKey, String className) {
        if (notificationKey == null || notificationKey.isEmpty()) return;
        emit(context, pkg, XposedEventReceiver.KIND_HEADS_UP, EventTypes.NOTIFICATION,
                "", className, notificationKey);
    }

    private void emit(Context context, String pkg, String kind, String type,
                      String text, String className, String notificationKey) {
        if (context == null) return;
        text = text == null ? "" : text.trim();
        if (XposedEventReceiver.KIND_UI.equals(kind) && text.isEmpty()) return;
        try {
            SharedPreferences prefs = runtimePrefs;
            String secret = prefs == null ? null : prefs.getString(NotifyLensApp.KEY_SECRET, null);
            if (secret == null || secret.isEmpty()) return;

            long time = System.currentTimeMillis();
            String nonce = UUID.randomUUID().toString();
            String signature = HookAuth.sign(secret, pkg, kind, type, text, className, notificationKey, time, nonce);

            Intent i = new Intent(XposedEventReceiver.ACTION);
            i.setClassName("com.yagay.NotifyLens", "com.yagay.NotifyLens.collector.XposedEventReceiver");
            i.putExtra("package", pkg);
            i.putExtra("kind", kind);
            i.putExtra("type", type);
            i.putExtra("text", text);
            i.putExtra("class", className);
            i.putExtra("notification_key", notificationKey);
            i.putExtra("time", time);
            i.putExtra("nonce", nonce);
            i.putExtra("signature", signature);
            context.sendBroadcast(i);
            markHeartbeat(pkg, pkg);
        } catch (Throwable ignored) {}
    }

    private void markHeartbeat(String process, String pkg) {
        try {
            SharedPreferences prefs = runtimePrefs;
            if (prefs == null) return;
            prefs.edit()
                    .putLong(NotifyLensApp.KEY_HOOK_HEARTBEAT, System.currentTimeMillis())
                    .putString(NotifyLensApp.KEY_HOOK_VERSION, BuildConfig.VERSION_NAME)
                    .putString(NotifyLensApp.KEY_HOOK_PROCESS, process == null ? "" : process)
                    .putString(NotifyLensApp.KEY_HOOK_PACKAGE, pkg == null ? "" : pkg)
                    .apply();
        } catch (Throwable ignored) {}
    }

    private static Context argContext(XposedInterface.Chain chain) {
        for (Object arg : chain.getArgs()) if (arg instanceof Context) return (Context) arg;
        return null;
    }

    private static String textArg(Context context, XposedInterface.Chain chain) {
        for (Object arg : chain.getArgs()) if (arg instanceof CharSequence) return arg.toString();
        if (context != null && chain.getArgs().size() > 1 && chain.getArg(1) instanceof Integer) {
            try { return context.getText((Integer) chain.getArg(1)).toString(); } catch (Throwable ignored) {}
        }
        return null;
    }

    private static StatusBarNotification extractSbn(Object entry) {
        if (entry == null) return null;
        if (entry instanceof StatusBarNotification) return (StatusBarNotification) entry;
        for (String method : new String[]{"getSbn", "getStatusBarNotification"}) {
            try {
                Method m = entry.getClass().getMethod(method);
                Object value = m.invoke(entry);
                if (value instanceof StatusBarNotification) return (StatusBarNotification) value;
            } catch (Throwable ignored) {}
        }
        for (String field : new String[]{"mSbn", "sbn"}) {
            try {
                Field f = entry.getClass().getDeclaredField(field);
                f.setAccessible(true);
                Object value = f.get(entry);
                if (value instanceof StatusBarNotification) return (StatusBarNotification) value;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static String extractKey(Object entry) {
        if (entry == null) return null;
        try {
            Method m = entry.getClass().getMethod("getKey");
            Object value = m.invoke(entry);
            return value == null ? null : value.toString();
        } catch (Throwable ignored) { return null; }
    }

    private static Context currentApplicationContext() {
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Method currentApplication = at.getDeclaredMethod("currentApplication");
            Object app = currentApplication.invoke(null);
            return app instanceof Context ? ((Context) app).getApplicationContext() : null;
        } catch (Throwable ignored) { return null; }
    }

    private static final class PendingUiEvent {
        final Context context;
        final String type;
        final String text;
        final String className;

        PendingUiEvent(Context context, String type, String text, String className) {
            this.context = context.getApplicationContext();
            this.type = type;
            this.text = text;
            this.className = className;
        }
    }
}
