package com.core.voidapp.data.guardian

import android.content.Context
import com.core.voidapp.data.db.VoidDatabase
import com.core.voidapp.data.db.toEntity
import com.core.voidapp.data.db.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.UUID

/**
 * The single source of truth for whether Guardian has any authority right
 * now. Every other Guardian file (GuardianEngine, the future orb/Settings
 * UI) checks isCommitted() here rather than tracking its own on/off flag —
 * that's what makes rule #17-18 ("not bypassable through a simple ON/OFF
 * toggle") actually hold: there is no toggle, only a commitment record
 * with a real start/end timestamp.
 */
object GuardianRepository {
    private var database: VoidDatabase? = null
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var commitment: GuardianCommitment? = null
    private var settings: GuardianSettings = GuardianSettings()

    /**
     * Never treated as a distraction, on every install, regardless of
     * whatever else is in Allowed Apps — the user explicitly needs to be
     * able to ask an AI assistant or reach someone on Telegram mid-session
     * without Guardian flagging it. Unioned in on every load (see init()),
     * not just the fresh-install default, so it applies even to a device
     * that already has a settings row from before this list existed.
     *
     * Package IDs are the standard Play Store ones as of this writing;
     * a sideloaded or regional build could use a different ID, in which
     * case add it manually in Settings -> Guardian -> Allowed Apps.
     */
    private val ALWAYS_ALLOWED = setOf(
        "com.openai.chatgpt",       // ChatGPT
        "com.anthropic.claude",     // Claude
        "com.deepseek.chat",        // DeepSeek
        "org.telegram.messenger"    // Telegram
    )

    fun init(context: Context) {
        if (database != null) return
        val db = VoidDatabase.getInstance(context)
        database = db

        runBlocking(Dispatchers.IO) {
            val commitments = db.guardianCommitmentDao().getAll().map { it.toModel() }
            // Only one commitment is ever "current": the most recently started one.
            // Its status is re-checked against the real clock every load, in case
            // the app wasn't running when it naturally expired.
            val latest = commitments.maxByOrNull { it.startedAt }
            commitment = latest?.let { expireIfNeeded(it) }

            val loaded = db.guardianSettingsDao().get()?.toModel() ?: GuardianSettings()
            val withAlwaysAllowed = loaded.copy(allowedPackages = loaded.allowedPackages + ALWAYS_ALLOWED)
            settings = withAlwaysAllowed
            if (withAlwaysAllowed.allowedPackages != loaded.allowedPackages) {
                database?.guardianSettingsDao()?.upsert(withAlwaysAllowed.toEntity())
            }
        }
    }

    private fun expireIfNeeded(c: GuardianCommitment): GuardianCommitment {
        if (c.status == CommitmentStatus.ACTIVE && !LocalDateTime.now().isBefore(c.endsAt)) {
            val expired = c.copy(status = CommitmentStatus.ENDED_NATURALLY)
            ioScope.launch { database?.guardianCommitmentDao()?.upsert(expired.toEntity()) }
            return expired
        }
        return c
    }

    fun currentCommitment(): GuardianCommitment? = commitment?.let { expireIfNeeded(it) }.also { commitment = it }

    /** The master gate: without this, Guardian must not speak, warn, or otherwise appear (rule #15-16). */
    fun isCommitted(now: LocalDateTime = LocalDateTime.now()): Boolean =
        currentCommitment()?.isCurrentlyActive(now) == true

    fun beginCommitment(duration: CommitmentDuration): GuardianCommitment {
        val now = LocalDateTime.now()
        val newCommitment = GuardianCommitment(
            id = UUID.randomUUID().toString(),
            duration = duration,
            startedAt = now,
            endsAt = now.plusDays(duration.days),
            status = CommitmentStatus.ACTIVE
        )
        commitment = newCommitment
        ioScope.launch { database?.guardianCommitmentDao()?.upsert(newCommitment.toEntity()) }
        return newCommitment
    }

    /**
     * The deliberate emergency override (rule #19) — ends enforcement
     * early. Never touches study history, plans, progress, sessions, or
     * exams; it only changes this one commitment row's status. Confirming
     * with the user is the caller's (Settings UI's) job, not this function's.
     */
    fun endCommitmentEarly() {
        val current = commitment ?: return
        val ended = current.copy(status = CommitmentStatus.ENDED_EARLY)
        commitment = ended
        ioScope.launch { database?.guardianCommitmentDao()?.upsert(ended.toEntity()) }
    }

    fun settings(): GuardianSettings = settings

    fun updateSettings(transform: (GuardianSettings) -> GuardianSettings) {
        val updated = transform(settings)
        settings = updated
        ioScope.launch { database?.guardianSettingsDao()?.upsert(updated.toEntity()) }
    }
}
