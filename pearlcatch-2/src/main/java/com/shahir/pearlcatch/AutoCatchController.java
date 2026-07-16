package com.shahir.pearlcatch;

import com.shahir.pearlcatch.physics.InterceptSolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * State machine, ticked once per client tick:
 *
 *  IDLE     -- player presses the arm key --> ARMED
 *  ARMED    -- a new EnderPearlEntity owned by the player appears --> TRACKING
 *  TRACKING -- every tick: re-read the REAL pearl entity's real position and
 *              velocity (so getting hit / drag / anything mid-flight is
 *              automatically accounted for, no need to model it separately),
 *              re-solve the intercept, and throw the moment a solution exists
 *              and a wind charge is available --> IDLE
 *
 * This intentionally never simulates the pearl from the moment of the throw
 * onward in one shot — that would compound any small error in PearlPhysics'
 * constants over dozens of ticks. Re-anchoring to the live entity every tick
 * means errors can't accumulate past a single tick's worth.
 */
public class AutoCatchController {

    private enum State { IDLE, ARMED, TRACKING }

    private State state = State.IDLE;
    private int trackedPearlId = -1;
    private int ticksSinceArmed = 0;
    private static final int ARM_TIMEOUT_TICKS = 100; // give up arming after 5s if no pearl thrown

    public void arm() {
        if (state == State.IDLE) {
            state = State.ARMED;
            ticksSinceArmed = 0;
        }
    }

    public void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            reset();
            return;
        }

        switch (state) {
            case IDLE -> {}

            case ARMED -> {
                ticksSinceArmed++;
                if (ticksSinceArmed > ARM_TIMEOUT_TICKS) {
                    reset();
                    return;
                }
                EnderPearlEntity pearl = findFreshOwnedPearl(client, player);
                if (pearl != null) {
                    trackedPearlId = pearl.getId();
                    state = State.TRACKING;
                }
            }

            case TRACKING -> {
                Entity entity = client.world.getEntityById(trackedPearlId);
                if (!(entity instanceof EnderPearlEntity pearl) || !pearl.isAlive()) {
                    // Pearl already landed/teleported the player, or despawned — nothing to intercept.
                    reset();
                    return;
                }

                Vec3d eyePos = player.getEyePos();
                Vec3d pearlPos = pearl.getPos();
                Vec3d pearlVel = pearl.getVelocity();

                InterceptSolver.Solution solution = InterceptSolver.solve(eyePos, pearlPos, pearlVel);

                if (solution != null) {
                    boolean thrown = tryThrowWindChargeAt(client, player, solution.aimDirection());
                    if (thrown) {
                        reset();
                    }
                    // if not thrown (no wind charge in hotbar), keep tracking and try again next tick —
                    // gives you a window to switch to one manually without losing the lock.
                }
            }
        }
    }

    /**
     * Finds a pearl owned by the player. No "just spawned" age check here — Entity
     * doesn't expose a generic age field (that's an ItemEntity-specific thing, not
     * universal, and I was wrong to assume otherwise on first pass). Instead this
     * relies entirely on the ARMED state: we only search while ARMED, and ARMED
     * only exists for a short window right after you press the key, so the first
     * owned pearl we find in that window is necessarily the one you just threw.
     */
    private EnderPearlEntity findFreshOwnedPearl(MinecraftClient client, ClientPlayerEntity player) {
        Box searchArea = player.getBoundingBox().expand(8.0);
        for (Entity e : client.world.getOtherEntities(player, searchArea)) {
            if (e instanceof EnderPearlEntity pearl && pearl.getOwner() == player) {
                return pearl;
            }
        }
        return null;
    }

    /** Returns true if a wind charge was actually thrown. */
    private boolean tryThrowWindChargeAt(MinecraftClient client, ClientPlayerEntity player, Vec3d direction) {
        int windChargeSlot = findHotbarSlot(player, Items.WIND_CHARGE);
        if (windChargeSlot == -1) {
            return false; // nothing to throw yet — caller keeps tracking and retries next tick
        }

        int previousSlot = player.getInventory().selectedSlot;
        float previousYaw = player.getYaw();
        float previousPitch = player.getPitch();

        // Setting the local field alone isn't enough — the server tracks its own copy
        // of your selected slot and would still think you're holding whatever you had
        // before. This is the same packet vanilla sends when you press a 1-9 hotbar key.
        player.getInventory().selectedSlot = windChargeSlot;
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(windChargeSlot));
        setLookDirection(player, direction);

        // Fires the same code path as a real right-click use, so it works identically
        // in singleplayer and on a real server (server remains authoritative for the spawn).
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);

        // Restore your camera and hotbar slot immediately after, sending the same
        // slot-restore packet so client and server agree again. There may still be a
        // very slight visible camera snap for the tick the throw happens — that's an
        // inherent limitation of auto-aiming, not a bug.
        player.getInventory().selectedSlot = previousSlot;
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
        player.setYaw(previousYaw);
        player.setPitch(previousPitch);

        return true;
    }

    private int findHotbarSlot(ClientPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private void setLookDirection(ClientPlayerEntity player, Vec3d direction) {
        double dx = direction.x;
        double dy = direction.y;
        double dz = direction.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
    }

    private void reset() {
        state = State.IDLE;
        trackedPearlId = -1;
        ticksSinceArmed = 0;
    }
}
