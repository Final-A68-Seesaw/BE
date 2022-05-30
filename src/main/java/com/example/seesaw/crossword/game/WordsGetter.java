package com.example.seesaw.crossword.game;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

// 미리 만들어둔 파일의 랜덤 단어들을 얻어 관련된 모든것을 처리
public class WordsGetter {

    // 모든 숫자의 배열을 가져올 수 있도록 음수를 넣자.
    public static String[] getRandomWords(int givenNumToGet) {
        // words.txt 파일을 불러와 두글자 이상인 단어를 \n 씩 끊는다.
        String[] words = readFile("words.txt", 2).split("\n");
        int toGet; //
        if (givenNumToGet < 0) {
            toGet = words.length; // 단어 개수 전체 toGet 에 담기
        } else { // 이부분 필요없음
            toGet = givenNumToGet;
        }
        // 랜덤 객체 생성
        Random RANDOM = new Random();

        ArrayList<Integer> indexesToGet = new ArrayList<>();
        int currentIndex;

        while (indexesToGet.size() < toGet) {
            currentIndex = RANDOM.nextInt(words.length); // 전체 단어 길이의 수에서 랜덤으로 돌려서 인덱스를 하나 뽑음
            if (!indexesToGet.contains(currentIndex)) { // 인덱스가 포함되어있지 않으면 하나씩 indexToGet에다가 넣음. (중복 x)
                indexesToGet.add(currentIndex);
            }
        }
        String[] output = new String[toGet];
        for (int i = 0; i < toGet; i++) {
            output[i] = words[indexesToGet.get(i)].toUpperCase(); //대문자로 output 배열에 넣음 (여기까지하면 랜덤으로 모든 단어가 들어가있음)
        }
        return removeNulls(output);
    }

    // txt 파일 읽어오기
    public static String readFile(String fileName, int minWordLength) {
        String data = "";
        String currentLine;
        try {
            File myObj = new File(fileName);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                currentLine = myReader.nextLine();
                if (currentLine.trim().length() < minWordLength) {
                    continue;
                }
                data += currentLine + "\n";
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return data;
    }

    // 배열에 null 제거
    private static String[] removeNulls(String[] original) {
        List<String> noNulls = new ArrayList<String>();
        // output 하나씩 꺼내서 null 이 아니고 글자 길이도 0보다 크면 noNulls에 넣는다.
        for (String s : original) {
            if (s != null && s.length() > 0) {
                noNulls.add(s);
            }
        }
        //배열을 리턴 noNull사이즈만큼
        return noNulls.toArray(new String[noNulls.size()]);
    }
}