use crate::models::{ChargeRecord, DashboardStats, Device};
use chrono::NaiveDate;
use std::time::{SystemTime, UNIX_EPOCH};

pub struct CostCalculator;

impl CostCalculator {
    pub fn calculate_daily_cost(purchase_price_cents: i64, purchase_date: &str) -> f64 {
        let price = purchase_price_cents as f64 / 100.0;
        let days = Self::days_since(purchase_date).max(1);
        price / days as f64
    }

    pub fn days_since(purchase_date: &str) -> i64 {
        match NaiveDate::parse_from_str(purchase_date, "%Y-%m-%d") {
            Ok(start) => {
                let today = chrono::Local::now().date_naive();
                (today - start).num_days().max(0)
            }
            Err(_) => 0,
        }
    }

    pub fn total_depreciation(purchase_price_cents: i64, purchase_date: &str) -> f64 {
        let price = purchase_price_cents as f64 / 100.0;
        let days = Self::days_since(purchase_date).max(1);
        let lifetime: i64 = 1095;
        let depreciation_per_day = price / lifetime as f64;
        (depreciation_per_day * days as f64).min(price)
    }

    pub fn needs_charge_reminder(last_charge_time: Option<i64>) -> bool {
        match last_charge_time {
            None => true,
            Some(t) => {
                let now = SystemTime::now()
                    .duration_since(UNIX_EPOCH)
                    .unwrap_or_default()
                    .as_millis() as i64;
                let days_since = (now - t) / 86_400_000;
                days_since >= 7
            }
        }
    }

    pub fn calculate_battery_health(records: &[ChargeRecord]) -> Vec<(String, f64)> {
        if records.is_empty() {
            return vec![];
        }

        let mut results = Vec::with_capacity(records.len());
        let mut _total_cycles: f64 = 0.0;
        let mut full_charge_count: i32 = 0;
        let window_size = 5;
        let mut max_levels: std::collections::VecDeque<f64> = std::collections::VecDeque::with_capacity(window_size);

        for (index, record) in records.iter().enumerate() {
            let end_lvl = record.end_level.unwrap_or(record.start_level) as f64;
            let charge_gained = end_lvl - record.start_level as f64;

            if charge_gained > 0.0 {
                _total_cycles += charge_gained / 100.0;
            }
            if end_lvl >= 95.0 {
                full_charge_count += 1;
            }

            max_levels.push_back(end_lvl);
            if max_levels.len() > window_size {
                max_levels.pop_front();
            }

            let full_charge_ratio = if index + 1 > 0 {
                full_charge_count as f64 / (index + 1) as f64
            } else {
                1.0
            };
            let recent_max_avg: f64 = max_levels.iter().sum::<f64>() / max_levels.len() as f64;
            let health_score = (full_charge_ratio * 40.0) + (recent_max_avg * 0.6);
            let clamped_health = health_score.clamp(0.0, 100.0);

            let date_str = chrono::DateTime::from_timestamp_millis(record.start_time)
                .map(|dt| dt.format("%m/%d").to_string())
                .unwrap_or_else(|| "?".to_string());

            results.push((date_str, clamped_health));
        }

        results
    }

    pub fn compute_dashboard_stats(devices: &[Device], charge_records: &[ChargeRecord]) -> DashboardStats {
        let total_devices = devices.len() as i32;
        let total_daily_cost: f64 = devices
            .iter()
            .map(|d| Self::calculate_daily_cost(d.purchase_price_cents, &d.purchase_date))
            .sum();
        let total_purchase_cost: f64 = devices.iter().map(|d| d.purchase_price()).sum();
        let average_daily_cost = if total_devices > 0 {
            total_daily_cost / total_devices as f64
        } else {
            0.0
        };

        let latest_charged_device_id = charge_records
            .iter()
            .max_by_key(|r| r.start_time)
            .map(|r| r.device_id);

        let longest_uncharged_device_id = if !devices.is_empty() {
            devices
                .iter()
                .filter(|d| d.category != "")
                .min_by_key(|d| {
                    charge_records
                        .iter()
                        .filter(|r| r.device_id == d.id)
                        .map(|r| r.start_time)
                        .max()
                        .unwrap_or(0)
                })
                .or_else(|| devices.iter().min_by_key(|d| Self::days_since(&d.purchase_date)))
                .map(|d| d.id)
        } else {
            None
        };

        let longest_uncharged_days = longest_uncharged_device_id
            .and_then(|id| devices.iter().find(|d| d.id == id))
            .map(|d| Self::days_since(&d.purchase_date))
            .unwrap_or(0);

        DashboardStats {
            total_devices,
            total_daily_cost,
            total_purchase_cost,
            average_daily_cost,
            latest_charged_device_id,
            longest_uncharged_device_id,
            longest_uncharged_days,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_calculate_daily_cost() {
        let cost = CostCalculator::calculate_daily_cost(100_000, "2024-01-01");
        assert!(cost > 0.0);
    }

    #[test]
    fn test_days_since() {
        let days = CostCalculator::days_since("2024-01-01");
        assert!(days > 0);
    }

    #[test]
    fn test_total_depreciation() {
        let dep = CostCalculator::total_depreciation(100_000, "2024-01-01");
        assert!(dep > 0.0);
        assert!(dep <= 1000.0);
    }

    #[test]
    fn test_needs_charge_reminder() {
        assert!(CostCalculator::needs_charge_reminder(None));
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_millis() as i64;
        assert!(!CostCalculator::needs_charge_reminder(Some(now)));
    }
}
