/*
 * This file is part of the Illarion Client.
 *
 * Copyright © 2012 - Illarion e.V.
 *
 * The Illarion Client is free software: you can redistribute and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * The Illarion Client is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with the Illarion Client. If not,
 * see <http://www.gnu.org/licenses/>.
 */
package illarion.client;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;

import illarion.client.states.LoadingState;
import illarion.client.states.LoginState;
import illarion.client.states.PlayingState;

/**
 * This is the game Illarion. This class takes care for actually building up Illarion. It will maintain the different
 * states of the game and allow switching them.
 *
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public final class Game
        extends StateBasedGame {
    /**
     * The ID of the login state. This is one of the constants to use in order to switch the current state of the game.
     */
    public static final int STATE_LOGIN = 0;

    /**
     * The ID of the loading state. This state can be used in order to display the current loading progress.
     */
    public static final int STATE_LOADING = 1;

    /**
     * The ID of the playing state. This can be used in order to display the current game.
     */
    public static final int STATE_PLAYING = 2;

    /**
     * Create the game with the fitting title, showing the name of the application and its version.
     */
    public Game() {
        super(IllaClient.APPLICATION + " " + IllaClient.VERSION);
    }

    /**
     * Prepare the list of the game states. Using this class all states of the game are load up.
     */
    @Override
    public void initStatesList(GameContainer container)
            throws SlickException {
        addState(new LoginState(STATE_LOGIN));
        addState(new LoadingState(STATE_LOADING));
        addState(new PlayingState());
    }
}
