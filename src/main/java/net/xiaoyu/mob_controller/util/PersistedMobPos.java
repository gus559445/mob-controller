package net.xiaoyu.mob_controller.util;

import java.util.UUID;

/**
 * 持久化的受控生物位置记录。
 */
public record PersistedMobPos(UUID mobUUID, String dimension, int x, int y, int z) {
}
