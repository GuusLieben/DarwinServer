package org.dockbox.darwin.sponge.util.player

import org.dockbox.darwin.core.objects.user.Player
import org.dockbox.darwin.core.util.player.PlayerStorageService
import org.dockbox.darwin.sponge.objects.targets.SpongePlayer
import org.spongepowered.api.Sponge
import org.spongepowered.api.service.user.UserStorageService
import java.util.*
import java.util.stream.Collectors

class SpongePlayerStorageService : PlayerStorageService() {

    override fun getOnlinePlayers(): List<Player> {
        return Sponge.getServer().onlinePlayers.stream().map { SpongePlayer(it.uniqueId, it.name) }.collect(Collectors.toList())
    }

    override fun getPlayer(name: String): Optional<Player> {
        val osp = Sponge.getServer().getPlayer(name)
        return getPlayer(osp, name)
    }

    override fun getPlayer(uuid: UUID): Optional<Player> {
        val osp = Sponge.getServer().getPlayer(uuid)
        return getPlayer(osp, uuid)
    }

    private fun getPlayer(osp: Optional<org.spongepowered.api.entity.living.player.Player>, obj: Any): Optional<Player> {
        return if (osp.isPresent) {
            osp.map { SpongePlayer(it.uniqueId, it.name) }
        } else {
            var player = Optional.empty<Player>()
            val ouss = Sponge.getServiceManager().provide(UserStorageService::class.java)
            val ou =
                    if (obj is UUID) ouss.flatMap { it[obj] }
                    else ouss.flatMap { it[obj.toString()] }
            if (ou.isPresent) player = ou.map { SpongePlayer(it.uniqueId, it.name) }
            player
        }
    }
}
