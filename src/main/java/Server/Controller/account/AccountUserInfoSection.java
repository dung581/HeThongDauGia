package Server.Controller.account;

import Common.DataBase.entities.Account;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;

// Nhóm hiển thị thông tin tài khoản cá nhân của Bidder/Seller.
final class AccountUserInfoSection {
    private final AccountController controller;

    AccountUserInfoSection(AccountController controller) {
        this.controller = controller;
    }

    // Hiển thị view tài khoản cá nhân, gồm số dư và lịch sử stake.
    void showUserInfoView() {
        if (controller.adminPane != null) {
            controller.adminPane.setManaged(false);
            controller.adminPane.setVisible(false);
        }
        controller.setVisibleManaged(controller.accountSummaryPane, true);
        controller.setVisibleManaged(controller.stakePane, true);
        if (controller.userInfoPane != null) {
            controller.userInfoPane.setManaged(true);
            controller.userInfoPane.setVisible(true);
        }

        long userId = UserAccount.getUserId();
        String username = UserAccount.getCurrentUsername();
        String fullname = UserAccount.getCurrentFullname();
        UserRole role = UserAccount.getCurrentRole();

        Account account = null;
        try {
            account = controller.accountService.getBalance(userId);
        } catch (Exception ignored) {
        }
        controller.lblTitle.setText("Thông tin tài khoản");
        controller.setLabel(controller.lblBreadcrumb, "BidNow / My Account");
        controller.setLabel(controller.lblSubtitle, "Theo doi so du, tien dang khoa va lich su stake cua tai khoan.");
        controller.setLabel(controller.lblFooter, "BidNow Desktop | JavaFX 21 | Account workspace");
        controller.lblUsername.setText(username == null ? "" : username);
        controller.lblFullname.setText(fullname == null ? "" : fullname);
        controller.lblRole.setText(role == null ? "" : role.name());

        controller.configureStakeList();
        controller.configureStakeFilters();
        controller.loadStakeDataAsync();

        if (account != null) {
            long balance = account.getBalance();
            long locked = account.getLocked_balance();
            controller.lblBalance.setText(String.valueOf(balance));
            controller.lblLocked.setText(String.valueOf(locked));
            controller.lblAvailable.setText(String.valueOf(balance - locked));
        } else {
            controller.lblBalance.setText("0");
            controller.lblLocked.setText("0");
            controller.lblAvailable.setText("0");
        }
    }
}
