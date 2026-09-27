package com.mt5dashboard.scenario;

import com.mt5dashboard.core.Signal;

/**
 * مرجع فعلی برای تشخیص Dual in One Timeframe: آخرین سیگنالی که واقعاً
 * با قیمت قبل از خودش فرق داشت.
 *
 * === تاریخچه تغییرات ===
 * نسخه v11: فقط (price, receivedAt) نگه می‌داشت.
 * نسخه v12: Signal کامل نگه می‌دارد.
 *
 * دلیل تغییر (تصمیم E): با تصمیم E، ترکیب Trigger در حالت Dual =
 * {Reference, Incoming} است. برای ساخت این ترکیب دو عضوی، به Signal
 * کامل نیاز داریم (نه فقط price و receivedAt). چون در UI و در
 * بررسی state، به Symbol/TF/Direction/Id هم نیاز است.
 *
 * Immutable است؛ هر بار قیمت جدیدی (متفاوت) برسد، یک نمونهٔ تازه
 * جایگزین نمونهٔ قبلی می‌شود - نه اینکه فیلدهای همان شیء تغییر کند.
 */
final class DualTimeframeReference {
    final Signal signal;

    DualTimeframeReference(Signal signal) {
        this.signal = signal;
    }
}
