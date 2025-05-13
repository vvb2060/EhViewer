-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void check*(...);
	public static void throw*(...);
}

-assumenosideeffects class java.util.Objects{
    ** requireNonNull(...);
}

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

-repackageclasses
-allowaccessmodification
-overloadaggressively

-assumenosideeffects class androidx.appcompat.app.AppCompatDelegate {
    static boolean isAutoStorageOptedIn(android.content.Context) return true;
    static void syncLocalesToFramework(android.content.Context);
}
-checkdiscard class androidx.appcompat.app.AppCompatDelegate {
    static boolean isAutoStorageOptedIn(android.content.Context);
    static void syncLocalesToFramework(android.content.Context);
}

-assumenosideeffects class okhttp3.internal.publicsuffix.PublicSuffixDatabase {
    public java.lang.String getEffectiveTldPlusOne(java.lang.String) return com.hippo.ehviewer.BuildConfig.APPLICATION_ID;
}
-checkdiscard class okhttp3.internal.publicsuffix.PublicSuffixDatabase {
    public java.lang.String getEffectiveTldPlusOne(java.lang.String);
}
-keep,allowobfuscation class com.hippo.ehviewer.BuildConfig {
    public static java.lang.String APPLICATION_ID;
}

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.openjsse.**
