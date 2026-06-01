package Server.Controller.account;

import Common.Enum.UserRole;
import Common.Model.user.UserAccount;

// Nhóm cấu hình điều hướng hiển thị theo role hiện tại.
final class AccountRoleUiSection {
    private final AccountController controller;

    AccountRoleUiSection(AccountController controller) {
        this.controller = controller;
    }

    // Ẩn/hiện các mục điều hướng theo role hiện tại của user.
    void configureRoleUi() {
        UserRole role = UserAccount.getCurrentRole();
        controller.setVisibleManaged(controller.browseItemsNav, role == UserRole.ADMIN);
        controller.setVisibleManaged(controller.uploadItemNav, role == UserRole.SELLER);
        controller.setVisibleManaged(controller.depositNav, role == UserRole.BIDDER);
        controller.setVisibleManaged(controller.accountSection, role == UserRole.SELLER || role == UserRole.BIDDER);
        if (controller.accountNavButton != null) {
            controller.accountNavButton.setText(role == UserRole.ADMIN ? "Account Management" : "My Account");
        }
    }
}
