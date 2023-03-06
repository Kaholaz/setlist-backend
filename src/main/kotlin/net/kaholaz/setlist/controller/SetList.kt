package net.kaholaz.setlist.controller

import net.kaholaz.setlist.database.SetListModel
import net.kaholaz.setlist.database.SetListRepository
import net.kaholaz.setlist.database.SongModel
import net.kaholaz.setlist.database.SongRepository
import net.kaholaz.setlist.service.SpotifyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

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
class SetListController {
    @Autowired
    lateinit var setListRepository: SetListRepository
    @Autowired
    lateinit var songRepository: SongRepository

    val spotifyService = SpotifyService()

    @GetMapping("/setlist/{id}")
    fun getSetList(@PathVariable id: String): SetListDTO {
        val setList = setListRepository.findById(id)
        return SetListDTO(setList.get())
    }

    @PostMapping("/setlist/new")
    fun newSetList(@RequestBody setList: SetListDTO): SetListDTO {
        val newSetList = setList.toModel()
        newSetList.id = null

        setListRepository.save(newSetList)
        return SetListDTO(newSetList)
    }

    @PostMapping("/setlist/{id}")
    fun updateSetlist(@PathVariable id: String, @RequestBody setlist: SetListDTO): SetListDTO {
        if (id != setlist.id) {
            throw IllegalArgumentException("ID in path and body do not match")
        }

        val newSetList = setlist.toModel()
        setListRepository.save(newSetList)
        return SetListDTO(newSetList)
    }

    @PostMapping("/setlist/{id}/syncSpotify")
    fun syncSpotify(@PathVariable id: String, @RequestBody setlist: SetListDTO): SetListDTO {
        if (id != setlist.id) {
            throw IllegalArgumentException("ID in path and body do not match")
        }

        val setListData = spotifyService.retrievePlayList(setlist.spotifyPlaylist)
        val model = SetListModel(
            id = setlist.id,
            title = setListData.title,
            spotifyPlaylist = setlist.spotifyPlaylist,
            songs = setListData.songs,
        )

        songRepository.saveAll(model.songs)
        setListRepository.save(model)
        return SetListDTO(model)
    }

}
