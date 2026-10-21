# Keep rules for release builds.
# App has no reflection-based serialization or third-party SDKs; add rules only when justified.

# Coroutines debugging metadata is not needed in release.
-dontwarn kotlinx.coroutines.**
