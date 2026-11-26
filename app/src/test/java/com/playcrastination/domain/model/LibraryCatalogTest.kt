package com.playcrastination.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class LibraryCatalogTest {
    @Test
    fun combinesSearchAndFacetsOfOriginalTitlesGenresAndPlatforms() {
        val matching = Game(
            id = 1, title = "L'ombre", originalTitle = "Étoile du Nord", genre = "Action; Rôle",
            platform = "PS5", availablePlatforms = "PC; Switch", status = GameStatus.PLAYING,
            releaseDate = LocalDate.of(2025, 4, 24), favorite = true
        )
        val catalog = LibraryCatalog(
            listOf(matching, matching.copy(id = 2, favorite = false), matching.copy(id = 3, genre = "Aventure")),
            Locale.FRENCH
        )

        val query = LibraryQuery(
            text = " etoile   DU nord ", genre = "role", platform = "switch",
            status = GameStatus.PLAYING, year = 2025, favoritesOnly = true
        )
        assertEquals(listOf(1L), catalog.find(query).map(Game::id))
        assertEquals(listOf("PC", "PS5", "Switch"), catalog.platforms)
        assertEquals(listOf(2025), catalog.years)
    }

    @Test
    fun sortsEveryKeyAndKeepsUnknownValuesAfterKnownInBothDirections() {
        val alpha = Game(
            id = 1, title = "Alpha", releaseDate = LocalDate.of(2025, 1, 1), durations = Durations(300),
            currentPlaythrough = Playthrough(playedMinutes = 150, lastPlayedOn = LocalDate.of(2025, 2, 1)),
            previousPlaythroughs = listOf(Playthrough(playedMinutes = 500))
        )
        val beta = Game(
            id = 2, title = "Beta", releaseYear = 2022, durations = Durations(1000),
            currentPlaythrough = Playthrough(playedMinutes = 20, lastPlayedOn = LocalDate.of(2025, 3, 1))
        )
        val unknown = Game(id = 3, title = "Aardvark")
        val catalog = LibraryCatalog(listOf(beta, unknown, alpha), Locale.ENGLISH)
        fun sorted(sort: LibrarySort, descending: Boolean = false): List<Long> =
            catalog.find(LibraryQuery(sort = sort, descending = descending)).map(Game::id)

        assertEquals(listOf(3L, 1L, 2L), sorted(LibrarySort.TITLE))
        assertEquals(listOf(2L, 1L, 3L), sorted(LibrarySort.TITLE, true))
        assertEquals(listOf(1L, 2L, 3L), sorted(LibrarySort.PLAYTIME, true))
        assertEquals(listOf(1L, 2L, 3L), sorted(LibrarySort.DURATION))
        assertEquals(listOf(2L, 1L, 3L), sorted(LibrarySort.PROGRESS))
        assertEquals(listOf(2L, 1L, 3L), sorted(LibrarySort.ACTIVITY, true))
        assertEquals(listOf(2L, 1L, 3L), sorted(LibrarySort.RELEASE))
        assertEquals(listOf(1L, 2L, 3L), sorted(LibrarySort.RELEASE, true))
    }

    @Test
    fun filtersACollectionOfFiveThousandGames() {
        val catalog = LibraryCatalog((1..5000).map { index ->
            Game(
                id = index.toLong(),
                title = "Game $index",
                genre = if (index % 2 == 0) "RPG" else "Racing",
                releaseYear = if (index % 3 == 0) 2025 else 2024
            )
        }, Locale.ENGLISH)

        val result = catalog.find(LibraryQuery(genre = "RPG", year = 2025))
        assertEquals(833, result.size)
        assertEquals(833, result.map(Game::id).distinct().size)
    }
}
