package com.minesis.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import com.minesis.entity.MinesisEntity;

public class HumanPlayerLookGoal extends Goal {
    private final MinesisEntity entity;
    private final double maxDistSq;
    private Player lookTarget = null;
    private int lookTimer    = 0;
    private int driftTimer   = 0;
    private double focusX, focusY, focusZ;

    public HumanPlayerLookGoal(MinesisEntity entity, double maxDist) {
        this.entity    = entity;
        this.maxDistSq = maxDist * maxDist;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (entity.getRandom().nextFloat() >= 0.02F) return false;
        lookTarget = entity.level().getNearestPlayer(entity, Math.sqrt(maxDistSq));
        return lookTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return lookTarget != null
            && lookTarget.isAlive()
            && entity.distanceToSqr(lookTarget) <= maxDistSq
            && lookTimer > 0;
    }

    @Override
    public void start() {
        lookTimer  = 10 + entity.getRandom().nextInt(20);
        driftTimer = 0;
    }

    @Override
    public void tick() {
        if (lookTarget == null) return;
        lookTimer--;
        driftTimer--;
        if (driftTimer <= 0) {
            // Face/chin area + micro-drift, updated every 5-12 ticks (~0.25-0.6 s)
            driftTimer = 5 + entity.getRandom().nextInt(8);
            double faceY = lookTarget.getY() + lookTarget.getEyeHeight() * 0.82;
            focusX = lookTarget.getX() + (entity.getRandom().nextDouble() - 0.5) * 0.35;
            focusY = faceY + (entity.getRandom().nextDouble() - 0.5) * 0.45;
            focusZ = lookTarget.getZ() + (entity.getRandom().nextDouble() - 0.5) * 0.35;
        }
        entity.getLookControl().setLookAt(focusX, focusY, focusZ);
    }

    @Override
    public void stop() {
        lookTarget = null;
        lookTimer  = 0;
        driftTimer = 0;
    }
}
