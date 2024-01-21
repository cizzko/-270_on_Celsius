package core.Utils;

import org.lwjgl.system.NativeResource;

import java.util.ArrayList;
import java.util.List;

public class NativeResources {
    private static final List<NativeResource> resources = new ArrayList<>();

    public static <R extends NativeResource> R addResource(R resource) {
        resources.add(resource);
        return resource;
    }

    public static void terminateResources() {
        for (NativeResource resource : resources) {
            resource.free();
        }
    }
}
