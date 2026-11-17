# حاجب الريلز (للاستخدام الشخصي)

## الطريقة الأسهل: Android Studio
1. ثبّت Android Studio (النسخة الأحدث) من developer.android.com/studio
2. File ← Open ← اختر مجلد المشروع، وانتظر انتهاء Gradle Sync (أول مرة يحمّل ملفات، يحتاج إنترنت).
3. من القائمة: Build ← Build Bundle(s) / APK(s) ← Build APK(s)
4. اضغط Locate في الإشعار، الملف هنا:
   app/build/outputs/apk/debug/app-debug.apk
5. انقله للهاتف وثبّته (اسمح بالتثبيت من مصدر غير معروف).

## بدون Android Studio: عبر GitHub
1. أنشئ مستودعاً جديداً (Private) على github.com وارفع محتوى المشروع.
2. تبويب Actions ← Build APK ← Run workflow.
3. بعد دقائق نزّل ملف الـ APK من قسم Artifacts.

## التفعيل على الهاتف
1. افتح التطبيق ← «فتح إعدادات إمكانية الوصول» ← حاجب الريلز ← فعّل.
2. أندرويد 13+: إذا كان الخيار رمادياً: إعدادات ← التطبيقات ← حاجب الريلز ← ⋮ ← «السماح بالإعدادات المقيّدة».

## إذا توقف الحجب بعد تحديث إنستغرام/فيسبوك
عدّل القوائم في ReelsBlockerService.kt (موضع ✏️)، ولإيجاد المعرفات الجديدة
غيّر DEBUG_LOG_VIEW_IDS إلى true وراقب Logcat بالفلتر ReelsBlocker.
