package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class InvertedIndexServiceImpl extends UnicastRemoteObject implements InvertedIndexService {
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private ForkJoinPool forkJoinPool;

    protected InvertedIndexServiceImpl() throws RemoteException {
        super();
        // Create a ForkJoinPool with the desired number of threads
        forkJoinPool = new ForkJoinPool(NUM_THREADS);
    }

    @Override
    public Map<String, List<Integer>> getInvertedIndex(String fileName) throws RemoteException {
        Map<String, List<Integer>> invertedIndex = new HashMap<>();
        try {
            // Create a task to read and process the file
            FileIndexingTask task = new FileIndexingTask(fileName, invertedIndex);
            // Submit the task using ForkJoinPool and obtain the result
            invertedIndex = forkJoinPool.submit(task).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return invertedIndex;
    }

    // Define a Callable for parallel file processing
    private class FileIndexingTask implements Callable<Map<String, List<Integer>>> {
        private String fileName;
        private Map<String, List<Integer>> invertedIndex;

        public FileIndexingTask(String fileName, Map<String, List<Integer>> invertedIndex) {
            this.fileName = fileName;
            this.invertedIndex = invertedIndex;
        }

        @Override
        public Map<String, List<Integer>> call() throws Exception {
            try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                String line;
                int lineNumber = 1;
                while ((line = br.readLine()) != null) {
                    processLine(line, lineNumber);
                    lineNumber++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return invertedIndex;
        }

        private void processLine(String line, int lineNumber) {
            String[] words = line.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                synchronized (invertedIndex) {
                    invertedIndex.computeIfAbsent(word, k -> new ArrayList<>()).add(lineNumber * 1000 + i);
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Create an instance of the service
            InvertedIndexServiceImpl invertedIndexService = new InvertedIndexServiceImpl();
            LocateRegistry.createRegistry(9999);
            // Bind the service to the RMI registry
            Naming.rebind("rmi://127.0.0.1:9999/InvertedIndexService", invertedIndexService);

            System.out.println("InvertedIndexService is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
