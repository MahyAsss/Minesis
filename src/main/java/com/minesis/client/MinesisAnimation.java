package com.minesis.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public class MinesisAnimation {

    public static final AnimationDefinition TRANSFORM = AnimationDefinition.Builder.withLength(2.0F)
        .addAnimation("left_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_leg", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_leg", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec(50.0F,      0.0F,      0.0F),     AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.0417F, KeyframeAnimations.degreeVec(49.6378F,  -8.1703F,  -9.4926F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.0833F, KeyframeAnimations.degreeVec(48.4359F,   7.0179F,   7.5159F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.125F,  KeyframeAnimations.degreeVec(46.9384F,   0.0566F,   0.0108F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1667F, KeyframeAnimations.degreeVec(55.5383F, -29.4355F, -35.6522F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2083F, KeyframeAnimations.degreeVec(46.6125F,  11.6909F,  11.045F),   AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.25F,   KeyframeAnimations.degreeVec(49.6378F,  -8.1703F,  -9.4926F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2917F, KeyframeAnimations.degreeVec(48.4359F,   7.0179F,   7.5159F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3333F, KeyframeAnimations.degreeVec(46.9384F,   0.0566F,   0.0108F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.375F,  KeyframeAnimations.degreeVec(55.5383F, -29.4355F, -35.6522F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.4167F, KeyframeAnimations.degreeVec(46.6125F,  11.6909F,  11.045F),   AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.4583F, KeyframeAnimations.degreeVec(49.6378F,  -8.1703F,  -9.4926F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F,    KeyframeAnimations.degreeVec(48.4359F,   7.0179F,   7.5159F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5417F, KeyframeAnimations.degreeVec(46.9384F,   0.0566F,   0.0108F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5833F, KeyframeAnimations.degreeVec(55.5383F, -29.4355F, -35.6522F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.625F,  KeyframeAnimations.degreeVec(46.6125F,  11.6909F,  11.045F),   AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.6667F, KeyframeAnimations.degreeVec(49.6378F,  -8.1703F,  -9.4926F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7083F, KeyframeAnimations.degreeVec(48.4359F,   7.0179F,   7.5159F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.75F,   KeyframeAnimations.degreeVec(46.9384F,   0.0566F,   0.0108F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7917F, KeyframeAnimations.degreeVec(55.5383F, -29.4355F, -35.6522F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8333F, KeyframeAnimations.degreeVec(46.6125F,  11.6909F,  11.045F),   AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.875F,  KeyframeAnimations.degreeVec(49.6378F,  -8.1703F,  -9.4926F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9167F, KeyframeAnimations.degreeVec(48.4359F,   7.0179F,   7.5159F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9583F, KeyframeAnimations.degreeVec(46.9384F,   0.0566F,   0.0108F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F,    KeyframeAnimations.degreeVec(55.5383F, -29.4355F, -35.6522F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0417F, KeyframeAnimations.degreeVec(46.6125F,  11.6909F,  11.045F),   AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0833F, KeyframeAnimations.degreeVec(49.6378F,  -8.1703F,  -9.4926F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.125F,  KeyframeAnimations.degreeVec(48.4359F,   7.0179F,   7.5159F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1667F, KeyframeAnimations.degreeVec(46.9384F,   0.0566F,   0.0108F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2083F, KeyframeAnimations.degreeVec(55.5383F, -29.4355F, -35.6522F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.25F,   KeyframeAnimations.degreeVec(46.6125F,  11.6909F,  11.045F),   AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2917F, KeyframeAnimations.degreeVec(49.6378F,  -8.1703F,  -9.4926F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3333F, KeyframeAnimations.degreeVec(48.4359F,   7.0179F,   7.5159F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.375F,  KeyframeAnimations.degreeVec(46.9384F,   0.0566F,   0.0108F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4167F, KeyframeAnimations.degreeVec(55.5383F, -29.4355F, -35.6522F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4583F, KeyframeAnimations.degreeVec(46.6125F,  11.6909F,  11.045F),   AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F,    KeyframeAnimations.degreeVec(49.6378F,  -8.1703F,  -9.4926F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5417F, KeyframeAnimations.degreeVec(48.4359F,   7.0179F,   7.5159F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5833F, KeyframeAnimations.degreeVec(46.9384F,   0.0566F,   0.0108F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.625F,  KeyframeAnimations.degreeVec(55.5383F, -29.4355F, -35.6522F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6667F, KeyframeAnimations.degreeVec(46.6125F,  11.6909F,  11.045F),   AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7083F, KeyframeAnimations.degreeVec(49.6378F,  -8.1703F,  -9.4926F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.75F,   KeyframeAnimations.degreeVec(48.4359F,   7.0179F,   7.5159F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7917F, KeyframeAnimations.degreeVec(46.9384F,   0.0566F,   0.0108F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.8333F, KeyframeAnimations.degreeVec(55.5383F, -29.4355F, -35.6522F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.875F,  KeyframeAnimations.degreeVec(46.6125F,  11.6909F,  11.045F),   AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9167F, KeyframeAnimations.degreeVec(49.6378F,  -8.1703F,  -9.4926F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9583F, KeyframeAnimations.degreeVec(48.4359F,   7.0179F,   7.5159F),  AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F,    KeyframeAnimations.degreeVec(0.0F,       0.0F,      0.0F),     AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -4.0F, 14.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-33.4562F, 8.8435F, -33.5211F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),              AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(-1.0F, -12.0F, 12.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec( 0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-29.9885F, 0.5409F, 19.9408F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),             AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(1.0F, -12.0F, 12.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("upper_left_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-122.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(   0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("upper_right_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-120.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(   0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("hip", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec(-22.5F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5417F, KeyframeAnimations.degreeVec(-40.89F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F,    KeyframeAnimations.degreeVec(-28.75F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F,    KeyframeAnimations.degreeVec( -6.88F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F,    KeyframeAnimations.degreeVec(  0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("hip", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -19.0F, -1.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F,  -9.5F,  4.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("upper", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(  0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("upper", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -14.0F, 15.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("middle", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(  0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("middle", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -20.0F, 9.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F,   0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("lower", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec(-62.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.6667F, KeyframeAnimations.degreeVec( -4.17F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8333F, KeyframeAnimations.degreeVec(  6.35F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.125F,  KeyframeAnimations.degreeVec(  9.76F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F,    KeyframeAnimations.degreeVec(  0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("lower", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F,    KeyframeAnimations.posVec(0.0F, -19.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.6667F, KeyframeAnimations.posVec(0.0F, -12.67F, 6.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8333F, KeyframeAnimations.posVec(0.0F, -10.09F, 6.25F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.125F,  KeyframeAnimations.posVec(0.0F,  -7.57F, 5.69F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F,    KeyframeAnimations.posVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("tentacles", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec(-57.5F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0833F, KeyframeAnimations.degreeVec(-21.35F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F,    KeyframeAnimations.degreeVec(  0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("tentacles", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -10.0F, 17.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle2_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle2_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 27.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle2_3", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(3.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle2_4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 42.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle2_4", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(5.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .build();

    public static final AnimationDefinition HEAD_SHAKE = AnimationDefinition.Builder.withLength(0.25F).looping()
        .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec( 0.0F,  5.0F,   7.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.0417F, KeyframeAnimations.degreeVec( 0.0F,-10.0F, -15.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.0833F, KeyframeAnimations.degreeVec(15.0F,  0.0F,  25.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.125F,  KeyframeAnimations.degreeVec( 0.0F, -5.0F,  -7.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1667F, KeyframeAnimations.degreeVec(-15.0F, 0.0F,   7.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2083F, KeyframeAnimations.degreeVec( 0.0F,  2.5F, -15.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .build();

    public static final AnimationDefinition ATTACK = AnimationDefinition.Builder.withLength(0.7917F)
        .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec(   0.0F,    0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.125F,  KeyframeAnimations.degreeVec(114.2505F,-22.3292F,40.144F),AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.375F,  KeyframeAnimations.degreeVec(114.25F, -22.33F,  40.14F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5833F, KeyframeAnimations.degreeVec(-15.75F, -22.33F,  40.14F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.6667F, KeyframeAnimations.degreeVec(-15.75F, -22.33F,  40.14F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7917F, KeyframeAnimations.degreeVec(   0.0F,    0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("lower_right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec(  0.0F,     0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.125F,  KeyframeAnimations.degreeVec( 55.0F,     0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.375F,  KeyframeAnimations.degreeVec( 55.0F,     0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5833F, KeyframeAnimations.degreeVec( 18.5484F,-19.0377F,-6.246F),AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7917F, KeyframeAnimations.degreeVec(  0.0F,     0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("lower_right_arm", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .build();

    public static final AnimationDefinition WALK = AnimationDefinition.Builder.withLength(0.5F).looping()
        .addAnimation("left_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec( 15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1667F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3333F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F,    KeyframeAnimations.degreeVec( 15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec(-15.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1667F, KeyframeAnimations.degreeVec(-17.5F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3333F, KeyframeAnimations.degreeVec( 15.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F,    KeyframeAnimations.degreeVec(-15.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,  KeyframeAnimations.degreeVec( 0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(32.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F,  KeyframeAnimations.degreeVec( 0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,  KeyframeAnimations.degreeVec(32.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F,  KeyframeAnimations.degreeVec(32.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("lower_left_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec( 42.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1667F, KeyframeAnimations.degreeVec( 42.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3333F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F,    KeyframeAnimations.degreeVec( 42.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("lower_left_leg", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F,    KeyframeAnimations.posVec(0.0F,  3.0F,  7.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F,  3.0F,  7.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, -1.0F, -3.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F,    KeyframeAnimations.posVec(0.0F,  3.0F,  7.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("lower_right_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1667F, KeyframeAnimations.degreeVec( 42.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3333F, KeyframeAnimations.degreeVec( 42.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F,    KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("lower_right_leg", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F,    KeyframeAnimations.posVec(0.0F, -1.0F, -3.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F,  3.0F,  7.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F,  3.0F,  7.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F,    KeyframeAnimations.posVec(0.0F, -1.0F, -3.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .build();

    public static final AnimationDefinition TENTACLES_JIGGLE = AnimationDefinition.Builder.withLength(2.0F).looping()
        .addAnimation("left_tentacle1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -60.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle1_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -35.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle1_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -102.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle1_3", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(   0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(-3.75F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(   0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle1_4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F,    KeyframeAnimations.degreeVec(0.0F, 0.0F, -152.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9583F, KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle1_4", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F,    KeyframeAnimations.posVec(    0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F,    KeyframeAnimations.posVec(-14.25F, -1.75F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9583F, KeyframeAnimations.posVec(    0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -65.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -65.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle2_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F,    KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle2_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F, 0.0F, -97.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F,    KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -97.5F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle2_3", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F,    KeyframeAnimations.posVec(-2.5F, -0.75F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F,    KeyframeAnimations.posVec(  0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9583F, KeyframeAnimations.posVec(-2.5F, -0.75F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle2_4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -160.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -160.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle2_4", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(-12.75F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(    0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(-12.75F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -87.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle3_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle3_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -90.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle3_3", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(   0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(-4.5F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(   0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle3_4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -147.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_tentacle3_4", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(    0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(-14.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(    0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 70.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle1_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -27.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle1_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -92.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle1_3", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(    0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(-3.25F, -0.75F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(    0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle1_4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -150.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle1_4", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(    0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(-13.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(    0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 62.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 62.5F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle2_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle2_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -80.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -80.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle2_3", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(-2.75F, -0.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(   0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(-2.75F, -0.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle2_4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -145.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -145.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle2_4", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(-11.5F,  0.25F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(   0.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(-11.5F,  0.25F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 77.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle3_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle3_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -87.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle3_3", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(    0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(-2.5F, -0.25F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(    0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle3_4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -152.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_tentacle3_4", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(     0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(-11.5F, -0.25F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(     0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .build();
}
