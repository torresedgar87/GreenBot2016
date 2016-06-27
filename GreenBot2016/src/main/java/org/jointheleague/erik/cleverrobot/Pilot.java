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
    int changeOfAngleLeft = 0;
    int changeOfAngleRight = 0;
    private int doneWithRace = 0;

    int rightWheelSpeed = 51;
    int leftWheelSpeed = 500;

    int numModulus = 3;
    int count = 0;
    boolean forMineral = false;

    public Pilot(IRobotInterface iRobot, Dashboard dashboard, IOIO ioio)
            throws ConnectionLostException {
        super(iRobot);
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

        driveDirect(500,494);
        SystemClock.sleep(8000);
        dashboard.log("done driving");
        driveDirect(0,0);
    }

    /**
     * This method is called repeatedly.
     **/
    public void loop() throws ConnectionLostException {
        mineralChallenge();
    }

    public void mineralChallenge() throws ConnectionLostException {
        count++;

        if(bumpLeft() || bumpRight()){
            dashboard.log("bumped");
            forMineral = true;
        }

        if(forMineral){
            traverseMaze();
        }
        else {
            driveDirect(leftWheelSpeed, rightWheelSpeed);
            dashboard.log("Count " + count + " modulus " + numModulus + " = " +  (count % numModulus) + " rightWheel " + rightWheelSpeed);

            if (count % numModulus == 0) {
                rightWheelSpeed += 2;
                dashboard.log("add right speed if");
            }

            if (rightWheelSpeed % 50 == 0) {
                numModulus++;
            }
        }
    }

    public void dragRace() throws ConnectionLostException
    {
        if(bumpRight() || lightBumpRightLargest(collectBumpValues())){
            turnLeftSomeDistance();
            SystemClock.sleep(100);
        }
        else if(bumpLeft() || lightBumpLeftLargest(collectBumpValues())){
            turnRightSomeDistance();
            SystemClock.sleep(100);
        }

        if(lightBumpCenterLargest(collectBumpValues()) && doneWithRace != 2)
        {

            turnLeftSomeDistance();
            SystemClock.sleep(1820);
            doneWithRace += 1;

        }

        if (doneWithRace != 2) {
            driveDirect(500, 493);
        }else
        {
            driveDirect(0,0);
        }
    }

    public void traverseMaze() throws ConnectionLostException {

        int infrared = getInfrared();

        dashboard.log("infrared " + infrared);

        int[] bumpSums = collectBumpValues();

        if(bumpRight()){
            reverse(100, 200);
            hasBumpedCenter = true;
        }
        else if(bumpLeft()) {
            /*reverse(200, 200);
            SystemClock.sleep(500);*/
            drive(-200, -90);
            SystemClock.sleep(500);
            drive(0, 0);
        }
        else if(lightBumpRightLargest(bumpSums) && hasBumpedRight == false && infrared < 250){
            hasBumpedRight = true;
        }
        else if(lightBumpCenterLargest(bumpSums) && hasBumpedCenter == false && infrared < 250) {
            hasBumpedCenter = true;
            driveDirect(0, 0);
            SystemClock.sleep(200);
        }

        if(hasBumpedRight == true && distanceBeforeTurn < 1000 && hasBumpedCenter == false){
            distanceBeforeTurn += getDistance();
            driveDirect(100,200);
        }
        else if(hasBumpedCenter == true && changeOfAngleLeft < 90){
            distanceBeforeTurn = 0;
            turnLeftSomeDistance();
        }
        else{
            driveDirect(400,50);
            distanceBeforeTurn = 0;
            changeOfAngleLeft = 0;
            changeOfAngleRight = 0;
            hasBumpedRight = false;
            hasBumpedCenter = false;
            getAngle();
            getDistance();
        }
    }

    public void turnRightSomeDistance() throws ConnectionLostException {
        changeOfAngleRight += Math.abs(getAngle());
        driveDirect(200, -200);
    }

    public void turnLeftSomeDistance() throws ConnectionLostException {
        changeOfAngleLeft += Math.abs(getAngle());
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
            return true;
        }
        return false;
    }

    public boolean bumpLeft() throws ConnectionLostException{
        readSensors(SENSORS_BUMPS_AND_WHEEL_DROPS);
        if (isBumpLeft()) {
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


    public boolean lightBumpLeftLargest(int[] bumpSums) throws ConnectionLostException {
        if (bumpSums[2] > bumpSums[0] || bumpSums[2] > bumpSums[1]) {

            if (bumpValues[0] > 100 || bumpValues[1] > 100) {
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

    public void keepBumping() throws ConnectionLostException {
        reverse(200, 200);
        reverse(200, 200);
        driveDirect(0, 0);
    }
}