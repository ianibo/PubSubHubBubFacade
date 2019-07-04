package feedfacade

import grails.transaction.Transactional

import java.text.SimpleDateFormat
import java.net.InetAddress;

import org.elasticsearch.client.Client
import org.elasticsearch.node.Node
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress

import static groovy.json.JsonOutput.*

// https://www.javadoc.io/doc/org.elasticsearch/elasticsearch/5.3.0
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders

import org.elasticsearch.index.reindex.DeleteByQueryAction 
import org.elasticsearch.index.reindex.BulkByScrollResponse


@Transactional
class ESWrapperService {

  TransportClient esclient = null;
  def grailsApplication

  String eshost

  @javax.annotation.PostConstruct
  def init() {
    // grails 3 changed log init, so logger may not be available post construct

    def eshost = grailsApplication.config.getProperty('eshost',String,'elasticsearch');

    log.debug("init ES wrapper service eshost:${eshost}");

    Settings settings = Settings.builder().put("cluster.name", "elasticsearch").build();
    esclient = new org.elasticsearch.transport.client.PreBuiltTransportClient(settings);
    esclient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(eshost), 9300));
  }

  def index(index,typename,id,record) {
    def result=null;
    try {
      // Convert the record to JSON
      // def json_string = toJson( record )
      // log.debug("Sending to ${index} ${typename} \n${json_string}\n");
      def future = esclient.prepareIndex(index,typename,id).setSource(record)
      result=future.get()
    }
    catch ( Exception e ) {
      log.error("Error processing ${toJson(record)}",e);
    }
    result
  }

  def index(index,typename,record) {
    def result=null;
    try {
      // Convert the record to JSON
      // def json_string = toJson( record )
      // log.debug("Sending to ${index} ${typename} \n${json_string}\n");
      def future = esclient.prepareIndex(index,typename).setSource(record)
      result=future.get()
    }
    catch ( Exception e ) {
      log.error("Error processing ${toJson(record)}",e);
    }
    result
  }

  def get(index,typename,id) {
    GetResponse response = esclient.prepareGet(index, typename, id)
        .setOperationThreaded(false)
        .get();

    response
  }

  def update(index,typename,id,record) {
    def result=null;
    try {
      // Convert the record to JSON
      // def json_string = toJson( record )
      // log.debug("Sending to ${index} ${typename} \n${json_string}\n");
      def future = esclient.prepareUpdate(index,typename,id).setUpsert(record)
      result=future.get()
    }
    catch ( Exception e ) {
      log.error("Error processing ${toJson(record)}",e);
    }
    result
  }

  def search(String[] indexes, String query_json) {
    def result=null;
    org.elasticsearch.action.search.SearchRequestBuilder srb = esclient.prepareSearch(indexes)
    srb.setQuery(QueryBuilders.wrapperQuery(query_json))
    result = srb.get()
    result
  }

  def search(String[] indexes, String query_json, int from, int num_results, String sort_field, String sort_direction) {
    def result=null;
    org.elasticsearch.action.search.SearchRequestBuilder srb = esclient.prepareSearch(indexes)
    srb.setQuery(QueryBuilders.wrapperQuery(query_json))
    srb.setSize(num_results)
    if ( sort_field ) {
      srb.addSort(sort_field, ( sort_direction?.equalsIgnoreCase('desc') ? org.elasticsearch.search.sort.SortOrder.DESC : org.elasticsearch.search.sort.SortOrder.ASC ) );
    }
    srb.setFrom(from);

    result = srb.get()
    result
  }


  def deleteByQuery(source, json_query) {
    BulkByScrollResponse response =
      DeleteByQueryAction.INSTANCE.newRequestBuilder(esclient)
        .filter(QueryBuilders.wrapperQuery(json_query)) 
        .source(source)                                  
        .get();                                             

    long deleted = response.getDeleted(); 

    log.info("Delete by query ${json_query} removed ${deleted} entries");
  }
}