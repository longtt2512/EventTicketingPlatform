import { HttpErrorResponse } from '@angular/common/http';

const messages: Record<string, string> = {
  INVALID_CREDENTIALS: 'Email hoặc mật khẩu chưa đúng. Vui lòng kiểm tra và thử lại.',
  EMAIL_NOT_VERIFIED: 'Hãy xác thực email trước khi đăng nhập.',
  AUTH_REQUIRED: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  ACCOUNT_NOT_ACTIVE: 'Tài khoản hiện không hoạt động. Vui lòng liên hệ quản trị viên.',
  INVALID_VERIFICATION_TOKEN: 'Liên kết xác thực không hợp lệ, đã dùng hoặc đã hết hạn.',
  INVALID_RESET_TOKEN: 'Liên kết đặt lại mật khẩu không hợp lệ, đã dùng hoặc đã hết hạn. Hãy yêu cầu liên kết mới.',
  FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này.',
  RESOURCE_NOT_FOUND: 'Không tìm thấy dữ liệu trong phạm vi quyền của bạn.',
  VALIDATION_ERROR: 'Thông tin chưa hợp lệ. Vui lòng kiểm tra các trường trong biểu mẫu.',
  APPLICATION_ALREADY_PENDING: 'Bạn đã có hồ sơ đang chờ duyệt.',
  INVALID_APPLICATION_STATE: 'Trạng thái hồ sơ đã thay đổi. Hãy tải lại dữ liệu.',
  ORGANIZATION_SUSPENDED: 'Tổ chức đang tạm ngưng. Bạn có thể xem thông tin nhưng không thể thay đổi.',
  STALE_VERSION: 'Dữ liệu đã được người khác cập nhật. Hãy tải lại trước khi thao tác.',
  RATE_LIMITED: 'Bạn đã thử quá nhiều lần. Vui lòng chờ rồi thử lại.',
  OWNER_MEMBERSHIP_REQUIRED: 'Không thể xóa quyền của chủ sở hữu tổ chức.',
  MEMBER_NOT_ELIGIBLE: 'Thành viên phải có tài khoản đang hoạt động và đã xác thực email.',
  OWNER_REQUIRED: 'Chỉ chủ sở hữu tổ chức được thực hiện thao tác này.',
  OWNER_PROTECTED: 'Không thể thay đổi hoặc thu hồi thành viên chủ sở hữu.',
  MEMBER_EXISTS: 'Người dùng đã là thành viên đang hoạt động.',
  PERMISSION_REQUIRED: 'Bạn chưa được cấp quyền cho thao tác này.',
  VERSION_CONFLICT: 'Thông tin đã thay đổi. Hãy tải lại trước khi lưu.',
  INVALID_DOCUMENT: 'Tài liệu phải là PDF, PNG hoặc JPEG hợp lệ, tối đa 5 MB.',
  DOCUMENT_TOO_LARGE: 'Tài liệu vượt quá giới hạn 5 MB.',
  APPLICATION_EXISTS: 'Bạn đã có hồ sơ. Hãy tải lại để xem hoặc gửi lại hồ sơ đó.',
  DOCUMENT_STORAGE_UNAVAILABLE: 'Chưa thể lưu tài liệu. Vui lòng thử lại.',
  INVALID_FILE: 'Tài liệu phải là PDF, PNG hoặc JPEG và không quá 5 MB.',
};

export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'Không thể kết nối máy chủ. Kiểm tra mạng và thử lại. Thông tin bạn nhập vẫn được giữ.';
    const problem = error.error as { code?: string } | null;
    return messages[problem?.code ?? ''] ?? 'Không thể hoàn tất thao tác. Vui lòng thử lại.';
  }
  return 'Không thể hoàn tất thao tác. Vui lòng thử lại.';
}
