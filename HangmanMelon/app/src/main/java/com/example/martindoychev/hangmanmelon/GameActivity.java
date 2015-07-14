package com.example.martindoychev.hangmanmelon;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasDocument;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasQuery;
import com.baasbox.android.BaasResult;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private static final int MAX_ERROR_COUNT = 5;

    private List<BaasDocument> wordsCollection; //words from the db
    private String gameWord; //the random word, selected for the current game
    private List<Character> unusedAlphabet, usedAlphabet; //the used and unused letters from the alphabet
    private int errorCount = 0; //wrong guesses made by the user

    private ProgressDialog loadingDialog;
    private BaasDocument fbUser; //user details

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        fbUser = getIntent().getParcelableExtra("fbUser");

        final EditText letterInput = (EditText) findViewById(R.id.letterInput);
        final EditText guessInput = (EditText) findViewById(R.id.guessInput);

        Button letterButton = (Button) findViewById(R.id.letterButton);
        Button guessButton = (Button) findViewById(R.id.guessButton);


        letterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processLetter(letterInput.getText().toString());
                letterInput.setText("");
            }
        });

        guessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processGuess(guessInput.getText().toString());
                guessInput.setText("");
            }
        });

        //uncomment the line below to populate the db with the collection words and the documents for it
        //populateDB();
        prepareGame();
    }

    private void prepareGame() {
        loadingDialog = ProgressDialog.show(GameActivity.this, "", "Loading. Please wait...", true);

        //if the collection is not yet fetched, fetch it; otherwise - proceed with game initiation
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

            //generate the string, viewed by the user
            String word = generateVisibleGameWord();
            TextView gameWordView = (TextView) findViewById(R.id.gameWordView);
            gameWordView.setText(word);

            TextView wordDescriptionView = (TextView) findViewById(R.id.wordDescriptionView);
            wordDescriptionView.setText(wordsCollection.get(randomWordId).getString("description"));

            Log.d("prepareGame", "dummy");
        }
        loadingDialog.dismiss();
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
        visible.deleteCharAt(visible.length()-1);
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
        inputStr = inputStr.toUpperCase();
        char inputChar = inputStr.charAt(0);
        if (inputStr.length() > 1) {
            //TODO: prepare error message, try again - 1 char
        } else if (!unusedAlphabet.contains(inputChar)) {
            //TODO: prepare error message, try again - already used
        } else {
            int index = gameWord.indexOf(inputChar, 1);
            if (index > -1 && index < gameWord.length()-1) {
                unusedAlphabet.remove(unusedAlphabet.indexOf(inputChar));
                usedAlphabet.add(inputChar);
                String word = generateVisibleGameWord();
                TextView gameWordView = (TextView) findViewById(R.id.gameWordView);
                gameWordView.setText(word);
                if (!word.contains(" _ ")) {
                    endGame(true);
                }
            } else {
                ++errorCount;
                ImageView errorsImageView = (ImageView) findViewById(R.id.errorsImageView);
                String idStr = "errors" + errorCount;
                int id = getResources().getIdentifier("com.example.martindoychev.hangmanmelon:drawable/" + idStr, null, null);
                errorsImageView.setImageResource(id);
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

    //when the user attempts a complete guess, process it here
    private void processGuess(String inputStr) {
        loadingDialog = ProgressDialog.show(GameActivity.this, "", "Loading. Please wait...", true);
        if (gameWord.equals(inputStr)) {
            endGame(true);
        } else {
            endGame(false);
        }
        loadingDialog.dismiss();
    }

    //when the game is over, process the result here
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
