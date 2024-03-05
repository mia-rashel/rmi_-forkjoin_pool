package org.example;

import org.junit.jupiter.api.Test;
import java.rmi.Naming;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InvertedIndexClientTest {

    @Test
    public void testInvertedIndex() throws Exception {
        // Lookup the RMI service
        InvertedIndexService invertedIndexService = (InvertedIndexService) Naming.lookup("rmi://127.0.0.1:9999/InvertedIndexService");

        // Specify the file name to process
        String fileName = "src/main/resources/sample_data.txt"; // Replace with the actual file name
        // Get the inverted index from the RMI service
        Map<String, List<Integer>> invertedIndex = invertedIndexService.getInvertedIndex(fileName);

        // Calculate the frequency of each token
        Map<String, Long> tokenFrequency = invertedIndex.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (long) e.getValue().size()));

        // Sort the tokens by frequency (descending order)
        List<Map.Entry<String, Long>> sortedTokens = tokenFrequency.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5) // Take the top 5 tokens
                .collect(Collectors.toList());

        // Display the top 5 tokens with their locations
        System.out.println("Top 5 tokens with the most frequent appearance:");
        for (Map.Entry<String, Long> entry : sortedTokens) {
            String token = entry.getKey();
            Long frequency = entry.getValue();
            System.out.println("Token: " + token + ", Frequency: " + frequency);
            System.out.print("Locations: ");
            List<Integer> locations = invertedIndex.get(token);
            for (Integer location : locations) {
                int lineNumber = location / 1000;
                int positionIndex = location % 1000;
                System.out.print("(" + lineNumber + ":" + positionIndex + ") ");
            }
            System.out.println();
        }
    }
}
