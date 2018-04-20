package pers.msidolphin.mblog.service;

import org.apache.commons.beanutils.BeanUtils;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pers.msidolphin.mblog.common.enums.ResponseCode;
import pers.msidolphin.mblog.exception.InvalidParameterException;
import pers.msidolphin.mblog.exception.ServiceException;
import pers.msidolphin.mblog.helper.JsonHelper;
import pers.msidolphin.mblog.helper.Util;
import pers.msidolphin.mblog.model.repository.ArticleRepository;
import pers.msidolphin.mblog.object.index.ArticleIndexTemplate;
import pers.msidolphin.mblog.object.po.Article;
import pers.msidolphin.mblog.service.inter.ISearchService;

import java.util.List;


/**
 * Created by msidolphin on 2018/4/20.
 */
@Service
public class ElasticsearchSearchService implements ISearchService {

	private static final Logger log = LoggerFactory.getLogger(ElasticsearchSearchService.class);

	@Autowired
	private ArticleService articleService;

	@Autowired
	private ArticleRepository articleRepository;

	@Autowired
	private TransportClient elasticsearchClient;

	@Override
	public boolean index(String id) {
		if(Util.isEmpty(id)) throw new InvalidParameterException("article id cannot be null");
		//根据id从数据库中获取文章信息
		Article article = articleRepository.findByArticleIdAndIsDelete(id, 0);
		if(Util.isEmpty(article)) {
			log.warn("article {} is not present...", id);
			throw new ServiceException(ResponseCode.NOT_FOUND);
		}
		//将文章po映射到文章索引模板
		ArticleIndexTemplate indexTemplate = new ArticleIndexTemplate();
		try {
			BeanUtils.copyProperties(indexTemplate, article);
		} catch (Exception e) {
			throw new ServiceException(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		//获取文章标签
		List<String> tagList =  articleService.getArticleTagsToList(article.getArticleId());
		indexTemplate.setTag(tagList);

		//查询es中是否存在该文章doc
		SearchRequestBuilder searchRequestBuilder =
				elasticsearchClient.prepareSearch(ArticleIndexTemplate.INDEX_NAME)
				.setTypes(ArticleIndexTemplate.TYPE)
				.setQuery(QueryBuilders.termQuery(ArticleIndexTemplate.ARTICLE_ID, indexTemplate.getArticleId()));
		SearchResponse response = searchRequestBuilder.get();
		boolean success = false;
		long hits = response.getHits().getTotalHits();
		if(hits == 0) {
			//创建文档
			success = create(indexTemplate);
		}else if(hits == 1) {
			//更新文档
			success = update(indexTemplate);
		}else if(hits > 0){
			log.warn("There are multiple replicas of article {}...", indexTemplate.getArticleId());
			//删除文章
			success = delete(hits, indexTemplate.getArticleId());
			//如果删除成功则创建文档
			if(success) success = create(indexTemplate);
		}

		//判断操作是否成功
		if(!success) throw new ServiceException(ResponseCode.INTERNAL_SERVER_ERROR, "index failed");
		return success;
	}


	@Override
	public boolean remove(String id) {
		return false;
	}

	/**
	 * 创建一个索引文档
	 * @param indexTemplate 索引模板
	 * @return {boolean}
	 */
	private boolean create(ArticleIndexTemplate indexTemplate) {
		IndexResponse response = elasticsearchClient.prepareIndex(ArticleIndexTemplate.INDEX_NAME, ArticleIndexTemplate.TYPE, indexTemplate.getArticleId())
				.setSource(JsonHelper.object2String(indexTemplate), XContentType.JSON).get();
		if(response.status().getStatus() == ResponseCode.OK.getCode()) {
			log.debug("create article index {} success...", indexTemplate.getArticleId());
			return true;
		}
		log.warn("create article index {} failed...", indexTemplate.getArticleId());
		return false;
	}

	/**
	 * 更新索引文档
	 * @param indexTemplate 索引模板
	 * @return {boolean}
	 */
	private boolean update(ArticleIndexTemplate indexTemplate) {
		UpdateResponse response = elasticsearchClient.prepareUpdate(ArticleIndexTemplate.INDEX_NAME, ArticleIndexTemplate.TYPE, indexTemplate.getArticleId())
				.setDoc(JsonHelper.object2String(indexTemplate), XContentType.JSON).get();
		if(response.status().getStatus() == ResponseCode.OK.getCode()) {
			log.debug("update article index {} success...", indexTemplate.getArticleId());
			return true;
		}
		log.warn("update article index {} failed...", indexTemplate.getArticleId());
		return false;
	}

	/**
	 * 删除索引文档
	 * @param hits 期望删除数
	 * @param id   索引id
	 * @return	{boolean}
	 */
	private boolean delete(long hits, String id) {
		BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(elasticsearchClient)
				.filter(QueryBuilders.termQuery(ArticleIndexTemplate.ARTICLE_ID, id))
				.source(ArticleIndexTemplate.INDEX_NAME).get();
		log.debug("delete article index {} success ", id);
		if(hits != response.getDeleted()) {
			log.warn("expected delete {}, but deleted {}", hits, response.getDeleted());
			return false;
		}
		return true;
	}
}
