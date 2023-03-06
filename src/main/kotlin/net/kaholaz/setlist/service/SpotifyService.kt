package net.kaholaz.setlist.service

import net.kaholaz.setlist.database.SetListModel
import net.kaholaz.setlist.database.SongModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.model_objects.specification.Playlist
import se.michaelthelin.spotify.model_objects.specification.Track

fun Playlist.toSetListModel() = SetListModel(
        id = null,
        title = this.name,
        spotifyPlaylist = this.uri,
        songs = this.tracks.items.map { track -> (track.track as Track).toSongModel() }
    )

fun Track.toSongModel() : SongModel = SongModel(
        id = null,
        title = this.name,
        artist = this.artists[0].name,
        tempo = SpotifyService().getTempo(this),
    )

@Service
class SpotifyService {
    val logger: Logger = LoggerFactory.getLogger(SpotifyService::class.java)

    val spotifyApi: SpotifyApi = SpotifyApi.Builder()
        .setClientId(System.getenv("SPOTIFY_CLIENT_ID"))
        .setClientSecret(System.getenv("SPOTIFY_CLIENT_SECRET"))
        .build()

    fun retrievePlayList(playListUrl : String) : SetListModel {
        val clientCredentialsRequest = spotifyApi.clientCredentials().build()

        logger.debug("Retrieving access token from Spotify before retrieving playlist")
        val clientCredentials = clientCredentialsRequest.execute()
        spotifyApi.accessToken = clientCredentials.accessToken

        logger.info("Retrieving playlist from Spotify with URL: $playListUrl")
        logger.debug("Spotify ID: ${spotifyIdFromUrl(playListUrl)}")
        val playListRequest = spotifyApi
            .getPlaylist(spotifyIdFromUrl(playListUrl)).build()
        val playList = playListRequest.execute()

        return playList.toSetListModel()
    }

    fun getTempo(track: Track) : Int {
        logger.debug("Retrieving access token from Spotify before retrieving playlist")
        val clientCredentialsRequest = spotifyApi.clientCredentials().build()
        val clientCredentials = clientCredentialsRequest.execute()
        spotifyApi.accessToken = clientCredentials.accessToken

        logger.debug("Retrieving tempo for track: ${track.id}")
        val audioFeaturesRequest = spotifyApi.getAudioFeaturesForTrack(track.id).build()
        val audioFeatures = audioFeaturesRequest.execute()
        return audioFeatures.tempo.toInt()
    }

    fun spotifyIdFromUrl(url: String) : String {
        return url.substringAfterLast("/").substringBefore("?");
    }
}
