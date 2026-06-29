package com.gridless.storage;

import java.util.List;

public interface GridlessStorage {
    List<PlacedItem> gridless$getPlacedItems();
    void gridless$setPlacedItems(List<PlacedItem> items);
    void gridless$addPlacedItem(PlacedItem item);
    void gridless$removePlacedItem(PlacedItem item);
}
