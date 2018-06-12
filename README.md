## Bold360ai Android SDK Samples

This project hosts samples for the [Bold360AI Android SDK](https://github.com/nanorepsdk/Bold360ai-Android-SDK)

## Requirements

* Android Studio 3.0+
* Android Api Level 16+

## Installation Instructions

### Running the ConversationDemo sample:
1. Clone the repository
2. Navigate into "ConversationDemo\app" and open gradle.build file. Insert valid values for acccount values in the proper places.
```gradle
buildTypes.each {
            it.buildConfigField "String", "ACCOUNT_NAME", "\"insert value here\""
            it.buildConfigField "String", "API_KEY", "\"insert value here\""
            it.buildConfigField "String", "KNOWLEDGE_BASE", "\"insert value here\""
            it.buildConfigField "String", "SERVER", "\"insert value here\""
        }
```
3. build + run

[Learn more about the SDK and how to use it](https://github.com/bold360ai/bold360ai_android_sdk/wiki/HowToUseSDK).

## License and Copyright Information
All code in this project is released under the [Apache License 2.0](http://www.apache.org/licenses/) unless a different license for a particular library is specified in the applicable library path.   

Copyright © 2018 Nanorep. All rights reserved.   
Authors and contributors: See [GitHub contributors list](https://github.com/nanorepsdk/NRSDK-Samples/graphs/contributors).
