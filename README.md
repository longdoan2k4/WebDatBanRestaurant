# Restaurant Pro - Spring Boot + XAMPP MySQL + VNPAY

Chuc nang gom:
- Dang ky / dang nhap khach hang
- Dat ban theo so luong nguoi
- Chon mon truoc khi den
- Phan quyen 4 vai tro: CUSTOMER, TABLE_MANAGER, MENU_MANAGER, ADMIN
- Dashboard quan tri: ban an, menu, nguoi dung, booking
- Thanh toan booking mon dat truoc bang VNPAY Sandbox

## Cong nghe
- Java 21
- Spring Boot 3.2.6
- Spring Security
- Spring Data JPA
- Thymeleaf
- MySQL (XAMPP)
- CSS/JS thuan

## Cau hinh MySQL / XAMPP
Bat MySQL trong XAMPP, sau do kiem tra port dang dung la 3306 hay 3307.

Mac dinh file `src/main/resources/application.yml` dang dung:
- host: `127.0.0.1`
- port: `3306`
- database: `restaurant_pro_db`
- username: `root`
- password: rong

Neu may ban dang dung port khac, co the set bien moi truong:
```bash
DB_PORT=3307
```

## Cau hinh VNPAY Sandbox
Cap nhat trong `src/main/resources/application.yml`:
```yaml
payment:
  vnpay:
    enabled: true
    tmn-code: YOUR_TMN_CODE
    hash-secret: YOUR_HASH_SECRET
    pay-url: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
    return-url: http://localhost:8089/payment/vnpay/return
    order-type: other
    locale: vn
    expire-minutes: 15
```

Luu y:
- `tmn-code` va `hash-secret` la thong tin sandbox merchant do VNPAY cap.
- `return-url` la URL trinh duyet cua khach hang se quay ve sau khi thanh toan.
- `IPN URL` la endpoint `/payment/vnpay/ipn`. De VNPAY goi duoc tu ben ngoai, ban can public local server bang ngrok / Cloudflare Tunnel va dang ky URL public voi VNPAY.
- Ban van co the test local flow qua Return URL; code da cho phep cap nhat fallback khi giao dich dang o trang thai pending.

## Luong thanh toan da them
1. Khach chon ban va mon.
2. Chon `Thanh toan bang VNPAY` hoac `Thanh toan tai nha hang`.
3. He thong tao booking, tao transaction VNPAY va redirect sang cong thanh toan.
4. Sau khi thanh toan, VNPAY redirect ve `payment/vnpay/return`.
5. He thong doi chieu chu ky, cap nhat payment status va hien thi ket qua.
6. Trang `Booking cua toi` va trang admin deu hien thi trang thai thanh toan.

## Chay du an
```bash
mvn spring-boot:run
```

Hoac:
```bash
mvn clean package
java -jar target/restaurant-pro-1.0.0.jar
```

## Tai khoan demo
- Admin: `0988000001 / 123456`
- Quan ly ban: `0988000002 / 123456`
- Quan ly menu: `0988000003 / 123456`
- Khach hang: `0988000004 / 123456`

## Test nhanh VNPAY
1. Dang nhap bang tai khoan khach hang.
2. Tao booking co mon dat truoc.
3. Chon `Thanh toan bang VNPAY`.
4. Sau khi VNPAY redirect ve he thong, mo `Booking cua toi` de xem trang thai `Da thanh toan`.

## Ghi chu
- JPA de `ddl-auto: update`, nen bang se tu tao trong MySQL.
- He thong them bang `payment_transactions` de luu lich su giao dich VNPAY.
- Trang Return va IPN da duoc them trong SecurityConfig de VNPAY co the goi toi.
