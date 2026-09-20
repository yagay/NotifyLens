package com.yagay.NotifyLens.xposed;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.yagay.NotifyLens.collector.XposedEventReceiver;
import com.yagay.NotifyLens.data.EventTypes;
import com.yagay.NotifyLens.util.TextUtil;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class NotifyLensModule extends XposedModule {
    private static final String TAG = "NotifyLens-Xposed";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        log(Log.INFO, TAG, "module loaded");
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        String pkg = param.getPackageName();
        if (!param.isFirstPackage() || pkg == null || pkg.equals("com.yagay.NotifyLens")) return;
        try {
            installFrameworkHooks(pkg);
            installSnackbarHooks(param.getClassLoader(), pkg);
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
            emit(context, pkg, EventTypes.TOAST, text, Toast.class.getName());
            return result;
        });

        hookNamed(Dialog.class, "show", chain -> {
            Object result = chain.proceed();
            Object self = chain.getThisObject();
            if (self instanceof Dialog) {
                Dialog d = (Dialog) self;
                View root = d.getWindow() == null ? null : d.getWindow().getDecorView();
                emit(d.getContext(), pkg, EventTypes.DIALOG, TextUtil.collectText(root), d.getClass().getName());
            }
            return result;
        });

        XposedInterface.Hooker popupHook = chain -> {
            Object result = chain.proceed();
            Object self = chain.getThisObject();
            if (self instanceof PopupWindow) {
                PopupWindow p = (PopupWindow) self;
                View content = p.getContentView();
                emit(content == null ? null : content.getContext(), pkg, EventTypes.POPUP,
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
                emit(context, pkg, EventTypes.SNACKBAR, text, snackbar.getName());
                return result;
            });
        } catch (Throwable ignored) {
            // App does not use Material Snackbar.
        }
    }

    private void hookNamed(Class<?> clazz, String name, XposedInterface.Hooker hooker) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (!m.getName().equals(name)) continue;
            try {
                m.setAccessible(true);
                hook(m)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(hooker);
            } catch (Throwable t) {
                log(Log.WARN, TAG, "skip " + clazz.getName() + "#" + name + ": " + t);
            }
        }
    }

    private static Context argContext(XposedInterface.Chain chain) {
        for (Object arg : chain.getArgs()) if (arg instanceof Context) return (Context) arg;
        return null;
    }

    private static String textArg(Context context, XposedInterface.Chain chain) {
        for (Object arg : chain.getArgs()) {
            if (arg instanceof CharSequence) return arg.toString();
        }
        if (context != null && chain.getArgs().size() > 1 && chain.getArg(1) instanceof Integer) {
            try { return context.getText((Integer) chain.getArg(1)).toString(); } catch (Throwable ignored) {}
        }
        return null;
    }

    private static void emit(Context context, String pkg, String type, String text, String className) {
        if (context == null || text == null || text.trim().isEmpty()) return;
        try {
            Intent i = new Intent(XposedEventReceiver.ACTION);
            i.setClassName("com.yagay.NotifyLens", "com.yagay.NotifyLens.collector.XposedEventReceiver");
            i.putExtra("package", pkg);
            i.putExtra("type", type);
            i.putExtra("text", text.trim());
            i.putExtra("class", className);
            i.putExtra("time", System.currentTimeMillis());
            context.sendBroadcast(i);
        } catch (Throwable ignored) {}
    }
}
