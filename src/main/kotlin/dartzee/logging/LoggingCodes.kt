package dartzee.logging

//Info
val CODE_SQL = LoggingCode("sql")
val CODE_BULK_SQL = LoggingCode("bulkSql")
val CODE_USERNAME_UNSET = LoggingCode("usernameUnset")
val CODE_USERNAME_SET = LoggingCode("usernameSet")
val CODE_JUST_UPDATED = LoggingCode("justUpdated")
val CODE_MEMORY_SETTINGS = LoggingCode("memorySettings")
val CODE_TABLE_CREATED = LoggingCode("tableCreated")
val CODE_TABLE_EXISTS = LoggingCode("tableExists")
val CODE_LOOK_AND_FEEL_SET = LoggingCode("lafSet")
val CODE_DATABASE_UP_TO_DATE = LoggingCode("databaseCurrent")
val CODE_DATABASE_NEEDS_UPDATE = LoggingCode("databaseNeedsUpdate")
val CODE_DATABASE_CREATING = LoggingCode("databaseCreating")
val CODE_DATABASE_CREATED = LoggingCode("databaseCreated")
val CODE_THREAD_STACKS = LoggingCode("threadStacks")
val CODE_THREAD_STACK = LoggingCode("threadStack")
val CODE_NEW_CONNECTION = LoggingCode("newConnection")
val CODE_SANITY_CHECK_STARTED = LoggingCode("sanityCheckStarted")
val CODE_SANITY_CHECK_COMPLETED = LoggingCode("sanityCheckCompleted")
val CODE_SANITY_CHECK_RESULT = LoggingCode("sanityCheckResult")
val CODE_SIMULATION_STARTED = LoggingCode("simulationStarted")
val CODE_SIMULATION_PROGRESS = LoggingCode("simulationProgress")
val CODE_SIMULATION_CANCELLED = LoggingCode("simulationCancelled")
val CODE_SIMULATION_FINISHED = LoggingCode("simulationFinished")
val CODE_DIALOG_SHOWN = LoggingCode("dialogShown")
val CODE_DIALOG_CLOSED = LoggingCode("dialogClosed")
val CODE_COMMAND_ENTERED = LoggingCode("commandEntered")
val CODE_UPDATE_CHECK = LoggingCode("updateCheck")
val CODE_UPDATE_CHECK_RESULT = LoggingCode("updateCheckResult")
val CODE_LOADED_RESOURCES = LoggingCode("loadedResources")
val CODE_STARTING_BACKUP = LoggingCode("startingBackup")
val CODE_STARTING_RESTORE = LoggingCode("startingRestore")
val CODE_RENDERED_DARTBOARD = LoggingCode("renderedDartboard")
val CODE_PLAYER_PAUSED = LoggingCode("playerPaused")
val CODE_PLAYER_UNPAUSED = LoggingCode("playerUnpaused")
val CODE_FETCHING_DATABASE = LoggingCode("fetchingDatabase")
val CODE_FETCHED_DATABASE = LoggingCode("fetchedDatabase")
val CODE_UNZIPPED_DATABASE = LoggingCode("unzippedDatabase")
val CODE_PUSHING_DATABASE = LoggingCode("pushingDatabase")
val CODE_ZIPPED_DATABASE = LoggingCode("zippedDatabase")
val CODE_PUSHED_DATABASE = LoggingCode("pushedDatabase")
val CODE_PUSHED_DATABASE_BACKUP = LoggingCode("pushedDatabaseBackup")
val CODE_MERGE_STARTED = LoggingCode("mergeStarted")
val CODE_MERGING_ENTITY = LoggingCode("mergingEntity")
val CODE_ACHIEVEMENT_CONVERSION_STARTED = LoggingCode("achievementConversionStarted")
val CODE_ACHIEVEMENT_CONVERSION_FINISHED = LoggingCode("achievementConversionFinished")
val CODE_SWITCHING_FILES = LoggingCode("switchingFiles")

//Warn
val CODE_UNEXPECTED_ARGUMENT = LoggingCode("unexpectedArgument")
val CODE_DATABASE_TOO_OLD = LoggingCode("databaseTooOld")
val CODE_RESOURCE_CACHE_NOT_INITIALISED = LoggingCode("resourceCacheNotInitialised")
val CODE_DATABASE_IN_USE = LoggingCode("databaseInUse")
val CODE_NO_STREAMS = LoggingCode("noStreams")

//Error
val CODE_LOOK_AND_FEEL_ERROR = LoggingCode("lafError")
val CODE_SQL_EXCEPTION = LoggingCode("sqlException")
val CODE_UNCAUGHT_EXCEPTION = LoggingCode("uncaughtException")
val CODE_SIMULATION_ERROR = LoggingCode("simulationError")
val CODE_LOAD_ERROR = LoggingCode("loadError")
val CODE_INSTANTIATION_ERROR = LoggingCode("instantiationError")
val CODE_AUDIO_ERROR = LoggingCode("audioError")
val CODE_SCREEN_LOAD_ERROR = LoggingCode("screenLoadError")
val CODE_UPDATE_ERROR = LoggingCode("updateError")
val CODE_PARSE_ERROR = LoggingCode("parseError")
val CODE_BATCH_ERROR = LoggingCode("batchFileError")
val CODE_TEST_CONNECTION_ERROR = LoggingCode("testConnectionError")
val CODE_RESOURCE_LOAD_ERROR = LoggingCode("resourceLoadError")
val CODE_COMMAND_ERROR = LoggingCode("commandError")
val CODE_RENDER_ERROR = LoggingCode("renderError")
val CODE_FILE_ERROR = LoggingCode("fileError")
val CODE_SWING_ERROR = LoggingCode("swingError")
val CODE_AI_ERROR = LoggingCode("aiError")
val CODE_ELASTICSEARCH_ERROR = LoggingCode("elasticsearchError")
val CODE_MERGE_ERROR = LoggingCode("mergeError")
val CODE_SYNC_ERROR = LoggingCode("syncError")
val CODE_PUSH_ERROR = LoggingCode("pushError")
val CODE_PULL_ERROR = LoggingCode("pullError")
val CODE_BACKUP_ERROR = LoggingCode("backupError")
val CODE_RESTORE_ERROR = LoggingCode("restoreError")