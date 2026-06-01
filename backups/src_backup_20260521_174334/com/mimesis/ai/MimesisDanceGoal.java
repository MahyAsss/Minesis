package com.mimesis.ai;

import net.minecraft.world.entity.ai.goal.Goal;

import com.mimesis.entity.MimesisEntity;

/**
 * Makes Mimesis perform sneaking "dance" moves without blocking movement
 */
public class MimesisDanceGoal extends Goal {
    private final MimesisEntity mimesis;
    private int danceTimer = 0;

    public MimesisDanceGoal(MimesisEntity mimesis) {
        this.mimesis = mimesis;
    }

    @Override
    public boolean canUse() {
        // Dance rarely and very briefly
        return this.mimesis.getRandom().nextInt(200) == 0;
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
            this.mimesis.setShiftKeyDown(true);
        } else {
            this.mimesis.setShiftKeyDown(false);
        }

        // Stop dancing after a few ticks
        if (this.danceTimer >= 40) {
            this.stop();
        }
    }

    @Override
    public void stop() {
        this.mimesis.setShiftKeyDown(false);
        this.danceTimer = 0;
    }
}
