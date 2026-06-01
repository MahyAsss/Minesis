/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.PathfinderMob
 *  net.minecraft.world.entity.ai.goal.MeleeAttackGoal
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 */
package com.mimesis.ai;

import com.mimesis.entity.MimesisEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class MimesisMobCombatGoal
extends MeleeAttackGoal {
    private final MimesisEntity mimesis;
    private int attackCooldown = 0;

    public MimesisMobCombatGoal(MimesisEntity mimesis, double speed) {
        super((PathfinderMob)mimesis, speed, false);
        this.mimesis = mimesis;
    }

    public boolean m_8036_() {
        if (this.mimesis.isHostile() || !this.mimesis.isProvokedByMob()) {
            return false;
        }
        Mob target = this.findNearestHostileMob();
        if (target != null) {
            this.mimesis.m_6710_((LivingEntity)target);
            return true;
        }
        return false;
    }

    public void m_8037_() {
        Mob target;
        super.m_8037_();
        if (this.mimesis.m_5448_() instanceof Mob && (target = (Mob)this.mimesis.m_5448_()) != null && target.m_6084_()) {
            this.equipSword();
            --this.attackCooldown;
            if (this.attackCooldown <= 0 && (double)this.mimesis.m_20270_((Entity)target) < 3.0) {
                this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
                if (!this.mimesis.m_9236_().f_46443_) {
                    this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
                }
                this.mimesis.m_7327_((Entity)target);
                this.attackCooldown = 8;
            }
        }
    }

    public void m_8041_() {
        this.mimesis.m_6710_(null);
        this.attackCooldown = 0;
        if (!this.mimesis.isHostile()) {
            this.mimesis.clearHeldItems();
        }
    }

    private Mob findNearestHostileMob() {
        Mob nearest = null;
        double nearestDist = 32.0;
        for (Mob mob : this.mimesis.m_9236_().m_45976_(Mob.class, this.mimesis.m_20191_().m_82400_(nearestDist))) {
            double dist;
            if (!(mob instanceof Monster) || mob instanceof MimesisEntity || !((dist = (double)this.mimesis.m_20270_((Entity)mob)) < nearestDist)) continue;
            nearestDist = dist;
            nearest = mob;
        }
        return nearest;
    }

    private void equipSword() {
        ItemStack mainHand = this.mimesis.m_6844_(EquipmentSlot.MAINHAND);
        if (!mainHand.m_41720_().toString().toLowerCase().contains("sword")) {
            this.mimesis.m_8061_(EquipmentSlot.MAINHAND, new ItemStack((ItemLike)Items.f_42425_));
        }
    }
}

