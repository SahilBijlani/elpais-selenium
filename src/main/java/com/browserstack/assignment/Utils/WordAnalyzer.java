package com.browserstack.assignment.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordAnalyzer {

    public static Map<String, Integer> analyzeRepeatedWords(List<String> headers) {
        Map<String, Integer> wordCounts = new HashMap<>();

        for (String header : headers) {
            // clean header: remove punctuation, to lowercase
            String cleanHeader = header.replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase();
            String[] words = cleanHeader.split("\\s+");

            for (String word : words) {
                if (word.length() > 2) { // Filter out very short words
                    wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                }
            }
        }

        // Filter for counts > 2
        Map<String, Integer> repeatedWords = new HashMap<>();
        for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
            if (entry.getValue() > 2) {
                repeatedWords.put(entry.getKey(), entry.getValue());
            }
        }

        return repeatedWords;
    }
}
