package com.exlyo.gmfmt;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Basic information of a marker used to display as floating text: its coordinates and its title
 */
public class MarkerInfo {
	@Nullable
	private Marker marker;
	@NonNull
	private LatLng coordinates;
	@NonNull
	private String title;
	private int color;
	private boolean visible;

	@SuppressWarnings("unused")
	public MarkerInfo(@NonNull final LatLng _coordinates, @NonNull final String _title, final int _color) {
		this(_coordinates, _title, _color, true);
	}

	@SuppressWarnings("unused")
	public MarkerInfo(@NonNull final Marker _marker, final int _color) {
		this(_marker.getPosition(), _marker.getTitle(), _color, _marker.isVisible());
		marker = _marker;
	}

	@SuppressWarnings("unused")
	public MarkerInfo(@NonNull final LatLng _coordinates, @NonNull final String _title, final int _color, final boolean _visible) {
		coordinates = _coordinates;
		title = _title;
		color = _color;
		visible = _visible;
	}

	public void setCoordinates(@NonNull final LatLng _coordinates) {
		coordinates = _coordinates;
	}

	public void setTitle(@NonNull final String _title) {
		title = _title;
	}

	public void setVisible(final boolean _visible) {
		visible = _visible;
	}

	/**
	 * Sets a marker as source of the information.
	 * This will clear the previous values passed to setCoordinates() and setTitle()
	 */
	public void setMarker(@NonNull final Marker _marker) {
		marker = _marker;
		coordinates = _marker.getPosition();
		title = marker.getTitle();
		visible = marker.isVisible();
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
}
