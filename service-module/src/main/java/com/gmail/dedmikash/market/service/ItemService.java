package com.gmail.dedmikash.market.service;

import com.gmail.dedmikash.market.service.model.ItemDTO;
import com.gmail.dedmikash.market.service.model.PageDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ItemService {
    @Transactional
    void saveItem(ItemDTO itemDTO);

    @Transactional
    ItemDTO getItemById(Long id);

    @Transactional
    List<ItemDTO> getAllItems();

    void deleteItemById(Long id);

    PageDTO<ItemDTO> getItems(int page);
}
