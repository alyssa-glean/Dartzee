package dartzee.utils

import com.mashape.unirest.http.Unirest
import dartzee.core.util.Debug
import dartzee.core.util.DialogUtil
import org.json.JSONObject
import java.io.File
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/**
 * Automatically check for and download updates using the Github API
 *
 * https://developer.github.com/v3/repos/releases/#get-the-latest-release
 */
object UpdateManager
{
    fun checkForUpdates(currentVersion: String)
    {
        //Show this here, checking the CRC can take time
        Debug.append("Checking for updates - my version is $currentVersion")

        val jsonResponse = queryLatestReleaseJson(DARTZEE_REPOSITORY_URL)
        jsonResponse ?: return

        val metadata = parseUpdateMetadata(jsonResponse)
        if (metadata == null || !shouldUpdate(currentVersion, metadata))
        {
            return
        }

        startUpdate(metadata.getArgs(), Runtime.getRuntime())
    }

    fun queryLatestReleaseJson(repositoryUrl: String): JSONObject?
    {
        try
        {
            DialogUtil.showLoadingDialog("Checking for updates...")

            val response = Unirest.get("$repositoryUrl/releases/latest").asJson()
            if (response.status != 200)
            {
                Debug.append("Received non-success HTTP status: ${response.status} - ${response.statusText}")
                Debug.append(response.body.toString())
                DialogUtil.showError("Failed to check for updates (unable to connect).")
                return null
            }

            return response.body.`object`
        }
        catch (t: Throwable)
        {
            Debug.stackTraceSilently(t)
            DialogUtil.showError("Failed to check for updates (unable to connect).")
            return null
        }
        finally
        {
            DialogUtil.dismissLoadingDialog()
        }
    }

    fun shouldUpdate(currentVersion: String, metadata: UpdateMetadata): Boolean
    {
        if (metadata.version == currentVersion)
        {
            Debug.append("I am up to date")
            return false
        }

        //An update is available
        Debug.append("Newer release available - ${metadata.version}")
        val answer = DialogUtil.showQuestion("An update is available (${metadata.version}). Would you like to download it now?", false)
        return answer == JOptionPane.YES_OPTION
    }

    fun parseUpdateMetadata(responseJson: JSONObject): UpdateMetadata?
    {
        return try
        {
            val remoteVersion = responseJson.getString("tag_name")
            val assets = responseJson.getJSONArray("assets")
            val asset = assets.getJSONObject(0)

            val assetId = asset.getLong("id")
            val fileName = asset.getString("name")
            val size = asset.getLong("size")
            UpdateMetadata(remoteVersion,  assetId, fileName, size)
        }
        catch (t: Throwable)
        {
            Debug.stackTrace(t, "Error parsing JSON: $responseJson")
            null
        }
    }

    fun startUpdate(args: String, runtime: Runtime)
    {
        prepareBatchFile()

        try
        {
            runtime.exec("cmd /c start update.bat $args")
            exitProcess(0)
        }
        catch (t: Throwable)
        {
            Debug.stackTrace(t, suppressError = true)
            val manualCommand = "update.bat $args"

            val msg = "Failed to launch update.bat - call the following manually to perform the update: \n\n$manualCommand"
            DialogUtil.showError(msg)
        }
    }

    fun prepareBatchFile()
    {
        val updateFile = File("update.bat")

        updateFile.delete()
        val updateScript = javaClass.getResource("/update/update.bat").readText()
        updateFile.writeText(updateScript)
    }
}

data class UpdateMetadata(val version: String, val assetId: Long, val fileName: String, val size: Long)
{
    fun getArgs() = "$size $version $fileName $assetId"
}