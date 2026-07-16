package com.shahir.pearlcatch.physics;

/**
 * Wind charges fly in a straight line, no gravity, no drag (confirmed on the
 * Minecraft Wiki "Wind Charge" page: "wind charges... fly in a straight line
 * through the air").
 *
 * SPEED is the one number in this whole mod I'm least sure of — I do not have
 * a verified source for the exact blocks/tick value on 1.21.11, only that
 * thrown-item projectiles commonly use a base power of ~1.5 in their launch
 * formula. Treat SPEED as a starting guess and calibrate it:
 *   1. Stand still, throw a wind charge at a wall a known distance away.
 *   2. Count ticks (20 ticks = 1 second) until impact using F3 + a stopwatch,
 *      or a mod like Tick Counter / a stopwatch clip.
 *   3. SPEED = distance_in_blocks / ticks_to_impact.
 * Update the constant below once you've measured it — the intercept math
 * is correct regardless of this value, it just needs the right number in.
 */
public final class WindChargePhysics {

    public static final double SPEED = 1.5; // blocks/tick — CALIBRATE THIS, see above

    private WindChargePhysics() {}
}
