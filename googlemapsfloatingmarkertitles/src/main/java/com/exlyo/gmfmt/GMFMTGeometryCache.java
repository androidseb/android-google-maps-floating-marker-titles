package com.exlyo.gmfmt;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.TextPaint;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Map;

public class GMFMTGeometryCache {
	@NonNull
	private final FloatingMarkerTitlesOverlay fmto;
	@NonNull
	private final Rect viewBounds;
	@NonNull
	private final Projection mapProjection;
	@NonNull
	private final Map<LatLng, Point> cacheMap = new HashMap<>();

	GMFMTGeometryCache(@NonNull final FloatingMarkerTitlesOverlay _fmto, @NonNull final Canvas _canvas,
		@NonNull final Projection _mapProjection) {
		fmto = _fmto;
		viewBounds = new Rect(0, 0, GMFMTUtils.getCanvasWidth(_canvas), GMFMTUtils.getCanvasHeight(_canvas));
		mapProjection = _mapProjection;
	}

	@NonNull
	public Point get(@NonNull final LatLng _latLng) {
		final Point cacheRes = cacheMap.get(_latLng);
		if (cacheRes != null) {
			return cacheRes;
		}
		final Point res = mapProjection.toScreenLocation(_latLng);
		cacheMap.put(_latLng, res);
		return res;
	}

	@NonNull
	public RectF computeDisplayAreaRect(@NonNull final MarkerInfo _markerInfo) {
		final Point screenLocation = get(_markerInfo.getCoordinates());
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
		final Point point = get(_coordinates);
		return viewBounds.contains(point.x, point.y);
	}
}
