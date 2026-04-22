package core.World.Creatures.Player.Inventory.Items;

import com.google.gson.JsonObject;
import core.Global;
import core.World.Creatures.Player.Inventory.Items.Weapons.Weapons;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.assets.AssetsManager;
import core.g2d.Atlas;
import core.util.Sized;
import core.util.StringUtils;

import java.io.Serializable;
import java.util.Map;

public class Items implements Serializable, Cloneable {
    public Items[] requiredForBuild;
    public Weapons weapon;
    public short placeable;
    public Tools tool;
    public Details detail;
    public int id, countInCell;
    public String description, name, filename;
    public Atlas.Region texture;
    public Types type;

    private record DefaultValues(String name, Atlas.Region texture, String description, Items[] requiredForBuild) {}

    public enum Types {
        TOOL,
        WEAPON,
        PLACEABLE,
        DETAIL
    }

    private Items(Weapons weapon, short placeable, Tools tool, Details detail, int id, Atlas.Region texture, String description, String name, String filename, Items[] requiredForBuild, Types type) {
        this.name = name;
        this.weapon = weapon;
        this.placeable = placeable;
        this.tool = tool;
        this.detail = detail;
        this.id = id;
        this.texture = texture;
        this.description = description;
        this.filename = filename;
        this.requiredForBuild = requiredForBuild;
        this.type = type;
    }

    //todo поленился сделать красиво, потом

    private static DefaultValues getDefault(JsonObject json) {
        String path = json.has("Texture") ? json.get("Texture").getAsString() : "World/textureNotFound.png";
        Atlas.Region texture = Global.atlas.byPath(path);

        String name = json.has("Name") ? json.get("Name").getAsString() : "";
        String description = json.has("Description") ? json.get("Description").getAsString() : "";

        Items[] output = null;

        if (json.has("Requirements") && json.get("Requirements").isJsonObject()) {
            JsonObject reqs = json.getAsJsonObject("Requirements");
            var keys = reqs.keySet();
            output = new Items[keys.size()];

            int i = 0;
            for (String itemKey : keys) {
                output[i] = createItem(AssetsManager.normalizePath(itemKey));
                i++;
            }
        }

        return new DefaultValues(name, texture, description, output);
    }

    public static Items createWeapon(String fileName) {
        JsonObject json = Global.assets.jsonReader("World/ItemsCharacteristics/" + fileName + ".json");
        DefaultValues defaultValues = getDefault(json);

        int id = fileName.hashCode();

        float fireRate = json.has("FireRate") ? json.get("FireRate").getAsFloat() : 100f;
        float damage = json.has("Damage") ? json.get("Damage").getAsFloat() : 100f;
        float ammoSpeed = json.has("AmmoSpeed") ? json.get("AmmoSpeed").getAsFloat() : 100f;
        float reloadTime = json.has("ReloadTime") ? json.get("ReloadTime").getAsFloat() : 100f;
        float bulletSpread = json.has("BulletSpread") ? json.get("BulletSpread").getAsFloat() : 0f;
        int magazineSize = json.has("MagazineSize") ? json.get("MagazineSize").getAsInt() : 10;

        String sound = json.has("Sound") ? AssetsManager.normalizePath(json.get("Sound").getAsString()) : null;
        String bulletPath = json.has("BulletPath") ? AssetsManager.normalizePath(json.get("BulletPath").getAsString()) : "World/Items/someBullet.png";

        Weapons.Types type = json.has("Type") ? Weapons.Types.valueOf(json.get("Type").getAsString().toUpperCase()) : Weapons.Types.BULLET;

        return new Items(new Weapons(fireRate, damage, ammoSpeed, reloadTime, bulletSpread, magazineSize, sound, bulletPath, type), (short) 0, null, null, id, defaultValues.texture(), defaultValues.description, defaultValues.name, fileName, defaultValues.requiredForBuild, Types.WEAPON);
    }

    public static Items createTool(String fileName) {
        JsonObject json = Global.assets.jsonReader("World/ItemsCharacteristics/" + fileName + ".json");
        DefaultValues defaultValues = getDefault(json);

        int id = fileName.hashCode();

        float maxHp = json.has("Max-hp") ? json.get("Max-hp").getAsFloat() : 100f;
        float damage = json.has("Damage") ? json.get("Damage").getAsFloat() : 30f;
        float secBetweenHits = json.has("SecBetweenHits") ? json.get("SecBetweenHits").getAsFloat() : 100f;
        float maxInteractionRange = json.has("MaxInteractionRange") ? json.get("MaxInteractionRange").getAsFloat() : 8f;

        return new Items(null, (short) 0, new Tools(maxHp, damage, secBetweenHits, maxInteractionRange), null, id, defaultValues.texture(), defaultValues.description, defaultValues.name, fileName, defaultValues.requiredForBuild, Types.TOOL);
    }

    public static Items createPlaceable(short placeable) {
        String fileName = StaticWorldObjects.getFileName(StaticWorldObjects.getId(placeable));
        JsonObject json = Global.assets.jsonReader("World/ItemsCharacteristics/" + fileName + ".json");
        DefaultValues defaultValues = getDefault(json);

        StaticObjectsConst placeableProp = StaticObjectsConst.getConst(StaticWorldObjects.getId(placeable));
        int id = StaticWorldObjects.getId(placeable);

        return new Items(null, placeable, null, null, id, placeableProp.texture, "", placeableProp.objectName, fileName, defaultValues.requiredForBuild, Types.PLACEABLE);
    }

    public static Items createDetail(String fileName) {
        JsonObject json = Global.assets.jsonReader("World/ItemsCharacteristics/" + fileName + ".json");
        String normPath = AssetsManager.normalizePath(fileName);
        DefaultValues defaultValues = getDefault(json);

        return new Items(null, (short) 0, null, new Details(""), normPath.hashCode(), defaultValues.texture(), defaultValues.description, defaultValues.name, normPath, defaultValues.requiredForBuild, Types.DETAIL);
    }


    public static Items createItem(String fileName) {
        String path = StringUtils.normalizePath(fileName.toLowerCase());

        return switch (path.split("/")[0]) {
            case "blocks"  -> createPlaceable(StaticWorldObjects.createStatic(fileName));
            case "weapons" -> createWeapon(fileName);
            case "details" -> createDetail(fileName);
            case "tools"   -> createTool(fileName);
            default        -> null;
        };
    }

    public static Items createItem(short placeable) {
        return createPlaceable(placeable);
    }

    public static float computeZoom(Sized size) {
        // 32 - target structure size
        return 32f / Math.max(size.width(), size.height());
    }

    @Override
    public Items clone() {
        try {
            return (Items) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
