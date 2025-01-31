package core.assets;

import java.util.List;

interface BaseAssetResolver extends AssetResolver {
    List<AssetsManager.Asset<?>> depends();
}
