package barqsoft.footballscores.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by yehya khaled on 2/25/2015.
 */
public class ScoresDBHelper extends SQLiteOpenHelper
{
    public static final String DATABASE_NAME = "Scores.db";
    private static final int DATABASE_VERSION = 2;
    public ScoresDBHelper(Context context)
    {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        final String CreateScoresTable = "CREATE TABLE "+ ScoreContract.SCORES_TABLE +" ("+
                ScoreContract.ScoreColumn._ID +" INTEGER PRIMARY KEY,"+
                ScoreContract.ScoreColumn.DATE_COL +" TEXT NOT NULL,"+
                ScoreContract.ScoreColumn.TIME_COL +" INTEGER NOT NULL,"+
                ScoreContract.ScoreColumn.HOME_COL +" TEXT NOT NULL,"+
                ScoreContract.ScoreColumn.HOME_ID_COL +" INTEGER NOT NULL,"+
                ScoreContract.ScoreColumn.HOME_LOGO_COL +" TEXT,"+
                ScoreContract.ScoreColumn.HOME_GOALS_COL +" TEXT NOT NULL,"+
                ScoreContract.ScoreColumn.AWAY_COL +" TEXT NOT NULL,"+
                ScoreContract.ScoreColumn.AWAY_ID_COL +" INTEGER NOT NULL,"+
                ScoreContract.ScoreColumn.AWAY_LOGO_COL +" TEXT,"+
                ScoreContract.ScoreColumn.AWAY_GOALS_COL +" TEXT NOT NULL,"+
                ScoreContract.ScoreColumn.LEAGUE_COL +" INTEGER NOT NULL,"+
                ScoreContract.ScoreColumn.MATCH_ID +" INTEGER NOT NULL,"+
                ScoreContract.ScoreColumn.MATCH_DAY +" INTEGER NOT NULL,"+
                " UNIQUE ("+ ScoreContract.ScoreColumn.MATCH_ID +") ON CONFLICT REPLACE);";
        db.execSQL(CreateScoresTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        //Remove old values when upgrading.
        db.execSQL("DROP TABLE IF EXISTS " + ScoreContract.SCORES_TABLE);
    }
}
