/*
 * This file is part of the Illarion project.
 *
 * Copyright © 2015 - Illarion e.V.
 *
 * Illarion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Illarion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package illarion.client.world.movement;

import illarion.client.graphics.AnimatedMove;
import illarion.client.graphics.MoveAnimation;
import illarion.client.net.client.MoveCmd;
import illarion.client.net.client.TurnCmd;
import illarion.client.util.UpdateTask;
import illarion.client.world.CharMovementMode;
import illarion.client.world.MapTile;
import illarion.client.world.Player;
import illarion.client.world.World;
import illarion.client.world.characters.CharacterAttribute;
import illarion.common.graphics.CharAnimations;
import illarion.common.types.CharacterId;
import illarion.common.types.Direction;
import illarion.common.types.Location;
import illarion.common.util.FastMath;
import org.illarion.engine.GameContainer;
import org.illarion.engine.input.Input;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is the main controlling class for the movement. It maintains the references to the different handlers and
 * makes sure that the movement commands of the handlers are put in action.
 *
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public class Movement {
    @Nonnull
    private static final Logger log = LoggerFactory.getLogger(Movement.class);
    @Nonnull
    private static final Marker marker = MarkerFactory.getMarker("Movement");

    /**
     * The instance of the player that is moved around by this class.
     */
    @Nonnull
    private final Player player;

    /**
     * The currently active movement handler.
     */
    @Nullable
    private MovementHandler activeHandler;

    @Nonnull
    private CharMovementMode defaultMovementMode;

    private boolean stepInProgress;

    @Nonnull
    private final MoveAnimator animator;

    @Nonnull
    private final MouseMovementHandler followMouseHandler;

    @Nonnull
    private final KeyboardMovementHandler keyboardHandler;

    @Nonnull
    private final TargetMovementHandler targetMovementHandler;

    @Nonnull
    private final MouseTargetMovementHandler targetMouseMovementHandler;

    @Nonnull
    private final MoveAnimation moveAnimation;

    /**
     * This instance of the player location is kept in sync with the location that was last confirmed by the server
     * to keep track of where the player REALLY is.
     */
    @Nonnull
    private final Location playerLocation;

    public Movement(@Nonnull Player player, @Nonnull Input input, @Nonnull AnimatedMove movementReceiver) {
        this.player = player;
        moveAnimation = new MoveAnimation(movementReceiver);
        defaultMovementMode = CharMovementMode.Walk;
        stepInProgress = false;
        animator = new MoveAnimator(this, moveAnimation);
        moveAnimation.addTarget(animator, false);

        followMouseHandler = new FollowMouseMovementHandler(this, input);
        keyboardHandler = new SimpleKeyboardMovementHandler(this, input);
        targetMovementHandler = new WalkToMovementHandler(this);
        targetMouseMovementHandler = new WalkToMouseMovementHandler(this, input);
        playerLocation = new Location(player.getLocation());
    }

    /**
     * The location of the player as its known to the server.
     *
     * @return the server location of the player
     */
    @Nonnull
    public Location getServerLocation() {
        return playerLocation;
    }

    public boolean isMoving() {
        return moveAnimation.isRunning();
    }

    void activate(@Nonnull MovementHandler handler) {
        if (!isActive(handler)) {
            MovementHandler oldHandler = activeHandler;
            if (oldHandler != null) {
                oldHandler.disengage(false);
            }
            activeHandler = handler;
            log.debug(marker, "New movement handler is assuming control: {}", activeHandler);
        }
        update();
    }

    void disengage(@Nonnull MovementHandler handler) {
        if (isActive(handler)) {
            activeHandler = null;
        } else {
            log.debug(marker, "Tried to disengage a movement handler ({}) that was not active!", handler);
        }
    }

    boolean isActive(@Nonnull MovementHandler handler) {
        return (activeHandler != null) && handler.equals(activeHandler);
    }

    public void executeServerRespTurn(@Nonnull final Direction direction) {
        World.getUpdateTaskManager().addTask(new UpdateTask() {
            @Override
            public void onUpdateGame(@Nonnull GameContainer container, int delta) {
                executeServerRespTurnInternal(direction);
            }
        });
    }

    private void executeServerRespTurnInternal(@Nonnull Direction direction) {
        animator.scheduleTurn(direction);
    }

    private void scheduleEarlyTurn(@Nonnull Direction direction) {
        animator.scheduleTurn(direction);
    }

    public void executeServerRespMoveTooEarly() {
        World.getUpdateTaskManager().addTaskForLater(new UpdateTask() {
            @Override
            public void onUpdateGame(@Nonnull GameContainer container, int delta) {
                log.debug(
                        "Response indicates that the request was received too early. A new request is required later.");
                resendMoveToServer();
            }
        });
    }

    public void executeServerRespMove(
            @Nonnull final CharMovementMode mode, @Nonnull final Location target, final int duration) {
        final Location playerLocationBeforeMove = new Location(playerLocation);
        World.getUpdateTaskManager().addTask(new UpdateTask() {
            @Override
            public void onUpdateGame(@Nonnull GameContainer container, int delta) {
                executeServerRespMoveInternal(playerLocationBeforeMove, mode, target, duration);
            }
        });
        playerLocation.set(target);
    }

    private void executeServerRespMoveInternal(
            @Nonnull Location playerLocationBeforeMove,
            @Nonnull CharMovementMode mode,
            @Nonnull Location target,
            int duration) {
        log.debug("Received response from the server! Mode: {} Target {} Duration {}ms", mode, target, duration);
        if (playerLocationBeforeMove.equals(target)) {
            log.debug("Current location and target location match. Cancel any pending move.");
            animator.cancelMove(target);
        } else {
            // confirm a move that was started early
            animator.confirmMove(mode, target, duration);
        }
    }

    private static final int MAX_WALK_AGI = 20;
    private static final int MIN_WALK_COST = 300;
    private static final int MAX_WALK_COST = 800;

    private void scheduleEarlyMove(@Nonnull CharMovementMode mode, @Nonnull Direction direction) {
        if (player.getCarryLoad().isWalkingPossible()) {
            Location target = getTargetLocation(mode, direction);
            MapTile targetTile = World.getMap().getMapAt(target);

            if ((targetTile != null) && !targetTile.isBlocked()) {
                int agility = Math.min(player.getCharacter().getAttribute(CharacterAttribute.Agility), MAX_WALK_AGI);
                double agilityMod = (10 - agility) / 100.0;
                double loadMod = (player.getCarryLoad().getLoadFactor() / 10.0) * 3.0;
                double mods = agilityMod + loadMod + 1.0;

                int movementDuration = getMovementDuration(targetTile.getMovementCost(), mods, direction.isDiagonal(),
                                                           mode == CharMovementMode.Run);
                if (mode == CharMovementMode.Run) {
                    Location walkTarget = getTargetLocation(CharMovementMode.Walk, direction);
                    MapTile walkTargetTile = World.getMap().getMapAt(walkTarget);
                    if ((walkTargetTile != null) && !walkTargetTile.isBlocked()) {
                        movementDuration += getMovementDuration(walkTargetTile.getMovementCost(), mods,
                                                                direction.isDiagonal(), true);
                    } else {
                        return;
                    }
                }
                animator.scheduleEarlyMove(mode, target, (movementDuration / 100) * 100);
            }
        }
    }

    private static int getMovementDuration(int tileMovementCost, double mods, boolean diagonal, boolean running) {
        int movementDuration = FastMath.clamp((int) (tileMovementCost * 100.0 * mods), MIN_WALK_COST, MAX_WALK_COST);

        if (diagonal) {
            movementDuration = (int) (1.4142135623730951 * movementDuration); // sqrt(2)
        }
        if (running) {
            movementDuration = (int) (0.6 * movementDuration);
        }
        return movementDuration;
    }

    public void executeServerLocation(@Nonnull Location target) {
        animator.cancelAll();
        stepInProgress = false;
        World.getPlayer().setLocation(target);

        MovementHandler currentHandler = activeHandler;
        if (currentHandler != null) {
            currentHandler.disengage(false);
        }
        playerLocation.set(target);
    }

    @Nullable
    private MoveCmd lastSendMoveCommand;

    /**
     * Send the movement command to the server.
     *
     * @param direction the direction of the requested move
     * @param mode the mode of the requested move
     */
    private void sendMoveToServer(@Nonnull Direction direction, @Nonnull CharMovementMode mode) {
        CharacterId playerId = player.getPlayerId();
        if (playerId == null) {
            log.error(marker, "Send move to server while ID is not known.");
            return;
        }
        MoveCmd cmd = new MoveCmd(playerId, mode, direction);
        lastSendMoveCommand = cmd;
        log.debug(marker, "Sending move command to server: {}", cmd);
        World.getNet().sendCommand(cmd);
    }

    private void resendMoveToServer() {
        if (lastSendMoveCommand != null) {
            log.debug(marker, "Re-Sending move command to server: {}", lastSendMoveCommand);
            World.getNet().sendCommand(lastSendMoveCommand);
        } else {
            log.warn(marker, "Tried to resend a move command to the server, but there was no old move command.");
        }
    }

    private void sendTurnToServer(@Nonnull Direction direction) {
        if (player.getCharacter().getDirection() != direction) {
            TurnCmd cmd = new TurnCmd(direction);
            log.debug(marker, "Sending turn command to server: {}", cmd);
            World.getNet().sendCommand(cmd);
        }
    }

    /**
     * Notify the handler that everything is ready to request the next step from the server.
     */
    void reportReadyForNextStep() {
        log.debug("Reported ready for the next step.");
        stepInProgress = false;
        update();
    }

    /**
     * This function triggers the lifecycle run of the movement handler. Its executed automatically as the movement
     * handler sees fit. How ever for special events that might require a action of the movement handler this function
     * can be triggered.
     * <p/>
     * Calling it too often causes no harm. The handler checks internally if any actual operations need to be executed
     * or not.
     */
    public void update() {
        if (stepInProgress) {
            return;
        }
        MovementHandler handler = activeHandler;
        if (handler != null) {
            long start = System.currentTimeMillis();
            StepData nextStep = handler.getNextStep(playerLocation);
            if (log.isDebugEnabled(marker)) {
                log.debug(marker, "Requesting new step data from handler: {} (took {} milliseconds)", nextStep,
                          System.currentTimeMillis() - start);
            }
            if ((nextStep != null) && (nextStep.getDirection() != null)) {
                switch (nextStep.getMovementMode()) {
                    case None:
                        sendTurnToServer(nextStep.getDirection());
                        scheduleEarlyTurn(nextStep.getDirection());
                        break;
                    default:
                        stepInProgress = true;
                        sendMoveToServer(nextStep.getDirection(), nextStep.getMovementMode());
                        scheduleEarlyTurn(nextStep.getDirection());
                        scheduleEarlyMove(nextStep.getMovementMode(), nextStep.getDirection());
                }
            }
        }
    }

    @Nonnull
    @Contract(pure = true)
    private Location getTargetLocation(@Nonnull CharMovementMode mode, @Nonnull Direction direction) {
        Location result = new Location(playerLocation);
        switch (mode) {
            case Run:
                result.moveSC(direction);
                result.moveSC(direction);
                break;
            case Walk:
                result.moveSC(direction);
                break;
            case Push:
                result.moveSC(direction);
                break;
            default:
                throw new IllegalArgumentException("Invalid movement mode for selecting the target location");
        }
        return result;
    }

    @Nonnull
    @Contract(pure = true)
    Player getPlayer() {
        return player;
    }

    @Contract(pure = true)
    public boolean isMovementModePossible(@Nonnull CharMovementMode mode) {
        return (mode != CharMovementMode.Run) ||
                World.getPlayer().getCharacter().isAnimationAvailable(CharAnimations.RUN);
    }

    @Nonnull
    @Contract(pure = true)
    public CharMovementMode getDefaultMovementMode() {
        return defaultMovementMode;
    }

    @Nonnull
    @Contract(pure = true)
    public MouseMovementHandler getFollowMouseHandler() {
        return followMouseHandler;
    }

    @Nonnull
    @Contract(pure = true)
    public KeyboardMovementHandler getKeyboardHandler() {
        return keyboardHandler;
    }

    @Nonnull
    @Contract(pure = true)
    public TargetMovementHandler getTargetMovementHandler() {
        return targetMovementHandler;
    }

    @Nonnull
    @Contract(pure = true)
    public MouseTargetMovementHandler getTargetMouseMovementHandler() {
        return targetMouseMovementHandler;
    }

    public void setDefaultMovementMode(@Nonnull CharMovementMode defaultMovementMode) {
        this.defaultMovementMode = defaultMovementMode;
    }

    public void shutdown() {
        activeHandler = null;
    }
}
