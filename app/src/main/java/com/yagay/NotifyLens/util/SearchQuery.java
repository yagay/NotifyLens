package com.yagay.NotifyLens.util;

public final class SearchQuery {
    private SearchQuery() {}

    public static String fts(String input) {
        if (input == null) return "";
        String[] parts = input.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            String clean = part.replace('"', ' ').replace("*", "").trim();
            if (clean.isEmpty()) continue;
            if (out.length() > 0) out.append(" AND ");
            out.append('"').append(clean).append('"').append('*');
        }
        return out.toString();
    }
}
