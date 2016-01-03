package barqsoft.footballscores.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import barqsoft.footballscores.BuildConfig;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilities;
import barqsoft.footballscores.data.ScoreContract;

/**
 * Created by yehya khaled on 3/2/2015.
 */
public class ScoreFetchService extends IntentService {
    public static final String LOG_TAG = "myFetchService";
    private Vector<ContentValues> mTeamsVector;

    public ScoreFetchService() {
        super("ScoreFetchService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        mTeamsVector = new Vector<ContentValues>();
        //requestAllTeams();
        getFixtureData("n3");
        getFixtureData("p2");

        return;
    }

    private static int[] soccerseasons = {394, 396, 397,398, 401, 402, 403, 404, 405};

    private void requestAllTeams() {

        for (final int soccerseason : soccerseasons) {
            try {
                URL queryTeamsUrl = new URL(Uri.parse(getString(R.string.soccerseason_url))
                        .buildUpon()
                        .appendPath(Integer.toString(soccerseason))
                        .appendPath("teams")
                        .build()
                        .toString());

                String allTeams = sendRequest(queryTeamsUrl);

                if (allTeams != null) {
                    processAllTeamsData(allTeams);
                } else {
                    Log.d(LOG_TAG, "no Teams Available for :-" + ": " + soccerseason);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception here in requestAllTeams: " + e.getMessage());
            }
        }
    }


    private void processAllTeamsData(String teamsString) {
        try {
            JSONArray teams = new JSONObject(teamsString).getJSONArray("teams");

            if (teams.length() > 0) {
                for (int i = 0; i < teams.length(); i++) {

                    JSONObject team = teams.getJSONObject(i);

                    String teamId = team.getJSONObject("_links").getJSONObject("self").getString("href");

                    teamId = teamId.replace(getString(R.string.teams_url), "");

                    String crestUrl = team.getString("crestUrl");

                    //thanks to marcolangebeeke in discussion forum this became easy

                    if (crestUrl != null && crestUrl.endsWith(".svg")) {
                        String svg = crestUrl;
                        String filename = svg.substring(svg.lastIndexOf("/") + 1);
                        int wikipediaPathEndPos = svg.indexOf("/wikipedia/") + 11;
                        String afterWikipediaPath = svg.substring(wikipediaPathEndPos);
                        int thumbInsertPos = wikipediaPathEndPos + afterWikipediaPath.indexOf("/") + 1;
                        String afterLanguageCodePath = svg.substring(thumbInsertPos);
                        crestUrl = svg.substring(0, thumbInsertPos);
                        crestUrl += "thumb/" + afterLanguageCodePath;
                        crestUrl += "/200px-" + filename + ".png";
                    }

                    // create contentvalues object containing the team details
                    ContentValues teamValues = new ContentValues();
                    teamValues.put(ScoreContract.ScoreColumn.HOME_ID_COL, Integer.parseInt(teamId));
                    teamValues.put(ScoreContract.ScoreColumn.HOME_LOGO_COL, crestUrl);

                    // add team to the vector
                    mTeamsVector.add(teamValues);
                }
            } else {
                Log.e(LOG_TAG, "No teams playing in the season");
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }


    private String sendRequest(URL apiUrl) {

        String apiResult = null;

        if (Utilities.isNetworkAvailable(getApplicationContext())) {

            HttpURLConnection m_connection = null;
            BufferedReader apiReader = null;

            if (apiUrl != null) {
                try {


                    m_connection = (HttpURLConnection) apiUrl.openConnection();
                    m_connection.setRequestMethod("GET");
                    m_connection.addRequestProperty("X-Auth-Token", BuildConfig.THE_API_KEY);
                    m_connection.connect();

                    InputStream inputStream = m_connection.getInputStream();
                    if (inputStream == null) {
                        return null;
                    }

                    apiReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder buffer = new StringBuilder();
                    String line;
                    while ((line = apiReader.readLine()) != null) {
                        buffer.append(line);
                    }

                    if (buffer.length() > 0) {
                        apiResult = buffer.toString();
                    } else {
                        return null;
                    }

                } catch (Exception e) {
                    Log.e(LOG_TAG, "Exception here in sendRequest: " + e.getMessage());
                } finally {

                    // disconnect the api connection and close the reader
                    if (m_connection != null) {
                        m_connection.disconnect();
                    }
                    if (apiReader != null) {
                        try {
                            apiReader.close();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Error Closing Stream");
                        }
                    }
                }
            }
        } else {
            return null;
        }

        return apiResult;
    }


    private void getFixtureData(String timeFrame) {
        //Creating fetch URL
        final String BASE_URL = getString(R.string.fixtures_url); //Base URL
        final String QUERY_TIME_FRAME = "timeFrame"; //Time Frame parameter to determine days
        //final String QUERY_MATCH_DAY = "matchday";

        URL fetch_build = null;
        try {
            fetch_build = new URL(Uri.parse(BASE_URL).buildUpon().
                    appendQueryParameter(QUERY_TIME_FRAME, timeFrame).build().toString());


            String result = sendRequest(fetch_build);
            int count = 0;
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    count = jsonObject.getInt("count");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (result != null && count != 0) {
                getApplicationContext().getContentResolver().bulkInsert(
                        ScoreContract.BASE_CONTENT_URI,
                        processFixtures(result));
            } else {
                Log.d(LOG_TAG, getString(R.string.no_teams));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private ContentValues[] processFixtures(String fixturesString) {

        ContentValues[] fixturesArray = null;

        // indicator for real or dummy data
        boolean isReal = true;

        // json element names
        final String FIXTURES = "fixtures";
        final String LINKS = "_links";
        final String SOCCER_SEASON = "soccerseason";
        final String SELF = "self";
        final String MATCH_DATE = "date";
        final String HOME_TEAM = "homeTeamName";
        final String AWAY_TEAM = "awayTeamName";
        final String RESULT = "result";
        final String HOME_GOALS = "goalsHomeTeam";
        final String AWAY_GOALS = "goalsAwayTeam";
        final String MATCH_DAY = "matchday";
        final String HOME_TEAM_ID = "homeTeam";
        final String AWAY_TEAM_ID = "awayTeam";

        final String SEASON_LINK = getString(R.string.soccerseason_url);
        final String MATCH_LINK = getString(R.string.fixtures_url) ;
        final String TEAM_LINK = getString(R.string.teams_url);

        // get matches and convert them to an array
        try {
            JSONArray fixtures = new JSONObject(fixturesString).getJSONArray(FIXTURES);

            // load dummy data if no matches found
            if (fixtures.length() == 0) {
                fixturesString = getString(R.string.dummy_data);
                fixtures = new JSONObject(fixturesString).getJSONArray(FIXTURES);
                isReal = false;
            }

            // create contentvalues vector with length of amount of matches
            Vector<ContentValues> fixturesVector = new Vector <ContentValues> (fixtures.length());

            for(int i = 0;i < fixtures.length(); i++) {

                // get the match
                JSONObject fixture = fixtures.getJSONObject(i);

                // extract league from href in links.soccerseason
                String leagueValue = fixture.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON).getString("href");
                leagueValue = leagueValue.replace(SEASON_LINK, "");
                int league = Integer.parseInt(leagueValue);

                // only include matches from selected leagues
                if (Utilities.contains(soccerseasons, league)) {

                    // extract the match id from href in links.self
                    String matchId = fixture.getJSONObject(LINKS).getJSONObject(SELF).getString("href");
                    matchId = matchId.replace(MATCH_LINK, "");

                    // extract the home team id from href in links.homeTeam
                    String homeTeamIdString = fixture.getJSONObject(LINKS).getJSONObject(HOME_TEAM_ID).getString("href");
                    homeTeamIdString = homeTeamIdString.replace(TEAM_LINK, "");
                    int homeTeamId = Integer.parseInt(homeTeamIdString);

                    // extract the away team id from href in links.awayTeam
                    String awayTeamIdString = fixture.getJSONObject(LINKS).getJSONObject(AWAY_TEAM_ID).getString("href");
                    awayTeamIdString = awayTeamIdString.replace(TEAM_LINK, "");
                    int awayTeamId = Integer.parseInt(awayTeamIdString);

                    // increment the match id of the dummy data (makes it unique)
                    if (!isReal) {
                        matchId = matchId + Integer.toString(i);
                    }

                    // get the date and time from match date field
                    String date = fixture.getString(MATCH_DATE);
                    String time = date.substring(date.indexOf("T") + 1, date.indexOf("Z"));
                    date = date.substring(0, date.indexOf("T"));
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss", Locale.US);
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                    // convert date and time to local datetime and extract date and time again
                    try {
                        Date parsedDate = simpleDateFormat.parse(date + time);
                        SimpleDateFormat newDate = new SimpleDateFormat("yyyy-MM-dd:HH:mm", Locale.US);
                        newDate.setTimeZone(TimeZone.getDefault());
                        date = newDate.format(parsedDate);
                        time = date.substring(date.indexOf(":") + 1);
                        date = date.substring(0, date.indexOf(":"));

                        // change the dummy data's date to match current date range
                        if(!isReal) {
                            Date dummyDate = new Date(System.currentTimeMillis() + ((i-2)*86400000));
                            SimpleDateFormat dummyDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                            date = dummyDateFormat.format(dummyDate);
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }

                    // create contentvalues object containing the match details
                    ContentValues fixtureValues = new ContentValues();
                    fixtureValues.put(ScoreContract.ScoreColumn.MATCH_ID, matchId);
                    fixtureValues.put(ScoreContract.ScoreColumn.DATE_COL, date);
                    fixtureValues.put(ScoreContract.ScoreColumn.TIME_COL, time);
                    fixtureValues.put(ScoreContract.ScoreColumn.HOME_COL, fixture.getString(HOME_TEAM));
                    fixtureValues.put(ScoreContract.ScoreColumn.HOME_ID_COL, homeTeamId);
                    fixtureValues.put(ScoreContract.ScoreColumn.HOME_LOGO_COL, getTeamLogoById(homeTeamId));
                    fixtureValues.put(ScoreContract.ScoreColumn.HOME_GOALS_COL, fixture.getJSONObject(RESULT).getString(HOME_GOALS));
                    fixtureValues.put(ScoreContract.ScoreColumn.AWAY_COL, fixture.getString(AWAY_TEAM));
                    fixtureValues.put(ScoreContract.ScoreColumn.AWAY_ID_COL, awayTeamId);
                    fixtureValues.put(ScoreContract.ScoreColumn.AWAY_LOGO_COL, getTeamLogoById(awayTeamId));
                    fixtureValues.put(ScoreContract.ScoreColumn.AWAY_GOALS_COL, fixture.getJSONObject(RESULT).getString(AWAY_GOALS));
                    fixtureValues.put(ScoreContract.ScoreColumn.LEAGUE_COL, league);
                    fixtureValues.put(ScoreContract.ScoreColumn.MATCH_DAY, fixture.getString(MATCH_DAY));

                    // add match to the vector
                    fixturesVector.add(fixtureValues);
                }
            }

            // convert vector to array
            fixturesArray = new ContentValues[fixturesVector.size()];
            fixturesVector.toArray(fixturesArray);

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Exception here in processFixtures: " + e.getMessage());
        }

        return fixturesArray;
    }

    private String getTeamLogoById(int teamId) {

        // loop through the teams and return logo url when team id found
        for (ContentValues team: mTeamsVector) {
            if (team.getAsInteger(ScoreContract.ScoreColumn.HOME_ID_COL).equals(teamId)) {
                return team.getAsString(ScoreContract.ScoreColumn.HOME_LOGO_COL);
            }
        }

        return "";
    }
}

