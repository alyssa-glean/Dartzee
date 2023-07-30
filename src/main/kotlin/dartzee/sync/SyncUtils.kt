package dartzee.sync

import dartzee.core.util.DialogUtil
import dartzee.db.AchievementEntity
import dartzee.db.DeletionAuditEntity
import dartzee.db.GameEntity
import dartzee.db.SyncAuditEntity
import dartzee.screen.ScreenCache
import dartzee.screen.sync.SyncManagementScreen
import dartzee.utils.DartsDatabaseUtil
import dartzee.utils.InjectedThings.mainDatabase
import java.sql.Timestamp

const val SYNC_BUCKET_NAME = "dartzee-databases"

enum class SyncMode
{
    CREATE_REMOTE,
    OVERWRITE_LOCAL,
    NORMAL_SYNC
}

data class SyncConfig(val mode: SyncMode, val remoteName: String)

data class LastSyncData(val remoteName: String, val lastSynced: Timestamp)

data class SyncResult(val gamesPushed: Int, val gamesPulled: Int)

fun getModifiedGameCount(): Int
{
    val lastSynced = SyncAuditEntity.getLastSyncData(mainDatabase)?.lastSynced
    return GameEntity().countModifiedSince(lastSynced)
}

fun resetRemote()
{
    SyncAuditEntity().deleteAll()
    ScreenCache.get<SyncManagementScreen>().initialise()
}

fun validateSyncAction(): Boolean
{
    val openScreens = ScreenCache.getDartsGameScreens()
    if (openScreens.isNotEmpty())
    {
        DialogUtil.showErrorOLD("You must close all open games before performing this action.")
        return false
    }

    return true
}

fun needsSync(): Boolean
{
    val entitiesToCheck = DartsDatabaseUtil.getSyncEntities(mainDatabase) + AchievementEntity(mainDatabase) + DeletionAuditEntity(mainDatabase)
    val lastLocalSync = SyncAuditEntity.getLastSyncData(mainDatabase)?.lastSynced
    return entitiesToCheck.flatMap { it.retrieveModifiedSince(lastLocalSync) }.isNotEmpty()
}