/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.ai.goal.Goal
 */
package com.mimesis.ai;

import com.mimesis.entity.MimesisEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class MimesisDanceGoal
extends Goal {
    private final MimesisEntity mimesis;
    private int danceTimer = 0;

    public MimesisDanceGoal(MimesisEntity mimesis) {
        this.mimesis = mimesis;
    }

    public boolean m_8036_() {
        return this.mimesis.m_217043_().m_188503_(200) == 0;
    }

    public boolean m_6767_() {
        return true;
    }

    public void m_8037_() {
        ++this.danceTimer;
        if (this.danceTimer % 20 < 10) {
            this.mimesis.m_20260_(true);
        } else {
            this.mimesis.m_20260_(false);
        }
        if (this.danceTimer >= 40) {
            this.m_8041_();
        }
    }

    public void m_8041_() {
        this.mimesis.m_20260_(false);
        this.danceTimer = 0;
    }
}

