package feedfacade

class Setting {

  String key
  String value

  static constraints = {
    key blank: false, unique: true
    value blank: false
  }

  static mapping = {
    table 'ff_setting'
  }
}
