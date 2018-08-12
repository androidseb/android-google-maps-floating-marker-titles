package com.exlyo.gmfmtexample;

import com.exlyo.gmfmt.MarkerInfo;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class SampleMarkerData {
    private static final int MARKER_COUNT_SQRT = 20;

    private static final double BASE_LAT = -34;

    private static final double BASE_LNG = 151;

    private static String[] MARKER_TITLES = {
            "A",
            "A marker",
            "And another marker",
            "Wow",
            "This marker has a long title",
            "This marker has a very very very very very very very very very long title",
            "This marker has a very very very very very very very very very very very very very very very very very very long title",
            "This marker has a very very very very very very very very very very very very very very very very very very very very very very very very very very very very very very very very very very very very long title",
            "Restaurant",
            "Bakery",
            "Shop",
            "And another example",
    };

    private static int[] MARKER_COLORS = {
            0xFFf44336,
            0xFFe91e63,
            0xFF9c27b0,
            0xFF673ab7,
            0xFF3f51b5,
            0xFF2196f3,
            0xFF03a9f4,
            0xFF00bcd4,
            0xFF009688,
            0xFF71B300,
            0xFF8bc34a,
            0xFFcddc39,
            0xFFffeb3b,
            0xFFffc107,
            0xFFff9800,
            0xFFff5722,
            0xFF795548,
            0xFF9e9e9e,
            0xFF607d8b,
    };

    public static List<MarkerInfo> getSampleMarkersInfo() {
        final ArrayList<MarkerInfo> res = new ArrayList<>();
        int counter = 0;
        for (int i = 0; i < MARKER_COUNT_SQRT; i++) {
            for (int j = 0; j < MARKER_COUNT_SQRT; j++) {
                final LatLng latLng = new LatLng(//
                        BASE_LAT + (double) i / (double) MARKER_COUNT_SQRT,//
                        BASE_LNG + (double) j / (double) MARKER_COUNT_SQRT//
                );
                final String title = MARKER_TITLES[counter % MARKER_TITLES.length];
                final int color = MARKER_COLORS[counter % MARKER_COLORS.length];
                res.add(new MarkerInfo(latLng, title, color));
                counter++;
            }
        }
        return res;
    }
}
