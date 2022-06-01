package com.example.seesaw.crossword.game;

import com.example.seesaw.crossword.dto.CrossWordResponseDto;
import com.example.seesaw.crossword.repository.CrosswordRepository;
import com.example.seesaw.crossword.repository.QuizNumRepository;

import java.util.*;

public class Board {
    // 차원 정하는 변수 dimension
    private int dim;
    // 게임판 단어 추가할 비어있는 List
    private ArrayList<Word> wordsOnBoard = new ArrayList<>();

    // 각 방향에 대한 추가된 단어 수
    private int upWords = 0;
    private int rightWords = 0;

    // 각 방향에 대한 최대 단어 수
    public final int maxPerDirection = 9;

    // 게임판에 배치된 모든 문자 좌표 <letter, list of coordinates>
    private HashMap<String, ArrayList<String>> letterPositions = new HashMap<>();

    // 완성된 게임판
    private String[][] fullBoard;

    // 현재 게임판에 있는 단어
    private String[][] partialBoard;

    //repository
    private final CrosswordRepository crosswordRepository;
    private final QuizNumRepository quizNumRepository;

    // 게임판 만들 생성자
    public Board(int dim, CrosswordRepository crosswordRepository, QuizNumRepository quizNumRepository) {
        this.dim = dim;
        this.fullBoard = new String[dim][dim];
        this.partialBoard = new String[dim][dim];
        this.crosswordRepository = crosswordRepository;
        this.quizNumRepository = quizNumRepository;
    }

    // 모든 단어가 배치될경우 true로 리턴.
    public boolean allWordsPlaced() {
        if (numOfPlacedWords() == (maxPerDirection * (Direction.values().length))) {
            return true;
        }
        if (numOfPlacedWords() > (maxPerDirection * (Direction.values().length))) {
            throw new IllegalArgumentException(numOfPlacedWords() + ">" + (maxPerDirection * (Direction.values().length)));
        }
        return false;
    }

    // 계산된 값들 response
    public List<CrossWordResponseDto> printClues() {
        List<CrossWordResponseDto> crossWordResponseDtos = new ArrayList<>();
        int id = 0;
        for (Word word : wordsOnBoard) {
            crossWordResponseDtos.add(new CrossWordResponseDto(id++, word, false));
        }
        return crossWordResponseDtos;
    }

    // 게임판에 단어를 추가한다, 성공하면 true를 리턴
    public boolean tryAddWord(String givenWord) {
        // 단어생성 (단어, 방향)
        Word word = new Word(givenWord);
        // 가로세로둘다 최대 9를 넘어가면 바로 false
        if ((rightWords>=maxPerDirection) && (upWords>=maxPerDirection)) {
            return false;
        }
        // 가로 단어가 9개가 넘어가면 가로 제거 -> up 방향으로
        if (rightWords >= maxPerDirection) {
            word.removeDirectionFromIteration(Direction.RIGHT);
        }
        // 세로단어가 9개가 넘어가면 세로 제거 -> right 방향으로
        if (upWords >= maxPerDirection) {
            word.removeDirectionFromIteration(Direction.UP);
        }
        // wordsOnBoard가 비어있으면(첫단어일 경우, (3,3)으로 고정, 방향은 랜덤으로 설정)
        if (wordsOnBoard.size() == 0) {
            try {
                addWord(word, (dim/3) + " " + (dim/3), word.getDirectionIteration().get(0));
            } catch (Exception e) { //첫단어가
                addWord(word, "0 " + dim/2, Direction.RIGHT);
            }
            return true;
        }
        String name = word.getName();
        String adjustedPosition;

        for (int c=0; c<name.length(); c++) {
            if (!letterPositions.containsKey(name.charAt(c) + "")) {
                continue;
            }
            // 가능한 위치를 찾아라
            for (String possiblePosition : letterPositions.get(name.charAt(c) + "")) { //3 4
                for (Direction direction : word.getDirectionIteration()) {
                    adjustedPosition = adjustPosition(possiblePosition, direction, c);  //2 4 시작점의 좌표를 구한다.
                    // 단어 간격이 잘 맞도록 단어를 추가할 수 있는지 확인.
                    if (spacedCanAdd(word, adjustedPosition, direction)) {
                        // 단어 추가 (단어 , 위치, 방향)
                        addWord(word, adjustedPosition, direction); //단어, 위치, 방향
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // 게임판에 단어 추가 메서드
    private void addWord(Word word, String position, Direction direction) {
        word.setPosition(position); // 위치 변경
        word.setDirection(direction); // 방향 변경
        updateLetterPositions(word); // 문자 위치 업데이트
        wordsOnBoard.add(word); // 전체 게임판에 들어갈 단어 추가

        if (direction == Direction.UP) {
            upWords++; // 방향이 세로면 세로 단어 올리기
        } else if (direction == Direction.RIGHT) {
            rightWords++; // 가로면 가로 단어 올리기
        } else {
            throw new IllegalArgumentException("Direction is invalid.");
        }

        int[] xTrail = word.getXTrail();
        int[] yTrail = word.getYTrail();
        String name = word.getName();
        assert ((name.length() == xTrail.length) && (xTrail.length == yTrail.length));

        // 게임판에 채우기
        for (int i=0; i<name.length(); i++) {
            fullBoard[xTrail[i]][yTrail[i]] = "" + name.charAt(i);
        }
    }

    // 지정된 위치에 지정된 문자를 배치하도록 위치 조정 메서드
    private String adjustPosition(String originalPos, Direction direction, int charOn) { // 3 4, right, 1
        int x = Utils.getX(originalPos);  //3
        int y = Utils.getY(originalPos);  //4
        if (direction == Direction.UP) {
            return x + " " + (y-charOn);
        }
        if (direction == Direction.RIGHT) {

            return (x-charOn) + " " + y;  //2 4
        }
        throw new IllegalArgumentException("현재 주어진 방향은 : " + direction);
    }

    // 새 단어로 문자 위치를 업데이트
    private void updateLetterPositions(Word word) {
        Objects.requireNonNull(word.getPosition());
        Objects.requireNonNull(word.getDirection());

        String name = word.getName().toUpperCase();
        ArrayList<String> currentValue;
        String currentKey;
        int[] xTrail = word.getXTrail();
        int[] yTrail = word.getYTrail();

        for (int c=0; c<name.length(); c++) {
            currentKey = "" + name.charAt(c);
            if (letterPositions.containsKey(currentKey)) {
                currentValue = letterPositions.get(currentKey);
                currentValue.add(xTrail[c] + " " + yTrail[c]);
            } else {
                currentValue = new ArrayList<>(Arrays.asList(xTrail[c] + " " + yTrail[c]));
            }
            letterPositions.put(currentKey, currentValue); //어, (3,3) //쩔(3,4), //티(3,5), //비(3,6)
        }
    }

    // 단어가 게임판의 지정된 위치에 배치될 수 있는지 확인.
    private boolean tightCanAdd(Word word, String position, Direction direction) {
        int currentX = Utils.getX(position); //2
        int currentY = Utils.getY(position); //4
        String name = word.getName(); //저쩔티비

        if ((currentX<0) || (currentY<0)){
            return false;
        }

        for (int c=0; c<name.length(); c++) {
            if ((currentX >= dim) || (currentY >= dim)) { //9를 넘으면 false
                return false;
            }
            if ((fullBoard[currentX][currentY] != null) &&  //글자가 차있고, 그 좌표에 같은 글자가 아닐 경우 false
                    (!fullBoard[currentX][currentY].equals(name.charAt(c) + ""))) {
                return false;
            }
            if (direction == Direction.UP) {
                currentY++;
                continue;
            }
            if (direction == Direction.RIGHT) {
                currentX++;
                continue;
            }
            throw new IllegalArgumentException("The given direction was: " + direction);
        }
        return true;
    }

    // 단어 간격이 잘 맞도록 단어를 추가할 수 있는지 확인.
    private boolean spacedCanAdd(Word word, String position, Direction direction) { //단어, 시작좌표, 방향 -> 양사이드가 비어있는지 확인
        if (!tightCanAdd(word, position, direction)) { //들어갈 수 있는 곳인지 확인
            return false;
        }

        int fixedY = Utils.getY(position);
        int plusCount = 0;
        int minusCount = 0;
        if (direction == Direction.RIGHT) {
            for (int x : Utils.getXTrail(position, direction, word.getName().length())) {
                try {
                    if (fullBoard[x][fixedY+1] != null) {
                        plusCount++;
                    } else {
                        plusCount = 0;
                    }
                } catch (IndexOutOfBoundsException e) {
                    plusCount = 0;
                }
                if (plusCount == 2) {
                    return false;
                }

                try {
                    if (fullBoard[x][fixedY-1] != null) {
                        minusCount++;
                    } else {
                        minusCount = 0;
                    }
                } catch (IndexOutOfBoundsException e) {
                    minusCount = 0;
                }
                if (minusCount == 2) {
                    return false;
                }
            }
        }

        int fixedX = Utils.getX(position);
        plusCount = 0;
        minusCount = 0;
        if (direction == Direction.UP) {
            for (int y : Utils.getYTrail(position, direction, word.getName().length())) {
                try {
                    if (fullBoard[fixedX+1][y] != null) {
                        plusCount++;
                    } else {
                        plusCount = 0;
                    }
                } catch (IndexOutOfBoundsException e) {
                    plusCount = 0;
                }
                if (plusCount == 2) {
                    return false;
                }

                try {
                    if (fullBoard[fixedX-1][y] != null) {
                        minusCount++;
                    } else {
                        minusCount = 0;
                    }
                } catch (IndexOutOfBoundsException e) {
                    minusCount = 0;
                }
                if (minusCount == 2) {
                    return false;
                }
            }
        }

        return true;
    }

    // 게임판 단어 수 리턴
    public int numOfPlacedWords() {
        assert ((upWords + rightWords) == wordsOnBoard.size());
        return wordsOnBoard.size();
    }
}