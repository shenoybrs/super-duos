package barqsoft.footballscores.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Created by yehya khaled on 2/25/2015.
 */
public class ScoresProvider extends ContentProvider
{
    private static ScoresDBHelper mOpenHelper;
    private static final int MATCHES = 100;
    private static final int MATCHES_WITH_LEAGUE = 101;
    private static final int MATCHES_WITH_ID = 102;
    private static final int MATCHES_WITH_DATE = 103;
    private static final int MATCHES_MOST_RECENT = 104;

    private UriMatcher muriMatcher = buildUriMatcher();
    private static final SQLiteQueryBuilder ScoreQuery =
            new SQLiteQueryBuilder();
    private static final String SCORES_BY_LEAGUE = ScoreContract.ScoreColumn.LEAGUE_COL + " = ?";
    private static final String SCORES_MOST_RECENT =
            ScoreContract.ScoreColumn.DATE_COL + " <= ? AND "+
                    ScoreContract.ScoreColumn.HOME_GOALS_COL + " != 'null' AND "+
                    ScoreContract.ScoreColumn.AWAY_GOALS_COL + " != 'null'";
    private static final String SCORES_BY_DATE =
            ScoreContract.ScoreColumn.DATE_COL + " LIKE ?";
    private static final String SCORES_BY_ID =
            ScoreContract.ScoreColumn.MATCH_ID + " = ?";
    public static final String ACTION_DATA_UPDATED = ScoreContract.CONTENT_AUTHORITY + ".ACTION_DATA_UPDATED";

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ScoreContract.BASE_CONTENT_URI.toString();
        matcher.addURI(authority, null , MATCHES);
        matcher.addURI(authority, "league" , MATCHES_WITH_LEAGUE);
        matcher.addURI(authority, "id" , MATCHES_WITH_ID);
        matcher.addURI(authority, "date" , MATCHES_WITH_DATE);
        matcher.addURI(authority, "recent", MATCHES_MOST_RECENT);
        return matcher;
    }

    private int match_uri(Uri uri)
    {
        String link = uri.toString();
        {
           if(link.contentEquals(ScoreContract.BASE_CONTENT_URI.toString()))
           {
               return MATCHES;
           }
           else if(link.contentEquals(ScoreContract.ScoreColumn.buildScoreWithDate().toString()))
           {
               return MATCHES_WITH_DATE;
           }
           else if(link.contentEquals(ScoreContract.ScoreColumn.buildScoreWithId().toString()))
           {
               return MATCHES_WITH_ID;
           }
           else if(link.contentEquals(ScoreContract.ScoreColumn.buildScoreWithLeague().toString()))
           {
               return MATCHES_WITH_LEAGUE;
           }else if(link.contentEquals(ScoreContract.ScoreColumn.buildScoreMostRecent().toString())) {
               return MATCHES_MOST_RECENT;
           }
        }
        return -1;
    }
    @Override
    public boolean onCreate()
    {
        mOpenHelper = new ScoresDBHelper(getContext());
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        return 0;
    }

    @Override
    public String getType(Uri uri)
    {
        final int match = muriMatcher.match(uri);
        switch (match) {
            case MATCHES:
                return ScoreContract.ScoreColumn.CONTENT_TYPE;
            case MATCHES_WITH_LEAGUE:
                return ScoreContract.ScoreColumn.CONTENT_TYPE;
            case MATCHES_WITH_ID:
                return ScoreContract.ScoreColumn.CONTENT_ITEM_TYPE;
            case MATCHES_WITH_DATE:
                return ScoreContract.ScoreColumn.CONTENT_TYPE;
            case MATCHES_MOST_RECENT:
                return ScoreContract.ScoreColumn.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri :" + uri );
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        Cursor retCursor;

        int match = match_uri(uri);

        switch (match)
        {
            case MATCHES: retCursor = mOpenHelper.getReadableDatabase().query(
                    ScoreContract.SCORES_TABLE,
                    projection,null,null,null,null,sortOrder); break;
            case MATCHES_WITH_DATE:

                    retCursor = mOpenHelper.getReadableDatabase().query(
                    ScoreContract.SCORES_TABLE,
                    projection,SCORES_BY_DATE,selectionArgs,null,null,sortOrder); break;
            case MATCHES_WITH_ID: retCursor = mOpenHelper.getReadableDatabase().query(
                    ScoreContract.SCORES_TABLE,
                    projection,SCORES_BY_ID,selectionArgs,null,null,sortOrder); break;
            case MATCHES_WITH_LEAGUE: retCursor = mOpenHelper.getReadableDatabase().query(
                    ScoreContract.SCORES_TABLE,
                    projection,SCORES_BY_LEAGUE,selectionArgs,null,null,sortOrder); break;
            case MATCHES_MOST_RECENT: retCursor = mOpenHelper.getReadableDatabase().query(
                    ScoreContract.SCORES_TABLE,
                    projection,SCORES_MOST_RECENT,selectionArgs,null,null,sortOrder);break;
            default: throw new UnsupportedOperationException("Unknown Uri" + uri);
        }
        if (getContext() != null)
            retCursor.setNotificationUri(getContext().getContentResolver(),uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values)
    {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (match_uri(uri))
        {
            case MATCHES:
                db.beginTransaction();
                int returncount = 0;
                try
                {
                    for(ContentValues value : values)
                    {
                        long _id = db.insertWithOnConflict(ScoreContract.SCORES_TABLE, null, value,
                                SQLiteDatabase.CONFLICT_REPLACE);
                        if (_id != -1)
                        {
                            returncount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED).setPackage(getContext().getPackageName());
                    getContext().sendBroadcast(dataUpdatedIntent);
                }
                return returncount;
            default:
                return super.bulkInsert(uri,values);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
}
