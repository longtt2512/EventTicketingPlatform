# Execution Plan: Phase 6 — kiểm chứng chất lượng toàn hệ thống

Date: 2026-11-20

## Status

Completed — nghiệm thu phạm vi local đã thu hẹp theo user ngày 2026-11-20.
Gap analysis, concurrency và bốn ứng dụng đã kiểm chứng; phục hồi sự cố,
backup/restore, security/dependency/performance được loại khỏi đợt này theo yêu cầu.

## Outcome

Có ma trận PRD/BRS/UX → bằng chứng thực thi, kiểm chứng mới có thể chạy lại,
các lỗi trong phạm vi đã sửa và hồi quy, báo cáo kết quả/giới hạn rõ ràng.
Không suy ra production readiness từ provider local hoặc số liệu máy phát triển.

## Context and authority

- `docs/01-product-requirements.md` §12–16 và Appendix B: mục tiêu MVP/Phase 6.
- `docs/02-business-rules.md` §17: BRS-T01–50, các invariant MUST.
- `docs/03-ui-ux-design.md` §10–11: accessibility và UX-01–22.
- `docs/04-system-design.md` §16: concurrency, recovery, backup/restore proof.
- `docs/07-security-design.md`: kiểm tra security; OPEN/PROPOSED không tự thành policy.
- `docs/decisions/0001`–`0005` và Phase 1 foundation plan: các lựa chọn local accepted.
- `docs/local-development.md`: ownership, fixture và cách vận hành validation.
- `docs/WORKFLOW.md`, `docs/patterns/encoding-invariants.md`: proof/authority boundary.

## Scope and approach

1. Đối chiếu 50 BRS, 22 UX và 13 module; lưu chiến lược và ma trận bằng chứng.
2. Bổ sung/chạy positive + negative tests cho khoảng trống và các race nghiệp vụ.
3. Không mở rộng audit/nâng dependency, security/performance hay diễn tập phục hồi.
   Giữ nâng Angular 19 đã có trong working tree và xác minh tương thích chức năng.
4. Chạy lại native suite + browser regressions, accessibility/responsive/keyboard/
   lỗi và recovery trên bốn app; sửa lỗi có authority rồi xác minh lại.
5. Tổng hợp báo cáo nghiệm thu, chuyển plan sang completed chỉ khi proof kết thúc.

Ngoài phạm vi theo yêu cầu mới: phục hồi sự cố, backup/restore, audit bảo mật,
dependency remediation, benchmark hiệu năng và mọi công việc production/deploy.
Native suite/luồng hồi quy có sẵn vẫn chạy nguyên vẹn; các assertion recovery có sẵn
không tạo thành cam kết thực hiện một đợt diễn tập recovery mới.

## Decisions

- 2026-11-20: chỉ nghiệm thu phạm vi local đã thu hẹp ở trên. Yêu cầu mới thay thế
  quyền/phạm vi rộng trước đó; không tiếp tục nhánh audit hoặc nâng dependency.

- User đã cho phép active local security/performance và sửa lỗi trong bốn phần.
  Không cần xin lại quyền cho test/fix có authority; production policy OPEN được
  ghi thành giới hạn nghiệm thu, không chọn ngầm.
- Dùng native Gradle/Testcontainers, Angular tests, browser runner hiện hữu.
- Benchmark local là measurement có traffic profile công khai; target PRD không
  tự chứng minh 5.000 concurrent users/HA/RPO bằng laptop.
- Không đổi hệ thống CI vì workflow yêu cầu authorization riêng.

## Risks and recovery

- Nhiều source đang untracked: baseline source ở `/tmp/etp-phase6-baseline-*.tar.gz`,
  đường dẫn cụ thể trong protected `.runtime/phase6/baseline.json`; không reset/clean.
- Giữ credentials/key trong ignored `.runtime/`; output công khai chỉ dữ liệu tổng hợp.
- Kiểm tra process/container ownership; failure rehearsal dùng tài nguyên tách biệt
  hoặc worker validation do lượt này quản lý. Giữ audit/order/evidence synthetic.
- Backup/restore giữ key và artifact, phục hồi vào database riêng; không drop nguồn.
- Không nới assertion, timeout nghiệp vụ, bundle budget để làm xanh kết quả.

## Progress

- [x] Đọc workflow, runbook, accepted decisions và baseline Phase 5.
- [x] Gap analysis và test strategy/traceability: docs/08-test-strategy.md, 50 BRS/22 UX/13 modules.
- [x] Concurrency: booking, confirmation/expiry, payment, admission, settlement; native suite passed.
- [x] Recovery/restore, security/dependency/performance: loại khỏi phạm vi theo user.
- [x] Bốn app: regression, a11y, keyboard, responsive, lỗi tải và thử lại.
- [x] Final verification và báo cáo nghiệm thu: docs/qa/phase6-local-acceptance.md.

## Validation

Native: `./backend/gradlew -p backend test bootJar`,
`npm --prefix frontend run test:unit`, `npm --prefix frontend run build:all`.
Browser: Phase 1–5, payment methods, transaction edges và Phase 6 proof mới.
Chi tiết lệnh, environment, thời gian, positive/negative findings và artifact sẽ
được cập nhật theo kết quả thực tế; report cũ không được tính là rerun.

## Result

Hoàn tất theo phạm vi local đã chốt: 193 backend tests, 148 frontend tests,
bốn builds và 733 browser/API checks. Báo cáo cuối: docs/qa/phase6-local-acceptance.md.
Evidence: output/playwright/phase6-validation-summary.json.

## Checkpoint — revised local scope

- Native baseline rerun passed; Angular 19: 146 tests and four builds passed.
- Phase 1/2/3/edges/4 rerun: 88/65/80/8/76 checks. Phase 1 runner now waits for
  first Admin users page before paging; prior failure was a synchronization error.
- Added four Commerce tests for mixed orders, locked buyer callback, confirmed
  cancellation rejection and 12 simultaneous payment facts. Focused suite passed.
- New browser proof confirms mixed order → exactly three tickets and one winner
  for the final seat. It exposed a UX dead end: selected sold-out quantity could
  not be reduced. Minus control now permits removal; final rerun pending.
- Small login help text increased to 12px after rendered detector finding.
- Phase 5 rerun passed 87 checks and real MailHog delivery; QR/card configuration
  rerun passed 66 checks, original settings restored.
- Real keyboard proof exposed the shared skip link resolving against base href,
  changing routes. It now focuses main without route navigation; final four-app
  keyboard rerun pending. Baseline browser result reached 72 checks before this defect.
- Full requirement matrix/strategy is in docs/08-test-strategy.md; final results
  and remaining limitations will be recorded after verification.

- Expanded native proof covers held price after owner changes price, zero-sale
  payment rate N/A, and duplicate payout reference across distinct event claims.
  Frontend now has 148 passing tests including sold-out selection removal and
  skip-link focus. Final browser inspection is running against rebuilt assets.

- Final four-app run reached 249 checks / 31 surfaces with no JS or unexpected
  5xx and no simple-background contrast findings. Keyboard traversal reached
  outside the dialog at its final action; added explicit first/last Tab wrap
  (UX §10.1 focus trap). Full final browser run remains pending after rebuilding.

- Final native backend: 193 tests, zero failures/errors/skips. Frontend: 148
  tests; 88.56% lines, 79.98% statements, 59.95% branches, 76.77% functions.
  Four builds passed with unchanged budgets. Final full Phase 6 browser rerun
  is now testing the dialog Tab wrap against these exact assets.

## Final completion checkpoint

- Phase 6 cuối: 259 checks, 31 surfaces, 0 JS/unexpected 5xx/contrast findings
  trong phạm vi sampler. Đã xem render desktop/mobile và kiểm chứng keyboard.
- Tổng fresh runtime checks: 733; native 193 backend + 148 frontend; bốn builds.
- Đã sửa sold-out decrement, skip-link routing/focus, dialog Tab wrap và login
  help text; native và browser regression đều passed.
- Mười backend health UP, bốn login URL 200; runtime jars khớp build hashes.
- Giữ local stack và dữ liệu synthetic để nghiệm thu. Hạng mục user loại khỏi
  phạm vi không được ghi là đã kiểm chứng. Báo cáo liệt kê warning và coverage
  theo từng metric, không che hoặc nới ngưỡng.
- Chuyển plan sang completed sau khi summary script xác nhận evidence sạch.
