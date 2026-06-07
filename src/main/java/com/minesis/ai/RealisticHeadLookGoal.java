package com.minesis.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.util.Mth;

import com.minesis.entity.MinesisEntity;

/**
 * Realistic head movement that mimics player behavior
 */
public class RealisticHeadLookGoal extends Goal {
    private final MinesisEntity entity;
    private int lookTimer = 0;
    private double lookX = 0;
    private double lookY = 0;
    private double lookZ = 0;
    private int focusTimer = 0;

    public RealisticHeadLookGoal(MinesisEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean canUse() {
        return this.entity.getTarget() == null && this.entity.getDeltaMovement().lengthSqr() < 0.001D && this.entity.getNavigation().isDone();
    }

    @Override
    public void tick() {
        this.lookTimer--;
        this.focusTimer--;

        // Change focus target less frequently for smoother looking
        if (this.focusTimer <= 0) {
            this.focusTimer = 60 + this.entity.getRandom().nextInt(120); // Longer focus periods
            
            // Decide what to look at
            int choice = this.entity.getRandom().nextInt(10);
            
            if (choice < 6) {
                // Look ahead in direction of movement (60%)
                this.lookAheadNaturally();
            } else if (choice < 8) {
                // Glance around (20%)
                this.glanceAround();
            } else {
                // Look down occasionally (20%)
                this.lookDown();
            }
        }

        // Apply smooth head movement - slower updates for less shaking
        if (this.lookTimer <= 0) {
            double dx = this.lookX - this.entity.getX();
            double dy = this.lookY - (this.entity.getY() + this.entity.getEyeHeight());
            double dz = this.lookZ - this.entity.getZ();
            
            this.entity.getLookControl().setLookAt(
                this.entity.getX() + dx,
                this.entity.getY() + this.entity.getEyeHeight() + dy,
                this.entity.getZ() + dz
            );
            
            this.lookTimer = 4; // Less frequent updates
        }
    }

    private void lookAheadNaturally() {
        // Look ahead based on movement direction
        double yaw = this.entity.getYRot() * (Math.PI / 180.0);
        double distance = 8.0;
        
        this.lookX = this.entity.getX() + Math.sin(-yaw) * distance;
        this.lookZ = this.entity.getZ() + Math.cos(yaw) * distance;
        // Slight vertical variation when looking ahead
        this.lookY = this.entity.getY() + this.entity.getEyeHeight() + (this.entity.getRandom().nextDouble() - 0.5) * 1.5;
    }

    private void glanceAround() {
        // Random glance with some limits to look natural, including vertical
        double angle = (this.entity.getRandom().nextDouble() - 0.5) * Math.PI;
        double distance = 4.0 + this.entity.getRandom().nextDouble() * 4.0;
        
        double yaw = (this.entity.getYRot() + (Math.random() - 0.5) * 60) * (Math.PI / 180.0);
        this.lookX = this.entity.getX() + Math.sin(-yaw) * distance;
        this.lookZ = this.entity.getZ() + Math.cos(yaw) * distance;
        // More vertical variation when glancing
        this.lookY = this.entity.getY() + this.entity.getEyeHeight() + (this.entity.getRandom().nextDouble() - 0.5) * 4.0;
    }

    private void lookDown() {
        // Look down at the ground
        this.lookX = this.entity.getX() + (this.entity.getRandom().nextDouble() - 0.5) * 2;
        this.lookY = this.entity.getY() - 2.0; // Look well below
        this.lookZ = this.entity.getZ() + (this.entity.getRandom().nextDouble() - 0.5) * 2;
    }

    @Override
    public void stop() {
        this.lookTimer = 0;
        this.focusTimer = 0;
    }
}
