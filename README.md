# sphericalmercator-kotlin
Spherical Mercator math in Kotlin, ported from [@mapbox/sphericalmercator](https://github.com/mapbox/sphericalmercator)

## Spatial Reference System (srs)

* `Projection.WGS84` = wgs84
* `Projection.WEB_MERCATOR` = 900913

## API

### `px(ll, zoom)`

Convert longitude/latitude to screen pixel value.

* `ll` Array of `[longitude, latitude]` of geographic coordinate.
*  `zoom` Zoom level

Returns screen pixel value in `[x, y]`

### `ll(px, zoom)`

Convert screen pixel value to longitude/latitude.

* `px` Screen pixel `[x,y]` of geographic coordinate.
* `zoom` Zoom level.

Returns geographic coordinate `[longitude, latitude]`.

### `bbox(x, y, zoom, tmsStyle, srs)`

Convert tile x/y and zoom level to bounding box.

* `x` X (along longitude line) number.
* `y` Y (along latitude line) number.
* `zoom` Zoom level.
* `tmsStyle` Whether to compute using tms-style. Default is false.
* `srs` Projection for resulting bounding box. Default is Projection.WGS84.

Returns bounding box array of values in the form `[w, s, e, n]`.

### `xyz(bbox, zoom, tmsStyle, srs)`

Convert bounding box to xyz bounds in the form `[minX, maxX, minY, maxY]`.

* `bbox` Bounding box in the form `[w, s, e, n]`.
* `zoom` Zoom level.
* `tmsStyle` Whether to compute using tms-style. Default is false.
* `srs` Map projection. Default is Projection.WGS84.

Returns XYZ bounds containing minX, maxX, minY and maxY.

### `convert(bbox, to)`

Convert projection of given box.

* `bbox` Bounding box in the form `[w, s, e, n]`.
* `to` Projection of output bounding box. Input bounding box assumed to be the "other" projection. Default is Projection.WGS84.

Returns bounding box with reprojected coordinates in the form `[w, s, e, n]`.

### `forward(ll)`

Convert longitude/latitude values to 900913 x/y.

* `ll` Geographic coordinate in the form `[longitude, latitude]`.

Returns converted geographic coordinate in the form `[longitude, latitude]`.

### `inverse(xy)`

Convert 900913 x/y values to lon/lat.

* `xy` Geographic coordinate in the form `[x,y]`.

Returns converted geographic coordinate in the form `[longitude, latitude]`.
