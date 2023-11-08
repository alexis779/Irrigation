import tech.vineyard.irrigation.CellType;
import tech.vineyard.irrigation.Input;
import tech.vineyard.irrigation.Service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Irrigation {


    public static void main(String[] args) throws Exception {
//    InputStream inputStream = System.in;
        InputStream inputStream = new FileInputStream(args[0]);
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        int N = Integer.parseInt(in.readLine());
        int C = Integer.parseInt(in.readLine());
        int P = Integer.parseInt(in.readLine());
        int T = Integer.parseInt(in.readLine());
        int Z = Integer.parseInt(in.readLine());

        CellType[][] cellTypes = new CellType[N][N];

        // Read the grid
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int cellId = Integer.parseInt(in.readLine());
                cellTypes[i][j] = CellType.idToCell(cellId);
            }
        }

        Input input = new Input(N, C, P, T, Z, cellTypes);
        Service service = new Service(input);
        service.outputSolution();
    }
}
