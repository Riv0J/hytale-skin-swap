package ss.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPartTexture;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Converts a player's live {@link PlayerSkin} into a Model JSON asset file
 * and writes it to a per-skin subfolder inside {@link #SKINS_DIR}.
 *
 * <p>Structure written per skin:
 * <pre>
 *   exports/skins/NAME/SS_MODEL_NAME.json      — ModelAsset for NPC Appearance
 *   exports/skins/NAME/SS_SKINDATA_NAME.json   — raw PlayerSkin data for /ss swap
 * </pre>
 */
public final class SkinModelExporter {

    /** Root directory for all saved skins. */
    public static final Path   SKINS_DIR        = Path.of("exports", "skins");
    /** Prefix for Model JSON files. */
    public static final String PREFIX           = "SS_MODEL_";
    /** Prefix for raw skin-data files. */
    public static final String PREFIX_SKINDATA  = "SS_SKIN_";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SkinModelExporter() {}

    // ── Public API ─────────────────────────────────────────────────────────

    /** Returns the subfolder for a given skin name: {@code exports/skins/<name>}. */
    public static Path skinDir(String name) {
        return SKINS_DIR.resolve(name);
    }

    /**
     * Returns {@code true} if a ModelAsset named {@code PREFIX + name} is already
     * registered in the runtime asset store.
     */
    public static boolean exists(String name) {
        try {
            return ModelAsset.getAssetStore().getAssetMap().getAsset(PREFIX + name) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Exports the player skin into {@code exports/skins/<name>/}:
     * <ul>
     *   <li>{@code SS_MODEL_<name>.json}    — Model JSON (for NPC Appearance)</li>
     *   <li>{@code SS_SKINDATA_<name>.json} — raw PlayerSkin data (for /ss swap)</li>
     * </ul>
     *
     * @return the path of the written Model JSON
     */
    public static Path export(PlayerSkin skin, String name) throws IOException {
        Path dir = skinDir(name);
        Files.createDirectories(dir);

        Path modelOut = dir.resolve(PREFIX + name + ".json");
        Files.writeString(modelOut, buildModelJson(skin));

        Files.writeString(dir.resolve(PREFIX_SKINDATA + name + ".json"),
                GSON.toJson(SkinSnapshot.of(skin)));

        final Path absPath = modelOut.toAbsolutePath();
        CompletableFuture.runAsync(() -> {
            try {
                ModelAsset.getAssetStore().loadAssetsFromPaths("ss:exports", List.of(absPath));
            } catch (Exception e) {
                System.err.println("[ss] Runtime asset load failed: " + e.getMessage());
            }
        });

        return modelOut;
    }

    /**
     * Loads the raw {@link PlayerSkin} previously saved by {@link #export}.
     * Returns {@code null} if no data file exists for this name.
     */
    @Nullable
    public static PlayerSkin loadSkinData(String name) {
        try {
            Path skinFile = skinDir(name).resolve(PREFIX_SKINDATA + name + ".json");
            if (!Files.exists(skinFile)) return null;
            SkinSnapshot snap = GSON.fromJson(Files.readString(skinFile), SkinSnapshot.class);
            PlayerSkin skin = GSON.fromJson(GSON.toJson(snap), PlayerSkin.class);
            System.out.println("[ss] Loaded skin '" + name + "': body=" + skin.bodyCharacteristic
                    + " face=" + skin.face + " haircut=" + skin.haircut);
            return skin;
        } catch (Exception e) {
            System.err.println("[ss] Failed to load skin data for '" + name + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Deletes the entire subfolder for the given skin name.
     * Returns {@code true} if the folder existed and was deleted.
     */
    public static boolean delete(String name) {
        Path dir = skinDir(name);
        if (!Files.exists(dir)) return false;
        try {
            Files.deleteIfExists(dir.resolve(PREFIX + name + ".json"));
            Files.deleteIfExists(dir.resolve(PREFIX_SKINDATA + name + ".json"));
            Files.deleteIfExists(dir);
            return true;
        } catch (IOException e) {
            System.err.println("[ss] Failed to delete skin '" + name + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Lists all skin names available in {@link #SKINS_DIR} (one subfolder per skin).
     */
    public static List<String> listNames() {
        try {
            if (!Files.exists(SKINS_DIR)) return List.of();
            return Files.list(SKINS_DIR)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            System.err.println("[ss] Failed to list skins: " + e.getMessage());
            return List.of();
        }
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private record SkinSnapshot(
            String bodyCharacteristic, String face, String ears, String eyes,
            String eyebrows, String mouth, String facialHair, String haircut,
            String underwear, String undertop, String overtop, String pants,
            String overpants, String shoes, String gloves, String skinFeature,
            String headAccessory, String faceAccessory, String earAccessory, String cape) {

        static SkinSnapshot of(PlayerSkin s) {
            return new SkinSnapshot(
                    s.bodyCharacteristic, s.face, s.ears, s.eyes, s.eyebrows,
                    s.mouth, s.facialHair, s.haircut, s.underwear, s.undertop,
                    s.overtop, s.pants, s.overpants, s.shoes, s.gloves,
                    s.skinFeature, s.headAccessory, s.faceAccessory, s.earAccessory, s.cape);
        }
    }

    public static String buildModelJson(PlayerSkin skin) {
        CosmeticRegistry reg = CosmeticsModule.get().getRegistry();

        JsonObject root = new JsonObject();
        root.addProperty("Parent", "Player");

        JsonArray attachments = new JsonArray();
        String skinGradientId = resolveSkinGradientId(skin.bodyCharacteristic);

        addPart(attachments, skin.bodyCharacteristic, reg.getBodyCharacteristics(), skinGradientId);
        addPart(attachments, skin.face,       reg.getFaces(),       skinGradientId);
        addPart(attachments, skin.ears,       reg.getEars(),        skinGradientId);
        addPart(attachments, skin.eyes,       reg.getEyes(),        null);
        addPart(attachments, skin.eyebrows,   reg.getEyebrows(),    null);
        addPart(attachments, skin.mouth,      reg.getMouths(),      skinGradientId);
        addPart(attachments, skin.facialHair, reg.getFacialHairs(), null);
        addPart(attachments, skin.haircut,    reg.getHaircuts(),    null);
        addPart(attachments, skin.underwear,  reg.getUnderwear(),   null);
        addPart(attachments, skin.undertop,   reg.getUndertops(),   null);
        addPart(attachments, skin.overtop,    reg.getOvertops(),    null);
        addPart(attachments, skin.pants,      reg.getPants(),       null);
        addPart(attachments, skin.overpants,  reg.getOverpants(),   null);
        addPart(attachments, skin.shoes,      reg.getShoes(),       null);
        addPart(attachments, skin.gloves,         reg.getGloves(),          null);
        addPart(attachments, skin.skinFeature,    reg.getSkinFeatures(),    null);
        addPart(attachments, skin.headAccessory,  reg.getHeadAccessories(), null);
        addPart(attachments, skin.faceAccessory,  reg.getFaceAccessories(), null);
        addPart(attachments, skin.earAccessory,   reg.getEarAccessories(),  null);
        addPart(attachments, skin.cape,           reg.getCapes(),           null);

        if (attachments.size() > 0) root.add("DefaultAttachments", attachments);
        root.addProperty("GradientSet", "Skin");
        root.addProperty("GradientId", skinGradientId);

        return GSON.toJson(root);
    }

    // ── Part helpers ───────────────────────────────────────────────────────

    private static void addPart(
            JsonArray arr,
            @Nullable String rawId,
            @Nullable Map<String, PlayerSkinPart> collection,
            @Nullable String fallbackGradientId
    ) {
        if (rawId == null || collection == null) return;

        String[] parts = rawId.split("\\.", 3);
        String assetId   = parts[0];
        String textureId = parts.length > 1 ? parts[1] : null;
        String variantId = parts.length > 2 ? parts[2] : null;

        PlayerSkinPart part = collection.get(assetId);
        if (part == null) return;

        String model, texture, gradientSet, gradientId;

        if (variantId != null && part.getVariants() != null) {
            PlayerSkinPart.Variant variant = part.getVariants().get(variantId);
            if (variant == null) return;
            model       = variant.getModel();
            texture     = pickTexture(variant.getGreyscaleTexture(), variant.getTextures(), textureId);
            gradientSet = hasPreColored(variant.getTextures(), textureId) ? null : part.getGradientSet();
        } else {
            model       = part.getModel();
            texture     = pickTexture(part.getGreyscaleTexture(), part.getTextures(), textureId);
            gradientSet = hasPreColored(part.getTextures(), textureId) ? null : part.getGradientSet();
        }

        if ("Skin".equals(gradientSet)) {
            gradientId = (fallbackGradientId != null) ? fallbackGradientId : textureId;
        } else if (gradientSet != null) {
            gradientId = textureId;
        } else if (fallbackGradientId != null) {
            gradientSet = "Skin";
            gradientId  = fallbackGradientId;
        } else {
            gradientId = null;
        }

        if (model == null) return;

        JsonObject obj = new JsonObject();
        obj.addProperty("Model", model);
        if (texture     != null) obj.addProperty("Texture",     texture);
        if (gradientSet != null) obj.addProperty("GradientSet", gradientSet);
        if (gradientId  != null) obj.addProperty("GradientId",  gradientId);
        arr.add(obj);
    }

    @Nullable
    private static String pickTexture(
            @Nullable String greyscale,
            @Nullable Map<String, PlayerSkinPartTexture> textures,
            @Nullable String textureId
    ) {
        if (textures != null && textureId != null) {
            PlayerSkinPartTexture pre = textures.get(textureId);
            if (pre != null) return pre.getTexture();
        }
        return greyscale;
    }

    private static boolean hasPreColored(
            @Nullable Map<String, PlayerSkinPartTexture> textures,
            @Nullable String textureId
    ) {
        return textures != null && textureId != null && textures.containsKey(textureId);
    }

    private static String resolveSkinGradientId(@Nullable String rawBodyChar) {
        if (rawBodyChar == null) return "02";
        String[] parts = rawBodyChar.split("\\.", 3);
        return parts.length > 1 ? parts[1] : "02";
    }
}
