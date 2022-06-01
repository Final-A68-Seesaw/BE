package com.example.seesaw.crossword.service;

import com.example.seesaw.crossword.dto.CrossWordResponseDto;
import com.example.seesaw.crossword.game.Board;
import com.example.seesaw.crossword.game.WordsGetter;
import com.example.seesaw.crossword.repository.CrosswordRepository;
import com.example.seesaw.crossword.repository.QuizNumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CrosswordService {

    private final CrosswordRepository crosswordRepository;
    private final QuizNumRepository quizNumRepository;

    // 게임판 변수
    private static Board board;

    public List<CrossWordResponseDto> getWord() {
        System.out.println("게임시작\n");

        // 스타트 게시
        startGame(10);

        // 데이터 Response
        return board.printClues();
    }

    // 게임을 초기화하고 시작하는 메서드
    private void startGame(int dim) {
        // words 에는 정렬된 txt 파일들의 파일(단어들)이 랜덤으로 들어있다.
        // -1 이면 0부터 모든 단어를 가져올 수 있다.
        String[] words = WordsGetter.getRandomWords(-1);

        board = new Board(dim, crosswordRepository, quizNumRepository);

        for (String word : words) {
            // 모든 단어가 배치가되면 break;  배치가 되지 않았으면 tryAddword 로
            if (!board.allWordsPlaced()) {
                board.tryAddWord(word);
            } else {
                break;
            }
        }
    }
}