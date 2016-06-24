package org.jointheleague.erik.cleverrobot;

/**
 * Created by StarWars on 6/21/16.
 */
public class Point {

    public int x;
    public int y;
    public boolean north;
    public boolean south;
    public boolean west;
    public boolean east;

    @Override
    public String toString() {
        return "x: " + x + " y: " + y + " north: " + north + " south: " + south + " west: " + west + " east: " + east;
    }
}
