import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.io.*;
import javax.imageio.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.BufferedImage;

import com.topcoder.marathon.*;

public class IrrigationTester extends MarathonAnimatedVis {
  // Parameter ranges
  private static final int minN = 8, maxN = 50;         // grid size range
  private static final int minS = 1, maxS = 5;          // water source range
  private static final int minC = 1, maxC = 30;         // connector cost range
  private static final int minP = 1, maxP = 30;         // pipe cost range
  private static final int minT = 30, maxT = 90;        // sprinkler cost range
  private static final int minZ = 1, maxZ = 4;          // sprinkler range
  private static final double minD = 0.05, maxD = 0.3;   // plant density range

  // Inputs
  private int N;                // grid size
  private int S;                // number of water sources
  private int C;                // connector cost
  private int P;                // pipe cost
  private int T;                // sprinkler cost
  private int Z;                // spray radius
  private int ZR;               // spray radius squared
  private double D;             // density

  // Constants
  private static final int EMPTY = 0;
  private static final int WATER = 1;
  private static final int PLANT = 2;
  private static final int SPRINKLER = 3;
  private static final int[] bits = {1,2,4,8};
  private static final int[] bx = {0,1,0,-1};
  private static final int[] by = {-1,0,1,0};

  // State Control
  private int[][] gridWorld; // fixed objects like water source, plants and sprinklers
  private int[][] gridPipe;  // pipe structure
  private int[][] gridWaterFlow; // water flow
  private int[][] gridWaterSpray; // Spray from working sprinklers
  private double score = 0;  
  private int[] numConnectors = new int[5];
  private int pipeLen = 0;
  private int numSprinklers = 0;
  private int numDryPlants = 0;
  private int[][] reachable; // Used to generate valid test cases
  private int[][] wet;       // Used to generate valid test cases
    
  // Graphics
  Color[] colours; 

  protected void generate()
  {
    N = randomInt(minN, maxN);
    S = randomInt(minS, maxS);
    C = randomInt(minC, maxC);
    P = randomInt(minP, maxP);
    T = randomInt(minT, maxT);
    Z = randomInt(minZ, maxZ);
    D = randomDouble(minD, maxD);

    //Special cases
    if (seed == 1)
    {
      N = minN;
      Z = minZ;
      S = minS;     
    }
    else if (seed == 2)
    {
      N = maxN;
      D = maxD;
      S = maxS; 
      Z = minZ;
    }

    //User defined parameters
    if (parameters.isDefined("N")) N = randomInt(parameters.getIntRange("N"), minN, maxN);
    if (parameters.isDefined("S")) S = randomInt(parameters.getIntRange("S"), minS, maxS);
    if (parameters.isDefined("C")) C = randomInt(parameters.getIntRange("C"), minC, maxC);
    if (parameters.isDefined("P")) P = randomInt(parameters.getIntRange("P"), minP, maxP);
    if (parameters.isDefined("T")) T = randomInt(parameters.getIntRange("T"), minT, maxT);
    if (parameters.isDefined("Z")) Z = randomInt(parameters.getIntRange("Z"), minZ, maxZ);
    if (parameters.isDefined("D")) D = randomDouble(parameters.getDoubleRange("D"), minD, maxD);

    ZR = Z*Z;
    gridPipe = new int[N][N];     
    gridWaterFlow = new int[N][N];  
    gridWaterSpray = new int[N][N];

    int itr = 0;
    gridWorld = new int[N][N];

    //generate the grid
    for (int row = 0; row < N; row++)
      for (int col = 0; col < N; col++)
        if (randomDouble(0,1) < D)
        {
          gridWorld[row][col] = PLANT;
        }
    // add the water sources
    for (int s=0; s<S;)
    {
      int row = randomInt(0,N-1);
      int col = randomInt(0,N-1);
      if (gridWorld[row][col]!=WATER)
      {
        gridWorld[row][col] = WATER;
        s++;
      }
    }
    makeValid();

    if (debug)
    {
      System.out.println("Grid size, N = " + N);
      System.out.println("Number of water sources, S = " + S);
      System.out.println("Connector cost, C = " + C);
      System.out.println("Pipe cost, P = " + P);
      System.out.println("Sprinkler cost, T = " + T);
      System.out.println("Sprinkler size, Z = " + Z);
      System.out.println("Density, D = " + D);
      System.out.println("Dry plant penalty, N*N = " + N*N);
      System.out.println();
      System.out.println("Grid values:");
      for (int row = 0; row < N; row++)
      {
        for (int col = 0; col < N; col++) System.out.print(gridWorld[row][col]+" ");
        System.out.println();
      }
    }
  }

  private void sprayIt(int r, int c)
  {
    for (int nr=-Z;nr<=Z;nr++)
      for (int nc=-Z;nc<=Z;nc++)
        if (inGrid(r+nr,c+nc) && nc*nc+nr*nr<=ZR)
          wet[r+nr][c+nc] = 1;
  }

  private void fill(int r, int c)
  {
    for (int d=0;d<4;d++)
    {
      int nr = r+bx[d];
      int nc = c+by[d];
      if (inGrid(nr, nc) && gridWorld[nr][nc]==EMPTY && reachable[nr][nc]==0)
      {
        reachable[nr][nc] = 1;
        sprayIt(nr, nc);
        fill(nr, nc);
      }
    }
  }

  // Make sure every plant can be reached
  private void makeValid()
  {
    reachable = new int[N][N];
    wet = new int[N][N];
    // Check if every plant can be irrigated
    for (int r=0;r<N;r++)
      for (int c=0;c<N;c++)
        if (gridWorld[r][c]==WATER)
        {
          fill(r, c);
        }
    for (int r=0;r<N;r++)
      for (int c=0;c<N;c++)
        if (gridWorld[r][c]==PLANT && wet[r][c]==0)
          gridWorld[r][c] = EMPTY;
  }

  protected boolean isMaximize() 
  {
    return false;
  }

  protected double run() throws Exception {
    init();
    return runAuto();
  }

  protected double runAuto() throws Exception {
    double score = callSolution();
    if (score < 0) {
      if (!isReadActive()) return getErrorScore();
      return fatalError();
    }
    return score;
  }

  protected void timeout() {
    addInfo("Time", getRunTime());
    update();
  }

  private void waterFlow(int r, int c)
  {
    if (gridWaterFlow[r][c]==1) return;
    if (gridPipe[r][c]==0) return;
    gridWaterFlow[r][c] = 1;
    int v = gridPipe[r][c];
    for (int d=0;d<4;d++)
      if ((v&bits[d])!=0)
      {
        int nr = r+by[d];
        int nc = c+bx[d];
        if (gridWaterFlow[nr][nc]==0)
          waterFlow(nr, nc);
      }
  }

  private void calculateScore()
  {
    score = 0;
    for (int i=0;i<5;i++) numConnectors[i] = 0;
    for (int row = 0; row < N; row++)
      for (int col = 0; col < N; col++)
      {     
        if (gridWorld[row][col]==WATER) continue; // No connectors at the water source
        if (gridPipe[row][col]>0)
        {
          int v = gridPipe[row][col];
          if (v==5 || v==10) continue; // straight, no connector
          int connector = 0;
          for (int d=0;d<4;d++)
            if ((v&bits[d])!=0) connector++;
          numConnectors[connector]++;
        }
      }
    // Water flow
    for (int row = 0; row < N; row++)
      for (int col = 0; col < N; col++)
      {
        if (gridWorld[row][col]==WATER)
        {
          waterFlow(row, col);
        }
      }
    // Add sprinklers
    for (int row = 0; row < N; row++)
      for (int col = 0; col < N; col++)
      {
        if (gridWorld[row][col]==SPRINKLER && gridWaterFlow[row][col]==1)
        {
          for (int nr=-Z;nr<=Z;nr++)
            for (int nc=-Z;nc<=Z;nc++)
              if (inGrid(row+nr,col+nc) && nc*nc+nr*nr<=ZR)
                gridWaterSpray[row+nr][col+nc]++;
        }
      }
    // Count dry plants
    numDryPlants = 0;
    for (int row = 0; row < N; row++)
      for (int col = 0; col < N; col++)
      {
        if (gridWorld[row][col]==PLANT && gridWaterSpray[row][col]==0)
        {
          numDryPlants++;
        }
      }
    // Calculate score
    score = pipeLen * P + numSprinklers * T + numConnectors[1] * C + numConnectors[2] * C * 2 + numConnectors[3] * C * 3 + numConnectors[4] * C * 4 + numDryPlants * N * N;
  }


  private double addPipe(int r1, int c1, int r2, int c2) throws Exception 
  {
    if (debug)
    {
      System.out.println("Adding pipe: ("+r1+","+c1+") - ("+r2+","+c2+")");
    }
    int dx,dy;
    // Find vertical direction
    if (r2>r1) dy = 1;
    else if (r2<r1) dy = -1;
    else dy = 0;
    // Find horizontal direction
    if (c2>c1) dx = 1;
    else if (c2<c1) dx = -1;
    else dx = 0;
    int pipeBits1,pipeBits2;
    /*
         1
       8   2
         4  
    */
    if (dx==0) 
    {
      if (dy<0)
      {
        pipeBits1 = 1;
        pipeBits2 = 4;
      } else
      {
        pipeBits1 = 4;
        pipeBits2 = 1;      
      }
    }
    else
    {
      if (dx<0)
      {
        pipeBits1 = 8;
        pipeBits2 = 2;
      } else
      {
        pipeBits1 = 2;
        pipeBits2 = 8;      
      }
    }
    int len = Math.max(Math.abs(r1-r2), Math.abs(c1-c2));
    pipeLen += len;
    for (int i=0;i<=len;i++)
    {
      int piece = 0;
      if (i==0) piece = pipeBits1;
      else if (i==len) piece = pipeBits2;
      else piece = pipeBits1 | pipeBits2;

      if (gridWorld[r1][c1]==PLANT) return fatalError("You can not place a pipe on a plant");
      if ((gridPipe[r1][c1]&piece)>0) return fatalError("You can not place a pipe on top of another pipe at ("+r1+","+c1+")");
      gridPipe[r1][c1] |= piece;

      r1 += dy;
      c1 += dx;
    }
    return 0;
  }

  private double callSolution() throws Exception {

    writeLine(N);
    writeLine(C);
    writeLine(P);
    writeLine(T);
    writeLine(Z);
    // print grid
    for (int row = 0; row < N; row++)
      for (int col = 0; col < N; col++)
        writeLine(gridWorld[row][col]);
    flush();
    if (!isReadActive()) return -1;

    updateState();

    try {
      List<String> commands = new ArrayList<String>();
      // Read the input and time the solution
      startTime();
      int numCommands = Integer.parseInt(readLine()); 
      if (numCommands<0 || numCommands>N*N*2)
        return fatalError("Illegal number of commands");
      for (int i = 0; i < numCommands; i++)
      {
        commands.add(readLine());
      }
      stopTime();

      // Process the commands
      for (String cmd : commands)
      {
        String[] loc = cmd.split(" ");
        if (loc[0].equals("P")) // Adding a pipe
        {
          if (loc.length != 5)
            return fatalError("You need to provide the pipe details as (Row1 Col1 Row2 Col2)");
          int r1 = Integer.parseInt(loc[1]);
          int c1 = Integer.parseInt(loc[2]);
          if (!inGrid(r1,c1))
            return fatalError("The cell (" + r1 + ", " + c1 + ") is outside the grid.");
          int r2 = Integer.parseInt(loc[3]);
          int c2 = Integer.parseInt(loc[4]);
          if (!inGrid(r2,c2))
            return fatalError("The cell (" + r2 + ", " + c2 + ") is outside the grid.");
          if (r1!=r2 && c1!=c2)
            return fatalError("The pipe must be horizontal or vertical.");
          if (r1==r2 && c1==c2)
            return fatalError("The start and end of a pipe must be different.");
          if (addPipe(r1, c1, r2, c2)<0) return -1;
          // Optional, show all commands visualized one by one
          if (parameters.isDefined("showAllSteps")) updateState();
        } else if (loc[0].equals("S")) // Adding a sprinkler
        {
          if (loc.length != 3)
            return fatalError("You need to provide the sprinkler details as (Row Col)");
          int r = Integer.parseInt(loc[1]);
          int c = Integer.parseInt(loc[2]);
          if (!inGrid(r,c))
            return fatalError("The cell (" + r + ", " + c + ") is outside the grid.");
          if (gridWorld[r][c]!=EMPTY)
            return fatalError("You can not place a sprinkler on top of a plant, sprinkler or water source. (" + r + ", " + c + ")");
          if (gridPipe[r][c]==0)
            return fatalError("You can only place a sprinkler on a pipe. (" + r + ", " + c + ")");
          gridWorld[r][c] = SPRINKLER;
          numSprinklers++;
          // Optional, show all commands visualized one by one
          if (parameters.isDefined("showAllSteps")) updateState();
        } else
        {
          return fatalError("Invalid command found ["+loc[0]+"]");
        }
      }

      
      calculateScore();
      updateState();
    } catch (Exception e) 
    {
      if (debug) System.out.println(e.toString());
      return fatalError("Cannot parse your output");
    }

    if (hasVis()) // add spray effect
    {
      for (int i=0;i<10;i++)
        updateState();
    }

    if (debug)
    {
      System.out.println("Grid pipes:");
      for (int row = 0; row < N; row++)
      {
        for (int col = 0; col < N; col++) System.out.print(gridPipe[row][col]+" ");
        System.out.println();
      }
    }
    
    return score;
  }


  protected void updateState()
  {
    if (hasVis())
    {      
      synchronized (updateLock) {    
        addInfo("Connectors[1]", numConnectors[1]);
        addInfo("Connectors[2]", numConnectors[2]);
        addInfo("Connectors[3]", numConnectors[3]);
        addInfo("Connectors[4]", numConnectors[4]);
        addInfo("Pipe length", pipeLen);
        addInfo("Sprinklers", numSprinklers);
        addInfo("Dry Plants", numDryPlants);
        addInfo("Time",  getRunTime());  
        addInfo("Score", shorten(score)); 
      }
      updateDelay();
    }
  }     

  private boolean inGrid(int row, int col) {
    return row >= 0 && row < N && col >= 0 && col < N;
  }


  protected void paintContent(Graphics2D g)
  {
    adjustFont(g, Font.SANS_SERIF, Font.PLAIN, String.valueOf("1"), new Rectangle2D.Double(0, 0, 0.5, 0.5));
    g.setStroke(new BasicStroke(0.005f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));    
    
    //draw grid         
    g.setColor(Color.white);
    g.fillRect(0, 0, N, N);    
    g.setColor(Color.gray);
    for (int row = 0; row <= N; row++)
    {
      g.drawLine(0, row, N, row);
      g.drawLine(row, 0, row, N);
    }    

    // Draw plants and water sources    
    for (int row = 0; row < N; row++)
      for (int col = 0; col < N; col++)
      {     
        if (gridWorld[row][col]==PLANT)
        {
          if (gridWaterSpray[row][col]>0)
            g.setColor(Color.green);
          else
            g.setColor(Color.red);
          g.fillRect(col, row, 1, 1);  
          g.drawRect(col, row, 1, 1);   
        } else
        if (gridWorld[row][col]==WATER)
        {
          g.setColor(Color.black);
          g.fillRect(col, row, 1, 1);
          g.setColor(Color.blue);
          g.fillOval(col, row, 1, 1);
        }
      }

    // Draw pipes
    g.setStroke(new BasicStroke(0.4f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
    
    for (int row = 0; row < N; row++)
      for (int col = 0; col < N; col++)
      {     
        if (gridPipe[row][col]>0)
        {
          if (gridWaterFlow[row][col]==0)
            g.setColor(Color.black);
          else
            g.setColor(Color.blue);
          int v = gridPipe[row][col];
          for (int d=0;d<4;d++)
            if ((v&bits[d])!=0)
            {
              GeneralPath gp = new GeneralPath();
              gp.moveTo(col+0.5,row+0.5);
              gp.lineTo(col+bx[d]+0.5,row+by[d]+0.5);
              g.draw(gp);
            }
        }
      }
    // Draw connectors
    g.setStroke(new BasicStroke(0.4f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
    g.setColor(Color.green);
    for (int row = 0; row < N; row++)
      for (int col = 0; col < N; col++)
      { 
        if (gridWorld[row][col]==WATER) continue; // no connector at water source
        if (gridPipe[row][col]>0)
        {
          int v = gridPipe[row][col];
          if (v==5 || v==10) continue; // straight, no connector
          int c = 0;
          for (int d=0;d<4;d++)
            if ((v&bits[d])!=0) c++;
          g.setColor(new Color(255,255-c*32,0));
          for (int d=0;d<4;d++)
            if ((v&bits[d])!=0)
            {
              GeneralPath gp = new GeneralPath();
              gp.moveTo(col+0.5,row+0.5);
              gp.lineTo(col+bx[d]*0.1+0.5,row+by[d]*0.1+0.5);
              g.draw(gp);
            }
        }
      }

    for (int t=0;t<2;t++)
    {
      if (t==0)
      {
        g.setStroke(new BasicStroke(0.2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
        g.setColor(Color.black);
      } else
      {
        g.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
        g.setColor(Color.cyan);
      }
      // Draw sprinklers
      for (int row = 0; row < N; row++)
        for (int col = 0; col < N; col++)
        {     
          if (gridWorld[row][col]==SPRINKLER)
          {
            g.drawOval(col, row, 1, 1);   
          }
        }
    }

    // Draw spray
    g.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); 
    g.setColor(Color.blue);
    for (int row = 0; row < N; row++)
      for (int col = 0; col < N; col++)
      {     
        if (gridWaterSpray[row][col]>0)
        {
          GeneralPath gp = new GeneralPath();
          for (int k=0;k<15;k++)
          {
            double x = randomDouble(0,1);
            double y = randomDouble(0,1);
            gp.moveTo(col+x,row+y);
            gp.lineTo(col+x,row+y);
          }
          g.draw(gp);
        }
      }
         
  }

  private double shorten(double a)
  {
    return (double)Math.round(a * 1000.0) / 1000.0;
  }

  private void init() {
    if (hasVis())
    {
      colours = new Color[]{Color.blue, Color.red, Color.magenta, Color.orange, Color.cyan, Color.pink, Color.green};            
      //make transparent colours
      for (int i=0; i<colours.length; i++)
        colours[i] = new Color(colours[i].getRed(), colours[i].getGreen(), colours[i].getBlue(), 80);
     
      setDefaultDelay(1500);
      setContentRect(0, 0, N, N);
      setInfoMaxDimension(15, 9);
      addInfo("Seed", seed);
      addInfo("N", N);
      addInfo("S", S);
      addInfo("D", shorten(D));
      addInfoBreak();
      addInfo("Connector Cost", C);
      addInfo("Pipe Cost", P);
      addInfo("Sprinkler Cost", T);
      addInfo("Spray Size", Z);
      addInfoBreak();
      addInfo("Connectors[1]", 0);
      addInfo("Connectors[2]", 0);
      addInfo("Connectors[3]", 0);
      addInfo("Connectors[4]", 0);
      addInfo("Pipe length", 0);
      addInfo("Sprinklers", 0);
      addInfo("Dry Plants", 0);
      addInfoBreak();
      addInfo("Time", 0);
      update();
    }
  }
 
  public static void main(String[] args) {
      new MarathonController().run(args);
  }
}