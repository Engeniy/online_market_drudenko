package com.gmail.dedmikash.market.service.impl;

import com.gmail.dedmikash.market.repository.ArticleRepository;
import com.gmail.dedmikash.market.repository.exception.StatementException;
import com.gmail.dedmikash.market.repository.model.Article;
import com.gmail.dedmikash.market.service.ArticleService;
import com.gmail.dedmikash.market.service.converter.ArticleConverter;
import com.gmail.dedmikash.market.service.exception.DataBaseConnectionException;
import com.gmail.dedmikash.market.service.exception.QueryFailedException;
import com.gmail.dedmikash.market.service.model.ArticleDTO;
import com.gmail.dedmikash.market.service.model.CommentDTO;
import com.gmail.dedmikash.market.service.model.PageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import static com.gmail.dedmikash.market.repository.constant.RepositoryErrorMessages.NO_CONNECTION_ERROR_MESSAGE;
import static com.gmail.dedmikash.market.repository.constant.RepositoryErrorMessages.QUERY_FAILED_ERROR_MESSAGE;

@Service
public class ArticleServiceImpl implements ArticleService {
    private static final Logger logger = LoggerFactory.getLogger(ArticleServiceImpl.class);
    private final ArticleConverter articleConverter;
    private final ArticleRepository articleRepository;
    private static final int MAX_TEXT_LENGTH = 200;


    public ArticleServiceImpl(ArticleConverter articleConverter, ArticleRepository articleRepository) {
        this.articleConverter = articleConverter;
        this.articleRepository = articleRepository;
    }

    @Override
    @Transactional
    public void saveArticle(ArticleDTO articleDTO) {
        articleDTO.setViews(0L);
        Article article = articleConverter.fromDTO(articleDTO);
        articleRepository.create(article);
    }

    @Override
    @Transactional
    public ArticleDTO getArticleById(Long id) {
        Article article = articleRepository.findById(id);
        article.setViews(article.getViews() + 1);
        articleRepository.update(article);
        return articleConverter.toDTO(articleRepository.findById(id));
    }

    @Override
    @Transactional
    public List<ArticleDTO> getAllArticles() {
        return articleRepository.findAll().stream()
                .map(articleConverter::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PageDTO<ArticleDTO> getArticles(int page, String sort, String order) {
        try (Connection connection = articleRepository.getConnection()) {
            try {
                connection.setAutoCommit(false);
                PageDTO<ArticleDTO> articles = new PageDTO<>();
                List<ArticleDTO> articleDTOS = getPageOfArticles(connection, page, sort, order);
                cutLongTexts(articleDTOS);
                articles.setList(articleDTOS);
                articles.setCountOfPages(articleRepository.getCountOfArticlesPages(connection));
                connection.commit();
                return articles;
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
    @Transactional
    public void delete(Long id) {
        Article article = articleRepository.findById(id);
        if (article != null && !article.isDeleted()) {
            articleRepository.delete(articleRepository.findById(id));
        }
    }

    @Override
    @Transactional
    public ArticleDTO addCommentToArticle(ArticleDTO articleDTO, CommentDTO commentDTO) {
        commentDTO.setCreated(new Timestamp(System.currentTimeMillis()));
        commentDTO.setArticleID(articleDTO.getId());
        articleDTO.getComments().add(0, commentDTO);
        Article article = articleConverter.fromDTO(articleDTO);
        articleRepository.update(article);
        return articleConverter.toDTO(articleRepository.findById(articleDTO.getId()));
    }

    private List<ArticleDTO> getPageOfArticles(
            Connection connection,
            int page,
            String sort,
            String order) throws StatementException {
        return articleRepository.getArticles(connection, page, sort, order)
                .stream()
                .map(articleConverter::toDTO)
                .collect(Collectors.toList());
    }

    private void cutLongTexts(List<ArticleDTO> articleDTOS) {
        articleDTOS.stream()
                .filter(articleDTO -> articleDTO.getText().length() > MAX_TEXT_LENGTH)
                .forEach(articleDTO -> articleDTO.setText(articleDTO.getText().substring(
                        0, articleDTO.getText().substring(0, MAX_TEXT_LENGTH).lastIndexOf(" ")
                ).concat("...")));
    }
}
