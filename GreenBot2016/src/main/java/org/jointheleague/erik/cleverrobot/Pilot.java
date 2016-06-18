package org.jointheleague.erik.cleverrobot;

import android.os.SystemClock;
import android.util.Log;

import org.jointheleague.erik.cleverrobot.sensors.UltraSonicSensors;
import org.jointheleague.erik.irobot.IRobotAdapter;
import org.jointheleague.erik.irobot.IRobotInterface;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

public class Pilot extends IRobotAdapter {

    private static final String TAG = "Pilot";
    // The following measurements are taken from the interface specification
    private static final double WHEEL_DISTANCE = 235.0; //in mm
    private static final double WHEEL_DIAMETER = 72.0; //in mm
    private static final double ENCODER_COUNTS_PER_REVOLUTION = 508.8;

    private final Dashboard dashboard;
    public UltraSonicSensors sonar;
    int angle;
    private int startLeft;
    private int startRight;
    private int countsToGoWheelLeft;
    private int countsToGoWheelRight;
    private int directionLeft;
    private int directionRight;
    private static final int STRAIGHT_SPEED = 200;
    private static final int TURN_SPEED = 100;

    private int currentCommand = 0;
    private final boolean debug = true; // Set to true to get debug messages.

    private int totalDistanceForward = 0;
    private int totalDistanceBackward = 0;
    private int changeOfAngle = 0;
    private int totalAngle = 0;
    private int actionCount = 0;
    private boolean bumpLeft = false;
    private boolean bumpRight = false;
    private boolean bumpCenter = false;
    private int[] bumpValues;
    //private int bumpThreshold = 1000;

    public Pilot(IRobotInterface iRobot, Dashboard dashboard, IOIO ioio)
            throws ConnectionLostException {
        super(iRobot);
        sonar = new UltraSonicSensors(ioio);
        safe();
        this.dashboard = dashboard;
        dashboard.log(dashboard.getString(R.string.hello));
    }

    /** This method is executed when the robot first starts up. **/
    public void initialize() throws ConnectionLostException {
        //what would you like me to do, Clever Human?
        actionCount = 1;
    }

    /** This method is called repeatedly. **/
    public void loop() throws ConnectionLostException {
        if(actionCount == 1){
            goStraightSomeDistance(1000000, 2, 3, 2, 4);
        }else if(actionCount == 2){
            turnLeftSomeDistance(90, 1);
            SystemClock.sleep(1000);
        }else if(actionCount == 3){
            turnRightSomeDistance(90, 1);
            SystemClock.sleep(1000);
        }
        else if(actionCount == 4){
            //reverse(1000000, 1);
            driveDirect(0, 0);
        }
    }

    public void goStraightSomeDistance(int distanceToTravel, int ifBumpRight, int ifBumpLeft, int ifBumpCenter, int actionWhenDone) throws ConnectionLostException{
        totalDistanceForward += getDistance();
        getLightBumpValues();
        if(bumpRight()) {
            driveDirect(0, 0);
            actionCount = ifBumpRight;
        }
        else if(bumpLeft()){
            driveDirect(0, 0);
            actionCount = ifBumpLeft;
        }
        else if(bumpCenter()) {
            driveDirect(0, 0);
            actionCount = ifBumpCenter;
        }
        else if(totalDistanceForward > distanceToTravel) {
            driveDirect(0, 0);
            totalDistanceForward = 0;
            actionCount = actionWhenDone;
        }
        else{
            driveDirect(200, 200);
        }
    }

    public void turnRightSomeDistance(int angle, int action) throws ConnectionLostException {
        changeOfAngle += Math.abs(getAngle());

        if(changeOfAngle > angle) {
            driveDirect(0, 0);
            changeOfAngle = 0;
            actionCount = action;
        }else{
            driveDirect(200, -200);
        }
    }

    public void turnLeftSomeDistance(int angle, int action) throws ConnectionLostException {
        changeOfAngle += getAngle();

        if(changeOfAngle > angle){
            driveDirect(0, 0);
            changeOfAngle = 0;
            actionCount = action;
        }else{
            driveDirect(-200, 200);
        }
    }

    public void reverse(int distance, int action) throws ConnectionLostException {
        totalDistanceBackward += Math.abs(getDistance());

        if(totalDistanceBackward > distance) {
            driveDirect(0, 0);
            totalDistanceBackward = 0;
            actionCount = action;
            bumpRight = false;
            bumpLeft = false;
        }else{
            SystemClock.sleep(1000);
            driveDirect(-200, -200);
        }
    }

    public boolean bumpRight() throws ConnectionLostException {
        readSensors(SENSORS_BUMPS_AND_WHEEL_DROPS);

        if(isBumpRight()){
            return true;
        }

        int rightSum = bumpValues[4] + bumpValues[5];
        int leftSum = bumpValues[0] + bumpValues[1];
        int centerSum = bumpValues[2] + bumpValues[3];

        if(rightSum > leftSum || rightSum > centerSum){

            if(bumpValues[4] > 500 || bumpValues[5] > 500){
                return true;
            }
        }

        return false;
    }


    public boolean bumpLeft() throws ConnectionLostException {
        readSensors(SENSORS_BUMPS_AND_WHEEL_DROPS);

        if(isBumpLeft()){
            return true;
        }

        int rightSum = bumpValues[4] + bumpValues[5];
        int leftSum = bumpValues[0] + bumpValues[1];
        int centerSum = bumpValues[2] + bumpValues[3];

        if(leftSum > rightSum || leftSum > centerSum){

            if(bumpValues[0] > 500 || bumpValues[1] > 500){
                return true;
            }
        }

        return false;
    }

    public boolean bumpCenter() throws ConnectionLostException {
        int rightSum = bumpValues[4] + bumpValues[5];
        int leftSum = bumpValues[0] + bumpValues[1];
        int centerSum = bumpValues[2] + bumpValues[3];

        if(centerSum > leftSum || centerSum > rightSum){

            if(bumpValues[2] > 1000 || bumpValues[3] > 1000){
                return true;
            }
        }

        return false;
    }



    public int getWallStrength() throws ConnectionLostException{
        readSensors(SENSORS_WALL_SIGNAL);
        return getWallSignal();
    }

    public int getInfrared() throws ConnectionLostException {
        readSensors(SENSORS_INFRARED_BYTE);
        return getInfraredByte();
    }

    public void autoAngleCorrect() throws ConnectionLostException
    {
        if(Math.abs(totalAngle) > 5)
        {
            if(totalAngle > 5)
            {
                turnRightSomeDistance(5, 1);
            }
            else if(totalAngle < -5)
            {
                turnLeftSomeDistance(5, 1);
            }
            totalAngle = 0;
        }
    }
    public void getLightBumpValues() throws ConnectionLostException
    {
        readSensors(SENSORS_GROUP_ID101);
        bumpValues = getLightBumps();
    }

    /**
     * This method determines where to go next. This is a very simple Tortoise-like
     * implementation, but a more advanced implementation could take into account
     * sensory input, maze mapping, and other.
     *
     * @throws ConnectionLostException
     */
    private void nextCommand() throws ConnectionLostException {
//        try {
//            sonar.read();
//            int front = sonar.getDistanceFront();
//            if ( front < 50 ) {
//                currentCommand = 4; // shutdown if distance to object in front is less than 5 cm
//            }
//        } catch (InterruptedException e) {
//            dashboard.log(e.getMessage());
//        }
        dashboard.log("currentCommand = " + currentCommand);
        switch (currentCommand) {
            case 0:
                goStraight(1000);
                break;
            case 1:
                turnLeft(180);
                break;
            case 2:
                goStraight(1000);
                break;
            case 3:
                turnRight(180);
                break;
            case 4:
                shutDown();
                break;
            default:
        }
        currentCommand++;
    }

    private void shutDown() throws ConnectionLostException {
        dashboard.log("Shutting down... Bye!");
        stop();
        closeConnection();
    }

    /**
     * This method determines where to go next. This is a very simple Tortoise-like
     * implementation, but a more advanced implementation could take into account
     * sensory input, maze mapping, and other.
     *
     * @throws ConnectionLostException
     */
    private void nextCommandBis() throws ConnectionLostException {
        if (currentCommand < 8) {
            if (currentCommand % 2 == 0) {
                goStraight(1000);
            } else {
                turnRight(90);
            }
            currentCommand++;
        } else if (currentCommand == 8) {
            shutDown();
        }
    }

    /**
     * Moves the robot in a straight line. Note: Unexpected behavior may occur if distance
     * is larger than 14567mm.
     *
     * @param distance the distance to go in mm. Must be &le; 14567.
     */
    private void goStraight(int distance) throws ConnectionLostException {
        countsToGoWheelLeft = (int) (distance * ENCODER_COUNTS_PER_REVOLUTION
                / (Math.PI * WHEEL_DIAMETER));
        countsToGoWheelRight = countsToGoWheelLeft;
        if (debug) {
            String msg = String.format("Going straight  L: %d  R: %d",
                    countsToGoWheelLeft, countsToGoWheelRight);
            Log.d(TAG, msg);
            dashboard.log(msg);
        }
        directionLeft = 1;
        directionRight = 1;
        recordEncodersAndDrive(directionLeft * STRAIGHT_SPEED, directionRight * STRAIGHT_SPEED);
    }


    /**
     * Turns in place rightwards. Note: Unexpected behavior may occur if degrees is
     * larger than 7103 degrees (a little less than 20 revolutions).
     *
     * @param degrees the number of degrees to turn. Must be &le; 7103.
     */
    private void turnRight(int degrees) throws ConnectionLostException {
        countsToGoWheelRight = (int) (degrees * WHEEL_DISTANCE * ENCODER_COUNTS_PER_REVOLUTION
                / (360.0 * WHEEL_DIAMETER));
        countsToGoWheelLeft = countsToGoWheelRight;
        directionLeft = 1;
        directionRight = -1;
        recordEncodersAndDrive(directionLeft * TURN_SPEED, directionRight * TURN_SPEED);
        if (debug) {
            String msg = String.format("Turning right  L: %d  R: %d",
                    countsToGoWheelLeft, countsToGoWheelRight);
            Log.d(TAG, msg);
            dashboard.log(msg);
        }
    }

    /**
     * Turns in place leftwards. Note: Unexpected behavior may occur if degrees is
     * larger than 7103 degrees (a little less than 20 revolutions).
     *
     * @param degrees the number of degrees to turn. Must be &le; 7103.
     */
    private void turnLeft(int degrees) throws ConnectionLostException {
        countsToGoWheelRight = (int) (degrees * WHEEL_DISTANCE * ENCODER_COUNTS_PER_REVOLUTION
                / (360.0 * WHEEL_DIAMETER));
        countsToGoWheelLeft = countsToGoWheelRight;
        if (debug) {
            String msg = String.format("Turning left  L: %d  R: %d",
                    countsToGoWheelLeft, countsToGoWheelRight);
            Log.d(TAG, msg);
            dashboard.log(msg);
        }
        directionLeft = -1;
        directionRight = 1;
        recordEncodersAndDrive(directionLeft * TURN_SPEED, directionRight * TURN_SPEED);
    }

    private void recordEncodersAndDrive(int leftVelocity, int rightVelocity) throws ConnectionLostException {
        readSensors(SENSORS_GROUP_ID101);
        startLeft = getEncoderCountLeft();
        startRight = getEncoderCountRight();
        driveDirect(leftVelocity, rightVelocity);
    }


    /**
     * Checks if the last command has been completed.
     *
     * @return true if the last command has been completed
     * @throws ConnectionLostException
     */
    private boolean checkDone() throws ConnectionLostException {
        readSensors(SENSORS_GROUP_ID101);
        int countLeft = getEncoderCountLeft();
        int countRight = getEncoderCountRight();
        boolean done = false;
        int doneLeft = (directionLeft * (countLeft - startLeft)) & 0xFFFF;
        int doneRight = (directionRight * (countRight - startRight)) & 0xFFFF;
        if (debug) {
            String msg = String.format("L: %d  R: %d  azimuth: %.2f",
                    doneLeft, doneRight, dashboard.getAzimuth());
            dashboard.log(msg);
            Log.d(TAG, msg);
        }
        if (countsToGoWheelLeft <= doneLeft && doneLeft < 0x7FFF ||
                countsToGoWheelRight <= doneRight && doneRight < 0x7FFF) {
            driveDirect(0, 0);
            waitForCompleteStop();
            done = true;
        }
        return done;
    }

    private void waitForCompleteStop() throws ConnectionLostException {
        boolean done = false;
        int prevCountLeft = -1;
        int prevCountRight = -1;
        while (!done) {
            readSensors(SENSORS_GROUP_ID101);
            int countLeft = getEncoderCountLeft();
            int countRight = getEncoderCountRight();
            if (debug) {
                String msg = String.format("Stopping  L: %d  R: %d", countLeft, countRight);
                Log.d(TAG, msg);
                dashboard.log(msg);
            }
            if (prevCountLeft == countLeft && prevCountRight == countRight) {
                done = true;
            } else {
                prevCountLeft = countLeft;
                prevCountRight = countRight;
            }
        }
    }


}
