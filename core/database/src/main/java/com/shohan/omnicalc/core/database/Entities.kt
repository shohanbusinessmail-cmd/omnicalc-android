package com.shohan.omnicalc.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One calculation in history (spec §15.1).
 *
 * Stored independently of display formatting: [resultPlain] is always a
 * canonical Western-digit plain decimal (or "1E+308"-style overflow-free
 * plain string), so numeral style / grouping / decimal-place changes never
 * rewrite history.
 *
 * [contextPairs] carries reproducibility metadata as `k=v` pairs joined by
 * `&` (e.g. `angle=DEG&unitFrom=km&unitTo=m`) — deliberately dependency-free
 * instead of a JSON library; parsed with simple string handling and validated.
 */
@Entity(
    tableName = "history",
    indices = [Index(value = ["createdAt"]), Index(value = ["toolType"])],
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val expression: String,
    val resultPlain: String,
    @ColumnInfo(defaultValue = "0")
    val exact: Boolean,
    /** "standard" | "scientific" | "converter" | "formula" | "finance" | … */
    val toolType: String,
    /** Optional sub-category for filtering (e.g. "land", "vat"). */
    val category: String? = null,
    /** Optional reproducibility context, `k=v&k=v`. */
    val contextPairs: String? = null,
    /** Optional user label. */
    val label: String? = null,
    val createdAt: Long,
)

/**
 * A saved favorite (spec §15.2). Snapshots work for history entries,
 * formulas, conversions and saved presets alike.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** "calculation" | "formula" | "conversion" | "preset" */
    val type: String,
    val title: String,
    /** Payload shown/reused when opened (expression, formula id, …). */
    val payload: String,
    val subtitle: String? = null,
    val createdAt: Long,
)
