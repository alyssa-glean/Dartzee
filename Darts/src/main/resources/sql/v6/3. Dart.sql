/********************
 *       Dart       *
 ********************/

CREATE TABLE Dart_Tmp
(
	RowId VARCHAR(36) PRIMARY KEY,
	DtCreation TIMESTAMP NOT NULL,
	DtLastUpdate TIMESTAMP NOT NULL,
	RoundId VARCHAR(36) NOT NULL,
	Ordinal INT NOT NULL,
	Score INT NOT NULL,
	Multiplier INT NOT NULL,
	StartingScore INT NOT NULL,
	PosX INT NOT NULL,
	PosY INT NOT NULL,
	SegmentType INT NOT NULL
);

INSERT INTO
	Dart_Tmp
SELECT
	zzD.Guid,
	DtCreation,
	DtLastUpdate,
	zzR.Guid,
	Ordinal,
	Score,
	Multiplier,
	StartingScore,
	PosX,
	PosY,
	SegmentType
FROM
	Dart d,
	zzDartGuids zzD,
	zzRoundGuids zzR
WHERE
    d.RowId = zzD.RowId
    AND d.RoundId = zzR.RowId;

RENAME TABLE Dart TO zzDart;
RENAME TABLE Dart_Tmp TO Dart;
DROP TABLE zzDart;

CREATE INDEX RoundId_Ordinal ON Dart(RoundId, Ordinal)