package ntnu.idatt2106.backend.service;

import lombok.RequiredArgsConstructor;
import ntnu.idatt2106.backend.model.*;
import ntnu.idatt2106.backend.model.requests.EditGroceryRequest;
import ntnu.idatt2106.backend.model.requests.SaveGroceryRequest;
import ntnu.idatt2106.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShoppingListService {
    private final ShoppingListRepository shoppingListRepository;

    private final RefrigeratorRepository refrigeratorRepository;
    private final GroceryListRepository groceryListRepository;
    private final SubCategoryRepository subCategoryRepository;

    private final GroceryRepository groceryRepository;
    private Logger logger = LoggerFactory.getLogger(ShoppingListService.class);

    public long createShoppingList(long refrigeratorId) {
        logger.info("Creating shopping list for refrigerator with id {}", refrigeratorId);
        Optional<Refrigerator> refrigerator = refrigeratorRepository.findById(refrigeratorId);

        if (refrigerator.isPresent()) {
            logger.info("Found refrigerator for refrigerator id {}", refrigeratorId);
            ShoppingList shoppingList = new ShoppingList();
            shoppingList.setRefrigerator(refrigerator.get());

            shoppingListRepository.save(shoppingList);
            logger.info("Created shopping list with id {}", shoppingList.getId());
            return shoppingList.getId();
        }
        logger.info("Could not find a matching refrigerator for refrigerator id {}", refrigeratorId);
        return -1;
    }

    public List<Grocery> getGroceries(long shoppingListId) {
        logger.info("Retrieving groceries from the database");
        List<Grocery> groceries = groceryListRepository.findByShoppingListId(shoppingListId);
        if (groceries.isEmpty()) {
            logger.info("Received no groceries from the database");
        }
        logger.info("Received groceries from the database");
        return groceries;
    }

    public List<Grocery> getGroceries(long shoppingListId, long subCategoryId) {
        logger.info("Retrieving groceries from the database");
        List<Grocery> groceries = groceryListRepository.findByShoppingListIdAndSubCategoryId(shoppingListId, subCategoryId);
        if (groceries.isEmpty()) {
            logger.info("Received no groceries from the database");
        }
        logger.info("Received groceries from the database");
        return groceries;
    }

    public List<Category> getCategories(long shoppingListId) {
        logger.info("Retrieving categories from shopping list with id {}", shoppingListId);
        List<Category> categories = groceryListRepository.findCategoryByShoppingListId(shoppingListId);
        if (categories.isEmpty()) {
            logger.info("Received no categories from shopping list with id {}", shoppingListId);
        }
        logger.info("Received categories from the database for shopping list with id {}", shoppingListId);
        return categories;
    }

    public Optional<GroceryShoppingList> editGrocery(EditGroceryRequest groceryRequest) {
        logger.info("Editing grocery with id: {} to shopping list with id {}", groceryRequest.getId(), groceryRequest.getShoppingListId());

        Optional<GroceryShoppingList> groceryShoppingList = groceryListRepository.findById(groceryRequest.getId());
        if (groceryShoppingList.isPresent()) {
            logger.info("Found grocery in the shopping list");
            logger.info("Data for the grocery request: id {}, quantity {}, isRequested {}, shoppingListId {}",
                    groceryRequest.getId(), groceryRequest.getQuantity(), groceryRequest.isRequested(), groceryRequest.getShoppingListId() );//todo: delete
            groceryShoppingList.get().setRequest(groceryRequest.isRequested());
            groceryShoppingList.get().setQuantity(groceryRequest.getQuantity());

            logger.info("Edit grocery in the grocery list");
            return Optional.of(groceryListRepository.save(groceryShoppingList.get()));
        }
        logger.info("Could not find the grocery in the shopping list");
        return Optional.empty();
    }

    public Optional<GroceryShoppingList> saveGrocery(SaveGroceryRequest groceryRequest) {
        logger.info("Saving grocery: {} to shopping list with id {}", groceryRequest.getName(), groceryRequest.getShoppingListId());
        Optional<ShoppingList> shoppingList = shoppingListRepository.findById(groceryRequest.getShoppingListId());
        if (shoppingList.isPresent()) {
            logger.info("Found shopping list for shopping list id {}", shoppingList.get().getId());

            logger.info("Found shopping list for shopping list id {}", shoppingList.get().getId());


            Optional<SubCategory> subCategory = subCategoryRepository.findById(groceryRequest.getSubCategoryId());
            GroceryShoppingList groceryShoppingList = new GroceryShoppingList();


            if (subCategory.isPresent()) {
                logger.info("Found sub category");
                Grocery grocery = Grocery.builder().name(groceryRequest.getName()).groceryExpiryDays(groceryRequest.getGroceryExpiryDays())
                        .description(groceryRequest.getDescription()).subCategory(subCategory.get()).build();
                groceryRepository.save(grocery);
                logger.info("Created grocery with name {}", grocery.getName());

                groceryShoppingList.setGrocery(grocery);
                groceryShoppingList.setShoppingList(shoppingList.get());
                groceryShoppingList.setQuantity(groceryRequest.getQuantity());
                groceryShoppingList.setRequest(groceryRequest.isRequested());
            }
            logger.info("Saved new grocery to the grocery list");

            return Optional.of(groceryListRepository.save(groceryShoppingList));
        }
        logger.info("Could not find a shopping list with id {}", groceryRequest.getShoppingListId());
        logger.info("Could not find a shopping list with id {}", groceryRequest.getShoppingListId());
        return Optional.empty();
    }
}
