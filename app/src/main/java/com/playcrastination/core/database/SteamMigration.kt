package com.playcrastination.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE steam_links (
                gameId INTEGER NOT NULL PRIMARY KEY, steamId TEXT NOT NULL, appId INTEGER NOT NULL,
                playedMinutes INTEGER, recentMinutes INTEGER, lastPlayedSeconds INTEGER,
                syncedAtMillis INTEGER NOT NULL, hidden INTEGER NOT NULL, metadataAtMillis INTEGER,
                unlocked INTEGER, achievementCount INTEGER,
                FOREIGN KEY(gameId) REFERENCES games(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX index_steam_links_steamId_appId ON steam_links (steamId, appId)")
        db.execSQL(
            """
            CREATE TABLE steam_profiles (
                steamId TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, avatarUrl TEXT,
                currentGame TEXT, lastSyncMillis INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE steam_playtime_snapshots (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, gameId INTEGER NOT NULL,
                observedAtMillis INTEGER NOT NULL, cumulativeMinutes INTEGER NOT NULL,
                FOREIGN KEY(gameId) REFERENCES steam_links(gameId) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_steam_playtime_snapshots_gameId_observedAtMillis ON steam_playtime_snapshots (gameId, observedAtMillis)")
    }
}
