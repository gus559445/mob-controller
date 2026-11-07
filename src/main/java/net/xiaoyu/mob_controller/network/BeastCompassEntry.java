package net.xiaoyu.mob_controller.network;

import java.util.UUID;

/**
 * 野兽罗盘列表条目。
 */
public record BeastCompassEntry(UUID mobId, String name, String typeId, int x, int y, int z, int distance,
                                boolean loaded) {
}
