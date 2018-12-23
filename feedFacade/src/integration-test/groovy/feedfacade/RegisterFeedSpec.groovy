package feedfacade

import grails.testing.mixin.integration.Integration
import grails.transaction.*

import spock.lang.*
import geb.spock.*

/**
 * See http://www.gebish.org/manual/current/ for more instructions
 */
@Integration
@Rollback
@Stepwise
class RegisterFeedSpec extends GebSpec {

  def setup() {
  }

  def cleanup() {
  }

  void "Navigate to Register Test Feed page and Login"() {
    when:"Navigate to the new feed page and fill out form"
      go '/sourcefeed/registerFeed'
      $("form").username = 'admin'
      $("form").password = 'admin'
      //    $("form").feedname = 'localTest'
      //    $("form").baseUrl = 'file:testfeed.xml'
      //    $("form").submit
      $('#submit').click()

    then:"The title is Login"
      title == "Register Feed"
  }
}
