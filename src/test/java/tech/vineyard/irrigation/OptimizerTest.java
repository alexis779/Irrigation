package tech.vineyard.irrigation;

import org.junit.jupiter.api.Test;
import tech.vineyard.irrigation.optimization.Optimizer;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OptimizerTest {

    @Test
    public void simple() throws IOException {
        CellType[][] cellTypes = new CellType[][] {
            { CellType.SOURCE, CellType.EMPTY, CellType.EMPTY },
            { CellType.EMPTY, CellType.EMPTY, CellType.EMPTY },
            { CellType.EMPTY, CellType.EMPTY, CellType.PLANT },
        };
        Input input = new Input(3, 1, 1, 1, 1, cellTypes);
        Optimizer optimizer = new Optimizer(input);
        Output output = optimizer.solution();
        boolean[][] p = output.pipes();
        assertTrue(p[0][1] ^ p[1][0]);
        assertTrue(p[0][2] ^ p[2][0]);
    }
}
