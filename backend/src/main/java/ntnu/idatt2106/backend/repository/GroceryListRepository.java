package ntnu.idatt2106.backend.repository;

import ntnu.idatt2106.backend.model.Category;
import ntnu.idatt2106.backend.model.Grocery;
import ntnu.idatt2106.backend.model.GroceryShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroceryListRepository extends JpaRepository<GroceryShoppingList, Long> {
    @Query(value = "SELECT gsl.grocery" +
            " FROM GroceryShoppingList gsl, Grocery g" +
            " WHERE gsl.grocery.id = g.id AND gsl.id = :shoppingListId")
    List<Grocery> findByShoppingListId(@Param("shoppingListId")Long shoppingListId);

    @Query(value = "SELECT gsl.grocery" +
            " FROM GroceryShoppingList gsl, Grocery g" +
            " WHERE gsl.grocery.id = g.id AND gsl.id = :shoppingListId AND g.subCategory.id = :subCategoryId")
    List<Grocery> findByShoppingListIdAndSubCategoryId(@Param("shoppingListId")Long shoppingListId, @Param("subCategoryId")Long subCategoryId);

    // SELECT * FROM Grocery_List gsl, Grocery g, Sub_Category sc WHERE gsl.id = g.id AND sc.id = g.sub_category_id
    @Query(value = "SELECT sc.category" +
            " FROM GroceryShoppingList gsl, Grocery g, SubCategory sc" +
            " WHERE gsl.grocery.id = g.id AND gsl.id = :shoppingListId AND sc.id = g.subCategory.id")
    List<Category> findCategoryByShoppingListId(@Param("shoppingListId")Long shoppingListId);
}

