package com.example.martindoychev.hangmanmelon;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by Martin Doychev on 09/07/2015.
 */
public enum Category {

    Cities (1),
    Animals (2);

    private final int catId;
    private static final List<Category> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
    private static final int SIZE = VALUES.size();
    private static final Random RANDOM = new Random();


    Category(int id) {
        this.catId = id;
    }

    public int getCatId() {
        return catId;
    }

    public static Category getRandomCategory()  {
        return VALUES.get(RANDOM.nextInt(SIZE));
    }

}
