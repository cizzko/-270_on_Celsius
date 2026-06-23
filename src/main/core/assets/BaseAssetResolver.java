package core.assets;

import java.util.List;

sealed interface BaseAssetResolver
        extends AssetResolver
        permits AsyncAssetResolver, SyncAssetResolver {

    List<AssetsManager.Asset<?>> depends();
}
