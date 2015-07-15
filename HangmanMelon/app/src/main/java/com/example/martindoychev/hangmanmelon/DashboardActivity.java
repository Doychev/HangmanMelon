package com.example.martindoychev.hangmanmelon;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.baasbox.android.BaasDocument;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;


public class DashboardActivity extends AppCompatActivity {

    private ShareDialog shareDialog;

    private BaasDocument fbUser; //user details


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        fbUser = getIntent().getParcelableExtra("fbUser");

        shareDialog = new ShareDialog(this);

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

    private void openGameActivity() {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("fbUser", fbUser);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
