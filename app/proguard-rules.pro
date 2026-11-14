# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to the flags specified
# in getDefaultProguardFile("proguard-android-optimize.txt").

# Keep annotations and generic signatures for reflection/serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlinx Serialization rules
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
