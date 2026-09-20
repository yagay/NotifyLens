package com.yagay.NotifyLens.util;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.LinkedHashSet;

public final class TextUtil {
    private TextUtil() {}

    public static String collectText(View root) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        collect(root, out, 0);
        return String.join("\n", out);
    }

    private static void collect(View view, LinkedHashSet<String> out, int depth) {
        if (view == null || depth > 16) return;
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            if (cs != null) {
                String s = cs.toString().trim();
                if (!s.isEmpty()) out.add(s);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            int max = Math.min(g.getChildCount(), 100);
            for (int i = 0; i < max; i++) collect(g.getChildAt(i), out, depth + 1);
        }
    }
}
