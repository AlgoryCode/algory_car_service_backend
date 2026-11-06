package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;

/**
 * Gives every user the UUID of the business (servis işletmesi) they belong to. Until now an installation held a
 * single business, so all existing users share one freshly generated UUID. Written in Java rather than SQL because
 * PostgreSQL and H2 name their UUID generators differently.
 */
public class V5__user_business_uuid extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE users ADD COLUMN business_uuid UUID");
        }
        try (PreparedStatement update = connection.prepareStatement("UPDATE users SET business_uuid = ?")) {
            update.setObject(1, UUID.randomUUID());
            update.executeUpdate();
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE users ALTER COLUMN business_uuid SET NOT NULL");
            statement.execute("CREATE INDEX idx_users_business_uuid ON users (business_uuid)");
        }
    }
}
