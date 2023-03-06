package net.kaholaz.setlist.controller

import net.kaholaz.setlist.database.SetListModel
import net.kaholaz.setlist.database.SetListRepository
import net.kaholaz.setlist.database.SongModel
import net.kaholaz.setlist.database.SongRepository
import net.kaholaz.setlist.service.SpotifyService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import se.michaelthelin.spotify.exceptions.detailed.NotFoundException
import kotlin.jvm.optionals.getOrElse

data class SetListDTO(
    val id: String?,
    val name: String,
    val songs: List<SongDTO>,
    val spotifyPlaylist: String,
) {
    constructor(model: SetListModel) :
            this(
                    model.id,
                    model.title,
                    model.songs.map { song -> SongDTO(song) },
                    model.spotifyPlaylist,
                )
    fun toModel() = SetListModel(
        id,
        name,
        spotifyPlaylist,
        songs.map { song -> song.toModel() },
    )
}

data class SongDTO(
    val id: String?,
    val title: String,
    val artist: String,
    val tempo: Int,
) {
    constructor(model: SongModel) : this(model.id, model.title, model.artist, model.tempo)
    fun toModel() = SongModel(
        id,
        title,
        artist,
        tempo,
    )
}

@RestController
@OptIn(ExperimentalStdlibApi::class)
class SetListController {
    val logger = LoggerFactory.getLogger(SetListController::class.java)

    @Autowired
    lateinit var setListRepository: SetListRepository
    @Autowired
    lateinit var songRepository: SongRepository

    val spotifyService = SpotifyService()

    @GetMapping("/setlist/{id}")
    fun getSetList(@PathVariable id: String): SetListDTO {
        logger.info("Retrieving set list with id: $id")
        val setList = setListRepository.findById(id)

        return SetListDTO(
            setList.getOrElse
            { throw ResponseStatusException(HttpStatus.NOT_FOUND, "Set list not found") }
        )
    }

    @PostMapping("/setlist/new")
    fun newSetList(@RequestBody setList: SetListDTO): SetListDTO {
        logger.info("Creating new set list with name: ${setList.name}")
        val newSetList = setList.toModel()
        newSetList.id = null

        setListRepository.save(newSetList)
        return SetListDTO(newSetList)
    }

    @PostMapping("/setlist/{id}")
    fun updateSetlist(@PathVariable id: String, @RequestBody setList: SetListDTO): SetListDTO {
        logger.info("Updating set list with id: $id")
        if (id != setList.id) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "ID in path and body do not match")
        }
        setListRepository.findById(id).getOrElse {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Set list not found")
        }

        val newSetList = setList.toModel()
        setListRepository.save(newSetList)
        return SetListDTO(newSetList)
    }

    @PostMapping("/setlist/{id}/syncSpotify")
    fun syncSpotify(@PathVariable id: String, @RequestBody setList: SetListDTO): SetListDTO {
        logger.info("Syncing set list with id: $id")
        if (id != setList.id) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "ID in path and body do not match")
        }
        val setListModel = setListRepository.findById(id).getOrElse {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Set list not found")
        }

        val setListData: SetListModel
        try {
            setListData = spotifyService.retrievePlaylist(setList.spotifyPlaylist)
        } catch (e: NotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Spotify playlist not found")
        }
        val model = SetListModel(
            id = setList.id,
            title = setListData.title,
            spotifyPlaylist = setList.spotifyPlaylist,
            songs = setListData.songs,
        )

        songRepository.saveAll(model.songs)
        setListRepository.save(model)
        return SetListDTO(model)
    }

}
