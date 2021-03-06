package pers.msidolphin.mblog.object.po;

import lombok.Getter;
import lombok.Setter;
import pers.msidolphin.mblog.common.annotation.Validation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 回复实体类
 * Created by msidolphin on 2018/3/26.
 */
@Entity
@Setter @Getter
@Table(name = "replies", schema = "mblog")
@Validation
public class Reply extends BasePo{

	@Id
	@Column(name = "id")
	private Long id;				//主键

	@Column(name = "comment_id")
	@NotNull(message = "所属文章id不能为空")
	private String commentId;		//所属文章id

	@Column(name = "user_id")
	private String userId;			//用户id

	@Column(name = "to_user_id")
	@NotNull(message = "回复对象id不能为空")
	private String toUserId;			//回复对象id

	@Column(name = "content")
	@NotEmpty(message = "评论内容不能为空")
	private String content;			//评论内容

	@Column(name = "status")
	private Integer status;			//状态 0-正常 1-删除

	@Column(name = "vote")
	private Integer vote;			//点赞数
}
