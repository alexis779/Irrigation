package tech.vineyard.irrigation;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CellType {
    EMPTY(0),
    SOURCE(1),
    PLANT(2);

    private static final Map<Integer, CellType> ID_TO_CELL = Arrays.stream(CellType.values())
            .collect(Collectors.toMap(CellType::getI, Function.identity()));

    int i;
    CellType(int i) {
        this.i = i;
    }

    public int getI() {
        return i;
    }

    public static CellType idToCell(int i) {
        return ID_TO_CELL.get(i);
    }
}
