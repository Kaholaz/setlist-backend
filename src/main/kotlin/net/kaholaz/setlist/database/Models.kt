package net.kaholaz.setlist.database

import jakarta.persistence.*
import org.hibernate.annotations.GenericGenerator
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Entity
class SetListModel(
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    var id: String?,

    @Column(nullable = false)
    val title: String,

    @Column
    val spotifyPlaylist: String,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @OrderColumn(name = "song_order")
    val songs: List<SongModel>,
)

@Repository
interface SetListRepository : JpaRepository<SetListModel, String>


@Entity
data class SongModel(
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    var id: String?,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val artist: String,

    @Column(nullable = false)
    val tempo: Int,
)

@Repository
interface SongRepository : JpaRepository<SongModel, String>
