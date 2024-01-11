package beastmode;

import battlecode.common.*;
import beastmode.before_specialists.*;
import beastmode.after_specialists.*;
import beastmode.either_specialists.*;

import java.util.Arrays;
import java.util.Random;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {
    static int turnCount = 0;
    static final MapLocation NONELOCATION = new MapLocation(-1, -1);

    static int localID;
    static boolean lefty = true; // do u pathfind favouring left first or right first
    static boolean firstRoundFlagBearer = false;
    static final int maxNumberOfScout = 30;
    static final int maxNumberOfBuilder = 20;

    // before divider drop
    static boolean isScout = false;

    // after divider drop
    static boolean isBomber = false;
    static boolean isFlagrunner = false;

    // either
    static boolean isBuilder = false;
    static boolean isCommander = false;
    static Utility.CoolRobotInfo[] coolRobotInfoArray = new Utility.CoolRobotInfo[50];

    static final Random rng = new Random(6147);

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };


    /**
     * SHARED ARRAY
     * [0,          1-50,          51,  52,  53,         54,           55]
     * id     id's of all ducks    3 spawn loc's    # of scouts   # of builders
     *
     */

    static final int assigningLocalIDIndex = 0;
    static final int spawnLocCenterOneIndex = 51;
    static final int spawnLocCenterTwoIndex = 52;
    static final int spawnLocCenterThreeIndex = 53;
    static final int numberOfScoutsIndex = 54;
    static final int numberOfBuildersIndex = 55;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // Create objects for the other files
        Movement movement = new Movement(rc, lefty);
        Utility util = new Utility(rc);

        // before strategies
        Scout scout = new Scout(rc, movement, util);

        // after strategies
        Bomber bomber = new Bomber(rc, movement, util);
        Flagrunner flagrunner = new Flagrunner(rc, movement, util);

        // either strategies
        Unspecialized unspecialized = new Unspecialized(rc, movement, util);
        Builder builder = new Builder(rc, movement, util);
        Commander commander = new Commander(rc, movement, util);

        while (true) {
            turnCount += 1;  // We have now been alive for one more turn!
            try {
                if (rc.getRoundNum() == 1) {
                    localID = util.makeLocalID(assigningLocalIDIndex);
                    movement.setLefty((localID % 2) == 1);
                }

                if (turnCount == 1) {
                    rc.setIndicatorString("I am robot #" + localID);
                }

                if (!rc.isSpawned()) {
                    util.trySpawning();
                }
                if (rc.isSpawned()) {

                    // ----------------------------------------
                    // start of turn logic
                    // ----------------------------------------

                    // read every other robots info from the shared array, store it in coolRobotInfoArray
                    if (rc.getRoundNum() > 1) { // not all bots have written their stuff into their index until round 2 starts
                        for (int i = 1; i <= 50; i++) {
                            int coolRobotInfoInt = rc.readSharedArray(i);
                            coolRobotInfoArray[i-1] = util.new CoolRobotInfo(i, coolRobotInfoInt);
                        }
                    }

                    // testing
                    if (rc.getRoundNum() == 2) {
                        rc.setIndicatorString("heheheha, localID= " + localID);
                        if (localID == 10) {
                            System.out.println("coolRobot: " + Arrays.toString(coolRobotInfoArray));
                            rc.setIndicatorString("I am robot #" + localID + " Duck 50 is at the coords " + coolRobotInfoArray[50-1].curLocation.toString() + ".");
                        }
                    }


                    // ----------------------------------------
                    // logic for who will specialize to what (subject to change idrk what im doing ong no cap on 4nem)
                    // ----------------------------------------

                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        int amountOfScouts = rc.readSharedArray(numberOfScoutsIndex);
                        int amountOfBuilders = rc.readSharedArray(numberOfBuildersIndex);
                        if (amountOfScouts < maxNumberOfScout) {
                            isScout = true;
                            rc.writeSharedArray(numberOfScoutsIndex, amountOfScouts + 1);
                        } else if (amountOfBuilders < maxNumberOfBuilder) {
                            isBuilder = false;
                            rc.writeSharedArray(numberOfBuildersIndex, amountOfBuilders + 1);
                        }
                    }

                    if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
                        // set all the before divider specializations to false
                        isScout = false;
                    }

                    // ----------------------------------------
                    // big switch statement thing for what strategy the robot will run for this turn
                    // ----------------------------------------

                    if (rc.getRoundNum() <= GameConstants.SETUP_ROUNDS) { // before divider drop strategies
                        if (isCommander) { // no commanders rn
                            commander.run();
                        } else if (isScout) { // rn we make 30 scouts
                            scout.run();
                        } else if (isBuilder) { // and 20 builders, so all 50 ducks get specialized, so none hit the default case
                            builder.run();
                        } else { // none unspecialized rn (all taken up to be scouts or builders)
                            unspecialized.run();
                        }
                    } else { // after divider drop strategies
                        if (isCommander) {
                            commander.run();
                        } else if (isBomber) { // none rn
                            bomber.run();
                        } else if (isBuilder) { // carry over of 20 builders assigned before divider drop
                            builder.run();
                        } else if (isFlagrunner){ // so 30 bots switch from scout to unspecialized
                            flagrunner.run();
                        } else {
                            unspecialized.run();
                        }
                    }

                    // ----------------------------------------
                    // end of turn stuff
                    // ----------------------------------------

                }

                // after every round whether spawned or not, convert your info to an int and write it to the shared array
                MapLocation locationToStore;
                if (rc.isSpawned()) locationToStore = rc.getLocation();
                else locationToStore = NONELOCATION; // NONELOCATION (-1, -1) represents not spawned rn (no location)
                int coolRobotInfoInt = util.convertRobotInfoToInt(locationToStore, false);
//                if (rc.getRoundNum() == 1) System.out.println("id: " + localID + "coolRobotInfoInt: " + Integer.toBinaryString(coolRobotInfoInt));
                rc.writeSharedArray(localID, coolRobotInfoInt);

            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    public static MapLocation findClosestEnemy(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        MapLocation closestEnemy = null;
        if (enemies.length != 0) {
            rc.setIndicatorString("There are nearby enemy robots! Scary!");
            for (RobotInfo enemy : enemies) {
                MapLocation enemyLoc = enemy.getLocation();
                if (closestEnemy == null) {
                    closestEnemy = enemyLoc;
                }
                else if (rc.getLocation().distanceSquaredTo(enemyLoc) < rc.getLocation().distanceSquaredTo(closestEnemy)) {
                    closestEnemy = enemyLoc;
                }
            }
        }
        return closestEnemy;
    }

    public static void tryToHeal(RobotController rc) throws GameActionException {
        if (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT) {
            if (rc.getHealth() < 1000 && rc.canHeal(rc.getLocation())) rc.heal(rc.getLocation());
            else {
                RobotInfo[] teammies = rc.senseNearbyRobots(-1, rc.getTeam());
                for (RobotInfo teammie : teammies) {
                    if (rc.getHealth() < 1000 && rc.canHeal(teammie.getLocation())) rc.heal(teammie.getLocation());
                }
            }
        }
    }
}
