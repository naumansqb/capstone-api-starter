package org.yearup.data.mysql;

import org.springframework.stereotype.Component;
import org.yearup.data.ProductDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.models.Product;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MySqlShoppingCartDao extends MySqlDaoBase implements ShoppingCartDao {

    private ProductDao productDao;

    public MySqlShoppingCartDao(DataSource dataSource, ProductDao productDao) {
        super(dataSource);
        this.productDao = productDao;
    }

    /**
     * Gets the cart for a user.
     * @param userId the user id
     * @return the cart with all items
     */
    @Override
    public ShoppingCart getByUserId(int userId) {
        ShoppingCart shoppingCart = new ShoppingCart();
        String sql = "SELECT * FROM shopping_cart WHERE user_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    ShoppingCartItem item = mapRow(resultSet);
                    shoppingCart.add(item);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return shoppingCart;
    }

    /**
     * Adds a product to cart. If already there, bumps quantity by 1.
     * @param userId the user id
     * @param productId the product to add
     */
    @Override
    public void addItem(int userId, int productId) {
        ShoppingCart cart = getByUserId(userId);

        if (cart.contains(productId)) {
            ShoppingCartItem item = cart.get(productId);
            int newQuantity = item.getQuantity() + 1;
            updateQuantity(userId, productId, newQuantity);
        } else {
            String sql = "INSERT INTO shopping_cart(user_id, product_id) VALUES (?, ?)";
            try (Connection connection = getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, userId);
                preparedStatement.setInt(2, productId);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Updates quantity of an item in the cart.
     * @param userId the user id
     * @param productId the product id
     * @param quantity the new quantity
     */
    @Override
    public void updateQuantity(int userId, int productId, int quantity) {
        String sql = """
                UPDATE shopping_cart
                SET quantity = ?
                WHERE user_id = ? AND product_id = ?
                """;
        try(Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql)){
            preparedStatement.setInt(1,quantity);
            preparedStatement.setInt(2,userId);
            preparedStatement.setInt(3,productId);

            preparedStatement.executeUpdate();
        }catch (SQLException e){
            throw new RuntimeException(e);
        }

    }

    /**
     * Clears all items from the user's cart.
     * @param userId the user id
     */
    @Override
    public void deleteCart(int userId) {
        String sql = "DELETE FROM shopping_cart WHERE user_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Maps a row to a ShoppingCartItem.
     * @param row the result set row
     * @return the ShoppingCartItem
     * @throws SQLException if something goes wrong
     */
    private ShoppingCartItem mapRow(ResultSet row) throws SQLException {
        int productId = row.getInt("product_id");
        int quantity = row.getInt("quantity");

        Product product = productDao.getById(productId);

        ShoppingCartItem item = new ShoppingCartItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }
}
