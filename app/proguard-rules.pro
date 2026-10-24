# Reguły R8 dla wersji release.
# Room, Compose, WorkManager i Navigation dostarczają własne reguły – tu tylko to, co używa refleksji.

# SDK Anthropic (generowanie quizów) serializuje żądania i odpowiedzi Jacksonem, przez refleksję.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keep class com.anthropic.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn com.anthropic.**
-dontwarn com.fasterxml.jackson.**

# Klasy JDK nieobecne na Androidzie – używane tylko przez nieużywane przez nas ścieżki SDK.
-dontwarn java.lang.reflect.AnnotatedParameterizedType
-dontwarn java.lang.reflect.AnnotatedType
