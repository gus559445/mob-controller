// ============================================================
// 源文件: C:/Users/Mnibr/Desktop/生物控制器源码/mob_controller-1.20.1-Forge/src\main\java\net\xiaoyu\mob_controller\registry\ModItems.java
// ============================================================

package net.xiaoyu.mob_controller.registry;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.item.*;

/**
 * 本模组物品注册表。
 */
public class ModItems {
    /**
     * 物品延迟注册器。
     */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MobController.MOD_ID);

    /**
     * 「触发型物品」用的空食物组件。
     *
     * <p>触屏玩家靠 TouchController 的「长按 = 按住右键」来用物品，而它判断「手上这件东西
     * 能不能长按使用」时，只看三样：物品白名单、物品子类、以及<b>是否带食物组件</b>。
     * 号角 / 驭兽哨这类本模组自制的触发物品三样都不占，于是长按会被判成「挖方块」，
     * 只有轻点一下才发一次右键——这正是「点击才能生效，长按没反应」的来源。</p>
     *
     * <p>给它们挂一个营养 0、饱和度 0 的空食物组件，就能让 TouchController 把手上的东西
     * 认成「可长按使用」。物品自身的 {@code use()} / {@code finishUsingItem()} 全部重写过，
     * 所以玩家按右键既不会真的吃掉它，也不会被消耗。</p>
     */
    private static final FoodProperties TRIGGER_FOOD = new FoodProperties.Builder()
            .nutrition(0)
            .saturationMod(0.0F)
            .alwaysEat()
            .build();

    /**
     * 生物控制器。
     */
    public static final RegistryObject<Item> MOB_CONTROLLER_ITEM = ITEMS.register(
            "mob_controller",
            // 不竭金羹本来就是饮品：补上食物组件，TouchController 才会把它当「可长按使用」的物品，
            // 长按屏幕才能一口一口喝下去（没有组件时它只认点按）。
            () -> new MobControllerItem(new Item.Properties().stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(8).saturationMod(1.6F).alwaysEat().build()))
    );

    /**
     * 控制令。
     */
    public static final RegistryObject<Item> CONTROL_COMMAND_ITEM = ITEMS.register(
            "control_command",
            () -> new ControlCommandItem(new Item.Properties().stacksTo(1).food(TRIGGER_FOOD))
    );

    /**
     * 心变契约。
     */
    public static final RegistryObject<Item> HEART_CONTRACT_ITEM = ITEMS.register(
            "heart_contract",
            () -> new HeartContractItem(new Item.Properties().stacksTo(1))
    );

    /**
     * 盔甲编辑蓝图。
     */
    public static final RegistryObject<Item> ARMOR_EDITING_BLUEPRINT = ITEMS.register(
            "armor_editing_blueprint",
            () -> new MobArmor(new Item.Properties().stacksTo(1))
    );

    /**
     * 护主切换器
     */
    public static final RegistryObject<Item> AGGRESSIVE_SWITCH_ITEM = ITEMS.register(
            "aggressive_switch",
            () -> new AggressiveSwitchItem(new Item.Properties().stacksTo(1).food(TRIGGER_FOOD))
    );


    /**
     * 骑乘令
     */
    public static final RegistryObject<Item> RIDE_COMMAND_ITEM = ITEMS.register(
            "ride_command",
            () -> new RideCommandItem(new Item.Properties().stacksTo(1))
    );

    /**
     * 创造模式生物控制器
     */
    public static final RegistryObject<Item> CREATIVE_MOB_CONTROLLER_ITEM = ITEMS.register(
            "creative_mob_controller",
            () -> new CreativeMobControllerItem(new Item.Properties().stacksTo(1))
    );

    /**
     * 五谷杂粮
     */
    public static final RegistryObject<Item> GRAIN_ITEM = ITEMS.register(
            "grain",
            () -> new GrainItem(new Item.Properties().stacksTo(64))
    );

    /**
     * 控制令切换模式版
     */
    public static final RegistryObject<Item> MODE_SELECT_CONTROL_COMMAND_ITEM = ITEMS.register(
            "mode_select_control_command",
            () -> new ModeSelectControlCommandItem(new Item.Properties().stacksTo(1).food(TRIGGER_FOOD))
    );

    public static final RegistryObject<Item> LEGION_BANNER_ITEM = ITEMS.register(
            "legion_banner",
            () -> new LegionBannerItem(new Item.Properties().stacksTo(1))
    );
}