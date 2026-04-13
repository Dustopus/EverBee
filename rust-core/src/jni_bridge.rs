use crate::calculator::CostCalculator;
use jni::objects::{JClass, JString};
use jni::sys::{jdouble, jlong};
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_com_apislens_rust_RustCore_nativeCalculateDailyCost(
    mut env: JNIEnv,
    _class: JClass,
    purchase_price_cents: jlong,
    purchase_date: JString,
) -> jdouble {
    let date_str: String = match env.get_string(&purchase_date) {
        Ok(s) => s.into(),
        Err(_) => return 0.0,
    };
    CostCalculator::calculate_daily_cost(purchase_price_cents, &date_str)
}

#[no_mangle]
pub extern "system" fn Java_com_apislens_rust_RustCore_nativeDaysSince(
    mut env: JNIEnv,
    _class: JClass,
    purchase_date: JString,
) -> jlong {
    let date_str: String = match env.get_string(&purchase_date) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    CostCalculator::days_since(&date_str)
}

#[no_mangle]
pub extern "system" fn Java_com_apislens_rust_RustCore_nativeTotalDepreciation(
    mut env: JNIEnv,
    _class: JClass,
    purchase_price_cents: jlong,
    purchase_date: JString,
) -> jdouble {
    let date_str: String = match env.get_string(&purchase_date) {
        Ok(s) => s.into(),
        Err(_) => return 0.0,
    };
    CostCalculator::total_depreciation(purchase_price_cents, &date_str)
}

#[no_mangle]
pub extern "system" fn Java_com_apislens_rust_RustCore_nativeNeedsChargeReminder(
    mut _env: JNIEnv,
    _class: JClass,
    last_charge_time: jlong,
) -> bool {
    let time = if last_charge_time == 0 {
        None
    } else {
        Some(last_charge_time)
    };
    CostCalculator::needs_charge_reminder(time)
}

#[no_mangle]
pub extern "system" fn Java_com_apislens_rust_RustCore_nativeGetVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jni::sys::jstring {
    let version = env.new_string("0.1.0").unwrap_or_else(|_| env.new_string("").unwrap());
    version.into_raw()
}
