package com.exlyo.gmfmt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;

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
	private GMFMTGeometryCache geometryCache;

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

	float textPaddingToMarker;

	private int maxFloatingTitlesCount;

	private int maxNewMarkersCheckPerFrame;

	float maxTextWidth;

	float maxTextHeight;

	TextPaint regularTextPaint;
	TextPaint boldTextPaint;

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
		regularTextPaint = new TextPaint();
		regularTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		regularTextPaint.setStrokeWidth(GMFMTUtils.dipToPixels(getContext(), 3));
		boldTextPaint = new TextPaint();
		boldTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		boldTextPaint.setStrokeWidth(GMFMTUtils.dipToPixels(getContext(), 3));
		boldTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

		setTextSizeDIP(14);
		setTextPaddingToMarkerDIP(8);
		setMaxFloatingTitlesCount(100);
		setSetMaxNewMarkersCheckPerFrame(10);
		setMaxTextWidthDIP(200);
		setMaxTextHeightDIP(48);
	}

	public void setTextSizeDIP(final int _textSizeDIP) {
		regularTextPaint.setTextSize(GMFMTUtils.dipToPixels(getContext(), _textSizeDIP));
		boldTextPaint.setTextSize(GMFMTUtils.dipToPixels(getContext(), _textSizeDIP));
	}

	/**
	 * Set the spacing between the marker location and the floating text
	 */
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
		postInvalidate();
	}

	/**
	 * Set the maximum number of checks for new markers every display frame. Raising the value will decrease performance for maps with a
	 * lot of markers, but increase responsiveness when a marker's title should appear. The default value is 10.
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
			geometryCache = null;
		} else {
			geometryCache = new GMFMTGeometryCache(this, _googleMap);
		}
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
		if (maxFloatingTitlesCount == 0) {
			return;
		}
		final GMFMTGeometryCache gc = geometryCache;
		if (_canvas == null || gc == null) {
			return;
		}
		synchronized (markerInfoList) {
			drawFloatingMarkerTitles(_canvas, gc);
		}
		postInvalidate();
	}

	private void drawFloatingMarkerTitles(@NonNull final Canvas _canvas, @NonNull final GMFMTGeometryCache _geometryCache) {
		_geometryCache.prepareForNewFrame(_canvas);
		updateCurrentlyDisplayedMarkers(_geometryCache);
		for (final MarkerInfo mi : displayedMarkersList) {
			drawMarkerFloatingTitle(_canvas, mi);
		}
	}

	private void updateCurrentlyDisplayedMarkers(@NonNull final GMFMTGeometryCache _geometryCache) {
		// Remove the currently displayed markers that are no longer in the view bounds
		removeOutOfViewMarkerTitles(_geometryCache);

		// Remove the currently displayed marker floating titles that are in conflict with another displayed marker floating title
		removeConflictedMarkerTitles();

		// Determine the minimum z-index among the visible floating marker titles
		float minVisibleZIndex = 0F;

		// Update the displayed marker titles display area rectangles
		for (final MarkerInfo mi : displayedMarkersList) {
			final RectF currentArea = displayedMarkerIdToScreenRect.get(mi);
			//We only recompute the location, because the text size is still correct and expensive to calculate
			final Point newLocation = _geometryCache.getScreenLocation(mi.getCoordinates());
			currentArea.set(//
				(float) newLocation.x + textPaddingToMarker,//
				(float) newLocation.y - currentArea.height() / 2,//
				(float) newLocation.x + textPaddingToMarker + currentArea.width(),//
				(float) newLocation.y + currentArea.height() / 2//
			);
			if (minVisibleZIndex > mi.getZIndex()) {
				minVisibleZIndex = mi.getZIndex();
			}
		}

		// Prepare the list of markers to add
		final List<MarkerInfo> markersToAdd = computeMarkersToAdd(_geometryCache, minVisibleZIndex);

		// Fill the displayed markers list with markers to check
		for (final MarkerInfo mi : markersToAdd) {
			final RectF displayAreaRect = _geometryCache.computeDisplayAreaRect(mi);
			displayedMarkersList.add(mi);
			displayedMarkerIdToScreenRect.put(mi, displayAreaRect);
			displayedMarkerIdToAddedTime.put(mi, System.currentTimeMillis());
		}
	}

	private void removeOutOfViewMarkerTitles(@NonNull final GMFMTGeometryCache _geometryCache) {
		for (int i = displayedMarkersList.size() - 1; i >= 0; i--) {
			final MarkerInfo mi = displayedMarkersList.get(i);
			boolean needToRemove = false;
			if (mi.isVisible()) {
				if (!_geometryCache.isInScreenBounds(mi.getCoordinates())) {
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
	}

	private void removeConflictedMarkerTitles() {
		final List<MarkerInfo> markerInfoToRemove = new ArrayList<>();

		float minZIndex = 0;
		for (final MarkerInfo mi : displayedMarkerIdToScreenRect.keySet()) {
			if (mi.getZIndex() < minZIndex) {
				minZIndex = mi.getZIndex();
			}
			for (final MarkerInfo mi2 : displayedMarkerIdToScreenRect.keySet()) {
				if (mi == mi2) {
					continue;
				}
				if (markerInfoToRemove.contains(mi)) {
					continue;
				}
				if (markerInfoToRemove.contains(mi2)) {
					continue;
				}
				final boolean displayConflict;
				final RectF miDisplayArea = displayedMarkerIdToScreenRect.get(mi);
				final RectF mi2DisplayArea = displayedMarkerIdToScreenRect.get(mi2);
				displayConflict = RectF.intersects(miDisplayArea, mi2DisplayArea);
				if (displayConflict) {
					if (mi.getZIndex() > mi2.getZIndex()) {
						markerInfoToRemove.add(mi2);
					} else {
						markerInfoToRemove.add(mi);
					}

					break;
				}
			}
		}

		for (int i = 0;//
			 i < displayedMarkersList.size() &&//
				 displayedMarkersList.size() - markerInfoToRemove.size() > maxFloatingTitlesCount//
			; i++) {
			final MarkerInfo mi = displayedMarkersList.get(i);
			if (!markerInfoToRemove.contains(mi) && mi.getZIndex() == minZIndex) {
				markerInfoToRemove.add(mi);
			}
		}

		for (final MarkerInfo mi : markerInfoToRemove) {
			displayedMarkersList.remove(mi);
			displayedMarkerIdToScreenRect.remove(mi);
			displayedMarkerIdToAddedTime.remove(mi);
		}
	}

	/**
	 * Determines the list of markers to add next. Since the number of markers we will check is limited by maxNewMarkersCheckPerFrame, the
	 * list rotation is essential to ensure all the markers in the list are checked eventually (over several draw() calls).
	 * <p>
	 * The created list will attempt to respect maxFloatingTitlesCount. However if some markers have a higher z-index than _minZIndex, they
	 * will still be added, which will make the limit go over for the current frame.
	 * On the next frame however, lower z-indexes will be discared.
	 */
	@NonNull
	private List<MarkerInfo> computeMarkersToAdd(@NonNull final GMFMTGeometryCache _geometryCache, final float _minZIndex) {
		final ArrayList<MarkerInfo> markersToAdd = new ArrayList<>();

		// Adding the maximum number of markers to markersToAdd
		final int numberOfMarkersToCheck = Math.min(markerInfoList.size(), maxNewMarkersCheckPerFrame);
		for (int i = 0; i < numberOfMarkersToCheck; i++) {
			// List rotation, we will take the first element of the list and put it to the end, numberOfMarkersToCheck times
			final MarkerInfo mi = markerInfoList.remove(0);
			markerInfoList.add(mi);

			if (!mi.isVisible()) {
				// If the marker is not visible, we don't add it
				continue;
			}
			if (displayedMarkersList.contains(mi)) {
				// If the marker is already in the displayed markers, we don't add it
				continue;
			}

			if (isMarkerTitleInConflictWithDisplay(_geometryCache, mi)) {
				// If the marker is in conflict with display, we don't add it
				continue;
			}

			markersToAdd.add(mi);
		}

		// While we're above display limit count, we remove markers without a stricly higher z-index than _minZIndex
		final int remainingDisplaySlots = maxFloatingTitlesCount - displayedMarkersList.size();

		for (int i = markersToAdd.size() - 1; i >= 0 && remainingDisplaySlots < markersToAdd.size(); i--) {
			final MarkerInfo mi = markersToAdd.get(i);
			if (!_geometryCache.isInScreenBounds(mi.getCoordinates())) {
				// If the marker is not visible, we remove it
				markersToAdd.remove(i);
			}
		}
		for (int i = markersToAdd.size() - 1; i >= 0 && remainingDisplaySlots < markersToAdd.size(); i--) {
			final MarkerInfo mi = markersToAdd.get(i);
			if (mi.getZIndex() <= _minZIndex) {
				markersToAdd.remove(i);
			}
		}

		return markersToAdd;
	}

	private boolean isMarkerTitleInConflictWithDisplay(final GMFMTGeometryCache _geometryCache, final MarkerInfo _markerInfo) {
		final RectF displayAreaRect = _geometryCache.computeDisplayAreaRect(_markerInfo);
		for (final MarkerInfo mi2 : displayedMarkerIdToScreenRect.keySet()) {
			final RectF rect = displayedMarkerIdToScreenRect.get(mi2);
			if (RectF.intersects(rect, displayAreaRect)) {
				// If _markerInfo is in conflict with another marker, we compare the z-index
				if (_markerInfo.getZIndex() <= mi2.getZIndex()) {
					// If _markerInfo has equal or lower Z-index, it's considered in conflict with display
					return true;
				}
				// If _markerInfo has higher Z-index, it's considered prioritary compared to the other marker
			}
		}
		return false;
	}

	private void drawMarkerFloatingTitle(final @NonNull Canvas _canvas, @Nullable final MarkerInfo _markerInfo) {
		if (_markerInfo == null) {
			return;
		}
		final RectF displayArea = displayedMarkerIdToScreenRect.get(_markerInfo);
		if (displayArea == null) {
			return;
		}
		final Long addedTime = displayedMarkerIdToAddedTime.get(_markerInfo);
		final int alpha = computeMarkerFloatingTitleAlpha(addedTime);
		drawMarkerFloatingTitleOnCanvas(_canvas, _markerInfo, displayArea, alpha);
	}

	private int computeMarkerFloatingTitleAlpha(@Nullable final Long _addedTime) {
		final int alpha;
		if (_addedTime == null) {
			alpha = 255;
		} else {
			long currentTimeMillis = System.currentTimeMillis();
			if (currentTimeMillis < _addedTime) {
				alpha = 255;
			} else {
				final long elapsedTime = currentTimeMillis - _addedTime;
				if (elapsedTime > FADE_ANIMATION_TIME) {
					alpha = 255;
				} else {
					alpha = (int) ((float) elapsedTime / (float) FADE_ANIMATION_TIME * 255F);
				}
			}
		}
		return alpha;
	}

	private void drawMarkerFloatingTitleOnCanvas(final @NonNull Canvas _canvas, @NonNull final MarkerInfo _markerInfo,
		@NonNull final RectF _displayArea, final int _alpha) {
		final int markerColor = _markerInfo.getColor();
		final String markerTitle = _markerInfo.getTitle();
		final TextPaint usedTextPaint = _markerInfo.isBoldText() ? boldTextPaint : regularTextPaint;
		usedTextPaint.setStyle(Paint.Style.STROKE);
		if (GMFMTUtils.isDarkColor(markerColor)) {
			usedTextPaint.setColor(Color.WHITE);
			usedTextPaint.setAlpha((int) (_alpha / 1.2F));
		} else {
			usedTextPaint.setColor(Color.BLACK);
			usedTextPaint.setAlpha((int) (_alpha / 2F));
		}
		final String truncatedText = GMFMTUtils.getTruncatedText(usedTextPaint, maxTextWidth, _displayArea.height(), markerTitle);
		if (truncatedText == null) {
			return;
		}
		GMFMTUtils.drawMultiLineText(//
			_canvas,//
			usedTextPaint,//
			_displayArea.left,//
			_displayArea.top,//
			(float) Math.ceil(_displayArea.width()),//
			truncatedText//
		);
		usedTextPaint.setStyle(Paint.Style.FILL);
		usedTextPaint.setColor(markerColor);
		usedTextPaint.setAlpha(_alpha);
		GMFMTUtils.drawMultiLineText(//
			_canvas,//
			usedTextPaint,//
			_displayArea.left,//
			_displayArea.top,//
			(float) Math.ceil(_displayArea.width()),//
			truncatedText//
		);
	}
}
