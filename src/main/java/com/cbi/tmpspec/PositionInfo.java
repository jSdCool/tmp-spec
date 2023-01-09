package com.cbi.tmpspec;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PositionInfo {
    public Vec3d pos;
    public RegistryKey<World> dimension;

    PositionInfo(Vec3d pos,RegistryKey<World> dim){
        this.pos=pos;
        dimension=dim;
    }
}
