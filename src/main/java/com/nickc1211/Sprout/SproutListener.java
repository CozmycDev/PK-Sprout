package com.nickc1211.Sprout;

import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;

public class SproutListener implements Listener {

    @EventHandler
    public void onClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (event.isCancelled() || bPlayer == null) return;

        if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Sprout")) {
            new Sprout(player);
        }
    }
}
