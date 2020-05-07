package com.darwinreforged.server.sponge.utils;

import com.darwinreforged.server.core.init.DarwinServer;
import com.darwinreforged.server.core.init.UtilityImplementation;
import com.darwinreforged.server.core.modules.Module;
import com.darwinreforged.server.core.util.FileUtils;

import org.spongepowered.api.Sponge;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

@UtilityImplementation(FileUtils.class)
public class SpongeFileUtils extends FileUtils {
    @Override
    public Path getDataDirectory(Object plugin) {
        Optional<Module> infoOptional = DarwinServer.getModuleInfo(plugin.getClass());
        if (infoOptional.isPresent()) {
            Path darwinDataPath = infoOptional.map(moduleInfo -> Sponge.getGame().getSavesDirectory().resolve("data/" + moduleInfo.id())).get().toAbsolutePath();
            return createPathIfNotExist(darwinDataPath);
        }
        return getConfigDirectory(plugin);
    }

    @Override
    public Path getModuleDirectory() {
        Path modDir = Sponge.getGame().getGameDirectory().resolve("modules/").toAbsolutePath();
        return createPathIfNotExist(modDir);
    }


    @Override
    public Path getConfigDirectory(Object plugin) {
        Optional<Module> infoOptional = DarwinServer.getModuleInfo(plugin.getClass());
        Path darwinConfigPath = Sponge.getConfigManager().getPluginConfig(DarwinServer.getServer()).getDirectory();

        return createPathIfNotExist(infoOptional.map(moduleInfo -> new File(
                darwinConfigPath.toFile().getParent(),
                moduleInfo.id().replaceAll("\\.", "_")).toPath()).orElse(darwinConfigPath));
    }

    @Override
    public Path getLogDirectory() {
        return Sponge.getGame().getGameDirectory().resolve("logs").toAbsolutePath();
    }
}
