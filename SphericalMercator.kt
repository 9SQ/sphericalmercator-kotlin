import kotlin.math.*

enum class Projection(val value: String) {
    WGS84("wgs84"),
    WEB_MERCATOR("900913");
}

class SphericalMercator(private val size: Int = 256) {
    private val cache: MutableMap<Int, Map<String, List<Double>>> = mutableMapOf()
    private val EPSLN: Double = 1e-10
    private val D2R: Double = PI / 180.0
    private val R2D: Double = 180.0 / PI
    // 900913 properties.
    private val A: Double = 6378137.0
    private val MAXEXTENT: Double = 20037508.342789244

    private val Bc: List<Double>
    private val Cc: List<Double>
    private val zc: List<Double>
    private val Ac: List<Double>

    init {
        if (this.cache[size] == null) {
            var _size = size
            val c: MutableMap<String, MutableList<Double>> = mutableMapOf()
            c["Bc"] = mutableListOf()
            c["Cc"] = mutableListOf()
            c["zc"] = mutableListOf()
            c["Ac"] = mutableListOf()
            for (_0 in 0 until 30) {
                c["Bc"]!!.add(_size / 360.0)
                c["Cc"]!!.add(_size / (2.0 * PI))
                c["zc"]!!.add(_size / 2.0)
                c["Ac"]!!.add(_size * 1.0)
                _size *= 2
            }
            this.cache[size] = c
        }
        this.Bc = this.cache[size]!!["Bc"]!!
        this.Cc = this.cache[size]!!["Cc"]!!
        this.zc = this.cache[size]!!["zc"]!!
        this.Ac = this.cache[size]!!["Ac"]!!
    }

    /**
     * Convert longitude/latitude to screen pixel value
     *
     * @param[ll] Array of [longitude, latitude] of geographic coordinate
     * @param[zoom] Zoom level
     * @return Screen pixel value in [x, y]
     */
    fun px(ll: List<Double>, zoom: Int): List<Double> {
        val d: Double = this.zc[zoom]
        val f: Double = min(max(sin(D2R * ll[1]), -0.9999), 0.9999)
        var x: Double = round(d + ll[0] * this.Bc[zoom])
        var y: Double = round(d + 0.5 * log((1.0 + f) / (1.0 - f), E) * (-this.Cc[zoom]))
        if (x > this.Ac[zoom]) {
            x = this.Ac[zoom]
        }
        if (y > this.Ac[zoom]) {
            y = this.Ac[zoom]
        }
        return listOf(x, y)
    }

    /**
     * Convert screen pixel value to longitude/latitude
     *
     * @param[px] Screen pixel [x,y] of geographic coordinate
     * @param[zoom] Zoom level
     * @return Geographic coordinate [longitude, latitude]
     */
    fun ll(px: List<Double>, zoom: Int): List<Double> {
        val g: Double = (px[1] - this.zc[zoom]) / (-this.Cc[zoom])
        val lon: Double = (px[0] - this.zc[zoom]) / this.Bc[zoom]
        val lat: Double = R2D * (2.0 * atan(exp(g)) - 0.5 * PI)
        return listOf(lon, lat)
    }

    /**
     * Convert tile x/y and zoom level to bounding box.
     *
     * @param[x] X (along longitude line) number
     * @param[y] Y (along latitude line) number
     * @param[zoom] Zoom level
     * @param[tmsStyle] Whether to compute using tms-style. Default is false
     * @param[srs] Projection for resulting bounding box. Default is WGS84.
     * @return Bounding box array of values in the form [w, s, e, n]
     */
    fun bbox(x: Double, y: Double, zoom: Int, tmsStyle: Boolean = false, srs: Projection = Projection.WGS84): List<Double> {
        var _y: Double = y
        // Convert xyz into bbox with srs WGS84
        if (tmsStyle) {
            _y = (2.0.pow(zoom) - 1.0) - y
        }
        // Use +y to make sure it's a number to avoid inadvertent concatenation.
        val ll: List<Double> = listOf(x * this.size, (+_y + 1.0) * this.size) // lower left
        // Use +x to make sure it's a number to avoid inadvertent concatenation.
        val ur: List<Double> = listOf((+x + 1.0) * this.size, _y * this.size) // upper right
        val bbox: List<Double> = this.ll(ll, zoom).plus(this.ll(ur, zoom))

        // If web mercator requested reproject to 900913.
        if (srs == Projection.WEB_MERCATOR) {
            return this.convert(bbox, Projection.WEB_MERCATOR)
        } else {
            return bbox
        }
    }

    /**
     * Convert bounding box to xyz bounds in the form [minX, maxX, minY, maxY].
     *
     * @param[bbox] Bounding box in the form [w, s, e, n]
     * @param[zoom] Zoom level
     * @param[tmsStyle] Whether to compute using tms-style. Default is false.
     * @param[srs] Map projection. Default is WGS84.
     * @return XYZ bounds containing minX, maxX, minY and maxY.
     */
    fun xyz(bbox: List<Double>, zoom: Int, tmsStyle: Boolean = false, srs: Projection = Projection.WGS84): Map<String, Double> {
        var _bbox: List<Double> = bbox
        // If web mercator provided reproject to WGS84.
        if (srs == Projection.WEB_MERCATOR) {
            _bbox = this.convert(bbox, Projection.WGS84)
        }

        val ll: List<Double> = listOf(_bbox[0], _bbox[1]) // lower left
        val ur: List<Double> = listOf(_bbox[2], _bbox[3]) // upper right
        val px_ll: List<Double> = this.px(ll, zoom)
        val px_ur: List<Double> = this.px(ur, zoom)
        // Y = 0 for XYZ is the top hence minY uses px_ur[1].
        val x: List<Double> = listOf(floor(px_ll[0] / this.size), floor((px_ur[0] - 1.0) / this.size))
        val y: List<Double> = listOf(floor(px_ur[1] / this.size), floor((px_ll[1] - 1.0) / this.size))
        val bounds: MutableMap<String, Double> = mutableMapOf(
            "minX" to if (min(x[0], x[1]) < 0.0) {
                0.0
            } else {
                min(x[0], x[1])
            },
            "minY" to if (min(y[0], y[1]) < 0.0) {
                0.0
            } else {
                min(y[0], y[1])
            },
            "maxX" to max(x[0], x[1]),
            "maxY" to max(y[0], y[1])
        )
        if (tmsStyle) {
            val tms: Map<String, Double> = mapOf(
                "minY" to (2.0.pow(zoom) - 1.0) - bounds["maxY"]!!,
                "maxY" to (2.0.pow(zoom) - 1.0) - bounds["minY"]!!
            )
            bounds["minY"] = tms["minY"] as Double
            bounds["maxY"] = tms["maxY"] as Double
        }
        return bounds
    }

    /**
     * Convert projection of given box.
     *
     * @param[bbox] Bounding box in the form [w, s, e, n]
     * @param[to] Projection of output bounding box. Input bounding box assumed to be the "other" projection. Default is WGS84.
     * @return Bounding box with reprojected coordinates in the form [w, s, e, n].
     */
    fun convert(bbox: List<Double>, to: Projection = Projection.WGS84): List<Double> {
        return if (to == Projection.WEB_MERCATOR) {
            this.forward(bbox.slice(0..1)).plus(this.forward(bbox.slice(2..3)))
        } else {
            this.inverse(bbox.slice(0..1)).plus(this.inverse(bbox.slice(2..3)))
        }
    }

    /**
     * Convert longitude/latitude values to 900913 x/y.
     *
     * @param[ll] Geographic coordinate in the form [longitude, latitude]
     * @return Converted geographic coordinate in the form [longitude, latitude].
     */
    fun forward(ll: List<Double>): List<Double> {
        val xy: MutableList<Double> = mutableListOf(A * ll[0] * D2R, A * log(tan((PI * 0.25) + (0.5 * ll[1] * D2R)), E))
        // if xy value is beyond maxextent (e.g. poles), return maxextent.
        if (xy[0] > MAXEXTENT) {
            xy[0] = MAXEXTENT
        }
        if (xy[0] < -MAXEXTENT) {
            xy[0] = -MAXEXTENT
        }
        if (xy[1] > MAXEXTENT) {
            xy[1] = MAXEXTENT
        }
        if (xy[1] < -MAXEXTENT) {
            xy[1] = -MAXEXTENT
        }
        return xy
    }

    /**
     * Convert 900913 x/y values to lon/lat.
     *
     * @param[xy] Geographic coordinate in the form [x,y]
     * @return Converted geographic coordinate in the form [longitude, latitude]
     */
    fun inverse(xy: List<Double>): List<Double> {
        return listOf(xy[0] * R2D / A, ((PI * 0.5) - 2.0 * atan(exp(-xy[1] / A))) * R2D)
    }
}