package com.shahir.pearlcatch.physics;

import net.minecraft.util.math.Vec3d;

/**
 * Given the pearl's CURRENT real position/velocity and the player's CURRENT
 * eye position, finds the soonest future tick k at which a wind charge fired
 * right now (constant velocity, no gravity) could arrive at the same point
 * the pearl will be at.
 *
 * The condition for "can arrive in time": the straight-line distance from the
 * player's eye to the pearl's predicted position at tick k must be <=
 * WindChargePhysics.SPEED * k (the wind charge covers that much ground in k
 * ticks). We scan forward tick by tick and take the first k where this holds
 * — that's the earliest possible intercept, which is also the most accurate
 * one since the pearl has moved the least from its predicted path by then.
 */
public final class InterceptSolver {

    public static final int MAX_LOOKAHEAD_TICKS = 100; // 5 seconds; pearls rarely fly usefully far past this

    public record Solution(int tick, Vec3d aimPoint, Vec3d aimDirection) {}

    private InterceptSolver() {}

    /**
     * @param eyePos        player's current eye position (throw origin for the wind charge)
     * @param pearlPos      pearl's current real position (read from the live entity)
     * @param pearlVelocity pearl's current real velocity (read from the live entity)
     * @return a solution if an intercept is reachable within MAX_LOOKAHEAD_TICKS, else null
     */
    public static Solution solve(Vec3d eyePos, Vec3d pearlPos, Vec3d pearlVelocity) {
        Vec3d[] path = PearlPhysics.simulatePath(pearlPos, pearlVelocity, MAX_LOOKAHEAD_TICKS);

        for (int k = 1; k <= MAX_LOOKAHEAD_TICKS; k++) {
            Vec3d predictedPearlPos = path[k];
            double distanceNeeded = eyePos.distanceTo(predictedPearlPos);
            double distanceWindChargeCovers = WindChargePhysics.SPEED * k;

            if (distanceWindChargeCovers >= distanceNeeded) {
                Vec3d direction = predictedPearlPos.subtract(eyePos).normalize();
                return new Solution(k, predictedPearlPos, direction);
            }
        }
        return null; // pearl is outrunning the wind charge — no valid intercept this tick, try again next tick
    }
}
