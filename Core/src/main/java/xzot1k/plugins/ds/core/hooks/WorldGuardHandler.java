/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.core.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import xzot1k.plugins.ds.DisplayShops;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;

public class WorldGuardHandler {
    private final DisplayShops instance;
    private final WorldGuardPlugin worldGuardPlugin;

    private StateFlag DS_CREATE;

    public WorldGuardHandler(DisplayShops instance) {
        this.instance = instance;
        this.worldGuardPlugin = this.getInstance().getServer().getPluginManager().getPlugin("WorldGuard") != null ? WorldGuardPlugin.inst() : null;
        if(worldGuardPlugin!=null)
            setupFlags();
    }

    private void setupFlags() {
        instance.logColor("&aInitializing WorldGuard flags");
        FlagRegistry registry=null;
        if(worldGuardPlugin.getDescription().getVersion().startsWith("6")){
            try {
                Method method = worldGuardPlugin.getClass().getMethod("getFlagRegistry");
                registry=(FlagRegistry) method.invoke(worldGuardPlugin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            registry= WorldGuard.getInstance().getFlagRegistry();
        }
        if(registry==null){
            instance.log(Level.WARNING,"Failed to initialize WorldGuard flags!");
            return;
        }
        try {
            StateFlag flag = new StateFlag("ds-create",false);
            registry.register(flag);
            DS_CREATE=flag;
        } catch (Exception e) {
            e.printStackTrace();
            Flag<?> existing = registry.get("ds-create");
            if(existing instanceof StateFlag)
                DS_CREATE=(StateFlag) existing;
        }
        if(DS_CREATE==null){
            instance.log(Level.WARNING,"Failed to initialize WorldGuard flags!");
        }else {
            instance.logColor("&aSuccessfully initialized WorldGuard flags!");
        }
    }

    public boolean handleShop(Player player,Block block){
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if(block.getLocation().getWorld()==null)
            return false;
        RegionQuery query = container.createQuery();
        StateFlag.State state = query.queryState(BukkitAdapter.adapt(block.getLocation()), WorldGuardPlugin.inst().wrapPlayer(player), DS_CREATE);
        return StateFlag.State.DENY.equals(state);
    }


    private DisplayShops getInstance() {
        return this.instance;
    }

    public Optional<WorldGuardPlugin> getWorldGuard(){
        return Optional.ofNullable(this.worldGuardPlugin);
    }
}

