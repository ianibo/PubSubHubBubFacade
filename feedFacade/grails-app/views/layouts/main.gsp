<!doctype html>
<html lang="en" class="no-js">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <title>
        <g:layoutTitle default="Library Resource Manager"/>
    </title>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>

    <asset:stylesheet src="application.css"/>

    <g:layoutHead/>
</head>
<body>

  <div class="navbar navbar-default navbar-fixed-top">
    <div class="container-fluid">
      <div class="navbar-header">
        <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
        </button>
        <a class="navbar-brand" href="#">FeedFaçade</a>
      </div>
      <div class="collapse navbar-collapse">
        <ul class="nav navbar-nav">
          <li class="${controllerName=='home' && actionName=='index' ? 'active' : ''}"><g:link controller="home" action="index">Home</g:link></li>
          <li class="${controllerName=='sourcefeed' && actionName=='index' ? 'active' : ''}"><g:link controller="sourcefeed" action="index">Feeds</g:link></li>
          <li class="${controllerName=='topics' && actionName=='index' ? 'active' : ''}"><g:link controller="topics" action="index">Topics</g:link></li>
          <li class="dropdown">
            <a href="#" class="dropdown-toggle" data-toggle="dropdown">Admin <b class="caret"></b></a>
            <ul class="dropdown-menu">
              <li class="${controllerName=='admin' && actionName=='feedCheckerLog' ? 'active' : ''}"><g:link controller="admin" action="feedCheckerLog">Feed Checker Log</g:link></li>
              <li class="${controllerName=='admin' && actionName=='notificationLog' ? 'active' : ''}"><g:link controller="admin" action="notificationLog">Notifications</g:link></li>
              <li class="${controllerName=='sourcefeed' && actionName=='registerFeed' ? 'active' : ''}"><g:link controller="sourcefeed" action="registerFeed">Register Feed</g:link></li>
              <!--
              <li class="divider"></li>
              <li class="dropdown-header">Nav header</li>
              <li><a href="#">Separated link</a></li>
              -->
            </ul>
          </li>
        </ul>
      </div><!--/.nav-collapse -->
    </div>
  </div>
  
  <g:layoutBody/>

  <asset:javascript src="application.js"/>

</body>
</html>
