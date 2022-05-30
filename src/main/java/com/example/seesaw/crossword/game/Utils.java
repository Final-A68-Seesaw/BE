package com.example.seesaw.crossword.game;

import java.util.Objects;

public class Utils {
    // 좌표 분할
    public static int getX(String position) {
        Objects.requireNonNull(position);
        return Integer.parseInt(position.split(" ")[0]);
    }

    public static int getY(String position) {
        Objects.requireNonNull(position);
        return Integer.parseInt(position.split(" ")[1]);
    }

    // 주어진 단어 길이의 xTrail 리턴
    public static int[] getXTrail(String initPos, Direction direction, int length) {
        int[] output = new int[length];
        int currentX = getX(initPos);
        for (int i=0; i<length; i++) {
            if (direction == Direction.RIGHT) {
                output[i] = currentX;
                currentX++;
            } else {
                output[i] = currentX;
            }
        }
        return output;
    }

    // 주어진 단어 길이의 yTrail 을 리턴
    public static int[] getYTrail(String initPos, Direction direction, int length) {
        int[] output = new int[length];
        int currentY = getY(initPos);
        for (int i=0; i<length; i++) {
            if (direction == Direction.UP) {
                output[i] = currentY;
                currentY++;
            } else {
                output[i] = currentY;
            }
        }
        return output;
    }
}