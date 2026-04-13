use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Device {
    pub id: i64,
    pub name: String,
    pub model: String,
    pub icon_uri: Option<String>,
    pub category: String,
    pub purchase_date: String,
    pub purchase_price_cents: i64,
    pub note: String,
    pub created_at: i64,
    pub updated_at: i64,
}

impl Device {
    pub fn purchase_price(&self) -> f64 {
        self.purchase_price_cents as f64 / 100.0
    }

    pub const CATEGORIES: &'static [&'static str] = &[
        "手机", "平板", "笔记本", "智能手表", "耳机",
        "充电宝", "电池", "游戏机", "相机", "音箱", "其他",
    ];
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChargeRecord {
    pub id: i64,
    pub device_id: i64,
    pub start_time: i64,
    pub end_time: Option<i64>,
    pub start_level: i32,
    pub end_level: Option<i32>,
    pub duration_minutes: Option<i32>,
    pub note: String,
    pub created_at: i64,
}

impl ChargeRecord {
    pub fn charge_gained(&self) -> Option<i32> {
        self.end_level.map(|end| end - self.start_level)
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UsageRecord {
    pub id: i64,
    pub device_id: i64,
    pub date: String,
    pub usage_minutes: i32,
    pub note: String,
    pub created_at: i64,
    pub updated_at: i64,
}

impl UsageRecord {
    pub fn usage_hours(&self) -> f64 {
        self.usage_minutes as f64 / 60.0
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DashboardStats {
    pub total_devices: i32,
    pub total_daily_cost: f64,
    pub total_purchase_cost: f64,
    pub average_daily_cost: f64,
    pub latest_charged_device_id: Option<i64>,
    pub longest_uncharged_device_id: Option<i64>,
    pub longest_uncharged_days: i64,
}
