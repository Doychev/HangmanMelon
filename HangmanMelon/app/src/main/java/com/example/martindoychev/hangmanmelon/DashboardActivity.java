package com.example.martindoychev.hangmanmelon;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.baasbox.android.BaasDocument;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;


public class DashboardActivity extends AppCompatActivity {

    private Tracker mTracker;

    private ShareDialog shareDialog; //facebook share dialog

    private BaasDocument fbUser; //user details


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initiate the google analytics module
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        mTracker = analytics.newTracker(R.xml.global_tracker);

        setContentView(R.layout.activity_dashboard);

        fbUser = getIntent().getParcelableExtra("fbUser");

        shareDialog = new ShareDialog(this);

        //prepare all views

        Button startGameButton= (Button) findViewById(R.id.startGameButton);
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGameActivity();
            }
        });

        TextView welcomeUserText = (TextView) findViewById(R.id.welcomeUserText);
        welcomeUserText.setText("Welcome, " + fbUser.getString("username") + "!");

        int totalGames = fbUser.getInt("totalGames");
        int wins = fbUser.getInt("wonGames");
        int loses = totalGames - wins;

//        TextView statisticsTextView = (TextView) findViewById(R.id.statisticsTextView);
        TextView totalGamesTextView = (TextView) findViewById(R.id.totalGamesTextView);
        totalGamesTextView.setText("Total games: " + totalGames);
        TextView wonGamesTextView = (TextView) findViewById(R.id.wonGamesTextView);
        wonGamesTextView.setText("Won/lost games: " + wins + "/" + loses);
        TextView totalLetterGuessesTextView = (TextView) findViewById(R.id.totalLetterGuessesTextView);
        totalLetterGuessesTextView.setText("Total letter guesses: " + fbUser.getInt("letterGuesses"));
        TextView totalGuessedWordsTextView = (TextView) findViewById(R.id.totalGuessedWordsTextView);
        totalGuessedWordsTextView.setText("Total guessed words: " + fbUser.getInt("wordGuesses"));

        Button shareButton = (Button) findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ShareDialog.canShow(ShareLinkContent.class)) {
                    //facebook share
                    ShareLinkContent linkContent = new ShareLinkContent.Builder()
                            .setContentTitle("Play Hangman!")
                            .setContentDescription(
                                    "Join me in this interesting game!")
                            .setContentUrl(Uri.parse("http://developers.facebook.com/android"))
                            .build();

                    shareDialog.show(linkContent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //analytics preparation
        String name = "DashboardActivity";
        Log.i("analytics", "Setting screen name: " + name);
        mTracker.setScreenName(name);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    private void openGameActivity() {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("fbUser", fbUser);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
