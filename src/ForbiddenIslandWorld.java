/*
 Assignment 9
 Into, Kyle
 kinto
 Russell-Benoit, Bryce
 bryce
*/

import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javalib.impworld.World;
import javalib.impworld.WorldScene;
import javalib.worldimages.*;
import tester.Tester;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

// represents a list
interface IList<T> extends Iterable<T> {

  // gets length of IList, used only for testing
  int length();
}

// represents the empty list
class MtList<T> implements IList {

  // accepts the iterator
  public Iterator iterator() {
    return new IListIterator(this);
  }

  // length of empty list
  public int length() {
    return 0;
  }

}

// represents a nonempty list
class ConsList<T> implements IList {
  T first;
  IList rest;

  // constructor
  ConsList(T first, IList rest) {
    this.first = first;
    this.rest = rest;
  }

  // accepts the iterator
  public Iterator iterator() {
    return new IListIterator(this);
  }

  // length of ConsList
  public int length() {
    return 1 + this.rest.length();
  }
}

// represents the iterator for an IList
class IListIterator<T> implements Iterator<T> {
  IList<T> list;

  // constructor
  public IListIterator(IList<T> list) {
    this.list = list;
  }

  // says if the list has a next element
  public boolean hasNext() {
    return list instanceof ConsList;
  }

  // gets the next item and preps for the next one
  public T next() {
    T first = ((ConsList<T>) list).first;
    list = ((ConsList<T>) list).rest;
    return first;
  }
}

// Represents a single square of the game area
class Cell {
  // represents absolute height of this cell, in feet
  int height;
  // In logical coordinates, with the origin at the top-left corner of the screen
  int x;
  int y;
  // the four adjacent cells to this one
  Cell left;
  Cell top;
  Cell right;
  Cell bottom;
  // reports whether this cell is flooded or not
  boolean isFlooded;
  // if this cell has been raised yet
  boolean engineered;

  // constructor
  public Cell(double height, int x, int y) {
    this.height = (int) height;
    this.x = x * Utils.SCALE;
    this.y = y * Utils.SCALE;
    this.left = null;
    this.top = null;
    this.right = null;
    this.bottom = null;
    this.isFlooded = false;
    this.engineered = false;
  }

  // returns the image of the cell
  public WorldImage cellImage(int waterHeight) {

    Color color;
    if (isFlooded) {
      color = new Color(0, 0, (int) Math.min(255, (int)
              Math.abs(255 - (int) Math.abs((height - waterHeight) * 5))));
    } else if (height < waterHeight) {
      color = new Color(125, (int) Math.min(255, 255 - (waterHeight - height)), 0);
    } else {
      int rb = (int) Math.min(255, (255 * (height - waterHeight) / Utils.ISLAND_HEIGHT));
      color = new Color(rb, 255, rb);
    }
    return new RectangleImage(Utils.SCALE, Utils.SCALE, OutlineMode.SOLID, color);
  }

  // returns whether cell is on edge of island
  public boolean onEdge() {
    return this.left.isFlooded ||
            this.right.isFlooded ||
            this.top.isFlooded ||
            this.bottom.isFlooded
                    && !this.isFlooded;
  }

  // floods current cell and all around it
  public void flood(int oceanHeight) {
    if (height <= oceanHeight) {
      isFlooded = true;
      if (!this.left.isFlooded) {
        this.left.flood(oceanHeight);
      }
      if (!this.right.isFlooded) {
        this.right.flood(oceanHeight);
      }
      if (!this.top.isFlooded) {
        this.top.flood(oceanHeight);
      }
      if (!this.bottom.isFlooded) {
        this.bottom.flood(oceanHeight);
      }
    }
  }

  // raises cell to 5 above water and raises ones around it
  public void engineer(int count, int waterLevel) {
    if (count > 0) {
      this.engineered = true;
      this.height = 5 + waterLevel;

      if (!top.engineered) {
        top.engineer(count - 1, waterLevel);
      }
      if (!bottom.engineered) {
        bottom.engineer(count - 1, waterLevel);
      }
      if (!left.engineered) {
        left.engineer(count - 1, waterLevel);
      }
      if (!right.engineered) {
        right.engineer(count - 1, waterLevel);
      }
    }
  }

}

// represents and cell in the ocean
class OceanCell extends Cell {

  // constructor
  public OceanCell(double height, int x, int y) {
    super(height, x, y);
    this.isFlooded = true;
  }

  // creates the image for an ocean cell
  @Override
  public WorldImage cellImage(int waterHeight) {
    return new RectangleImage(Utils.SCALE, Utils.SCALE, OutlineMode.SOLID, Color.BLUE);
  }
}

// class for player
class Player {
  int x;
  int y;
  int parts;
  Cell cur;

  // constructor
  Player(Cell cur) {
    this.cur = cur;
    this.x = this.cur.x;
    this.y = this.cur.y;
    this.parts = 0;
  }

  // image for player
  public WorldImage playerImage() {
    return new FromFileImage("assignment-9-2/images/pilot-icon.png");
  }

}

// player2 is another player
class Player2 extends Player {

  // constructor
  Player2(Cell cur) {
    super(cur);
  }
}


// represents the parts of the helicopter
class Target {
  int x;
  int y;
  Cell cur;
  boolean gone;

  Target(Cell cur) {
    this.cur = cur;
    this.x = this.cur.x;
    this.y = this.cur.y;
    gone = false;
  }

  // renders the parts
  public WorldImage partImage() {
    if (!gone) {
      return new FromFileImage("assignment-9-2/images/wrench.png");
    } else {
      return new EmptyImage();
    }
  }
}


// scuba extra credit
class Scuba extends Target {
  int activePlayer1;
  boolean activatePlayer1;
  int activePlayer2;
  boolean activatePlayer2;

  Scuba(Cell cur) {
    super(cur);
    activePlayer2 = 0;
    activePlayer1 = 0;
    activatePlayer2 = false;
    activatePlayer1 = false;
  }

  // renders the scuba icon
  public WorldImage partImage() {
    if (!gone) {
      return new FromFileImage("assignment-9-2/images/scuba.png");
    } else {
      return new EmptyImage();
    }
  }
}

// engineer extra credit
class Engineer extends Target {
  int activePlayer1;
  boolean activatePlayer2;
  int activePlayer2;
  boolean activatePlayer1;

  Engineer(Cell cur) {
    super(cur);
    activePlayer2 = 0;
    activePlayer1 = 0;
    activatePlayer2 = false;
    activatePlayer1 = false;
  }

  // renders the engineer icon
  public WorldImage partImage() {
    if (!gone) {
      return new FromFileImage("assignment-9-2/images/engineer.png");
    } else {
      return new EmptyImage();
    }
  }
}

// represents the helicopter/goal after getting all the parts
class HelicopterTarget extends Target {

  HelicopterTarget(Cell cur) {
    super(cur);
  }

  // renders the Helicopter
  public WorldImage partImage() {
    return new FromFileImage("assignment-9-2/images/helicopter.png");
  }
}

// world class
class ForbiddenIslandWorld extends World {

  // defines an int constant
  static final int ISLAND_SIZE = Utils.ISLAND_SIZE;
  static final int ISLAND_HEIGHT = Utils.ISLAND_HEIGHT;
  static final int BOARD_SIZE = Utils.BOARD_SIZE;


  // All the cells of the game, including the ocean
  IList<Cell> board = new MtList<Cell>();
  // 2d array of heights of cells
  ArrayList<ArrayList<Double>> heights = new ArrayList<ArrayList<Double>>();
  // 2d arraylist of cells
  ArrayList<ArrayList<Cell>> list = new ArrayList<ArrayList<Cell>>();

  // the current height of the ocean
  double waterHeight;

  // random
  Random r = new Random();

  Player player;
  Player2 player2;

  int numparts;

  IList<Target> parts;

  HelicopterTarget target;

  Scuba scuba;

  Engineer engineer;

  int score;

  MediaPlayer mediaPlayer;

  // constructs the forbidden island
  public ForbiddenIslandWorld(int type) {
    board = new MtList<Cell>();

    waterHeight = 0.0;

    score = 0;

    make2dBoards(type);
    checkForUnderwaterCellsAndMake1d();

    player = new Player(this.randomCell());
    player2 = new Player2(this.randomCell());

    numparts = 0;

    parts = buildTargets(Utils.TARGET_NUM);

    target = new HelicopterTarget(maxHeight());

    scuba = new Scuba(this.randomCell());
    engineer = new Engineer(this.randomCell());

    playMusic();

  }

  // plays music for extra credit
  public void playMusic() {
    final JFXPanel FXPANEL = new JFXPanel();
    Media med = new Media(new File("assignment-9-2/images/music.mp3").toURI().toString());
    mediaPlayer = new MediaPlayer(med);
    mediaPlayer.play();
  }

  public Cell maxHeight() {
    Cell max = new Cell(0, 0, 0);
    for (Cell c : board) {
      if (c.height > max.height) {
        max = c;
      }
    }
    return max;
  }

  public Cell randomCell() {
    IList<Cell> landcells = this.land();
    int randindex = this.r.nextInt(land().length());
    Iterator<Cell> landiter = landcells.iterator();
    Cell ce = landiter.next();
    while (randindex > 1 && landiter.hasNext()) {
      Cell cur = landiter.next();
      ce = cur;
      randindex--;
    }
    return ce;
  }

  // builds the list of targets to be used by the game
  IList<Target> buildTargets(int tnum) {
    if (tnum > 0) {
      return new ConsList<Target>(new Target(randomCell()), buildTargets(tnum - 1));
    } else {
      return new MtList<Target>();
    }
  }

  // finds all cells on the edge of the island
  public IList<Cell> findEdges() {
    Iterator<Cell> iterator = board.iterator();
    IList<Cell> edgeCells = new MtList<Cell>();

    while (iterator.hasNext()) {
      Cell nextCell = iterator.next();
      if (nextCell.onEdge()) {
        edgeCells = new ConsList<Cell>(nextCell, edgeCells);
      }
    }

    return edgeCells;
  }

  // checks for cell collisions
  public void checkForCollisions() {
    for (Target part : parts) {
      if ((player.x == part.x && player.y == part.y && !part.gone) ||
              (player2.x == part.x && player2.y == part.y && !part.gone)) {
        part.gone = true;
        score++;
      }
      if (player.x == scuba.x && player.y == scuba.y && !scuba.gone) {
        scuba.gone = true;
        scuba.activePlayer1 += 100;
      }
      if (player2.x == scuba.x && player2.y == scuba.y && !scuba.gone) {
        scuba.gone = true;
        scuba.activePlayer2 += 100;
      }
      if (player.x == engineer.x && player.y == engineer.y && !engineer.gone) {
        engineer.gone = true;
        engineer.activePlayer1 += 1;
      }
      if (player2.x == engineer.x && player2.y == engineer.y && !engineer.gone) {
        engineer.gone = true;
        engineer.activePlayer2 += 1;
      }

    }
  }

  // ends the world
  public WorldEnd worldEnds() {
    WorldScene win = new WorldScene(Utils.ISLAND_SIZE, Utils.ISLAND_SIZE);
    win.placeImageXY(new OverlayImage(new TextImage("You win!", 30, Color.RED),
                    new RectangleImage(Utils.BOARD_SIZE, Utils.BOARD_SIZE,
                            OutlineMode.SOLID, Color.BLUE)),
            Utils.BOARD_SIZE / 2, Utils.BOARD_SIZE / 2);
    WorldScene lose = new WorldScene(Utils.ISLAND_SIZE, Utils.ISLAND_SIZE);
    lose.placeImageXY(new OverlayImage(new TextImage(
                    "You lose! You had " + score + " out of 3 helicopter parts!", 30, Color.RED),
                    new RectangleImage(Utils.BOARD_SIZE, Utils.BOARD_SIZE,
                            OutlineMode.SOLID, Color.BLUE)),
            Utils.BOARD_SIZE / 2, Utils.BOARD_SIZE / 2);
    // if player is touching water
    if (player.cur.isFlooded && !(scuba.activePlayer1 > 0 && scuba.activatePlayer1)) {
      return new WorldEnd(true, lose);
    }
    if (player2.cur.isFlooded && !(scuba.activePlayer2 > 0 && scuba.activatePlayer2)) {
      return new WorldEnd(true, lose);
    }

    // player is touching helicopter
    else if ((player.x == target.x && player.y == target.y) ||
            (player2.x == target.x && player2.y == target.y)) {
      if (score >= 3) {
        return new WorldEnd(true, win);
      }
    }
    return new WorldEnd(false, this.makeScene());
  }

  //create a list of all the land cells
  IList<Cell> land() {
    Iterator<Cell> iter = this.board.iterator();
    IList<Cell> landcells = new MtList<Cell>();
    while (iter.hasNext()) {
      Cell next = iter.next();
      if (!next.isFlooded) {
        landcells = new ConsList(next, landcells);
      }
    }
    return landcells;
  }


  public void checkForUnderwaterCellsAndMake1d() {
    // check every cell in arraylist for being underwater
    IList<Cell> underwaterCells = findEdges();
    Iterator<Cell> iter = underwaterCells.iterator();

    while (iter.hasNext()) {
      iter.next().flood((int) waterHeight);
    }
  }

  public void make2dBoards(int type) {
    heights.clear();
    list.clear();

    // for regular mountain
    if (type == 1) {
      for (int i = 0; i < ISLAND_SIZE; i++) {
        heights.add(i, new ArrayList<Double>());
        for (int j = 0; j < ISLAND_SIZE; j++) {
          heights.get(i).add(j, ISLAND_HEIGHT -
                  (Math.abs(ISLAND_SIZE / 2.0 - i) + Math.abs(ISLAND_SIZE / 2.0 - j)));
        }
      }

      // make into 2d array of cells
      for (int i = 0; i < heights.size(); i++) {
        list.add(i, new ArrayList<Cell>());
        for (int j = 0; j < heights.get(i).size(); j++) {
          if (heights.get(i).get(j) > 0) {
            list.get(i).add(j, new Cell(heights.get(i).get(j), i, j));
          } else {
            list.get(i).add(j, new OceanCell(heights.get(i).get(j), i, j));
          }
        }
      }
    }

    // for random mountain
    if (type == 2) {

      // makes a 2d array of heights
      for (int i = 0; i < ISLAND_SIZE; i++) {
        heights.add(i, new ArrayList<Double>());
        for (int j = 0; j < ISLAND_SIZE; j++) {
          heights.get(i).add(j, r.nextDouble() * ISLAND_HEIGHT);
        }
      }
      // make into 2d array of cells
      for (int i = 0; i < heights.size(); i++) {
        list.add(i, new ArrayList<Cell>());
        for (int j = 0; j < heights.get(i).size(); j++) {
          if (ISLAND_HEIGHT -
                  (Math.abs(ISLAND_SIZE / 2.0 - i) + Math.abs(ISLAND_SIZE / 2.0 - j)) > 0) {
            list.get(i).add(j, new Cell(heights.get(i).get(j), i, j));
          } else {
            list.get(i).add(j, new OceanCell(0.0, i, j));
          }
        }
      }
    }

    // for random terrain
    if (type == 3) {

      for (int i = 0; i <= ISLAND_SIZE; i++) {
        heights.add(i, new ArrayList<Double>());
        for (int j = 0; j <= ISLAND_SIZE; j++) {
          heights.get(i).add(j, 0.0);
        }
      }

      heights.get(0).set(ISLAND_SIZE / 2, 1.0);
      heights.get(ISLAND_SIZE / 2).set(0, 1.0);
      heights.get(ISLAND_SIZE).set(ISLAND_SIZE / 2, 1.0);
      heights.get(ISLAND_SIZE / 2).set(ISLAND_SIZE, 1.0);
      heights.get(ISLAND_SIZE / 2).set(ISLAND_SIZE / 2, (double) ISLAND_HEIGHT);

      randomTerrain(0, ISLAND_SIZE / 2, 0, ISLAND_SIZE / 2, heights);
      randomTerrain(0, ISLAND_SIZE / 2, ISLAND_SIZE / 2, ISLAND_SIZE, heights);
      randomTerrain(ISLAND_SIZE / 2, ISLAND_SIZE, 0, ISLAND_SIZE / 2, heights);
      randomTerrain(ISLAND_SIZE / 2, ISLAND_SIZE, ISLAND_SIZE / 2, ISLAND_SIZE, heights);


      // make into 2d array of cells
      list.clear();
      for (int i = 0; i < heights.size(); i++) {
        list.add(i, new ArrayList<Cell>());
        for (int j = 0; j < heights.get(i).size(); j++) {
          if (heights.get(i).get(j) > 0) {
            list.get(i).add(j, new Cell(heights.get(i).get(j), i, j));
          } else {
            list.get(i).add(j, new OceanCell(heights.get(i).get(j), i, j));
          }
        }
      }
    }

    // add top, bottom, left, right to cells
    for (int i = 0; i < list.size(); i++) {
      for (int j = 0; j < list.get(i).size(); j++) {
        if (i != 0) {
          list.get(i).get(j).left = list.get(i - 1).get(j);
        } else {
          list.get(i).get(j).left = list.get(i).get(j);
        }
        if (i < list.size() - 1) {
          list.get(i).get(j).right = list.get(i + 1).get(j);
        } else {
          list.get(i).get(j).right = list.get(i).get(j);
        }
        if (j != 0) {
          list.get(i).get(j).top = list.get(i).get(j - 1);
        } else {
          list.get(i).get(j).top = list.get(i).get(j);
        }
        if (j < list.get(i).size() - 1) {
          list.get(i).get(j).bottom = list.get(i).get(j + 1);
        } else {
          list.get(i).get(j).bottom = list.get(i).get(j);
        }
      }
    }

    board = new MtList<Cell>();
    // make into 1d array of cells
    for (int i = 0; i < list.size(); i++) {
      for (int j = 0; j < list.get(i).size(); j++) {
        assert list.get(i).get(j) instanceof Cell;
        board = new ConsList<Cell>(list.get(i).get(j), board);
      }
    }
  }


  // draws the game
  public WorldScene makeScene() {
    WorldScene world = getEmptyScene();

    for (Cell c : board) {
      world.placeImageXY(c.cellImage((int) waterHeight), c.x + Utils.SCALE, c.y + Utils.SCALE);
    }
    world.placeImageXY(player.playerImage(), player.x, player.y);
    world.placeImageXY(player2.playerImage(), player2.x, player2.y);
    world.placeImageXY(target.partImage(), target.x, target.y);
    world.placeImageXY(scuba.partImage(), scuba.x, scuba.y);
    world.placeImageXY(engineer.partImage(), engineer.x, engineer.y);

    // place targets
    for (Target t : parts) {
      world.placeImageXY(t.partImage(), t.x, t.y);
    }

    // place score
    world.placeImageXY(new TextImage("Parts: " + score + "/3", 20, Color.RED), 500, 50);
    world.placeImageXY(new OverlayOffsetImage(new TextImage(
                    "Player 1: Scuba " + scuba.activePlayer1 +
                            ", Engineer: " + engineer.activePlayer1,
                    20, Color.RED), 0, 20,
                    new TextImage("Player 2: Scuba " + scuba.activePlayer2 +
                            ", Engineer: " + engineer.activePlayer2, 20, Color.RED)),
            500, Utils.BOARD_SIZE - 100);
    return world;
  }

  // handles keystrokes
  public void onKeyEvent(String k) {
    // needs to handle up, down, left, right to move the player
    // handles resets
    if (k.equalsIgnoreCase("m")) {
      this.waterHeight = 0;
      this.make2dBoards(1);
      checkForUnderwaterCellsAndMake1d();
      this.player = new Player(randomCell());
      this.player2 = new Player2(randomCell());
      this.target = new HelicopterTarget(maxHeight());
      this.scuba = new Scuba(randomCell());
      this.engineer = new Engineer(randomCell());
      this.parts = buildTargets(Utils.TARGET_NUM);
    } else if (k.equalsIgnoreCase("r")) {
      this.waterHeight = 0;
      this.make2dBoards(2);
      checkForUnderwaterCellsAndMake1d();
      this.player = new Player(randomCell());
      this.player2 = new Player2(randomCell());
      this.target = new HelicopterTarget(maxHeight());
      this.scuba = new Scuba(randomCell());
      this.engineer = new Engineer(randomCell());
      this.parts = buildTargets(Utils.TARGET_NUM);
    } else if (k.equalsIgnoreCase("t")) {
      this.waterHeight = 0;
      this.make2dBoards(3);
      checkForUnderwaterCellsAndMake1d();
      this.player = new Player(randomCell());
      this.player2 = new Player2(randomCell());
      this.target = new HelicopterTarget(maxHeight());
      this.scuba = new Scuba(randomCell());
      this.engineer = new Engineer(randomCell());
      this.parts = buildTargets(Utils.TARGET_NUM);
    } else if (k.equals("up")) {
      player.y -= Utils.SCALE;
      player.cur = player.cur.top;
      checkForCollisions();
    } else if (k.equals("down")) {
      player.y += Utils.SCALE;
      player.cur = player.cur.bottom;
      checkForCollisions();
    } else if (k.equals("left")) {
      player.x -= Utils.SCALE;
      player.cur = player.cur.left;
      checkForCollisions();
    } else if (k.equals("right")) {
      player.x += Utils.SCALE;
      player.cur = player.cur.right;
      checkForCollisions();
    } else if (k.equals("w")) {
      player2.y -= Utils.SCALE;
      player2.cur = player2.cur.top;
      checkForCollisions();
    } else if (k.equals("s")) {
      player2.y += Utils.SCALE;
      player2.cur = player2.cur.bottom;
      checkForCollisions();
    } else if (k.equals("a")) {
      player2.x -= Utils.SCALE;
      player2.cur = player2.cur.left;
      checkForCollisions();
    } else if (k.equals("d")) {
      player2.x += Utils.SCALE;
      player2.cur = player2.cur.right;
      checkForCollisions();
    } else if (k.equals("j")) {
      scuba.activatePlayer1 = true;
    } else if (k.equals("k")) {
      activateEngineer(1);
    } else if (k.equals("q")) {
      scuba.activatePlayer2 = true;
    } else if (k.equals("e")) {
      activateEngineer(2);
    }
  }

  // builds cells around player up 10
  public void activateEngineer(int p) {
    if (engineer.activePlayer1 > 0 && p == 1) {
      player.cur.engineer(5, (int) waterHeight);
      engineer.activePlayer1--;
    }
    if (engineer.activePlayer2 > 0 && p == 2) {
      player2.cur.engineer(5, (int) waterHeight);
      engineer.activePlayer2--;
    }
  }


  // handle on tick
  public void onTick() {
    if (scuba.activePlayer1 > 0 && scuba.activatePlayer1) {
      scuba.activePlayer1--;
    }
    if (scuba.activePlayer2 > 0 && scuba.activatePlayer2) {
      scuba.activePlayer2--;
    }
    //water height increases by .1 every tick so it increases by 1 every 10 ticks
    waterHeight = waterHeight + .1;
    checkForUnderwaterCellsAndMake1d();
  }

  // generates random terrain of map
  public void randomTerrain(int xLeft, int xRight, int yTop, int yBottom,
                            ArrayList<ArrayList<Double>> arr) {
    double area = (yBottom - yTop) * (xRight - xLeft);

    // multiplier for height of island
    double multiplier = (yBottom - yTop);
    double addiplier = -.5;


    double tl = arr.get(yTop).get(xLeft);
    double tr = arr.get(yTop).get(xRight);
    double bl = arr.get(yBottom).get(xLeft);
    double br = arr.get(yBottom).get(xRight);


    double t = (addiplier + r.nextDouble()) * multiplier + (tl + tr) / 2;
    double b = (addiplier + r.nextDouble()) * multiplier + (bl + br) / 2;
    double l = (addiplier + r.nextDouble()) * multiplier + (tl + bl) / 2;
    double right = (addiplier + r.nextDouble()) * multiplier + (tr + br) / 2;
    double middle = (addiplier + r.nextDouble()) * multiplier + (tr + tl + br + bl) / 4;


    // if we haven't already changed something...
    if (arr.get(yTop).get((xLeft + xRight) / 2) == 0) {
      arr.get(yTop).set((xLeft + xRight) / 2, t);
    }
    if (arr.get(yBottom).get((xLeft + xRight) / 2) == 0) {
      arr.get(yBottom).set((xLeft + xRight) / 2, b);
    }
    if (arr.get((yBottom + yTop) / 2).get(xLeft) == 0) {
      arr.get((yBottom + yTop) / 2).set(xLeft, l);
    }
    if (arr.get((yBottom + yTop) / 2).get(xRight) == 0) {
      arr.get((yBottom + yTop) / 2).set(xRight, right);
    }
    if (arr.get((yBottom + yTop) / 2).get((xLeft + xRight) / 2) == 0) {
      arr.get((yBottom + yTop) / 2).set((xLeft + xRight) / 2, middle);
    }

    // if we are at 1 pixel big, return this
    if (area > 2) {
      randomTerrain(xLeft, (xRight + xLeft) / 2, yTop, (yBottom + yTop) / 2, arr);
      randomTerrain((xLeft + xRight) / 2, xRight, yTop, (yBottom + yTop) / 2, arr);
      randomTerrain(xLeft, (xRight + xLeft) / 2, (yTop + yBottom) / 2, yBottom, arr);
      randomTerrain((xRight + xLeft) / 2, xRight, (yTop + yBottom) / 2, yBottom, arr);

    }

  }
}

// contains the constants for island
class Utils {
  public static int SCALE = 7;
  // don't set island_size > 120, or else IList will become stack overflowed while counting!!
  public static int ISLAND_SIZE = 120;
  public static int ISLAND_HEIGHT = 32;
  public static int BOARD_SIZE = ISLAND_SIZE * SCALE;
  public static int TARGET_NUM = 3;
}

// examples class
class ExamplesForbiddenIsland {
  int scale = Utils.SCALE;

  ConsList<Cell> itestbed;
  ConsList<String> nextlist;
  MtList<String> mt;

  IListIterator<String> stringiter;

  Cell cell1;
  Cell cell2;
  Cell cell3;
  Cell cell4;

  OceanCell ocean1;
  OceanCell ocean2;

  RectangleImage oceanrect1;
  RectangleImage landrectFlooded;
  RectangleImage landrectAboveWater;
  RectangleImage landrectBelowWater;

  ForbiddenIslandWorld fworld1;
  ForbiddenIslandWorld fworld2;
  ForbiddenIslandWorld fworld3;

  ArrayList<ArrayList<Cell>> testBed;

  Player playerDude;
  Target t1;
  Target t2;
  Scuba scuba1;
  Engineer engineer1;
  HelicopterTarget h1;

  WorldScene worldwin;
  WorldScene worldlose;

  // initialize all examples
  void initTest() {
    nextlist = new ConsList<String>("a",
            new ConsList<String>("b",
                    new ConsList<String>("c",
                            new MtList())));
    mt = new MtList<String>();

    stringiter = new IListIterator<String>(nextlist);

    cell1 = new Cell(4, 30, 10);
    cell2 = new Cell(4, 1, 1);
    cell3 = new Cell(10, 32, 32);
    cell4 = new Cell(100, 5, 5);

    ocean1 = new OceanCell(10, 20, 15);
    ocean2 = new OceanCell(1, 1, 1);

    oceanrect1 = new RectangleImage(scale, scale, OutlineMode.SOLID, Color.BLUE);
    landrectFlooded = new RectangleImage(scale, scale, OutlineMode.SOLID, new Color(0, 0,
            (int) Math.min(255, (int) Math.abs(255 - (int) Math.abs((cell1.height - 1) * 5)))));

    int rb = (int) Math.min(255, (255 * (cell1.height - 1) / Utils.ISLAND_HEIGHT));
    landrectAboveWater = new RectangleImage(scale, scale,
            OutlineMode.SOLID, new Color(rb, 255, rb));
    landrectBelowWater = new RectangleImage(scale, scale, OutlineMode.SOLID, new Color(125,
            (int) Math.min(255, 255 - (10 - 4)), 0));

    testBed = new ArrayList<ArrayList<Cell>>();
    testBed.add(new ArrayList<Cell>());
    testBed.get(0).add(cell1);
    testBed.get(0).add(cell2);
    testBed.get(0).add(cell3);

    cell1.left = cell1;
    cell1.right = cell2;
    cell1.top = cell1;
    cell1.bottom = cell1;
    cell2.left = cell1;
    cell2.right = cell3;
    cell2.top = cell2;
    cell2.bottom = cell2;
    cell3.left = cell2;
    cell3.right = cell3;
    cell3.top = cell3;
    cell3.bottom = cell3;

    itestbed = new ConsList<Cell>(cell1,
            new ConsList(cell2,
                    new ConsList(cell3, mt)));
    playerDude = new Player(cell2);
    t1 = new Target(cell1);
    t2 = new Target(cell2);
    scuba1 = new Scuba(cell3);
    engineer1 = new Engineer(cell3);
    h1 = new HelicopterTarget(cell1);

    fworld1 = new ForbiddenIslandWorld(1);
    fworld2 = new ForbiddenIslandWorld(2);
    fworld3 = new ForbiddenIslandWorld(3);

  }

  // test rendering of game
  void testGame(Tester t) {
    initTest();
    fworld1.bigBang(Utils.BOARD_SIZE, Utils.BOARD_SIZE, 0.5);
  }

  // test the iterator methods in Cons and Mt
  void testIterator(Tester t) {
    initTest();

    t.checkExpect(mt.iterator(), new IListIterator<String>(mt));
    t.checkExpect(nextlist.iterator(), new IListIterator<String>(nextlist));
  }

  // test next and hasNext methods used in IListIterator
  void testIListIterator(Tester t) {
    initTest();

    t.checkExpect(stringiter.hasNext(), true);
    t.checkExpect(stringiter.next(), "a");
    t.checkExpect(stringiter.hasNext(), true);
    t.checkExpect(stringiter.next(), "b");
    t.checkExpect(stringiter.hasNext(), true);
    t.checkExpect(stringiter.next(), "c");
    t.checkExpect(stringiter.hasNext(), false);
  }

  // test length method used solely for testing other methods
  void testLength(Tester t) {
    initTest();

    t.checkExpect(this.nextlist.length(), 3);
    t.checkExpect(this.mt.length(), 0);
  }

  // test cellImage method
  void testCellImage(Tester t) {
    initTest();

    // ocean cell
    t.checkExpect(this.ocean1.cellImage(0), this.oceanrect1);
    t.checkExpect(this.ocean2.cellImage(5), this.oceanrect1);

    // land cell
    t.checkExpect(this.cell1.cellImage(1), this.landrectAboveWater);
    t.checkExpect(this.cell1.cellImage(10), this.landrectBelowWater);
    cell1.isFlooded = true;
    t.checkExpect(this.cell1.cellImage(1), this.landrectFlooded);
  }

  // test cell onEdge
  void testCellEdge(Tester t) {
    initTest();

    t.checkExpect(fworld1.list.get(44
    ).get(44).onEdge(), true);
    t.checkExpect(fworld1.list.get(45).get(45).onEdge(), false);
  }

  // test cell flood
  void testCellFlood(Tester t) {
    initTest();
    t.checkExpect(cell1.isFlooded, false);
    t.checkExpect(cell2.isFlooded, false);
    cell1.flood(5);
    t.checkExpect(cell1.isFlooded, true);
    t.checkExpect(cell2.isFlooded, true);
    t.checkExpect(cell3.isFlooded, false);

    initTest();
    t.checkExpect(cell1.isFlooded, false);
    t.checkExpect(cell2.isFlooded, false);
    cell1.flood(0);
    t.checkExpect(cell1.isFlooded, false);
    t.checkExpect(cell2.isFlooded, false);

  }

  // test cell engineer
  public void testCellEngineer(Tester t) {
    initTest();
    t.checkExpect(cell1.height, 4);
    cell1.engineer(2, 4);
    t.checkExpect(cell1.height, 9);
    t.checkExpect(cell2.height, 9);
    t.checkExpect(cell3.height, 10);
  }

  // PLAYER CLASS
  // test player image
  public void testPlayerImage(Tester t) {
    t.checkExpect(playerDude.playerImage(),
            new FromFileImage("assignment-9-2/images/pilot-icon.png"));
  }

  // test TARGET
  public void testTarget(Tester t) {
    t.checkExpect(t1.partImage(), new FromFileImage("assignment-9-2/images/wrench.png"));
  }

  // test scuba image
  public void testScubaImage(Tester t) {
    t.checkExpect(scuba1.partImage(), new FromFileImage("assignment-9-2/images/scuba.png"));
  }

  // test engineer image
  public void testEngineerImage(Tester t) {
    t.checkExpect(engineer1.partImage(), new FromFileImage("assignment-9-2/images/engineer.png"));
  }

  // test helicopter image
  public void testHelicopterImage(Tester t) {
    t.checkExpect(h1.partImage(), new FromFileImage("assignment-9-2/images/helicopter.png"));
  }

  // test the ForbiddenIsland constructor and all data it creates
  void testForbiddenIslandConstructor(Tester t) {
    initTest();

    // test the size of list of cells
    t.checkExpect(fworld1.list.size(), Utils.ISLAND_SIZE);
    t.checkExpect(fworld1.list.get(32).size(), Utils.ISLAND_SIZE);
    t.checkExpect(fworld2.list.size(), Utils.ISLAND_SIZE);
    t.checkExpect(fworld2.list.get(32).size(), Utils.ISLAND_SIZE);

    // test size of list of heights
    t.checkExpect(fworld1.heights.size(), Utils.ISLAND_SIZE);
    t.checkExpect(fworld1.heights.get(32).size(), Utils.ISLAND_SIZE);
    t.checkExpect(fworld2.heights.size(), Utils.ISLAND_SIZE);
    t.checkExpect(fworld2.heights.get(32).size(), Utils.ISLAND_SIZE);

    // test neighbor assignment
    initTest();
    t.checkExpect(fworld1.list.get(10).get(10).top, fworld1.list.get(10).get(9));
    t.checkExpect(fworld1.list.get(10).get(10).bottom, fworld1.list.get(10).get(11));
    t.checkExpect(fworld1.list.get(10).get(10).left, fworld1.list.get(9).get(10));
    t.checkExpect(fworld1.list.get(10).get(10).right, fworld1.list.get(11).get(10));

    t.checkExpect(fworld2.list.get(10).get(10).top, fworld2.list.get(10).get(9));
    t.checkExpect(fworld2.list.get(10).get(10).bottom, fworld2.list.get(10).get(11));
    t.checkExpect(fworld2.list.get(10).get(10).left, fworld2.list.get(9).get(10));
    t.checkExpect(fworld2.list.get(10).get(10).right, fworld2.list.get(11).get(10));

    t.checkExpect(fworld3.list.get(10).get(10).top, fworld3.list.get(10).get(9));
    t.checkExpect(fworld3.list.get(10).get(10).bottom, fworld3.list.get(10).get(11));
    t.checkExpect(fworld3.list.get(10).get(10).left, fworld3.list.get(9).get(10));
    t.checkExpect(fworld3.list.get(10).get(10).right, fworld3.list.get(11).get(10));

    // test size of list of all cells
    t.checkExpect(fworld1.board.length(), Utils.ISLAND_SIZE * Utils.ISLAND_SIZE);
    t.checkExpect(fworld2.board.length(), Utils.ISLAND_SIZE * Utils.ISLAND_SIZE);
  }

  // test engineer
  public void testEngineer(Tester t) {

    initTest();
    t.checkExpect(fworld1.engineer.activePlayer2, 0);
    fworld1.activateEngineer(2);
    t.checkExpect(fworld1.engineer.activePlayer2, 0);

    fworld1.engineer.activePlayer2 = 3;
    fworld1.activateEngineer(2);
    t.checkExpect(fworld1.engineer.activePlayer2, 2);
  }

  // test random terrain
  public void testRandomTerrain(Tester t) {
    initTest();
    ArrayList<ArrayList<Double>> example = new ArrayList<ArrayList<Double>>();
    for (int i = 0; i <= Utils.ISLAND_SIZE; i++) {
      example.add(i, new ArrayList<Double>());
      for (int j = 0; j <= Utils.ISLAND_SIZE; j++) {
        example.get(i).add(j, 0.0);
      }
    }

    example.get(0).set(Utils.ISLAND_SIZE / 2, 1.0);
    example.get(Utils.ISLAND_SIZE / 2).set(0, 1.0);
    example.get(Utils.ISLAND_SIZE).set(Utils.ISLAND_SIZE / 2, 1.0);
    example.get(Utils.ISLAND_SIZE / 2).set(Utils.ISLAND_SIZE, 1.0);
    example.get(Utils.ISLAND_SIZE / 2).set(Utils.ISLAND_SIZE / 2, (double) Utils.ISLAND_HEIGHT);

    fworld1.randomTerrain(0, Utils.ISLAND_SIZE / 2, 0, Utils.ISLAND_SIZE / 2, example);
    fworld1.randomTerrain(0, Utils.ISLAND_SIZE / 2, Utils.ISLAND_SIZE / 2,
            Utils.ISLAND_SIZE, example);
    fworld1.randomTerrain(Utils.ISLAND_SIZE / 2, Utils.ISLAND_SIZE, 0,
            Utils.ISLAND_SIZE / 2, example);
    fworld1.randomTerrain(Utils.ISLAND_SIZE / 2, Utils.ISLAND_SIZE, Utils.ISLAND_SIZE / 2,
            Utils.ISLAND_SIZE, example);

    t.checkExpect(example.size(), Utils.ISLAND_SIZE + 1);
    t.checkExpect(example.get(0).size(), Utils.ISLAND_SIZE + 1);
    t.checkRange(example.get(0).get(0), -100.0, 34.0);

  }

  // test maxHeight method
  public void testMaxHeight(Tester t) {
    initTest();
    t.checkExpect(fworld1.maxHeight(),
            fworld1.list.get(Utils.ISLAND_SIZE / 2).get(Utils.ISLAND_SIZE / 2));
    fworld2.list.get(Utils.ISLAND_SIZE / 2).set(Utils.ISLAND_SIZE / 2, cell4);


  }

  // test buildTargets method
  public void testBuildTargets(Tester t) {
    initTest();
    fworld1.parts = fworld1.buildTargets(5);
    t.checkExpect(fworld1.parts.length(), 5);

    fworld2.parts = fworld2.buildTargets(8);
    t.checkExpect(fworld2.parts.length(), 8);

    fworld3.parts = fworld3.buildTargets(1);
    t.checkExpect(fworld3.parts.length(), 1);
  }

  // test make2dBoard method
  public void testMake2dBoards(Tester t) {
    initTest();
    fworld1.make2dBoards(1);
    t.checkExpect(fworld1.land().length(), 1985);
    fworld2.make2dBoards(2);
    t.checkExpect(fworld2.land().length(), 1985);
  }

  // test findEdges method
  public void testFindEdges(Tester t) {
    initTest();
    t.checkExpect(fworld1.findEdges().length(), 12539);
    fworld1.board = new ConsList<Cell>(cell1, new ConsList<Cell>(cell2, new MtList<Cell>()));
    t.checkExpect(fworld1.findEdges(), new MtList<Cell>());
  }

  // test the checkForCollisions method
  public void testCheckForCollisions(Tester t) {
    initTest();
    fworld1.parts = new ConsList<Target>(t2, mt);
    fworld1.player = playerDude;
    // no collision before
    t.checkExpect(t2.gone, false);
    fworld1.checkForCollisions();
    // collision recognized after
    t.checkExpect(t2.gone, true);
  }

  // test checkForUnderwaterCellsAndMake1d
  public void testForUnderwaterCells(Tester t) {
    initTest();
    // is it flooded before?
    t.checkExpect(cell1.isFlooded, false);
    fworld1.board = new ConsList<Cell>(cell1, mt);
    cell2.isFlooded = true;
    fworld1.waterHeight = 5;
    fworld1.checkForUnderwaterCellsAndMake1d();
    // is it flooded after?
    t.checkExpect(cell1.isFlooded, true);
  }

  // test music
  public void testMusic(Tester t) {
    initTest();
    fworld1.playMusic();
    fworld1.mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING);
  }

  // tests on key
  public void testOnKey(Tester t) {
    initTest();
    fworld1.player = playerDude;
    fworld1.onKeyEvent("down");
    t.checkExpect(fworld1.player.y, 2 * Utils.SCALE);
    fworld1.onKeyEvent("up");
    t.checkExpect(fworld1.player.y, Utils.SCALE);
    fworld1.onKeyEvent("right");
    t.checkExpect(fworld1.player.x, 2 * Utils.SCALE);
    fworld1.onKeyEvent("left");
    t.checkExpect(fworld1.player.x, Utils.SCALE);
    fworld1.player2.x = 10;
    fworld1.player2.y = 10;
    fworld1.onKeyEvent("s");
    t.checkExpect(fworld1.player2.y, 17);
    fworld1.onKeyEvent("w");
    t.checkExpect(fworld1.player2.y, 10);
    fworld1.onKeyEvent("d");
    t.checkExpect(fworld1.player2.x, 17);
    fworld1.onKeyEvent("a");
    t.checkExpect(fworld1.player2.x, 10);
    fworld1.onKeyEvent("j");
    t.checkExpect(fworld1.scuba.activatePlayer1, true);
    fworld1.onKeyEvent("q");
    t.checkExpect(fworld1.scuba.activatePlayer2, true);
    fworld1.onKeyEvent("k");
    fworld1.onKeyEvent("e");
    // tested on 1089
  }

  public void testOnTick(Tester t) {
    initTest();

    // height
    double height = fworld1.waterHeight;
    fworld1.onTick();
    t.checkExpect(fworld1.waterHeight, height + .1);

    //scuba
    fworld1.scuba.activePlayer1 = 10;
    fworld1.scuba.activatePlayer1 = true;
    fworld1.onTick();
    t.checkExpect(fworld1.scuba.activePlayer1, 9);
  }

  public void testWorldEnds(Tester t) {
    initTest();
    WorldScene lose = new WorldScene(Utils.ISLAND_SIZE, Utils.ISLAND_SIZE);
    lose.placeImageXY(new OverlayImage(new TextImage(
                    "You lose! You had " + 0 + " out of 3 helicopter parts!", 30, Color.RED),
                    new RectangleImage(Utils.BOARD_SIZE, Utils.BOARD_SIZE,
                            OutlineMode.SOLID, Color.BLUE)),
            Utils.BOARD_SIZE / 2, Utils.BOARD_SIZE / 2);
    WorldScene win = new WorldScene(Utils.ISLAND_SIZE, Utils.ISLAND_SIZE);
    win.placeImageXY(new OverlayImage(new TextImage("You win!", 30, Color.RED),
                    new RectangleImage(Utils.BOARD_SIZE, Utils.BOARD_SIZE,
                            OutlineMode.SOLID, Color.BLUE)),
            Utils.BOARD_SIZE / 2, Utils.BOARD_SIZE / 2);
    fworld1.player.cur.isFlooded = true;
    t.checkExpect(fworld1.worldEnds(), new WorldEnd(true, lose));

    initTest();
    fworld1.score = 3;
    fworld1.player.x = fworld1.target.x;
    fworld1.player.y = fworld1.target.y;
    t.checkExpect(fworld1.worldEnds(), new WorldEnd(true, win));
  }

}