package org.yearup.data.mysql;

import org.springframework.stereotype.Component;
import org.yearup.data.OrdersDao;
import org.yearup.data.ProfileDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.models.Order;
import org.yearup.models.Profile;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

@Component
public class MySqlOrdersDao extends MySqlDaoBase implements OrdersDao {

    private ProfileDao profileDao;
    private ShoppingCartDao shoppingCartDao;


    public MySqlOrdersDao(DataSource dataSource, ProfileDao profileDao, ShoppingCartDao shoppingCartDao) {
        super(dataSource);
        this.profileDao = profileDao;
        this.shoppingCartDao = shoppingCartDao;
    }

    /**
     * Creates an order from the user's cart. Uses profile for shipping address,
     * saves each cart item as a line item, then clears the cart.
     * @param userId the user id
     * @return the new order
     */
    @Override
    public Order create(int userId) {
        ShoppingCart shoppingCart = shoppingCartDao.getByUserId(userId);
        Profile profile = profileDao.getProfile(userId);

        String orderQuery =
                """
                INSERT INTO orders (user_id, date, address, city, state, zip, shipping_amount)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        String orderLineItemQuery =
                """
                INSERT INTO order_line_items (order_id, product_id, sales_price, quantity, discount)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = getConnection();
             PreparedStatement orderStatement = connection.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime orderTime = LocalDateTime.now();

            orderStatement.setInt(1, userId);
            orderStatement.setTimestamp(2, Timestamp.valueOf(orderTime));
            orderStatement.setString(3, profile.getAddress());
            orderStatement.setString(4, profile.getCity());
            orderStatement.setString(5, profile.getState());
            orderStatement.setString(6, profile.getZip());
            orderStatement.setBigDecimal(7, BigDecimal.ZERO);

            orderStatement.executeUpdate();

            ResultSet generatedKeys = orderStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int orderId = generatedKeys.getInt(1);
                try (PreparedStatement lineStatement = connection.prepareStatement(orderLineItemQuery)) {
                    for (ShoppingCartItem item : shoppingCart.getItems().values()) {
                        lineStatement.setInt(1, orderId);
                        lineStatement.setInt(2, item.getProductId());
                        lineStatement.setBigDecimal(3, item.getProduct().getPrice());
                        lineStatement.setInt(4, item.getQuantity());
                        lineStatement.setBigDecimal(5, item.getDiscountPercent());

                        lineStatement.executeUpdate();
                    }
                }
                shoppingCartDao.deleteCart(userId);

                return new Order(
                        orderId,
                        userId,
                        orderTime,
                        profile.getAddress(),
                        profile.getCity(),
                        profile.getState(),
                        profile.getZip(),
                        BigDecimal.ZERO
                        );
            }else{
                throw new RuntimeException("Failed to create order");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
