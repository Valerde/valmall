package com.sovava.order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 退货原因
 * 
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 18:47:29
 */
@Data
@TableName("oms_order_return_reason")
public class OrderReturnReasonEntity  {
	private static final long serialVersionUID = 1L;
//implements Serializable
	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * 退货原因名
	 */
	private String name;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 启用状态
	 */
	private Integer status;
	/**
	 * create_time
	 */
	private Date createTime;

}
