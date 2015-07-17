package com.example.martindoychev.hangmanmelon;

import android.util.Log;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasDocument;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Doychev on 17/07/2015.
 */
public class DatabaseWordsGenerator {


    //a short script to repopulate the DB when necesssary (ex. after a db reset)
    public static void populateDB() {
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
        word5.put("word", "Bald Eagle");
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

    private static void saveWords(final List<BaasDocument> words, final int index) {
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
