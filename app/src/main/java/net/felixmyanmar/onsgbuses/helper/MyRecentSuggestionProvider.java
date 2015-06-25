package net.felixmyanmar.onsgbuses.helper;

import android.content.SearchRecentSuggestionsProvider;

public class MyRecentSuggestionProvider extends SearchRecentSuggestionsProvider {

    public final static String AUTHORITY = "net.felixmyanmar.onsgbuses.helper.MyRecentSuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public MyRecentSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

}
