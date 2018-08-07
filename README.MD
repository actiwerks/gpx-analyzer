# GPX Analyzer for Android

## Building and installation

GPX Analyzer uses standard Android Studio project structure. Targeting Lollipop (API 21 and higher).
Build.gradle file contains a list of dependencies, alter this list to adjust the linked libraries versions.
Provided configuration should work out of the box.
There is a pre-built APK in the /bin folder which is signed and ready to be installed on the device
(non Google Play sources needs to be enabled for this to work). Production build should create a new signed copy
of the APK, but you need first create your own release.properties file and provide path to the Keystore file
containing the Signing Certificate. Use the release.properties.sample file as template.

## Usage

When application is started, default blank map is shown. Select a track to analyze by using the ActionBar menu
option "Select". The Selection Activity appears, which requires network connection to be present.
Only links with the names of the tracks on the Open Street Map site are supported, as well as navigation between pages,
all other links are ignored. Once the track is selected, its description is downloaded and it is being analyzed
in the background. Once this step is complete (which might take some time, if the track consists of large
amount of points)

## Implementation details

For Distance and Bearing calculations, [the haversine formula](https://en.wikipedia.org/wiki/Haversine_formula) is used
All the lists are kept in the memory, and looped sequentially, which is satisfactory from both memory and performance
point of views with intended usage of low thousands of points in the analyzed GPX files. The application works reasonably
well on the mid-range Android Lollipop device.

## Troubleshooting

On certain devices the map might fail to show the background tiles. This is caused by the External storage being mountend as read only,
please use the application on the device that has external storage in write mode.

## Feedback

Please send your comments or questions to [the author](mailto:pavel@actiwerks.com)
