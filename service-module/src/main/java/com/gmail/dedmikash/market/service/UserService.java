package com.gmail.dedmikash.market.service;

import com.gmail.dedmikash.market.service.model.PageDTO;
import com.gmail.dedmikash.market.service.model.UserDTO;

import java.util.Map;

public interface UserService {
    void saveUser(UserDTO userDTO);

    UserDTO readByUsername(String username);

    PageDTO<UserDTO> getUsers(int page);

    void deleteUsersByIds(Long[] ids);

    void changeUsersPasswordsByUsernames(Long[] ids);

    void changeUsersRolesById(Map<Long, String> changes);

    UserDTO getUserById(Long id);

    int updateUserProfileAndPassword(UserDTO userDTO, String oldPassword, String newPassword);
}
