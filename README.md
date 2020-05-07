# MockLocation

This app is for GPS location testing.

## How to enable this app

1. Install
1. Allow MockLocation to access location in App info
    * Apps & notifications
    * Select MockLocation
    * Tap "Permissions" and allow location access all the time
1. In Developer Options, tap "Select mock location app" and select MockLocation


## How to mock location

1. Start app
1. Tap "Start mocked location service" button
1. Send broadcast intent via adb as follows;
    * `adb shell am broadcast -a inject_location --esa latlon "48.873792,2.295028"`
1. You can see lat & lon change in app screen


## TODO

* Make enable to receive other Location element (time, attlitude...)