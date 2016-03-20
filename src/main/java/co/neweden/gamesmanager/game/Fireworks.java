package co.neweden.gamesmanager.game;

import co.neweden.gamesmanager.Game;
import co.neweden.gamesmanager.GamesManager;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class Fireworks {

    private Game game;

    public Fireworks(Game game) {
        this.game = game;
    }

    public void randomFireworks(final Location loc, final Entity entity, final Integer duration) {
        new BukkitRunnable() {
            Integer counter = duration;
            @Override
            public void run() {
                if (loc == null)
                    randomFirework(entity.getLocation());
                else
                    randomFirework(loc);
                if (counter > 0) {
                    counter--;
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(game.getPlugin(), 0L, 10L);
    }

    private void randomFirework(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        Random r = new Random();
        Integer rt = r.nextInt(5);
        FireworkEffect.Type type = FireworkEffect.Type.BALL;
        switch (rt) {
            case 0: type = FireworkEffect.Type.BALL; break;
            case 1: type = FireworkEffect.Type.BALL_LARGE; break;
            case 2: type = FireworkEffect.Type.BURST; break;
            case 3: type = FireworkEffect.Type.CREEPER; break;
            case 4: type = FireworkEffect.Type.STAR; break;
        }
        Color c1 = randomColor();
        Color c2 = randomColor();
        FireworkEffect effect = FireworkEffect.builder().flicker(r.nextBoolean()).withColor(c1).withFade(c2).with(type).trail(r.nextBoolean()).build();
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.addEffect(effect);
        fwm.setPower(r.nextInt(2) + 1);
        fw.setFireworkMeta(fwm);
    }

    private Color randomColor() {
        Random r = new Random();
        Integer rn = r.nextInt(17);
        Color color = Color.BLUE;
        switch (rn) {
            case 0: color = Color.WHITE; break;
            case 1: color = Color.SILVER; break;
            case 2: color = Color.GRAY; break;
            case 3: color = Color.BLACK; break;
            case 4: color = Color.RED; break;
            case 5: color = Color.MAROON; break;
            case 6: color = Color.YELLOW; break;
            case 7: color = Color.OLIVE; break;
            case 8: color = Color.LIME; break;
            case 9: color = Color.GREEN; break;
            case 10: color = Color.AQUA; break;
            case 11: color = Color.TEAL; break;
            case 12: color = Color.BLUE; break;
            case 13: color = Color.NAVY; break;
            case 14: color = Color.FUCHSIA; break;
            case 15: color = Color.PURPLE; break;
            case 16: color = Color.ORANGE; break;
        }
        return color;
    }

}
