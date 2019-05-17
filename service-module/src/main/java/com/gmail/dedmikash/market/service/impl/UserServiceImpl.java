package com.gmail.dedmikash.market.service.impl;

import com.gmail.dedmikash.market.repository.UserRepository;
import com.gmail.dedmikash.market.repository.exception.StatementException;
import com.gmail.dedmikash.market.repository.model.User;
import com.gmail.dedmikash.market.service.UserService;
import com.gmail.dedmikash.market.service.converter.UserConverter;
import com.gmail.dedmikash.market.service.exception.DataBaseConnectionException;
import com.gmail.dedmikash.market.service.exception.QueryFailedException;
import com.gmail.dedmikash.market.service.model.PageDTO;
import com.gmail.dedmikash.market.service.model.UserDTO;
import com.gmail.dedmikash.market.service.util.RandomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.gmail.dedmikash.market.repository.constant.RepositoryErrorMessages.NO_CONNECTION_ERROR_MESSAGE;
import static com.gmail.dedmikash.market.repository.constant.RepositoryErrorMessages.QUERY_FAILED_ERROR_MESSAGE;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserConverter userConverter;
    private final UserRepository userRepository;
    private final RandomService randomService;
    private final PasswordEncoder passwordEncoder;


    public UserServiceImpl(UserConverter userConverter,
                           UserRepository userRepository,
                           RandomService randomService,
                           PasswordEncoder passwordEncoder) {
        this.userConverter = userConverter;
        this.userRepository = userRepository;
        this.randomService = randomService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void saveUser(UserDTO userDTO) {
        userRepository.create(userConverter.fromDTO(userDTO));
    }

    @Override
    public UserDTO readByUsername(String username) {
        try (Connection connection = userRepository.getConnection()) {
            try {
                connection.setAutoCommit(false);
                User user = userRepository.readByUsername(connection, username);
                if (user == null) {
                    return null;
                }
                UserDTO readedUserDTO = userConverter.toDTO(user);
                connection.commit();
                return readedUserDTO;
            } catch (StatementException e) {
                connection.rollback();
                logger.error(e.getMessage(), e);
                throw new QueryFailedException(QUERY_FAILED_ERROR_MESSAGE, e);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new DataBaseConnectionException(NO_CONNECTION_ERROR_MESSAGE, e);
        }
    }

    @Override
    public PageDTO<UserDTO> getUsers(int page) {
        try (Connection connection = userRepository.getConnection()) {
            try {
                PageDTO<UserDTO> users = new PageDTO<>();
                connection.setAutoCommit(false);
                users.setList(getPageOfUsers(page, connection));
                users.setCountOfPages(userRepository.getCountOfUsersPages(connection));
                connection.commit();
                return users;
            } catch (StatementException e) {
                connection.rollback();
                logger.error(e.getMessage(), e);
                throw new QueryFailedException(QUERY_FAILED_ERROR_MESSAGE, e);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new DataBaseConnectionException(NO_CONNECTION_ERROR_MESSAGE, e);
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteUsersByIds(Long[] ids) {
        for (Long id : ids) {
            userRepository.delete(userRepository.findById(id));
        }
    }

    @Override
    public void changeUsersPasswordsByUsernames(String[] usernames) {
        try (Connection connection = userRepository.getConnection()) {
            try {
                connection.setAutoCommit(false);
                Map<String, String> newPasswords = new HashMap<>();
                for (String username : usernames) {
                    newPasswords.put(username, randomService.getNewPassword());
                }
                for (Map.Entry<String, String> entry : newPasswords.entrySet()) {
                    userRepository.changePasswordByUsername(
                            connection,
                            entry.getKey(),
                            passwordEncoder.encode(entry.getValue())
                    );
                }
                for (Map.Entry<String, String> entry : newPasswords.entrySet()) {
                    logger.info("Password of user with username: {} - was changed to: {}. Email has been sent."
                            , entry.getKey(), entry.getValue());
                }
                connection.commit();
            } catch (StatementException e) {
                connection.rollback();
                logger.error(e.getMessage(), e);
                throw new QueryFailedException(QUERY_FAILED_ERROR_MESSAGE, e);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new DataBaseConnectionException(NO_CONNECTION_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void changeUsersRolesById(Map<Long, String> changes) {
        try (Connection connection = userRepository.getConnection()) {
            try {
                connection.setAutoCommit(false);
                userRepository.changeRolesByIds(connection, changes);
                connection.commit();
            } catch (StatementException e) {
                connection.rollback();
                logger.error(e.getMessage(), e);
                throw new QueryFailedException(QUERY_FAILED_ERROR_MESSAGE, e);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new DataBaseConnectionException(NO_CONNECTION_ERROR_MESSAGE, e);
        }
    }

    private List<UserDTO> getPageOfUsers(int page, Connection connection) throws StatementException {
        return userRepository.getUsers(connection, page)
                .stream()
                .map(userConverter::toDTO)
                .collect(Collectors.toList());
    }
}
