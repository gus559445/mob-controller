package net.xiaoyu.mob_controller.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 一只受控生物的最后已知状态。
 */
public record PositionEntry(UUID mobId, UUID ownerId, ResourceKey<Level> dimension,
                            double x, double y, double z, String name, String typeId) {
}
