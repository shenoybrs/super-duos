package it.jaschke.alexandria.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import it.jaschke.alexandria.MainActivity;
import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class BookService extends IntentService {

    private final String LOG_TAG = BookService.class.getSimpleName();

    public static final String FETCH_BOOK = "it.jaschke.alexandria.services.action.FETCH_BOOK";
    public static final String CONFIRM_BOOK = "it.jaschke.alexandria.services.action.CONFIRM_BOOK";
    public static final String DELETE_BOOK = "it.jaschke.alexandria.services.action.DELETE_BOOK";
    public static final String DELETE_NOT_SAVED = "it.jaschke.alexandria.services.action.DELETE_NOT_SAVED";

    public static final String EAN = "it.jaschke.alexandria.services.extra.EAN";

    public BookService() {
        super("Alexandria");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
			final String ean = intent.getStringExtra(EAN);
            if (FETCH_BOOK.equals(action)) {
                                fetchBook(ean);

            } else if (CONFIRM_BOOK.equals(action)) {

                int saved = saveBook(ean);

                if (saved != 0) {
                    Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                    messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.book_saved));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                }

            } else if (DELETE_BOOK.equals(action)) {

                // delete book with given ean from the database
                int deleted = deleteBook(ean);

                if (deleted != 0) {
                    Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                    messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.book_deleted));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                }

            } else if (DELETE_NOT_SAVED.equals(action)) {

                // cleanup books table by removing books that were previously cached, but not added to the booklist
                deleteNotSavedBooks();
            }
        }
    }

    /**
     * Handle action fetchBook in the provided background thread with the provided
     * parameters.
     */
    private boolean fetchBook(String ean) {

        boolean found = false;
        boolean cached = false;

        // only continue if ean number has 13 digits
        if (ean.length() != 13) {
            return found;
        }

        // create an intent for broadcasting the result back to the mainactivity
        Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);

        // try to load the book with given ean using the bookuri with given ean from the database
        Cursor bookEntry = getContentResolver().query(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );


        if (bookEntry != null) {
            if (bookEntry.getCount() > 0) {
                if (bookEntry.moveToFirst()) {
                    int saved = bookEntry.getInt(bookEntry.getColumnIndex(AlexandriaContract.BookEntry.SAVED));
                    if (saved == 1) {
                        found = true;
                        messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.book_saved_before));
                    } else {
                        cached = true;
                    }
                }
            }
            bookEntry.close();
        }

        if (!found) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String bookJsonString = null;

            try {

                final String BOOKS_BASE_URL = getString(R.string.books_api);
                final String QUERY_PARAM = "q";
                final String ISBN_PARAM = "isbn:" + ean;

                Uri builtUri = Uri.parse(BOOKS_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                        .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if (inputStream != null) {
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;

                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                        buffer.append("\n");
                    }

                    if (buffer.length() != 0) {
                        bookJsonString = buffer.toString();
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

        final String ITEMS = "items";

        final String VOLUME_INFO = "volumeInfo";

        final String TITLE = "title";
        final String SUBTITLE = "subtitle";
        final String AUTHORS = "authors";
        final String DESC = "description";
        final String CATEGORIES = "categories";
        final String IMG_URL_PATH = "imageLinks";
        final String IMG_URL = "thumbnail";

            try {
                JSONObject bookJson = new JSONObject(bookJsonString);
                JSONArray bookArray;

                if (!bookJson.has(ITEMS)) {

                    // if json has no items, send message with intent to main activity to show a toast
                    messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.book_not_found));

                } else {

                    // get the books from the json
                    bookArray = bookJson.getJSONArray(ITEMS);
                    JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(VOLUME_INFO);

            String title = bookInfo.getString(TITLE);

            String subtitle = "";
            if(bookInfo.has(SUBTITLE)) {
                subtitle = bookInfo.getString(SUBTITLE);
            }

            String desc="";
            if(bookInfo.has(DESC)){
                desc = bookInfo.getString(DESC);
            }

            String imgUrl = "";
            if(bookInfo.has(IMG_URL_PATH) && bookInfo.getJSONObject(IMG_URL_PATH).has(IMG_URL)) {
                imgUrl = bookInfo.getJSONObject(IMG_URL_PATH).getString(IMG_URL);
            }

 		if (!cached) {
             writeBackBook(ean, title, subtitle, desc, imgUrl);
		}

            if(bookInfo.has(AUTHORS)) {
                writeBackAuthors(ean, bookInfo.getJSONArray(AUTHORS));
            }
            if(bookInfo.has(CATEGORIES)){
                writeBackCategories(ean,bookInfo.getJSONArray(CATEGORIES) );
            }

                    found = true;
                }

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
            }
        }

        // send message back to the mainactivity about the result
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);

        return found;
    }

    private void writeBackBook(String ean, String title, String subtitle, String desc, String imgUrl) {
        ContentValues values= new ContentValues();
        values.put(AlexandriaContract.BookEntry._ID, ean);
        values.put(AlexandriaContract.BookEntry.SAVED, 0);
        values.put(AlexandriaContract.BookEntry.TITLE, title);
        values.put(AlexandriaContract.BookEntry.IMAGE_URL, imgUrl);
        values.put(AlexandriaContract.BookEntry.SUBTITLE, subtitle);
        values.put(AlexandriaContract.BookEntry.DESC, desc);
        getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI,values);
    }

    /**
     * Mark a temporarily inserted book as saved
     * @param ean String
     * @return int
     */
    private int saveBook(String ean) {

        if (ean != null) {

            ContentValues values = new ContentValues();
            values.put(AlexandriaContract.BookEntry.SAVED, 1);

            return getContentResolver().update(
                    AlexandriaContract.BookEntry.CONTENT_URI,
                    values,
                    AlexandriaContract.BookEntry._ID +" = ?",
                    new String[] { ean }
            );
        } else {
            return 0;
        }
    }

    /**
     * Delete book with given ean number from the database
     * @param ean String
     * @return int
     */
    private int deleteBook(String ean) {
        if(ean != null) {
            return getContentResolver().delete(
                    AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                    null,
                    null
            );
        } else {
            return 0;
        }
    }

    /**
     * Remove the previously fetched, but not saved, books from the books table
     * @return int
     */
    private int deleteNotSavedBooks() {
        return getContentResolver().delete(
                AlexandriaContract.BookEntry.CONTENT_URI,
                AlexandriaContract.BookEntry.SAVED +" = ?",
                new String[] {"0"}
        );
    }

    /**
     * Insert list of author names in the database for given book ean
     * @param ean String
     * @param authors JSONArray
     * @throws JSONException
     */
    private void writeBackAuthors(String ean, JSONArray authors) throws JSONException {

        // loop through array containing the authors
        for (int i = 0; i < authors.length(); i++) {

            // create contentvalues object and add fields for database insert
            ContentValues authorValues = new ContentValues();
            authorValues.put(AlexandriaContract.AuthorEntry._ID, ean);
            authorValues.put(AlexandriaContract.AuthorEntry.AUTHOR, authors.getString(i));

            // insert author in the database
            getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, authorValues);
        }
    }

    private void writeBackCategories(String ean, JSONArray jsonArray) throws JSONException {
        
        for (int i = 0; i < jsonArray.length(); i++) {
		ContentValues values= new ContentValues();
            values.put(AlexandriaContract.CategoryEntry._ID, ean);
            values.put(AlexandriaContract.CategoryEntry.CATEGORY, jsonArray.getString(i));
            getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
            
        }
    }
 }