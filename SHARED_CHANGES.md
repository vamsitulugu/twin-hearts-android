# SHARED_CHANGES — Phase 1 (1.1 Get Started, 1.2 Sign In / Sign Up / Forgot Password)

## Gradle/catalog lines
None.

## Manifest entries
None. (INTERNET is already there.)

## AppNavHost line
`FeatureGraphs.kt` is the one designated plug-in file (R-43). To save you hand-editing, the zip contains the finished file:
`app/src/main/java/com/twinhearts/app/core/navigation/FeatureGraphs.kt`
It only does two things: adds `getStartedGraph(navController)` and `authGraph(navController)`, and removes the four stand-in screens (GetStarted, SignIn, SignUp, ForgotPassword) from the pending list.

## TwinDatabase entities
None.

## Strings/permissions
New files: `strings_getstarted.xml`, `strings_auth.xml`. No permissions.

## Backend changes
None.
