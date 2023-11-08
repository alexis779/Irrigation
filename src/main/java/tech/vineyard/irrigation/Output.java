package tech.vineyard.irrigation;

public class Output {

    private final boolean[][] pipes;

    private final boolean[][] sprinklers;

    public Output(boolean[][] pipes, boolean[][] sprinklers) {
        this.pipes = pipes;
        this.sprinklers = sprinklers;
    }

    public boolean[][] pipes() {
        return pipes;
    }

    public boolean[][] sprinklers() {
        return sprinklers;
    }
}
