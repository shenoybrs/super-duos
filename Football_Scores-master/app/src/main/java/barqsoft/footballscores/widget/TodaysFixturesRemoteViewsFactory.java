package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilities;
import barqsoft.footballscores.data.ScoreContract;

public class TodaysFixturesRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    // tag logging with classname
    private static final String LOG_TAG = TodaysFixturesRemoteViewsFactory.class.getSimpleName();

    // reference to the context
    private Context mContext;

    // cursor containing the fixtures
    private Cursor data = null;

    /**
     * Constructor
     * @param context Context
     */
    public TodaysFixturesRemoteViewsFactory(Context context) {
        mContext = context;
    }

    /**
     * Populate the view at the given position
     * @param position int
     * @return RemoteViews
     */
    @Override
    public RemoteViews getViewAt(int position) {
        if (position == AdapterView.INVALID_POSITION || data == null || !data.moveToPosition(position)) {
            return null;
        }

        // get the list item layout
        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_todays_list_item);


        views.setImageViewResource(R.id.home_crest,Utilities.getTeamCrestByTeamName(
                data.getString(data.getColumnIndex(ScoreContract.ScoreColumn.HOME_COL))));
        views.setImageViewResource(R.id.away_crest,Utilities.getTeamCrestByTeamName(
                data.getString(data.getColumnIndex(ScoreContract.ScoreColumn.AWAY_COL))));



        // home team name
        String homeTeamName = data.getString(data.getColumnIndex(ScoreContract.ScoreColumn.HOME_COL));
        views.setTextViewText(R.id.home_name, homeTeamName);
        views.setTextColor(R.id.home_name, ContextCompat.getColor(mContext, R.color.secondary_text));

        // set content description on home team logo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            setRemoteContentDescription(views, R.id.home_crest, homeTeamName);
        }

        // match time
        views.setTextViewText(R.id.date_textview,
                data.getString(data.getColumnIndex(ScoreContract.ScoreColumn.TIME_COL)));
        views.setTextColor(R.id.date_textview, ContextCompat.getColor(mContext, R.color.secondary_text));

        // match score
        views.setTextViewText(R.id.score_textview, Utilities.getScores(
                data.getInt(data.getColumnIndex(ScoreContract.ScoreColumn.HOME_GOALS_COL)),
                data.getInt(data.getColumnIndex(ScoreContract.ScoreColumn.AWAY_GOALS_COL))));
        views.setTextColor(R.id.score_textview, ContextCompat.getColor(mContext, R.color.secondary_text));



        // away team name
        String awayTeamName = data.getString(data.getColumnIndex(ScoreContract.ScoreColumn.AWAY_COL));
        views.setTextViewText(R.id.away_name, awayTeamName);
        views.setTextColor(R.id.away_name, ContextCompat.getColor(mContext, R.color.secondary_text));

        views.setContentDescription(R.id.home_crest,homeTeamName);
        views.setContentDescription(R.id.away_crest,awayTeamName);

        // set content description on away team logo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            setRemoteContentDescription(views, R.id.away_crest, awayTeamName);
        }

        // fill the intet template we received from the provider and add the listview position as
        // extra and launch it onclick
        final Intent fillInIntent = new Intent();
        final Bundle extras = new Bundle();
        extras.putString(LatestFixtureWidgetProvider.SCORES_DATE,
                data.getString(data.getColumnIndex(ScoreContract.ScoreColumn.DATE_COL)));
        extras.putInt(TodaysFixturesWidgetProvider.SCORES_MATCH_ID,
                data.getInt(data.getColumnIndex(ScoreContract.ScoreColumn.MATCH_ID)));
        fillInIntent.putExtras(extras);
        views.setOnClickFillInIntent(R.id.widget_todays_list_item, fillInIntent);

        return views;
    }

    /**
     * Update the data when changed
     */
    @Override
    public void onDataSetChanged() {
        if (data != null) {
            data.close();
        }

        // clear the calling identity
        final long identityToken = Binder.clearCallingIdentity();

        // set the date for today
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // load the fixtures of today
        data = mContext.getContentResolver().query(
                ScoreContract.ScoreColumn.buildScoreWithDate(),
                null,
                null,
                new String[] { simpleDateFormat.format(date) },
                ScoreContract.ScoreColumn.TIME_COL +" ASC, "+ ScoreContract.ScoreColumn.HOME_COL +" ASC");

        // and restore the identity again
        Binder.restoreCallingIdentity(identityToken);
    }

    @Override
    public RemoteViews getLoadingView() {
        return new RemoteViews(mContext.getPackageName(), R.layout.scores_list_item);
    }

    @Override
    public long getItemId(int position) {
        if (data.moveToPosition(position))
            return data.getLong(0);
        return position;
    }

    @Override
    public int getCount() {
        return data == null ? 0 : data.getCount();
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
        if (data != null) {
            data.close();
            data = null;
        }
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
