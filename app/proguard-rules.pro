# ProGuard / R8 Rules for Holy Quran App
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep Room database and entities
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep our data models and DAO
-keep class com.example.data.model.** { *; }
-keep class com.example.data.db.** { *; }

# Keep Kotlin coroutines and reflection
-keepclassmembers class kotlinx.coroutines.** { *; }
