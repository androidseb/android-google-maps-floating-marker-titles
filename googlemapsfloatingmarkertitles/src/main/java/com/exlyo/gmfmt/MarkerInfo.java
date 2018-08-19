package com.exlyo.gmfmt;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Basic information of a marker used to display as floating text: its coordinates and its title
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class MarkerInfo {
	@Nullable
	private Marker marker;
	@NonNull
	private LatLng coordinates;
	@NonNull
	private String title;
	private int color;
	private boolean visible;
	private float zIndex;
	private boolean boldText;

	public MarkerInfo(@NonNull final LatLng _coordinates, @NonNull final String _title, final int _color) {
		this(_coordinates, _title, _color, true);
	}

	public MarkerInfo(@NonNull final Marker _marker, final int _color) {
		this(_marker.getPosition(), _marker.getTitle(), _color, _marker.isVisible());
		marker = _marker;
	}

	private MarkerInfo(@NonNull final LatLng _coordinates, @NonNull final String _title, final int _color, final boolean _visible) {
		coordinates = _coordinates;
		title = _title;
		color = _color;
		visible = _visible;
	}

	public MarkerInfo setCoordinates(@NonNull final LatLng _coordinates) {
		coordinates = _coordinates;
		return this;
	}

	public MarkerInfo setTitle(@NonNull final String _title) {
		title = _title;
		return this;
	}

	public MarkerInfo setVisible(final boolean _visible) {
		visible = _visible;
		return this;
	}

	/**
	 * Sets the Z index of the marker info.
	 * <p>
	 * The z-index specifies the stack order of this marker, relative to other markers on the map. A marker with a high z-index will have
	 * its floating title drawn on top of floating titles for markers with lower z-indexes. The default z-index value is 0.
	 *
	 * @param _zIndex: the desired z-index
	 */
	public MarkerInfo setZIndex(final float _zIndex) {
		zIndex = _zIndex;
		return this;
	}

	public MarkerInfo setBoldText(final boolean _boldText) {
		boldText = _boldText;
		return this;
	}

	/**
	 * Sets a marker as source of the information.
	 * This will clear the previous values passed to setCoordinates() and setTitle()
	 */
	public MarkerInfo setMarker(@NonNull final Marker _marker) {
		marker = _marker;
		coordinates = _marker.getPosition();
		title = marker.getTitle();
		visible = marker.isVisible();
		return this;
	}

	@NonNull
	public LatLng getCoordinates() {
		final Marker m = marker;
		if (m == null) {
			return coordinates;
		} else {
			return m.getPosition();
		}
	}

	@NonNull
	public String getTitle() {
		final Marker m = marker;
		if (m == null) {
			return title;
		} else {
			return m.getTitle();
		}
	}

	public int getColor() {
		return color;
	}

	public boolean isVisible() {
		final Marker m = marker;
		if (m == null) {
			return visible;
		} else {
			return m.isVisible();
		}
	}

	public float getZIndex() {
		final Marker m = marker;
		if (m == null) {
			return zIndex;
		} else {
			return m.getZIndex();
		}
	}

	public boolean isBoldText() {
		return boldText;
	}
}
