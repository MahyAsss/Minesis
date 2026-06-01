/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.ai.goal.Goal
 */
package com.mimesis.ai;

import com.mimesis.entity.MimesisEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class RealisticHeadLookGoal
extends Goal {
    private final MimesisEntity entity;
    private int lookTimer = 0;
    private double lookX = 0.0;
    private double lookY = 0.0;
    private double lookZ = 0.0;
    private int focusTimer = 0;

    public RealisticHeadLookGoal(MimesisEntity entity) {
        this.entity = entity;
    }

    public boolean m_8036_() {
        return this.entity.m_5448_() == null && this.entity.m_20184_().m_82556_() < 0.001 && this.entity.m_21573_().m_26571_();
    }

    public void m_8037_() {
        --this.lookTimer;
        --this.focusTimer;
        if (this.focusTimer <= 0) {
            this.focusTimer = 60 + this.entity.m_217043_().m_188503_(120);
            int choice = this.entity.m_217043_().m_188503_(10);
            if (choice < 6) {
                this.lookAheadNaturally();
            } else if (choice < 8) {
                this.glanceAround();
            } else {
                this.lookDown();
            }
        }
        if (this.lookTimer <= 0) {
            double dx = this.lookX - this.entity.m_20185_();
            double dy = this.lookY - (this.entity.m_20186_() + (double)this.entity.m_20192_());
            double dz = this.lookZ - this.entity.m_20189_();
            this.entity.m_21563_().m_24946_(this.entity.m_20185_() + dx, this.entity.m_20186_() + (double)this.entity.m_20192_() + dy, this.entity.m_20189_() + dz);
            this.lookTimer = 4;
        }
    }

    private void lookAheadNaturally() {
        double yaw = (double)this.entity.m_146908_() * (Math.PI / 180);
        double distance = 8.0;
        this.lookX = this.entity.m_20185_() + Math.sin(-yaw) * distance;
        this.lookZ = this.entity.m_20189_() + Math.cos(yaw) * distance;
        this.lookY = this.entity.m_20186_() + (double)this.entity.m_20192_() + (this.entity.m_217043_().m_188500_() - 0.5) * 1.5;
    }

    private void glanceAround() {
        double angle = (this.entity.m_217043_().m_188500_() - 0.5) * Math.PI;
        double distance = 4.0 + this.entity.m_217043_().m_188500_() * 4.0;
        double yaw = ((double)this.entity.m_146908_() + (Math.random() - 0.5) * 60.0) * (Math.PI / 180);
        this.lookX = this.entity.m_20185_() + Math.sin(-yaw) * distance;
        this.lookZ = this.entity.m_20189_() + Math.cos(yaw) * distance;
        this.lookY = this.entity.m_20186_() + (double)this.entity.m_20192_() + (this.entity.m_217043_().m_188500_() - 0.5) * 4.0;
    }

    private void lookDown() {
        this.lookX = this.entity.m_20185_() + (this.entity.m_217043_().m_188500_() - 0.5) * 2.0;
        this.lookY = this.entity.m_20186_() - 2.0;
        this.lookZ = this.entity.m_20189_() + (this.entity.m_217043_().m_188500_() - 0.5) * 2.0;
    }

    public void m_8041_() {
        this.lookTimer = 0;
        this.focusTimer = 0;
    }
}

