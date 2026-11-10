package net.xiaoyu.mob_controller.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * 待重生受控生物的数据快照。
 *
 * <p>（从 MobControlledData 的嵌套 record 提升为顶层类：运行时动态加载嵌套 record
 * 在部分运行环境下会触发 NoClassDefFoundError。）</p>
 */
public record PendingRespawnData(
        UUID deadMobUUID, UUID controllerUUID, CompoundTag entityNbt,
        CompoundTag capabilityNbt, int triggerTick,
        ResourceKey<Level> deathDimension,
        BlockPos deathPos,
        String mobTypeId,
        boolean highHealthMob,
        String deathCause,
        List<UUID> passengerIds,
        List<UUID[]> ridingPairs
) {
}
