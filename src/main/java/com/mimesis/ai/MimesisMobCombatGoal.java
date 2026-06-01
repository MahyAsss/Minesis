package com.mimesis.ai;

import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.mimesis.entity.MimesisEntity;

/**
 * Makes Mimesis fight hostile mobs with a sword
 */
public class MimesisMobCombatGoal extends MeleeAttackGoal {
    private final MimesisEntity mimesis;
    private int attackCooldown = 0;

    public MimesisMobCombatGoal(MimesisEntity mimesis, double speed) {
        super(mimesis, speed, false);
        this.mimesis = mimesis;
    }

    @Override
    public boolean canUse() {
        // Only engage mobs if provoked by a mob
        if (this.mimesis.isHostile() || !this.mimesis.isProvokedByMob()) {
            return false;
        }

        Mob target = this.findNearestHostileMob();
        if (target != null) {
            this.mimesis.setTarget(target);
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.mimesis.getTarget() instanceof Mob) {
            Mob target = (Mob) this.mimesis.getTarget();
            if (target != null && target.isAlive()) {
                // Equip sword for combat
                this.equipSword();
                
                // Attack when close
                this.attackCooldown--;
                if (this.attackCooldown <= 0 && this.mimesis.distanceTo(target) < 3.0D) {
                    this.mimesis.swing(InteractionHand.MAIN_HAND);
                    if (!this.mimesis.level().isClientSide) {
                        this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
                    }
                    this.mimesis.doHurtTarget(target);
                    this.attackCooldown = 8;
                }
            }
        }
    }

    @Override
    public void stop() {
        this.attackCooldown = 0;
        if (!this.mimesis.isHostile()) {
            this.mimesis.clearHeldItems();
        }
    }

    private Mob findNearestHostileMob() {
        Mob nearest = null;
        double nearestDist = 32.0D; // 8 blocks

        for (Mob mob : this.mimesis.level().getEntitiesOfClass(Mob.class, 
             this.mimesis.getBoundingBox().inflate(nearestDist))) {
            
            if (mob instanceof Monster && !(mob instanceof MimesisEntity)) {
                double dist = this.mimesis.distanceTo(mob);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = mob;
                }
            }
        }
        return nearest;
    }

    private void equipSword() {
        ItemStack mainHand = this.mimesis.getItemBySlot(EquipmentSlot.MAINHAND);
        
        // Only change if not already holding a sword
        if (!mainHand.getItem().toString().toLowerCase().contains("sword")) {
            this.mimesis.setItemSlot(
                EquipmentSlot.MAINHAND,
                new ItemStack(Items.IRON_SWORD)
            );
        }
    }
}
