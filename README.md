# Android Google Maps Floating Marker Titles
This library is useful if you want to see the titles of the markers on the map floating next to the marker. It attempts to reproduce the behavior for points of interest shown on the map in Google Maps. Since there is no way to draw directly into the map component, it works as a view to apply on top of the map as an overlay.

I created this library for my app Map Marker (not open source at the moment):
https://play.google.com/store/apps/details?id=com.exlyo.mapmarker

![](./visual_demo.gif)

## Project structure
The library project is the `googlemapsfloatingmarkertitles` folder. The root of the repository is also an Android studio project with a sample app's code.

## Sample app setup
To make the sample app work, you will need to update the Android manifest file `app/src/main/AndroidManifest.xml` and update this section with your Google Maps API key:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY_GOES_HERE" />
```

## How to use this library in your code
1. You will have to add the library folder manually to your Android project
2. Add a FloatingMarkerTitlesOverlay view on top of your map view in your XML layout
3. In your code, retrieve the FloatingMarkerTitlesOverlay view and initialize it like this
```java
private FloatingMarkerTitlesOverlay floatingMarkersOverlay;
@Override
public void onMapReady(GoogleMap googleMap) {
    //Code related to initializing googleMap related attributes

    //Initializing FloatingMarkerTitlesOverlay
    floatingMarkersOverlay = findViewById(R.id.map_floating_markers_overlay);
    floatingMarkersOverlay.setSource(googleMap);

    //From now on, you can add markers to be tracked by floatingMarkersOverlay
}
```
4. Add a marker to the map
```java
final long id = 0;
final LatLng latLng = new LatLng(-34, 151);
final String title = "A cool marker";
final int color = Color.GREEN;
final MarkerInfo mi = new MarkerInfo(latLng, title, color);
googleMap.addMarker(new MarkerOptions().position(mi.getCoordinates()));
//Adding the marker to track by the overlay
//To remove that marker, you will need to call floatingMarkersOverlay.removeMarker(id)
floatingMarkersOverlay.addMarker(id, mi);
```

## Main library features
- Display floating markers titles on top of the map
- Works with ancient versions of Android: minSdkVersion 9
- Automatically avoids overlap between floating marker titles, will not display a title if overlapping with others
- Set z-indexes for floating marker titles to specify which title has the most priority for display: <code>MarkerInfo.setZIndex(...)</code>
- Set whether floating marker titles should be written in bold: <code>MarkerInfo.setBoldText(...)</code>
- Marker title text transparent outline for better visuals: the text will be readable no matter the map background and the outline color will adapt to white or black depending on the text color's luminance (perceived brightness)
- Marker title fade-in animation for better visuals
- Set the text size: <code>FloatingMarkerTitlesOverlay.setTextSizeDIP(...)</code>
- Set the distance between the text and the marker center: <code>FloatingMarkerTitlesOverlay.setTextPaddingToMarkerDIP(...)</code>
- Set the maximum number of floating titles: <code>FloatingMarkerTitlesOverlay.setMaxFloatingTitlesCount(...)</code>
- No performance drop with more markers once the maximum number of floating titles has been reached, since the library only scans for a limited number of markers per frame, which can be set with <code>FloatingMarkerTitlesOverlay.setSetMaxNewMarkersCheckPerFrame(...)</code>
- Set the maximum width of floating titles: <code>FloatingMarkerTitlesOverlay.setMaxTextWidthDIP(...)</code>
- Set the maximum height of floating titles: <code>FloatingMarkerTitlesOverlay.setMaxTextHeightDIP(...)</code>
