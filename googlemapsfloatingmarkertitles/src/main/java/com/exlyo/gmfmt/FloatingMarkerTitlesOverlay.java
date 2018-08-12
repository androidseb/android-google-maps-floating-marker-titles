package com.exlyo.gmfmt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This view is meant to be overlayed on top of a map with the exact same dimensions as the map.
 * It will attempt to redraw all the time to keep the marker floating titles up to date with the map below.
 */
public class FloatingMarkerTitlesOverlay extends View {
	/* The fade in animation time for text appearing */
	private static final long FADE_ANIMATION_TIME = 300;

	@Nullable
	private GoogleMap googleMap;

	@NonNull
	private final Map<Long, MarkerInfo> markerIdToMarkerInfoMap = new HashMap<>();

	@NonNull
	private final List<MarkerInfo> markerInfoList = new ArrayList<>();

	/* List of markers that are currently displayed as floating text */
	@NonNull
	private final List<MarkerInfo> displayedMarkersList = new ArrayList<>();

	/* Map of displayed MarkerInfo to the rectangle their floating text is taking on the screen */
	@NonNull
	private final Map<MarkerInfo, RectF> displayedMarkerIdToScreenRect = new HashMap<>();

	/* Map of displayed MarkerInfo to time they were added, to properly calculate the animation state (text alpha) */
	@NonNull
	private final Map<MarkerInfo, Long> displayedMarkerIdToAddedTime = new HashMap<>();

	private float textPaddingToMarker;

	private int maxFloatingTitlesCount;

	private int maxNewMarkersCheckPerFrame;

	private float maxTextWidth;

	private float maxTextHeight;

	private TextPaint textPaint;

	public FloatingMarkerTitlesOverlay(final Context context) {
		super(context);
		initFMTOverlay();
	}

	public FloatingMarkerTitlesOverlay(final Context context, @Nullable final AttributeSet attrs) {
		super(context, attrs);
		initFMTOverlay();
	}

	public FloatingMarkerTitlesOverlay(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initFMTOverlay();
	}

	private void initFMTOverlay() {
		textPaint = new TextPaint();
		textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		textPaint.setStrokeWidth(GMFMTUtils.dipToPixels(getContext(), 3));

		setTextSizeDIP(12);
		setTextPaddingToMarkerDIP(8);
		setMaxFloatingTitlesCount(20);
		setSetMaxNewMarkersCheckPerFrame(100);
		setMaxTextWidthDIP(200);
		setMaxTextHeightDIP(48);
	}

	public void setTextSizeDIP(final int _textSizeDIP) {
		textPaint.setTextSize(GMFMTUtils.dipToPixels(getContext(), _textSizeDIP));
	}

	/**
	 * Set the spacing between the marker location and the floating text
	 */
	@SuppressWarnings("unused")
	public void setTextPaddingToMarkerDIP(final int _textPaddingToMarkerDIP) {
		textPaddingToMarker = GMFMTUtils.dipToPixels(getContext(), _textPaddingToMarkerDIP);
	}

	/**
	 * Set the maximum number of floating titles displayed at the same time
	 */
	public void setMaxFloatingTitlesCount(final int _maxFloatingTitlesCount) {
		synchronized (markerInfoList) {
			maxFloatingTitlesCount = _maxFloatingTitlesCount;
			displayedMarkersList.clear();
			displayedMarkerIdToScreenRect.clear();
			displayedMarkerIdToAddedTime.clear();
		}
	}

	/**
	 * Set the maximum number of checks for new markers every display frame. Raising the value will decrease performance for maps with a
	 * lot of markers, but increase responsiveness when a marker's title should appear. The default value is 100.
	 * <p>
	 * For example, if you set this value to 50 and you have 2000 markers, it might take up to 2000/50 = 40 frames before a specific
	 * marker's title to appear when it should display. Assuming you're having 60 frames per second, it will take about 0.66 seconds.
	 */
	public void setSetMaxNewMarkersCheckPerFrame(final int _setMaxNewMarkersCheckPerFrame) {
		maxNewMarkersCheckPerFrame = _setMaxNewMarkersCheckPerFrame;
	}

	public void setMaxTextWidthDIP(final int _maxTextWidthDIP) {
		maxTextWidth = GMFMTUtils.dipToPixels(getContext(), _maxTextWidthDIP);
	}

	public void setMaxTextHeightDIP(final int _maxTextHeightDIP) {
		maxTextHeight = GMFMTUtils.dipToPixels(getContext(), _maxTextHeightDIP);
	}

	public void setSource(@Nullable final GoogleMap _googleMap) {
		if (_googleMap == null) {
			clearMarkers();
			this.googleMap = null;
		}
		this.googleMap = _googleMap;
	}

	/**
	 * Removes all the tracked markers from the overlay.
	 */
	public void clearMarkers() {
		synchronized (markerInfoList) {
			markerIdToMarkerInfoMap.clear();
			markerInfoList.clear();
			displayedMarkersList.clear();
			displayedMarkerIdToScreenRect.clear();
			displayedMarkerIdToAddedTime.clear();
		}
	}

	/**
	 * Adds a marker to track with the overlay.
	 *
	 * @param _id:         ID to track the marker for further removal
	 * @param _markerInfo: MarkerInfo object containing the info of the marker
	 */
	public void addMarker(final long _id, @NonNull final MarkerInfo _markerInfo) {
		synchronized (markerInfoList) {
			markerIdToMarkerInfoMap.put(_id, _markerInfo);
			markerInfoList.add(_markerInfo);
		}
	}

	/**
	 * Removes a marker from the overlay by ID.
	 *
	 * @param _id: ID of the marker to remove from the overlay
	 */
	public void removeMarker(final long _id) {
		synchronized (markerInfoList) {
			final MarkerInfo markerInfo = markerIdToMarkerInfoMap.get(_id);
			if (markerInfo != null) {
				markerInfoList.remove(markerInfo);
				displayedMarkersList.remove(markerInfo);
				displayedMarkerIdToScreenRect.remove(markerInfo);
				displayedMarkerIdToAddedTime.remove(markerInfo);
			}
		}
	}

	@Override
	public void draw(final Canvas _canvas) {
		super.draw(_canvas);
		final GoogleMap gm = googleMap;
		if (_canvas == null || gm == null) {
			return;
		}
		synchronized (markerInfoList) {
			drawFloatingMarkerTitles(_canvas, gm);
		}
		postInvalidate();
	}

	private void drawFloatingMarkerTitles(@NonNull final Canvas _canvas, @NonNull final GoogleMap _googleMap) {
		final Projection mapProjection = _googleMap.getProjection();
		updateCurrentlyDisplayedMarkers(_canvas, mapProjection);
		for (final MarkerInfo mi : displayedMarkersList) {
			final RectF displayArea = displayedMarkerIdToScreenRect.get(mi);
			if (displayArea == null) {
				continue;
			}
			final Long addedTime = displayedMarkerIdToAddedTime.get(mi);
			final int alpha;
			if (addedTime == null) {
				alpha = 255;
			} else {
				long currentTimeMillis = System.currentTimeMillis();
				if (currentTimeMillis < addedTime) {
					alpha = 255;
				} else {
					final long elapsedTime = currentTimeMillis - addedTime;
					if (elapsedTime > FADE_ANIMATION_TIME) {
						alpha = 255;
					} else {
						alpha = (int) ((float) elapsedTime / (float) FADE_ANIMATION_TIME * 255F);
					}
				}
			}
			drawMarkerFloatingTitle(_canvas, mi, displayArea, alpha);
		}
	}

	private void updateCurrentlyDisplayedMarkers(@NonNull final Canvas _canvas, @NonNull final Projection _mapProjection) {
		final Rect currentViewBounds = new Rect(0, 0, GMFMTUtils.getCanvasWidth(_canvas), GMFMTUtils.getCanvasHeight(_canvas));

		// Remove the currently displayed markers that are no longer in the view bounds
		for (int i = displayedMarkersList.size() - 1; i >= 0; i--) {
			final MarkerInfo mi = displayedMarkersList.get(i);
			final LatLng coordinates = mi.getCoordinates();
			boolean needToRemove = false;
			if (mi.isVisible()) {
				final Point markerScreenLocation = _mapProjection.toScreenLocation(coordinates);
				if (!currentViewBounds.contains(markerScreenLocation.x, markerScreenLocation.y)) {
					needToRemove = true;
				}
			} else {
				needToRemove = true;
			}
			if (!needToRemove) {
				continue;
			}
			displayedMarkersList.remove(i);
			displayedMarkerIdToScreenRect.remove(mi);
			displayedMarkerIdToAddedTime.remove(mi);
		}

		// Remove the currently displayed marker foating titles that are in conflict with another displayed marker foating title
		final List<MarkerInfo> markerInfoToRemove = new ArrayList<>();
		for (final MarkerInfo mi : displayedMarkerIdToScreenRect.keySet()) {
			for (final MarkerInfo mi2 : displayedMarkerIdToScreenRect.keySet()) {
				if (mi == mi2) {
					continue;
				}
				if (markerInfoToRemove.contains(mi2)) {
					continue;
				}
				final RectF miDisplayArea = displayedMarkerIdToScreenRect.get(mi);
				final RectF mi2DisplayArea = displayedMarkerIdToScreenRect.get(mi2);
				if (RectF.intersects(miDisplayArea, mi2DisplayArea)) {
					markerInfoToRemove.add(mi);
					break;
				}
			}
		}
		for (final MarkerInfo mi : markerInfoToRemove) {
			displayedMarkersList.remove(mi);
			displayedMarkerIdToScreenRect.remove(mi);
			displayedMarkerIdToAddedTime.remove(mi);
		}

		// Update the displayed marker titles display area rectangles
		for (final MarkerInfo mi : displayedMarkersList) {
			final RectF displayAreaRect = computeDisplayAreaRect(_mapProjection, mi);
			displayedMarkerIdToScreenRect.put(mi, displayAreaRect);
		}

		// If the number of displayed floating titles is already maxed, we exit the function because we're done
		if (displayedMarkersList.size() >= maxFloatingTitlesCount) {
			return;
		}

		// Fill up the displayed markers list with visible markers
		final int numberOfMarkersToCheck = Math.min(markerInfoList.size(), maxNewMarkersCheckPerFrame);
		for (int i = 0; i < numberOfMarkersToCheck; i++) {
			final MarkerInfo mi = markerInfoList.remove(0);
			markerInfoList.add(mi);
			if (!mi.isVisible()) {
				// If the marker is not visible, we continue to the next marker
				continue;
			}
			if (displayedMarkersList.contains(mi)) {
				// If the marker is already in the displayed markers, we continue to the next marker
				continue;
			}
			final LatLng coordinates = mi.getCoordinates();
			final Point markerScreenLocation = _mapProjection.toScreenLocation(coordinates);
			if (!currentViewBounds.contains(markerScreenLocation.x, markerScreenLocation.y)) {
				// If the marker is not visible, we continue to the next marker
				continue;
			}

			final RectF displayAreaRect = computeDisplayAreaRect(_mapProjection, mi);
			boolean displayAreaConflict = false;
			for (final RectF rect : displayedMarkerIdToScreenRect.values()) {
				if (RectF.intersects(rect, displayAreaRect)) {
					displayAreaConflict = true;
					break;
				}
			}
			if (displayAreaConflict) {
				// If the marker is in conflict with another marker (title overlap), we continue to the next marker
				continue;
			}

			displayedMarkersList.add(mi);
			displayedMarkerIdToScreenRect.put(mi, displayAreaRect);
			displayedMarkerIdToAddedTime.put(mi, System.currentTimeMillis());

			// If the number of displayed floating titles is already maxed, we exit the function because we're done
			if (displayedMarkersList.size() >= maxFloatingTitlesCount) {
				return;
			}
		}
	}

	@NonNull
	private RectF computeDisplayAreaRect(@NonNull final Projection _mapProjection, @NonNull final MarkerInfo _markerInfo) {
		final Point screenLocation = _mapProjection.toScreenLocation(_markerInfo.getCoordinates());
		final Point textSize = GMFMTUtils.measureMultiLineEllipsizedText(//
			textPaint,//
			(int) maxTextWidth,//
			(int) maxTextHeight,//
			_markerInfo.getTitle()//
		);
		final float left = screenLocation.x + textPaddingToMarker;
		final int top = screenLocation.y - textSize.y / 2;
		final int right = screenLocation.x + textSize.x;
		final int bottom = screenLocation.y + textSize.y / 2;
		return new RectF(left, top, right, bottom);
	}

	private void drawMarkerFloatingTitle(final @NonNull Canvas _canvas, @NonNull final MarkerInfo _markerInfo,
		@NonNull final RectF _displayArea, final int _alpha) {
		final int markerColor = _markerInfo.getColor();
		final String markerTitle = _markerInfo.getTitle();
		textPaint.setStyle(Paint.Style.STROKE);
		if (GMFMTUtils.isDarkColor(markerColor)) {
			textPaint.setColor(Color.WHITE);
		} else {
			textPaint.setColor(Color.BLACK);
		}
		textPaint.setAlpha(_alpha / 2);
		GMFMTUtils.drawMultiLineEllipsizedText(//
			_canvas,//
			textPaint,//
			_displayArea.left,//
			_displayArea.top,//
			_displayArea.left + maxTextWidth,//
			_displayArea.bottom,//
			markerTitle//
		);
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setColor(markerColor);
		textPaint.setAlpha(_alpha);
		GMFMTUtils.drawMultiLineEllipsizedText(//
			_canvas,//
			textPaint,//
			_displayArea.left,//
			_displayArea.top,//
			_displayArea.left + maxTextWidth,//
			_displayArea.bottom,//
			markerTitle//
		);
	}
}
