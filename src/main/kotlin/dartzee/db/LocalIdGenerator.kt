package dartzee.db

import dartzee.utils.DatabaseUtil

object LocalIdGenerator
{
    private val UNIQUE_ID_SYNCH_OBJECT = Any()
    val hmLastAssignedIdByTableName = mutableMapOf<String, Long>()

    fun generateLocalId(tableName: String): Long
    {
        synchronized(UNIQUE_ID_SYNCH_OBJECT)
        {
            val lastAssignedId = hmLastAssignedIdByTableName[tableName] ?: retrieveLastAssignedId(tableName)

            val nextId = lastAssignedId + 1
            hmLastAssignedIdByTableName[tableName] = nextId

            return nextId
        }
    }
    private fun retrieveLastAssignedId(tableName: String): Long
    {
        return DatabaseUtil.executeQueryAggregate("SELECT MAX(LocalId) FROM $tableName").toLong()
    }
}