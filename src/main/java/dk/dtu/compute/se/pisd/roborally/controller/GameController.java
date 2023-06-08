/*
 *  This file is part of the initial project provided for the
 *  course "Project in Software Development (02362)" held at
 *  DTU Compute at the Technical University of Denmark.
 *
 *  Copyright (C) 2019, 2020: Ekkart Kindler, ekki@dtu.dk
 *
 *  This software is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; version 2 of the License.
 *
 *  This project is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this project; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package dk.dtu.compute.se.pisd.roborally.controller;

import dk.dtu.compute.se.pisd.roborally.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ...
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 *
 */
public class GameController {

    final public Board board;
    private Map<Space, List<Space>> conveyorPaths = new HashMap<>();
    private boolean stepFinished = false;

    public GameController(@NotNull Board board) {
        this.board = board;
        calculateConveyorPaths();
        printTest1();
    }

    private void printTest1() {
        for (int i = 0; i < conveyorPaths.size(); i++) {
            Space key = (Space) conveyorPaths.keySet().toArray()[i];
            System.out.println(i);
            System.out.println("Source: (" + key.x + ", " + key.y + ")");
            System.out.print("Path:");
            for (Space space : conveyorPaths.get(key)) {
                System.out.print(" (" + space.x + ", " + space.y + ")");
            }
            System.out.println();
            System.out.println();
        }
    }

    /**
     * This is just some dummy controller operation to make a simple move to see something
     * happening on the board. This method should eventually be deleted!
     *
     * @param space the space to which the current player should move
     */
    public void moveCurrentPlayerToSpace(@NotNull Space space)  {
        // TODO Assignment V1: method should be implemented by the students:
        //   - the current player should be moved to the given space
        //     (if it is free()
        //   - and the current player should be set to the player
        //     following the current player
        //   - the counter of moves in the game should be increased by one
        //     if the player is moved

        if (space != null && space.board == board) {
            Player currentPlayer = board.getCurrentPlayer();
            if (currentPlayer != null && space.getPlayer() == null) {
                currentPlayer.setSpace(space);
                int playerNumber = (board.getPlayerNumber(currentPlayer) + 1) % board.getPlayersNumber();
                board.setCurrentPlayer(board.getPlayer(playerNumber));
            }
        }

    }

    // XXX: V2
    public void startProgrammingPhase() {
        board.setPhase(Phase.PROGRAMMING);
        board.setCurrentPlayer(board.getPlayer(0));
        board.setStep(0);

        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player player = board.getPlayer(i);
            if (player != null) {
                for (int j = 0; j < Player.NO_REGISTERS; j++) {
                    CommandCardField field = player.getProgramField(j);
                    field.setCard(null);
                    field.setVisible(true);
                }
                for (int j = 0; j < Player.NO_CARDS; j++) {
                    CommandCardField field = player.getCardField(j);
                    field.setCard(generateRandomCommandCard());
                    field.setVisible(true);
                }
            }
        }
    }

    // XXX: V2
    private CommandCard generateRandomCommandCard() {
        Command[] commands = Command.values();
        int random = (int) (Math.random() * commands.length);
        return new CommandCard(commands[random]);
    }

    // XXX: V2
    public void finishProgrammingPhase() {
        makeProgramFieldsInvisible();
        makeProgramFieldsVisible(0);
        board.setPhase(Phase.ACTIVATION);
        board.setCurrentPlayer(board.getPlayer(0));
        board.setStep(0);
    }

    // XXX: V2
    private void makeProgramFieldsVisible(int register) {
        if (register >= 0 && register < Player.NO_REGISTERS) {
            for (int i = 0; i < board.getPlayersNumber(); i++) {
                Player player = board.getPlayer(i);
                CommandCardField field = player.getProgramField(register);
                field.setVisible(true);
            }
        }
    }

    // XXX: V2
    private void makeProgramFieldsInvisible() {
        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player player = board.getPlayer(i);
            for (int j = 0; j < Player.NO_REGISTERS; j++) {
                CommandCardField field = player.getProgramField(j);
                field.setVisible(false);
            }
        }
    }

    // XXX: V2
    public void executePrograms() {
        board.setStepMode(false);
        continuePrograms();
    }

    // XXX: V2
    public void executeStep() {
        board.setStepMode(true);
        continuePrograms();
    }

    // XXX: V2
    private void continuePrograms() {
        do {
            executeNextStep();
        } while (board.getPhase() == Phase.ACTIVATION && !board.isStepMode());
    }

    private List<Space> calculateConveyorPath(Player player, Space currentSpace, List<Space> pathAccumulator) {

        pathAccumulator.add(currentSpace);

        ConveyorBelt currentBelt = currentSpace.getConveyorBelt();

        //If we reached the space at the end of the conveyor belt
        if (currentBelt == null) {
            return pathAccumulator;
        }

        //If belt has been visited by player then no action
        if (currentBelt.visitedByPlayer(player)) {
            return pathAccumulator;
        }

        // If action is possible recursively check target space
        if (!currentBelt.doAction(this, currentSpace)) {
            return pathAccumulator;
        }
        return calculateConveyorPath(player, currentBelt.getTarget(), pathAccumulator);

    }

    // Include in report
    private void handleConveyorMove(List<Player> players) {

        Map<Player, List<Space>> pathMap = new HashMap<>();

        // Get path for each player
        for (Player player : players) {
            List<Space> path = calculateConveyorPath(player, player.getSpace(), new ArrayList<>());
            pathMap.put(player, path);
        }

        // Iterate over the entries of pathMap
        for (Map.Entry<Player, List<Space>> entry : pathMap.entrySet()) {
            Player player = entry.getKey();
            List<Space> path = entry.getValue();
            Space finalDestination = path.get(path.size() - 1);

            // Check if this player's final destination is unique
            boolean isUnique = pathMap.values().stream()
                    .filter(spaces -> spaces.get(spaces.size() - 1).equals(finalDestination))
                    .count() == 1;

            // Update player's position
            if (isUnique) {

                player.setSpace(finalDestination);

                // Mark all conveyor spaces on path as visited, under the assumption that at most only the end space could not be a conveyor
                if (finalDestination.getConveyorBelt() == null) {
                    path.remove(finalDestination);
                }
                for (Space space : path) {
                    space.getConveyorBelt().addVisitedPlayer(player);
                }
            }
        }

    }


    //** Use in report**
    private void executeFieldActions() {

        List<Player> playersOnConveyors = new ArrayList<>();

        for (Player player: board.getPlayers()) {
            Space space = player.getSpace();
            List<FieldAction> fieldActions = space.getActions();

            if (!fieldActions.isEmpty()) {

                for (FieldAction action : fieldActions) {

                    if (action instanceof ConveyorBelt) {
                        if (!((ConveyorBelt) action).visitedByPlayer(player)) playersOnConveyors.add(player);
                    }

                    if (action.doAction(this, space)) {
                        //Add other action cases here

                    }
                }
            }
        }

        if (!playersOnConveyors.isEmpty()) handleConveyorMove(playersOnConveyors);
    }

    // XXX: V2
    private void executeNextStep() {
        //** Use in report **
        if (stepFinished) executeFieldActions();



        Player currentPlayer = board.getCurrentPlayer();
        if (board.getPhase() == Phase.ACTIVATION && currentPlayer != null) {
            int step = board.getStep();
            if (step >= 0 && step < Player.NO_REGISTERS) {
                CommandCard card = currentPlayer.getProgramField(step).getCard();
                if (card != null) {
                    Command command = card.command;

                    if (command.isInteractive()) {
                        board.setPhase(Phase.PLAYER_INTERACTION);
                        return;
                    }
                    executeCommand(currentPlayer, command);
                }

                moveToNextProgramCard(currentPlayer);

            } else {
                // this should not happen
                assert false;
            }
        } else {
            // this should not happen
            assert false;
        }
    }

    private void moveToNextProgramCard(@NotNull Player currentPlayer) {
        int nextPlayerNumber = board.getPlayerNumber(currentPlayer) + 1;
        if (nextPlayerNumber < board.getPlayersNumber()) {
            stepFinished = false;
            board.setCurrentPlayer(board.getPlayer(nextPlayerNumber));
        } else {
            stepFinished = true;
            int step = board.getStep();
            step++;
            if (step < Player.NO_REGISTERS) {
                makeProgramFieldsVisible(step);
                board.setStep(step);
                board.setCurrentPlayer(board.getPlayer(0));
            } else {
                startProgrammingPhase();
            }
        }
    }

    // XXX: V2
    private void executeCommand(@NotNull Player player, Command command) {
        if (player != null && player.board == board && command != null) {

            switch (command) {
                case FORWARD:
                    this.moveForward(player, player.getHeading());
                    break;
                case RIGHT:
                    this.turnRight(player);
                    break;
                case LEFT:
                    this.turnLeft(player);
                    break;
                case FAST_FORWARD:
                    this.fastForward(player);
                    break;
                default:
                    // DO NOTHING (for now)
            }
        }
    }

    public void executeCommandOptionAndContinue(@NotNull Player player, Command option) {

        if (player.board == board && player == board.getCurrentPlayer()) {
            board.setPhase(Phase.ACTIVATION);
            executeCommand(player, option);
            moveToNextProgramCard(player);


            //Automatic execution continues after interactive map has been executed
            if (!board.isStepMode() && board.getPhase() == Phase.ACTIVATION) {
                continuePrograms();
            }
        }
    }

    // TODO: V2
    public void moveForward(@NotNull Player player, Heading heading) {
        if (player.board == board) {
            Space source = player.getSpace();
            Space destination = board.getNeighbour(source, heading);

            if (destination != null) {
                try {
                    moveToSpace(player, source, destination, heading, 1);
                } catch (ImpossibleMoveException e) {
                    System.out.println(e);
                }

            }
        }
    }


    // *Include in report*
    private void moveToSpace(@NotNull Player player, @NotNull Space source, @NotNull Space destination, @NotNull Heading heading, int moveCount) throws ImpossibleMoveException {
        assert board.getNeighbour(player.getSpace(), heading) == destination; // make sure the move to here is possible in principle


        if (source.getWalls().contains(heading) || destination.getWalls().contains(heading.opposing())) return;

        Player other = destination.getPlayer();
        if (other != null ){

            Space otherDestination = board.getNeighbour(destination, heading);
            if (moveCount >= board.getPlayersNumber() || otherDestination != null && !otherDestination.getWalls().contains(heading.opposing())) {
                // XXX Note that there might be additional problems with
                //     infinite recursion here (in some special cases)!
                //     We will come back to that!
                moveToSpace(other, destination, otherDestination, heading, moveCount+1);


                assert otherDestination.getPlayer() == null : otherDestination; // make sure target is free now
            } else {
                throw new ImpossibleMoveException(player, destination, heading);
            }
        }
        player.setSpace(destination);
    }

    // TODO: V2
    public void fastForward(@NotNull Player player) {
        moveForward(player, player.getHeading());
        moveForward(player, player.getHeading());
    }

    // TODO: V2
    public void turnRight(@NotNull Player player) {
        if (player != null && player.board == board) {
            player.setHeading(player.getHeading().next());
        }
    }

    // TODO: V2
    public void turnLeft(@NotNull Player player) {
        if (player != null && player.board == board) {
            player.setHeading(player.getHeading().prev());
        }
    }

    public boolean moveCards(@NotNull CommandCardField source, @NotNull CommandCardField target) {
        CommandCard sourceCard = source.getCard();
        CommandCard targetCard = target.getCard();
        if (sourceCard != null && targetCard == null) {
            target.setCard(sourceCard);
            source.setCard(null);
            return true;
        } else {
            return false;
        }
    }


    // To be deleted
    private void calculateConveyorPaths() {

        // For each space that has a conveyor belt, calculate the conveyor path
        Arrays.stream(board.getSpaces())
                .flatMap(Arrays::stream)
                .filter(space ->
                        space.getActions().stream().anyMatch(action -> action instanceof ConveyorBelt))
                .forEach(space -> {

                    if (conveyorPaths.containsKey(space)) return;

                    List<Space> path = new ArrayList<>();

                    ConveyorBelt belt = space.getConveyorBelt();

                    Space target = board.getNeighbour(space, belt.getHeading());

                    //Build path
                    path.add(target);
                    while (target.getConveyorBelt() != null) {
                        ConveyorBelt destConveyor = target.getConveyorBelt();
                        target = board.getNeighbour(target, destConveyor.getHeading());
                        path.add(target);
                    }

                    conveyorPaths.put(space, path);

                    IntStream.range(1, path.size() - 1)
                            .forEach(i -> conveyorPaths.put(path.get(i), path.subList(i + 1, path.size())));

                });
    }



    /**
     * A method called when no corresponding controller operation is implemented yet. This
     * should eventually be removed.
     */
    public void notImplemented() {
        // XXX just for now to indicate that the actual method is not yet implemented
        assert false;
    }

    class ImpossibleMoveException extends Exception {

        private Player player;
        private Space space;
        private Heading heading;

        public ImpossibleMoveException(Player player, Space space, Heading heading) {
            super("Move impossible");
            this.player = player;
            this.space = space;
            this.heading = heading;
        }
    }

}
