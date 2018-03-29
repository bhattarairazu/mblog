package pers.msidolphin.mblog.portal.controller;

import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pers.msidolphin.mblog.common.ServerResponse;
import pers.msidolphin.mblog.object.dto.ArticleDto;
import pers.msidolphin.mblog.object.query.ArticleQuery;
import pers.msidolphin.mblog.service.ArticleService;

import java.text.ParseException;

/**
 * 门户 文章控制器
 * Created by msidolphin on 2018/3/26.
 */
@RestController("portalArticlesController")
@RequestMapping("/articles")
public class ArticlesController {

	@Autowired
	private ArticleService articleService;

	/**
	 * 文章列表
	 * @param query {ArticleQuery} 文章查询对象
	 * @return ServerResponse<?>
	 */
	@GetMapping("")
	public ServerResponse<?> list(ArticleQuery query) throws ParseException {
		PageInfo<ArticleDto> page = articleService.getArticles(query);
		return ServerResponse.success(page);
	}

	/**
	 * 文章详情
	 * @param id {String} 文章id
	 * @return ServerResponse<?>
	 */
	@GetMapping("/detail/{id}")
	public ServerResponse<?> detail(@PathVariable String id) {
		return ServerResponse.success(articleService.getArticleDetail(id));
	}
}
