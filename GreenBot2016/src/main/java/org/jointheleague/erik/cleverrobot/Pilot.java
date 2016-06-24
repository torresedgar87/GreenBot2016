package org.jointheleague.erik.cleverrobot;

import android.os.SystemClock;

import org.jointheleague.erik.irobot.IRobotAdapter;
import org.jointheleague.erik.irobot.IRobotInterface;

import java.util.ArrayList;
import java.util.List;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

public class Pilot extends IRobotAdapter {

    private static final String TAG = "Pilot";
    private final Dashboard dashboard;


    private int actionCount = 0;
    private int[] bumpValues;
    private List<Point> visitedPoints = null;
    //private int bumpThreshold = 1000;
    private int wheelSpeed = 150;
    private Point currentPoint = null;
    private int distanceBeforeTurn = 0;
    private boolean hasBumpedRight = false;
    private boolean hasBumpedLeft = false;
    private boolean hasBumpedCenter = false;
    int changeOfAngle = 0;

    public Pilot(IRobotInterface iRobot, Dashboard dashboard, IOIO ioio)
            throws ConnectionLostException {
        super(iRobot);
        safe();
        this.dashboard = dashboard;
        dashboard.log(dashboard.getString(R.string.hello));
    }

    /**
     * This method is executed when the robot first starts up.
     **/
    public void initialize() throws ConnectionLostException {
        //what would you like me to do, Clever Human?
        actionCount = 1;
        visitedPoints = new ArrayList<Point>();
        getAngle();
        getDistance();
    }

    /**
     * This method is called repeatedly.
     **/
    public void loop() throws ConnectionLostException {
        goStraightSomeDistance();
    }

    public void goStraightSomeDistance() throws ConnectionLostException {

        int[] bumpSums = collectBumpValues();

        if(bumpRight()){
            reverse(100, 200);
            hasBumpedCenter = true;
        }
        else if(bumpLeft()) {
            reverse(200, 100);
        }
        else if(lightBumpRightLargest(bumpSums) && hasBumpedRight == false){
            hasBumpedRight = true;
        }
        else if(lightBumpCenterLargest(bumpSums) && hasBumpedCenter == false) {
            hasBumpedCenter = true;
            driveDirect(0, 0);
            SystemClock.sleep(500);
        }

        if(hasBumpedRight == true && distanceBeforeTurn < 1000 && hasBumpedCenter == false){
            distanceBeforeTurn += getDistance();
            driveDirect(100,200);
        }
        else if(hasBumpedCenter == true && changeOfAngle < 90){
            distanceBeforeTurn = 0;
            turnLeftSomeDistance();
        }
        else{
            driveDirect(300,100);
            distanceBeforeTurn = 0;
            changeOfAngle = 0;
            hasBumpedRight = false;
            hasBumpedCenter = false;
            getAngle();
            getDistance();
        }
    }

    public void turnRightSomeDistance() throws ConnectionLostException {

    }

    public void turnLeftSomeDistance() throws ConnectionLostException {
        changeOfAngle += Math.abs(getAngle());
        driveDirect(-200, 200);
    }

    public void reverse(int leftWheel, int rightWheel) throws ConnectionLostException {
        drive(0, 0);
        SystemClock.sleep(200);
        drive(-leftWheel, -rightWheel);
        SystemClock.sleep(500);
    }

    public int[] collectBumpValues() throws ConnectionLostException {
        getLightBumpValues();
        int rightSum = bumpValues[4] + bumpValues[5];
        int leftSum = bumpValues[0] + bumpValues[1];
        int centerSum = bumpValues[2] + bumpValues[3];

        return new int[]{ rightSum, centerSum, leftSum };
    }

    public boolean bumpRight() throws ConnectionLostException{
        readSensors(SENSORS_BUMPS_AND_WHEEL_DROPS);
        if (isBumpRight()) {
            dashboard.log("it bumped right");
            return true;
        }
        return false;
    }

    public boolean bumpLeft() throws ConnectionLostException{
        readSensors(SENSORS_BUMPS_AND_WHEEL_DROPS);
        if (isBumpLeft()) {
            dashboard.log("it bumped right");
            return true;
        }
        return false;
    }

    public boolean lightBumpRightLargest(int[] bumpSums) throws ConnectionLostException {
        if (bumpSums[0] > bumpSums[2] || bumpSums[0] > bumpSums[1]) {

            if (bumpValues[4] > 200 || bumpValues[5] > 200) {
                return true;
            }
        }

        return false;
    }


    public boolean bumpLeft(int[] bumpSums) throws ConnectionLostException {
        if (bumpSums[2] > bumpSums[0] || bumpSums[2] > bumpSums[1]) {

            if (bumpValues[0] > 200 || bumpValues[1] > 200) {
                return true;
            }
        }

        return false;
    }

    public boolean lightBumpCenterLargest(int[] bumpSums) throws ConnectionLostException {

        if (bumpSums[1] > bumpSums[2] || bumpSums[1] > bumpSums[0]) {

            if (bumpValues[2] > 200 || bumpValues[3] > 200) {
                return true;
            }
        }

        return false;
    }


    public int getWallStrength() throws ConnectionLostException {
        readSensors(SENSORS_WALL_SIGNAL);
        return getWallSignal();
    }

    public int getInfrared() throws ConnectionLostException {
        readSensors(SENSORS_INFRARED_BYTE);
        return getInfraredByte();
    }

    public void getLightBumpValues() throws ConnectionLostException {
        readSensors(SENSORS_GROUP_ID101);
        bumpValues = getLightBumps();
    }

    public void stop() throws ConnectionLostException
    {
        driveDirect(0, 0);
    }

    public void storingPoints(Point p1) throws ConnectionLostException{

    }
}