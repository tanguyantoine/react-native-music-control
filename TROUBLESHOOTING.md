# react-native-music control

## Troubleshooting

Some users reported this error while compiling the Android version:

```
Multiple dex files define Landroid/support/v4/accessibilityservice/AccessibilityServiceInfoCompat
```

To solve this, issue just copy this line at the end of your application build.gradle

**android/app/build.gradle**

```diff
+configurations.all {
+    resolutionStrategy.eachDependency { DependencyResolveDetails details ->
+        def requested = details.requested
+        if (requested.group == 'com.android.support') {
+            if (!requested.name.startsWith("multidex")) {
+                details.useVersion '26.0.1'
+            }
+        }
+    }
+}
```
