-- =============================================================================
-- PriceHawk AI — Seed Data (Dev / Demo)
-- Run against: user_db  AND  catalog_db
-- Usage: psql -U pricehawk -h localhost -f seed-data.sql
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- user_db
-- ─────────────────────────────────────────────────────────────────────────────
\c user_db

-- Test accounts (password = "Test123456" — bcrypt hash)
-- user: test@pricehawk.vn   / Test123456
-- admin: admin@pricehawk.vn / Test123456
INSERT INTO users (id, email, password_hash, auth_provider, full_name, subscription_plan, is_active)
VALUES
  ('11111111-0000-0000-0000-000000000001',
   'test@pricehawk.vn',
   '$2a$12$iWzQiSfOmfYjRhz3wXFKIOo3NNEHBKwt9n.yUKpHv7ZOJfwqW4.hi',
   'LOCAL', 'Nguyễn Test', 'FREE', TRUE),
  ('11111111-0000-0000-0000-000000000002',
   'premium@pricehawk.vn',
   '$2a$12$iWzQiSfOmfYjRhz3wXFKIOo3NNEHBKwt9n.yUKpHv7ZOJfwqW4.hi',
   'LOCAL', 'Trần Premium', 'PREMIUM_USER', TRUE),
  ('11111111-0000-0000-0000-000000000003',
   'admin@pricehawk.vn',
   '$2a$12$iWzQiSfOmfYjRhz3wXFKIOo3NNEHBKwt9n.yUKpHv7ZOJfwqW4.hi',
   'LOCAL', 'Admin PriceHawk', 'PREMIUM_USER', TRUE)
ON CONFLICT (email) DO NOTHING;

-- Assign roles
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'test@pricehawk.vn' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'premium@pricehawk.vn' AND r.name IN ('USER','PREMIUM_USER')
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin@pricehawk.vn' AND r.name IN ('USER','ADMIN')
ON CONFLICT DO NOTHING;

-- Wishlists
INSERT INTO wishlists (id, user_id, name)
VALUES
  ('22222222-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', 'Wishlist của tôi'),
  ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000002', 'Sắm Tết')
ON CONFLICT DO NOTHING;

-- Wishlist items (will reference catalog products)
INSERT INTO wishlist_items (wishlist_id, product_id, target_price)
VALUES
  ('22222222-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 25000000),
  ('22222222-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000002', 18000000)
ON CONFLICT DO NOTHING;

-- Search history
INSERT INTO search_histories (user_id, query_type, query_value, result_count)
VALUES
  ('11111111-0000-0000-0000-000000000001', 'TEXT', 'iPhone 15 Pro Max', 12),
  ('11111111-0000-0000-0000-000000000001', 'URL', 'https://shopee.vn/product/123456', 1),
  ('11111111-0000-0000-0000-000000000001', 'TEXT', 'Samsung Galaxy S24 Ultra', 8),
  ('11111111-0000-0000-0000-000000000002', 'TEXT', 'MacBook Pro M3', 5),
  ('11111111-0000-0000-0000-000000000002', 'TEXT', 'AirPods Pro 2', 15)
ON CONFLICT DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- catalog_db
-- ─────────────────────────────────────────────────────────────────────────────
\c catalog_db

-- Sub-categories
INSERT INTO categories (id, name, slug, parent_id, level, sort_order) VALUES
  ('cccccccc-0000-0000-0000-000000000010', 'Điện thoại', 'dien-thoai',
   (SELECT id FROM categories WHERE slug = 'dien-thoai-may-tinh-bang'), 1, 1),
  ('cccccccc-0000-0000-0000-000000000011', 'Máy tính bảng', 'may-tinh-bang',
   (SELECT id FROM categories WHERE slug = 'dien-thoai-may-tinh-bang'), 1, 2),
  ('cccccccc-0000-0000-0000-000000000012', 'Laptop', 'laptop',
   (SELECT id FROM categories WHERE slug = 'laptop-may-tinh'), 1, 1),
  ('cccccccc-0000-0000-0000-000000000013', 'Tai nghe', 'tai-nghe',
   (SELECT id FROM categories WHERE slug = 'thiet-bi-am-thanh'), 1, 1),
  ('cccccccc-0000-0000-0000-000000000014', 'Loa Bluetooth', 'loa-bluetooth',
   (SELECT id FROM categories WHERE slug = 'thiet-bi-am-thanh'), 1, 2)
ON CONFLICT (slug) DO NOTHING;

-- ─── Products ────────────────────────────────────────────────────────────────
INSERT INTO products (id, name, slug, brand, description, thumbnail_url, category_id,
                      ai_summary, sentiment_score, total_reviews, real_review_ratio,
                      lowest_price, lowest_price_seller, lowest_price_source)
VALUES

-- iPhone 15 Pro Max
('aaaaaaaa-0000-0000-0000-000000000001',
 'Apple iPhone 15 Pro Max 256GB',
 'apple-iphone-15-pro-max-256gb',
 'Apple',
 'iPhone 15 Pro Max với chip A17 Pro, camera 48MP, titanium frame và màn hình Super Retina XDR 6.7 inch.',
 'https://cdn.tgdd.vn/Products/Images/42/299033/iphone-15-pro-max-blue-600x600.jpg',
 'cccccccc-0000-0000-0000-000000000010',
 'Sản phẩm đáng tin cậy cao với 87% review thật. Hiệu năng xuất sắc, camera tốt nhất phân khúc. Một số user phàn nàn giá cao và pin trung bình.',
 0.82, 1247, 0.87, 29990000, 'Thế Giới Di Động', 'tgdd.vn'),

-- Samsung Galaxy S24 Ultra
('aaaaaaaa-0000-0000-0000-000000000002',
 'Samsung Galaxy S24 Ultra 256GB',
 'samsung-galaxy-s24-ultra-256gb',
 'Samsung',
 'Galaxy S24 Ultra với S Pen, camera 200MP, chip Snapdragon 8 Gen 3 và màn hình Dynamic AMOLED 6.8 inch.',
 'https://cdn.tgdd.vn/Products/Images/42/321160/samsung-galaxy-s24-ultra-xam-600x600.jpg',
 'cccccccc-0000-0000-0000-000000000010',
 'Điện thoại Android cao cấp nhất với 79% review thật. S Pen được cải thiện đáng kể. Camera zoom 100x ấn tượng. Pin lớn nhưng sạc chậm hơn đối thủ.',
 0.75, 892, 0.79, 27990000, 'Shopee Mall', 'shopee.vn'),

-- MacBook Pro M3
('aaaaaaaa-0000-0000-0000-000000000003',
 'Apple MacBook Pro 14 inch M3 Pro 18GB RAM 512GB',
 'apple-macbook-pro-14-m3-pro-18gb-512gb',
 'Apple',
 'MacBook Pro 14 inch với chip M3 Pro, RAM 18GB Unified, SSD 512GB, màn hình Liquid Retina XDR.',
 'https://cdn.tgdd.vn/Products/Images/44/325072/macbook-pro-14-m3-pro-space-gray-600x600.jpg',
 'cccccccc-0000-0000-0000-000000000012',
 'Laptop tốt nhất phân khúc với 92% review thật. Hiệu năng M3 Pro vượt trội, màn hình đẹp, thời lượng pin ấn tượng. Chỉ thiếu kết nối HDMI full-size.',
 0.91, 456, 0.92, 52990000, 'FPT Shop', 'fptshop.com.vn'),

-- AirPods Pro 2
('aaaaaaaa-0000-0000-0000-000000000004',
 'Apple AirPods Pro Gen 2 với MagSafe USB-C',
 'apple-airpods-pro-gen-2-magsafe-usb-c',
 'Apple',
 'AirPods Pro Gen 2 với chip H2, chống ồn chủ động H2, âm thanh không gian và hộp sạc MagSafe cổng USB-C.',
 'https://cdn.tgdd.vn/Products/Images/54/313090/airpods-pro-2-usb-c-600x600.jpg',
 'cccccccc-0000-0000-0000-000000000013',
 'Tai nghe true wireless tốt nhất 2024 với 84% review thật. ANC cải thiện 2x so với gen 1. Âm thanh chất lượng cao. Một số user gặp vấn đề kết nối Bluetooth.',
 0.84, 678, 0.84, 6490000, 'Lazada', 'lazada.vn'),

-- Sony WH-1000XM5
('aaaaaaaa-0000-0000-0000-000000000005',
 'Sony WH-1000XM5 Wireless Noise Canceling Headphones',
 'sony-wh-1000xm5-wireless-noise-canceling',
 'Sony',
 'Tai nghe chụp tai Sony WH-1000XM5 với chống ồn chủ động hàng đầu, 30 giờ pin, codec LDAC hi-res.',
 'https://www.sony.com.vn/image/5d02da5df552836db894cead06d09875?fmt=pjpeg&wid=330&bgcolor=FFFFFF&bgc=FFFFFF',
 'cccccccc-0000-0000-0000-000000000013',
 'Tai nghe over-ear chống ồn tốt nhất với 88% review thật. ANC class-leading, âm thanh chi tiết. Không có jack 3.5mm khi pin hết gây bất tiện.',
 0.88, 534, 0.88, 7990000, 'Tiki', 'tiki.vn'),

-- iPad Pro M4
('aaaaaaaa-0000-0000-0000-000000000006',
 'Apple iPad Pro 11 inch M4 WiFi 256GB',
 'apple-ipad-pro-11-m4-wifi-256gb',
 'Apple',
 'iPad Pro 11 inch mỏng nhất từ trước tới nay với chip M4, màn hình Ultra Retina XDR OLED, hỗ trợ Apple Pencil Pro.',
 'https://cdn.tgdd.vn/Products/Images/522/332536/ipad-pro-11-m4-silver-600x600.jpg',
 'cccccccc-0000-0000-0000-000000000011',
 'Máy tính bảng cao cấp nhất với 89% review thật. Màn hình OLED tuyệt đẹp, hiệu năng M4 mạnh mẽ. Giá cao, accessories đắt tiền.',
 0.89, 321, 0.89, 23990000, 'CellphoneS', 'cellphones.com.vn'),

-- Samsung Galaxy Tab S9
('aaaaaaaa-0000-0000-0000-000000000007',
 'Samsung Galaxy Tab S9 WiFi 128GB',
 'samsung-galaxy-tab-s9-wifi-128gb',
 'Samsung',
 'Galaxy Tab S9 với S Pen đi kèm, màn hình AMOLED 11 inch, chip Snapdragon 8 Gen 2, IP68.',
 'https://cdn.tgdd.vn/Products/Images/522/313764/samsung-galaxy-tab-s9-begie-600x600.jpg',
 'cccccccc-0000-0000-0000-000000000011',
 'Máy tính bảng Android tốt nhất với 76% review thật. S Pen đi kèm là điểm cộng lớn. DeX mode hữu ích cho productivity.',
 0.76, 287, 0.76, 17990000, 'Thế Giới Di Động', 'tgdd.vn')

ON CONFLICT (slug) DO NOTHING;

-- ─── Product Specs ────────────────────────────────────────────────────────────
INSERT INTO product_specs (product_id, specs) VALUES
('aaaaaaaa-0000-0000-0000-000000000001', '{
  "chipset": "Apple A17 Pro",
  "ram": "8GB",
  "storage": "256GB",
  "display": "6.7 inch Super Retina XDR OLED",
  "mainCamera": "48MP + 12MP + 12MP",
  "frontCamera": "12MP TrueDepth",
  "battery": "4422 mAh",
  "os": "iOS 17",
  "connectivity": "5G, WiFi 6E, Bluetooth 5.3, NFC",
  "dimensions": "159.9 x 76.7 x 8.25 mm",
  "weight": "221g",
  "color": "Titanium Trắng / Đen / Titan Tự Nhiên / Xanh"
}'),
('aaaaaaaa-0000-0000-0000-000000000002', '{
  "chipset": "Snapdragon 8 Gen 3",
  "ram": "12GB",
  "storage": "256GB",
  "display": "6.8 inch Dynamic AMOLED 2X 120Hz",
  "mainCamera": "200MP + 10MP + 50MP + 12MP",
  "frontCamera": "12MP",
  "battery": "5000 mAh",
  "os": "Android 14 / One UI 6.1",
  "connectivity": "5G, WiFi 7, Bluetooth 5.3, NFC, UWB",
  "dimensions": "162.3 x 79 x 8.6 mm",
  "weight": "232g",
  "sPen": "Có, Air Actions"
}'),
('aaaaaaaa-0000-0000-0000-000000000003', '{
  "chipset": "Apple M3 Pro 12-core",
  "ram": "18GB Unified Memory",
  "storage": "512GB SSD",
  "display": "14.2 inch Liquid Retina XDR 120Hz",
  "ports": "3x Thunderbolt 4, HDMI, SD card, MagSafe 3",
  "battery": "Up to 18 hours",
  "os": "macOS Sonoma",
  "weight": "1.61 kg",
  "dimensions": "31.26 x 22.12 x 1.55 cm"
}'),
('aaaaaaaa-0000-0000-0000-000000000004', '{
  "chip": "Apple H2",
  "anc": "Chống ồn chủ động H2 (2x gen trước)",
  "battery": "6h (30h với hộp sạc)",
  "waterResistance": "IPX4",
  "charging": "MagSafe, Qi2, USB-C",
  "connectivity": "Bluetooth 5.3",
  "codecs": "AAC, SBC",
  "features": "Adaptive Audio, Conversation Awareness, Personalized Spatial Audio"
}'),
('aaaaaaaa-0000-0000-0000-000000000005', '{
  "driverSize": "30mm",
  "frequencyResponse": "4Hz-40kHz",
  "battery": "30h (anc on), 40h (anc off)",
  "charging": "USB-C, 3 min = 3h",
  "connectivity": "Bluetooth 5.2, 3.5mm jack (charging required)",
  "codecs": "LDAC, LHDC, AAC, SBC",
  "weight": "250g",
  "anc": "Industry-leading noise cancellation"
}')
ON CONFLICT (product_id) DO NOTHING;

-- ─── Seller Listings ──────────────────────────────────────────────────────────
INSERT INTO seller_listings (id, product_id, domain, source_type, scraper_tier,
  seller_name, is_official_store, external_url, current_price, original_price,
  currency, promotion_info, trust_score, review_count, average_rating,
  fake_review_ratio, scrape_status, platform, is_available)
VALUES

-- iPhone 15 Pro Max listings
('bbbbbbbb-1001-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001',
 'tgdd.vn', 'RETAILER', 'CONFIG_BASED', 'Thế Giới Di Động', TRUE,
 'https://www.thegioididong.com/tin-tuc/iphone-15-pro-max', 29990000, 34990000,
 'VND', 'Trả góp 0% lãi suất 24 tháng', 0.91, 412, 4.8, 0.08, 'COMPLETED', 'TGDD', TRUE),

('bbbbbbbb-1001-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001',
 'fptshop.com.vn', 'RETAILER', 'CONFIG_BASED', 'FPT Shop', TRUE,
 'https://fptshop.com.vn/dien-thoai/apple/iphone-15-pro-max', 30490000, 34990000,
 'VND', 'Tặng gói bảo hành mở rộng 12 tháng', 0.87, 287, 4.7, 0.10, 'COMPLETED', 'FPT_SHOP', TRUE),

('bbbbbbbb-1001-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001',
 'shopee.vn', 'MARKETPLACE', 'API_BASED', 'Apple Authorised Reseller', TRUE,
 'https://shopee.vn/product/apple-iphone-15-pro-max-256gb', 30200000, 34990000,
 'VND', 'Mã SHOPEE10 giảm thêm 500k', 0.78, 1156, 4.6, 0.14, 'COMPLETED', 'SHOPEE', TRUE),

('bbbbbbbb-1001-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000001',
 'lazada.vn', 'MARKETPLACE', 'API_BASED', 'iShop Official', TRUE,
 'https://www.lazada.vn/products/iphone-15-pro-max-256gb', 31000000, 34990000,
 'VND', NULL, 0.72, 523, 4.5, 0.17, 'COMPLETED', 'LAZADA', TRUE),

-- Samsung Galaxy S24 Ultra listings
('bbbbbbbb-1002-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000002',
 'shopee.vn', 'MARKETPLACE', 'API_BASED', 'Samsung Official Store', TRUE,
 'https://shopee.vn/product/samsung-s24-ultra', 27990000, 31990000,
 'VND', 'Flash sale cuối tuần', 0.83, 654, 4.7, 0.11, 'COMPLETED', 'SHOPEE', TRUE),

('bbbbbbbb-1002-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000002',
 'tgdd.vn', 'RETAILER', 'CONFIG_BASED', 'Thế Giới Di Động', TRUE,
 'https://www.thegioididong.com/dien-thoai/samsung-galaxy-s24-ultra', 28490000, 31990000,
 'VND', 'Tặng ốp lưng chính hãng 500k', 0.88, 321, 4.6, 0.09, 'COMPLETED', 'TGDD', TRUE),

('bbbbbbbb-1002-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000002',
 'cellphones.com.vn', 'RETAILER', 'CONFIG_BASED', 'CellphoneS', FALSE,
 'https://cellphones.com.vn/samsung-galaxy-s24-ultra.html', 28200000, 31990000,
 'VND', 'Trả góp từ 1.17 triệu/tháng', 0.81, 198, 4.5, 0.12, 'COMPLETED', 'CELLPHONES', TRUE),

-- MacBook Pro M3 listings
('bbbbbbbb-1003-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000003',
 'fptshop.com.vn', 'RETAILER', 'CONFIG_BASED', 'FPT Shop', TRUE,
 'https://fptshop.com.vn/may-tinh-xach-tay/apple/macbook-pro-14-m3-pro', 52990000, 57990000,
 'VND', 'Trả góp 0% 36 tháng', 0.94, 187, 4.9, 0.06, 'COMPLETED', 'FPT_SHOP', TRUE),

('bbbbbbbb-1003-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000003',
 'tgdd.vn', 'RETAILER', 'CONFIG_BASED', 'Thế Giới Di Động', TRUE,
 'https://www.thegioididong.com/laptop/apple-macbook-pro-14-m3-pro', 53490000, 57990000,
 'VND', NULL, 0.90, 143, 4.8, 0.07, 'COMPLETED', 'TGDD', TRUE),

('bbbbbbbb-1003-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000003',
 'shopee.vn', 'MARKETPLACE', 'API_BASED', 'Mac Store Official', FALSE,
 'https://shopee.vn/product/macbook-pro-m3-pro', 54000000, 57990000,
 'VND', 'Voucher 1 triệu cho đơn đầu', 0.75, 89, 4.7, 0.16, 'COMPLETED', 'SHOPEE', TRUE),

-- AirPods Pro 2 listings
('bbbbbbbb-1004-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000004',
 'lazada.vn', 'MARKETPLACE', 'API_BASED', 'Apple Official Store VN', TRUE,
 'https://www.lazada.vn/products/airpods-pro-2-usb-c', 6490000, 6990000,
 'VND', 'Freeship Extra', 0.86, 234, 4.7, 0.09, 'COMPLETED', 'LAZADA', TRUE),

('bbbbbbbb-1004-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000004',
 'tiki.vn', 'MARKETPLACE', 'API_BASED', 'TikiNOW Smart Logistics', TRUE,
 'https://tiki.vn/airpods-pro-gen-2-usb-c', 6690000, 6990000,
 'VND', 'Tặng kèm túi đựng Apple', 0.89, 312, 4.8, 0.08, 'COMPLETED', 'TIKI', TRUE),

-- Sony WH-1000XM5 listings
('bbbbbbbb-1005-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000005',
 'tiki.vn', 'MARKETPLACE', 'API_BASED', 'Sony Official', TRUE,
 'https://tiki.vn/sony-wh1000xm5', 7990000, 8990000,
 'VND', NULL, 0.91, 198, 4.8, 0.07, 'COMPLETED', 'TIKI', TRUE),

('bbbbbbbb-1005-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000005',
 'shopee.vn', 'MARKETPLACE', 'API_BASED', 'Sony Vietnam', TRUE,
 'https://shopee.vn/product/sony-wh1000xm5', 8200000, 8990000,
 'VND', 'Flash Deal -8%', 0.82, 445, 4.6, 0.12, 'COMPLETED', 'SHOPEE', TRUE)

ON CONFLICT DO NOTHING;

-- ─── Price History (30 ngày qua) ──────────────────────────────────────────────

-- iPhone 15 Pro Max / TGDĐ — giá giảm dần
INSERT INTO price_history (seller_listing_id, price, recorded_at) VALUES
('bbbbbbbb-1001-0000-0000-000000000001', 34990000, NOW() - INTERVAL '30 days'),
('bbbbbbbb-1001-0000-0000-000000000001', 33990000, NOW() - INTERVAL '25 days'),
('bbbbbbbb-1001-0000-0000-000000000001', 32500000, NOW() - INTERVAL '20 days'),
('bbbbbbbb-1001-0000-0000-000000000001', 31990000, NOW() - INTERVAL '15 days'),
('bbbbbbbb-1001-0000-0000-000000000001', 30990000, NOW() - INTERVAL '10 days'),
('bbbbbbbb-1001-0000-0000-000000000001', 30490000, NOW() - INTERVAL '5 days'),
('bbbbbbbb-1001-0000-0000-000000000001', 29990000, NOW() - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

-- iPhone 15 Pro Max / Shopee
INSERT INTO price_history (seller_listing_id, price, recorded_at) VALUES
('bbbbbbbb-1001-0000-0000-000000000003', 34990000, NOW() - INTERVAL '30 days'),
('bbbbbbbb-1001-0000-0000-000000000003', 33500000, NOW() - INTERVAL '22 days'),
('bbbbbbbb-1001-0000-0000-000000000003', 32000000, NOW() - INTERVAL '15 days'),
('bbbbbbbb-1001-0000-0000-000000000003', 31000000, NOW() - INTERVAL '8 days'),
('bbbbbbbb-1001-0000-0000-000000000003', 30200000, NOW() - INTERVAL '2 days')
ON CONFLICT DO NOTHING;

-- Samsung S24 Ultra / Shopee
INSERT INTO price_history (seller_listing_id, price, recorded_at) VALUES
('bbbbbbbb-1002-0000-0000-000000000001', 31990000, NOW() - INTERVAL '30 days'),
('bbbbbbbb-1002-0000-0000-000000000001', 30500000, NOW() - INTERVAL '20 days'),
('bbbbbbbb-1002-0000-0000-000000000001', 29500000, NOW() - INTERVAL '12 days'),
('bbbbbbbb-1002-0000-0000-000000000001', 28500000, NOW() - INTERVAL '5 days'),
('bbbbbbbb-1002-0000-0000-000000000001', 27990000, NOW() - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

-- MacBook Pro M3 / FPT
INSERT INTO price_history (seller_listing_id, price, recorded_at) VALUES
('bbbbbbbb-1003-0000-0000-000000000001', 57990000, NOW() - INTERVAL '30 days'),
('bbbbbbbb-1003-0000-0000-000000000001', 56000000, NOW() - INTERVAL '20 days'),
('bbbbbbbb-1003-0000-0000-000000000001', 54500000, NOW() - INTERVAL '10 days'),
('bbbbbbbb-1003-0000-0000-000000000001', 52990000, NOW() - INTERVAL '2 days')
ON CONFLICT DO NOTHING;

-- AirPods Pro 2 / Tiki
INSERT INTO price_history (seller_listing_id, price, recorded_at) VALUES
('bbbbbbbb-1004-0000-0000-000000000002', 6990000, NOW() - INTERVAL '30 days'),
('bbbbbbbb-1004-0000-0000-000000000002', 6790000, NOW() - INTERVAL '18 days'),
('bbbbbbbb-1004-0000-0000-000000000002', 6690000, NOW() - INTERVAL '7 days')
ON CONFLICT DO NOTHING;

-- ─── Reviews ──────────────────────────────────────────────────────────────────

-- iPhone 15 Pro Max reviews (TGDĐ)
INSERT INTO reviews (seller_listing_id, reviewer_name, rating, content, review_date,
                     sentiment, is_likely_fake, fake_reason, source_review_id)
VALUES
('bbbbbbbb-1001-0000-0000-000000000001', 'Minh Tuấn', 5,
 'Máy rất đẹp, camera chụp ban đêm cực kỳ ấn tượng. Dùng được 2 tuần rồi mà vẫn mê. Màn hình sáng, pin ổn hơn hẳn gen trước.',
 NOW() - INTERVAL '3 days', 'POSITIVE', FALSE, NULL, 'tgdd-rev-001'),
('bbbbbbbb-1001-0000-0000-000000000001', 'Thu Hà', 4,
 'Đẹp lắm nhưng hơi nặng so với máy cũ. Camera thực sự tốt, chụp ảnh trẻ em sắc nét không bị nhòe. Giá hơi cao nhưng chất lượng xứng đáng.',
 NOW() - INTERVAL '5 days', 'POSITIVE', FALSE, NULL, 'tgdd-rev-002'),
('bbbbbbbb-1001-0000-0000-000000000001', 'Quang Hùng', 3,
 'Máy ổn nhưng pin xuống khá nhanh khi dùng 5G. Có lẽ phải tắt bớt kết nối.',
 NOW() - INTERVAL '8 days', 'NEUTRAL', FALSE, NULL, 'tgdd-rev-003'),
('bbbbbbbb-1001-0000-0000-000000000001', 'Linh Chi', 5,
 'Mua tặng chồng nhân dịp sinh nhật. Anh ấy cực kỳ thích, dùng từ sáng đến tối không rời tay.',
 NOW() - INTERVAL '10 days', 'POSITIVE', FALSE, NULL, 'tgdd-rev-004'),
('bbbbbbbb-1001-0000-0000-000000000001', 'Fake Review Bot', 5,
 'Tốt lắm mua ngay đi.',
 NOW() - INTERVAL '1 day', 'POSITIVE', TRUE, 'SHORT_5STAR', 'tgdd-rev-005'),

-- Samsung S24 Ultra reviews (Shopee)
('bbbbbbbb-1002-0000-0000-000000000001', 'Đức Anh', 5,
 'S Pen cải thiện rất nhiều so với S23 Ultra. Viết rất mượt, AI Sketch to Image thú vị. Camera zoom 100x dùng chụp sân khấu concert rõ không tưởng.',
 NOW() - INTERVAL '4 days', 'POSITIVE', FALSE, NULL, 'shopee-s24-001'),
('bbbbbbbb-1002-0000-0000-000000000001', 'Vân Anh', 4,
 'Màn hình đẹp xuất sắc, nhưng sạc 45W hơi chậm so với Android khác đang làm 65-120W. Pin 5000mAh bù lại.',
 NOW() - INTERVAL '6 days', 'POSITIVE', FALSE, NULL, 'shopee-s24-002'),
('bbbbbbbb-1002-0000-0000-000000000001', 'Trọng Nghĩa', 3,
 'Giá hơi đắt cho một số tính năng chưa thực sự hoàn thiện. AI Circle to Search hay nhưng không phải lúc nào cũng chính xác.',
 NOW() - INTERVAL '9 days', 'NEUTRAL', FALSE, NULL, 'shopee-s24-003'),

-- MacBook Pro M3 reviews (FPT)
('bbbbbbbb-1003-0000-0000-000000000001', 'Thanh Tùng', 5,
 'Sau 2 tuần dùng thì mình có thể khẳng định đây là laptop tốt nhất mình từng dùng. Render video 4K nhanh gần gấp đôi máy cũ (i9 2019). Fan hầu như không quay. Pin 12 tiếng thực tế.',
 NOW() - INTERVAL '7 days', 'POSITIVE', FALSE, NULL, 'fpt-mbp-001'),
('bbbbbbbb-1003-0000-0000-000000000001', 'Phương Linh', 5,
 'Làm đồ họa, video thì đây là sự lựa chọn không thể tốt hơn ở tầm giá. Màn hình XDR cực đẹp, hỗ trợ ProMotion 120Hz.',
 NOW() - INTERVAL '12 days', 'POSITIVE', FALSE, NULL, 'fpt-mbp-002'),
('bbbbbbbb-1003-0000-0000-000000000001', 'Nam Khánh', 4,
 'Máy tốt nhưng ecosytem Apple khá đóng. Kết nối thiết bị Android phức tạp. Nếu đã dùng iPhone/iPad thì đây là lựa chọn số 1.',
 NOW() - INTERVAL '15 days', 'POSITIVE', FALSE, NULL, 'fpt-mbp-003'),

-- AirPods Pro 2 reviews (Tiki)
('bbbbbbbb-1004-0000-0000-000000000002', 'Hải Long', 5,
 'ANC tốt nhất tôi từng dùng trong phân khúc true wireless. Đeo suốt ngày làm việc, chống ồn văn phòng rất tốt. Âm thanh Spatial Audio ảo diệu.',
 NOW() - INTERVAL '3 days', 'POSITIVE', FALSE, NULL, 'tiki-app-001'),
('bbbbbbbb-1004-0000-0000-000000000002', 'Ngọc Mai', 4,
 'Tốt nhưng dây đeo hơi lỏng với tai nhỏ. May mắn có các size eartip khác nhau. USB-C rất tiện so với gen trước.',
 NOW() - INTERVAL '6 days', 'POSITIVE', FALSE, NULL, 'tiki-app-002'),

-- Sony WH-1000XM5 reviews (Tiki)
('bbbbbbbb-1005-0000-0000-000000000001', 'Bảo Châu', 5,
 'Tai nghe chụp tai hay nhất tôi từng đeo. ANC loại bỏ hoàn toàn tiếng ồn máy bay và văn phòng. LDAC với Tidal hi-fi thì không còn gì để chê.',
 NOW() - INTERVAL '5 days', 'POSITIVE', FALSE, NULL, 'tiki-sony-001'),
('bbbbbbbb-1005-0000-0000-000000000001', 'Kiên Trung', 4,
 'Mọi thứ đều tốt nhưng không có jack 3.5mm khi pin hết là điểm trừ lớn với mình. Thường xuyên đi máy bay mà cắm vào ghế thì không được.',
 NOW() - INTERVAL '8 days', 'NEGATIVE', FALSE, NULL, 'tiki-sony-002'),
('bbbbbbbb-1005-0000-0000-000000000001', 'Thúy Ngân', 5,
 'Mua về tặng bố làm nhân viên ngân hàng. Ông rất thích vì lọc tiếng ồn khi nói chuyện điện thoại cực kỳ rõ ràng.',
 NOW() - INTERVAL '11 days', 'POSITIVE', FALSE, NULL, 'tiki-sony-003')

ON CONFLICT (seller_listing_id, source_review_id) DO NOTHING;

-- ─── Update lowest_price trên products ────────────────────────────────────────
UPDATE products p SET
  lowest_price = sub.min_price,
  lowest_price_seller = sub.seller_name,
  lowest_price_source = sub.domain
FROM (
  SELECT DISTINCT ON (product_id)
    product_id, current_price AS min_price, seller_name, domain
  FROM seller_listings
  WHERE is_available = TRUE AND current_price IS NOT NULL
  ORDER BY product_id, current_price ASC
) sub
WHERE p.id = sub.product_id;

SELECT 'Seed data inserted successfully!' AS status;
SELECT 'Products: ' || COUNT(*) FROM products;
SELECT 'Seller Listings: ' || COUNT(*) FROM seller_listings;
SELECT 'Price History: ' || COUNT(*) FROM price_history;
SELECT 'Reviews: ' || COUNT(*) FROM reviews;
