package com.minesis.client;

import com.minesis.entity.MinesisEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MinesisHuntModel extends PlayerModel<MinesisEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation("minesis", "minesis_hunt"), "main");

    private final ModelPart huntRoot;
    // Limbs for walk animation
    private final ModelPart huntLeftArm;
    private final ModelPart huntRightArm;
    private final ModelPart huntLeftLeg;
    private final ModelPart huntRightLeg;
    private final ModelPart huntLowerLeftLeg;
    private final ModelPart huntLowerRightLeg;
    private final ModelPart huntLowerRightArm;
    // Head for head_shake
    private final ModelPart huntHead;
    // Body chain for transform
    private final ModelPart huntBody;
    private final ModelPart huntHip;
    private final ModelPart huntUpper;
    private final ModelPart huntMiddle;
    private final ModelPart huntLower;
    // Tentacle roots for jiggle
    private final ModelPart huntTentacles;
    private final ModelPart huntLeftTentacle1;
    private final ModelPart huntLeftTentacle2;
    private final ModelPart huntLeftTentacle3;
    private final ModelPart huntRightTentacle1;
    private final ModelPart huntRightTentacle2;
    private final ModelPart huntRightTentacle3;

    // Bone-name → ModelPart cache (populated lazily on first lookup)
    private final Map<String, Optional<ModelPart>> boneCache = new HashMap<>();
    // Shared scratch vector — rendering is single-threaded so this is safe
    private static final Vector3f ANIM_VEC = new Vector3f();

    public MinesisHuntModel(ModelPart root) {
        super(root, false);
        this.huntRoot         = root.getChild("Minesis");
        this.huntLeftArm      = this.huntRoot.getChild("left_arm");
        this.huntRightArm     = this.huntRoot.getChild("right_arm");
        this.huntLeftLeg      = this.huntRoot.getChild("left_leg");
        this.huntRightLeg     = this.huntRoot.getChild("right_leg");
        this.huntLowerLeftLeg  = this.huntLeftLeg.getChild("lower_left_leg");
        this.huntLowerRightLeg = this.huntRightLeg.getChild("lower_right_leg");
        this.huntLowerRightArm = this.huntRightArm.getChild("lower_right_arm");
        this.huntHead         = this.huntRoot.getChild("head");
        this.huntBody         = this.huntRoot.getChild("body");
        this.huntHip          = this.huntBody.getChild("hip");
        this.huntUpper        = this.huntBody.getChild("upper");
        this.huntMiddle       = this.huntBody.getChild("middle");
        this.huntLower        = this.huntBody.getChild("lower");
        this.huntTentacles    = this.huntRoot.getChild("tentacles");
        this.huntLeftTentacle1  = this.huntTentacles.getChild("left_tentacle1");
        this.huntLeftTentacle2  = this.huntTentacles.getChild("left_tentacle2");
        this.huntLeftTentacle3  = this.huntTentacles.getChild("left_tentacle3");
        this.huntRightTentacle1 = this.huntTentacles.getChild("right_tentacle1");
        this.huntRightTentacle2 = this.huntTentacles.getChild("right_tentacle2");
        this.huntRightTentacle3 = this.huntTentacles.getChild("right_tentacle3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 14 empty stubs required by PlayerModel/HumanoidModel constructors.
        // right_arm and left_arm are positioned at the lower-arm / handle chain
        // so that ItemInHandLayer places held items near the actual hand.
        root.addOrReplaceChild("head",         CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat",          CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body",         CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm",    CubeListBuilder.create(), PartPose.offset(-6.0F,  6.0F, -0.75F));
        root.addOrReplaceChild("left_arm",     CubeListBuilder.create(), PartPose.offset( 7.0F,  5.0F, -1.0F));
        root.addOrReplaceChild("right_leg",    CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg",     CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("ear",          CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("jacket",       CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_pants",   CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_pants",  CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_sleeve",  CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_sleeve", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("cloak",        CubeListBuilder.create(), PartPose.ZERO);

        // ── Blockbench geometry (128×128 texture) ────────────────────────────
        PartDefinition Minesis = root.addOrReplaceChild("Minesis", CubeListBuilder.create(), PartPose.offset(7.0F, 5.0F, -1.0F));

        // Left arm
        PartDefinition left_arm = Minesis.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, -12.0F, -1.0F));
        PartDefinition upper_left_arm = left_arm.addOrReplaceChild("upper_left_arm", CubeListBuilder.create(), PartPose.offset(2.0F, 1.0F, 1.0F));
        upper_left_arm.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(16, 16).addBox(-3.0F, -2.0F, -3.0F, 4.0F, 14.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2618F));
        PartDefinition lower_left_arm = left_arm.addOrReplaceChild("lower_left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 12.0F, 1.0F));
        lower_left_arm.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(32, 24).addBox(-2.0F, -1.0F, -2.0F, 2.0F, 22.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.3526F, 0.0F, -0.2618F));

        // Left leg
        PartDefinition left_leg = Minesis.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(-2.0F, -1.1F, 11.1F));
        PartDefinition upper_left_leg = left_leg.addOrReplaceChild("upper_left_leg", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, -1.9F, -1.8F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 9.0F, -6.0F));
        upper_left_leg.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(48, 50).addBox(-1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5672F, 0.0F, 0.0F));
        PartDefinition lower_left_leg = left_leg.addOrReplaceChild("lower_left_leg", CubeListBuilder.create(), PartPose.offset(0.0F, 20.1F, -3.8F));
        lower_left_leg.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(40, 22).addBox(-2.0F, -2.0F, -5.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0436F, 0.0F, 0.0F));
        lower_left_leg.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(40, 50).addBox(-1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1745F, 0.0F, 0.0F));

        // Right leg
        PartDefinition right_leg = Minesis.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-12.0F, -1.1F, 11.1F));
        PartDefinition upper_right_leg = right_leg.addOrReplaceChild("upper_right_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, -1.9F, -1.8F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 9.0F, -6.0F));
        upper_right_leg.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 55).addBox(-1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5672F, 0.0F, 0.0F));
        PartDefinition lower_right_leg = right_leg.addOrReplaceChild("lower_right_leg", CubeListBuilder.create(), PartPose.offset(0.0F, 20.1F, -3.8F));
        lower_right_leg.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1745F, 0.0F, 0.0F));
        lower_right_leg.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(40, 14).addBox(-2.0F, -2.0F, -5.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0436F, 0.0F, 0.0F));

        // Head
        PartDefinition head = Minesis.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(-7.0F, -12.0F, -11.7F));
        head.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -18.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, 10.0F, 1.7012F, 0.028F, 0.0102F));

        // Body
        PartDefinition body = Minesis.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(-7.0F, -7.0F, 11.3F));
        PartDefinition hip = body.addOrReplaceChild("hip", CubeListBuilder.create(), PartPose.offset(-6.0F, 6.0F, 0.0F));
        hip.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(40, 30).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.9163F));
        hip.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(40, 40).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.9163F));
        PartDefinition upper = body.addOrReplaceChild("upper", CubeListBuilder.create(), PartPose.offset(0.0F, -6.0F, -13.0F));
        upper.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(40, 0).addBox(-2.0F, -10.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.614F, 0.028F, 0.0102F));
        PartDefinition middle = body.addOrReplaceChild("middle", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, -5.0F));
        middle.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(16, 34).addBox(-2.0F, -10.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.3233F, -0.1837F, -0.7043F));
        PartDefinition lower = body.addOrReplaceChild("lower", CubeListBuilder.create(), PartPose.offset(0.0F, 4.0F, 0.0F));
        lower.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(0, 34).addBox(-2.0F, -10.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6977F, 0.028F, 0.0102F));

        // Right arm + handle
        PartDefinition right_arm = Minesis.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-8.5F, -12.5F, -1.0F));
        PartDefinition upper_right_arm = right_arm.addOrReplaceChild("upper_right_arm", CubeListBuilder.create(), PartPose.offset(-1.0F, 2.0F, 1.0F));
        upper_right_arm.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -2.0F, -3.0F, 4.0F, 14.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2618F));
        PartDefinition lower_right_arm = right_arm.addOrReplaceChild("lower_right_arm", CubeListBuilder.create(), PartPose.offset(-4.0F, 13.0F, 1.0F));
        lower_right_arm.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(32, 0).addBox(-2.0F, -1.0F, -2.0F, 2.0F, 22.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.3526F, 0.0F, 0.2618F));
        PartDefinition handle = lower_right_arm.addOrReplaceChild("handle", CubeListBuilder.create(), PartPose.offset(-0.5F, 0.5F, 0.25F));
        handle.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(56, 8).addBox(-1.0F, 20.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.3526F, 0.0F, 0.2618F));

        // Tentacles
        PartDefinition tentacles = Minesis.addOrReplaceChild("tentacles", CubeListBuilder.create(), PartPose.offset(-6.0F, -13.0F, -9.0F));

        PartDefinition left_tentacle1 = tentacles.addOrReplaceChild("left_tentacle1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.9199F));
        left_tentacle1.addOrReplaceChild("left_tentacle1_1", CubeListBuilder.create().texOffs(8, 55).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition left_tentacle1_2 = left_tentacle1.addOrReplaceChild("left_tentacle1_2", CubeListBuilder.create(), PartPose.offset(0.0F, -7.0F, 0.0F));
        left_tentacle1_2.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(12, 55).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition left_tentacle1_3 = left_tentacle1.addOrReplaceChild("left_tentacle1_3", CubeListBuilder.create(), PartPose.offset(1.25F, -13.75F, 0.0F));
        left_tentacle1_3.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(16, 55).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0346F, 0.0266F, 0.8286F));
        PartDefinition left_tentacle1_4 = left_tentacle1.addOrReplaceChild("left_tentacle1_4", CubeListBuilder.create(), PartPose.offset(6.5F, -18.5F, 0.25F));
        left_tentacle1_4.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(20, 55).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0184F, 0.0395F, 1.3086F));

        PartDefinition right_tentacle1 = tentacles.addOrReplaceChild("right_tentacle1", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.0F, 0.0F, 0.0F, 3.1416F, 0.0F, 1.2217F));
        right_tentacle1.addOrReplaceChild("right_tentacle1_1", CubeListBuilder.create().texOffs(60, 8).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition right_tentacle1_2 = right_tentacle1.addOrReplaceChild("right_tentacle1_2", CubeListBuilder.create(), PartPose.offset(0.0F, -7.0F, 0.0F));
        right_tentacle1_2.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(60, 16).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition right_tentacle1_3 = right_tentacle1.addOrReplaceChild("right_tentacle1_3", CubeListBuilder.create(), PartPose.offset(1.25F, -13.75F, 0.0F));
        right_tentacle1_3.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(60, 24).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0346F, 0.0266F, 0.8286F));
        PartDefinition right_tentacle1_4 = right_tentacle1.addOrReplaceChild("right_tentacle1_4", CubeListBuilder.create(), PartPose.offset(6.5F, -18.5F, 0.25F));
        right_tentacle1_4.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(60, 32).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0184F, 0.0395F, 1.3086F));

        PartDefinition right_tentacle2 = tentacles.addOrReplaceChild("right_tentacle2", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.0F, 0.0F, 10.0F, 3.1416F, 0.3927F, 1.2217F));
        right_tentacle2.addOrReplaceChild("right_tentacle2_1", CubeListBuilder.create().texOffs(60, 40).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition right_tentacle2_2 = right_tentacle2.addOrReplaceChild("right_tentacle2_2", CubeListBuilder.create(), PartPose.offset(0.0F, -7.0F, 0.0F));
        right_tentacle2_2.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(60, 48).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition right_tentacle2_3 = right_tentacle2.addOrReplaceChild("right_tentacle2_3", CubeListBuilder.create(), PartPose.offset(1.25F, -13.75F, 0.0F));
        right_tentacle2_3.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(60, 56).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0346F, 0.0266F, 0.8286F));
        PartDefinition right_tentacle2_4 = right_tentacle2.addOrReplaceChild("right_tentacle2_4", CubeListBuilder.create(), PartPose.offset(6.5F, -18.5F, 0.25F));
        right_tentacle2_4.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(32, 62).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0184F, 0.0395F, 1.3086F));

        PartDefinition right_tentacle3 = tentacles.addOrReplaceChild("right_tentacle3", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.0F, 4.0F, 16.6F, -3.0543F, 0.6981F, 1.2217F));
        right_tentacle3.addOrReplaceChild("right_tentacle3_1", CubeListBuilder.create().texOffs(36, 62).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition right_tentacle3_2 = right_tentacle3.addOrReplaceChild("right_tentacle3_2", CubeListBuilder.create(), PartPose.offset(0.0F, -7.0F, 0.0F));
        right_tentacle3_2.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(56, 62).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition right_tentacle3_3 = right_tentacle3.addOrReplaceChild("right_tentacle3_3", CubeListBuilder.create(), PartPose.offset(1.25F, -13.75F, 0.0F));
        right_tentacle3_3.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(8, 63).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0346F, 0.0266F, 0.8286F));
        PartDefinition right_tentacle3_4 = right_tentacle3.addOrReplaceChild("right_tentacle3_4", CubeListBuilder.create(), PartPose.offset(6.5F, -18.5F, 0.25F));
        right_tentacle3_4.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs(12, 63).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0184F, 0.0395F, 1.3086F));

        PartDefinition left_tentacle2 = tentacles.addOrReplaceChild("left_tentacle2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 11.0F, 0.0F, 0.48F, 1.9199F));
        left_tentacle2.addOrReplaceChild("left_tentacle2_1", CubeListBuilder.create().texOffs(24, 55).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition left_tentacle2_2 = left_tentacle2.addOrReplaceChild("left_tentacle2_2", CubeListBuilder.create(), PartPose.offset(0.0F, -7.0F, 0.0F));
        left_tentacle2_2.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs(28, 55).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition left_tentacle2_3 = left_tentacle2.addOrReplaceChild("left_tentacle2_3", CubeListBuilder.create(), PartPose.offset(1.25F, -13.75F, 0.0F));
        left_tentacle2_3.addOrReplaceChild("cube_r31", CubeListBuilder.create().texOffs(56, 0).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0346F, 0.0266F, 0.8286F));
        PartDefinition left_tentacle2_4 = left_tentacle2.addOrReplaceChild("left_tentacle2_4", CubeListBuilder.create(), PartPose.offset(6.5F, -18.5F, 0.25F));
        left_tentacle2_4.addOrReplaceChild("cube_r32", CubeListBuilder.create().texOffs(56, 30).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0184F, 0.0395F, 1.3086F));

        PartDefinition left_tentacle3 = tentacles.addOrReplaceChild("left_tentacle3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 5.0F, 17.0F, 0.0F, 0.7418F, 1.9199F));
        left_tentacle3.addOrReplaceChild("left_tentacle3_1", CubeListBuilder.create().texOffs(56, 38).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition left_tentacle3_2 = left_tentacle3.addOrReplaceChild("left_tentacle3_2", CubeListBuilder.create(), PartPose.offset(0.0F, -7.0F, 0.0F));
        left_tentacle3_2.addOrReplaceChild("cube_r33", CubeListBuilder.create().texOffs(56, 46).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition left_tentacle3_3 = left_tentacle3.addOrReplaceChild("left_tentacle3_3", CubeListBuilder.create(), PartPose.offset(1.25F, -13.75F, 0.0F));
        left_tentacle3_3.addOrReplaceChild("cube_r34", CubeListBuilder.create().texOffs(56, 54).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0346F, 0.0266F, 0.8286F));
        PartDefinition left_tentacle3_4 = left_tentacle3.addOrReplaceChild("left_tentacle3_4", CubeListBuilder.create(), PartPose.offset(6.5F, -18.5F, 0.25F));
        left_tentacle3_4.addOrReplaceChild("cube_r35", CubeListBuilder.create().texOffs(60, 0).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0184F, 0.0395F, 1.3086F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    /**
     * Finds a named part anywhere under huntRoot, using the same strategy as
     * HierarchicalModel.getAnyDescendantWithName(): stream all parts, find any that
     * has a child with the given name, then return that child. Results are cached.
     */
    private Optional<ModelPart> findPart(String name) {
        return boneCache.computeIfAbsent(name, n ->
            this.huntRoot.getAllParts()
                .filter(p -> p.hasChild(n))
                .findFirst()
                .map(p -> p.getChild(n))
        );
    }

    /**
     * Applies an AnimationDefinition to this model's parts. Replicates the logic
     * of KeyframeAnimations.animate() without requiring HierarchicalModel.
     * accumulatedMs is the AnimationState.getAccumulatedTime() value (milliseconds).
     */
    private void animateDef(AnimationDefinition def, long accumulatedMs) {
        float raw = (float) accumulatedMs / 1000.0F;
        final float elapsed = def.looping() ? raw % def.lengthInSeconds() : raw;

        for (Map.Entry<String, List<AnimationChannel>> entry : def.boneAnimations().entrySet()) {
            Optional<ModelPart> optPart = findPart(entry.getKey());
            if (!optPart.isPresent()) continue;
            ModelPart part = optPart.get();
            for (AnimationChannel ch : entry.getValue()) {
                Keyframe[] frames = ch.keyframes();
                int i = Math.max(0, Mth.binarySearch(0, frames.length,
                        idx -> elapsed <= frames[idx].timestamp()) - 1);
                int j = Math.min(frames.length - 1, i + 1);
                float t = elapsed - frames[i].timestamp();
                float alpha = (j != i)
                        ? Mth.clamp(t / (frames[j].timestamp() - frames[i].timestamp()), 0.0F, 1.0F)
                        : 0.0F;
                frames[j].interpolation().apply(ANIM_VEC, alpha, frames, i, j, 1.0F);
                ch.target().apply(part, ANIM_VEC);
            }
        }
    }

    @Override
    public void setupAnim(MinesisEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {

        // Reset all parts to their baked PartPose so animation offsets accumulate
        // from a clean baseline rather than lingering from the previous frame.
        this.huntRoot.getAllParts().forEach(ModelPart::resetPose);

        // ── Looping states: always active while the hunt model is visible ────
        entity.huntHeadShakeState.startIfStopped(entity.tickCount);
        entity.huntTentaclesState.startIfStopped(entity.tickCount);

        // ── Walk: only when the entity is actually moving ────────────────────
        entity.huntWalkState.animateWhen(limbSwingAmount > 0.01F, entity.tickCount);

        // ── Attack: trigger on the first frame of a swing ────────────────────
        if (entity.swinging && !entity.huntAttackState.isStarted()) {
            entity.huntAttackState.start(entity.tickCount);
        }

        // ── Transform intro: plays once when hostile mode first activates ────
        if (!entity.huntTransformAnimPlayed) {
            entity.huntTransformState.start(entity.tickCount);
            entity.huntTransformAnimPlayed = true;
        }

        // Advance all states by the current entity age
        entity.huntHeadShakeState.updateTime(ageInTicks, 1.0F);
        entity.huntTentaclesState.updateTime(ageInTicks, 1.0F);
        entity.huntWalkState.updateTime(ageInTicks, 1.0F);
        entity.huntAttackState.updateTime(ageInTicks, 1.0F);
        entity.huntTransformState.updateTime(ageInTicks, 1.0F);

        // Apply animations — order matters: later animations override earlier ones
        entity.huntHeadShakeState.ifStarted(s ->
                animateDef(MinesisAnimation.HEAD_SHAKE, s.getAccumulatedTime()));
        entity.huntTentaclesState.ifStarted(s ->
                animateDef(MinesisAnimation.TENTACLES_JIGGLE, s.getAccumulatedTime()));
        entity.huntWalkState.ifStarted(s ->
                animateDef(MinesisAnimation.WALK, s.getAccumulatedTime()));
        entity.huntAttackState.ifStarted(s -> {
            animateDef(MinesisAnimation.ATTACK, s.getAccumulatedTime());
            if (s.getAccumulatedTime() >= (long)(MinesisAnimation.ATTACK.lengthInSeconds() * 1000L)) {
                entity.huntAttackState.stop();
            }
        });
        entity.huntTransformState.ifStarted(s -> {
            animateDef(MinesisAnimation.TRANSFORM, s.getAccumulatedTime());
            if (s.getAccumulatedTime() >= (long)(MinesisAnimation.TRANSFORM.lengthInSeconds() * 1000L)) {
                entity.huntTransformState.stop();
            }
        });
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
            int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.huntRoot.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /** Renders only the head at full brightness — called by MinesisEyeLayer for the glow effect. */
    public void renderEyes(PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        poseStack.pushPose();
        // Apply huntRoot's own transform so the head lands at the correct world position.
        this.huntRoot.translateAndRotate(poseStack);
        this.huntHead.render(poseStack, vertexConsumer, 0xF000F0, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
