# Event Ticketing Platform

**Business Rules Specification (BRS) | v1.0.0 | MVP 1.0**

## 1. Document control

| Attribute | Value |
| --- | --- |
| Product | Event Ticketing Platform |
| Document | 02-business-rules.md |
| Version | 1.0.0 |
| Updated | 19/11/2026 |
| Scope | 13 MVP modules |
| Source of truth | `01-product-requirements.md` — PRD v1.1.0 |
| Related document | `03-ui-ux-design.md` — UI/UX v1.0.0 |
| Status | Baseline PRD rules + proposed detailed policies for review |

### 1.1. Purpose and precedence

Tài liệu đặc tả điều kiện, ràng buộc, quyền thao tác, tính nhất quán dữ liệu, vòng đời và ngoại lệ cho các nghiệp vụ. Đối tượng sử dụng: Product Owner, Solution Designer, Backend/Frontend Engineer, QA và coding agent (Codex).

- **MUST / MUST NOT:** yêu cầu bắt buộc hoặc điều bị cấm đã có trong PRD, hoặc điều kiện cần thiết để bảo toàn yêu cầu đó.
- **PROPOSED:** quy tắc chi tiết được đề xuất để giải quyết điểm chưa chốt. Có thể dùng cho thiết kế/thử nghiệm nếu ghi rõ giả định, nhưng cần xác nhận trước khi coi là chính sách sản xuất.
- Nếu mâu thuẫn, PRD được ưu tiên. Không tự mở rộng MVP; ghi nhận discrepancy hoặc Change Request. UI/UX mô tả trải nghiệm, không cấp thêm quyền hay thay đổi nghiệp vụ.
- Giữ nguyên BR-01…BR-25 từ PRD. Các mã theo domain bên dưới là mã bổ trợ, không thay thế ID chức năng (AUTH-01, ORD-01, v.v.).
- Không quy định tên bảng, endpoint, loại lock, framework, database, message broker hoặc kiến trúc vật lý. Những quyết định đó thuộc System Design / Database Design / API Specification.

Local Phase 3 đã được chủ repository chấp thuận theo [decision 0002](decisions/0002-local-phase3-policy.md): giữ vé 10 phút, quy tắc thử lại/hủy đơn, tổng tiền không cộng phí và bằng chứng hoàn mô phỏng. Quyết định này chốt phạm vi local cho các mục PROPOSED tương ứng ở §8–9; không xác lập chính sách thanh toán production.

### 1.2. Scope

| Domain | In scope |
| --- | --- |
| IDN | Authentication & Authorization |
| ORG | Organizer Management |
| EVT | Event Management |
| INV | Ticket Type & Inventory Management |
| DSC | Event Discovery |
| ORD | Ticket Booking & Order Management |
| PAY | Online Payment Integration |
| ETK | QR Code E-Ticket |
| CHK | Check-in Management |
| FIN | Commission & Settlement |
| RPT | Reporting & Dashboard |
| ADM | Admin Portal |
| NTF | Email Notification |

Out of scope: Refund Management độc lập, customer self-service refund, automated refund, seat map, reserved seating, virtual queue, advanced anti-bot, promotion/coupon, early bird automation, group ticket, multiple sessions, transfer/resale, dynamic QR, offline check-in, waitlist, AI recommendation, social login, push/SMS, auto payout, mobile wallet, native app. Xử lý ngoại lệ và hoàn trả thủ công tiền đã thu vẫn là nghĩa vụ vận hành trong MVP, không phải module hoàn vé mới.

### 1.3. Glossary

| Term | Meaning |
| --- | --- |
| Organizer | Cá nhân/tổ chức đã đăng ký quản lý sự kiện; không đồng nghĩa mọi user có role ORGANIZER đều có quyền với mọi tổ chức. |
| Ticket Type | Hạng vé có giá, hạn mức, cửa sổ bán và giới hạn mua riêng. |
| Reservation | Quyền giữ một số lượng vé trong thời gian hữu hạn, chưa phải vé vào cửa. |
| Order | Đơn hàng thuộc một customer và một event, gồm các order item. |
| Payment attempt | Một lần khởi tạo thanh toán; một order có thể có nhiều attempt theo policy retry. |
| Payment transaction | Bản ghi sự kiện/giao dịch đã xác minh của gateway, phân biệt với order. |
| Ticket | Một quyền tham dự độc lập được phát hành cho một đơn vị vé đã xác nhận. |
| Confirmed allocation | Số vé được cam kết cho order CONFIRMED, kể cả chưa phát hành QR. |
| Payment exception | Khoản tiền đã thu hoặc giao dịch có bất thường cần điều tra/ghi nhận hoàn trả thủ công. |
| Commission snapshot | Cấu hình phí và kết quả tính phí được đóng băng cho giao dịch. |
| Settlement | Yêu cầu đối soát khoản tiền được phép chi trả cho Organizer. |
| Idempotency | Lặp lại cùng thao tác logic không tạo thêm hiệu ứng nghiệp vụ. |
| Authoritative state | Trạng thái do backend xác nhận, không lấy từ timer/client cache hay callback trên trình duyệt. |

## 2. Baseline business rules (PRD §8)

| Rule ID | Mandatory rule | Detailed section |
| --- | --- | --- |
| BR-01 | Chỉ Organizer được xác minh mới có thể gửi sự kiện để kiểm duyệt. | §4–5 |
| BR-02 | Chỉ sự kiện đã được phê duyệt mới được công khai. | §5 |
| BR-03 | Chỉ bán vé trong thời gian mở bán. | §6–8 |
| BR-04 | Tổng số vé phát hành không vượt sức chứa sự kiện. | §5–6 |
| BR-05 | Không được overselling. | §6–9 |
| BR-06 | Reservation phải có thời gian hết hạn. | §8 |
| BR-07 | Tự động giải phóng vé hết hạn giữ chỗ. | §8 |
| BR-08 | Không phát hành vé cho đơn hàng chưa xác nhận. | §8–10 |
| BR-09 | Payment Webhook phải được xử lý idempotent. | §9 |
| BR-10 | Mỗi vé có mã định danh duy nhất. | §10 |
| BR-11 | Mỗi vé chỉ được check-in thành công một lần. | §11 |
| BR-12 | Vé đã vô hiệu hóa không được sử dụng. | §10–11 |
| BR-13 | Không xóa vật lý sự kiện đã phát sinh giao dịch. | §5 |
| BR-14 | Thay đổi giá không ảnh hưởng đơn hàng đã tạo. | §6–9 |
| BR-15 | Thay đổi phí không ảnh hưởng snapshot của giao dịch đang có. | §12 |
| BR-16 | Chỉ đối soát khi sự kiện đã kết thúc và không còn nghĩa vụ tài chính chưa xử lý. | §12 |
| BR-17 | Các thao tác tài chính phải có Audit Log. | §9, §12, §14 |
| BR-18 | Organizer không được truy cập dữ liệu của tổ chức khác. | §3–4 |
| BR-19 | Vé miễn phí không yêu cầu thanh toán qua Payment Gateway. | §8–9 |
| BR-20 | Sự kiện bị hủy phải dừng bán vé và dừng check-in. | §5, §9–11 |
| BR-21 | Không được giảm hạn mức vé xuống dưới tổng số vé đã bán và đang giữ chỗ. | §6 |
| BR-22 | Không được phát hành trùng vé khi hệ thống retry. | §10 |
| BR-23 | Không được thay đổi trạng thái thanh toán thành công bởi callback thất bại đến muộn. | §9 |
| BR-24 | Không được đối soát cùng một khoản doanh thu nhiều lần. | §12 |
| BR-25 | Không lưu thông tin thẻ thanh toán nhạy cảm trên hệ thống. | §9, §14 |

## 3. Authentication & Authorization

### 3.1. Rule catalogue

| ID | Rule | Level |
| --- | --- | --- |
| IDN-R01 | Guest chỉ truy cập nội dung công khai; phải đăng nhập trước khi reserve hoặc quản lý dữ liệu riêng tư. | MUST |
| IDN-R02 | Backend kiểm tra identity, role, resource ownership/membership và trạng thái tài khoản cho mọi request riêng tư. | MUST |
| IDN-R03 | Một user có thể có nhiều role; không tự cấp quyền Organizer/Staff/Admin từ request của client. | MUST |
| IDN-R04 | Tài khoản bị khóa không thực hiện thao tác riêng tư; token còn hạn không được vượt qua khóa. Không làm gián đoạn xử lý callback tài chính đã phát sinh. | MUST |
| IDN-R05 | Email được xác minh bằng token có hạn, dùng một lần; reset password không tiết lộ tài khoản có tồn tại hay không. | PROPOSED |
| IDN-R06 | Email duy nhất sau chuẩn hóa; email verified là điều kiện reserve, onboarding Organizer và truy cập tác vụ Staff. | PROPOSED |
| IDN-R07 | Logout/reset/lock thu hồi phiên theo security policy; refresh token không dùng để vượt qua role/membership bị thu hồi. | PROPOSED |
| IDN-R08 | Mật khẩu không lưu dạng rõ, token bí mật không hiển thị trong log; chống brute force và enumeration. | MUST |

### 3.2. Authorization matrix

Ký hiệu ✓: được phép trong phạm vi; —: không được phép với vai trò đó. User đa vai trò có thể sử dụng workspace tương ứng, không kế thừa quyền tổ chức khác.

| Action | Guest | Customer | Organizer | Check-in Staff | Admin |
| --- | --- | --- | --- | --- | --- |
| Browse published event | ✓ | ✓ | ✓ | ✓ | ✓ |
| Reserve / pay / cancel pending order | — | ✓ (own) | — | — | — |
| View/download My Tickets | — | ✓ (own) | — | — | — |
| Apply Organizer | — | ✓ (own) | — | — | — |
| Manage event/ticket type | — | — | ✓ (own org) | — | — |
| Submit/publish approved event | — | — | ✓ (verified org) | — | — |
| Assign/revoke event staff | — | — | ✓ (own org) | — | — |
| View event orders / attendees | — | — | ✓ (own org) | Limited lookup | ✓ |
| Scan/manual check-in | — | — | ✓ (authorized event) | ✓ (assigned event) | — |
| View revenue/request settlement | — | — | ✓ (finance permission) | — | Review only |
| Verify organizer / moderate event | — | — | — | — | ✓ |
| Configure platform fee | — | — | — | — | ✓ |
| Resolve payment exception / confirm settlement | — | — | — | — | ✓ |
| Manage user/category/audit | — | — | — | — | ✓ |

Granular organization permission (management/finance/check-in) chưa được chốt; bảng xác định capability, không bắt buộc tạo role mới. Admin không mặc nhiên thay mặt customer mua vé hay trực tiếp sửa số lượng tồn kho. UI route guard chỉ hỗ trợ UX; backend là nguồn xác thực quyền.

**Traceability:** AUTH-01–11, BR-18; C-05–07, C-15, X-01–02, A-02.

## 4. Organizer Management

| ID | Rule | Level |
| --- | --- | --- |
| ORG-R01 | Hồ sơ gửi xác minh phải được Admin quyết định; không tự chuyển VERIFIED. | MUST |
| ORG-R02 | Organizer chưa VERIFIED không submit event review, publish hoặc mở bán; draft có thể chuẩn bị nếu được phép. | MUST (gate); PROPOSED (draft) |
| ORG-R03 | Quyền tổ chức gắn với membership còn hiệu lực; quyền Staff gắn với event được phân công; revoke có hiệu lực ở lần xác thực server tiếp theo. | MUST |
| ORG-R04 | Tài khoản nhận tiền và hồ sơ xác minh được giới hạn truy cập, không hiển thị full trên danh sách. | MUST |
| ORG-R05 | Quyết định reject/suspend phải có lý do, actor và thời điểm; không xóa lịch sử. | PROPOSED |
| ORG-R06 | Khi suspend, chặn bán mới và mutation quản lý thông thường; dữ liệu lịch sử, giao dịch đã trả tiền, exception vẫn được xử lý an toàn. Quyền check-in của staff hiện hữu cần policy riêng. | PROPOSED |
| ORG-R07 | Đổi thông tin payout không thay đổi snapshot của settlement đã tạo; yêu cầu xác minh lại trước payout mới. | PROPOSED |
| ORG-R08 | Không gán staff sang event thuộc org khác; không để việc thu hồi membership làm mất audit log cũ. | MUST |

### 4.1. Organizer state machine

| Current | Action | Actor | Guard | Next |
| --- | --- | --- | --- | --- |
| None | Submit application | Customer | Dữ liệu hồ sơ hợp lệ | PENDING_VERIFICATION |
| PENDING_VERIFICATION | Approve | Admin | Review hồ sơ thành công | VERIFIED |
| PENDING_VERIFICATION | Reject | Admin | Lý do (proposed) | REJECTED |
| REJECTED | Resubmit | Applicant | Sửa hồ sơ hợp lệ | PENDING_VERIFICATION |
| VERIFIED | Suspend | Admin | Có căn cứ và audit | SUSPENDED |
| SUSPENDED | Restore | Admin | Đủ điều kiện xác minh lại | VERIFIED (proposed) |

Không cho client đặt state trực tiếp. Chính sách hồ sơ đang review được sửa hay không, hồ sơ bắt buộc và restore cần xác nhận.

**Traceability:** ORG-01–09, ADM-03–04, BR-01/18; O-01/11/14, A-03–04.

## 5. Event Management

### 5.1. Data and publication rules

| ID | Rule | Level |
| --- | --- | --- |
| EVT-R01 | Event thuộc đúng một Organization, một địa điểm, một khoảng thời gian; `startAt < endAt`. | MUST |
| EVT-R02 | Event có sức chứa hợp lệ, tổng quota ticket types không vượt sức chứa. | MUST |
| EVT-R03 | Cho lưu draft chưa đầy đủ; submit yêu cầu tên, category, mô tả, banner, venue, thời gian, sức chứa, điều khoản, ít nhất một hạng vé hợp lệ và VERIFIED organizer. | PROPOSED |
| EVT-R04 | Admin approve không tự publish. Organizer chỉ publish bản hiện hành đã được approve. | MUST |
| EVT-R05 | Không cho sửa dữ liệu đang review làm approve nhầm phiên bản; các quyết định review và publish phải kiểm tra version/state hiện hành. | MUST (version safety); PROPOSED (locking policy) |
| EVT-R06 | Sau khi phát sinh order/transactions không xóa vật lý event; không sửa lịch sử các order đã ghi nhận. | MUST |
| EVT-R07 | Event bị hủy phải chặn reserve và check-in từ thời điểm hủy có hiệu lực; vô hiệu quyền tham dự, giải quyết các khoản đã thu, thông báo người bị ảnh hưởng. | MUST |
| EVT-R08 | Lifecycle tự cập nhật theo thời gian server, không phụ thuộc client hoặc việc người dùng có mở trang hay không. | PROPOSED |
| EVT-R09 | Các thao tác moderation/cancel có lý do và lịch sử; không publish/cancel từ dữ liệu stale. | PROPOSED |

### 5.2. Event state machine

| From | Action | Actor | Guard | To |
| --- | --- | --- | --- | --- |
| None | Create | Organizer | Có quyền tạo draft | DRAFT |
| DRAFT / REJECTED | Submit | Organizer | VERIFIED, đủ thông tin và ticket config | PENDING_REVIEW |
| PENDING_REVIEW | Approve | Admin | Đúng revision, hợp lệ | APPROVED |
| PENDING_REVIEW | Reject | Admin | Lý do rejection | REJECTED |
| APPROVED | Publish | Organizer | VERIFIED, đúng revision, sự kiện chưa bắt đầu | PUBLISHED |
| PUBLISHED | Start | System | `now >= startAt` và chưa hủy | ONGOING |
| ONGOING | Finish | System | `now >= endAt` | COMPLETED |
| PUBLISHED / ONGOING | Cancel | Authorized actor | Lý do, xác nhận tác động | CANCELLED |

Bảng chuyển trạng thái chi tiết là đề xuất phù hợp PRD §7: cancel trước publish, quyền admin cancel, lifecycle catch-up và xử lý trường hợp đặc biệt cần chốt. CANCELLED/COMPLETED không tự mở bán lại. Nếu job trễ, backend vẫn đánh giá cửa sổ thời gian thực tế khi quyết định bán/check-in. Không chuyển COMPLETED thành PUBLISHED.

### 5.3. Event edit policy (PROPOSED)

| Stage | Allowed changes | Guard |
| --- | --- | --- |
| DRAFT / REJECTED | Sửa nội dung, thời gian, địa điểm, ticket config | Hợp lệ khi submit |
| PENDING_REVIEW | Read-only trường đang review | Muốn sửa cần quy trình rút/gửi lại chưa chốt |
| APPROVED / PUBLISHED, chưa có holds hoặc sales | Cho sửa mô tả/hình ảnh không trọng yếu; thay đổi trọng yếu bắt buộc review lại | Không tự công khai revision chưa approve; nếu unpublish cần policy |
| PUBLISHED, đã có hold hoặc order CONFIRMED | Quản lý giá/quota cho tương lai theo §6; sửa thông tin không trọng yếu | Khóa đổi venue, start/end, điều khoản tham dự trọng yếu; không làm mất quyền đã mua |
| ONGOING / COMPLETED / CANCELLED | Read-only nội dung trọng yếu, thao tác vận hành phù hợp state | Không tự sửa số liệu lịch sử |

Nếu cần đổi địa điểm/thời gian sau khi bán, xử lý qua policy được phê duyệt (outstanding decision), không tự xây feature reschedule/transfer.

### 5.4. Cancellation workflow

1. Authorize actor; hiển thị impact và yêu cầu xác nhận/lý do.
2. Ghi trạng thái CANCELLED tại điểm quyết định nhất quán với reserve/confirm/check-in; chặn giao dịch mới ngay. Nếu callback đến cùng lúc, một quyết định phải thắng theo thứ tự commit; tiền thu mà không còn quyền phát hành phải vào exception.
3. Giải phóng reservations chưa consumed, hủy đơn pending theo policy; không xóa CONFIRMED hoặc SUCCESS lịch sử. Đồng bộ payment attempt còn chưa rõ kết quả qua gateway.
4. Vô hiệu quyền check-in của tất cả vé; ISSUED → VOIDED. CHECKED_IN đã dùng được giữ lịch sử, không mở quyền check-in lại. Chặn issuance còn pending hoặc đánh dấu void nếu cần lưu dấu vết; không phát vé có thể dùng sau hủy.
5. Ghi nghĩa vụ hoàn trả tiền cho mọi khoản đã thu bị ảnh hưởng, chống tạo trùng; Admin xử lý bên ngoài gateway và ghi evidence (§9.6). Không hứa hoàn tự động.
6. Gửi email và lưu audit; sự kiện hủy không eligible settlement thông thường. Nếu có chi trả prior settlement thì ghi lệch/tổn thất, escalate theo quy trình finance, không tự sửa sổ đã settle.

Các bước xử lý hàng loạt có thể bất đồng bộ; chặn bán/check-in không được phụ thuộc vào hoàn thành từng record. Không làm mất nghĩa vụ tài chính nếu một job lỗi hoặc retry.

**Traceability:** EVT-01–14, BR-01–04/13/20; O-03–07, A-05–06, C-04.

## 6. Ticket Type & Inventory Management

### 6.1. Data rules

| ID | Rule | Level |
| --- | --- | --- |
| INV-R01 | Mỗi ticket type thuộc một event; giá VND không âm, số lượng là số nguyên; ticket miễn phí có giá 0. | MUST (nonnegative); PROPOSED (VND rounding) |
| INV-R02 | Sale start trước sale end; mỗi hạng vé có cửa sổ bán và max quantity per order hợp lệ. | MUST |
| INV-R03 | Chỉ bán hạng vé được mở bán, trong thời gian bán và event đủ điều kiện; trạng thái pause chặn reservation mới. | MUST |
| INV-R04 | Không giảm quota xuống dưới số lượng đã xác nhận + đang giữ; tổng quota không vượt capacity event. | MUST |
| INV-R05 | Kiểm tra quota, event capacity và cập nhật tồn kho dưới cơ chế atomic consistency; không dùng read cache để quyết định. | MUST |
| INV-R06 | Giá mới chỉ áp dụng cho order mới; order đã giữ chỗ có snapshot giá. | MUST |
| INV-R07 | Không tự sửa `sold`/`reserved` bằng admin form; thay đổi phải đến từ allocation transition được truy vết. | MUST |
| INV-R08 | Sale window đề xuất `[saleStart, saleEnd)` với `saleEnd <= eventStart`; không bán trong ONGOING nếu chưa có policy. | PROPOSED |
| INV-R09 | Pause/close sale không làm mất hold hợp lệ đã có, trừ event cancel hoặc policy bắt buộc; checkout phải hoàn tất trước hạn hold. | PROPOSED |

### 6.2. Inventory invariants

Với mỗi ticket type `t`:

```text
Q(t) = configured quota
R(t) = quantity allocated to active reservations
S(t) = quantity committed to confirmed orders
A(t) = Q(t) - R(t) - S(t)

Q,R,S,A are non-negative integers
R + S <= Q
sum(Q of all ticket types) <= event.capacity
```

`S` bao gồm vé miễn phí và vé chưa tạo QR nhưng đã CONFIRMED. `S` không tăng/giảm chỉ vì email gửi lỗi, QR generation, check-in hoặc render My Tickets. MVP không có chức năng trả vé tự phục vụ/restock; hủy event hoặc vô hiệu vé không tự đưa vé trở lại bán. Các điều chỉnh ngoại lệ phải có policy kế toán/tồn kho riêng, không sửa thủ công counters.

Hold đến hạn dù job chưa chạy không còn đủ điều kiện confirm. Đề xuất cách biểu diễn tách:

- `R_allocated`: lượng hold chưa được consume/release/expire trong authoritative inventory; có thể gồm hold quá thời gian nhưng job chưa cleanup.
- `R_effective`: lượng hold chưa hết hạn theo server clock; dùng cho reporting nếu tính đúng.
- Khi quyết định giữ vé, có thể release hold quá hạn một cách nguyên tử hoặc bảo thủ tiếp tục tính chúng trong `R_allocated` tới khi release; không lấy `R_effective` để bán lại rồi release lần nữa. API `available` chỉ báo lượng thật sự có thể allocate theo backend. Nếu UI hiển thị reserved/available theo cách khác nhau, ghi rõ pending cleanup; không tuyên bố còn vé không thể cấp.

### 6.3. Inventory transition ledger

| Operation | Precondition | Delta R | Delta S |
| --- | --- | ---: | ---: |
| Reserve q | A >= q and sale eligible | +q | 0 |
| Consume q | ACTIVE, now < expiresAt, order eligible | -q | +q |
| Expire q | ACTIVE, now >= expiresAt | -q | 0 |
| Release q | ACTIVE, authorized cancel | -q | 0 |
| Issue ticket | confirmed allocation exists | 0 | 0 |
| Check-in | valid ticket | 0 | 0 |
| Duplicate/retry same committed operation | prior result exists | 0 | 0 |

Mỗi reservation line chỉ đóng allocation một lần. Với order nhiều hạng vé: all-or-nothing; nếu một hạng thiếu vé không được giữ các hạng còn lại.

### 6.4. Ticket sale display state (derived, not a new persisted enum)

Thứ tự đánh giá đề xuất: event không đủ điều kiện → UNAVAILABLE; paused → PAUSED; chưa đến giờ → NOT_STARTED; đã đến giờ đóng → CLOSED; không có vé khả dụng → SOLD_OUT; còn lại ON_SALE. UI label không tự cấp quyền mua. Dữ liệu public có thể lag, backend phải revalidate.

**Traceability:** INV-01–13, BR-03–05/14/21; O-07, C-04/08.

## 7. Event Discovery

| ID | Rule | Level |
| --- | --- | --- |
| DSC-R01 | Guest/customer được browse dữ liệu event công khai; không lộ DRAFT/PENDING_REVIEW/APPROVED chưa publish/REJECTED trong public search. | MUST |
| DSC-R02 | Event đã hủy không còn trong danh sách bán; deep link có thể trả thông tin công khai đã hủy theo policy, không cung cấp CTA mua. | MUST (no sale); PROPOSED (public detail) |
| DSC-R03 | Search/filter theo event, category, địa điểm, ngày và giá theo metadata công khai; pagination không làm thay đổi điều kiện quyền. | MUST |
| DSC-R04 | Giá từ hiển thị giá nhỏ nhất trong các hạng đang đủ điều kiện hiển thị; phân biệt chưa mở bán/hết vé/miễn phí; không lấy thiếu dữ liệu thành 0. | PROPOSED |
| DSC-R05 | Featured do Admin quản lý; Popular đề xuất xếp theo số vé được xác nhận trong cửa sổ thời gian (cần chốt). | PROPOSED |

Kết quả search không phải cam kết tồn kho hay giá. Tại lúc gửi reserve, kiểm tra lại phiên bản giá, số lượng và quyền; dữ liệu thay đổi thì trả lỗi có thể phục hồi và yêu cầu customer xác nhận lại.

**Traceability:** DSC-01–11, ADM-08; C-01–04, C-08.

## 8. Ticket Booking & Order Management

### 8.1. Rule catalogue

| ID | Rule | Level |
| --- | --- | --- |
| ORD-R01 | Một order thuộc một customer và một event; ít nhất một ticket line có quantity > 0; nhiều ticket types cùng event được phép. | MUST |
| ORD-R02 | Tất cả giá và tổng tiền do backend tính; order giữ unit price snapshot, quantity, total và currency; client không tự set tổng tiền. | MUST |
| ORD-R03 | Reserve và tạo pending order là một nghiệp vụ all-or-nothing: không để orphan hold khi create order lỗi. | MUST |
| ORD-R04 | Hạn hold được xác định lúc reserve thành công; mặc định 10 phút theo PRD, không reset khi reload, retry hoặc đổi tab. | MUST (expiry); PROPOSED (10 min default) |
| ORD-R05 | Chủ order được hủy PENDING_PAYMENT chưa thanh toán nếu không có attempt đang chưa rõ kết quả (retry/cancel guard đề xuất). CONFIRMED không thể tự hủy. | MUST (not confirmed); PROPOSED (pending attempt guard) |
| ORD-R06 | Hết hạn thì EXPIRED + giải phóng hold; không thể xác nhận order đã EXPIRED/CANCELLED. | MUST |
| ORD-R07 | Order total = 0 bỏ gateway; xác nhận free order bằng backend khi hold còn hợp lệ và user đáp ứng điều kiện. | MUST |
| ORD-R08 | Lặp thao tác reserve/confirm cùng logical request không tạo thêm order hoặc double allocation. | MUST (no duplicate effects); PROPOSED (request key contract) |
| ORD-R09 | Hệ thống trả server time, expiresAt và action eligibility; client timer chỉ để hiển thị, không tự thay state. | PROPOSED |
| ORD-R10 | Đề xuất không sửa ticket lines của order pending; đổi lựa chọn thì cancel/release nếu an toàn rồi tạo order mới. | PROPOSED |

### 8.2. Reservation and order state machines

| Reservation from | Trigger | Guard | To |
| --- | --- | --- | --- |
| None | Reserve | Valid quantities, stock, sale, owner | ACTIVE |
| ACTIVE | Confirm order | now < expiresAt, all payment/free guards pass | CONSUMED |
| ACTIVE | Expire | now >= expiresAt | EXPIRED |
| ACTIVE | Cancel/release | Order cancel allowed / event canceled | RELEASED |

CONSUMED, EXPIRED, RELEASED là terminal. Không chuyển lại ACTIVE; retry logical operation trả trạng thái hiện hành.

| Order from | Trigger | Guard | To |
| --- | --- | --- | --- |
| None | Reserve/create | Reservation được tạo cùng logical operation | PENDING_PAYMENT |
| PENDING_PAYMENT | Confirm | ACTIVE hold chưa hết hạn; paid verified hoặc free; event không hủy | CONFIRMED |
| PENDING_PAYMENT | Expire | now >= expiresAt và chưa confirm | EXPIRED |
| PENDING_PAYMENT | Cancel | Owner allowed, chưa thanh toán hoặc event cancellation | CANCELLED |

CONFIRMED/EXPIRED/CANCELLED là terminal theo MVP. Payment exception không mở lại order. Không thêm REFUND_PENDING vào OrderStatus. Trạng thái xử lý tiền và issuance là thông tin riêng.

### 8.3. Atomic reservation decision

```text
reserve(customer, event, selectedLines, requestIdentity):
  authenticate and authorize customer
  validate non-empty lines, positive integer quantities, same event
  combine duplicate ticket types for limit checks
  compare current price/terms with customer-confirmed selection
  atomically:
    recheck event/org eligibility and sale windows
    recheck quota and per-order limit for every line
    if any invalid or insufficient: no allocation, no order
    capture server timestamp, price snapshots and expiry
    allocate all lines; create ACTIVE reservation + PENDING_PAYMENT order
  return order and expiresAt
```

Pseudocode chỉ mô tả outcome; cơ chế serialization/transaction do SDD xác định. Kiểm tra lặp trong vùng nhất quán là bắt buộc, không phải kiểm tra ngoài vùng rồi update mù.

### 8.4. Confirmation decision and races

```text
confirm(order, verifiedPaymentEvidence or FREE_ORDER):
  atomically evaluate current order + reservation + event + allocation
  if order already confirmed with same evidence: replay current result
  if order non-pending or reservation not ACTIVE or now >= expiresAt:
      do not confirm; if money collected, record payment exception
  if event cancelled: do not confirm; if money collected, record exception
  verify amount/currency/order reference (or total == 0 for FREE_ORDER)
  consume allocation (R -= q, S += q)
  set order CONFIRMED; persist financial snapshot and issuance obligation
```

Mốc đề xuất: dùng thời điểm quyết định confirmation của backend, không chỉ provider paidAt; giao dịch nhận muộn sau expiry không được cấp vé ngay cả khi gateway báo đã trả trước đó. Đây là baseline xử lý late success của PRD; chính xác clock/lock contract phải được chốt. Nếu expire thắng, không confirm dù có vé vừa được release; nếu confirm thắng trước expiry, expire job không trừ R lần nữa. Job cleanup và webhook phải chia sẻ cùng quy tắc cạnh tranh. Nếu action timeout, client tra cứu trạng thái authoritative trước khi retry.

### 8.5. Checkout total

```text
lineTotal = unitPriceSnapshot * quantity
orderTotal = sum(lineTotals)
```

Mô hình đề xuất mặc định Organizer chịu phí nền tảng/gateway nên customer trả đúng tổng giá vé; không thêm coupon/phí người mua ngoài phạm vi. Free order nếu tất cả line total = 0; order trộn free/paid vẫn thanh toán tổng > 0. Số nguyên VND, không dùng floating-point cho tiền.

**Traceability:** ORD-01–15, BR-05–09/14/19/22; C-08–12.

## 9. Online Payment Integration

### 9.1. Rule catalogue

| ID | Rule | Level |
| --- | --- | --- |
| PAY-R01 | Một gateway trong MVP; order có tổng tiền > 0 mới tạo attempt. | MUST |
| PAY-R02 | Backend tạo request từ order snapshot; xác thực signature, merchant, reference, amount, currency và trạng thái theo contract của gateway. | MUST |
| PAY-R03 | Browser redirect/return URL chỉ để điều hướng; không xác nhận giao dịch từ client. | MUST |
| PAY-R04 | Webhook/query/reconciliation có thể đến trùng hoặc sai thứ tự; cùng một giao dịch chỉ được áp dụng một lần. | MUST |
| PAY-R05 | Một attempt đang pending/unknown không được retry payment tự động; phải query và xác định có thể retry an toàn. | PROPOSED |
| PAY-R06 | Một order có tối đa một khoản thanh toán được áp dụng để confirm; các khoản thu dư là exception, không tăng số vé. | MUST |
| PAY-R07 | SUCCESS đã xác minh không bị hạ xuống FAILED/EXPIRED bởi callback lỗi đến muộn. | MUST |
| PAY-R08 | Tiền đã thu không thể áp dụng cho order hợp lệ phải được ghi nhận, đối soát và hoàn trả thủ công; không bỏ qua callback vì order hết hạn. | MUST |
| PAY-R09 | Không lưu PAN/CVV hoặc thông tin thẻ nhạy cảm; không ghi secrets, full callback payload chứa PII vào log. | MUST |
| PAY-R10 | Tách kết quả payment attempt với kết quả order, issuance và settlement; không suy diễn payment failed từ mất kết nối. | MUST |
| PAY-R11 | Lưu audit cho thay đổi tiền, gateway references, reconciliation và exception resolution. | MUST |

### 9.2. Payment state machine

| From | Verified event / action | Guard | To |
| --- | --- | --- | --- |
| None | Initiate | Pending order, valid hold, total > 0, retry eligible | PENDING |
| PENDING | Provider confirms successful collection | Verified evidence; reference/amount reconciled | SUCCESS |
| PENDING | Provider confirms final failure | Không có evidence đã thu tiền | FAILED |
| PENDING | Expire attempt | Provider confirms expiry/non-payment hoặc policy đã chốt; local timeout không đủ chứng cứ | EXPIRED |
| FAILED / EXPIRED | Later verified collection | Evidence đáng tin cậy xác nhận thực thu; ghi anomaly | SUCCESS |
| SUCCESS | Record full manual return | Verified actual returned amount equals collected amount | REFUNDED |

FAILED/EXPIRED → SUCCESS là sửa trạng thái theo evidence thực thu, không phải retry order cũ. Retry tạo attempt identity mới. UNKNOWN là presentation/operational condition của attempt chưa có kết luận, không phải enum mới. Không chuyển SUCCESS về PENDING/FAILED/EXPIRED. Không đánh dấu REFUNDED chỉ vì đã gửi yêu cầu hoàn trả. Nếu trả một phần, giữ SUCCESS và ghi số tiền đã trả/exception còn tồn tại; quy trình partial refund tự phục vụ vẫn out of scope.

### 9.3. Payment decision table

| Evidence | Order / reservation | Outcome |
| --- | --- | --- |
| Invalid signature / untrusted payload | Any | Reject/quarantine; security audit; không thay đổi tiền, order, inventory |
| Verified pending | Pending / active | Keep pending, query later |
| Verified failed, no collection | Pending / active | Attempt FAILED; retry nếu backend cho phép |
| Verified correct success, first applied payment | Pending / active, before expiry, event eligible | Confirm once, consume hold, create issuance obligation |
| Same verified success repeated | Confirmed with same applied payment | Idempotent replay; không tạo thêm vé/fee |
| Verified success | Expired / cancelled / inactive hold | Record collected amount and exception; no issuance |
| Distinct verified success | Already confirmed with another payment | Duplicate collection exception; original order unchanged |
| Verified collection but wrong amount/currency | Any | Investigate; no confirm based on mismatched amount; record actual funds separately |
| Verified transaction not mapped to known order | Any | Unmatched exception/reconciliation; no new order |
| Failure callback arrives after success | Any | Preserve SUCCESS; retain anomaly evidence if conflicting |
| Provider return page says success but server has no verified evidence | Any | Show pending/unknown; no confirmation |
| Success arrives during event cancellation | Any | Serialize with cancel; if already confirmed, revoke admission and reconcile; otherwise no issuance, record return obligation |

### 9.4. Idempotency requirements

- Identity for a provider transaction must include provider/merchant scope and authoritative transaction reference. Use provider-supported unique event ID if available; otherwise safe canonical identity based on provider contract.
- A callback may repeat with different delivery IDs: dedup delivery is insufficient; also dedup the underlying financial transaction and the order-confirmation effect.
- Persist verified receipt and processing outcome durably before acknowledging successful handling to provider. If processing is asynchronous, receipt must be recoverable until terminal handling. Exact acknowledgement/status codes depend on provider.
- A repeated request with the same logical key and same payload returns previous result; conflicting payload under the same key fails explicitly. Key retention/lifetime is part of SDD/API design.
- External operations cannot be assumed exactly-once; use reconciliation and durable effect identity to achieve at-most-one business effect. No second charge just because a client request timed out.

### 9.5. Expiry, cancellation, and reconciliation

1. Timer expiry means hold can no longer be consumed; it does not prove payment was not received.
2. Expire order/hold atomically; unresolved payment remains tracked and is queried in reconciliation until definitive outcome or manual investigation.
3. If verified success is processed after expiry, do not reacquire inventory or reopen the order. Record exception linked to actual collection. Customer sees reference/status, not a usable QR.
4. If verified confirmation wins while hold is valid, expiry cannot release consumed inventory. Failure callback cannot reverse the paid state.
5. Reconciliation compares gateway references, received amount, currency, fee, external status and local records. Differences go to investigation. Do not fabricate SUCCESS or SETTLED to close a mismatch.

### 9.6. Manual financial exception handling (within MVP)

Proposed exception categories: `LATE_SUCCESS`, `DUPLICATE_COLLECTION`, `AMOUNT_MISMATCH`, `UNMATCHED_TRANSACTION`, `EVENT_CANCELLED`, `RECONCILIATION_MISMATCH`, `OTHER`. These are contract proposals, not fixed physical schema.

| State (proposed) | Action | Guard |
| --- | --- | --- |
| OPEN | Start review → IN_REVIEW | Admin; lock/claim logical case if needed |
| IN_REVIEW | Record actual return progress | Provider/manual evidence, no automatic gateway refund API |
| IN_REVIEW | Resolve → RESOLVED | Actual required funds returned or discrepancy reconciled with documentary evidence |
| IN_REVIEW | Stay open/escalate | Evidence missing, amount outstanding, reversal/chargeback unknown |

Minimum case information: exception ID/type, order/event/org link where known, actual collection reference, expected vs received amount/currency, outstanding amount, actor, reason, timestamps, external refund/return reference, evidence, resolution. For mismatched currency retain original currency and require finance review; never add incompatible currencies. For unmatched collection, keep in suspense/reconciliation without allocating it as event revenue. Multiple reasons may apply to one received transaction; do not count the same money twice. Each recorded return must be uniquely referenced, amount positive and bounded by outstanding balance. Failed attempt to transfer is not completed return. If chargeback or bank reversal appears externally, reconcile evidence without designing a new self-service feature.

Finance blocking rule: unresolved exceptions affecting an event block its ordinary settlement. If an unidentified payment may affect a settlement, hold the disputed amount or block the affected settlement until allocation is resolved; exact risk policy requires approval. Never silently mark unresolved as resolved. Customer-facing state follows order/payment plus exception summary, not a new OrderStatus.

**Traceability:** PAY-01–13, ADM-09–11, BR-08–09/16–17/20/23/25; C-09–12, A-08–10.

## 10. QR Code E-Ticket

| ID | Rule | Level |
| --- | --- | --- |
| ETK-R01 | Chỉ order CONFIRMED có quyền phát hành vé; số vé bằng tổng confirmed quantity theo từng order line. | MUST |
| ETK-R02 | Một vé có một ticket identity duy nhất và một QR credential hợp lệ; một unit trong order line không thể sinh hai vé khi retry. | MUST |
| ETK-R03 | Phát hành phải recoverable sau lỗi; retry phần chưa hoàn thành, không tạo mới đơn hoặc trừ thêm inventory. | MUST |
| ETK-R04 | QR chứa credential không dễ đoán, không chứa trực tiếp PII hoặc dữ liệu thẻ; backend xác thực credential, event và current status. | PROPOSED |
| ETK-R05 | Chỉ owner được xem/download vé cá nhân; organizer/staff xem metadata theo quyền; không trả QR credential cho danh sách quản trị thông thường. | MUST (access); PROPOSED (QR exposure) |
| ETK-R06 | Ticket VOIDED hoặc CHECKED_IN không được dùng để check-in lần nữa; QR đã cache/PDF cũ không vượt qua server validation. | MUST |
| ETK-R07 | Event hủy không cấp vé có thể sử dụng; vô hiệu admission ngay cả khi issuance batch đang chạy. | MUST |
| ETK-R08 | Dùng buyer name/contact cho từng vé nếu MVP chưa có attendee form riêng; buyer identity không xác định người check-in thực tế. | PROPOSED |

### 10.1. Ticket state machine

| From | Trigger | Guard | To |
| --- | --- | --- | --- |
| None | Issue | Order CONFIRMED, unique allocated unit, event not canceled | ISSUED |
| ISSUED | Successful check-in | Authorized staff, correct event/time, credential valid | CHECKED_IN |
| ISSUED | Void | Event cancelled / authorized financial invalidation | VOIDED |

CHECKED_IN và VOIDED không quay lại ISSUED. Vé đã check-in giữ lịch sử; nếu event hủy sau đó, trạng thái event khiến vé không còn quyền tham dự. Không tự reset check-in. Không cung cấp transfer/resale/reissue đổi chủ trong MVP. Nếu phát hành một phần và sự kiện hủy, không tiếp tục phát hành QR dùng được; ghi nhận số đơn vị chưa phát hành để đối soát và audit. Tiêu chí equality về số vé áp dụng cho order CONFIRMED đủ điều kiện phát hành, không yêu cầu tạo vé sử dụng được cho event đã hủy.

### 10.2. Issuance lifecycle (derived operational view)

| Derived condition | User-visible state |
| --- | --- |
| Order CONFIRMED, issued count < expected, no terminal failure | ISSUING |
| Order CONFIRMED, all eligible units issued | READY |
| Retryable issuance error | ISSUING / delayed, support reference if prolonged |
| Event CANCELLED | No admission; display cancellation/void information |

`ISSUING` và `READY` không phải TicketStatus PRD. Phát hành hoặc tạo PDF/email lỗi không đổi Order CONFIRMED hoặc Payment SUCCESS. Mỗi unit có identity ổn định (ví dụ order item + ordinal) để retry phát hành. Generate/download PDF có cùng ticket ID/credential, không sinh vé mới. Static QR có thể bị sao chép; online single-use validation ngăn sử dụng lần hai, không bảo đảm người quét đầu tiên là chủ sở hữu. Không tuyên bố chống mọi hình thức sao chép vé.

**Traceability:** ETK-01–10, BR-08/10/12/20/22; C-10/12–14, O-09.

## 11. Check-in Management

| ID | Rule | Level |
| --- | --- | --- |
| CHK-R01 | Chỉ actor đã xác thực, được phân quyền với đúng event mới tra cứu/xác thực/check-in. | MUST |
| CHK-R02 | Check-in chỉ trực tuyến; không ghi thành công khi offline hoặc backend chưa xác nhận. | MUST |
| CHK-R03 | Vé phải tồn tại, thuộc event, ISSUED, event không hủy, trong cửa sổ check-in và credential hợp lệ (hoặc manual lookup được phép). | MUST |
| CHK-R04 | Validate + consume admission + tạo success record là một nghiệp vụ atomic; hai scanner không thể cùng thành công. | MUST |
| CHK-R05 | Tra cứu thủ công chỉ trả dữ liệu cần thiết; lookup không tự check-in; confirm thủ công chịu cùng guard như QR scan. | MUST |
| CHK-R06 | Success record lưu ticket/event/staff/time/method, không mất khi staff bị xóa quyền. | MUST |
| CHK-R07 | Mất kết nối sau submit: truy vấn lại hoặc replay cùng request identity; không cho qua cổng dựa trên timeout. | PROPOSED |
| CHK-R08 | Cửa sổ mặc định đề xuất `[startAt - 2h, endAt)`; event có thể đang PUBLISHED hoặc ONGOING trong window. | PROPOSED |

### 11.1. Validation outcomes (semantic codes, API spelling TBD)

| Condition | Outcome | Mutation |
| --- | --- | --- |
| No valid session / staff permission | UNAUTHORIZED / FORBIDDEN | None; no ticket metadata |
| Invalid or unknown QR | INVALID_TICKET | None |
| Ticket belongs to different event | WRONG_EVENT | None; do not leak other event's PII |
| Event canceled / ticket voided | EVENT_CANCELLED / VOIDED | None |
| Outside allowed window | OUTSIDE_CHECK_IN_WINDOW | None |
| Ticket already checked in by distinct request | ALREADY_USED | None; return existing time if authorized |
| Valid ticket and conditional consume succeeds | SUCCESS | Single success record + CHECKED_IN |
| Same check-in logical request replayed | Original decision | No new record |
| Network / unknown outcome | UNKNOWN (UI) | Query authoritative state; do not assume failure/success |

Check authorization first; then event context, ticket, admission and current state. Exact precedence between other denial reasons belongs API spec, but no invalid request may mutate inventory/admission. Replayed success is not a new admission: UI should distinguish a replay for the same operation from a fresh successful scan to avoid admitting a second person. Count unique successful ticket admissions, not HTTP 200s or scan attempts. Do not expose QR secret in logs. Failed scans may be audited with minimal metadata and appropriate rate limiting; not counted as attendees.

**Traceability:** CHK-01–10, BR-11–12/18/20; S-01–06, O-09–11.

## 12. Commission & Settlement

### 12.1. Commission rules

| ID | Rule | Level |
| --- | --- | --- |
| FIN-R01 | Admin configures one supported fee mode: PERCENTAGE or FIXED_AMOUNT per paid ticket; no hybrid. | MUST |
| FIN-R02 | Freeze applied commission configuration and calculated amount for each confirmed transaction; no retroactive recompute from current configuration. | MUST |
| FIN-R03 | Default proposed model: Organizer bears platform/gateway fees; buyer pays ticket total. Snapshot configuration at order creation, finalize amount at confirmation. | PROPOSED |
| FIN-R04 | Percentage within 0–100%; fixed fee nonnegative; cap per paid ticket at its price and free ticket fee = 0 to avoid platform fee creating negative balance by itself. | PROPOSED |
| FIN-R05 | Record actual gateway fee from reconciliation; estimates may be displayed separately but cannot authorize payout. | MUST (reconciliation); PROPOSED (eligibility guard) |
| FIN-R06 | Separate collected money, confirmed sales, fee revenue, return obligations, adjustments and net payout; no double counting. | MUST |
| FIN-R07 | All monetary amounts use integer VND; proposed percentage rounding HALF_UP per ticket, then multiply quantity. | PROPOSED |

### 12.2. Calculation examples

Proposed calculation model (review Finance before production):

```text
lineGross = quantity * priceSnapshot
feePerTicket = 0 if priceSnapshot == 0
               min(priceSnapshot, roundHalfUp(priceSnapshot * rate / 100)) for PERCENTAGE
               min(priceSnapshot, fixedFee) for FIXED_AMOUNT
linePlatformFee = quantity * feePerTicket
orderGross = sum(lineGross)
orderPlatformFee = sum(linePlatformFee)
settlementNet = eligibleGross - eligiblePlatformFee - actualGatewayFee - applicableAdjustments
```

`applicableAdjustments` là các khoản giảm trừ đã xác nhận, có provenance và sign rõ ràng; nếu hỗ trợ credit adjustment, dùng quy ước signed amount minh bạch. Không trừ cùng một khoản vừa ở gross/fee vừa ở adjustments. Không tự áp dụng thuế hoặc phí gateway giả định làm số liệu thực. Chính sách fee reversal khi hoàn tiền, gateway fee không hoàn và chargeback cần xác nhận. Nếu net âm (có thể do phí thực/adjustment), block ordinary payout, finance xử lý ngoài flow tự động; net bằng 0 không tạo transfer giả.

| Case | Input | Expected (proposed) |
| --- | --- | --- |
| Percentage | 2 × 500,000; 5%; gateway fee 10,000 | Gross 1,000,000; platform fee 50,000; net 940,000 VND |
| Fixed amount | 3 × 200,000; 10,000/ticket; gateway fee 6,000 | Gross 600,000; platform fee 30,000; net 564,000 VND |
| Free ticket | 3 × 0 | Gross 0; platform fee 0; no gateway attempt |
| Mixed order | 1 × 100,000 + 2 × 0; 5%; gateway fee 2,000 | Gross 100,000; platform fee 5,000; net 93,000 VND |
| Rate changes | Existing order quote 5%; current config 7% | Existing order uses frozen 5%; future eligible order uses 7% |
| Fixed exceeds price | Price 5,000; fixed fee 10,000 | Cap at 5,000; further actual fees may create negative net and require review |

### 12.3. Settlement eligibility and states

| ID | Rule | Level |
| --- | --- | --- |
| FIN-R08 | Ordinary settlement requires event COMPLETED, no unresolved financial obligations, and applicable payment reconciliation completed. | MUST |
| FIN-R09 | Organizer can request settlement only for authorized organization; amount/items computed by backend from eligible records. | MUST |
| FIN-R10 | Each eligible financial entitlement can be included in at most one effective payout; duplicate/concurrent requests cannot reserve same entitlement twice. | MUST |
| FIN-R11 | MVP payout is a manual external transfer; admin records verifiable reference, date, amount, payout snapshot and evidence; only then SETTLED. | MUST |
| FIN-R12 | Recheck eligibility, amount and destination before directing external transfer and before marking SETTLED; changes/conflicts block and require review. | MUST |
| FIN-R13 | A timeout/ambiguous bank outcome keeps settlement blocked for investigation; never instruct transfer again until prior transfer confirmed absent. | MUST |
| FIN-R14 | Proposed: entire event's eligible sales in one settlement batch, no partial payout feature; retry within same batch after confirmed failure. | PROPOSED |
| FIN-R15 | Proposed: zero-amount settlement may close with audited no-transfer decision; require approved policy, never invent transfer reference. | PROPOSED |

| From | Trigger | Actor | Guard | To |
| --- | --- | --- | --- | --- |
| None | Request settlement | Organizer (finance permission) | Eligible event and unclaimed entitlements | PENDING |
| PENDING | Mark processing | Admin | Rechecked items, amount, exception and destination | PROCESSING |
| PROCESSING | Record completed external transfer | Admin | Verified matching amount/reference/evidence, no duplicate | SETTLED |
| PROCESSING | Record definite failure | Admin | Evidence transfer not completed | FAILED |
| FAILED | Retry review | Admin | Prior transfer confirmed not paid, entitlements still protected, eligibility rechecked | PROCESSING (proposed) |

SETTLED terminal. No deletion, status rollback or direct edit of settled amount. FAILED retains claim on financial entitlement until safe retry or explicit authorized release policy; cannot create another payout from same items while outcome unclear. External transfer cannot be made atomic with local state: manual runbook must distinguish not started, transfer status unknown, confirmed paid, confirmed not paid; those may be operational flags/evidence, not new PRD enums. If external money moved but eligibility changes before local marking, record actual movement in financial exception/reconciliation; no second transfer or hiding cash movement. Subsequent adjustments are separately audited and reconciled, not rewriting a settled record. Canceled event cannot use ordinary completed-event settlement.

### 12.4. Settlement data and workflow

Minimum snapshot: settlement ID, organization/event, included order/payment/financial entitlement references, gross/fee/adjustment/net, currency, payout destination version/masked display, created/requested actor/time, processing/review details, transfer reference/evidence and result. Keep audit and financial history. Changing organizer bank account after request does not silently redirect payout; revalidate through approved process. Distinct transfer reference cannot be applied to two settlements. Access to full payout data restricted to authorized actors needing it.

**Traceability:** STL-01–12, ADM-12–13, BR-15–17/24; O-12–14, A-10–12.

## 13. Reporting & Dashboard

Reporting phải dựa trên authoritative transactions/aggregates, không cộng dữ liệu pagination ở frontend. Các định nghĩa đề xuất tương thích UI/UX §8; cần chốt date basis và adjustment policy trước khi triển khai reporting contract.

| ID | Metric / rule | Definition |
| --- | --- | --- |
| RPT-R01 | Tickets sold | Sum confirmed quantities (includes free; not number of QR already issued); voided/cancelled count shown separately. |
| RPT-R02 | Reserved / available | Authoritative inventory per §6; if effective hold metrics differ from allocations pending cleanup, label them separately. |
| RPT-R03 | Sell-through | Confirmed sold / sum of ticket quotas × 100%; display N/A if zero denominator; event capacity may be displayed separately. Proposed denominator requires UI/Finance agreement. |
| RPT-R04 | Successful check-ins | Count unique successful admission records, not scans/replays. |
| RPT-R05 | Check-in rate | Valid attendee admissions / eligible ticket population for comparable event snapshot; exclude voided/noneligible tickets consistently from numerator and denominator if computing post-adjustment rate. N/A if none. |
| RPT-R06 | Gross confirmed sales / GMV | Sum order snapshot totals of CONFIRMED orders in selected confirmation period; exclude unallocated cash and show returns/adjustments separately. |
| RPT-R07 | Cash collected | Verified actual gateway collections (dedup transaction), including unallocated exceptions; subtract verified returns separately for net cash. Free orders not collections. |
| RPT-R08 | Platform fee | Sum frozen earned/allocated fees; actual recognized fee after applicable adjustments separately defined by Finance. |
| RPT-R09 | Gateway fee | Actual reconciled fee; estimated/pending distinguished. |
| RPT-R10 | Net organizer revenue | Gross minus applicable fees/adjustments, calculated server-side; estimated net distinguished from payable amount. |
| RPT-R11 | Settled amount | Sum SETTLED confirmed payouts; no PROCESSING/FAILED, no duplicated transfer. |
| RPT-R12 | Payment success rate | Verified successful attempts / attempts with definitive outcomes in chosen period; dedup attempts, exclude unknown/pending and free orders; time basis specified. |
| RPT-R13 | Reporting access | Organizer scope by membership/event; admin global authorized scope; CSV follows same filter and data permission. |
| RPT-R14 | Time filter | Explicit timezone, date basis and half-open period `[from,to)`; export consistent with dashboard snapshot or timestamp. |

RPT-R01/02/04/06/07/08/09/10/11/13 derive from core business invariants; remaining presentation/metric policy is proposed. Historical confirmed GMV is not reduced by deleting order on event cancellation; report adjustments separately. Define paid ticket count separate from free ticket count when useful. After canceled event, admissions may be historical; distinguish historical count from current eligible denominator. Currency totals must not combine different currencies without approved conversion rules. Do not expose unnecessary PII in exports. Protect CSV from spreadsheet formula injection.

**Traceability:** PRD §6.11/13, BR-17–18/24; O-02/06/10/12/13, S-06, A-01.

## 14. Admin Portal and cross-domain controls

| ID | Rule | Level |
| --- | --- | --- |
| ADM-R01 | Admin actions require authorized role and server-side guard; never expose direct unrestricted update of state/amount/inventory counters. | MUST |
| ADM-R02 | Account lock/unlock, organizer verification/suspension, event review/cancellation, fee changes, financial actions and permission changes retain actor, timestamp, resource, action, result and appropriate reason. | MUST (financial audit); PROPOSED (full catalogue) |
| ADM-R03 | Financial operations preserve before/after relevant values, correlation ID, reference and evidence; mask sensitive fields. | MUST |
| ADM-R04 | Admin cannot approve stale version, resolve closed exception twice, or settle already paid items. | MUST |
| ADM-R05 | Category in use cannot be deleted in a way that breaks event history; proposed deactivate from new selection instead. | PROPOSED |
| ADM-R06 | Featured assignment only affects discovery, not publication/booking eligibility. | MUST |
| ADM-R07 | System config updates validated, versioned and audited; do not change existing reservation expiry or historical price/fee snapshots. | MUST (snapshot integrity); PROPOSED (versioning) |
| ADM-R08 | No log of plain passwords, credentials, raw QR secrets, full bank account, card details or unrestricted personal documents. | MUST |
| ADM-R09 | Admin cannot mark order paid manually without verified gateway evidence; exception handling cannot create tickets for expired/cancelled order. | MUST |

Audit is append-oriented and protected from unauthorized modification. Read/export access to audit itself must be restricted. Retention, data subject requests, privacy policy, segregation of duties and whether finance actions require dual approval are open compliance/security decisions. No assumption that local privacy/legal obligations are satisfied by this specification.

**Traceability:** ADM-01–15, BR-13/17/18/25; A-01–14.

## 15. Email Notification

| ID | Rule | Level |
| --- | --- | --- |
| NTF-R01 | Send only after relevant business change commits; email failures do not roll back order/payment/ticket/settlement. | MUST |
| NTF-R02 | Notification delivery obligation is recoverable after crash/retry; prevent duplicates by logical event + recipient + notification type, without claiming exactly-once email delivery. | MUST (recoverability); PROPOSED (dedupe identity) |
| NTF-R03 | Email verification/reset link expiring, single-use; resend rate-limited; avoid PII/secret in unnecessary links and logs. | PROPOSED |
| NTF-R04 | Payment success message is not admission confirmation; ticket ready message only when ticket is actually available. | MUST |
| NTF-R05 | Reminder only for eligible upcoming tickets; do not send routine reminder for canceled event or voided ticket. | MUST |
| NTF-R06 | Cancellation message explains status and manual financial handling where applicable; does not promise unconfirmed refund timetable. | MUST |
| NTF-R07 | Proposed retry policy bounded exponential backoff with final failure observable to operations; no unlimited retries. | PROPOSED |

| Trigger | Recipient | Condition / dedupe identity example |
| --- | --- | --- |
| Account Registration | Customer | Account created / user + registration event |
| Email Verification | User | Valid requested verification / token request identity |
| Password Reset | User | Valid reset request / request identity |
| Organizer Approved / Rejected | Organizer | Committed decision / application revision + decision |
| Event Approved / Rejected | Organizer | Committed review / event revision + decision |
| Order Confirmation | Customer | CONFIRMED / order ID |
| Payment Successful | Customer | Verified successful payment applied to order / payment reference; late/extra collection uses exception-specific communication, not confirmed ticket copy |
| Ticket Issued | Customer | Issued ticket(s) available / ticket or batch identity |
| Event Reminder | Customer | Upcoming eligible order/ticket / event + recipient + reminder occurrence |
| Event Cancelled | Customer, Organizer, Staff | Committed cancel / event + recipient; include relevant pending/confirmed affected customers |
| Settlement Completed | Organizer | SETTLED / settlement ID |

Proposed reminder 24 hours before start (time/config to confirm); no reminder after event begins. Locale Vietnamese/English according to user preference and fallback policy. No notification center, template editor, push/SMS in MVP. Email can use authenticated app link for PDF/ticket; distribution of static QR as attachment requires explicit security decision. Record safe delivery metadata, not token contents.

**Traceability:** PRD §6.13, ETK-07, BR-08/20/22; X-01–02, C-10/13–14, O-01/06/13.

## 16. Cross-domain time, money and idempotency contracts

| Topic | Contract |
| --- | --- |
| Time | Server clock authoritative; store/compare instants consistently (UTC proposed); display event timezone, Vietnam default Asia/Ho_Chi_Minh. Do not compare local browser time for business authorization. |
| Boundaries | Proposed half-open intervals: `[start,end)`, reservation invalid at `now >= expiresAt`. Test exactly at both boundaries. |
| Money | VND only for orders in MVP; integer minor representation (VND whole units); verify gateway amounts/units per provider contract. No client-trusted arithmetic. |
| Authorization | Verify current rights for protected actions, not merely stale role claim or UX guard; system jobs use trusted actor identity independent of customer session. |
| Concurrency | Re-evaluate guards atomically at business decision point; serialize conflicting allocation/expiry/confirmation/cancel/check-in/settlement operations on shared invariant. |
| Retry | Stable operation identity where mutation can be replayed; retries cannot increase inventory, number of tickets, charge application, admission or payout. |
| Crash recovery | Persist business commitment and recoverable obligation to issue/notify/reconcile; use mechanism defined by SDD. |
| Errors | Return semantic reason, current safe state and recovery action; no stack traces/secret or cross-organization disclosure. API status/field contract TBD. |
| Observability | Record safe correlation IDs, payment/exception references, state transition metrics, outstanding obligations and audit actor/time. |

### 16.1. Semantic rejection examples (API contract candidate)

| Code candidate | Meaning / safe next action |
| --- | --- |
| AUTH_REQUIRED / FORBIDDEN | Sign in or switch permitted workspace; no resource data leaked |
| ORGANIZER_NOT_VERIFIED | Complete verification workflow |
| EVENT_NOT_BOOKABLE / EVENT_CANCELLED | Stop purchase; view updated event status |
| SALE_NOT_STARTED / SALE_CLOSED / SALE_PAUSED | Refresh sale state; do not create hold |
| INSUFFICIENT_INVENTORY / LIMIT_EXCEEDED | Re-select quantities |
| PRICE_CHANGED / STALE_VERSION | Refresh and ask for reconfirmation |
| ORDER_EXPIRED / ORDER_CANCELLED | No new payment for this order; view status |
| PAYMENT_IN_PROGRESS / PAYMENT_UNKNOWN | Query existing attempt; no blind retry |
| PAYMENT_EXCEPTION_OPEN | Show reference; do not issue or promise automatic refund |
| TICKET_ALREADY_USED / INVALID_TICKET / WRONG_EVENT | Deny admission |
| SETTLEMENT_NOT_ELIGIBLE / ALREADY_CLAIMED | Show blocking reason; no payout |
| IDEMPOTENCY_CONFLICT | Same request identity with different data; reject |

Names above are illustrative, not API response schema. Do not use error code as authorization decision on frontend alone.

## 17. Cross-domain acceptance scenarios (Given / When / Then)

All scenarios are required invariant tests unless marked proposed. Use a controllable clock, deterministic provider stub/sandbox and concurrency barriers where needed. Verify persisted outcomes and side effects, not only UI or HTTP code.

| Test ID | Given | When | Then | Rules |
| --- | --- | --- | --- | --- |
| BRS-T01 | Guest selected tickets | Attempts reserve | No reservation/order; authentication required | IDN-R01 |
| BRS-T02 | Organizer A, event B belongs to another org | Access detail/mutation/export | Forbidden/not found, no data leaked | BR-18 |
| BRS-T03 | Staff assigned then revoked | Uses previously valid session to check-in | Denied, no mutation | ORG-R03 |
| BRS-T04 | Organizer pending verification | Submits/publishes event | Denied | BR-01 |
| BRS-T05 | Event approved but not published | Public search | Not listed | BR-02, DSC-R01 |
| BRS-T06 | Review revision changed | Admin approves old revision | Conflict, no invalid publish | EVT-R05 |
| BRS-T07 | Quota sum exceeds event capacity | Update event/config | Rejected atomically | BR-04 |
| BRS-T08 | Sold 60, reserved 20, quota 100 | Change quota to 79 | Reject; inventory intact | BR-21 |
| BRS-T09 | One ticket available | 100 simultaneous reserve requests for one | Exactly one allocation; no negative stock | BR-05 |
| BRS-T10 | Type A enough; B insufficient | Reserve mixed order | No allocation for either line, no orphan order | ORD-R03 |
| BRS-T11 | Active reservation | Retry identical reserve request after response lost | Same logical order, stock decremented once | ORD-R08 |
| BRS-T12 | Active hold | Reload checkout/change tab | Same expiresAt; no extra time | BR-06, ORD-R04 |
| BRS-T13 | Hold expires exactly now | Confirm request | Cannot consume expired allocation; payment exception if collected | BR-06/07, PAY-R08 |
| BRS-T14 | Confirm and expiry compete | Execute concurrently | One terminal reservation result; conservation invariant holds | BR-05/07 |
| BRS-T15 | Hold active with old ticket price | Organizer changes price | Existing order total unchanged | BR-14 |
| BRS-T16 | Free-only order | Confirm with valid hold | CONFIRMED, S increases, no provider request | BR-19 |
| BRS-T17 | Mixed free/paid order | Confirm checkout | Provider amount equals paid line sum; all units issued once | ORD-R02, BR-19 |
| BRS-T18 | Untrusted browser success param | Open payment return route | Pending/unknown; no confirmed order | PAY-R03 |
| BRS-T19 | Invalid webhook signature | Submit callback | No order/payment effect; safe security audit | PAY-R02 |
| BRS-T20 | Valid callback repeated many times including distinct delivery IDs | Process concurrently | One payment application/confirmation; expected ticket count only | BR-09/22 |
| BRS-T21 | Payment SUCCESS | Later FAILED callback | SUCCESS preserved | BR-23 |
| BRS-T22 | Expired order, inventory now sold to another user | Valid delayed success | Record actual funds + exception; no ticket or stock change | PAY-R08 |
| BRS-T23 | One order paid with two distinct provider transactions | Both success received | One applied payment, other actual collection exception | PAY-R06 |
| BRS-T24 | Wrong amount or currency received | Verified callback | No automatic confirm; reconciliation case | PAY-R02/08 |
| BRS-T25 | Attempt pending and customer taps pay again | Requests new attempt | No second attempt until safe eligibility (proposed) | PAY-R05 |
| BRS-T26 | Confirmed order, 3 allocated units, 1 issued before crash | Retry fulfillment | Exactly 3 total eligible ticket IDs, no additional allocation | BR-08/22 |
| BRS-T27 | Ticket issued, email fails | Delivery retries | Order/ticket unchanged; accessible via app | NTF-R01 |
| BRS-T28 | Two assigned scanners scan same ISSUED QR | Concurrent validation | One fresh success, other already used; one admission record | BR-11 |
| BRS-T29 | Scan committed but response lost | Same logical request replayed | Same result, no extra admission or count | CHK-R07 |
| BRS-T30 | Valid credential for another event | Scan at wrong event | Denied; no unrelated event PII | CHK-R03 |
| BRS-T31 | VOIDED ticket shown in old PDF | Scan online | Denied | BR-12 |
| BRS-T32 | Event canceled while reservations, paid orders and scanners active | Process cancellation and races | No sale/admission after cancel; affected money tracked; no invalid QR | BR-20 |
| BRS-T33 | Event canceled, paid customers | Admin records external return without sufficient evidence | Remains unresolved; no fake REFUNDED | PAY-R08, BR-17 |
| BRS-T34 | Fee changed after order creation | Confirm existing order | Snapshot applied according proposed timing; no retroactive fee | BR-15 |
| BRS-T35 | Event not COMPLETED or exception open | Organizer requests payout | Ineligible; no claim/transfer | BR-16 |
| BRS-T36 | Eligible event | Two users submit settlement concurrently | One claim for same entitlement | BR-24 |
| BRS-T37 | Manual transfer status unknown | Admin tries retry | Block second transfer; verify external reference first | FIN-R13 |
| BRS-T38 | Settlement paid and recorded | Duplicate confirm or same ref on another settlement | No duplicate payout/settled total | BR-24 |
| BRS-T39 | Payout destination changed after request | Admin processes settlement | Snapshot not silently replaced; review required (proposed policy) | ORG-R07, FIN-R12 |
| BRS-T40 | Dashboard filtered, table paginated | Compare totals and CSV | Same scoped aggregate and date basis, not page-only totals | RPT-R13/14 |
| BRS-T41 | Event canceled after payment | Notification retry/job reminder | Cancellation notice recoverable; no routine reminder | NTF-R02/05/06 |
| BRS-T42 | Customer account now locked after payment initiated | Gateway sends verified callback | Backend reconciles funds independent of user session; no auth bypass for client | IDN-R04 |
| BRS-T43 | Expired reservation cleanup delayed | Another customer attempts reserve | No double use of released allocation; counters recover once | INV-R05, BR-07 |
| BRS-T44 | Event with zero gross and no payment attempts | Calculate payment success rate | N/A (no denominator), not misleading 100% | RPT-R12 |
| BRS-T45 | Successful order/paid attempt plus extra captured payment | Calculate GMV and collected cash | GMV counted once; extra cash isolated in exception | RPT-R06/07 |
| BRS-T46 | Settlement has estimated gateway fee only | Request or process settlement | Not payable until reconciliation (proposed guard) | FIN-R05/08 |
| BRS-T47 | Provider status confirms full return | Admin records same return twice | Unique evidence/ref, outstanding reduced once | PAY-R08, BR-17 |
| BRS-T48 | Confirmation commits, then service crashes before publishing email/issuance | Recover processing | No lost fulfillment obligation; no duplicate business effects | ETK-R03, NTF-R02 |
| BRS-T49 | Customer attempts cancel CONFIRMED order | Call cancellation directly | Rejected; no inventory or financial change | ORD-R05 |
| BRS-T50 | Cancel event while check-in requests compete | Concurrent execution | Admission committed before cancellation remains historical; none committed after cancellation | EVT-R07, CHK-R04 |

### 17.1. Release gates

- Zero overselling, duplicate allocation consumption, duplicate issued unit, duplicate fresh successful admission, duplicate settlement entitlement.
- Paid order confirmation uses verified authoritative evidence; outstanding collection exceptions remain detectable after restart.
- All role/membership/ownership boundaries enforced server-side, including resource ID tampering and exports.
- Critical workflows cover success, retry, stale state, exact expiry boundary and crash recovery; test concurrency with realistic load profile in Test Strategy.
- Proposed policies must be reviewed; unresolved commercial/legal/payment decisions cannot be silently implemented as final production policy.

## 18. Open decisions and proposed implementation defaults

These decisions are not new approved scope. Product owner/Finance/Security must review. Proposals provide a coherent starting point for System Design and Codex analysis; do not pretend provider or legal obligations have been validated.

| Decision | Related rules | Proposed default / question | Impact |
| --- | --- | --- | --- |
| D-BR-01 | IDN-R05–07 | Verified email gate before booking; password, token lifetimes, reset/revoke and throttling policy to define. | Security Design |
| D-BR-02 | ORG-R05–07 | Verification documents, applicant type, membership permissions, suspend/restore and existing staff behavior; reverify payout changes. | Organizer/Admin |
| D-BR-03 | EVT-R03/05/08/09 | Mandatory submit fields, revision/re-review, cancel authorization before/after publish, suspension impact, exceptional reschedule. | Event lifecycle |
| D-BR-04 | INV-R08/09, ORD-R04/05/10 | 10-minute hold, half-open windows, sales close before start, preserve existing holds on pause, immutable pending lines, safe attempt retry/cancel. | Booking/Checkout |
| D-BR-05 | PAY-R01–11 | Select gateway, signature and receipt semantics, reference identity, query/refund evidence, expiry race clock, retry/reconciliation schedule. | Payment integration |
| D-BR-06 | §9.6, EVT-R07 | Responsibility and policy for manual return on late success, duplicate charge, cancellation, chargeback; operational SLA, legal terms, proof format. | Financial safety / launch blocker |
| D-BR-07 | ETK-R04/08, CHK-R08 | Buyer vs attendee fields, QR threat model/PDF distribution, default check-in start 2h before event; no offline. | Ticket/Check-in |
| D-BR-08 | FIN-R01–07 | Default fee value (not set), scope, snapshot timing, rounding, fee cap, free ticket fee, organizer pays, gateway fee/adjustment/reversal/tax treatment. | Finance/pricing |
| D-BR-09 | FIN-R08–15 | Whole-event manual settlement, zero/negative net, transfer evidence and destination verification, payout failure/unknown retry, separate approval if needed. | Finance/operations |
| D-BR-10 | RPT-R03/05/12/14, DSC-R04/05 | Sales date basis, KPI denominators, adjusted vs historical views, Popular lookback period and search price basis. | Analytics |
| D-BR-11 | NTF-R03/07 | Reminder 24h before start, retry schedule, locale fallback and message templates. | Notification |
| D-BR-12 | ADM-R02/05/07/08, §16 | Audit retention, privacy/consent, support, CSV access, legal obligations, security parameters and data retention. | Governance / launch blocker |

## 19. Traceability summary and Codex handoff

| MVP module | PRD requirement IDs | BRS section | Principal UI/UX screens |
| --- | --- | --- | --- |
| Authentication & Authorization | AUTH-01–11 | §3 | C-05–07/15, X-01–02 |
| Organizer Management | ORG-01–09 | §4 | O-01/11/14, A-03–04 |
| Event Management | EVT-01–14 | §5 | O-03–06, A-05–06 |
| Ticket Type & Inventory | INV-01–13 | §6 | O-07, C-04/08 |
| Event Discovery | DSC-01–11 | §7 | C-01–04 |
| Ticket Booking & Order | ORD-01–15 | §8 | C-08–12, O-08 |
| Online Payment | PAY-01–13 | §9 | C-09–12, A-09–10 |
| QR Code E-Ticket | ETK-01–10 | §10 | C-10/12–14 |
| Check-in | CHK-01–10 | §11 | S-01–06, O-10 |
| Commission & Settlement | STL-01–12 | §12 | O-12–14, A-11–12 |
| Reporting & Dashboard | PRD §6.11 | §13 | O-02/12, S-06, A-01 |
| Admin Portal | ADM-01–15 | §14 | A-01–14 |
| Email Notification | PRD §6.13 | §15 | C-10/13–14, X-01–02, O-01/06/13 |

### 19.1. Coding agent instructions

Read together:

1. `docs/01-product-requirements.md`: product scope and functional requirements (authoritative).
2. `docs/02-business-rules.md`: invariants, transitions, guards, proposed policies and acceptance tests.
3. `docs/03-ui-ux-design.md`: user flows, routes, screens and presentation states.

Before implementation, produce a list of domain boundaries, consistency boundaries, state transitions, data constraints, integration contracts, operational exception handling and open decisions. Map use cases to PRD/BRS IDs. Do not add an independent refund module, seats, discounts, queue, ticket transfer or new native apps. Reuse PRD enums; derived UX/operational labels are not automatically new persisted statuses. Flag conflicts (especially inventory reporting effective vs allocated holds, fee/financial assumptions and cancellation) rather than modifying the baseline silently.

Design critical tests (BRS-T01–50) alongside the implementation. Do not substitute happy-path mocks for payment verification/concurrency tests. Do not select infrastructure/locking/transaction designs without documenting why the invariants are satisfied. Next documents: `04-system-design.md`, `05-database-design.md`, `06-api-specification.md`, `07-security-design.md`, `08-test-strategy.md`, `09-product-backlog.md`, `10-deployment-guide.md`.
