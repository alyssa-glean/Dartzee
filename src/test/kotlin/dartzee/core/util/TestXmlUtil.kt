package dartzee.core.util

import dartzee.helper.AbstractTest
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class TestXmlUtil: AbstractTest()
{
    @Test
    fun `Should convert an XML doc to a string`()
    {
        val doc = XmlUtil.factoryNewDocument()

        val rootElement = doc.createRootElement("Root")

        val childA = doc.createElement("A")
        rootElement.appendChild(childA)

        rootElement.setAttribute("Foo", "Bar")

        doc.toXmlString() shouldBe """<Root Foo="Bar"><A/></Root>"""
    }

    @Test
    fun `Should handle an empty document`()
    {
        val doc = XmlUtil.factoryNewDocument()
        doc.toXmlString() shouldBe ""
    }

    @Test
    fun `Should return null for an invalid string`()
    {
        val str = "bugg'rit"
        str.toXmlDoc() shouldBe null
    }

    @Test
    fun `Should convert a valid xml string back to a document`()
    {
        val str = """<Xml Foo="bar"/>"""
        val doc = str.toXmlDoc()!!

        val element = doc.documentElement
        element.tagName shouldBe "Xml"
        element.getAttribute("Foo") shouldBe "bar"
    }

    @Test
    fun `Should create a root element and append it to the doc`()
    {
        val doc = XmlUtil.factoryNewDocument()
        doc.createRootElement("Baz")

        val element = doc.documentElement
        element.tagName shouldBe "Baz"
    }

    @Test
    fun `Should handle setting non-string attributes`()
    {
        val doc = XmlUtil.factoryNewDocument()
        val root = doc.createRootElement("Root")

        root.setAttributeAny("Foo", 1)
        root.getAttribute("Foo") shouldBe "1"
    }

    @Test
    fun `Should return integer value if valid`()
    {
        val doc = XmlUtil.factoryNewDocument()
        val root = doc.createRootElement("Root")

        root.setAttributeAny("Foo", 1)
        root.getAttributeInt("Foo") shouldBe 1
    }

    @Test
    fun `Should return defaultValue if not present`()
    {
        val doc = XmlUtil.factoryNewDocument()
        val root = doc.createRootElement("Root")

        root.getAttributeInt("Foo", 12) shouldBe 12
    }

    @Test
    fun `Should return 0 if not present and no default value specified`()
    {
        val doc = XmlUtil.factoryNewDocument()
        val root = doc.createRootElement("Root")

        root.getAttributeInt("Foo") shouldBe 0
    }
}