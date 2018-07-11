
### How to run the ConversationDemo:
1. Download the demo project
2. Navigate into "ConversationDemo\app" and open gradle.build file. Insert valid values for acccount parameters in the proper places.   
__*<sup>`(Ensure to keep slashes and quots as is!!)`</sup>*__
```gradle
buildTypes.each {
            it.buildConfigField "String", "ACCOUNT_NAME", "\"insert value here\""
            it.buildConfigField "String", "API_KEY", "\"insert value here\""
            it.buildConfigField "String", "KNOWLEDGE_BASE", "\"insert value here\""
            it.buildConfigField "String", "SERVER", "\"insert value here\""
}
```
3. build + run
