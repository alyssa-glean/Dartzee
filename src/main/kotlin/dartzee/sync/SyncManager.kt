package dartzee.sync

import dartzee.core.util.DialogUtil
import dartzee.core.util.runInOtherThread
import dartzee.db.DatabaseMerger
import dartzee.db.DatabaseMigrator
import dartzee.db.SyncAuditEntity
import dartzee.screen.sync.SyncProgressDialog
import dartzee.utils.DATABASE_FILE_PATH
import dartzee.utils.DartsDatabaseUtil
import dartzee.utils.Database
import dartzee.utils.DatabaseMigrations
import dartzee.utils.InjectedThings.mainDatabase
import java.io.File
import java.io.InterruptedIOException
import java.net.SocketException
import javax.swing.SwingUtilities

val SYNC_DIR = "${System.getProperty("user.dir")}/Sync"

class SyncManager(private val dbStore: IRemoteDatabaseStore)
{
    fun doPush(remoteName: String)
    {
        runInOtherThread { doPushOnOtherThread(remoteName) }
    }
    private fun doPushOnOtherThread(remoteName: String)
    {
        try
        {
            SwingUtilities.invokeLater { DialogUtil.showLoadingDialog("Pushing $remoteName...") }
            setUpSyncDir()

            SyncAuditEntity.insertSyncAudit(mainDatabase, remoteName)
            dbStore.pushDatabase(remoteName, mainDatabase)
        }
        finally
        {
            tidyUpAllSyncDirs()
            SwingUtilities.invokeLater { DialogUtil.dismissLoadingDialog() }
            refreshSyncSummary()
        }
    }

    fun doPull(remoteName: String)
    {
        runInOtherThread { doPullOnOtherThread(remoteName) }
    }
    private fun doPullOnOtherThread(remoteName: String)
    {
        try
        {
            SwingUtilities.invokeLater { DialogUtil.showLoadingDialog("Pulling $remoteName...") }
            setUpSyncDir()

            val remote = dbStore.fetchDatabase(remoteName).database
            SyncAuditEntity.insertSyncAudit(remote, remoteName)
            DartsDatabaseUtil.swapInDatabase(remote)
        }
        finally
        {
            tidyUpAllSyncDirs()
            SwingUtilities.invokeLater { DialogUtil.dismissLoadingDialog() }
            refreshSyncSummary()
        }
    }

    fun doSync(remoteName: String)
    {
        runInOtherThread { doSyncOnOtherThread(remoteName) }
    }
    private fun doSyncOnOtherThread(remoteName: String)
    {
        try
        {
            setUpSyncDir()

            SyncProgressDialog.syncStarted()

            val fetchResult = dbStore.fetchDatabase(remoteName)
            val merger = makeDatabaseMerger(fetchResult.database, remoteName)
            if (!merger.validateMerge())
            {
                return
            }

            SyncProgressDialog.progressToStage(SyncStage.MERGE_LOCAL_CHANGES)

            val resultingDatabase = merger.performMerge()
            dbStore.pushDatabase(remoteName, resultingDatabase, fetchResult.lastModified)

            SyncProgressDialog.progressToStage(SyncStage.OVERWRITE_LOCAL)

            val success = DartsDatabaseUtil.swapInDatabase(resultingDatabase)
            SyncProgressDialog.dispose()
            if (success)
            {
                DialogUtil.showInfo("Sync completed successfully!")
            }
        }
        catch (e: Exception)
        {
            when (e)
            {
                is SocketException, is InterruptedIOException ->
                    DialogUtil.showError("A connection error occurred during database sync. Check your internet connection and try again.")
                else -> DialogUtil.showError("An unexpected error occurred during database sync. No data has been changed.")
            }

            throw e
        }
        finally
        {
            tidyUpAllSyncDirs()
            SyncProgressDialog.dispose()
            refreshSyncSummary()
        }
    }

    private fun setUpSyncDir()
    {
        tidyUpAllSyncDirs()
        File(SYNC_DIR).mkdirs()
    }

    private fun tidyUpAllSyncDirs()
    {
        File(SYNC_DIR).deleteRecursively()
        File("$DATABASE_FILE_PATH/DartsOther").deleteRecursively()
    }

    private fun makeDatabaseMerger(remoteDatabase: Database, remoteName: String)
      = DatabaseMerger(mainDatabase, remoteDatabase, DatabaseMigrator(DatabaseMigrations.getConversionsMap()), remoteName)
}