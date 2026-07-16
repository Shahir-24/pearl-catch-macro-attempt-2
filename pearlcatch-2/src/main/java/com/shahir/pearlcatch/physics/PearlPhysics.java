package com.shahir.pearlcatch.physics;

import net.minecraft.util.math.Vec3d;

/**
 * Mirrors vanilla ThrowableItemProjectile / EnderPearlEntity physics.
 *
 * These are the long-standing documented values (Minecraft Wiki "Ender Pearl" /
 * "Entity" physics pages): gravity 0.03 blocks/tick^2, drag multiplier 0.99/tick,
 * applied in the order: move -> apply drag -> subtract gravity from Y.
 *
 * CALIBRATION WARNING: I could not compile this against the real 1.21.11
 * decompiled source in this environment (no Mojang/Fabric maven access here),
 * so these numbers are "well-known vanilla constants," not numbers I verified
 * against this exact version's bytecode. If your intercepts consistently miss
 * high or low, throw a pearl in a flat superflat world, log its Y velocity
 * with an F3 debug screen or a /data get entity command each tick, and adjust
 * GRAVITY / DRAG here to match.
 */
public final class PearlPhysics {

    public static final double GRAVITY = 0.03; // blocks/tick^2, subtracted from Y velocity each tick
    public static final double DRAG = 0.99;    // multiplier applied to velocity each tick

    private PearlPhysics() {}

    /** Advances one tick of vanilla throwable-projectile motion. Returns new [pos, vel]. */
    public static Vec3d[] step(Vec3d pos, Vec3d vel) {
        Vec3d newPos = pos.add(vel);
        Vec3d dragged = vel.multiply(DRAG);
        Vec3d newVel = new Vec3d(dragged.x, dragged.y - GRAVITY, dragged.z);
        return new Vec3d[]{newPos, newVel};
    }

    /** Simulates forward `ticks` steps from a starting pos/vel and returns the full path (index 0 = start). */
    public static Vec3d[] simulatePath(Vec3d startPos, Vec3d startVel, int ticks) {
        Vec3d[] path = new Vec3d[ticks + 1];
        path[0] = startPos;
        Vec3d pos = startPos;
        Vec3d vel = startVel;
        for (int i = 1; i <= ticks; i++) {
            Vec3d[] next = step(pos, vel);
            pos = next[0];
            vel = next[1];
            path[i] = pos;
        }
        return path;
    }
}
