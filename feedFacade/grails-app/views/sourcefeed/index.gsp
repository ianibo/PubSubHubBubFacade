<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Registered Source Feeds</title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>
  <div class="container-fluid">
    <div class="row">
      <div class="container" style="vertical-align: middle; text-align:center;">

        <h1>Registered Feeds (${totalFeeds} found)</h1>
        <g:set var="UTCZONE" value="${TimeZone.getTimeZone('Z')}"/>
        <g:set var="curime" value="${new Date()}"/>
        As at: <g:formatDate format="yyyy-MM-dd'T'HH:mm:ssz" date="${curime}" timeZone="${UTCZONE}"/> (UTC) /
        <g:formatDate format="yyyy-MM-dd'T'HH:mm:ssz" date="${curime}"/> (Server)
        
        <g:if test="${(blocked_feeds?:0) > 0}">
          <h3>Warning: there are currently ${blocked_feeds} feeds in-process for longer than expected.
            <sec:ifLoggedIn>You can <g:link action="releaseBlockedFeeds">Release Manually</g:link></sec:ifLoggedIn>
          </h3>
        </g:if>

        <g:form controller="sourcefeed" action="index" method="get" class="form">
          <div class="input-group">
              <input type="text" name="q" class="form-control" placeholder="Text input" value="${params.q}">
              <span class="input-group-addon"><input type="checkbox" name="filterHasErrors" value="on" ${params.filterHasErrors=='on'?'checked':''}> Has Errors</span>
              <span class="input-group-addon"><input type="checkbox" name="filterEnabled" value="on" ${params.filterEnabled=='on'?'checked':''}> Enabled</span>
              <span class="input-group-addon"><input type="checkbox" name="filterInProcess" value="on" ${params.filterInProcess=='on'?'checked':''}> In Process</span>
              <span class="input-group-btn"><button type="submit" class="btn btn-primary">Search</button></span>
              <input type="hidden" name="max" value="${params.max}"/>
          </div>
        </g:form>

        <div class="pagination">
          <g:paginate controller="sourcefeed" action="index" total="${totalFeeds}" next="Next" 
                      prev="Previous" omitNext="false" omitPrev="false" 
                      params="${params}"
                      />
        </div>

      </div>
    </div>
    <div class="row">
      <div class="container-fluid">
      
        <g:each in="${feeds}" var="f" >
          <div class="card">
            <div class="card-header">
              <h3><a href="${f.baseUrl}">${f.baseUrl}</a> (<g:link controller="sourcefeed" action="feed" id="${f.uriname}">${f.uriname}</g:link>)</h3>
            </div>
            <div class="card-body">
              <table class="table table-striped well">
                <thead>
                  <tr>
                    <th>Enabled</th>
                    <th>Status/Health</th>
                    <th>Topics</th>
                    <th>Tags</th>
                    <th>Fetcher Status</th>
                    <th>CAP Status</th>
                    <th>Last Completed</th>
                    <th>Poll Interval</th>
                    <th>Next Due</th>
                    <th>HTTP Last Modified</th>
                    <th>HTTP Expires</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>
                      ${f.enabled?'Yes':'No'}
                      <sec:ifAllGranted roles='ROLE_ADMIN'>
                        <br/><g:link controller="sourcefeed" action="toggleSourceEnabled" id="${f.uriname}">toggle</g:link>
                      </sec:ifAllGranted>
                    </td>
                    <td>
                      <g:if test="${f.feedStatus=='ERROR'}">
                        <div class="alert alert-danger" role="alert">
                          <span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true"></span>
                          <span class="sr-only">Error</span>
                        </div>
                      </g:if>
                      <g:if test="${f.feedStatus=='OK'}">
                        <div class="alert alert-success" role="alert">
                          <span class="glyphicon glyphicon-ok" aria-hidden="true"></span>
                          <span class="sr-only">OK</span>
                        </div>
                      </g:if>
                      <g:if test="${f.feedStatus!='OK' && f.feedStatus!='ERROR'}">
                        ${f.feedStatus?:'Unset'}
                      </g:if>
                      <g:if test="${f.latestHealth}">
                        health:${f.latestHealth}
                      </g:if>
                    </td>
                    <td><ul><g:each in="${f.topics}" var="topic"><li>${topic.topic.name}</li></g:each></ul></td>
                    <td><ul><g:each in="${f.tags}" var="tv"><li>${tv.tag.tag}: <strong>${tv.value}</strong></li></g:each></ul></td>
                    <td>
                      ${f.status}
                      <g:if test="${f.status=='in-process'}">
                        <br/>started: <br/><g:formatDate date="${new Date(f.lastStarted)}" format="yyyy-MM-dd HH:mm:ssz" timeZone="${UTCZONE}"/>
                        <br/>elapsed: <br/>${System.currentTimeMillis() - f.lastStarted}
                      </g:if>
                    </td>
                    <td>${f.capAlertFeedStatus}</td>
                    <td><g:formatDate date="${new Date(f.lastCompleted)}" format="yyyy-MM-dd HH:mm:ssz" timeZone="${UTCZONE}"/></td>
                    <td>${f.pollInterval}</td>
                    <td> 
                      <g:formatDate date="${new Date(f.nextPollTime)}" format="yyyy-MM-dd HH:mm:ssz" timeZone="${UTCZONE}"/>
                    </td>
                    <td>${f.httpLastModified}</td>
                    <td>${f.httpExpires}</td>
                  </tr>
                  <g:set var="latest_issues" value="${f.latestIssues(5)}"/>
                  <tr>
                    <g:if test="${latest_issues.size() > 0 }">
                      <td colspan="12">
                        <table class="table table-striped">
                          <thead>
                            <tr> 
                              <th>Key</th>
                              <th>Message</th>
                              <th>First Seen</th>
                              <th>Last Seen</th>
                              <th>Repeated</th>
                            </tr>
                          </thead>
                          <tbody>
                            <g:each in="${latest_issues}" var="li">
                              <tr>
                                <td>${li.key} </td>
                                <td>${li.message} </td>
                                <td><g:formatDate date="${new Date(li.firstSeen)}" format="yyyy-MM-dd HH:mm:ssz" timeZone="${UTCZONE}"/></td>
                                <td><g:formatDate date="${new Date(li.lastSeen)}" format="yyyy-MM-dd HH:mm:ssz" timeZone="${UTCZONE}"/></td>
                                <td>${li.occurrences} </td>
                              </tr>
                            </g:each>
                          </tbody>
                        </table>
                      </td>
                    </g:if>
                  </tr>
                  <tr>
                    <td colspan="12">
                      <g:set var="stats" value="${f.getHistogramLastDay()}"/>
                      <table class="table">
                        <thead>
                          <tr>
                            <th class="col-md-1">Hour (UTC)&nbsp;</th>
                            <g:each in="${stats}" var="h">
                              <th>${h.hour}</th>
                            </g:each>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                           <th>Error Count &nbsp;</th>
                            <g:each in="${stats}" var="h">
                              <td>${h.errorCount}</td>
                            </g:each>
                          </tr>
                          <tr>
                           <th>Success Count &nbsp;</th>
                            <g:each in="${stats}" var="h">
                              <td>${h.successCount}</td>
                            </g:each>
                          </tr>
                          <tr>
                           <th>New Entries</th>
                            <g:each in="${stats}" var="h">
                              <td>${h.newEntryCount}</td>
                            </g:each>
                          </tr>
                          <tr>
                           <th>Health</th>
                            <g:each in="${stats}" var="h">
                              <td>${h.health}</td>
                            </g:each>
                          </tr>
                        </tbody>
                      </table>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </g:each>
    
        <g:link class="btn" action="registerFeed">Register New Feed</g:link>
  
      </div>
    </div>
  </div>
</body>
</html>
