# Begin: Debug ProGuard rules

#-dontobfuscate
#-keepattributes SoureFile,LineNumberTable
#-injars      bin/classes
#-injars      libs
#-outjars     bin/classes-processed.jar
#-libraryjars /home/olivia/Android/Sdk/platforms/android-26/android.jar
#-injars      libs
#-injars device.sdk.jar
#-libraryjars /home/olivia/PMPOS_share/WorkSpace/PMPOS_1211_ok/app/libs/device.sdk.jar
#-keep class device.sdk.MsrManager.** {
#
#public *;
#
#}



#-ignorewarnings device.sdk.MsrManager.**
-dontwarn device.sdk.*
-dontwarn device.common.*
#injars      bin/classes
#-injars      libs
#-injars     device.sdk.jar
#-libraryjars ‘/home/olivia/PMPOS_share/WorkSpace/PMPOS_0118/app/libs/device.sdk.jar’
#// The -dontwarn option tells ProGuard not to complain about some artefacts in the Scala runtime

