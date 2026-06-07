package com.minesis.ai;

import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.minesis.entity.MinesisEntity;

/**
 * Makes Minesis fight hostile mobs with a sword
 */
public class MinesisMobCombatGoal extends MeleeAttackGoal {
    private final MinesisEntity minesis;
    private int attackCooldown = 0;

    public MinesisMobCombatGoal(MinesisEntity minesis, double speed) {
        super(minesis, speed, false);
        this.minesis = minesis;
    }

    @Override
    public boolean canUse() {
        if (this.minesis.isHostile()) return false;
        Mob target = this.findNearestHostileMob();
        if (target != null) {
            this.minesis.setTarget(target);
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.minesis.getTarget() instanceof Mob) {
            Mob target = (Mob) this.minesis.getTarget();
            if (target != null && target.isAlive()) {
                // Equip sword for combat
                this.equipSword();
                
                // Attack when close
                this.attackCooldown--;
                if (this.attackCooldown <= 0 && this.minesis.distanceTo(target) < 3.0D) {
                    this.minesis.swing(InteractionHand.MAIN_HAND);
                    if (!this.minesis.level().isClientSide) {
                        this.minesis.level().broadcastEntityEvent(this.minesis, (byte)4);
                    }
                    this.minesis.doHurtTarget(target);
                    this.attackCooldown = 8;
                }
            }
        }
    }

    @Override
    public void stop() {
        this.attackCooldown = 0;
        this.minesis.clearHeldItems();
    }

    private Mob findNearestHostileMob() {
        Mob nearest = null;
        double nearestDist = 10.0D;

        for (Mob mob : this.minesis.level().getEntitiesOfClass(Mob.class,
             this.minesis.getBoundingBox().inflate(nearestDist))) {
            
            if (mob instanceof Monster && !(mob instanceof MinesisEntity)) {
                double dist = this.minesis.distanceTo(mob);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = mob;
                }
            }
        }
        return nearest;
    }

    private void equipSword() {
        ItemStack mainHand = this.minesis.getItemBySlot(EquipmentSlot.MAINHAND);
        
        // Only change if not already holding a sword
        if (!mainHand.getItem().toString().toLowerCase().contains("sword")) {
            this.minesis.setItemSlot(
                EquipmentSlot.MAINHAND,
                new ItemStack(Items.IRON_SWORD)
            );
        }
    }
}
