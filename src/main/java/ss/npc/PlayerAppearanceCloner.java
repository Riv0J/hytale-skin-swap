package ss.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.spawning.ISpawnableWithModel;
import com.hypixel.hytale.server.spawning.SpawningContext;
import it.unimi.dsi.fastutil.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Spawns an NPC whose appearance mirrors a live player's skin.
 *
 * <p>Usage:
 * <pre>
 *   PlayerAppearanceCloner.spawn()
 *       .copyFrom(senderRef)
 *       .name("Rivito")
 *       .interact()
 *       .freeze()
 *       .at(position, rotation)
 *       .execute(store);
 * </pre>
 */
public final class PlayerAppearanceCloner {

    /** Role used when no explicit role is specified. */
    public static final String DEFAULT_ROLE = "MR_CLONE_QUEUE";

    private PlayerAppearanceCloner() {}

    public static CloneRequest spawn() {
        return new CloneRequest(DEFAULT_ROLE);
    }

    public static CloneRequest spawn(@Nonnull String roleName) {
        return new CloneRequest(roleName);
    }

    // -------------------------------------------------------------------------

    public static final class CloneRequest {

        private final String roleName;
        private Ref<EntityStore> sourceRef;
        private String nameplate;
        private boolean interactable;
        private boolean frozen;
        private Vector3d position = new Vector3d(0, 0, 0);
        private Vector3f rotation = new Vector3f(0f, 0f, 0f);

        private CloneRequest(@Nonnull String roleName) {
            this.roleName = roleName;
        }

        /** The player entity whose skin will be cloned onto the NPC. */
        public CloneRequest copyFrom(@Nonnull Ref<EntityStore> playerRef) {
            this.sourceRef = playerRef;
            return this;
        }

        /** Nameplate text shown above the NPC. */
        public CloneRequest name(@Nullable String text) {
            this.nameplate = text;
            return this;
        }

        /** Adds the Interactable marker — shows interaction prompt to nearby players. */
        public CloneRequest interact() {
            this.interactable = true;
            return this;
        }

        /** Adds the Frozen marker — same effect as /npc freeze. */
        public CloneRequest freeze() {
            this.frozen = true;
            return this;
        }

        public CloneRequest at(@Nonnull Vector3d position) {
            this.position = position;
            return this;
        }

        public CloneRequest at(@Nonnull Vector3d position, @Nonnull Vector3f rotation) {
            this.position = position;
            this.rotation = rotation;
            return this;
        }

        /**
         * Executes the spawn.
         *
         * @return the spawned entity pair, or {@code null} if the source player skin
         *         could not be read or the role is invalid.
         */
        @Nullable
        public Pair<Ref<EntityStore>, NPCEntity> execute(@Nonnull Store<EntityStore> store) {
            if (sourceRef == null) return null;

            // Read the source player's skin
            PlayerSkinComponent skinComp =
                    store.getComponent(sourceRef, PlayerSkinComponent.getComponentType());
            if (skinComp == null) return null;

            // Spawn base NPC from role
            NPCPlugin npcPlugin = NPCPlugin.get();
            int roleIndex = npcPlugin.getIndex(roleName);
            if (roleIndex < 0) return null;

            Builder<Role> roleBuilder = npcPlugin.tryGetCachedValidRole(roleIndex);
            if (roleBuilder == null || !roleBuilder.isSpawnable()) return null;
            if (!(roleBuilder instanceof ISpawnableWithModel spawnable)) return null;

            SpawningContext ctx = new SpawningContext();
            if (!ctx.setSpawnable(spawnable)) return null;

            Pair<Ref<EntityStore>, NPCEntity> pair =
                    npcPlugin.spawnEntity(store, roleIndex, position, rotation, ctx.getModel(), null);
            if (pair == null) return null;

            Ref<EntityStore> npcRef = pair.first();

            // Override model with the player's live skin
            Model clonedModel = CosmeticsModule.get().createModel(skinComp.getPlayerSkin(), 1.0f);
            store.putComponent(npcRef, ModelComponent.getComponentType(), new ModelComponent(clonedModel));

            if (nameplate != null) {
                store.ensureAndGetComponent(npcRef, Nameplate.getComponentType()).setText(nameplate);
            }
            if (interactable) {
                store.ensureComponent(npcRef, Interactable.getComponentType());
            }
            if (frozen) {
                store.ensureComponent(npcRef, Frozen.getComponentType());
            }

            return pair;
        }
    }
}
