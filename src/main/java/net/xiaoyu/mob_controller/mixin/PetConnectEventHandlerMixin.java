package net.xiaoyu.mob_controller.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 混入 PetConnect 的宠物搜索方法，使其能识别 mob_controller 的受控生物。
 * 使用 @Pseudo 标记，即使 PetConnect 不存在也不会导致崩溃。
 */
@Pseudo
@Mixin(targets = "com.wolf.petconnect.PetConnectEventHandler")
public abstract class PetConnectEventHandlerMixin {

    /**
     * 修改 findTamedPets 方法，将受控生物添加到宠物列表中。
     */
    @Inject(
            method = "findTamedPets(Lnet/minecraft/world/entity/player/Player;DLnet/minecraft/core/BlockPos;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = false,
            remap = false // PetConnect 不是原版类，不重映射
    )
    private static void onFindTamedPets(Player player, double range, BlockPos blockPos,
                                        CallbackInfoReturnable<List<LivingEntity>> cir) {
        List<LivingEntity> originalList = cir.getReturnValue();
        // PetConnect 返回的是 ArrayList，可以直接添加
        if (originalList == null) return;

        Level level = player.level();
        AABB area = new AABB(blockPos).inflate(range);
        List<Mob> controlledMobs = level.getEntitiesOfClass(Mob.class, area,
                mob -> MobControlledData.isControlledEntity(mob)
                        && Objects.equals(MobControlledData.getControllerUUID(mob), player.getUUID()));

        // 添加受控生物到结果列表
        originalList.addAll(controlledMobs);
    }

    /**
     * 修改 findTamedPets1 方法（用于从特殊维度召回宠物）。
     * 逻辑同上，但搜索范围固定为 10 格（PetConnect 内部写死）。
     */
    @Inject(
            method = "findTamedPets1(Lnet/minecraft/world/entity/player/Player;D)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = false,
            remap = false
    )
    private static void onFindTamedPets1(Player player, double range,
                                         CallbackInfoReturnable<List<LivingEntity>> cir) {
        List<LivingEntity> originalList = cir.getReturnValue();
        if (originalList == null) return;

        // PetConnect 的 findTamedPets1 固定搜索维度 petconnect:petconnect 中 y=-510 附近 10 格
        // 我们直接复用其内部逻辑，也可自己获取目标世界，但为避免重复计算，这里简单地再次获取受控生物
        // 注意：受控生物通常不会待在那个特殊维度，所以这段其实可选，为了完整性也加上。
        // 实际受控生物被收容后会传送到那个维度，因此后续召回时需要能检测到。
        Level targetWorld = player.getServer() != null
                ? player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation("petconnect:petconnect")))
                : null;
        if (targetWorld == null) return;

        AABB area = new AABB(new BlockPos(0, -510, 0)).inflate(range);
        List<Mob> controlledMobs = targetWorld.getEntitiesOfClass(Mob.class, area,
                mob -> MobControlledData.isControlledEntity(mob)
                        && Objects.equals(MobControlledData.getControllerUUID(mob), player.getUUID()));
        originalList.addAll(controlledMobs);
    }
}