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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.xiaoyu.mob_controller.util.MobDimensionTeleporter;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

    /** 契约之戒右键前后的乘客快照：宠物 UUID -> 乘客 UUID 列表。 */
    private static final Map<UUID, List<UUID>> MOB_CONTROLLER_PASSENGERS = new ConcurrentHashMap<>();

    /**
     * 右键前记录：附近受控生物身上骑着的实体。
     */
    @Inject(method = "rightClick", at = @At("HEAD"), remap = false, require = 0)
    private static void mobController$snapshotRiderPassengers(PlayerInteractEvent.RightClickItem event, CallbackInfo ci) {
        MOB_CONTROLLER_PASSENGERS.clear();
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (Entity entity : serverLevel.getEntities().getAll()) {
            if (!(entity instanceof LivingEntity living) || living.getPassengers().isEmpty()) {
                continue;
            }
            if (!MobControlledData.isControlledEntity(living)) {
                continue;
            }
            List<UUID> riders = new ArrayList<>();
            for (Entity passenger : living.getPassengers()) {
                riders.add(passenger.getUUID());
            }
            MOB_CONTROLLER_PASSENGERS.put(living.getUUID(), riders);
        }
    }

    /**
     * 右键后恢复：被收容/放出的宠物如果换了维度，把乘客也带过去并保持骑乘。
     */
    @Inject(method = "rightClick", at = @At("RETURN"), remap = false, require = 0)
    private static void mobController$restoreRiderPassengers(PlayerInteractEvent.RightClickItem event, CallbackInfo ci) {
        if (MOB_CONTROLLER_PASSENGERS.isEmpty() || !(event.getEntity() instanceof ServerPlayer player)) {
            MOB_CONTROLLER_PASSENGERS.clear();
            return;
        }
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, List<UUID>> entry : MOB_CONTROLLER_PASSENGERS.entrySet()) {
            Entity mount = mobController$findEntity(player, entry.getKey());
            if (mount == null || mount.isRemoved()) {
                stale.add(entry.getKey());
                continue;
            }
            for (UUID riderId : entry.getValue()) {
                Entity rider = mobController$findEntity(player, riderId);
                if (rider == null || rider.isRemoved() || rider.getVehicle() == mount) {
                    continue;
                }
                if (rider.level() == mount.level()) {
                    rider.startRiding(mount, true);
                } else if (mount.level() instanceof ServerLevel mountLevel) {
                    Entity moved = rider.changeDimension(mountLevel,
                            new MobDimensionTeleporter(mount.getX(), mount.getY(), mount.getZ()));
                    if (moved != null) {
                        moved.startRiding(mount, true);
                    }
                }
            }
        }
        for (UUID id : stale) {
            MOB_CONTROLLER_PASSENGERS.remove(id);
        }
        MOB_CONTROLLER_PASSENGERS.clear();
    }

    /**
     * 在所有已加载维度中按 UUID 找实体。
     */
    private static Entity mobController$findEntity(ServerPlayer player, UUID uuid) {
        if (player.getServer() == null) {
            return null;
        }
        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity entity = level.getEntities().get(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

}
