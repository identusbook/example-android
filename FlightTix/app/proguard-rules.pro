# Keep Identus SDK + kotlinx.serialization generated serializers.
-keep class org.hyperledger.identus.** { *; }
-keepclassmembers class com.identusbook.flighttix.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.identusbook.flighttix.**$$serializer { *; }
