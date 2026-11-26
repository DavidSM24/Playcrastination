package com.playcrastination.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN originalTitle TEXT")
        db.execSQL("ALTER TABLE games ADD COLUMN releaseDay INTEGER")
        db.execSQL("ALTER TABLE games ADD COLUMN developer TEXT")
        db.execSQL("ALTER TABLE games ADD COLUMN publisher TEXT")
        db.execSQL("ALTER TABLE games ADD COLUMN availablePlatforms TEXT")
        db.execSQL("ALTER TABLE games ADD COLUMN backgroundUrl TEXT")
        db.execSQL("ALTER TABLE games ADD COLUMN rating INTEGER")

        db.execSQL(
            """
            CREATE TABLE playthroughs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                gameId INTEGER NOT NULL,
                playedMinutes INTEGER,
                narrativePercent INTEGER,
                startedDay INTEGER,
                finishedDay INTEGER,
                lastPlayedDay INTEGER,
                storyMinutes INTEGER,
                extrasMinutes INTEGER,
                otherMinutes INTEGER,
                FOREIGN KEY(gameId) REFERENCES games(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_playthroughs_gameId_id ON playthroughs (gameId, id)")
        db.execSQL(
            """
            INSERT INTO playthroughs (gameId, playedMinutes, narrativePercent)
            SELECT games.id, game_progress.playedMinutes, game_progress.narrativePercent
            FROM games LEFT JOIN game_progress ON game_progress.gameId = games.id
            """.trimIndent()
        )
        db.execSQL("DROP TABLE game_progress")
    }
}
