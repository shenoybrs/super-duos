package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilities;
import barqsoft.footballscores.data.ScoreContract;

public class LatestFixtureWidgetService extends IntentService {

    // tag logging with classname
    private static final String LOG_TAG = LatestFixtureWidgetService.class.getSimpleName();

    /**
     * Constructor
     */
    public LatestFixtureWidgetService() {
        super("LatestFixtureWidgetService");
    }

    /**
     * Handle the given intent when starting the service
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        // find the active instances of our widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, LatestFixtureWidgetProvider.class));

        // create the date and time of now
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // get most recent fixture from the contentprovider
        Uri scoreMostRecentUri = ScoreContract.ScoreColumn.buildScoreMostRecent();
        Cursor cursor = getContentResolver().query(
                scoreMostRecentUri,
                null,
                null,
                new String[] { simpleDateFormat.format(date) },
                ScoreContract.ScoreColumn.DATE_COL +" DESC, "+ ScoreContract.ScoreColumn.TIME_COL +" DESC");

        // manage the cursor
        if (cursor == null) {
            return;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }

        // loop through all our active widget instances
        for (int appWidgetId : appWidgetIds) {

            // get our layout
            final RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_latest_fixture);


            views.setImageViewResource(R.id.home_crest,Utilities.getTeamCrestByTeamName(
                    cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreColumn.HOME_COL))));


            views.setImageViewResource(R.id.away_crest,Utilities.getTeamCrestByTeamName(
                    cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreColumn.AWAY_COL))));

            // home team name
            String homeTeamName = cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreColumn.HOME_COL));
            views.setTextViewText(R.id.home_name, homeTeamName);
            views.setTextColor(R.id.home_name, ContextCompat.getColor(getApplicationContext(), R.color.secondary_text));

            // set content description on home team logo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, R.id.home_crest, homeTeamName);
            }

            // score
            views.setTextViewText(R.id.score_textview, Utilities.getScores(
                    cursor.getInt(cursor.getColumnIndex(ScoreContract.ScoreColumn.HOME_GOALS_COL)),
                    cursor.getInt(cursor.getColumnIndex(ScoreContract.ScoreColumn.AWAY_GOALS_COL))));
            views.setTextColor(R.id.score_textview, ContextCompat.getColor(getApplicationContext(), R.color.secondary_text));

            // match time
            views.setTextViewText(R.id.date_textview, cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreColumn.TIME_COL)));
            views.setTextColor(R.id.date_textview, ContextCompat.getColor(getApplicationContext(), R.color.secondary_text));



            // away team name
            String awayTeamName = cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreColumn.AWAY_COL));
            views.setTextViewText(R.id.away_name, awayTeamName);
            views.setTextColor(R.id.away_name, ContextCompat.getColor(getApplicationContext(), R.color.secondary_text));

            // set content description on away team logo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, R.id.away_crest, awayTeamName);
            }

            // launch the app onclick on the widget and pass the fixture details for date and position selection
            Intent launchIntent = new Intent(this, MainActivity.class);
            final Bundle extras = new Bundle();
            extras.putString(LatestFixtureWidgetProvider.SCORES_DATE,
                    cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreColumn.DATE_COL)));
            extras.putInt(TodaysFixturesWidgetProvider.SCORES_MATCH_ID,
                    cursor.getInt(cursor.getColumnIndex(ScoreContract.ScoreColumn.MATCH_ID)));
            launchIntent.putExtras(extras);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, Intent.FILL_IN_ACTION);
            views.setOnClickPendingIntent(R.id.widget_latest_fixtures, pendingIntent);

            // update the widget with the set views
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        // close the cursor when we're done
        cursor.close();
    }

     /**
     * Set the contentdescription on a remote view
     * @param views RemoteViews
     * @param viewId int
     * @param description String
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, int viewId, String description) {
        views.setContentDescription(viewId, description);
    }
}