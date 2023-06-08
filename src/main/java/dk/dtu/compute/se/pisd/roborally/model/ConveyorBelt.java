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

import dk.dtu.compute.se.pisd.roborally.controller.GameController;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * ...
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 *
 */
public class ConveyorBelt extends FieldAction {

    private List<Player> visitedPlayers = new ArrayList<>();
    private Heading heading;

    private Space target;

    public ConveyorBelt(Heading heading) {
        this.heading = heading;
    }

    public Heading getHeading() {
        return heading;
    }

    public void setHeading(Heading heading) {
        this.heading = heading;
    }

    public void addVisitedPlayer(Player player) {
        visitedPlayers.add(player);
    }

    public boolean visitedByPlayer(Player player) {
        return visitedPlayers.contains(player);
    }

    public void removeVisitedPlayer(Player player) {
        visitedPlayers.remove(player);
    }

    public Space getTarget() {
        return target;
    };

    @Override
    public boolean doAction(@NotNull GameController gameController, @NotNull Space space) {
        target = gameController.board.getNeighbour(space, heading);
        boolean reachable = target != null;
        boolean notOccupied = target.getPlayer() == null;
        return notOccupied && reachable;
    }
}