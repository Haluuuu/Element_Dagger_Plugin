package com.haluuu.interactions;


import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class TeleportDagger extends SimpleInstantInteraction {
    public static final BuilderCodec<TeleportDagger> CODEC = BuilderCodec.builder(TeleportDagger.class, TeleportDagger::new, SimpleInstantInteraction.CODEC)

            .build();
    @Override
    protected void firstRun(@NonNullDecl InteractionType type, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {

        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) return;

        Ref<EntityStore> playerRef = context.getEntity();

        TransformComponent transformComponent = commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
        if (transformComponent == null) return;

        // 📍 Posición ACTUAL del proyectil (la daga)
        Vector3d impactPosition = transformComponent.getPosition();

        // 🛡️ Buscar posición segura para no quedar dentro de bloques
        Vector3d safePos = findSafeTeleportPosition(commandBuffer, impactPosition);
        if (safePos != null) {
            impactPosition = safePos;
        }

        HeadRotation headRotation = commandBuffer.getComponent(playerRef, HeadRotation.getComponentType());
        Vector3f bodyRot = transformComponent.getRotation();
        Vector3f headRot = headRotation != null ? headRotation.getRotation() : bodyRot;

        // 🚀 Teleport real
        Teleport teleport = new Teleport(impactPosition, bodyRot).setHeadRotation(headRot);
        commandBuffer.addComponent(playerRef, Teleport.getComponentType(), teleport);
    }

    @Nullable
    private Vector3d findSafeTeleportPosition(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Vector3d targetPos) {
        World world = ((EntityStore)commandBuffer.getExternalData()).getWorld();
        int blockX = MathUtil.floor(targetPos.x);
        int blockY = MathUtil.floor(targetPos.y);
        int blockZ = MathUtil.floor(targetPos.z);
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(blockX, blockZ));
        if (chunk == null)
            return null;
        int scanUp = 0;
        while (scanUp < 10) {
            int checkY = blockY + scanUp;
            if (checkY >= 320)
                break;
            BlockType blockType = chunk.getBlockType(blockX, checkY, blockZ);
            if (blockType != null && blockType.getMaterial() == BlockMaterial.Solid)
                scanUp++;
        }
        int groundY = -1;
        int startY = blockY + scanUp;
        int dy;
        for (dy = 0; dy < 20 && startY - dy >= 0; dy++) {
            int checkY = startY - dy;
            BlockType blockType = chunk.getBlockType(blockX, checkY, blockZ);
            if (blockType != null && blockType.getMaterial() == BlockMaterial.Solid) {
                groundY = checkY;
                break;
            }
        }
        if (groundY >= 0) {
            double safeY = groundY + 1.0D + 0.1D;
            return new Vector3d(targetPos.x, safeY, targetPos.z);
        }
        for (dy = 0; dy < 10; dy++) {
            int checkY = blockY + scanUp + dy;
            if (checkY >= 320)
                break;
            BlockType blockType = chunk.getBlockType(blockX, checkY, blockZ);
            if ((blockType == null || blockType.getMaterial() != BlockMaterial.Solid) &&
                    checkY > 0) {
                BlockType belowBlockType = chunk.getBlockType(blockX, checkY - 1, blockZ);
                if (belowBlockType != null && belowBlockType.getMaterial() == BlockMaterial.Solid)
                    return new Vector3d(targetPos.x, checkY + 0.1D, targetPos.z);
            }
        }
        return new Vector3d(targetPos.x, Math.max(targetPos.y + 0.5D, blockY + 1.1D), targetPos.z);
    }
}
