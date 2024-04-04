package core.World.Creatures.Player.Inventory.Items;

import core.EventHandling.Logging.Config;
import core.Global;
import core.Utils.Sized;
import core.World.Creatures.Player.Inventory.Items.Weapons.Weapons;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.assets.AssetsManager;
import core.g2d.Atlas;

import java.io.Serializable;
import java.util.Properties;

import static core.Global.assets;

public class Items implements Serializable {
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

    private static DefaultValues getDefault(Properties properties) {
        Atlas.Region texture = Global.atlas.byPath((String) properties.getOrDefault("Path", "/World/textureNotFound.png"));
        String description = (String) properties.getOrDefault("Description", "");
        String name = (String) properties.getOrDefault("Name", "");
        String requiredForBuild = (String) properties.getOrDefault("RequiredForBuild", null);

        if (requiredForBuild != null) {
            String[] required = requiredForBuild.split(",");
            //required for build items
            Items[] output = new Items[required.length];

            for (int i = 0; i < required.length; i++) {
                required[i] = AssetsManager.normalizePath(required[i]);
                output[i] = createItem(required[i]);
            }
            return new DefaultValues(name, texture, description, output);
        }

        return new DefaultValues(name, texture, description, null);
    }

    public static Items createWeapon(String fileName) {
        Properties weapon = Config.getProperties(assets.assetsDir("/World/ItemsCharacteristics/" + fileName + ".properties"));
        fileName = AssetsManager.normalizePath(fileName);
        DefaultValues defaultValues = getDefault(weapon);

        int id = fileName.hashCode();
        float fireRate = Float.parseFloat((String) weapon.getOrDefault("FireRate", "100"));
        float damage = Float.parseFloat((String) weapon.getOrDefault("Damage", "100"));
        float ammoSpeed = Float.parseFloat((String) weapon.getOrDefault("AmmoSpeed", "100"));
        float reloadTime = Float.parseFloat((String) weapon.getOrDefault("ReloadTime", "100"));
        float bulletSpread = Float.parseFloat((String) weapon.getOrDefault("BulletSpread", "0"));
        int magazineSize = Integer.parseInt((String) weapon.getOrDefault("MagazineSize", "10"));
        String path1 = (String) weapon.getOrDefault("Sound", null);
        String sound = assets.assetsDir(path1);
        String path = (String) weapon.getOrDefault("BulletPath", "World/Items/someBullet.png");
        String bulletPath = assets.assetsDir(path);
        Weapons.Types type = Weapons.Types.valueOf((String) weapon.getOrDefault("Type", "BULLET"));

        return new Items(new Weapons(fireRate, damage, ammoSpeed, reloadTime, bulletSpread, magazineSize, sound, bulletPath, type), (short) 0, null, null, id, defaultValues.texture(), defaultValues.description, defaultValues.name, fileName, defaultValues.requiredForBuild, Types.WEAPON);
    }

    public static Items createTool(String fileName) {
        Properties tool = Config.getProperties(assets.assetsDir("/World/ItemsCharacteristics/" + fileName + ".properties"));
        fileName = AssetsManager.normalizePath(fileName);
        DefaultValues defaultValues = getDefault(tool);

        int id = fileName.hashCode();
        float maxHp = Float.parseFloat((String) tool.getOrDefault("MaxHp", "100"));
        float damage = Float.parseFloat((String) tool.getOrDefault("Damage", "30"));
        float secBetweenHits = Float.parseFloat((String) tool.getOrDefault("SecBetweenHits", "100"));
        float maxInteractionRange = Float.parseFloat((String) tool.getOrDefault("MaxInteractionRange", "8"));

        return new Items(null, (short) 0, new Tools(maxHp, damage, secBetweenHits, maxInteractionRange), null, id, defaultValues.texture(), defaultValues.description, defaultValues.name, fileName, defaultValues.requiredForBuild, Types.TOOL);
    }

    public static Items createPlaceable(short placeable) {
        Properties detail = Config.getProperties(assets.assetsDir("/World/ItemsCharacteristics/" + StaticWorldObjects.getFileName(StaticWorldObjects.getId(placeable)) + ".properties"));
        DefaultValues defaultValues = getDefault(detail);
        StaticObjectsConst placeableProp = StaticObjectsConst.getConst(StaticWorldObjects.getId(placeable));
        int id = StaticWorldObjects.getId(placeable);

        return new Items(null, placeable, null, null, id, placeableProp.texture, "", placeableProp.objectName, StaticWorldObjects.getFileName(placeable), defaultValues.requiredForBuild, Types.PLACEABLE);
    }

    public static Items createDetail(String fileName) {
        Properties detail = Config.getProperties(assets.assetsDir("/World/ItemsCharacteristics/" + fileName + ".properties"));
        fileName = AssetsManager.normalizePath(fileName);
        DefaultValues defaultValues = getDefault(detail);
        int id = fileName.hashCode();

        return new Items(null, (short) 0, null, new Details(""), id, defaultValues.texture(), defaultValues.description, defaultValues.name, fileName, defaultValues.requiredForBuild, Types.DETAIL);
    }

    public static Items createItem(String fileName) {
        if (fileName.toLowerCase().startsWith("blocks")) {
            return createPlaceable(StaticWorldObjects.createStatic(fileName));
        } else if (fileName.toLowerCase().startsWith("weapons")) {
            return createWeapon(fileName);
        } else if (fileName.toLowerCase().startsWith("details")) {
            return createDetail(fileName);
        } else if (fileName.toLowerCase().startsWith("tools")) {
            return createTool(fileName);
        }
        return null;
    }

    public static Items createItem(short placeable) {
        return createPlaceable(placeable);
    }

    public static float computeZoom(Sized size) {
        //32 - целевой размер текстуры
        return 32f / (Math.max(size.width(), size.height()));
    }
}
