package com.fuli;

import com.alibaba.fastjson.JSON;
import com.fuli.pojo.User;
import org.apache.lucene.search.TermQuery;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.AcknowledgedResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


//elasticsearch 高级客户端restful api 使用实例
@SpringBootTest
class FuliEsApiApplicationTests {
	@Autowired
	private RestHighLevelClient restHighLevelClient;

	//创建索引,注意，创建索引前面不加/
	@Test
	void testCreateIndex() throws IOException{
		//创建索引请求
		CreateIndexRequest request = new CreateIndexRequest("fuli_test1");
		//客户端执行请求IndicesClient, 返回响应
		CreateIndexResponse response = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
		System.out.println(response);
	}

	//查询索引
	@Test
	void testExistIndex() throws IOException{
		GetIndexRequest request = new GetIndexRequest().indices("fuli_test1");
		boolean exists = restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
		System.out.println(exists);
	}

	//删除索引，这个无法运行，先跳过，后续处理
//	@Test
//	void testDeleteIndex() throws IOException{
//		DeleteIndexRequest request = new DeleteIndexRequest("fuli_test1");
//		AcknowledgedResponse deleteIndexResponse = restHighLevelClient.indices().delete(request, RequestOptions.DEFAULT);
//		System.out.println(deleteIndexResponse);
//	}

	//添加文档
	@Test
	void testCreateDocument() throws IOException {
		//创建对象
		User user = new User("福利", 1);
		//创建请求
		IndexRequest request = new IndexRequest("fuli_test2");
		//创建规则
		request.id("2");
		request.timeout("1m");

		//将数据放入json
		request.source(JSON.toJSONString(user), XContentType.JSON);

		//客户端发送请求
		IndexResponse response = restHighLevelClient.index(request, RequestOptions.DEFAULT);
		System.out.println(response.toString());
		System.out.println(response.status());
	}

	//判断文档是否存在
	@Test
	void testIsExistsDocument() throws IOException {
		GetRequest request = new GetRequest("fuli_test2", "1");
		//不获取返回的_source 的上下文，这个可加可不加，加了提升查询效率
		request.fetchSourceContext(new FetchSourceContext(false));
		request.storedFields("_none_");

		boolean exists = restHighLevelClient.exists(request, RequestOptions.DEFAULT);
		System.out.println(exists);
	}

	//查询文档信息
	@Test
	void testGetDocument() throws IOException {
		GetRequest request = new GetRequest("fuli_test2", "1");
		GetResponse response = restHighLevelClient.get(request, RequestOptions.DEFAULT);
		System.out.println(response);
		System.out.println(response.getSourceAsString());
	}

	//更新文档信息
	@Test
	void testUpdateDocument() throws IOException {
		UpdateRequest request = new UpdateRequest("fuli_test2", "1");
		request.timeout("1m");

		User user = new User("福利大屁股", 12);
		request.doc(JSON.toJSONString(user), XContentType.JSON);


		UpdateResponse response = restHighLevelClient.update(request, RequestOptions.DEFAULT);
		System.out.println(response);
		System.out.println(response.status());
	}

	//删除文档
	@Test
	void testDeleteDocument() throws IOException{
		DeleteRequest request = new DeleteRequest("fuli_test2", "2");
		request.timeout("1m");
		DeleteResponse response = restHighLevelClient.delete(request, RequestOptions.DEFAULT);
		System.out.println(response);
		System.out.println(response.status());
	}


	//批量插入文档
	@Test
	void testBulkDocument() throws IOException{
		BulkRequest request = new BulkRequest();
		request.timeout("20s");

		List<User> userList = new ArrayList<>();
		userList.add(new User("福利11", 11));
		userList.add(new User("福利111", 111));
		userList.add(new User("福2利11", 131));
		userList.add(new User("福34利11", 131));

		//批处理请求
		for(int i=0;i < userList.size(); i ++){
			//批量更新、删除也是只修改这里
			request.add(
					new IndexRequest("fuli_test2")
					.id("" + (i + 1)) //不写id,会生成随机id
					.source(JSON.toJSONString(userList.get(i)), XContentType.JSON));
		}

		BulkResponse responses = restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
		System.out.println(responses);
		System.out.println(responses.status());
	}

	//查询
	@Test
	void testSearchDocument() throws IOException{
		SearchRequest request = new SearchRequest("fuli_test2");
		//构建查询条件
		SearchSourceBuilder builder = new SearchSourceBuilder();
		//查询条件，可以使用QueryBuilders工具来实现
		//精确查询：QueryBuilders.termQuery
		//匹配所有：QueryBuilders.matchAllQuery
		TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("name", "福利");
//		MatchAllQueryBuilder matchAllQueryBuilder = QueryBuilders.matchAllQuery();

		builder.query(termQueryBuilder);
		builder.timeout(new TimeValue(60, TimeUnit.SECONDS));

		request.source(builder);

		SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
		System.out.println(JSON.toJSONString(response.getHits()));

		//转成map
		System.out.println("===============");
		for(SearchHit documentFields:response.getHits().getHits()){
			System.out.println(documentFields.getSourceAsMap());
		}
	}
}
