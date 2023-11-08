package tech.vineyard.irrigation;

import tech.vineyard.irrigation.optimization.Optimizer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.IntStream;

public class Service {

    private final PrintWriter printWriter;
    private final Input input;
    private final int N;

    public Service(Input input) throws IOException {
        this.input = input;
        N = input.N();

        OutputStream outputStream = new FileOutputStream("1.out"); // System.out; //
        printWriter = new PrintWriter(outputStream);
    }

    public void outputSolution() throws IOException {
        Optimizer optimizer = new Optimizer(input);
        Output output = optimizer.solution();

        boolean[][] pipes = output.pipes();
        boolean[][] sprinklers = output.sprinklers();

        int p = count(pipes);
        int s = count(sprinklers);
        int K = p + s;

        printWriter.println(K);
        IntStream.range(0, pipes.length)
                .forEach(i -> IntStream.range(0, N)
                        .filter(j -> pipes[i][j])
                        .forEach(j -> printPipe(i, j)));

        IntStream.range(0, pipes.length)
                .forEach(i -> IntStream.range(0, N)
                        .filter(j -> sprinklers[i][j])
                        .forEach(j -> printSprinkler(i, j)));

        printWriter.close();
    }

    private void printPipe(int i, int j) {
        printWriter.println(String.format("P %d %d %d %d", i, j, i, j));
    }

    private void printSprinkler(int i, int j) {
        printWriter.println(String.format("S %d %d", i, j));
    }

    private int count(boolean[][] b) {
        return Arrays.stream(b)
                .mapToInt(this::count)
                .sum();
    }

    private int count(boolean[] b) {
        return (int) IntStream.range(0, b.length)
                .filter(i -> b[i])
                .count();
    }
}
