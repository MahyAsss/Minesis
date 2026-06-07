package com.minesis.ai;

import net.minecraft.world.entity.ai.goal.Goal;

import com.minesis.entity.MinesisEntity;

/**
 * Makes Minesis perform sneaking "dance" moves without blocking movement
 */
public class MinesisDanceGoal extends Goal {
    private final MinesisEntity minesis;
    private int danceTimer = 0;

    public MinesisDanceGoal(MinesisEntity minesis) {
        this.minesis = minesis;
    }

    @Override
    public boolean canUse() {
        // Dance rarely and very briefly
        return this.minesis.getRandom().nextInt(200) == 0;
    }

    @Override
    public boolean isInterruptable() {
        // Allow other goals to interrupt dance immediately
        return true;
    }

    @Override
    public void tick() {
        this.danceTimer++;
        
        // Toggle sneak only 1 in 20 ticks for a subtle effect
        if (this.danceTimer % 20 < 10) {
            this.minesis.setShiftKeyDown(true);
        } else {
            this.minesis.setShiftKeyDown(false);
        }

        // Stop dancing after a few ticks
        if (this.danceTimer >= 40) {
            this.stop();
        }
    }

    @Override
    public void stop() {
        this.minesis.setShiftKeyDown(false);
        this.danceTimer = 0;
    }
}
