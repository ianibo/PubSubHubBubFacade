package feedfacade

import grails.transaction.Transactional
import java.security.MessageDigest
import org.apache.commons.io.input.BOMInputStream
import java.text.SimpleDateFormat
import static groovy.json.JsonOutput.*
import java.text.SimpleDateFormat
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher

@Transactional
class FeedCheckerService {

  def executorService
  def running = false;
  def error_count = 0;
  def newEventService
  def statsService
  def feedCheckLog=new org.apache.commons.collections.buffer.CircularFifoBuffer(100);
  RabbitMessagePublisher rabbitMessagePublisher

  def possible_date_formats = [
    // new SimpleDateFormat('yyyy-MM-dd'), // Default format Owen is pushing ATM.
    // new SimpleDateFormat('yyyy/MM/dd'),
    // new SimpleDateFormat('dd/MM/yyyy'),
    // new SimpleDateFormat('dd/MM/yy'),
    // new SimpleDateFormat('yyyy/MM'),
    // new SimpleDateFormat('yyyy')
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX"),
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
  ];

  def getLastLog() {
    feedCheckLog
  }

  def isRunning() { 
    running
  }

  def getFeedCheckLog() {
    feedCheckLog
  }


  def triggerFeedCheck() {
    log.debug("FeedCheckerService::triggerFeedCheck thread pool count ${executorService.executor.getActiveCount()}");
    if ( running ) {
      log.debug("Feed checker already running - not launching another [${error_count++}]");
    }
    else {
      def error_count = 0;
      doFeedCheck()
    }
  }

  def doFeedCheck() {
    def start_time = System.currentTimeMillis()
    def start_time_as_date = new Date(start_time)
    log.debug("FeedCheckerService::doFeedChecki ${start_time}");
    running=true;
    feedCheckLog=[]
    feedCheckLog.add([timestamp:new Date(),message:'Feed check started']);
    log.debug("Finding all feeds due on or after ${start_time}");
    def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

    logEvent('System.notification',[
        timestamp:new Date(),
        message:"Feed Check Started at ${start_time} ${sdf.format(start_time_as_date)}"
    ]);

    def processed_feed_counter = 0;

    try {
      def cont = true
      while ( cont ) {

        log.debug("Processing feed ${++processed_feed_counter}");

        // Grab the next feed to examine -- do it in a transaction
        def feed_info = null
        SourceFeed.withNewTransaction {
          log.debug("Searching for paused feeds where lastCompleted+pollInterval < now ${start_time}");

          def q = SourceFeed.executeQuery('select sf.id, sf.baseUrl, sf.lastHash, sf.highestTimestamp, sf.httpExpires, sf.httpLastModified, sf.uriname from SourceFeed as sf where sf.baseUrl is not null and sf.status=:paused AND sf.lastCompleted + sf.pollInterval < :ctm and ( sf.capAlertFeedStatus = :operating or capAlertFeedStatus = :testing ) order by (sf.lastCompleted + sf.pollInterval) asc',
                                           [paused:'paused',ctm:start_time,operating:'operating',testing:'testing'],[lock:false])

          def num_paused_feeds = q.size();
          log.debug("feedChecher detects ${num_paused_feeds} feeds paused that are overdue a check");

          if ( num_paused_feeds > 0 ) {
            def row = q.get(0)
            feed_info = [:]
            feed_info.id = row[0]
            feed_info.url = row[1]
            feed_info.hash = row[2]
            feed_info.highesTimestamp = row[3]
            feed_info.expires = row[4]
            feed_info.lastModified = row[5]
            feed_info.uriname = row[6]
          }
         
        }

        if ( feed_info ) {
          feedCheckLog.add([timestamp:new Date(),message:'Identified feed '+feed_info]);
          log.debug("Process feed");
            processFeed(start_time, 
                        feed_info.id,
                        feed_info.uriname,
                        feed_info.url,
                        feed_info.hash,
                        feed_info.highesTimestamp,
                        feed_info.expires,
                        feed_info.lastModified);
        }
        else {  
          // nothing left in the queue
          log.debug("Nothing left to process.. Continue");
          cont = false
        }
      }
    }
    catch ( Exception e ) {
      feedCheckLog.add([type:'ERROR', timestamp:new Date(),message:'Feed check error '+e.message]);
      log.error("Problem processing feeds",e);
      e.printStackTrace()
    }
    finally {
      log.info("processed ${processed_feed_counter} feeds");
    }

    logEvent('System.notification',[
        timestamp:new Date(),
        message:"Feed Check Ended at ${sdf.format(new Date())} tpc:${executorService.executor.getActiveCount()}"
    ]);

    feedCheckLog.add([timestamp:new Date(),message:'Feed check finished tpc:'+executorService.executor.getActiveCount()]);
    running=false;
  }

  def processFeed(start_time, 
                  id, 
                  uriname, 
                  url, 
                  hash, 
                  highestRecordedTimestamp,
                  httpExpires,
                  httpLastModified) {

    log.debug("processFeed(${start_time},${id},${url},${hash},${highestRecordedTimestamp})");
    def newhash = null;
    def highestSeenTimestamp = null;
    def error = false
    def error_message = null
    def new_entry_count = 0

    logEvent('Feed.'+uriname,[
      timestamp:new Date(),
      message:"Checking feed ${uriname} / ${url} (${Thread.currentThread().getName()})",
      relatedType:"feed",
      relatedId:uriname
    ]);

    def continue_processing = false;

    SourceFeed.withNewTransaction {
      log.debug('Mark feed as in-process');
      def sf = SourceFeed.get(id)

      sf.lock()
      if ( sf.status == 'paused' ) {

        if ( url.toLowerCase().startsWith('http') ) {
          log.debug("Feed really is paused -- mark it as in process and proceed");
          sf.status = 'in-process'
          continue_processing = true;
        }
        else {
          sf.capAlertFeedStatus = 'error'
          sf.lastError='Feed URL seems to be malformed - must start http:// or https:// feed status set to error'
          logEvent('Feed.'+uriname,[
            timestamp:new Date(),
            message:"Invalud URL - must start http: or https: for ${uriname} - ${url}",
            relatedType:"feed",
            relatedId:uriname
          ]);
        }

        sf.save(flush:true, failOnError:true);
      }
      else {
        log.debug("On more thorough inspection, someone else already grabbed the feed to process, so skip");
      }
    }

    if ( continue_processing ) {

      runAsync {
        log.debug("Processing....feed ${id} :: ${url} ${hash}");
        
        def feed_info = null;
  
        try {
  
          feed_info = fetchFeedPage(url, httpExpires, httpLastModified);
  
          // If we got a hash back from fetching the page AND the storred hash is different OR not set, then process the feed.
          if ( ( feed_info.hash != null ) && 
               ( ( hash == null ) || ( feed_info.hash != hash ) ) ) {
            newhash = feed_info.hash
            log.debug("Detected hash change (old:${hash},new:${feed_info.hash}).. Process");
      
            def processing_result = getNewEntries(id, new java.net.URL(url).openStream(), highestRecordedTimestamp)
            new_entry_count = processing_result.numNewEntries
            processing_result.newEntries.each { entry ->
  
              logEvent('Feed.'+uriname,[
                timestamp:new Date(),
                message:"Detected new entry ${entry.id.text()}",
                relatedType:"entry",
                relatedId:uriname+'/'+entry.id.text()
              ]);
  
              newEventService.handleNewEvent(id,entry)
            }
      
            if ( new_entry_count > 0 ) {
              logEvent('Feed.'+uriname,[
                timestamp:new Date(),
                message:"${uriname} Processing complete (${url}) - ${new_entry_count} new entries",
                relatedType:"feed",
                relatedId:uriname
              ]);
            }
  
            if ( processing_result.highestSeenTimestamp ) {
              highestSeenTimestamp = processing_result.highestSeenTimestamp
            }
          }
          else {
            log.debug("${url} unchanged");
          }
        }
        catch ( java.io.FileNotFoundException fnfe ) {
          error=true
          error_message = fnfe.toString()
          log.error("Feed seems not to exist",fnfe.message);
          logEvent('Feed.'+uriname,[
            timestamp:new Date(),
            message:fnfe.toString(),
            relatedType:"feed",
            relatedId:uriname
          ]);
        }
        catch ( java.io.IOException ioe ) {
          error=true
          error_message = ioe.toString()
          log.error("IO Problem feed_id:${id} feed_url:${url} ${ioe.message}",ioe);
          logEvent('Feed.'+uriname,[
            timestamp:new Date(),
            message:ioe.toString(),
            relatedType:"feed",
            relatedId:uriname
          ]);
        }
        catch ( Exception e ) {
          error=true
          error_message = e.toString()
          log.error("problem fetching feed",e);
          logEvent('Feed.'+uriname,[
            timestamp:new Date(),
            message:e.toString(),
            relatedType:"feed",
            relatedId:uriname
          ]);
        }
    
        log.debug("After processing entries, highest timestamp seen is ${highestSeenTimestamp}");
    
        SourceFeed.withNewTransaction {
          log.debug("Mark feed ${id} as paused");
          def sf = SourceFeed.get(id)
          sf.lock()
          sf.status = 'paused'
          sf.httpExpires = feed_info?.expires
          sf.httpLastModified = feed_info?.lastModified
  
          if ( newhash ) {
            log.debug("Updating hash to ${newhash}");
            sf.lastHash = newhash
          }
  
          if ( highestSeenTimestamp ) {
            log.debug("Updating sf.highestTimestamp to be ${highestSeenTimestamp}");
            sf.highestTimestamp = highestSeenTimestamp
          }
          // sf.lastCompleted=start_time
          // Use the actual last completed time to try and even out the feed checking over time - this will skew each feed
          // So that all feeds become eligible over time, rather than being based on the start time of the batch
          sf.lastCompleted=System.currentTimeMillis();
          sf.lastElapsed=start_time-sf.lastCompleted
          sf.lastError=error_message
    
          if ( error ) {
  
            sf.feedStatus='ERROR'
            statsService.logFailure(sf,start_time);
  
            logEvent('Feed.'+uriname,[
              timestamp:new Date(),
              message:'Feed status : ERROR '+error_message,
              relatedType:"feed",
              relatedId:uriname
            ]);
          }
          else { 
            sf.feedStatus='OK'
            statsService.logSuccess(sf,start_time,new_entry_count);
          }
    
          log.debug("Saving source feed");
          feedCheckLog.add([timestamp:new Date(),message:"Processing completed on ${id}/${url} at ${sf.lastCompleted} / ${error_message}"]);
          sf.save(flush:true, failOnError:true);
        }
      }
    }


    feedCheckLog.add([timestamp:new Date(),message:"Process feed completed :: ${id} ${url} / error:${error} ${error_message}"]);
    log.debug("processFeed ${id} returning");
  }


  /**
   * @Param feed_address
   * @Param httpExpires expires header from the last time we fetched this page
   * @Param httpLastModified last modified header from the last time we fetched this page
   *
   * @See http://stackoverflow.com/questions/7095897/im-trying-to-use-javas-httpurlconnection-to-do-a-conditional-get-but-i-neve
   * 
   */
  def fetchFeedPage(feed_address,httpExpires, httpLastModified) {
    log.debug("fetchFeedPage(${feed_address})");
    def result = [:]
    java.net.URL feed_url = new java.net.URL(feed_address)

    java.net.URLConnection url_connection = feed_url.openConnection()
    url_connection.setConnectTimeout(4000)
    url_connection.setReadTimeout(4000)
    // Set this to the time we last checked the feed. uc.setIfModifiedSince(System.currentTimeMillis());
    if ( httpLastModified != null ) {
      log.debug("${feed_address} has last modified ${httpLastModified} so sending that in a If-Modified-Since header");
      // url_connection.setRequestProperty("If-Modified-Since", httpLastModified);
      url_connection.setIfModifiedSince(Long.parseLong(httpLastModified));
    }
   
    result.lastModified = url_connection.getLastModified()
    result.expires = url_connection.getExpiration()
    log.debug("${feed_address} [URLC]expires: ${result.expires}");
    log.debug("${feed_address} [URLC]ifModifiedSince: ${url_connection.getIfModifiedSince()}");
    log.debug("${feed_address} [URLC]lastModified: ${result.lastModified}");

    // If you get an Expires response header, then it just means that you don't need to request anything until the specified expire time. 
    // If you get a Last-Modified response header, then it means that you should be able to use If-Modified-Since to test it. 
    // If you get an ETag response header, then it means that you should be able to use If-None-Match to test it.

    log.debug("Connection response code: ${url_connection.getResponseCode()}");

    // If we had no lastModified OR the last modified returned was different
    if ( ( result.lastModified == null ) ||
         ( result.lastModified != httpLastModified ) ) {
      log.debug("${feed_address} **FEEDSTATUS** updated (req lm:${result.lastModified}/db lm:${httpLastModified})");
      // result.feed_text = feed_url.getText([connectTimeout: 2000, readTimeout: 3000])
      result.feed_text = url_connection.getInputStream().getText()
      MessageDigest md5_digest = MessageDigest.getInstance("MD5");
      md5_digest.update(result.feed_text.getBytes())
      byte[] md5sum = md5_digest.digest();
      result.hash = new BigInteger(1, md5sum).toString(16);
    }
    else {
      log.debug("${feed_address} **FEEDSTATUS** Unchanged since ${result.lastModified}");
    }

    result
  }

  def getNewEntries(id, feed_is, highestRecordedTimestamp) {
    def result = [:]
    result.numNewEntries=0
    result.newEntries=[]

    def atom_ns = new groovy.xml.Namespace("http://www.w3.org/2005/Atom", 'atom')
    // http://docs.groovy-lang.org/latest/html/api/groovy/util/XmlParser.html
    // def rootNodeParser = new XmlParser(false,false,true)
    def rootNodeParser = new XmlParser()

    def bom_is = new BOMInputStream(feed_is)
    if (bom_is.hasBOM() == false) {
      log.debug("No BOM in input stream");
    }
    else {
      log.debug("BOM detected in input stream");
    }

    // rootNodeParser.setFeature('http://apache.org/xml/features/disallow-doctype-decl',false);
    log.debug("Parse...");
    def rootNode = rootNodeParser.parse(bom_is)

    // If using namespaces:: rootNode.[atom_ns.entry].each { entry ->
    log.debug("Processing...");
    rootNode.entry.each { entry ->

      def entry_updated_time = parseDate(entry.updated.text()).getTime();
      
      log.debug("${entry.id.text()} :: ${entry_updated_time}");

      // Keep track of the highest timestamp we have seen in this pass over the changed feed
      if ( entry_updated_time && ( ( result.highestSeenTimestamp == null ) || ( result.highestSeenTimestamp < entry_updated_time ) ) ) {
        result.highestSeenTimestamp = entry_updated_time
      }

      // See if this entry has a timestamp greater than any we have seen so far
      if ( entry_updated_time > highestRecordedTimestamp ?: 0 ) {
        log.debug("    -> ${entry.id.text()} has a timestamp (${entry_updated_time} > ${highestRecordedTimestamp} so process it");
        result.numNewEntries++
        result.newEntries.add(entry)
      }
    }

    log.debug("Found ${result.numNewEntries} new entries, highest timestamp seen ${result.highestSeenTimestamp}, highest timestamp recorded ${highestRecordedTimestamp}");
    result
  }

  /**
   * Dates can come in many different formats, use the list defined in possible_date_formats as a list of possible formats.
   */
  Date parseDate(String datestr) {
    def parsed_date = null;
    if ( datestr && ( datestr.length() > 0 ) ) {
      for(Iterator<SimpleDateFormat> i = possible_date_formats.iterator(); ( i.hasNext() && ( parsed_date == null ) ); ) {
        try {
          parsed_date = i.next().clone().parse(datestr);
        }
        catch ( Exception e ) {
        }
      }
    }
    parsed_date
  }

  def logEvent(key,evt) {
   try {

      log.debug("logEvent(${key},${evt})");

      def evt_str = toJson(evt);

      def result = rabbitMessagePublisher.send {
                      exchange = "FeedFetcher"
                      routingKey = key
                      body = toJson(evt)
                   }
    }
    catch ( Exception e ) {
      log.error("Problem trying to publish to rabbit",e);
    }
  }
}
