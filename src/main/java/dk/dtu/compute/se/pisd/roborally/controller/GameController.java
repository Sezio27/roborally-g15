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
 */
public class GameController {

    final public Board board;

    public GameController(@NotNull Board board) {
        this.board = board;
    }

    public void initializeGame(int maxPlayers) {
        board.setStartSpacesDefault(maxPlayers);
        for (int i = 0; i < maxPlayers; i++) {
            Player player = board.getPlayer(i);
            Space startSpace = board.getStartSpaces()[i];
            player.setSpace(startSpace);
            player.setSpawnSpace(startSpace);
        }

    }

    public void moveCurrentPlayerToSpace(@NotNull Space space) {

        if (space != null && space.board == board) {
            Player currentPlayer = board.getCurrentPlayer();
            if (currentPlayer != null && space.getPlayer() == null) {
                currentPlayer.setSpace(space);
                int playerNumber = (board.getPlayerNumber(currentPlayer) + 1) % board.getPlayersNumber();
                board.setCurrentPlayer(board.getPlayer(playerNumber));
            }
        }

    }

    public void updateCheckpoint(@NotNull Player player, Space space, int number) {

        System.out.println(player.getName() + " - new checkpoint: " + player.getCurrentCheckpoint());
        if (board.getNumberOfCheckpoints() == number) {
            handleWin(player);
        }
        player.updateCheckpoint();
        player.setSpawnSpace(space);

    }

    public void handleWin(@NotNull Player player) {
        System.out.println(player.getName() + " has won!");
    }

    public void startProgrammingPhase() {
        board.setPhase(Phase.PROGRAMMING);
        board.setCurrentPlayer(board.getPlayer(0));
        board.setStep(0);

        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player player = board.getPlayer(i);
            if (player.isRebooting()) {
                player.setSpace(player.getSpawnSpace());
                player.setRebooting(false);
            }
            if (player != null) {
                for (int j = 0; j < Player.NO_REGISTERS; j++) {
                    CommandCardField field = player.getProgramField(j);
                    field.setCard(null);
                    field.setVisible(true);
                }
                for (int j = 0; j < Player.NO_CARDS; j++) {
                    CommandCardField field = player.getCardField(j);
                    if(field.getCard() == null)
                        field.setCard(generateRandomCommandCard());
                    field.setVisible(true);
                }
            }
        }
    }

    private CommandCard generateRandomCommandCard() {
        Command[] commands = Command.values();
        int random = (int) (Math.random() * commands.length);
        return new CommandCard(commands[random]);
    }

    public void finishProgrammingPhase() {
        makeProgramFieldsInvisible();
        makeProgramFieldsVisible(0);
        board.setPhase(Phase.ACTIVATION);
        board.setCurrentPlayer(board.getPlayer(0));
        board.setStep(0);
    }

    private void makeProgramFieldsVisible(int register) {
        if (register >= 0 && register < Player.NO_REGISTERS) {
            for (int i = 0; i < board.getPlayersNumber(); i++) {
                Player player = board.getPlayer(i);
                CommandCardField field = player.getProgramField(register);
                field.setVisible(true);
            }
        }
    }

    private void makeProgramFieldsInvisible() {
        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player player = board.getPlayer(i);
            for (int j = 0; j < Player.NO_REGISTERS; j++) {
                CommandCardField field = player.getProgramField(j);
                field.setVisible(false);
            }
        }
    }

    public void executePrograms() {
        board.setStepMode(false);
        continuePrograms();
    }

    public void executeStep() {
        board.setStepMode(true);
        continuePrograms();
    }

    private void continuePrograms() {
        do {
            executeNextStep();
        } while (board.getPhase() == Phase.ACTIVATION && !board.isStepMode());
    }

    private void handleConveyorMove(List<Player> players) {

        Map<Space, List<Player>> destinationMap = new HashMap<>();

        //Determine all final destinations of players on conveyor belts
        for (Player player : players) {
            Space source = player.getSpace();
            ConveyorBelt belt = source.getActionType(ConveyorBelt.class);

            if (belt.doAction(this, source)) {
                Space destination = belt.getTarget();
                destinationMap.computeIfAbsent(destination, k -> new ArrayList<>()).add(player);
            }
        }

        //Only move players with unique destinations
        destinationMap.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 1)
                .forEach(entry -> {
                    Player player = entry.getValue().get(0);
                    Space destination = entry.getKey();
                    player.setSpace(destination);
                });

    }


    private void executeFieldActions() {

        List<Player> playersOnConveyors = new ArrayList<>();

        for (Player player : board.getPlayers()) {
            Space space = player.getSpace();
            if (space != null) {
                FieldAction action = space.getAction();
                if (action != null) {


                    //Special case for conveyors, must be handled separately
                    if (action instanceof ConveyorBelt) {
                        playersOnConveyors.add(player);
                    } else action.doAction(this, space);

                }
            }

        }

        if (!playersOnConveyors.isEmpty()) handleConveyorMove(playersOnConveyors);
    }

    private void executeNextStep() {

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

                finishCommand();


            } else {
                // this should not happen
                assert false;
            }
        } else {
            // this should not happen
            assert false;
        }
    }

    private void finishCommand() {
        int nextPlayerNumber = board.getPlayerNumber(board.getCurrentPlayer()) + 1;
        if (nextPlayerNumber < board.getPlayersNumber()) {
            board.setCurrentPlayer(board.getPlayer(nextPlayerNumber));
        } else {
            int nextStep = board.getStep() + 1;
            if (nextStep < Player.NO_REGISTERS) {
                executeFieldActions();
                makeProgramFieldsVisible(nextStep);
                board.setStep(nextStep);
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
            finishCommand();

            //Automatic execution continues after interactive map has been executed
            if (!board.isStepMode() && board.getPhase() == Phase.ACTIVATION) {
                continuePrograms();
            }
        }
    }

    public void moveForward(@NotNull Player player, Heading heading) {
        if (player.board == board) {
            Space source = player.getSpace();
            Space destination = board.getNeighbour(source, heading);

            try {
                int moveCount = 1; // Used to avoid possible cyclic infinite recursion
                moveToSpace(player, source, destination, heading, moveCount);
            } catch (ImpossibleMoveException e) {
                System.out.println(e);
            }

        }
    }

    // *Include in report*
    private void moveToSpace(@NotNull Player player, @NotNull Space source, Space destination, @NotNull Heading heading, int moveCount) throws ImpossibleMoveException {
        //Or a pit
        if (destination == board.getDeadSpace()) {
            handleReboot(player);
            return;
        };

        if (source.getWalls().contains(heading) || destination.getWalls().contains(heading.opposing())) return;

        Player other = destination.getPlayer();
        if (other != null) {

            Space otherDestination = board.getNeighbour(destination, heading);
            if (otherDestination == board.getDeadSpace()) {
                handleReboot(other);
                player.setSpace(destination);
                return;
            }

            if (moveCount <= board.getPlayersNumber() && !otherDestination.getWalls().contains(heading.opposing())) {

                moveToSpace(other, destination, otherDestination, heading, moveCount + 1);

            } else {
                throw new ImpossibleMoveException(player, destination, heading);
            }
        }

        player.setSpace(destination);

    }

    private void handleReboot(@NotNull Player player) {

        player.setRebooting(true);
        player.setSpace(board.getDeadSpace());

        int i = player == board.getCurrentPlayer() ? board.getStep() + 1: board.getStep();

        while (i < Player.NO_REGISTERS ) {
            CommandCardField field = player.getProgramField(i);
            field.setCard(null);
            i++;
        }

        for (int j = 0; j < Player.NO_CARDS; j++) {
            CommandCardField field = player.getCardField(j);
            field.setCard(null);
            field.setVisible(true);
        }



    }

    public void fastForward(@NotNull Player player) {
        moveForward(player, player.getHeading());
        moveForward(player, player.getHeading());
    }

    public void turnRight(@NotNull Player player) {
        if (player != null && player.board == board) {
            player.setHeading(player.getHeading().next());
        }
    }

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
