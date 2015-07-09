package com.example.martindoychev.hangmanmelon;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.baasbox.android.BaasDocument;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasQuery;
import com.baasbox.android.BaasResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private static final int MAX_ERROR_COUNT = 5;

    private List<BaasDocument> wordsCollection;
    private String gameWord;
    private List<Character> unusedAlphabet, usedAlphabet;
    private int errorCount = 0;

    private ProgressDialog loadingDialog;

    private final BaasHandler<List<BaasDocument>> getWordsHandler = new BaasHandler<List<BaasDocument>>() {
        @Override
        public void handle(BaasResult<List<BaasDocument>> res) {
            if (res.isSuccess()) {
                Log.d("Baasboxdocs", "success");
                wordsCollection = res.value();
                prepareGame();
//                for (BaasDocument doc : res.value()) {
//                    Log.d("Baasboxdocs", "Doc: " + doc);
//                }
            } else {
                Log.e("Baasboxdocs", "Error", res.error());
            }
        }
    };

//        BaasDocument note = new BaasDocument("words");
//        note.put("category","Cities");
//        note.put("word","Sofia");
//        note.save(saveWordsHandler);
       private final BaasHandler<BaasDocument> saveWordsHandler =
            new BaasHandler<BaasDocument>() {
                @Override
                public void handle(BaasResult<BaasDocument> doc) {
                    if (doc.isSuccess()) {
                        Log.d("Baasboxdocs", "success");
                    } else {
                        Log.d("Baasboxdocs", "fail");
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        prepareGame();
    }

    private void prepareGame() {
        loadingDialog = ProgressDialog.show(GameActivity.this, "", "Loading. Please wait...", true);
        if (wordsCollection == null || wordsCollection.isEmpty()) {
            populateRandomCategory();
        } else {
            char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray(); //abcdefghijklmnopqrstuvwxyz
            unusedAlphabet = new ArrayList<Character>();
            for (char c : alphabet) {
                unusedAlphabet.add(c);
            }
            usedAlphabet = new ArrayList<Character>();

            Random random = new Random();
            int randomWordId = Math.abs(random.nextInt(wordsCollection.size()));
            gameWord = wordsCollection.get(randomWordId).getString("word");
            gameWord = gameWord.toUpperCase();
//            gameWord = "SOFIA";

            String word = generateVisibleGameWord();

//            processLetter("I");
//            word = generateVisibleGameWord();

             Log.d("prepareGame", "dummy");
        }
        loadingDialog.dismiss();
    }

    private String generateVisibleGameWord() {
        StringBuilder visible = new StringBuilder();
        visible.append(gameWord.charAt(0));
        for (int i = 1; i < gameWord.length()-1; i++) {
            char current = gameWord.charAt(i);
            if (current >= 'A' && current <= 'Z' && unusedAlphabet.contains(current)) {
                visible.append(" _ ");
            } else {
                visible.append(current);
            }
        }
        visible.append(gameWord.charAt(gameWord.length() - 1));
        return visible.toString();
    }

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

    private void processLetter(String inputStr) {
        loadingDialog = ProgressDialog.show(GameActivity.this, "", "Loading. Please wait...", true);
        char inputChar = inputStr.charAt(0);
        if (inputStr.length() > 1) {
            //TODO: prepare error message, try again - 1 char
        } else if (!unusedAlphabet.contains(inputChar)) {
            //TODO: prepare error message, try again - already used
        } else {
            int index = gameWord.indexOf(inputChar, 1);
            if (index > -1 && index < gameWord.length()-1) {
                unusedAlphabet.remove(unusedAlphabet.indexOf('I'));
                usedAlphabet.add(inputChar);
                String word = generateVisibleGameWord();
                if (!word.contains(" _ ")) {
                    endGame(true);
                }
            } else {
                ++errorCount;
                if (errorCount >= MAX_ERROR_COUNT) {
                    endGame(false);
                } else {
                    //TODO: warn player about error
                }
            }
        }
        loadingDialog.dismiss();
        //TODO: show error messages if any
    }

    private void processGuess(String inputStr) {
        loadingDialog = ProgressDialog.show(GameActivity.this, "", "Loading. Please wait...", true);
        if (gameWord.equals(inputStr)) {
            endGame(true);
        } else {
            endGame(false);
        }
        loadingDialog.dismiss();
    }

    private void endGame(boolean isWinning) {
        //TODO: end game
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
}
