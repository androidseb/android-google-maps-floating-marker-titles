package com.exlyo.gmfmt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;

final class GMFMTUtils {
	private static final float MIN_LUMINANCE_TO_LIGHT_TINTING = 0.75F;

	private static float colorLuminance(@ColorInt int _color) {
		final float red = Color.red(_color) / 255F;
		final float green = Color.green(_color) / 255F;
		final float blue = Color.blue(_color) / 255F;

		return 0.2126F * red + 0.7152F * green + 0.0722F * blue;
	}

	public static boolean isDarkColor(@ColorInt final int _color) {
		return colorLuminance(_color) < MIN_LUMINANCE_TO_LIGHT_TINTING;
	}

	public static float dipToPixels(final Context _context, final float _dipValue) {
		final DisplayMetrics metrics = _context.getResources().getDisplayMetrics();
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, _dipValue, metrics);
	}

	public static int getCanvasWidth(@NonNull final Canvas _canvas) {
		final Rect clipBounds = _canvas.getClipBounds();
		return Math.abs(clipBounds.right - clipBounds.left);
	}

	public static int getCanvasHeight(@NonNull final Canvas _canvas) {
		final Rect clipBounds = _canvas.getClipBounds();
		return Math.abs(clipBounds.bottom - clipBounds.top);
	}

	/**
	 * Computes the screen space (width and height) occupied by some text with a given text paint, if the text needed to fit in a given
	 * width/height with ellipsis
	 */
	public static Point measureMultiLineEllipsizedText(@NonNull final TextPaint _textPaint, final int _maxWidth, final int _maxHeight,
		@NonNull final String _text) {
		final StaticLayout measuringTextLayout =
			new StaticLayout(_text, _textPaint, _maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
		final int resWidth;
		final int resHeight;
		if (measuringTextLayout.getLineCount() == 1) {
			final Rect lineBounds = new Rect();
			measuringTextLayout.getLineBounds(0, lineBounds);
			resWidth = (int) Math.ceil(_textPaint.measureText(_text));
			resHeight = measuringTextLayout.getHeight();
		} else {
			resWidth = measuringTextLayout.getWidth();
			resHeight = Math.min(_maxHeight, measuringTextLayout.getHeight());
		}
		return new Point(resWidth, resHeight);
	}

	public static void drawMultiLineText(@NonNull final Canvas _canvas, @NonNull final TextPaint _textPaint, final float _x, final float _y,
		final float _width, @NonNull final String _text) {
		final StaticLayout drawingTextLayout =
			new StaticLayout(_text, _textPaint, (int) Math.abs(_width), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

		_canvas.save();
		_canvas.translate(_x, _y);
		drawingTextLayout.draw(_canvas);
		_canvas.restore();
	}

	@Nullable
	public static String getTruncatedText(final @NonNull TextPaint _textPaint, final float _width, final float _height,
		final @NonNull String _text) {
		if (_text.length() < 3) {
			return _text;
		}
		final StaticLayout measuringTextLayout =
			new StaticLayout(_text, _textPaint, (int) Math.abs(_width), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

		final int totalLineCount = measuringTextLayout.getLineCount();

		int line;
		for (line = 1; line < totalLineCount; line++) {
			final int lineBottom = measuringTextLayout.getLineBottom(line);
			if (lineBottom > _height) {
				break;
			}
		}
		line--;

		if (line < 0) {
			return null;
		}

		int lineEnd;
		try {
			lineEnd = measuringTextLayout.getLineEnd(line);
		} catch (Throwable t) {
			lineEnd = _text.length();
		}
		String truncatedText = _text.substring(0, Math.max(0, lineEnd));

		if (truncatedText.length() < 3) {
			return null;
		}

		if (truncatedText.length() < _text.length()) {
			truncatedText = truncatedText.substring(0, Math.max(0, truncatedText.length() - 3));
			truncatedText += "...";
		}
		return truncatedText;
	}
}
