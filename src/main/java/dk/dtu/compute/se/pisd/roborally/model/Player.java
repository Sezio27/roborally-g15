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
package dk.dtu.compute.se.pisd.roborally.model;

import com.google.gson.annotations.Expose;
import dk.dtu.compute.se.pisd.designpatterns.observer.Observer;
import dk.dtu.compute.se.pisd.designpatterns.observer.Subject;
import org.jetbrains.annotations.NotNull;

import static dk.dtu.compute.se.pisd.roborally.model.Heading.SOUTH;

/**
 * Represents a player in the game. The player's movements and actions are driven by CommandCard objects and FieldActions.
 *
 * @author Jakob Jacobsen, s204502
 */
public class Player extends Subject {

    final public static int NO_REGISTERS = 5;
    final public static int NO_CARDS = 8;

    final public Board board;
    @Expose
    private String name;
    @Expose
    private String color;
    @Expose
    private Space space;
    @Expose
    private Heading heading = SOUTH;

    @Expose
    private CommandCardField[] program;
    @Expose
    private CommandCardField[] cards;
    @Expose
    private int currentCheckpoint;
    @Expose
    private Space spawnSpace;
    @Expose
    private boolean rebooting;
    /**
     * Constructs a new Player.
     *
     * @param board the board this player is playing on
     * @param color the color assigned to this player
     * @param name  the name of this player
     */
    public Player(@NotNull Board board, String color, @NotNull String name) {
        this.board = board;
        this.name = name;
        this.color = color;
        this.space = null;
        this.currentCheckpoint = 0;
        this.spawnSpace = null;
        this.rebooting = false;


        program = new CommandCardField[NO_REGISTERS];
        for (int i = 0; i < program.length; i++) {
            program[i] = new CommandCardField(this);
        }

        cards = new CommandCardField[NO_CARDS];
        for (int i = 0; i < cards.length; i++) {
            cards[i] = new CommandCardField(this);
        }
    }

    public String getName() {
        return name;
    }

    public void setCurrentCheckpoint(int n) {currentCheckpoint = n;}

    public void setSpawnSpace(Space spawnSpace) {
        this.spawnSpace = spawnSpace;
    }
    /**
     * Returns the player's current spawn location.
     * @return a Space object representing the player's spawn location
     */
    public Space getSpawnSpace() {
        return spawnSpace;
    }

    public void setName(String name) {
        if (name != null && !name.equals(this.name)) {
            this.name = name;
            notifyChange();
            if (space != null) {
                space.playerChanged();
            }
        }
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
        notifyChange();
        if (space != null) {
            space.playerChanged();
        }
    }

    public Space getSpace() {
        return space;
    }
    /**
     * Respawns the player at their spawn location if it is unoccupied.
     *
     */
    public void respawn() {
        if (spawnSpace == null || spawnSpace.getPlayer() != null) {
            System.out.println("Error spawning"); //exception..
            return;
        }
        Space oldSpace = this.space;
        oldSpace.setPlayer(null);
        space = spawnSpace;
        space.setPlayer(this);
        notifyChange();

    }
    /**
     * Sets the space for this player and updates the player reference in the old and new space accordingly.
     *
     * It also ensures that the old space no longer references this player,
     * and the new space now references this player.
     *
     * @param space the new space to be set for this player
     */
    public void setSpace(Space space) {
        Space oldSpace = this.space;
        if (space != oldSpace &&
                (space == null || space.board == this.board)) {
            this.space = space;
            if (oldSpace != null) {
                oldSpace.setPlayer(null);
            }
            if (space != null) {
                space.setPlayer(this);
            }
            notifyChange();
        }
    }

    public Heading getHeading() {
        return heading;
    }
    /**
     * Sets the current heading (direction) of the player.
     * @param heading the new heading of the player
     */
    public void setHeading(@NotNull Heading heading) {
        if (heading != this.heading) {
            this.heading = heading;
            notifyChange();
            if (space != null) {
                space.playerChanged();
            }
        }
    }

    public void setRebooting(boolean rebooting) {
        this.rebooting = rebooting;
    }
    /**
     * Returns whether the player is currently rebooting or not.
     * A rebooting player is temporarily inactive and will become active again after the last register round.
     * @return true if the player is rebooting, false otherwise
     */
    public boolean isRebooting() {
        return rebooting;
    }

    public CommandCardField getProgramField(int i) {
        return program[i];
    }
    /**
     * Assigns a CommandCardField to a specific register in the player's program.
     * This represents a command card slot in the player's program area.
     * @param i the index of the register to assign the command card field to
     * @param field the CommandCardField to be assigned
     */
    public void setProgramField(int i, CommandCardField field) {
        program[i].setCard(field.getCard());
        program[i].setVisible(field.isVisible());
        notifyChange();
    }

    public CommandCardField getCardField(int i) {
        return cards[i];

    }

    public void setCardField(int i, CommandCardField field) {
        cards[i].setCard(field.getCard());
        notifyChange();
    }


    public int getCurrentCheckpoint() {
        return currentCheckpoint;
    }

    public void updateCheckpoint() {
        currentCheckpoint++;
    }

}
