package Common.DataBase;

import java.sql.Connection;
import java.sql.Statement;

public class DbIndexBootstrap {

    private static final String[] INDEX_SQL = new String[] {
            "CREATE INDEX IF NOT EXISTS idx_account_user_id ON account(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_auction_state ON auction(state)",
            "CREATE INDEX IF NOT EXISTS idx_auction_item_id ON auction(item_id)",
            "CREATE INDEX IF NOT EXISTS idx_item_owner_user_id ON item(owner_user_id)",
            "CREATE INDEX IF NOT EXISTS idx_item_owner_id_desc ON item(owner_user_id, id DESC)",
            "CREATE INDEX IF NOT EXISTS idx_item_id_desc ON item(id DESC)",
            "CREATE INDEX IF NOT EXISTS idx_item_status ON item(status)",
            "CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)"
    };

    private DbIndexBootstrap() {
    }

    public static void ensureIndexes() {
        try (Connection conn = DbConnection.getConnection();
             Statement st = conn.createStatement()) {
            for (String sql : INDEX_SQL) {
                st.execute(sql);
            }
        } catch (Exception e) {
            System.err.println("Khong the tao index toi uu: " + e.getMessage());
        }
    }
}
