package com.shohan.omnicalc

import android.app.Application

/**
 * Application entry point. Holds the manual dependency container once the
 * data layer is wired in (kept intentionally simple — no DI framework until
 * it earns its complexity).
 */
class OmniCalcApp : Application()
