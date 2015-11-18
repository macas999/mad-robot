# Mad Robot #
Welcome to the MadRobot Android library project page. We hope you find madrobot useful. This library is capable of many low/high level features, a few of them are illustrated below.

## Featuring Utilities that include: ##
### Custom Android Widgets ###
Well tested custom android UI widgets like,
  * Color Picker
  * Gauge
  * Horizontal scroll view
  * Joystick
  * Fluid Page curl view
  * Sortable list view
  * SVG Image view
  * GIF Image View with custom GIF decoder (Cause's android's native support for .GIF files is really bad)
  * Vertical text view
  * Fling Wheel (iPhone style)
  * Zoomable image view
  * Web image view
Custom Layouts like
  * Stick to top scroll layout
  * Greedy gird layout
  * Slide layout

and many more.

### Geometry ###
The _**com.madrobot.geom**_ package contains utilities that help with any 2D game or custom widget creation
  * Angle Utilities
  * Circle Utilities
  * Line Utilities
  * Vector Utilities
  * Point, Polygon, Rectangle, Triangle  and many more

### Bitmap Manipulation & Graphics ###
The _**com.madrobot.graphics**_ package provides extensive graphics support for creating great custom views or games. The Bitmap Utilities help perform a wide range of bitmap operations like
  * Color Depth Manipulation
  * Edge detection (Normal, LaPlace, Bump)
  * Embossing
  * Brightness
  * Stipple
  * Fade
  * Dissolve
  * Blur and debluring (motion, gaussian, box)
  * Shape Transformation (twirl, rotate, shear)
  * Tint
  * Bitmap Stretching and Scaling
  * Rotation
  * Inversion
  * Posterize
  * Saturation
  * High pass/Low pass
  * Plasma
  * Temperature
  * Channel mixing
  * Transparency
  * Sepia
  * Embossing
  * Filters and much more.

### Java Beans ###
As you are aware, Bean introspection is not supported in android's java.beans package. This library bridges that missing link by adding Bean introspection and analysis similar to java SE. The _**com.madrobot.beans**_ package includes
  * A Flexible mechanism for obtaining bean information using the **Introspector** class
  * Provides an in-depth analysis of the given bean right down to the class' field   information

### Database Utilities ###
The _**com.madrobot.db**_ package allows us to populate any java bean based on any cursor or ResultSet returned by a database operation. Thanks to introspection, we can map a table in database to a java class.

### I/O and Protocol Implementations ###
The _**com.madrobot.io**_ package allows us to perform complex I/O operations easily. Apart from network based I/O this package allows you to carried out operations on the local file system as well. The various protocol client implementations include,
  * Datagram Socket client
  * Echo TCP/UDP client
  * Finger client
  * POP3 client
  * SMTP client
  * Socket client
  * XML/RPC client
  * Comet client
  * OAuth client
  * Async Http client
  * Upload client
  * Web socket client
  * File Filters
  * File Monitors

### Data Interchange Parsers ###
The _**com.madrobot.di**_ package is home to a huge collection of parsers for formats like
  * CSV
  * JSON
  * PLIST
  * XML.

### Device Hardware Utilities ###
Query device capabilities and get elaborate information on device hardware using the _**com.madrobot.device**_ package. The DeviceInfo class allows you to get information on
  * RAM
  * Internal Storage Size
  * Network Information
  * Sensor Information
  * Device build information

### Task Pool implementation based on the Concurrent classes ###
A high performance task pool implementation that is developed for scalability and  tested for reliability. The prominent features of the _**com.madrobot.taskpool**_ package include
  * Ability to control the number of threads
  * Flexibility to perform any complex or time consuming task with minimal turn-around time.
  * Ample callbacks to determine the phase of any running or suspended task

### Logging Framework ###
The _**com.madrobot.log**_ package implements a logging framework thats completely unique to android and provides a whole new set of log appenders .
  * HTML appender with inbuilt java script based keyword search
  * File System appender that needs no log file maintenance  as it maintains only the current set of logs
  * The default DDMS and console appenders

### Reflection Utilities ###
The _**com.madrobot.reflect**_ package allows us to examine classes, enums,methods and objects

### Security Utilities ###
Fast implementations for checksum algorithms like
  * CRC16
  * CRC32
  * BSD
  * Luhn Mod10
  * Adler32
  * Verhoeff check digit
and Hashing
  * MD5
  * TigerHash
  * Whirlpool Hash
  * SHA-1
  * Cyclic
  * RabinKarp
  * Jenkins

### String Manipulation ###
The text manipulation package _**com.madrobot.text**_ provides a wide range of API's right from Character manipulation, Date formatting, String parsing/formatting to word utilities


And much more..

---

**Initated by:**Elton Kent (eltonkent@gmail.com)