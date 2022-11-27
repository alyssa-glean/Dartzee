package dartzee.core.util

import dartzee.helper.AbstractTest
import dartzee.logging.CODE_FILE_ERROR
import dartzee.logging.Severity
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Dimension
import java.io.File
import java.nio.file.DirectoryNotEmptyException
import javax.swing.ImageIcon

class TestFileUtil: AbstractTest()
{
    private val TEST_DIR = File("Test/")

    @BeforeEach
    fun beforeEach()
    {
        TEST_DIR.deleteRecursively()
        TEST_DIR.mkdirs()
    }

    @AfterEach
    fun afterEach()
    {
        TEST_DIR.deleteRecursively()
    }

    @Test
    fun `Should successfully delete a file`()
    {
        val f = File("Test/Test.txt")
        f.createNewFile()
        f.exists() shouldBe true

        val result = FileUtil.deleteFileIfExists("Test/Test.txt")
        result shouldBe true
        f.exists() shouldBe false
    }

    @Test
    fun `Should handle a non-existent file`()
    {
        val result = FileUtil.deleteFileIfExists("DoesNotExist.txt")
        result shouldBe false
    }

    @Test
    fun `Should stack trace and return false if the deletion fails`()
    {
        val f = File("Test/File.txt")
        f.createNewFile()

        FileUtil.deleteFileIfExists("Test") shouldBe false

        val log = verifyLog(CODE_FILE_ERROR, Severity.ERROR)
        log.message shouldBe "Failed to delete file Test"
        log.errorObject?.shouldBeInstanceOf<DirectoryNotEmptyException>()
    }

    @Test
    fun `Should swap in a file successfully`()
    {
        val current = File("Test/Current.txt")
        current.createNewFile()
        current.writeText("Current")

        val new = File("Test/New.txt")
        new.createNewFile()
        new.writeText("New")

        FileUtil.swapInFile(current.path, new.path)

        new.exists() shouldBe false
        current.exists() shouldBe true
        current.readLines().shouldContainExactly("New")
    }

    @Test
    fun `Should swap in a directory successfully`()
    {
        File("Test/Current").mkdir()
        File("Test/New").mkdir()

        val current = File("Test/Current/File.txt")
        current.createNewFile()
        current.writeText("Current")

        val new = File("Test/New/File.txt")
        new.createNewFile()
        new.writeText("New")

        FileUtil.swapInFile("Test/Current", "Test/New")

        new.exists() shouldBe false
        current.exists() shouldBe true
        current.readLines().shouldContainExactly("New")
    }

    @Test
    fun `Should read image dimensions correctly`()
    {
        val bean = javaClass.getResource("/Bean.png")
        val statsIcon = javaClass.getResource("/stats_large.png")

        FileUtil.getImageDim(bean.path) shouldBe Dimension(150, 150)
        FileUtil.getImageDim(statsIcon.path) shouldBe Dimension(48, 48)
    }

    @Test
    fun `Should extract correct bytes`()
    {
        val bytes = FileUtil.getByteArrayForResource("/Bean.png")

        val ii = ImageIcon(bytes)
        ii.iconWidth shouldBe 150
        ii.iconHeight shouldBe 150
    }
}