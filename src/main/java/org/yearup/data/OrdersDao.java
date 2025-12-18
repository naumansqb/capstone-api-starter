package org.yearup.data;

import org.yearup.models.Order;

public interface OrdersDao {
    Order create(int userId);
}
