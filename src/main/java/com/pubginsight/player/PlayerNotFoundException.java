package com.pubginsight.player;

public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(String playerName) {
        super("No PUBG player found with name '" + playerName + "'");
    }
}
