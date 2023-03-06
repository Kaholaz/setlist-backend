package net.kaholaz.setlist.controller

import net.kaholaz.setlist.database.SetListModel
import net.kaholaz.setlist.database.SetListRepository
import net.kaholaz.setlist.database.SongModel
import net.kaholaz.setlist.database.SongRepository
import net.kaholaz.setlist.service.SpotifyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
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
    @Autowired
    lateinit var setListRepository: SetListRepository
    @Autowired
    lateinit var songRepository: SongRepository

    val spotifyService = SpotifyService()

    @GetMapping("/setlist/{id}")
    fun getSetList(@PathVariable id: String): SetListDTO {
        val setList = setListRepository.findById(id)

        return SetListDTO(
            setList.getOrElse
            { throw ResponseStatusException(HttpStatus.NOT_FOUND, "Set list not found") }
        )
    }

    @PostMapping("/setlist/new")
    fun newSetList(@RequestBody setList: SetListDTO): SetListDTO {
        val newSetList = setList.toModel()
        newSetList.id = null

        setListRepository.save(newSetList)
        return SetListDTO(newSetList)
    }

    @PostMapping("/setlist/{id}")
    fun updateSetlist(@PathVariable id: String, @RequestBody setList: SetListDTO): SetListDTO {
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
        if (id != setList.id) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "ID in path and body do not match")
        }
        val setListModel = setListRepository.findById(id).getOrElse {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Set list not found")
        }

        val setListData = spotifyService.retrievePlayList(setList.spotifyPlaylist)
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
