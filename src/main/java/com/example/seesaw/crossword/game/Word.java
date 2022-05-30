package com.example.seesaw.crossword.game;

import java.util.ArrayList;
import java.util.Objects;

public class Word {
    // 게임판의 단어 첫좌표
    private String position;

    // 게임판 단어 가로세로 방향
    private Direction direction;

    // 단어 이름
    private String name; //static 으로 바꿔

    // 단어 힌트 contents
    private String clue;

    //방향정하기 순서
    private ArrayList<Direction> directionIteration;

    // 단어 할당 번호 (사용안함)
    private int num;

    // 새 단어를 넣는다.
    public Word(String name) {
        this.name = name;
        directionIteration = Direction.getRndIteration(); // 방향정하기
    }

    // getter
    public ArrayList<Direction> getDirectionIteration() {
        return directionIteration;
    }

    public String getPosition() { return position; }

    public String getName() { return name; }

    public Direction getDirection() { return direction; }

    public String getClue() {
        if (clue == null) {
            mkClue();
        }
        return clue;
    }

    public int getNum() {
        return num;
    }

    // setter
    public void setPosition(String position) {
        this.position = position;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void removeDirectionFromIteration(Direction toRemove) {
        directionIteration.remove(toRemove);
    }

    // 단어 첫 글자의 좌표 리턴 (첫좌표)
    public int getX() {
        Objects.requireNonNull(position);
        return Utils.getX(position);
    }

    public int getY() {
        Objects.requireNonNull(position);
        return Utils.getY(position);
    }

    public void setNum(int num) {
        this.num = num;
    }

    // 단어의 x 축 흔적을 반환
    public int[] getXTrail() {
        Objects.requireNonNull(position);
        Objects.requireNonNull(direction);
        int[] output = new int[name.length()];
        int currentX = getX();
        for (int i=0; i<name.length(); i++) {
            if (direction == Direction.RIGHT) {
                output[i] = currentX;
                currentX++;
            } else {
                output[i] = currentX;
            }
        }
        return output;
    }

    // 단어의 y 축 흔적을 반환
    public int[] getYTrail() {
        Objects.requireNonNull(position);
        Objects.requireNonNull(direction);
        int[] output = new int[name.length()];
        int currentY = getY();
        for (int i=0; i<name.length(); i++) {
            if (direction == Direction.UP) {
                output[i] = currentY;
                currentY++;
            } else {
                output[i] = currentY;
            }
        }
        return output;
    }

    // 단어 힌트 만들기
    private void mkClue() {
        clue = DefinitionGetter.getDefinition(name);
    }

    // 재정의
    @Override
    public String toString() {
        return name;
    }
}