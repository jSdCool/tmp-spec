package com.cbi.tmpspec;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PositionInfo {
    public Vec3 pos;
    public ResourceKey<Level> dimension;

    PositionInfo(Vec3 pos,ResourceKey<Level> dim){
        this.pos=pos;
        dimension=dim;
    }
}
