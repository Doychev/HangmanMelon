package com.example.martindoychev.hangmanmelon;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasDocument;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasQuery;
import com.baasbox.android.BaasResult;
import com.baasbox.android.SaveMode;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private static final int MAX_ERROR_COUNT = 5;

    private Tracker mTracker;

    private List<BaasDocument> wordsCollection; //words from the db
    private String gameWord; //the random word, selected for the current game
    private List<Character> unusedAlphabet; //the used and unused letters from the alphabet
    private int errorCount = 0; //wrong guesses made by the user
    private int currentLetterGuesses = 0; //letter guesses made in the current game
    private boolean wordGuessMade = false; //check if the player tried to guess the word

    private ProgressDialog loadingDialog;
    private BaasDocument fbUser; //user details

    private CallbackManager callbackManager;
    private ShareDialog shareDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        mTracker = analytics.newTracker(R.xml.global_tracker);

        setContentView(R.layout.activity_game);

        loadingDialog = ProgressDialog.show(GameActivity.this, "", "Loading. Please wait...", true);

        fbUser = getIntent().getParcelableExtra("fbUser");

        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                openDashboardActivity();
            }

            @Override
            public void onCancel() {
                openDashboardActivity();
            }

            @Override
            public void onError(FacebookException e) {
                //TODO: handle error
            }
        });

        final EditText inputField = (EditText) findViewById(R.id.inputField);
        final InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);


        Button letterButton = (Button) findViewById(R.id.letterButton);
        letterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                processLetter(inputField.getText().toString());
                inputField.setText("");
            }
        });

        Button guessButton = (Button) findViewById(R.id.guessButton);
        guessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                processGuess(inputField.getText().toString());
                inputField.setText("");
            }
        });

        Button quitButton = (Button) findViewById(R.id.quitButton);
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                quitGame();
            }
        });

        //uncomment the line below to populate the db with the collection words and the documents for it
        //populateDB();
        prepareGame();
    }

    @Override
    protected void onResume() {
        super.onResume();

        String name = "GameActivity";
        Log.i("analytics", "Setting screen name: " + name);
        mTracker.setScreenName(name);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void prepareGame() {
        //if the collection is not yet fetched, fetch it; otherwise - proceed with game initiation
        if (wordsCollection == null || wordsCollection.isEmpty()) {
            populateRandomCategory();
        } else {
            char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray(); //abcdefghijklmnopqrstuvwxyz
            unusedAlphabet = new ArrayList<Character>();
            for (char c : alphabet) {
                unusedAlphabet.add(c);
            }

            Random random = new Random();
            int randomWordId = Math.abs(random.nextInt(wordsCollection.size()));
            gameWord = wordsCollection.get(randomWordId).getString("word");
            gameWord = gameWord.toUpperCase();

            //generate the string, viewed by the user
            String word = generateVisibleGameWord();
            TextView gameWordView = (TextView) findViewById(R.id.gameWordView);
            gameWordView.setText(word);

            TextView wordDescriptionView = (TextView) findViewById(R.id.wordDescriptionView);
            wordDescriptionView.setText(wordsCollection.get(randomWordId).getString("description"));

            Log.d("prepareGame", "dummy");
            loadingDialog.dismiss();
        }
    }

    //if the letter is not yet guessed - replace it with " _ "
    //if the random word consists of more than one real word - show the first and last char for all words
    private String generateVisibleGameWord() {
        StringBuilder visible = new StringBuilder();
        String[] gameWords = gameWord.split(" ");
        for (String currentWord : gameWords) {
            visible.append(currentWord.charAt(0));
            for (int i = 1; i < currentWord.length()-1; i++) {
                char current = currentWord.charAt(i);
                if (current >= 'A' && current <= 'Z' && unusedAlphabet.contains(current)) {
                    visible.append(" _ ");
                } else {
                    visible.append(current);
                }
            }
            visible.append(currentWord.charAt(currentWord.length() - 1) + "  ");
        }
        visible.deleteCharAt(visible.length() - 1);
        return visible.toString();
    }

    //pick a random category from the Category enum and fetch all words from that category
    private void populateRandomCategory() {

        Category randomCategory = Category.getRandomCategory();
        String randomCategoryName = randomCategory.name();

        BaasQuery.Criteria filter = BaasQuery.builder().pagination(0, 100)
                .orderBy("wid desc")
                .where("category = ?")      //.and("wid = ?")
                .whereParams(randomCategoryName)
                .criteria();

        BaasDocument.fetchAll("words", filter, getWordsHandler);
    }

    private final BaasHandler<List<BaasDocument>> getWordsHandler = new BaasHandler<List<BaasDocument>>() {
        @Override
        public void handle(BaasResult<List<BaasDocument>> res) {
            //populate the words and continue with game initiation
            if (res.isSuccess()) {
                Log.d("Baasboxdocs", "success");
                wordsCollection = res.value();
                prepareGame();
            } else {
                //TODO: check if proper error handling is required here
                Log.e("Baasboxdocs", "Error", res.error());
            }
        }
    };

    //when the user tries to guess a letter, process it here;
    private void processLetter(String inputStr) {
        loadingDialog = ProgressDialog.show(GameActivity.this, "", "Loading. Please wait...", true);
        if (inputStr.isEmpty()) {
            showSnackbar("Invalid input:", "EMPTY", Color.RED);
            loadingDialog.dismiss();
            return;
        }
        inputStr = inputStr.toUpperCase();
        char inputChar = inputStr.charAt(0);
        if (inputStr.length() > 1) {
            showSnackbar("Invalid input:", "MULTIPLE CHARS", Color.RED);
            loadingDialog.dismiss();
        } else if (!unusedAlphabet.contains(inputChar)) {
            showSnackbar("Invalid input:", "ALREADY USED", Color.RED);
            loadingDialog.dismiss();
        } else {
            currentLetterGuesses++;
            unusedAlphabet.remove(unusedAlphabet.indexOf(inputChar));
            int index = gameWord.indexOf(inputChar, 1);
            if (index > -1 && index < gameWord.length()-1) {
                String word = generateVisibleGameWord();
                TextView gameWordView = (TextView) findViewById(R.id.gameWordView);
                gameWordView.setText(word);
                showSnackbar("Your guess is:", "CORRECT", Color.GREEN);
                loadingDialog.dismiss();
                if (!word.contains(" _ ")) {
                    endGame(true);
                }
            } else {
                ++errorCount;
                ImageView errorsImageView = (ImageView) findViewById(R.id.errorsImageView);
                String idStr = "errors" + errorCount;
                int id = getResources().getIdentifier("com.example.martindoychev.hangmanmelon:drawable/" + idStr, null, null);
                errorsImageView.setImageResource(id);
                showSnackbar("Your guess is wrong!", (MAX_ERROR_COUNT - errorCount) + " ATTEMPTS LEFT", Color.RED);
                loadingDialog.dismiss();
                if (errorCount >= MAX_ERROR_COUNT) {
                    endGame(false);
                }
            }
        }
    }

    private void showSnackbar(String message, String action, int color) {
        Snackbar.make(findViewById(R.id.wordDescriptionView), message, Snackbar.LENGTH_SHORT).
                setAction(action, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //do nothing
                    }
                }).
                setActionTextColor(color)
                .show();
    }

    //when the user attempts a complete guess, process it here
    private void processGuess(String inputStr) {
        loadingDialog = ProgressDialog.show(GameActivity.this, "", "Loading. Please wait...", true);
        if (inputStr.length() < 5 ) {
            loadingDialog.dismiss();
            showSnackbar("Invalid input:", "EMPTY", Color.RED);
        } else {
            inputStr = inputStr.toUpperCase();
            wordGuessMade = true;
            loadingDialog.dismiss();
            if (gameWord.equals(inputStr)) {
                endGame(true);
            } else {
                endGame(false);
            }
        }
    }

    //when the game is over, process the result here
    private void endGame(boolean isWinning) {
        loadingDialog = ProgressDialog.show(GameActivity.this, "", "Loading. Please wait...", true);
        int totalGames = fbUser.getInt("totalGames") + 1;
        int wonGames = fbUser.getInt("wonGames") + (isWinning ? 1 : 0);
        int totalLetterGuesses = fbUser.getInt("letterGuesses") + currentLetterGuesses;
        int wordGuesses = fbUser.getInt("wordGuesses") + (wordGuessMade ? 1 : 0);
        fbUser.put("totalGames", totalGames).
                put("wonGames", wonGames).
                put("letterGuesses", totalLetterGuesses).
                put("wordGuesses", wordGuesses);

        fbUser.save(SaveMode.IGNORE_VERSION, new BaasHandler<BaasDocument>() {
            @Override
            public void handle(BaasResult<BaasDocument> res) {
                if (res.isSuccess()) {
                    Log.d("LOG", "Document saved " + res.value().getId());
                } else {
                    Log.e("LOG", "Error", res.error());
                }
            }
        });
        loadingDialog.dismiss();
        showEndGameDialog(isWinning);
    }

    private void showEndGameDialog(boolean isWinning) {
        AlertDialog alertDialog = new AlertDialog.Builder(GameActivity.this).create();
        alertDialog.setCancelable(false);
        alertDialog.setTitle("Game over");
        String message;
        final String shareTitle;
        if (isWinning) {
            message = "You won! Well done!";
            shareTitle = "I guessed the word " + gameWord + "!";
        } else {
            message = "You lost! The word was: " + gameWord;
            shareTitle = "I couldn't guess the word " + gameWord + "!";
        }
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "PLAY AGAIN",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        recreate();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "SHARE",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (ShareDialog.canShow(ShareLinkContent.class)) {
                            ShareLinkContent linkContent = new ShareLinkContent.Builder()
                                    .setContentTitle(shareTitle)
                                    .setContentDescription(
                                            "Join me and play Hangman!")
                                    .setContentUrl(Uri.parse("http://developers.facebook.com/android"))
                                    .build();

                            shareDialog.show(linkContent);
                        }
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "STATISTICS",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        openDashboardActivity();
                    }
                });
        alertDialog.show();
    }

    private void quitGame() {
        AlertDialog alertDialog = new AlertDialog.Builder(GameActivity.this).create();
        alertDialog.setTitle("Quit game");
        alertDialog.setMessage("Are you sure you want to quit? You will lose the current game!");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        endGame(false);
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //do nothing
                    }
                });
        alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialog.show();
    }

    private void openDashboardActivity() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("fbUser", fbUser);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        quitGame();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game, menu);
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

    //a short script to repopulate the DB when necesssary (ex. after a db reset)
    private void populateDB() {
        List<BaasDocument> words = new ArrayList<BaasDocument>();

        BaasDocument word = new BaasDocument("words");
        word.put("category", "Cities");
        word.put("word", "Sofia");
        word.put("description", "City in Bulgaria");
        word.put("wid", "0");
        words.add(word);

        BaasDocument word1 = new BaasDocument("words");
        word1.put("category", "Cities");
        word1.put("word", "Milan");
        word1.put("description", "City in Italy");
        word1.put("wid", "1");
        words.add(word1);

        BaasDocument word2 = new BaasDocument("words");
        word2.put("category", "Cities");
        word2.put("word", "L'Aquila");
        word2.put("description", "City in Italy");
        word2.put("wid", "2");
        words.add(word2);

        BaasDocument word3 = new BaasDocument("words");
        word3.put("category", "Animals");
        word3.put("word", "Tiger");
        word3.put("description", "Animal, Cats family");
        word3.put("wid", "0");
        words.add(word3);

        BaasDocument word4 = new BaasDocument("words");
        word4.put("category", "Animals");
        word4.put("word", "Snake");
        word4.put("description", "Animal, Reptile");
        word4.put("wid", "1");
        words.add(word4);

        BaasDocument word5 = new BaasDocument("words");
        word5.put("category", "Animals");
        word5.put("word", "Big Eagle");
        word5.put("description", "Animal, Bird");
        word5.put("wid", "2");
        words.add(word5);

        BaasBox client = BaasBox.getDefault();
        String collectionName = "words";
        client.rest(HttpRequest.POST, "admin/collection/" + collectionName, null, true,
                new BaasHandler<JsonObject>() {
                    @Override
                    public void handle(BaasResult<JsonObject> res) {
                        if (res.isSuccess()) {
                            Log.d("LOG", "Collection created");
                        } else {
                            Log.e("LOG", "Error", res.error());
                        }
                    }
                });

        saveWords(words, 0);
    }

    private void saveWords(final List<BaasDocument> words, final int index) {
        BaasHandler<BaasDocument> saveWordsHandler =
                new BaasHandler<BaasDocument>() {
                    @Override
                    public void handle(BaasResult<BaasDocument> doc) {
                        if (doc.isSuccess()) {
                            saveWords(words, index + 1);
                            Log.d("Baasboxdocs", "success");
                        } else {
                            Log.d("Baasboxdocs", "fail");
                        }
                    }
                };

        if (index < words.size()) {
            words.get(index).save(saveWordsHandler);
        }
    }
}
