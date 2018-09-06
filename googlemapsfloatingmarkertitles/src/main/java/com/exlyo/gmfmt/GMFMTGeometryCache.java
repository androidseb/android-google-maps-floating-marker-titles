package com.exlyo.gmfmt;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class GMFMTGeometryCache {
	@NonNull
	private final FloatingMarkerTitlesOverlay fmto;
	@NonNull
	private final Rect viewBounds;
	@NonNull
	private final GoogleMap googleMap;
	@NonNull
	private final Map<LatLng, Point> cacheMap = new HashMap<>();

	@Nullable
	private CameraPosition lastFrameCameraPosition = null;

	GMFMTGeometryCache(@NonNull final FloatingMarkerTitlesOverlay _fmto, @NonNull final GoogleMap _googleMap) {
		fmto = _fmto;
		viewBounds = new Rect(0, 0, 1, 1);
		googleMap = _googleMap;
	}

	/**
	 * Called by the parent FloatingMarkerTitlesOverlay before drawing every frame. Updates information important for the cache and gets
	 * to a ready state to draw the next frame.
	 */
	public void prepareForNewFrame(@NonNull final Canvas _canvas) {
		viewBounds.right = GMFMTUtils.getCanvasWidth(_canvas);
		viewBounds.bottom = GMFMTUtils.getCanvasHeight(_canvas);
		final CameraPosition cameraPosition = googleMap.getCameraPosition();
		if (lastFrameCameraPosition != null) {
			smartCacheUpdate(lastFrameCameraPosition, cameraPosition);
		}
		lastFrameCameraPosition = cameraPosition;
	}

	/**
	 * Updates the cache content for cacheMap in a smart way: normally, each floating marker title's location on screen needs to be
	 * calculated using the following code: <code>googleMap.getProjection().toScreenLocation(_latLng)</code>
	 * <p>
	 * In the case the map's zoom level or bearing hasn't changed any marker's screen location will receive the same update/translation. The
	 * translation is applicable to any marker already cached, so in this case we will only compute the updated screen location for one
	 * marker, calculate the deltaX and deltaY, and apply that change to all cached screen locations.
	 */
	private void smartCacheUpdate(@NonNull final CameraPosition _previousCameraPosition, @NonNull final CameraPosition _cameraPosition) {
		if (cacheMap.isEmpty()) {
			// If the cache map is empty, there is nothing smart to do about it anyways
			return;
		}

		if (_previousCameraPosition.equals(_cameraPosition)) {
			// If the camera position hasn't changed, then the cache doesn't need any change, returning here
			return;
		}

		// Check for cases where we cannot use a smart update
		if (_cameraPosition.tilt != 0 // The tilt of the camera is not 0 (aka perspective aka multiplier applied to coordinates)
			|| _previousCameraPosition.zoom != _cameraPosition.zoom // The zoom changed (aka multiplier applied to coordinates)
			|| _previousCameraPosition.bearing != _cameraPosition.bearing // The bearing changed (aka rotation applied to coordinates)
			) {
			// If anything else than the target of the camera position has changed, we cannot use a smart update
			cacheMap.clear();
			return;
		}

		final Iterator<LatLng> iterator = cacheMap.keySet().iterator();
		final LatLng latLngSample = iterator.next();
		final Point pointSample = cacheMap.get(latLngSample);
		final Point updatedPointSample = googleMap.getProjection().toScreenLocation(latLngSample);
		final int deltaX = updatedPointSample.x - pointSample.x;
		final int deltaY = updatedPointSample.y - pointSample.y;
		for (final Point p : cacheMap.values()) {
			p.x += deltaX;
			p.y += deltaY;
		}
	}

	@NonNull
	public Point getScreenLocation(@NonNull final LatLng _latLng) {
		final Point cacheRes = cacheMap.get(_latLng);
		if (cacheRes != null) {
			return cacheRes;
		}
		final Point res = googleMap.getProjection().toScreenLocation(_latLng);
		cacheMap.put(_latLng, res);
		return res;
	}

	@NonNull
	public RectF computeDisplayAreaRect(@NonNull final MarkerInfo _markerInfo) {
		final Point screenLocation = getScreenLocation(_markerInfo.getCoordinates());
		final TextPaint usedTextPaint = _markerInfo.isBoldText() ? fmto.boldTextPaint : fmto.regularTextPaint;
		final Point textSize = GMFMTUtils.measureMultiLineEllipsizedText(//
			usedTextPaint,//
			(int) fmto.maxTextWidth,//
			(int) fmto.maxTextHeight,//
			_markerInfo.getTitle()//
		);
		final float left = screenLocation.x + fmto.textPaddingToMarker;
		final int top = screenLocation.y - textSize.y / 2;
		final float right = screenLocation.x + textSize.x + fmto.textPaddingToMarker;
		final int bottom = screenLocation.y + textSize.y / 2;
		return new RectF(left, top, right, bottom);
	}

	public boolean isInScreenBounds(@NonNull final LatLng _coordinates) {
		final Point point = getScreenLocation(_coordinates);
		return viewBounds.contains(point.x, point.y);
	}
}
