package com.playcrastination.domain.model

import java.text.Collator
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

enum class LibrarySort { TITLE, PLAYTIME, DURATION, PROGRESS, ACTIVITY, RELEASE }

data class LibraryQuery(
    val text: String = "",
    val genre: String? = null,
    val platform: String? = null,
    val status: GameStatus? = null,
    val year: Int? = null,
    val favoritesOnly: Boolean = false,
    val sort: LibrarySort = LibrarySort.TITLE,
    val descending: Boolean = false
)

class LibraryCatalog(games: List<Game>, private val locale: Locale) {
    private val entries = games.map(::Entry)

    val genres: List<String> = labels(games.flatMap(Game::genres))
    val platforms: List<String> = labels(games.flatMap(Game::platforms))
    val years: List<Int> = games.mapNotNull(Game::resolvedReleaseYear).distinct().sortedDescending()

    fun find(query: LibraryQuery): List<Game> {
        val collator = Collator.getInstance(locale)
        val text = query.text.searchKey()
        val genre = query.genre?.searchKey()
        val platform = query.platform?.searchKey()
        return entries.asSequence().filter { entry ->
            val game = entry.game
            (text in entry.title || text in entry.originalTitle) &&
                (genre == null || genre in entry.genres) &&
                (platform == null || platform in entry.platforms) &&
                (query.status == null || game.status == query.status) &&
                (query.year == null || game.resolvedReleaseYear == query.year) &&
                (!query.favoritesOnly || game.favorite)
        }.sortedWith { first, second ->
            val primary = when (query.sort) {
                LibrarySort.TITLE -> titleOrder(first.game, second.game, collator, query.descending)
                LibrarySort.PLAYTIME -> valueOrder(first.playtime, second.playtime, query.descending)
                LibrarySort.DURATION -> valueOrder(first.duration, second.duration, query.descending)
                LibrarySort.PROGRESS -> valueOrder(first.progress, second.progress, query.descending)
                LibrarySort.ACTIVITY -> valueOrder(first.activity, second.activity, query.descending)
                LibrarySort.RELEASE -> valueOrder(first.release, second.release, query.descending)
            }
            primary.takeIf { it != 0 } ?: titleOrder(first.game, second.game, collator, false)
        }.map(Entry::game).toList()
    }

    private fun labels(values: List<String>): List<String> {
        val collator = Collator.getInstance(locale)
        return values.distinctBy(String::searchKey).sortedWith { a, b -> collator.compare(a, b) }
    }

    private fun titleOrder(first: Game, second: Game, collator: Collator, descending: Boolean): Int {
        val titleOrder = if (descending) collator.compare(second.title, first.title)
            else collator.compare(first.title, second.title)
        return titleOrder.takeIf { it != 0 } ?: first.id.compareTo(second.id)
    }

    private class Entry(val game: Game) {
        val title = game.title.searchKey()
        val originalTitle = game.originalTitle.searchKey()
        val genres = game.genres.map(String::searchKey)
        val platforms = game.platforms.map(String::searchKey)
        val playtime = game.recordedMinutes
        val duration = game.durations[game.goal]
        val progress = game.timeProgress().percent
        val activity = game.lastPlayedOn
        val release = game.releaseDate ?: game.releaseYear?.let { LocalDate.of(it, 1, 1) }
    }
}

private val marks = Regex("\\p{M}+")
private val whitespace = Regex("\\s+")

private fun String.searchKey(): String = Normalizer.normalize(trim(), Normalizer.Form.NFD)
    .replace(marks, "").replace(whitespace, " ").lowercase(Locale.ROOT)

private fun <T : Comparable<T>> valueOrder(first: T?, second: T?, descending: Boolean): Int = when {
    first == null -> if (second == null) 0 else 1
    second == null -> -1
    descending -> second.compareTo(first)
    else -> first.compareTo(second)
}
