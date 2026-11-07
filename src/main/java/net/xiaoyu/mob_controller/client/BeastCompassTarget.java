package net.xiaoyu.mob_controller.client;

import java.util.UUID;

/**
 * 野兽罗盘当前锁定的目标。
 */
public record BeastCompassTarget(UUID mobId, String name, double x, double y, double z, boolean loaded) {
}
