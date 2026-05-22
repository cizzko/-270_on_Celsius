package core.g2d;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectAVLTreeMap;

public class ResourceCache {
    public static final Byte2ObjectAVLTreeMap<Shader> shadersById = new Byte2ObjectAVLTreeMap<>();
    public static final Short2ObjectAVLTreeMap<Texture> texturesById = new Short2ObjectAVLTreeMap<>();
}
