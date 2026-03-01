package ss;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import ss.npc.SkinModelExporter;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.List;

public class SSCommand extends AbstractCommandCollection {

    public SSCommand() {
        super("ss", "Skin Swap commands");

        this.addSubCommand(new SSCloneCommand());
        this.addSubCommand(new SSListCommand());
        this.addSubCommand(new SSDeleteCommand());
        this.addSubCommand(new SSSwapCommand());
    }

    // ── /ss clone <name> [player] ──────────────────────────────────────────

    private static class SSCloneCommand extends AbstractPlayerCommand {
        private final RequiredArg<String>    nameArg   = withRequiredArg("name", "Skin name to save as", ArgTypes.STRING);
        private final OptionalArg<PlayerRef> playerArg = withOptionalArg("player", "Player to clone (default: yourself)", ArgTypes.PLAYER_REF);

        SSCloneCommand() {
            super("clone", "Clone your (or another player's) current skin appearance");
            this.requirePermission(HytalePermissions.fromCommand("ss.clone"));
        }

        @Override
        protected void execute(
                @Nonnull CommandContext ctx,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            String name    = nameArg.get(ctx);
            String fullId  = SkinModelExporter.PREFIX + name;
            boolean update = SkinModelExporter.exists(name);

            if (update) send(ctx, "'" + fullId + "' already exists — overwriting.");

            PlayerRef target = playerArg.provided(ctx) ? playerArg.get(ctx) : playerRef;
            PlayerSkinComponent skinComp = target.getComponent(PlayerSkinComponent.getComponentType());
            if (skinComp == null) {
                send(ctx, "Could not read skin data for " + target.getUsername() + ".");
                return;
            }

            try {
                Path output = SkinModelExporter.export(skinComp.getPlayerSkin(), name);
                boolean isSelf = target.equals(playerRef);
                String source  = isSelf ? "your character" : "player " + target.getUsername();
                send(ctx, (update ? "Updated" : "Saved") + " '" + name + "' from " + source
                        + "\nAsset ID: " + fullId);
            } catch (Exception e) {
                send(ctx, "Export failed: " + e.getMessage());
            }
        }
    }

    // ── /ss list ───────────────────────────────────────────────────────────

    private static class SSListCommand extends AbstractPlayerCommand {
        SSListCommand() {
            super("list", "List all saved skins");
            this.requirePermission(HytalePermissions.fromCommand("ss.list"));
        }

        @Override
        protected void execute(
                @Nonnull CommandContext ctx,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            List<String> names = SkinModelExporter.listNames();
            if (names.isEmpty()) {
                send(ctx, "No saved skins. Use /ss clone <name> to save one.");
                return;
            }
            send(ctx, "Saved skins (" + names.size() + "):\n" + String.join(", ", names));
        }
    }

    // ── /ss delete <name> ─────────────────────────────────────────────────

    private static class SSDeleteCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> nameArg = withRequiredArg("name", "Skin name to delete", ArgTypes.STRING);

        SSDeleteCommand() {
            super("delete", "Delete a saved skin");
            this.requirePermission(HytalePermissions.fromCommand("ss.delete"));
        }

        @Override
        protected void execute(
                @Nonnull CommandContext ctx,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            String name = nameArg.get(ctx);
            if (SkinModelExporter.delete(name)) {
                send(ctx, "Deleted: " + name);
            } else {
                send(ctx, "Skin not found: '" + name + "'. Use /ss list.");
            }
        }
    }

    // ── /ss swap <name> ───────────────────────────────────────────────────

    private static class SSSwapCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> nameArg = withRequiredArg("name", "Skin name to apply", ArgTypes.STRING);

        SSSwapCommand() {
            super("swap", "Apply a saved skin to your appearance");
            this.requirePermission(HytalePermissions.fromCommand("ss.swap"));
        }

        @Override
        protected void execute(
                @Nonnull CommandContext ctx,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            String name = nameArg.get(ctx);
            PlayerSkin skin = SkinModelExporter.loadSkinData(name);
            if (skin == null) {
                send(ctx, "Skin '" + name + "' not found. Use /ss list.");
                return;
            }

            store.putComponent(ref, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(skin));
            send(ctx, "Skin swapped to: " + name);
        }
    }

    // ── Shared helpers ─────────────────────────────────────────────────────

    private static void send(CommandContext ctx, String text) {
        ctx.sender().sendMessage(cmdMsg(text));
    }

    private static Message cmdMsg(String text) {
        return Message.raw("[Skin Swap] ").color("#FFD700").bold(true)
                .insert(Message.raw(text).color("#ffffff"));
    }
}
