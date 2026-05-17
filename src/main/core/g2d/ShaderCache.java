package core.g2d;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectAVLTreeMap;

public class ShaderCache {
    public static final Byte2ObjectAVLTreeMap<Shader> shadersById = new Byte2ObjectAVLTreeMap<>();
}
