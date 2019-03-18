/********************
 *   PlayerImage    *
 ********************/

CREATE TABLE PlayerImage_Tmp
(
	RowId VARCHAR(36) PRIMARY KEY,
	DtCreation TIMESTAMP NOT NULL,
	DtLastUpdate TIMESTAMP NOT NULL,
	BlobData Blob NOT NULL,
	Filepath VARCHAR(1000) NOT NULL,
	Preset BOOLEAN NOT NULL
);

INSERT INTO
	PlayerImage_Tmp
SELECT
	CAST(RowId AS CHAR(36)),
	DtCreation,
	DtLastUpdate,
	BlobData,
	Filepath,
	Preset
FROM
	PlayerImage;

RENAME TABLE PlayerImage TO zzPlayerImage;
RENAME TABLE PlayerImage_Tmp TO PlayerImage;
DROP TABLE zzPlayerImage;