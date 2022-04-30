import org.junit.Test
import kotlin.math.floor
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SphericalMercatorTest {

    private val sm = SphericalMercator()
    val MAX_EXTENT_MERC = listOf(-20037508.342789244,-20037508.342789244,20037508.342789244,20037508.342789244)
    private val MAX_EXTENT_WGS84 = listOf(-180.0, -85.0511287798066, 180.0, 85.0511287798066)

    @Test
    fun bbox1() {
        assertEquals(
            listOf(-180.0, -85.05112877980659, 180.0, 85.0511287798066),
            sm.bbox(0.0, 0.0, 0, true, Projection.WGS84)
        )
    }

    @Test
    fun bbox2() {
        assertEquals(
            listOf(-180.0, -85.05112877980659, 0.0, 0.0),
            sm.bbox(0.0, 0.0, 1, true, Projection.WGS84)
        )
    }

    @Test
    fun xyz1() {
        assertEquals(
            mapOf("minX" to 0.0, "minY" to 0.0, "maxX" to 0.0, "maxY" to 0.0),
            sm.xyz(listOf(-180.0, -85.05112877980659, 180.0 ,85.0511287798066),0 , true, Projection.WGS84)
        )
    }

    @Test
    fun xyz2() {
        assertEquals(
            mapOf("minX" to 0.0, "minY" to 0.0, "maxX" to 0.0, "maxY" to 0.0),
            sm.xyz(listOf(-180.0, -85.05112877980659, 0.0, 0.0),1 , true, Projection.WGS84)
        )
    }

    @Test
    fun xyzBroken() {
        val xyz = sm.xyz(listOf(-0.087891, 40.95703, 0.087891, 41.044916),3 , true, Projection.WGS84)
        assertTrue(
            xyz["minX"]!! <= xyz["maxX"]!!
        )
        assertTrue(
            xyz["minY"]!! <= xyz["maxY"]!!
        )
    }

    @Test
    fun xyzNegative() {
        val xyz = sm.xyz(listOf(-112.5, 85.0511, -112.5, 85.0511),0)
        assertEquals(xyz["minY"]!!, 0.0)
    }

    @Test
    fun xyzFuzz() {
        val x = listOf(-180 + (360*Math.random()), -180 + (360*Math.random()))
        val y = listOf(-85 + (170*Math.random()), -85 + (170*Math.random()))
        val z = floor(22*Math.random())
        val extent = listOf(
            x.minOrNull()!!,
            y.minOrNull()!!,
            x.maxOrNull()!!,
            y.maxOrNull()!!
        )
        val xyz = sm.xyz(extent, z.toInt(), true, Projection.WGS84)
        if (xyz["minX"]!! > xyz["maxX"]!!) {
            assertTrue(
                xyz["minX"]!! <= xyz["maxX"]!!
            )
        }
        if (xyz["minY"]!! > xyz["maxY"]!!) {
            assertTrue(
                xyz["minY"]!! <= xyz["maxY"]!!
            )
        }
    }

    @Test
    fun convert() {
        assertEquals(
            MAX_EXTENT_MERC,
            sm.convert(MAX_EXTENT_WGS84, Projection.WEB_MERCATOR)
        )
        assertEquals(
            MAX_EXTENT_WGS84,
            sm.convert(MAX_EXTENT_MERC, Projection.WGS84)
        )
    }

    @Test
    fun extents() {
        assertEquals(
            MAX_EXTENT_MERC,
            sm.convert(listOf(-240.0, -90.0, 240.0, 90.0), Projection.WEB_MERCATOR)
        )
        assertEquals(
            mapOf("minX" to 0.0, "minY" to 0.0, "maxX" to 15.0, "maxY" to 15.0),
            sm.xyz(listOf(-240.0, -90.0, 240.0, 90.0), 4, true, Projection.WGS84)
        )
    }

    @Test
    fun ll() {
        assertEquals(
            listOf(-179.45068359375, 85.00351401304403),
            sm.ll(listOf(200.0, 200.0), 9)
        )
    }

    @Test
    fun px() {
        assertEquals(
            listOf(364.0, 215.0),
            sm.px(listOf(-179.0, 85.0), 9)
        )
        assertEquals(
            listOf(4096.0, 2014.0),
            sm.px(listOf(250.0, 3.0), 4)
        )
    }
}