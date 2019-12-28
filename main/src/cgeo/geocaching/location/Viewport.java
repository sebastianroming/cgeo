package cgeo.geocaching.location;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.ICoordinates;
import cgeo.geocaching.models.Waypoint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public final class Viewport {

    @NonNull public final Geopoint center;
    @NonNull public final Geopoint bottomLeft;
    @NonNull public final Geopoint topRight;

    public Viewport(@NonNull final ICoordinates point1, @NonNull final ICoordinates point2) {
        final Geopoint gp1 = point1.getCoords();
        final Geopoint gp2 = point2.getCoords();
        this.bottomLeft = new Geopoint(Math.min(gp1.getLatitude(), gp2.getLatitude()),
                Math.min(gp1.getLongitude(), gp2.getLongitude()));
        this.topRight = new Geopoint(Math.max(gp1.getLatitude(), gp2.getLatitude()),
                Math.max(gp1.getLongitude(), gp2.getLongitude()));
        this.center = new Geopoint((gp1.getLatitude() + gp2.getLatitude()) / 2,
                (gp1.getLongitude() + gp2.getLongitude()) / 2);
    }

    public Viewport(@NonNull final ICoordinates center, final double latSpan, final double lonSpan) {
        this.center = center.getCoords();
        final double centerLat = this.center.getLatitude();
        final double centerLon = this.center.getLongitude();
        final double latHalfSpan = Math.abs(latSpan) / 2;
        final double lonHalfSpan = Math.abs(lonSpan) / 2;
        bottomLeft = new Geopoint(centerLat - latHalfSpan, centerLon - lonHalfSpan);
        topRight = new Geopoint(centerLat + latHalfSpan, centerLon + lonHalfSpan);
    }

    public Viewport(@NonNull final ICoordinates point) {
        center = point.getCoords();
        bottomLeft = point.getCoords();
        topRight = point.getCoords();
    }

    public double getLatitudeMin() {
        return bottomLeft.getLatitude();
    }

    public double getLatitudeMax() {
        return topRight.getLatitude();
    }

    public double getLongitudeMin() {
        return bottomLeft.getLongitude();
    }

    public double getLongitudeMax() {
        return topRight.getLongitude();
    }

    @NonNull
    public Geopoint getCenter() {
        return center;
    }

    public double getLatitudeSpan() {
        return getLatitudeMax() - getLatitudeMin();
    }

    public double getLongitudeSpan() {
        return getLongitudeMax() - getLongitudeMin();
    }

    /**
     * Check whether a point is contained in this viewport.
     *
     * @param point
     *            the coordinates to check
     * @return true if the point is contained in this viewport, false otherwise or if the point contains no coordinates
     */
    public boolean contains(@NonNull final ICoordinates point) {
        final Geopoint coords = point.getCoords();
        return coords != null
                && coords.getLongitudeE6() >= bottomLeft.getLongitudeE6()
                && coords.getLongitudeE6() <= topRight.getLongitudeE6()
                && coords.getLatitudeE6() >= bottomLeft.getLatitudeE6()
                && coords.getLatitudeE6() <= topRight.getLatitudeE6();
    }

    /**
     * Count the number of points present in the viewport.
     *
     * @param points a collection of (possibly null) points
     * @return the number of non-null points in the viewport
     */
    public int count(@NonNull final Collection<? extends ICoordinates> points) {
        int total = 0;
        for (final ICoordinates point: points) {
            if (point != null && contains(point)) {
                total += 1;
            }
        }
        return total;
    }

    /**
     * Filter return the points present in the viewport.
     *
     * @param points
     *            a collection of (possibly null) points
     * @return a new collection containing the points in the viewport
     */
    public <T extends ICoordinates> Collection<T> filter(@NonNull final Collection<T> points) {
        final Collection<T> result = new ArrayList<>();
        for (final T point : points) {
            if (point != null && contains(point)) {
                result.add(point);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "(" + bottomLeft.toString() + "," + topRight.toString() + ")";
    }

    /**
     * Check whether another viewport is fully included into the current one.
     *
     * @param vp
     *            the other viewport
     * @return true if the viewport is fully included into this one, false otherwise
     */
    public boolean includes(@NonNull final Viewport vp) {
        return contains(vp.bottomLeft) && contains(vp.topRight);
    }

    /**
     * Return the "where" part of the string appropriate for a SQL query.
     *
     * @param dbTable
     *            the database table to use as prefix, or null if no prefix is required
     * @return the string without the "where" keyword
     */
    @NonNull
    public StringBuilder sqlWhere(@Nullable final String dbTable) {
        final String prefix = dbTable == null ? "" : (dbTable + ".");
        return new StringBuilder(prefix).append("latitude >= ").append(getLatitudeMin()).append(" and ")
                .append(prefix).append("latitude <= ").append(getLatitudeMax()).append(" and ")
                .append(prefix).append("longitude >= ").append(getLongitudeMin()).append(" and ")
                .append(prefix).append("longitude <= ").append(getLongitudeMax());
    }

    /**
     * Return a widened or shrunk viewport.
     *
     * @param factor
     *            multiplicative factor for the latitude and longitude span (> 1 to widen, < 1 to shrink)
     * @return a widened or shrunk viewport
     */
    @NonNull
    public Viewport resize(final double factor) {
        return new Viewport(getCenter(), getLatitudeSpan() * factor, getLongitudeSpan() * factor);
    }

    /**
     * Return the smallest viewport containing all the given points.
     *
     * @param points
     *            a set of points. Point with null coordinates (or null themselves) will be ignored
     * @return the smallest viewport containing the non-null coordinates, or null if no coordinates are non-null
     */
    @Nullable
    public static Viewport containing(final Collection<? extends ICoordinates> points) {
        boolean valid = false;
        double latMin = Double.MAX_VALUE;
        double latMax = -Double.MAX_VALUE;
        double lonMin = Double.MAX_VALUE;
        double lonMax = -Double.MAX_VALUE;
        for (final ICoordinates point : points) {
            if (point != null) {
                final Geopoint coords = point.getCoords();
                if (coords != null) {
                    valid = true;
                    final double latitude = coords.getLatitude();
                    final double longitude = coords.getLongitude();
                    latMin = Math.min(latMin, latitude);
                    latMax = Math.max(latMax, latitude);
                    lonMin = Math.min(lonMin, longitude);
                    lonMax = Math.max(lonMax, longitude);
                }
            }
        }
        if (!valid) {
            return null;
        }
        return new Viewport(new Geopoint(latMin, lonMin), new Geopoint(latMax, lonMax));
    }

    @Nullable
    public static Viewport containingCachesAndWaypoints(final Collection<Geocache> caches) {
        boolean valid = false;
        double latMin = Double.MAX_VALUE;
        double latMax = -Double.MAX_VALUE;
        double lonMin = Double.MAX_VALUE;
        double lonMax = -Double.MAX_VALUE;
        for (final Geocache cache : caches) {
            if (cache != null) {
                final Geopoint coords = cache.getCoords();
                if (coords != null) {
                    valid = true;
                    final double latitude = coords.getLatitude();
                    final double longitude = coords.getLongitude();
                    latMin = Math.min(latMin, latitude);
                    latMax = Math.max(latMax, latitude);
                    lonMin = Math.min(lonMin, longitude);
                    lonMax = Math.max(lonMax, longitude);
                }
                if (cache.hasWaypoints()) {
                    for (final Waypoint waypoint : cache.getWaypoints()) {
                        if (waypoint != null) {
                            final Geopoint wpcoords = waypoint.getCoords();
                            if (wpcoords != null) {
                                valid = true;
                                final double latitude = wpcoords.getLatitude();
                                final double longitude = wpcoords.getLongitude();
                                latMin = Math.min(latMin, latitude);
                                latMax = Math.max(latMax, latitude);
                                lonMin = Math.min(lonMin, longitude);
                                lonMax = Math.max(lonMax, longitude);
                            }
                        }
                    }
                }
            }
        }
        if (!valid) {
            return null;
        }
        return new Viewport(new Geopoint(latMin, lonMin), new Geopoint(latMax, lonMax));
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Viewport)) {
            return false;
        }
        final Viewport vp = (Viewport) other;
        return bottomLeft.equals(vp.bottomLeft) && topRight.equals(vp.topRight);
    }

    @Override
    public int hashCode() {
        return bottomLeft.hashCode() ^ topRight.hashCode();
    }

}
