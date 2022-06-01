package com.example.seesaw.crossword.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public enum Direction {
    UP,
    RIGHT; //같은 글자에서 시작하면 안됨.

    public static ArrayList<Direction> getRndIteration() {
        ArrayList<Direction> output = new ArrayList<>(Arrays.asList(UP, RIGHT));
        Collections.shuffle(output);
        return output;
    }
}
